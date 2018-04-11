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

package org.cablelabs.impl.havi;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import org.havi.ui.HVisible;

/**
 * A utility class used to render <code>Images</code>.
 * 
 * @author Aaron Kamienski
 * @version $Id: ImageRender.java,v 1.4 2002/06/03 21:32:58 aaronk Exp $
 */
public class ImageRender
{
    /**
     * Renders the <code>Image</code> to the given <code>Graphics</code>
     * context. The <code>Image</code> is rendered within the given
     * <code>bounds</code>, following the alignment and resize rules as
     * specified by the <code>HVisible</code>.
     * 
     * @param g
     *            current graphics context
     * @param image
     *            image to render
     * @param bounds
     *            rendering bounds (within current graphics context)
     * @param visible
     *            specifies resize mode and alignment; used as
     *            <code>ImageObserver</code>
     */
    public static void render(java.awt.Graphics g, java.awt.Image image, Rectangle bounds, HVisible visible)
    {
        int w, h;

        if (image != null && (w = image.getWidth(visible)) > 0 && (h = image.getHeight(visible)) > 0)
        {
            // Size the image to resizeMode
            int resizeMode = visible.getResizeMode();
            Dimension size = imageSize(resizeMode, w, h, bounds.width, bounds.height);

            // Align the image to horiz/vert alignment
            Point loc = alignLocation(bounds, size.width, size.height, visible.getHorizontalAlignment(),
                    visible.getVerticalAlignment());

            // Render the image
            switch (resizeMode)
            {
                case HVisible.RESIZE_PRESERVE_ASPECT:
                case HVisible.RESIZE_ARBITRARY:
                    g.drawImage(image, loc.x, loc.y, size.width, size.height, visible);
                    break;
                case HVisible.RESIZE_NONE:
                    java.awt.Graphics g2 = g.create(bounds.x, bounds.y, bounds.width, bounds.height);
                    try
                    {
                        g2.drawImage(image, loc.x - bounds.x, loc.y - bounds.y, visible);
                    }
                    finally
                    {
                        g2.dispose();
                    }
            }
        }
        return;
    }

    /**
     * Calculate the image size based on the actual image size, the size of the
     * rendering bounds, and the resize mode.
     * 
     * @param resizeMode
     *            the current resize mode
     * @param w
     *            the actual Image width
     * @param h
     *            the actual Image height
     * @param width
     *            the rendering width
     * @param height
     *            the rendering height
     */
    private static Dimension imageSize(int resizeMode, int w, int h, int width, int height)
    {
        switch (resizeMode)
        {
            default:
            case HVisible.RESIZE_NONE:
                return new Dimension(w, h);
            case HVisible.RESIZE_PRESERVE_ASPECT:
                return scaleBounds(w, h, width, height);
            case HVisible.RESIZE_ARBITRARY:
                return new Dimension(width, height);
        }
    }

    /**
     * Calculates a new location based on the requested alignment.
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
    private static Point alignLocation(Rectangle r, int width, int height, int hAlign, int vAlign)
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
    public static Dimension scaleBounds(int w, int h, int width, int height)
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
}
