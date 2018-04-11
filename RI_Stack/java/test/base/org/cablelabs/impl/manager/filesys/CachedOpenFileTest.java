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

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CachedOpenFileTest extends TestCase
{
    /**
     * Test the read() and read(byte[], int, int) methods
     * 
     * @throws Exception
     */
    public void testRead() throws Exception
    {
        // verify that read data matches expected data
        AuthOpenFile of = new AuthOpenFile(TestFileSys.EXISTS_FILE, TestFileSys.FILE_DATA);
        for (int i = 0; i < TestFileSys.FILE_DATA.length; i++)
        {
            assertEquals("Read data does not match expected data index=" + i, TestFileSys.FILE_DATA[i],
                    (byte) of.read());
        }

        // verify that reading again returns -1
        int data = of.read();
        assertEquals("Expected -1 to be returned after all data read", -1, data);

        // verify that read data matches expected data for read(byte[], int,
        // int)
        of = new AuthOpenFile(TestFileSys.EXISTS_FILE, TestFileSys.FILE_DATA);
        byte array[] = new byte[TestFileSys.FILE_DATA.length];
        int length = of.read(array, 0, array.length);

        // was the number of bytes read the same as FILE_DATA.length
        assertEquals("Number of bytes read not same as test data length", TestFileSys.FILE_DATA.length, length);

        // verify the data read
        for (int i = 0; i < array.length; i++)
        {
            assertEquals("Read data does not match expected data index=" + i, TestFileSys.FILE_DATA[i], array[i]);
        }

        // verify that reading again returns -1
        length = of.read(array, 0, array.length);
        assertEquals("Expected -1 to be returned after all data read", -1, length);

        // verify that read(byte[], int, int) returns the correct values for bad
        // parameters
        of = new AuthOpenFile(TestFileSys.EXISTS_FILE, TestFileSys.FILE_DATA);
        array = new byte[TestFileSys.FILE_DATA.length];

        // read with offset past the end of the data
        try
        {
            length = of.read(array, array.length + 1, array.length);
            fail("Expected ArrayIndexOutOfBoundsException to be thrown");
        }
        catch (IndexOutOfBoundsException e)
        {
            // pass
        }

        // read with length = 0
        length = of.read(array, 0, 0);
        assertEquals("Expected 0 to be returned", 0, length);

        // read with length = -1
        try
        {
            length = of.read(array, 0, -1);
            fail("Expected ArrayIndexOutOfBoundsException to be thrown");
        }
        catch (IndexOutOfBoundsException e)
        {
            // pass
        }

        // verify read where index is part way through data and try to read
        // into an array larger than available
        // clear out the array object
        for (int i = 0; i < array.length; i++)
        {
            array[i] = 0;
        }
        of = new AuthOpenFile(TestFileSys.EXISTS_FILE, TestFileSys.FILE_DATA);

        // read the first two bytes and discard the data
        of.read();
        of.read();

        // try to read a whole array length of data
        of.read(array, 0, array.length);
        for (int i = 0; i < array.length - 2; i++)
        {
            assertEquals("Read data does not match expected data index=" + i, TestFileSys.FILE_DATA[i + 2], array[i]);
        }

        // verify that the end of the array is unchanged. Elements should be 0
        assertEquals("Expected element to be zero", 0, array[array.length - 2]);
        assertEquals("Expected element to be zero", 0, array[array.length - 1]);
    }

    /**
     * Test the skip method.
     * 
     * @throws Exception
     */
    public void testSkip() throws Exception
    {
        AuthOpenFile of = new AuthOpenFile(TestFileSys.EXISTS_FILE, TestFileSys.FILE_DATA);
        // read a byte of data
        int data = of.read();
        assertEquals("byte read does not match expected data", TestFileSys.FILE_DATA[0], (byte) data);

        // skip the next byte
        long skipped = of.skip(1);
        assertEquals("Expected only 1 byte to be skipped", 1, skipped);

        // get the next byte
        data = of.read();
        assertEquals("byte read does not match expected data", TestFileSys.FILE_DATA[2], (byte) data);

        // skip past the end of the file
        skipped = of.skip(TestFileSys.FILE_DATA.length);
        assertEquals("Expected number of bytes skipped does not match expected", TestFileSys.FILE_DATA.length - 3,
                skipped);

        // try to read a byte -- should fail and return -1
        data = of.read();
        assertEquals("byte read does not match expected data", -1, data);

        // skip back to the front of the file
        skipped = of.skip(-TestFileSys.FILE_DATA.length);
        assertEquals("Expected number of bytes skipped does not match expected", -TestFileSys.FILE_DATA.length, skipped);

        // read a byte of data and it should be the first byte from the file
        data = of.read();
        assertEquals("byte read does not match expected data", TestFileSys.FILE_DATA[0], (byte) data);

        // try to skip past the front of the file
        skipped = of.skip(-2);
        assertEquals("Number of bytes skipped does not match expected", -1, skipped);

        // read a byte of data and it should be the first byte from the file
        data = of.read();
        assertEquals("byte read does not match expected data", TestFileSys.FILE_DATA[0], (byte) data);

        // skip back to the beginning
        skipped = of.skip(-1);
        assertEquals("Expected number of bytes skipped does not match expected", -1, skipped);

        // skip to the end
        skipped = of.skip(TestFileSys.FILE_DATA.length);
        assertEquals("Expected number of bytes skipped does not match expected", TestFileSys.FILE_DATA.length, skipped);

        // try to read a byte -- should fail and return -1
        data = of.read();
        assertEquals("byte read does not match expected data", -1, data);
    }

    /**
     * Test the available() method.
     * 
     * @throws Exception
     */
    public void testAvailable() throws Exception
    {
        // create OpenFile instance
        AuthOpenFile of = new AuthOpenFile(TestFileSys.EXISTS_FILE, TestFileSys.FILE_DATA);
        int avail = of.available();

        // available returned k bytes and expect to read those k bytes.
        byte array[] = new byte[avail];
        int read = of.read(array, 0, array.length);
        assertEquals("expected number of bytes read to equal available bytes", avail, read);

        // should be no more available
        assertEquals("Expected no bytes to be available", 0, of.available());
        // no more data should be successfully read
        read = of.read();
        assertEquals("expected no more bytes to be available", -1, read);
    }

    /**
     * Test the getFilePointer() method.
     * 
     * @throws Exception
     */
    public void testGetFilePointer() throws Exception
    {
        // file pointer should start at 0
        AuthOpenFile of = new AuthOpenFile(TestFileSys.EXISTS_FILE, TestFileSys.FILE_DATA);
        assertEquals("File pointer should start at zero", 0, of.getFilePointer());

        // skip a few bytes
        long skipped = of.skip(2);
        assertEquals("Expected only 2 bytes to be skipped", 2, skipped);

        // file pointer should move
        assertEquals("File pointer should of moved", skipped, of.getFilePointer());

        // skip past the end
        of.skip(TestFileSys.FILE_DATA.length);

        // pointer should be at end.
        assertEquals("Expected file pointer to be at the end", TestFileSys.FILE_DATA.length, of.getFilePointer());
    }

    /**
     * Test the length() method.
     * 
     * @throws Exception
     */
    public void testLength() throws Exception
    {
        AuthOpenFile of = new AuthOpenFile(TestFileSys.EXISTS_FILE, TestFileSys.FILE_DATA);
        // length should be same as testfilesys data length
        assertEquals("File length does not match expected length", TestFileSys.FILE_DATA.length, of.length());

        // skip past end, length should still be the same.
        of.skip(TestFileSys.FILE_DATA.length);
        assertEquals("File length does not match expected length", TestFileSys.FILE_DATA.length, of.length());
    }

    /**
     * Test the seek() method.
     * 
     * @throws Exception
     */
    public void testSeek() throws Exception
    {
        AuthOpenFile of = new AuthOpenFile(TestFileSys.EXISTS_FILE, TestFileSys.FILE_DATA);

        // seek to a spot and read the data. Should match master data
        of.seek(2);
        assertEquals("File data does not match expected.", TestFileSys.FILE_DATA[2], of.read());

        // seek past end and read
        try
        {
            of.seek(TestFileSys.FILE_DATA.length + 1);
            fail("Expected IOException to be thrown!");
        }
        catch (IOException e)
        {
            // pass
        }
        // seek past front and read
        try
        {
            of.seek(-1);
            fail("Expected IOException to be thrown!");
        }
        catch (IOException e)
        {
            // pass
        }

        // seek to a front and read the data. Should match master data
        of.seek(0);
        assertEquals("File data does not match expected.", TestFileSys.FILE_DATA[0], of.read());
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AuthOpenFileTest.class);
        return suite;
    }

    public CachedOpenFileTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
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

}
