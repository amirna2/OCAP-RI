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
import org.havi.ui.HChangeData;
import org.havi.ui.HOrientable;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * The <code>PairDecorator</code> fills the <i>layout hole</i> in the decorator
 * design pattern as implemented by the <code>DecoratorLook</code> hierarchy. A
 * decorator <i>chain</i> can easily be built up to display different types of
 * content (e.g., both graphic and text), but each content is rendered
 * irrespective of the other content (e.g., while it is simple to center text on
 * a graphic, displaying text directly below a graphic isn't so simple). The
 * <code>PairDecorator</code>, along with the ({@link ExtendedLook} interface,
 * fills this hole.
 * <p>
 * A <code>PairDecorator</code> is composed of at most two (2)
 * <code>ExtendedLooks</code>: a <i>primary</i> and a <i>secondary</i> look.
 * Generally, each <code>ExtendedLook</code> will be used to display a different
 * kind of content. The key here is the
 * {@link ExtendedLook#showLook(Graphics,HVisible,int,Rectangle,int,int,int)}
 * method that <code>ExtendedLook</code> adds to the standard <code>HLook</code>
 * interface. This method takes parameters which allow the
 * <code>PairDecorator</code> to specify the sub-areas of the component in which
 * each <code>ExtendedLook</code> should render itself.
 * <p>
 * In order to present more than two types of content, aligned with each other,
 * one can compose a <code>PairDecorator</code> of other
 * <code>PairDecorators</code>. This is because <code>PairDecorator</code> also
 * implements the <code>ExtendedLook</code> interface.
 * <p>
 * The <i>layout</i> of the <code>ExtendedLooks</code> is determined by four
 * things:
 * <ul>
 * <li> {@link #getOrientation orientation}
 * <li> {@link #getGap gap}
 * <li>HVisible rendering bounds
 * <li><i>preferred</i> size of the primary look
 * </ul>
 * The primary <code>ExtendedLook</code> gets sized first according to the
 * <code>HVisible</code> rendering bounds and its preferred size in the
 * orientation direction. In this case, <i>preferred</i> size is defined as the
 * {@link ExtendedLook#getMaxContentSize(HVisible) maximum} or
 * {@link ExtendedLook#getMaxContentSize(HVisible) minimum} content sizes,
 * whichever fits best). It is sized to fill the entire bounds in the direction
 * opposite of the orientation. The secondary <code>ExtendedLook</code> is sized
 * to fill the space remaining in the rendering bounds. So, if the bounds are
 * smaller than necessary to show both, it is possible that only the primary
 * look will be shown (in its entirety). If the bounds are larger then
 * necessary, then it is likely that the secondary look will have more space
 * than necessary in which to render itself.
 * 
 * @author Aaron Kamienski
 * @version $Id: PairDecorator.java,v 1.2 2002/06/03 21:32:30 aaronk Exp $
 */
public class PairDecorator extends AbstractSizingDecorator implements ExtendedLook, HOrientable
{
    /**
     * Specifies that the horizontal alignment should be retrieved from the
     * <code>HVisible</code> being drawn.
     */
    public static final int HALIGN_DEFAULT = -1;

    /**
     * Specifies that the vertical alignment should be retrieved from the
     * <code>HVisible</code> being drawn.
     */
    public static final int VALIGN_DEFAULT = -1;

    /**
     * Specifies that the orientation is such that the two
     * <code>ExtendedLook</code> are rendered one (#2) on top of the other (#1).
     * It serves little purpose, as the same effect can be achieved without a
     * <code>PairDecorator</code>, with a simple decorator chain.
     */
    public static final int ORIENT_NONE = 10;

    /**
     * Constructs a new <code>PairDecorator</code>.
     */
    public PairDecorator()
    {
        this(null);
    }

    /**
     * Constructs a new <code>PairDecorator</code> with the given
     * <code>componentLook</code>.
     * 
     * @param componentLook
     */
    public PairDecorator(HLook componentLook)
    {
        super(componentLook);
    }

    /**
     * Constructs a new <code>PairDecorator</code> composed of the given
     * <code>ExtendedLooks</code>.
     * 
     * @param primary
     * @param secondary
     */
    public PairDecorator(ExtendedLook primary, ExtendedLook secondary)
    {
        this(null, primary, secondary);
    }

    /**
     * Constructs a new <code>PairDecorator</code> composed of the given
     * <code>componentLook</code> and <code>ExtendedLooks</code>.
     * 
     * @param componentLook
     * @param primary
     * @param secondary
     */
    public PairDecorator(HLook componentLook, ExtendedLook primary, ExtendedLook secondary)
    {
        super(componentLook);
        looks[0] = primary;
        looks[1] = secondary;
    }

    /**
     * Renders the given <code>visible</code> using this
     * <code>PairDecorator</code>'s component <code>ExtendedLooks</code>. The
     * {@link ExtendedLook#showLook(Graphics,HVisible,int,Rectangle,int,int,int)}
     * method of each <code>ExtendedLook</code> is called, in turn.
     * 
     * @param g
     *            current Graphics context
     * @param visible
     *            the component whose content should be rendered
     * @param state
     *            the state of the component
     */
    public void showLook(Graphics g, HVisible visible, int state)
    {
        Dimension size = visible.getSize();
        Insets insets = getInsets(visible);
        showLook(g, visible, state, new Rectangle(insets.left, insets.top, size.width - insets.left - insets.right,
                size.height - insets.top - insets.bottom), visible.getHorizontalAlignment(),
                visible.getVerticalAlignment(), visible.getResizeMode());
        super.showLook(g, visible, state);
    }

    /**
     * Renders the given <code>visible</code> using this
     * <code>PairDecorator</code>'s component <code>ExtendedLooks</code>. The
     * {@link ExtendedLook#showLook(Graphics,HVisible,int,Rectangle,int,int,int)}
     * method of each <code>ExtendedLook</code> is called, in turn.
     * 
     * @param g
     *            current Graphics context
     * @param visible
     *            the component whose content should be rendered
     * @param state
     *            the state of the component
     * @param bounds
     *            the bounds that the content should occupy within the Graphics
     *            coordinate system
     * @param hAlign
     *            the horizontal alignment
     * @param vAlign
     *            the vertical alignment
     * @param resize
     *            the resize mode to employ
     */
    public void showLook(Graphics g, HVisible visible, int state, Rectangle bounds, int hAlign, int vAlign, int resize)
    {
        Rectangle[] layout = layout(visible, bounds);

        for (int i = 0; i < 2; ++i)
        {
            if (looks[i] != null)
            {
                int hAlign2 = this.hAlign[i];
                int vAlign2 = this.vAlign[i];
                looks[i].showLook(g, visible, state, layout[i], (hAlign2 != HALIGN_DEFAULT) ? hAlign2 : hAlign,
                        (vAlign2 != VALIGN_DEFAULT) ? vAlign2 : vAlign, resize);
            }
        }
    }

    /**
     * Retrieves the <i>primary</i> extended look.
     * 
     * @return the <i>primary</i> extended look
     */
    public ExtendedLook getPrimaryLook()
    {
        return looks[0];
    }

    /**
     * Sets the <i>primary</i> extended look.
     * 
     * @param primary
     *            the new <i>primary</i> extended look
     */
    public void setPrimaryLook(ExtendedLook primary)
    {
        this.looks[0] = primary;
    }

    /**
     * Retrieves the horizontal alignment used for the <i>primary</i>
     * <code>ExtendedLook</code> when determining the rendering layout. If the
     * alignment is {@link #HALIGN_DEFAULT} then the default (as retrieved from
     * {@link HVisible#getHorizontalAlignment()}) is used.
     * 
     * @return the <i>primary</i> look's horizontal alignment
     */
    public int getPrimaryHorizontalAlignment()
    {
        return hAlign[0];
    }

    /**
     * Sets the horizontal alignment used for the <i>primary</i>
     * <code>ExtendedLook</code> when determining the rendering layout. If the
     * alignment is {@link #HALIGN_DEFAULT} then the default (as retrieved from
     * {@link HVisible#getHorizontalAlignment()}) is used.
     * 
     * @param hAlign
     *            the <i>primary</i> look's horizontal alignment
     */
    public void setPrimaryHorizontalAlignment(int hAlign)
    {
        switch (hAlign)
        {
            case HVisible.HALIGN_LEFT:
            case HVisible.HALIGN_CENTER:
            case HVisible.HALIGN_RIGHT:
            case HVisible.HALIGN_JUSTIFY:
            case HALIGN_DEFAULT:
                this.hAlign[0] = hAlign;
                break;
            default:
                throw new IllegalArgumentException("Invalid horizontal alignment");
        }
    }

    /**
     * Retrieves the vertical alignment used for the <i>primary</i>
     * <code>ExtendedLook</code> when determining the rendering layout. If the
     * alignment is {@link #VALIGN_DEFAULT} then the default (as retrieved from
     * {@link HVisible#getVerticalAlignment()}) is used.
     * 
     * @return the <i>primary</i> look's vertical alignment
     */
    public int getPrimaryVerticalAlignment()
    {
        return vAlign[0];
    }

    /**
     * Sets the vertical alignment used for the <i>primary</i>
     * <code>ExtendedLook</code> when determining the rendering layout. If the
     * alignment is {@link #VALIGN_DEFAULT} then the default (as retrieved from
     * {@link HVisible#getVerticalAlignment()}) is used.
     * 
     * @param vAlign
     *            the <i>primary</i> look's vertical alignment
     */
    public void setPrimaryVerticalAlignment(int vAlign)
    {
        switch (vAlign)
        {
            case HVisible.VALIGN_TOP:
            case HVisible.VALIGN_BOTTOM:
            case HVisible.VALIGN_CENTER:
            case HVisible.VALIGN_JUSTIFY:
            case VALIGN_DEFAULT:
                this.vAlign[0] = vAlign;
                break;
            default:
                throw new IllegalArgumentException("Invalid vertical alignment");
        }
    }

    /**
     * Retrieves the <i>secondary</i> extended look.
     * 
     * @return the <i>secondary</i> extended look
     */
    public ExtendedLook getSecondaryLook()
    {
        return looks[1];
    }

    /**
     * Sets the <i>secondary</i> extended look.
     * 
     * @param secondary
     *            the new <i>secondary</i> extended look
     */
    public void setSecondaryLook(ExtendedLook secondary)
    {
        this.looks[1] = secondary;
    }

    /**
     * Retrieves the horizontal alignment used for the <i>secondary</i>
     * <code>ExtendedLook</code> when determining the rendering layout. If the
     * alignment is {@link #HALIGN_DEFAULT} then the default (as retrieved from
     * {@link HVisible#getHorizontalAlignment()}) is used.
     * 
     * @return the <i>secondary</i> look's horizontal alignment
     */
    public int getSecondaryHorizontalAlignment()
    {
        return hAlign[1];
    }

    /**
     * Sets the horizontal alignment used for the <i>secondary</i>
     * <code>ExtendedLook</code> when determining the rendering layout. If the
     * alignment is {@link #HALIGN_DEFAULT} then the default (as retrieved from
     * {@link HVisible#getHorizontalAlignment()}) is used.
     * 
     * @param hAlign
     *            the <i>secondary</i> look's horizontal alignment
     */
    public void setSecondaryHorizontalAlignment(int hAlign)
    {
        switch (hAlign)
        {
            case HVisible.HALIGN_LEFT:
            case HVisible.HALIGN_CENTER:
            case HVisible.HALIGN_RIGHT:
            case HVisible.HALIGN_JUSTIFY:
            case HALIGN_DEFAULT:
                this.hAlign[1] = hAlign;
                break;
            default:
                throw new IllegalArgumentException("Invalid horizontal alignment");
        }
    }

    /**
     * Retrieves the vertical alignment used for the <i>secondary</i>
     * <code>ExtendedLook</code> when determining the rendering layout. If the
     * alignment is {@link #VALIGN_DEFAULT} then the default (as retrieved from
     * {@link HVisible#getVerticalAlignment()}) is used.
     * 
     * @return the <i>secondary</i> look's vertical alignment
     */
    public int getSecondaryVerticalAlignment()
    {
        return vAlign[1];
    }

    /**
     * Sets the vertical alignment used for the <i>secondary</i>
     * <code>ExtendedLook</code> when determining the rendering layout. If the
     * alignment is {@link #VALIGN_DEFAULT} then the default (as retrieved from
     * {@link HVisible#getVerticalAlignment()}) is used.
     * 
     * @param vAlign
     *            the <i>secondary</i> look's vertical alignment
     */
    public void setSecondaryVerticalAlignment(int vAlign)
    {
        switch (vAlign)
        {
            case HVisible.VALIGN_TOP:
            case HVisible.VALIGN_BOTTOM:
            case HVisible.VALIGN_CENTER:
            case HVisible.VALIGN_JUSTIFY:
            case VALIGN_DEFAULT:
                this.vAlign[1] = vAlign;
                break;
            default:
                throw new IllegalArgumentException("Invalid vertical alignment");
        }
    }

    /**
     * Notifies both <code>ExtendedLooks</code> of the
     * <code>widgetChanged</code> message before calling
     * <code>super.widgetChanged</code>.
     */
    public void widgetChanged(HVisible visible, HChangeData[] changes)
    {
        if (looks[0] != null) looks[0].widgetChanged(visible, changes);
        if (looks[1] != null) looks[1].widgetChanged(visible, changes);

        super.widgetChanged(visible, changes);
    }

    /**
     * Retrieves the maximum size of content (rendered by this look) for the
     * given <code>HVisible</code>.
     * 
     * This is defined to be the sum of the primary and secondary looks' maximum
     * content size and gap spacing in the direction of the layout orientation
     * and the maximum of their maximum content size in the direction opposite
     * of the orientation.
     * 
     * @param visible
     *            the <code>HVisible</code> whose content should be sized
     * @return the maximum size of content (rendered by this look) for the given
     *         <code>HVisible</code>
     */
    public Dimension getMaxContentSize(HVisible visible)
    {
        return getContentSize((looks[0] == null) ? null : looks[0].getMaxContentSize(visible),
                (looks[1] == null) ? null : looks[1].getMaxContentSize(visible));
    }

    /**
     * Retrieves the minimum size of content (rendered by this look) for the
     * given <code>HVisible</code>.
     * 
     * This is defined to be the sum of the primary and secondary looks' minimum
     * content size and gap spacing in the direction of the layout orientation
     * and the maximum of their minimum content size in the direction opposite
     * of the orientation.
     * 
     * @param visible
     *            the <code>HVisible</code> whose content should be sized
     * @return the minimum size of content (rendered by this look) for the given
     *         <code>HVisible</code>
     */
    public Dimension getMinContentSize(HVisible visible)
    {
        return getContentSize((looks[0] == null) ? null : looks[0].getMinContentSize(visible),
                (looks[1] == null) ? null : looks[1].getMinContentSize(visible));
    }

    /**
     * Calculates the total content size based on the given individual content
     * sizes. The orientation and gap are both taken into account. If a size
     * reference is <code>null</code>, then that size is ignored completely.
     * 
     * @param size1
     *            content size for <code>ExtendedLook</code> #1
     * @param size2
     *            content size for <code>ExtendedLook</code> #2
     * 
     * @return the sum of <code>size1</code> and <code>size2</code>, taking
     *         orientation and gap into account
     */
    private Dimension getContentSize(Dimension size1, Dimension size2)
    {
        int w = 0, h = 0;
        if (size1 != null)
        {
            w = size1.width;
            h = size1.height;
        }
        switch (getOrientation())
        {
            case ORIENT_TOP_TO_BOTTOM:
            case ORIENT_BOTTOM_TO_TOP:
                if (size2 != null)
                {
                    w = Math.max(w, size2.width);
                    h += size2.height;
                    if (size1 != null) h += getGap();
                }
                break;
            case ORIENT_LEFT_TO_RIGHT:
            case ORIENT_RIGHT_TO_LEFT:
                if (size2 != null)
                {
                    w += size2.width;
                    h = Math.max(h, size2.height);
                    if (size1 != null) w += getGap();
                }
                break;
            case ORIENT_NONE:
                if (size2 != null)
                {
                    w = Math.max(w, size2.width);
                    h = Math.max(h, size2.height);
                }
        }
        return new Dimension(w, h);
    }

    /**
     * Returns <code>true</code> if <code>supportsScaling</code> is
     * <code>true</code> for either of the component <code>ExtendedLooks</code>.
     * 
     * @return <code>true</code> if <code>supportScaling</code> is
     *         <code>true</code> for either of the component
     *         <code>ExtendedLooks</code>; <code>false</code> otherwise
     */
    public boolean supportsScaling()
    {
        return (looks[0] != null && looks[0].supportsScaling()) || (looks[1] != null && looks[1].supportsScaling());
    }

    /**
     * Returns <code>true</code> if <code>hasContent</code> is <code>true</code>
     * for either of the component <code>ExtendedLooks</code>.
     * 
     * @param the
     *            <code>HVisible</code> to query for content
     * @return <code>true</code> if <code>hasContent</code> is <code>true</code>
     *         for either of the component <code>ExtendedLooks</code>;
     *         <code>false</code> otherwise
     */
    public boolean hasContent(HVisible hvisible)
    {
        return (looks[0] != null && looks[0].hasContent(hvisible))
                || (looks[1] != null && looks[1].hasContent(hvisible));
    }

    /**
     * Retrieve the orientation of this <code>PairDecorator</code>. The
     * orientation controls how the component <code>ExtendedLooks</code> render
     * content, with respect to each other.
     * <p>
     * The orientation specifies the vertical or horizontal orientation and
     * display order.
     * 
     * @return one of {@link #ORIENT_LEFT_TO_RIGHT},
     *         {@link #ORIENT_RIGHT_TO_LEFT}, {@link #ORIENT_TOP_TO_BOTTOM}, or
     *         {@link #ORIENT_BOTTOM_TO_TOP}.
     */
    public int getOrientation()
    {
        return orientation;
    }

    /**
     * Set the orientation of the <code>PairDecorator</code>. The orientation
     * controls how the component <code>ExtendedLooks</code> render content,
     * with respect to each other.
     * <p>
     * The orientation specifies the vertical or horizontal orientation and
     * display order.
     * 
     * @param orient
     *            one of {@link #ORIENT_LEFT_TO_RIGHT},
     *            {@link #ORIENT_RIGHT_TO_LEFT}, {@link #ORIENT_TOP_TO_BOTTOM},
     *            {@link #ORIENT_BOTTOM_TO_TOP}, or {@link #ORIENT_NONE}.
     */
    public void setOrientation(int orient)
    {
        switch (orient)
        {
            case ORIENT_LEFT_TO_RIGHT:
            case ORIENT_RIGHT_TO_LEFT:
            case ORIENT_TOP_TO_BOTTOM:
            case ORIENT_BOTTOM_TO_TOP:
            case ORIENT_NONE:
                orientation = orient;
                break;
            default:
                throw new IllegalArgumentException("Invalid orientation " + orient);
        }
    }

    /**
     * Retrieves the gap (in pixels) between the <code>ExtendedLooks</code>'
     * rendering boundaries.
     * 
     * @return the gap (in pixels) between the <code>ExtendedLooks</code>'
     *         rendering boundaries
     */
    public int getGap()
    {
        return gap;
    }

    /**
     * Sets the gap (in pixels) between the <code>ExtendedLooks</code>'
     * rendering boundaries.
     * 
     * @param the
     *            gap (in pixels) between the <code>ExtendedLooks</code>'
     *            rendering boundaries
     */
    public void setGap(int gap)
    {
        this.gap = gap;
    }

    /**
     * Calculates and returns the layout of the <code>ExtendedLooks</code> based
     * on orientation, gap, and preferred sizes. When calculating the layout,
     * the following steps are taken:
     * <ol>
     * <li>If no looks are available, the return value is undefined
     * <li>If only one look is available, then it is allocated the entire
     * rendering bounds.
     * <li>The #1 look is given it's <i>preferred</i> size (filled opposite of
     * the orientation).
     * <li>The #2 look is given whatever may or may not be left over
     * </ol>
     * The definition of <i>preferred</i> size is as follows:
     * <ol>
     * <li>the maximum content size, if it fits within the given bounds.
     * <li>the minimum content size is it does not fit within <i>both</i> the
     * given width and height. <!--
     * <li>shrink, preserving the aspect ratio, if resize mode is to preserve
     * aspect.
     * <li>use the minimum of the maximum content size and the given bounds (in
     * both directions). -->
     * </ol>
     */
    protected Rectangle[] layout(HVisible visible, Rectangle bounds)
    {
        Rectangle[] layout = { new Rectangle(bounds), new Rectangle(bounds), };

        if (looks[0] == null || looks[1] == null || !looks[0].hasContent(visible) || !looks[1].hasContent(visible))
            return layout;

        int width = bounds.width;
        int height = bounds.height;

        // Get the "preferred" size of the "preferred" look
        // If it is entirely too big, then use minContentSize
        Dimension pref = looks[0].getMaxContentSize(visible);
        if (pref.width > width && pref.height > height) pref = looks[0].getMinContentSize(visible);

        // re-use width/height as secondary width/height
        height = height - pref.height - gap;
        width = width - pref.width - gap;
        if (height < 0) height = 0;
        if (width < 0) width = 0;
        switch (getOrientation())
        {
            case ORIENT_TOP_TO_BOTTOM:
                layout[0].height = pref.height;
                layout[1].y += pref.height + gap;
                layout[1].height = height;
                break;
            case ORIENT_BOTTOM_TO_TOP:
                layout[0].y += height + gap;
                layout[0].height = pref.height;
                layout[1].height = height;
                break;
            case ORIENT_LEFT_TO_RIGHT:
                layout[0].width = pref.width;
                layout[1].x += pref.width + gap;
                layout[1].width = width;
                break;
            case ORIENT_RIGHT_TO_LEFT:
                layout[1].width = width;
                layout[0].width = pref.width;
                layout[0].x += width + gap;
                break;
            case ORIENT_NONE:
                // layout is same as internal bounds for both
                break;
        }
        return layout;
    }

    /**
     * The extended looks.
     */
    private ExtendedLook[] looks = new ExtendedLook[2];

    /**
     * The horizontal alignment used for the extended looks.
     */
    private int[] hAlign = { HALIGN_DEFAULT, HALIGN_DEFAULT };

    /**
     * The vertical alignment used for the extended looks.
     */
    private int[] vAlign = { VALIGN_DEFAULT, VALIGN_DEFAULT };

    /**
     * The component ExtendedLook orientation.
     */
    private int orientation = ORIENT_LEFT_TO_RIGHT;

    /**
     * Specifies the gap between the extended looks' rendering bounds.
     */
    private int gap = 0;
}
