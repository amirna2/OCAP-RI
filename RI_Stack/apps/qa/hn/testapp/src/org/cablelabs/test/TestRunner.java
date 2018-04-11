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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Vector;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

public class TestRunner implements TVTimerWentOffListener
{
    private static final String TEST_LIST_TOKEN = "TEST_CATAGORY:";

    private static final String TEST_DELIM = ",";

    private static final String TEST_COMMENT = "#";

    private Logger m_logger = null;

    private ElementList m_rootList = null;

    private int m_state = 0;

    private Test m_currentTest = null;

    private Thread m_testThread = null;

    private Vector m_stateChangeListeners = null;

    private Vector m_statusListeners = null;

    private TVTimer m_testTimer = null;

    private TVTimerSpec m_timerSpec = null;

    public TestRunner(String testFileName) throws FileNotFoundException
    {
        m_state = TestingState.TESTING_STATE_IDLE;
        m_currentTest = null;
        m_testThread = null;
        m_stateChangeListeners = new Vector();
        m_statusListeners = new Vector();

        File aFile = new File(testFileName);

        m_logger = new Logger();
        m_logger.setPrefix("TestRunner: ");
        m_logger.log("TestFile name = " + testFileName);

        TestElementBuilder builder = new TestElementBuilder();
        builder.buildTestListFromStream(aFile);
        m_rootList = builder.getRootElements();
    }

    public TestRunner()
    {

        m_state = TestingState.TESTING_STATE_IDLE;
        m_currentTest = null;
        m_testThread = null;
        m_stateChangeListeners = new Vector();
        m_statusListeners = new Vector();

        m_logger = new Logger();
        m_logger.setPrefix("TestRunner: ");
    }

    public void runTests(String[] testNames)
    {
        TestElementBuilder builder = new TestElementBuilder();
        ElementList list = null;// builder.buildTests(testNames)
        if (null != list)
        {
            this.runCategory(list);
        }
    }

