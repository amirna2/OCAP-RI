/*
 * @(#)AssertConstructorUTest.java
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

import org.easymock.EasyMock;
import org.easymock.MockControl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.AssertionFailedError;


/**
 * Tests the AssertConstructor class.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     March 1, 2002
 * @version   $Date: 2002/10/31 05:54:22 $
 */
public class AssertConstructorUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = AssertConstructorUTest.class;
    
    public AssertConstructorUTest( String name )
    {
        super( name );
    }

    


    //-------------------------------------------------------------------------
    // Tests
    
    public void testAssertHasDefaultConstructor1()
    {
        AssertConstructor.assertHasDefaultConstructor( Object.class );
    }
    
    
    public void testAssertHasDefaultConstructor2()
    {
        boolean failed = true;
        try
        {
            AssertConstructor.assertHasDefaultConstructor( Integer.class );
        }
        catch (AssertionFailedError e)
        {
            failed = false;
        }
        if (failed)
        {
            fail( "Did not cause an assertion failure." );
        }
    }
    
    
    public void testAssertHasDefaultConstructor3()
    {
        AssertConstructor.assertHasDefaultConstructor( new Object() );
    }
    
    
    public void testAssertHasDefaultConstructor4()
    {
        boolean failed = true;
        try
        {
            AssertConstructor.assertHasDefaultConstructor( new Integer( 1 ) );
        }
        catch (AssertionFailedError e)
        {
            failed = false;
        }
        if (failed)
        {
            fail( "Did not cause an assertion failure." );
        }
    }
    
    public void testAssertHasDefaultConstructor1a()
    {
        AssertConstructor.assertHasDefaultConstructor( "A", Object.class );
    }
    
    
    public void testAssertHasDefaultConstructor2a()
    {
        boolean failed = true;
        try
        {
            AssertConstructor.assertHasDefaultConstructor( ":A:",
                Integer.class );
        }
        catch (AssertionFailedError e)
        {
            assertTrue(
                "Did not throw an error with the message text.",
                e.getMessage().indexOf( ":A:" ) >= 0 );
            failed = false;
        }
        if (failed)
        {
            fail( "Did not cause an assertion failure." );
        }
    }
    
    
    public void testAssertHasDefaultConstructor3a()
    {
        AssertConstructor.assertHasDefaultConstructor( "A", new Object() );
    }
    
    
    public void testAssertHasDefaultConstructor4a()
    {
        boolean failed = true;
        try
        {
            AssertConstructor.assertHasDefaultConstructor( ":A:",
                new Integer( 1 ) );
        }
        catch (AssertionFailedError e)
        {
            assertTrue(
                "Did not throw an error with the message text.",
                e.getMessage().indexOf( ":A:" ) >= 0 );
            failed = false;
        }
        if (failed)
        {
            fail( "Did not cause an assertion failure." );
        }
    }

    
    
    public void testAssertHasConstructor1()
    {
        AssertConstructor.assertHasConstructor( Object.class, new Class[0],
            AssertConstructor.ANY_PROTECTION );
    }
    
    
    public void testAssertHasConstructor2()
    {
        boolean failed = true;
        try
        {
            AssertConstructor.assertHasConstructor( Object.class,
                new Class[0], AssertConstructor.PRIVATE );
        }
        catch (AssertionFailedError e)
        {
            failed = false;
        }
        if (failed)
        {
            fail( "Did not cause an assertion failure." );
        }
    }


    public void testAssertHasConstructor3()
    {
        AssertConstructor.assertHasConstructor( Integer.class,
            new Class[] { Integer.TYPE },
            AssertConstructor.PUBLIC );
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

