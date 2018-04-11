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

/**
 * Utility class used to draw text.
 * 
 * @author Aaron Kamienski
 * @version $Id: TextRender.java,v 1.8 2002/06/03 21:31:06 aaronk Exp $
 */
public class TextRender
{
    /** Horizontally Left-aligned text. */
    public static final int LEFT = 0;

    /** Horizontally Right-aligned text. */
    public static final int RIGHT = 1;

    /** Vertically Top-aligned text. */
    public static final int TOP = 0;

    /** Vertically Bottom-aligned text. */
    public static final int BOTTOM = 1;

    /** Horizontally/Vertically Center-aligned text. */
    public static final int CENTER = 2;

    /** Horizontally/Vertically fully-justified text. */
    public static final int JUSTIFY = 3;

    /**
     * Renders a marked-up String, aligned within a given rectangle.
     * 
     * @param markedUpString
     *            the marked-up string to parse, and render. The string of text
     *            to be laid out may be multi-line, where each line is separated
     *            by a '\n' (0x0A).
     * @param g
     *            the java.awt.Graphics object whose properties should be used
     *            as a basis for the rendering; text is displayed in the current
     *            color (as returned by {@link Graphics#getColor() g.getColor()}
     *            ).
     * @param r
     *            the rectangle in which to align the displayed text.
     * @param hAlign
     *            the horizontal alignment (i.e., one of {@link #LEFT},
     *            {@link #RIGHT}, {@link #CENTER}, {@link #JUSTIFY}).
     * @param vAlign
     *            the vertical alignment (i.e., one of {@link #TOP},
     *            {@link #BOTTOM}, {@link #CENTER}, {@link #JUSTIFY}).
     */
    public static void render(String markedUpString, Graphics g, Rectangle r, int hAlign, int vAlign)
    {
        render(markedUpString, g, r, hAlign, vAlign, false);
    }

    /**
     * Renders a marked-up String, aligned within a given rectangle.
     * 
     * @param markedUpString
     *            the marked-up string to parse, and render. The string of text
     *            to be laid out may be multi-line, where each line is separated
     *            by a '\n' (0x0A).
     * @param g
     *            the java.awt.Graphics object whose properties should be used
     *            as a basis for the rendering; text is displayed in the current
     *            color (as returned by {@link Graphics#getColor() g.getColor()}
     *            ).
     * @param r
     *            the rectangle in which to align the displayed text.
     * @param hAlign
     *            the horizontal alignment (i.e., one of {@link #LEFT},
     *            {@link #RIGHT}, {@link #CENTER}, {@link #JUSTIFY}).
     * @param vAlign
     *            the vertical alignment (i.e., one of {@link #TOP},
     *            {@link #BOTTOM}, {@link #CENTER}, {@link #JUSTIFY}).
     * @param ellipsis
     *            if <code>true</code>, then the an ellipsis should be used when
     *            truncating the text
     */
    public static void render(String markedUpString, Graphics g, Rectangle r, int hAlign, int vAlign, boolean ellipsis)
    {
        String text = markedUpString;

        if (text != null)
        {
            String lines[] = TextLines.getLines(text);
            int lineCount = lines.length;
            int y = 0;
            FontMetrics metrics = g.getFontMetrics();
            int vFill = 0, vSpacing = 0, vXtra = 0;

            // Figure starting baseline y coordinate
            switch (vAlign)
            {
                case JUSTIFY:
                    vFill = r.height - lineCount * getFontHeight(metrics);
                    if (vFill > 0)
                    {
                        vSpacing = (lineCount > 1) ? (vFill / (lineCount - 1)) : 1;
                        // I don't believe it's possible...
                        // vSpacing will be >= getFontHeight(metrics)
                        if (vSpacing < 1) vSpacing = 1;

                        if (lineCount > 1) vXtra = vFill - (vSpacing * (lineCount - 1));
                    }
                    // fall through for y calculation
                case TOP:
                    y = getFontAscent(metrics);
                    break;
                case BOTTOM:
                    y = r.height - metrics.getDescent();
                    break;
                case CENTER:
                default:
                    if (lineCount == 1)
                        y = (r.height + getFontAscent(metrics)) / 2;
                    else
                        y = (r.height - lineCount * getFontHeight(metrics)) / 2 + getFontAscent(metrics);
                    break;
            }
            y += r.y;

            // Figure and draw each line's h coordinate
            for (int i = 0; i < lineCount; ++i)
            {
                String line;
                if (vAlign != BOTTOM)
                    line = lines[i];
                else
                    line = lines[lineCount - i - 1];

                // Render the line
                renderLine(line, g, r.x, y, r.width, hAlign, ellipsis);

                // update vertical location
                switch (vAlign)
                {
                    case CENTER:
                    case TOP:
                    default:
                        y += getFontHeight(metrics);
                        break;
                    case BOTTOM:
                        y -= getFontHeight(metrics);
                        break;
                    case JUSTIFY:
                        y += getFontHeight(metrics);
                        y += vSpacing;
                        vFill -= vSpacing;
                        if (vXtra > 0)
                        {
                            --vXtra;
                            y += 1;
                        }
                        break;
                }
            }
        }
    }

