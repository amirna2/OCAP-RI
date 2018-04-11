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

import java.io.*;
import java.util.Enumeration;

import org.cablelabs.test.autoxlet.TestFailure;
import org.cablelabs.test.autoxlet.TestResult;

/**
 * A slightly modified version of the JUnit results printer. Prints test results
 * from a <code>TestResults</code> object to a <code>PrintStream</code>
 * 
 * @author Greg Rutz
 */
public class ResultPrinter
{
    /**
     * Construct a ResultPrinter for writing to a particular print stream
     * 
     * @param writer
     *            The <code>PrintStream</code> where the test results will be
     *            sent
     */
    public ResultPrinter(PrintStream writer)
    {
        fWriter = writer;
    }

    /**
     * Print test results
     * 
     * @param result
     *            the test results to print
     * @param runTime
     *            the total time for this set of tests
     */
    public synchronized void print(TestResult result, long runTime)
    {
        printHeader(runTime);
        printFailures(result);
        printFooter(result);
    }

    /*
     * Internal methods
     */

    protected void printHeader(long runTime)
    {
        fWriter.println();
    }

    protected void printFailures(TestResult result)
    {
        printDefects(result.failures(), result.failureCount(), "failure");
    }

    protected void printDefects(Enumeration booBoos, int count, String type)
    {
        if (count == 0) return;
        if (count == 1)
            fWriter.println("There was " + count + " " + type + ":");
        else
            fWriter.println("There were " + count + " " + type + "s:");
        for (int i = 1; booBoos.hasMoreElements(); i++)
        {
            printDefect((TestFailure) booBoos.nextElement(), i);
        }
    }

    protected void printDefect(TestFailure booBoo, int count)
    {
        printDefectHeader(booBoo, count);
        printDefectTrace(booBoo);
    }

    protected void printDefectHeader(TestFailure booBoo, int count)
    {
        // I feel like making this a println, then adding a line giving the
        // throwable a chance to print something
        // before we get to the stack trace.
        fWriter.print(count + ") ");
    }

    /**
     * Returns a filtered stack trace
     */
    protected static String getFilteredTrace(Throwable t)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        StringBuffer buffer = stringWriter.getBuffer();
        String trace = buffer.toString();
        return getFilteredTrace(trace);
    }

    /**
     * Filters stack frames from internal JUnit classes
     */
    protected static String getFilteredTrace(String stack)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StringReader sr = new StringReader(stack);
        BufferedReader br = new BufferedReader(sr);

        String line;
        try
        {
            while ((line = br.readLine()) != null)
            {
                if (!filterLine(line)) pw.println(line);
            }
        }
        catch (Exception IOException)
        {
            return stack; // return the stack unfiltered
        }
        return sw.toString();
    }

    protected void printDefectTrace(TestFailure booBoo)
    {
        fWriter.print(getFilteredTrace(booBoo.trace()));
    }

    protected void printFooter(TestResult result)
    {
        if (result.wasSuccessful())
        {
            fWriter.println();
            fWriter.print("OK");
            fWriter.println(" (" + result.runCount() + " test" + (result.runCount() == 1 ? "" : "s") + ")");

        }
        else
        {
            fWriter.println();
            fWriter.println("FAILURES!!!");
            fWriter.println("Tests run: " + result.runCount() + ",  Failures: " + result.failureCount());
        }
        fWriter.println();
    }

    private static boolean filterLine(String line)
    {
        String[] patterns = new String[] { "org.cablelabs.test.autoxlet.TestResult",
                "org.cablelabs.test.autoxlet.Test.", // don't filter
                                                     // AssertionFailure
                "java.lang.reflect.Method.invoke(" };
        for (int i = 0; i < patterns.length; i++)
        {
            if (line.indexOf(patterns[i]) > 0) return true;
        }
        return false;
    }

    void printWaitPrompt()
    {
        fWriter.println();
        fWriter.println("<RETURN> to continue");
    }

    PrintStream fWriter;

    int fColumn = 0;
}
