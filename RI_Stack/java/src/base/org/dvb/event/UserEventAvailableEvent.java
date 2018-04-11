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

package org.dvb.event;

/**
 * This event is sent to the resource status event listeners when user input
 * events which had been exclusively reserved by an application are no longer
 * exclusively reserved. Where one change in user input event reservation
 * results in instances of this event being sent to several applications, the
 * following shall apply.
 * <p>
 * <ul>
 * <li>Each application shall receive its own instance of the
 * <code>UserEventRepository</code> object which forms the source to this event.
 * Any changes made to that repository by any one application shall not impact
 * the instance seen by any other application.
 * <li>Any application receiving an instance of this event is allowed to attempt
 * to exclusively reserve some of the newly available user events. In this
 * situation, the normal resource management policy of the platform as described
 * elsewhere in the present document shall be obeyed.
 * <li>Any applications which have registered for shared access to any of these
 * user events shall start receiving those events following receipt of this
 * event.
 * </ul>
 *
 * @since MHP 1.0.2
 */

public class UserEventAvailableEvent extends org.davic.resources.ResourceStatusEvent
{
    /**
     * Constructor for the event.
     *
     * @param source
     *            a <code>UserEventRepository</code> which contains the events
     *            which stopped being exclusively reserved.
     * @since MHP 1.0.2
     */
    public UserEventAvailableEvent(Object source)
    {
        super(source);
    }

    /**
     * Returns a <code>UserEventRepository</code> which contains the events
     * which were formerly exclusively reserved as passed into the constructor
     * of the instance.
     *
     * @return a <code>UserEventRepository</code>
     * @since MHP 1.0.2
     */
    public Object getSource()
    {
        return super.getSource();
    }
}
