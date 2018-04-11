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

package org.cablelabs.impl.util;

import java.util.EventListener;
import java.util.EventObject;

import javax.media.ControllerListener;

import org.apache.log4j.Logger;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;

/**
 * This is an {@link EventMulticaster} that maintains an
 * {@link EventListener} list per
 * {@link CallerContext}. This pattern is found
 * several places in JMF and probably other places in the stack as well.
 * 
 * @author schoonma
 */
public abstract class CallerContextEventMulticaster
{
    /** Logging */
    private static final Logger log = Logger.getLogger(CallerContextEventMulticaster.class);

    /*
     * 
     * CallerContext
     */

    /**
     * {@link CallerContextManager} instance, used to register
     * {@link CallerContextData} for {@link ControllerListener}s.
     */
    protected static final CallerContextManager ccMgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * List of {@link CallerContext} that have registered listeners with this
     * instance.
     */
    private CallerContext ccList;

    /** Object for synchronizing access to {@link #ccList}. */
    private final Object ccListSync = new Object();

    /**
     * For each {@link CallerContext} in the {@link #ccList}, remove the
     * associated {@link CCData} that was added when a {@link EventListener} was
     * added. Once all {@link CCData} is removed, clear the {@link #ccList}.
     */
    public void cleanup()
    {
        synchronized (ccListSync)
        {
            if (ccList == null) return;

            // Copy the ccList and remove CallbackData for all CallerContexts
            // that registered a listener.
            CallerContext ccListCopy = ccList;
            // Clear the CallerContext list.
            ccList = null;
            // Iterate over the CallerContexts, removing CCData if associated.
            ccListCopy.runInContext(new Runnable()
            {
                public void run()
                {
                    CallerContext cc = ccMgr.getCurrentContext();
                    CallbackData data = cc.getCallbackData(ccDataKey);
                    if (data != null)
                    {
                        cc.removeCallbackData(ccDataKey);
                    }
                }
            });
        }
    }

    /*
     * 
     * CCData
     */

    /**
     * This maintains the {@link EventListener} list for a specific
     * {@link CallerContext} instance, which exists in
     * {@link CallerContextEventMulticaster#ccList}.
     */
    private class CCData implements CallbackData
    {
        /**
         * This Object is used for synchronized access to the {@link #listeners}
         * list.
         */
        private final Object lock = new Object();

        /**
         * The list of {@link EventListener}s that are registered under the
         * associated {@link CallerContext}.
         */
        private EventListener listeners;

        public void destroy(CallerContext cc)
        {
            synchronized (ccListSync)
            {
                // Remove this CallerContext's CCData from the ccList.
                ccList = CallerContext.Multicaster.remove(ccList, cc);
            }
        }

        public void pause(CallerContext callerContext)
        { /* no-op */
        }

        public void active(CallerContext callerContext)
        { /* no-op */
        }
    }

    /**
     * Lookup key for {@link CallbackData} for a
     * {@link org.cablelabs.impl.manager.CallerContext}.
     */
    private Object ccDataKey = new Object();

    /**
     * Lookup the {@link CCData} associated with a {@link CallerContext}. If
     * none is found, create one and associate it with the {@link CallerContext}
     * .
     * 
     * @param cc
     *            The {@link CallerContext} for which to lookup/create
     *            {@link CCData}.
     * @return The found/created {@link CCData}.
     */
    private synchronized CCData getCCData(CallerContext cc)
    {
        // Lookup the CCData instance for the caller context cc.
        CCData data = (CCData) cc.getCallbackData(ccDataKey);

        // If a CCData object has not yet been assigned to this CallerContext,
        // then allocate one and add this CallerContext to ccList.
        if (data == null)
        {
            data = new CCData();
            cc.addCallbackData(data, ccDataKey);
            synchronized (ccListSync)
            {
                ccList = CallerContext.Multicaster.add(ccList, cc);
            }
        }
        return data;
    }

