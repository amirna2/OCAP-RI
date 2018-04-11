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

import java.awt.*;
import java.util.*;

import org.apache.log4j.Logger;

import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.havi.TextSupport;
import org.cablelabs.impl.havi.SizingHelper;
import org.cablelabs.impl.havi.SizingHelper.Strategy;

/**
 * The {@link org.havi.ui.HMultilineEntryLook HMultilineEntryLook} class is used
 * by the {@link org.havi.ui.HMultilineEntry HMultilineEntry} component to
 * display the entering of text. This look will be provided by the platform and
 * the exact way in which it is rendered will be platform dependent.
 *
 * <p>
 * The {@link org.havi.ui.HMultilineEntryLook HMultilineEntryLook} class draws
 * the content set on an {@link org.havi.ui.HMultilineEntry HMultilineEntry}. It
 * uses the {@link org.havi.ui.HSinglelineEntry#getTextContent
 * getTextContent(int state)} method to determine the content to render. The
 * interaction state of the {@link org.havi.ui.HMultilineEntry HMultilineEntry}
 * is ignored.
 *
 * <p>
 * This is the default look that is used by {@link org.havi.ui.HMultilineEntry
 * HMultilineEntry} and its subclasses.
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
 *
 *
 *
 * </table>
 *
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=4>None.</td>
 * </tr>
 *
 * </table>
 *
 * @see org.havi.ui.HMultilineEntry
 * @author Tom Henriksen
 * @author Aaron Kamienski (1.1 support)
 * @version 1.1
 */

public class HMultilineEntryLook extends HSinglelineEntryLook
{
    /**
     * Creates a {@link org.havi.ui.HMultilineEntryLook HMultilineEntryLook}
     * object. See the class description for details of constructor parameters
     * and default values.
     */
    public HMultilineEntryLook()
    {
    }

    /**
     * The {@link org.havi.ui.HExtendedLook#fillBackground} method paints the
     * component with its current background <code>Color</code> according to the
     * {@link org.havi.ui.HVisible#setBackgroundMode} method of
     * {@link org.havi.ui.HVisible}.
     * <p>
     * This method is only called from <code>showLook</code> within this
     * <code>HExtendedLook</code> implementation. It's not the intention to call
     * this method directly from outside of the <code>HExtendedLook</code>.
     * <p>
     * Regardless of the background mode, it is an implementation option for
     * this method to render added decorations which may affect the look's
     * transparency. This method shall not be used to render any HVisible
     * related data or content associated with the HVisible. It is purely for
     * background and decoration only.
     * <p>
     * The <code>fillBackground</code> method should not modify the clipRect
     * (clipping rectangle) of the <code>Graphics</code> object that is passed
     * to it in a way which includes any area not part of that original
     * clipRect. If any modifications are made, the original clipRect shall be
     * restored.
     * <p>
     *
     * @param g
     *            the graphics context.
     * @param visible
     *            the visible.
     * @param state
     *            the state parameter indicates the state of the visible
     * @see HLook#isOpaque
     * @since MHP 1.0.3/1.1.1
     */
    public void fillBackground(java.awt.Graphics g, HVisible visible, int state)
    {
        super.fillBackground(g, visible, state);
    }

    /**
     * The {@link org.havi.ui.HExtendedLook#renderBorders} method paints any
     * implementation specific borders according to the
     * {@link org.havi.ui.HVisible#setBordersEnabled} method of
     * {@link org.havi.ui.HVisible}. If borders are drawn, the border decoration
     * shall be rendered within the border area as returned by
     * <code>getInsets</code>. The behavior of this method, when a subclass
     * overrides the method <code>getInsets</code> is undefined, except that it
     * will never draw outside the border area as returned by
     * <code>getInsets</code>.
     * <p>
     * This method is only called from <code>showLook</code> within this
     * <code>HExtendedLook</code> implementation. It's not the intention to call
     * this method directly from outside of the <code>HExtendedLook</code>.
     * <p>
     * The {@link org.havi.ui.HExtendedLook#renderBorders} method should not
     * modify the clipRect (clipping rectangle) of the <code>Graphics</code>
     * object that is passed to it in a way which includes any area not part of
     * that original clipRect. If any modifications are made, the original
     * clipRect shall be restored.
     * <p>
     *
     * @param g
     *            the graphics context.
     * @param visible
     *            the visible.
     * @param state
     *            the state parameter indicates the state of the visible
     * @since MHP 1.0.3/1.1.1
     */
    public void renderBorders(java.awt.Graphics g, HVisible visible, int state)
    {
        super.renderBorders(g, visible, state);
    }

