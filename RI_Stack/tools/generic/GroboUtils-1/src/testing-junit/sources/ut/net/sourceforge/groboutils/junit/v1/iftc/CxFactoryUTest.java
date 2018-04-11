/*
 * @(#)CxFactoryUTest.java
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

package net.sourceforge.groboutils.junit.v1.iftc;

import org.easymock.EasyMock;
import org.easymock.MockControl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Tests the CxFactory class.  Concrete test for an abstract class.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     October 30, 2002
 * @version   $Date: 2002/12/09 04:43:24 $
 */
public class CxFactoryUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = CxFactoryUTest.class;
    private static final String TC = "CxFactoryUTest";
    private static final org.apache.log4j.Logger LOG =
        org.apache.log4j.Logger.getLogger( THIS_CLASS );
    
    public CxFactoryUTest( String name )
    {
        super( name );
    }
    
    
    // intentionally non-static inner class
    public class MyCxFactory extends CxFactory
    {
        public MyCxFactory( String n )
        {
            super( n );
        }
        
        public MyCxFactory( String n, boolean a )
        {
            super( n, a );
        }
        
        public Object createImplObject()
        {
            return "";
        }
    }
    
    
    // intentionally static inner class
    public static class MyStaticCxFactory extends CxFactory
    {
        public MyStaticCxFactory( String n )
        {
            super( n );
        }
        
        public MyStaticCxFactory( String n, boolean a )
        {
            super( n, a );
        }
        
        public Object createImplObject()
        {
            return "";
        }
    }
    

    //-------------------------------------------------------------------------
    // Tests
    
    
    
    
    public void testToString1()
    {
        // try with a named inner class.
        CxFactory cf = new MyCxFactory( "1" );
        assertEquals(
            "Returned unexpected factory name.",
            "1",
            cf.toString() );
    }
    
    
    public void testToString1b()
    {
        // try with a named inner class.
        CxFactory cf = new MyCxFactory( "1b", false );
        assertEquals(
            "Returned unexpected factory name.",
            "1b",
            cf.toString() );
    }
    
    
    public void testToString1a()
    {
        // try with a named inner class.
        CxFactory cf = new MyCxFactory( "1a", true );
        assertEquals(
            "Returned unexpected factory name.",
            TC+"-1a",
            cf.toString() );
    }
    
    
    public void testToString2()
    {
        // try with an anonymous inner class.
        // CxFactory should use the name of the owning class's name,
        // not the inner class's name.
        CxFactory cf = new MyCxFactory( "2" ) { };
        assertEquals(
            "Returned unexpected factory name.",
            "2",
            cf.toString() );
    }
    
    
    public void testToString2b()
    {
        // try with an anonymous inner class.
        // CxFactory should use the name of the owning class's name,
        // not the inner class's name.
        CxFactory cf = new MyCxFactory( "2b", false ) { };
        assertEquals(
            "Returned unexpected factory name.",
            "2b",
            cf.toString() );
    }
    
    
    public void testToString2a()
    {
        // try with an anonymous inner class.
        // CxFactory should use the name of the owning class's name,
        // not the inner class's name.
        CxFactory cf = new MyCxFactory( "2a", true ) { };
        assertEquals(
            "Returned unexpected factory name.",
            TC+"-2a",
            cf.toString() );
    }
    
    
    public void testToString3()
    {
        // try with a static inner class.
        // CxFactory should use the name of the owning class's name,
        // not the inner class's name.
        CxFactory cf = new MyStaticCxFactory( "3" ) { };
        assertEquals(
            "Returned unexpected factory name.",
            "3",
            cf.toString() );
    }
    
    
    public void testToString3b()
    {
        // try with a static inner class.
        // CxFactory should use the name of the owning class's name,
        // not the inner class's name.
        CxFactory cf = new MyStaticCxFactory( "3b", false ) { };
        assertEquals(
            "Returned unexpected factory name.",
            "3b",
            cf.toString() );
    }
    
    
    public void testToString3a()
    {
        // try with a static inner class.
        // CxFactory should use the name of the owning class's name,
        // not the inner class's name.
        CxFactory cf = new MyStaticCxFactory( "3a", true ) { };
        assertEquals(
            "Returned unexpected factory name.",
            TC+"-3a",
            cf.toString() );
    }
    
    
    public void testToString4()
    {
        // try with an outside, stand-alone class.
        LOG.debug( "Test4:" );
        CxFactory cf = new CxFactorySample( "4" );
        LOG.debug( "Returned Sample factory toString: ["+cf.toString()+"]" );
        assertEquals(
            "Returned unexpected factory name.",
            "4",
            cf.toString() );
    }
    
    
    public void testToString4b()
    {
        // try with an outside, stand-alone class.
        LOG.debug( "Test4:" );
        CxFactory cf = new CxFactorySample( "4b", false );
        LOG.debug( "Returned Sample factory toString: ["+cf.toString()+"]" );
        assertEquals(
            "Returned unexpected factory name.",
            "4b",
            cf.toString() );
    }
    
    
    public void testToString4a()
    {
        // try with an outside, stand-alone class.
        LOG.debug( "Test4:" );
        CxFactory cf = new CxFactorySample( "4a", true );
        LOG.debug( "Returned Sample factory toString: ["+cf.toString()+"]" );
        assertEquals(
            "Returned unexpected factory name.",
            "CxFactorySample-4a",
            cf.toString() );
    }
    
    
    //-------------------------------------------------------------------------
    // Standard JUnit declarations
    
    
    public static InterfaceTestSuite suite()
    {
        InterfaceTestSuite suite = ImplFactoryUTestI.suite();
        
        // yes, this is an inner class inside an inner class!
        // shudder - luckily, this is only for testing.
        suite.addFactory( new ImplFactory() {
            public Object createImplObject() {
                return new MyStaticCxFactory( "A-B" );
            }
            
            public String toString() {
                return "CxFactory-A";
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

