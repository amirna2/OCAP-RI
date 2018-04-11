/*
 * @(#)JUnitTestSuiteEUTest.java
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
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.lang.reflect.Method;


/**
 * Tests the functionality of the JUnit TestSuite class for conformance to
 * expected behaviors.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     October 31, 2002
 * @version   $Date: 2002/11/05 02:56:12 $
 */
public class InnerClassNameEUTest extends TestCase
{
    //-------------------------------------------------------------------------
    // Standard JUnit Class-specific declarations
    
    private static final Class THIS_CLASS = InnerClassNameEUTest.class;
    
    public InnerClassNameEUTest( String name )
    {
        super( name );
    }

    
    static boolean IS_JDK_12_COMPAT = true;
    static {
        try
        {
            Class.forName("java.lang.ThreadLocal");
        }
        catch (ThreadDeath td)
        {
            throw td;
        }
        catch (Throwable t)
        {
            IS_JDK_12_COMPAT = false;
        }
    }


    //-------------------------------------------------------------------------
    // Tests
    
    /*
     * These tests show off very interesting behavior:
     *      - JDK 1.1 always returns NULL for the "getDeclaringClass()"
     *        call.
     *      - JDK 1.2-1.4 correctly report the declared class for all classes
     *        except anonymous inner classes.
     */
    
    
    
    public void testGetDeclaringClass1()
    {
        Class owner = THIS_CLASS.getDeclaringClass();
        assertNull(
            "Test class has a declaring class.",
            owner );
    }
    
    
    private class MyClass1 {}
    
    public void testGetDeclaringClass2()
    {
        Class c = MyClass1.class;
        Class owner = c.getDeclaringClass();
        if (IS_JDK_12_COMPAT)
        {
            assertNotNull(
                "Inner class has no declaring class.",
                owner );
            assertEquals(
                "Did not return expected owning class.",
                THIS_CLASS,
                owner
                );
        }
        else
        {
            assertNull(
                "Inner class's owner is not null.",
                owner );
        }
    }
    
    
    private static class MyClass2 {}
    
    public void testGetDeclaringClass3()
    {
        Class c = MyClass2.class;
        Class owner = c.getDeclaringClass();
        if (IS_JDK_12_COMPAT)
        {
            assertNotNull(
                "Static inner class has no declaring class.",
                owner );
            assertEquals(
                "Did not return expected owning class.",
                THIS_CLASS,
                owner
                );
        }
        else
        {
            assertNull(
                "Static inner class' owner is not null.",
                owner );
        }
    }
    
    
    public void testGetDeclaringClass4()
    {
        /* The following test works on JDK 1.1.8 and JDK 1.4.0, but fails for
           JDK 1.2.2 (all on Windows).  It's all so confusing!!!
        Object o = new MyClass2() {};
        Class c = o.getClass();
        Class owner = c.getDeclaringClass();
        
        // Not really what you expect, is it?
        assertNull(
            "Anonymous inner class has a declaring class.",
            owner );
        */
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

