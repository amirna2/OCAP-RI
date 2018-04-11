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

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui;

import java.awt.Point;
import java.awt.Dimension;
import org.cablelabs.impl.havi.ExtendedScreenDevice;
import org.cablelabs.impl.manager.HostManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.MPEEnv;

/**
 * The {@link org.havi.ui.HScreenConfiguration HScreenConfiguration} class
 * describes the characteristics (settings) of an
 * {@link org.havi.ui.HScreenDevice HScreenDevice}. There can be many
 * {@link org.havi.ui.HScreenConfiguration HScreenConfiguration} objects
 * associated with a single {@link org.havi.ui.HScreenDevice HScreenDevice}.
 * 
 * @see org.havi.ui.HScreenDevice
 * @author Alex Resh
 * @author Todd Earles
 * @version 1.1
 */

public abstract class HScreenConfiguration extends Object
{
    /**
     * Used if DSExt is being used
     */
    protected static int ZOOM_PREFERENCE = -1;

    /**
     * package scope constructor to stop javadoc generating one
     */
    HScreenConfiguration()
    {
        boolean dsExtUsed = (MPEEnv.getEnv("ocap.api.option.ds") != null);
        if (dsExtUsed && ZOOM_PREFERENCE == -1)
        {
            HostManager hm = (HostManager) ManagerManager.getInstance(HostManager.class);
            ZOOM_PREFERENCE = hm.getZoomModePreference();
        }
    }

    /**
     * Convert a pixel position from one coordinate system to another without
     * including any rounding errors from passing through normalized
     * coordinates. This returns null if this transformation isn't possible for
     * various reasons. These reasons are:
     * <ul>
     * <li>at least one of the two {@link HScreenConfiguration
     * HScreenConfigurations} isn't pixel based or doesn't yet have a fixed
     * location on the HScreen.
     * <li>a non-linear transformation is in use between the two.
     * <li>the information needed to calculate this isn't available.
     * <li>the transformation is changing with time (e.g. due to pan & scan).
     * </ul>
     * <p>
     * The source position is interpreted in the coordinate system of the
     * HScreenConfiguration object on which this method is called.
     * 
     * @param destination
     *            the destination {@link HScreenConfiguration
     *            HScreenConfiguration}.
     * @param source
     *            the pixel position in this {@link HScreenConfiguration
     *            HScreenConfiguration}.
     * @return the position of the specified pixel position measured in the
     *         destination coordinate system, or null if this isn't possible.
     */
    public Point convertTo(HScreenConfiguration destination, Point source)
    {
        Point p = null;
        try
        {
            // Make sure destination configuration is not changing
            Dimension pr = destination.getPixelResolution();
            Dimension rez = getPixelResolution();
            // If at least one of configuration isn't pixel based
            if (pr != null && rez != null && !pr.equals(new Dimension(0, 0)) && !rez.equals(new Dimension(0, 0)))
            {
                HScreenRectangle sr = destination.getScreenArea();

                // The x and y offset coordinates of the top, left corner of the
                // area are not constrained - they may be negative, or have
                // values greater than one - and hence, may denote an offset
                // location that is not "on-screen".

                // The width and height of the area should be positive
                // (including zero), but are otherwise unconstrained - and hence
                // may denote areas greater in size than the entire screen.

                // STEP 1: compute virtual resolution
                HScreenPoint vr = new HScreenPoint((float) (getPixelResolution().width) / getScreenArea().width,
                        (float) (getPixelResolution().height) / getScreenArea().height);

                // STEP 2: Convert given coordinate to an HScreenPoint
                HScreenPoint vp = new HScreenPoint((getScreenArea().x + source.x / vr.x), (getScreenArea().y + source.y
                        / vr.y));

                // STEP 3: repeat STEP1 for proposed configuration
                vr = new HScreenPoint((float) (pr.width) / sr.width, (float) (pr.height) / sr.height);

                // STEP 4: convert HScreenPoint back to pixel coordinates
                p = new Point((int) (Math.floor((vp.x - sr.x) * vr.x + 0.5)), (int) (Math.floor((vp.y - sr.y) * vr.y
                        + 0.5)));
            }
        }
        catch (NullPointerException e)
        {
            // Bad argument or uknown/not initialized variables prevent from
            // computing
            p = null;
        }
        catch (ArithmeticException e)
        {
            // New position cannot be computed because of divide by zero
            p = null;
        }
        return p;
    }

    /**
     * Return whether this configuration includes filtering to reduce interlace
     * flicker.
     * 
     * @return true if filtering is included, false otherwise.
     */
    public boolean getFlickerFilter()
    {
        // This method must be implemented in the port specific subclass.
        return false;
    }

    /**
     * Return whether this configuration is interlaced
     * 
     * @return true if this configuration is interlaced, false otherwise.
     */
    public boolean getInterlaced()
    {
        // This method must be implemented in the port specific subclass.
        return false;
    }

    /**
     * Return the pixel aspect ratio of this configuration. Some examples are
     * {16:15}, {64:45}, {1:1}.
     * 
     * @return the aspect ratio of the pixels in this configuration.
     */
    public Dimension getPixelAspectRatio()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Return the resolution of this configuration in pixels. The pixel
     * coordinate system used is that of the device concerned.
     * 
     * @return the resolution of this configuration in pixels.
     */
    public Dimension getPixelResolution()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Return the position and size of this configuration on the screen in
     * screen coordinates.
     * 
     * @return the area on the screen of this configuration in screen
     *         coordinates.
     */
    public HScreenRectangle getScreenArea()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Returns the offset between the origin of the pixel coordinate space of
     * the specified {@link org.havi.ui.HScreenConfiguration
     * HScreenConfiguration}, and the origin of the current pixel coordinate
     * space of this {@link org.havi.ui.HScreenConfiguration
     * HScreenConfiguration}. The offset is returned in the pixel coordinate
     * space of this {@link org.havi.ui.HScreenConfiguration
     * HScreenConfiguration}.
     * 
     * @param hsc
     *            the {@link org.havi.ui.HScreenConfiguration
     *            HScreenConfiguration} to which the offset between pixel
     *            origins should be recovered.
     * @return the offset between the pixel coordinate space of the specified
     *         {@link org.havi.ui.HScreenConfiguration HScreenConfiguration} and
     *         the current pixel coordinate space of this
     *         {@link org.havi.ui.HScreenConfiguration HScreenConfiguration}. A
     *         null object will be returned if there is insufficient information
     *         to recover the pixel offset.
     */
    public java.awt.Dimension getOffset(HScreenConfiguration hsc)
    {
        Point p = hsc.convertTo(this, new Point(0, 0));
        return (p == null) ? null : (new Dimension(p.x, p.y));
    }

    /**
     * Return the screen device associated with this screen configuration
     */
    ExtendedScreenDevice getScreenDevice()
    {
        // Handle graphics configurations
        if (this instanceof HGraphicsConfiguration)
            return (ExtendedScreenDevice) (((HGraphicsConfiguration) this).getDevice());

        // Handle video configurations
        if (this instanceof HVideoConfiguration)
            return (ExtendedScreenDevice) (((HVideoConfiguration) this).getDevice());

        // Handle background configurations
        if (this instanceof HBackgroundConfiguration)
            return (ExtendedScreenDevice) (((HBackgroundConfiguration) this).getDevice());

        // Throw a run-time exception
        throw new RuntimeException("Unknown screen configuration class type");
    }
}
