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

package org.cablelabs.gear.havi.decorator;

import org.havi.ui.HLook;
import org.havi.ui.HVisible;
import org.havi.ui.HState;
import org.havi.ui.HTextLayoutManager;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Insets;
import java.awt.Font;
import java.awt.FontMetrics;
import org.cablelabs.gear.util.TextLines;
import org.cablelabs.gear.util.TextRender;

/**
 * Replacement for {@link org.havi.ui.HTextLook} which fits within the
 * <code>DecoratorLook </code> hierarchy, <code>TextDecorator</code> renders the
 * text content of a component. However, <code>TextDecorator</code> never fills
 * in the background as <code>HTextLook</code> may.
 * <p>
 * A <code>TextDecorator</code> can be used as a <i>leaf</i> object in a
 * decorator chain or it can be used as a integral part of a decorator chain,
 * complete with its own component look. For example, a
 * <code>TextDecorator</code> can be chained with a {@link GraphicDecorator} to
 * display graphic content on top of text content, with no regard to placement.
 * <p>
 * <i><b>Note</b></i> that <code>TextDecorator</code> cannot be used as a direct
 * replacement for an <code>HTextLook</code> -- it is not an
 * <code>HTextLook</code>, so components that require an <code>HTextLook</code>
 * may throw <code>HInvalidLookException</code>. In which case a
 * <code>TextDecorator</code> should be wrapped in a {@link TextLookAdapter}.
 * 
 * @author Aaron Kamienski
 * @author Jeff Bonin (havi 1.0.1 update)
 * @version $Id: TextDecorator.java,v 1.13 2002/06/03 21:32:31 aaronk Exp $
 * 
 * @see org.havi.ui.HTextLayoutManager
 * @see org.havi.ui.HTextLook
 * @see TextLookAdapter
 */
public class TextDecorator extends AbstractSizingDecorator implements ExtendedLook
{
    /**
     * Default constructor. No component look is provided.
     * 
     * @see #setComponentLook(HLook)
     */
    public TextDecorator()
    {
        this(null);
    }

    /**
     * Constructor to create a new <code>DecoratorLook</code> with the given
     * component look.
     * 
     * @param componentLook
     *            The <code>HLook</code> to which this decorator is adding
     *            responsibilities; can be <code>null</code> if none is desired
     *            (i.e., this is a <i>leaf</i> look).
     * 
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public TextDecorator(HLook componentLook)
    {
        super(componentLook);
    }

    /**
     * Similar to <code>HTextLook.showLook</code> but does not fill the bounds
     * of the <code>HVisible</code> with its <i>background</i> color (rendering
     * it <i>transparent</i>) or draw borders.
     * 
     * @param g
     *            the graphics context.
     * @param visible
     *            the visible.
     * @param state
     *            the state parameter indicates the state of the visible,
     *            allowing the look to render the appropriate content for that
     *            state.
     * 
     * @see org.havi.ui.HTextLayoutManager
     * @see org.havi.ui.HTextLook#showLook(Graphics, HVisible, int)
     */
    public void showLook(java.awt.Graphics g, HVisible visible, int state)
    {
        // Do NOT draw the background

        String content = getContent(visible, state);
        HTextLayoutManager text = visible.getTextLayoutManager();

        if ((content != null) && (text != null))
        {
            // Reconstruct insets based on new bounds
            Dimension size = visible.getSize();

            text.render(content, g, visible, getInsets(visible));
        }

        // Do NOT draw border decorations

        super.showLook(g, visible, state);
    }

    // Description copied from superclass
    public void showLook(java.awt.Graphics g, HVisible visible, int state, Rectangle bounds, int hAlign, int vAlign,
            int resize)
    {
        String content = getContent(visible, state);
        HTextLayoutManager text = visible.getTextLayoutManager();

        if ((content != null) && (text != null))
        {
            // Reconstruct insets based on new bounds
            Dimension size = visible.getSize();

            text.render(content, g, visible, new Insets(bounds.y, bounds.x, size.height - bounds.height - bounds.y,
                    size.width - bounds.width - bounds.x));
        }
    }