    /**
     * The {@link org.havi.ui.HExtendedLook#renderVisible} method paints any
     * content or other data associated with the look's HVisible. This method
     * shall not be used to render a background nor any other decoration. Its
     * purpose is purely to render content or other value data stored on the
     * HVisible. This may be set via HVisible methods such as
     * <code>setTextContent</code> and <code>setGraphicContent</code>. Rendering
     * shall take place within the bounds returned by the <code>getInsets</code>
     * method.
     * <p>
     * This method is only called from <code>showLook</code> within this
     * <code>HExtendedLook</code> implementation. It's not the intention to call
     * this method directly from outside of the <code>HExtendedLook</code>.
     * <p>
     * For looks which draw content (e.g. {@link org.havi.ui.HTextLook},
     * {@link org.havi.ui.HGraphicLook} and {@link org.havi.ui.HAnimateLook}),
     * if no content is associated with the component, this method does nothing.
     * <p>
     * The {@link org.havi.ui.HExtendedLook#renderVisible} method should not
     * modify the clipRect (clipping rectangle) of the <code>Graphics</code>
     * object that is passed to it in a way which includes any area not part of
     * that original clipRect. If any modifications are made, the original
     * clipRect shall be restored.
     * <p>
     *
     * @param g
     *            the graphics context.
     * @param visible
     *            the visible.
     * @param state
     *            the state parameter indicates the state of the visible
     * @since MHP 1.0.3/1.1.1
     */
    public void renderVisible(java.awt.Graphics g, HVisible visible, int state)
    {
        if (!NEW_CODE)
        { // old
            HMultilineEntry mle;
            String content = null;
            Dimension d;
            MultilineEntryMetrics mets = null;

            if ((g != null) && (visible instanceof HMultilineEntry))
            {

                mle = (HMultilineEntry) visible;
                d = mle.getSize();

                mets = getMetrics(visible);
                g.setFont(mle.getFont());
                g.setColor(mle.getForeground());

                // Display the actual text.
                content = mets.displayText(g, mle);

                // We only paint our "Caret" if this component is in edit mode.
                if (mle.getEditMode() == true)
                {
                    int caretLine = mets.calcCaretLine();
                    int baseY = mets.visibleBaseLine(caretLine);
                    int beginChar = 0, endChar = 0;
                    int offset = mets.xDisplayOffset;

                    if ((content != null) && (content.length() > 0))
                    {
                        beginChar = ((Integer) mets.textOffsets.elementAt(caretLine)).intValue();
                        endChar = beginChar + (mle.getCaretCharPosition() - beginChar);
                        offset += mets.fontMetrics.stringWidth(content.substring(beginChar, endChar));
                    }
                    g.drawLine(offset, baseY + mets.maxDescent - 1, offset, baseY - mets.stringHeight - mets.maxDescent
                            + 1);
                }
            }
        }
        else
        { // new
            HMultilineEntry mle = (HMultilineEntry) visible;
            Dimension d = visible.getSize();
            Insets insets = getInsets(visible);
            Rectangle bounds = new Rectangle(insets.left, insets.top, d.width - insets.left - insets.right, d.height
                    - insets.top - insets.bottom);

            Font font = mle.getFont();
            g.setFont(font);
            g.setColor(mle.getForeground());

            int y = 0;
            FontMetrics metrics = g.getFontMetrics();
            int padding = metrics.getMaxDescent();
            int vFill = 0, vSpacing = 0, vXtra = 0;
            final int fontHeight = TextSupport.getFontHeight(metrics);
            final int fontAscent = TextSupport.getFontAscent(metrics);
            bounds.x += padding;
            bounds.y += padding;
            bounds.width -= padding * 2;
            bounds.height -= padding * 2;

            // ****start DIFFERENT FROM SLELook
            Data data = getData(mle, font, metrics, bounds.getSize());
            int lineCount = getNumVisible(mle, fontHeight, data);
            // ****end DIFFERENT FROM SLELook

            // Figure starting baseline y coordinate
            int vAlign = mle.getVerticalAlignment();
            int hAlign = mle.getHorizontalAlignment();
            switch (vAlign)
            {
                case HVisible.VALIGN_JUSTIFY:
                    // ****start DIFFERENT FROM SLELook
                    vFill = bounds.height - lineCount * fontHeight;
                    if (vFill > 0)
                    {
                        vSpacing = (lineCount > 1) ? (vFill / (lineCount - 1)) : 1;
                        // I don't believe it's possible...
                        // vSpacing will be >= fontHeight
                        if (vSpacing < 1) vSpacing = 1;
                        if (lineCount > 1) vXtra = vFill - (vSpacing * (lineCount - 1));
                    }
                    // ****end DIFFERENT FROM SLELook
                    // fall through for y calculation
                case HVisible.VALIGN_TOP:
                    y = fontAscent + padding;
                    break;
                case HVisible.VALIGN_BOTTOM:
                    y = bounds.height - metrics.getDescent() - padding;
                    break;
                case HVisible.VALIGN_CENTER:
                    if (lineCount == 1)
                        y = (bounds.height + fontAscent) / 2;
                    else
                        y = (bounds.height - lineCount * fontHeight) / 2 + fontAscent + padding;
                    break;
            }
            y += bounds.y;

            // ****start DIFFERENT FROM SLELook
            final CaretPos caret = getCaretPos(mle, data);
            final int caretLine = caret.line;
            final int caretOfs = caret.ofs;
            // ****end DIFFERENT FROM SLELook

            // Figure and draw each line's h coordinate
            // ****start DIFFERENT FROM SLELook
            final int scroll = getScrollPosition(mle, data, caret, lineCount);
            int lineno = (vAlign != HVisible.VALIGN_BOTTOM) ? scroll : scroll + lineCount - 1;
            for (int i = 0; i < lineCount; ++i)
            {
                String line;
                line = data.getLine(lineno).line;

                // Render the line
                renderLine(line, g, bounds.x, y, bounds.width, hAlign, metrics, mle.getEditMode(),
                        (lineno == caretLine) ? caretOfs : -1);

                // Update vertical location
                switch (vAlign)
                {
                    case HVisible.VALIGN_CENTER:
                    case HVisible.VALIGN_TOP:
                    default:
                        ++lineno;
                        y += fontHeight;
                        break;
                    case HVisible.VALIGN_BOTTOM:
                        --lineno;
                        y -= fontHeight;
                        break;
                    case HVisible.VALIGN_JUSTIFY:
                        ++lineno;
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
            // ****end DIFFERENT FROM SLELook

            if (DEBUG)
            {
                g.setColor(Color.red);
                g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
            }
        }

        return;
    }

    /**
     * The {@link org.havi.ui.HExtendedLook#showLook showLook} method is the
     * entry point for repainting the entire {@link org.havi.ui.HVisible}
     * component. This method delegates the responsibility of drawing the
     * component background, borders and any <code>HVisible</code> related data
     * or content to the <code>fillBackground</code>, <code>renderVisible</code>
     * and <code>renderBorders</code> methods respectively, subject to the
     * clipping rectangle of the Graphics object passed to it. This method shall
     * call the methods <code>fillBackground</code>, <code>renderVisible</code>,
     * and <code>renderBorders</code> in that order and shall not do any
     * rendering itself.
     * <p>
     * The {@link org.havi.ui.HExtendedLook#showLook showLook} method should not
     * modify the clipRect (clipping rectangle) of the <code>Graphics</code>
     * object that is passed to it in a way which includes any area not part of
     * that original clipRect. If any modifications are made, the original
     * clipRect shall be restored.
     * <p>
     * Any resources <b>explicitly</b> associated with an
     * {@link org.havi.ui.HExtendedLook} should be loaded by the
     * {@link org.havi.ui.HExtendedLook} during its creation, etc. Note that the
     * &quot;standard&quot; looks don't load content by default.
     * <p>
     * This method is called from the {@link org.havi.ui.HVisible#paint} method
     * of {@link org.havi.ui.HVisible} and must never be called from elsewhere.
     * Components wishing to redraw themselves should call their repaint method
     * in the usual way.
     *
     * @param g
     *            the graphics context.
     * @param visible
     *            the visible.
     * @param state
     *            the state parameter indicates the state of the visible,
     *            allowing the look to render the appropriate content for that
     *            state. Note that some components (e.g. HStaticRange, HRange,
     *            HRangeValue) do not use state-based content.
     */
    public void showLook(java.awt.Graphics g, HVisible visible, int state)
    {
        // Draw the background
        fillBackground(g, visible, state);

        // Render Content
        renderVisible(g, visible, state);

        // Draw border decorations
        renderBorders(g, visible, state);

        return;
    }

    /**
     * Called by the {@link org.havi.ui.HVisible HVisible} whenever its content,
     * state, or any other data changes. See the class description of
     * {@link org.havi.ui.HVisible HVisible} for more information about the
     * <code>changes</code> parameter.
     * <p>
     * The implementation of this method should work out which graphical areas
     * of the {@link org.havi.ui.HVisible HVisible} have changed and make any
     * relevant calls to trigger the repainting of those areas.
     * <p>
     * A minimum implementation of this method could simply call
     *
     * <pre>
     * visible.repaint()
     * </pre>
     *
     * @param visible
     *            the {@link org.havi.ui.HVisible HVisible} which has changed
     * @param changes
     *            an array containing hint data and associated hint objects. If
     *            this argument is <code>null</code> a full repaint will be
     *            triggered.
     */
    public void widgetChanged(HVisible visible, HChangeData[] changes)
    {
        if (!NEW_CODE)
        { // old
            MultilineEntryMetrics mets = getMetrics(visible);

            if (changes != null)
            {
                for (int x = 0; x < changes.length; x++)
                {
                    HChangeData change = changes[x];

                    switch (change.hint)
                    {
                        case HVisible.TEXT_CONTENT_CHANGE:
                            mets.textChange = true;
                            break;
                        case HVisible.CARET_POSITION_CHANGE:
                        case HVisible.ECHO_CHAR_CHANGE:
                        case HVisible.EDIT_MODE_CHANGE:
                        default:
                            break;
                    }
                }
            }
        }
        else
        { // new
            for (int x = 0; x < changes.length; x++)
            {
                HChangeData change = changes[x];

                switch (change.hint)
                {
                    case HVisible.TEXT_CONTENT_CHANGE:
                    case HVisible.ECHO_CHAR_CHANGE:
                        Data data = getData0((HMultilineEntry) visible);
                        if (data != null)
                        {
                            data.notValid = true;
                            data.content = null;
                        }
                        break;
                    case HVisible.CARET_POSITION_CHANGE:
                    case HVisible.EDIT_MODE_CHANGE:
                    default:
                        break;
                }
            }
        }
        super.widgetChanged(visible, changes);
    }

    /**
     * Gets the minimum size of the {@link org.havi.ui.HVisible HVisible}
     * component when drawn with this {@link org.havi.ui.HLook HLook}.
     * <p>
     * This size may be determined in several ways depending on the information
     * available to the look. These steps are performed in order and the first
     * available result is returned. For the purposes of this algorithm
     * {@link org.havi.ui.HLook HLook} classes that do not use content (e.g.
     * {@link org.havi.ui.HRangeLook HRangeLook}) are treated as if no content
     * was present.
     * <p>
     * The extra space required for border decoration can be determined from the
     * {@link org.havi.ui.HLook#getInsets getInsets} method.
     * <p>
     * <ol>
     *
     * <li>If this look is an {@link org.havi.ui.HTextLook HTextLook} and if
     * {@link org.havi.ui.HVisible#getTextLayoutManager
     * HVisible.getTextLayoutManager()} returns an
     * {@link org.havi.ui.HDefaultTextLayoutManager HDefaultTextLayoutManager},
     * then this method should delegate the call to its
     * {@link org.havi.ui.HDefaultTextLayoutManager#getMinimumSize
     * getMinimumSize()} method plus any additional dimensions that the HLook
     * requires for border decoration etc. If the HDefaultTextLayoutManager
     * returns a zero size, then proceed with the following steps.
     * <li>If the HLook supports the scaling of its content (e.g. an
     * HGraphicLook) and scaling is requested and content is set, then the
     * return value is a size containing the width of the narrowest content and
     * the height of the shortest content plus any additional dimensions that
     * the HLook requires for border decoration etc.
     * <li>If the {@link org.havi.ui.HLook HLook} does not support scaling of
     * content or no scaling is requested, <em>and</em> content is set then the
     * return value is a size sufficiently large to hold each piece of content
     * plus any additional dimensions that the HLook requires for border
     * decoration etc.
     * <li>If no content is available but a default preferred size has been set
     * using {@link org.havi.ui.HVisible#setDefaultSize setDefaultSize} has been
     * called to set then the return value is this value (as obtained with
     * {@link org.havi.ui.HVisible#getDefaultSize getDefaultSize}) plus any
     * additional dimensions that the HLook requires for border decoration etc.
     * <li>If there is no content or default size set then the return value is
     * an implementation-specific minimum size plus any additional dimensions
     * that the HLook requires for border decoration etc.
     * </ol>
     *
     * @param hvisible
     *            {@link org.havi.ui.HVisible HVisible} to which this
     *            {@link org.havi.ui.HLook HLook} is attached.
     * @return A dimension object indicating this {@link org.havi.ui.HLook
     *         HLook's} minimum size.
     * @see org.havi.ui.HVisible#getMinimumSize
     */
    public Dimension getMinimumSize(HVisible hvisible)
    {
        return SizingHelper.getMinimumSize(hvisible, this, strategy);
    }

    /**
     * Gets the preferred size of the {@link org.havi.ui.HVisible HVisible}
     * component when drawn with this {@link org.havi.ui.HLook HLook}.
     * <p>
     * This size may be determined in several ways depending on the information
     * available to the look. These steps are performed in order and the first
     * available result is returned. For the purposes of this algorithm
     * {@link org.havi.ui.HLook HLook} classes that do not use content (e.g.
     * {@link org.havi.ui.HRangeLook HRangeLook}) are treated as if no content
     * was present.
     * <p>
     * The extra space required for border decoration can be determined from the
     * {@link org.havi.ui.HLook#getInsets getInsets} method.
     * <p>
     * <ol>
     * <li>If a default preferred size has been set for this
     * {@link org.havi.ui.HVisible HVisible} (using
     * {@link org.havi.ui.HVisible#setDefaultSize setDefaultSize}) then the
     * return value is this size (obtained with
     * {@link org.havi.ui.HVisible#getDefaultSize getDefaultSize}) plus any
     * additional dimensions that the HLook requires for border decoration etc.
     * <li>If this look is an {@link org.havi.ui.HTextLook HTextLook} and if a
     * default preferred size has not been set and
     * {@link org.havi.ui.HVisible#getTextLayoutManager
     * HVisible.getTextLayoutManager()} returns an
     * {@link org.havi.ui.HDefaultTextLayoutManager HDefaultTextLayoutManager},
     * then this method should delegate the call to its
     * {@link org.havi.ui.HDefaultTextLayoutManager#getPreferredSize
     * getPreferredSize()} method plus any additional dimensions that the HLook
     * requires for border decoration etc. If the HDefaultTextLayoutManager
     * returns a zero size, then proceed with the following steps.
     * <li>If this {@link org.havi.ui.HLook HLook} does not support scaling of
     * content or no scaling is requested, and content is present then the
     * return value is a size that is sufficiently large to hold each piece of
     * content plus any additional dimensions that the HLook requires for border
     * decoration etc.
     * <li>If this {@link org.havi.ui.HLook HLook} supports the scaling of its
     * content (e.g. an {@link org.havi.ui.HGraphicLook HGraphicLook}) and
     * content is set then the return value is the current size of the
     * {@link org.havi.ui.HVisible HVisible} as returned by
     * {@link org.havi.ui.HVisible#getSize getSize}).
     * <li>If there is no content and no default size set then the return value
     * is the current size of the {@link org.havi.ui.HVisible HVisible} as
     * returned by {@link org.havi.ui.HVisible#getSize getSize}).
     * </ol>
     * <p>
     * If a default preferred size has been set for this <code>HVisible</code>
     * (using <code>setDefaultSize()</code>) and the default preferred size has
     * a <code>NO_DEFAULT_WIDTH</code> then the return value is a
     * <code>Dimension</code> with this height (obtained with
     * <code>getDefaultSize()</code>) and the preferred width for the content
     * plus any additional dimensions that the <code>HLook</code> requires for
     * border decoration etc.
     * <p>
     * If a default preferred size has been set for this <code>HVisible</code>
     * (using <code>setDefaultSize()</code>) and the default preferred size has
     * a <code>NO_DEFAULT_HEIGHT</code> then the return value is a
     * <code>Dimension</code> with this width (obtained with
     * <code>getDefaultSize()</code>) and the preferred height for the content
     * plus any additional dimensions that the <code>HLook</code> requires for
     * border decoration etc.
     *
     * @param hvisible
     *            {@link org.havi.ui.HVisible HVisible} to which this
     *            {@link org.havi.ui.HLook HLook} is attached.
     * @return A dimension object indicating the preferred size of the
     *         {@link org.havi.ui.HVisible HVisible} when drawn with this
     *         {@link org.havi.ui.HLook HLook}.
     * @see org.havi.ui.HVisible#getPreferredSize
     * @see org.havi.ui.HVisible#setDefaultSize
     */
    public Dimension getPreferredSize(HVisible hvisible)
    {
        return SizingHelper.getPreferredSize(hvisible, this, strategy);
    }

    /**
     * Gets the maximum size of the {@link org.havi.ui.HVisible HVisible}
     * component when drawn with this {@link org.havi.ui.HLook HLook}.
     * <p>
     * This size may be determined in several ways depending on the information
     * available to the look. These steps are performed in order and the first
     * available result is returned. For the purposes of this algorithm
     * {@link org.havi.ui.HLook HLook} classes that do not use content (e.g.
     * {@link org.havi.ui.HRangeLook HRangeLook}) are treated as if no content
     * was present.
     * <p>
     * The extra space required for border decoration can be determined from the
     * {@link org.havi.ui.HLook#getInsets getInsets} method.
     * <p>
     * <ol>
     * <li>If this look is an {@link org.havi.ui.HTextLook HTextLook} and if
     * {@link org.havi.ui.HVisible#getTextLayoutManager
     * HVisible.getTextLayoutManager()} returns an
     * {@link org.havi.ui.HDefaultTextLayoutManager HDefaultTextLayoutManager},
     * then this method should delegate the call to its
     * {@link org.havi.ui.HDefaultTextLayoutManager#getMaximumSize
     * getMaximumSize()} method plus any additional dimensions that the HLook
     * requires for border decoration etc. If the HDefaultTextLayoutManager
     * returns a zero size, then proceed with the following steps.
     * <li>If the {@link org.havi.ui.HLook HLook} supports the scaling of its
     * content (e.g. an {@link org.havi.ui.HGraphicLook HGraphicLook}) then the
     * return value is the current size of the {@link org.havi.ui.HVisible
     * HVisible} (as returned by {@link org.havi.ui.HVisible#getSize
     * HVisible#getSize}).
     * <li>If the {@link org.havi.ui.HLook HLook} does not support scaling of
     * content or no scaling is requested, and content is set then the return
     * value is a size sufficiently large to hold each piece of content plus any
     * additional dimensions that the HLook requires for border decoration etc.
     * <li>If there is no content set then a maximum size of
     * <code>[Short.MAX_VALUE, Short.MAX_VALUE]</code> is returned as a
     * Dimension.
     * </ol>
     *
     * @param hvisible
     *            {@link org.havi.ui.HVisible HVisible} to which this
     *            {@link org.havi.ui.HLook HLook} is attached.
     * @return A dimension object indicating this {@link org.havi.ui.HLook
     *         HLook's} maximum size.
     * @see org.havi.ui.HVisible#getMaximumSize
     */
    public Dimension getMaximumSize(HVisible hvisible)
    {
        return SizingHelper.getMaximumSize(hvisible, this, strategy);
    }

    /**
     * Returns true if the entire painted area of the
     * {@link org.havi.ui.HVisible HVisible} when using this look is fully
     * opaque, i.e. the {@link org.havi.ui.HLook#showLook showLook} method
     * guarantees that all pixels are painted in an opaque Color.
     * <p>
     * The default value is implementation specific and depends on the
     * background painting mode of the given {@link org.havi.ui.HVisible
     * HVisible}. The consequences of an invalid overridden value are
     * implementation specific.
     *
     * @param visible
     *            the visible to test
     * @return true if all the pixels with the java.awt.Component#getBounds
     *         method of an {@link org.havi.ui.HVisible HVisible} using this
     *         look are fully opaque, i.e. the
     *         {@link org.havi.ui.HLook#showLook showLook} method guarantees
     *         that all pixels are painted in an opaque Color, otherwise false.
     */
    public boolean isOpaque(HVisible visible)
    {
        return super.isOpaque(visible);
    }

    /**
     * Determines the insets of this {@link org.havi.ui.HLook HLook}, which
     * indicate the size of the border. This area is reserved for the
     * {@link org.havi.ui.HLook HLook} to use for drawing borders around the
     * associated {@link org.havi.ui.HVisible HVisible}.
     *
     * @param hvisible
     *            {@link org.havi.ui.HVisible HVisible} to which this
     *            {@link org.havi.ui.HLook HLook} is attached.
     * @return the insets of this {@link org.havi.ui.HLook HLook}.
     */
    public java.awt.Insets getInsets(HVisible visible)
    {
        return super.getInsets(visible);
    }

    /**
     * Returns the character position of the caret within the content string if
     * it were to be moved down one line.
     *
     * @param visible
     *            a multiline text entry component.
     *            <p>
     *            Note that if this component is not actually using this
     *            {@link org.havi.ui.HLook HLook} to display itself the return
     *            value of this method will probably be wrong. Application
     *            authors should take care to only call this method with
     *            components which are using this instance of the
     *            {@link org.havi.ui.HMultilineEntryLook HMultilineEntryLook}.
     * @return the caret position (as defined in
     *         {@link org.havi.ui.HSinglelineEntry#getCaretCharPosition
     *         HSinglelineEntry#getCaretCharPosition}) to move the caret to if
     *         it were moved down one line, or the nearest position if this is
     *         not possible.
     */
    public int getCaretPositionNextLine(HVisible visible)
    {
        if (!NEW_CODE)
        { // old
            MultilineEntryMetrics mets = getMetrics(visible);

            int newCharPos = mets.mle.getCaretCharPosition();
            int charPos = getCharPosOnCurrentLine(visible);
            int caretLine = mets.calcCaretLine();

            if ((caretLine + 1) < mets.textOffsets.size())
                newCharPos = setCaretPosOnLine(visible, caretLine + 1, charPos);

            return newCharPos;
        }
        else
        { // new
            HMultilineEntry mle = (HMultilineEntry) visible;
            Data data = getData(mle);
            CaretPos caret = getCaretPos(mle, data);
            int line = caret.line;
            if (line + 1 < data.getLineCount()) ++line;
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getCaretNext of " + caret.line + " " + caret.ofs + " -> " + line);
                }
                data.dump();
            }
            return getCaretCharPositionForLine(mle, data, line);
        }
    }

