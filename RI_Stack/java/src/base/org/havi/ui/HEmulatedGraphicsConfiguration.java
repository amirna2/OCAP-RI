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
 * An {@link org.havi.ui.HEmulatedGraphicsConfiguration
 * HEmulatedGraphicsConfiguration} is a configuration for a &quot;virtual&quot;
 * graphics device that may perform one or more emulations, e.g. in the ATSC
 * context an {@link org.havi.ui.HEmulatedGraphicsDevice
 * HEmulatedGraphicsDevice} might implement multiple
 * {@link org.havi.ui.HEmulatedGraphicsConfiguration
 * HEmulatedGraphicsConfigurations}, corresponding to each of the possible
 * relationships to the high-definition display modes. The
 * {@link org.havi.ui.HEmulatedGraphicsConfiguration
 * HEmulatedGraphicsConfiguration} would be used to configure a device
 * appropriately for rendering into, whilst mapping the emulated device onto the
 * &quot;true&quot; physical display, e.g. by down-sampling to
 * standard-definition display.
 * 
 * <p>
 * In essence the {@link org.havi.ui.HEmulatedGraphicsConfiguration
 * HEmulatedGraphicsConfiguration} may be considered as a pair of
 * {@link org.havi.ui.HGraphicsConfiguration HGraphicsConfiguration} objects:
 * one describing the configuration of the emulation and the second describing
 * the corresponding configuration of the implementation.
 * 
 * <p>
 * Hence, an {@link org.havi.ui.HGraphicsConfiguration HGraphicsConfiguration}
 * may be considered as a special case of the
 * {@link org.havi.ui.HEmulatedGraphicsConfiguration
 * HEmulatedGraphicsConfiguration} class, where the emulation and implementation
 * are equivalent.
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=5>None.</td>
 * </tr>
 * </table>
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=4>None.</td>
 * </tr>
 * </table>
 * 
 * @see HEmulatedGraphicsDevice
 * @see HGraphicsConfiguration
 * @author Alex Resh
 * @author Todd Earles
 * @version 1.1
 */

public class HEmulatedGraphicsConfiguration extends HGraphicsConfiguration
{

    /**
     * It is not intended that applications should directly construct
     * {@link org.havi.ui.HEmulatedGraphicsConfiguration
     * HEmulatedGraphicsConfiguration} objects.
     * <p>
     * Creates an {@link org.havi.ui.HEmulatedGraphicsConfiguration
     * HEmulatedGraphicsConfiguration} object. See the class description for
     * details of constructor parameters and default values.
     */
    protected HEmulatedGraphicsConfiguration()
    {
    }

    /**
     * Returns an {@link org.havi.ui.HGraphicsConfigTemplate
     * HGraphicsConfigTemplate} describing the virtual (emulation)
     * characteristics of the {@link org.havi.ui.HEmulatedGraphicsDevice
     * HEmulatedGraphicsDevice}.
     * 
     * <p>
     * Overridden method from {@link org.havi.ui.HGraphicsConfiguration
     * HGraphicsConfiguration} -- for an
     * {@link org.havi.ui.HEmulatedGraphicsConfiguration
     * HEmulatedGraphicsConfiguration} this returns a description of the
     * emulation characteristics.
     * 
     * @return an {@link org.havi.ui.HGraphicsConfigTemplate
     *         HGraphicsConfigTemplate} describing the virtual (emulation)
     *         characteristics of the
     *         {@link org.havi.ui.HEmulatedGraphicsDevice
     *         HEmulatedGraphicsDevice}.
     * 
     * @see HGraphicsConfigTemplate
     * @see HGraphicsConfiguration
     * @see HEmulatedGraphicsDevice
     */
    public HGraphicsConfigTemplate getConfigTemplate()
    {
        // Subclasses must override this method to provide the platform specific
        // implementation.
        return null;
    }

    /**
     * Returns an {@link org.havi.ui.HGraphicsConfigTemplate
     * HGraphicsConfigTemplate} describing the virtual (emulation)
     * characteristics of the {@link org.havi.ui.HEmulatedGraphicsDevice
     * HEmulatedGraphicsDevice}.
     * 
     * @return an {@link org.havi.ui.HGraphicsConfigTemplate
     *         HGraphicsConfigTemplate} describing the virtual (emulation)
     *         characteristics of the
     *         {@link org.havi.ui.HEmulatedGraphicsDevice
     *         HEmulatedGraphicsDevice}.
     * 
     * @see HGraphicsConfigTemplate
     * @see HEmulatedGraphicsDevice
     */
    public HGraphicsConfigTemplate getEmulation()
    {
        // Subclasses must override this method to provide the platform specific
        // implementation.
        return null;
    }

    /**
     * Returns an {@link org.havi.ui.HGraphicsConfigTemplate
     * HGraphicsConfigTemplate} describing the physical (implementation)
     * characteristics of the {@link org.havi.ui.HEmulatedGraphicsDevice
     * HEmulatedGraphicsDevice}.
     * 
     * @return an {@link org.havi.ui.HGraphicsConfigTemplate
     *         HGraphicsConfigTemplate} describing the physical (implementation)
     *         characteristics of the
     *         {@link org.havi.ui.HEmulatedGraphicsDevice
     *         HEmulatedGraphicsDevice}.
     * 
     * @see HGraphicsConfigTemplate
     * @see HEmulatedGraphicsDevice
     */
    public HGraphicsConfigTemplate getImplementation()
    {
        // Subclasses must override this method to provide the platform specific
        // implementation.
        return null;
    }
}
