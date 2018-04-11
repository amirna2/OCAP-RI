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

import org.w3c.dom.Document;


/**
 * This interface represents a UPnP service as it is advertised on a particular
 * network.
 * It provides the data constituting a UPnP service, portions of
 * which depend on the network interface on which it is advertised.
 * Corresponds to the information carried in the UPnP service description
 * document plus service-specific
 * data from the UPnP device description document.
 */
public interface UPnPAdvertisedService extends UPnPService {

    /**
     * Gets the UPnP controlURL of this service. This value is taken
     * from the value of the {@code controlURL} element within the device
     * description.
     *
     * @return The URL used by a control point to invoke actions on
     * this service.
     */
    String getControlURL();

    /**
     * Gets the UPnP device that this service is a part of.
     *
     * @return The device that this service is a part of.
     */
    // UPnPAdvertisedDevice getAdvertisedDevice();

    /**
     * Gets the UPnP eventSubURL of this service. This value is
     * taken from the value of the {@code eventSubURL} element within a
     * device description. If this service does not have eventing,
     * the value returned is the empty string.
     *
     * @return The URL used by a control point to subscribe to
     * evented state variables.
     */
    String getEventSubURL();

    /**
     * Gets the UPnP SCPDURL of this service. This value is taken
     * from the value of the {@code SCPDURL} element within a device
     * description.
     *
     * @return The URL used to retrieve the service description of
     * this service.
     */
    String getSCPDURL();

    /**
     * Gets a UPnP state variable from the UPnP description of this
     * service.  Supported state variable names are provided by a UPnP device
     * in the name element of each stateVariable element in a device
     * service description.
     *
     * @param stateVariableName The name of the state variable to get.
     *
     * @return The state variable corresponding to the
     * <code>stateVariableName</code> parameter.
     *
     * @throws IllegalArgumentException if the <code>stateVariableName</code>
     * does not match a state variable name in this service.
     */
    UPnPAdvertisedStateVariable getAdvertisedStateVariable(String stateVariableName);

    /**
     * Gets all of the UPnP state variables supported by this
     * service.  UPnP state variable information is taken from the
     * {@code stateVariable} elements in the UPnP service description.
     *
     * @return  The UPnP state variables supported by this
     * service. If the service has no state variables, returns a zero-length
     * array.
     */
    UPnPAdvertisedStateVariable[] getAdvertisedStateVariables();

    /**
     * Gets the service description document (SCPD document) in XML.
     * The form of the document is defined by
     * the UPnP Device Architecture specification.
     *
     * @return The service description document.
     */
    Document getXML();

}
