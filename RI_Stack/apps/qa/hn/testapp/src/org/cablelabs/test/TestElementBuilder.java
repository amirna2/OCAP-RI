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

package org.cablelabs.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

public class TestElementBuilder
{

    private static final String TEST_LIST_TOKEN = "TEST_CATAGORY:";

    private static final String TEST_DELIM = ",";

    private static final String CAT_DELIM = ".";

    private static final String TEST_COMMENT = "#";

    private static final String TEST_PREFIX_MARKER = "PREFIX:";

    private Logger m_logger = null;

    private ElementList m_rootElement = null;

    private Vector m_prefixList = null;

    public TestElementBuilder()
    {
        this.m_rootElement = new ElementList("Main");
        this.m_prefixList = new Vector();
        this.m_logger = new Logger();
        this.m_logger.setPrefix("TestElementBuilder: ");
    }

    public ElementList getRootElements()
    {
        return this.m_rootElement;
    }

    private boolean lineHasPrefix(String line)
    {
        for (int ii = 0; ii < this.m_prefixList.size(); ii++)
        {
            if (true == line.startsWith((String) this.m_prefixList.elementAt(ii)))
            {
                return true;
            }
        }
        return false;
    }

    private void buildPrefixList(FileReader reader)
    {
        m_logger.log("buildPrefixList Enter.");

        BufferedReader input = new BufferedReader(reader);
        String line = null; // not declared within while loop

        try
        {
            while ((line = input.readLine()) != null)
            {
                line = line.trim();
                m_logger.log("buildPrefixList - line: " + line.substring(0, (10 > line.length()) ? line.length() : 10));
                if (true == line.startsWith(TEST_PREFIX_MARKER))
                {
                    // This is a list identifier. Create a new list.
                    String prefix = line.substring(TEST_PREFIX_MARKER.length());
                    prefix = prefix.trim();
                    m_prefixList.add(prefix);
                    m_logger.log("buildPrefixList - acquired prefix = " + prefix);
                    continue;
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        m_logger.log("buildPrefixList Exit.");
    }

    public String[] parseTestNames(String commaDelimTestNames)
    {
        String[] testNameArray = null;
        int numOfTokens = 0;

        if (null == commaDelimTestNames) return null;

        StringTokenizer st = new StringTokenizer(commaDelimTestNames, ",");
        numOfTokens = st.countTokens();

        testNameArray = new String[numOfTokens];

        for (int ii = 0; ii < numOfTokens; ii++)
        {
            String testName = st.nextToken().trim();
            testNameArray[ii] = testName;
        }
        return testNameArray;
    }

    public ElementList buildTests(String[] testNames)
    {
        Test newTest = null;
        ElementList list = new ElementList("Main List");

        if (null == testNames) return null;

        for (int ii = 0; ii < testNames.length; ii++)
        {
            if (null != (newTest = createTestFromString(testNames[ii])))
            {
                list.addElement(newTest);
            }
            else
            {
                m_logger.logError("Test Name is not a valid test: " + testNames[ii]);
            }
        }
        return list;
    }

    public static void main(String args[])
    {
        System.out.print("Main enter");
        // String testFileName = "c:\\tests.txt";
        // File aFile = new File(testFileName);
        //		
        // TestElementBuilder teb = new TestElementBuilder();
        // try {
        // teb.buildTestListFromStream(aFile);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        String commaDelimTestNames = "org.cablelabs.test.media.Test1,org.cablelabs.test.media.Test2";
        TestElementBuilder teb = new TestElementBuilder();
        String[] testNames = teb.parseTestNames(commaDelimTestNames);
        ElementList list = teb.buildTests(testNames);
        TestRunner runner = new TestRunner();
        runner.dumpTests(list);

        System.out.print("Main exit");
    }

    private int getMatchingPrefixSize(String s)
    {
        for (int ii = 0; ii < this.m_prefixList.size(); ii++)
        {
            String prefixString = (String) this.m_prefixList.elementAt(ii);
            if (true == s.startsWith(prefixString))
            {
                return prefixString.length();
            }
        }
        return 0;
    }

    public Test createTestFromString(String testName)
    {
        m_logger.log("createTestFromString - testName: " + testName);
        Test testObject = null;
        try
        {
            Class test = Class.forName(testName);
            Object t = test.newInstance();
            if (t instanceof Test)
            {
                m_logger.log("createTestFromString - created instance of : " + testName);
                testObject = (Test) t;
                // push it into the list
                // this.m_rootElement.addTest(testObject);
            }
            else
            {
                m_logger.log("createTestFromString - found invalid test class");
            }
        }
        catch (ClassNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InstantiationException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return testObject;
    }

    public void buildTestListFromStream(File file)
    {
        m_logger.log("buildTestListFromStream Enter.");
        Test newTest = null;
        String testString = null;
        String packageString = null;
        StringTokenizer tokenizer = null;
        FileReader reader = null;
        BufferedReader input = null;
        String line = null;

        try
        {
            reader = new FileReader(file);

            input = new BufferedReader(reader);

            // Build the prefix list.
            this.buildPrefixList(reader);

            if (0 == this.m_prefixList.size())
            {
                m_logger.logError("No prefixes found. Building flat element structure.");
            }

        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }

        try
        {
            // Reset the reader.
            reader = new FileReader(file);
            input = new BufferedReader(reader);

            while ((line = input.readLine()) != null)
            {
                line = line.trim();
                m_logger.log("buildTestListFromStream - line: "
                        + line.substring(0, (10 > line.length()) ? line.length() : 10));
                m_logger.log("buildTestListFromStream - line: " + line);
                if (line.equals(""))
                {
                    m_logger.log("buildTestListFromStream - found empty line");
                    continue;
                }
                if (line.startsWith(TEST_COMMENT))
                {
                    m_logger.log("buildTestListFromStream - found comment line");
                    continue;
                }
                if (line.startsWith(TEST_PREFIX_MARKER))
                {
                    m_logger.log("buildTestListFromStream - Prefix marker found");
                    continue;
                }

                if (false == this.lineHasPrefix(line))
                {
                    m_logger.log("buildTestListFromStream - prefix did not match - ignoring");
                    // tokenizer = new StringTokenizer(line,TEST_DELIM);
                    //					
                    // String testName = tokenizer.nextToken();
                    // String timeout = tokenizer.nextToken();
                    // m_logger.log("buildTestListFromStream - testName: " +
                    // testName);
                    // m_logger.log("buildTestListFromStream - timeout: " +
                    // timeout);
                    // m_logger.log("buildTestListFromStream - line doesn't have contain "
                    // +
                    // "define prefix - adding as base test");
                    // if (null != (newTest = this.createTestFromString(line)))
                    // this.m_rootElement.addElement(newTest);
                    continue;
                }

                // Now all lines that have made it here have a matching prefix.
                // First save off the line as a new test.
                testString = line;

                // crop off the prefix and the first '.' in this
                // package name.
                int prefixSize = this.getMatchingPrefixSize(line);
                // add 1 to remove the '.' in the package name.
                prefixSize++;

                // Get the remaining string which should be the remaining
                // packages
                // after the prefix and then the test name.
                packageString = line.substring(prefixSize);

                // Tokenize the package string
                // Otherwise, it is a test class name and a timeout.
                tokenizer = new StringTokenizer(packageString, CAT_DELIM);

                int numOfTokens = tokenizer.countTokens();

                // The last token is the test name and timeout - don't process
                numOfTokens--;
                String[] categories = new String[numOfTokens];

                for (int ii = 0; ii < numOfTokens; ii++)
                {
                    categories[ii] = tokenizer.nextToken();
                }

                tokenizer = new StringTokenizer(testString, TEST_DELIM);

                ElementList currentElement = createCategory(categories);

                String testName = tokenizer.nextToken();
                String timeout = tokenizer.nextToken();
                testName = testName.trim();
                timeout = timeout.trim();
                m_logger.log("buildTestListFromStream - testName: " + testName);
                m_logger.log("buildTestListFromStream - timeout: " + timeout);

                if (null != (newTest = createTestFromString(testName)))
                {
                    newTest.setRunTime(new Integer(timeout).longValue());
                    currentElement.addElement(newTest);
                }
                else
                {
                    m_logger.logError("Test Name is not valid: " + testName);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            reader.close();
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }
    }

    private ElementList createCategory(String[] packages)
    {
        int numOfPackages = packages.length;

        ElementList currentElement = this.m_rootElement;

        for (int ii = 0; ii < numOfPackages; ii++)
        {
            // sub list categories are built into lists
            m_logger.log("createCategory - category: " + packages[ii]);
            // does this category exist.
            currentElement = findCategory(packages[ii], currentElement);
        }

        return currentElement;
    }

    private ElementList findCategory(String catName, ElementList startingElement)
    {
        int numChildElements = startingElement.getElements().size();
        Vector elements = startingElement.getElements();
        ElementList currentElement = null;
        for (int jj = 0; jj < numChildElements; jj++)
        {
            Object o = elements.elementAt(jj);
            if (o instanceof ElementList)
            {
                currentElement = (ElementList) elements.elementAt(jj);
                if (currentElement.getName().equalsIgnoreCase(catName))
                {
                    return currentElement;
                }
            }
        }
        // Didn't find one so add it as new.
        ElementList newList = new ElementList(catName.toUpperCase());
        newList.setParent(startingElement);
        startingElement.addElement((TestElement) newList);
        return newList;
    }

}
