/*
 * @(#)Sample3Impl.java
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

/**
 * Sample implementation.  Must provide a constructor that takes a
 * string array as the only argument.
 * <P>
 * Formatted for 70-column publication.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @since     July 21, 2002
 * @version   $Date: 2002/07/28 22:43:58 $
 */
public class Sample3Impl extends Sample3 implements Sample2, Sample4 {
    private String[] defaultStrings;

    public Sample3Impl() {}
    public Sample3Impl( String s[] ) {
        this.defaultStrings = s;
    }
    
    public String[] getStrings() {
        return this.defaultStrings;
    }
    
    public void processStrings() {
        // do nothing
    }
}

