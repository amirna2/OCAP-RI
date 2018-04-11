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

package org.ocap.event;

import org.dvb.application.*;

/**
 * UserEventAction is returned by the UserEventFilter.filterUserEvent() method
 * in order to inform the EventManager the value of the event and to which
 * applications the event shall be forwarded. See the
 * org.ocap.event.UserEventFilter.filterUserEvent() method for further details.
 * UserEventAction has separate methods to provide the list of AppIDs and the
 * modified UserEvent instance. The modified UserEvent instance will be
 * forwarded to the applications specified by AppIDs by EventManager. If the
 * list of AppIDs is null, the EventManager shall forward the event to all
 * registered UserEventListeners. If the list of AppIDs is not null, the
 * EventManager shall forward the event to the registered UserEventListeners
 * that match the AppIDs in the list. Note that if
 * UserEventFilter.filterUserEvent() returns null, the event is not sent to any
 * applications.
 * 
 * @author Aaron Kamienski
 */
public class UserEventAction
{
    /**
     * Creates a UserEventAction instance.
     * <p>
     * 
     * The event passed to this constructor SHOULD NOT be an application-defined
     * subclass of UserEvent. If it is an application-defined subclass, then
     * when the platform dispatches the event the platform MUST extract the
     * parameters of the event (e.g., source, type, code etc.) and construct a
     * new instance of the UserEvent class with those parameters. I.e., the
     * EventManager MUST NOT deliver the application-defined subclass. (NOTE:
     * This translation is done by the platform, NOT by this class).
     * 
     * @param event
     *            The event to forward, or null for none.
     * @param appIDs
     *            The AppIDs to which the filtered event will be forwarded, or
     *            null for default handling.
     */
    public UserEventAction(UserEvent event, AppID[] appIDs)
    {
        this.event = event;
        this.appIDs = appIDs;
    }

    /**
     * Sets the event returned by this class.
     * <p>
     * 
     * The event passed to this function SHOULD NOT be an application-defined
     * subclass of UserEvent. If it is an application-defined subclass, then
     * when the platform dispatches the event the platform MUST extract the data
     * and construct a real UserEvent instance. (NOTE: This translation is done
     * by the platform, NOT by this class.).
     * 
     * @param event
     *            The event to forward, or null for none.
     */
    public void setEvent(UserEvent event)
    {
        this.event = event;
    }

    /**
     * Sets the application IDs returned by this class.
     * 
     * @param appIDs
     *            The AppIDs to which the filtered event will be forwarded, or
     *            null for default handling.
     */
    public void setAppIDs(AppID[] appIDs)
    {
        this.appIDs = appIDs;
    }

    /**
     * Get the event to be forwarded. The event may be modified while filtering.
     * EventManager shall forward this modified event instead of the original
     * user input event.
     * 
     *@return The event to be forwarded. If null, no event is forwarded to any
     *         application.
     */
    public UserEvent getEvent()
    {
        return event;
    }

    /**
     *Get the AppIDs to which the filtered event will be forwarded.
     * 
     *@return The AppIDs to which the filtered event will be forwarded. If
     *         null, the EventManager shall forward the event to all registered
     *         UserEventListeners.
     */
    public org.dvb.application.AppID[] getAppIDs()
    {
        return appIDs;
    }

    /**
     * The event to be forwarded.
     */
    private UserEvent event;

    /**
     * The AppIDs that events are to be forwarded to.
     */
    private AppID[] appIDs;
}
