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
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.havi.SizingHelper;
import org.cablelabs.impl.havi.SizingHelper.Strategy;
import org.cablelabs.impl.havi.TextSupport;

/**
 * The {@link org.havi.ui.HTextLook HTextLook} class is used by any
 * {@link org.havi.ui.HVisible HVisible} component to display static text
 * content. This look will be provided by the platform and the exact way in
 * which it is rendered will be platform dependant.
 * 
 * <p>
 * The {@link org.havi.ui.HTextLook HTextLook} class draws the content set on an
 * {@link org.havi.ui.HVisible HVisible}. It uses the
 * {@link org.havi.ui.HVisible#getTextContent getTextContent(int state)} method
 * with the current state of the {@link org.havi.ui.HVisible HVisible} to
 * determine the content to render.
 * 
 * <p>
 * In addition to the basic class behavior defined by the
 * {@link org.havi.ui.HLook HLook} interface the {@link org.havi.ui.HTextLook
 * HTextLook} associated with a given {@link org.havi.ui.HVisible HVisible}
 * should use:
 * <ul>
 * <li>HVisible.getTextLayoutManager() to determine the
 * {@link org.havi.ui.HTextLayoutManager HTextLayoutManager} to perform the
 * rendering. If the returned object is null, then the text should not be
 * rendered.
 * </ul>
 * 
 * Implementations of {@link org.havi.ui.HTextLook HTextLook} need not use the
 * alignment mode methods on {@link org.havi.ui.HVisible HVisible}, since all
 * rendering of content is deferred to the associated
 * {@link org.havi.ui.HTextLayoutManager HTextLayoutManager} which will use
 * these methods accordingly.
 * 
 * <p>
 * This is the default look that is used by {@link HStaticText} and its
 * subclasses.
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
 * <tr>
 * <td colspan=4>None.</td>
 * </tr>
 * </table>
 * 
 * @see org.havi.ui.HStaticText
 * @see org.havi.ui.HDefaultTextLayoutManager
 * @author Todd Earles
 * @author Aaron Kamienski
 * @version 1.1
 */
