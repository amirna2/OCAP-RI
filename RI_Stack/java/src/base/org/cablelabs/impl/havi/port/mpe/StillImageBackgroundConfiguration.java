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

package org.cablelabs.impl.havi.port.mpe;

import org.cablelabs.impl.havi.AwtBackgroundImage;
import org.cablelabs.impl.havi.ExtendedStillImageBackgroundConfiguration;
import org.cablelabs.impl.havi.HaviToolkit;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;

import org.havi.ui.HBackgroundConfigTemplate;
import org.havi.ui.HBackgroundDevice;
import org.havi.ui.HBackgroundImage;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HStillImageBackgroundConfiguration;

/**
 * Implementation of {@link HStillImageBackgroundConfiguration} for the MPE port
 * intended to run on an OCAP implementation.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 * @version $Revision: 1.14 $ $Date: 2002/06/03 21:31:03 $
 */
public class StillImageBackgroundConfiguration extends HStillImageBackgroundConfiguration implements
        ExtendedStillImageBackgroundConfiguration
{
    /** The base configuration for this background */
    private BackgroundConfiguration baseConfiguration = null;

    /** The current AWT image */
    private Image awtImage = null;

    /** Center the image on the background */
    private static final int CENTERED = 1;

    /** Scale the image to the size of the background */
    private static final int SCALED = 2;

    /** Tile the image over the entire background */
    private static final int TILED = 3;

    /** The drawing method */
    private static int drawMethod;

    /** The top-left corner of the area to be drawn by the background image */
    private Point drawLocation;

    /** The size of the area to be drawn by the background image */
    private Dimension drawSize;

    /**
     * Construct a configuration for the specified device. This is a package
     * private construction since it should only used by the platform specific
     * code.
     * 
     * @param device
     *            the device to which this configuration belongs
     * @param d
     *            the dimensions of this configuration
     */
    StillImageBackgroundConfiguration(HBackgroundDevice device, Dimension d)
    {
        // Initlialize the base configuration
        baseConfiguration = new BackgroundConfiguration(device, d);

        // Add the still image preferences to the template
        baseConfiguration.stillImage = true;
    }

    // Definition copied from superclass
    public boolean getFlickerFilter()
    {
        return baseConfiguration.getFlickerFilter();
    }

    // Definition copied from superclass
    public boolean getInterlaced()
    {
        return baseConfiguration.getInterlaced();
    }

    // Definition copied from superclass
    public Dimension getPixelAspectRatio()
    {
        return baseConfiguration.getPixelAspectRatio();
    }

    // Definition copied from superclass
    public Dimension getPixelResolution()
    {
        return baseConfiguration.getPixelResolution();
    }

    // Definition copied from superclass
    public HScreenRectangle getScreenArea()
    {
        return baseConfiguration.getScreenArea();
    }

    // Definition copied from superclass
    public HBackgroundDevice getDevice()
    {
        return baseConfiguration.getDevice();
    }

    // Definition copied from superclass
    public HBackgroundConfigTemplate getConfigTemplate()
    {
        return baseConfiguration.getConfigTemplate();
    }

    // Definition copied from superclass
    public Color getColor()
    {
        return baseConfiguration.getColor();
    }

    // Definition copied from superclass
    public void setColor(Color color) throws HPermissionDeniedException, HConfigurationException
    {
        baseConfiguration.setColor(color);
    }

    // Definition copied from superclass
    public boolean getChangeableSingleColor()
    {
        return baseConfiguration.getChangeableSingleColor();
    }

    // Definition copied from superclass
    public boolean getStillImage()
    {
        return baseConfiguration.getStillImage();
    }

    // Definition copied from superclass
    public void displayImage(HBackgroundImage image) throws java.io.IOException, HPermissionDeniedException,
            HConfigurationException
    {
        // Set the draw location and size to the full background
        drawLocation = new Point(0, 0);
        drawSize = getPixelResolution();

        // Display the image
        display(image);
    }

    // Definition copied from superclass
    public void displayImage(HBackgroundImage image, HScreenRectangle r) throws java.io.IOException,
            HPermissionDeniedException, HConfigurationException
    {
        // Get the area of the screen covered by this configuration
        HScreenRectangle cr = this.getScreenArea();

        // Get the resolution of this configuration
        Dimension cd = getPixelResolution();

        // Compute the screen resolution in pixels of the same size as this
        // configuration.
        Dimension sd = new Dimension((int) (cd.width / cr.width), (int) (cd.height / cr.height));

        // Compute the area (in pixels of this configuration) covered by the
        // specified screen rectangle.
        drawLocation = new Point((int) ((r.x - cr.x) * sd.width), (int) ((r.y - cr.y) * sd.height));
        drawSize = new Dimension((int) (r.width * sd.width), (int) (r.height * sd.height));

        // Display the image
        display(image);
    }

    /**
     * Display the image
     */
    private void display(HBackgroundImage image)
    {
        // Get and save the actual AWT image.
        awtImage = AwtBackgroundImage.getImpl(image).getAwtImage();

        // Make sure the image is loaded before we continue.
        image.load(null);
        try
        {
            MediaTracker tracker = new MediaTracker(new Component()
            {
            });
            tracker.addImage(awtImage, 1);
            tracker.waitForID(1);
            if (tracker.isErrorID(1)) throw new RuntimeException("Cannot load background image");
        }
        catch (Exception e)
        {
            throw new RuntimeException("Cannot load background image");
        }

        // Force a refresh
        ((BackgroundDevice) getDevice()).refresh(this);
    }

    /**
     * Refresh (paint) the background component. This method is called whenever
     * the component that represents the device needs to be repainted.
     * 
     * @param g
     *            the graphics to use for drawing
     */
    void refresh(Graphics g)
    {
        // Just return if the image has not been set
        if ((drawLocation == null) || (drawSize == null)) return;

        // Paint the background color
        baseConfiguration.refresh(g);

        // Clip to the area to be drawn
        Graphics g2 = g.create(drawLocation.x, drawLocation.y, drawSize.width, drawSize.height);
        try
        {
            // Paint the background image
            int imageWidth = awtImage.getWidth(null);
            int imageHeight = awtImage.getHeight(null);
            if (drawMethod == CENTERED)
            {
                // Center the image
                g.drawImage(awtImage, ((drawSize.width - imageWidth) / 2), ((drawSize.height - imageHeight) / 2), null);
            }
            else if (drawMethod == SCALED)
            {
                // Scale the image to cover the entire background
                g.drawImage(awtImage, 0, 0, drawSize.width, drawSize.height, null);
            }
            else if (drawMethod == TILED)
            {
                // Tile the image to cover the entire background
                int x = 0;
                int y = 0;
                while (y < drawSize.height)
                {
                    while (x < drawSize.width)
                    {
                        g.drawImage(awtImage, x, y, null);
                        x += imageWidth;
                    }
                    y += imageHeight;
                    x = 0;
                }
            }
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Static block
     */
    static
    {
        try
        {
            // Lookup the drawing method for background images
            HaviToolkit tk = HaviToolkit.getToolkit();
            String name = tk.getProperty(Property.BG_DRAWMETHOD);
            if (name.equals("CENTERED"))
                drawMethod = CENTERED;
            else if (name.equals("SCALED"))
                drawMethod = SCALED;
            else if (name.equals("TILED")) drawMethod = TILED;
        }
        catch (Exception e)
        {
            // The default drawing method is CENTERED
            drawMethod = CENTERED;
        }
    }
}
