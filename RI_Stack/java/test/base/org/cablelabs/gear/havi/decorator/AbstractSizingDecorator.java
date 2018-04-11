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

import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * <code>AbstractSizingDecorator</code> extends <code>DecoratorLook</code> to
 * provide the basis for an implementation of proper <code>HLook</code> sizing
 * methods. <code>AbstractSizingDecorator</code> implements the
 * {@link #getPreferredSize(HVisible)}, {@link #getMaximumSize(HVisible)}, and
 * {@link #getMaximumSize(HVisible)} methods according to the algorithms
 * described by the HAVi <code>HLook</code> specification.
 * <p>
 * Note that <code>AbstractSizingDecorator</code> adds one additional step to
 * the algorithms specified by HAVi <code>HLook</code>: it returns the maximum
 * of the size specified by this look and that specified by the
 * {@link #getComponentLook() component look}.
 * <p>
 * Subclasses round out the implementation by implementing the following
 * methods:
 * <ul>
 * <li> {@link #supportsScaling()}
 * <li> {@link #getMaxContentSize(HVisible)}
 * <li> {@link #getMinContentSize(HVisible)}
 * <li> {@link #hasContent(HVisible)}
 * </ul>
 * 
 * @see org.havi.ui.HLook#getPreferredSize(HVisible)
 * @see org.havi.ui.HLook#getMaximumSize(HVisible)
 * @see org.havi.ui.HLook#getMinimumSize(HVisible)
 * 
 * @author Aaron Kamienski
 * @version $Id: AbstractSizingDecorator.java,v 1.2 2002/06/03 21:32:26 aaronk
 *          Exp $
 */
public abstract class AbstractSizingDecorator extends DecoratorLook
{
    /**
     * Default constructor. No component look is provided.
     * 
     * @see #setComponentLook(HLook)
     */
    protected AbstractSizingDecorator()
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
    protected AbstractSizingDecorator(HLook componentLook)
    {
        super(componentLook);
    }

    /**
     * Implements <code>getPreferredSize</code> as specified by the
     * {@link HLook#getPreferredSize(HVisible) HAVi specification} with an
     * additional step. That additional step is that the preferredSize is the
     * maximum of the preferredSize as determined by this look and a
     * {@link #getComponentLook() component look}.
     * 
     * @return maximum of the preferred size calculation for the given
     *         <code>visible</code> and the dimensions returned by
     *         <code>getComponentLook().getPreferredSize()</code>
     */
    public java.awt.Dimension getPreferredSize(HVisible hvisible)
    {
        Dimension size = null;
        boolean hasContent = false;

        if ((size = hvisible.getDefaultSize()) != null && size != HVisible.NO_DEFAULT_SIZE)
        {
            // make copy so we don't change the reference recieved from the
            // HVisible
            size = new Dimension(size);

            // use default size + insets
            // go with size, add insets below before returning...
        }
        else if ((hasContent = hasContent(hvisible))
                && (!supportsScaling() || hvisible.getResizeMode() == HVisible.RESIZE_NONE))
        {
            size = getMaxContentSize(hvisible);
            // add insets below, before returning...
        }
        else if (hasContent /* && resize != HVisible.RESIZE_NONE */)
        {
            // Why no accounting for border decoration?
            return maxSize(hvisible.getSize(), super.getPreferredSize(hvisible));
        }
        else
        // No content && no default size
        {
            return maxSize(hvisible.getSize(), super.getPreferredSize(hvisible));
        }

        return maxSize(addInsets(hvisible, size), super.getPreferredSize(hvisible));
    }

    /**
     * Implements <code>getMaximumSize</code> as specified by the
     * {@link HLook#getMaximumSize(HVisible) HAVi specification} with an
     * additional step. That additional step is that the maximumSize is the
     * maximum of the maximumSize as determined by this look and a
     * {@link #getComponentLook() component look}.
     * 
     * @return maximum of the maximum size calculation for the given
     *         <code>visible</code> and the dimensions returned by
     *         <code>getComponentLook().getMaximumSize()</code>
     */
    public java.awt.Dimension getMaximumSize(HVisible hvisible)
    {
        Dimension size;

        if (supportsScaling() && hvisible.getResizeMode() != HVisible.RESIZE_NONE)
        {
            size = hvisible.getSize();
        }
        else if (hasContent(hvisible))
        {
            // largest size + border decoration
            size = addInsets(hvisible, getMaxContentSize(hvisible));
        }
        else
        // no scaling && no content
        {
            size = new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
        }

        return maxSize(size, super.getMaximumSize(hvisible));
    }

    /**
     * Implements <code>getMinimumSize</code> as specified by the
     * {@link HLook#getMinimumSize(HVisible) HAVi specification} with an
     * additional step. That additional step is that the minimumSize is the
     * maximum of the minimumSize as determined by this look and a
     * {@link #getComponentLook() component look}.
     * 
     * @return maximum of the minimum size calculation for the given
     *         <code>visible</code> and the dimensions returned by
     *         <code>getComponentLook().getMinimumSize()</code>
     */
    public java.awt.Dimension getMinimumSize(HVisible hvisible)
    {
        Dimension size = null;
        boolean hasContent = hasContent(hvisible);

        if (hasContent && (supportsScaling() && hvisible.getResizeMode() != HVisible.RESIZE_NONE))
        {
            // smallest content + border decoration
            size = getMinContentSize(hvisible);
        }
        else if (hasContent /* && resize == HVisible.RESIZE_NONE */)
        {
            // largest content + border decoration
            size = getMaxContentSize(hvisible);
        }
        else if ((size = hvisible.getDefaultSize()) != null && size != HVisible.NO_DEFAULT_SIZE)
        {
            // make copy so we don't change the reference recieved from the
            // HVisible
            size = new Dimension(size);

            // use default size + insets
            // go with size, add insets below before returning...
        }
        else
        // no content && no default size
        {
            // implementation-specific minimum + border decorations
            size = new Dimension(0, 0);
        }

        return maxSize(addInsets(hvisible, size), super.getMinimumSize(hvisible));
    }

    /**
     * Returns the maximum of the two given sizes. Note the <code>d1</code> is
     * modified in the process.
     * 
     * @return <code>d1</code> modified such that
     * 
     *         <pre>
     * d1.width = Math.max(d1.width, d2.width);
     * d1.height = Math.max(d1.height, d2.height);
     * </pre>
     */
    private Dimension maxSize(Dimension d1, Dimension d2)
    {
        d1.width = Math.max(d1.width, d2.width);
        d1.height = Math.max(d1.height, d2.height);
        return d1;
    }

    /**
     * Adds the insets to the given dimension and returns it.
     * 
     * @param hvisible
     *            the associated <code>HVisible</code>
     * @param look
     *            the HLook that can provide the insets
     * @param dimension
     *            the <code>Dimension</code> to adjust
     * @return the given <code>Dimension</code> object with the
     *         <code>width</code> and <code>height</code> adjusted by the
     *         current <code>Insets</code>.
     */
    private Dimension addInsets(HVisible hvisible, Dimension dimension)
    {
        Insets insets = getInsets(hvisible);
        dimension.width += insets.left + insets.right;
        dimension.height += insets.top + insets.bottom;
        return dimension;
    }

    /**
     * Calculate the rendered content size based on the actual content size, the
     * size of the rendering bounds, and the resize mode.
     * <p>
     * This is a utility method used in actually rendering content (i.e.,
     * determining where it should be drawn).
     * 
     * @param resizeMode
     *            the current resize mode
     * @param w
     *            the actual content width
     * @param h
     *            the actual content height
     * @param width
     *            the rendering width
     * @param height
     *            the rendering height
     */
    static Dimension sizeContent(int resizeMode, int w, int h, int width, int height)
    {
        switch (resizeMode)
        {
            default:
            case HVisible.RESIZE_NONE:
                return new Dimension(w, h); // actual size
            case HVisible.RESIZE_ARBITRARY:
                return new Dimension(width, height); // current bounds
            case HVisible.RESIZE_PRESERVE_ASPECT:
                return scaleBounds(w, h, width, height);
        }
    }

    /**
     * Calculates a new location based on the requested alignment.
     * <p>
     * This is a utility method used in actually rendering content (i.e.,
     * determining where it should be drawn).
     * 
     * @param r
     *            the current bounds
     * @param width
     *            the image drawing width
     * @param height
     *            the image drawing height
     * @param hAlign
     *            the desired horizontal alignment
     * @param vAlign
     *            the desired vertical alignment
     */
    static Point alignLocation(Rectangle r, int width, int height, int hAlign, int vAlign)
    {
        int x = r.x;
        int y = r.y;

        switch (hAlign)
        {
            case HVisible.HALIGN_LEFT:
                // x = r.x
                break;
            case HVisible.HALIGN_JUSTIFY:
            case HVisible.HALIGN_CENTER:
                x = r.x + (r.width - width) / 2;
                break;
            case HVisible.HALIGN_RIGHT:
                x = (r.x + r.width) - width;
                break;
        }
        switch (vAlign)
        {
            case HVisible.VALIGN_TOP:
                // y = r.y
                break;
            case HVisible.VALIGN_JUSTIFY:
            case HVisible.VALIGN_CENTER:
                y = r.y + (r.height - height) / 2;
                break;
            case HVisible.VALIGN_BOTTOM:
                y = (r.y + r.height) - height;
                break;
        }

        return new Point(x, y);
    }

    /**
     * Calculates the scaling bounds of an <i>item</i> of the given dimensions (
     * <code>w</code> and <code>h</code>) within the given
     * <code>Rectangle</code>, assuming that the width-to-height ratio should be
     * preserved.
     * 
     * @param w
     *            the width of the object to scale
     * @param h
     *            the height of the object to scale
     * @param width
     *            the width of the space within which an object of the given w/h
     *            should be scaled
     * @param height
     *            the height of the space within which an object of the given
     *            w/h should be scaled
     */
    private static Dimension scaleBounds(int w, int h, int width, int height)
    {
        /*
         * We wish to preserve the scaling of the image. AWT does not do this
         * for us automatically. So we must determine which of two scaling
         * factors we should use: horizontal or vertical.
         * 
         * double hScale = width / (double)w double vScale = height / (double)h
         * 
         * We want to use the smallest scaling value, because it will allow us
         * to preserve scaling in the given bounds.
         * 
         * If we choose hScale, then we use 'width' and scale 'h' by hScale;
         * 'width' is already 'scaled'. Vice versa for vScale.
         * 
         * In order to avoid double precision multiplication we won't actually
         * compute hScale and vScale. Instead of comparing: (width/(double)w) <
         * (height / (double)h) We will compare: width*h < height*w Instead of
         * scaling 'h', for example, by: (int)(h*hScale) We will scale like so:
         * (h*width)/w
         */
        int wScale = width * h;
        int hScale = height * w;

        // Calculate the smallest scaling value
        // Then, scale both width and height by the same
        if (wScale < hScale)
        {
            // h must be scaled, width already is
            return new Dimension(width, wScale / w);
        }
        else
        {
            // w must be scaled, height already is
            return new Dimension(hScale / h, height);
        }
    }

    /**
     * Returns whether the <code>HLook</code> in question supports sizing of
     * content or not. Looks that do not draw any content are expected to return
     * <code>false</code>. For example, the <code>HGraphicLook</code> supports
     * scaling but the <code>HRangeLook</code> does not.
     * 
     * @return <code>true</code> if scaling of content is supported;
     *         <code>false</code> otherwise
     */
    protected abstract boolean supportsScaling();

    /**
     * Calculates the largest dimensions of all content.
     * 
     * @param the
     *            <code>HVisible</code> to query for content
     * @return the largest dimensions of all content.
     */
    protected abstract Dimension getMaxContentSize(HVisible hvisible);

    /**
     * Calculates the smallest dimensions of all content.
     * 
     * @param the
     *            <code>HVisible</code> to query for content
     * @return the smallest dimensions of all content.
     */
    protected abstract Dimension getMinContentSize(HVisible hvisible);

    /**
     * Returns whether the given <code>HVisible</code> has any content of the
     * appropriate type or not.
     * 
     * @return <code>true</code> if the <code>hvisible</code> has the
     *         appropriate content
     */
    protected abstract boolean hasContent(HVisible hvisible);
}