    /**
     * Renders a marked-up String, aligned within a given rectangle. The text is
     * clipped such that it does not extend beyond the bounds of the rectangle.
     * 
     * @param markedUpString
     *            the marked-up string to parse, and render. The string of text
     *            to be laid out may be multi-line, where each line is separated
     *            by a '\n' (0x0A).
     * @param g
     *            the java.awt.Graphics object whose properties should be used
     *            as a basis for the rendering; text is displayed in the current
     *            color (as returned by {@link Graphics#getColor() g.getColor()}
     *            ).
     * @param r
     *            the rectangle in which to align the displayed text.
     * @param hAlign
     *            the horizontal alignment (i.e., one of {@link #LEFT},
     *            {@link #RIGHT}, {@link #CENTER}, {@link #JUSTIFY}).
     * @param vAlign
     *            the vertical alignment (i.e., one of {@link #TOP},
     *            {@link #BOTTOM}, {@link #CENTER}, {@link #JUSTIFY}).
     */
    public static void renderClipped(String markedUpString, Graphics g, Rectangle r, int hAlign, int vAlign)
    {
        Graphics g2 = null;

        try
        {
            // Clip text to fit within rectangle
            g2.create(r.x, r.y, r.width, r.height);

            render(markedUpString, g2, r, hAlign, vAlign);
        }
        finally
        {
            if (g2 != null) g2.dispose();
        }
    }

    /**
     * Renders a String representing a single line of text, aligned horizontally
     * within the given width. The point <i>(x,y)</i> specifies the baseline of
     * the leftmost character when the text is to be left-aligned,
     * center-aligned, or fully justified. The point <i>(x+width,y)</i>
     * specifies the baseline of the rightmost character when the text is to be
     * right-aligned.
     * 
     * @param line
     *            a <code>String</code> representing a single line of text.
     * @param g
     *            the current graphics context
     * @param x
     *            the x-coordinate in the graphics context's coordinate system
     * @param y
     *            the y-coordinate in the graphics context's coordinate system
     * @param width
     *            the width within which the text should be alinged
     * @param hAlign
     *            the horizontal alignment (i.e., one of {@link #LEFT},
     *            {@link #RIGHT}, {@link #CENTER}, {@link #JUSTIFY}).
     * @param ellipsis
     *            if <code>true</code>, then an ellipsis should be used when
     *            truncating the text
     */
    public static void renderLine(String line, Graphics g, int x, int y, int width, int hAlign, boolean ellipsis)
    {
        FontMetrics metrics = g.getFontMetrics();
        int hFill = 0;

        if (ellipsis) line = TextLines.addEllipsis(line, width, metrics, hAlign);

        // Figure horizontal location
        switch (hAlign)
        {
            case JUSTIFY:
                hFill = width - metrics.stringWidth(line);
                // fall through
            case LEFT:
                // x = x; // essentially
                break;
            case RIGHT:
                x = x + width - metrics.stringWidth(line);
                break;
            case CENTER:
            default:
                x = x + (width - metrics.stringWidth(line)) / 2;
                break;
        }

        if (hFill <= 0)
            g.drawString(line, x, y);
        else
            drawHJustified(g, line, x, y, metrics, hFill);

        return;
    }

    /**
     * Draws the given string with horizontal justification. I.e., artificial
     * spacing is added such that the text fills its entire display area. The
     * text is justified to use up an additional <code>hFill</code> pixels.
     * 
     * @param g
     *            Graphics
     * @param text
     *            the single-line of text to draw
     * @param x
     *            the x coordinate of the baseline
     * @param y
     *            the y coordinate of the baseline
     * @param metrics
     *            the <code>FontMetrics</code> for the current font
     * @param hFill
     *            the number of extra pixels which must be filled in to
     *            correctly justify <code>text</code>.
     */
    private static void drawHJustified(Graphics g, String text, int x, int y, FontMetrics metrics, int hFill)
    {
        // Count characters
        int n = text.length();

        // Figure spacing between chars (n-1 spaces)
        int space = 1; // average spacing between tokens
        int xtra = 0; // spacing remainder
        if (n > 1) // otherwise, cannot justify!
        {
            space = hFill / (n - 1);
            if (space < 1)
                space = 1;
            else
                xtra = hFill - (space * (n - 1));
        }

        // Pull characters out of String
        char chars[] = new char[n];
        text.getChars(0, n, chars, 0);

        // Print each character, with approp. spacing in between
        for (int i = 0; i < chars.length; ++i)
        {
            g.drawChars(chars, i, 1, x, y);

            // Advance to next char
            x += metrics.charWidth(chars[i]);
            // Advance for justification filler
            if (hFill > 0)
            {
                hFill -= space;
                x += space;

                // Make up space remainder (if any left)
                if (xtra > 0)
                {
                    --xtra;
                    ++x;
                }
            }
        }
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
     * <li>getAscent() - getDescent() appears to work (turn on the line drawing
     * debugging code above to see it).
     * </ul>
     * 
     * @see "Java Bug Parade: 4035331"
     * 
     * 
     * @param fm
     *            the font metrics to use when calculating font ascent
     * @return the calculated ascent for the font specified by the given font
     *         metrics.
     */
    public static int getFontAscent(FontMetrics fm)
    {
        // Since according to the docs, getHeight() should be equal to
        // getAscent + getDescent + getLeading, we'll do a little computation
        // to see if bug #4035331 is still around. We'll subtract 1 from the
        // leading just to make sure we don't get caught in a rounding error.
        // If the bug is rearing its ugly head, then the ascent+descent+leading
        // should be greater than the height even though we subtracted 1 from
        // the leading.
        int calculated_height = (fm.getAscent() + fm.getDescent()) + (fm.getLeading() - 1);

        if (fm.getHeight() < calculated_height)
            return fm.getAscent() - fm.getDescent();
        else
            return fm.getAscent();

        // return (fm.getHeight() * 7) / 10 - 1;
        // return fm.getHeight() - (fm.getLeading() + fm.getDescent());
        // return fm.getAscent() - fm.getDescent();
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

    private static final boolean DEBUG = false;
}
