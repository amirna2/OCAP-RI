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

package org.cablelabs.impl.manager.filesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppIcon;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseFilter;
import org.dvb.io.persistent.FileAttributes;
import org.ocap.application.AppManagerProxy;

import org.cablelabs.impl.io.DefaultWriteableFileSys;
import org.cablelabs.impl.io.FileSysManager;
import org.cablelabs.impl.io.StorageMediaFullException;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FileManagerTest;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ManagerTest;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.MPEEnv;

public class FileManagerImplTest extends TestCase
{
    public void testDummy() throws Exception
    {

    }

    /**
     * Tests the getFileSys(), registerWriteableMount() and
     * unregisterWriteableMount() methods by registering some mount points and
     * then tries to retrive the writeable file systems associated with the
     * mounts. This essentially tests the support that's included in the stack
     * for the OCAP.filesys.persistent property that lets systems define the
     * persistent storage devices that require purging support.
     */
    public void testRegisterWriteableMount()
    {

        // First register some example mount points.
        filemgr.registerWriteableMount(TESTDIR);
        new File(TESTDIR + "/test0Dir").mkdir();
        filemgr.registerWriteableMount(TESTDIR + "/test0Dir");
        new File(TESTDIR + "/test1Dir").mkdir();
        filemgr.registerWriteableMount(TESTDIR + "/test1Dir=10000");
        new File(TESTDIR + "/test2Dir").mkdir();
        filemgr.registerWriteableMount(TESTDIR + "/test2Dir=200k");
        new File(TESTDIR + "/test3Dir").mkdir();
        filemgr.registerWriteableMount(TESTDIR + "/test3Dir=300K");
        new File(TESTDIR + "/test4Dir").mkdir();
        filemgr.registerWriteableMount(TESTDIR + "/test4Dir=40m");
        new File(TESTDIR + "/test5Dir").mkdir();
        filemgr.registerWriteableMount(TESTDIR + "/test5Dir=50M");
        new File(TESTDIR + "/test6Dir").mkdir();
        filemgr.registerWriteableMount(TESTDIR + "/test6Dir=60g");
        new File(TESTDIR + "/test7Dir").mkdir();
        filemgr.registerWriteableMount(TESTDIR + "/test7Dir=70G");

        // next see if we can retrieve them
        assertTrue("Did not get a PersistentFileSys for path=/syscwd/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/xyz") instanceof PersistentFileSys);
        assertTrue("Did not get a PersistentFileSys for path=/syscwd/junit/test0Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test0Dir/xyz") instanceof PersistentFileSys);
        assertTrue("Did not get a QuotaFileSys for path=/syscwd/junit/test1Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test1Dir/xyz") instanceof QuotaFileSys);
        assertTrue("Did not get a QuotaFileSys for path=/syscwd/junit/test2Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test2Dir/xyz") instanceof QuotaFileSys);
        assertTrue("Did not get a QuotaFileSys for path=/syscwd/junit/test3Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test3Dir/xyz") instanceof QuotaFileSys);
        assertTrue("Did not get a QuotaFileSys path=/syscwd/junit/test4Dir/xyz", filemgr.getWriteableFileSys(TESTDIR
                + "/test4Dir/xyz") instanceof QuotaFileSys);
        assertTrue("Did not get a QuotaFileSys path=/syscwd/junit/test5Dir/xyz", filemgr.getWriteableFileSys(TESTDIR
                + "/test5Dir/xyz") instanceof QuotaFileSys);
        assertTrue("Did not get a QuotaFileSys path=/syscwd/junit/test6Dir/xyz", filemgr.getWriteableFileSys(TESTDIR
                + "/test6Dir/xyz") instanceof QuotaFileSys);
        assertTrue("Did not get a QuotaFileSys path=/syscwd/junit/test7Dir/xyz", filemgr.getWriteableFileSys(TESTDIR
                + "/test7Dir/xyz") instanceof QuotaFileSys);

        // Now unregister each of the writeable mounts.
        filemgr.unregisterWriteableMount(TESTDIR);
        filemgr.unregisterWriteableMount(TESTDIR + "/test0Dir");
        filemgr.unregisterWriteableMount(TESTDIR + "/test1Dir=10000");
        filemgr.unregisterWriteableMount(TESTDIR + "/test2Dir=200k");
        filemgr.unregisterWriteableMount(TESTDIR + "/test3Dir=300K");
        filemgr.unregisterWriteableMount(TESTDIR + "/test4Dir=40m");
        filemgr.unregisterWriteableMount(TESTDIR + "/test5Dir=50M");
        filemgr.unregisterWriteableMount(TESTDIR + "/test6Dir=60g");
        filemgr.unregisterWriteableMount(TESTDIR + "/test7Dir=70G");

        // Now verify that we get the default writeable file system for
        // unregisters references.
        assertTrue("Did not get DefaultWriteableFileSys for /abc/xyz",
                filemgr.getWriteableFileSys("/abc/xyz") instanceof DefaultWriteableFileSys);
        assertTrue("Did not get DefaultWriteableFileSys for /syscwd/xyz",
                filemgr.getWriteableFileSys("/syscwd/xyz") instanceof DefaultWriteableFileSys);
        assertTrue("Did not get DefaultWriteableFileSys for /syscwd/junit/test0Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test0Dir/xyz") instanceof DefaultWriteableFileSys);
        assertTrue("Did not get DefaultWriteableFileSys for /syscwd/junit/test1Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test1Dir/xyz") instanceof DefaultWriteableFileSys);
        assertTrue("Did not get DefaultWriteableFileSys for /syscwd/junit/test2Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test2Dir/xyz") instanceof DefaultWriteableFileSys);
        assertTrue("Did not get DefaultWriteableFileSys for /syscwd/junit/test3Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test3Dir/xyz") instanceof DefaultWriteableFileSys);
        assertTrue("Did not get DefaultWriteableFileSys for /syscwd/junit/test4Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test4Dir/xyz") instanceof DefaultWriteableFileSys);
        assertTrue("Did not get DefaultWriteableFileSys for /syscwd/junit/test5Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test5Dir/xyz") instanceof DefaultWriteableFileSys);
        assertTrue("Did not get DefaultWriteableFileSys for /syscwd/junit/test6Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test6Dir/xyz") instanceof DefaultWriteableFileSys);
        assertTrue("Did not get DefaultWriteableFileSys for /syscwd/junit/test7Dir/xyz",
                filemgr.getWriteableFileSys(TESTDIR + "/test7Dir/xyz") instanceof DefaultWriteableFileSys);

    }

    public void testDefaultWriteableFileSys()
    {
        String msg = "Hello world!";

        // First test FileOutputStream class...
        try
        {
            File testFile = new File(TESTDIR + "/FileOutStreamTest.txt");
            byte data[] = new byte[256];
            testFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(testFile);
            fos.write(msg.getBytes());
            fos.close();
            FileInputStream fis = new FileInputStream(testFile);
            fis.read(data, 0, (int) testFile.length());
            String s = new String(data, 0, (int) testFile.length());
            fis.close();
            testFile.delete();

            assertTrue("File data does not match data written to file", msg.compareTo(s) == 0);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }

        // Next, test RandomAccessFile class...
        try
        {
            File testFile = new File(TESTDIR + "/RandomAccessFileTest.txt");
            byte data[] = new byte[256];
            testFile.createNewFile();
            RandomAccessFile raf = new RandomAccessFile(testFile, "rw");
            raf.write(msg.getBytes());
            raf.close();
            FileInputStream fis = new FileInputStream(testFile);
            fis.read(data, 0, (int) testFile.length());
            String s = new String(data, 0, (int) testFile.length());
            fis.close();
            testFile.delete();

            assertTrue("File data does not match data written to file", msg.compareTo(s) == 0);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }

        // Now try using a RandomAccessFile.getFD() FD to intantiate a
        // FileOutputStream...
        try
        {
            File testFile = new File(TESTDIR + "/FileDescriptorTest.txt");
            byte data[] = new byte[256];
            testFile.createNewFile();
            RandomAccessFile raf = new RandomAccessFile(testFile, "rw");

            FileOutputStream fos = new FileOutputStream(raf.getFD());
            fos.write(msg.getBytes());
            fos.close();
            raf.close();
            FileInputStream fis = new FileInputStream(testFile);
            fis.read(data, 0, (int) testFile.length());
            String s = new String(data, 0, (int) testFile.length());
            fis.close();
            testFile.delete();

            assertTrue("File data does not match data written to file", msg.compareTo(s) == 0);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }

    }

    // Just simply test that the correct chain of file systems is called.
    public void testBasicPersistentFileSys()
    {
        String msg = "Hello world!";

        File dir = new File(TESTDIR + "/PersistentDir");
        dir.mkdir();
        filemgr.registerWriteableMount(TESTDIR + "/PersistentDir");

        // First test FileOutputStream class...
        try
        {
            File testFile = new File(TESTDIR + "/PersistentDir/FileOutStreamTest.txt");
            byte data[] = new byte[256];
            testFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(testFile);
            fos.write(msg.getBytes());
            fos.close();
            FileInputStream fis = new FileInputStream(testFile);
            fis.read(data, 0, (int) testFile.length());
            String s = new String(data, 0, (int) testFile.length());
            fis.close();
            testFile.delete();

            assertTrue("File data does not match data written to file", msg.compareTo(s) == 0);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }

        // Next, test RandomAccessFile class...
        try
        {
            File testFile = new File(TESTDIR + "/PersistentDir/RandomAccessFileTest.txt");
            byte data[] = new byte[256];
            testFile.createNewFile();
            RandomAccessFile raf = new RandomAccessFile(testFile, "rw");
            raf.write(msg.getBytes());
            raf.close();
            FileInputStream fis = new FileInputStream(testFile);
            fis.read(data, 0, (int) testFile.length());
            String s = new String(data, 0, (int) testFile.length());
            fis.close();
            testFile.delete();

            assertTrue("File data does not match data written to file", msg.compareTo(s) == 0);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }

        // Now try using a RandomAccessFile.getFD() FD to intantiate a
        // FileOutputStream...
        try
        {
            File testFile = new File(TESTDIR + "/PersistentDir/FileDescriptorTest.txt");
            byte data[] = new byte[256];
            testFile.createNewFile();
            RandomAccessFile raf = new RandomAccessFile(testFile, "rw");

            FileOutputStream fos = new FileOutputStream(raf.getFD());
            fos.write(msg.getBytes());
            fos.close();
            raf.close();
            FileInputStream fis = new FileInputStream(testFile);
            fis.read(data, 0, (int) testFile.length());
            String s = new String(data, 0, (int) testFile.length());
            fis.close();
            testFile.delete();

            assertTrue("File data does not match data written to file", msg.compareTo(s) == 0);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }

        // Next, test setLength() method of RandomAccessFile class...
        try
        {
            File testFile = new File(TESTDIR + "/PersistentDir/SetLengthTest.txt");
            byte data[] = new byte[256];
            testFile.createNewFile();
            RandomAccessFile raf = new RandomAccessFile(testFile, "rw");
            raf.setLength(msg.length() * 2);
            assertTrue("File size not set to correct length", raf.length() == msg.length() * 2);
            raf.write(msg.getBytes());
            raf.setLength(msg.length());
            assertTrue("File size not set to correct length", raf.length() == msg.length());
            raf.close();
            FileInputStream fis = new FileInputStream(testFile);
            fis.read(data, 0, (int) testFile.length());
            String s = new String(data, 0, (int) testFile.length());
            fis.close();
            testFile.delete();

            assertTrue("File data does not match data written to file", msg.compareTo(s) == 0);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }

        filemgr.unregisterWriteableMount(TESTDIR + "/PersistentDir");
        dir.delete();
    }

    // Just simply test that the correct chain of file systems is called.
    public void testBasicQuotaFileSys()
    {
        String msg = "Hello world!";

        File dir = new File(TESTDIR + "/QuotaDir");
        dir.mkdir();
        filemgr.registerWriteableMount(TESTDIR + "/QuotaDir=100k");

        // First test FileOutputStream class...
        try
        {
            File testFile = new File(TESTDIR + "/QuotaDir/FileOutStreamTest.txt");
            byte data[] = new byte[256];
            testFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(testFile);
            fos.write(msg.getBytes());
            fos.close();
            FileInputStream fis = new FileInputStream(testFile);
            fis.read(data, 0, (int) testFile.length());
            String s = new String(data, 0, (int) testFile.length());
            fis.close();
            testFile.delete();

            assertTrue("File data does not match data written to file", msg.compareTo(s) == 0);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }

        // Next, test RandomAccessFile class...
        try
        {
            File testFile = new File(TESTDIR + "/QuotaDir/RandomAccessFileTest.txt");
            byte data[] = new byte[256];
            testFile.createNewFile();
            RandomAccessFile raf = new RandomAccessFile(testFile, "rw");
            raf.write(msg.getBytes());
            raf.close();
            FileInputStream fis = new FileInputStream(testFile);
            fis.read(data, 0, (int) testFile.length());
            String s = new String(data, 0, (int) testFile.length());
            fis.close();
            testFile.delete();

            assertTrue("File data does not match data written to file", msg.compareTo(s) == 0);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }

        // Now try using a RandomAccessFile.getFD() FD to intantiate a
        // FileOutputStream...
        try
        {
            File testFile = new File(TESTDIR + "/QuotaDir/FileDescriptorTest.txt");
            byte data[] = new byte[256];
            testFile.createNewFile();
            RandomAccessFile raf = new RandomAccessFile(testFile, "rw");

            FileOutputStream fos = new FileOutputStream(raf.getFD());
            fos.write(msg.getBytes());
            fos.close();
            raf.close();
            FileInputStream fis = new FileInputStream(testFile);
            fis.read(data, 0, (int) testFile.length());
            String s = new String(data, 0, (int) testFile.length());
            fis.close();
            testFile.delete();

            assertTrue("File data does not match data written to file", msg.compareTo(s) == 0);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }

        // Next, test setLength() method of RandomAccessFile class...
        try
        {
            File testFile = new File(TESTDIR + "/QuotaDir/SetLengthTest.txt");
            byte data[] = new byte[256];
            testFile.createNewFile();
            RandomAccessFile raf = new RandomAccessFile(testFile, "rw");
            raf.setLength(msg.length() * 2);
            assertTrue("File size not set to correct length", raf.length() == msg.length() * 2);
            raf.write(msg.getBytes());
            raf.setLength(msg.length());
            assertTrue("File size not set to correct length", raf.length() == msg.length());
            raf.close();
            FileInputStream fis = new FileInputStream(testFile);
            fis.read(data, 0, (int) testFile.length());
            String s = new String(data, 0, (int) testFile.length());
            fis.close();
            testFile.delete();

            assertTrue("File data does not match data written to file", msg.compareTo(s) == 0);
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
        }

        filemgr.unregisterWriteableMount(TESTDIR + "/QuotaDir");
        dir.delete();
    }

    /**
     * Test the <code>PersistentFileSys</code> "purge" method. This test case
     * sets up a number of application files associated with some non-running
     * and some running applications. It installs a test application manager
     * that assists in faking the presense of the running applications. After
     * the setup phase it makes a series of calls to the "purge" method
     * requesting specific amounts to be purged and verifies that the correct
     * number of files have been removed and are still present.
     * 
     * @todo disabled per 4598
     */
    public void xxxtestPurgePersistentFileSys()
    {
        File d = new File(TESTDIR + "/Apps");
        d.mkdirs();
        filemgr.registerWriteableMount(TESTDIR + "/Apps");
        setupFiles();
        setupRunningApp(0x1020304, 0x5003, 150);
        setupRunningApp(0x5060708, 0x5003, 100);
        setupRunningApp(0x4a5b6c, 0x6003, 200);
        PersistentFileSys pfs = (PersistentFileSys) FileSysManager.getWriteableFileSys(TESTDIR + "/Apps");

        // This purge should remove all of the low-priority files associated
        // with non-running applications.
        pfs.purge(TESTDIR + "/Apps", (long) 7 * 1024);
        assertTrue("Files that should have been flushed are still present",
                fileExists(TESTDIR + "/Apps", "lowPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "medPriority") == 7);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 7);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This purge should remove all of the medium-priority files associated
        // with non-running applications.
        pfs.purge(TESTDIR + "/Apps", 7 * 1024);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "medPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 7);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This purge should remove all of the high-priority files associated
        // with non-running applications.
        pfs.purge(TESTDIR + "/Apps", 7 * 1024);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This purge should remove all of the low-priority files associated
        // with running applications.
        File f1 = new File(TESTDIR + "/Apps/5060708/5003/org/cablelabs/xlet", "lowPriority_running");
        File f2 = new File(TESTDIR + "/Apps/1020304/5003/org/cablelabs/xlet", "lowPriority_running");
        File f3 = new File(TESTDIR + "/Apps/4a5b6c/6003/org/cablelabs/xlet", "lowPriority_running");
        pfs.purge(TESTDIR + "/Apps", 1 * 1024);
        try
        {
            assertFalse("File should have been flushed: " + f1.getCanonicalPath(), f1.exists());
            assertTrue("File should not have been flushed: " + f2.getCanonicalPath(), f2.exists());
            assertTrue("File should not have been flushed: " + f3.getCanonicalPath(), f3.exists());
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 2);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            pfs.purge(TESTDIR + "/Apps", 1 * 1024);
            assertFalse("File should have been flushed: " + f2.getCanonicalPath(), f2.exists());
            assertTrue("File should not have been flushed: " + f3.getCanonicalPath(), f3.exists());
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 1);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            pfs.purge(TESTDIR + "/Apps", 1 * 1024);
            assertFalse("File should not have been flushed: " + f3.getCanonicalPath(), f3.exists());
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            // Now flush 1 file at a time verifying the proper order of deletion
            // based on app priority.
            f1 = new File(TESTDIR + "/Apps/5060708/5003/org/cablelabs/xlet", "medPriority_running");
            f2 = new File(TESTDIR + "/Apps/1020304/5003/org/cablelabs/xlet", "medPriority_running");
            f3 = new File(TESTDIR + "/Apps/4a5b6c/6003/org/cablelabs/xlet", "medPriority_running");
            pfs.purge(TESTDIR + "/Apps", 1 * 1024);
            assertFalse("File should have been flushed: " + f1.getCanonicalPath(), f1.exists());
            assertTrue("File should not have been flushed: " + f2.getCanonicalPath(), f2.exists());
            assertTrue("File should not have been flushed: " + f3.getCanonicalPath(), f3.exists());
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 2);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            pfs.purge(TESTDIR + "/Apps", 1 * 1024);
            assertFalse("File should have been flushed: " + f2.getCanonicalPath(), f2.exists());
            assertTrue("File should not have been flushed: " + f3.getCanonicalPath(), f3.exists());
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 1);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            pfs.purge(TESTDIR + "/Apps", 1 * 1024);
            assertFalse("File should not have been flushed: " + f3.getCanonicalPath(), f3.exists());
            assertTrue("Files that should have been flushed were",
                    fileExists(TESTDIR + "/Apps", "medPriority_running") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            f1 = new File(TESTDIR + "/Apps/5060708/5003/org/cablelabs/xlet", "highPriority_running");
            f2 = new File(TESTDIR + "/Apps/1020304/5003/org/cablelabs/xlet", "highPriority_running");
            f3 = new File(TESTDIR + "/Apps/4a5b6c/6003/org/cablelabs/xlet", "highPriority_running");
            pfs.purge(TESTDIR + "/Apps", 1 * 1024);
            assertFalse("File should have been flushed: " + f1.getCanonicalPath(), f1.exists());
            assertTrue("File should not have been flushed: " + f2.getCanonicalPath(), f2.exists());
            assertTrue("File should not have been flushed: " + f3.getCanonicalPath(), f3.exists());
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 2);

            pfs.purge(TESTDIR + "/Apps", 1 * 1024);
            assertFalse("File should have been flushed: " + f2.getCanonicalPath(), f2.exists());
            assertTrue("File should not have been flushed: " + f3.getCanonicalPath(), f3.exists());
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 1);

            pfs.purge(TESTDIR + "/Apps", 1 * 1024);
            assertFalse("File should have been flushed: " + f3.getCanonicalPath(), f3.exists());
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 0);
        }
        catch (Exception e)
        {
            fail("can't acquire canonical path for files");
        }
    }

    /**
     * Test the <code>PersistentFileSys</code> "purge" method. This test case
     * sets up a number of application files associated with some non-running
     * and some running applications. It installs a test application manager
     * that assists in faking the presense of the running applications. After
     * the setup phase it makes a series of calls to the "purge" method
     * requesting specific amounts to be purged and verifies that the correct
     * number of files have been removed and are still present.
     */
    public void testPurgeAllPersistentFileSys()
    {
        File d = new File(TESTDIR + "/Apps");
        d.mkdirs();
        filemgr.registerWriteableMount(TESTDIR + "/Apps");
        setupFiles();
        setupRunningApp(0x1020304, 0x5003, 150);
        setupRunningApp(0x5060708, 0x5003, 100);
        setupRunningApp(0x4a5b6c, 0x6003, 200);
        PersistentFileSys pfs = (PersistentFileSys) FileSysManager.getWriteableFileSys(TESTDIR + "/Apps");

        // This purge should remove all of the high-priority files associated
        // with running applications.
        pfs.purge(TESTDIR + "/Apps", 30 * 1024);
        assertTrue("Files that should have been flushed are still present",
                fileExists(TESTDIR + "/Apps", "lowPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "medPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 0);
        d.delete();
    }

    /**
     * Test the <code>PersistentFileSys</code> "purge" method. This test case
     * sets up a number of application files associated with some non-running
     * and some running applications. It installs a test application manager
     * that assists in faking the presense of the running applications. After
     * the setup phase it makes a series of calls to the "purge" method
     * requesting specific amounts to be purged and verifies that the correct
     * number of files have been removed and are still present.
     */
    public void testPurgeAllPersistent2FileSys()
    {
        File d = new File(TESTDIR + "/Apps/10000001/4001");
        d.mkdirs();
        createFile(new File(d, "lowPriority"), FileAttributes.PRIORITY_LOW, 15 * 1024);
        d = new File(TESTDIR + "/Apps/80000001/4001");
        d.mkdirs();
        createFile(new File(d, "lowPriority"), FileAttributes.PRIORITY_LOW, 15 * 1024);

        filemgr.registerWriteableMount(TESTDIR + "/Apps");

        File f = new File(TESTDIR + "/Apps/", "overflow");
        PersistentFileSys pfs = (PersistentFileSys) FileSysManager.getWriteableFileSys(TESTDIR + "/Apps");

        // This purge should remove all of the high-priority files associated
        // with running applications.
        pfs.purge(TESTDIR + "/Apps", 30 * 1024);
        assertTrue("Files that should have been flushed are still present",
                fileExists(TESTDIR + "/Apps", "lowPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "medPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 0);
    }

    /**
     * Test the <code>QuotaFileSys</code> implicitly. This test case sets up a
     * number of application files associated with some non-running and some
     * running applications. It installs a test application manager that assists
     * in faking the presense of the running applications. After the setup phase
     * it makes a series of calls to the create and write to files of specific
     * sizes to cause implicit purges and then verifies that the correct number
     * of files have been removed and are still present.
     * 
     * @todo disabled per 4598
     */
    public void xxxtestWritePurgeQuotaFileSys()
    {
        File d = new File(TESTDIR + "/Apps");
        d.mkdirs();
        filemgr.registerWriteableMount(TESTDIR + "/Apps=31000");
        setupFiles();
        setupRunningApp(0x1020304, 0x5003, 150);
        setupRunningApp(0x5060708, 0x5003, 100);
        setupRunningApp(0x4a5b6c, 0x6003, 200);

        File f = new File(TESTDIR + "/Apps/", "overflow1");

        // This "write" should purge all of the low-priority files associated
        // with non-running applications.
        createFile(f, (-1), 7 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 7 * 1024);
        assertTrue("Files that should have been flushed are still present",
                fileExists(TESTDIR + "/Apps", "lowPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "medPriority") == 7);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 7);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This "write" should purge all of the medium-priority files associated
        // with non-running applications.
        f = new File(TESTDIR + "/Apps/", "overflow2");
        createFile(f, (-1), 7 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 7 * 1024);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "medPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 7);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This "write" should purge all of the high-priority files associated
        // with non-running applications.
        f = new File(TESTDIR + "/Apps/", "overflow3");
        createFile(f, (-1), 7 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 7 * 1024);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This "write" should purge all of the low-priority files associated
        // with running applications.
        f = new File(TESTDIR + "/Apps/", "overflow4");
        createFile(f, (-1), 3 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 3 * 1024);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This "write" should purge all of the medium-priority files associated
        // with running applications.
        f = new File(TESTDIR + "/Apps/", "overflow5");
        createFile(f, (-1), 3 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 3 * 1024);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This "write" should purge all of the high-priority files associated
        // with running applications.
        f = new File(TESTDIR + "/Apps/", "overflow6");
        createFile(f, (-1), 3 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 3 * 1024);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 0);

        d.delete();
    }

    /**
     * Test the <code>QuotaFileSys</code> implicitly. This test case sets up a
     * number of application files associated with some non-running and some
     * running applications. It installs a test application manager that assists
     * in faking the presense of the running applications. After the setup phase
     * it makes a single call to create and write to a file such that an
     * implicit purge removes all the files and then verifies that the correct
     * number of files have been removed.
     */
    public void testWritePurgeAllQuotaFileSys()
    {
        File d = new File(TESTDIR + "/Apps");
        d.mkdirs();
        filemgr.registerWriteableMount(TESTDIR + "/Apps=30K");
        setupFiles();
        setupRunningApp(0x1020304, 0x5003, 150);
        setupRunningApp(0x5060708, 0x5003, 100);
        setupRunningApp(0x4a5b6c, 0x6003, 200);

        // This "write" should purge all of the high-priority files associated
        // with running applications.
        File f = new File(TESTDIR + "/Apps/", "overflow");
        createFile(f, (-1), 30 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 30 * 1024);
        assertTrue("Files that should have been flushed are still present",
                fileExists(TESTDIR + "/Apps", "lowPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "medPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 0);
        f.delete();
    }

    /**
     * Test the <code>QuotaFileSys</code> implicitly. This test case sets up a
     * number of application files associated with some non-running and some
     * running applications. It installs a test application manager that assists
     * in faking the presense of the running applications. After the setup phase
     * it makes a single call to create and write to a file such that an
     * implicit purge removes all the files and then verifies that the correct
     * number of files have been removed.
     * 
     * @todo disabled per 4598
     */
    public void xxxtestWritePurgeOrgQuotaFileSys()
    {
        File d = new File(TESTDIR + "/Apps");
        d.mkdirs();
        filemgr.registerWriteableMount(TESTDIR + "/Apps=36K");
        setupFiles();

        // Create additional files in a couple of the orgId directories.
        // Setup directories and setup the files to be purged.
        d = new File(TESTDIR + "/Apps/1020304");
        createFile(new File(d, "lowPriority_running"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority_running"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority_running"), FileAttributes.PRIORITY_HIGH, 1024);

        d = new File(TESTDIR + "/Apps/3040506/Common/1234");
        d.mkdirs();
        createFile(new File(d, "lowPriority"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority"), FileAttributes.PRIORITY_HIGH, 1024);

        setupRunningApp(0x1020304, 0x5003, 150);
        setupRunningApp(0x5060708, 0x5003, 100);
        setupRunningApp(0x4a5b6c, 0x6003, 200);

        File f = new File(TESTDIR + "/Apps/", "overflow");
        FileOutputStream fos = null;
        try
        {
            if (f.createNewFile() == false)
            {
                RandomAccessFile raf = new RandomAccessFile(f, "rw");
                raf.setLength(0);
                raf.close();
            }
            fos = new FileOutputStream(f);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("couldn't create output file for testing");
        }

        try
        {
            // This "write" should purge all of the low-priority files
            // associated with non-running applications.
            int offset = 0;
            try
            {
                fos.write(data40k, offset, 6 * 1024);
                offset += 6 * 1024;
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length, file = " + f.length() + " offset = " + offset,
                    f.length() == offset);
            assertTrue("Files that should have been flushed are still present", fileExists(TESTDIR + "/Apps",
                    "lowPriority") == 0);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "medPriority") == 8);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "highPriority") == 8);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 4);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 4);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 4);

            // This "write" should purge all of the medium-priority files
            // associated with non-running applications.
            try
            {
                fos.write(data40k, offset, 6 * 1024);
                offset += 6 * 1024;
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == offset);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "medPriority") == 0);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "highPriority") == 8);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 4);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 4);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 4);

            // This "write" should purge all of the high-priority files
            // associated with non-running applications.
            try
            {
                fos.write(data40k, offset, 6 * 1024);
                offset += 6 * 1024;
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == offset);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "highPriority") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 4);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 4);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 4);

            // This "write" should purge all of the low-priority files
            // associated with running applications.
            try
            {
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == offset);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 4);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 4);

            // This "write" should purge all of the medium-priority files
            // associated with running applications.
            try
            {
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == offset);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 4);

            // This "write" should purge all of the high-priority files
            // associated with running applications.
            try
            {
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == offset);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 0);
        }
        finally
        {
            f.delete();
            d.delete();

            try
            {
                fos.close();
            }
            catch (IOException e)
            {

            }
        }
    }

    /**
     * Test the <code>PersistentFileSys</code> "purge" method. This test case
     * sets up a number of application files associated with some non-running
     * and some running applications. It installs a test application manager
     * that assists in faking the presense of the running applications. After
     * the setup phase it makes a series of calls to the "purge" method
     * requesting specific amounts to be purged and verifies that the correct
     * number of files have been removed and are still present.
     */
    public void testPurgeFailureQuotaFileSys()
    {
        File d = new File(TESTDIR + "/Apps");
        d.mkdirs();
        filemgr.registerWriteableMount(TESTDIR + "/Apps=30k");
        setupFiles();
        setupRunningApp(0x1020304, 0x5003, 150);
        setupRunningApp(0x5060708, 0x5003, 100);
        setupRunningApp(0x4a5b6c, 0x6003, 200);

        // This purge should remove all of the high-priority files associated
        // with running applications.
        File f = new File(TESTDIR + "/Apps/", "overflow");
        FileOutputStream fos = null;
        try
        {
            if (f.createNewFile() == false)
            {
                RandomAccessFile raf = new RandomAccessFile(f, "rw");
                raf.setLength(0);
                raf.close();
            }
            fos = new FileOutputStream(f);
            fos.write(data40k, 0, 40 * 1024);
            fail("IOException excpected from file write larger than partition size.");
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace();
            assertTrue("Exception caught, but it's not an IOException", ioe instanceof IOException);
            assertTrue("Files that should have been flushed are still present", fileExists(TESTDIR + "/Apps",
                    "lowPriority") == 0);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "medPriority") == 0);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "highPriority") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 0);
        }
        finally
        {
            f.delete();
            try
            {
                if (fos != null) fos.close();
            }
            catch (IOException e)
            {

            }
        }
    }

    public void XtestPreferredAppRegistration()
    {
        File d = new File(TESTDIR + "/Apps");
        d.mkdirs();
        filemgr.registerWriteableMount(TESTDIR + "/Apps");
        setupFiles();

        d = new File(TESTDIR + "/Apps/4a5b6c/5001/org/cablelabs/xlet");
        d.mkdirs();
        createFile(new File(d, "lowPriority_preferred"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority_preferred"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority_preferred"), FileAttributes.PRIORITY_HIGH, 1024);

        setupRunningApp(0x1020304, 0x5003, 150);
        setupRunningApp(0x5060708, 0x5003, 100);
        setupRunningApp(0x4a5b6c, 0x6003, 200);
        PersistentFileSys pfs = (PersistentFileSys) FileSysManager.getWriteableFileSys(TESTDIR + "/Apps");

        // This purge should remove all of the low-priority files associated
        // with non-running applications.
        pfs.purge(TESTDIR + "/Apps", 7 * 1024);
        assertTrue("Files that should have been flushed are still present",
                fileExists(TESTDIR + "/Apps", "lowPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "medPriority") == 7);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 7);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "lowPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "medPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "highPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This purge should remove all of the medium-priority files associated
        // with non-running applications.
        pfs.purge(TESTDIR + "/Apps", 7 * 1024);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "medPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 7);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "lowPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "medPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "highPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This purge should remove all of the high-priority files associated
        // with non-running applications.
        pfs.purge(TESTDIR + "/Apps", 7 * 1024);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "lowPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "medPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "highPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        pfs.purge(TESTDIR + "/Apps", 1 * 1024);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "lowPriority_preferred") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "medPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "highPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        pfs.purge(TESTDIR + "/Apps", 1 * 1024);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "medPriority_preferred") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "highPriority_preferred") == 1);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        pfs.purge(TESTDIR + "/Apps", 1 * 1024);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                "highPriority_preferred") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This purge should remove all of the low-priority files associated
        // with running applications.
        pfs.purge(TESTDIR + "/Apps", 3 * 1024);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This purge should remove all of the medium-priority files associated
        // with running applications.
        pfs.purge(TESTDIR + "/Apps", 3 * 1024);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This purge should remove all of the high-priority files associated
        // with running applications.
        pfs.purge(TESTDIR + "/Apps", 3 * 1024);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 0);
    }

    /**
     * Test the <code>DefaultFileSys</code> implicitly. This test case sets up a
     * number of application files associated with some non-running and some
     * running applications. It installs a test application manager that assists
     * in faking the presense of the running applications. After the setup phase
     * it makes a series of calls to the create and write to files of specific
     * sizes to cause implicit purges and then verifies that the correct number
     * of files have been removed and are still present.
     * 
     * The key difference in this test is that the normal
     * <code>DefaultWriteableFileSys</code>, which is simply a wrapper for the
     * native file system get replaced with a test version that simulates
     * StorageMediaFullExceptions being thrown from the native file system.
     * 
     * @todo disabled per 4598
     */
    public void xxxtestWriteDefaultWriteableFileSys()
    {
        // First replace the normal DefaultWriteableFileSys.
        dwfs = (DefaultWriteableFileSys) filemgr.getDefaultWriteableFileSys();
        filemgr.setDefaultWriteableFileSys(new TestDefaultWFS(dwfs, 31000));

        File d = new File(TESTDIR + "/Apps");
        d.mkdirs();
        filemgr.registerWriteableMount(TESTDIR + "/Apps");
        setupFiles();
        setupRunningApp(0x1020304, 0x5003, 150);
        setupRunningApp(0x5060708, 0x5003, 100);
        setupRunningApp(0x4a5b6c, 0x6003, 200);

        File f = new File(TESTDIR + "/Apps/", "overflow1");

        // This "write" should purge all of the low-priority files associated
        // with non-running applications.
        createFile(f, (-1), 7 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 7 * 1024);
        assertTrue("Files that should have been flushed are still present",
                fileExists(TESTDIR + "/Apps", "lowPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "medPriority") == 7);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 7);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This "write" should purge all of the medium-priority files associated
        // with non-running applications.
        f = new File(TESTDIR + "/Apps/", "overflow2");
        createFile(f, (-1), 7 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 7 * 1024);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "medPriority") == 0);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 7);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This "write" should purge all of the high-priority files associated
        // with non-running applications.
        f = new File(TESTDIR + "/Apps/", "overflow3");
        createFile(f, (-1), 7 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 7 * 1024);
        assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps", "highPriority") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This "write" should purge all of the low-priority files associated
        // with running applications.
        f = new File(TESTDIR + "/Apps/", "overflow4");
        createFile(f, (-1), 3 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 3 * 1024);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "lowPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 3);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This "write" should purge all of the medium-priority files associated
        // with running applications.
        f = new File(TESTDIR + "/Apps/", "overflow5");
        createFile(f, (-1), 3 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 3 * 1024);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "medPriority_running") == 0);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 3);

        // This "write" should purge all of the high-priority files associated
        // with running applications.
        f = new File(TESTDIR + "/Apps/", "overflow6");
        createFile(f, (-1), 3 * 1024);
        assertTrue("Overflow file not correct length", f.length() == 3 * 1024);
        assertTrue("Files that should not have been flushed were",
                fileExists(TESTDIR + "/Apps", "highPriority_running") == 0);
    }

    /**
     * Test the <code>DefaultFileSys</code> implicitly. This test case sets up a
     * number of application files associated with some non-running and some
     * running applications. It installs a test application manager that assists
     * in faking the presense of the running applications. After the setup phase
     * it makes a series of calls to the create and write to files of specific
     * sizes to cause implicit purges and then verifies that the correct number
     * of files have been removed and are still present.
     * 
     * The key difference in this test is that the normal
     * <code>DefaultWriteableFileSys</code>, which is simply a wrapper for the
     * native file system get replaced with a test version that simulates
     * StorageMediaFullExceptions being thrown from the native file system.
     * 
     * @todo disabled per 4598
     */
    public void xxxtestPartialWriteDefaultWriteableFileSys()
    {
        // First replace the normal DefaultWriteableFileSys.
        dwfs = (DefaultWriteableFileSys) filemgr.getDefaultWriteableFileSys();
        filemgr.setDefaultWriteableFileSys(new TestDefaultWFS(dwfs, 31000));

        File d = new File(TESTDIR + "/Apps");
        d.mkdirs();
        filemgr.registerWriteableMount(TESTDIR + "/Apps");
        setupFiles();
        setupRunningApp(0x1020304, 0x5003, 150);
        setupRunningApp(0x5060708, 0x5003, 100);
        setupRunningApp(0x4a5b6c, 0x6003, 200);

        File f = new File(TESTDIR + "/Apps/", "overflow");
        FileOutputStream fos = null;
        try
        {
            if (f.createNewFile() == false)
            {
                RandomAccessFile raf = new RandomAccessFile(f, "rw");
                raf.setLength(0);
                raf.close();
            }
            fos = new FileOutputStream(f);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("couldn't create output file for testing");
        }

        // This "write" should purge all of the low-priority files associated
        // with non-running applications.
        try
        {
            int offset = 0;
            try
            {
                fos.write(data40k, offset, 6 * 1024);
                offset += 6 * 1024;
                fos.write(data40k, offset, 1 * 1024);
                offset += 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == offset);
            assertTrue("Files that should have been flushed are still present", fileExists(TESTDIR + "/Apps",
                    "lowPriority") == 0);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "medPriority") == 7);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "highPriority") == 7);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            // This "write" should purge all of the medium-priority files
            // associated with non-running applications.
            try
            {
                fos.write(data40k, offset, 6 * 1024);
                offset += 6 * 1024;
                fos.write(data40k, offset, 1 * 1024);
                offset += 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == offset);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "medPriority") == 0);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "highPriority") == 7);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            // This "write" should purge all of the high-priority files
            // associated with non-running applications.
            try
            {
                fos.write(data40k, offset, 6 * 1024);
                offset += 6 * 1024;
                fos.write(data40k, offset, 1 * 1024);
                offset += 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == offset);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "highPriority") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            // This "write" should purge all of the low-priority files
            // associated with running applications.
            try
            {
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
                fos.write(data40k, offset, 1 * 1024);
                offset += 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == offset);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            // This "write" should purge all of the medium-priority files
            // associated with running applications.
            try
            {
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
                fos.write(data40k, offset, 1 * 1024);
                offset += 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == offset);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            // This "write" should purge all of the high-priority files
            // associated with running applications.
            try
            {
                fos.write(data40k, offset, 2 * 1024);
                offset += 2 * 1024;
                fos.write(data40k, offset, 1 * 1024);
                offset += 1024;
            }
            catch (Exception e)
            {
                fail("failed to write to overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == offset);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 0);
        }
        finally
        {
            try
            {
                if (fos != null)
                {
                    fos.close();
                }
            }
            catch (IOException e)
            {

            }
            f.delete();
        }
    }

    /**
     * Test the <code>DefaultFileSys</code> implicitly. This test case sets up a
     * number of application files associated with some non-running and some
     * running applications. It installs a test application manager that assists
     * in faking the presense of the running applications. After the setup phase
     * it makes a series of calls to set the length of a file. Each of lengths
     * set should cause implicit purges. On each length setting the test case
     * verifies: the file size, the number of files that should have been
     * removed and the number of files still present.
     * 
     * @todo disabled per 4598
     */
    public void xxxtestSetLengthDefaultWriteableFileSys()
    {
        // First replace the normal DefaultWriteableFileSys.
        dwfs = (DefaultWriteableFileSys) filemgr.getDefaultWriteableFileSys();
        filemgr.setDefaultWriteableFileSys(new TestDefaultWFS(dwfs, 30 * 1024));

        File d = new File(TESTDIR + "/Apps");
        d.mkdirs();
        filemgr.registerWriteableMount(TESTDIR + "/Apps");
        setupFiles();
        setupRunningApp(0x1020304, 0x5003, 150);
        setupRunningApp(0x5060708, 0x5003, 100);
        setupRunningApp(0x4a5b6c, 0x6003, 200);

        File f = new File(TESTDIR + "/Apps/", "setLength_overflow");
        f.delete();
        RandomAccessFile raf = null;
        try
        {
            raf = new RandomAccessFile(f, "rw");
        }
        catch (Exception e)
        {
            fail("can't create RandomAccessFile for set length testing");
        }

        try
        {
            // This "write" should purge all of the low-priority files
            // associated with non-running applications.
            int length = 0;
            try
            {
                length += 6 * 1024;
                System.out.println("RandomAccessFile - length of " + f.getCanonicalPath() + " is " + raf.length());
                System.out.println("File - length of " + f.getCanonicalPath() + " is " + f.length());
                raf.setLength(length);
                length += 1024;
                raf.setLength(length);
            }
            catch (Exception e)
            {
                fail("failed to set length of overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == length);
            assertTrue("Files that should have been flushed are still present", fileExists(TESTDIR + "/Apps",
                    "lowPriority") == 0);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "medPriority") == 7);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "highPriority") == 7);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            // This "write" should purge all of the medium-priority files
            // associated with non-running applications.
            try
            {
                length += 6 * 1024;
                raf.setLength(length);
                length += 1024;
                raf.setLength(length);
            }
            catch (Exception e)
            {
                fail("failed to set length of overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == length);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "medPriority") == 0);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "highPriority") == 7);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            // This "write" should purge all of the high-priority files
            // associated with non-running applications.
            try
            {
                length += 6 * 1024;
                raf.setLength(length);
                length += 1024;
                raf.setLength(length);
            }
            catch (Exception e)
            {
                fail("failed to set length of overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == length);
            assertTrue("Files that should not have been flushed were",
                    fileExists(TESTDIR + "/Apps", "highPriority") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            // This "write" should purge all of the low-priority files
            // associated with running applications.
            try
            {
                length += 2 * 1024;
                raf.setLength(length);
                length += 1024;
                raf.setLength(length);
            }
            catch (Exception e)
            {
                fail("failed to set length of overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == length);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "lowPriority_running") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 3);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            // This "write" should purge all of the medium-priority files
            // associated with running applications.
            try
            {
                length += 2 * 1024;
                raf.setLength(length);
                length += 1024;
                raf.setLength(length);
            }
            catch (Exception e)
            {
                fail("failed to set length of overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == length);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "medPriority_running") == 0);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 3);

            // This "write" should purge all of the high-priority files
            // associated with running applications.
            try
            {
                length += 2 * 1024;
                raf.setLength(length);
                length += 1024;
                raf.setLength(length);
            }
            catch (Exception e)
            {
                fail("failed to set length of overflow file");
            }
            assertTrue("Overflow file not correct length", f.length() == length);
            assertTrue("Files that should not have been flushed were", fileExists(TESTDIR + "/Apps",
                    "highPriority_running") == 0);
        }
        finally
        {
            try
            {
                raf.close();
            }
            catch (IOException e)
            {

            }
            f.delete();
            d.delete();
        }
    }

    /**
     * Adds an entry in the array of running applications at the specified
     * priority level. This is used by the AppsDatabase to supply information
     * about the running applications.
     * 
     * @param oid
     * @param aid
     * @param priority
     */
    private void setupRunningApp(int oid, int aid, int priority)
    {
        AppID appId = new AppID(oid, aid);
        AppAttributes attr = new TestAppAttributes(priority);
        runningAppIds.add(appId);
        runningAppAttr.put(appId, attr);
    }

    /**
     * Sets up 30k of application file data for purging. 21K of the file data is
     * associated with non-running applications and 9K is associated with
     * running applications. Each application directory is setup with three
     * files each of low, medium and high storage priority respectively.
     */
    private void setupFiles()
    {
        // 1K of data to file the files with.
        byte[] data = new byte[1024];
        for (int i = 0; i < 1024; ++i)
            data[i] = (byte) ((i % 26) + 'a');

        // Setup directories and setup the files to be purged.
        File d = new File(TESTDIR + "/Apps/1020304/5001/org/cablelabs/xlet");
        d.mkdirs();
        createFile(new File(d, "lowPriority"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority"), FileAttributes.PRIORITY_HIGH, 1024);

        d = new File(TESTDIR + "/Apps/1020304/5002/org/cablelabs/xlet");
        d.mkdirs();
        createFile(new File(d, "lowPriority"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority"), FileAttributes.PRIORITY_HIGH, 1024);

        d = new File(TESTDIR + "/Apps/1020304/5003/org/cablelabs/xlet");
        d.mkdirs();
        createFile(new File(d, "lowPriority_running"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority_running"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority_running"), FileAttributes.PRIORITY_HIGH, 1024);

        d = new File(TESTDIR + "/Apps/5060708/5001/org/cablelabs/xlet");
        d.mkdirs();
        createFile(new File(d, "lowPriority"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority"), FileAttributes.PRIORITY_HIGH, 1024);

        d = new File(TESTDIR + "/Apps/5060708/5002/org/cablelabs/xlet");
        d.mkdirs();
        createFile(new File(d, "lowPriority"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority"), FileAttributes.PRIORITY_HIGH, 1024);

        d = new File(TESTDIR + "/Apps/5060708/5003/org/cablelabs/xlet");
        d.mkdirs();
        createFile(new File(d, "lowPriority_running"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority_running"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority_running"), FileAttributes.PRIORITY_HIGH, 1024);

        d = new File(TESTDIR + "/Apps/2030405/5021/org/cablelabs/xlet");
        d.mkdirs();
        createFile(new File(d, "lowPriority"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority"), FileAttributes.PRIORITY_HIGH, 1024);

        d = new File(TESTDIR + "/Apps/3040506/5031/org/cablelabs/xlet");
        d.mkdirs();
        createFile(new File(d, "lowPriority"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority"), FileAttributes.PRIORITY_HIGH, 1024);

        d = new File(TESTDIR + "/Apps/4a5b6c/5041/org/cablelabs/xlet");
        d.mkdirs();
        createFile(new File(d, "lowPriority"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority"), FileAttributes.PRIORITY_HIGH, 1024);

        d = new File(TESTDIR + "/Apps/4a5b6c/6003/org/cablelabs/xlet");
        d.mkdirs();
        createFile(new File(d, "lowPriority_running"), FileAttributes.PRIORITY_LOW, 1024);
        createFile(new File(d, "medPriority_running"), FileAttributes.PRIORITY_MEDIUM, 1024);
        createFile(new File(d, "highPriority_running"), FileAttributes.PRIORITY_HIGH, 1024);
    }

    private void createFile(File f, int priority, int size)
    {
        if (f.exists() == false)
        {
            try
            {
                if (f.createNewFile() == false) return;
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(data40k, 0, size);
                fos.close();
                if (priority != (-1))
                {
                    FileAttributes attr = FileAttributes.getFileAttributes(f);
                    attr.setPriority(priority);
                    FileAttributes.setFileAttributes(attr, f);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return;
            }
        }
    }

    private int fileExists(String loc, String pattern)
    {
        int instances = 0;
        File dir = new File(loc);
        if (dir.exists() == false) return 0;

        String[] contents = dir.list();
        for (int i = 0; i < contents.length; ++i)
        {
            if (pattern.compareTo(contents[i]) == 0) ++instances;
            File f;
            try
            {
                f = new File(dir.getCanonicalFile() + "/" + contents[i]);
            }
            catch (Exception e)
            {
                continue;
            }

            if (f.isDirectory())
            {
                try
                {
                    instances += fileExists(f.getCanonicalPath(), pattern);
                }
                catch (IOException ioe)
                {
                }
            }
        }
        return instances;
    }

    private void cleanDir(File dir)
    {
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

    /*  *********** Boilerplate ************ */

    private FileManagerImpl filemgr;

    private Manager save;

    private static CallerContextManager ccMgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);;

    public void setUp() throws Exception
    {
        super.setUp();
        filemgr = (FileManagerImpl) FileManagerImpl.getInstance();
        String ocap = MPEEnv.getEnv("OCAP.persistent.root");
        assertNotNull("Error! Need a place to write files", ocap);
        TESTDIR = ocap + "/junit";
        File testDir = new File(TESTDIR);
        if (!testDir.exists()) testDir.mkdirs();

        replaceAppMgr();
        runningAppIds = new Vector();
        runningAppAttr = new Hashtable();
    }

    public void tearDown() throws Exception
    {
        // Make sure directories & contents are purged.
        cleanDir(new File(TESTDIR));
        runningAppIds = null;
        runningAppAttr = null;
        if (dwfs != null) filemgr.setDefaultWriteableFileSys(dwfs);
        restoreAppMgr();
        super.tearDown();
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(FileManagerImplTest.class);

        ImplFactory factory = new ManagerTest.ManagerFactory()
        {
            public Object createImplObject()
            {
                return FileManagerImpl.getInstance();
            }

            public void destroyImplObject(Object obj)
            {
                ((Manager) obj).destroy();
            }
        };
        InterfaceTestSuite ifts = FileManagerTest.isuite();
        ifts.addFactory(factory);
        suite.addTest(ifts);

        return suite;
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(FileManagerImplTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new FileManagerImplTest(tests[i]));
            return suite;
        }
    }

    public FileManagerImplTest(String name)
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

    String TESTDIR;

    static Vector runningAppIds;

    static Hashtable runningAppAttr;

    static byte[] data40k;

    DefaultWriteableFileSys dwfs = null;

    static
    {
        data40k = new byte[40 * 1024];
        for (int i = 0; i < 7 * 1024; ++i)
            data40k[i] = (byte) ((i % 26) + 'a');
    }

    /*
     * The following internal classes are replacement classes for parts stack
     * that need to be replaced in order to provide sufficient emulation of the
     * components of the stack utilized by the persistent file system support.
     */
    private ApplicationManager saveAppMgr;

    private void replaceAppMgr()
    {
        saveAppMgr = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
        ManagerManagerTest.updateManager(ApplicationManager.class, AppMgr.class, false, new AppMgr());
    }

    private void restoreAppMgr()
    {
        ManagerManagerTest.updateManager(ApplicationManager.class, saveAppMgr.getClass(), true, saveAppMgr);
    }

    /**
     * Replacement ApplicationManager so we can return and AppsDatabase suitable
     * for providing the set of running applications and AppAttributes.
     * <p>
     * Assumes that implementation only use getAppsDatabase().
     */
    public static class AppMgr implements ApplicationManager, CallerContextManager
    {
        public static Manager getInstance()
        {
            return new AppMgr();
        }

        private void die()
        {
            fail("Unimplemented - not expected to be called");
        }

        public void destroy()
        {
            die();
        }

        public AppManagerProxy getAppManagerProxy()
        {
            die();
            return null;
        }

        public org.ocap.system.RegisteredApiManager getRegisteredApiManager()
        {
            die();
            return null;
        }

        public ClassLoader getAppClassLoader(CallerContext ctx)
        {
            die();
            return null;
        }

        public void registerResidentApp(java.io.InputStream in)
        {
            die();
        }

        public org.cablelabs.impl.manager.AppDomain createAppDomain(javax.tv.service.selection.ServiceContext sc)
        {
            die();
            return null;
        }

        public org.ocap.application.OcapAppAttributes createAppAttributes(org.cablelabs.impl.signalling.AppEntry entry,
                javax.tv.service.Service service)
        {
            die();
            return null;
        }

        public boolean purgeLowestPriorityApp(long x, long y, boolean urgent)
        {
            die();
            return false;
        }

        public AppAttributes getAppAttributes(final CallerContext ctx)
        {
            die();
            return null;
        }

        public AppsDatabase getAppsDatabase()
        {
            return new TestDB();
        }

        public int getRuntimePriority(AppID id)
        {
            return 0;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.CallerContextManager#getCurrentContext()
         */
        public CallerContext getCurrentContext()
        {
            return ccMgr.getCurrentContext();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.CallerContextManager#getSystemContext()
         */
        public CallerContext getSystemContext()
        {
            return ccMgr.getSystemContext();
        }

        public AppEntry getRunningVersion(AppID id)
        {
            return null;
        }

    }

    /**
     * Replacement AppsDatabase suitable for returning a "canned" set of running
     * applications and their associated AppAttributes.
     */
    private static class TestDB extends AppsDatabase
    {
        public TestDB()
        {
        }

        public Enumeration getAppIDs(AppsDatabaseFilter filter)
        {
            return runningAppIds.elements();
        }

        public AppAttributes getAppAttributes(AppID appId)
        {
            return (AppAttributes) runningAppAttr.get(appId);
        }
    }

    /**
     * Replacement AppAttributes suitable for returning the application priority
     * of the "canned" set of running applications.
     */
    private static class TestAppAttributes implements AppAttributes
    {
        TestAppAttributes(int priority)
        {
            this.priority = priority;
        }

        public int getPriority()
        {
            return priority;
        }

        private void die()
        {
            fail("Unimplemented - not expected to be called");
        }

        public AppID getIdentifier()
        {
            die();
            return null;
        }

        public int getType()
        {
            die();
            return 0;
        }

        public String getName()
        {
            die();
            return null;
        }

        public String getName(String iso)
        {
            die();
            return null;
        }

        public String[][] getNames()
        {
            die();
            return null;
        }

        public String[] getProfiles()
        {
            die();
            return null;
        }

        public int[] getVersions(String profile)
        {
            die();
            return null;
        }

        public boolean getIsServiceBound()
        {
            die();
            return false;
        }

        public boolean isStartable()
        {
            die();
            return false;
        }

        public AppIcon getAppIcon()
        {
            die();
            return null;
        }

        public org.davic.net.Locator getServiceLocator()
        {
            die();
            return null;
        }

        public Object getProperty(String key)
        {
            die();
            return null;
        }

        public boolean isVisible()
        {
            die();
            return false;
        }

        private int priority = 0;
    }

    private class TestDefaultWFS extends DefaultWriteableFileSys
    {
        TestDefaultWFS(DefaultWriteableFileSys dwfs, long limit)
        {
            this.dwfs = dwfs;
            this.limit = limit;
        }

        /**
         * Native write method.
         * 
         * @param fd
         * @param buf
         * @param off
         * @param len
         * @throws IOException
         * @throws StorageMediaFullException
         */
        public void write(int nativeHandle, byte[] buf, int off, int len) throws IOException, StorageMediaFullException
        {
            int toWrite = len;

            if (len + usage > limit) toWrite = (int) (limit - usage);
            if (toWrite > 0) dwfs.write(nativeHandle, buf, off, toWrite);

            if (toWrite < len)
            {
                // The purge that will result from the following exception
                // should free up
                // space equal to the requested length, so back the usage amount
                // off
                // byt the length to keep in sync for testing purposes.
                usage -= len;
                throw new StorageMediaFullException("write operation not complete, storage device full");
            }
            usage += toWrite;
        }

        /**
         * Native set length of writeable file method.
         * 
         * @param fd
         * @param length
         * @param current
         * @throws IOException
         * @throws StorageMediaFullException
         */
        public void setLength(int nativeHandle, long length, long current) throws IOException,
                StorageMediaFullException
        {
            long expansion = length - current;
            long setLength = length;

            if (expansion + usage > limit)
            {
                if (expansion > 0) usage -= expansion;
                throw new StorageMediaFullException("set length operation not complete, storage media full");
            }

            if (setLength > 0)
            {
                dwfs.setLength(nativeHandle, setLength);
                if (expansion > 0) usage += expansion;
            }
        }

        /***
         * The following two methods are not intercepted from their normal
         * location within RandomAccessFile. They are provide here for
         * convenient access to the associated native methods for the
         * <code>PersistentFileSys</code>.
         */
        public void seek(int nativeHandle, long pos) throws IOException
        {
            dwfs.seek(nativeHandle, pos);
        }

        public long getFilePointer(int nativeHandle) throws IOException
        {
            return dwfs.getFilePointer(nativeHandle);
        }

        private DefaultWriteableFileSys dwfs;

        private long limit;

        private long usage = 0;
    }
}
