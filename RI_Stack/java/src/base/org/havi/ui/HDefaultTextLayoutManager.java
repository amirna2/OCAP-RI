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

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
import org.cablelabs.impl.havi.TextSupport;
import org.cablelabs.impl.havi.HaviToolkit;

/**
 * The {@link org.havi.ui.HDefaultTextLayoutManager HDefaultTextLayoutManager}
 * provides the default text rendering mechanism for the
 * {@link org.havi.ui.HStaticText HStaticText} {@link org.havi.ui.HText HText}
 * and {@link org.havi.ui.HTextButton HTextButton} classes.
 * 
 * <p>
 * The {@link org.havi.ui.HDefaultTextLayoutManager HDefaultTextLayoutManager}
 * handles alignment and justification of text in both horizontal and vertical
 * directions as specified by the current alignment modes set on
 * {@link org.havi.ui.HVisible HVisible}. It does not support scaling of text
 * content, and the scaling mode of an associated {@link org.havi.ui.HVisible
 * HVisible} is ignored.
 * 
 * <p>
 * The string passed to the {@link org.havi.ui.HDefaultTextLayoutManager#render
 * render} method may be multi-line, where each line is separated by a
 * &quot;\n&quot; (0x0A). If the string does not fit in the space available, the
 * string shall be truncated and an ellipsis (&quot;...&quot;) appended to
 * indicate the truncation.
 * 
 * <p>
 * The {@link org.havi.ui.HDefaultTextLayoutManager HDefaultTextLayoutManager}
 * should query the {@link org.havi.ui.HVisible HVisible} passed to its
 * {@link org.havi.ui.HDefaultTextLayoutManager#render render} method to
 * determine the basic font to render text in. If the specified font cannot be
 * accessed the default behavior is to replace it with the nearest builtin font.
 * Each missing character is replaced with an &quot;!&quot; character.
 * <p>
 * The antialiasing behavior of {@link org.havi.ui.HDefaultTextLayoutManager
 * HDefaultTextLayoutManager} is platform dependent.
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=5>None.</td>
 * </tr>
 * </table>
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * </table>
 * 
 * @see HTextLayoutManager
 * @see HStaticText
 * @see HText
 * @see HTextButton
 * @see HTextLook
 * @see HVisible
 * @author Aaron Kamienski
 * @version 1.1
 */

public class HDefaultTextLayoutManager implements HTextLayoutManager
{
    /**
     * Creates an {@link org.havi.ui.HDefaultTextLayoutManager
     * HDefaultTextLayoutManager} object. See the class description for details
     * of constructor parameters and default values.
     */
    public HDefaultTextLayoutManager()
    {
    }

    /**
     * Returns the minimum size required to render the text content in any
     * possible interaction state of the specified {@link org.havi.ui.HVisible
     * HVisible} component. To achieve this, the maximum width and maximum
     * height of all minimum sizes are returned.
     */
    public Dimension getMinimumSize(HVisible hvisible)
    {
        // Need to show ALL text in ALL states.
        // Essentially the MAXIMUM size!
        // Essentially HTextLook.Strategy.getMaxContentSize.
        // So, that code was lifted (could be refactored).

        // Need a font to calculate sizing info
        Font font = hvisible.getFont();
        if (font == null) font = HaviToolkit.getToolkit().getDefaultFont();
        FontMetrics metrics = hvisible.getFontMetrics(font);

        int maxW = 0, lineCount = 0;

        // largest size of test content
        for (int i = (HState.FIRST_STATE & HState.ALL_STATES); i <= (HState.LAST_STATE & HState.ALL_STATES); ++i)
        {
            String content = hvisible.getTextContent(i | HState.NORMAL_STATE);
            if (content != null)
            {
                String[] text = TextSupport.getLines(content);

                // Use the widest line
                maxW = Math.max(maxW, TextSupport.getMaxWidth(text, metrics));
                lineCount = Math.max(lineCount, text.length);
            }
        }

        return new Dimension(maxW, TextSupport.getFontHeight(metrics) * lineCount);
    }