    /**
     * Gets the content for the given state. If no content exists, attempts to
     * retrieve the nearest appropriate content.
     */
    private static String getContent(HVisible visible, int state)
    {
        String text = visible.getTextContent(state);
        if (text == null)
        {
            switch (state)
            {
                case HState.FOCUSED_STATE:
                case HState.DISABLED_STATE:
                    return getContent(visible, HState.NORMAL_STATE);
                case HState.ACTIONED_STATE:
                case HState.ACTIONED_FOCUSED_STATE:
                    return getContent(visible, HState.FOCUSED_STATE);
                case HState.DISABLED_FOCUSED_STATE:
                case HState.DISABLED_ACTIONED_FOCUSED_STATE:
                    return getContent(visible, HState.DISABLED_STATE);
                case HState.DISABLED_ACTIONED_STATE:
                    return getContent(visible, HState.ACTIONED_STATE);
            }
        }
        return text;
    }

    /**
     * Returns whether the given <code>hvisible</code> has any
     * {@link HVisible#getTextContent(int) text} content or not.
     * 
     * @return <code>hvisible.getTextContent(i) != null</code> for any
     *         <code>i</code>
     */
    public boolean hasContent(HVisible hvisible)
    {
        for (int i = (HState.FIRST_STATE & HState.ALL_STATES); i <= (HState.LAST_STATE & HState.ALL_STATES); ++i)
            if (hvisible.getTextContent(i | HState.NORMAL_STATE) != null) return true;
        return false;
    }

    /**
     * Calculates the largest dimensions of all content.
     * <p>
     * Returns the dimensions necessary to hold ALL content for ALL states given
     * the current font (or the default font if no font is set).
     * 
     * @param the
     *            <code>HVisible</code> to query for content
     * @return the largest dimensions of all content.
     */
    public Dimension getMaxContentSize(HVisible hvisible)
    {
        // Need a font to calculate sizing info
        Font font = hvisible.getFont();
        if (font == null) font = getDefaultFont();
        FontMetrics metrics = hvisible.getFontMetrics(font);

        int maxW = 0, lineCount = 0;

        // largest size of test content
        for (int i = (HState.FIRST_STATE & HState.ALL_STATES); i <= (HState.LAST_STATE & HState.ALL_STATES); ++i)
        {
            String content = hvisible.getTextContent(i | HState.NORMAL_STATE);
            if (content != null)
            {
                String[] text = TextLines.getLines(content);

                // Use the widest line
                maxW = Math.max(maxW, TextLines.getMaxWidth(text, metrics));
                lineCount = Math.max(lineCount, text.length);
            }
        }

        return new Dimension(maxW, TextRender.getFontHeight(metrics) * lineCount);
    }

    /**
     * Calculates the smallest dimensions of all content.
     * <p>
     * Returns the dimensions necessary to show the smallest content.
     * 
     * @param the
     *            <code>HVisible</code> to query for content
     * @return the smallest dimensions of all content.
     */
    public Dimension getMinContentSize(HVisible hvisible)
    {
        // Need a font to calculate sizing info
        Font font = hvisible.getFont();
        if (font == null) font = getDefaultFont();
        FontMetrics metrics = hvisible.getFontMetrics(font);

        int minW = Integer.MAX_VALUE, lineCount = Integer.MAX_VALUE;

        // smallest size of text content
        for (int i = (HState.FIRST_STATE & HState.ALL_STATES); i <= (HState.LAST_STATE & HState.ALL_STATES); ++i)
        {
            String content = hvisible.getTextContent(i | HState.NORMAL_STATE);
            if (content != null)
            {
                String[] text = TextLines.getLines(content);

                // Use the widest line
                minW = Math.min(minW, TextLines.getMaxWidth(text, metrics));
                lineCount = Math.min(lineCount, text.length);
            }
        }
        if (minW == Integer.MAX_VALUE) minW = 0;
        if (lineCount == Integer.MAX_VALUE) lineCount = 0;

        return new Dimension(minW, TextRender.getFontHeight(metrics) * lineCount);
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

    /**
     * Returns the default font to use in calculations when one is not
     * available.
     */
    private synchronized static Font getDefaultFont()
    {
        if (defaultFont == null) defaultFont = new Font("SansSerif", Font.PLAIN, 18);
        return defaultFont;
    }

    /**
     * Default font to use in calculations when one is not available.
     */
    private static Font defaultFont;
}
