/*
 * @(#)TestRunnableUTest.java
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

//import net.sourceforge.groboutils.testing.junitlog.v1.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.lang.reflect.Method;


/**
 * Tests the TestRunnable class.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     March 1, 2002
 * @version   $Date: 2002/11/05 01:02:05 $
 */
public class TestRunnableUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = TestRunnableUTest.class;
//    private static final IJUnitDocumentor LOG = (new JUnitLog(THIS_CLASS)).getDocumentor();
    
    public TestRunnableUTest( String name )
    {
        super( name );
    }

    


    //-------------------------------------------------------------------------
    // Tests
    
    private static class MyTestRunnable extends TestRunnable
    {
        Throwable t;
        public MyTestRunnable( Throwable t )
        {
            this.t = t;
        }
        
        public void runTest() throws Throwable
        {
            if (this.t != null)
            {
                throw this.t;
            }
        }
    }
    
    
    public void testDelay1() throws InterruptedException
    {
        // JVMs do not have to delay exactly the amount listed, hence the
        // error.
        long delay = 100L;
        long error = 10L;
        long minDelay = delay - error;
        TestRunnable tr = createTestRunnable( null );
        long start = System.currentTimeMillis();
        tr.delay( delay );
        long end = System.currentTimeMillis();
        assertTrue(
            "Did not delay for long enough (delayed "+(end - start)+
            " ms, should have delayed at least "+delay+" ms).",
            (end - start) >= minDelay );
    }
    
    
    public void testRun1()
    {
        TestRunnable tr = createTestRunnable( null );
        try
        {
            tr.run();
            fail( "Did not throw IllegalStateException." );
        }
        catch (IllegalStateException e)
        {
            // test exception ???
        }
    }
    
    
    public void testRun2() throws Throwable
    {
        TestRunnable tr = createTestRunnable( null );
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(
            new TestRunnable[] { tr } );
        try
        {
            tr.run();
            fail( "Did not throw IllegalStateException." );
        }
        catch (IllegalStateException e)
        {
            // test exception ???
        }
    }
    
    
    public void testRun3() throws Throwable
    {
        TestRunnable tr = createTestRunnable( null );
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(
            new TestRunnable[] { tr } );
        mttr.runTestRunnables( 1000 );
    }
    
    
    public void testRun4()
    {
        Throwable t = new Throwable( "Ignore" );
        TestRunnable tr = createTestRunnable( t );
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(
            new TestRunnable[] { tr } );
        try
        {
            mttr.runTestRunnables( 1000 );
            fail( "Did not throw an exception." );
        }
        catch (Throwable actualT)
        {
            assertEquals(
                "Did not throw the intended exception.",
                actualT,
                t );
        }
    }
    
    
    
    
    
    //-------------------------------------------------------------------------
    // Helpers
    
    
    
    
    protected TestRunnable createTestRunnable( Throwable throwThis )
    {
        return new MyTestRunnable( throwThis );
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

