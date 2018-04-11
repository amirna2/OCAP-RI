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
import org.havi.ui.HAnimateEffect;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Insets;
import java.awt.Image;

/**
 * Replacement for {@link org.havi.ui.HAnimateLook HAnimateLook} which fits
 * within the {@link DecoratorLook} hierarchy, <code>AnimateDecorator</code>
 * renders the animate content of a component. However,
 * <code>AnimateDecorator</code> never fills in the background as
 * <code>HAnimateLook</code> may.
 * <p>
 * An <code>AnimateDecorator</code> can be used as a <i>leaf</i> object in a
 * decorator chain or it can be used as a integral part of a decorator chain,
 * complete with its own component look. For example, a
 * <code>AnimateDecorator</code> can be chained with a
 * <code>TextDecorator</code> to display text content on top of animated
 * content, with no regard to placement.
 * <p>
 * <i><b>Note</b></i> that <code>AnimateDecorator</code> cannot be used as a
 * direct replacement for an <code>HAnimateLook</code> -- it is not an
 * <code>HAnimateLook</code>, so components that require an
 * <code>HAnimateLook</code> may throw <code>HInvalidLookException</code>. In
 * which case a <code>AnimateDecorator</code> should be wrapped in a
 * <code>AnimateLookAdapter</code>.
 * 
 * @author Aaron Kamienski
 * @author Jeff Bonin (havi 1.0.1 update)
 * @version $Id: AnimateDecorator.java,v 1.12 2002/06/03 21:32:26 aaronk Exp $
 * 
 * @see org.havi.ui.HAnimateLook
 */
public class AnimateDecorator extends AbstractSizingDecorator implements ExtendedLook
{
    /**
     * Default constructor. No component look is provided.
     * 
     * @see #setComponentLook(HLook)
     */
    public AnimateDecorator()
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
    public AnimateDecorator(HLook componentLook)
    {
        super(componentLook);
    }

    /**
     * Similar to <code>HAnimateLook.showLook</code> but does not fill the
     * bounds of the <code>HVisible</code> with its <i>background</i> color
     * (rendering it <i>transparent</i>) or draw borders.
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
     * @see org.havi.ui.HAnimateLook#showLook(Graphics, HVisible, int)
     */
    public void showLook(java.awt.Graphics g, HVisible visible, int state)
    {
        // Do NOT draw the background

        Dimension size = visible.getSize();
        Insets insets = getInsets(visible);
        showLook(g, visible, state, new Rectangle(insets.left, insets.top, size.width - insets.left - insets.right,
                size.height - insets.top - insets.bottom), visible.getHorizontalAlignment(),
                visible.getVerticalAlignment(), visible.getResizeMode());

        // Do NOT draw border decorations

        super.showLook(g, visible, state);
    }

    // Description copied from superclass
    public void showLook(java.awt.Graphics g, HVisible visible, int state, Rectangle bounds, int hAlign, int vAlign,
            int resize)
    {
        Image[] array;

        // If we have content and it is fully-loaded
        if ((visible instanceof HAnimateEffect) && (array = getContent(visible, state)) != null)
        {
            HAnimateEffect anim = (HAnimateEffect) visible;
            int i = anim.getPosition();
            Image content;
            int w, h;

            if (array.length > i && (content = array[i]) != null && (w = content.getWidth(visible)) > 0
                    && (h = content.getHeight(visible)) > 0)
            {
                Dimension size = sizeContent(resize, w, h, bounds.width, bounds.height);
                Point loc = alignLocation(bounds, size.width, size.height, hAlign, vAlign);

                switch (resize)
                {
                    case HVisible.RESIZE_PRESERVE_ASPECT:
                    case HVisible.RESIZE_ARBITRARY:
                        g.drawImage(content, loc.x, loc.y, size.width, size.height, visible);
                        break;
                    case HVisible.RESIZE_NONE:
                        java.awt.Graphics g2 = g.create(bounds.x, bounds.y, bounds.width, bounds.height);
                        try
                        {
                            g2.drawImage(content, loc.x - bounds.x, loc.y - bounds.y, visible);
                        }
                        finally
                        {
                            g2.dispose();
                        }
                }
            }
        }
        return;
    }

    /**
     * Gets the content for the given state. If no content exists, attempts to
     * retrieve the nearest appropriate content.
     */
    private static Image[] getContent(HVisible visible, int state)
    {
        Image[] animate = visible.getAnimateContent(state);
        if (animate == null)
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
        return animate;
    }

    /**
     * Returns whether the given <code>hvisible</code> has any
     * {@link HVisible#getAnimateContent(int) animate} content or not.
     * 
     * @return <code>hvisible.getAnimateContent(i) != null</code> for any
     *         <code>i</code>
     */
    public boolean hasContent(HVisible hvisible)
    {
        for (int i = (HState.FIRST_STATE & HState.ALL_STATES); i <= (HState.LAST_STATE & HState.ALL_STATES); ++i)
            if (hvisible.getAnimateContent(i | HState.NORMAL_STATE) != null) return true;
        return false;
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
        int maxW = 0, maxH = 0;

        // largest size of graphic content
        for (int i = (HState.FIRST_STATE & HState.ALL_STATES); i <= (HState.LAST_STATE & HState.ALL_STATES); ++i)
        {
            Image[] array = hvisible.getAnimateContent(i | HState.NORMAL_STATE);
            if (array != null)
            {
                for (int j = 0; j < array.length; ++j)
                {
                    Image img;
                    if ((img = array[j]) != null)
                    {
                        int width = img.getWidth(hvisible);
                        int height = img.getHeight(hvisible);

                        if (width > 0 && height > 0)
                        {
                            maxW = Math.max(maxW, width);
                            maxH = Math.max(maxH, height);
                        }
                    }
                }
            }
        }
        return new Dimension(maxW, maxH);
    }

    /**
     * Calculates the smallest dimensions of all content.
     * 
     * @param the
     *            <code>HVisible</code> to query for content
     * @return the smallest dimensions of all content.
     */
    public Dimension getMinContentSize(HVisible hvisible)
    {
        int minW = Integer.MAX_VALUE, minH = Integer.MAX_VALUE;

        // smallest size of graphic content
        for (int i = (HState.FIRST_STATE & HState.ALL_STATES); i <= (HState.LAST_STATE & HState.ALL_STATES); ++i)
        {
            Image[] array = hvisible.getAnimateContent(i | HState.NORMAL_STATE);
            if (array != null)
            {
                for (int j = 0; j < array.length; ++j)
                {
                    Image img;
                    int width, height;
                    if ((img = array[j]) != null && (width = img.getWidth(hvisible)) > 0
                            && (height = img.getHeight(hvisible)) > 0)
                    {
                        minW = Math.min(minW, width);
                        minH = Math.min(minH, height);
                    }
                }
            }
        }
        if (minW == Integer.MAX_VALUE) minW = 0;
        if (minH == Integer.MAX_VALUE) minH = 0;
        return new Dimension(minW, minH);
    }

    /**
     * Returns whether the <code>HLook</code> in question supports sizing of
     * content or not.
     * 
     * @return <code>true</code>
     */
    public boolean supportsScaling()
    {
        return true;
    }
}
