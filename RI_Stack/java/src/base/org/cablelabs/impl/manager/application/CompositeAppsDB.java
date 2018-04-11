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

package org.cablelabs.impl.manager.application;

import java.util.Enumeration;
import java.util.Vector;

import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseEvent;
import org.dvb.application.AppsDatabaseEventListener;
import org.dvb.application.AppsDatabaseFilter;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.EventMulticaster;

/**
 * A <code>CompositeAppsDB</code> combines multiple <code>AppsDatabase</code>
 * instances together to create one <i>composite</i> <code>AppsDatabase</code>.
 * 
 * The purpose of the <code>CompositeAppsDB</code> is to support OCAP Monitor
 * Applications with {@link MonitorAppPermission MonitorAppPermission("")}. Such
 * applications are able to see <i>all</i> applications that may be run on the
 * host device, not just those within its own <code>ServiceContext</code>.
 * 
 * @author Aaron Kamienski
 * 
 * @see "OCAP-1.0 10.2.2.3 Application Signaling and Lifecycle"
 */
class CompositeAppsDB extends AppsDatabase
{
    /**
     * Creates an instance of CompositeAppsDB.
     * 
     * @see #composite
     * @see #listener
     */
    public CompositeAppsDB()
    {
        // Empty
        // instance variables are initialized in-line as part of declaration
    }

    /**
     * This method adds the given <code>AppsDatabase</code> to this composite
     * <code>AppsDatabase</code>. After being added, the given database may be
     * consulted in response to calls to <code>AppsDatabase</code> methods on
     * this instance.
     * <p>
     * Adding a given <code>AppsDatabase</code> more than once has no effect. In
     * other words, only one call to <code>removeAppsDatabase()</code> is be
     * necessary, even given multiple calls.
     * <p>
     * A {@link AppsDatabaseEvent#NEW_DATABASE NEW_DATABASE} event is implicitly
     * generated and sent to all currently listening listeners.
     * 
     * @param add
     *            database to add
     * @see #removeAppsDatabase(AppsDatabase)
     */
    void addAppsDatabase(AppsDatabase add)
    {
        synchronized (lock)
        {
            composite = Multicaster.add(composite, add);
            // register via compositeAppsDB-specific method (will be notified of
            // all events)
            ((AppDomainImpl) add).addCompositeDbAppsDatabaseEventListener(listener);
        }
    }

    /**
     * This method removes the given <code>AppsDatabase</code> from this
     * composite <code>AppsDatabase</code>. After being removed, the given
     * database will not be consulted in response to calls to
     * <code>AppsDatabase</code> methods on this instance.
     * <p>
     * Only one call to <code>removeAppsDatabase()</code> is necessary,
     * regardless of the number of times that <code>addAppsDatabase()</code> was
     * made.
     * <p>
     * A {@link AppsDatabaseEvent#NEW_DATABASE NEW_DATABASE} event is implicitly
     * generated and sent to all currently listening listeners.
     * 
     * @param old
     *            database to remove
     * @see #addAppsDatabase(AppsDatabase)
     */
    void removeAppsDatabase(AppsDatabase old)
    {
        boolean removed;
        synchronized (lock)
        {
            AppsDatabase oldComposite = composite;
            composite = Multicaster.remove(composite, old);
            old.removeListener(listener);
            removed = composite != oldComposite;
        }

        if (removed) listener.notifyEvent(new AppsDatabaseEvent(AppsDatabaseEvent.NEW_DATABASE, null, this));
    }

    // Comment copied from AppsDatabase
    public void addListener(AppsDatabaseEventListener l)
    {
        listener.addListener(l);
    }

    // Comment copied from AppsDatabase
    public void removeListener(AppsDatabaseEventListener l)
    {
        listener.removeListener(l);
    }

    /**
     * Implements
     * {@link AppsDatabase#getAppAttributes(org.dvb.application.AppID)},
     * returning an instance of <code>AppAttributes</code> for the given
     * <code>AppID</code>. The component <code>AppsDatabase</code>s are
     * searched, and the first non-<code>null</code> instance is returned.
     * 
     * @return first found <code>AppAttributes</code> corresponding to the given
     *         <code>AppID</code>; <code>null</code> if no such application is
     *         known by any of the component <code>AppsDatabase</code>s
     */
    public AppAttributes getAppAttributes(AppID key)
    {
        return composite.getAppAttributes(key);
    }

