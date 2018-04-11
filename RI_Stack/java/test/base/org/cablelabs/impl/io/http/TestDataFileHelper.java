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

import java.io.FileOutputStream;
import java.io.IOException;

import org.cablelabs.impl.util.MPEEnv;

/**
 * This Class provides a standard interface to the data file used to test many
 * of the io methods, and provides one-stop shopping for changing the attributes
 * if the data file is ever changed.
 * 
 * @author Paul Bramble
 * 
 */
public class TestDataFileHelper
{

    /** Defines the name to use for the test data file. */
    public static final String TEST_FILE_NAME = "HttpOpenFileTestData";

    /** Data contents to put in test data file. */
    private static String testData = "my test data";

    /** Number of bytes in test data file. */
    private static long fileLength = testData.length();

    /** Directory in which test data file resides. */
    private static String testDirName;

    /** Contains name of test data file. */
    private static String testFileName = TEST_FILE_NAME;

    /** Full path to test data file. */
    private static String testFilePath;

    /**
     * Returns length in bytes of test data file.
     * 
     * @return long value containing number of bytes in test data file.
     */
    public static long getTestFileLength()
    {
        return fileLength;
    }

    /**
     * Returns name of directory containing test file.
     * 
     * @return String defining directory containing test file.
     */
    public static String getTestDirName()
    {
        return testDirName;
    }

    /**
     * Returns name of test data file.
     * 
     * @return String containing name of test data file.
     */
    public static String getTestFileName()
    {
        return testFileName;
    }

    /**
     * Returns file path of test data file.
     * 
     * @return String defining test data file's path.
     */
    public static String getTestFilePath()
    {
        return testFilePath;
    }

    /**
     * Generates byte array of data in the test file, allowing comparision of
     * actual versus expected results.
     * 
     * @return byte[] representing data in test file.
     */
    public static byte[] generateExpectedResults()
    {

        byte[] expected = "my test data".getBytes();

        return expected;
    }

    /**
     * Stores a copy of the test file in the defined file path.
     * 
     * @throws IOException
     */
    public static void createDataTestFile() throws IOException
    {

        testDirName = MPEEnv.getEnv("OCAP.persistent.root");

        testFilePath = testDirName + "/" + testFileName;

        try
        {
            FileOutputStream fos = new FileOutputStream(testFilePath);
            fos.write(generateExpectedResults());
            fos.close();
        }
        catch (IOException e)
        {
            throw e;
        }
    }
}