    public void addListenerOnce(EventListener listener)
    {
        CCData data = getCCData(ccMgr.getCurrentContext());
        synchronized (data.lock)
        {
            data.listeners = EventListMulticaster.addOnce(data.listeners, listener);
        }
    }

    public void addListenerMulti(EventListener listener)
    {
        CCData data = getCCData(ccMgr.getCurrentContext());
        synchronized (data.lock)
        {
            data.listeners = EventListMulticaster.addMulti(data.listeners, listener);
        }
    }

    public void removeListener(EventListener listener)
    {
        CCData data = getCCData(ccMgr.getCurrentContext());
        synchronized (data.lock)
        {
            data.listeners = EventListMulticaster.remove(data.listeners, listener);
        }
    }

    /*
     * 
     * Event Delivery
     */

    /**
     * Deliver an {@link EventObject} to all {@link EventListener}s registered
     * with this {@link CallerContextEventMulticaster}. Events are delivered on
     * a thread belonging to the {@link CallerContext} under which each
     * {@link EventLister} was registered.
     * 
     * @param event
     *            The {@link EventObject} to be delivered.
     * @param dispatcher
     *            The {@link EventDispatcher} that will be used to deliver the
     *            event.
     */
    public final void multicast(EventObject event)
    {
        // Send event to all listeners, with event delivery occurring on the
        // CallerContext
        // of the regsitered listeners.
        CallerContext ccListCopy = ccList;
        if (ccListCopy != null)
        {
            // Execute the runnable in each caller context in ccList
            ccListCopy.runInContext(new CCEventDispatcher(event));
        }
    }

    /**
     * This {@link Runnable} delivers an {@link EventObject} to all
     * {@link EventListener}s that are registered for a single
     * {@link CallerContext}.
     */
    class CCEventDispatcher implements Runnable
    {
        private final EventObject event;

        CCEventDispatcher(final EventObject event)
        {
            this.event = event;
        }

        public void run()
        {
            // Get the CCData associated with the CallerContext, using the
            // Player as a key.
            CallerContext cc = ccMgr.getCurrentContext();
            CCData data = (CCData) cc.getCallbackData(ccDataKey);

            // If there is no CCData, just return. (This shouldn't happen: It
            // wouldn't be
            // in the ccList if it didn't have registered CCData. But check just
            // to be safe.)
            if (data == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Failed to find CCData for key " 
                             + ((ccDataKey != null) ? ccDataKey.toString() : "null")
                             + " attempting to multicast " + event);
                }
                return;
            }

            // Get the EventListeners from the CCData and call dispatch() on the
            // list as a whole.
            // If any unchecked exceptions are thrown, catch them and log them.
            EventListener listeners = data.listeners;
            Throwable error = null;
            try
            {
                if (listeners != null) dispatch(listeners, event);
            }
            catch (Throwable e)
            {
                error = e;
            }

            if (error != null)
            {
                String msg = "Unchecked exception during dispatch(" + event + "): " + error;
                if (log.isWarnEnabled())
                {
                    log.warn(msg);
                }
                SystemEventUtil.logUncaughtException(error);
            }
        }
    }

    protected abstract void dispatch(EventListener listeners, EventObject event);
}

/**
 * This class is needed to expose protected methods of {@link EventMulticaster}
 * that are required by {@link CallerContextEventMulticaster}.
 */
class EventListMulticaster extends EventMulticaster
{
    /**
     * Construct a {@link CallerContextEventMulticaster} from two
     * {@link EventListener}s.
     */
    EventListMulticaster(EventListener a, EventListener b)
    {
        super(a, b);
    }

    static EventListener addOnce(EventListener a, EventListener b)
    {
        return addOnceInternal(a, b);
    }

    static EventListener addMulti(EventListener a, EventListener b)
    {
        return addInternal(a, b);
    }

    static EventListener remove(EventListener list, EventListener toRemove)
    {
        return removeInternal(list, toRemove);
    }
}
