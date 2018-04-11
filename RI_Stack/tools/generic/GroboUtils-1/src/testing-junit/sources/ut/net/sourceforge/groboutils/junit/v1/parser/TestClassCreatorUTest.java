/*
 * @(#)TestClassCreatorUTest.java
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

package net.sourceforge.groboutils.junit.v1.parser;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.AssertionFailedError;

import java.io.IOException;
import java.lang.reflect.Method;


/**
 * Tests the TestClassCreator class.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     November 4, 2002
 * @version   $Date: 2002/11/05 01:02:05 $
 */
public class TestClassCreatorUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = TestClassCreatorUTest.class;
    
    public TestClassCreatorUTest( String name )
    {
        super( name );
    }

    


    //-------------------------------------------------------------------------
    // Tests
    
    
    public void testConstructor1()
    {
        try
        {
            new TestClassCreator( null );
        }
        catch (IllegalArgumentException e)
        {
            // test exception?
        }
    }
    
    
    public void testConstructor2()
    {
        new TestClassCreator( new JUnitOrigCreator() );
    }
    
    
    public void testWarnings1()
    {
        TestClassCreator tcc = new TestClassCreator( new JUnitOrigCreator() );
        
        String s[] = tcc.getWarnings();
        
        assertNotNull( "Returned null warnings array.",
            s );
        assertEquals( "Returned warnings array with elements.",
            0,
            s.length );
    }
    
    
    public void testWarnings2()
    {
        TestClassCreator tcc = new TestClassCreator( new JUnitOrigCreator() );
        
        tcc.warning( "a" );
        String s[] = tcc.getWarnings();
        
        assertNotNull( "Returned null warnings array.",
            s );
        assertEquals( "Returned warnings array with incorrect element count.",
            1,
            s.length );
        assertEquals( "Did not return warnings array with correct entry.",
            "a",
            s[0] );
    }
    
    
    public void testWarnings3()
    {
        TestClassCreator tcc = new TestClassCreator( new JUnitOrigCreator() );
        
        tcc.warning( "a" );
        tcc.warning( "b" );
        String s[] = tcc.getWarnings();
        
        assertNotNull( "Returned null warnings array.",
            s );
        assertEquals( "Returned warnings array with incorrect element count.",
            2,
            s.length );
        assertEquals( "Did not return warnings array with correct entry.",
            "a",
            s[0] );
        assertEquals( "Did not return warnings array with correct entry.",
            "b",
            s[1] );
    }
    
    
    public void testClearWarnings1()
    {
        TestClassCreator tcc = new TestClassCreator( new JUnitOrigCreator() );
        
        tcc.clearWarnings();
        String s[] = tcc.getWarnings();
        
        assertNotNull( "Returned null warnings array.",
            s );
        assertEquals( "Returned warnings array with elements.",
            0,
            s.length );
    }
    
    
    public void testClearWarnings2()
    {
        TestClassCreator tcc = new TestClassCreator( new JUnitOrigCreator() );
        
        tcc.warning( "a" );
        tcc.clearWarnings();
        String s[] = tcc.getWarnings();
        
        assertNotNull( "Returned null warnings array.",
            s );
        assertEquals( "Returned warnings array with elements.",
            0,
            s.length );
    }
    
    
    public void testClearWarnings3()
    {
        TestClassCreator tcc = new TestClassCreator( new JUnitOrigCreator() );
        
        tcc.warning( "a" );
        tcc.warning( "b" );
        tcc.clearWarnings();
        String s[] = tcc.getWarnings();
        
        assertNotNull( "Returned null warnings array.",
            s );
        assertEquals( "Returned warnings array with elements.",
            0,
            s.length );
    }
    
    
    public void testCreateWarningTests1()
    {
        TestClassCreator tcc = new TestClassCreator( new JUnitOrigCreator() );
        
        tcc.warning( "a" );
        tcc.warning( "b" );
        Test t[] = tcc.createWarningTests( new TestClassParser( THIS_CLASS ) );
        
        assertNotNull( "Returned null warning test list.",
            t );
        assertEquals(
            "Returned warnings test list with incorrect element count.",
            2,
            t.length );
        // need to test failures!!!
        
        
        
        // warnings should not have been cleared
        String s[] = tcc.getWarnings();
        
        assertNotNull( "Returned null warnings array.",
            s );
        assertEquals( "Returned warnings array with incorrect element count.",
            2,
            s.length );
        assertEquals( "Did not return warnings array with correct entry.",
            "a",
            s[0] );
        assertEquals( "Did not return warnings array with correct entry.",
            "b",
            s[1] );
    }
    
    
    
/*    
    
    
    public static class TesterNoTestMethods implements Test
    {
        public int countTestCases() { return 0; }
        public void run( junit.framework.TestResult tr ) {}
    }
    
    
    public static class TesterOneTestMethod implements Test
    {
        public int countTestCases() { return 0; }
        public void run( junit.framework.TestResult tr ) {}
        
        public void testMyTest() {}
    }
    
    
    private class StaticClass {}
    public class InnerClass {}
    
    public void testGetTestMethods1()
    {
        TestClassParser tcp = new TestClassParser( String.class );
        Method m[] = tcp.getTestMethods();
        assertNotNull(
            "Must never return null.",
            m );
        assertEquals(
            "String should have no test methods.",
            0,
            m.length );
        assertTrue(
            "Must never return the same array, but rather a copy.",
            m != tcp.getTestMethods() );
    }
    
    public void testGetTestMethods2()
    {
        TestClassParser tcp = new TestClassParser( Runnable.class );
        Method m[] = tcp.getTestMethods();
        assertNotNull(
            "Must never return null.",
            m );
        assertEquals(
            "Runnable should have no test methods.",
            0,
            m.length );
        assertTrue(
            "Must never return the same array, but rather a copy.",
            m != tcp.getTestMethods() );
    }
    
    public void testGetTestMethods3()
    {
        TestClassParser tcp = new TestClassParser( StaticClass.class );
        Method m[] = tcp.getTestMethods();
        assertNotNull(
            "Must never return null.",
            m );
        assertEquals(
            "Runnable should have no test methods.",
            0,
            m.length );
        assertTrue(
            "Must never return the same array, but rather a copy.",
            m != tcp.getTestMethods() );
    }
    
    public void testGetTestMethods4()
    {
        TestClassParser tcp = new TestClassParser( InnerClass.class );
        Method m[] = tcp.getTestMethods();
        assertNotNull(
            "Must never return null.",
            m );
        assertEquals(
            "Runnable should have no test methods.",
            0,
            m.length );
        assertTrue(
            "Must never return the same array, but rather a copy.",
            m != tcp.getTestMethods() );
    }
    
    public void testGetTestMethods5()
    {
        TestClassParser tcp = new TestClassParser( TesterNoTestMethods.class );
        Method m[] = tcp.getTestMethods();
        assertNotNull(
            "Must never return null.",
            m );
        assertEquals(
            "Runnable should have no test methods.",
            0,
            m.length );
        assertTrue(
            "Must never return the same array, but rather a copy.",
            m != tcp.getTestMethods() );
    }
    
    public void testGetTestMethods6()
    {
        TestClassParser tcp = new TestClassParser( TesterOneTestMethod.class );
        Method m[] = tcp.getTestMethods();
        assertNotNull(
            "Must never return null.",
            m );
        assertEquals(
            "Runnable should have one test method.",
            1,
            m.length );
        assertTrue(
            "Must never return the same array, but rather a copy.",
            m != tcp.getTestMethods() );
    }
    
    
    */
    
    
    
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