    /**
     * Implements
     * {@link AppsDatabase#getAppAttributes(org.dvb.application.AppsDatabaseFilter)}
     * , returning an <code>Enumeration</code> of all <code>AppAttributes</code>
     * maintained by all component <code>AppsDatabase</code>s according to the
     * given filter.
     * 
     * @return an <code>Enumeration</code> of all <code>AppAttribute</code>s
     *         maintained by all component <code>AppsDatabase</code>s according
     *         to the given filter
     */
    public Enumeration getAppAttributes(AppsDatabaseFilter filter)
    {
        return composite.getAppAttributes(filter);
    }

    /**
     * Implements
     * {@link AppsDatabase#getAppIDs(org.dvb.application.AppsDatabaseFilter)},
     * returning an <code>Enumeration</code> of all <code>AppID</code>s
     * maintained by all component <code>AppsDatabase</code>s according to the
     * given filter.
     * 
     * @return an <code>Enumeration</code> of all <code>AppID</code>s maintained
     *         by all component <code>AppsDatabase</code>s according to the
     *         given filter
     */
    public Enumeration getAppIDs(final AppsDatabaseFilter filter)
    {
        return composite.getAppIDs(filter);
    }

    /**
     * Implements {@link AppsDatabase#getAppProxy(org.dvb.application.AppID)},
     * returning an instance of <code>AppProxy</code> for the given
     * <code>AppID</code>. The component <code>AppsDatabase</code>s are
     * searched, and the first non-<code>null</code> instance is returned.
     * <p>
     * The <code>AppProxy</code> can be used to control an instance of the
     * application running within the original <code>ServiceContext</code>.
     * 
     * @return first found <code>AppAttributes</code> corresponding to the given
     *         <code>AppID</code>; <code>null</code> if no such application is
     *         known by any of the component <code>AppsDatabase</code>s
     */
    public AppProxy getAppProxy(AppID key)
    {
        return composite.getAppProxy(key);
    }

    /**
     * Implements {@link AppsDatabase#size()}, returning the total number of
     * applications currently known by all component <code>AppsDatabase</code>s.
     * 
     * @return the total number of applications currently known by all component
     *         <code>AppsDatabase</code>s.
     */
    public int size()
    {
        return composite.size();
    }

    /**
     * The component <code>AppsDatabase</code>(s). May be an instance of
     * {@link Multicaster}, which is a composition of 2 or more
     * <code>AppsDatabase</code>s.
     */
    private volatile AppsDatabase composite = new NullDB();

    /**
     * Manages the set of <i>AppsDatabaseEventListener</code>s added to this
     * <code>CompositeAppsDB</code>.
     */
    private final Listener listener = new Listener();

    /**
     * The private <code>Object</code> that is used for synchronization.
     */
    private Object lock = listener;

    /**
     * An empty <code>AppsDatabase</code>.
     * <p>
     * An instance of this is used to ensure that the
     * <code>CompositeAppsDB</code> is always composed of <i>at least</code> one
     * <code>AppsDatabase</code>. Theoretically, the
     * <code>CompositeAppsDB</code> should <i>never</i> be composed of less than
     * at least one real <code>AppsDatabase</code> (as any app that is accessing
     * the <code>CompositeAppsDB</code> must itself be running within a domain
     * that includes a <i>real</i> <code>AppsDatabase</code>).
     * 
     * @author Aaron Kamienski
     */
    private static class NullDB extends AppsDatabase
    {
        public void addListener(AppsDatabaseEventListener listener)
        {
            // no-op
        }

        public void removeListener(AppsDatabaseEventListener listener)
        {
            // no-op
        }

        public AppAttributes getAppAttributes(AppID key)
        {
            return null;
        }

        public Enumeration getAppAttributes(AppsDatabaseFilter filter)
        {
            return empty.elements();
        }

        public Enumeration getAppIDs(AppsDatabaseFilter filter)
        {
            return empty.elements();
        }

        public AppProxy getAppProxy(AppID key)
        {
            return null;
        }

        public int size()
        {
            return 0;
        }

        private Vector empty = new Vector();
    }

    /**
     * A composition of two (or more!) <code>AppsDatabase</code>s. This function
     * similar to an {@link org.cablelabs.impl.util.EventMulticaster event
     * multicaster}, providing efficient invocation and type safety
     * simultaneously.
     * 
     * @author Aaron Kamienski
     * 
     * @see #add(AppsDatabase, AppsDatabase)
     * @see #remove(AppsDatabase, AppsDatabase)
     */
    private static class Multicaster extends AppsDatabase
    {
        private Multicaster(AppsDatabase a, AppsDatabase b)
        {
            this.a = a;
            this.b = b;
        }

