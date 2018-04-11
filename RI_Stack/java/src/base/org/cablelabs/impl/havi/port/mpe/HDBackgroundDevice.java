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

import org.davic.resources.ResourceClient;
import org.havi.ui.*;

import java.awt.Dimension;
import java.awt.Rectangle;
import org.cablelabs.impl.havi.ExtendedScreen;
import org.cablelabs.impl.havi.ExtendedBackgroundDevice;
import org.cablelabs.impl.havi.MpegBackgroundImage;
import org.cablelabs.impl.havi.ReservationAction;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.util.NativeHandle;
import org.cablelabs.impl.manager.CallerContext;

import org.apache.log4j.Logger;


/**
 * Implementation of {@link HBackgroundDevice} for the MPE port intended to run
 * on an OCAP implementation.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 */
public class HDBackgroundDevice extends HBackgroundDevice implements ExtendedBackgroundDevice, HDScreenDevice
{
    private static final Logger log = Logger.getLogger(HBackgroundDevice.class.getName());

    /**
     * The native device handle.
     */
    private int nDevice;

    /**
     * Reference to the containing screen.
     */
    private HDScreen screen;

    /**
     * Configurations.
     */
    private HBackgroundConfiguration[] configurations = null;

    /**
     * Current configuration.
     */
    private HBackgroundConfiguration currentConfiguration = null;

    /**
     * Current configuration.
     */
    private HBackgroundConfiguration defaultConfiguration = null;

    /**
     * Constructs a background device based upon the given native device handle.
     * 
     * @param nDevice
     *            the native device handle
     */
    HDBackgroundDevice(ExtendedScreen screen, int nDevice)
    {
        this.screen = (HDScreen) screen;
        this.nDevice = nDevice;

        // make native calls, create configurations
        initConfigurations();
    }

    /**
     * Initializes the configurations associated with this device.
     */
    private void initConfigurations()
    {
        // Get the current/default configuration
        int curr = HDScreen.nGetDeviceConfig(nDevice);

        // Initialize the configurations
        int config[] = HDScreen.nGetDeviceConfigs(nDevice);
        configurations = new HBackgroundConfiguration[config.length];
        for (int i = 0; i < configurations.length; ++i)
        {
            configurations[i] = HDBackgroundConfiguration.newInstance(this, config[i]);
            if (config[i] == curr)
            {
                // Save current/default configuration
                defaultConfiguration = currentConfiguration = configurations[i];
            }
        }

        // Shouldn't occur... but could
        if (defaultConfiguration == null || currentConfiguration == null)
        {
            throw new NullPointerException();
        }
    }

    // Definition copied from NativeHandle
    public int getHandle()
    {
        return nDevice;
    }

    // Definition copied from superclass
    public boolean tempReserveDevice() throws HPermissionDeniedException
    {
        return super.tempReserveDevice();
    }

    // Definition copied from superclass
    public void withReservation(ReservationAction action) throws HPermissionDeniedException, HConfigurationException
    {
        super.withReservation(action);
    }

    // Definition copied from superclass
    public int getMatchStrength(HScreenConfiguration hsc, HScreenConfigTemplate hsct)
    {
        return super.getMatchStrength(hsc, hsct);
    }

    // Definition copied from superclass
    public String getIDstring()
    {
        String idString = HDScreen.nGetDeviceIdString(nDevice);
        if (idString == null) idString = "BackgroundDevice" + nDevice;
        return idString;
    }

    // Definition copied from superclass
    public Dimension getScreenAspectRatio()
    {
        Dimension d = new Dimension();
        return HDScreen.nGetDeviceScreenAspectRatio(nDevice, d);
    }

    // Definition copied from superclass
    public ExtendedScreen getScreen()
    {
        return screen;
    }

    // Definition copied from superclass
    public HBackgroundConfiguration[] getConfigurations()
    {
        HBackgroundConfiguration[] copy = new HBackgroundConfiguration[configurations.length];
        System.arraycopy(configurations, 0, copy, 0, configurations.length);
        return copy;
    }

    // Definition copied from superclass
    public HBackgroundConfiguration getDefaultConfiguration()
    {
        return defaultConfiguration;
    }

    // Definition copied from superclass
    public HBackgroundConfiguration getCurrentConfiguration()
    {
        synchronized (screen.lock)
        {
            return currentConfiguration;
        }
    }

    // Definition copied from HDScreenDevice
    public HScreenConfiguration getScreenConfig()
    {
        return getCurrentConfiguration();
    }

    // Definition copied from superclass
    public boolean setBackgroundConfiguration(final HBackgroundConfiguration hbc) throws SecurityException,
            HPermissionDeniedException, HConfigurationException
    {
        if (log.isDebugEnabled())
        {
            log.debug("setBackgroundConfiguration: hbc = " + hbc);
        }

        // Check if it's a valid configuration
        boolean valid = false;
        for (int i = 0; i < configurations.length; ++i)
            if (configurations[i] == hbc)
            {
                valid = true;
                break;
            }
        if (!valid) throw new HConfigurationException("Unsupported configuration");

        // Make sure the caller has reserved the device
        synchronized (screen.lock)
        {
            withReservation(new ReservationAction()
            {
                public void run() throws HPermissionDeniedException, HConfigurationException
                {
                    // If same, then we are done (permission check was enough)
                    if (hbc == currentConfiguration) return;

                    if (!HDScreen.nSetDeviceConfig(nDevice, ((NativeHandle) hbc).getHandle())) // !!!!
                                                                                               // Add
                                                                                               // helper
                                                                                               // to
                                                                                               // remove
                                                                                               // cast
                                                                                               // here!!!!
                    {
                        // No conflict, simply set this configuration
                        if (log.isDebugEnabled())
                        {
                            log.debug("setBackgroundConfiguration: calling changeConfiguration...");
                        }

                        changeConfiguration(hbc);
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("setBackgroundConfiguration: calling setWithCoherentConfigurations...");
                        }

                        // There is a conflict.
                        // "Implicitly" reserve the other devices,
                        // Set a coherent configuration.
                        screen.setWithCoherentConfigurations(hbc);
                    }
                }
            });
        } // synchronized(screen.lock)