    public void executeTest(final Test t)
    {
        m_currentTest = t;
        m_testTimer = TVTimer.getTimer();
        m_timerSpec = new TVTimerSpec();
        m_timerSpec.addTVTimerWentOffListener(this);
        m_timerSpec.setTime(System.currentTimeMillis() + t.getTimeout());
        try
        {
            m_testTimer.scheduleTimerSpec(m_timerSpec);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        m_testThread = new Thread()
        {

            public void run()
            {
                TestRunner.this.m_logger.log("executeTest.run() - test: " + t.getName());
                runTest(t);
                changeStatus(t.getStatus(), t);
            }
        };
        m_testThread.start();
    }

    public void timerWentOff(TVTimerWentOffEvent e)
    {
        if (m_testThread != null && m_testThread.isAlive())
        {
            // Hmmmmm - not sure how to kill this thread
            // m_testThread.stop();
            m_testThread = null;
            changeState(TestingState.TESTING_STATE_INTERRUPTED, m_currentTest);
            m_currentTest.setStatus(Test.TEST_STATUS_INTERRUPTED);
        }
    }

    public void dumpTests(ElementList list)
    {
        m_logger.log("dumpTests Enter - parentList = "
                + ((null != list.getParent()) ? list.getParent().getName() : "none"));
        Vector rootlist = list.getElements();
        for (int ii = 0; ii < rootlist.size(); ii++)
        {
            if (rootlist.elementAt(ii) instanceof Test)
            {
                Test t = (Test) rootlist.elementAt(ii);
                m_logger.log("Test: " + t.getName());
            }
            else
            {
                dumpTests((ElementList) rootlist.elementAt(ii));
            }
        }
        m_logger.log("dumpTests Exit.");
    }

    public static void main(String args[])
    {
        System.out.print("Main enter");

        try
        {
            TestRunner runner = new TestRunner("c:\\tests.txt");

            runner.dumpTests(runner.getRootElementList());
            ElementList catList = runner.getRootElementList();
            runner.runCategory(catList);
        }
        catch (FileNotFoundException fnfe)
        {
            fnfe.printStackTrace();
        }
        System.out.print("Main exit");
    }

    public void runCategory(ElementList list)
    {
        Vector testElements = list.getElements();
        for (int ii = 0; ii < testElements.size(); ii++)
        {
            if (testElements.elementAt(ii) instanceof ElementList)
            {
                m_logger.log("Found list: " + ((ElementList) testElements.elementAt(ii)).getName()
                        + " running Catagory...");
                runCategory((ElementList) testElements.elementAt(ii));
            }
            else
            {
                Test t = (Test) testElements.elementAt(ii);
                m_logger.log("Executing test: " + t.getName());
                runTest(t);
                changeStatus(t.getStatus(), t);
                m_logger.log("Test: " + t.getName());
                m_logger.log("    Description: " + t.getDescription());
                m_logger.log("    Status: " + Test.statusToString(t.getStatus()));
            }
        }

    }

    public void setCurrentTest(Test t) throws IllegalArgumentException
    {
        if (t == null)
        {
            throw new IllegalArgumentException("Invalid test: " + t);
        }
        else
        {
            m_currentTest = t;
            return;
        }
    }

    public void runTest(Test t)
    {

        int result = Test.TEST_STATUS_NONE;

        if (t == null)
        {
            changeState(TestingState.TESTING_STATE_IDLE, t);
            return;
        }

        this.m_logger.log("runTest Enter - test: " + t.getName());

        changeState(TestingState.TESTING_STATE_PREPARING, t);
        result = t.prepare();
        t.setStatus(result);
        // changeStatus(result, t);
        if (result != Test.TEST_STATUS_PASS)
        {
            changeState(TestingState.TESTING_STATE_IDLE, t);
        }

        changeState(TestingState.TESTING_STATE_EXECUTING, t);
        result = t.execute();
        t.setStatus(result);
        // changeStatus(result, t);
        if (result != Test.TEST_STATUS_PASS)
        {
            changeState(TestingState.TESTING_STATE_IDLE, t);
            return;
        }

        changeState(TestingState.TESTING_STATE_CLEANING, t);
        result = t.clean();
        t.setStatus(result);
        // changeStatus(result, t);

        changeState(TestingState.TESTING_STATE_IDLE, t);
        this.m_testTimer.deschedule(this.m_timerSpec);
    }

    public int getCurrentTestStatus()
    {
        return (this.m_currentTest == null) ? Test.TEST_STATUS_NONE : this.m_currentTest.getStatus();
    }

    public void addTestStateChangeListener(TestStateChangeListener listener)
    {
        m_stateChangeListeners.add(listener);
    }

    public void removeTestStateChangeListener(TestStateChangeListener listener)
    {
        m_stateChangeListeners.remove(listener);
    }

    public void addTestStatusListener(TestStatusListener listener)
    {
        this.m_statusListeners.add(listener);
    }

    public void removeTestStatusListener(TestStatusListener listener)
    {
        m_statusListeners.remove(listener);
    }

    private void changeState(int newState, Test t)
    {
        for (int ii = 0; ii < m_stateChangeListeners.size(); ii++)
        {
            TestStateChangeListener listener = (TestStateChangeListener) m_stateChangeListeners.elementAt(ii);
            listener.notifyStateChanged(newState, m_state, t);
        }

        m_state = newState;
    }

    private void changeStatus(int status, Test t)
    {
        t.setStatus(status);
        for (int ii = 0; ii < m_statusListeners.size(); ii++)
        {
            TestStatusListener listener = (TestStatusListener) m_statusListeners.elementAt(ii);
            listener.notifyStatusChanged(status, t);
        }
    }

    public ElementList getRootElementList()
    {
        return this.m_rootList;
    }

    public String getCurrentTestName()
    {
        return (this.m_currentTest == null) ? "NONE" : this.m_currentTest.m_name;
    }

}
