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
 * Tests the Sample1 interface.  These tests are in no way exhaustive, but
 * only shown for example.
 * <P>
 * Formatted for 70-column publication.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     March 1, 2002
 * @version   $Date: 2002/07/28 22:43:58 $
 */
public class Sample1IUTestI extends InterfaceTestCase
{
    private static final Class THIS_CLASS = Sample1IUTestI.class;
    
    public Sample1IUTestI( String name, ImplFactory f ) {
        super( name, Sample1.class, f );
    }

    protected Sample1 createSample1() {
        return (Sample1)createImplObject();
    }

    //---------------------------------------------------------------
    // Tests
    
    public void testAddString1() {
        Sample1 s1 = createSample1();
        try {
            s1.addString( null );
            fail( "Did not throw IllegalArgumentException." );
        } catch (IllegalArgumentException e) {
            // successfully threw an IllegalArgumentException
        }
    }
    
    public static InterfaceTestSuite suite() {
        InterfaceTestSuite suite =
            new InterfaceTestSuite( THIS_CLASS );
        return suite;
    }
}

