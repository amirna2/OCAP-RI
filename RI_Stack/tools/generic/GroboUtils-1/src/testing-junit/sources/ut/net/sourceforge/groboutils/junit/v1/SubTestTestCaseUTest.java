/*
 * @(#)SubTestTestCaseUTest.java
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
import junit.framework.TestResult;
import junit.framework.AssertionFailedError;

import java.io.IOException;
import java.lang.reflect.Method;


/**
 * Tests the SubTestTestCase class.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     July 26, 2002
 * @version   $Date: 2002/11/05 01:02:05 $
 */
public class SubTestTestCaseUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = SubTestTestCaseUTest.class;
//    private static final IJUnitDocumentor LOG = (new JUnitLog(THIS_CLASS)).getDocumentor();
    
    public SubTestTestCaseUTest( String name )
    {
        super( name );
    }

    


    //-------------------------------------------------------------------------
    // Tests
    
    
    public static class MyTestCase1 extends TestCase
    {
        public MyTestCase1( String name )
        {
            super( name );
        }
        
        public void testFail()
        {
            fail("this should fail.");
        }
        
        public static Test suite()
        {
            return new TestSuite( MyTestCase1.class );
        }
    }
    
    
    public static class MyTestCase2 extends SubTestTestCase
    {
        public MyTestCase2( String name )
        {
            super( name );
        }
        
        public void testFailTwice()
        {
            addSubTest( MyTestCase1.suite() );
            fail( "this should fail too." );
        }
        
        public static Test suite()
        {
            return new TestSuite( MyTestCase2.class );
        }
    }
    
    
    public static class MyTestCase3 extends SubTestTestCase
    {
        boolean enteredOnce = false;
        long createdTime = System.currentTimeMillis();
        
        public MyTestCase3( String name )
        {
            super( name );
        }
        
        public void testReentryTwice()
        {
            try
            {
                System.out.println("Entering Created Time="+createdTime);
                if (!this.enteredOnce)
                {
                    this.enteredOnce = true;
                    System.out.println("Running again Created Time="+createdTime);
                    run( new TestResult() );
                    System.out.println("Back from Created Time="+createdTime);
                }
                addSubTest( MyTestCase1.suite() );
                System.out.println("Leaving Created Time="+createdTime);
            }
            catch (RuntimeException re)
            {
                re.printStackTrace();
                throw re;
            }
            catch (Error e)
            {
                e.printStackTrace();
                throw e;
            }
        }
        
        public static Test suite()
        {
            return new TestSuite( MyTestCase3.class );
        }
    }
    
    
    public void testSameResult1()
    {
        // ensure the same TestResult object is used for both test instances.
        try
        {
            Test t = MyTestCase2.suite();
            TestResult tr = new TestResult();
            
            t.run( tr );
            
            assertEquals(
                "Should have 2 failures now.",
                2,
                tr.failureCount() );
            assertEquals(
                "Should have no errors now.",
                0,
                tr.errorCount() );
            assertEquals(
                "Should have 2 tests now.",
                2,
                tr.runCount() );
        }
        catch (RuntimeException re)
        {
            re.printStackTrace();
            throw re;
        }
        catch (Error e)
        {
            e.printStackTrace();
            throw e;
        }
    }
    
    
    public void testReentry1()
    {
        try
        {
            // ensure the same TestResult object is used for both test instances.
            
            Test t = MyTestCase3.suite();
            TestResult tr = new TestResult();
            
            // this should:
            //    1. execute the testReentryTwice() method w/ tr
            //    2. execute the testReentryTwice() method w/ a new TestResult
            //    3. add a new fail test
            //    4. return from testReentryTwice() w/ a new TestResult
            //    5. add a new fail test
            //    6. execute 2 fail tests.
            // for a total of 2 fail tests and 1 testReentryTwice() for tr.
            t.run( tr );
            
            assertEquals(
                "Should have 2 failures.",
                2,
                tr.failureCount() );
            assertEquals(
                "Should have no errors now.",
                0,
                tr.errorCount() );
            assertEquals(
                "Should have 3 tests now.",
                3,
                tr.runCount() );
        }
        catch (RuntimeException re)
        {
            re.printStackTrace();
            throw re;
        }
        catch (Error e)
        {
            e.printStackTrace();
            throw e;
        }
    }
    
    
    
    //-------------------------------------------------------------------------
    // Helpers
    
    
    
    //-------------------------------------------------------------------------
    // Standard JUnit declarations
    
    
    public static Test suite()
    {
        try
        {
            TestSuite suite = new TestSuite( THIS_CLASS );
            
            return suite;
        }
        catch (RuntimeException re)
        {
            re.printStackTrace();
            throw re;
        }
        catch (Error e)
        {
            e.printStackTrace();
            throw e;
        }
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