public class HTextLook implements HExtendedLook
{
    /**
     * Creates a {@link org.havi.ui.HTextLook HTextLook} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HTextLook()
    {
        // ...
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
        if (visible.getBackgroundMode() == HVisible.BACKGROUND_FILL)
        {
            Dimension size = visible.getSize();
            Color bg = visible.getBackground();
            g.setColor(bg);
            g.fillRect(0, 0, size.width, size.height);
        }
        return;
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
        Insets insets = getInsets(visible);
        HaviToolkit.getToolkit().drawBorder(g, visible, state, insets);
        return;
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

        String content = getContent(visible, state);
        HTextLayoutManager text = visible.getTextLayoutManager();
        Insets insets = getInsets(visible);

        if ((content != null) && (text != null)) text.render(content, g, visible, insets);

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

        // Render content
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
        // Really lame-o implementation
        if (visible.isVisible()) visible.repaint();
        // More complex implementation follows...
        /*
         * for(int i = 0; i < changes.length; ++i) { switch(changes[i].hint) {
         * case TEXT_CONTENT_CHANGE: case STATE_CHANGE: case UNKNOWN_CHANGE: //
         * Should probably get even MORE precise... if (visible.isVisible())
         * visible.repaint(); break; } }
         */
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
     * requires for border decoration etc. If HVisible.getTextLayoutManager()
     * does not return an instance of HDefaultTextLayoutManager, or
     * HDefaultTextLayoutManager returns a zero size, then proceed with the
     * following steps as if content for this HVisible had not been set.
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
    public java.awt.Dimension getMinimumSize(HVisible hvisible)
    {
        HTextLayoutManager tlm;
        HDefaultTextLayoutManager dtlm;
        Dimension size;

        /* HTextLook/HDefaultTextLayoutManager special case. */
        tlm = hvisible.getTextLayoutManager();
        if ((tlm instanceof HDefaultTextLayoutManager) && (dtlm = (HDefaultTextLayoutManager) tlm) != null
                && (size = dtlm.getMinimumSize(hvisible)) != null && size.width != 0 && size.height != 0)
        {
            return addInsets(size, getInsets(hvisible));
        }

        /* Standard case. */
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
     * requires for border decoration etc. If HVisible.getTextLayoutManager()
     * does not return an instance of HDefaultTextLayoutManager, or
     * HDefaultTextLayoutManager returns a zero size, then proceed with the
     * following steps as if content for this HVisible had not been set.
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
    public java.awt.Dimension getPreferredSize(HVisible hvisible)
    {
        HTextLayoutManager tlm;
        HDefaultTextLayoutManager dtlm;
        Dimension size;

        /* HTextLook/HDefaultTextLayoutManager special case. */
        /* Test first for presence of default size. */
        if (((size = hvisible.getDefaultSize()) == null || size == HVisible.NO_DEFAULT_SIZE)
                && ((tlm = hvisible.getTextLayoutManager()) instanceof HDefaultTextLayoutManager)
                && (dtlm = (HDefaultTextLayoutManager) tlm) != null && (size = dtlm.getMinimumSize(hvisible)) != null
                && size.width != 0 && size.height != 0)
        {
            return addInsets(size, getInsets(hvisible));
        }

        /* Standard case. */
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
     * requires for border decoration etc. If HVisible.getTextLayoutManager()
     * does not return an instance of HDefaultTextLayoutManager, or
     * HDefaultTextLayoutManager returns a zero size, then proceed with the
     * following steps as if content for this HVisible had not been set.
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
    public java.awt.Dimension getMaximumSize(HVisible hvisible)
    {
        HTextLayoutManager tlm;
        HDefaultTextLayoutManager dtlm;
        Dimension size;

        /* HTextLook/HDefaultTextLayoutManager special case. */
        tlm = hvisible.getTextLayoutManager();
        if ((tlm instanceof HDefaultTextLayoutManager) && (dtlm = (HDefaultTextLayoutManager) tlm) != null
                && (size = dtlm.getMinimumSize(hvisible)) != null && size.width != 0 && size.height != 0)
        {
            return addInsets(size, getInsets(hvisible));
        }

        /* Standard case. */
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
        return (visible.getBackgroundMode() != HVisible.NO_BACKGROUND_FILL)
                && HaviToolkit.getToolkit().isOpaque(visible.getBackground());
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
        return visible.getBordersEnabled() ? (Insets) insets.clone() : new Insets(0, 0, 0, 0);
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
     * Adds insets to the given <code>Dimension</code>. Note, that the input
     * dimension is adjusted directly.
     * 
     * @param dim
     *            the input dimension
     * @param insets
     *            the insets to added to dimension
     * @return <code>dim</code> with insets added
     */
    private Dimension addInsets(Dimension dim, Insets insets)
    {
        // Maybe we shouldn't do this...?
        // Are we allowed to modify the size given to us?
        // I think we are...
        // The caller shouldn't give us something they don't want modified
        dim.width += insets.left + insets.right;
        dim.height += insets.top + insets.bottom;
        return dim;
    }

    /**
     * Implements the <code>SizingHelper.Strategy</code> interface to provide
     * the <code>SizingHelper</code> methods with information about the content
     * to be sized by this look.
     */
    private static final Strategy strategy = new Strategy()
    {
        /**
         * Returns whether the given <code>hvisible</code> has any
         * {@link HVisible#getTextContent(int)} or not.
         * 
         * @return <code>hvisible.getTextContent(i) != null</code> for all
         *         <code>i</code>
         */
        public boolean hasContent(HVisible hvisible)
        {
            for (int i = 0; i < 8; ++i)
                if (hvisible.getTextContent(i | HState.NORMAL_STATE) != null) return true;
            return false;
        }

        /**
         * Calculates the largest dimensions of all content.
         * <p>
         * Returns the dimensions necessary to hold ALL content for ALL states
         * given the current font (or the default font if no font is set).
         * 
         * @param the
         *            <code>HVisible</code> to query for content
         * @return the largest dimensions of all content.
         */
        public Dimension getMaxContentSize(HVisible hvisible)
        {
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
            if (font == null) font = HaviToolkit.getToolkit().getDefaultFont();
            FontMetrics metrics = hvisible.getFontMetrics(font);

            int minW = Integer.MAX_VALUE, lineCount = Integer.MAX_VALUE;

            // smallest size of text content
            for (int i = (HState.FIRST_STATE & HState.ALL_STATES); i <= (HState.LAST_STATE & HState.ALL_STATES); ++i)
            {
                String content = hvisible.getTextContent(i | HState.NORMAL_STATE);
                if (content != null)
                {
                    String[] text = TextSupport.getLines(content);

                    // Use the widest line
                    minW = Math.min(minW, TextSupport.getMaxWidth(text, metrics));
                    lineCount = Math.min(lineCount, text.length);
                }
            }
            if (minW == Integer.MAX_VALUE) minW = 0;
            if (lineCount == Integer.MAX_VALUE) lineCount = 0;

            return new Dimension(minW, TextSupport.getFontHeight(metrics) * lineCount);
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

    /**
     * The insets associated with this look. Currently defaults to values
     * compatible with HAVi 1.0.
     */
    private Insets insets = HaviToolkit.getToolkit().getDefaultHLookInsets();
}