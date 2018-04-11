/*
 * @(#)MultiThreadedTestRunnerUTest.java
 *
 * Copyright (C) 2002 Matt Albrecht
 * groboclown@users.sourceforge.net
 * http://groboutils.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.sourceforge.groboutils.junit.v1;

import net.sourceforge.groboutils.autodoc.v1.AutoDoc;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;


/**
 * Tests the MultiThreadedTestRunner class.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     March 1, 2002
 * @version   $Date: 2002/11/05 01:02:05 $
 */
public class MultiThreadedTestRunnerUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = MultiThreadedTestRunnerUTest.class;
    private static final AutoDoc DOC = new AutoDoc( THIS_CLASS );
    
    public MultiThreadedTestRunnerUTest( String name )
    {
        super( name );
    }

    


    //-------------------------------------------------------------------------
    // Tests
    
    
    public void testConstructor1()
    {
        try
        {
            new MultiThreadedTestRunner( null );
            fail("did not throw an IllegalArgumentException.");
        }
        catch (IllegalArgumentException e)
        {
            // check exception text?
        }
    }
    
    
    public void testConstructor2()
    {
        try
        {
            new MultiThreadedTestRunner( new TestRunnable[0] );
            fail("did not throw an IllegalArgumentException.");
        }
        catch (IllegalArgumentException e)
        {
            // check exception text?
        }
    }
    
    int runCount = 0;
    
    public synchronized void testRun1()
            throws Throwable
    {
        DOC.getIT().testsIssue( 526710 );
        
        runCount = 0;
        TestRunnable tr1 = new TestRunnable() {
                public void runTest() throws Throwable
                {
                    DOC.getLog().debug("Running test testRun1");
                    ++runCount;
                }
        };
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(
            new TestRunnable[] { tr1 } );
        mttr.runTestRunnables();
        
        assertEquals(
            "Did not run the runTest method enough times.",
            1,
            runCount );
    }
    
    
    public synchronized void testRun2()
            throws Throwable
    {
        DOC.getIT().testsIssue( 526710 );
        
        runCount = 0;
        TestRunnable tr1 = new TestRunnable() {
                public void runTest() throws Throwable
                {
                    ++runCount;
                }
        };
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(
            new TestRunnable[] { tr1 } );
        mttr.runTestRunnables( 100 );
        
        assertEquals(
            "Did not run the runTest method enough times.",
            1,
            runCount );
    }
    
    
    public synchronized void testRun3a()
            throws Throwable
    {
        DOC.getIT().testsIssue( 526710 );
        DOC.getLog().warn(
        "****************\n"+
        "This test may expose timing issues, where only one test is run.\n"+
        "I've only seen this with JDK 1.1, but that may expose an underlying\n"+
        "problem.  This will require further investigation.\n"+
        "****************" );
        
        runCount = 0;
        TestRunnable tr1 = new TestRunnable() {
                public synchronized void runTest() throws Throwable
                {
                    ++runCount;
                }
        };
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(
            new TestRunnable[] { tr1, tr1 } );
        mttr.runTestRunnables();
        
        assertEquals(
            "Did not run the runTest method enough times.",
            2,
            runCount );
    }
    
    
    public synchronized void testRun3b()
            throws Throwable
    {
        runCount = 0;
        TestRunnable tr1 = new TestRunnable() {
                public void runTest() throws Throwable
                {
                    ++runCount;
                }
        };
        int totalCount = 15;
        TestRunnable trList[] = new TestRunnable[ totalCount ];
        for (int i = 0; i < totalCount; ++i)
        {
            trList[ i ] = tr1;
        }
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner( trList );
        mttr.runTestRunnables();
        
        assertEquals(
            "Did not run the runTest method enough times.",
            totalCount,
            runCount );
    }
    
    
    public void testRun4()
            throws Throwable
    {
        TestRunnable tr1 = new TestRunnable() {
                public void runTest() throws Throwable
                {
                    throw new IOException("My exception");
                }
        };
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(
            new TestRunnable[] { tr1 } );
        try
        {
            mttr.runTestRunnables();
            fail("did not throw IOException.");
        }
        catch (IOException ioe)
        {
            // check message
            assertEquals(
                "IOException not the one we threw.",
                "My exception",
                ioe.getMessage() );
        }
    }
    
    
    public void testRun5()
            throws Throwable
    {
        TestRunnable tr1 = new TestRunnable() {
                public void runTest() throws Throwable
                {
                    Object o = new Object();
                    synchronized( o )
                    {
                        o.wait();
                    }
                }
        };
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(
            new TestRunnable[] { tr1 } );
        try
        {
            mttr.runTestRunnables( 10 );
            fail("Did not throw an assertion failed error.");
        }
        catch (junit.framework.AssertionFailedError e)
        {
            // test failure?
        }
    }
    
    
    //-------------------------------------------------------------------------
    // Standard JUnit declarations
    
    
    public static Test suite()
    {
        TestSuite suite = new TestSuite( THIS_CLASS );
        
        return suite;
    }
    
    public static void main( String[] args )
    {
        String[] name = { THIS_CLASS.getName() };
        
        // junit.textui.TestRunner.main( name );
        // junit.swingui.TestRunner.main( name );
        
        junit.textui.TestRunner.main( name );
    }
    
    
    /**
     * 
     * @exception Exception thrown under any exceptional condition.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        // set ourself up
    }
    
    
    /**
     * 
     * @exception Exception thrown under any exceptional condition.
     */
    protected void tearDown() throws Exception
    {
        // tear ourself down
        
        
        super.tearDown();
    }
}

