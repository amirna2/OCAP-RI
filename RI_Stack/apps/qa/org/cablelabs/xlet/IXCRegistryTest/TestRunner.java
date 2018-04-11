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

package org.cablelabs.xlet.IXCRegistryTest;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Vector;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.event.HRcEvent;

import org.dvb.application.AppID;
import org.cablelabs.test.autoxlet.*;

public abstract class TestRunner extends Container implements KeyListener, Driveable
{
    // autoXlet
    protected AutoXletClient axc = null;

    protected Logger logger = null;

    protected Test test = null;

    private HScene m_scene = null;

    private AppID m_appID;

    protected RemoteObject m_myRemote = new RemoteObject();

    private static final Font FONT = new Font("tiresias", Font.PLAIN, 14);

    // Test xlet name (from script) along with orgID/appID info
    private String m_testInfo;

    // Tests parsed from the test script
    private Vector m_tests = new Vector();

    protected static final int TEST_RESULT_PASS = 0x1;

    protected static final int TEST_RESULT_FAIL = 0x2;

    /**
     * Initialize the testrunner xlet
     * 
     * @throws IOException
     * @throws FileNotFoundException
     * 
     * @throws FileNotFoundException
     *             if the test script file could not be found
     * @throws IOException
     *             if an error occurred while reading the script file
     */
    public void init(AppID appID, javax.tv.xlet.XletContext ctx) throws FileNotFoundException, IOException
    {
        // Set up the AutoXlet mechanism and populate local Test and Logger
        // references.
        axc = new AutoXletClient(this, ctx);
        test = axc.getTest();
        if (axc.isConnected())
            logger = axc.getLogger();
        else
            logger = new XletLogger();

        m_appID = appID;

        // Set up the scene to display text strings.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setVisible(false);
        m_scene.add(this);

        setBounds(0, 0, 640, 480);
        setBackground(Color.blue);
        setForeground(Color.white);
        setFont(FONT);

        // Parse the test script
        m_tests = new TestScriptParser().parseTestScript(getScriptFilename(m_appID));
    }

    /**
     * Abstract method allowing derived classes to return their own definitions
     * of <code>BindTest</code> test classes that perform IXC "bind" function
     * 
     * @return the <code>BindTest</code> test class
     */
    public abstract BindTest createBindTest();

    /**
     * Abstract method allowing derived classes to return their own definitions
     * of <code>BindTest</code> test classes that perform IXC "rebind" function
     * 
     * @return the <code>BindTest</code> test class
     */
    public abstract BindTest createRebindTest();

    /**
     * Abstract method allowing derived classes to return their own definitions
     * of <code>LookupTest</code> test classes that perform IXC "lookup"
     * function
     * 
     * @return the <code>LookupTest</code> test class
     */
    public abstract LookupTest createLookupTest();

    /**
     * Abstract method allowing derived classes to return their own definitions
     * of <code>ListTest</code> test classes that perform IXC "list" function
     * 
     * @return the <code>LookupTest</code> test class
     */
    public abstract ListTest createListTest();

    /**
     * Returns the script filename associated with the given <code>AppID</code>
     * 
     * @param appID
     *            the appID from which to create a script filename
     * @return the script filename
     */
    public String getScriptFilename(AppID appID)
    {
        // Create the test script filename from our orgID, appID
        return "org/cablelabs/xlet/IXCRegistryTest/scripts/" + "ixctest_" + Integer.toHexString(m_appID.getOID()) + "_"
                + Integer.toHexString(m_appID.getAID()) + ".script";
    }

    /**
     * Xlet startup tasks.
     */
    public void start()
    {
        m_scene.show();
        m_scene.requestFocus();
        m_scene.repaint();

        m_scene.addKeyListener(this);
    }

    /**
     * Xlet pause tasks
     */
    public void pause()
    {
        m_scene.setVisible(false);
        m_scene.removeKeyListener(this);
    }

    /**
     * Xlet destroy tasks
     */
    public void destroy(boolean arg0)
    {
        m_scene.setVisible(false);
        m_scene.remove(this);
        m_scene.removeKeyListener(this);
        HSceneFactory.getInstance().dispose(m_scene);
        m_scene = null;
    }

    /**
     * Set this xlet's testInfo string based on a descriptive test name (usually
     * parsed from the test script) and the xlet orgID/appID info
     * 
     * @param testName
     *            the test name
     */
    public void setTestInfo(String testName)
    {
        m_testInfo = testName + ", " + "OrgID = " + Integer.toHexString(m_appID.getOID()) + ", " + "AppID = "
                + Integer.toHexString(m_appID.getAID());
    }