        /**
         * Returns a new <code>AppsDatabase</code> composed of the given
         * <code>AppsDatabase</code>s <i>a</i> and <i>b</i>. If either is
         * <code>null</code>, then the other is returned.
         * 
         * @param a
         * @param b
         * @return a composition of <i>a</i> and <i>b</i>
         */
        public static AppsDatabase add(AppsDatabase a, AppsDatabase b)
        {
            if (a == null || contains(b, a)) return b;
            if (b == null || contains(a, b)) return a;
            return new Multicaster(a, b);
        }

        /**
         * Returns a new <code>AppsDatabase</code> that is not composed of the
         * given <i>old</i> instance.
         * 
         * @param db
         *            the current composition
         * @param old
         *            the <code>AppsDatabase</code> to remove
         * @return a new composition, or <code>null</code> if
         *         <code>db==old</code>
         */
        public static AppsDatabase remove(AppsDatabase db, AppsDatabase old)
        {
            if (db == old || db == null)
            {
                return null;
            }
            else if (db instanceof Multicaster)
            {
                return ((Multicaster) db).remove(old);
            }
            else
            {
                return db;
            }
        }

        /**
         * Determines if <i>multi</i> is considered to contain <i>single</i>.
         * This will return <code>true</code> if <code>multi == single</code> or
         * <code>multi instanceof Multicaster</code> and
         * {@link #contains(AppsDatabase) multi.contains(single)}. Otherwise,
         * <code>false</code> is returned.
         * 
         * @param multi
         *            the AppsDatabase/Multicaster to search
         * @param single
         *            the AppsDatabase to search for
         * @return <code>true</code> if <i>multi</i> is considered to
         *         <i>contain</i> <i>single</i>, <code>false</code> otherwise
         */
        private static boolean contains(AppsDatabase multi, AppsDatabase single)
        {
            if (multi == single)
                return true;
            else if (multi != null && multi instanceof Multicaster)
                return ((Multicaster) multi).contains(single);
            else
                return false;
        }

        /**
         * Determine if this <code>Multicaster</code> contains the given
         * <i>db</i>. This will return <code>true</code> if any of the following
         * are true:
         * <ul>
         * <li> <code>this.a == db</code>
         * <li> <code>this.b == db</code>
         * <li> <code>contains(this.a, db)</code>
         * <li> <code>contains(this.b, db)</code>
         * </ul>
         * Otherwise, <code>false</code> is returned.
         * 
         * @param db
         *            entry to search for
         * @return <code>true</code> if <code>this</code> <i>contains</i>
         *         <i>db</i>; <code>false</code> otherwise
         */
        private boolean contains(AppsDatabase db)
        {
            return a == db || b == db || contains(a, db) || contains(b, db);
        }

        /**
         * Private <i>remove</i> method that returns a new composition, similar
         * to <code>this</code>, except with <i>old</i> removed.
         * 
         * @param old
         *            component to remove
         * @return a new composition, minus <i>old</i>
         */
        private AppsDatabase remove(AppsDatabase old)
        {
            if (old == a) return b;
            if (old == b) return a;
            AppsDatabase a2 = remove(a, old);
            AppsDatabase b2 = remove(b, old);
            if (a2 == a && b2 == b)
            {
                return this;
            }
            return add(a2, b2);
        }

        // Description copied from AppsDatabase
        public Enumeration getAppAttributes(AppsDatabaseFilter filter)
        {
            return new CompositeEnum(a.getAppAttributes(filter), b.getAppAttributes(filter));
        }

        // Description copied from AppsDatabase
        public Enumeration getAppIDs(AppsDatabaseFilter filter)
        {
            return new CompositeEnum(a.getAppIDs(filter), b.getAppIDs(filter));
        }

        // Description copied from AppsDatabase
        public AppProxy getAppProxy(AppID key)
        {
            AppProxy app = null;
            if ((app = a.getAppProxy(key)) == null) app = b.getAppProxy(key);
            return app;
        }

        // Description copied from AppsDatabase
        public AppAttributes getAppAttributes(AppID key)
        {
            AppAttributes app = null;
            if ((app = a.getAppAttributes(key)) == null) app = b.getAppAttributes(key);
            return app;
        }

        // Description copied from AppsDatabase
        public int size()
        {
            return a.size() + b.size();
        }

        private final AppsDatabase a;

        private final AppsDatabase b;
    }

