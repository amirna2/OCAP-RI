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

package org.ocap.hn.upnp.server;

/**
 * This interface represents a handler for the evented UPnP state
 * variables on a service.  The hander is called as a result of
 * incoming subscription and query actions, and supplies the state
 * variable values to be evented.
 */
public interface UPnPStateVariableHandler
{

    /**
     * Notifies the listener that a control point has requested the
     * value of a state variable through the {@code QueryStateVariable} action.
     * The handler must return the current value of the
     * requested state variable.
     *
     * @param variable The UPnP state variable that was queried.
     *
     * @return The current value of the state variable.
     */
    public String getValue(UPnPManagedStateVariable variable);

    /**
     * Notifies the listener that a control point has subscribed to 
     * state variable eventing on the specified service.
     * This method is called subsequent to the transmission of
     * subscription response message,
     * but prior to the transmission of the initial event message.
     * The eventing process blocks until this method returns,
     * permitting the handler to set the initial values of the service's
     * state variables as desired.
     *
     * @param service The UPnP service that was subscribed to.
     */
    public void notifySubscribed(UPnPManagedService service);

    /**
     * Notifies the listener that a control point has successfully
     * unsubscribed from state variable eventing on the specified service,
     * or that a prior subscription has expired.
     * This method is called subsequent to the transmission of the
     * unsubscription response message.
     *
     * @param service The UPnP service that was unsubscribed from.
     *  
     * @param remainingSubs The number of remaining active 
     *                      subscriptions to this service.
     */
    public void notifyUnsubscribed(UPnPManagedService service,
                                    int remainingSubs);
}
