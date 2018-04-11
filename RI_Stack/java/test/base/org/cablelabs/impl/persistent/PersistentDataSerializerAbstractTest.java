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

package org.cablelabs.impl.persistent;

import org.cablelabs.test.TestUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import org.cablelabs.impl.util.MPEEnv;

import junit.framework.TestCase;

/**
 * Tests PersistentDataSerializer.
 * 
 * @author Aaron Kamienski
 */
public abstract class PersistentDataSerializerAbstractTest extends TestCase
{
    public void testNoPublicConstructors()
    {
        TestUtils.testNoPublicConstructors(PersistentDataSerializer.class);
    }

    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(PersistentDataSerializer.class);
    }

    /**
     * Tests file names. Uses the prefix/baseDir specified by this test.
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testFilenames() throws Exception
    {
        // New data
        PersistentData info = newData(0);
        String name = prefix + Long.toHexString(info.uniqueId);
        File file = new File(baseDir, name);
        File file1 = new File(baseDir, name + ".1");

        assertFalse("Did not expect file " + file + " to exist yet", file.exists());
        assertFalse("Did not expect file " + file1 + " to exist yet", file1.exists());

        // Save data
        doSave(pds, info);

        // Verify file is created and location
        assertTrue("Expected file " + file + " to be created", file.exists());
        assertFalse("Did not expect file " + file1 + " to be created", file1.exists());

        Vector v = doLoad(pds);
        assertEquals("Expected 1 record to be loaded", 1, v.size());
        assertEquals("Loaded record doesn't match saved record", info, v.elementAt(0));

        // Modify data
        PersistentData info2 = newData(info.uniqueId);
        modifyData(info2);

        // Update
        doSave(pds, info2);

        // Verify file is created and location
        assertTrue("Expected file " + file + " to be updated", file.exists());
        assertFalse("Did not expect file " + file1 + " to be updated", file1.exists());

        v = doLoad(pds);
        assertEquals("Expected 1 record to be loaded (2)", 1, v.size());
        assertEquals("Loaded record doesn't match saved record (2)", info2, v.elementAt(0));
    }

    /**
     * Tests file names w/ large uniqueIds.
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testFilenames_big() throws Exception
    {
        // New data
        PersistentData info = newData(0x123456789abcdefL);
        records.addElement(info);
        String name = prefix + "123456789abcdef";
        File file = new File(baseDir, name);
        File file1 = new File(baseDir, name + ".1");

        assertFalse("Did not expect file " + file + " to exist yet", file.exists());
        assertFalse("Did not expect file " + file1 + " to exist yet", file1.exists());

        // Save data
        doSave(pds, info);

        // Verify file is created and location
        assertTrue("Expected file " + file + " to be created", file.exists());
        assertFalse("Did not expect file " + file1 + " to be created", file1.exists());

        Vector v = doLoad(pds);
        assertEquals("Expected 1 record to be loaded", 1, v.size());
        assertEquals("Loaded record doesn't match saved record", info, v.elementAt(0));

    }

    /**
     * Tests cleanup operation.
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testSave_cleanup() throws Exception
    {
        // New data
        PersistentData info = newData(1);
        String name = PFX + Long.toHexString(info.uniqueId);
        File file = new File(baseDir, name);
        File file1 = new File(baseDir, name + ".1");

        assertFalse("Did not expect file " + file + " to exist yet", file.exists());
        assertFalse("Did not expect file " + file1 + " to exist yet", file1.exists());

        // manually write out just file.1
        doSave(pds, info);
        assertTrue("rename " + file + " -> " + file1 + " failed", file.renameTo(file1));
        assertTrue("Expect file exist - " + file1, file1.exists());
        assertFalse("Expect file *not* to exist - " + file, file.exists());

        // Modify
        modifyData(info);

        // Try to save data
        doSave(pds, info);

        // Verify that only file exists
        assertTrue("Expect file exist - " + file, file.exists());
        assertFalse("Expect file *not* to exist - " + file1, file1.exists());

        // Verify contents of file
        Vector v = doLoad(pds);
        assertEquals("Expected 1 record to be loaded", 1, v.size());
        assertEquals("Expected updated record to be saved", info, v.elementAt(0));
        // Delete
        doDelete(pds, info);

        // manually write out file and file.1
        doSave(pds, info);
        copy(file, file1);
        assertTrue("Expect file exist - " + file, file.exists());
        assertTrue("Expect file exist - " + file1, file1.exists());

        // Modify
        modifyData(info);

        // Try to save data
        doSave(pds, info);

        // Verify that only file exists
        assertTrue("Expect file exist - " + file, file.exists());
        assertFalse("Expect file *not* to exist - " + file1, file1.exists());

        // Verify contents of file
        v = doLoad(pds);
        assertEquals("Expected 1 record to be loaded", 1, v.size());
        assertEquals("Expected updated record to be saved", info, v.elementAt(0));
        // Delete
        doDelete(pds, info);
    }

    private void copy(File src, File dst) throws Exception
    {
        InputStream in = new BufferedInputStream(new FileInputStream(src));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(dst));
        byte[] array = new byte[256];
        int n;

        while ((n = in.read(array)) > 0)
        {
            out.write(array, 0, n);
        }
        in.close();
        out.flush();
        out.close();
    }

    /**
     * Tests corruption of file.
     */
    public void testCorruption_empty() throws Exception
    {
        doCorruption(0);
    }

    /**
     * Tests corruption of file.
     */
    public void testCorruption_text() throws Exception
    {
        doCorruption(1);
    }

    /**
     * Tests corruption of file.
     */
    public void testCorruption_trunc() throws Exception
    {
        doCorruption(2);
    }

    /**
     * Tests corruption of file.
     */
    public void testCorruption_missing() throws Exception
    {
        doCorruption(3);
    }

    /**
     * Tests corruption of file.
     */
    public void testCorruption_mod() throws Exception
    {
        doCorruption(4);
    }

    /**
     * Tests corruption of file.
     */
    public void testCorruption_csum() throws Exception
    {
        doCorruption(4);
    }

    /**
     * Tests corruption of file.
     */
    public void testCorruption_type() throws Exception
    {
        doCorruption(4);
    }

    /**
     * Tests corruption of file.1.
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testCorruption1_empty() throws Exception
    {
        doCorruption1(0);
    }

    /**
     * Tests corruption of file.1.
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testCorruption1_text() throws Exception
    {
        doCorruption1(1);
    }

    /**
     * Tests corruption of file.1.
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testCorruption1_trunc() throws Exception
    {
        doCorruption1(2);
    }

    /**
     * Tests corruption of file.1.
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testCorruption1_missing() throws Exception
    {
        doCorruption1(3);
    }

    /**
     * Tests corruption of file.1.
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testCorruption1_mod() throws Exception
    {
        doCorruption1(4);
    }

    /**
     * Tests corruption of file.1.
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testCorruption1_csum() throws Exception
    {
        doCorruption1(4);
    }

    /**
     * Tests corruption of file.1.
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testCorruption1_type() throws Exception
    {
        doCorruption1(4);
    }

    /**
     * Corrupts the given file based on the given request id. Request <i>i</i>
     * indicates:
     * <ol>
     * <li value="0">empty file
     * <li>text file
     * <li>truncated file
     * <li>pre-truncated file
     * <li>modified file (change 1 bit in middle)
     * <li>modified file (change 8-byte checksum)
     * <li>manually serialized other object
     * </ol>
     * 
     * @param i
     *            indicates how to corrupt file
     * @param file
     *            the file to corrupt
     */
    private void doCorruption(int i, File file) throws Exception
    {
        // Try with:
        // - empty file
        // - text file
        // - truncated file
        // - modified file (middle)
        // - modified file (end, csum)
        // - (manually) serialized other object
        switch (i)
        {
            default:
                break;

            case 0: // empty file
            {
                file.delete();

                FileOutputStream out = new FileOutputStream(file);
                out.flush();
                out.close();
                break;
            }
            case 1: // text file
            {
                file.delete();

                PrintWriter out = new PrintWriter(new FileWriter(file));
                out.println("Hello, world");
                out.flush();
                out.close();
                break;
            }
            case 2: // truncated file
            case 3: // pre-truncated file
            case 4: // modified file
            case 5: // modified csum
            {
                // Read entire file into an array
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                FileInputStream in = new FileInputStream(file);
                byte[] array = new byte[256];
                int size;
                while ((size = in.read(array)) > 0)
                {
                    bos.write(array, 0, size);
                }
                in.close();
                bos.flush();

                // delete original file
                file.delete();

                // Write all but the last 12 bytes to disk
                FileOutputStream out = new FileOutputStream(file);
                array = bos.toByteArray();
                switch (i)
                {
                    case 2:
                        out.write(array, 0, array.length - 12);
                        break;
                    case 3:
                        out.write(array, 12, array.length - 12);
                        break;
                    case 4:
                        // Modify one byte in the middle
                        array = bos.toByteArray();
                        array[array.length / 2] ^= 0x10;
                        out.write(array);
                        break;
                    case 5:
                        out.write(array, 0, array.length - 8);
                        out.write(new byte[8]);
                        break;
                }
                out.flush();
                out.close();
                bos.close();

                break;
            }
            case 6: // manually serialized other object
            {
                // delete original file
                file.delete();

                // write another object to disk
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
                out.writeObject(new Vector());
                out.flush();
                out.close();
                break;
            }
        }
    }

    /**
     * Tests dealing with corruption.
     */
    protected void doCorruption(int i) throws Exception
    {
        // New data
        PersistentData info = newData(10 + i);

        // save data
        doSave(pds, info);

        String name = prefix + Long.toHexString(info.uniqueId);
        File file = new File(baseDir, name);
        File file1 = new File(baseDir, name + ".1");
        file1.delete();

        // Do Corruption
        doCorruption(i, file);

        assertFalse("Internal Test Error - Didn't expect file.1 - " + i + " - " + file1, file1.exists());
        assertTrue("Internal Test Error - File should exist - " + i + " - " + file, file.exists());

        // load data - should fail
        Vector v = null;
        try
        {
            v = doLoad(pds);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Unexpected exception - " + i + " - " + file + " " + e);
        }
        assertEquals("Expected load to fail - " + i + " - " + file, 0, v.size());

        // Expect corrupted file to be deleted
        assertFalse("Internal Test Error - Didn't expect file.1 - " + i + " - " + file1, file1.exists());
        assertFalse("Expected corrupt file to be deleted - " + i + " - " + file, file.exists());
    }

    /**
     * Tests load() with corruption of file.1.
     */
    protected void doCorruption1(int i) throws Exception
    {
        // New data
        PersistentData info = newData(20 + i);

        // save data
        doSave(pds, info);

        String name = prefix + Long.toHexString(info.uniqueId);
        File file = new File(baseDir, name);
        File file1 = new File(baseDir, name + ".1");
        File save = new File(baseDir, name + ".save");

        // mv file (for safe keeping)
        file.renameTo(save);

        // Update
        modifyData(info);
        doSave(pds, info);

        // mv file (from safe keeping)
        save.renameTo(file1);

        // Do corruption
        doCorruption(i, file1);

        assertTrue("Internal Test Error - File should exist - " + i + " - " + file, file.exists());
        assertTrue("Internal Test Error - File should exist - " + i + " - " + file1, file1.exists());

        // load data - should succeed
        Vector v = null;
        try
        {
            v = doLoad(pds);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Unexpected exception - " + i + " - " + file + " " + e);
        }

        assertEquals("Expected one record", 1, v.size());
        assertEquals("Expected to fall back to file", info, v.elementAt(0));

        // Expect corrupted file to be deleted
        assertTrue("Internal Test Error - File should exist - " + i + " - " + file, file.exists());
        assertFalse("Expected corrupted file.1 to be deleted - " + i + " - " + file1, file1.exists());

    }

    /**
     * Tests load() with both file and file.1.
     */
    public void testLoad_multiple() throws Exception
    {
        // New data
        PersistentData info = newData(1000);
        String name = prefix + Long.toHexString(info.uniqueId);
        File file = new File(baseDir, name);
        File file1 = new File(baseDir, name + ".1");

        assertFalse("Did not expect file " + file + " to exist yet", file.exists());
        assertFalse("Did not expect file " + file1 + " to exist yet", file1.exists());

        doSave(pds, info);
        copy(file, file1);

        // Load data
        Vector v = doLoad(pds);
        assertEquals("Only expected 1 record to be loaded", 1, v.size());
    }

    public PersistentDataSerializerAbstractTest(String name)
    {
        super(name);
    }

    protected PersistentData newData(long i) throws Exception
    {
        PersistentData data = createData(i);
        records.addElement(data);
        return data;
    }

    protected abstract PersistentDataSerializer createInstance(File baseDir, String prefix);

    protected abstract PersistentData createData(long i) throws Exception;

    protected abstract void modifyData(PersistentData data) throws Exception;

    protected abstract boolean isEquals(PersistentData d0, PersistentData d1);

    protected abstract void doSave(PersistentDataSerializer pds, PersistentData data) throws Exception;

    protected abstract void doDelete(PersistentDataSerializer pds, PersistentData data) throws Exception;

    protected abstract Vector doLoad(PersistentDataSerializer pds) throws Exception;

    private static int spfx = 0;

    private int pfx = ++spfx;

    protected String PFX = "tst" + pfx + "-";

    protected Vector records = new Vector();

    protected File baseDir;

    protected String prefix;

    protected PersistentDataSerializer pds;

    protected void setUp() throws Exception
    {
        super.setUp();

        String path = MPEEnv.getSystemProperty("PersistentDataTest.dir");
        if (path == null)
        {
            path = MPEEnv.getSystemProperty("dvb.persistent.root");
            if (path == null)
            {
                path = "/snfs";
            }
        }
        File dir = new File(path);
        if (!dir.exists()) dir = new File("/syscwd");
        if (!dir.exists()) dir = new File(".");
        baseDir = new File(dir, "testpds");

        prefix = PFX;

        pds = createInstance(baseDir, prefix);
    }

    protected void tearDown() throws Exception
    {
        for (Enumeration e = records.elements(); e.hasMoreElements();)
        {
            PersistentData info = (PersistentData) e.nextElement();
            doDelete(pds, info);
        }

        // pds.destroy

        super.tearDown();
    }
}
