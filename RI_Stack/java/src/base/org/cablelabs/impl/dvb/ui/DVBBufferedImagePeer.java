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
package org.cablelabs.impl.dvb.ui;

import java.awt.*;
import java.awt.image.*;

/**
 * Peer interface providing platform-specific implementation of
 * <code>DVBBufferedImage</code> native peer.
 * <p>
 * After the {@link #dispose} method is called, the
 * <code>DVBBufferedImagePeer</code> should not longer be used. Any calls to
 * subsequent operations will have unspecified results.
 * <p>
 * Also defines an {@link DVBBufferedImagePeer.Factory interface} that
 * implementations can implement to provide a <i>factory</i> object which can
 * create instances of a peer.
 * 
 * @see org.dvb.ui.DVBBufferedImage
 * @author Aaron Kamienski
 */
public interface DVBBufferedImagePeer
{
    /**
     * An <i>object factory</i> interface that creates
     * <code>DVBBufferedImagePeer</code> implementations.
     * 
     * @see org.dvb.ui.DVBBufferedImage
     * @author Aaron Kamienski
     */
    public static interface Factory
    {
        /**
         * Creates and returns a new DVBBufferedImagePeer with the requested
         * width and height in an implicit <i>basic</i> Sample Model.
         * 
         * @param width
         *            the width of the DVBBufferedImagePeer
         * @param height
         *            the height of the DVBBufferedImagePeer
         */
        public DVBBufferedImagePeer newInstance(int w, int h);

        /**
         * Creates and returns a new DVBBufferedImagePeer with the requested
         * width and height in the Sample Model specified by type.
         * 
         * @param width
         *            the width of the DVBBufferedImagePeer
         * @param height
         *            the height of the DVBBufferedImagePeer
         * @param advanced
         *            if <code>true</code> then an image with an <i>advanced</i>
         *            Sample Model is requested; otherwise an image with a
         *            <i>base</i> Sample Model is requested
         */
        public DVBBufferedImagePeer newInstance(int w, int h, boolean advanced);
    }

    /**
     * Returns a <code>Graphics</code> for drawing into this
     * <code>DVBBufferedImagePeer</code>. This should be a
     * <code>DVBGraphics</code>, but may not be.
     * 
     * @return a <code>Graphics</code>, used for drawing into this image.
     */
    public Graphics getGraphics();

    /**
     * Flushes all resources being used to cache optimization information. The
     * underlying pixel data is unaffected.
     */
    public void flush();

    /**
     * Returns the height of the image peer.
     * 
     * @return the height of this image peer.
     */
    public int getHeight();

    /**
     * Returns the height of the image. If the height is not known yet then the
     * <code>ImageObserver</code> is notified later and <code>-1</code> is
     * returned.
     * 
     * @param observer
     *            the <code>ImageObserver</code> that receives information about
     *            the image
     * @return the height of the image or <code>-1</code> if the height is not
     *         yet known.
     * @see java.awt.Image#getWidth(ImageObserver)
     * @see ImageObserver
     */
    public int getHeight(ImageObserver observer);

    /**
     * Returns the width of the image peer.
     * 
     * @return the width of this image peer.
     */
    public int getWidth();

    /**
     * Returns the width of the image. If the width is not known yet then the
     * {@link ImageObserver} is notified later and <code>-1</code> is returned.
     * 
     * @param observer
     *            the <code>ImageObserver</code> that receives information about
     *            the image
     * @return the width of the image or <code>-1</code> if the width is not yet
     *         known.
     * @see java.awt.Image#getHeight(ImageObserver)
     * @see ImageObserver
     */
    public int getWidth(ImageObserver observer);

    /**
     * Returns a java.awt.Image representing this buffered image. In
     * implementations which implement java.awt.image.BufferedImage this returns
     * a java.awt.image.BufferedImage cast to a java.awt.Image. Otherwise it
     * returns an instance of an underlying platform specific sub-class of
     * java.awt.Image.
     * 
     * @return a java.awt.image representing this buffered image
     */
    public Image getImage();

    /**
     * Disposes of this buffered image. This method releases the resources (e.g.
     * pixel memory) underlying this buffered image.
     * <p>
     * After calling this method, the peer should no longer be referenced. Any
     * subsequent operations on this peer will result in undefined behavior.
     */
    public void dispose();

    /**
     * Returns a property of the image by name. Individual property names are
     * defined by the various image formats. If a property is not defined for a
     * particular image, this method returns the <code>UndefinedProperty</code>
     * field. If the properties for this image are not yet known, then this
     * method returns <code>null</code> and the <code>ImageObserver</code>
     * object is notified later. The property name "comment" should be used to
     * store an optional comment that can be presented to the user as a
     * description of the image, its source, or its author.
     * 
     * @param name
     *            the property name
     * @param observer
     *            the <code>ImageObserver</code> that receives notification
     *            regarding image information
     * @return an {@link Object} that is the property referred to by the
     *         specified <code>name</code> or <code>null</code> if the
     *         properties of this image are not yet known.
     * @see ImageObserver
     * @see java.awt.Image#UndefinedProperty
     */
    public Object getProperty(String name, ImageObserver observer);

