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

package org.havi.ui.event;

import org.cablelabs.impl.havi.HaviToolkit;

/**
 * This class is used to describe the (basic) remote control capabilities of the
 * platform.
 * <p>
 * This class is not intended to be constructed by applications.
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
 * @author Aaron Kamienski
 */

public class HRcCapabilities extends HKeyCapabilities
{
    /**
     * It is not intended that applications should directly construct
     * {@link org.havi.ui.event.HRcCapabilities HRcCapabilities} objects.
     * <p>
     * Creates an {@link org.havi.ui.event.HRcCapabilities HRcCapabilities}
     * object. See the class description for details of constructor parameters
     * and default values.
     * <p>
     * This method is protected to allow the platform to override it in a
     * different package scope.
     */
    protected HRcCapabilities()
    {
    }

    /**
     * Get the <code>HEventRepresentation</code> object for a specified key
     * event <code>keyCode</code>.
     * 
     * @param aCode
     *            the key event <code>keyCode</code> for which the
     *            <code>HEventRepresentation</code> should be returned.
     * @return an <code>HEventRepresentation</code> object for the specified key
     *         event <code>keyCode</code>, or <code>null</code> if there is no
     *         valid representation available.
     */
    public static HEventRepresentation getRepresentation(int aCode)
    {
        try
        {
            return HaviToolkit.getToolkit().getCapabilities().getRepresentation(aCode);
        }
        catch (NullPointerException e)
        {
            throw new RuntimeException("CapabilitiesSupport is not initialized");
        }
    }

    /**
     * Determine if a physical remote control exists in the system.
     * 
     * @return true if a physical remote control exists in the system, false
     *         otherwise.
     * @see HKeyCapabilities#getInputDeviceSupported
     */
    public static boolean getInputDeviceSupported()
    {
        try
        {
            return HaviToolkit.getToolkit().getCapabilities().isRemoteSupported();
        }
        catch (NullPointerException e)
        {
            throw new RuntimeException("CapabilitiesSupport is not initialized");
        }
    }

    /**
     * Queries whether the remote control can directly generate an event of the
     * given type. Note that this method will return false for key codes which
     * can only be generated on this system via a virtual keyboard.
     * 
     * @param keycode
     *            the keycode to query e.g. <code>VK_SPACE</code>
     * @return true if events with the given key code can be directly generated
     *         on this system via a physical remote control, false otherwise.
     * @see HKeyCapabilities#isSupported
     */
    public static boolean isSupported(int keycode)
    {
        try
        {
            return HaviToolkit.getToolkit().getCapabilities().isRcKeySupported(keycode);
        }
        catch (NullPointerException e)
        {
            throw new RuntimeException("CapabilitiesSupport is not initialized");
        }
    }

}
