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
 * The {@link org.havi.ui.HBackgroundConfiguration} class describes the
 * characteristics (settings) of an {@link org.havi.ui.HBackgroundDevice}. There
 * can be many {@link org.havi.ui.HBackgroundConfiguration} objects associated
 * with a single {@link org.havi.ui.HBackgroundDevice} .
 * <p>
 * The basic background configuration supports backgrounds of a single color.
 * More sophisticated backgrounds can be supported by defining new classes
 * inheriting from this class. Where a device has a single non- changeable
 * background color, this class will provide applications the ability to read
 * that color however all attempts to reserve control of the background will
 * fail.
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
 * @see HBackgroundDevice
 * @author Alex Resh
 * @author Todd Earles
 * @version 1.1
 */

public class HBackgroundConfiguration extends HScreenConfiguration
{
    /**
     * It is not intended that applications should directly construct
     * {@link org.havi.ui.HBackgroundConfiguration} objects.
     * <p>
     * Creates an {@link org.havi.ui.HBackgroundConfiguration} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    protected HBackgroundConfiguration()
    {
    }

    /**
     * Returns the {@link org.havi.ui.HBackgroundDevice} associated with this
     * {@link org.havi.ui.HBackgroundConfiguration}.
     * 
     * @return the {@link org.havi.ui.HBackgroundDevice HBackgroundDevice}
     *         object that is associated with this
     *         {@link org.havi.ui.HBackgroundConfiguration}.
     */
    public HBackgroundDevice getDevice()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Returns an {@link org.havi.ui.HBackgroundConfigTemplate} object that
     * describes and uniquely identifies this
     * {@link org.havi.ui.HBackgroundConfiguration}. Hence, the following
     * sequence should return the original
     * {@link org.havi.ui.HBackgroundConfiguration}
     * 
     * <pre>
     * HBackgroundDevice.getBestMatch(HBackgroundConfiguration.getConfigTemplate())
     * </pre>
     * <p>
     * Features that are implemented in the
     * {@link org.havi.ui.HBackgroundConfiguration} will return
     * {@link HScreenConfigTemplate#REQUIRED} priority. Features that are not
     * implemented in the {@link org.havi.ui.HBackgroundConfiguration} will
     * return {@link HScreenConfigTemplate#REQUIRED_NOT} priority. Preferences
     * that are not filled in by the platform will return
     * {@link HScreenConfigTemplate#DONT_CARE} priority.
     * 
     * @return an {@link org.havi.ui.HBackgroundConfigTemplate} object which
     *         both describes and uniquely identifies this
     *         {@link org.havi.ui.HBackgroundConfiguration}.
     */
    public HBackgroundConfigTemplate getConfigTemplate()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Obtain the current color of this background. This method may be called
     * without ownership of the resource. The value returned is not guaranteed
     * to be the value set in the last call to
     * {@link org.havi.ui.HBackgroundConfiguration#setColor} since platforms may
     * offer a reduced color space for backgrounds and the actual value used
     * will be returned.
     * 
     * @return the current Color
     */
    public java.awt.Color getColor()
    {
        // This method must be implemented in the port specific subclass.
        return null;
    }

    /**
     * Set the current color of this background. On platforms where there is a
     * sub-class of java.awt.Color supporting transparency of any kind, passing
     * an object representing a non-opaque color is illegal. Platforms with a
     * limited color resolution for backgrounds may approximate this value to
     * the nearest available. The
     * {@link org.havi.ui.HBackgroundConfiguration#getColor} method will return
     * the actual value used.
     * 
     * @param color
     *            the color to be used for the background
     * @exception HPermissionDeniedException
     *                if the application has not currently reserved the
     *                HBackgroundDevice associated with this configuration or
     *                this configuration is not the current configuration of
     *                that HBackgroundDevice.
     * @exception HConfigurationException
     *                if the color specified is illegal for this platform.
     */
    public void setColor(java.awt.Color color) throws org.havi.ui.HPermissionDeniedException,
            org.havi.ui.HConfigurationException
    {
        // This method must be implemented in the port specific subclass.
    }
}
