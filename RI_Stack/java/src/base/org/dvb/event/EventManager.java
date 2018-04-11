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

import org.cablelabs.impl.manager.ManagerManager;

/**
 * The event manager allows an application to receive events coming from the
 * user. These events can be sent exclusively to an application or can be shared
 * between applications. The Event Manager allows also the application to ask
 * for exclusive access to some events, these events being received either from
 * the standard java.awt event mechanism or by the mechanism defined in this
 * package. The EventManager is either a singleton for each MHP application or a
 * singleton for the MHP terminal.
 * <p>
 * The right to receive events is considered as the same resource regardless of
 * whether it is being handled exclusively or shared. An application
 * successfully obtaining exclusive access to an event results in all other
 * applications losing access to that event, whether the access of those
 * applications was shared or exclusive.
 */
public class EventManager implements org.davic.resources.ResourceServer
{

    /**
     * Constructor for instances of this class. This constructor is provided for
     * the use of implementations and specifications which extend this
     * specification. Applications shall not define sub-classes of this class.
     * Implementations are not required to behave correctly if any such
     * application defined sub-classes are used.
     */
    protected EventManager()
    {
    }

    /**
     * This method returns the sole instance of the EventManager class. The
     * EventManager class is a singleton.
     *
     * @return the instance of the EventManager.
     */
    public static EventManager getInstance()
    {
        org.cablelabs.impl.manager.EventManager em = (org.cablelabs.impl.manager.EventManager) ManagerManager.getInstance(org.cablelabs.impl.manager.EventManager.class);

        return em.getEventManager();
    }

    /**
     * Adds the specified listener to receive events coming from the user in an
     * exclusive manner. The events the application wishes to receive are
     * defined by the means of the UserEventRepository class. This repository is
     * resolved at the time when this method call is made and adding or removing
     * events from the repository after this method call does not affect the
     * subscription to those events. The ResourceClient parameter indicates that
     * the application wants to have an exclusive access to the user event
     * defined in the repository.
     * <p>
     * The effect of multiple calls to this method by the same application with
     * different instances of UserEventRepository shall be cumulative. If
     * multiple calls to this method succeed in acquiring the events in the
     * specified repositories then the semantics of each successful method call
     * shall be obeyed as specified. Note that this can result in applications
     * receiving the same event through more than one event listener.
     *
     * @param listener
     *            the listener to receive the user events.
     * @param client
     *            resource client.
     * @param userEvents
     *            a class which contains the user events it wants to be informed
     *            of.
     *
     * @return true if the events defined in the repository have been acquired,
     *         false otherwise.
     *
     * @exception IllegalArgumentException
     *                if the client argument is set to null.
     */
    public boolean addUserEventListener(UserEventListener listener, org.davic.resources.ResourceClient client,
            UserEventRepository userEvents)
    {
        // this method should not be directly called - the EventManagerImpl
        // subclassed version should be called instead
        return false;
    }

    /**
     * Adds the specified listener to receive events coming from the user. The
     * events the application wishes to receive are defined by the means of the
     * UserEventRepository class. This repository is resolved at the time when
     * this method call is made and adding or removing events from the
     * repository after this method call does not affect the subscription to
     * those events.
     * <p>
     * The effect of multiple calls to this method by the same application with
     * different instances of UserEventRepository shall be cumulative. If
     * multiple calls to this method succeed in acquiring the events in the
     * specified repositories then the semantics of each successful method call
     * shall be obeyed as specified. Note that this can result in applications
     * receiving the same event through more than one event listener.
     *
     * @param listener
     *            the listener to receive the user events.
     * @param userEvents
     *            a class which contains the user events it wants to be informed
     *            of.
     */
    public void addUserEventListener(UserEventListener listener, UserEventRepository userEvents)
    {
        // this method should not be directly called - the EventManagerImpl
        // subclassed version should be called instead
        return;
    }

    /**
     * An application should use this method to express its intend to have
     * exclusive access to some events, but for these events to be received
     * through the java.awt mechanism. The events the application wishes to
     * receive are defined by the means of the UserEventRepository class. This
     * repository is resolved at the time when this method call is made and
     * adding or removing events from the repository after this method call
     * does not affect the subscription to those events. An exclusive event will
     * be sent to the application if this latest is focused.
     * <p>
     * The effect of multiple calls to this method by the same application with
     * different instances of UserEventRepository shall be cumulative. If
     * multiple calls to this method succeed in acquiring the events in the
     * specified repositories then the semantics of each successful method call
     * shall be obeyed as specified.
     *
     * @param client
     *            resource client.
     * @param userEvents
     *            the user events the application wants to be inform of.
     *
     * @return true if the events defined in the repository have been acquired,
     *         false otherwise.
     *
     * @exception IllegalArgumentException
     *                if the client argument is set to null.
     */
    public boolean addExclusiveAccessToAWTEvent(org.davic.resources.ResourceClient client,
            UserEventRepository userEvents)
    {
        // this method should not be directly called - the EventManagerImpl
        // subclassed version should be called instead
        return false;
    }

    /**
     * Removes the specified listener so that it will no longer receives user
     * events. If it is appropriate (i.e the application has asked for an
     * exclusive access), the exclusive access is lost.
     *
     * @param listener
     *            the user event listener.
     */
    public void removeUserEventListener(UserEventListener listener)
    {
        // this method should not be directly called - the EventManagerImpl
        // subclassed version should be called instead
        return;
    }

    /**
     * The application should use this method to release its exclusive access to
     * user events defined by the means of the addExclusiveAccessToAWTEvent
     * method.
     *
     * @param client
     *            the client that is no longer interested in events previously
     *            registered.
     */
    public void removeExclusiveAccessToAWTEvent(org.davic.resources.ResourceClient client)
    {
        // this method should not be directly called - the EventManagerImpl
        // subclassed version should be called instead
        return;
    }

    /**
     * Adds the specified resource status listener so that an application can be
     * aware of any changes regarding exclusive access to some events.
     *
     * @param listener
     *            the resource status listener.
     */
    public void addResourceStatusEventListener(org.davic.resources.ResourceStatusListener listener)
    {
        // this method should not be directly called - the EventManagerImpl
        // subclassed version should be called instead
        return;
    }

    /**
     * Removes the specified resource status listener.
     *
     * @param listener
     *            the listener to remove.
     */
    public void removeResourceStatusEventListener(org.davic.resources.ResourceStatusListener listener)
    {
        // this method should not be directly called - the EventManagerImpl
        // subclassed version should be called instead
        return;
    }
}
