/*
 * @(#)InterfaceTestSuiteUTest.java
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

//import net.sourceforge.groboutils.testing.junitlog.v1.*;
import org.easymock.EasyMock;
import org.easymock.MockControl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.util.Enumeration;


/**
 * Tests the InterfaceTestSuite class.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     March 1, 2002
 * @version   $Date: 2002/11/05 00:49:31 $
 */
public class InterfaceTestSuiteUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = InterfaceTestSuiteUTest.class;
//    private static final IJUnitDocumentor LOG = (new JUnitLog(THIS_CLASS)).getDocumentor();
    
    public InterfaceTestSuiteUTest( String name )
    {
        super( name );
    }

    


    //-------------------------------------------------------------------------
    // Tests
    
    
    public void testConstructor1()
    {
        new InterfaceTestSuite();
    }
    
    
    public void testConstructor2()
    {
        try
        {
            new InterfaceTestSuite( (Class)null );
            fail("Did not throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException iae)
        {
            // test exception?
        }
    }
    
    
    public void testConstructor3()
    {
        new InterfaceTestSuite( this.getClass() );
    }
    
    
    public void testConstructor4()
    {
        try
        {
            new InterfaceTestSuite( (Class)null, (ImplFactory)null );
            fail("Did not throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException iae)
        {
            // test exception?
        }
    }
    
    
    public class ObjectFactory implements ImplFactory
    {
        public Object createImplObject()
        {
            return new Object();
        }
    }
    
    
    public class NullFactory implements ImplFactory
    {
        public Object createImplObject()
        {
            return null;
        }
    }
    
    
    public class IAEFactory implements ImplFactory
    {
        public Object createImplObject()
        {
            return new IllegalArgumentException();
        }
    }
    
    
    
    public void testConstructor5()
    {
        try
        {
            new InterfaceTestSuite( (Class)null, new ObjectFactory() );
            fail("Did not throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException iae)
        {
            // test exception?
        }
    }
    
    
    public void testConstructor6()
    {
        try
        {
            new InterfaceTestSuite( this.getClass(), null );
            fail("Did not throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException iae)
        {
            // test exception?
        }
    }
    
    
    public void testConstructor7()
    {
        new InterfaceTestSuite( this.getClass(), new ObjectFactory() );
    }
    
    
    public void testAddFactory1()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        try
        {
            its.addFactory( null );
            fail("Did not throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException iae)
        {
            // test exception?
        }
    }
    
    
    public void testAddFactory2()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        its.addFactory( new ObjectFactory() );
        assertEquals(
            "Not right number of factories.",
            1,
            its.creators.size() );
    }
    
    
    public void testAddFactories1()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        try
        {
            its.addFactories( null );
            fail("Did not throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException iae)
        {
            // test exception?
        }
    }
    
    
    public void testAddFactories2()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        try
        {
            its.addFactories( new ImplFactory[1] );
            fail("Did not throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException iae)
        {
            // test exception?
        }
    }
    
    
    public void testAddFactories3()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        its.addFactories( new ImplFactory[0] );
        assertEquals(
            "Not right number of factories.",
            0,
            its.creators.size() );
    }
    
    
    public void testAddFactories4()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        its.addFactories( new ImplFactory[] { new ObjectFactory(),
            new ObjectFactory() } );
        assertEquals(
            "Not right number of factories.",
            2,
            its.creators.size() );
    }
    
    
    public void testAddTests1()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        try
        {
            its.addTests( null );
            fail("Did not throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException iae)
        {
            // test exception?
        }
    }
    
    
    public void testAddTests2()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        its.addTests( new Test[0] );
    }
    
    
    public void testAddTests3()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        its.addTests( new Test[1] );
    }
    
    
    public void testAddTests4()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        its.addTests( new Test[] { new TestCase( "" ) {},
            new TestCase( "" ) {} } );
    }
    
    
    public void testAddTestSuite1()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        its.addTestSuite( getClass() );
    }
    
    
    public void testTestAt1()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        its.addTestSuite( getClass() );
        its.testAt( 0 );
    }
    
    
    public void testTestCount1()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        its.testCount();
    }
    
    
    public void testTests1()
    {
        InterfaceTestSuite its = new InterfaceTestSuite();
        its.tests();
    }
    
    
    
    public static interface Intfc1
    {}
    
    public static interface Intfc2 extends Intfc1
    {}
    
    public static class Infc1TestCase extends InterfaceTestCase
    {
        public Infc1TestCase( String name, ImplFactory f )
        {
            super( name, Intfc1.class, f );
        }
        
        public void test1()
        {}
        
        public static InterfaceTestSuite suite()
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(
                Infc1TestCase.class );
            return suite;
        }
    }
    
    public static class Infc2TestCase extends InterfaceTestCase
    {
        public Infc2TestCase( String name, ImplFactory f )
        {
            super( name, Intfc1.class, f );
        }
        
        public void test2()
        {}
        
        public static InterfaceTestSuite suite()
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(
                Infc2TestCase.class );
            suite.addInterfaceTestSuite( Infc1TestCase.suite() );
            return suite;
        }
    }
    
    
    public void testAddInnerITS()
    {
        InterfaceTestSuite its = Infc2TestCase.suite();
        
        Enumeration enum = its.tests();
        assertNotNull(
            "Must not return null",
            enum );
        assertTrue(
            "Must have at least 1 test.",
            enum.hasMoreElements() );
        assertNotNull(
            "Must have non-null first test.",
            enum.nextElement() );
        assertTrue(
            "Must have 2 tests.",
            enum.hasMoreElements() );
        assertNotNull(
            "Must have non-null second test.",
            enum.nextElement() );
        assertTrue(
            "Must have exactly 2 tests.",
            !enum.hasMoreElements() );
    }
    
    
    
    public static class JUnit3_8TestCase extends TestCase
    {
        public void test1() {}
    }
    
    
    public void testJUnit3_8Compat()
    {
        InterfaceTestSuite its = new InterfaceTestSuite(
            JUnit3_8TestCase.class );
        
        Enumeration enum = its.tests();
        assertNotNull(
            "Must not return null",
            enum );
        assertTrue(
            "Must have at least 1 test.",
            enum.hasMoreElements() );
        assertNotNull(
            "Must have non-null first test.",
            enum.nextElement() );
        assertTrue(
            "Must have exactly 1 test.",
            !enum.hasMoreElements() );
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

