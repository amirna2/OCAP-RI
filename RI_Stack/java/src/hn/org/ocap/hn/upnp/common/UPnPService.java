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
 * This interface is an abstract representation of a UPnP service.
 * It provides the data constituting a UPnP service that is
 * independent of the network interface on which it has been advertised.
 */
public interface UPnPService {

    /**
     * Gets the named action from this service.
     *
     * @param actionName The name of the UPnPAction to retrieve.
     *
     * @return The UPnPAction object from this service with the
     * matched name.
     *
     * @throws IllegalArgumentException if the
     * <code>actionName</code> does not match an action
     * name in this service.
     */
    UPnPAction getAction(String actionName);

    /**
     * Gets the actions that can be used with this service.
     *
     * @return An array of <code>UPnPAction</code>s. If the service
     * has no actions, returns an zero-length array.
     */
    UPnPAction[] getActions();

    /**
     * Gets the UPnP serviceId of this service. This value is taken
     * from the value of the {@code serviceId} element within the device
     * description.
     *
     * @return The serviceId of this service.
     */
    String getServiceId();

    /**
     * Gets the UPnP serviceType of this service. This value is
     * taken from the value of the {@code serviceType} element within the
     * device description.
     *
     * @return The type of this service.
     */
    String getServiceType();

    /**
     * Gets the UPnP specVersion major and minor values of this
     * service. This value is taken from the value of the major
     * and minor sub-elements of the {@code specVersion} element within the
     * service description.
     * The format of the returned String is the &lt;major&gt;
     * value, followed by '.', followed by the &lt;minor&gt; value.
     *
     * @return The UPnP specVersion of this service.
     */
    String getSpecVersion();
}
