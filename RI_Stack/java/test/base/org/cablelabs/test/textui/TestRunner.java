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

package org.cablelabs.test.textui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Vector;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.runner.ReloadingTestSuiteLoader;
import junit.runner.TestSuiteLoader;
import junit.runner.Version;
import junit.textui.ResultPrinter;

import org.cablelabs.impl.util.MPEEnv;

/**
 * Adapts the standard <code>junit.textui.TestRunner</code> to support output to
 * a file.
 */
//findbugs complains about this pattern - shadowing superclass' name.
//Unfortunately, its a common pattern in the RI (so we ignore it).
public class TestRunner extends junit.textui.TestRunner
{
    private static boolean appendLog;

    public TestRunner(PrintStream writer)
    {
        super(writer);
    }

    public TestRunner()
    {
        super();
    }

    public static void run(Class testClass)
    {
        System.out.println("\n\n##########\n\n  Entered 'run(Class)'\n\n##########\n\n");
        run(new TestSuite(testClass));
        System.out.println("\n\n##########\n\n  'run(Class)' finished\n\n##########\n\n");
    }

    protected static PrintStream openOutput()
    {
        String logName = new String(MPEEnv.getEnv("TEST.JUNIT.LOGFILE", "/syscwd/junit_test_results.log"));
        System.out.println("\nJUnit log file name is " + logName + "\n");
        return openOutput(logName);
    }

    protected static PrintStream openOutput(String filename)
    {
        PrintStream out = null;
        File logFile = new File(filename);

        try
        {
            /*
             * This ugly hack below was written to account for the possible bug
             * in appending to a log file across SNFS. Normally, we would just
             * use FileOutputStream(String filename, boolean append)
             */
            byte[] contents = null;
            if (appendLog)
            {
                FileInputStream fis = new FileInputStream(logFile);
                int size = fis.available();
                contents = new byte[size];
                fis.read(contents);
            }

            FileOutputStream fos = new FileOutputStream(logFile);
            if (contents != null)
            {
                fos.write(contents);
                fos.flush();
            }
            out = new PrintStream(new TeeOutputStream(fos, System.out));
        }
        catch (Exception e)
        {
            System.err.println("Could not create output file " + filename);
            e.printStackTrace();
            out = System.out;
        }
        return out;
    }

    public static TestResult run(Test test)
    {
        System.out.println("\n\n##########\n\n  Entered 'run(Test)'\n\n##########\n\n");
        PrintStream out = openOutput();
        TestRunner runner = new TestRunner(out);
        TestResult result = runner.doRun(test);
        out.close();
        System.out.println("\n\n##########\n\n  'run(Test)' finished\n\n##########\n\n");
        return result;
    }

    // Only used by OcapSuite...
    public static void appendLog(boolean append)
    {
        appendLog = append;
    }

    /**
     * Overrides super implementation.
     * 
     * @see junit.runner.BaseTestRunner#getTest(java.lang.String)
     */
    public Test getTest(String test)
    {
        int idx = test.indexOf(":");
        if (idx < 0)
            return super.getTest(test);
        else
        {
            String testClass = test.substring(0, idx);
            String testMethod = test.substring(idx + 1);

            try
            {
                Class theClass = getLoader().load(testClass);
                Constructor ctor = theClass.getConstructor(new Class[] { String.class });
                return (Test) ctor.newInstance(new Object[] { testMethod });
            }
            catch (ClassNotFoundException e)
            {
                runFailed("Class not found \"" + testClass + "\"");
            }
            catch (Exception e)
            {
                runFailed("Could not construct test \"" + test + "\"");
            }
            return null;
        }
    }

    /**
     * Overrides super implementation.
     * 
     * @see junit.textui.TestRunner#start(java.lang.String[])
     */
    protected TestResult start(String[] args) throws Exception
    {
        Vector testCases = new Vector();
        boolean wait = false;
        for (int i = 0; i < args.length; i++)
        {
            if ("-wait".equals(args[i]))
                wait = true;
            else if ("-c".equals(args[i]))
                testCases.addElement(extractClassName(args[++i]));
            else if ("-v".equals(args[i]))
                System.err.println("JUnit " + Version.id() + " by Kent Beck and Erich Gamma");
            else if ("-tee".equals(args[i]))
                setPrinter(new ResultPrinter(openOutput(args[++i])));
            else if (args[i].startsWith("-tee="))
                setPrinter(new ResultPrinter(openOutput(args[i].substring("-tee=".length()))));
            else if ("-?".equals(args[i]) || "-h".equals(args[i]) || "-help".equals(args[i]))
                usage();
            else
                testCases.addElement(args[i]);
        }
        if (testCases.size() == 0) usage();

        if (testCases.size() == 1)
        {
            try
            {
                Test suite = getTest((String) testCases.elementAt(0));
                return doRun(suite, wait);
            }
            catch (Exception e)
            {
                throw new Exception("Could not create and run test suite: " + e);
            }
        }
        else
        {
            try
            {
                TestSuite suite = new TestSuite();
                for (Enumeration e = testCases.elements(); e.hasMoreElements();)
                {
                    String testCase = (String) e.nextElement();
                    Test test = getTest(testCase);
                    suite.addTest(test);
                }

                return doRun(suite, wait);
            }
            catch (Exception e)
            {
                throw new Exception("Could not create and run suites: " + testCases);
            }
        }
    }