    /**
     * Implementation of an <code>Enumeration</code> that is actually a
     * composition of two <code>Enumeration</code>s. By composing
     * <code>CompositeEnum</code>s, a single <code>Enumeration</code> can be
     * created from an arbitrary number of <code>Enumeration</code>s.
     * <p>
     * Note that stack usage grows each time the number of <i>leaf</i>
     * <code>Enumeration</code>s doubles.
     * 
     * @author Aaron Kamienski
     */
    private static class CompositeEnum implements Enumeration
    {
        /**
         * Creates a <code>CompositeEnum</code>, composed of the given
         * <code>Enumeration</code>s.
         * 
         * @param a
         *            enumeration
         * @param b
         *            enumeration
         */
        CompositeEnum(Enumeration a, Enumeration b)
        {
            e = a;
            extra = b;
        }

        // Description copied from Enumeration
        public boolean hasMoreElements()
        {
            if (e.hasMoreElements())
                return true;
            else
            {
                next();
                return e.hasMoreElements();
            }
        }

        // Description copied from Enumeration
        public Object nextElement()
        {
            if (!e.hasMoreElements()) next();
            return e.nextElement();
        }

        /**
         * Advances to the next available <code>Enumeration</code>. If there
         * isn't an available one, sticks with the current one.
         */
        private void next()
        {
            if (extra != null)
            {
                e = extra;
                extra = null;
            }
        }

        /**
         * The current <code>Enumeration</code>.
         */
        private Enumeration e;

        /**
         * The next <code>Enumeration</code>, if there is one.
         */
        private Enumeration extra;
    }

    /**
     * An instance of this class is used to track each component
     * <code>AppsDatabase</code>. This class implements
     * <code>AppsDatabaseEventListener</code> so that it can be notified of
     * changes in the component apps database. In addition, this class manages
     * the set of <code>AppsDatabaseEventListener</code>s added to the
     * <code>CompositeAppsDB</code> instance.
     * 
     * @author Aaron Kamienski
     * 
     * @see CompositeAppsDB#addListener
     * @see CompositeAppsDB#removeListener
     */
    private class Listener implements AppsDatabaseEventListener
    {
        /**
         * Implements {@link CompositeAppsDB#addListener}. Listeners are added
         * to the caller's <code>CallerContext</code>. The caller's
         * <code>CallerContext</code> is added to a list of contexts maintained
         * as a {@link CallerContext.Multicaster}.
         * 
         * @param l
         *            listener to add
         */
        public synchronized void addListener(AppsDatabaseEventListener l)
        {
            // Get current context
            CallerContext ctx = getCurrentContext();

            // Listeners are maintained in-context
            Data data = getData(ctx, true);

            // Update listener/multicaster
            data.listeners = EventMulticaster.add(data.listeners, l);

            // Manage context/multicaster
            ccList = CallerContext.Multicaster.add(ccList, ctx);
        }

        /**
         * Implements {@link CompositeAppsDB#removeListener}. Listeners are
         * removed from the caller's <code>CallerContext</code>. The caller's
         * <code>CallerContext</code> remains as part of the list of contexts
         * maintained until the calling application is
         * {@link CallbackData#destroy(CallerContext) destroyed}.
         * 
         * @param l
         *            listener to remove
         */
        public synchronized void removeListener(AppsDatabaseEventListener l)
        {
            // Listeners are maintained in-context
            Data data = getData(false);

            // Remove the given listener from the set of listeners
            if (data != null && data.listeners != null)
            {
                data.listeners = EventMulticaster.remove(data.listeners, l);
            }
        }

        /**
         * Translates the given <code>AppsDatabase</code> (replacing the source
         * with this instance of <code>CompositeAppsDB</code> and forwards the
         * event on to listeners of the composite database.
         * 
         * @param e
         *            event to be translated and forwarded
         */
        public void newDatabase(AppsDatabaseEvent e)
        {
            notifyEvent(updateEvent(e));
        }

        /**
         * Translates the given <code>AppsDatabase</code> (replacing the source
         * with this instance of <code>CompositeAppsDB</code> and forwards the
         * event on to listeners of the composite database.
         * 
         * @param e
         *            event to be translated and forwarded
         */
        public void entryRemoved(AppsDatabaseEvent e)
        {
            notifyEvent(updateEvent(e));
        }

        /**
         * Translates the given <code>AppsDatabase</code> (replacing the source
         * with this instance of <code>CompositeAppsDB</code> and forwards the
         * event on to listeners of the composite database.
         * 
         * @param e
         *            event to be translated and forwarded
         */
        public void entryAdded(AppsDatabaseEvent e)
        {
            notifyEvent(updateEvent(e));
        }

