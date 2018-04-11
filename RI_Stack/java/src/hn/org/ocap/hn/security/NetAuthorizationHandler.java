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

package org.ocap.hn.security;

import java.net.InetAddress;
import java.net.URL;

/**
 * This interface represents a callback mechanism to a registered network
 * authorization handler.
 */
public interface NetAuthorizationHandler
{

    /**
     * Notifies the registered authorization handler that an
     * activity to access cable services has been started.  The handler
     * will permit or deny the ability for the activity to
     * continue.
     *
     * @param inetAddress IP address the transaction was sent from.
     * @param macAddress An empty string; this parameter is not used.
     * @param url The <code>URL</code> requested by the transaction.
     * @param activityID The unique identifier of the activity.
     *
     * @return true if the activity is accepted; false otherwise.
     */
    public boolean notifyActivityStart(InetAddress inetAddress,
                                       String macAddress,
                                       URL url,
                                       int activityID);

    /**
     * Notifies the registered authorization handler that an
     * activity has ended.
     *
     * @param activityID Unique identifier assigned to the activity and
     *      passed to the <code>notifyActivityStart</code> method.
     */
    public void notifyActivityEnd(int activityID);

    /**
     * Notifies the authorization handler that an action it
     * registered interest in has been received.
     *
     * @param actionName Name of the action received.  Will match a name
     *      in the {@code actionNames} parameter previously passed to
     * {@link NetSecurityManager#.setAuthorizationHandler(NetAuthorizationHandler, String[] boolean)}.
     * @param inetAddress IP address the transaction was sent from.
     * @param macAddress An empty string; this parameter is not used.
     * @param activityID The unique identifier of the activity if known.  If
     *      no activityID is associated with the transaction the
     *      implementation SHALL pass a value of -1;
     *
     * @return True if the activity is accepted, otherwise returns false.
     */
    public boolean notifyAction(String actionName,
                                InetAddress inetAddress,
                                String macAddress,
                                int activityID);
}
