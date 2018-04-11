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

package org.cablelabs.impl.service;

import java.util.EventListener;
import java.util.Hashtable;

import javax.tv.locator.Locator;
import javax.tv.service.ReadPermission;
import javax.tv.service.SIChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.transport.NetworkChangeEvent;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.TransportStreamChangeEvent;
import javax.tv.service.transport.TransportStreamChangeListener;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * An instance of <code>ListenerList</code> maintains a list of listeners per
 * caller context per unique identifier. The unique identifiers used must be
 * unique system wide because each is used as the key to data registered with
 * the {@link CallerContext}.
 * <p>
 * All listeners registered with a single instance of this object must be of the
 * same type and must be one of the following.
 * <ul>
 * <li> {@link NetworkChangeListener}
 * <li> {@link TransportStreamChangeListener}
 * <li> {@link ServiceDetailsChangeListener}
 * <li> {@link ServiceComponentChangeListener}
 * </ul>
 * 
 * @author Todd Earles
 */
public class ListenerList
{
    /**
     * Construct a <code>ListenerList</code>
     * 
     * @param listenerClass
     *            All listeners registered with this list must be instances of
     *            this class.
     * @throws ClassCastException
     *             If the <code>listenerClass</code> is not supported.
     */
    public ListenerList(Class listenerClass)
    {
        // Get caller context manager
        ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        // Save listener class
        this.listenerClass = listenerClass;

        // Determine the listener class type
        if (NetworkChangeListener.class.isAssignableFrom(listenerClass))
            listenerClassType = 1;
        else if (TransportStreamChangeListener.class.isAssignableFrom(listenerClass))
            listenerClassType = 2;
        else if (ServiceDetailsChangeListener.class.isAssignableFrom(listenerClass))
            listenerClassType = 3;
        else if (ServiceComponentChangeListener.class.isAssignableFrom(listenerClass))
            listenerClassType = 4;
        else
            throw new ClassCastException("Listener class " + listenerClass + " not a supported type");
    }

    /** The listener class for all listeners maintained by this ListenerList */
    private final Class listenerClass;

    /** A numeric value used to indicate the listener class type */
    private final int listenerClassType;

    /** The caller context manager */
    private final CallerContextManager ccm;

    /**
     * A hash table used to lookup the CC list by unique identifier. The key is
     * the unique identifier and the value is the CC list.
     */
    private final Hashtable ccListLookup = new Hashtable();

    /**
     * Add a listener for the current caller context to the list of listeners
     * for the specified unique identifier. If the specified listener is already
     * registered for this caller context and unique identifier no action is
     * performed.
     * 
     * @param uniqueID
     *            The unique identifer which represents the logical object to
     *            which this listener should be added.
     * @param listener
     *            The listener object to be notified by {@link PostEvent}.
     * @throws ClassCastException
     *             If the <code>listener</code> type is not valid for this list.
     */
    public synchronized void addListener(Object uniqueID, EventListener listener)
    {
        // Verify listener type is valid
        if (!listenerClass.isInstance(listener)) throw new ClassCastException("Listener not a " + listenerClass);

        // Get the current caller context
        CallerContext cc = ccm.getCurrentContext();

        // Get the CC list for this ID and add the current CC to it
        CallerContext ccList = (CallerContext) ccListLookup.get(uniqueID);
        ccList = CallerContext.Multicaster.add(ccList, cc);
        ccListLookup.put(uniqueID, ccList);

        // Get the CC data keyed by this ID for the current CC. Create the CC
        // data if it
        // does not yet exist.
        CCData data = (CCData) cc.getCallbackData(uniqueID);
        if (data == null)
        {
            data = new CCData(uniqueID);
            cc.addCallbackData(data, uniqueID);
        }

        // Add the listener to the list maintained in the CC data
        data.listeners = Multicaster.add(data.listeners, listener);
    }

