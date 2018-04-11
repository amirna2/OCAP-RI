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

package org.cablelabs.impl.ocap.manager.eas;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.cablelabs.ocap.util.ConversionUtil;
import org.cablelabs.ocap.util.CountDownLatch;
import org.dvb.application.AppID;
import org.ocap.system.EASEvent;
import org.ocap.system.EASHandler;
import org.ocap.system.EASListener;
import org.ocap.system.EASManager;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * An abstract class representing the message processing state of an accepted
 * emergency alert message. Only one EA message can be presented at a time, and
 * the instance of this class provides the shared context of that message to the
 * subclasses representing the state transitions of that message.
 * 
 * @author Dave Beidle
 * @version $Revision$
 * @see EASStateNotInProgress
 * @see EASStateInProgress
 * @see EASStateReceived
 */
public abstract class EASState
{
    private static final Logger log = Logger.getLogger(EASState.class);

    /**
     * An instance of this class encapsulates an {@link EASHandler} and its
     * associated {@link CallerContext}. This class' methods invoke the
     * equivalent ones on the <code>EASHandler</code> but within in the given
     * caller context.
     */
    class EASHandlerContext implements CallbackData, EASHandler
    {
        private final CallerContext m_context;

        private final EASHandler m_handler;

        /**
         * Constructs a new instance of the receiver with the given parameters.
         * 
         * @param handler
         *            the registered <code>EASHandler</code> to notify of an
         *            alternative audio location for EAS presentation
         * @param context
         *            the <code>CallerContext</code> in which the handler
         *            functions
         */
        public EASHandlerContext(final EASHandler handler, final CallerContext context)
        {
            this.m_handler = handler;
            this.m_context = context;
            context.addCallbackData(this, EASHandlerContext.class);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.CallbackData#active(org.cablelabs.impl
         * .manager.CallerContext)
         */
        public void active(CallerContext callerContext)
        {
            // intentionally left empty
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.CallbackData#destroy(org.cablelabs.impl
         * .manager.CallerContext)
         */
        public void destroy(CallerContext callerContext)
        {
            unregisterEASHandler();
        }

