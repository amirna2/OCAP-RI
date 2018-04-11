/*
 * @(#)Sample4IUTestI.java
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
 * Tests the Sample4 interface.
 * <P>
 * Formatted for 70-column publication.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     July 21, 2002
 * @version   $Date: 2002/07/28 22:43:58 $
 */
public class Sample4IUTestI extends InterfaceTestCase {
    private static final Class THIS_CLASS = Sample4IUTestI.class;
    
    public Sample4IUTestI( String name, ImplFactory f ) {
        super( name, Sample4.class, f );
    }

    protected Sample4 createSample4() {
        return (Sample4)createImplObject();
    }

    //---------------------------------------------------------------
    // Tests
    
    public void testProcessString1() {
        Sample4 s4 = createSample4();
        s4.processStrings();
    }
    
    public static InterfaceTestSuite suite() {
        InterfaceTestSuite suite =
            new InterfaceTestSuite( THIS_CLASS );
        return suite;
    }
}

