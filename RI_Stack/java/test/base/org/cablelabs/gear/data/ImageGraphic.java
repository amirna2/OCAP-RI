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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.ImageObserver;
import java.net.URL;

/**
 * An implementation of <code>GraphicData</code> which is based upon a
 * {@link java.awt.Image}. Images can be automatically loaded from filesystem-,
 * URL-, or class resource-based files. The loading of the image is tracked
 * using a <code>MediaTracker</code>, alleving the user of this task.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.8 $, $Date: 2002/06/03 21:31:07 $
 */
public class ImageGraphic implements GraphicData
{
    /**
     * Default constructor. No {@link java.awt.Image} has been assigned.
     * 
     * <p>
     * 
     * Implicitly does the equivalent to:
     * 
     * <pre>
     * this.setInitString(null);
     * </pre>
     * 
     * @see #setImage(Image)
     */
    public ImageGraphic()
    {
    }

    /**
     * <code>Image</code> constructor. The given <code>Image</code> is
     * associated with this <code>ImageGraphic</code>.
     * 
     * <p>
     * 
     * Implicitly does the equivalent to:
     * 
     * <pre>
     * this.setInitString(null);
     * </pre>
     * 
     * @param img
     *            the image to associate with this <code>ImageGraphic</code>
     */
    public ImageGraphic(Image img)
    {
        setImage(img);
    }

    /**
     * <code>URL</code> constructor. The given <code>URL</code> is used to load
     * the {@link java.awt.Image} to be associated with this
     * <code>ImageGraphic</code>.
     * 
     * <p>
     * 
     * Implicitly does the equivalent to:
     * 
     * <pre>
     * this.setInitString(&quot;url &quot; + url);
     * </pre>
     * 
     * @param url
     *            the location of the <code>Image</code> to be associated with
     *            this <code>ImageGraphic</code>.
     */
    public ImageGraphic(URL url)
    {
        this(Toolkit.getDefaultToolkit().getImage(url));
        setInitString("url " + url);
    }

    /**
     * Filename constructor. The given <code>filename</code> is used to load the
     * {@link java.awt.Image} to be associated with this
     * <code>ImageGraphic</code>.
     * 
     * <p>
     * 
     * Implicitly does the equivalent to:
     * 
     * <pre>
     * this.setInitString(&quot;file &quot; + filename);
     * </pre>
     * 
     * @param filename
     *            the location of the <code>Image</code> to be associated with
     *            this <code>ImageGraphic</code>.
     */
    public ImageGraphic(String filename)
    {
        this(Toolkit.getDefaultToolkit().getImage(filename));
        setInitString("file " + filename);
    }

    /**
     * Resource constructor. The given class-relative resource is used to load
     * the {@link java.awt.Image} to be associated with this
     * <code>ImageGraphic</code>.
     * 
     * <p>
     * 
     * Implicitly does the equivalent to:
     * 
     * <pre>
     * this.setInitString(&quot;rez &quot; + resource);
     * </pre>
     * 
     * @param base
     *            the class to which the referenced resource is relative
     * @param resource
     *            the location of the <code>Image</code> to be associated with
     *            this <code>ImageGraphic</code>, relative to the given
     *            <code>Class</code>
     */
    public ImageGraphic(Class base, String resource)
    {
        this(base.getResource(resource));
        setInitString("rez " + resource);
    }

    /**
     * Locator constructor. The given {@link org.cablelabs.gear.data.Locator} is
     * used to load the {@link java.awt.Image} to be associated with this
     * <code>ImageGraphic</code>.
     * 
     * @param locator
     *            the <code>Locator</code> specifying the location of the
     *            <code>Image</code> to be associated with this
     *            <code>ImageGraphic</code>
     */
    public ImageGraphic(Locator locator)
    {
        this(locator.getLocation());

        if (locator instanceof FileLocator)
            setInitString("file " + ((FileLocator) locator).getPath());
        else if (locator instanceof URLLocator)
            setInitString("url " + locator.getLocation());
        else if (locator instanceof ResourceLocator) setInitString("rez " + ((ResourceLocator) locator).getPath());
    }

    // Description copied from GraphicData
    public Dimension getSize()
    {
        return new Dimension(w, h);
    }

    // Description copied from GraphicData
    public int getWidth()
    {
        return w;
    }

    // Description copied from GraphicData
    public int getHeight()
    {
        return h;
    }

    // Description copied from GraphicData
    public void draw(Graphics g, int x, int y, ImageObserver io)
    {
        g.drawImage(image, x, y, io);
    }

