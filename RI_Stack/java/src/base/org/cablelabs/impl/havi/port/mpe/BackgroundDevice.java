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

import org.havi.ui.*;

import java.awt.Dimension;
import java.awt.Container;
import org.cablelabs.impl.havi.ExtendedScreen;
import org.cablelabs.impl.havi.ExtendedBackgroundDevice;
import org.cablelabs.impl.havi.ReservationAction;

/**
 * Implementation of {@link HBackgroundDevice} for the MPE port intended to run
 * on an OCAP implementation.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 * @version $Revision: 1.14 $, $Date: 2002/06/03 21:31:03 $
 */
public class BackgroundDevice extends HBackgroundDevice implements ExtendedBackgroundDevice
{
    /** Configurations */
    private HBackgroundConfiguration[] configurations = null;

    /** Current configuration */
    private HBackgroundConfiguration currentConfiguration = null;

    /** The component that represents this background device */
    private HComponent backgroundComponent = null;

    /** Device dimensions */
    private Dimension deviceSize;

    /** Enabled background device emulation using a component. */
    private static final boolean ENABLED_COMPONENT = false;

    /**
     * Construct a background device with the specified resolution and add it to
     * the screen.
     * 
     * @param screenSize
     *            the resolution of the screen
     * @param root
     *            the root container that represents the screen
     */
    BackgroundDevice(Dimension screenSize, Container root)
    {
        // The device size is the same as the screen size
        deviceSize = screenSize;

        if (ENABLED_COMPONENT)
        {
            // Create the component that will be the representation of this
            // background device.
            backgroundComponent = new HComponent()
            {
                // On this platform we emulate a background as a component that
                // is
                // painted. We let the configuration class handle the actual
                // painting via its refresh method.
                public void paint(java.awt.Graphics g)
                {
                    HBackgroundConfiguration hbc = getCurrentConfiguration();
                    if (hbc instanceof StillImageBackgroundConfiguration)
                        ((StillImageBackgroundConfiguration) hbc).refresh(g);
                    else if (hbc instanceof BackgroundConfiguration) ((BackgroundConfiguration) hbc).refresh(g);
                }
            };
            backgroundComponent.setBounds(0, 0, deviceSize.width, deviceSize.height);

            // Add the component to the screen
            root.add(backgroundComponent);
        }

        // Create the background device configurations and assign them to the
        // background device. We create one color based configuration and one
        // image based configuration.
        configurations = new HBackgroundConfiguration[] { new BackgroundConfiguration(this, deviceSize),
                new StillImageBackgroundConfiguration(this, deviceSize) };
        currentConfiguration = configurations[0];
    }

    // Definition copied from superclass
    public String getIDstring()
    {
        return "Generic Background Device";
    }

    // Definition copied from superclass
    public Dimension getScreenAspectRatio()
    {
        return new Dimension(4, 3);
    }

    // Definition copied from superclass
    public ExtendedScreen getScreen()
    {
        // In this implementation there is only one screen so we can just ask
        // for it.
        return (ExtendedScreen) HScreen.getDefaultHScreen();
    }

    // Definition copied from superclass
    public HBackgroundConfiguration[] getConfigurations()
    {
        return configurations;
    }

    // Definition copied from superclass
    public HBackgroundConfiguration getDefaultConfiguration()
    {
        return configurations[0];
    }

    // Definition copied from superclass
    public HBackgroundConfiguration getCurrentConfiguration()
    {
        return currentConfiguration;
    }

    // Definition copied from superclass
    public boolean setBackgroundConfiguration(final HBackgroundConfiguration hbc) throws SecurityException,
            HPermissionDeniedException, HConfigurationException
    {
        // Make sure the caller has reserved the device
        final boolean[] failed = { false };
        withReservation(new ReservationAction()
        {
            public void run()
            {
                // Make sure it is a valid configuration for this device
                if ((hbc != configurations[0]) && (hbc != configurations[1])) failed[0] = true;

                // If this is a new configuration then switch to it and notify
                // listeners
                if (currentConfiguration != hbc)
                {
                    currentConfiguration = hbc;
                    refresh(hbc);
                    notifyScreenConfigListeners(hbc);
                }
            }
        });
        if (failed[0]) throw new HConfigurationException();

        return true;
    }

    /**
     * Refresh the background device by calling the repaint method. This forces
     * the paint method to be called. The repaint is only performed if the
     * current configuration is the one that has changed.
     * 
     * @param hbc
     *            the background configuration that changed
     */
    void refresh(HBackgroundConfiguration hbc)
    {
        if (ENABLED_COMPONENT) if (hbc == getCurrentConfiguration()) backgroundComponent.repaint();
    }
}
