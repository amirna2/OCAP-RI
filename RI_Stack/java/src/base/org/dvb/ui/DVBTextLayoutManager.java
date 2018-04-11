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

package org.dvb.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.Vector;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.havi.ui.HVisible;

import org.cablelabs.impl.util.EventMulticaster;

/*
 * There are some known problems with this code:
 * 1. Not completely MHP 1.0.2 compliant.  Much addition clarification
 *    was added to the 1.0.2 spec, most of which hasn't been incorporated.
 * 2. markUp support does not work fully.
 * 3. Also, I don't have complete confidence in the vertical measurements
 *    (both initial y and deltas).  This can be shown in odd truncation
 *    differences for VSTART, VEND, and VCENTER.
 * 4. This may be the result of Java's inexact (and even incorrect)
 *    values for height, ascent, and descent.
 */

/**
 * The DVBTextLayoutManager provides a text rendering layout mechanism for the
 * org.havi.ui.HStaticText org.havi.ui.HText and org.havi.ui.HTextButton
 * classes.
 * <p>
 * The semantics of the rendering behaviour and the settings are specified in
 * the "Text presentation" annex of the present document. The DVBTextLayoutManager
 * renders the text according to the semantics described in that annex.
 *
 * @author Aaron Kamienski
 * @author Michael Schoonover - fixed bug 5229 (event delivery incorrect), bug
 *         5228 (valid args throw illegal arg exception)
 */

public class DVBTextLayoutManager implements org.havi.ui.HTextLayoutManager
{