    /**
     * Returns the character position of the caret within the content string if
     * it were to be moved up one line.
     *
     * @param visible
     *            a multiline text entry component.
     *            <p>
     *            Note that if this component is not actually using this
     *            {@link org.havi.ui.HLook HLook} to display itself the return
     *            value of this method will probably be wrong. Application
     *            authors should take care to only call this method with
     *            components which are using this instance of the
     *            {@link org.havi.ui.HMultilineEntryLook HMultilineEntryLook}.
     * @return the caret position (as defined in
     *         {@link org.havi.ui.HSinglelineEntry#getCaretCharPosition
     *         HSinglelineEntry#getCaretCharPosition}) to move the caret to if
     *         it were moved up one line, or the nearest position if this is not
     *         possible.
     */
    public int getCaretPositionPreviousLine(HVisible visible)
    {
        if (!NEW_CODE)
        { // old
            MultilineEntryMetrics mets = getMetrics(visible);

            int newCharPos = mets.mle.getCaretCharPosition();
            int charPos = getCharPosOnCurrentLine(visible);
            int caretLine = mets.calcCaretLine();

            if ((caretLine - 1) >= 0) newCharPos = setCaretPosOnLine(visible, caretLine - 1, charPos);

            return newCharPos;
        }
        else
        { // new
            HMultilineEntry mle = (HMultilineEntry) visible;
            Data data = getData(mle);
            CaretPos caret = getCaretPos(mle, data);
            int line = caret.line;
            if (line > 0) --line;
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getCaretPrevious of " + caret.line + " " + caret.ofs + " -> " + line);
                }
                data.dump();
            }
            return getCaretCharPositionForLine(mle, data, line);
        }
    }

    /**
     * Returns the character position of the caret within the content string if
     * it were to be moved vertically to the given 'line'. A line is identified
     * by its first character as obtained from the getSoftLineBreakPositions()
     * method. If an invalid line is specified an
     * <code>IllegalArgumentException</code> is thrown. If the caret cannot be
     * moved to the same column position on this line, the nearest position
     * should be returned.
     *
     * @param visible
     *            a multiline text entry component.
     *            <p>
     *            Note that if this component is not actually using this
     *            {@link org.havi.ui.HLook HLook} to display itself the return
     *            value of this method will probably be wrong. Application
     *            authors should take care to only call this method with
     *            components which are using this instance of the
     *            {@link org.havi.ui.HMultilineEntryLook HMultilineEntryLook}.
     * @param line
     *            the line number for which the caret position is requested
     * @return the caret position (as defined in
     *         {@link org.havi.ui.HSinglelineEntry#getCaretCharPosition
     *         HSinglelineEntry#getCaretCharPosition}) to move the caret to if
     *         it were moved to this line.
     *
     * @see HSinglelineEntry#getCaretCharPosition
     * @see HMultilineEntryLook#getSoftLineBreakPositions
     */
    public int getCaretCharPositionForLine(HVisible visible, int line)
    {
        if (!NEW_CODE)
        {// old
            MultilineEntryMetrics mets = getMetrics(visible);

            int newCharPos = mets.mle.getCaretCharPosition();
            int charPos = getCharPosOnCurrentLine(visible);

            if ((line >= 0) && (line < mets.textOffsets.size()))
                newCharPos = setCaretPosOnLine(visible, line, charPos);
            else
                throw new IllegalArgumentException("The line number is out of range.");

            return newCharPos;
        }
        else
        {// new
            HMultilineEntry mle = (HMultilineEntry) visible;
            Data data = getData(mle);
            int pos = getCaretCharPositionForLine(mle, data, line);

            if (pos < 0) throw new IllegalArgumentException("The line number is out of range.");
            return pos;
        }
    }

    /**
     * Returns the starting positions of lines currently shown within the
     * HVisible. Lines are identified by the positions of those characters that
     * start on a new 'line' (including those following \n), in order, from the
     * first visible line to the last. This method can be used to calculate the
     * number of visible lines currently shown in the look as well as the line
     * shown at the top of the visible window.
     *
     * @param visible
     *            a multiline text entry component.
     * @return the starting positions of the lines currently shown within the
     *         <code>HVisible</code>. If there is no text content within the
     *         <code>HVisible</code>, a zero length array shall be returned.
     */
    public int[] getVisibleSoftLineBreakPositions(HVisible visible)
    {
        if (!NEW_CODE)
        { // old
            int index = 0;
            MultilineEntryMetrics mets = getMetrics(visible);
            int size = Math.min(mets.visibleLines, mets.textOffsets.size());

            int[] lineBreaks = new int[size];

            for (int x = mets.topVisibleLine; x < (mets.topVisibleLine + size); x++)
                lineBreaks[index++] = ((Integer) mets.textOffsets.elementAt(x)).intValue();
            return lineBreaks;
        }
        else
        { // new
            HMultilineEntry mle = (HMultilineEntry) visible;
            Data data = getData(mle);

            // Figure number of visible lines
            int n = getNumVisible(mle, TextSupport.getFontHeight(data.fm), data);
            // Figure scroll position
            int scroll = getScrollPosition(mle, data, getCaretPos(mle, data), n);
            // Modify n to be number actually displayed
            n = Math.min(n, data.getLineCount() - scroll);

            int[] lineBreaks = new int[n];
            for (int i = 0; i < lineBreaks.length; ++i)
                lineBreaks[i] = data.getLine(i + scroll).offset;

            return lineBreaks;
        }
    }

    /**
     * Returns the positions within the content string of all those characters
     * that start on a new 'line' (including those following \n), in order from
     * the first line to the last, including the line starting at 0.
     *
     * @param visible
     *            a multiline text entry component.
     *            <p>
     *            Note that if this component is not actually using this
     *            {@link org.havi.ui.HLook HLook} to display itself the return
     *            value of this method will probably be wrong. Application
     *            authors should take care to only call this method with
     *            components which are using this instance of the
     *            {@link org.havi.ui.HMultilineEntryLook HMultilineEntryLook}.
     * @return the positions of 'soft line breaks' introduced by the HLook. If
     *         there is no text content within the <code>HVisible</code>, a zero
     *         length array shall be returned.
     */
    public int[] getSoftLineBreakPositions(HVisible visible)
    {
        if (!NEW_CODE)
        { // old
            MultilineEntryMetrics mets = getMetrics(visible);
            int size = mets.textOffsets.size();

            int[] lineBreaks = new int[size];

            for (int x = 0; x < size; x++)
                lineBreaks[x] = ((Integer) mets.textOffsets.elementAt(x)).intValue();
            return lineBreaks;
        }
        else
        { // new
            HMultilineEntry mle = (HMultilineEntry) visible;
            Data data = getData(mle);

            // Create array of lines, starting at 0 position
            int[] lineBreaks = new int[data.getLineCount()];
            for (int i = 0; i < lineBreaks.length; ++i)
                lineBreaks[i] = data.getLine(i).offset;

            return lineBreaks;
        }
    }

    private MultilineEntryMetrics getMetrics(HVisible visible)
    {
        if (NEW_CODE)
            return null;
        else
        {
            MultilineEntryMetrics mets = null;
            // Retrieve the stored calculations from the HMultilineEntry.
            // If they don't exist or the object type is incorrect,
            // allocate space for new stored calculations.
            mets = (MultilineEntryMetrics) visible.getLookData(HMultilineEntryLook.this);

            try
            {
                if ((mets == null) || !(mets instanceof MultilineEntryMetrics))
                {
                    mets = new MultilineEntryMetrics((HMultilineEntry) visible);
                    visible.setLookData(this, mets);
                }
            }
            catch (Exception e)
            {
            }

            return mets;
        }
    }

    private int getCharPosOnCurrentLine(HVisible visible)
    {
        MultilineEntryMetrics mets = getMetrics(visible);

        return mets.mle.getCaretCharPosition()
                - ((Integer) mets.textOffsets.elementAt(mets.calcCaretLine())).intValue();
    }

    private int setCaretPosOnLine(HVisible visible, int line, int lineCharPos)
    {
        MultilineEntryMetrics mets = getMetrics(visible);

        int newLineStart = ((Integer) mets.textOffsets.elementAt(line)).intValue();
        // Calculate the line length in this manner, because if the line ends
        // with
        // a newline, the actual text will not contain that newline, but the
        // stored
        // line length will reflect that newline. We want to calculate based on
        // the text the user can actually see with his eyes.
        int newLineLength = Math.min(((Integer) mets.lineWidths.elementAt(line)).intValue(),
                ((String) mets.lines.elementAt(line)).length());

        int newCharPos = newLineStart + lineCharPos;
        if (newCharPos > (newLineStart + newLineLength)) newCharPos = newLineStart + newLineLength;

        return newCharPos;
    }

    static Dimension calcHeightFromWidth(HMultilineEntry mle, final Dimension d)
    {

        // Create a metrics and plug in our own dimensions for the
        // calculateMetrics method.
        MultilineEntryMetrics mets = new MultilineEntryMetrics(mle)
        {
            public void calculateMetrics(Dimension size)
            {
                super.calculateMetrics(d);
            }
        };
        int padding = mets.fontMetrics.getMaxDescent() * 2;

        // The width is the same one that was passed in. Based on that width,
        // the text was wrapped. After wrapping, we know how many lines we have.
        // Calculate the height as font height * number of lines and then add in
        // the border decorations and fudge factor.
        return new Dimension(d.width, (mets.stringHeight + mets.fontMetrics.getMaxDescent()) * mets.lines.size()
                + padding);
    }

    /**
     * Returns the caret position of the first character for the specified line.
     * FOR TEST PURPOSES ONLY.
     */
    /*
     * int _getTextOffset(HVisible visible, int line) { MultilineEntryMetrics
     * mets = getMetrics(visible);
     *
     * return ((line > 0) && (line < mets.textOffsets.size())) ? ((Integer)
     * mets.textOffsets.elementAt(line)).intValue() : -1; }
     */

    /**
     * Returns the character width for the specified line. FOR TEST PURPOSES
     * ONLY.
     */
    /*
     * int _getLineWidth(HVisible visible, int line) { MultilineEntryMetrics
     * mets = getMetrics(visible);
     *
     * return ((line > 0) && (line < mets.lineWidths.size())) ? ((Integer)
     * mets.lineWidths.elementAt(line)).intValue() : -1; }
     */

    /**
     * Implements the <code>SizingHelper.Strategy</code> interface to provide
     * the <code>SizingHelper</code> methods with information about the content
     * to be sized by this look.
     */
    private static final Strategy strategy = new Strategy()
    {

        /**
         * Returns whether the given <code>hvisible</code> has any
         * {@link HVisible#getGraphicContent(int)} or not.
         *
         * @return <code>hvisible.getGraphicContent(i) != null</code> for all
         *         <code>i</code>
         */
        public boolean hasContent(HVisible hvisible)
        {
            return hvisible.getTextContent(HState.NORMAL_STATE) != null;
        }

        /**
         * Calculates the largest dimensions of all content.
         *
         * @param the
         *            <code>HVisible</code> to query for content
         * @return the largest dimensions of all content.
         */
        public Dimension getMaxContentSize(HVisible hvisible)
        {
            String content = hvisible.getTextContent(HState.NORMAL_STATE);
            if (content == null) content = "";

            Font f = hvisible.getFont();
            if (f == null) f = HaviToolkit.getToolkit().getDefaultFont();
            FontMetrics metrics = hvisible.getFontMetrics(f);
            int padding = metrics.getMaxDescent() * 2;
            HMultilineEntry mle = (HMultilineEntry) hvisible;

            try
            {
                Insets insets = mle.getLook().getInsets(mle);

                // Lower case 'a' is a character of average width, isn't it?
                int testWidth = (metrics.charWidth('a') * HaviToolkit.getToolkit().getMaxCharsPerLine(hvisible))
                        + padding + (insets.left + insets.right);
                int testHeight = TextSupport.getFontHeight(metrics) + padding + (insets.top + insets.bottom);

                Dimension d = calcHeightFromWidth(mle, new Dimension(testWidth, testHeight));

                // Remove insets (not part of max content size)
                d.width -= insets.left + insets.right;

                return d;
            }
            catch (Exception e)
            {
                return new Dimension((metrics.getMaxAdvance() * HaviToolkit.getToolkit().getMaxCharsPerLine(hvisible))
                        + padding, TextSupport.getFontHeight(metrics) * 2 + padding);
            }
        }

        /**
         * Calculates the smallest dimensions of all content. This is the same
         * as the largest dimension of all content since there is only one piece
         * of content.
         *
         * @param the
         *            <code>HVisible</code> to query for content
         * @return the smallest dimensions of all content.
         */
        public Dimension getMinContentSize(HVisible hvisible)
        {
            return getMaxContentSize(hvisible);
        }

        /**
         * Returns whether the <code>HLook</code> in question supports sizing of
         * content or not.
         *
         * @return <code>false</code>
         */
        public boolean supportsScaling()
        {
            return false;
        }
    };

    static class MultilineEntryMetrics
    {
        HSinglelineEntry mle;

        int stringHeight = 0;

        FontMetrics fontMetrics = null;

        int maxDescent = 0;

        Dimension displayArea = new Dimension();

        boolean textChange = true;

        int xDisplayOffset;

        int yDisplayOffset;

        int visibleLines;

        int topVisibleLine = 0;

        Vector textOffsets = new Vector();

        Vector lineWidths = new Vector();

        Vector lines = new Vector();

        /**
         *
         * Creates a <code>MultilineEntryMetrics</code> object.
         *
         * @param mle
         *            org.havi.ui.HMultilineEntry
         */
        public MultilineEntryMetrics(HMultilineEntry mle)
        {
            this.mle = mle;
            textOffsets.insertElementAt(new Integer(0), 0);
            lineWidths.insertElementAt(new Integer(0), 0);
        }

        /**
         * Calculates some simple metrics not related to the actual content.
         */
        public void calculateMetrics(Dimension size)
        {

            if ((mle.getFont() != null) && (size != null))
            {

                fontMetrics = mle.getFontMetrics(mle.getFont());

                // calculate some things that only change with new fontMetrics:
                stringHeight = TextSupport.getFontHeight(fontMetrics);
                maxDescent = fontMetrics.getMaxDescent();

                HLook hlook = mle.getLook();

                if (hlook != null)
                {
                    Insets insets = hlook.getInsets(mle);

                    xDisplayOffset = insets.left + maxDescent;
                    yDisplayOffset = insets.top + maxDescent;

                    // we now have all the information needed to do the
                    // calculations
                    displayArea.setSize(size.width - ((insets.left + insets.right) + (maxDescent * 2)), size.height
                            - ((insets.top + insets.bottom) + (maxDescent * 2)));

                    visibleLines = displayArea.height / (stringHeight + maxDescent);
                }
            }
        }

        String calcText(HSinglelineEntry mle)
        {
            String editContent = mle.getTextContent(HState.NORMAL_STATE);

            calculateMetrics(mle.getSize());

            if (fontMetrics == null) return null;

            if ((editContent != null) && (editContent.length() > 0))
            {
                // If we have a special echo character set, change "content" to
                // hold X number of the echo character.
                if (mle.echoCharIsSet()) editContent = encodeString(mle, editContent);
                wrapText(editContent);

                int caretLine = calcCaretLine();

                if (isLineVisible(caretLine) == false)
                {
                    if (caretLine < topVisibleLine)
                        topVisibleLine = caretLine;
                    else
                        topVisibleLine = caretLine - visibleLines + 1;
                }
            }
            else
                emptyContent();
            return editContent;
        }

        private String encodeString(HSinglelineEntry le, String content)
        {

            char[] contentArray = content.toCharArray();
            char echoChar = le.getEchoChar();

            for (int i = 0; i < contentArray.length; i++)
            {
                // We don't want to convert new line characters to the
                // echo character.
                switch (contentArray[i])
                {
                    case '\n':
                        break;
                    default:
                        contentArray[i] = echoChar;
                        break;
                }
            }
            return new String(contentArray);
        }

        private int calcCaretLine()
        {
            int caretLine = 0;
            int caretPos = mle.getCaretCharPosition();

            for (int x = textOffsets.size() - 1; x >= 0; x--)
            {
                int lineBegin = ((Integer) textOffsets.elementAt(x)).intValue();
                int lineEnd = lineBegin + ((Integer) lineWidths.elementAt(x)).intValue();

                if ((caretPos >= lineBegin) && (caretPos <= lineEnd))
                {
                    caretLine = x;
                    break;
                }
            }
            return caretLine;
        }

        private void wrapText(String editContent)
        {
            String currentLine, widthTest;
            StringTokenizer tok;
            int lineNum = 0, textOffset;
            boolean flushCurrent = false;
            int currentLineWidth = 0;
            boolean newline = false;
            boolean longLine = false;
            boolean toolong = false;

            if (textChange == true)
            {
                // Clear out the array
                lines.removeAllElements();

                textChange = false;

                // Create a string tokenizer so we perform word wrapping.
                tok = new StringTokenizer(editContent, " \n", true);

                textOffsets.removeAllElements();
                lineWidths.removeAllElements();

                currentLine = widthTest = "";
                textOffset = 0;
                do
                {
                    boolean finishUp = false;

                    String token = tok.nextToken();

                    if (token.equals("\n"))
                    {
                        flushCurrent = true;
                        currentLine = widthTest;
                        newline = true;
                    }
                    else
                        widthTest = widthTest + token;

                    do
                    {
                        longLine = false;

                        toolong = (fontMetrics.stringWidth(widthTest) > displayArea.width);

                        // Check to see if what we've constructed will fit on
                        // one line.
                        if ((flushCurrent == true) || (toolong == true))
                        {
                            // If the line is too long and there are no
                            // delimeters in
                            // the string, then we need to print out what will
                            // fit and loop
                            if (toolong == true
                                    && (((widthTest.indexOf(' ') == -1) && (widthTest.indexOf('\n') == -1)) ||
                                    // We also need to handle lines that may be
                                    // too long, but they have
                                    // whitespace on either side of a single
                                    // word.
                                    (widthTest.trim().indexOf(' ') == -1)))
                            {
                                currentLine = fitToLine(widthTest, displayArea.width);
                                widthTest = widthTest.substring(currentLine.length());
                                longLine = true;
                            }

                            currentLineWidth += currentLine.length();
                            if (newline == true)
                            {
                                currentLineWidth++;
                            }
                            lines.addElement(currentLine);
                            textOffsets.insertElementAt(new Integer(textOffset), lineNum);
                            lineWidths.insertElementAt(new Integer(currentLineWidth), lineNum);
                            textOffset += currentLineWidth;
                            lineNum++;
                            currentLineWidth = 0;

                            if (longLine == true)
                            {
                                if (finishUp == true)
                                {
                                    currentLine = widthTest;
                                    flushCurrent = true;
                                }
                                continue;
                            }
                            else
                            {
                                currentLine = "";
                                widthTest = (newline == true || finishUp == true) ? "" : token;
                            }

                        }
                        else
                        {
                            // If it fits, put it in currentLine and try another
                            // token.
                            currentLine = widthTest;
                        }
                        flushCurrent = false;

                        if (!finishUp && !tok.hasMoreTokens() && ((widthTest.length() > 0) || (newline == true)))
                        {

                            flushCurrent = true;
                            finishUp = true;

                            if (fontMetrics.stringWidth(widthTest) > displayArea.width)
                            {
                                flushCurrent = false;
                                longLine = true;
                                currentLine = "";
                            }
                            else
                                currentLine = (newline == true) ? "" : widthTest;
                        }
                        newline = false;
                    }
                    while ((flushCurrent == true) || (longLine == true));

                }
                while (tok.hasMoreTokens());
            }
        }

        private String fitToLine(String toFit, int width)
        {
            int nChars = toFit.length();
            char[] chars = new char[nChars];
            toFit.getChars(0, nChars, chars, 0);

            // Remove chars until it fits
            do
            {
                --nChars;
            }
            while (fontMetrics.charsWidth(chars, 0, nChars) > width);
            return new String(chars, 0, nChars);
        }

        private void emptyContent()
        {
            lines.removeAllElements();
            textOffsets.removeAllElements();
            lineWidths.removeAllElements();
            textOffsets.insertElementAt(new Integer(0), 0);
            lineWidths.insertElementAt(new Integer(0), 0);
            textChange = false;
        }

        private String trimNewLine(String toTrim)
        {

            if (toTrim.indexOf('\n') == -1)
                return toTrim;
            else
            {
                StringBuffer sb = new StringBuffer(toTrim.length());

                for (int x = 0; x < toTrim.length(); x++)
                {
                    char transfer = toTrim.charAt(x);
                    if (transfer != '\n') sb.append(transfer);
                }
                return sb.toString();
            }
        }

        /**
         * Returns whether the specified line should be visible.
         *
         * @return boolean
         * @param lineNum
         *            int
         */
        private boolean isLineVisible(int lineNum)
        {
            // Check to see if this line number is within the range
            // of visible lines.
            return ((lineNum >= topVisibleLine) && (lineNum < (topVisibleLine + visibleLines)));
        }

        /**
         * Returns the calculated base line for a specified line.
         *
         * @return int
         * @param lineNum
         *            int
         */
        private int visibleBaseLine(int lineNum)
        {
            return (yDisplayOffset + ((maxDescent + stringHeight) * (lineNum - topVisibleLine + 1)));
        }

        private String displayText(Graphics g, HVisible visible)
        {
            String content = null;

            if (g != null)
            {
                fontMetrics = g.getFontMetrics();
                content = calcText(mle);

                for (int i = 0; i < lines.size(); i++)
                {
                    if (isLineVisible(i) == true)
                        g.drawString(trimNewLine((String) lines.elementAt(i)), xDisplayOffset, visibleBaseLine(i));
                }
            }
            return content;
        }
    }

    /**
     * Same as {@link #getCaretCharPositionForLine(HVisible,int)} but does not
     * throw an exception. If the given line is invalid, then -1 is returned.
     */
    private int getCaretCharPositionForLine(HMultilineEntry mle, Data data, int line)
    {
        if (line < 0 || line >= data.getLineCount()) return -1;
        CaretPos pos = getCaretPos(mle, data);
        Line ln = data.getLine(line);
        int ofs = Math.min(pos.ofs, ln.line.length());
        if (DEBUG)
        {
            if (log.isDebugEnabled())
            {
                log.debug("forLine(" + line + ") of " + pos.line + ":" + pos.ofs + " -> " + ofs);
            }
        }
        return ln.offset + ofs;
    }

    /**
     * Calculates and returns the line+offset where the caret currently sits.
     */
    private CaretPos getCaretPos(HMultilineEntry mle, Data data)
    {
        int i, ofs = 0;
        Line line;
        int caretPos = mle.getCaretCharPosition();

        if (DEBUG)
        {
            if (log.isDebugEnabled())
            {
                log.debug("CaretPos: " + caretPos);
            }
        }
        for (i = data.getLineCount() - 1; i >= 0; --i)
        {
            line = data.getLine(i);
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Check line: " + i + " @ofs " + line.offset);
                }
            }
            ofs = caretPos - line.offset;
            if (ofs >= 0) break;
        }
        if (i < 0)
        {
            i = 0;
            ofs = 0;
        }
        return new CaretPos(i, ofs);
    }

    /**
     * Calculates the current scoll position (based on the current caret line).
     */
    private int getScrollPosition(HMultilineEntry mle, Data data, CaretPos caret, int nLines)
    {
        int scroll = data.scroll;

        // If Caret line isn't visible
        if (caret.line < scroll)
        {
            data.scroll = scroll = caret.line;
        }
        else if (caret.line >= (scroll + nLines))
        {
            data.scroll = scroll = caret.line - nLines + 1;
        }
        if ((scroll + nLines) > data.getLineCount()) data.scroll = scroll -= (scroll + nLines) - data.getLineCount();
        if (scroll < 0) data.scroll = scroll = 0;
        if (DEBUG && mle.getEditMode())
        {
            if (log.isDebugEnabled())
            {
                log.debug("caretline = " + caret.line);
            }
            if (log.isDebugEnabled())
            {
                log.debug("scrollline = " + scroll);
            }
        }

        return scroll;
    }

    /**
     *
     */
    private int getNumVisible(HMultilineEntry mle, int fontHeight, Data data)
    {
        return Math.min(data.area.height / fontHeight, data.lines.size());
    }

    /**
     * Calculates the formatting data for the current text content.
     * Specifically, it wraps the text appropriately.
     */
    private static Data reformat(HMultilineEntry mle, Data data)
    {
        if (true)
        { // original modified
            final String editContent = data.content;
            final FontMetrics fontMetrics = data.fm;
            final Vector lines = data.lines;
            final int wol = data.area.width;
            String currentLine, widthTest;
            StringTokenizer tok;
            int lineNum = 0, textOffset;
            boolean flushCurrent = false;
            int currentLineWidth = 0;
            boolean newline = false;
            boolean longLine = false;
            boolean toolong = false;

            // Clear out the array
            lines.removeAllElements();

            // Create a string tokenizer so we perform word wrapping.
            tok = new StringTokenizer(editContent, " \n", true);

            /*
             * textOffsets.removeAllElements(); lineWidths.removeAllElements();
             */

            currentLine = widthTest = "";
            textOffset = 0;
            while (tok.hasMoreTokens())
            {
                boolean finishUp = false;

                String token = tok.nextToken();

                if (token.equals("\n"))
                {
                    flushCurrent = true;
                    currentLine = widthTest;
                    newline = true;
                }
                else
                    widthTest = widthTest + token;

                do
                {
                    longLine = false;

                    toolong = (fontMetrics.stringWidth(widthTest) > wol);

                    // Check to see if what we've constructed will fit on one
                    // line.
                    if ((flushCurrent == true) || (toolong == true))
                    {
                        // If the line is too long and there are no delimeters
                        // in
                        // the string, then we need to print out what will fit
                        // and loop
                        /*
                         * if (toolong == true && (((widthTest.indexOf(' ') ==
                         * -1) && (widthTest.indexOf('\n') == -1)) || // We also
                         * need to handle lines that may be too long, but they
                         * have // whitespace on either side of a single word.
                         * (widthTest.trim().indexOf(' ') == -1))) { currentLine
                         * = fitToLine(widthTest, wol); widthTest =
                         * widthTest.substring(currentLine.length()); longLine =
                         * true; }
                         */
                        if (toolong == true && widthTest.indexOf(' ') == -1 && widthTest.indexOf('\n') == -1)
                        {
                            currentLine = widthTest;
                            finishUp = true;
                            // longLine = true;
                        }

                        currentLineWidth += currentLine.length();
                        if (newline == true)
                        {
                            currentLineWidth++;
                        }
                        lines.addElement(new Line(currentLine, textOffset, currentLineWidth));
                        /*
                         * lines.addElement(currentLine);
                         * textOffsets.insertElementAt(new Integer(textOffset),
                         * lineNum); lineWidths.insertElementAt(new
                         * Integer(currentLineWidth), lineNum);
                         */
                        textOffset += currentLineWidth;
                        lineNum++;
                        currentLineWidth = 0;

                        if (longLine == true)
                        {
                            if (finishUp == true)
                            {
                                currentLine = widthTest;
                                flushCurrent = true;
                            }
                            continue;
                        }
                        else
                        {
                            currentLine = "";
                            widthTest = (newline == true || finishUp == true) ? "" : token;
                        }

                    }
                    else
                    {
                        // If it fits, put it in currentLine and try another
                        // token.
                        currentLine = widthTest;
                    }
                    flushCurrent = false;

                    if (!finishUp && !tok.hasMoreTokens() && ((widthTest.length() > 0) || (newline == true)))
                    {
                        flushCurrent = true;
                        finishUp = true;

                        if (fontMetrics.stringWidth(widthTest) > wol)
                        {
                            flushCurrent = false;
                            longLine = true;
                            currentLine = "";
                        }
                        else
                            currentLine = (newline == true) ? "" : widthTest;
                    }
                    newline = false;
                }
                while ((flushCurrent == true) || (longLine == true));
            } // while hasMoreTokens
        }
        else
        { // new
            final String editContent = data.content;
            final char[] buf = new char[editContent.length()];
            editContent.getChars(0, buf.length, buf, 0);

            final int wol = data.area.width;
            final Vector lines = data.lines;
            final FontMetrics fontMetrics = data.fm;

            // Loop over lines
            for (int i = 0; i < buf.length;)
            {
                int width = 0;
                int lastWidth = 0;
                boolean inToken = false;

                int start = i, end = i;
                int lastEnd = 0;
                boolean lastLF = false;

                // Loop over characters and generate a line
                for (; i < buf.length; ++i)
                {
                    char c = buf[i];

                    // Skip non-printing characters??

                    // If an explicit newline, collect and move
                    if (c == '\n')
                    {
                        ++i; // move past LF
                        // If this is the last character, must add another line!
                        // lastLF = true;
                        break;
                    }
                    lastLF = false;

                    // If start of a token (whitespace of 1st non-whitespace)
                    // Then save off previous line width/ending
                    if (c == ' ' || c == '\t' || !inToken)
                    {
                        // new token start
                        // new token end as well
                        lastWidth = width;
                        lastEnd = end;
                        inToken = !(c == ' ' || c == '\t');
                    }

                    // Advance character
                    end = i + 1;

                    // Figure in width of character
                    width += fontMetrics.charWidth(c);

                    // If no good, revert to last token width
                    if (width >= wol && lastWidth != 0)
                    {
                        width = lastWidth;
                        end = i = lastEnd; // go back to end of previous
                        break;
                    }

                    // If good, width is incremented and we loop!
                }
                /*
                 * if (end < 0) end = start;
                 */

                // add the line
                if (DEBUG)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Add line : '" + editContent.substring(start, end) + "'");
                    }
                }
                lines.addElement(new Line(editContent.substring(start, end), start, end - start));
                if (lastLF)
                {
                    // If last character was a LF, may need to add empty line
                    // Add empty line if lookahead shows "$"
                    if (i == buf.length)
                    {
                        if (DEBUG)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("lastLF ");
                            }
                        }
                        lines.addElement(new Line("", i - 1, 1));
                    }
                    lastLF = false;
                }
            }
        }
        if (data.getLineCount() == 0)
        {
            data.lines.addElement(new Line("", 0, 0));
        }
        if (DEBUG)
        {
            if (log.isDebugEnabled())
            {
                log.debug("REFORMATTED");
            }
            data.dump();
        }
        return data;
    }

    private Data getData(HMultilineEntry mle)
    {
        Font font = mle.getFont();
        Insets insets = getInsets(mle);
        Dimension d = mle.getSize();
        Dimension area = new Dimension(d.width - insets.left - insets.right, d.height - insets.top - insets.bottom);
        FontMetrics fm = mle.getFontMetrics(font);
        int padding = fm.getMaxDescent();

        area.width -= padding * 2;
        area.height -= padding * 2;

        return getData(mle, font, fm, area);
    }

    /**
     * Extract the current formatting <code>Data</code>.
     *
     * Calls <code>mle.getLookData()</code> to get the current look data. If it
     * does not exist or is of the wrong type, <code>null</code> is returned.
     *
     * @param mle
     *            the component being drawn
     * @return the current data or <code>null</code>
     */
    private Data getData0(HMultilineEntry mle)
    {
        Object privData = mle.getLookData(this);
        if (privData != null && (privData instanceof Data)) return (Data) privData;
        return null;
    }

    /**
     * Extract the current formatting <code>Data</code>.
     *
     * Calls <code>mle.getLookData()</code> to get the current look data. If it
     * does not exist, it is created. If it is invalid or newly created, the
     * text content is formatted.
     *
     * @param mle
     *            the component being drawn
     * @param displayArea
     *            the current displayArea for this component (used in the
     *            validation of the current data as well as the formatting
     *            calculations).
     * @return the current, newly constructed, or reformatted data
     */
    private Data getData(HMultilineEntry mle, Font font, FontMetrics fm, Dimension area)
    {
        Data data = getData0(mle);
        if (data != null)
        {
            if (!data.isValid(font, area))
            {
                if (DEBUG)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("!!!!Reformatting!!!!");
                    }
                    data.dump();
                }

                // Clear current data
                data.reset(getTextContent(mle), font, fm, area);

                // Reformat
                reformat(mle, data);
            }
        }
        else
        {
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("!!!!New Data!!!!");
                }
            }

            // Create a new Data object
            data = new Data(getTextContent(mle), font, fm, area);
            // Set it
            mle.setLookData(this, data);

            // Format
            reformat(mle, data);
        }
        return data;
    }

    /**
     * This class represents a single line. A <code>Vector</code> or array of
     * them represent all lines.
     */
    private static class Line
    {
        Line(String line, int offset, int width)
        {
            this.offset = offset;
            this.width = width;
            this.line = line;
        }

        /** Offset of this line within content. */
        int offset;

        /** Pixel width of this line. */
        int width;

        /** The actual string that is this line. */
        String line;
    }

    /**
     * This class represents private data for a given <code>HVisible</code>.
     */
    private static class Data
    {
        Data()
        {
        }

        Data(String content, Font font, FontMetrics fm, Dimension area)
        {
            this.content = content;
            this.area = area;
            this.font = font;
            this.fm = fm;
            notValid = false;
        }

        void reset(String content, Font font, FontMetrics fm, Dimension area)
        {
            lines.removeAllElements();
            this.content = content;
            this.area = area;
            this.font = font;
            this.fm = fm;
            notValid = false;
        }

        /**
         * Given the provided <code>String</code> content, current
         * <code>Font</code> and current displayable <code>Dimension</code>s,
         * determine if this <code>Data</code> object is valid or not.
         * <p>
         * If not valid it should be reformated.
         */
        boolean isValid(Font font, Dimension area)
        {
            if (DEBUG)
            {
                if (notValid)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Content INVALID!");
                    }
                }
                else if (this.font != font)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Font INVALID: " + this.font + " != " + font);
                    }
                }
                else if (this.area != area && (this.area == null || area == null || !this.area.equals(area)))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Bad areas: " + this.area + " != " + area);
                    }
                }
            }
            return !notValid && this.font == font
                    && (this.area == area || (this.area != null && area != null && this.area.equals(area)));
        }

        /**
         * Returns the requested line object.
         */
        Line getLine(int i)
        {
            return (Line) lines.elementAt(i);
        }

        /**
         * Returns the number of line objects.
         */
        int getLineCount()
        {
            return lines.size();
        }

        void dump()
        {
            if (DEBUG)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("******** Data *********");
                }
                int n = lines.size();
                if (content == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("  ? content");
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("  " + content.length() + " chars");
                    }
                }
                if (log.isDebugEnabled())
                {
                    log.debug("  " + n + " lines");
                }
                for (int i = 0; i < n; ++i)
                {
                    Line ln = (Line) lines.elementAt(i);
                    if (log.isDebugEnabled())
                    {
                        log.debug("  [" + ln.offset + "]: \"" + ln.line + "\"");
                    }
                }
            }
        }

        /** Vector of Line objects. */
        Vector lines = new Vector();

        /**
         * Whether content is not valid or is. Used to decide whether to
         * reformat.
         */
        boolean notValid;

        /** Current content data. Used to decide whether to reformat. */
        String content;

        /** Current display area. Used to decide whether to reformat. */
        Dimension area;

        /** Current font. Used to decide whether to reformat. */
        Font font;

        /** Current fontMetrics. */
        FontMetrics fm;

        /** Current scroll position (first visible line). */
        int scroll;
    }

    private class CaretPos
    {
        CaretPos(int line, int ofs)
        {
            this.line = line;
            this.ofs = ofs;
        }

        int line, ofs;
    }

    private static final boolean DEBUG = false;

    private static final boolean NEW_CODE = true;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(HMultilineEntryLook.class.getName());

}
