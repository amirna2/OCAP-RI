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

package org.cablelabs.gear.data;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.ImageObserver;

/**
 * An implementation of the <code>AnimationData</code> interface based upon
 * <code>Image</code> data.
 * 
 * @author Aaron Kamienski
 * @version $Id: ImageAnimation.java,v 1.2 2002/06/03 21:31:08 aaronk Exp $
 */
public class ImageAnimation implements AnimationData
{
    /**
     * Default constructor. No <code>Image</code> data has been set.
     */
    public ImageAnimation()
    {
    }

    /**
     * Creates an <code>ImageAnimation</code> based on the given
     * <code>Image</code> array.
     * 
     * @data the <code>Image</code> data that will make up the frames of the
     *       animation
     */
    public ImageAnimation(Image data[])
    {
        setImage(data);
    }

    // Description copied from AnimationData
    public void draw(int i, Graphics g, int x, int y, ImageObserver io)
    {
        Image img = images[i];
        if (img != null) g.drawImage(img, x, y, io);
    }

    // Description copied from AnimationData
    public void draw(int i, Graphics g, int x, int y, int width, int height, ImageObserver io)
    {
        Image img = images[i];
        if (img != null) g.drawImage(img, x, y, width, height, io);
    }

    // Description copied from AnimationData
    public Dimension getSize()
    {
        return new Dimension(getSizeInternal());
    }

    /**
     * Returns the size of this animation (as specified for <code>getSize</code>
     * ). This may be calculated on the fly as necessary or it may return a
     * cached <code>Dimension</code> object.
     * 
     * @return a newly created or privately cached <code>Dimension</code> giving
     *         the maximum size of this animation
     */
    private Dimension getSizeInternal()
    {
        // If necessary, calculate new maximum size
        if (size == null)
        {
            int maxW = 0, maxH = 0;

            if (images != null)
            {
                for (int i = 0; i < images.length; ++i)
                {
                    Image img = images[i];
                    int w, h;
                    if (img != null && (w = img.getWidth(null)) > 0 && (h = img.getHeight(null)) > 0)
                    {
                        maxW = Math.max(maxW, w);
                        maxH = Math.max(maxH, h);
                    }
                }
            }
            size = new Dimension(maxW, maxH);
        }

        return size;
    }

    // Description copied from AnimationData
    public int getWidth()
    {
        return getSizeInternal().width;
    }

    // Description copied from AnimationData
    public int getHeight()
    {
        return getSizeInternal().height;
    }

    // Description copied from AnimationData
    public int getLength()
    {
        return (images == null) ? 0 : images.length;
    }

    /**
     * Returns the array of <code>java.awt.Image</code> objects that make up the
     * individual frames of this <code>ImageAnimation</code>.
     * 
     * @return the array of <code>java.awt.Image</code> objects that make up the
     *         individual frames of this <code>ImageAnimation</code>
     * @see #setImage(Image[])
     * @see #setImage(int,Image)
     * @see #getImage(int)
     */
    public Image[] getImage()
    {
        // Return a copy
        Image[] retImages = null;
        Image[] tmp = images;
        if (tmp != null)
        {
            retImages = new Image[tmp.length];
            System.arraycopy(tmp, 0, retImages, 0, tmp.length);
        }
        return retImages;
    }

    /**
     * Sets the array of <code>java.awt.Image</code> objects that make up the
     * individual frames of this <code>ImageAnimation</code>.
     * 
     * @param imgs
     *            the array of <code>java.awt.Image</code> objects that make up
     *            the individual frames of this <code>ImageAnimation</code>
     * @see #getImage()
     * @see #setImage(int,Image)
     * @see #getImage(int)
     */
    public void setImage(Image[] imgs)
    {
        size = null; // clear cached size
        images = imgs;
    }

    /**
     * Returns an individual <code>java.awt.Image</code> frame at the specified
     * index.
     * 
     * @return the <code>java.awt.Image</code> at the specified index
     * @see #getImage()
     * @see #setImage(Image[])
     * @see #setImage(int,Image)
     */
    public Image getImage(int i)
    {
        return images[i];
    }

    /**
     * Sets the individual <code>java.awt.Image</code> frame at the specified
     * index.
     * 
     * @param i
     *            the index of the frame to be set
     * @param img
     *            the <code>java.awt.Image</code> at the specified index
     * @see #getImage()
     * @see #setImage(Image[])
     * @see #getImage(int)
     * 
     * @throws IndexOutOfBoundsException
     *             if the index is outside the current array bounds (
     *             {@link #setImage(Image[])} must be used to change the size of
     *             the array)
     */
    public void setImage(int i, Image img)
    {
        size = null; // clear cached size
        images[i] = img;
    }

    /**
     * Overrides <code>Object.toString()</code> to provide descriptive
     * information about this object.
     * 
     * @return a <code>String</code> representation of this object
     */
    public String toString()
    {
        String str;
        if (images == null || images.length == 0)
            str = "<empty>";
        else
        {
            StringBuffer buf = new StringBuffer();
            String comma = "";

            for (int i = 0; i < images.length; ++i)
            {
                buf.append(comma);
                buf.append(images[i]);
                comma = ",";
            }
            str = buf.toString();
        }
        return super.toString() + " [" + str + "]";
    }

    /**
     * The Images that make up the frames of this animation.
     */
    private Image[] images;

    /**
     * The current total <i>size</i> of the animation frames, if currently set.
     */
    private Dimension size;
}
