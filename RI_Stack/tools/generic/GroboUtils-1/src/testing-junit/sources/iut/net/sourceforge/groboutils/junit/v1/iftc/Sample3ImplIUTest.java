/*
 * @(#)Sample3ImplIUTest.java
 *
 * Original author is Matt Albrecht
 * groboclown@users.sourceforge.net
 * http://groboutils.sourceforge.net
 *
 * This code sample has been submitted to the public domain, to show uses
 * for the Interface testing framework.
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
 * Tests the Sample3Impl class.
 * <P>
 * Formatted for 70-column publication.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @version   $Date: 2002/07/28 22:43:58 $
 * @since     July 20, 2002
 */
public class Sample3ImplIUTest extends TestCase {
    private static final Class THIS_CLASS = Sample3ImplIUTest.class;
    
    public Sample3ImplIUTest( String name ) {
        super( name );
    }

    //-------------------------------------------------------------------------
    // Tests
    
    public void testConstructor1() {
        new Sample3Impl( (String[])null );
    }
    
    
    public static class Sample2ImplFactory
        implements Sample2IUTestI.Sample2Factory {
        public Sample2 create( String[] s ) {
            return new Sample3Impl( s );
        }
        
        public Sample2 create( String s ) {
            return new Sample3Impl( new String[] { s } );
        }
    }
    
    
    public static Test suite() {
        TestSuite suite = new TestSuite( THIS_CLASS );
        
        //------
        // These two InterfaceTestSuites will share the same set of
        // factories
        InterfaceTestSuite its = Sample3IUTestI.suite();
        its.addInterfaceTestSuite( Sample4IUTestI.suite() );
        its.addFactory( new ImplFactory() {
            public Object createImplObject() {
                return new Sample3Impl();
            }
        } );
        its.addFactory( new ImplFactory() {
            public Object createImplObject() {
                return new Sample3Impl( new String[] { "a", "b" } );
            }
        } );
        suite.addTest( its );
        
        //------
        // This InterfaceTestSuite needs a different kind of factory.
        its = Sample2IUTestI.suite();
        its.addFactory( new ImplFactory() {
            public Object createImplObject() {
                return new Sample2ImplFactory();
            }
        } );
        suite.addTest( its );
        
        return suite;
    }
    
    public static void main( String[] args ) {
        String[] name = { THIS_CLASS.getName() };
        
        junit.textui.TestRunner.main( name );
    }
}

