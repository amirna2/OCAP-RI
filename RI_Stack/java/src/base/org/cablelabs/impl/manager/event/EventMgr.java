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

package org.cablelabs.impl.manager.event;

import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FocusManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.CallerContext.Multicaster;
import org.cablelabs.impl.manager.FocusManager.FocusContext;
import org.cablelabs.impl.manager.FocusManager.DispatchFilter;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;

import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.dvb.application.AppID;
import org.dvb.event.UserEventAvailableEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.dvb.event.UserEventUnavailableEvent;
import org.havi.ui.event.HRcEvent;
import org.ocap.event.EventManager;
import org.ocap.event.UserEvent;
import org.ocap.event.UserEventAction;
import org.ocap.event.UserEventFilter;
import org.ocap.system.MonitorAppPermission;
import org.ocap.ui.event.OCRcEvent;

/**
 * Implements the {@link org.cablelabs.impl.manager.EventManager} interface,
 * providing a clearing house for event-related activities. This includes
 * providinC the OCAP {@link org.ocap.event.EventManager} implementation.
 * 
 * 
 * @author Aaron Kamienski
 */
public class EventMgr implements org.cablelabs.impl.manager.EventManager
{
    /**
     * Protected constructor to allow subclassing. Instances should be
     * created via {@link #getInstance}.
     */
    protected EventMgr()
    { /* EMPTY */
    }

    /**
     * Returns the singleton instance of the <code>EventManager</code>. Intended
     * to be called by the {@link org.cablelabs.impl.manager.ManagerManager
     * ManagerManager} only and not called directly.
     * 
     * @return the singleton instance of the <code>EventManager</code>.
     * @see org.cablelabs.impl.manager.ManagerManager#getInstance(Class)
     */
    public static Manager getInstance()
    {
        return new EventMgr();
    }

    /**
     * Returns an instance of {@link EventManagerImpl}. A singleton instance is
     * returned for each {@link org.cablelabs.impl.manager.CalerContext}.
     * 
     * @return an instance of {@link EventManagerImpl}.
     */
    public EventManager getEventManager()
    {
        CallerContext ctx = getContext();
        return getData(ctx).eventMgr;
    }

