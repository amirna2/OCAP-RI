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

/*
 * SystemEventManager.java
 */

package org.ocap.system.event;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.ExtendedSystemEventManager;
import org.cablelabs.impl.util.SecurityUtil;

import org.apache.log4j.Logger;
import org.ocap.system.MonitorAppPermission;

/**
 * Registration mechanism for trusted applications to set the error handler.
 * 
 * @author Aaron Kamienski
 */
public abstract class SystemEventManager
{
    /**
     * Identifies the system error event listener.
     * 
     * @see #setEventListener(int, org.ocap.system.event.SystemEventListener)
     */
    public final static int ERROR_EVENT_LISTENER = 0x0;

    /**
     * Identifies the reboot event listener.
     * 
     * @see #setEventListener(int,org.ocap.system.event.SystemEventListener)
     */
    public final static int REBOOT_EVENT_LISTENER = 0x1;

    /**
     * Identifies the system resource depletion event listener.
     * 
     * @see #setEventListener(int,org.ocap.system.event.SystemEventListener)
     */
    public final static int RESOURCE_DEPLETION_EVENT_LISTENER = 0x2;

    /**
     * Identifies the deferred download event listener.
     * 
     * @see #setEventListener(int, org.ocap.system.event.SystemEventListener)
     */
    public final static int DEFERRED_DOWNLOAD_EVENT_LISTENER = 0X03;

    /**
     *  Identifies the CableCARD reset event listener.
     * @see #setEventListener(int, org.ocap.system.event.SystemEventListener)
     */
    public final static int CABLE_CARD_EVENT_LISTENER = 0X04;
    
    /**
     * This constructor must not be used by OCAP applications. It is only
     * provided for implementors of the OCAP APIs.
     */

    protected SystemEventManager()
    {
        // Empty
    }

    /**
     * Gets the singleton instance of the system event manager.
     * 
     * @return The system event manager instance.
     */
    public static SystemEventManager getInstance()
    {
        return singleton;
    }

    /**
     * Set the system event listener specified by type and the new handler. On a
     * successful call, any previously set SystemEventListener for the same type
     * is discarded. By default no SystemEventListener is set for any type.
     * 
     * @param type
     *            - {@link #ERROR_EVENT_LISTENER},
     *            {@link #REBOOT_EVENT_LISTENER}, or
     *            {@link #RESOURCE_DEPLETION_EVENT_LISTENER}, or
     *            {@link #DEFERRED_DOWNLOAD_EVENT_LISTENER}, or
     *            {@link #CABLE_CARD_EVENT_LISTENER}.
     * 
     * @param sel
     *            - System event listener created by the registering
     *            application.
     * 
     * @throws java.lang.SecurityException
     *             if the application does not have
     *             MonitorAppPermission("systemevent")
     * @throws java.lang.IllegalArgumentException
     *             if type is not one of {@link #ERROR_EVENT_LISTENER},
     *             {@link #REBOOT_EVENT_LISTENER}, or
     *             {@link #RESOURCE_DEPLETION_EVENT_LISTENER}, or
     *             {@link #DEFERRED_DOWNLOAD_EVENT_LISTENER}, or
     *             {@link #CABLE_CARD_EVENT_LISTENER}.
     */
    public abstract void setEventListener(int type, SystemEventListener sel) throws IllegalArgumentException;

    /**
     * Unset the system event handler specified by type.
     * 
     * @param type
     *            - One of {@link #ERROR_EVENT_LISTENER},
     *            {@link #REBOOT_EVENT_LISTENER}, or
     *            {@link #RESOURCE_DEPLETION_EVENT_LISTENER}, or
     *            {@link #DEFERRED_DOWNLOAD_EVENT_LISTENER}, or
     *            {@link #CABLE_CARD_EVENT_LISTENER}.
     * 
     * @throws java.lang.SecurityException
     *             if the application does not have
     *             MonitorAppPermission("systemevent")
     */
    public abstract void unsetEventListener(int type);

