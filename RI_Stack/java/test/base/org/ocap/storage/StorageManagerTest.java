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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.filesys.FileManagerImpl;
import org.cablelabs.impl.util.MPEEnv;

public class StorageManagerTest extends TestCase
{

    public void testAvailableStorageListeners() throws Exception
    {
        File f = null;
        StorageManager mgr = StorageManager.getInstance();
        FileOutputStream fos = null;

        // calling addAvailableStorageListener() with null throws
        // IllegalArgumentException
        try
        {
            mgr.addAvailableStorageListener(null, 50);
            fail("Expected IllegalArgumentException to be thrown");
        }
        catch (IllegalArgumentException e)
        {
            // pass
        }

        // calling removeAvailableStorageListener() with null throws
        // IllegalArgumentException
        try
        {
            mgr.removeAvailableStorageListener(null);
            fail("Expected IllegalArgumentException to be thrown");
        }
        catch (IllegalArgumentException e)
        {
            // pass
        }

        try
        {
            cleanDir(new File(TESTDIR));
            filemgr.registerWriteableMount(TESTDIR + "=10K");
            f = new File(TESTDIR + "/testFile.txt");
            fos = new FileOutputStream(f);

            // add a listener w/highWater = 50%. Verify listener called.
            TestListener tl50 = new TestListener();
            mgr.addAvailableStorageListener(tl50, 50);
            synchronized (tl50)
            {
                tl50.reset();
                // write some stuff to the disk
                fos.write(data10k, 0, 5 * 1024);
                // wait for an event
                tl50.waitEvent(5000);
            }
            assertTrue("Expected notifyHighWaterMarkReached() to be called on listener", tl50.notifyHWMRCalled);
            assertEquals("Expected notifyHighWaterMarkReached() to be called once", 1, tl50.count);

            // add a listener w/highWater = 70% and 90%. Verify listeners are
            // called.
            TestListener tl75 = new TestListener();
            TestListener tl90 = new TestListener();
            mgr.addAvailableStorageListener(tl75, 75);
            mgr.addAvailableStorageListener(tl90, 90);

            // reset first listener
            tl50.reset();

            // write some data and verify that the 75% listener was called.
            synchronized (tl75)
            {
                tl75.reset();
                fos.write(data10k, 0, (int) (2.5 * 1024));
                // wait for an event
                tl75.waitEvent(5000);
            }
            assertTrue("Expected notifyHighWaterMarkReached() to be called on listener", tl75.notifyHWMRCalled);
            assertEquals("Expected notifyHighWaterMarkReached() to be called once", 1, tl75.count);

            // verify that they 50% and 90% listeners were not called.
            assertFalse("Did not expect 90% listener to be called", tl90.notifyHWMRCalled);
            assertFalse("Did not expect 50% listener to be called", tl50.notifyHWMRCalled);

            // reset all listeners
            tl75.reset();
            tl50.reset();
            tl90.reset();

            // write some data and verify that the 90% listener is called.
            synchronized (tl90)
            {
                tl90.reset();
                fos.write(data10k, 0, 2 * 1024);
                // wait for an event
                tl90.waitEvent(5000);
            }
            assertTrue("Expected notifyHighWaterMarkReached() to be called on listener", tl90.notifyHWMRCalled);
            assertEquals("Expected notifyHighWaterMarkReached() to be called once", 1, tl90.count);

            // verify that the 50% and 70% listeners were not called.
            assertFalse("Did not expect 75% listener to be called", tl75.notifyHWMRCalled);
            assertFalse("Did not expect 50% listener to be called", tl50.notifyHWMRCalled);

            // clear all - remove all listeners from the StorageManager
            mgr.removeAvailableStorageListener(tl50);
            mgr.removeAvailableStorageListener(tl75);
            mgr.removeAvailableStorageListener(tl90);
            tl50.reset();
            tl75.reset();
            tl90.reset();

            // try to remove a listener again. Should do nothing without any
            // exceptions
            try
            {
                mgr.removeAvailableStorageListener(tl50);
            }
            catch (Exception e)
            {
                fail("Unexpected exception thrown when a listener removed twice. e=" + e);
            }

            // clean up the disk
            fos.close();
            // truncate the file
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            raf.setLength(0);
            raf.close();

            // create new FileOutputStream for writing
            fos = new FileOutputStream(f);
            // add a listener w/highWater = 50%
            mgr.addAvailableStorageListener(tl50, 50);
            // add same listener again with highwater = 75%
            mgr.addAvailableStorageListener(tl50, 75);

            synchronized (tl50)
            {
                // reset the listener
                tl50.reset();
                // fill to 50% and verify that listener is called.
                fos.write(data10k, 0, 5 * 1024);
                tl50.wait(5000);
            }
            assertTrue("Expected notifyHighWaterMarkReached() to be called on listener", tl50.notifyHWMRCalled);
            assertEquals("Expected notifyHighWaterMarkReached() to be called once", 1, tl50.count);

            synchronized (tl50)
            {
                // reset the listener
                tl50.reset();
                // fill to 50% and verify that listener is called.
                fos.write(data10k, 0, 3 * 1024);
                tl50.wait(5000);
            }
            // verify listener was not called again even though it was added
            // with a high water mark value of 75.
            assertFalse("Did not expect notifyHighWaterMarkReached() to be called on listener", tl50.notifyHWMRCalled);
            assertEquals("Did not expect notifyHighWaterMarkReached() to be called", 0, tl50.count);

            assertFalse("Did not expect 90% listener to be called because not added", tl90.notifyHWMRCalled);
            assertFalse("Did not expect 75% listener to be called because not added", tl75.notifyHWMRCalled);

        }
        finally
        {
            filemgr.unregisterWriteableMount(TESTDIR + "=1K");
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

            if (f != null) f.delete();
        }
    }

