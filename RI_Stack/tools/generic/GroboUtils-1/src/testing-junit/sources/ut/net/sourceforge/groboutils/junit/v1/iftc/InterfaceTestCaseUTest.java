/*
 * @(#)InterfaceTestCaseUTest.java
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
import junit.framework.AssertionFailedError;


/**
 * Tests the InterfaceTestCase class.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     March 1, 2002
 * @version   $Date: 2002/12/09 04:43:24 $
 */
public class InterfaceTestCaseUTest extends TestCase
{
    
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = InterfaceTestCaseUTest.class;
    private static final org.apache.log4j.Logger LOG =
        org.apache.log4j.Logger.getLogger( InterfaceTestCaseUTest.class );
    
    public InterfaceTestCaseUTest( String name )
    {
        super( name );
    }

    


    //-------------------------------------------------------------------------
    // Tests
    
    public static class MyInterfaceTestCase extends InterfaceTestCase
    {
        public MyInterfaceTestCase( String name, Class interfaceClass,
                ImplFactory f )
        {
            super( name, interfaceClass, f );
        }
    }
    
    public void testConstructor1()
    {
        LOG.debug( "Entering testConstructor1" );
        ImplFactory f = new ImplFactory() {
            public Object createImplObject() {
                return new Runnable() { public void run() {} };
            }
        };
        InterfaceTestCase itc = new MyInterfaceTestCase( "test",
            Runnable.class, f );
        assertEquals(
            "Did not store the interface class correctly.",
            Runnable.class,
            itc.getInterfaceClass() );
        LOG.debug( "Leaving testConstructor1" );
    }
    
    
    public void testConstructor2()
    {
        LOG.debug( "Entering testConstructor2" );
        try
        {
            new MyInterfaceTestCase( "test", null, null );
            fail("Did not throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException e)
        {
            // test exception?
        }
        LOG.debug( "Leaving testConstructor2" );
    }
    
    
    public void testConstructor3()
    {
        LOG.debug( "Entering testConstructor3" );
        try
        {
            new MyInterfaceTestCase( "test", Runnable.class, null );
            fail("Did not throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException e)
        {
            // test exception?
        }
        LOG.debug( "Leaving testConstructor3" );
    }
    
    
    public void testConstructor4()
    {
        LOG.debug( "Entering testConstructor4" );
        ImplFactory f = new ImplFactory() {
            public Object createImplObject() {
                return new Runnable() { public void run() {} };
            }
        };
        try
        {
            new MyInterfaceTestCase( "test", null, f );
            fail("Did not throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException e)
        {
            // test exception?
        }
        LOG.debug( "Leaving testConstructor4" );
    }
    
    
    public void testCreate1()
    {
        LOG.debug( "Entering testCreate1" );
        ImplFactory f = new ImplFactory() {
            public Object createImplObject() {
                return "a string";
            }
        };
        InterfaceTestCase itc = new MyInterfaceTestCase( "test",
            Runnable.class, f );
        try
        {
            itc.createImplObject();
            fail("Did not fail.");
        }
        catch (AssertionFailedError e)
        {
            // test error?
        }
        LOG.debug( "Leaving testCreate1" );
    }
    
    
    public void testCreate2()
    {
        LOG.debug( "Entering testCreate2" );
        ImplFactory f = new ImplFactory() {
            public Object createImplObject() {
                return null;
            }
        };
        InterfaceTestCase itc = new MyInterfaceTestCase( "test",
            Runnable.class, f );
        try
        {
            itc.createImplObject();
            fail("Did not fail.");
        }
        catch (AssertionFailedError e)
        {
            // test error?
        }
        LOG.debug( "Leaving testCreate2" );
    }
    
    
    public void testCreate3()
    {
        LOG.debug( "Entering testCreate3" );
        ImplFactory f = new ImplFactory() {
            public Object createImplObject() {
                throw new IllegalArgumentException("IAE");
            }
        };
        InterfaceTestCase itc = new MyInterfaceTestCase( "test",
            Runnable.class, f );
        try
        {
            itc.createImplObject();
            fail("Did not throw exception.");
        }
        catch (AssertionFailedError e)
        {
            // test error
            assertTrue(
                "Does not contain the correct exception text.",
                e.getMessage().indexOf( " threw exception "+
                    "java.lang.IllegalArgumentException: IAE during "+
                    "creation:" ) > 0 );
                
        }
    }
    
    
    final String teststring = "a string";
    public void testCreate4()
    {
        LOG.debug( "Entering testCreate4" );
        ImplFactory f = new ImplFactory() {
            public Object createImplObject() {
                return teststring;
            }
        };
        InterfaceTestCase itc = new MyInterfaceTestCase( "test",
            String.class, f );
        Object o = itc.createImplObject();
        assertSame(
            "Did not return the exact object we thought we returned.",
            teststring,
            o );
    }
    
    
    final static String factoryname = "TestFactoryName";
    public void testToString1()
    {
        LOG.debug( "Entering testToString1" );
        ImplFactory f = new ImplFactory() {
            public Object createImplObject() {
                return "";
            }
            public String toString() {
                return factoryname;
            }
        };
        InterfaceTestCase itc = new MyInterfaceTestCase( "test",
            String.class, f );
        itc.setUseClassInName( false );
        assertEquals(
            "Did not return the correct text.",
            "test["+factoryname+"]("+itc.getClass().getName()+")",
            itc.toString() );
    }
    
    
    public void testGetName1()
    {
        LOG.debug( "Entering testGetName1" );
        ImplFactory f = new ImplFactory() {
            public Object createImplObject() {
                return "";
            }
            public String toString() {
                return factoryname;
            }
        };
        InterfaceTestCase itc = new MyInterfaceTestCase( "test",
            String.class, f );
        itc.setUseClassInName( false );
        assertEquals(
            "Did not return the correct text.",
            "test["+factoryname+"]",
            itc.getName() );
    }
    
    
    public void testGetName2()
    {
        LOG.debug( "Entering testGetName1" );
        ImplFactory f = new ImplFactory() {
            public Object createImplObject() {
                return "";
            }
            public String toString() {
                return factoryname;
            }
        };
        InterfaceTestCase itc = new MyInterfaceTestCase( "test",
            String.class, f );
        itc.setUseClassInName( true );
        assertEquals(
            "Did not return the correct text.",
            "InterfaceTestCaseUTest$MyInterfaceTestCase.test["+factoryname+"]",
            itc.getName() );
    }
    
    
    public void testName1()
    {
        LOG.debug( "Entering testName1" );
        ImplFactory f = new ImplFactory() {
            public Object createImplObject() {
                return "";
            }
            public String toString() {
                return factoryname;
            }
        };
        InterfaceTestCase itc = new MyInterfaceTestCase( "test",
            String.class, f );
        itc.setUseClassInName( false );
        assertEquals(
            "Did not return the correct text.",
            itc.getName(),
            itc.name() );
    }
    
    //---------
    
    private static class MyCxFactory implements ICxFactory
    {
        public int createCount = 0;
        public int tearDownCount = 0;
        
        public Object createImplObject()
        {
            ++this.createCount;
            return new Integer( this.createCount );
        }
        
        public void tearDown( Object o )
        {
            int i = ((Integer)o).intValue();
            assertEquals(
                "Did not tear down in the right order.",
                createCount - tearDownCount,
                i );
            ++tearDownCount;
        }
    }
    
    public void testTearDown1() throws Exception
    {
        LOG.debug( "Entering testTearDown1()" );
        MyCxFactory f = new MyCxFactory();
        InterfaceTestCase itc = new MyInterfaceTestCase( "test",
            Integer.class, f );
        int instantCount = 100;
        for (int i = 0; i < instantCount; ++i)
        {
            itc.createImplObject();
        }
        itc.tearDown();
        assertEquals(
            "Did not tear down all expected instantiated objects.",
            instantCount,
            f.tearDownCount );
    }
    
    //---------
    
    private static class MyCxFactory2 implements ICxFactory
    {
        public Object createImplObject()
        {
            return new Integer( 0 );
        }
        
        public void tearDown( Object o ) throws Exception
        {
            throw new IllegalStateException();
        }
    }
    
    public void testTearDown2() throws Exception
    {
        LOG.debug( "Entering testTearDown2()" );
        MyCxFactory2 f = new MyCxFactory2();
        InterfaceTestCase itc = new MyInterfaceTestCase( "test",
            Integer.class, f );
        int instantCount = 100;
        for (int i = 0; i < instantCount; ++i)
        {
            itc.createImplObject();
        }
        try
        {
            itc.tearDown();
            fail( "tearDown did not propigate any exceptions to the top." );
        }
        catch (AssertionFailedError ex)
        {
            String s = ex.toString();
            int count = -1;
            int pos = 0;
            while (pos >= 0)
            {
                ++count;
                pos = s.indexOf( IllegalStateException.class.getName(),
                    pos+1 );
            }
            assertEquals(
                "Did not catch or report all exceptions.",
                instantCount,
                count );
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