    /**
     * Returns the maximum size required to render the text content in any
     * possible interaction state of the specified {@link org.havi.ui.HVisible
     * HVisible} component. To achieve this, the maximum width and maximum
     * height of all maximum sizes are returned. It is a valid implementation
     * option to return a dimension with a width and height of Short.MAX_VALUE.
     */
    public Dimension getMaximumSize(HVisible hvisible)
    {
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /**
     * Returns the preferred size to render the text content in any possible
     * interaction state of the specified {@link org.havi.ui.HVisible HVisible}
     * component. To achieve this, the maximum width and maximum height of all
     * preferred sizes are returned.
     */
    public Dimension getPreferredSize(HVisible hvisible)
    {
        // Want to show ALL text in ALL states.
        // Essentially the MAXIMUM size!
        // Might as well be getMinimumSize!
        return getMinimumSize(hvisible);
    }

    /**
     * Render the string. The {@link org.havi.ui.HTextLayoutManager
     * HTextLayoutManager} should use the passed {@link org.havi.ui.HVisible
     * HVisible} object to determine any additional information required to
     * render the string, e.g. <code>Font</code>, <code>Color</code> etc.
     * <p>
     * The text should be laid out in the layout area, which is defined by the
     * bounds of the specified {@link org.havi.ui.HVisible HVisible}, after
     * subtracting the insets. If the insets are <code>null</code> the full
     * bounding rectangle is used as the area to render text into.
     * <p>
     * The {@link org.havi.ui.HTextLayoutManager HTextLayoutManager} should not
     * modify the clipping rectangle of the <code>Graphics</code> object.
     * 
     * @param markedUpString
     *            the string to render.
     * @param g
     *            the graphics context, including a clipping rectangle which
     *            encapsulates the area within which rendering is permitted. If
     *            a valid insets value is passed to this method then text must
     *            only be rendered into the bounds of the widget after the
     *            insets are subtracted. If the insets value is
     *            <code>null</code> then text is rendered into the entire
     *            bounding area of the {@link org.havi.ui.HVisible HVisible}. It
     *            is implementation specific whether or not the renderer takes
     *            into account the intersection of the clipping rectangle in
     *            each case for optimization purposes.
     * @param v
     *            the {@link org.havi.ui.HVisible HVisible} into which to
     *            render.
     * @param insets
     *            the insets to determine the area in which to layout the text,
     *            or <code>null</code>.
     */
    public void render(String markedUpString, java.awt.Graphics g, HVisible v, java.awt.Insets insets)
    {
        if (markedUpString == null) return;

        if (insets == null) insets = ZERO_INSETS;

        // !!!How can be handle missing fonts/characters?

        // Figure the bounding rectangle for the text
        Dimension d = v.getSize();
        Rectangle bounds = new Rectangle(insets.left, insets.top, d.width - insets.left - insets.right, d.height
                - insets.top - insets.bottom);

        g.setFont(v.getFont());
        g.setColor(v.getForeground());

        String lines[] = TextSupport.getLines(markedUpString);
        int lineCount = lines.length;
        int y = 0;
        FontMetrics metrics = g.getFontMetrics();
        int vFill = 0, vSpacing = 0, vXtra = 0;
        final int fontHeight = TextSupport.getFontHeight(metrics);
        final int fontAscent = TextSupport.getFontAscent(metrics);

        // Figure starting baseline y coordinate
        int vAlign = v.getVerticalAlignment();
        int hAlign = v.getHorizontalAlignment();
        switch (vAlign)
        {
            case HVisible.VALIGN_JUSTIFY:
                vFill = bounds.height - lineCount * fontHeight;
                if (vFill > 0)
                {
                    vSpacing = (lineCount > 1) ? (vFill / (lineCount - 1)) : 1;
                    // I don't believe it's possible...
                    // vSpacing will be >= fontHeight
                    if (vSpacing < 1) vSpacing = 1;

                    if (lineCount > 1) vXtra = vFill - (vSpacing * (lineCount - 1));
                }
                // fall through for y calculation
            case HVisible.VALIGN_TOP:
                y = fontAscent;
                break;
            case HVisible.VALIGN_BOTTOM:
                y = bounds.height - metrics.getDescent();
                break;
            case HVisible.VALIGN_CENTER:
            default:
                if (lineCount == 1)
                    y = (bounds.height + fontAscent) / 2;
                else
                    y = (bounds.height - lineCount * fontHeight) / 2 + fontAscent;
                break;
        }
        y += bounds.y;

        // Figure and draw each line's h coordinate
        for (int i = 0; i < lineCount; ++i)
        {
            String line;
            if (vAlign != HVisible.VALIGN_BOTTOM)
                line = lines[i];
            else
                line = lines[lineCount - i - 1];

            // Render the line
            renderLine(line, g, bounds.x, y, bounds.width, hAlign, metrics);

            // update vertical location
            switch (vAlign)
            {
                case HVisible.VALIGN_CENTER:
                case HVisible.VALIGN_TOP:
                default:
                    y += fontHeight;
                    break;
                case HVisible.VALIGN_BOTTOM:
                    y -= fontHeight;
                    break;
                case HVisible.VALIGN_JUSTIFY:
                    y += fontHeight;
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
     *            the horizontal alignment
     * @param metrics
     *            the <code>FontMetrics</code>
     */
    private static void renderLine(String line, Graphics g, int x, int y, int width, int hAlign, FontMetrics metrics)
    {
        int hFill = 0;

        line = addEllipsis(line, width, metrics, hAlign);

        // Figure horizontal location
        switch (hAlign)
        {
            case HVisible.HALIGN_JUSTIFY:
                line = line.trim(); // remove front/backend whitespace
                hFill = width - metrics.stringWidth(line);
                // fall through
            case HVisible.HALIGN_LEFT:
                // x = x; // essentially
                break;
            case HVisible.HALIGN_RIGHT:
                x = x + width - metrics.stringWidth(line);
                break;
            case HVisible.HALIGN_CENTER:
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
     * Returns a new <code>String</code> with ellipsis ("...") added so that the
     * entire string will fit in the given width. If the given string already
     * fits in this width, the same string is returned. For example,
     * <code>"Scenes from an Italian Restaurant"</code> may be shortened to
     * <code>"Scenes frome an..."</code> (depending upon the given width and
     * font metrics).
     * 
     * @param text
     *            the source text
     * @param width
     *            the desired maximum width in pixels of the resulting text
     * @param fm
     *            the <code>FontMetrics</code> used to calculate string width.
     * @param where
     *            determines where to add the ellipsis characters (expressed as
     *            an <code>HVisible</code> horizontal alignment)
     * 
     * @return a <code>String</code> with an ellipsis ("...") added if
     *         necessary.
     */
    private static String addEllipsis(String text, int width, FontMetrics fm, int where)
    {
        // only add if necessary
        if (fm.stringWidth(text) > width)
        {
            int eWidth = fm.stringWidth(ellipsis);

            if (where == HVisible.HALIGN_CENTER && eWidth * 2 < width) eWidth *= 2;

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
                    case HVisible.HALIGN_CENTER:
                        index = (chars.length - nChars) / 2;
                        break;
                    case HVisible.HALIGN_RIGHT:
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
                        if (where == HVisible.HALIGN_RIGHT)
                            ++index;
                        else if (where == HVisible.HALIGN_CENTER && (nChars & 1) == 0) ++index;
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
                        if (where == HVisible.HALIGN_RIGHT)
                            --index;
                        else if (where == HVisible.HALIGN_CENTER && (nChars & 1) != 0) --index;
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
                        case HVisible.HALIGN_LEFT:
                        case HVisible.HALIGN_JUSTIFY:
                            text = new String(chars, 0, nChars) + ellipsis;
                            break;
                        case HVisible.HALIGN_CENTER:
                            text = ellipsis + new String(chars, index, nChars) + ellipsis;
                            break;
                        case HVisible.HALIGN_RIGHT:
                            text = ellipsis + new String(chars, index, nChars);
                            break;
                    }
                }
            }
        }
        return text;
    }

    private static final String ellipsis = "...";

    /** Constant zero insets. */
    private static final Insets ZERO_INSETS = new Insets(0, 0, 0, 0);
}