    /**
     * Dispatches the given <code>AWTEvent</code> according to the decision tree
     * shown in Annex K of the OCAP-1.0 specification. This handles sending the
     * event to the <i>MonitorApp</i> for filtering, exclusive access dispatch
     * of AWT events, focused application dispatch of AWT events, and dispatch
     * of {@link org.dvb.event.UserEvent}s.
     * <p>
     * This method should do the following:
     * <ol>
     * <li>Send event to registered {@link org.ocap.event.UserEventFilter
     * filter}.
     * <li>If an application has registered exclusive access to this event:
     * <ol>
     * <li>If access was registered through
     * {@link org.dvb.event.EventManager#addExclusiveAccessToAWTEvent}, then the
     * event is delivered to the currently focused component (if the app is
     * currently focused).
     * <li>Otherwise the event is dispatched via the registered
     * {@link org.dvb.event.UserEventListener}.
     * </ol>
     * <li>If no application has registered exclusive access then:
     * <ol>
     * <li>The event is sent to the currently focused application component via
     * the application's event queue.
     * <li>The event is sent to applications registered through the
     * EventRepository for shared access.
     * </ol>
     * </ol>
     * <p>
     * 
     * @see "OCAP-1.0 Annex K - OCAP User Event Input API"
     * @see org.dvb.event
     * @see org.ocap.event
     */
    // Description copied from EventDispatcher interface
    public void dispatch(AWTEvent e)
    {
        if (e instanceof KeyEvent)
        {
            KeyEvent ke = (KeyEvent) e;

            // If necessary translate KeyEvent.CHAR_UNDEFINED to OCAP-specific
            // value.
            if (TRANSLATE_CHAR_UNDEFINED && ke.getKeyChar() == KeyEvent.CHAR_UNDEFINED)
            {
                ke.setKeyChar(CHAR_UNDEFINED);
            }

            DispatchContext context = newDispatchContext(ke);

            // Handles the entire process
            context.dispatch((KeyEvent) e);
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("Event type not handled " + e);
            }
    }
    }

    // Description copied from Manager interface
    public void destroy()
    {
        // done!
        // forget listeners!
    }

    /*----------- EventManager implementation code --------------*/

    /**
     * Adds the specified listener to receive events coming from the user.
     * 
     * @param listener
     *            the listener to receive the user events.
     * @param userEvents
     *            a class which contains the user events it wants to be informed
     *            of.
     * 
     * @see org.dvb.event.EventManager#addUserEventListener(UserEventListener,
     *      UserEventRepository)
     */
    public synchronized void addUserEventListener(UserEventListener listener, UserEventRepository userEvents)
    {
        org.dvb.event.UserEvent[] events = userEvents.getUserEvent();

        // Add listener for each event
        CallerContext ctx = getContext();
        Data data = getData(ctx);
        for (int i = 0; i < events.length; ++i)
        {
            Key key = Key.createKey(events[i]);

            // Update listener/multicaster
            UserEventListener ueListeners = (UserEventListener) data.ueListeners.get(key);
            ueListeners = EventMulticaster.add(ueListeners, listener);
            data.ueListeners.put(key, ueListeners);

            // Update CallerContext/multicaster
            CallerContext ccList = (CallerContext) ueContexts.get(key);
            ccList = Multicaster.add(ccList, ctx);
            ueContexts.put(key, ccList);
        }
    }

    /**
     * Adds the specified listener to receive events coming from the user in an
     * exclusive manner.
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
     * 
     * @see org.dvb.event.EventManager#addUserEventListener(UserEventListener,ResourceClient,UserEventRepository)
     */
    public boolean addUserEventListener(UserEventListener listener, ResourceClient client,
            UserEventRepository userEvents)
    {
        if (listener == null || client == null || userEvents == null)
            throw new IllegalArgumentException("Null argument");

        org.dvb.event.UserEvent[] events = userEvents.getUserEvent();
        // Sure, you can reserve no events ;-)
        if (events.length == 0) return true;

        try
        {
            Owner newOwner = new Owner(client, userEvents.getName(), events, true);
            ListenerReceiver receiver = new ListenerReceiver(listener);
            ExclusiveDispatcher dispatcher = new UserEventDispatcher(newOwner, receiver);

            return reserveEvents(newOwner, dispatcher, receiver);
        }
        catch (UnsupportedKeys e)
        {
            return false;
        }
    }

    /**
     * An application should use this method to express its intent to have
     * exclusive access to some events but for these events to be received
     * through the java.awt mechanism.
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
     * 
     * @see org.dvb.event.EventManager#addExclusiveAccessToAWTEvent(ResourceClient,
     *      UserEventRepository)
     */
    public boolean addExclusiveAccessToAWTEvent(ResourceClient client, UserEventRepository userEvents)
    {
        if (client == null || userEvents == null) throw new IllegalArgumentException("null parameter");

        org.dvb.event.UserEvent[] events = userEvents.getUserEvent();
        // Sure, you can reserve no events ;-)
        if (events.length == 0) return true;

        try
        {
            Owner newOwner = new Owner(client, userEvents.getName(), events, false);
            AWTReceiver receiver = new AWTReceiver(client);
            ExclusiveDispatcher dispatcher = new AWTDispatcher(newOwner, receiver);

            return reserveEvents(newOwner, dispatcher, receiver);
        }
        catch (UnsupportedKeys e)
        {
            return false;
        }
    }

    /**
     * Common method used for reserving either <code>UserEvent</code>s or
     * <code>AWTEvent</code>s for exclusive access.
     * <p>
     * This method does the following:
     * <ol>
     * <li>Attempt to {@link #tryReserveEvents take} reservations for all
     * desired events.<br>
     * If successful, then <code>true</code> is returned. <br>
     * If any events are already reserved, this operation fails and we
     * continue...
     * <li>Attempt to get current owner's to {@link #tryReleaseEvents release}
     * reservations. <br>
     * If unsuccessful, then <code>false</code> is returned. <br>
     * If potentially successful continue...
     * <li>Repeat (until reservation is successful or explicitly denied).
     * </ol>
     * 
     * Note that this operation is not synchronized in its entirety. Only
     * individual steps are synchronized (specifically those that access or
     * modify current {@link #exclusiveDispatch reservation information}).
     * 
     * @param newOwner
     *            representation of the ownership request (may be identical to
     *            existing owner)
     * @param dispatcher
     *            representation of the method for dispatch
     * @param listener
     *            representation of the listener being added
     * 
     * @return <code>true</code> if reservation is successful;
     *         <code>false</code> otherwise
     * 
     * @see #addUserEventListener(UserEventListener, ResourceClient,
     *      UserEventRepository)
     * @see #addExclusiveAccessToAWTEvent(ResourceClient, UserEventRepository)
     */
    private boolean reserveEvents(Owner newOwner, ExclusiveDispatcher dispatcher, ExclusiveReceiver listener)
    {
        // Loop until reservation is succesful or outright denied
        while (true)
        {
            // Attempt to reserve the events
            Set owners = tryReserveEvents(newOwner, dispatcher, listener);

            // If there is no previous owners, then return true
            if (owners == null) return true; // successful reservation!

            if (!tryReleaseEvents(owners, newOwner)) return false; // reservation
                                                                   // outright
                                                                   // denied!
            // Else try again until absolutely denied
        }
    }

    /**
     * Attempt to reserve the events requested by the given
     * <code>ExclusiveDispatcher</code> and <code>ExclusiveReceiver</code>
     * handler. Events can only be reserved if there is no current owner for all
     * of the events (including <i>corresponding</i> events). If events cannot
     * be reserved, then the current set of owner
     * <code>ExclusiveDispatcher</code>s is returned.
     * 
     * @param newOwner
     *            the owner trying to reserve the given events
     * @param dispatcher
     *            the method of dispatching events
     * @param listener
     *            the reciever of events to be remembered (if successful) for
     *            later removal via {@link Data#ownedKeys}; also may be added to
     *            existing <i>dispatcher</i> if has equivalent owner
     * 
     * @return <code>null</code> if reservation is successful
     */
    private synchronized Set tryReserveEvents(Owner newOwner, ExclusiveDispatcher dispatcher, ExclusiveReceiver listener)
    {
        // Get the current set of Owners
        Set owners = getOwners(newOwner);
        boolean unOwned = owners.isEmpty();

        // Reserve if not owned by anybody
        // Or is owned by equivalent to newOwner
        if (unOwned || (owners.size() == 1 && owners.contains(newOwner)))
        {
            Data data = getData(newOwner.getCallerContext());

            // Reserve requested events for dispatch
            markEventsReserved(newOwner.getKeys(), dispatcher, listener, unOwned);

            // Reserve additional events (but not for dispatch)
            markEventsReserved(newOwner.getAdditionalKeys(), new NullDispatcher(dispatcher), listener, unOwned);

            // Remember listener->key mappings (for later removal)
            Set keys = newOwner.getAllKeys();
            HashSet receiver2Keys = (HashSet) data.ownedKeys.get(listener);
            if (receiver2Keys != null) // listener's been added once before
                receiver2Keys.addAll(keys);
            else
                // first time adding this listener
                data.ownedKeys.put(listener, keys);

            // Notify ResourceStatusListeners
            notifyUnavailable(newOwner.getUserEventRepository());

            // Return successful reservation
            return null;
        }

        // Return unsuccessful reservation
        return owners;
    }

    /**
     * Marks the requested keys as reserved.
     * <p>
     * Updates {@link #exclusiveDispatch} with new ({@link Key},
     * {@link ExclusiveDispatcher}) pairs.
     * 
     * @param requestedKeys
     *            the keys being requested for reservation
     * @param dispatcher
     *            the dispatcher to be used to dispatch events
     * @param listener
     *            proxy for the event receiver (e.g.,
     *            <code>UserEventListener</code>)
     * @param unOwned
     *            if <code>true</code> then can just set new dispatcher; if
     *            <code>false</code> then update existing dispatcher
     */
    private synchronized void markEventsReserved(Set requestedKeys, ExclusiveDispatcher dispatcher,
            ExclusiveReceiver listener, boolean unOwned)
    {
        // Foreach key, update exclusiveDispatch
        for (Iterator i = requestedKeys.iterator(); i.hasNext();)
        {
            Key key = (Key) i.next();

            if (unOwned)
            {
                // Mark reservation
                exclusiveDispatch.put(key, dispatcher);
            }
            else
            {
                // Already reserved, add to existing dispatcher
                ExclusiveDispatcher currDispatcher = (ExclusiveDispatcher) exclusiveDispatch.get(key);
                // Add the listener for dispatch
                currDispatcher.add(listener);
            }
        }
    }

    /**
     * Requests that the given set of <code>Owner</code>s give up their events.
     * Returns <code>false</code> if the events have <i>not</i> been released.
     * Returns <code>true</code> if the events have been released.
     * <p>
     * Results should be double-checked (by calling {@link #getOwners} again) in
     * case the reservation status of the desired keys has changed since it was
     * called last.
     * <p>
     * Also may return <code>true</code> if the events have not been explicitly
     * released, but may have been
     * <p>
     * Note that this method is not synchronized in its entirety.
     * Synchronization is only done when changing reservations.
     * 
     * @param owners
     *            the set of <code>Owner</code>s which should give up their
     *            events
     * @param newOwner
     *            the caller requesting the events be released
     * 
     * @return <code>false</code> if the events were not given up;
     *         <code>true</code> if the events <i>might</i> have been given up
     */
    private boolean tryReleaseEvents(Set owners, Owner newOwner)
    {
        // Priority for forced release
        int priority = newOwner.getPriority();

        // There are current owners, then request that they release.
        for (Iterator iter = owners.iterator(); iter.hasNext();)
        {
            Owner owner = (Owner) iter.next();

            Set keys = owner.requestRelease();
            // If requestRelease() is denied, then resolve contention using
            // priorities
            if (keys == null) keys = owner.forceRelease(priority);
            if (keys == null) return false;

            // Implicitly remove the reservation
            synchronized (this)
            {
                // If still owner of keys, then clear ownership/dispatch
                if (keys.size() != 0)
                {
                    boolean available = false;

                    // Remove ownership
                    Key[] ownedKeys = (Key[]) keys.toArray(new Key[keys.size()]);
                    for (int i = 0; i < ownedKeys.length; ++i)
                    {
                        ExclusiveDispatcher disp = (ExclusiveDispatcher) exclusiveDispatch.get(ownedKeys[i]);
                        if (disp != null && disp.getOwner() == owner)
                        {
                            available = true;
                            exclusiveDispatch.remove(ownedKeys[i]);
                        }
                    }
                    // Forget keys tracked by listeners
                    // TODO: clean this up!
                    Data data = getData(owner.cc);
                    // Iterate over all receivers associated with these owned
                    // keys
                    for (Iterator iter2 = owner.receivers.iterator(); iter2.hasNext();)
                    {
                        // Locate all keys reserved by this listener
                        ExclusiveReceiver receiver = (ExclusiveReceiver) iter2.next();
                        Set set = (Set) data.ownedKeys.get(receiver);
                        if (set != null)
                        {
                            // Remove the set of keys currently being released
                            set.removeAll(keys);
                            // If no keys remain, forget the reciever
                            if (set.size() == 0) data.ownedKeys.remove(receiver);
                        }
                    }

                    // Notify ResourceStatusListeners
                    if (available) notifyAvailable(owner.getUserEventRepository());
                }
            }
        }
        return true;
    }

    /**
     * Removes the specified listener so that it will no longer receives user
     * events. If it is appropriate (i.e the application has asked for an
     * exclusive access), the exclusive access is lost.
     * 
     * @param listener
     *            the user event listener.
     */
    public synchronized void removeUserEventListener(UserEventListener listener)
    {
        if (listener == null) return;

        CallerContext ctx = getContext();
        Data data = (Data) ctx.getCallbackData(this);

        // Remove the given listener from the set of (shared) listeners
        if (data != null && data.ueListeners != null)
        {
            for (Enumeration e = data.ueListeners.keys(); e.hasMoreElements();)
            {
                Object key = e.nextElement();
                UserEventListener ueListeners = (UserEventListener) data.ueListeners.get(key);
                ueListeners = EventMulticaster.remove(ueListeners, listener);
                data.ueListeners.put(key, ueListeners);
            }
        }

        // Release exclusive UserEvents
        releaseEvents(ctx, new ListenerReceiver(listener));
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
    public synchronized void removeExclusiveAccessToAWTEvent(ResourceClient client)
    {
        if (client == null) return;

        // Release AWTEvents
        releaseEvents(getContext(), new AWTReceiver(client));
    }

    /**
     * Release the events currently owned by the given
     * <code>ExclusiveReceiver</code> within the given
     * <code>CallerContext</code>.
     * 
     * @param ctx
     *            caller context releasing ownership
     * @param receiver
     *            wrapper around <code>ResourceClient</code> or
     *            <code>UserEventListener</code>
     */
    private synchronized void releaseEvents(CallerContext ctx, ExclusiveReceiver receiver)
    {
        Data data = getData(ctx);

        // Forget this OwnerKey and the keys it owns
        HashSet keys = (HashSet) data.ownedKeys.remove(receiver);
        if (keys == null) return;

        Set owners = new HashSet();

        // Foreach key: remove handler
        for (Iterator i = keys.iterator(); i.hasNext();)
        {
            Key key = (Key) i.next();

            ExclusiveDispatcher dispatch = (ExclusiveDispatcher) exclusiveDispatch.get(key);
            if (dispatch == null) // I doubt think we should get this far...
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Implicit release may not have cleaned up data.ownedKeys");
                }
                continue; // if implicit release doesn't cleanup
                          // data.ownedKeys... we might see this...
            }
            // Remove listener
            if (dispatch.remove(receiver))
            {
                // Dispatcher is empty, remove it.
                exclusiveDispatch.remove(key);

                owners.add(dispatch.getOwner());
            }
        }

        // Notify ResourceStatusListeners
        for (Iterator i = owners.iterator(); i.hasNext();)
        {
            Owner owner = (Owner) i.next();
            notifyAvailable(owner.getUserEventRepository());
        }
    }

    /**
     * Adds the specified resource status listener so that an application can be
     * aware of any changes regarding exclusive access to some events.
     * 
     * @param listener
     *            the resource status listener.
     */
    public synchronized void addResourceStatusEventListener(ResourceStatusListener listener)
    {
        // Listeners are maintained in-context
        CallerContext ctx = getContext();
        Data data = getData(ctx);

        // Update listener/multicaster
        data.rseListeners = EventMulticaster.add(data.rseListeners, listener);

        // Manage context/multicaster
        rseContexts = Multicaster.add(rseContexts, ctx);
    }

    /**
     * Removes the specified resource status listener.
     * 
     * @param listener
     *            the listener to remove.
     */
    public synchronized void removeResourceStatusEventListener(ResourceStatusListener listener)
    {
        // Listeners are maintained in-context
        CallerContext ctx = getContext();
        Data data = (Data) ctx.getCallbackData(this);

        // Remove the given listener from the set of listeners
        if (data != null && data.rseListeners != null)
        {
            data.rseListeners = EventMulticaster.remove(data.rseListeners, listener);
        }
    }

    /**
     * Notifies all registered <code>ResourceStatusListener</code>s that the
     * given set of <code>UserEvent</code>s is available for exclusive
     * reservation.
     * 
     * @param uer
     *            the <code>UserEventRepository</code> representing the user
     *            events that are now available for exclusive reservation
     */
    private void notifyAvailable(UserEventRepository uer)
    {
        notifyResourceStatus(new UserEventAvailableEvent(uer));
    }

    /**
     * Notifies all registered <code>ResourceStatusListener</code>s that the
     * given set of <code>UserEvent</code>s has been made unavailable due to
     * exclusive reservation.
     * 
     * @param uer
     *            the <code>UserEventRepository</code> representing the user
     *            events that have been made unavailable
     */
    private void notifyUnavailable(UserEventRepository uer)
    {
        notifyResourceStatus(new UserEventUnavailableEvent(uer));
    }

    /**
     * Notifies all registered <code>ResourceStatusListener</code>s of the given
     * <code>ResourceStatusEvent</code>.
     * 
     * @param e
     *            the event to deliver to all installed
     *            <code>ResourceStatusListener</code>s
     */
    private void notifyResourceStatus(final ResourceStatusEvent e)
    {
        CallerContext ccList = rseContexts;
        if (ccList == null) return;
        ccList.runInContext(new Runnable()
        {
            public void run()
            {
                CallerContext ctx = getContext();
                Data data = getData(ctx);

                if (data == null) return;
                ResourceStatusListener rsl = data.rseListeners;
                if (rsl == null) return;
                rsl.statusChanged(e);
            }
        });
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
     * monitorapplicaiton permission is necessary to call this method.
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
    public void setFilteredRepository(UserEventRepository repository) throws SecurityException
    {
        checkPermission(monitor_filter);

        // Check that keys are available for filtering
        String name;
        Set keyset;
        if (repository == null)
        {
            name = null;
            keyset = null;
        }
        else
        {
            name = repository.getName();
            keyset = createMinimumKeySet(repository);
        }

        // Remember filtered repository
        synchronized (this)
        {
            filteredRepository = name;
            filteredKeySet = keyset;

            repositoryContext = getContext();
        }
    }

    /**
     * Get the current UserEventRepository which specify the events to be
     * filtered. The monitorapplicaion permission is not necessary to call this
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
    public synchronized UserEventRepository getFilteredRepository()
    {
        // Synchronized ensure atomicity with setFilteredRepository.
        if (filteredRepository == null && filteredKeySet == null) return null;

        return createRepository(filteredKeySet, filteredRepository, null);
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
     *             if the caller does not have
     *             MonitorAppPermission("filterUserEvents") permission.
     */
    public synchronized void setUserEventFilter(UserEventFilter filter) throws SecurityException
    {
        checkPermission(monitor_filter);

        eventFilter = filter;
        filterContext = getContext();
    }

    /*------------------------------------------------------------------------*/

    /**
     * Creates a new <code>Set</code> of <code>Key</code>s from the given
     * <code>UserEventRepository</code>.
     * 
     * @param events
     *            the set of events acquire from the UserEventRepository
     * @return unsynchronized <code>Set</code> of <code>Key</code>s
     */
    private static Set createKeySet(org.dvb.event.UserEvent[] events)
    {
        if (events.length == 0) return Collections.EMPTY_SET;

        HashSet set = new HashSet();
        for (int i = 0; i < events.length; ++i)
        {
            set.add(Key.createKey(events[i]));
        }
        return set;
    }

    /**
     * Create and return a <code>Set</code> of <i>corresponding</i>
     * <code>Key</code>s for the given <code>Set</code> of keys.
     * 
     * @param events
     *            the set of <code>Key</code>s for which <i>corresponding</i>
     *            keys are desired
     * @return a <code>Set</code> of <i>corresponding</i> <code>Key</code>s
     * 
     * @throws UnsupportedKeys
     *             if cannot determine whether corresponding keys exist or not
     *             for the given set
     */
    private static Set createCorrespondingKeySet(Set events) throws UnsupportedKeys
    {
        if (events.size() == 0) return Collections.EMPTY_SET;

        HashSet set = new HashSet();
        for (Iterator i = events.iterator(); i.hasNext();)
        {
            Key key = (Key) i.next();
            Key key2, key3;

            switch (key.type)
            {
                case KeyEvent.KEY_RELEASED:
                    // if KEY_RELEASED, may add KEY_PRESSED (if not present)
                    key2 = Key.createKey(key, KeyEvent.KEY_PRESSED);
                    if (!events.contains(key2)) set.add(key2);
                    break;
                case KeyEvent.KEY_PRESSED:
                    // if KEY_PRESSED, may add KEY_RELEASED (if not present)
                    key2 = Key.createKey(key, KeyEvent.KEY_RELEASED);
                    if (!events.contains(key2)) set.add(key2);
                    break;
                case KeyEvent.KEY_TYPED:
                    // if KEY_TYPED, may add multiple KEY_PRESSED/KEY_RELEASED
                    if (correspondingKeys != null)
                    {
                        List keys = (List) correspondingKeys.get(new Character(key.ch));
                        if (keys != null)
                            set.addAll(keys);
                        else
                            throw new UnsupportedKeys();
                    }
                    else
                    {
                        char ch = Character.toUpperCase(key.ch);
                        switch (ch)
                        {
                            // TODO: expand this
                            // Basically keys where ASCII == VK_*
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\b':
                            case ',':
                            case '.':
                            case ';':
                            case '/':
                            case '-':
                            case '=':
                            case '[':
                            case ']':
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                            case 'G':
                            case 'H':
                            case 'I':
                            case 'J':
                            case 'K':
                            case 'L':
                            case 'M':
                            case 'N':
                            case 'O':
                            case 'P':
                            case 'Q':
                            case 'R':
                            case 'S':
                            case 'T':
                            case 'U':
                            case 'V':
                            case 'W':
                            case 'X':
                            case 'Y':
                            case 'Z':
                                key2 = Key.createKey(ch, KeyEvent.KEY_PRESSED, CHAR_UNDEFINED);
                                key3 = Key.createKey(ch, KeyEvent.KEY_RELEASED, CHAR_UNDEFINED);
                                if (!events.contains(key2) && !events.contains(key3))
                                {
                                    set.add(key2);
                                    set.add(key3);
                                }
                                break;
                            default:
                                // Simply because we don't know (or we claim not
                                // to)
                                throw new UnsupportedKeys();
                        }
                    }
                    break;
                default:
                    throw new UnsupportedKeys();
            }
        }
        return set;
    }

    /**
     * Used to load a completely custom set of <i>corresponding</i> key codes.
     * This method looks for a resource file located in the current package or
     * in the default package named <code>"keygen.properties"</code>. This
     * should be a properties file containing
     * <code><i>key</i>=<i>value</i></code> pairs where keys indicate the key
     * character and value includes the virtual key codes.
     * <p>
     * The key char can be expressed as a single character, an escaped
     * character, or a unicode character. The virtual key codes is a list of
     * <code>VK_*</code> symbols defined by {@link KeyEvent}, {@link HRcEvent},
     * or {@link OCRcEvent}. For example:
     * 
     * <pre>
     * *=VK_8 VK_NUMPAD8 VK_MULTIPLY
     * A=VK_A
     * a=VK_A
     * \n=VK_ENTER
     * \s=VK_SPACE
     * .=VK_PERIOD
     * >=VK_PERIOD VK_GREATER
     * ,=VK_COMMA
     * <=VK_COMMA VK_LESS
     * </pre>
     * 
     * @return a <code>Map</code> of {@link Character}s to {@link List}s of
     *         {@link Key}s; or <code>null</code> if no custom definitions are
     *         provided
     */
    private static Map loadCorrespondingKeys()
    {
        try
        {
            Class clazz = EventMgr.class;
            InputStream is = clazz.getResourceAsStream("/keygen.properties");
            if (is == null) is = clazz.getResourceAsStream("keygen.properties");
            if (is == null) return null;

            Properties props = new Properties();
            props.load(is);

            Class eventClass = OCRcEvent.class;

            Map map = new HashMap();
            for (Enumeration e = props.keys(); e.hasMoreElements();)
            {
                String key = (String) e.nextElement();

                StringTokenizer tok = new StringTokenizer(props.getProperty(key), " \t,:");
                List list = new ArrayList(tok.countTokens());
                for (; tok.hasMoreTokens();)
                {
                    String val = tok.nextToken();
                    Field field = eventClass.getField(val);
                    int vk = field.getInt(null);

                    list.add(Key.createKey(vk, KeyEvent.KEY_PRESSED, CHAR_UNDEFINED));
                    list.add(Key.createKey(vk, KeyEvent.KEY_RELEASED, CHAR_UNDEFINED));
                }

                StringTokenizer keyTok = new StringTokenizer(key, ",");
                for (; keyTok.hasMoreTokens();)
                {
                    String k = keyTok.nextToken();
                    char ch = '\0';
                    if (k.length() == 1)
                        ch = k.charAt(0);
                    else if (k.length() == 2 && k.charAt(0) == '\\')
                    {
                        switch (k.charAt(1))
                        {
                            case 't':
                                ch = '\t';
                                break;
                            case 'r':
                                ch = '\r';
                                break;
                            case 'n':
                                ch = '\n';
                                break;
                            case 's':
                                ch = ' ';
                                break;
                            default:
                                throw new IllegalArgumentException("Cannot parse key '" + key + "'");
                        }
                    }
                    else if (k.length() == 5 && k.charAt(0) == 'u')
                    {
                        ch = (char) Integer.parseInt(k.substring(1), 16);
                    }
                    else
                        throw new IllegalArgumentException("Cannot parse key '" + key + "'");
                    Character c = new Character(ch);

                    List oldList = (List) map.get(c);
                    if (oldList != null)
                        oldList.addAll(list);
                    else
                        map.put(c, list);
                }
            }

            if (log.isDebugEnabled())
            {
                log.debug("Custom corresponding key definitions: " + map);
            }

            return map;
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
            return null;
        }
    }

    /**
     * Creates a new <code>Set</code> of <code>Key</code>s from the given
     * <code>UserEventRepository</code>.
     * <p>
     * Note that keys are limited to the <i>minimum key set</i> minus the set of
     * <i>mandatory ordinary key codes</i>. If any keys from the
     * <i>mandatory</i> set are included, then an exception is thrown.
     * Otherwise, keys outside the <i>minimum</i> set are simply ignored.
     * 
     * @param uer
     *            user event repository
     * @return unsynchronized <code>Set</code> of <code>Key</code>s
     * 
     * @throws IllegalArgumentException
     *             if an mandatory keys are included
     */
    private static Set createMinimumKeySet(UserEventRepository uer) throws IllegalArgumentException
    {
        org.dvb.event.UserEvent[] events = uer.getUserEvent();
        if (events.length == 0) return Collections.EMPTY_SET;

        HashSet set = new HashSet();
        for (int i = 0; i < events.length; ++i)
        {
            if (MANDATORY.isPresent(events[i]))
                throw new IllegalArgumentException("Cannot filter Mandatory Ordinary Keycodes: " + events[i].getCode());
            else if (events[i].getCode() != KeyEvent.VK_UNDEFINED && MINIMUM_MINUS_MANDATORY.isPresent(events[i]))
            {
                set.add(Key.createKey(events[i]));
            }
        }
        return set;
    }

    /**
     * Creates a new <code>UserEventRepository</code> based upon the given
     * <code>Set</code> of <code>Key</code>s.
     * 
     * @param set
     *            <code>Set</code> of <code>Key</code>s
     * @param name
     *            name for the new <code>UserEventRepository</code>
     * @return a new, populated <code>UserEventRepository</code> with the given
     *         <i>name</i>
     */
    private static UserEventRepository createRepository(Set set, String name, ResourceClient client)
    {
        UserEventRepository uer = new UserEventRepositoryExt(name, client);
        if (set != null)
        {
            for (Iterator i = set.iterator(); i.hasNext();)
                uer.addUserEvent(((Key) i.next()).getUserEvent());
        }
        return uer;
    }

    /**
     * Return the set of <code>Owner</code>s for the given set of
     * <code>UserEvent</code>s.
     * 
     * @return the set of <code>Owner</code>s for the given set of
     *         <code>UserEvent</code>s
     */
    private synchronized Set getOwners(Owner requestor)
    {
        HashSet set = new HashSet();

        for (Iterator i = requestor.getKeys().iterator(); i.hasNext();)
        {
            ExclusiveDispatcher dispatcher = (ExclusiveDispatcher) exclusiveDispatch.get(i.next());
            if (dispatcher != null) set.add(dispatcher.getOwner());
        }
        for (Iterator i = requestor.getAdditionalKeys().iterator(); i.hasNext();)
        {
            ExclusiveDispatcher dispatcher = (ExclusiveDispatcher) exclusiveDispatch.get(i.next());
            if (dispatcher != null) set.add(dispatcher.getOwner());
        }

        return set;
    }

    /**
     * Creates a new <code>FilterContext</code>. This is <i>synchronized</i> to
     * synchronize access to the relevant filter attributes.
     * 
     * @return the <code>FilterContext</code> to use during dispatch
     */
    private synchronized DispatchContext newDispatchContext(KeyEvent e)
    {
        return new DispatchContext(e);
    }

    /**
     * Returns a <i>clean</i> instance of the given <code>UserEvent</code>. This
     * serves three purposes:
     * <ul>
     * <li>Sets the appropriate source on a per-application basis.
     * <li>Produces an implementation-created instance in case the instance
     * returned by the filter is application-defined.
     * <li>Produces an instance unique to each application, disallowing
     * synchronization-based communication (least important here).
     * </ul>
     * 
     * @param event
     *            the original UserEvent
     * @param source
     *            the new source to be specified
     * @return a <i>clean</i> version of <i>event</i>
     */
    private static UserEvent cleanUserEvent(UserEvent event, Object source)
    {
        int type = event.getType();
        int family = event.getFamily();
        long when = event.getWhen();

        return (type == KeyEvent.KEY_TYPED) ? new UserEvent(source, family, event.getKeyChar(), when) : new UserEvent(
                source, family, type, event.getCode(), event.getModifiers(), when);
    }

    /**
     * Converts a <code>KeyEvent</code> to a <code>UserEvent</code>.
     * 
     * @return the given <code>KeyEvent</code> represented as a
     *         <code>UserEvent</code>
     * 
     *         TODO: Problem here is that user2Key(key2User(e)) won't produce an
     *         event of the original type!
     */
    private static UserEvent key2User(KeyEvent e)
    {
        UserEvent ue = (e.getID() == KeyEvent.KEY_TYPED) ? (new UserEvent(e.getSource(),
                org.dvb.event.UserEvent.UEF_KEY_EVENT, e.getKeyChar(), e.getWhen())) : (new UserEvent(e.getSource(),
                org.dvb.event.UserEvent.UEF_KEY_EVENT, e.getID(), e.getKeyCode(), e.getModifiers(), e.getWhen()));
        ue.setKeyChar(e.getKeyChar());
        return ue;
    }

    /**
     * Converts a <code>UserEvent</code> to a <code>KeyEvent</code>.
     * 
     * @return the given <code>UserEvent</code> represented as a
     *         <code>KeyEvent</code>.
     * 
     *         TODO: Problem here is that user2Key(key2User(e)) won't produce an
     *         event of the original type!
     */
    private static KeyEvent user2Key(UserEvent e)
    {
        try
        {
            return new KeyEvent((java.awt.Component) e.getSource(), e.getType(), e.getWhen(), e.getModifiers(),
                    e.getCode(), e.getKeyChar());
        }
        catch (ClassCastException ccex)
        {
            return null;
        }
    }

    /**
     * Access the EventManager's global data object associated with the current
     * context. If none is assigned, then one is created.
     * <p>
     * Synchronizes on <code>this</code>. No <i>internal</i> lock is required
     * because no reference to this object will be provided directly to
     * applications.
     * 
     * @param ctx
     *            the context to access
     * @return the <code>Data</code> object
     */
    private synchronized Data getData(CallerContext ctx)
    {
        Data data = (Data) ctx.getCallbackData(this);
        if (data == null)
        {
            data = new Data();
            ctx.addCallbackData(data, this);
        }
        return data;
    }

    /**
     * Cleans up after a CallerContext is destroyed, forgetting any listeners
     * previously associated with it.
     * 
     * @param ctx
     *            the context to forget
     */
    private synchronized void cleanup(CallerContext ctx)
    {
        // Remove ctx from the set of contexts with listeners
        rseContexts = Multicaster.remove(rseContexts, ctx);
        for (Enumeration e = ueContexts.keys(); e.hasMoreElements();)
        {
            Object key = e.nextElement();
            CallerContext ccList = (CallerContext) ueContexts.get(key);
            ccList = Multicaster.remove(ccList, ctx);
            ueContexts.put(key, ccList);
        }

        // Remove exclusive ownership
        Data data = getData(ctx);
        Set set = data.ownedKeys.keySet();
        // Copy out receivers (ownedKeys will get cleaned up by cleanup())
        ExclusiveReceiver[] receivers = (ExclusiveReceiver[]) set.toArray(new ExclusiveReceiver[set.size()]);
        for (int i = 0; i < receivers.length; ++i)
        {
            // Cleanup listener/client
            receivers[i].cleanup();
            // Count on this to notify ResourceStatusListeners
        }

        // Forget installed filter
        if (filterContext == ctx)
        {
            filterContext = null;
            eventFilter = null;
        }

        // Forget installed filtered repository
        if (repositoryContext == ctx)
        {
            filteredRepository = ""; // empty UserEventRepository
            filteredKeySet = Collections.EMPTY_SET;

            repositoryContext = null;
        }
    }

    /**
     * Common method used to determine the CallerContext. This dynamically
     * determines the CallerContextManager, rather than accessing a single
     * instance variable, to enable testing.
     * 
     * @return the CallerContextManager
     */
    private static CallerContext getContext()
    {
        CallerContextManager cm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        return cm.getCurrentContext();
    }

    /**
     * Check for proper permissions.
     * 
     * @throws SecurityException
     */
    private static void checkPermission(Permission p) throws SecurityException
    {
        SecurityUtil.checkPermission(p);
    }

    /**
     * Shared instance of MonitorAppPermission("filterUserEvents");
     */
    private static final MonitorAppPermission monitor_filter = new MonitorAppPermission("filterUserEvents");

    /**
     * The currently set filteredRepository. Actually, this is just the name.
     * The actual repository will be reconstructed on-demand. If
     * <code>null</code>, then a <code>null</code>
     * <code>UserEventRepository</code> should be returned by
     * {@link #getFilteredRepository()}; if non-null then a non-null repository
     * should be returned.
     * 
     * @see #filteredKeySet
     */
    private String filteredRepository = "";

    /**
     * The <code>CallerContext</code> that installed the filteredRepository.
     */
    private CallerContext repositoryContext;

    /**
     * The currently set filteredRepository, maintained as a
     * <code>KeyRepository</code> which provides for faster lookup. Defaults to
     * what amounts to an empty repository, which always returns
     * <code>false</code> when queried about the presence of an event.
     * 
     * @see #filteredRepository
     */
    private Set filteredKeySet = Collections.EMPTY_SET;

    /**
     * The current monitor-app-installed strategy used to filter events.
     */
    private UserEventFilter eventFilter;

    /**
     * The <code>CallerContext</code> that installed the eventFilter.
     */
    private CallerContext filterContext;

    /**
     * The set of CallerContext's that added UserEventListeners. Mapped to be
     * {@link Key}.
     * <p>
     * {@link MyHashtable} is used because it allow {@link MyHashtable#put} with
     * a <code>null</code> value (implying a {@link Hashtable#remove}.
     */
    private Hashtable ueContexts = new MyHashtable();

    /**
     * The set of <code>Exclusive</code> objects that are to be dispatched to.
     * Mapped to by {@link Key}.
     */
    private Hashtable exclusiveDispatch = new Hashtable();

    /**
     * The set of CallerContext's that added ResourceStatusListeners.
     */
    private CallerContext rseContexts = null;

    /**
     * The value to be used for {@link KeyEvent#CHAR_UNDEFINED}. This is used
     * rather than the actual value because the required value may be different
     * than the actual value. OCAP-1.0 requires a value of zero be used because
     * that was the value as defined in JDK 1.1.8 (upon which OCAP-1.0 is
     * based). Java2 (including J2ME PBP 1.0) changed this value to 65535.
     * <p>
     * NOTE: this is currently defined in terms of
     * {@link HaviToolkit#getCharUndefined()}; while <code>EventMgr</code> is
     * not otherwise dependent upon HAVi, this was done so that there is only
     * one place that this needs to be defined and <code>HaviToolkit</code>
     * already had such support.
     */
    static final char CHAR_UNDEFINED = HaviToolkit.getCharUndefined();

    /**
     * Flag that determines if translation from native {KeyEvent#CHAR_UNDEFINED}
     * to the OCAP-1.0 {@link #CHAR_UNDEFINED} is necessary.
     */
    private static final boolean TRANSLATE_CHAR_UNDEFINED = CHAR_UNDEFINED != KeyEvent.CHAR_UNDEFINED;

    /**
     * The set of mandatory system keycodes. Used to populate the KeyRepository
     * for fast lookup.
     */
    private static final int[] mandatoryKeyCodes = { KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3,
            KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9,
            KeyEvent.VK_ENTER, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN, };

    /*
     * The set of minimum keycodes (minus the mandatory set).
     */
    private static final int[] minimumKeyCodes = { HRcEvent.VK_POWER, HRcEvent.VK_CHANNEL_UP, HRcEvent.VK_CHANNEL_DOWN,
            HRcEvent.VK_VOLUME_DOWN, HRcEvent.VK_VOLUME_UP, HRcEvent.VK_MUTE, KeyEvent.VK_PAUSE, HRcEvent.VK_PLAY,
            HRcEvent.VK_STOP, HRcEvent.VK_RECORD, HRcEvent.VK_FAST_FWD, HRcEvent.VK_REWIND, HRcEvent.VK_GUIDE,
            OCRcEvent.VK_RF_BYPASS, OCRcEvent.VK_MENU, HRcEvent.VK_INFO, OCRcEvent.VK_EXIT, OCRcEvent.VK_LAST,
            HRcEvent.VK_COLORED_KEY_0, HRcEvent.VK_COLORED_KEY_1, HRcEvent.VK_COLORED_KEY_2, HRcEvent.VK_COLORED_KEY_3,
            OCRcEvent.VK_NEXT_FAVORITE_CHANNEL, OCRcEvent.VK_ON_DEMAND, };

    /**
     * The set of mandatory key codes. This is used during
     * {@link #setFilteredRepository(UserEventRepository)} where an exception is
     * thrown if it contains any of the mandatory keys.
     */
    private static final KeyRepository MANDATORY = new SimpleKeyRepository(mandatoryKeyCodes);

    /**
     * The set of minimum key codes, with the mandatory key codes subtracted.
     * This is used during {@link #setFilteredRepository(UserEventRepository)}
     * where any keys not in this set are ignored as not <i>"qualified"</i>.
     * <p>
     * 
     * OCAP K.2.1 says: <quote> Qualified user input keycodes are those defined
     * in Table 25-5 [which defines minimum and mandatory key codes] that are
     * not identified as Mandatory Ordinary Keycodes. </quote>
     * 
     * @see "OCAP K.2.1 Event Filtering"
     */
    private static final KeyRepository MINIMUM_MINUS_MANDATORY = new SimpleKeyRepository(minimumKeyCodes);

    /**
     * If <code>true</code>, then filtering of AWTEvents is enabled.
     */
    private static final boolean FILTER_AWT = true;

    /** Log4J Logger. */
    private static final Logger log = Logger.getLogger(EventMgr.class.getName());

    /**
     * The set of corresponding keys as defined for the platform. This will be
     * <code>null</code> if none are defined for the platform, in which case we
     * fall back to the following implicit definition:
     * <ul>
     * <li>'0'-'9' is mapped directly to key code
     * <li>'A'-'Z' is mapped directly to key code
     * <li>'a'-'z' is mapped to key code following upper case conversion
     * <li>' ', '\n', '\t', and '\b' map directly to key code
     * </ul>
     */
    private static final Map correspondingKeys = loadCorrespondingKeys();

    /**
     * An object that captures the <i>context</i> within which an event is
     * dispatched. This includes filtering and subsequent dispatch of the event
     * per OCAP.
     * 
     * @author Aaron Kamienski
     */
    private class DispatchContext implements DispatchFilter
    {
        DispatchContext(KeyEvent e)
        {
            // Does nothing for now (except for implicit instance var setup)
            origKeyEvent = e;
        }

        public void dispatch(KeyEvent keyEvent)
        {
            // 1. Filter the event (if qualified, non-mandatory keycode)
            // a. This generates an event and a set of applications to map to...
            if (filterEvent(key2User(keyEvent)))
            {
                // Event is not to be delivered
                return;
            }

            // 2. Does an app have focus or is it in a managed repository?
            // If not, then done.

            // Dispatch to app with exclusive access, if there is one
            // 2. If an application has registered exclusive access to this
            // event...
            if (!postExclusive())
            {
                // 3. If no application has registered exclusive access then...

                // a. Send to currently focused app's event queue
                postAWTEvent();
                // b. Sent to apps registered for shared access
                postUserEvent();
            }
            return;

        }

        /**
         * Calls the current monitor application <code>UserEventFilter</code> if
         * one has been set <i>and</i> the event's keycode is qualified for
         * filtering.
         * <p>
         * 
         * @param ue
         *            the <code>UserEvent</code> to filter
         * @return <code>true</code> if the event should be consumed and no
         *         further dispatching should occur; <code>false</code> if the
         *         event should be dispatched
         */
        private boolean filterEvent(UserEvent ue)
        {
            userEvent = ue;
            key = Key.createKey(ue);

            // Check if not mandatory key...
            // and if in repository...
            if (filter != null && context != null
                    && (filteredKeys == null ? !MANDATORY.isPresent(key.code) : filteredKeys.contains(key)))
            {
                // Call filter handler...
                final boolean[] snarfed = { false };

                if (!CallerContext.Util.doRunInContextSync(context, new Runnable()
                {
                    public void run()
                    {
                        UserEventAction uea = filter.filterUserEvent(userEvent);

                        // If null is returned, then the event should be
                        // consumed
                        if (uea == null)
                        {
                            snarfed[0] = true;
                            return;
                        }

                        userEvent = uea.getEvent();
                        if (userEvent == null)
                            snarfed[0] = true;
                        else
                        {
                            key = Key.createKey(userEvent);
                            ids = uea.getAppIDs();
                        }
                    }
                }))
                {
                    snarfed[0] = true;
                }
                return snarfed[0];
            }
            return false;
        }

        /**
         * Posts the given <code>UserEvent</code> to the appropriate context for
         * exclusive delivery.
         * 
         * @return <code>true</code> if an app had the event reserved for
         *         exclusive access; <code>false</code> if no app had the event
         *         reserved for exclusive access
         */
        private boolean postExclusive()
        {
            ExclusiveDispatcher handler = (ExclusiveDispatcher) exclusiveDispatch.get(key);
            if (handler == null) return false;

            handler.dispatch(this);
            return true;
        }

        /**
         * Posts the given <code>KeyEvent</code> to the currently focused
         * application.
         */
        private void postAWTEvent()
        {
            postAWTEvent(this, true);
        }

        /**
         * Posts the appropriate event to the current focus context, if any,
         * using the provided filter.
         * 
         * @param dispatchFilter
         *            used to control whether the current focus context
         *            dispatches the event or not
         * @param interestFilter
         *            if <code>true</code> then the <code>FocusContext</code>
         *            only dispatches if interested in the event; if
         *            <code>false</code> then it always dispatches
         */
        void postAWTEvent(DispatchFilter dispatchFilter, boolean interestFilter)
        {
            FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
            FocusContext focus = fm.getFocusOwnerContext();

            if (focus != null)
            {
                // FocusContext is now responsible for the event.
                // Including setting the source and consuming it.
                // It is also responsible for delivering within the appropriate
                // context
                // It is also responsible for delivering based only if accepted
                // by the filter
                if (FILTER_AWT)
                    focus.dispatchEvent(user2Key(userEvent), dispatchFilter, interestFilter);
                else
                    focus.dispatchEvent(origKeyEvent, dispatchFilter, interestFilter);
            }
        }

        /**
         * Posts the given <code>UserEvent</code> to the given context or
         * multicasted contexts.
         */
        private void postUserEvent()
        {
            CallerContext contexts = (CallerContext) ueContexts.get(key);
            if (contexts != null)
            {
                final CallerContextManager cm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

                contexts.runInContext(new Runnable()
                {
                    public void run()
                    {
                        // Lookup current context
                        CallerContext ctx = cm.getCurrentContext();

                        // Check if this app was filtered out
                        if (isExcluded(ctx))
                        {
                            return;
                        }

                        // Lookup global data for app
                        Data data = (Data) ctx.getCallbackData(EventMgr.this);
                        UserEventListener listener;

                        // Call listener if listener is available
                        // and event is in repository.
                        if (data != null && (listener = (UserEventListener) data.ueListeners.get(key)) != null)
                        {
                            listener.userEventReceived(cleanUserEvent(userEvent, data.eventMgr));
                        }
                    }
                });
            }
        }

        /**
         * Implements {@link DispatchFilter#accept} for delivery of
         * non-exclusive AWT events.
         * 
         * @param cc
         *            the context to which the event will be delivered, if
         *            accepted
         * @return <code>true</code> if the event can be delivered to the given
         *         context
         */
        public boolean accept(CallerContext cc)
        {
            return !isExcluded(cc);
        }

        /**
         * Used to determine if the given <code>CallerContext</code>'s listeners
         * should be notified about an event. A <code>null</code> value for
         * <i>ids</i> implies that all listeners should be notified; in which
         * case <code>false</code> is always returned.
         * 
         * @param ctx
         *            the CallerContext to test
         * @return <code>false</code> if the <code>CallerContext</code>'s
         *         listeners should be notified; <code>true</code> otherwise.
         */
        public boolean isExcluded(CallerContext ctx)
        {
            if (ids == null) return false;

            AppID id = (AppID) ctx.get(CallerContext.APP_ID);
            // For now, never exclude system context...
            if (id == null) return false;

            for (int i = 0; i < ids.length; ++i)
            {
                if (id.equals(ids[i]))
                {
                    return false;
                }
            }
            return true;
        }

        /**
         * The original KeyEvent being dispatched.
         */
        private final KeyEvent origKeyEvent;

        /**
         * Specifies the event to be dispatched.
         */
        public UserEvent userEvent;

        /**
         * A representation of the key to be dispatched.
         */
        private Key key;

        /**
         * Specifies the set of <code>AppID</code>s to which the event should be
         * dispatched. If <code>null</code> then it should be dispatched to all
         * relevant applications.
         */
        public AppID[] ids = null;

        private final CallerContext context = filterContext;

        private final UserEventFilter filter = eventFilter;

        private final Set filteredKeys = filteredKeySet;
    }

    /**
     * Per-context global data.
     * 
     * TODO: Could probably be refactored such that it extends an existing
     * implementation to get the dummy method impls.
     */
    private class Data implements CallbackData
    {
        /**
         * The EventManager proxy object for the given context. Returned by
         * {@link EventMgr#getEventManager}.
         */
        public final EventManagerImpl eventMgr = new EventManagerImpl(EventMgr.this);

        /**
         * The installed UserEventListener(s), mapped to by key. Value may be a
         * UserEventMulticaster.
         * <p>
         * {@link MyHashtable} is used because it allow {@link MyHashtable#put}
         * with a <code>null</code> value (implying a {@link Hashtable#remove}.
         */
        public final Hashtable ueListeners = new MyHashtable();

        /**
         * The installed ResourceStatusEventListener(s). May be a
         * ResourceStatusEventMulticaster.
         */
        public ResourceStatusListener rseListeners;

        /**
         * The set of <i>owned<i> keys, mapped to by
         * <code>UserEventListener</code> or <code>ResourceClient</code>.
         * 
         * This is used for reservation release/cleanup operations.
         */
        public final Map ownedKeys = new HashMap();

        public void destroy(CallerContext cc)
        {
            cleanup(cc);
        }

        public void active(CallerContext cc)
        { /* empty */
        }

        public void pause(CallerContext cc)
        { /* empty */
        }
    }

    /**
     * Extension of {@link Hashtable} that allows a call to {@link #put} with a
     * <code>null</code> value, implying a {@link Hashtable#remove} of the given
     * key.
     * 
     * @author Aaron Kamienski
     */
    private static class MyHashtable extends Hashtable
    {
        public Object put(Object key, Object value)
        {
            if (value == null)
                return remove(key);
            else
                return super.put(key, value);
        }
    }

    /**
     * Represents an exclusive owner of a set of events. An <code>Owner</code>
     * is composed of:
     * <ul>
     * <li> {@link ResourceClient}
     * <li> {@link CallerContext}
     * <li>requested/owned keys (both {@link #getKeys explicit} and
     * {@link #getAdditionalKeys implicit}
     * </ul>
     * 
     * @author Aaron Kamienski
     */
    private static class Owner
    {
        Owner(ResourceClient rc, String repositoryName, org.dvb.event.UserEvent[] events, boolean userEvents)
                throws UnsupportedKeys
        {
            cc = getContext();
            client = rc;
            repository = repositoryName;
            keyset = Collections.unmodifiableSet(createKeySet(events));
            addlKeyset = Collections.unmodifiableSet(createCorrespondingKeySet(keyset));
            type = userEvents;
        }

        /**
         * Returns the <code>Set</code> of <code>Key</code>s being explicitly
         * reserved.
         * 
         * @return a <code>Set</code> of <code>Key</code>s
         */
        public Set getKeys()
        {
            return keyset;
        }

        /**
         * Returns the additional <code>Set</code> of <i>corresponding</i>
         * <code>Key</code>s being implicitly reserved. These are the set of
         * keys whose reservation is implied by the {@link #getKeys explicit
         * set}.
         * 
         * @return a <code>Set</code> of <code>Key</code>s
         */
        public Set getAdditionalKeys()
        {
            return addlKeyset;
        }

        /**
         * Returns the entire set of <code>Key</code>s being explicitly
         * reserved, explicitly and implicitly. This is the union of
         * {@link #getKeys} and {@link #getAdditionalKeys}.
         * <p>
         * Note that this always returns a new <code>Set</code>.
         * 
         * @return the entire set of <code>Key</code>s being explicitly reserved
         */
        public Set getAllKeys()
        {
            Set set = new HashSet();
            set.addAll(keyset);
            set.addAll(addlKeyset);
            return set;
        }

        /**
         * Retrieves the <code>CallerContext</code> for this <code>Owner</code>.
         * 
         * @return the <code>CallerContext</code> for this <code>Owner</code>
         */
        public CallerContext getCallerContext()
        {
            return cc;
        }

        /**
         * Request that this <code>Owner</code> give up the reservation of the
         * associated keys.
         * 
         * @return the set of keys to be released; <code>null</code> if none are
         *         released
         */
        public Set requestRelease()
        {
            final boolean[] rc = { true };
            CallerContext.Util.doRunInContextSync(cc, new Runnable()
            {
                public void run()
                {
                    rc[0] = client.requestRelease(getUserEventRepository(), null);
                }
            });

            return rc[0] ? getAllKeys() : null;
        }

        /**
         * Invokes {@link ResourceClient#release(ResourceProxy)} to request that
         * the <code>UserEventRepository</code> be released.
         * 
         * @param priority
         *            the application priority of the caller
         * @return the set of keys to be released; <code>null</code> if none are
         *         released
         */
        public Set forceRelease(int priority)
        {
            if (priority > getPriority())
            {
                CallerContext.Util.doRunInContextSync(cc, new Runnable()
                {
                    public void run()
                    {
                        client.release(getUserEventRepository());
                    }
                });
                cc.runInContext(new Runnable()
                {
                    public void run()
                    {
                        client.notifyRelease(getUserEventRepository());
                    }
                });

                return getAllKeys();
            }
            return null;
        }

        /**
         * Add the given <code>OwnerKey</code> as a receiver for this
         * dispatcher.
         * <p>
         * Note that this method should be overridden by subclasses, which
         * should invoke <code>super.add()</code>.
         * 
         * @param receiver
         *            the object to be notified of events (or a proxy)
         */
        void add(ExclusiveReceiver receiver)
        {
            receivers.add(receiver);
        }

        /**
         * Returns the application priority of the calling application.
         * 
         * @return the priority or -1 if there is none (i.e., if caller is
         *         system context)
         */
        int getPriority()
        {
            Integer priorityObject = (Integer) cc.get(CallerContext.APP_PRIORITY);
            return priorityObject == null ? -1 : priorityObject.intValue();
        }

        /**
         * Returns a copy of the <code>UserEventRepository</code> maintained by
         * this owner.
         * 
         * @return a copy of the <code>UserEventRepository</code> owned by this
         *         object
         */
        UserEventRepository getUserEventRepository()
        {
            return createRepository(keyset, repository, client);
        }

        /**
         * Overrides {@link Object#equals(java.lang.Object)}.
         * 
         * @return <code>true</code> if <i>obj</i> is an identical
         *         <code>Owner</code>
         */
        public boolean equals(Object obj)
        {
            return obj != null && (getClass() == obj.getClass()) && type == ((Owner) obj).type
                    && client == ((Owner) obj).client && cc == ((Owner) obj).cc
                    && repository.equals(((Owner) obj).repository) && keyset.equals(((Owner) obj).keyset)
                    && addlKeyset.equals(((Owner) obj).addlKeyset);
        }

        /**
         * Overrides {@link Object#hashCode()}.
         * 
         * @return a hash code derived from the <code>ResourceClient</code>,
         *         <code>CallerContext</code>, and keys
         */
        public int hashCode()
        {
            return client.hashCode() ^ cc.hashCode() ^ repository.hashCode() ^ (type ? 0 : 1)
            // ^ keyset.hashCode()
            // ^ addlKeyset.hashCode()
            ;
        }

        /**
         * Represents the type of ownership. If <code>true</code> then ownership
         * for <code>UserEvents</code>. If <code>false</code> then ownership for
         * <code>AWTEvent</code>.
         */
        private final boolean type;

        /**
         * The set of <code>UserEventListener</code>s or
         * <code>ResourceClient</code>s set to receive events owned by this
         * owner. This is remembered so that these listeners can be subsequently
         * forgotten after ownership is implicitly released by the stack.
         */
        private final Set receivers = new HashSet();

        /**
         * Reference to the owning application. This is used in notification of
         * the application.
         */
        private final CallerContext cc;

        /**
         * The <code>ResourceClient</code> used to reserve events.
         */
        private final ResourceClient client;

        /**
         * The name of the <code>UserEventRepository</code> that contains the
         * events to be reserved.
         */
        private final String repository;

        /**
         * The <code>Set</code> of <code>Key</code>s being explicitly reserved.
         */
        private final Set keyset;

        /**
         * The <code>Set</code> of <code>Key</code>s being implicitly reserved.
         * This set is generated from the {@link #keyset explicit set} such that
         * the rules outlined in {@link UserEventRepository} for
         * <i>corresponding</i> keys are preserved.
         */
        private final Set addlKeyset;
    }

    /**
     * Exception thrown once it is discovered that the given keys cannot be
     * supported for reservation.
     * 
     * @author Aaron Kamienski
     */
    private static class UnsupportedKeys extends Exception
    {
        // Empty
    }

    /**
     * An abstract base representing a manner of dispatching exclusively-owned
     * events.
     * 
     * @author Aaron Kamienski
     * 
     * @see Owner
     * @see UserEventDispatcher
     * @see AWTDispatcher
     */
    private static abstract class ExclusiveDispatcher
    {
        ExclusiveDispatcher(Owner owner, ExclusiveReceiver receiver)
        {
            this(owner);
            owner.add(receiver);
        }

        ExclusiveDispatcher(Owner owner)
        {
            this.owner = owner;
        }

        /**
         * Returns the <code>Owner</code> that owns the keys to be dispatched by
         * this <code>ExclusiveDispatcher</code>.
         * 
         * @return the associated owner
         */
        public Owner getOwner()
        {
            return owner;
        }

        /**
         * Dispatch the given event to the intended application.
         * 
         * @param filtered
         *            the user event to dispatch
         */
        abstract void dispatch(DispatchContext filtered);

        /**
         * Add the given <code>OwnerKey</code> as a receiver for this
         * dispatcher.
         * <p>
         * Note that the default implementation simply passes the add along to
         * the owner. As such, this method should be overridden by subclasses,
         * which should invoke <code>super.add()</code>.
         * 
         * @param receiver
         *            the object to be notified of events (or a proxy)
         */
        void add(ExclusiveReceiver receiver)
        {
            owner.add(receiver);
        }

        /**
         * Remove the given <code>OwnerKey</code> from this dispatcher.
         * 
         * @param receiver
         *            the object to be removed form notification
         * 
         * @return <code>true</code> if no more listeners are present;
         *         <code>false</code> if additional listeners are present
         */
        abstract boolean remove(ExclusiveReceiver receiver);

        /**
         * The <i>owner</i> of the keys to be dispatched using this
         * <code>ExclusiveDispatcher</code>.
         */
        private final Owner owner;
    }

    /**
     * Handles dispatching of implicitly reserved <i>corresponding</i> events.
     * No events are ever dispatched -- instead they are simply ignored.
     * 
     * @author Aaron Kamienski
     */
    private static class NullDispatcher extends ExclusiveDispatcher
    {
        NullDispatcher(ExclusiveDispatcher base)
        {
            super(base.getOwner());
            this.base = base;
        }

        /**
         * Does nothing. The event is eaten.
         */
        void dispatch(DispatchContext filtered)
        {
            // Does nothing
        }

        /**
         * Passes the <i>remove</i> request on to the <i>base</i> dispatcher.
         * Returns whatever that returns.
         * 
         * @return <code><i>base</i>.remove(<i>receiver</i>)</code>
         */
        boolean remove(ExclusiveReceiver receiver)
        {
            // Return what the real dispatcher would return
            // Signal that this dispatcher should be removed or not
            return base.remove(receiver);
        }

        /** The base dispatcher. */
        private ExclusiveDispatcher base;
    }

    /**
     * Dispatches exclusively reserved events via {@link UserEventListener}.
     * 
     * @author Aaron Kamienski
     */
    private static class UserEventDispatcher extends ExclusiveDispatcher
    {
        UserEventDispatcher(Owner owner, ListenerReceiver listener)
        {
            super(owner, listener);
            this.listener = (UserEventListener) listener.key;
        }

        /**
         * Dispatches the given <code>UserEvent</code> to the set of listeners
         * added to this object.
         * 
         * @param filtered
         *            represents the event to dispatch
         */
        void dispatch(final DispatchContext filtered)
        {
            CallerContext cc = getOwner().getCallerContext();
            if (filtered.isExcluded(cc)) return;

            final UserEventListener l = listener;
            if (l == null)
            {
                // Looks like there's been a race to get here...
                // We shouldn't have it reserved anymore.
                return;
            }
                    
            if (log.isInfoEnabled())
            {
                log.info("UserEventDispatcher: calling runInContext: l = " + l);
            }
            // Deliver the event to the installed listeners
            cc.runInContext(new Runnable()
            {
                public void run()
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Calling userEventReceived");
                    }
                    l.userEventReceived(filtered.userEvent);
                }
            });
            return;
        }

        void add(ExclusiveReceiver receiver)
        {
            super.add(receiver);
            listener = EventMulticaster.add(listener, (UserEventListener) receiver.key);
        }

        boolean remove(ExclusiveReceiver receiver)
        {
            listener = EventMulticaster.remove(listener, (UserEventListener) receiver.key);
            return listener == null;
        }

        /**
         * The set of <code>UserEventListener</code>s.
         */
        private UserEventListener listener;
    }

    /**
     * Dispatches exclusively reserved events via the standard
     * <code>AWTEvent</code> queue mechanism.
     * 
     * @author Aaron Kamienski
     */
    private static class AWTDispatcher extends ExclusiveDispatcher
    {
        AWTDispatcher(Owner owner, AWTReceiver receiver)
        {
            super(owner, receiver);
        }

        /**
         * Dispatches the given <code>UserEvent</code> to the application
         * associated with this object.
         * 
         * @param filtered
         *            the filtered user event
         */
        void dispatch(final DispatchContext filtered)
        {
            DispatchFilter filter = new DispatchFilter()
            {
                public boolean accept(CallerContext cc)
                {
                    return cc == getOwner().getCallerContext() && !filtered.isExcluded(cc);
                }
            };
            filtered.postAWTEvent(filter, false);
            return;
        }

        boolean remove(ExclusiveReceiver receiver)
        {
            // Does nothing -- cannot be added to
            return true;
        }
    }

    /**
     * Used to wrap user-defined objects for use as keys.
     * <p>
     * Used to map <code>UserEventListener</code>s or
     * <code>ResourceClient</code>s to their reserved keys for implementation of
     * {@link EventMgr#removeUserEventListener} and
     * {@link EventMgr#removeExclusiveAccessToAWTEvent}.
     * 
     * @author Aaron Kamienski
     * 
     * @see AWTReceiver
     * @see ListenerReceiver
     */
    private static abstract class ExclusiveReceiver
    {
        /**
         * Creates an instance of OwnerKey for the given <i>key</i> object.
         * 
         * @param key
         *            the base key
         */
        ExclusiveReceiver(Object key)
        {
            this.key = key;
        }

        /**
         * Overrides {@link Object#equals(java.lang.Object)} to allow for
         * different purposes for the same base <i>key</i> object. Purpose is
         * dictated by the specific class of <code>OwnerKey</code>.
         * 
         * @return <code>true</code> if same key, for same purpose
         */
        public boolean equals(Object obj)
        {
            return obj != null && obj.getClass() == this.getClass() && ((ExclusiveReceiver) obj).key == key;
        }

        /**
         * Overrides {@link Object#hashCode()} such that
         * {@link System#identityHashCode} is always used. This is done in case
         * the user over-rode {@link Object#hashCode}.
         * 
         * @return <code>System.identityHashCode()</code> for the given key
         */
        public int hashCode()
        {
            return System.identityHashCode(key);
        }

        /**
         * Cleanup method invoked during {@link EventMgr#cleanup}.
         */
        abstract void cleanup();

        protected final Object key;
    }

    /**
     * Encapsulates a <code>UserEventListener</code>.
     * 
     * @author Aaron Kamienski
     */
    private class ListenerReceiver extends ExclusiveReceiver
    {
        ListenerReceiver(UserEventListener uel)
        {
            super(uel);
        }

        void cleanup()
        {
            removeUserEventListener((UserEventListener) key);
        }
    }

    /**
     * Encapsulated a <code>ResourceClient</code> associated with exclusive
     * <code>AWTEvent</code> reservation.
     * 
     * @author Aaron Kamienski
     */
    private class AWTReceiver extends ExclusiveReceiver
    {
        AWTReceiver(ResourceClient rc)
        {
            super(rc);
        }

        void cleanup()
        {
            removeExclusiveAccessToAWTEvent((ResourceClient) key);
        }
    }

    private static class UserEventRepositoryExt extends UserEventRepository
    {
        private ResourceClient client;

        public UserEventRepositoryExt(String name, ResourceClient client)
        {
            super(name);
            this.client = client;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.dvb.event.RepositoryDescriptor#getClient()
         */
        public ResourceClient getClient()
        {
            return client;
        }

    }
}
