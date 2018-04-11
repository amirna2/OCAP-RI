/*
 * @(#)JUnitTestCaseEUTest.java
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.lang.reflect.Method;


/**
 * Tests the functionality of the JUnit TestCase class for conformance to
 * expected behaviors.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     March 1, 2002
 * @version   $Date: 2002/12/10 23:38:47 $
 */
public class JUnitTestCaseEUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = JUnitTestCaseEUTest.class;
    
    public JUnitTestCaseEUTest( String name )
    {
        super( name );
    }

    


    //-------------------------------------------------------------------------
    // Tests
    
    
    public static class MyTestCase1 extends TestCase
    {
        String myName;
        public MyTestCase1()
        {
            // the name of our only test method.
            super( "test1" );
        }
        public void test1()
        {
            // do nothing
        }
        public String getName()
        {
            return myName;
        }
        public String name()
        {
            return getName();
        }
        
        public void runTest() throws Throwable
        {
            super.runTest();
        }
    }
    
    
    /**
     * There are some dependencies on TestCase using the internal
     * "fName" field, as opposed to calling "getName()".  You see,
     * Ant's JUnit reporter task calls "getName()" to make a fancy
     * printable name, whereas TestCase uses "fName" to reference which
     * method to call.
     */
    public void testRunTestName1() throws Throwable
    {
        MyTestCase1 tc = new MyTestCase1();
        tc.myName = "Just Another Test";
        
        // make sure it still works with an overridden getName() method.
        tc.runTest();
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

