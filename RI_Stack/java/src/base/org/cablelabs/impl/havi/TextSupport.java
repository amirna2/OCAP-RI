// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END

package org.cablelabs.impl.havi;

import java.awt.FontMetrics;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * A utility class useful in manipulating text.
 * 
 * @author Aaron Kamienski
 * @version $Id: TextSupport.java,v 1.3 2002/11/07 21:13:41 aaronk Exp $
 */
public class TextSupport
{
    /**
     * Returns an array of <code>String</code>s representing the individual
     * lines of the given single text <code>String</code>. Lines are delimited
     * by end-of-line characters; i.e., <code>'\n'</code> or <code>'\r'</code>.
     * <p>
     * 
     * @param text
     *            the <code>String</code> to split into separate lines
     * @return <code>String[]</code> containing <code>text</code> split into
     *         individual lines. Will always return an array of size >= 1. If no
     *         end-of-line characters are found in <code>text</code>, then the
     *         array will be of size 1.
     */
    public static String[] getLines(String text)
    {
        StringTokenizer tok = new StringTokenizer(text, "\n", true);
        Vector lines = new Vector();

        while (tok.hasMoreTokens())
        {
            String token = tok.nextToken();

            char c = token.charAt(0);
            if (c == '\n') // empty line
                lines.addElement("");
            else
            // full line, w/ or w/out '\n'
            {
                lines.addElement(token);
                if (tok.hasMoreTokens())
                {
                    // It is a newline -- simply toss it
                    tok.nextToken();
                }
                // else break; // no newline following a line!
            }
        }

        // Only available under 1.2
        // return (String[])lines.toArray(new String[] {});
        String array[] = new String[lines.size()];
        lines.copyInto(array);
        return array;
    }

    /**
     * Computes the maximum width for all lines of text.
     * 
     * @param lines
     *            the array of <code>String</code>s representing lines of text
     * @param metrics
     *            the <code>FontMetrics</code> to use in calculating the width
     *            of the given lines
     * @return the maximum width in pixels for all lines of text using the given
     *         <code>FontMetrics</code>.
     */
    public static int getMaxWidth(String[] lines, FontMetrics metrics)
    {
        int width = 0;

        for (int i = 0; i < lines.length; ++i)
            width = Math.max(width, metrics.stringWidth(lines[i]));
        return width;
    }

    /**
     * Computes the minimum width for all lines of text.
     * 
     * @param lines
     *            the array of <code>String</code>s representing lines of text
     * @param metrics
     *            the <code>FontMetrics</code> to use in calculating the width
     *            of the given lines
     * @return the minimum width in pixels for all lines of text using the given
     *         <code>FontMetrics</code>.
     */
    public static int getMinWidth(String[] lines, FontMetrics metrics)
    {
        int width = 0;

        if (lines.length > 0)
        {
            width = metrics.stringWidth(lines[0]);

            for (int i = 1; i < lines.length; ++i)
                width = Math.min(width, metrics.stringWidth(lines[i]));
        }
        return width;
    }

    /**
     * Figures the ascent of the given font based on its font metrics.
     * Apparently getAscent() does not return the right value for Ascent.
     * 
     * <ul>
     * <li>The O2 LWC use the 0.7 of height calculation. However, this isn't
     * right (in particular for Courier).
     * <li>getHeight() should be leading+ascent+descent, however finding ascent
     * this way doesn't give correct results either.
     * <li>getAscent() - getDescent() appears to work (under Windows).
     * </ul>
     * 
     * @see "Java Bug Parade: 4035331"
     * 
     * @param fm
     *            the font metrics to use when calculating font ascent
     * @return the calculated ascent for the font specified by the given font
     *         metrics.
     */
    public static int getFontAscent(FontMetrics fm)
    {
        if (true) // Believe ascent value
            return fm.getAscent();
        else if (false) // Assume incorrect - use 02 LWC value
            return (fm.getHeight() * 7) / 10 - 1;
        else if (false) // Assume incorrect - works in general on Windows
            return fm.getAscent() - fm.getDescent();
        else
            // Assume incorrect - calculate from other values
            return fm.getHeight() - (fm.getLeading() + fm.getDescent());
    }

    /**
     * Calculates the height of the given font given its font metrics.
     * 
     * @param fm
     *            the font metrics to use when calculating font total height.
     * @return the total height for the font specified by the given font
     *         metrics.
     * 
     * @see #getFontAscent(FontMetrics)
     */
    public static int getFontHeight(FontMetrics fm)
    {
        return fm.getLeading() + getFontAscent(fm) + fm.getDescent();
    }
}