    private static void usage() throws Exception
    {
        throw new Exception("Usage: TestRunner [-wait] name ..., where name is the name of the TestCase class");
    }

    public static void main(String args[])
    {
        TestRunner aTestRunner = new TestRunner(openOutput());
        try
        {
            TestResult r = aTestRunner.start(args);
            if (!r.wasSuccessful()) System.exit(FAILURE_EXIT);
            System.exit(SUCCESS_EXIT);
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            System.exit(EXCEPTION_EXIT);
        }
    }

    /**
     * Return a test result that minimizes the amount of memory used
     */
    protected TestResult createTestResult()
    {
        return new TestResultLowMemory();
    }
}

class TeeOutputStream extends OutputStream
{
    OutputStream tee = null, out = null;

    public TeeOutputStream(OutputStream chainedStream, OutputStream teeStream)
    {
        out = chainedStream;

        if (teeStream == null)
            tee = System.out;
        else
            tee = teeStream;
    }

    /**
     * Implementation for parent's abstract write method. This writes out the
     * passed in character to both the chained stream and "tee" stream.
     */
    public void write(int c) throws IOException
    {
        out.write(c);

        tee.write(c);
        tee.flush();
    }

    /**
     * Overrides parent's write method (which would simply call the
     * {@link #write(int)} method). This writes out to both the chained stream
     * and "tee" stream.
     */
    public void write(byte[] b) throws IOException
    {
        write(b, 0, b.length);
    }

    /**
     * Overrides parent's write method (which would simply call the
     * {@link #write(int)} method). This writes out to both the chained stream
     * and "tee" stream.
     */
    public void write(byte[] b, int off, int len) throws IOException
    {
        out.write(b, off, len);

        tee.write(b, off, len);
        tee.flush();
    }

    /**
     * Closes both, chained and tee, streams.
     */
    public void close() throws IOException
    {
        flush();

        // Avoid closing System.out and System.err
        if (out != System.out && out != System.err) out.close();
        if (tee != System.out && tee != System.err) tee.close();
    }

    /**
     * Flushes chained stream; the tee stream is flushed each time a character
     * is written to it.
     */
    public void flush() throws IOException
    {
        out.flush();
    }
}

class TestResultLowMemory extends TestResult
{
    /**
     * Adds an error to the list of errors. The passed in exception caused the
     * error.
     */
    public synchronized void addError(Test test, Throwable t)
    {
        fErrors.addElement(new TestFailureLowMemory(test, t));
        for (Enumeration e = fListeners.elements(); e.hasMoreElements();)
        {
            ((TestListener) e.nextElement()).addError(test, t);
        }
    }

    /**
     * Adds a failure to the list of failures. The passed in exception caused
     * the failure.
     */
    public synchronized void addFailure(Test test, AssertionFailedError t)
    {
        fFailures.addElement(new TestFailureLowMemory(test, t));
        for (Enumeration e = fListeners.elements(); e.hasMoreElements();)
        {
            ((TestListener) e.nextElement()).addFailure(test, t);
        }
    }
}

class TestFailureLowMemory extends TestFailure
{
    /**
     * Constructs a TestFailure with the given test and exception.
     */
    public TestFailureLowMemory(Test failedTest, Throwable thrownException)
    {
        super(failedTest, thrownException);
        //
        // overwrite the failed test with our facade so the real test object
        // can be garbage collected
        //
        fFailedTest = new TestFacade(failedTest);
    }

}

class TestFacade implements Test
{
    private int count;

    private String toString;

    public TestFacade(Test t)
    {
        count = t.countTestCases();
        toString = t.toString();
    }

    public int countTestCases()
    {
        return count;
    }

    public void run(TestResult result)
    {
        throw new RuntimeException("The test facade for " + toString + " cannot run");
    }

    public String toString()
    {
        return toString;
    }
}
