/*
 * @(#)Sample3.java
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


/**
 * Sample abstract class.
 * <P>
 * Formatted for 70-column publication.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     July 21, 2002
 * @version   $Date: 2002/07/28 22:43:58 $
 */
public abstract class Sample3 implements Sample1 {
    private java.util.Vector addedStrings = new java.util.Vector();
    
    protected Sample3() {}
    
    public void addString( String s ) {
        if (s == null) {
            throw new IllegalArgumentException("no null arguments");
        }
        this.addedStrings.addElement( s );
    }
    
    
    public java.util.Enumeration getAddedStrings() {
        return this.addedStrings.elements();
    }
}

