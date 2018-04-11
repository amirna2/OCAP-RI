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

package org.ocap.hn.upnp.client;

import org.ocap.hn.upnp.common.UPnPAdvertisedService;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPAction;


/**
 * This interface is the client representation of a UPnP 
 * service. 
 */
public interface UPnPClientService extends UPnPAdvertisedService
{

    /**
     * Gets the UPnP device that this service is a part of.
     *
     * @return The device that this service is a part of.
     */
    UPnPClientDevice getDevice();

    /**
     * Posts an action to the network.  Sends the action from the control
     * point to the device the service is in.  The device MAY be on 
     * the local host.  If no handler is set when this method is 
     * called, the response is consumed by the implementation in an 
     * implementation-specific fashion. 
     *
     * @param actionInvocation The action invocation to post.
     *
     * @param handler The handler that will be notified when the action
     *      response is received. May be null, in which case the
     *      action response will be discarded.
     *
     * @throws NullPointerException if action is null.
     *
     * @see UPnPActionInvocation
     */
    public void postActionInvocation(UPnPActionInvocation actionInvocation,
            UPnPActionResponseHandler handler);

    /**
     * Adds a state variable listener to this <code>UPnPClientService</code>.
     * If this service has evented state variables,
     * this method will cause the control point to attempt to
     * subscribe to the service if it is not already subscribed.
     * See UPnP Device Architecture specification for
     * UPnP service and state variable subscription. 
     *  
     * <p>Adding a listener which is the same instance as a 
     * previously added (and not removed) listener has no effect.
     *
     * @param listener The listener to add.
     *
     * @see #setSubscribedStatus(boolean)
     */
    public void addStateVariableListener(UPnPStateVariableListener listener);

    /**
     * Removes a change listener.
     *
     * @param listener The listener to remove.
     */
    public void removeStateVariableListener(UPnPStateVariableListener listener);

    /**
     * Attempts to subscribe or unsubscribe the control point to/from
     * this service.  Changes to subscription status are signaled
     * asynchronously via the {@link UPnPStateVariableListener} interface.
     *  
     * @param subscribed True to subscribe to evented state variable
     *                   updates, false to unsubscribe.
     *
     * @throws UnsupportedOperationException if {@code subscribed} is
     * {@code true} but the service has no evented state variables.
     */
    public void setSubscribedStatus(boolean subscribed);

    /**
     * Gets the subscription status of the service.
     * Defaults to subscribed if the service has evented state variables;
     * false otherwise.
     *
     * @return True if the control point is presently registered to
     *      receive UPnP events from the service, false if not.
     */
    public boolean getSubscribedStatus();

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
    UPnPClientStateVariable getStateVariable(String stateVariableName);

    /**
     * Gets all of the UPnP state variables supported by this
     * service.  UPnP state variable information is taken from the
     * {@code stateVariable} elements in the UPnP service description.
     *
     * @return  The UPnP state variables supported by this
     * service. If the service has no state variables, returns a zero-length
     * array.
     */
    UPnPClientStateVariable[] getStateVariables();
}
