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

import org.cablelabs.impl.manager.HostManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.MPEEnv;

/**
 * The {@link org.havi.ui.HVideoConfiguration} class describes the
 * characteristics (settings) of an {@link org.havi.ui.HVideoDevice}. There can
 * be many {@link org.havi.ui.HVideoConfiguration} objects associated with a
 * single {@link org.havi.ui.HVideoDevice}.
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
 * @see HVideoDevice
 * @author Alex Resh
 * @author Todd Earles
 * @version 1.1
 */

public class HVideoConfiguration extends HScreenConfiguration
{

    /**
     * It is not intended that applications should directly construct
     * {@link org.havi.ui.HVideoConfiguration} objects.
     * <p>
     * Creates an {@link org.havi.ui.HVideoConfiguration} object. See the class
     * description for details of constructor parameters and default values.
     */
    protected HVideoConfiguration()
    {
    }

    /**
     * Returns the {@link org.havi.ui.HVideoDevice} associated with this
     * {@link org.havi.ui.HVideoConfiguration}.
     * 
     * @return the {@link org.havi.ui.HVideoDevice} object that is associated
     *         with this {@link org.havi.ui.HVideoConfiguration},
     */
    public HVideoDevice getDevice()
    {
        // This method must be implemented in the port specific subclass.

        return null;
    }

    /**
     * Returns an {@link org.havi.ui.HVideoConfigTemplate} object that describes
     * and uniquely identifies this {@link org.havi.ui.HVideoConfiguration}.
     * <p>
     * Hence, the following sequence should return the original
     * {@link org.havi.ui.HVideoConfiguration}.
     * 
     * <pre>
     * HVideoDevice.getBestMatch({@link
     * org.havi.ui.HVideoConfiguration}.getConfigTemplate())
     * </pre>
     * <p>
     * Features that are implemented in the
     * {@link org.havi.ui.HVideoConfiguration} will return
     * {@link HScreenConfigTemplate#REQUIRED} priority. Features that are not
     * implemented in the {@link org.havi.ui.HVideoConfiguration
     * HVideoConfiguration} will return
     * {@link HScreenConfigTemplate#REQUIRED_NOT} priority. Preferences that are
     * not filled in by the platform will return
     * {@link HScreenConfigTemplate#DONT_CARE} priority.
     * 
     * @return an {@link org.havi.ui.HVideoConfigTemplate} object which both
     *         describes and uniquely identifies this
     *         {@link org.havi.ui.HVideoConfiguration}.
     * */
    public HVideoConfigTemplate getConfigTemplate()
    {
        // This method must be implemented in the port specific subclass.

        return null;
    }
}
