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

import org.cablelabs.impl.io.DefaultFileSys;
import org.cablelabs.impl.io.http.TestDataFileHelper;

/**
 * Tests org.cablelabs.impl.io.DefaultFileSys
 * 
 * @author Paul Bramble
 */
public class DefaultFileSysTest extends TestCase
{

    public static final String BAD_FILE_NAME = "Straight-Up--If-you-want-it";

    /**
     * Invoking DefaultFileSys.test(invalid file path) returns an
     * FileNotFoundException complaining that file is missing.
     * 
     */
    public void testNonExistentPath()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        try
        {
            dfs.open(BAD_FILE_NAME);
            fail("Failed to throw exception");
        }
        catch (FileNotFoundException e)
        {
            assertTrue("Wrong exception message - expected on on missing file", e.getMessage()
                    .indexOf("does not exist") > -1);
        }
    }

    /**
     * Invoking DefaultFileSys.testO(valid directory file path) returns an
     * FileNotFoundException complaining that file is a directory.
     * 
     */
    public void testOpenOnDirectory()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        try
        {
            dfs.open(TestDataFileHelper.getTestDirName());
            fail("Failed to throw exception");
        }
        catch (FileNotFoundException e)
        {
            assertTrue("Wrong exception message - expected one about directory", e.getMessage().indexOf(
                    "is a directory") > -1);
        }

    }

    /**
     * DefaultFileSys.testO(valid file path) returns an OpenFile descriptor for
     * the specified file.
     * 
     */
    public void testOpenFile()
    {
        DefaultFileSys dfs = new DefaultFileSys();

        try
        {
            OpenFile of = dfs.open(TestDataFileHelper.getTestFilePath());
            assertTrue("Null OpenFile returned", of != null);
        }
        catch (FileNotFoundException e)
        {
            fail("Unexpected execption occurred trying to open file " + TestDataFileHelper.getTestFilePath() + "- "
                    + e.getMessage());
        }
    }

    /**
     * DefaultFileSys.testOpenClass() returns an OpenFile descriptor for the
     * specified file.
     * 
     * According to the documenation, isn't different than open.
     * 
     */
    public void testOpenClass()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        try
        {
            OpenFile of = dfs.open(TestDataFileHelper.getTestFilePath());
            assertTrue("Null OpenClass returned", of != null);
        }
        catch (FileNotFoundException e)
        {
            fail("Unexpected execption occurred trying to open class " + TestDataFileHelper.getTestFilePath() + "- "
                    + e.getMessage());

        }
    }

    /**
     * Assertion: DefaultFileSys.getFileData reads the specified file.
     * 
     * @throws Exception
     */
    public void testGetFileData()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        byte[] readBuffer = null;
        try
        {
            readBuffer = dfs.getFileData(TestDataFileHelper.getTestFilePath()).getByteData();
            assertTrue("Null OpenClass returned", readBuffer != null);
        }
        catch (FileNotFoundException e)
        {
            fail("Unexpected FileNotFoundExecption occurred trying to open " + "class "
                    + TestDataFileHelper.getTestFilePath() + "- " + e.getMessage());
        }
        catch (IOException e)
        {
            fail("Unexpected IO execption occurred trying to open class " + TestDataFileHelper.getTestFilePath() + "- "
                    + e.getMessage());
        }
        // Verify contents of readbuffer.
        byte[] expected = TestDataFileHelper.generateExpectedResults();
        for (int i = 0; i < TestDataFileHelper.getTestFileLength(); i++)
        {
            assertTrue("Expected  " + expected[i] + " found " + readBuffer[i], readBuffer[i] == expected[i]);
        }
    }

    /**
     * Invoking DefaultFileSys.exists(invalid file name) returns false;
     * 
     */
    public void testExistsOnNonExistentFile()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        assertFalse("Exists returned true on non-existent file", dfs.exists(BAD_FILE_NAME));
    }

    /**
     * Invoking DefaultFileSys.exists(valid file name) returns true
     * 
     */
    public void testExistsOnExistentFile()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        assertTrue("Exists returned true on non-existent file", dfs.exists(TestDataFileHelper.getTestFilePath()));
    }

    /**
     * Invoking DefaultFileSys.exists(valid file name) returns true
     * 
     */
    public void testExistsOnDirectory()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        assertTrue("Exists returned true on non-existent file", dfs.exists(TestDataFileHelper.getTestDirName()));
    }

    /**
     * Invoking DefaultFileSys.isDir(invalid file name) returns false;
     * 
     */
    public void testIsDirsOnNonExistentFile()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        assertFalse("isDir returned true on non-existent file", dfs.isDir(BAD_FILE_NAME));
    }

    /**
     * Invoking DefaultFileSys.isDir(valid file name) returns false.
     * 
     */
    public void testIsDirOnExistentFile()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        assertFalse("idDir returned true on file", dfs.isDir(TestDataFileHelper.getTestFilePath()));
    }

    /**
     * Invoking DefaultFileSys.exists(valid dir name) returns true
     * 
     */
    public void testIsDirOnDirectory()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        assertTrue("isDir returned true on non-existent file", dfs.isDir(TestDataFileHelper.getTestDirName()));
    }

    /**
     * Invoking DefaultFileSys.list(missing directory) returns empty list.
     * 
     */
    public void testInvalidList()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        String[] results = dfs.list(BAD_FILE_NAME);

        // Capture length, if it exists, so test can report value.
        int length = 0;
        if (results != null)
        {
            length = results.length;
        }
        assertTrue("Expected empty results list, found " + length, length == 0);
    }

    /**
     * Invoking DefaultFileSys.list(file) returns empty list.
     * 
     */
    public void testFileList()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        String[] results = dfs.list(TestDataFileHelper.getTestFilePath());

        // Capture length, if it exists, so test can report value.
        int length = 0;
        if (results != null)
        {
            length = results.length;
        }
        assertTrue("Expected empty results list, found " + length, length == 0);

    }

    /**
     * Invoking DefaultFileSys.list(directory) returns list.
     * 
     */
    public void testDirList()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        String[] results = dfs.list(TestDataFileHelper.getTestDirName());
        assertTrue("Expected non-empty results list, found empty one instead", results.length > 0);
    }

    /**
     * Invoking DefaultFileSys.length(invalid file) returns 0.
     * 
     */
    public void testInvalidFileLength()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        long length = dfs.length(BAD_FILE_NAME);
        assertTrue("Expected 0 length for invalid file, found " + length, length == 0);
    }

    /**
     * Invoking DefaultFileSys.length(invalid file) returns 0.
     * 
     */
    public void testValidFileLength()
    {
        DefaultFileSys dfs = new DefaultFileSys();
        long length = dfs.length(TestDataFileHelper.getTestFilePath());
        assertTrue("Expected non-zero length for file", length > 0);
    }

    /**
     * Invoking DefaultFileSys. returns
     * 
     */
    // public void testStubb()
    // {
    // DefaultFileSys dfs = new DefaultFileSys();
    // }

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
        TestSuite suite = new TestSuite(DefaultFileSysTest.class);
        return suite;
    }

}
