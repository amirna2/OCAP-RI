/*
 * @(#)Sample2IUTestI.java
 *
 * Original author is Matt Albrecht
 * groboclown@users.sourceforge.net
 * http://groboutils.sourceforge.net
 *
 * This code sample has been submitted to the public domain, to show
 * uses for the Interface testing framework.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package net.sourceforge.groboutils.junit.v1.iftc;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Tests the Sample2 interface.
 * <P>
 * Formatted for 70-column publication.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     March 1, 2002
 * @version   $Date: 2002/07/28 22:43:58 $
 */
public class Sample2IUTestI extends InterfaceTestCase {
    private static final Class THIS_CLASS = Sample2IUTestI.class;
    
    public static interface Sample2Factory {
        public Sample2 create( String[] s );
        public Sample2 create( String s );
    }
    
    public Sample2IUTestI( String name, ImplFactory f ) {
        super( name, Sample2Factory.class, f );
    }

    protected Sample2Factory createSample2Factory() {
        return (Sample2Factory)createImplObject();
    }

    
    protected Sample2 createSample2( String[] s ) {
        Sample2 s2 = createSample2Factory().create( s );
        assertNotNull( "factory returned null.", s2 );
        return s2;
    }

    protected Sample2 createSample2( String s ) {
        Sample2 s2 = createSample2Factory().create( s );
        assertNotNull( "factory returned null.", s2 );
        return s2;
    }

    //---------------------------------------------------------------
    // Tests
    
    public void testConstructor1() throws Exception {
        Class c = createSample2( "a" ).getClass();
        java.lang.reflect.Constructor cntr = c.getConstructor(
            new Class[] { String[].class } );
        assertNotNull( "Does not contain valid constructor.", cntr );
    }
    
    
    public void testGetStrings1() throws Exception {
        Sample2 s2 = createSample2( "a" );
        String s[] = s2.getStrings();
        assertNotNull( "Null string array.", s );
        assertEquals( "Incorrect array length.", 1, s.length );
        assertEquals( "Returned element incorrect.", "a", s[0] );
    }
    
    
    public void testGetStrings2() throws Exception {
        Sample2 s2 = createSample2( new String[] { "a", "b" } );
        String s[] = s2.getStrings();
        assertNotNull( "Null string array.", s );
        assertEquals( "Incorrect array length.", 2, s.length );
        assertEquals( "Returned element incorrect.", "a", s[0] );
        assertEquals( "Returned element incorrect.", "b", s[1] );
    }
    
    public static InterfaceTestSuite suite() {
        InterfaceTestSuite suite =
            new InterfaceTestSuite( THIS_CLASS );
        return suite;
    }
}

