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

package org.cablelabs.test.autoxlet;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.tv.xlet.XletContext;
import org.dvb.io.ixc.IxcRegistry;

/**
 * The <code>AutoXletClient</code> class handles all IXC communication between
 * the <code>XletDriver</code> and a test Xlet. Once a link has been established
 * with the driver, the test Xlet has access to a logging object which will send
 * all log messages to a centralized log file managed by the driver.
 * Additionally, <code>AutoXletClient</code> provides JUnit-like testing
 * functions and to test results collection to the test Xlet. Test results are
 * accessible by the <code>XletDriver</code> via IXC.
 * 
 * @author Greg Rutz
 */
public class AutoXletClient implements TestCollector
{
    /**
     * Construct an AutoXlet test Xlet client object for establishing
     * communication with the <code>XletDriver</code>. If a connection to the
     * driver can not be created, events will not be delivered to the Xlet's
     * <code>Driveable</code> interface and log messages sent to the
     * <code>AutoXletClient</code>'s logger will be written to STDOUT.
     * 
     * @param driveable
     *            an object implementing the <code>Driveable</code> interface
     *            that will receive remote control events from the driver
     * @param context
     *            the context object for this xlet used in establishing IXC
     *            communication with the driver
     */
    public AutoXletClient(Driveable driveable, XletContext context)
    {
        // Try to lookup the automated Xlet runner's logging class. If this
        // fails, we can assume that we are not running under an automated
        // environment and we will use STDOUT for all log messages.
        Remote remoteObject = null;
        try
        {
            remoteObject = IxcRegistry.lookup(context, "/1/7000/IXCLogger");

            m_logger = new AutoXletClientLogger((IXCLogger) remoteObject);

            // Publish this TestCollector via IXC so that the automated test
            // runner
            // can access our test results when test script is completed
            IxcRegistry.bind(context, "TestCollector", this);

            // Publish the provided driveable via IXC so that the automated test
            // runner
            // can dispatch remote control events to the Xlet
            IxcRegistry.bind(context, "Driveable", driveable);

            m_isConnected = true;
        }
        catch (Exception e)
        {
            // If any of the IXC registration calls fail, then we are not
            // running
            // in an automated environment, so just create a default logger
            // which
            // will log messages to STDOUT
            m_logger = new AutoXletClientLogger(new XletLogger());
        }
    }

    /**
     * Constructor for Xlets to use when running outside the AutoXlet framework.
     * The logger is initialized to log debug statements and test results to a
     * local file.
     * 
     * @param localLogFile
     *            the name of the logfile on the local filesystem to which the
     *            logger will write debug statements and test results
     * @throws IOException
     *             if the file cannot be opened for writing
     */
    public AutoXletClient(String localLogFile) throws IOException
    {
        m_logger = new AutoXletClientLogger(new XletLogger(localLogFile));
    }

    /**
     * Get the <code>Logger</code> object to use for outputting debug statements
     * and test results. When running in the AutoXlet framework and connected to
     * the driver, this logger will actually write log messages to a driver-
     * specified log file.
     * 
     * @return the logger
     */
    public Logger getLogger()
    {
        return m_logger;
    }

    /**
     * Get the <code>Test</code> object that provides JUnit-like assert
     * statements and test result collection facilities
     * 
     * @return the test object
     */
    public Test getTest()
    {
        return m_test;
    }

    /**
     * Get the test results for all assert statements that have been executed on
     * the <code>Test</code> object. Required by the <code>TestCollector</code>
     * interface.
     */
    public TestResult getTestResults()
    {
        return m_test.getTestResult();
    }

    /**
     * Return the connectivity state between the driver and the client.
     * 
     * @return true if the driver and client are connected, false otherwise
     */
    public boolean isConnected()
    {
        return m_isConnected;
    }

    /**
     * Clear out all test results for assert statements that have been executed
     * on the <code>Test</code> object. The test count is also reset to 0.
     */
    public void clearTestResults()
    {
        m_test.getTestResult().clearTestResults();
    }

    /*
     * This is a helper class that takes care of catching the remote exceptions
     */
    class AutoXletClientLogger implements Logger
    {
        public AutoXletClientLogger(IXCLogger logger)
        {
            m_logger = logger;
        }

        public void log(TestResult testResults)
        {
            try
            {
                m_logger.log(testResults);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }

        public void log(String message)
        {
            try
            {
                m_logger.log(message);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }

        public void log(Exception e)
        {
            try
            {
                m_logger.log(e);
            }
            catch (RemoteException re)
            {
                re.printStackTrace();
            }
        }

        private IXCLogger m_logger;
    }

    private boolean m_isConnected = false;

    private Test m_test = new Test();

    private AutoXletClientLogger m_logger = null;
}
