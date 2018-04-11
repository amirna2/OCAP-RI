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
 * This interface represents the description of a UPnP service
 * action, parsed from the UPnP service description XML. It
 * contains both IN and OUT argument descriptions, but does not
 * carry any values.
 */
public interface UPnPAction
{
    /**
     * Gets the name of the action from the action name element in the
     * UPnP service description.
     *
     * @return name of the action.
     */
    public String getName();

    /**
     * Gets the action argument names from the action description in the UPnP
     * service description.
     *
     * @return The IN and OUT argument names for this action, in the order
     * specified by the UPnP service description defining this action.
     * If the action has no arguments, returns a zero length array.
     */
    public String[] getArgumentNames();

    /**
     * Gets the direction of an argument.
     *
     * @param name Name of the argument.
     *
     * @return True if the argument is an input argument.
     *
     * @throws IllegalArgumentException if the name does not represent a
     *      valid argument name for the action.
     */
    public boolean isInputArgument(String name);

    /**
     * Determines whether the specified argument is flagged as a
     * return value in the service description.
     *
     * @param name Name of the argument.
     *
     * @return true if the argument is flagged as a retval.
     *
     * @throws IllegalArgumentException if the name does not represent a
     *      valid argument name for the action.
     */
    public boolean isRetval(String name);

    /**
     * Gets the UPnP service that this <code>UPnPAction</code>
     * is associated with.
     * The returned {@code UPnPService} object may be cast to a
     * {@link org.ocap.hn.upnp.server.UPnPManagedService UPnPManagedService}
     * by server applications, or to a
     * {@link org.ocap.hn.upnp.client.UPnPClientService UPnPClientService}
     * by client applications.
     *
     * @return The UPnP service that this action is associated with.
     */
    public UPnPService getService();

    /**
     * Gets the UPnP state variable associated with the specified
     * argument name.
     * The returned {@code UPnPStateVariable} object may be cast to a
     * {@link org.ocap.hn.upnp.server.UPnPManagedStateVariable UPnPManagedStateVariable}
     * by server applications, or to a
     * {@link org.ocap.hn.upnp.client.UPnPClientStateVariable UPnPClientStateVariable}
     * by client applications.
     *
     * @param name Name of the argument.
     *
     * @return The UPnP state variable associated with the specified
     *          argument name.
     *
     * @throws IllegalArgumentException if the name does not represent a
     *      valid argument name for the action.
     */
    public UPnPStateVariable getRelatedStateVariable(String name);
}
