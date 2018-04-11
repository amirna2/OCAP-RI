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

import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.cablelabs.impl.io.DefaultOpenFile;
import org.cablelabs.impl.io.http.TestDataFileHelper;

/**
 * Tests org.cablelabs.impl.io.DefaultOpenFile
 * 
 * @author Paul Bramble
 */
public class DefaultOpenFileTest extends TestCase
{

    /**
     * Assertion: Invoking DefaultOpenFile(valid_path) results in a
     * DefaultOpenFile Instance.
     * 
     */
    public void testConstructor()
    {
        try
        {
            OpenFile dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
            assertTrue(true);
        }
        catch (FileNotFoundException e)
        {
            fail("Unexpected file not found exception : " + e.getMessage());
        }
    }

    /**
     * Assertion: Invoking DefaultOpenFile(valid_path) results in a
     * FileNotFoundExcepton.
     * 
     */
    public void testInvalidConstructor()
    {
        String path = "aManaPlanaCanalPanama";

        try
        {
            OpenFile dof = new DefaultOpenFile(path);
            fail("Constructor did not throw FileNotFoundException");

        }
        catch (FileNotFoundException e)
        {
            assertTrue(true);
        }
    }

    /**
     * Assertion: read (array, 0 offest, input length) reads the specified file
     * into the specified array using the underlying (native) file system.
     * 
     * @throws Exception
     */
    public void testBasicRead() throws Exception
    {
        byte[] expected = TestDataFileHelper.generateExpectedResults();

        OpenFile dof = null;
        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // read into buffer.
            byte[] b = new byte[(int) TestDataFileHelper.getTestFileLength()];
            int count = dof.read(b, 0, (int) TestDataFileHelper.getTestFileLength());

            // Verify whole file read.
            assertTrue("Exptected " + TestDataFileHelper.getTestFileLength() + " bytes, actually read " + count,
                    count == TestDataFileHelper.getTestFileLength());

            // Verify read correctly.
            for (int i = 0; i < TestDataFileHelper.getTestFileLength(); i++)
            {
                assertTrue("Expected  " + expected[i] + " found " + b[i], b[i] == expected[i]);
            }
        }
        catch (Exception e)
        {
            fail("Exception occurred reading HttpOpenFile : " + e.getMessage());
        }
        finally
        {
            if (dof != null)
            {
                dof.close();
            }

        }
    }

    /**
     * Assertion: read (array, 0 offest, 0 length) reads nothing, doesn't
     * advance position.
     * 
     * @throws Exception
     */
    public void testEmptyRead() throws Exception
    {
        byte[] expected = new byte[(int) TestDataFileHelper.getTestFileLength()];
        expected[0] = 0;
        expected[1] = 0;
        expected[2] = 0;
        expected[3] = 0;
        expected[4] = 0;
        expected[5] = 0;
        expected[6] = 0;
        expected[7] = 0;
        expected[8] = 0;
        expected[9] = 0;
        expected[10] = 0;
        expected[11] = 0;

        OpenFile dof = null;
        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // Verify before size.
            assertTrue("Expected " + TestDataFileHelper.getTestFileLength() + " avaiable, found " + dof.available(),
                    dof.available() == TestDataFileHelper.getTestFileLength());

            // read into buffer.
            byte[] b = new byte[(int) TestDataFileHelper.getTestFileLength()];
            int count = dof.read(b, 0, 0);

            // Verify position not advanced.
            assertTrue("Expected after read length of" + TestDataFileHelper.getTestFileLength()
                    + " bytes avaiable, found " + dof.available(),
                    dof.available() == TestDataFileHelper.getTestFileLength());

            // Verify - bytes read.
            assertTrue("Exptected 0 bytes, actually read " + count, count == 0);

            // Verify destination buffer unaffected.
            for (int i = 0; i < TestDataFileHelper.getTestFileLength(); i++)
            {
                assertTrue("Expected  " + expected[i] + " found " + b[i], b[i] == expected[i]);
            }
        }
        catch (Exception e)
        {
            fail("Exception occurred reading HttpOpenFile : " + e.getMessage());
        }
        finally
        {
            if (dof != null)
            {
                dof.close();
            }
        }
    }

    /**
     * Assertion: read (array, some offest o, length n) where offset + length >=
     * file length, reads n bytes into destination buffer, starting at postion
     * o, using the underlying (native) file system.
     * 
     * @throws Exception
     */
    public void testOffsetRead() throws Exception
    {
        byte[] expected = new byte[(int) TestDataFileHelper.getTestFileLength()];
        expected[0] = 0;
        expected[1] = 0;
        expected[2] = 0;
        expected[3] = 0;
        expected[4] = 0;
        expected[5] = 0;
        expected[6] = 'm';
        expected[7] = 'y';
        expected[8] = ' ';
        expected[9] = 't';
        expected[10] = 'e';
        expected[11] = 's';

        OpenFile dof = null;
        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // Verify before size.
            assertTrue("Expected " + TestDataFileHelper.getTestFileLength() + " avaiable, found " + dof.available(),
                    dof.available() == TestDataFileHelper.getTestFileLength());

            // read into buffer.
            byte[] b = new byte[(int) TestDataFileHelper.getTestFileLength()];
            int offset = 6;
            int length = 6;
            int count = dof.read(b, offset, length);

            // Verify position not advanced.
            assertTrue("Expected after read length of " + (TestDataFileHelper.getTestFileLength() - length)
                    + " bytes avaiable, found " + dof.available(),
                    dof.available() == TestDataFileHelper.getTestFileLength() - length);

            // Verify - bytes read.
            assertTrue("Exptected " + length + " bytes, actually read " + count, count == length);

            // Verify destination buffer values match expected results.
            for (int i = 0; i < TestDataFileHelper.getTestFileLength(); i++)
            {
                assertTrue("Expected  " + expected[i] + " found " + b[i], b[i] == expected[i]);
            }
        }
        catch (Exception e)
        {
            fail("Exception occurred reading HttpOpenFile : " + e.getMessage());
        }
        finally
        {
            if (dof != null)
            {
                dof.close();
            }
        }
    }

    /**
     * Assertion: When read () is called n times on an n length file, it reads
     * the entire file one byte at a time, using the underlying (native) file
     * system.
     * 
     * @throws Exception
     */
    public void testSimpleRead() throws Exception
    {
        /** current location in file. */
        int i = 0;

        OpenFile dof = null;

        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // read data into buffer one byte at a time.
            byte[] expected = TestDataFileHelper.generateExpectedResults();
            byte[] b = new byte[(int) TestDataFileHelper.getTestFileLength()];
            for (i = 0; i < (int) TestDataFileHelper.getTestFileLength(); i++)
            {
                b[i] = (byte) dof.read();
                assertTrue(" read " + b[i] + " expected " + expected[i], b[i] == expected[i]);
            }
        }
        catch (Exception e)
        {
            fail("Exception occurred reading HttpOpenFile at location " + i + " : " + e.getMessage());
        }
        finally
        {
            if (dof != null)
            {
                dof.close();
            }
        }
    }

    /**
     * Assertion: HttpOpenFile.available() returns the correct amount of unread
     * bytes left in the http file.
     * 
     * @throws Exception
     */
    public void testAvailable() throws Exception
    {
        OpenFile dof = null;

        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // Verify whole file left at start.
            assertTrue("Expected remaining length of " + TestDataFileHelper.getTestFileLength() + ", found "
                    + dof.available(), dof.available() == TestDataFileHelper.getTestFileLength());

            // Advance current position and verify remaining length.
            long skipLength = 5;
            long expectedRemaining = TestDataFileHelper.getTestFileLength() - skipLength;
            dof.seek(skipLength);
            assertTrue("Expected remaininglength of " + expectedRemaining + ", found " + dof.available(),
                    dof.available() == expectedRemaining);

            // Advance position to EOF and verify nothing left available.

            dof.seek(TestDataFileHelper.getTestFileLength());

            assertTrue("Expected remaining length of 0, found " + dof.available(), dof.available() == 0);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            fail("Unexpected IOExcepton : " + e.getMessage());
        }
        finally
        {
            dof.close();
        }
    }

    /**
     * Assertion: HttpOpenFile.skip(n) skips over n bytes in the http file.
     * 
     * @throws Exception
     */
    public void testSkip() throws Exception
    {
        OpenFile dof = null;

        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // Verify whole file left at start.
            assertTrue("Expected remaining length of " + TestDataFileHelper.getTestFileLength() + ", found "
                    + dof.available(), dof.available() == TestDataFileHelper.getTestFileLength());

            // Advance current position and verify remaining length.
            long skipLength = 5;
            long expectedRemaining = TestDataFileHelper.getTestFileLength() - skipLength;
            dof.skip(skipLength);
            assertTrue("Expected remaininglength of " + expectedRemaining + ", found " + dof.available(),
                    dof.available() == expectedRemaining);

            // Advance position to EOF and verify nothing left available.
            dof.skip(expectedRemaining);
            assertTrue("Expected remaining length of 0, found " + dof.available(), dof.available() == 0);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            fail("Unexpected IOExcepton : " + e.getMessage());
        }
        finally
        {
            dof.close();
        }
    }

    /**
     * Assertion: When HttpOpenFile.close() is called on an Http File, further
     * method calls on that object result in an Exception.
     * 
     * @throws Exception
     */
    public void testClose() throws Exception
    {
        OpenFile dof = null;

        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // Verify file is open by trying a seek.
            dof.seek(1);

            // Close file.
            dof.close();

            // Try seek again, and hopefully generate exception
            dof.seek(1);
            fail("Still able to do operations on closed file");
        }
        catch (Exception e)
        {
            assertTrue(true);
        }
        finally
        {
            try
            {
                // No clear requirment as to whether this should fail.
                dof.close();
                // fail("Second attempt at close failed to throw exception.");
            }
            catch (Exception e)
            {
                assertTrue(true);
            }
        }
    }

    /**
     * Assertion: HttpOpenFile.length() returns the length of the file,
     * regardless of how much of it has been read.
     * 
     * @throws Exception
     */
    public void testLength() throws Exception
    {
        OpenFile dof = null;

        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // Check length of file before reading any.
            assertTrue("Expected length of " + TestDataFileHelper.getTestFileLength() + ", found " + dof.length(),
                    dof.length() == TestDataFileHelper.getTestFileLength());

            // Advance current position by reading and verify location.
            dof.read();
            assertTrue("Expected post-read length of " + TestDataFileHelper.getTestFileLength() + ", found "
                    + dof.length(), dof.length() == TestDataFileHelper.getTestFileLength());
        }
        catch (IOException e)
        {
            fail("Unexpected IOExcepton while checking http file length " + e.getMessage());
        }
        finally
        {
            dof.close();
        }
    }

    /**
     * Assertion: HttpOpenFile.getFilePointer() returns the current file
     * position.
     * 
     * @throws Exception
     */
    public void testGetFilePointer() throws Exception
    {
        OpenFile dof = null;

        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // Advance current position by reading and verify location.
            assertTrue("Expected initial position of 0, found " + dof.getFilePointer(), dof.getFilePointer() == 0);
            dof.read();
            assertTrue("Expected initial  of 1, found " + dof.getFilePointer(), dof.getFilePointer() == 1);
            dof.read();
            assertTrue("Expected initial position of 2, found " + dof.getFilePointer(), dof.getFilePointer() == 2);
        }
        catch (IOException e)
        {
            fail("getFilePointer() threw unexpected exception " + e.getMessage());
        }
        finally
        {
            dof.close();
        }
    }

    /**
     * Assertion: HttpOpenFile.seek() throws an IO Exception when requested to
     * seek after end of file.
     * 
     * @throws Exception
     */
    /*
     * - I'd like to include this test, but I can't find a firm requirement as
     * to what this code shold do when attempting to seek past eof.
     * 
     * 
     * public void testSeekPastEOF() throws Exception { OpenFile dof = null;
     * 
     * try { //Open Default file. dof = new
     * DefaultOpenFile(TestDataFileHelper.getTestFilePath()); } catch (Exception
     * e) { fail("Exception occurred constructing DefaultOpenFile instance : " +
     * e.getMessage()); }
     * 
     * try { //Verify positioned to BOF. long position = dof.length() + 1000;
     * assertTrue("Expected initial position of 0, found " +
     * dof.getFilePointer(), dof.getFilePointer() == 0);
     * 
     * //Try to seek before BOF and hopefully generate IOException.
     * dof.seek(position); fail("seek(past EOF) failed to throw IOExcepton"); }
     * catch(IOException e) { assertTrue(true); } finally { dof.close(); } }
     */

    /**
     * Assertion: HttpOpenFile.seek() throws an IO Exception when request to
     * seek before beginning of file.
     * 
     * @throws Exception
     */
    public void testSeekBeforeBOF() throws Exception
    {
        OpenFile dof = null;

        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // Verify file pointer points to BOF.
            assertTrue("Expected initial position of 0, found " + dof.getFilePointer(), dof.getFilePointer() == 0);

            // Try to seek before BOF, and hopefully generate exception.
            dof.seek(-1);
            fail("seek(before BOF) failed to throw IOExcepton");
        }
        catch (IOException e)
        {
            assertTrue(true);
        }
        finally
        {
            dof.close();
        }
    }

    /**
     * Assertion: HttpOpenFile.seek(n) will throw exepction when n =
     * length_of_file.
     * 
     * @throws Exception
     */
    public void testBoundarySeek() throws Exception
    {
        OpenFile dof = null;

        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // Verify file pointer points to BOF.
            assertTrue("Expected initial position of 0, found " + dof.getFilePointer(), dof.getFilePointer() == 0);

            // Seek just before EOF.
            dof.seek(TestDataFileHelper.getTestFileLength() - 1);
            assertTrue("Expected 1 byte remaining, found " + dof.available(), dof.available() == 1);
        }
        catch (Exception e)
        {
            dof.close();
            fail("seek(EOF) threw IOExcepton " + e.getMessage());
        }

        try
        {

            // Verify seek(file_length) generates IOException.
            dof.seek(TestDataFileHelper.getTestFileLength());
            assertTrue("Expected 0 bytes remaining, found " + dof.available(), dof.available() == 0);
            // fail("seek(EOF) failed to throw IOExcepton ");
        }
        catch (IOException e)
        {
            assertTrue(true);
        }
        finally
        {
            dof.close();
        }
    }

    /**
     * Assertion: HttpOpenFile.seek() will set the file's current position to
     * the specified location.
     * 
     * @throws Exception
     */
    public void testSeek() throws Exception
    {
        OpenFile dof = null;

        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // Verify pointer points to BOF.
            assertTrue("Expected initial position of 0, found " + dof.getFilePointer(), dof.getFilePointer() == 0);

            // Skip 3 spaces and verify position reflects seek.
            dof.seek(3);
            assertTrue("Expected post-seek position of 3, found " + dof.getFilePointer(), dof.getFilePointer() == 3);
        }
        catch (IOException e)
        {
            fail("seek(valid value) threw unexpected Exception " + e.getMessage());
        }
        finally
        {
            dof.close();
        }
    }

    /**
     * Assertion: HttpOpenFile.seek() can seek in reverse direction from current
     * position.
     * 
     * @throws Exception
     */
    public void testReverseSeek() throws Exception
    {
        OpenFile dof = null;

        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // Verify file positioned at BOF.
            assertTrue("Expected initial position of 0, found " + dof.getFilePointer(), dof.getFilePointer() == 0);

            // Read several lines, and verify pointer points to current
            // position.
            dof.read();
            dof.read();
            dof.read();

            assertTrue("Expected position of 3, found " + dof.getFilePointer(), dof.getFilePointer() == 3);

            // Reset pointer and verify current position reflects seek.
            dof.seek(1);

            assertTrue("Expected initial position of 1, found " + dof.getFilePointer(), dof.getFilePointer() == 1);
        }
        catch (IOException e)
        {
            fail("seek(negative valule) threw unexpected IOExcepton" + e.getMessage());
        }
        finally
        {
            dof.close();
        }
    }

    /**
     * Assertion: When read () is called n times on an n length http file, it
     * reads the entire file one byte at a time.
     * 
     * @throws Exception
     */
    public void testSimpleReadWithAvailable() throws Exception
    {
        byte[] expected = TestDataFileHelper.generateExpectedResults();

        /** current location in file. */
        int i = 0;

        OpenFile dof = null;

        try
        {
            // Open Default file.
            dof = new DefaultOpenFile(TestDataFileHelper.getTestFilePath());
        }
        catch (Exception e)
        {
            fail("Exception occurred constructing DefaultOpenFile instance : " + e.getMessage());
        }

        try
        {
            // read data into buffer one byte at a time.
            byte[] b = new byte[(int) TestDataFileHelper.getTestFileLength()];
            for (i = 0; i < (int) TestDataFileHelper.getTestFileLength() / 2; i++)
            {
                b[i] = (byte) dof.read();
                assertTrue(" read " + b[i] + " expected " + expected[i], b[i] == expected[i]);
            }
            int expectedAvailable = ((int) TestDataFileHelper.getTestFileLength()) - i;
            assertTrue("expected " + expectedAvailable + " bytes available, found " + dof.available(),
                    dof.available() == expectedAvailable);
        }
        catch (Exception e)
        {
            fail("Exception occurred reading HttpOpenFile at location " + i + " : " + e.getMessage());
        }
        finally
        {
            dof.close();
        }
    }

    protected void setUp() throws Exception
    {
        TestDataFileHelper.createDataTestFile();
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
        TestSuite suite = new TestSuite(DefaultOpenFileTest.class);
        return suite;
    }
}