    /**
     * Logs an event. Checks the instance of the event and calls the appropriate
     * error, reboot, or resource depletion handler and passes the even to it.
     * 
     * @param event
     *            - The event to log.
     * 
     * @throws java.lang.IllegalArgumentException
     *             if the event parameter is an instance of an application
     *             defined class (i.e., applications cannot define their own
     *             subclasses of SystemEvent and use them with this method. This
     *             is due to implementation and security issues).
     */
    public abstract void log(SystemEvent event) throws IllegalArgumentException;

    /**
     * Singleton instance of <code>SystemEventManager</code>.
     */
    private static final SystemEventManager singleton = new SysEventMgrImpl();
}

/**
 * Implementation of <code>SystemEventManager</code>.
 * 
 * @author Aaron Kamienski
 */
class SysEventMgrImpl extends SystemEventManager implements ExtendedSystemEventManager
{
    // Description copied from SystemEventManager
    public void setEventListener(int type, SystemEventListener sel) throws SecurityException, IllegalArgumentException
    {
        checkPermission();

        // Validate the type
        switch (type)
        {
            case ERROR_EVENT_LISTENER:
            case REBOOT_EVENT_LISTENER:
            case RESOURCE_DEPLETION_EVENT_LISTENER:
            case DEFERRED_DOWNLOAD_EVENT_LISTENER:
            case CABLE_CARD_EVENT_LISTENER:
                break;
            default:
                throw new IllegalArgumentException("Unrecognized type: " + type);
        }

        // Replace the previous installation
        synchronized (lock)
        {
            listeners[type] = (sel == null) ? null : (new ListenerContext(sel));
        }
    }

    // Description copied from SystemEventManager
    public void unsetEventListener(int type) throws SecurityException
    {
        checkPermission();

        // Validate the type
        switch (type)
        {
            case ERROR_EVENT_LISTENER:
            case REBOOT_EVENT_LISTENER:
            case RESOURCE_DEPLETION_EVENT_LISTENER:
            case DEFERRED_DOWNLOAD_EVENT_LISTENER:
            case CABLE_CARD_EVENT_LISTENER:
                break;
            default:
                return;
        }

        // Simply forget the previous installation
        synchronized (lock)
        {
            listeners[type] = null;
        }
    }

    // Description copied from SystemEventManager
    public void log(SystemEvent event) throws IllegalArgumentException
    {
        // Simply check for same class loader
        if (event.getClass().getClassLoader() != getClass().getClassLoader())
        {
            throw new IllegalArgumentException("Application-defined SystemEvents aren't accepted");
        }

        logBySeverity(event);

        ListenerContext listener = listeners[getType(event)];
        if (listener != null) listener.notifyEvent(event);
    }

    /**
     * Implements {@link ExtendedSystemEventManager#log}.
     */
    public int log(SystemEvent event, long timeout) throws IllegalArgumentException
    {
        // Simply check for same class loader
        if (event.getClass().getClassLoader() != getClass().getClassLoader())
        {
            throw new IllegalArgumentException("Application-defined SystemEvents aren't accepted");
        }

        logBySeverity(event);

        ListenerContext listener = listeners[getType(event)];
        return (listener == null) ? NOLISTENER // no timeout because nobody to
                                               // invoke
                : listener.notifyEvent(event, timeout);
    }