    // Description copied from GraphicData
    public void draw(Graphics g, int x, int y, int width, int height, ImageObserver io)
    {
        g.drawImage(image, x, y, width, height, io);
    }

    /**
     * Sets the <code>java.awt.Image</code> currently associated with this
     * <code>ImageGraphic</code>. This is the image that is drawn by the
     * {@link #draw(Graphics,int,int,ImageObserver)} and
     * {@link #draw(Graphics,int,int,int,int,ImageObserver)} methods.
     * <p>
     * This call blocks until the given image is fully loaded.
     * 
     * @param img
     *            the image to associate with this <code>ImageGraphic</code>
     * @see #getImage()
     */
    public void setImage(Image img)
    {
        image = img;
        load(img);
    }

    /**
     * Returns the <code>java.awt.Image</code> currently associated with this
     * <code>ImageGraphic</code>. This is the image that is drawn by the
     * {@link #draw(Graphics,int,int,ImageObserver)} and
     * {@link #draw(Graphics,int,int,int,int,ImageObserver)} methods.
     * 
     * @return the image associated with this <code>ImageGraphic</code>
     * @see #setImage(Image)
     */
    public Image getImage()
    {
        return image;
    }

    /**
     * Block until the image is loaded.
     * 
     * @param image
     *            the image to wait for
     */
    private void load(Image image)
    {
        if (image == null)
        {
            w = -1;
            h = -1;
        }
        else
        {
            synchronized (mt)
            {
                mt.addImage(image, 0);

                try
                {
                    mt.waitForID(0);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                if (mt.isErrorID(0))
                {
                    System.err.println("Error loading image");
                }

                mt.removeImage(image, 0);
            }

            w = image.getWidth(null);
            h = image.getHeight(null);
        }
    }

    /**
     * Sets the <i>init</i> string for this object. This describes the manner in
     * which this object should be recreated and is purely informational. See
     * {@link #getInitString()} for the format of this string.
     * 
     * <p>
     * 
     * This string is automatically set by the constructor and need not be set
     * in general.
     * 
     * @param string
     *            the descriptive initialization string for this object
     * @see #getInitString()
     */
    void setInitString(String string)
    {
        initString = string;
    }

    /**
     * Gets the <i>init</i> string for this object. This describes the manner in
     * which this object should be recreated and is purely informational.
     * <p>
     * 
     * <table border>
     * <tr>
     * <th>Type</th>
     * <th>Format</th>
     * <th>Example</th>
     * </tr>
     * <tr>
     * <td>URL</td>
     * <td> <code>"url <i>url</i></code></td>
     * <td> <code>"url http://someplace.com/image1.gif"</code></td>
     * </tr>
     * <tr>
     * <td>File</td>
     * <td> <code>"file <i>filename</i>"</code></td>
     * <td> <code>"file c:/images/image1.gif"</code></td>
     * </tr>
     * <tr>
     * <td>Resource</td>
     * <td> <code>"rez <i>resourcename</i>"</code></td>
     * <td> <code>"rez /demo/images/image1.gif"</code></td>
     * </tr>
     * <tr>
     * <td>N/A</td>
     * <td> <code>null</code></td>
     * <td> <code>null</code></td>
     * </tr>
     * </table>
     * 
     * @return the descriptive initialization string for this object;
     *         <code>null</code> if no initialization string applies
     */
    String getInitString()
    {
        return initString;
    }

    /**
     * Overrides <code>Object.toString()</code> to add the initialization
     * string.
     * 
     * @return a <code>String</code> representation of this object
     */
    public String toString()
    {
        return super.toString() + " [" + getInitString() + "]";
    }

    /*  ************************ Attributes ************************ */

    /**
     * This image on which this GraphicData is based.
     */
    private Image image;

    /**
     * Cached width of image. Initialized to -1 in case referenced before an
     * image is assigned. Later filled in by {@link #load(Image)}.
     */
    private int w = -1;

    /**
     * Cached height of image. Initialized to -1 in case referenced before an
     * image is assigned. Later filled in by {@link #load(Image)}.
     */
    private int h = -1;

    /**
     * The descriptive initialization string for this ImageGraphic. This is
     * initialized by the various constructors.
     */
    private String initString;

    /**
     * The global MediaTracker used to track the loading of Images.
     */
    private final static MediaTracker mt = new MediaTracker(new java.awt.Component()
    {
    });
}