    public String getTestInfo()
    {
        return m_testInfo;
    }

    public void runTests()
    {
        for (Enumeration e = m_tests.elements(); e.hasMoreElements();)
        {
            IXCTest test = (IXCTest) e.nextElement();
            test.runTest();
        }
    }

    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();

        switch (key)
        {
            case HRcEvent.VK_PLAY:
                runTests();
                logger.log(test.getTestResult());
                break;
            default:
                break;
        }
    }

    public synchronized void paint(Graphics g)
    {
        g.drawString("** PRESS \"PLAY\" TO BEGIN TEST SCRIPT **", 75, 75);
    }

    public void dispatchEvent(KeyEvent arg0, boolean arg1, int arg2)
    {
        keyPressed(arg0);
    }

    public void keyTyped(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
    }

    private class RemoteObject implements TestRemote
    {
        public String getTestString() throws RemoteException
        {
            return m_appID.toString();
        }
    }

    // /////////////////////////////
    // Test Execution Classes
    // /////////////////////////////

    /**
     * Basic test class simply provides a method for executing a test and a test
     * type description
     */
    private abstract class IXCTest
    {
        protected String m_testType;

        public IXCTest(String testType)
        {
            m_testType = testType;
        }

        public String getTestType()
        {
            return m_testType;
        }

        public abstract void runTest();

        public abstract String toString();
    }

    /**
     * Abstract class with methods and data for IXC bind and rebind tests
     * 
     * @author Greg Rutz
     */
    protected abstract class BindTest extends IXCTest
    {
        private String m_bindName = null;

        private int m_expectedResult = -1;

        public BindTest(String testType)
        {
            super(testType);
        }

        public void setBindName(String bindName)
        {
            m_bindName = bindName;
        }

        public String getBindName()
        {
            return m_bindName;
        }

        public void setExpectedResult(int result)
        {
            m_expectedResult = result;
        }

        public int getExpectedResult()
        {
            return m_expectedResult;
        }

        public String toString()
        {
            return "[ " + getTestType() + " (bindName=" + getBindName() + ", expectedResult=" + getExpectedResult()
                    + ")]";
        }
    }

    /**
     * Abstract class with methods and data for IXC list tests
     * 
     * @author Greg Rutz
     */
    protected abstract class ListTest extends IXCTest
    {
        private Vector m_expectedNames = new Vector();

        private Vector m_notExpectedNames = new Vector();

        public ListTest(String testType)
        {
            super(testType);
        }

        public void addExpectedName(String name)
        {
            m_expectedNames.addElement(name);
        }

        public Vector getExpectedNames()
        {
            return m_expectedNames;
        }

        public void addNotExpectedName(String name)
        {
            m_notExpectedNames.addElement(name);
        }

        public Vector getNotExpectedNames()
        {
            return m_notExpectedNames;
        }

        public String toString()
        {
            String nameList = "";
            for (int i = 0; i < m_expectedNames.size(); ++i)
                nameList = nameList + (String) m_expectedNames.elementAt(i) + ", ";
            String notList = "";
            for (int i = 0; i < m_notExpectedNames.size(); ++i)
                notList = notList + (String) m_notExpectedNames.elementAt(i) + ", ";
            return "[ " + getTestType() + " (expected name list={" + nameList + "}" + " (not expected name list={"
                    + notList + ")]";
        }
    }

    /**
     * Abstract class with methods and data for IXC lookup tests
     * 
     * @author Greg Rutz
     */
    protected abstract class LookupTest extends IXCTest
    {
        private String m_lookupName = null;

        private int m_expectedResult = -1;

        private String m_expectedString = null;

        public LookupTest(String testType)
        {
            super(testType);
        }

        public void setLookupName(String lookupName)
        {
            m_lookupName = lookupName;
        }

        public String getLookupName()
        {
            return m_lookupName;
        }

        public void setExpectedResult(int result)
        {
            m_expectedResult = result;
        }

        public int getExpectedResult()
        {
            return m_expectedResult;
        }

        public void setExpectedString(String expectedString)
        {
            m_expectedString = expectedString;
        }

        public String getExpectedString()
        {
            return m_expectedString;
        }

        public String toString()
        {
            return "[ " + getTestType() + " (lookupName=" + getLookupName() + ", expectedResult=" + getExpectedResult()
                    + ", expectedString=" + getExpectedString() + ")]";
        }
    }

    /**
     * This class parses a test script file and returns a list of test objects
     * parsed from the file
     */
    private class TestScriptParser
    {
        private IXCTest m_currentTest;

        private Vector m_tests = new Vector();

        // State variable used by the script parsing code
        private int m_state = STATE_NONE;

        // ///////////////////////////////////////////////
        // Test Script Keywords
        // ///////////////////////////////////////////////
        private static final char SCRIPT_COMMENT_CHAR = '#';

        private static final String SCRIPT_TEST_NAME = "testName=";

        private static final String SCRIPT_BIND_TEST = "BIND_TEST";

        private static final String SCRIPT_REBIND_TEST = "REBIND_TEST";

        private static final String SCRIPT_BIND_NAME = "bindName=";

        private static final String SCRIPT_EXPECTED_RESULT = "expectedResult=";

        private static final String SCRIPT_LOOKUP_TEST = "LOOKUP_TEST";

        private static final String SCRIPT_LOOKUP_NAME = "lookupName=";

        private static final String SCRIPT_EXPECTED_STRING = "expectedString=";

        private static final String SCRIPT_LIST_TEST = "LIST_TEST";

        private static final String SCRIPT_LIST_NAME = "listName=";

        private static final String SCRIPT_NOLIST_NAME = "nolistName=";

        private static final String SCRIPT_END_TEST = "END_TEST";

        private static final String SCRIPT_RESULT_PASS = "pass";

        private static final String SCRIPT_RESULT_FAIL = "fail";

        // ///////////////////////////////////////////////
        // Test Script Parsing States
        // ///////////////////////////////////////////////
        private static final int STATE_NONE = 0x0;

        private static final int STATE_BIND_TEST = 0x1;

        private static final int STATE_REBIND_TEST = 0x2;

        private static final int STATE_LOOKUP_TEST = 0x3;

        private static final int STATE_LIST_TEST = 0x4;

        /**
         * Parse the test script associated with this test. Test scripts are
         * named based on the app's AppID and OrgID as would be output by
         * Integer.toHexString(int):
         * 
         * ixctest_[orgID]_[appID].script
         * 
         * @param filename
         *            the test script file
         * @throws FileNotFoundException
         *             if the script file cannot be found
         * @throws IOException
         *             if any other I/O error occurs while reading the script
         *             file
         */
        public Vector parseTestScript(String filename) throws FileNotFoundException, IOException
        {
            BufferedReader reader = new BufferedReader(new FileReader(filename));

            String line = null;
            while ((line = reader.readLine()) != null)
            {
                // Empty line or comment
                if (line.length() == 0 || line.charAt(0) == SCRIPT_COMMENT_CHAR) continue;

                switch (m_state)
                {
                    case STATE_NONE:
                        parseStateNone(line);
                        break;

                    case STATE_BIND_TEST:
                        parseStateBind(line);
                        break;

                    case STATE_REBIND_TEST:
                        parseStateRebind(line);
                        break;

                    case STATE_LOOKUP_TEST:
                        parseStateLookup(line);
                        break;

                    case STATE_LIST_TEST:
                        parseStateList(line);
                        break;
                }
            }

            return m_tests;
        }

        /**
         * Parse the given test script line when the parsing state is
         * <code>STATE_NONE</code>
         * 
         * @param line
         *            the line from the script file
         * @throws IOException
         *             if an error was encountered
         */
        private void parseStateNone(String line) throws IOException
        {
            if (line.startsWith(SCRIPT_TEST_NAME))
            {
                // Retrieve the test name and set the xlet's testInfo
                setTestInfo(getValue(line));
            }
            else if (line.startsWith(SCRIPT_BIND_TEST))
            {
                m_state = STATE_BIND_TEST;
                m_currentTest = createBindTest();
            }
            else if (line.startsWith(SCRIPT_REBIND_TEST))
            {
                m_state = STATE_REBIND_TEST;
                m_currentTest = createRebindTest();
            }
            else if (line.startsWith(SCRIPT_LOOKUP_TEST))
            {
                m_state = STATE_LOOKUP_TEST;
                m_currentTest = createLookupTest();
            }
            else if (line.startsWith(SCRIPT_LIST_TEST))
            {
                m_state = STATE_LIST_TEST;
                m_currentTest = createListTest();
            }
            else
            {
                throw new IOException("Illegal test script line! -- " + line);
            }
        }

        /**
         * Parse the given test script line when the parsing state is
         * <code>STATE_BIND_TEST</code>
         * 
         * @param line
         *            the line from the script file
         * @param test
         *            the test object that should receive the parsed data
         * @throws IOException
         *             if an error was encountered
         */
        private void parseStateBind(String line) throws IOException
        {
            BindTest test = (BindTest) m_currentTest;

            if (line.startsWith(SCRIPT_BIND_NAME))
            {
                test.setBindName(getValue(line));
            }
            else if (line.startsWith(SCRIPT_EXPECTED_RESULT))
            {
                test.setExpectedResult(convertResult(getValue(line)));
            }
            else if (line.startsWith(SCRIPT_END_TEST))
            {
                m_tests.addElement(test);
                m_state = STATE_NONE;
                m_currentTest = null;
            }
            else
            {
                throw new IOException("Illegal test script line in BIND_TEST! -- " + line);
            }
        }

        /**
         * Parse the given test script line when the parsing state is
         * <code>STATE_REBIND_TEST</code>
         * 
         * @param line
         *            the line from the script file
         * @throws IOException
         *             if an error was encountered
         */
        private void parseStateRebind(String line) throws IOException
        {
            BindTest test = (BindTest) m_currentTest;

            if (line.startsWith(SCRIPT_BIND_NAME))
            {
                test.setBindName(getValue(line));
            }
            else if (line.startsWith(SCRIPT_EXPECTED_RESULT))
            {
                test.setExpectedResult(convertResult(getValue(line)));
            }
            else if (line.startsWith(SCRIPT_END_TEST))
            {
                m_tests.addElement(test);
                m_state = STATE_NONE;
                m_currentTest = null;
            }
            else
            {
                throw new IOException("Illegal test script line in REBIND_TEST! -- " + line);
            }
        }

        /**
         * Parse the given test script line when the parsing state is
         * <code>STATE_LIST_TEST</code>
         * 
         * @param line
         *            the line from the script file
         * @throws IOException
         *             if an error was encountered
         */
        private void parseStateList(String line) throws IOException
        {
            ListTest test = (ListTest) m_currentTest;

            if (line.startsWith(SCRIPT_LIST_NAME))
            {
                test.addExpectedName(getValue(line));
            }
            else if (line.startsWith(SCRIPT_NOLIST_NAME))
            {
                test.addNotExpectedName(getValue(line));
            }
            else if (line.startsWith(SCRIPT_END_TEST))
            {
                m_tests.addElement(test);
                m_state = STATE_NONE;
                m_currentTest = null;
            }
            else
            {
                throw new IOException("Illegal test script line in LIST_TEST! -- " + line);
            }
        }

        /**
         * Parse the given test script line when the parsing state is
         * <code>STATE_LOOKUP_TEST</code>
         * 
         * @param line
         *            the line from the script file
         * @throws IOException
         *             if an error was encountered
         */
        private void parseStateLookup(String line) throws IOException
        {
            LookupTest test = (LookupTest) m_currentTest;

            if (line.startsWith(SCRIPT_LOOKUP_NAME))
            {
                test.setLookupName(getValue(line));
            }
            else if (line.startsWith(SCRIPT_EXPECTED_RESULT))
            {
                test.setExpectedResult(convertResult(getValue(line)));
            }
            else if (line.startsWith(SCRIPT_EXPECTED_STRING))
            {
                test.setExpectedString(getValue(line));
            }
            else if (line.startsWith(SCRIPT_END_TEST))
            {
                m_tests.addElement(test);
                m_state = STATE_NONE;
                m_currentTest = null;
            }
            else
            {
                throw new IOException("Illegal test script line in BIND_TEST! -- " + line);
            }
        }

        /**
         * Many of the lines in our test scripts contain standard Java
         * properties in the form: <property_name>=<property_value>. This method
         * simply returns the property value to the right of the '=' character
         * 
         * @param scriptLine
         *            the line from the script
         * @return the property value
         */
        private String getValue(String scriptLine)
        {
            return scriptLine.substring(scriptLine.indexOf('=') + 1);
        }

        /**
         * Converts the given string result from the test script into an integer
         * value
         * 
         * @param result
         *            a string result value from a test script. Should be one of
         *            <code>SCRIPT_RESULT_PASS</code> or
         *            <code>SCRIPT_RESULT_FAIL</code>
         * @return the integer result value corresponding to the string value
         *         from the test script. Will be one of
         *         <code>TEST_RESULT_PASS</code> or
         *         <code>TEST_RESULT_FAIL</code>
         * @throws IOException
         *             if the string does not correspond to one of the valid
         *             string results
         */
        private int convertResult(String result) throws IOException
        {
            if (result.toLowerCase().equals(SCRIPT_RESULT_PASS)) return TEST_RESULT_PASS;

            if (result.toLowerCase().equals(SCRIPT_RESULT_FAIL)) return TEST_RESULT_FAIL;

            throw new IOException("Invalid expected result! -- " + result);
        }
    }

}
