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

package org.cablelabs.gear.util;

import java.awt.*;
import java.util.Vector;
import java.util.StringTokenizer;

/**
 * <code>TextLines</code> is a utility class used to break text into distinct
 * lines and determine certain properties about that text.
 * 
 * @author Aaron Kamienski
 * @version $Id: TextLines.java,v 1.12 2002/06/03 21:31:06 aaronk Exp $
 */
public class TextLines
{
    /** Cannot instantiate. */
    private TextLines()
    {
    }

    /**
     * Computes the maximum bounds for all lines of text.
     * 
     * @param lines
     *            the array of <code>String</code>s representing lines of text
     * @param metrics
     *            the <code>FontMetrics</code> to use in calculating the bounds
     *            of the given lines
     * @return the maximum bounds for all lines of text using the given
     *         <code>FontMetrics</code>.
     */
    public static Dimension getBounds(String[] lines, FontMetrics metrics)
    {
        return new Dimension(getMaxWidth(lines, metrics), getHeight(lines, metrics));
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
     * Determines the total height of the text using the given
     * <code>FontMetrics</code>.
     * 
     * @param lines
     *            the array of <code>String</code>s representing lines of text
     * @param metrics
     *            the <code>FontMetrics</code> to use in calculating the height
     *            of the given lines
     * @return the total height of the given lines of text
     */
    public static int getHeight(String[] lines, FontMetrics metrics)
    {
        return lines.length * TextRender.getFontHeight(metrics);
    }

    /**
     * Returns an array of <code>String</code>s representing the individual
     * lines of the given single text <code>String</code>. Lines are delimited
     * by end-of-line characters (<code>'\n'</code>).
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
        char last = ' ';

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
     * Returns a new string, based on the previous string, with newline
     * characters inserted as linebreaks so that each line fits within the given
     * width pixels. Note that if no valid linebreaks are found within the given
     * width, then a line may be too long.
     * 
     * @param text
     *            the source text
     * @param width
     *            the desired maximum width in pixels of the resulting text
     * @param fm
     *            the <code>FontMetrics</code> used to calculate string width.
     * 
     * @return a <code>String</code> with newlines inserted where necessary to
     *         fit the text within the given width.
     */
    public static String breakLines(String text, int width, FontMetrics fm)
    {
        StringTokenizer tok = new StringTokenizer(text, " \t-\n\r", true);
        StringBuffer buf = new StringBuffer();
        boolean newLine = true;
        int currWidth = 0;

        while (tok.hasMoreTokens())
        {
            String token = tok.nextToken();
            char c;

            // Trim text off the beginning of a new line
            c = token.charAt(0);
            if (newLine && c != '\n' && Character.isWhitespace(c)) continue;

            // Respect embedded '\n'
            if (c == '\n')
            {
                buf.append("\n");
                currWidth = 0;
                newLine = true;
            }
            else
            {
                int addWidth = fm.stringWidth(token);

                newLine = false;

                // if it fits
                if (currWidth + addWidth <= width)
                {
                    currWidth += addWidth;
                    buf.append(token);
                }
                else
                {
                    // Essentially reset
                    buf.append("\n");

                    // Keep it if it wouldn't be trimmed
                    if (!Character.isWhitespace(c))
                    {
                        currWidth = addWidth;
                        buf.append(token);
                    }
                    else
                        currWidth = 0;
                }
            }
        }
        return buf.toString();
    }

    /**
     * Returns a new <code>String</code> with ellipsis ("...") added so that the
     * entire string will fit in the given width. If the given string already
     * fits in this width, the same string is returned. For example,
     * <code>"Scenes from an Italian Restaurant"</code> may be shortened to
     * <code>"Scenes from an..."</code> (depending upon the given width and font
     * metrics).
     * 
     * @param text
     *            the source text
     * @param width
     *            the desired maximum width in pixels of the resulting text
     * @param fm
     *            the <code>FontMetrics</code> used to calculate string width.
     * @param where
     *            determines where to add the ellipsis characters:
     *            <table border>
     *            <tr>
     *            <th>value</th>
     *            <th>meaning</th>
     *            </tr>
     *            <tr>
     *            <td>{@link TextRender#LEFT}</td>
     *            <td>right edge</td>
     *            </tr>
     *            <tr>
     *            <td>{@link TextRender#CENTER}</td>
     *            <td>both right and left</td>
     *            </tr>
     *            <tr>
     *            <td>{@link TextRender#RIGHT}</td>
     *            <td>left edge</td>
     *            </tr>
     *            <tr>
     *            <td>{@link TextRender#JUSTIFY}</td>
     *            <td>right edge</td>
     *            </tr>
     *            </table>
     * 
     * @return a <code>String</code> with an ellipsis ("...") added if
     *         necessary.
     */
    public static String addEllipsis(String text, int width, FontMetrics fm, int where)
    {
        // only add if necessary
        if (fm.stringWidth(text) > width)
        {
            int eWidth = fm.stringWidth(ellipsis);

            if (where == TextRender.CENTER && eWidth * 2 < width) eWidth *= 2;

            // if ellipsis is too much, just go with it
            if (eWidth >= width)
                text = ellipsis;
            else
            {
                width -= eWidth;

                /*
                 * Use an 'average' width to determine how many chars can fit in
                 * the given width. If it's too big, we'll have to extend the
                 * string. If it's too small, we'll have to shrink the string.
                 * 
                 * charWidth() of 'M', 'm', and 'N', 'n' is too much charWidth()
                 * of 'I', 'i' is too little
                 * 
                 * Compromise with average of "IN".
                 */
                // int nChars = width / fm.charWidth('n');
                int nChars = 2 * width / fm.stringWidth("IN");

                char[] chars = new char[text.length()];
                text.getChars(0, chars.length, chars, 0);

                if (nChars > text.length()) nChars = text.length();

                int index = 0;
                switch (where)
                {
                    case TextRender.CENTER:
                        index = (chars.length - nChars) / 2;
                        break;
                    case TextRender.RIGHT:
                        index = chars.length - nChars;
                        break;
                    default:
                        index = 0;
                }

                // Still too wide?
                if (fm.charsWidth(chars, index, nChars) > width)
                {
                    // Remove chars until it fits
                    do
                    {
                        --nChars;
                        if (where == TextRender.RIGHT)
                            ++index;
                        else if (where == TextRender.CENTER && (nChars & 1) == 0) ++index;
                    }
                    while (nChars != 0 && fm.charsWidth(chars, index, nChars) > width);
                }
                // Might not be as wide as we would like?
                else if (fm.charsWidth(chars, index, nChars) < width)
                {
                    // Add chars until it's too big

                    do
                    {
                        ++nChars;
                        if (where == TextRender.RIGHT)
                            --index;
                        else if (where == TextRender.CENTER && (nChars & 1) != 0) --index;
                    }
                    while (nChars < text.length() && fm.charsWidth(chars, index, nChars) <= width);
                    --nChars;
                }

                if (nChars <= 0 || nChars >= text.length())
                    text = ellipsis;
                else
                {
                    switch (where)
                    {
                        case TextRender.LEFT:
                        case TextRender.JUSTIFY:
                            text = new String(chars, 0, nChars) + ellipsis;
                            break;
                        case TextRender.CENTER:
                            text = ellipsis + new String(chars, index, nChars) + ellipsis;
                            break;
                        case TextRender.RIGHT:
                            text = ellipsis + new String(chars, index, nChars);
                            break;
                    }
                }
            }
        }
        return text;
    }

    private static final String ellipsis = "...";

    /**
     * Determines the number of lines within the given text <code>String</code>.
     * 
     * @return the number of lines in <code>text</code>; will be equal to
     *         <code>getLines(text).length</code>.
     */
    public static int getLineCount(String text)
    {
        StringTokenizer tok = new StringTokenizer(text, "\n\r");

        return tok.countTokens();
    }
}
