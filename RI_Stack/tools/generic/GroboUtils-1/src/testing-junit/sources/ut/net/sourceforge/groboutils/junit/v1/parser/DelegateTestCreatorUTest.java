/*
 * @(#)DelegateTestCreatorUTest.java
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

import net.sourceforge.groboutils.junit.v1.iftc.*;
import java.lang.reflect.*;

import org.easymock.EasyMock;
import org.easymock.MockControl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Tests the DelegateTestCreator class.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     November 4, 2002
 * @version   $Date: 2002/11/05 00:53:00 $
 */
public class DelegateTestCreatorUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = DelegateTestCreatorUTest.class;
    private static final org.apache.log4j.Logger LOG =
        org.apache.log4j.Logger.getLogger( THIS_CLASS );
    
    public DelegateTestCreatorUTest( String name )
    {
        super( name );
    }
    
    
    // mock object!
    private static class MyTestCreator implements ITestCreator
    {
        int canCreateCount = 0;
        int createTestCount = 0;
        boolean canCreate = false;
        Test test = null;
        
        public Test createTest( Class theClass, Method method )
        {
            ++this.createTestCount;
            return this.test;
        }
    
        public boolean canCreate( Class theClass )
        {
            ++this.canCreateCount;
            return this.canCreate;
        }
    }
    
    
    //-------------------------------------------------------------------------
    // Tests
    
    
    public void testConstructor1()
    {
        try
        {
            new DelegateTestCreator( null );
            fail( "Did not throw IllegalArgumentException." );
        }
        catch (IllegalArgumentException e)
        {
            // test exception?
        }
    }
    
    
    public void testConstructor2()
    {
        try
        {
            new DelegateTestCreator( new ITestCreator[0] );
            fail( "Did not throw IllegalArgumentException." );
        }
        catch (IllegalArgumentException e)
        {
            // test exception?
        }
    }
    
    
    public void testCanCreate1()
    {
        MyTestCreator tc1 = new MyTestCreator();
        MyTestCreator tc2 = new MyTestCreator();
        DelegateTestCreator dtc = new DelegateTestCreator(
            new ITestCreator[] { tc1, tc2 } );
        
        boolean res = dtc.canCreate( null );
        
        assertTrue(
            "Did not return correct result.",
            !res );
        assertEquals(
            "Did not call canCreate correct number of times for first instance.",
            1,
            tc1.canCreateCount );
        assertEquals(
            "Did not call canCreate correct number of times for second instance.",
            1,
            tc2.canCreateCount );
    }
    
    
    public void testCanCreate2()
    {
        MyTestCreator tc1 = new MyTestCreator();
        MyTestCreator tc2 = new MyTestCreator();
        tc1.canCreate = true;
        DelegateTestCreator dtc = new DelegateTestCreator(
            new ITestCreator[] { tc1, tc2 } );
        
        boolean res = dtc.canCreate( null );
        
        assertTrue(
            "Did not return correct result.",
            res );
        
        // order of checks shouldn't be dictated here.
        assertEquals(
            "Did not call canCreate correct number of times for first instance.",
            1,
            tc1.canCreateCount );
        assertEquals(
            "Did not call canCreate correct number of times for second instance.",
            1,
            tc2.canCreateCount );
    }
    
    
    public void testCanCreate3()
    {
        MyTestCreator tc1 = new MyTestCreator();
        MyTestCreator tc2 = new MyTestCreator();
        tc2.canCreate = true;
        DelegateTestCreator dtc = new DelegateTestCreator(
            new ITestCreator[] { tc1, tc2 } );
        
        boolean res = dtc.canCreate( null );
        
        assertTrue(
            "Did not return correct result.",
            res );
        
        // order of checks shouldn't be dictated here.
        assertEquals(
            "Did not call canCreate correct number of times for first instance.",
            0,
            tc1.canCreateCount );
        assertEquals(
            "Did not call canCreate correct number of times for second instance.",
            1,
            tc2.canCreateCount );
    }
    
    
    public void testCanCreate4()
    {
        MyTestCreator tc1 = new MyTestCreator();
        MyTestCreator tc2 = new MyTestCreator();
        tc1.canCreate = true;
        tc2.canCreate = true;
        DelegateTestCreator dtc = new DelegateTestCreator(
            new ITestCreator[] { tc1, tc2 } );
        
        boolean res = dtc.canCreate( null );
        
        assertTrue(
            "Did not return correct result.",
            res );
        
        // order of checks shouldn't be dictated here.
        assertEquals(
            "Did not call canCreate correct number of times for first instance.",
            0,
            tc1.canCreateCount );
        assertEquals(
            "Did not call canCreate correct number of times for second instance.",
            1,
            tc2.canCreateCount );
    }
    
    
    public void testCanCreate5()
    {
        MyTestCreator tc1 = new MyTestCreator();
        DelegateTestCreator dtc = new DelegateTestCreator(
            new ITestCreator[] { tc1 } );
        
        boolean res = dtc.canCreate( null );
        
        assertTrue(
            "Did not return correct result.",
            !res );
        assertEquals(
            "Did not call canCreate correct number of times for first instance.",
            1,
            tc1.canCreateCount );
    }
    
    
    public void testCanCreate6()
    {
        MyTestCreator tc1 = new MyTestCreator();
        tc1.canCreate = true;
        DelegateTestCreator dtc = new DelegateTestCreator(
            new ITestCreator[] { tc1 } );
        
        boolean res = dtc.canCreate( null );
        
        assertTrue(
            "Did not return correct result.",
            res );
        assertEquals(
            "Did not call canCreate correct number of times for first instance.",
            1,
            tc1.canCreateCount );
    }
    
    
    
    //-------------------------------------------------------------------------
    // Standard JUnit declarations
    
    
    public static Test suite()
    {
        InterfaceTestSuite suite = ITestCreatorUTestI.suite();
        
        // yes, this is an inner class inside an inner class!
        // shudder - luckily, this is only for testing.
        suite.addFactory( new CxFactory( "A" ) {
            public Object createImplObject() {
                return new DelegateTestCreator( new ITestCreator[] {
                        new MyTestCreator(), new MyTestCreator()
                    } );
            }
        } );
        suite.addTestSuite( THIS_CLASS );
        
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