        /**
         * Translates the given <code>AppsDatabase</code> (replacing the source
         * with this instance of <code>CompositeAppsDB</code> and forwards the
         * event on to listeners of the composite database.
         * 
         * @param e
         *            event to be translated and forwarded
         */
        public void entryChanged(AppsDatabaseEvent e)
        {
            notifyEvent(updateEvent(e));
        }

        /**
         * Creates a new <code>AppsDatabaseEvent</code> just like the given one,
         * but updated so that the source is this <code>CompositeAppsDB</code>.
         * 
         * @param e
         *            event to update
         * @return new instance of the given event with a different
         *         <i>source</i>
         */
        private AppsDatabaseEvent updateEvent(AppsDatabaseEvent e)
        {
            return new AppsDatabaseEvent(e.getEventId(), e.getAppID(), CompositeAppsDB.this);
        }

        /**
         * Notifies all installed listeners of the given event.
         * 
         * @param e
         *            <code>AppsDatabaseEvent</code> with this
         *            <code>CompositeAppsDB</code> as the source
         * 
         * @see #updateEvent
         */
        void notifyEvent(final AppsDatabaseEvent e)
        {
            CallerContext cc = ccList;
            if (cc == null) return;
            cc.runInContext(new Runnable()
            {
                public void run()
                {
                    // get ccdata (if there is one)
                    Data data = getData(false);

                    // Invoke listeners.
                    AppsDatabaseEventListener listeners;
                    if (data != null && (listeners = data.listeners) != null)
                    {
                        switch (e.getEventId())
                        {
                            case AppsDatabaseEvent.APP_ADDED:
                                listeners.entryAdded(e);
                                break;
                            case AppsDatabaseEvent.APP_CHANGED:
                                listeners.entryChanged(e);
                                break;
                            case AppsDatabaseEvent.APP_DELETED:
                                listeners.entryRemoved(e);
                                break;
                            case AppsDatabaseEvent.NEW_DATABASE:
                                listeners.newDatabase(e);
                                break;
                        }
                    }
                }
            });
        }

        /**
         * Access this object's global data object associated with the current
         * context. If none is assigned, then one is created.
         * 
         * @param create
         *            if <code>true</code> create a <code>Data</code> object if
         *            one doesn't exist
         * @return the <code>Data</code> object
         */
        private synchronized Data getData(boolean create)
        {
            return getData(getCurrentContext(), create);
        }

        /**
         * Returns the current/calling <code>CallerContext</code>.
         * 
         * @return the current/calling <code>CallerContext</code>
         */
        private CallerContext getCurrentContext()
        {
            return ((CallerContextManager) ManagerManager.getInstance(CallerContextManager.class)).getCurrentContext();
        }

        /**
         * Access this object's global data object associated with the given
         * context. If none is assigned, then one is created.
         * 
         * @param ctx
         *            the given <code>CallerContext</code>
         * @param create
         *            if <code>true</code> create a <code>Data</code> object if
         *            one doesn't exist
         * @return the <code>Data</code> object
         */
        private synchronized Data getData(CallerContext ctx, boolean create)
        {
            Data data = (Data) ctx.getCallbackData(this);
            if (data == null && create)
            {
                data = new Data();
                ctx.addCallbackData(data, this);
            }
            return data;
        }

        /**
         * Cause the applications database to <i>forget</i> all listeners
         * associated with this <code>CallerContext</code>. This is done simply
         * by setting the reference to <code>null</code> and letting the garbage
         * collector take care of the rest.
         * <p>
         * This is <i>package private</i> for testing purposes.
         * 
         * @param cc
         *            the <code>CallerContext</code> to forget
         */
        private synchronized void forgetListeners(CallerContext cc)
        {
            // Simply forget the given cc
            // No harm done if never added
            cc.removeCallbackData(this);
            ccList = CallerContext.Multicaster.remove(ccList, cc);
        }

        /**
         * Per-context global data. Remembers per-context
         * <code>AppsDatabaseListener</code>s.
         * 
         * @author Aaron Kamienski
         */
        private class Data implements CallbackData
        {
            public AppsDatabaseEventListener listeners;

            public void destroy(CallerContext cc)
            {
                forgetListeners(cc);
            }

            public void active(CallerContext cc)
            { /* empty */
            }

            public void pause(CallerContext cc)
            { /* empty */
            }
        }

        /**
         * List of <code>CallerContext</code>s that have added listeners.
         * 
         * @see #addListener(AppsDatabaseEventListener)
         * @see #getData(CallerContext, boolean)
         * @see #notifyEvent(AppsDatabaseEvent)
         */
        private CallerContext ccList;
    }
}