    /**
     * Returns the specified integer pixel in the default RGB color model
     * (TYPE_INT_ARGB) and default sRGB colorspace. Color conversion takes place
     * if the used Sample Model is not 8-bit for each color component There are
     * only 8-bits of precision for each color component in the returned data
     * when using this method. Note that when a lower precision is used in this
     * buffered image getRGB may return different values than those used in
     * setRGB()
     * 
     * @param x
     *            the x-coordinate of the pixel
     * @param y
     *            the y-coordinate of the pixel
     * @return an integer pixel in the default RGB color model (TYPE_INT_ARGB)
     *         and default sRGB colorspace.
     * @throws ArrayIndexOutOfBoundsException
     *             if x or y is out of bounds
     */
    public int getRGB(int x, int y) throws ArrayIndexOutOfBoundsException;

    /**
     * Returns an array of integer pixels in the default RGB color model
     * (TYPE_INT_ARGB) and default sRGB color space, from a rectangular region
     * of the image data. There are only 8-bits of precision for each color
     * component in the returned data when using this method. With a specified
     * coordinate (x,&nbsp;y) in the image, the ARGB pixel can be accessed in
     * this way:
     * 
     * <pre>
     * pixel = rgbArray[offset + (y - startY) * scansize + (x - startX)];
     * </pre>
     * 
     * @param startX
     *            the x-coordinate of the upper-left corner of the specified
     *            rectangular region
     * @param startY
     *            the y-coordinate of the upper-left corner of the specified
     *            rectangular region
     * @param w
     *            the width of the specified rectangular region
     * @param h
     *            the height of the specified rectangular region
     * @param rgbArray
     *            if not <code>null</code>, the rgb pixels are written here
     * @param offset
     *            offset into the <code>rgbArray</code>
     * @param scansize
     *            scanline stride for the <code>rgbArray</code>
     * @return array of ARGB pixels.
     * @throws ArrayIndexOutOfBoundsException
     *             if the specified portion of the image data is out of bounds
     */
    public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize)
            throws ArrayIndexOutOfBoundsException;

    /**
     * Sets a pixel in this image peer to the specified ARGB value. The pixel is
     * assumed to be in the default RGB color model, TYPE_INT_ARGB, and default
     * sRGB color space.
     * 
     * @param x
     *            the x-coordinate of the pixel to set
     * @param y
     *            the y-coordinate of the pixel to set
     * @param rgb
     *            the ARGB value
     */
    public void setRGB(int x, int y, int rgb);

    /**
     * Sets an array of integer pixels in the default RGB color model
     * (TYPE_INT_ARGB) and default sRGB color space, into a rectangular portion
     * of the image data. There are only 8-bits of precision for each color
     * component in the returned data when using this method. With a specified
     * coordinate (x,&nbsp;y) in the this image, the ARGB pixel can be accessed
     * in this way:
     * 
     * <pre>
     * pixel = rgbArray[offset + (y - startY) * scansize + (x - startX)];
     * </pre>
     * 
     * WARNING: No dithering takes place.
     * <p>
     * 
     * @param startX
     *            the x-coordinate of the upper-left corner of the specified
     *            rectangular region
     * @param startY
     *            the y-coordinate of the upper-left corner of the specified
     *            rectangular region
     * @param w
     *            the width of the specified rectangular region
     * @param h
     *            the height of the specified rectangular region
     * @param rgbArray
     *            the ARGB pixels
     * @param offset
     *            offset into the <code>rgbArray</code>
     * @param scansize
     *            scanline stride for the <code>rgbArray</code>
     */
    public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize);

    /**
     * Returns the object that produces the pixels for the image.
     * 
     * @return the {@link ImageProducer} that is used to produce the pixels for
     *         this image.
     *         <p>
     *         The source returned by this method is platform generated to
     *         provide access to the current contents of the image peer buffer.
     * @see ImageProducer
     */
    public ImageProducer getSource();

    /**
     * Returns a subimage defined by a specified rectangular region. The
     * returned image peer shares the same data array as the original image.
     * 
     * @param x
     *            the x-coordinate of the upper-left corner of the specified
     *            rectangular region
     * @param y
     *            the y-coordinate of the upper-left corner of the specified
     *            rectangular region
     * @param w
     *            the width of the specified rectangular region
     * @param h
     *            the height of the specified rectangular region
     * @return a image peer that is the subimage of this image peer; or
     *         <code>null</code> if an error occurred.
     */
    public DVBBufferedImagePeer getSubimage(int x, int y, int w, int h);

    /**
     * Creates a scaled version of this image. A new <code>Image</code> object
     * is returned which will render the image at the specified
     * <code>width</code> and <code>height</code> by default. The new
     * <code>Image</code> object may be loaded asynchronously even if the
     * original source image has already been loaded completely. If either the
     * <code>width</code> or <code>height</code> is a negative number then a
     * value is substituted to maintain the aspect ratio of the original image
     * dimensions.
     * 
     * @param width
     *            the width to which to scale the image.
     * @param height
     *            the height to which to scale the image.
     * @param hints
     *            flags to indicate the type of algorithm to use for image
     *            resampling.
     * @return a scaled version of the image.
     */
    public java.awt.Image getScaledInstance(int width, int height, int hints);
}
