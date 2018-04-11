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
import java.awt.Frame;
import java.awt.Container;

import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.havi.ExtendedGraphicsDevice;
import org.cablelabs.impl.havi.ExtendedScreen;
import org.cablelabs.impl.havi.DisplayMediator;
import org.cablelabs.impl.havi.ReservationAction;

/**
 * Implementation of {@link HGraphicsDevice} for the MPE port intended to run on
 * an OCAP implementation.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 * @version $Id: GraphicsDevice.java,v 1.15 2002/06/03 21:31:04 aaronk Exp $
 */
public class GraphicsDevice extends HGraphicsDevice implements ExtendedGraphicsDevice
{
    /** Configurations */
    private HGraphicsConfiguration[] configurations = null;

    /** Current configuration */
    private HGraphicsConfiguration currentConfiguration = null;

    /** The HScene mediator for the device */
    private DisplayMediator mediator = null;

    /**
     * Construct a graphics device with the specified resolution and add it to
     * the screen.
     * 
     * @param screenSize
     *            the resolution of the screen
     * @param frame
     *            the frame that represents the screen
     * @param root
     *            the root container that represents the screen
     */
    GraphicsDevice(Dimension screenSize, Frame frame, Container root)
    {
        // Create the container that will be the representation of this device.
        HContainer container = new HContainer();
        container.setBounds(0, 0, screenSize.width, screenSize.height);

        // Set the default values for the device. These propagate to all
        // children of the container unless overridden.
        java.awt.Color color;
        if ((color = Toolkit.getColor(Property.FOREGROUND)) != null) container.setForeground(color);
        if ((color = Toolkit.getColor(Property.BACKGROUND)) != null) container.setBackground(color);
        container.setFont(HaviToolkit.getToolkit().getDefaultFont());

        // Add the container to the screen
        root.add(container);

        // Create the device configuration
        configurations = new HGraphicsConfiguration[] { new GraphicsConfiguration(this, screenSize) };
        currentConfiguration = configurations[0];

        // Create the HScene mediator
        mediator = new DisplayMediatorImpl(frame, root, container);
    }

    // Definition copied from superclass
    public String getIDstring()
    {
        return "Generic Graphics Device";
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
    public HGraphicsConfiguration[] getConfigurations()
    {
        return configurations;
    }

    // Definition copied from superclass
    public HGraphicsConfiguration getDefaultConfiguration()
    {
        return configurations[0];
    }

    // Definition copied from superclass
    public HGraphicsConfiguration getCurrentConfiguration()
    {
        return currentConfiguration;
    }

    // Definition copied from superclass
    public boolean setGraphicsConfiguration(final HGraphicsConfiguration hgc) throws SecurityException,
            HPermissionDeniedException, HConfigurationException
    {
        // Make sure the caller has reserved the device
        final boolean[] failed = { false };
        withReservation(new ReservationAction()
        {
            public void run()
            {
                // There is only one configuration for this device so it cannot
                // change.
                // Therefore, we never need to notify listeners of a change.
                // However, we
                // do need to make sure this is the valid configuration.
                if (hgc != configurations[0]) failed[0] = true;

                // If this were a new configuration then we'd notify any
                // listeners
            }
        });
        if (failed[0]) throw new HConfigurationException();

        return true;
    }

    // Definition copied from ExtendedGraphicsDevice
    public DisplayMediator getDisplayMediator()
    {
        return mediator;
    }

    // Definition copied from superclass
    public HGraphicsConfiguration createEmulatedConfiguration(HGraphicsConfigTemplate hgct)
    {
        return null; // no emulated configurations are supported
    }

    // Definition copied from superclass
    public HGraphicsConfiguration createEmulatedConfiguration(HGraphicsConfigTemplate hgcta[])
    {
        return null; // no emulated configurations are supported
    }
}
