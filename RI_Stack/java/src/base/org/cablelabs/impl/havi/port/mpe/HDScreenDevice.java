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

import org.havi.ui.HScreenConfiguration;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HPermissionDeniedException;
import org.cablelabs.impl.havi.ReservationAction;
import org.cablelabs.impl.ocap.resource.ExtendedReserveDevice;
import org.cablelabs.impl.util.NativeHandle;

/**
 * This interface specifies an interface that is common to all
 * <code>HScreenDevice</code> implementations.
 * 
 * @see HDBackgroundDevice
 * @see HDGraphicsDevice
 * @see HDVideoDevice
 */
interface HDScreenDevice extends ExtendedReserveDevice, NativeHandle
{
    /**
     * Records a change in configuration for this configuration. This method
     * should only be called under the following circumstances:
     * <ul>
     * <li>The configuration of this device has already been changed to this
     * configuration at the native level.
     * <li>While holding a reservation for this device (i.e., from within a
     * <code>ReservationAction</code> called passed to
     * <code>withReservation()</code>).
     * <li>While holding the parent screen lock.
     * <li>After already verifying that this is a valid configuration for this
     * device.
     * </ul>
     * <p>
     * This will record the change and notify any registered listeners of the
     * change.
     * 
     * @see HDGraphicsDevice#setGraphicsConfiguration
     * @see HDVideoDevice#setVideoConfiguration
     * @see HDBackgroundDevice#setBackgroundConfiguration
     * @see HDCoherentConfig#select
     */
    public void changeConfiguration(HScreenConfiguration config);

    /**
     * Return a numeric value that indicates the strength of the match between
     * the specified configuration and the specified template.
     * 
     * @param hsc
     *            the screen configuration
     * @param hsct
     *            the screen configuration template
     * @return the strength of the match between the specified screen
     *         configuration and screen template. -1 indicates no match. Higher
     *         values indicate a better (stronger) match.
     */
    public int getMatchStrength(HScreenConfiguration hsc, HScreenConfigTemplate hsct);

    /**
     * Temporarily reserve the device for the purpose of changing the
     * configuration of a device which would conflict with this device. If the
     * device is already reserved by the calling context, then nothing needs to
     * be done and <code>false</code> is returned.
     * <p>
     * If the method returns without throwing an exception, then the device is
     * reserved.
     * 
     * <p>
     * This is similar to <code>reserverDevice()</code> except:
     * <ul>
     * <li>No attempt is made to steal a reservation from another device.
     * <li>There is no outside-visible change in ownership (meaning that no
     * <code>ResourceStatusEvent</code>s are generated).
     * </ul>
     * <p>
     * <i>This method is exposed by this interface so that it is made
     * accessible.</i>
     * 
     * <p>
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     * 
     * @return <code>true</code> if
     *         {@link org.havi.ui.HScreenDevice#releaseDevice} should should be
     *         called when finished; <code>false</code> if it need not be
     *         called.
     * 
     * @throws HConfigurationException
     *             if the device is already reserved by another context
     * @throws HPermissionDeniedException
     *             if the device cannot be reserved by the current context
     */
    public boolean tempReserveDevice() throws HPermissionDeniedException;

    /**
     * Used to execute code while holding the device reservation. If the device
     * is reserved, then <code>ReservationAction.run()</code> will be executed.
     * If the device is not currently reserved by the caller, then an
     * <code>HPermissionDeniedException</code> will be thrown.
     * <p>
     * Note that no calls should be made to <i>unknown</i> (e.g., user-installed
     * listeners) from within the <code>ReservationAction.run()</code> method,
     * as this could present a potential for deadlock.
     * 
     * <p>
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     * 
     * @param run
     *            the <code>ReservationAction</code> to execute
     * @throws HPermissionDeniedException
     *             if the calling context does not have the device reserved
     * @throws HConfigurationException
     *             if a configuration is invalid
     */
    public void withReservation(ReservationAction run) throws HPermissionDeniedException, HConfigurationException;

    /**
     * Returns the current configuration.
     * 
     * @return the current configuration
     */
    public HScreenConfiguration getScreenConfig();
}
