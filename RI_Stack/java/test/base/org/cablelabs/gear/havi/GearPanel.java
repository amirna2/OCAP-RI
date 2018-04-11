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

package org.cablelabs.gear.havi;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.cablelabs.gear.data.GraphicData;

/**
 * <code>GearPanel</code> is a double-buffered container which can use an
 * {@link GraphicData} instance to paint its background and/or use the
 * background color to fill the entire container area.
 * 
 * <p>
 * 
 * Double buffering is disabled by default. Background fill is enabled by
 * default. Double buffering requires that background fill be enabled -- if
 * double buffering is enabled, then background fill is implicitly enabled.
 * 
 * @author Tom Henriksen
 * @version $Revision: 1.2 $, $Date: 2002/06/03 21:33:16 $
 */

public class GearPanel extends SnapContainer
{

    /**
     * Default constructor. This container will have double-buffering disabled
     * and background fill enabled.
     */
    public GearPanel()
    {
        this(null, false, true);
    }

    /**
     * Constructor which specifies the bounds of the container. This container
     * will have double-buffering disabled and background fill enabled.
     * 
     * @param x
     *            initial x-location of this <code>Container</code>
     * @param y
     *            initial y-location of this <code>Container</code>
     * @param width
     *            initial width of this <code>Container</code>
     * @param height
     *            initial height of this <code>Container</code>
     */
    public GearPanel(int x, int y, int width, int height)
    {
        this(x, y, width, height, null, false, true);
    }

    /**
     * Simple constructor which takes parameters specifying whether or not the
     * container should be double-buffered and whether or not the container
     * should be opaque.
     * 
     * <p>
     * 
     * Note that <code>dbEnabled==true</code> implicitly sets
     * <code>bgFilled</code> to <code>true</code>.
     * 
     * @param graphic
     *            if not <code>null</code> then this container will render the
     *            graphic dependant on the resize flag.
     * @param dbEnabled
     *            if <code>true</code> then this container should be
     *            double-buffered (which will implicitly set
     *            <code>bgFilled</code> to <code>true</code>)
     * @param bgFilled
     *            if <code>true</code> then this container has its background
     *            filled with the background color; if <code>dbEnabled</code> is
     *            <code>true</code> then this is implicitly <code>true</code>
     */
    public GearPanel(GraphicData graphic, boolean dbEnabled, boolean bgFilled)
    {
        super();
        iniz(graphic, dbEnabled, bgFilled);
    }

    /**
     * Constructor which specifies the bounds of the container and takes
     * parameters specifying whether or not the container should be
     * double-buffered and whether or not the container should fill the
     * background and whether a graphic should be be rendered.
     * 
     * <p>
     * 
     * Note that <code>dbEnabled==true</code> implicitly sets
     * <code>bgFilled</code> to <code>true</code>.
     * 
     * @param x
     *            initial x-location of this <code>Container</code>
     * @param y
     *            initial y-location of this <code>Container</code>
     * @param width
     *            initial width of this <code>Container</code>
     * @param height
     *            initial height of this <code>Container</code>
     * @param graphic
     *            if not <code>null</code> then this container will render the
     *            graphic dependant on the resize flag.
     * @param dbEnabled
     *            if <code>true</code> then this container should be
     *            double-buffered (which will implicitly set
     *            <code>bgFilled</code> to <code>true</code>)
     * @param bgFilled
     *            if <code>true</code> then this container has its background
     *            filled with the background color; if <code>dbEnabled</code> is
     *            <code>true</code> then this is implicitly <code>true</code>
     */
    public GearPanel(int x, int y, int width, int height, GraphicData graphic, boolean dbEnabled, boolean bgFilled)
    {
        super(x, y, width, height);
        iniz(graphic, dbEnabled, bgFilled);
    }

    /**
     * Initializations used by all constructors.
     */
    private void iniz(GraphicData graphic, boolean dbEnabled, boolean bgFilled)
    {
        setLayout(new FlowLayout());
        addComponentListener(new ResizeTracker());
        setBackgroundFill(bgFilled);
        setDoubleBuffered(dbEnabled); // dbEnabled overrides bgFilled
        setResizeMode(RESIZE_NONE);
        setBackgroundGraphic(graphic);
    }

    /**
     * Returns <code>true</code> if this component is set to fill the background
     * with an opaque color. Otherwise <code>false</code> is returned.
     * 
     * @return <code>true</code> if background fill is enabled with an opaque
     *         background color; <code>false</code> otherwise.
     */
    public boolean isOpaque()
    {
        java.awt.Color c;
        return isBackgroundFill() && (c = getBackground()) != null && (c.getRGB() >>> 24) == 255;

    }