    /**
     * Remove a listener for the current caller context from the list of
     * listeners for the specified unique identifier.
     * 
     * @param uniqueID
     *            The unique identifer which represents the logical object from
     *            which this listener should be removed.
     * @param listener
     *            The listener to be removed
     */
    public synchronized void removeListener(Object uniqueID, EventListener listener)
    {
        // Get the current caller context
        CallerContext cc = ccm.getCurrentContext();

        // Get the CC data keyed by this ID for the current CC. If there is no
        // such CC data the listener must already have been removed.
        CCData data = (CCData) cc.getCallbackData(uniqueID);
        if (data == null) return;

        // Remove the listener
        data.listeners = Multicaster.remove(data.listeners, listener);

        // If the listener list is empty remove the CC data
        if (data.listeners == null)
        {
            // Remove the CC data from the CC
            cc.removeCallbackData(uniqueID);

            // Remove this CC from the CC list
            CallerContext ccList = (CallerContext) ccListLookup.get(uniqueID);
            ccList = CallerContext.Multicaster.remove(ccList, cc);
            if (ccList == null)
                ccListLookup.remove(uniqueID);
            else
                ccListLookup.put(uniqueID, ccList);
        }
    }

    /**
     * Post an event to all listeners for the specified unique identifier.
     * 
     * @param uniqueID
     *            The unique identifer which represents the logical object whose
     *            listeners should be notified.
     * @param event
     *            The event to be delivered
     */
    public void postEvent(final Object uniqueID, final SIChangeEvent event)
    {
        // Get the CC list for this ID and return if null
        CallerContext ccList = (CallerContext) ccListLookup.get(uniqueID);
        if (ccList == null) return;

        // Get the locator so we can check for read permission below
        final Locator locator;
        switch (listenerClassType)
        {
            case 1:
                locator = ((NetworkChangeEvent) event).getNetwork().getLocator();
                break;
            case 2:
                locator = ((TransportStreamChangeEvent) event).getTransportStream().getLocator();
                break;
            case 3:
                locator = ((ServiceDetailsChangeEvent) event).getServiceDetails().getLocator();
                break;
            case 4:
                locator = ((ServiceComponentChangeEvent) event).getServiceComponent().getLocator();
                break;
            default:
                locator = null;
        }

        // Deliver the event to all listeners
        if (ccList != null)
        {
            // Execute the runnable in each caller context in the CC list
            ccList.runInContext(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CallerContext cc = ccm.getCurrentContext();
                    CCData data = (CCData) cc.getCallbackData(uniqueID);
                    if (data != null && data.listeners != null
                            && SecurityUtil.hasPermission(new ReadPermission(locator)))
                    {
                        switch (listenerClassType)
                        {
                            case 1:
                                ((NetworkChangeListener) data.listeners).notifyChange((NetworkChangeEvent) event);
                                break;
                            case 2:
                                ((TransportStreamChangeListener) data.listeners).notifyChange((TransportStreamChangeEvent) event);
                                break;
                            case 3:
                                ((ServiceDetailsChangeListener) data.listeners).notifyChange((ServiceDetailsChangeEvent) event);
                                break;
                            case 4:
                                ((ServiceComponentChangeListener) data.listeners).notifyChange((ServiceComponentChangeEvent) event);
                                break;
                        }
                    }
                }
            });
        }
    }

    /**
     * Caller context specific data structure. This data structure contains the
     * listener list for a single caller context and for a single unique
     * identifier.
     */
    protected class CCData implements CallbackData
    {
        /** Constructor */
        public CCData(Object uniqueID)
        {
            this.uniqueID = uniqueID;
        }

        /** The unique identifier is stored here for use by destory() */
        public final Object uniqueID;

        /** The listeners to be notified */
        public volatile EventListener listeners;

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            synchronized (ListenerList.this)
            {
                // Remove this CC from the CC list
                CallerContext ccList = (CallerContext) ccListLookup.get(uniqueID);
                ccList = CallerContext.Multicaster.remove(ccList, cc);
                if (ccList == null)
                    ccListLookup.remove(uniqueID);
                else
                    ccListLookup.put(uniqueID, ccList);

                // Clear the listener list
                listeners = null;
            }
        }
    }

    /**
     * Event multicaster for listeners of type {@link EventListener}.
     */
    protected static class Multicaster extends EventMulticaster
    {
        /** Constructor */
        public Multicaster(EventListener a, EventListener b)
        {
            super(a, b);
        }

        /** Add a listener to the multicaster */
        public static EventListener add(EventListener a, EventListener b)
        {
            return addOnceInternal(a, b);
        }

        /** Remove a listener from the multicaster */
        public static EventListener remove(EventListener l, EventListener oldl)
        {
            return removeInternal(l, oldl);
        }
    }
}
