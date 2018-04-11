/*
 * Copyright  1990-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions.
 *
 */

package java.awt;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

/**
 * Abstract base class for MPE <code>GraphicsConfiguration</code>
 * implementations.
 *
 * @author Nicholas Allen (PBP RI)
 * @author Aaron Kamienski (MPE)
 * @version 1.11, 05/03/02
 *
 * Note: This is a renamed (and possibly modified) version of a phoneme file (MWGraphicsConfiguration.java)
 */
abstract class MPEGraphicsConfiguration extends GraphicsConfiguration
{

    MPEGraphicsConfiguration(MPEGraphicsDevice dev, int handle)
    {
        device = dev;
        config = handle;
        width = getScreenWidth(config);
        height = getScreenWidth(config);
    }

    public GraphicsDevice getDevice()
    {
        return device;
    }

    public Rectangle getBounds()
    {
        return new Rectangle(width, height);
    }

    public BufferedImage createCompatibleImage(int width, int height)
    {
        if (width <= 0 || height <= 0) return null;

        return createBufferedImageObject(new MPEOffscreenImage(null, width, height, this));
    }

    // TODO: I'm not sure this should be here.
    // I.e., not sure that a config should magically modify itself...
    void update(int handle) /* Update to current config */
    {
        config = handle; /* update the handle for this config */
        width = getScreenWidth(config); /* Update dimensions from this handle */
        height = getScreenHeight(config);
    }

    int getCompatibleImageType()
    {
        return imageType;
    }

    int createCompatibleImageSurface(int width, int height, java.awt.Color color)
    {
        return createCompatibleImageType(width, height, imageType, color.getRGB());
    }

    protected native int createCompatibleImageType(int width, int height, int imageType, int rgb);

    protected native BufferedImage createBufferedImageObject(MPEImage image);

    protected native int getScreenWidth(int config);

    protected native int getScreenHeight(int config);

    private MPEGraphicsDevice device; /* Device object */

    protected int config; /* Default config for the device */

    private int width; /* Width of screen */

    private int height; /* Height of screen */

    protected int imageType;
}