    /* Inherited methods from HTextLayoutManager */

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
     * @param ins
     *            the insets to determine the area in which to layout the text,
     *            or <code>null</code>.
     */
    public void render(String markedUpString, java.awt.Graphics g, HVisible v, java.awt.Insets ins)
    {
        // Fail silently if line orientation and starting corner are not
        // supported values.
        if (lineOrient != LINE_ORIENTATION_HORIZONTAL || startCorner != START_CORNER_UPPER_LEFT)
        {
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("unsupported line orientation (" + lineOrient + ")" + " or start corner (" + startCorner
                            + ")");
                }
            }
            return;
        }

        /*
         * Differences from HDefaultTextLayoutManager: 1) Support for wrapping
         * 2) Text starting position 3) Text orientation (horizontal or
         * vertical) 4) Spacing between lines/characters 5) Tab support 6) Do
         * not draw any partial characters 7) Text mark-up support
         */
        if (markedUpString != null)
        {
            FontMetrics metrics = g.getFontMetrics();
            Dimension d = v.getSize();
            int x = 0, y = 0;
            boolean overflowVert = false;
            boolean overflowHoriz = false;

            // Insets are cumulative
            if (ins == null)
            {
                if ((ins = getInsets()) == null) ins = new Insets(0, 0, 0, 0);
            }
            else
            {
                // Copy insets before adding...
                ins = new Insets(ins.top, ins.left, ins.bottom, ins.right);
                Insets addInsets = getInsets();

                ins.top += addInsets.top;
                ins.bottom += addInsets.bottom;
                ins.left += addInsets.left;
                ins.right += addInsets.right;
            }

            // Copy characters into more accessible array
            char[] buf = new char[markedUpString.length()];
            markedUpString.getChars(0, buf.length, buf, 0);

            int EXTRA = 0;
            Rectangle bounds = new Rectangle(ins.left + EXTRA, ins.top + EXTRA, d.width - ins.left - ins.right - 2
                    * EXTRA, d.height - ins.top - ins.bottom - 2 * EXTRA);
            Line lines[] = breakLines(buf, bounds, g, metrics);
            final int lineCount = lines.length;
            if (lineCount == 0) return;

            // NOTE: I wonder is the following should be max ascent and descent instead of standard ascent and descent
            int yMin = metrics.getDescent();
            int yMax = getFontAscent(metrics);
            int yHeight = yMin + yMax; // Not including spacing/leading

            // NOTE: metrics.getLeading is returning -1, so this height is alittle off
            int lineSpc = (this.lineSpace <= 0) ? metrics.getHeight() : this.lineSpace;

            int dispLines = Math.min((bounds.height - yHeight) / (lineSpc) + 1, lineCount);

            // Flag for an overflow if the number of lines is over that
            // which can fit in the container
            if (dispLines < lineCount) overflowVert = true;

            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("lineCount = " + lineCount + ", " + dispLines);
                }
            }

            // Figure starting point
            /*
             * switch(startCorner) { case START_CORNER_UPPER_LEFT: // Drawing
             * left-to-right (H) or top-to-bottom (V) x = insets.left; y =
             * insets.top + yMax; break; case START_CORNER_UPPER_RIGHT: //
             * Drawing right-to-left (H) or top-to-bottom (V) x = d.width -
             * insets.right; // Also must minus char width y = insets.top +
             * yMax; break; case START_CORNER_LOWER_LEFT: // Drawing
             * left-to-right (H) or bottom-to-top (V) x = insets.left; y =
             * d.height - insets.bottom - yMin; break; case
             * START_CORNER_LOWER_RIGHT: // Drawing right-to-left (H) or
             * bottom-to-top (V) x = d.width - insets.right; // Also must minus
             * char width y = d.height - insets.bottom - yMin; break; }
             */

            switch (vAlign)
            {
                default:
                case VERTICAL_START_ALIGN: // TOP
                    y = yMax;
                    break;
                case VERTICAL_END_ALIGN: // BOTTOM
                    y = bounds.height - yMin;
                    break;
                case VERTICAL_CENTER: // CENTERED
                    if (dispLines == 1)
                        y = (bounds.height + yMax) / 2;
                    else
                        y = (bounds.height - (getFontHeight(metrics) + (dispLines - 1) * lineSpc)) / 2
                                + yMax;
            }
            y += bounds.y;

            g.setFont(v.getFont());
            g.setColor(v.getForeground());

            // Draw characters...
            for (int i = 0; i < dispLines; ++i)
            {
                boolean noDraw = false;
                x = bounds.x; // reset each time through

                final int ln = (vAlign != VERTICAL_END_ALIGN) ? i : lineCount - i - 1;

                StringBuffer line = lines[ln].line;
                char[] lineChars = new char[line.length()];
                line.getChars(0, line.length(), lineChars, 0);

                int w = lines[ln].width;
                // int h = lines[ln].height;
                boolean markup = lines[ln].markup;

                // Check for line that would only be partially displayed
                /*
                 * I have some questions about the correctness of this with
                 * respect to our above calculations for the initial y (or vice
                 * versa). Because we get (with the test code below) some cases
                 * where VEND fits, but VSTART and VCENTER chop off.
                 */
                /*
                 * Also note that the way VCENTER truncation is handled is not
                 * correct w.r.t MHP 1.0.2 where, if the text won't fit,
                 * starting with the first line all text that can be shown
                 * centered should be.
                 */
                if (y + yMin > (bounds.y + bounds.height) || (y - yMax + 1 < bounds.y))
                {
                    // Flag for vertical overflow
                    overflowVert = true;
                    // Skip this line, but continue to advance the y location
                    noDraw = true;
                }

                // Figure horizontal location
                if (startCorner == START_CORNER_UPPER_LEFT)
                {
                    switch (hAlign)
                    {
                        default:
                        case HORIZONTAL_START_ALIGN: // LEFT
                            // x = x; // essentially
                            break;
                        case HORIZONTAL_END_ALIGN: // RIGHT
                            x = x + bounds.width - w;
                            break;
                        case HORIZONTAL_CENTER: // CENTER
                            x = x + (bounds.width - w) / 2;
                            break;
                    }
                }

                if (DEBUG)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("LINE @ (" + x + "," + y + ")");
                    }
                }

                // Render
                if (!noDraw)
                {
                    if (!markup && w < bounds.width)
                        g.drawString(line.toString(), x, y);
                    else
                    {
                        // Handle markup
                        final int cMax = line.length();
                        for (int cn = 0; cn < cMax; ++cn)
                        {
                            // Skip/Handle marker/markup
                            char c = line.charAt(cn);
                            if (isMarker(c))
                            {
                                continue;
                            }
                            else if (isMarkup(c))
                            {
                                cn += handleMarkup(lineChars, cn, g, false) - 1;
                                continue;
                            }
                            if (DEBUG)
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("Char ('" + c + "') @ (" + x + "," + y + ")");
                                }
                            }

                            int newX;

                            // Advance to next tab-stop if necessary
                            if (c == '\t' && hAlign == HORIZONTAL_START_ALIGN)
                            {
                                newX = advanceTab(x, metrics.charWidth(' '));
                            }
                            else
                            {
                                newX = x + metrics.charWidth(c) + letterSpace;
                                if (newX > (bounds.x + bounds.width) || x < bounds.x)
                                {
                                    // Should probably wait until ALL lines
                                    // are displayed to notify...
                                    // Flag for horizontal overflow
                                    overflowHoriz = true;
                                    break;
                                }
                                else
                                {
                                    // Render the character (as a string)
                                    g.drawString(new String(new char[] { c }), x, y);
                                }
                            }

                            // Advance the character position
                            x = newX;
                        }
                    }
                }

                // Update vertical location
                if (startCorner == START_CORNER_UPPER_LEFT)
                {
                    switch (vAlign)
                    {
                        default:
                        case VERTICAL_START_ALIGN: // TOP
                        case VERTICAL_CENTER: // CENTERED
                            y += lineSpc;
                            break;
                        case VERTICAL_END_ALIGN: // BOTTOM
                            y -= lineSpc;
                            break;
                    }
                }
            }

            // Did we get a flag of an overflow? If so post.
            if (overflowVert || overflowHoriz)
            {
                notifyTextOverflow(markedUpString, v, overflowHoriz, overflowVert);
            }
        }
    }

    /* DVBTextLayoutManager */

    /**
     * The text should be aligned horizontally to the horizontal start side
     * (e.g. when start corner is upper left and line orientation horizontal,
     * meaning text that is read left to right from top to bottom, this implies
     * alignment to left).
     */
    public static final int HORIZONTAL_START_ALIGN = 1;

    /**
     * The text should be horizontally to the horizontal end side (e.g. when
     * start corner is upper left and line orientation horizontal, meaning text
     * that is read left to right from top to bottom, this implies alignment to
     * right).
     */
    public static final int HORIZONTAL_END_ALIGN = 2;

    /**
     * The text should be centered horizontally.
     */
    public static final int HORIZONTAL_CENTER = 3;

    /**
     * The text should be aligned vertically to the vertical start side (e.g.
     * when start corner is upper left and line orientation horizontal, meaning
     * text that is read left to right from top to bottom, this implies
     * alignment to top).
     * <p>
     * This is defined by the clause "Vertical limits" in the
     * "Text presentation" annex of the present document.
     */
    public static final int VERTICAL_START_ALIGN = 4;

    /**
     * The text should be aligned vertically to the vertical end side (e.g. when
     * start corner is upper left and line orientation horizontal, meaning text
     * that is read left to right from top to bottom, this implies alignment to
     * bottom).
     * <p>
     * This is defined by the clause "Vertical limits" in the
     * "Text presentation" annex of the present document.
     */
    public static final int VERTICAL_END_ALIGN = 5;

    /**
     * The text should be centered vertically.
     */
    public static final int VERTICAL_CENTER = 6;

    /**
     * Horizontal line orientation.
     */
    public static final int LINE_ORIENTATION_HORIZONTAL = 10;

    /**
     * Vertical line orientation.
     */
    public static final int LINE_ORIENTATION_VERTICAL = 11;

    /**
     * Upper left text start corner.
     */
    public static final int START_CORNER_UPPER_LEFT = 20;

    /**
     * Upper right text start corner.
     */
    public static final int START_CORNER_UPPER_RIGHT = 21;

    /**
     * Lower left text start corner.
     */
    public static final int START_CORNER_LOWER_LEFT = 22;

    /**
     * Lower right text start corner.
     */
    public static final int START_CORNER_LOWER_RIGHT = 23;

    /**
     * Constructs a DVBTextLayoutManager object with default parameters
     * (HORIZONTAL_START_ALIGN, VERTICAL_START_ALIGN,
     * LINE_ORIENTATION_HORIZONTAL, START_CORNER_UPPER_LEFT, wrap = true,
     * linespace = (point size of the default font for HVisible) + 7,
     * letterspace = 0, horizontalTabSpace = 56)
     */
    public DVBTextLayoutManager()
    {
        this(HORIZONTAL_START_ALIGN, VERTICAL_START_ALIGN, LINE_ORIENTATION_HORIZONTAL, START_CORNER_UPPER_LEFT, true,
                -1, /* Treat special: means figure it out based on Font size. */
                0, 56);
    }

    /**
     * Constructs a DVBTextLayoutManager object.
     *
     * @param horizontalAlign
     *            Horizontal alignment setting
     * @param verticalAlign
     *            Vertical alignment setting
     * @param lineOrientation
     *            Line orientation setting
     * @param startCorner
     *            Starting corner setting
     * @param wrap
     *            Text wrapping setting
     * @param linespace
     *            Line spacing setting expressed in points
     * @param letterspace
     *            Letterspacing adjustment relative to the default
     *            letterspacing. Expressed in units of 1/256th point as the
     *            required increase in the spacing between consecutive
     *            characters. May be either positive or negative.
     * @param horizontalTabSpace
     *            Horizontal tabulation setting in points
     */
    public DVBTextLayoutManager(int horizontalAlign, int verticalAlign, int lineOrientation, int startCorner,
            boolean wrap, int linespace, int letterspace, int horizontalTabSpace)
    {
        setHorizontalAlign(horizontalAlign);
        setVerticalAlign(verticalAlign);
        setLineOrientation(lineOrientation);
        setStartCorner(startCorner);
        setTextWrapping(wrap);
        setLineSpace(linespace);
        setLetterSpace(letterspace);
        setHorizontalTabSpacing(horizontalTabSpace);

        iniz();
    }

    /**
     * Common construction.
     */
    private void iniz()
    {
        insets = new Insets(0, 0, 0, 0);
        colors = new Stack();
    }

    /**
     * Set the horizontal alignment. The setting shall be one of
     * <code>HORIZONTAL_CENTER</code>, <code>HORIZONTAL_END_ALIGN</code> or
     * <code>HORIZONTAL_START_ALIGN</code>. The failure mode if other values are
     * used is implementation dependent.
     *
     * @param horizontalAlign
     *            Horizontal alignment setting
     */
    public void setHorizontalAlign(int horizontalAlign)
    {
        switch (horizontalAlign)
        {
            case HORIZONTAL_CENTER:
            case HORIZONTAL_START_ALIGN:
            case HORIZONTAL_END_ALIGN:
                break;
            default:
                throw new IllegalArgumentException("Unsupported horizontalAlign = " + horizontalAlign);
        }
        hAlign = horizontalAlign;
    }

    /**
     * Set the vertical alignment. The setting shall be one of
     * <code>VERTICAL_CENTER</code>, <code>VERTICAL_END_ALIGN</code> or
     * <code>VERTICAL_START_ALIGN</code>. The failure mode if other values are
     * used is implementation dependent.
     *
     * @param verticalAlign
     *            Vertical alignment setting
     */
    public void setVerticalAlign(int verticalAlign)
    {
        switch (verticalAlign)
        {
            case VERTICAL_CENTER:
            case VERTICAL_START_ALIGN:
            case VERTICAL_END_ALIGN:
                break;
            default:
                throw new IllegalArgumentException("Unsupported verticalAlign = " + verticalAlign);
        }
        vAlign = verticalAlign;
    }

    /**
     * Set the line orientation. The setting shall be one of
     * <code>LINE_ORIENTATION_VERTICAL</code>,
     * <code>LINE_ORIENTATION_HORIZONTAL</code>. The failure mode if other
     * values are used is implementation dependent.
     *
     * @param lineOrientation
     *            Line orientation setting
     */
    public void setLineOrientation(int lineOrientation)
    {
        switch (lineOrientation)
        {
            case LINE_ORIENTATION_HORIZONTAL:
                // the following setting is unsupported and fails silently in
                // render()
            case LINE_ORIENTATION_VERTICAL:
                break;
            default:
                throw new IllegalArgumentException("Unsupported lineOrientation " + lineOrientation);
        }
        lineOrient = lineOrientation;
    }

    /**
     * Set the starting corner. The setting shall be one of
     * <code>START_CORNER_UPPER_LEFT</code>,
     * <code>START_CORNER_UPPER_RIGHT<code>, <code>START_CORNER_LOWER_LEFT</code>
     * or <code>START_CORNER_LOWER_RIGHT</code>. The failure mode if other
     * values are used is implementation dependent.
     *
     * @param startCorner
     *            Starting corner setting
     */
    public void setStartCorner(int startCorner)
    {
        switch (startCorner)
        {
            case START_CORNER_UPPER_LEFT:
                // the following settings are unsupported and fail silently in
                // render()
            case START_CORNER_LOWER_LEFT:
            case START_CORNER_LOWER_RIGHT:
            case START_CORNER_UPPER_RIGHT:
                break;
            default:
                throw new IllegalArgumentException("Unsupported startCorner " + startCorner);
        }
        this.startCorner = startCorner;
    }

    /**
     * Set the text wrapping setting.
     *
     * @param wrap
     *            Text wrapping setting
     */
    public void setTextWrapping(boolean wrap)
    {
        this.wrap = wrap;
    }

    /**
     * Set the line space setting. Using -1 as the line space setting shall
     * cause the line spacing to be determined from the size of the default
     * font.
     *
     * @param lineSpace
     *            line space setting
     */
    public void setLineSpace(int lineSpace)
    {
        this.lineSpace = lineSpace;
    }

    /**
     * Set the letter space setting.
     *
     * This is a 16 bit signed integer specifying in units of 1/256th point the
     * required increase in the spacing between consecutive characters. It
     * corresponds to the "track" parameter in the MHP text rendering rules.
     *
     * @param letterSpace
     *            letter space setting
     */
    public void setLetterSpace(int letterSpace)
    {
        if (letterSpace > Short.MAX_VALUE || letterSpace < Short.MIN_VALUE)
            throw new IllegalArgumentException("16-bit signed integer expected " + letterSpace);
        this.letterSpace = letterSpace;
    }

    /**
     * Set the horizontal tabulation spacing.
     *
     * @param horizontalTabSpace
     *            tab spacing in points
     */
    public void setHorizontalTabSpacing(int horizontalTabSpace)
    {
        hTab = horizontalTabSpace;
    }

    /**
     * Get the horizontal alignment.
     *
     * @return Horizontal alignment setting
     */
    public int getHorizontalAlign()
    {
        return hAlign;
    }

    /**
     * Get the vertical alignment.
     *
     * @return Vertical alignment setting
     */
    public int getVerticalAlign()
    {
        return vAlign;
    }

    /**
     * Get the line orientation.
     *
     * @return Line orientation setting
     */
    public int getLineOrientation()
    {
        return lineOrient;
    }

    /**
     * Get the starting corner.
     *
     * @return Starting corner setting
     */
    public int getStartCorner()
    {
        return startCorner;
    }

    /**
     * Get the text wrapping setting.
     *
     * @return text wrapping setting
     */
    public boolean getTextWrapping()
    {
        return wrap;
    }

    /**
     * Get the line space setting.
     *
     * @return line space setting or -1, if the default line spacing is
     *         determined from the size of the default font used.
     */
    public int getLineSpace()
    {
        return lineSpace;
    }

    /**
     * Get the letter space setting. This is a 16 bit signed integer specifying
     * in units of 1/256th point the required increase in the spacing between
     * consecutive characters. It corresponds to the "track" parameter in the
     * MHP text rendering rules.
     *
     * @return letter space setting
     */
    public int getLetterSpace()
    {
        return letterSpace;
    }

    /**
     * Get the horizontal tabulation spacing.
     *
     * @return the horizontal tabulation spacing
     */
    public int getHorizontalTabSpacing()
    {
        return hTab;
    }

    /**
     * Sets the insets which shall be used by this DVBTextLayoutManager to
     * provide a "virtual margin". These shall be added to the insets passed to
     * the <code>Render</code> method (which are to be considered as "bounds").
     * If this method is not called, the default insets are 0 at each edge.
     *
     * @param insets
     *            Insets that should be used
     */
    public void setInsets(java.awt.Insets insets)
    {
        this.insets = insets;
    }

    /**
     * Returns the insets set by the setInsets method. These Insets are added to
     * the ones passed to the <code>render</code> method for rendering the text.
     * When not previously set, zero Insets are returned.
     *
     * @return Insets set by the setInsets method
     */
    public java.awt.Insets getInsets()
    {
        return insets;
    }

    /** The listeners regsitered. */
    private TextOverflowListener listeners;

    /**
     * Register a TextOverflowListener that will be notified if the text string
     * does not fit in the component when rendering.
     *
     * @param l
     *            a listener object
     */
    public synchronized void addTextOverflowListener(TextOverflowListener l)
    {
        if (l == null) throw new NullPointerException("null listener");

        // Add the listener to the list of listeners
        listeners = EventMulticaster.add(listeners, l);
    }

    /**
     * Removes a TextOverflowListener that has been registered previously.
     *
     * @param l
     *            a listener object
     */
    public synchronized void removeTextOverflowListener(TextOverflowListener l)
    {
        if (l == null) throw new NullPointerException("null listener");

        // Remove the listener from the list of listeners
        listeners = EventMulticaster.remove(listeners, l);
    }

    /**
     * Notifies listeners about text overflow.
     *
     * @param markedUpString
     *            the string that was rendered
     * @param v
     *            the HVisible object that was being rendered
     * @param overflowedHorizontally
     *            <code>true</code> if the text overflew the bounds of the
     *            component in the horizontal direction; otherwise
     *            <code>false</code>
     * @param overflowedVertically
     *            <code>true</code> if the text overflew the bounds of the
     *            component in the vertical direction; otherwise
     *            <code>false</code>
     */
    private synchronized void notifyTextOverflow(String markedUpString, HVisible v, boolean overflowedHorizontally,
            boolean overflowedVertically)
    {
        if (listeners != null)
        {
            listeners.notifyTextOverflow(markedUpString, v, overflowedHorizontally, overflowedVertically);
        }
    }

    /**
     * Indicates whether the given character is a <i>marker</i> character as
     * specified in D.4.2 (Marker characters) of the MHP specification.
     * Characters with a value in the range 0x1C to 0x1F are these marker
     * characters.
     *
     * @param c
     *            the character to test
     * @return <code>true</code> if <code>c</code> is a marker character (that
     *         is, it is in the range 0x1C to 0x1F inclusive);
     *         <code>false</code> otherwise.
     */
    private final boolean isMarker(char c)
    {
        return (c >= 0x1C) && (c <= 0x1F);
    }

    /**
     * Indicates whether the given character signals the start of a format
     * control mark-up string as specified in D.4.4 (Format Control Mark-up) of
     * the MHP specification. A character with the value of 0x1B marks the start
     * of a mark-up string.
     *
     * @param c
     *            the character to test
     * @return <code>true</code> if <code>c</code> is a marker character (that
     *         is, it is 0x1B); <code>false</code> otherwise.
     */
    private final boolean isMarkup(char c)
    {
        return c == 0x1B;
    }

    /**
     * Contains information about a line.
     */
    static private class Line
    {
        public Line(StringBuffer line, int width, int height, boolean markup)
        {
            this.line = line;
            this.width = width;
            this.height = height;
            this.markup = markup;
        }

        public StringBuffer line;

        public int width;

        public int height;

        public boolean markup;
    }

    /**
     * Breaks the given <code>char</code> buffer into individual lines of text.
     * Lines are broken at <i>carriage-return</i> characters (as specified by
     * MHP Annex D.4.1) and at appropriate line-breaking locations (as specified
     * by MHP Annex D.3.7).
     *
     * @param buf
     *            the character buffer
     * @param bounds
     *            the bounds which govern the internal portions of this text
     *            field
     * @param g
     *            the current graphics context
     * @param metrics
     *            the font metrics currently in use
     *
     * @return an array of <code>Line</code> objects which specify the width and
     *         height of each line in addition to the actual text itself
     */
    private Line[] breakLines(char[] buf, Rectangle bounds, Graphics g, FontMetrics metrics)
    {
        // Figure the width-of-line
        int wol = (lineOrient == LINE_ORIENTATION_HORIZONTAL) ? bounds.width : bounds.height;

        Vector lines = new Vector();

        // color markup sequences can be nested, so we use the following list as a LIFO to store
        // the active color markup sequences
        ArrayList colorStartList = new ArrayList();

        int COLOR_MARKUP_START_SZ = 7;
        int COLOR_MARKUP_END_SZ = 2;
        char[] prependChars = new char[0];  // this an array of chars to be prepended to the line's text


        // Loop over lines and generate vector Line objects
        for (int i = 0; i < buf.length;)
        {
            int width = 0;
            int height = (lineOrient == LINE_ORIENTATION_HORIZONTAL) ? getFontHeight(metrics) : 0;

            boolean markup = false;  // this is a state bool to denote whether or not there are any markup sequences in the line
            boolean anyTokens = false;  // this is a state bool that is false until we have encoutered the first non-white, non-markup char in the line
            boolean inToken = false;  // this is a state bool that is true when we are inside a word and false when we are in whitespace
            boolean lastIsCR = false;  // this is true if the line ends on a carriage return
            boolean firstChar = true;  // this is true until we hit the first char

            int start = i;  // start index of line in char buffer
            int end = -1;  // end index of line in char buffer


            StringBuffer lineText = null;

            // first, scan into the line to find out where the current line will end;
            // this makes it easier to properly manage the markup chars
            end = prescanLine(buf, g, metrics, i, wol);

            if (end == i)
            {
                // no more lines -- this will happen if only white space is left
                break;
            }


            // Loop over characters and generate a Line object
            for (; i < end; )
            {
                char c = buf[i];

                // Skip non-printing characters
                if (isMarker(c))
                {
                    if (!anyTokens)
                    {
                        // We will delete any whitespace prior to the first non-white, non-markup char
                        // We need to save off any markup chars that precede the first non-white char,
                        // so they can be prepended onto the line text

                        char[] prependCharsTemp = new char[prependChars.length + 1];
                        System.arraycopy(prependChars, 0, prependCharsTemp, 0, prependChars.length);
                        System.arraycopy(buf, i, prependCharsTemp, prependChars.length, 1);
                        prependChars = prependCharsTemp;
                    }

                    markup = true;
                    i++;
                    continue;
                }
                else if (isColorMarkupStart(buf, i))
                {
                    if (!anyTokens)
                    {
                        // We will delete any whitespace prior to the first non-white, non-markup char.
                        // We need to save off any markup chars that precede the first non-white char,
                        // so they can be prepended onto the line text.

                        char[] prependCharsTemp = new char[prependChars.length + COLOR_MARKUP_START_SZ];
                        System.arraycopy(prependChars, 0, prependCharsTemp, 0, prependChars.length);
                        System.arraycopy(buf, i, prependCharsTemp, prependChars.length, COLOR_MARKUP_START_SZ);
                        prependChars = prependCharsTemp;
                    }

                    // add this color markup to the list of active color markups
                    char[] colorMarkupStart = new char[COLOR_MARKUP_START_SZ];
                    System.arraycopy(buf, i, colorMarkupStart, 0, COLOR_MARKUP_START_SZ);
                    colorStartList.add(colorMarkupStart);

                    i += COLOR_MARKUP_START_SZ;

                    markup = true;
                    continue;
                }
                else if (isColorMarkupEnd(buf, i))
                {
                    if (!anyTokens)
                    {
                        // We will delete any whitespace prior to the first non-white, non-markup char.
                        // We need to save off any markup chars that precede the first non-white char,
                        // so they can be prepended onto the line text.

                        char[] prependCharsTemp = new char[prependChars.length + COLOR_MARKUP_END_SZ];
                        System.arraycopy(prependChars, 0, prependCharsTemp, 0, prependChars.length);
                        System.arraycopy(buf, i, prependCharsTemp, prependChars.length, COLOR_MARKUP_END_SZ);
                        prependChars = prependCharsTemp;
                    }

                    i += COLOR_MARKUP_END_SZ;

                    // remove the most-recent color markup from the list of active color markups
                    colorStartList.remove (colorStartList.size() - 1);

                    markup = true;
                    continue;
                }
                else if (isMarkup(c))
                {
                    // non-color markup is not supported, so just leave the markup chars in the text string
                    int nMarkupSz = handleMarkup(buf, i, g, true) - 1;

                    if (!anyTokens)
                    {
                        // We will delete any whitespace prior to the first non-white, non-markup char.
                        // We need to save off any markup chars that precede the first non-white char,
                        // so they can be prepended onto the line text.

                        char[] prependCharsTemp = new char[prependChars.length + nMarkupSz];
                        System.arraycopy(prependChars, 0, prependCharsTemp, 0, prependChars.length);
                        System.arraycopy(buf, i, prependCharsTemp, prependChars.length, nMarkupSz);
                        prependChars = prependCharsTemp;
                    }

                    i += nMarkupSz;

                    markup = true;
                    continue;
                }

                // if we get to here, then the current char is NOT part of a markup

                // If an explicit CR character, collect and move to next line
                if ((lastIsCR = c == '\r') || c == '\n')
                {
                    int ii;
                    if (lastIsCR && (ii = i + 1) < buf.length && buf[ii] == '\n')
                    {
                        ++i; // move past LF too
                    }
                    lastIsCR = true;
                    ++i; // move past CR
                    break;
                }

                if (c != ' ')
                {
                    if (DEBUG)
                    {
                        if (!inToken)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(i + ": start-token");
                            }
                        }
                        else if (!anyTokens)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(i + ": first-token");
                            }
                        }
                    }
                    if (!anyTokens && !inToken)
                    {
                        // this is the first non-white char of the line, so set the start index here
                        // this skips white at front of a line
                        start = i;
                        width = 0;
                    }

                    // since this is not whitespace, we must be inside a word
                    anyTokens = true;
                    inToken = true;
                }
                else if (inToken)
                {
                    // we are now in white space, so if we were in a word, set the inToken bool to false

                    if (DEBUG && inToken)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(i + ": end-token");
                        }
                    }
                    inToken = false;
                }

                // Treat tabs as spaces if tabstops aren't supported
                if (c == '\t')
                {
                    if (lineOrient == LINE_ORIENTATION_HORIZONTAL && startCorner == START_CORNER_UPPER_LEFT
                            && hAlign == HORIZONTAL_START_ALIGN)
                    {
                        markup = true;
                        width = advanceTab(width, metrics.charWidth(' '));
                    }
                    else
                    {
                        c = '\u00a0'; // non-breaking space
                        buf[i] = c;
                    }
                }

                // Figure width of character (if not '\t')
                if (c != '\t')
                {
                    int woc = (lineOrient == LINE_ORIENTATION_HORIZONTAL) ? metrics.charWidth(c)
                            : getFontHeight(metrics);
                    // Test add it to the total width
                    width += woc;

                    // Add letter spacing if applicable
                    if (!firstChar) width += letterSpace;

                    firstChar = false;
                }

                // Figure height of character
                if (lineOrient == LINE_ORIENTATION_VERTICAL) height = Math.max(height, metrics.charWidth(c));

                // increment to next char, and re-loop
                i++;
            }

            if (end < 0)
            {
                end = start;
                if (DEBUG)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("end==start");
                    }
                }
            }

            // first, add the line text between start and end
            lineText = (new StringBuffer()).append(buf, start, end - start);

            // next prepend any markup chars that were not included in the line text.  this includes active color markup
            // sequences from previous lines, as well as any markup chars that preceded the first non-white non-markup char
            // of the line.
            if (prependChars.length != 0)
            {
                lineText.insert(0, prependChars);
                prependChars = new char[0];
            }

            // if there are active color markups remaining, form them into an array so that they can be prepended onto the
            // next line.  Also postpend and appropriate number of color markup closes onto the end of the current line so
            // that all open color markups in the line are closed within that line.  Any that are still active will
            // be re-opened at the start of the next line.
            if ((colorStartList.size() != 0))
            {
                prependChars = new char[colorStartList.size() * COLOR_MARKUP_START_SZ];
                for (int iColorMarkup = 0; iColorMarkup < colorStartList.size(); iColorMarkup++)
                {
                    lineText.append ((char) 0x1B);  // insert end of color markup
                    lineText.append ((char) 0x63);  // insert end of color markup

                    char[] tmpChars = (char[])colorStartList.get (iColorMarkup);
                    System.arraycopy(tmpChars, 0, prependChars, iColorMarkup * COLOR_MARKUP_START_SZ, COLOR_MARKUP_START_SZ);
                }

                markup = true;
            }

            // add the line
            lines.addElement(new Line(lineText, width, height, markup));

            // handle CR as final character
            if (lastIsCR && i >= buf.length) lines.addElement(new Line(new StringBuffer(), 0, height, false));
        }

        Line array[] = new Line[lines.size()];
        lines.copyInto(array);

        return array;
    }

    private int prescanLine(char[] buf, Graphics g, FontMetrics metrics, int nStartIndex, int wol)
    {
        int COLOR_MARKUP_START_SZ = 7;
        int COLOR_MARKUP_END_SZ = 2;

        // Loop over lines and generate vector Line objects
        int width = 0;  // we keep track of the width of the line
        // we keep track of the width to the last token break (e.g. space between words) -- this way
        // we can back up if we overshoot the max allowed width for the line
        int lastWidth = 0;

        boolean anyTokens = false;
        boolean inToken = false;
        boolean lastIsCR = false;
        boolean firstChar = true;

        int start = nStartIndex;
        int end = -1;
        int lastEnd = 0;


        // Loop over characters
        for (int i = nStartIndex; i < buf.length;)
        {
            char c = buf[i];

            // Skip non-printing characters
            if (isMarker(c))
            {
                i++;
                end = i;
                continue;
            }
            else if (isColorMarkupStart(buf, i))
            {
                i += COLOR_MARKUP_START_SZ;
                end = i;
                continue;
            }
            else if (isColorMarkupEnd(buf, i))
            {
                i += COLOR_MARKUP_END_SZ;
                end = i;
                continue;
            }
            else if (isMarkup(c))
            {
                // non-color markup is not supported, so just leave the markup chars in the text string
                int nMarkupSz = handleMarkup(buf, i, g, true) - 1;
                i += nMarkupSz;
                end = i;
                continue;
            }

            // If an explicit CR character: collect and move to next line
            if ((lastIsCR = c == '\r') || c == '\n')
            {
                int ii;
                if (lastIsCR && (ii = i + 1) < buf.length && buf[ii] == '\n')
                {
                    ++i; // move past LF too
                }
                lastIsCR = true;
                ++i; // move past CR

                end = i;
                break;
            }

            // if we get this far, then the current char is not part of a markup sequence

            if (c != ' ')
            {
                if (!anyTokens && !inToken) // skip white at front of line
                {
                    // if this is the first non-white char seen in the line, start the line here
                    start = i;
                    width = 0;
                }
                anyTokens = true;
                inToken = true;
                end = i + 1;
            }
            else if (inToken)
            {
                // if we were in a token, then this is whitespace and we are now not in a token
                lastWidth = width;
                lastEnd = end;
                inToken = false;
            }

            // Treat tabs as spaces if tabstops aren't supported
            if (c == '\t')
            {
                if (lineOrient == LINE_ORIENTATION_HORIZONTAL && startCorner == START_CORNER_UPPER_LEFT
                        && hAlign == HORIZONTAL_START_ALIGN)
                {
                    width = advanceTab(width, metrics.charWidth(' '));
                }
            }

            // Figure width of character (if not '\t')
            if (c != '\t')
            {
                int woc = (lineOrient == LINE_ORIENTATION_HORIZONTAL) ? metrics.charWidth(c)
                        : getFontHeight(metrics);
                // Test add it to the total width
                width += woc;

                // Add letter spacing if applicable
                if (!firstChar) width += letterSpace;
                firstChar = false;
            }

            if (getTextWrapping())
            {
                // If we have overshot the max allowed width of a line, revert to last token width and end line
                if (width > wol && lastWidth != 0)
                {
                    width = lastWidth;
                    end = i = lastEnd; // go back to end of previous
                    break;
                }
            }

            // If width is still under the allowed max, then go to next char and loop
            i++;
        }

        if (end < 0)
        {
            end = start;
        }

        return end;
    }

    /**
     * Returns the font ascent, massaged as necessary for the current platform.
     *
     * @return the font ascent, massaged as necessary for the current platform
     */
    private int getFontAscent(FontMetrics metrics)
    {
        return metrics.getAscent();
    }

    /**
     * Returns the font height, massaged as necessary for the current platform.
     *
     * @return the font height, massaged as necessary for the current platform
     */
    private int getFontHeight(FontMetrics metrics)
    {
        return metrics.getHeight();
    }

    /**
     * Advance x position by one tab character ('\t').
     *
     * @param x
     *            the input x position
     * @param space
     *            the width of a ' ' character
     * @return the output x position
     */
    private int advanceTab(int x, int space)
    {
        // Calculate tab-stop
        int t1 = ((x + hTab) / hTab) * hTab;
        int t2 = x + space;

        // Advance to next tab-stop (2nd if less than space's width)
        return (t1 >= t2) ? t1 : t1 + hTab;
    }

    /**
     * Handles the format control mark-up codes found in the given character
     * buffer at the given index. The format of the start and end text mark-ups
     * are given in the following table:
     * <p>
     *
     * <TABLE border>
     * <tr>
     * <th></th>
     * <th>bits</th>
     * <th>value</th>
     * <th>note</th>
     * </tr>
     *
     * <tr>
     * <td>
     *
     * <pre>
     * start_of_markup
     * </pre>
     *
     * <td>8</td>
     * <td>0x1B</td>
     * <td>Escape</td>
     * </tr>
     *
     * <tr>
     * <td>
     *
     * <pre>
     * markup_start_identifier
     * </pre>
     *
     * <td>8</td>
     * <td>0x40-0x5E</td>
     * <td>'@' to '^'</td>
     * </tr>
     *
     * <tr>
     * <td>
     *
     * <pre>
     * parameters_length
     * </pre>
     *
     * <td>8</td>
     * <td>N</td>
     * <td></td>
     * </tr>
     *
     * <tr>
     * <td>
     *
     * <pre>
     * for( i=0; i&lt;N; i++) {
     * </pre>
     *
     * <td></td>
     * <td></td>
     * <td></td>
     * </tr>
     *
     * <tr>
     * <td>
     *
     * <pre>
     * parameter_byte
     * </pre>
     *
     * <td>8</td>
     * <td>0x00-0xFF</td>
     * <td></td>
     * </tr>
     *
     * <tr>
     * <td>
     *
     * <pre>}</pre>
     * <td></td>
     * <td></td>
     * <td></td>
     * </tr>
     *
     * </TABLE>
     * <p>
     *
     * <TABLE border>
     * <tr>
     * <th></th>
     * <th>bits</th>
     * <th>value</th>
     * <th>note</th>
     * </tr>
     *
     * <tr>
     * <td>
     *
     * <pre>
     * end_of_markup
     * </pre>
     *
     * </td>
     * <td>8</td>
     * <td>0x1B</td>
     * <td>Escape</td>
     * </tr>
     *
     * <tr>
     * <td>
     *
     * <pre>
     * markup_end_identifier
     * </pre>
     *
     * </td>
     * <td>8</td>
     * <td>0x60-0x7E</td>
     * <td>''' to '~'</td>
     * </tr>
     *
     * </TABLE>
     *
     * <p>
     * Note that the end mark-up identifier is always 0x20 more than the start
     * mark-up identifier.
     * <p>
     * Currently, this only affects the current color of the
     * <code>Graphics</code> device. This is done with the following sequence:
     * <code>
     * 0x1B 0x43 0x04 0x<i>rr</i> 0x<i>gg</i> 0x<i>bb</i> 0x<i>tt</i>
     * </code>. Where <i>rr</i>, <i>gg</i>, <i>bb</i>, and <i>tt</i>
     * represent the red, green, blue, and transparency values respectively.
     * <p>
     * A 'bold' style mark-up code is described in the specification, but it is
     * not supported in the current profile. Just for reference, he is it's
     * sequence: <code>
     * 0x1B 0x42 0x00
     * </code>
     *
     * @param buf
     *            the character buffer in which to find mark-up characters
     * @param i
     *            the index of the mark-up string
     * @param g
     *            the <code>Graphics</code> object to perform any changes on
     * @param parseOnly
     *            if <code>true</code> then do not perform any mark-up
     *            operations.
     *
     * @return the size of the mark-up string (i.e., the number of
     *         <code>char</code>s to skip forwared before continuing normal
     *         parsing)
     */
    private final int handleMarkup(char[] buf, int start, Graphics g, boolean parseOnly)
    {
        int i = start;
        if (!isMarkup(buf[i++])) return 0;

        char code = buf[i++];
        if (code >= 0x40 && code <= 0x5E) // START of markup
        {
            int n = buf[i++];
            int j = i;
            i += n;

            if (!parseOnly)
            {
                // Currently, we only support Color
                if (code == 0x43)
                {
                    int R = buf[j], G = buf[j + 1], B = buf[j + 2], A = buf[j + 3];

                    colors.push(g.getColor());
                    g.setColor(createColor(R, G, B, A));
                }
            }
        }
        else if (code >= 0x60 && code <= 0x7e) // END of markup
        {
            if (!parseOnly)
            {
                // Currently, we only support Color
                if (code == 0x63)
                {
                   try
                    {
                        g.setColor((Color) colors.pop());
                    }
                    catch (EmptyStackException ese)
                    {
                    }
                    catch (ClassCastException cce)
                    {
                    }
                }
            }
        }

        return (i - start);
    }

    private boolean isColorMarkupStart(char[] buf, int start)
    {
        if (!isMarkup(buf[start++]))
            return false;

        return (buf[start] == 0x43);
    }

    private boolean isColorMarkupEnd(char[] buf, int start)
    {
        if (!isMarkup(buf[start++]))
            return false;

        return (buf[start] == 0x63);
    }

    /**
     * Creates a new Color (appropriate for the platform) using the given
     * component values.
     *
     * @param R
     *            the red component
     * @param G
     *            the green component
     * @param B
     *            the blue component
     * @param A
     *            the transparency component
     *
     * @return the request <code>Color</code> object
     */
    private final Color createColor(int R, int G, int B, int A)
    {
        return new DVBColor(R, G, B, A);
    }

    /**
     * Horizontal alignment setting. Valid values are:
     * <ul>
     * <li> <code>HORIZONTAL_CENTER</code>
     * <li> <code>HORIZONTAL_END_ALIGN</code>
     * <li> <code>HORIZONTAL_START_ALIGN</code>
     * </ul>
     */
    private int hAlign;

    /**
     * Vertical alignment setting. Valid values are:
     * <ul>
     * <li> <code>VERTICAL_CENTER</code>
     * <li> <code>VERTICAL_END_ALIGN</code>
     * <li> <code>VERTICAL_START_ALIGN</code>
     * </ul>
     */
    private int vAlign;

    /**
     * The starting corner setting. Valid values are:
     * <ul>
     * <li> <code>START_CORNER_LOWER_LEFT</code>
     * <li> <code>START_CORNER_LOWER_RIGHT</code>
     * <li> <code>START_CORNER_UPPER_LEFT</code>
     * <li> <code>START_CORNER_UPPER_RIGHT</code>
     * </ul>
     */
    private int startCorner;

    /**
     * Horizontal tabulation setting.
     * <p>
     * Tab characters only have meaning in left-aligned text. If the text is
     * right-aligned, centered, ore justified, then the tab character is treated
     * as a space. A tab character advances the minimum of 1 space character or
     * to the next tab stop.
     */
    private int hTab;

    /**
     * Insets used for rendering.
     * <p>
     * Text is displayed within the bounds of the given <code>HVisible</code>
     * with adjustments made for the inset boundaries.
     */
    private Insets insets;

    /**
     * The letter space setting.
     * <p>
     * This is the spacing between letters (positive or negative) in 1/256th of
     * a point.
     */
    private int letterSpace;

    /**
     * The line orientation. Valid values are:
     * <ul>
     * <li> <code>LINE_ORIENTATION_HORIZONTAL</code>
     * <li> <code>LINE_ORIENTATION_VERTICAL</code>
     * </ul>
     */
    private int lineOrient;

    /**
     * The line space setting.
     */
    private int lineSpace;

    /**
     * Whether line wrapping is on or off.
     * <p>
     * The behavior is equivalent to identifying (based on the "logical" length)
     * the first word that won't fit completely within the text object and
     * replacing the space character(s) that precedes the word with a CR
     * character. I.e., line breaks are only inserted where there are space
     * characters, this implies the receiver does not have to apply word
     * hypenation rules.
     * <p>
     * If a single word is too big, the word is truncated before the first
     * character that won't fit. (No partial characters!)
     */
    private boolean wrap;

    /**
     * Stack of <code>Color</code> markup adjustments.
     */
    private Stack colors;

    /**
     * Debug...
     */
    private static final boolean DEBUG = false;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(DVBTextLayoutManager.class.getName());

}
