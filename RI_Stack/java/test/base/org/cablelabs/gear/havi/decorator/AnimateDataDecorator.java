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

import org.cablelabs.gear.data.AnimationData;
import org.cablelabs.gear.havi.HasAnimationData;

import org.havi.ui.HLook;
import org.havi.ui.HVisible;
import org.havi.ui.HState;
import org.havi.ui.HAnimateEffect;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Insets;

/**
 * <code>AnimationDataDecorator</code> implements the <code>HLook</code>
 * interface in order to render {@link AnimationData} content. In addition, it
 * provides support for the <i>decorator</i> look paradigm, making it possible
 * to build a composite look from discrete component looks.
 * 
 * @author Aaron Kamienski
 * @version $Id: AnimateDataDecorator.java,v 1.2 2002/06/03 21:32:26 aaronk Exp
 *          $
 * 
 * @see org.havi.ui.HAnimateLook
 */
public class AnimateDataDecorator extends AbstractSizingDecorator implements ExtendedLook
{
    /**
     * Default constructor. No component look is provided.
     * 
     * @see #setComponentLook(HLook)
     */
    public AnimateDataDecorator()
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
    public AnimateDataDecorator(HLook componentLook)
    {
        super(componentLook);
    }

    /**
     * Renders the <code>AnimationData</code> content of the given
     * <code>HVisible</code>.
     * <p>
     * If the <code>visible</code> component implements
     * <code>HasAnimationData</code>, then the
     * {@link HasAnimationData#getAnimationData(int)} method is used to access
     * the data to render. Otherwise, no content is rendered by this look.
     * <p>
     * Note that <code>showLook</code> differs from the standard
     * <code>HAnimateLook.showLook</code> in that it does not draw the
     * background or render any borders. This is left up to other looks or
     * decorators.
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
     * @see #showLook(Graphics, HVisible, int, Rectangle, int, int, int)
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
        if ((visible instanceof HasAnimationData) && (visible instanceof HAnimateEffect))
        {
            AnimationData content = getContent((HasAnimationData) visible, state);
            int w, h;

            // If we have content and it is fully-loaded
            if (content != null && (w = content.getWidth()) > 0 && (h = content.getHeight()) > 0)
            {
                // Calculate the size of the (scaled?) content
                Dimension size = sizeContent(resize, w, h, bounds.width, bounds.height);

                // Calculate the location of the content
                Point loc = alignLocation(bounds, size.width, size.height, hAlign, vAlign);

                // Render the content
                int position = ((HAnimateEffect) visible).getPosition();
                switch (resize)
                {
                    case HVisible.RESIZE_PRESERVE_ASPECT:
                    case HVisible.RESIZE_ARBITRARY:
                        content.draw(position, g, loc.x, loc.y, size.width, size.height, visible);
                        break;
                    case HVisible.RESIZE_NONE:
                        // Clip to bounds
                        java.awt.Graphics g2 = g.create(bounds.x, bounds.y, bounds.width, bounds.height);
                        try
                        {
                            content.draw(position, g2, loc.x - bounds.x, loc.y - bounds.y, visible);
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
    private static AnimationData getContent(HasAnimationData visible, int state)
    {
        AnimationData data = visible.getAnimationData(state);
        if (data == null)
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
        return data;
    }

    /**
     * Returns whether the given <code>hvisible</code> has any
     * {@link HVisible#getAnimateContent(int)} or not.
     * 
     * @return <code>hvisible.getAnimateContent(i) != null</code> for any
     *         <code>i</code>
     */
    public boolean hasContent(HVisible hvisible)
    {
        if (hvisible instanceof HasAnimationData)
        {
            HasAnimationData has = (HasAnimationData) hvisible;
            for (int i = (HState.FIRST_STATE & HState.ALL_STATES); i <= (HState.LAST_STATE & HState.ALL_STATES); ++i)
                if (has.getAnimationData(i | HState.NORMAL_STATE) != null) return true;
        }
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
        if (hvisible instanceof HasAnimationData)
        {
            HasAnimationData has = (HasAnimationData) hvisible;
            for (int i = (HState.FIRST_STATE & HState.ALL_STATES); i <= (HState.LAST_STATE & HState.ALL_STATES); ++i)
            {
                AnimationData data = has.getAnimationData(i | HState.NORMAL_STATE);
                if (data != null)
                {
                    Dimension d = data.getSize();

                    if (d.width > 0 && d.height > 0)
                    {
                        maxW = Math.max(maxW, d.width);
                        maxH = Math.max(maxH, d.height);
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
        if (hvisible instanceof HasAnimationData)
        {
            HasAnimationData has = (HasAnimationData) hvisible;
            for (int i = (HState.FIRST_STATE & HState.ALL_STATES); i <= (HState.LAST_STATE & HState.ALL_STATES); ++i)
            {
                AnimationData data = has.getAnimationData(i | HState.NORMAL_STATE);
                if (data != null)
                {
                    Dimension d = data.getSize();
                    if (d.width > 0 && d.height > 0)
                    {
                        minW = Math.min(minW, d.width);
                        minH = Math.min(minH, d.height);
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