        return true;
    }

    // Definition copied from HDScreenDevice
    public void changeConfiguration(HScreenConfiguration config)
    {
        currentConfiguration = (HBackgroundConfiguration) config;
        notifyScreenConfigListeners(config);
    }

    // Definition copied from superclass
    public boolean reserveDevice(ResourceUsageImpl usage, ResourceClient client, CallerContext context)
    {
        return super.reserveDevice(usage, client, context);
    }

    /**
     * Displays the given color, ensuring that the correct configuration is
     * given and the device is reserved.
     * 
     * <p>
     * Called by {@link HDBackgroundConfiguration#setColor}.
     * 
     * @param config
     *            the expected configuration
     * @param color
     *            the color to use
     * 
     * @exception HPermissionDeniedException
     *                if this {@link org.havi.ui.HBackgroundDevice
     *                HBackgroundDevice} does not have the right to control the
     *                background
     * @exception HConfigurationException
     *                if the color specified is illegal for this platform.
     * 
     * @see HDBackgroundConfiguration#setColor
     * @see HDScreen#nSetDeviceBGColor
     */
    void setColor(final HBackgroundConfiguration config, final java.awt.Color color) throws HPermissionDeniedException,
            HConfigurationException
    {
        withReservation(new ReservationAction()
        {
            public void run() throws HPermissionDeniedException, HConfigurationException
            {
                if (config != currentConfiguration)
                    throw new HPermissionDeniedException("Not the current configuration");

                if (color.getAlpha() != 0xFF)
                    throw new HConfigurationException("Illegal color (invalid alpha channel) " + color);

                // put color into RGBA format
                int colorInt = color.getRGB();
                colorInt = colorInt << 8;
                colorInt |= color.getAlpha();

                switch (HDScreen.nSetDeviceBGColor(nDevice, colorInt))
                {
                    case 0:
                        break;
                    case 1:
                        throw new HPermissionDeniedException("Device cannot modify color");
                    case 2:
                        throw new HConfigurationException("Illegal color " + color);
                }
            }
        });
    }

    /**
     * Retrieves the currently set background color for this device.
     * 
     * <p>
     * Called by {@link HDBackgroundConfiguration#getColor}.
     * 
     * @return the currently set Color (may be modified from setColor)
     * 
     * @see HDBackgroundConfiguration#getColor
     * @see HDScreen#nGetDeviceBGColor
     */
    java.awt.Color getColor()
    {
        return new java.awt.Color(HDScreen.nGetDeviceBGColor(nDevice), true);
    }

    /**
     * Displays the given background image, ensuring that the correct
     * configuration is given and the device is reserved.
     * 
     * <p>
     * Called by {@link HDStillImageBackgroundConfiguration#displayImage}.
     * 
     * @param config
     *            the expected configuration
     * @param mpeg
     *            the mpeg I-frame
     * @param r
     *            the display rectangle (or null)
     * 
     * @exception HPermissionDeniedException
     *                if the {@link org.havi.ui.HBackgroundDevice
     *                HBackgroundDevice} concerned is not reserved.
     * @exception HConfigurationException
     *                if the
     *                {@link org.havi.ui.HStillImageBackgroundConfiguration
     *                HStillImageBackgroundConfiguration} is not the currently
     *                set configuration for its
     *                {@link org.havi.ui.HBackgroundDevice HBackgroundDevice}.
     */
    void displayImage(final HDStillImageBackgroundConfiguration config, final MpegBackgroundImage mpeg,
            final HScreenRectangle r) throws org.havi.ui.HPermissionDeniedException,
            org.havi.ui.HConfigurationException
    {
        withReservation(new ReservationAction()
        {
            public void run() throws HConfigurationException
            {
                if (config != currentConfiguration) throw new HConfigurationException("Not the current configuration");

                // Figure out the device pixel-relative placement
                // And display the image for the device
                mpeg.displayImage(nDevice, user(r, currentConfiguration));
            }
        });
    }

    /**
     * Converts the normalized screen rectangle to a user (device-specific)
     * pixel rectangle.
     * 
     * @param r
     *            the given screen rectangle (may be <code>null</code>)
     * @param c
     *            the given configuration
     * @return <code>USER(Xn,Yn,Wn,Hn) = floor((Xn - PXn) * (Wu/PWn) + 0.5),
     *                                   floor((Yn - PYn) * (Hu/PHn) + 0.5),
     *                                   floor(Wn * (Wu/PWn) + 0.5),
     *                                   floor(Wn * (Wu/PWn) + 0.5)</code>; or
     *         <code>null</code> if <code><i>r</i> == null</code>
     */
    private Rectangle user(HScreenRectangle r, HScreenConfiguration c)
    {
        if (r == null) return null;

        HScreenRectangle area = c.getScreenArea();
        Dimension size = c.getPixelResolution();

        return new Rectangle((int) Math.floor((r.x - area.x) * (size.width / area.width) + 0.5),
                (int) Math.floor((r.y - area.y) * (size.height / area.height) + 0.5), (int) Math.floor(r.width
                        * (size.width / area.width) + 0.5), (int) Math.floor(r.height * (size.height / area.height)
                        + 0.5));
    }
}
