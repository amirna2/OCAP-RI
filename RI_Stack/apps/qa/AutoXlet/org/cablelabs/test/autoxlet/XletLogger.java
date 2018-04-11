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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketException;

/**
 * The AutoXlet logging class. Supports logging <code>String</code>,
 * <code>TestResult</code>, and <code>Exception</code> types. Log messages can
 * be written to STDOUT, a local file, or a remote file (via UDP).
 * 
 * @author Greg Rutz
 */
public class XletLogger implements Logger, IXCLogger
{
    /**
     * Constructs an Xlet logger that will write its log output to the standard
     * output
     */
    public XletLogger()
    {
        m_pstream = System.out;
        m_startTime = System.currentTimeMillis();
    }

    /**
     * Constructs an Xlet logger that will write to the local log file specified
     * 
     * @throws IOException
     *             if the local file could not be opened
     */
    public XletLogger(String logfile) throws IOException
    {
        m_pstream = new PrintStream(new FileOutputStream(logfile), true);
        m_startTime = System.currentTimeMillis();
    }

    /**
     * Constructs an Xlet logger that will write logging output to a UDP logging
     * server specified by its server name and port number
     * 
     * @param server
     *            the log server IP or hostname
     * @param port
     *            the port on which the log server is listening
     * @throws IllegalArgumentException
     *             if the specified hostname is not valid
     * @throws SocketException
     *             if a connection could not be established to the
     */
    public XletLogger(String server, int port) throws IllegalArgumentException, SocketException
    {
        m_pstream = new PrintStream(new UDPLogger(server, port), true);
        m_startTime = System.currentTimeMillis();
    }

    /**
     * Writes a summary of test results (in JUnit-like fashion) to the log
     * 
     * @param testResults
     *            the test results to output to the log
     */
    public synchronized void log(TestResult testResults)
    {
        m_endTime = System.currentTimeMillis();
        (new ResultPrinter(m_pstream)).print(testResults, m_endTime - m_startTime);
    }

    /**
     * Writes the given string to the log
     * 
     * @param message
     *            the string to output to the log
     */
    public synchronized void log(String message)
    {
        m_pstream.println(message);
    }

    /**
     * Writes information about a thrown exception to the log. Writes exception
     * message and stack trace.
     * 
     * @param e
     *            the exception to output to the log
     */
    public synchronized void log(Exception e)
    {
        e.printStackTrace(m_pstream);
    }

    /**
     * Shutdown this logger
     */
    public void close()
    {
        m_pstream.close();
    }

    /**
     * Provides access to the <code>PrintStream</code> associated with this
     * logger for general purpose use
     * 
     * @return the <code>PrintStream</code> associated with this logger
     */
    public PrintStream getPrintStream()
    {
        return m_pstream;
    }

    private PrintStream m_pstream = null;

    private long m_startTime = 0;

    private long m_endTime = 0;
}