        /**
         * Disposes of resources used by the receiver.
         */
        public void dispose()
        {
            this.m_context.removeCallbackData(EASHandlerContext.class);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.ocap.system.EASHandler#notifyPrivateDescriptor(byte[])
         */
        public boolean notifyPrivateDescriptor(final byte[] descriptor)
        {
            final boolean[] answer = { false };

            if (this.m_context.isAlive())
            {
                try
                { // runInContextSync(Runnable) used since we need to block on a
                  // response from the handler before proceeding
                    this.m_context.runInContextSync(new Runnable()
                    {
                        public void run()
                        {
                            answer[0] = EASHandlerContext.this.m_handler.notifyPrivateDescriptor(descriptor);
                        }
                    });
                }
                catch (InvocationTargetException e)
                {
                    Throwable t = e.getCause();
                    if (log.isWarnEnabled())
                    {
                        log.warn(formatLogMessage(EASState.getAlertStrategy().getMessage(), "EASHandler threw exception during private descriptor notification"), t);
                    }
                    SystemEventUtil.logUncaughtException(t);
                    // TODO: unregister EASHandler since it threw an exception?
                }
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn(formatLogMessage(EASState.getAlertStrategy().getMessage(), "EASHandler no longer 'alive' to handle private descriptor notifications"));
                }
                unregisterEASHandler();
            }

            return answer[0];
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.CallbackData#pause(org.cablelabs.impl.
         * manager.CallerContext)
         */
        public void pause(CallerContext callerContext)
        {
            // intentionally left empty
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.ocap.system.EASHandler#stopAudio()
         */
        public void stopAudio()
        {
            if (this.m_context.isAlive())
            { // runInContext(Runnable) used since we don't need to block and
              // wait for a response from the handler
                this.m_context.runInContext(new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            EASHandlerContext.this.m_handler.stopAudio();
                        }
                        catch (Exception e)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn(formatLogMessage(EASState.getAlertStrategy().getMessage(), "EASHandler threw exception during stop audio notification"), e);
                            }
                            SystemEventUtil.logUncaughtException(e);
                        }
                    }
                });
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn(formatLogMessage(EASState.getAlertStrategy().getMessage(), "EASHandler no longer 'alive' to handle stopAudio notifications"));
                }
                unregisterEASHandler();
            }
        }
    }

    /**
     * An instance of this class encapsulates a list of {@link EASListener}
     * references for a given host application ({@link CallerContext}).
     */
    class EASListenerCallback implements CallbackData, EASListener
    {
        /**
         * The {@link EventMulticaster} list of registered EAS listeners for a
         * given host application.
         */
        private volatile EASListener m_listeners;

        /**
         * Constructs a new instance of the receiver with the given parameter.
         * <p>
         * Note: a synchronization lock is held on
         * <code>EASState.s_listenerMutex</code> when this constructor is
         * invoked.
         * 
         * @param callerContext
         *            the <code>CallerContext</code> in which the listener
         *            functions
         */
        public EASListenerCallback(final CallerContext callerContext)
        {
            callerContext.addCallbackData(this, EASListenerCallback.class);
            EASState.s_callerContextList = CallerContext.Multicaster.add(EASState.s_callerContextList, callerContext);
            ++EASState.s_callerContextCount;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.CallbackData#active(org.cablelabs.impl
         * .manager.CallerContext)
         */
        public void active(CallerContext callerContext)
        {
            // intentionally left empty
        }

        /**
         * Adds the given listener for EAS events. Note that a separate list of
         * listeners is maintained for each host application.
         * <p>
         * Note: a synchronization lock is held on
         * <code>EASState.s_listenerMutex</code> when this method is invoked.
         * 
         * @param listener
         *            the {@link EASListener} implementation to be added
         */
        public void addListener(final EASListener listener)
        {
            this.m_listeners = EventMulticaster.add(this.m_listeners, listener);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.CallbackData#destroy(org.cablelabs.impl
         * .manager.CallerContext)
         */
        public void destroy(CallerContext callerContext)
        {
            synchronized (EASState.s_listenerMutexLock)
            {
                EASState.s_callerContextList = CallerContext.Multicaster.remove(EASState.s_callerContextList,
                        callerContext);
                --EASState.s_callerContextCount;
                callerContext.removeCallbackData(EASListenerCallback.class);
                this.m_listeners = null;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.ocap.system.EASListener#notify(org.ocap.system.EASEvent)
         */
        public void notify(EASEvent event)
        {
            if (null != this.m_listeners)
            {
                EASListener listeners = this.m_listeners;
                listeners.notify(event);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.CallbackData#pause(org.cablelabs.impl.
         * manager.CallerContext)
         */
        public void pause(CallerContext callerContext)
        {
            // intentionally left empty
        }

        /**
         * Removes the given listener from receiving EAS events. Note that a
         * separate list of listeners is maintained for each host application.
         * <p>
         * Note: a synchronization lock is held on
         * <code>EASState.s_listenerMutex</code> when this method is invoked.
         * 
         * @param listener
         *            the {@link EASListener} implementation to be removed
         */
        public void removeListener(final EASListener listener)
        {
            this.m_listeners = EventMulticaster.remove(this.m_listeners, listener);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.ocap.system.EASListener#warn(org.ocap.system.EASEvent)
         */
        public void warn(EASEvent event)
        {
            if (null != this.m_listeners)
            {
                EASListener listeners = this.m_listeners;
                listeners.warn(event);
            }
        }
    }

    // Class Constants

    public static final AppID INVALID_APP_ID = new AppID(0, 0);

    public static final String MPEENV_PRESENTATION_INTERRUPT_APPID = "OCAP.eas.presentation.interrupt.appId";

    public static final String MPEENV_WARNING_TIMEOUT = "OCAP.eas.warning.timeout";

    private static final String DEFAULT_PRESENTATION_INTERRUPT_APPID = "0x000000000000"; // same
                                                                                         // as
                                                                                         // INVALID_APP_ID

    private static final long DEFAULT_WARNING_TIMEOUT = 5000L;

    // Class Fields (shared between concrete states)

    protected static final Object s_handlerMutexLock = new Object();

    protected static final Object s_listenerMutexLock = new Object();

    protected static final Object s_strategyMutexLock = new Object(); // TODO:
                                                                      // needed?
                                                                      // since
                                                                      // stack
                                                                      // synchronized

    protected static final TVTimer s_timer;

    /**
     * Multicast list of caller context objects for tracking {@link EASListener}
     * references per caller context. At any point in time this list will be the
     * complete list of caller context objects that have an assigned
     * {@link EASListenerCallback} objects.
     */
    protected static int s_callerContextCount = 0;

    protected static volatile CallerContext s_callerContextList = null;

    /**
     * The alert strategies, in descending order of precedence, that could be
     * used to process the current alert message. Each strategy contains a
     * reference to the current alert message being processed by the strategy.
     * No alert message is being processed when the stack is empty.
     * 
     * @see EASAlertDetailsChannel
     * @see EASAlertTextAudio
     * @see EASAlertTextOnly
     */
    protected static final Stack s_alertStrategiesStack = new Stack();

    /**
     * If not null, this field references the currently registered
     * {@link EASHandler} to receive notifications of a private descriptor for
     * an audio source. The same handler will be notified to stop playing audio
     * at the end of the alert.
     */
    protected static EASHandlerContext s_currentHandler;

    /**
     * The {@link EASManagerImpl} context that is introducing EAS messages into
     * the state machine (see {@link #initialize(EASManagerImpl)}). Setting this
     * class field before starting the state machine eliminates the need to pass
     * the context in the method calls.
     */
    protected static EASManagerImpl s_easManagerContext;

    /**
     * If not null and differs in value from {@link #s_currentHandler}, this
     * field references the pending {@link EASHandler} to receive notifications
     * of a private descriptor for an audio source.
     * <p>
     * Normally, {@link #s_currentHandler} and {@link #s_pendingHandler} contain
     * the same value indicating no change in the handler. However if the values
     * differ when an in-progress alert completes, the current handler is
     * replaced by the pending handler.
     */
    protected static EASHandlerContext s_pendingHandler;

    /**
     * The set of still "active" alerts, sorted in ascending order of event
     * expiration time, and the associated timer specification for purging
     * expired events from the set.
     */
    private static SortedSet s_activeAlertSet;

    private static TVTimerSpec s_activeAlertTimerSpec = new TVTimerSpec();

    /**
     * The time, in milliseconds, to wait for all registered {@link EASListener}
     * implementations to be warned of EAS resource acquisition.
     */
    private static long s_warningTimeout;

    // Class Methods

    /**
     * Initializes <code>final</code> class fields when the class is loaded.
     */
    static
    {
        s_timer = TVTimer.getTimer();

        try
        {
            EASState.s_warningTimeout = MPEEnv.getEnv(EASState.MPEENV_WARNING_TIMEOUT, EASState.DEFAULT_WARNING_TIMEOUT);
        }
        catch (NumberFormatException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Invalid EASListener timeout, using default timeout - " + e.getMessage());
            }
            EASState.s_warningTimeout = EASState.DEFAULT_WARNING_TIMEOUT;
        }
    }

    /**
     * Returns the {@link AppID} object identifying the EAS host key listener
     * application specified by the {@link #MPEENV_PRESENTATION_INTERRUPT_APPID}
     * MPE environment variable. The application identified by this identifier
     * will be started if an indefinite alert presentation is being processed.
     * <p>
     * An invalid application ID (i.e. <code>AppID(0,0)</code> is returned if
     * the environment variable is not defined, or is not defined with a valid
     * hexadecimal string prefixed with "0x".
     */
    public static AppID getEASHostKeyListenerAppID()
    {
        try
        {
            String id = MPEEnv.getEnv(EASState.MPEENV_PRESENTATION_INTERRUPT_APPID,
                    EASState.DEFAULT_PRESENTATION_INTERRUPT_APPID);
            return ConversionUtil.parseAppID(id);
        }
        catch (NumberFormatException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Invalid EASHostKeyListener application ID - " + e.getMessage());
            }
            return EASState.INVALID_APP_ID;
        }
    }

    /**
     * Resets the {@link #s_activeAlertSet} the eliminate knowledge of still
     * "active" alerts. This is done after system reboots and tune-overs since
     * the head-ends can reuse <code>EAS_event_ID</code>s for different streams
     * (i.e. <code>EAS_event_ID</code> is not a globally unique ID across all
     * channels).
     */
    public static synchronized void resetActiveAlertSet()
    {
        EASState.s_timer.deschedule(EASState.s_activeAlertTimerSpec);
        EASState.s_activeAlertSet.clear();
    }

    /**
     * Adds a presented, but still "active" alert, to the
     * {@link #s_activeAlertSet} for duplicate <code>EAS_event_ID</code>
     * detection.
     * 
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     */
    protected static synchronized void addActiveAlert(final EASMessage message)
    {
        if (!message.isAlertExpired())
        {
            if (EASState.s_activeAlertSet.add(message))
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("Added active alert: " + message + "; expiry=" + new Date(message.getExpirationTime()));
                }
                EASState.rescheduleActiveAlertTimer();
            }
        }
    }

    /**
     * Determines if the <code>EAS_event_ID</code> in the given message matches
     * the <code>EAS_event_ID</code> of a previously-processed event that is
     * still "active", meaning the the current time has not passed the
     * expiration time of the previous event. The expiration time is indicated
     * by <code>event_start_time</code> plus <code>event_duration</code>,
     * normalized to the system epoch.
     * 
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @return <code>true</code> if the <code>EAS_evnt_ID</code> of the given
     *         message matches that of a previous still-active alert; otherwise
     *         <code>false</code>
     */
    protected static synchronized boolean matchesActiveAlert(final EASMessage message)
    {
        return EASState.s_activeAlertSet.contains(message);
    }

    /**
     * Returns the current alert strategy.
     * 
     * @return the current alert strategy being used or <code>null</code> if no
     *         alert is being processed
     */
    static EASAlert getAlertStrategy()
    {
        synchronized (EASState.s_strategyMutexLock)
        {
            return (EASState.s_alertStrategiesStack.empty() ? null : (EASAlert) EASState.s_alertStrategiesStack.peek());
        }
    }

    /**
     * Initializes the EAS message processing state machine.
     * 
     * @param context
     *            the {@link EASManagerImpl} context in which messages are
     *            processed
     */
    static void initialize(final EASManagerImpl context)
    {
        EASState.s_easManagerContext = context;
        EASState.s_activeAlertSet = new TreeSet(EASMessage.EXPIRATION_TIME_COMPARATOR);

        // set up the listener for the active alert timer specification
        EASState.s_activeAlertTimerSpec.addTVTimerWentOffListener(new TVTimerWentOffListener()
        {
            public void timerWentOff(TVTimerWentOffEvent e)
            {
                EASState.purgeActiveAlerts();
            }
        });
    }

    /**
     * Clears alert strategies stack in preparation for processing the next
     * alert message.
     */
    static void purgeAlertStrategies()
    {
        synchronized (EASState.s_strategyMutexLock)
        {
            EASState.s_alertStrategiesStack.clear();
        }
    }

    /**
     * Pushes the given alert strategy to the top of the alert strategies stack.
     * If a failure to process an alert occurs with one strategy, the next
     * strategy in the stack can be used to process the alert.
     * 
     * @param strategy
     *            the {@link EASAlert} strategy to push onto the stack
     */
    static void pushAlertStrategy(final EASAlert strategy)
    {
        synchronized (EASState.s_strategyMutexLock)
        {
            EASState.s_alertStrategiesStack.push(strategy);
        }
    }

    /**
     * Purges expired "active" alerts from the {@link #s_activeAlertSet} if the
     * set is not empty. Since the set is sorted by ascending expiration time,
     * the first "unexpired" alert shortcuts the loop. After purging expired
     * alerts, the timer is rescheduled for the next purge.
     */
    private static synchronized void purgeActiveAlerts()
    {
        if (!EASState.s_activeAlertSet.isEmpty())
        {
            for (Iterator i = EASState.s_activeAlertSet.iterator(); i.hasNext();)
            {
                EASMessage message = (EASMessage) i.next();
                if (message.isAlertExpired())
                {
                    i.remove();
                    if (log.isInfoEnabled()) 
                    {
                        log.info("Purged expired alert: " + message + "; expiry="
                            + new Date(message.getExpirationTime()));
                    }
                }
                else
                {
                    break; // no need to continue after first unexpired alert
                           // detected
                }
            }

            EASState.rescheduleActiveAlertTimer();
        }
    }

    /**
     * Reschedules the timer for purging expired "active" alerts from the
     * {@link #s_activeAlertSet} if the set is not empty. Since the set is
     * sorted by ascending expiration time, the first item in the set determines
     * when the timer should next go off.
     */
    private static synchronized void rescheduleActiveAlertTimer()
    {
        if (!EASState.s_activeAlertSet.isEmpty())
        {
            try
            {
                EASMessage firstActiveAlert = (EASMessage) EASState.s_activeAlertSet.first();

                EASState.s_timer.deschedule(EASState.s_activeAlertTimerSpec);
                EASState.s_activeAlertTimerSpec.setAbsoluteTime(firstActiveAlert.getExpirationTime());
                EASState.s_activeAlertTimerSpec = EASState.s_timer.scheduleTimerSpec(EASState.s_activeAlertTimerSpec);
                // EASState.s_timer.scheduleTimerSpec(EASState.s_activeAlertTimerSpec);
                // // TODO: temporary work-around for OCORI-537
                if (log.isInfoEnabled()) 
                {
                    log.info("Rescheduled active alert timer for:<" + new Date(firstActiveAlert.getExpirationTime()) + ">");
                }
            }
            catch (TVTimerScheduleFailedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Failed to reschedule active alert timer - " + e.getMessage());
                }
                SystemEventUtil.logRecoverableError("Failed to reschedule active alert timer - ", e);
            }
        }
    }

    // Instance Fields

    protected final CallerContextManager m_callerContextManager;

    protected final EASAlertTextFactory m_easAlertTextFactory;

    // Constructors

    /**
     * Constructs a new instance of the receiver.
     */
    protected EASState()
    {
        this.m_callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        this.m_easAlertTextFactory = EASAlertTextFactory.getInstance();
    }

    // Instance Methods

    /**
     * Adds a listener for EAS events. Generally adding a listener is safe,
     * however no warn notification is sent for an alert already in progress.
     * <p>
     * Note that a separate list of listeners is maintained for each host
     * application.
     * 
     * @param listener
     *            the {@link EASListener} implementation to be added
     */
    public void addListener(final EASListener listener)
    {
        synchronized (EASState.s_listenerMutexLock)
        {
            CallerContext context = this.m_callerContextManager.getCurrentContext();
            EASListenerCallback listeners = (EASListenerCallback) context.getCallbackData(EASListenerCallback.class);

            if (null == listeners)
            {
                listeners = new EASListenerCallback(context);
            }

            listeners.addListener(listener);
        }
    }

    /**
     * Completes the current alert. This is the default implementation which
     * does nothing.
     * 
     * @see EASStateNotInProgress#completeAlert()
     */
    public void completeAlert()
    {
        // intentionally do nothing
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.system.EASModuleRegistrar#getEASAttribute(int attribute)
     */
    public Object getEASAttribute(int attribute)
    {
        return this.m_easAlertTextFactory.getEASAttribute(attribute);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.system.EASModuleRegistrar#getEASCapability(int attribute)
     */
    public Object[] getEASCapability(int attribute)
    {
        return this.m_easAlertTextFactory.getEASCapability(attribute);
    }

    /**
     * Returns the current emergency alert state. Possible return values are
     * defined by state constants in {@link EASManager}.
     * 
     * @return EAS state
     */
    public abstract int getState();

    /**
     * Returns the string representation of the current emergency alert state.
     * 
     * @return the string representation of the EAS state
     */
    public abstract String getStateString();

    /**
     * Determines if there's an alert currently in progress.
     * <p>
     * Subclasses implement the appropriate behavior for their state.
     * 
     * @return <code>true</code> if there's an alert in progress; otherwise
     *         <code>false</code>
     */
    public abstract boolean isAlertInProgress();

    /**
     * Indicates whether a forced tune to an EAS details channel or audio
     * override source was done to present the EAS content.
     * 
     * @return <code>true</code> if an force tune was done to present EAS
     *         content; otherwise <code>false</code>
     */
    public boolean isForceTune()
    {
        EASAlert strategy = EASState.getAlertStrategy();
        return (null == strategy) ? false : strategy.isForceTune();
    }

    /**
     * Receives a new emergency alert message into the current state. This is
     * the default implementation which does nothing.
     * 
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @see EASStateNotInProgress#receiveAlert(EASMessage)
     * @see EASStateReceived#receiveAlert(EASMessage)
     * @see EASStateInProgress#receiveAlert(EASMessage)
     */
    public void receiveAlert(final EASMessage message)
    {
        // intentionally do nothing
    }

    /**
     * Registers an {@link EASHandler} instance. At most only one instance can
     * be registered. Multiple calls of this method replace the previous
     * instance by a new one. By default, no instance is registered.
     * <p>
     * Subclasses implement the appropriate behavior depending on the state of
     * the alert. If an alert is in-progress, the registration is delayed until
     * the current alert is complete so the appropriate handler is invoked to
     * stop the audio on a private descriptor. Otherwise the handler is
     * replaced.
     * 
     * @param handler
     *            the handler to register
     * @throws IllegalArgumentException
     *             if null is specified.
     */
    public abstract void registerEASHandler(final EASHandler handler);

    /**
     * Removes a listener from receiving EAS events. If the
     * <code>listener</code> wasn't previously added with
     * {@link #addListener(EASListener)}, this method does nothing.
     * <p>
     * Note that a separate list of listeners is maintained for each host
     * application.
     * 
     * @param listener
     *            the {@link EASListener} implementation to be removed
     */
    public void removeListener(final EASListener listener)
    {
        synchronized (EASState.s_listenerMutexLock)
        {
            CallerContext context = this.m_callerContextManager.getCurrentContext();
            EASListenerCallback listenerContext = (EASListenerCallback) context.getCallbackData(EASListenerCallback.class);

            if (null != listenerContext)
            {
                listenerContext.removeListener(listener);
            }
        }
    }

    /**
     * Retries processing the alert message with the next available strategy in
     * the stack.
     */
    public void retryAlert()
    {
        if (log.isInfoEnabled()) 
        {
            log.info(EASState.getAlertStrategy().getMessage().formatLogMessage("Retrying alert message..."));
        }

        synchronized (EASState.s_strategyMutexLock)
        {
            EASState.s_alertStrategiesStack.pop();
        }

        EASState.getAlertStrategy().processAlert();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.system.EASModuleRegistrar#setEASAttribute(int attribute[],
     * Object value[])
     */
    public void setEASAttribute(int attribute[], Object value[])
    {
        this.m_easAlertTextFactory.setEASAttribute(attribute, value);
    }

    /**
     * Starts presenting the current alert. This is the default implementation
     * which does nothing.
     * 
     * @see EASStateInProgress#startPresentingAlert()
     */
    public void startPresentingAlert()
    {
        // intentionally do nothing
    }

    /**
     * Stops presenting the current alert. This is the default implementation
     * which does nothing.
     * 
     * @see EASStateInProgress#stopPresentingAlert()
     */
    public void stopPresentingAlert()
    {
        // intentionally do nothing
    }

    /**
     * Returns a string representation of the receiver.
     * 
     * @return a string representation of the object.
     */
    public String toString()
    {
        EASAlert strategy = EASState.getAlertStrategy();
        StringBuffer buf = new StringBuffer("EASState: message=[");
        buf.append((null != strategy) ? strategy.getMessage().toString() : "none");
        buf.append(']');
        return buf.toString();
    }

    /**
     * Unregisters the current registered {@link EASHandler} instance. If no
     * EASHandler instance has registered, do nothing.
     * <p>
     * Subclasses implement the appropriate behavior depending on the state of
     * the alert. If an alert is in-progress, the deregistration is delayed
     * until the current alert is complete so the appropriate handler is invoked
     * to stop the audio on a private descriptor. Otherwise the handler is
     * removed.
     */
    public abstract void unregisterEASHandler();

    /**
     * Formats a string identifying the message by its type (IB or OOB),
     * sequence number and event ID, and then appends the given message.
     * 
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @param text
     *            the text of the message to log
     * @return a string formatted for a log message
     */
    protected String formatLogMessage(final EASMessage message, String text)
    {
        StringBuffer buf = new StringBuffer(getStateString());
        buf.append(": ");
        buf.append(message == null ? "null" : message.formatLogMessage(text));
        return buf.toString();
    }

    /**
     * Returns a new instance of an {@link EASEvent} with the given reason.
     * 
     * @param reason
     *            an {@link EASEvent} constant indicating the event reason
     * @return an instance of {@link EASEvent}
     */
    protected EASEvent newEASEvent(final int reason)
    {
        return new EASEvent(EASState.s_easManagerContext.getEASManager(), reason);
    }

    /**
     * Notifies all registered {@link EASListener} implementations of an
     * {@link EASEvent} with the given reason.
     * 
     * @param reason
     *            an {@link EASEvent} constant indicating the event reason
     */
    protected void notifyEASListeners(final int reason)
    {
        int callerContextCount;
        CallerContext callerContextList;

        synchronized (EASState.s_listenerMutexLock)
        {
            callerContextCount = EASState.s_callerContextCount;
            callerContextList = EASState.s_callerContextList;
        }

        if (callerContextList != null)
        {
            if (log.isInfoEnabled()) 
            {
                log.info("Notifying EASListeners:<" + callerContextCount + ">, reason:<" + reason + ">");
            }

            final EASEvent event = newEASEvent(reason);

            // Use runInContextAsync(Runnable) since the order in which the
            // listeners are notified is not specified.
            callerContextList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    CallerContext context = EASState.this.m_callerContextManager.getCurrentContext();
                    EASListenerCallback listeners = (EASListenerCallback) context.getCallbackData(EASListenerCallback.class);

                    if (context.isAlive() && null != listeners)
                    {
                        listeners.notify(event);
                    }
                }

                public String toString()
                {
                    return EASState.this.getStateString() + "[notifyEASListeners]: eventReason=" + reason;
                }
            });
        }
    }

    /**
     * Warns all registered {@link EASListener} implementations of an
     * {@link EASEvent} with the given reason. The intent is to allow those
     * listeners to save state (and possibly release resources) so that they can
     * restore their state and services when they're notified that the EAS
     * presentation is complete ({@link EASEvent#EAS_COMPLETE}).
     * <p>
     * Per OCAP 1.1 Profile, 20.2.2.10, <cite>resources SHALL NOT be taken
     * away from the application for EAS use until all registered listener
     * {@link EASListener#warn(EASEvent) warn} methods have been called for the
     * first event of an EAS message.</cite>
     * 
     * @param reason
     *            an {@link EASEvent} constant indicating the event reason
     */
    protected void warnEASListeners(final int reason)
    {
        int callerContextCount;
        CallerContext callerContextList;

        synchronized (EASState.s_listenerMutexLock)
        {
            callerContextCount = EASState.s_callerContextCount;
            callerContextList = EASState.s_callerContextList;
        }

        if (callerContextList != null)
        {
            if (log.isInfoEnabled()) 
            {
                log.info("Warning EASListeners:<" + callerContextCount + ">, reason:<" + reason + ">");
            }

            final CountDownLatch applicationsWarned = new CountDownLatch(callerContextCount);
            final EASEvent event = newEASEvent(reason);

            // Use runInContextAsync(Runnable) since the order in which the
            // listeners are warned is not specified.
            callerContextList.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    CallerContext context = EASState.this.m_callerContextManager.getCurrentContext();
                    EASListenerCallback listeners = (EASListenerCallback) context.getCallbackData(EASListenerCallback.class);

                    // Per OCAP 1.1 Profile, 20.2.2.10, "The implementation
                    // MAY remove resources as soon as the method is
                    // called and is not required to wait for listeners to
                    // return from this method." Therefore, we can count
                    // down the latch first, invoke the warn method, and still
                    // comply with the specification. However, the
                    // count down must always be done in case "dead" caller
                    // contexts are found in the list (e.g. timing window).
                    // This technique also allows EAS processing to continue if
                    // a misbehaving application doesn't return from
                    // warn() invocation in a timely manner (or at all).

                    applicationsWarned.countDown();

                    if (context.isAlive() && null != listeners)
                    {
                        listeners.warn(event);
                    }
                }

                public String toString()
                {
                    return "EASStateReceived[warnEASListeners]: eventReason=" + reason;
                }
            });

            // Yield thread control to allow a limited time for all listener
            // warn() methods to be invoked in other threads before
            // continuing with service selection.
            try
            {
                if (applicationsWarned.await(EASState.s_warningTimeout))
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info("Warning EASListeners complete");
                    }
                }
                else
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(formatLogMessage(EASState.getAlertStrategy().getMessage(), "Timed out while waiting on warn() invocations:<" + applicationsWarned.getCount()
                                        + " left>"));
                    }
                }
            }
            catch (InterruptedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(formatLogMessage(EASState.getAlertStrategy().getMessage(), "Interrupted while waiting on warn() invocations"));
                }
            }
        }
    }
}
