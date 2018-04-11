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

/**
 * An {@link org.havi.ui.HEmulatedGraphicsDevice HEmulatedGraphicsDevice} is a
 * &quot;virtual&quot; graphics device that has the capability to be configured
 * to perform one (of many) possible emulations. For example, in the DVB context
 * a 4:3 television might have an {@link org.havi.ui.HEmulatedGraphicsDevice
 * HEmulatedGraphicsDevice} that had an
 * {@link org.havi.ui.HEmulatedGraphicsConfiguration
 * HEmulatedGraphicsConfiguration} that emulated a virtual 14:9 display. The
 * 14:9 {@link org.havi.ui.HEmulatedGraphicsConfiguration
 * HEmulatedGraphicsConfiguration} would be used for rendering into from AWT,
 * whilst being displayed on the &quot;true&quot; 4:3 physical display. The
 * relationship between the emulation and implementation is encapsulated within
 * the {@link org.havi.ui.HEmulatedGraphicsConfiguration
 * HEmulatedGraphicsConfiguration}.
 * 
 * <p>
 * An {@link org.havi.ui.HEmulatedGraphicsDevice HEmulatedGraphicsDevice}
 * transforms both AWT pixel-oriented drawing operations and AWT user-input
 * event coordinates, this is performed outside of the Java application
 * (typically in hardware).
 * 
 * <p>
 * An {@link org.havi.ui.HEmulatedGraphicsDevice HEmulatedGraphicsDevice} may
 * (of necessity) modify coordinates for Components and/or events to the nearest
 * physical / virtual pixel --- authors should not depend on single pixel
 * accuracy.
 * 
 * <p>
 * There is no difference to a Java application between an
 * {@link org.havi.ui.HGraphicsDevice HGraphicsDevice} and an
 * {@link org.havi.ui.HEmulatedGraphicsDevice HEmulatedGraphicsDevice}, except
 * for the implication of possible rounding errors in integer pixel positions,
 * e.g. Component placement and/or resolution of events.
 * 
 * <p>
 * Java2D mechanisms should behave as per their normal semantics, with respect
 * to display on-screen.
 * 
 * @author Alex Resh
 * @author Todd Earles
 * @version 1.1
 */

public abstract class HEmulatedGraphicsDevice extends HGraphicsDevice
{
    /**
     * It is not intended that applications should directly construct
     * {@link org.havi.ui.HEmulatedGraphicsDevice HEmulatedGraphicsDevice}
     * objects.
     * <p>
     * Creates an {@link org.havi.ui.HEmulatedGraphicsDevice
     * HEmulatedGraphicsDevice} object. See the class description for details of
     * constructor parameters and default values.
     */
    protected HEmulatedGraphicsDevice()
    {
    }

    /**
     * Set the graphics configuration for the device.
     * 
     * @param hegc
     *            the {@link HEmulatedGraphicsConfiguration
     *            HEmulatedGraphicsConfiguration } to which this device should
     *            be set.
     * @return A boolean indicating whether the configuration could be applied
     *         successfully. If the configuration could not be applied
     *         successfully, the configuration after this method may not match
     *         the configuration of the device prior to this method being called
     *         --- applications should take steps to determine whether a partial
     *         change of settings has been made.
     * 
     * @exception SecurityException
     *                if the application does not have sufficient rights to set
     *                the configuration for this device.
     * @exception HPermissionDeniedException
     *                ({@link org.havi.ui.HPermissionDeniedException
     *                HPermissionDeniedException}) if the application does not
     *                currently have the right to set the configuration for this
     *                device.
     * @exception HConfigurationException
     *                ({@link org.havi.ui.HConfigurationException
     *                HConfigurationException}) if the specified configuration
     *                is not valid for this device.
     */
    public boolean setGraphicsConfiguration(HEmulatedGraphicsConfiguration hegc) throws SecurityException,
            org.havi.ui.HPermissionDeniedException, org.havi.ui.HConfigurationException
    {
        // Subclasses must override this method to provide the platform specific
        // implementation.
        return false;
    }
}