    /**
     * Overrides <code>paint()</code> to paint this component and all
     * sub-components using an offscreen buffer, if {@link #isDoubleBuffered()}.
     * 
     * Prior to painting sub-components, the background color is painted,
     * effectively erasing any previously drawn graphics. This is necessary
     * because <code>update()</code> no longer clears the screen.
     * 
     * <p>
     * 
     * If !{@link #isDoubleBuffered()} then <code>super.paint</code> is used.
     * 
     * @param g
     *            the current graphics context
     * @see #isDoubleBuffered()
     */
    public void paint(Graphics g)
    {
        Dimension size = getSize();

        // Make sure we have a size
        if (size.width > 0 && size.height > 0)
        {

            if (!dbEnabled)
            {
                fillBackground(g, size);
                super.paint(g);
            }
            else
            {
                Image img;
                Graphics offscreen = null;

                try
                {
                    if ((img = getOffscreenImage()) == null || (offscreen = img.getGraphics()) == null)
                    {
                        // Forget about double-buffering... can't do it.
                        fillBackground(g, size);
                        super.paint(g);
                    }
                    else
                    {
                        // Paint double-buffered
                        fillBackground(offscreen, size);
                        super.paint(offscreen);
                        g.drawImage(img, 0, 0, null);
                    }
                }
                finally
                {
                    if (offscreen != null) offscreen.dispose();
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if this component is painted to an offscreen
     * image ("buffer") that's copied to the screen later. Otherwise
     * <code>false</code> is returned.
     * 
     * @return <code>true</code> if double buffering is currently enabled;
     *         <code>false</code> otherwise.
     * @see #setDoubleBuffered(boolean)
     */
    public boolean isDoubleBuffered()
    {
        return dbEnabled;
    }

    /**
     * Used to enable/disable double buffering for this container. When double
     * buffering is enabled, then this component will utilize double buffering
     * techniques to render itself.
     * 
     * <p>
     * 
     * Note that enabling double buffering will implicitly enable background
     * fill.
     * 
     * @param dbEnabled
     *            if <code>true</code> then double buffering should be enabled;
     *            if <code>false</code> then double buffering should be disabled
     * @see #isDoubleBuffered()
     * @see #setBackgroundFill(boolean)
     */
    public void setDoubleBuffered(boolean dbEnabled)
    {
        this.dbEnabled = dbEnabled;
        if (dbEnabled) bgFilled = true;
    }

    /**
     * Returns whether this component completely fills its area with the
     * background color.
     * 
     * @return <code>background fill flag</code>
     * @see #setBackgroundFill(boolean)
     */
    public boolean isBackgroundFill()
    {
        return bgFilled;
    }

    /**
     * Sets the background fill property of this container. When the background
     * fill is set to <code>true</code>, then the background color is always
     * painted over the entire component bounds. When <code>false</code>, then
     * the background is left alone or cleared (depending upon the
     * double-buffering option).
     * 
     * <p>
     * 
     * Note that setting background fill to <code>false</code> will implicitly
     * disable double buffering.
     * 
     * @param bgFilled
     * @see #isBackgroundFill()
     * @see #isDoubleBuffered()
     */
    public void setBackgroundFill(boolean bgFilled)
    {
        this.bgFilled = bgFilled;
        if (!bgFilled) dbEnabled = false;
    }

    /**
     * Returns the current offscreen <code>Image</code> used for
     * double-buffering.
     * 
     * @return the current offscreen <code>Image</code> used for
     *         double-buffering.
     * @see #isDoubleBuffered()
     */
    private Image getOffscreenImage()
    {
        if (offscreenImage == null)
        {
            Dimension size = getSize();
            offscreenImage = createImage(size.width, size.height);
        }
        return offscreenImage;
    }

    /**
     * Sets the <code>GraphicData</code> instance to use in drawing the
     * background. The background graphic will be rendered according to the
     * prevalent {@link #getResizeMode resize mode}.
     * 
     * @param graphic
     *            The <code>GraphicData</code> instance to use in drawing the
     *            background.
     */
    public void setBackgroundGraphic(GraphicData graphic)
    {
        this.graphic = graphic;
    }

    /**
     * Retrieves the <code>GraphicData</code> instance used by this
     * <code>GraphicPanel</code>.
     * 
     * @return the <code>GraphicData</code> instance used by this
     *         <code>GraphicPanel</code>.
     */
    public GraphicData getBackgroundGraphic()
    {
        return graphic;
    }

    /**
     * Sets the current image resize mode for this container. This governs how
     * an image is scaled within the container.
     * 
     * @param mod
     *            the new image resizing mode
     * @see #getResizeMode()
     * @see #RESIZE_NONE
     * @see #RESIZE_ARBITRARY
     * @see #RESIZE_PRESERVE_ASPECT
     * @see #RESIZE_WALLPAPER
     */
    public void setResizeMode(int mode)
    {
        switch (mode)
        {
            case RESIZE_NONE:
            case RESIZE_ARBITRARY:
            case RESIZE_PRESERVE_ASPECT:
            case RESIZE_WALLPAPER:
                resizeMode = mode;
                break;
            default:
                throw new IllegalArgumentException("Invalid resize mode " + mode);
        }
    }

    /**
     * Returns the current image resize mode for this container.
     * 
     * @return the current image resize mode for this container
     * @see #setResizeMode(int)
     * @see #RESIZE_NONE
     * @see #RESIZE_ARBITRARY
     * @see #RESIZE_PRESERVE_ASPECT
     * @see #RESIZE_WALLPAPER
     */
    public int getResizeMode()
    {
        return resizeMode;
    }

    /**
     * Performs drawing of the container background and/or background graphic.
     * 
     * @param g
     *            the current Graphics context (may or may not be offscreen)
     * @param bounds
     *            the internal bounds of this container
     * 
     * @see #paint(Graphics)
     * @see #isBackgroundFill()
     */
    private void fillBackground(Graphics g, Dimension size)
    {
        // Check to see if the background should be filled
        // in with the background color.
        if (isBackgroundFill())
        {
            g.setColor(getBackground());
            g.fillRect(0, 0, size.width, size.height);
        }

        // Check to see if there is a graphic which should
        // be rendered.
        if (graphic != null)
        {

            // Render it centered
            switch (getResizeMode())
            {
                case RESIZE_NONE:
                    graphic.draw(g, (size.width - graphic.getWidth()) / 2, (size.height - graphic.getHeight()) / 2,
                            this);
                    break;
                case RESIZE_PRESERVE_ASPECT:
                    // Save current size..
                    Dimension scaled;
                    scaled = scaleBounds(graphic.getWidth(), graphic.getHeight(), size);
                    // figure centered point
                    graphic.draw(g, (size.width - scaled.width) / 2, (size.height - scaled.height) / 2, scaled.width,
                            scaled.height, this);
                    break;
                case RESIZE_ARBITRARY:
                    graphic.draw(g, 0, 0, size.width, size.height, this);
                    break;
                case RESIZE_WALLPAPER:
                    paintWallpaper(g, size);
                    break;
            }
        }
    }

    /**
     * Calculates the scaling bounds of an <i>item</i> of the given dimensions (
     * <code>w</code> and <code>h</code>) within the given
     * <code>Dimension</code>, assuming that the width-to-height ratio should be
     * preserved.
     * 
     * @param w
     *            the width of the object to scale
     * @param h
     *            the height of the object to scale
     * @param d
     *            the size of the space within which an object of the given w/h
     *            should be scaled
     */
    private Dimension scaleBounds(int w, int h, Dimension d)
    {
        int width = d.width;
        int height = d.height;

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
     * Tile the current graphic within the given bounds of the given
     * <code>Graphics</code> context.
     */
    private void paintWallpaper(Graphics g, Dimension size)
    {
        int w = graphic.getWidth();
        int h = graphic.getHeight();

        // Local copies
        int x = 0;
        int y = 0;
        int width = size.width;
        int height = size.height;

        Graphics g2 = g.create(x, y, width, height);
        try
        {
            // Tile the image
            for (int r = 0; r < width; r += w)
                for (int c = 0; c < height; c += h)
                    graphic.draw(g2, r, c, this);
        }
        finally
        {
            // Clean up
            g2.dispose();
        }
    }

    /**
     * The current offscreen image used for double-buffering.
     */
    private Image offscreenImage;

    /**
     * Specifies whether this container is double-buffered or not.
     */
    private boolean dbEnabled;

    /**
     * Specifies whether this container is fully opaque (i.e., paints the
     * background color) or not.
     */
    private boolean bgFilled;

    /**
     * Resize mode which specifies that the background graphic should not be
     * scaled. The graphic will be centered within the bounds of the container.
     * 
     * @see #setResizeMode(int)
     */
    public static final int RESIZE_NONE = 0;

    /**
     * Resize mode which specifies that the background graphic should scaled
     * such that the original width-to-height ratio is preserved. The graphic
     * will be centered within the bounds of the container.
     * 
     * @see #setResizeMode(int)
     */
    public static final int RESIZE_PRESERVE_ASPECT = 1;

    /**
     * Resize mode which specifies that the background graphic should scaled
     * arbitrarily (i.e., to fill the entire container bounds). The graphic will
     * be centered within the bounds of the container.
     * 
     * @see #setResizeMode(int)
     */
    public static final int RESIZE_ARBITRARY = 2;

    /**
     * Resize mode which specifies that the background graphic should not be
     * scaled, but be repeated to fill the entire container bounds. The graphic
     * will be drawn initially in the upper-left hand corner and repeated in
     * both the horizontal and vertical directions (i.e., it won't be centered).
     * 
     * @see #setResizeMode(int)
     */
    public static final int RESIZE_WALLPAPER = 10;

    /**
     * The current resize (scaling) mode.
     */
    private int resizeMode;

    /**
     * The current graphic object (i.e., the background graphic).
     */
    private GraphicData graphic;

    /**
     * ComponentAdapter used to allocate a new offscreen image if necessary.
     */
    private class ResizeTracker extends ComponentAdapter
    {
        /**
         * Called when this <code>DBContainer</code> is resized. If the
         * <code>DBContainer</code> is made larger, then a new offscreen
         * <code>Image</code> is allocated.
         */
        public void componentResized(ComponentEvent e)
        {
            Dimension size = getSize();

            // If component is larger than offscreen image... resize!
            if (offscreenImage != null
                    && (size.width > offscreenImage.getWidth(null) || size.height > offscreenImage.getHeight(null)))
            {
                offscreenImage.flush();
                offscreenImage = null;
            }
        }
    }
}
