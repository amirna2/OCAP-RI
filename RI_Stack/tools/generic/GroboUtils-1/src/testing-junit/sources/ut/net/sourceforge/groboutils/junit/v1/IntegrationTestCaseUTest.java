/*
 * @(#)IntegrationTestCaseUTest.java
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

import java.io.IOException;
import java.lang.reflect.Method;


/**
 * Tests the IntegrationTestCase class.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     March 1, 2002
 * @version   $Date: 2002/11/05 01:02:05 $
 */
public class IntegrationTestCaseUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = IntegrationTestCaseUTest.class;
    private static final org.apache.log4j.Logger LOG =
        org.apache.log4j.Logger.getLogger( THIS_CLASS );
    
    public IntegrationTestCaseUTest( String name )
    {
        super( name );
    }

    


    //-------------------------------------------------------------------------
    // Tests
    
    
    public void testSoftAssert1()
    {
        IntegrationTestCase itc = createIntegrationTestCase();
        
        // ensure that adding a failing test doesn't cause failures in the
        // owning test.
        itc.softFail();
    }
    
    
    public void testSoftAssert2()
    {
        IntegrationTestCase itc = createIntegrationTestCase();
        
        // ensure that adding a passing test doesn't cause failures in owning
        // test.
        itc.softAssertTrue( true );
    }
    
    
    //---------
    public static class ITC_True1 extends IntegrationTestCase
    {
        public ITC_True1( String name ) { super( name ); }
        public void test1()
        {
            softAssertTrue( true );
        }
    }
    public void testSoftAssertTrue1()
    {
        // Ensure that two tests run, and there is one failure.
        runITC( ITC_True1.class, 2, 0, 0 );
    }
    
    //---------
    public static class ITC_True2 extends IntegrationTestCase
    {
        public ITC_True2( String name ) { super( name ); }
        public void test1()
        {
            softAssertTrue( "a", true );
        }
    }
    public void testSoftAssertTrue2()
    {
        // Ensure that two tests run, and there is one failure.
        runITC( ITC_True2.class, 2, 0, 0 );
    }
    
    //---------
    public static class ITC_True3 extends IntegrationTestCase
    {
        public ITC_True3( String name ) { super( name ); }
        public void test1()
        {
            softAssertTrue( false );
        }
    }
    public void testSoftAssertTrue3()
    {
        // Ensure that two tests run, and there is one failure.
        runITC( ITC_True3.class, 2, 1, 0 );
    }
    
    //---------
    public static class ITC_True4 extends IntegrationTestCase
    {
        public ITC_True4( String name ) { super( name ); }
        public void test1()
        {
            softAssertTrue( "a", false );
        }
    }
    public void testSoftAssertTrue4()
    {
        // Ensure that two tests run, and there is one failure.
        runITC( ITC_True4.class, 2, 1, 0 );
    }
    
    //---------
    public static class ITC_False1 extends IntegrationTestCase
    {
        public ITC_False1( String name ) { super( name ); }
        public void test1()
        {
            softAssertFalse( false );
        }
    }
    public void testSoftAssertFalse1()
    {
        // Ensure that two tests run, and there is one failure.
        runITC( ITC_False1.class, 2, 0, 0 );
    }
    
    //---------
    public static class ITC_False2 extends IntegrationTestCase
    {
        public ITC_False2( String name ) { super( name ); }
        public void test1()
        {
            softAssertFalse( "a", false );
        }
    }
    public void testSoftAssertFalse2()
    {
        // Ensure that two tests run, and there is one failure.
        runITC( ITC_False2.class, 2, 0, 0 );
    }
    
    //---------
    public static class ITC_False3 extends IntegrationTestCase
    {
        public ITC_False3( String name ) { super( name ); }
        public void test1()
        {
            softAssertFalse( true );
        }
    }
    public void testSoftAssertFalse3()
    {
        // Ensure that two tests run, and there is one failure.
        runITC( ITC_False3.class, 2, 1, 0 );
    }
    
    //---------
    public static class ITC_False4 extends IntegrationTestCase
    {
        public ITC_False4( String name ) { super( name ); }
        public void test1()
        {
            softAssertFalse( "a", true );
        }
    }
    public void testSoftAssertFalse4()
    {
        // Ensure that two tests run, and there is one failure.
        runITC( ITC_False4.class, 2, 1, 0 );
    }
    
    //---------
    public static class ITC_Fail1 extends IntegrationTestCase
    {
        public ITC_Fail1( String name ) { super( name ); }
        public void test1()
        {
            softFail();
        }
    }
    public void testSoftFail1()
    {
        // Ensure that two tests run, and there is one failure.
        runITC( ITC_Fail1.class, 2, 1, 0 );
    }
    
    
    //---------
    public static class ITC_Fail2 extends IntegrationTestCase
    {
        public ITC_Fail2( String name ) { super( name ); }
        public void test1()
        {
            softFail( "fail" );
        }
    }
    public void testSoftFail2()
    {
        // Ensure that two tests run, and there is one failure.
        runITC( ITC_Fail2.class, 2, 1, 0 );
    }
    
    
    
    
    //-------------------------------------------------------------------------
    // Helpers
    
    
    
    protected IntegrationTestCase createIntegrationTestCase()
    {
        return new IntegrationTestCase( "name" );
    }
    
    
    protected TestResult runITC( Class itcClass,
            int runCount, int failureCount, int errorCount )
    {
        TestSuite suite = new TestSuite( itcClass );
        TestResult tr = new TestResult();
        LOG.info( "Running tests..." );
        try
        {
            suite.run( tr );
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
        LOG.info( "Expected "+runCount+" runs, but found "+tr.runCount()+"." );
        LOG.info( "Expected "+failureCount+" failures, but found "+
            tr.failureCount()+"." );
        LOG.info( "Expected "+errorCount+" errors, but found "+
            tr.errorCount()+"." );
        //LOG.info( tr.failures().nextElement().toString() );
        assertEquals(
            "Did not have the correct number of failures.",
            failureCount,
            tr.failureCount() );
        assertEquals(
            "Did not have the correct number of errors.",
            errorCount,
            tr.errorCount() );
        assertEquals(
            "Did not have the correct number of test runs.",
            runCount,
            tr.runCount() );
        return tr;
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

