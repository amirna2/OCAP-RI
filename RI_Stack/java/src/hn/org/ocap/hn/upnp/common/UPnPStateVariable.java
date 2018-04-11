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

package org.ocap.hn.upnp.common;

/**
 * This interface is an abstract representation of a UPnP state variable.
 * It provides the data constituting a state variable that is
 * independent of the network interface on which it has been advertised.
 */
public interface UPnPStateVariable {

    /**
     * Gets the allowed values for this UPnP state variable.
     * The value returned is formatted per the UPnP Device
     * Architecture specification, service description,
     * {@code allowedValueList} element definition. If the
     * <code>UPnPStateVariable</code> does not have an
     * allowedValueList specified, returns zero length array.
     *
     * @return An array containing the allowed values for this state
     * variable.  Each element in the array contains the
     * value of one allowedValue element in the
     * allowedValueList.  The array has the same order as
     * the allowedValueList element.
     */
    String[] getAllowedValues();

    /**
     * Reports the default value of this UPnP state variable.
     * This value is taken from the {@code defaultValue} element in
     * the UPnP service description that defines this state variable.
     *
     * @return The default value of this state variable. Returns an empty
     * string if the variable does not have a defaultValue.
     */
    String getDefaultValue();

    /**
     * Gets the allowedValueRange maximum value of this UPnP state
     * variable.  The value returned is formatted per the UPnP Device
     * Architecture specification, service description,
     * {@code allowedValueRange} maximum element definition.
     *
     * @return A <code>String</code> containing the maximum allowed
     * value for this state variable. Returns an empty
     * string if the variable does not have an
     * allowedValueRange.
     */
    String getMaximumValue();

    /**
     * Gets the allowedValueRange minimum value for this UPnP state
     * variable.  The value returned is formatted per the UPnP Device
     * Architecture specification, service description,
     * {@code allowedValueRange} minimum element definition.
     *
     * @return A <code>String</code> containing the minimum allowed
     * value for this state variable. Returns an empty
     * string if the variable does not have an
     * allowedValueRange.
     */
    String getMinimumValue();

    /**
     * Gets the name of this UPnP state variable.
     * This value is taken from the {@code name} element of
     * the UPnP service description {@code stateVariable} element.
     *
     * @return The name of the state variable.
     */
    String getName();

    /**
     * Gets the data type of this UPnP state variable.  The value returned
     * is formatted per the UPnP Device Architecture specification,
     * service description, {@code dataType} element definition.
     *
     * @return The data type of the state variable.
     */
    String getDataType();

    /**
     * Gets the allowedValueRange step value for this UPnP state variable.
     * The value returned is formatted per the UPnP Device
     * Architecture specification, service description,
     * {@code allowedValueRange step} element definition.
     * <p>
     * Note that if the {@code step} element is omitted and the data type
     * of the state variable is an integer, the step value is considered to
     * be 1.
     *
     * @return A <code>String</code> containing the {@code step} value for
     * this state variable. Returns an empty string if service description of
     * this variable does not specify a {@code step} value.
     */
    String getStepValue();

    /**
     * Indicates if this state variable is evented.
     * The value is taken from the {@code sendEvents} attribute in the
     * UPnP service description that defines this state variable.
     *
     * @return True if this UPnP state variable is evented, otherwise
     * returns false.
     */
    boolean isEvented();

}
