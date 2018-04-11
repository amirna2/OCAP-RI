/*
 * @(#)Sample1IUTestI.java
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
 * Tests the Sample3 abstract class.
 * <P>
 * Formatted for 70-column publication.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     July 21, 2002
 * @version   $Date: 2002/07/28 22:43:58 $
 */
public class Sample3IUTestI extends InterfaceTestCase {
    private static final Class THIS_CLASS = Sample3IUTestI.class;
    
    public Sample3IUTestI( String name, ImplFactory f ) {
        super( name, Sample1.class, f );
    }

    protected Sample3 createSample3() {
        return (Sample3)createImplObject();
    }

    //---------------------------------------------------------------
    // Tests
    
    public void testGetAddedStrings1() {
        Sample3 s3 = createSample3();
        s3.addString( "a" );
        java.util.Enumeration enum = s3.getAddedStrings();
        assertNotNull( "Returned null.", enum );
        assertTrue( "Has no added strings.", enum.hasMoreElements() );
        assertEquals( "Not right string.", "a", enum.nextElement() );
        assertTrue( "Has too many added strings.",
            !enum.hasMoreElements() );
    }
    
    
    public static InterfaceTestSuite suite() {
        InterfaceTestSuite suite = Sample1IUTestI.suite();
        suite.addTestSuite( THIS_CLASS );
        
        return suite;
    }
}

