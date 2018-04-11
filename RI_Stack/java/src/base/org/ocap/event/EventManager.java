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

import org.dvb.event.UserEventRepository;

/**
 * <p>
 * The event manager allows an application to receive events coming from the
 * user. These events can be sent exclusively to an application or can be shared
 * between applications. The EventManager allows an application to ask for
 * exclusive access to some events, these events being received either from the
 * standard java.awt event mechanism or by the mechanism defined in this
 * package. The EventManager is a singleton, and the instance is gotten from the
 * getInstance() method. (Note that a type cast is necessary to gain reference
 * to object of type org.ocap.event.EventManager.)
 * </p>
 * <p>
 * The right to receive events is considered as the same resource regardless of
 * whether it is being handled exclusively or shared. An application
 * successfully obtaining exclusive access to an event results in all other
 * applications losing access to that event, whether the access of those
 * applications was shared or exclusive.
 * </p>
 * <p>
 * If an UserEventFilter instance is set via EventManager.setUserEventFilter(),
 * EventManager shall call the UserEventFilter.filterUserEvent() method before
 * delivering events to the listening applications that are specified by the
 * UserEventFilter. Note that EventManager shall call the filterUserEvent()
 * method for only the events specified by an UserEventRepository instance which
 * is set via EventManager.setFilteredRepository(). EventFilter may modify the
 * key value of the userEvent and/or may direct the platform to forward the
 * userEvent to a specific set of applications. Then EventManager gets the
 * (possibly modified) event via UserEventAction.getEvent() and the list of
 * AppIDs of the applications to receive the forwarded event via
 * UserEventAction.getAppIDs(). EventManager shall call the
 * UserEventListener.userEventReceived() of the applications which have the
 * AppIDs specified by UserEventAction.getAppIDs(). For this purpose,
 * EventManager shall track and keep the AppID of the applications which call
 * the addExclusiveAccessToAWTEvent() and addUserEventListener() methods in a
 * proprietary manner (manufacture dependent).
 * </p>
 */
//findbugs complains about this pattern - shadowing superclass' name.
//Unfortunately, its a common pattern in the RI (so we ignore it).
public class EventManager extends org.dvb.event.EventManager
{
    /**
     * Constructor for an instance of this class. This constructor is provided
     * for the use of implementations. Applications shall not define sub classes
     * of this class. Implementations are not required to behave correctly if
     * any such application defined sub classes are used.
     */
    protected EventManager()
    {
    }

    /**
     * This method returns the sole instance of the org.ocap.event.EventManager
     * class. The EventManager instance is either a singleton for each OCAP
     * application or a singleton for a whole OCAP implementation. Note that a
     * type cast is necessary for the return value.
     * 
     * @return the instance of org.ocap.event.EventManager.
     */
    public static org.dvb.event.EventManager getInstance()
    {
        return org.dvb.event.EventManager.getInstance();
    }

    /**
     * Get the current UserEventRepository which specify the events to be
     * filtered. The monitorapplication permission is not necessary to call this
     * method. This method is used to know which events are filtered at this
     * moment. The UserEventRepository for event filtering is set via the
     * setFilteredRepository() method.
     * 
     * @return the current UserEventRepository which specifies the events to be
     *         filtered . EventManager maintains an empty UserEventRepository by
     *         default, and this is returned if setFilteredRepository() has not
     *         yet been called. If setFilteredRepository() has been called with
     *         a null UserEventRepository, then null is returned.
     */
    public UserEventRepository getFilteredRepository()
    {
        // this method should not be directly called - the EventManagerImpl
        // subclassed version should be called instead
        return null;
    }

    /**
     * Sets the repository which specifies the events to be filtered. Only one
     * UserEventRepository instance can be set at a time. Multiple calls of this
     * method will be result in an update of the UserEventReposiotry, i.e., the
     * previous UserEventRepository is discarded and the new one is set.
     * EventManager shall call the UserEventFilter.filterUserEvent() method only
     * for the events specified by the UserEventRepository. By default,
     * EventManager has an empty UserEventRepository, i.e., no
     * UserEventFilter.filterUserEvent() method is called. The
     * monitorapplication permission is necessary to call this method.
     * 
     * @param repository
     *            a set of non-ordinary key events for calling the
     *            UserEventFilter.filterUserEvent() method. If null, the
     *            UserEventFilter.filterUserEvent() method is called for all
     *            events except the mandatory ordinary key events.
     * 
     * @throws SecurityException
     *             if the caller does not have monitorapplication
     *             permission("filterUserEvents") permission.
     * 
     * @throws IllegalArgumentException
     *             if UserEventRepository contains Mandatory Ordinary keycodes.
     */
    public void setFilteredRepository(org.dvb.event.UserEventRepository repository)
    {
        // this method should not be directly called - the EventManagerImpl
        // subclassed version should be called instead
        return;
    }

    /**
     * Set the specified UserEventFilter to modify or consume the event and/or
     * change the applications to deliver the event to. Only one UserEventFilter
     * instance can be sent at a time. Multiple call of this method will result
     * in update of the UserEventFilter, i.e., the previous UserEventFilter is
     * discarded and the new one is set. By default, EventManager has no
     * UserEventFilter (null). The monitorapplication permission is necessary to
     * call this method.
     * 
     * @param filter
     *            The filter to modify or consume the event and change the
     *            application to be delivered to.
     * 
     * @throws SecurityException
     *             if the caller does not have monitorapplication
     *             permission("filterUserEvents") permission.
     */
    public void setUserEventFilter(UserEventFilter filter)
    {
        // this method should not be directly called - the EventManagerImpl
        // subclassed version should be called instead
        return;
    }
}