    /**
     * Log a SystemEvent to log4j based on the event typeCode.
     * 
     * The event to log4j mapping is as follows: 
     * 
     * event code --> log4j level 
     * sys info --> info 
     * app info --> info 
     * sys recoverable error --> warn 
     * app recoverable error --> warn 
     * sys catastrophic error --> error 
     * app catastrophic error --> error 
     * reboot --> info 
     * system resource depletion --> warn 
     * download event --> info
     * 
     * @param event
     */
    protected void logBySeverity(SystemEvent event)
    {
        int code = event.getTypeCode();

        if (code >= SystemEvent.BEGIN_SYS_REC_ERROR_EVENT_TYPES 
                && code <= SystemEvent.END_APP_REC_ERROR_EVENT_TYPES)
        {
            // system or app recoverable
            if (log.isWarnEnabled())
            {
                log.warn(toString(event));
            }
        }
        else if (code >= SystemEvent.BEGIN_SYS_CAT_ERROR_EVENT_TYPES
                && code <= SystemEvent.END_APP_CAT_ERROR_EVENT_TYPES)
        {
            // non-recoverable catastrophic event
            if (log.isErrorEnabled())
            {
                log.error(toString(event));
            }
        }
        else if (code >= SystemEvent.BEGIN_SYS_RES_DEP_EVENT_TYPES 
                && code <= SystemEvent.END_SYS_RES_DEP_EVENT_TYPES)
        {
            // resource depletion
            if (log.isWarnEnabled())
            {
                log.warn(toString(event));
            }
        }
        else if (code >= SystemEvent.BEGIN_SYS_CABLECARD_RESET_EVENT_TYPES 
                 && code <= SystemEvent.END_SYS_CABLECARD_RESET_EVENT_TYPES)
        {
            // cable card reset 
            if (log.isWarnEnabled())
            {
                log.warn(toString(event));
            }
        }
        else
        {
            // everything else is an info level, including:
            // BEGIN_SYS_INFO_EVENT_TYPES - END_SYS_INFO_EVENT_TYPES
            // BEGIN_APP_INFO_EVENT_TYPES - END_APP_INFO_EVENT_TYPES
            // BEGIN_SYS_REBOOT_EVENT_TYPES - END_SYS_REBOOT_EVENT_TYPES
            // BEGIN_SYS_DNLD_EVENT_TYPES - END_SYS_DNLD_EVENT_TYPES
            // BEGIN_SYS_CABLECARD_RESET_EVENT_TYPES - END_SYS_CABLECARD_RESET_EVENT_TYPES
            if (log.isInfoEnabled())
            {
                log.info(toString(event));
            }
        }
    }

    /**
     * Generates a string representation of the given event This is used because
     * the {@link SystemEvent} class and subclasses are not specified to
     * overrided {@link Object#toString()} with something useful.
     * 
     * @param e
     *            the given event
     * @return a string representation of the given event
     */
    private String toString(SystemEvent e)
    {
        return e.privToString();
    }

    /**
     * Invokes the <code>SecurityManager</code> to determine if the caller has
     * <code>MonitorAppPermission("systemevent")</code>.
     * 
     * @throws java.lang.SecurityException
     *             if the application does not have
     *             MonitorAppPermission("systemevent")
     */
    private static void checkPermission() throws SecurityException
    {
        SecurityUtil.checkPermission(PERMISSION);
    }

    /**
     * Determines the type of listener to call for the given event.
     * 
     * @param e
     *            the event
     * @return One of {@link #ERROR_EVENT_LISTENER},
     *         {@link #REBOOT_EVENT_LISTENER}, or
     *         {@link #RESOURCE_DEPLETION_EVENT_LISTENER}
     */
    private static int getType(SystemEvent e)
    {
        if (e instanceof ErrorEvent)
            return ERROR_EVENT_LISTENER;
        else if (e instanceof RebootEvent)
            return REBOOT_EVENT_LISTENER;
        else if (e instanceof ResourceDepletionEvent)
            return RESOURCE_DEPLETION_EVENT_LISTENER;
        else if (e instanceof DeferredDownloadEvent)
            return DEFERRED_DOWNLOAD_EVENT_LISTENER;
        else if (e instanceof CableCARDResetEvent)
            return CABLE_CARD_EVENT_LISTENER;
        else
            throw new IllegalArgumentException("No listener type for " + e);
    }

    /**
     * Internal synchronization lock.
     */
    private Object lock = new Object();

    /**
     * Encapsulates a SystemEventListener and the corresponding CallerContext.
     */
    private volatile ListenerContext[] listeners = { null, null, null, null, null };

    /**
     * <code>MonitorAppPermission("systemevent")</code>.
     */
    private static final MonitorAppPermission PERMISSION = new MonitorAppPermission("systemevent");

    /**
     * Private logger.
     */
    private static final Logger log = Logger.getLogger(SystemEventManager.class.getName());

