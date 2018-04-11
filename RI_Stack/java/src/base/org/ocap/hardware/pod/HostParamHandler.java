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

// HostParamHandler.java

package org.ocap.hardware.pod;

/**
 * <p>
 * A class that implements this interface can reject the update of the Feature
 * parameter in the Host device. Feature parameter is defined for the Generic
 * Feature Control Support in the OpenCable CableCARD Interface specification.
 * An OCAP-J application can set only one instance of such classes to the OCAP
 * implementation via the {@link org.ocap.hardware.pod.POD#setHostParamHandler}
 * method.
 * </p>
 * <p>
 * Before Feature parameter in the Host is modified, the
 * {@link HostParamHandler#notifyUpdate} method shall be called with the Feature
 * ID to be modified and its Feature parameter value. And only if the
 * HostParamHandler.notifyUpdate() method returns true, the Feature parameter
 * value in the Host device will be modified. Note that the Host device may
 * reject the update of Feature parameter even if the
 * HostParamHandler.notifyUpdate() method returns true.
 * </p>
 * <p>
 * The Feature ID and the Feature parameter value format are defined in the
 * CableCARD Interface 2.0 Specification [4]. For example, the Feature ID of
 * "RF Output Channel" Feature is 0x1, and its parameter value format is <br>
 * <code>
 * Rf_output_channel() {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;Output_channel<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;Output_channel_ui<br>
 * }<br>
 * </code>
 * </p>
 * <p>
 * The Feature parameters in the Host device will be modified by the following
 * cases.
 * <ul>
 * <li>The CableCARD sends feature_parameters APDU to the Host.
 *     (See the [CCIF 2.0].)
 * <li>The Host modifies its own Feature parameters.
 * <li>An OCAP-J application calls the
 * {@link org.ocap.hardware.pod.POD#updateHostParam} method.
 * </ul>
 * In every cases, the HostParamHandler.notifyUpdate() method shall be called.
 * </p>
 *
 */
public interface HostParamHandler
{
    /**
     * <p>
     * This is a call back method to notify an update of the Feature parameter
     * in the Host device. This method shall be called every time before the
     * Feature parameter is modified. Only if this method returns true, the Host
     * device can modify its Feature parameter by the specified value.
     * </p>
     * <p>
     * Note that the Host device may reject the update of Feature parameter even
     * if the HostParamHandler.notifyUpdate() method returns true.
     * </p>
     * <p>
     * This method should return immediately without blocking.
     * </p>
     *
     * @param featureID
     *            a Feature ID for the Generic Feature Control Support in the
     *            CableCARD Interface 2.0 Specification [4]. The Feature ID
     *            reserved for proprietary use (0x70 - 0xFF) can be specified.
     *
     * @param value
     *            a Feature parameter value for the specified featureID. An
     *            actual format of each Feature parameter is defined in the
     *            CableCARD Interface 2.0 Specification [4]. For example, if the
     *            featureID is 0x1, the value is <br>
     *            <code>
     *                Rf_output_channel() {<br>
     *                &nbsp;&nbsp;&nbsp;&nbsp;Output_channel<br>
     *                &nbsp;&nbsp;&nbsp;&nbsp;Output_channel_ui<br>
     *                }
     *                </code>
     *
     * @return <code>true</code> to accept the modification of the specified
     *         value. <code>false</code> to reject it.
     *
     * @see org.ocap.hardware.pod.POD#setHostParamHandler
     **/
    public boolean notifyUpdate(int featureID, byte[] value);
}