    public void testGetAvailblePersistentStorage() throws Exception
    {
        try
        {
            StorageManager sm = StorageManager.getInstance();
            String mount = MPEEnv.getSystemProperty("dvb.persistent.root", "/syscwd") + "=1M";
            filemgr.registerWriteableMount(mount);
            long usage = getDirSize(new File(MPEEnv.getSystemProperty("dvb.persistent.root", "/syscwd")));
            assertEquals("available persistent storage does not match expected", 1024 * 1024 - usage,
                    sm.getAvailablePersistentStorage());
        }
        finally
        {
            filemgr.unregisterWriteableMount(TESTDIR + "=1M");
        }
    }

    public void testGetTotalPersistentStorage() throws Exception
    {
        try
        {
            StorageManager sm = StorageManager.getInstance();
            filemgr.registerWriteableMount(MPEEnv.getSystemProperty("dvb.persistent.root", "/syscwd") + "=1M");
            assertEquals("available persistent storage does not match expected", 1024 * 1024,
                    sm.getTotalPersistentStorage());
        }
        finally
        {
            filemgr.unregisterWriteableMount(TESTDIR + "=1M");
        }
    }

    private long getDirSize(File dir)
    {
        long size = 0;

        // Make sure the directory exists.
        if (dir.exists() == false) return 0;

        // Cycle through the list of directory entries, gathering sizes.
        String[] contents = dir.list();
        for (int i = 0; i < contents.length; ++i)
        {
            try
            {
                File f = new File(dir.getCanonicalFile() + "/" + contents[i]);
                if (f.isFile())
                {
                    size += f.length(); // Add file's size.
                }
                else
                    size += getDirSize(f); // Add size of files in directory.
            }
            catch (Exception e)
            {
                continue;
            }
        }
        return size; // Return size.
    }

    public StorageManagerTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(StorageManagerTest.class);
        return suite;
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(StorageManagerTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new StorageManagerTest(tests[i]));
            return suite;
        }
    }

    public void setUp() throws Exception
    {
        super.setUp();
        filemgr = (FileManagerImpl) ManagerManager.getInstance(FileManager.class);
        String ocap = MPEEnv.getEnv("OCAP.persistent.root");
        assertNotNull("Error! Need a place to write files", ocap);
        TESTDIR = ocap + "/junit";
        File testDir = new File(TESTDIR);
        if (!testDir.exists()) testDir.mkdirs();
    }

    public void tearDown() throws Exception
    {
        cleanDir(new File(TESTDIR));
        super.tearDown();
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

    static byte[] data10k;

    static
    {
        data10k = new byte[10 * 1024];
        for (int i = 0; i < 10 * 1024; ++i)
            data10k[i] = (byte) ((i % 26) + 'a');
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

    class TestListener implements AvailableStorageListener
    {
        boolean notifyHWMRCalled = false;

        int count = 0;

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.ocap.storage.AvailableStorageListener#notifyHighWaterMarkReached
         * ()
         */
        public synchronized void notifyHighWaterMarkReached()
        {
            notifyHWMRCalled = true;
            ++count;
            notifyAll();
        }

        public void reset()
        {
            notifyHWMRCalled = false;
            count = 0;
        }

        public void waitEvent(long millisec) throws InterruptedException
        {
            if (count > 0) return;

            wait(millisec);
        }
    }

    private String TESTDIR;

    private FileManagerImpl filemgr;

}