    /**
     * CallbackData object that performs cleanup of application-installed
     * listeners. This is separate from the <code>ListenerContext</code>
     * because, while there may be more than one listener per app, there should
     * be only one <code>Cleanup</code> object.
     * 
     * @author Aaron Kamienski
     */
    private class Cleanup implements CallbackData
    {
        /** Empty. */
        public void pause(CallerContext ctx)
        { /* EMPTY */
        }

        /** Empty. */
        public void active(CallerContext ctx)
        { /* EMPTY */
        }

        /**
         * Forget any installed listeners for the given
         * <code>CallerContext</code>.
         * 
         * @param ctx
         *            the context that is going away and should no longer be
         *            referenced
         */
        public void destroy(CallerContext ctx)
        {
            synchronized (lock)
            {
                for (int i = 0; i < listeners.length; ++i)
                {
                    // If listener is set and is cooresponds to given context
                    if (listeners[i] != null && listeners[i].context == ctx)
                    {
                        // Forget the listener
                        listeners[i] = null;
                    }
                }
            }
        }
    }

    /**
     * Encapsulates a SystemEventListener and the corresponding CallerContext.
     * 
     * @author Aaron Kamienski
     */
    private class ListenerContext
    {
        /**
         * Constructs a ListenerContext that encapsulates the given listener and
         * the current <code>CallerContext</code>.
         * 
         * @param listener
         *            the listener to encapsulate
         */
        ListenerContext(SystemEventListener listener)
        {
            this.listener = listener;
            this.context = getContext();

            // Make sure that there is a cleanup object...
            if (context.getCallbackData(Cleanup.class) == null) context.addCallbackData(new Cleanup(), Cleanup.class);
        }

        /**
         * Determines the caller context.
         * 
         * @return the calling context
         */
        private CallerContext getContext()
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

            return ccm.getCurrentContext();
        }

        /**
         * Invokes the listener's <code>notifyEvent()</code> method within the
         * associated <code>CallerContext</code>. This is <i>not</i> a blocking
         * calling. The event is logged to the listener's
         * <code>CallerContext</code> via
         * {@link CallerContext#runInContext(Runnable)}. As such it is generally
         * an asynchronous call, although the event is dispatched via the
         * context's main queue (as such events are delivered in-order).
         * 
         * @param event
         *            the event to dispatch
         */
        void notifyEvent(final SystemEvent event)
        {
            // Not a blocking call
            // But serialized on the main context queue
            try
            {
                context.runInContext(new Runnable()
                {
                    public void run()
                    {
                        listener.notifyEvent(event);
                    }
                });
            }
            catch (Throwable e)
            {
                // NOTE: this should not use
                // SystemEventUtil.logRecoverableError!!!
                if (log.isErrorEnabled())
                {
                    log.error("Problems logging event", e);
                }
        }
        }

        /**
         * Invokes the listener's <code>notifyEvent()</code> method within the
         * associated <code>CallerContext</code> and waits for completion or the
         * given timeout period to pass.
         * 
         * @param event
         *            the event to dispatch
         * @param timeout
         *            the period to wait in milliseconds
         * @return status of invocation (i.e., TIMEOUT or SUCCESS)
         */
        int notifyEvent(final SystemEvent event, long timeout)
        {
            final int[] finished = { TIMEOUT };
            try
            {
                synchronized (finished)
                {
                    // Execute asynchronously
                    context.runInContextAsync(new Runnable()
                    {
                        public void run()
                        {
                            try
                            {
                                listener.notifyEvent(event);
                            }
                            finally
                            {
                                // Signal completion w/out timeout
                                synchronized (finished)
                                {
                                    finished[0] = SUCCESS;
                                    finished.notify();
                                }
                            }
                        }
                    });
                    // Wait until signaled complete or timeout occurs
                    finished.wait(timeout);
                }
            }
            catch (Throwable e)
            {
                // NOTE: this should not use
                // SystemEventUtil.logRecoverableError!!!
                if (log.isErrorEnabled())
                {
                    log.error("Problems logging event", e);
                }
            }

            // return indication of status of invocation
            return finished[0];
        }

        /**
         * Encapsulated listener.
         */
        private SystemEventListener listener;

        /**
         * <code>CallerContext</code> associated with the encapsulated listener.
         */
        private CallerContext context;
    }
}
