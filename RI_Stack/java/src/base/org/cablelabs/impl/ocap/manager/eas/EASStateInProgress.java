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

import java.util.Arrays;
import java.util.Date;

import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SimpleCondition;
import org.cablelabs.impl.util.SystemEventUtil;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsDatabase;
import org.ocap.system.EASEvent;
import org.ocap.system.EASHandler;
import org.ocap.system.EASListener;
import org.ocap.system.EASManager;

/**
 * A concrete implementation of {@link EASState} that provides the message
 * processing actions for an EAS message in the "in-progress" state.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
class EASStateInProgress extends EASState implements AppStateChangeEventListener
{
    // Class Constants

    public static final String MPEENV_PRESENTATION_MINIMUM_TIME = "OCAP.eas.presentation.minimum.time";

    static final EASState INSTANCE = new EASStateInProgress();

    private static final long DEFAULT_MINIMUM_PRESENTATION_TIME = 0L; // dismiss
                                                                      // alert
                                                                      // immediately

    private static final long EAS_LISTENER_NOTIFICATION_INTERVAL = 10000L; // 10
                                                                           // seconds
                                                                           // per
                                                                           // OCAP
                                                                           // 1.x
                                                                           // Profile

    // Profile

    // Profile

    private static final Logger log = Logger.getLogger(EASStateInProgress.class.getName());

    // Instance Fields

    private AppsDatabase m_appsDatabase = AppsDatabase.getAppsDatabase();

    /**
     * The unique application ID of a host key listener application that is
     * started when the <code>alert_message_time_remaining</code> field value of
     * the EAS message has a indefinite value (0). The application allows the
     * user to dismiss the alert when any key is pressed on the remote. A
     * invalid ID of (0,0) is used to indicate that the host application was not
     * specified and should not be started (i.e. it won't match an application
     * registered in the <code>AppsDatabase</code>).
     */
    private AppID m_keyListenerAppId = EASState.getEASHostKeyListenerAppID();

    /**
     * The host key listener application proxy that allows this implementation
     * to start and stop the host key listener, and to detect when the listener
     * application terminates itself as an indication to dismiss the alert. A
     * non-null value indicates the listener has been started.
     */
    private AppProxy m_keyListenerProxy;

    private String[] m_keyListenerArguments;

    private boolean m_keyListenerStopInProgress;

    private SimpleCondition m_keyListenerStopped = new SimpleCondition(false);

    /**
     * The repeating timer specification for notifying each registered
     * {@link EASListener} of an alert in progress every 10 seconds while the
     * alert is being presented.
     */
    private TVTimerSpec m_notificationTimerSpec = new TVTimerSpec();

    /**
     * If not <code>null</code>, the overlapping alert message that was received
     * while the current alert was being presented. Set when the presentation is
     * stopped (see {@link #stopPresentingAlert(EASMessage)}.
     */
    private EASMessage m_overlappingAlert;

    /**
     * The non-repeating timer specification that determines how long the alert
     * is presented when the <code>alert_message_time_remaining</code> field
     * value of the EAS message has a finite value.
     */
    private TVTimerSpec m_presentationTimerSpec = new TVTimerSpec();

    /**
     * If <code>true</code>, the current presentation is in the process of being
     * stopped. Used to prevent stopping the same alert presentation multiple
     * times.
     */
    private boolean m_stoppingPresentation;

    // Constructors

    /**
     * Constructs a new instance of the receiver.
     */
    private EASStateInProgress()
    {
        // Set up the listener for the periodic notification timer
        // specification.
        this.m_notificationTimerSpec.addTVTimerWentOffListener(new TVTimerWentOffListener()
        {
            public void timerWentOff(TVTimerWentOffEvent e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("EAS notification timer went off");
                }
                EASAlert strategy = EASState.getAlertStrategy();
                if (null != strategy)
                {
                    notifyEASListeners(strategy.getReason());
                }
            }
        });

        // Set up the listener for the presentation timer specification.
        this.m_presentationTimerSpec.addTVTimerWentOffListener(new TVTimerWentOffListener()
        {
            public void timerWentOff(TVTimerWentOffEvent e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Presentation timer went off - stopping presenting EAS alert");
                }
                stopPresentingAlert();
            }
        });

        // Construct EASHostKeyListener arguments.
        try
        {
            long time = MPEEnv.getEnv(EASStateInProgress.MPEENV_PRESENTATION_MINIMUM_TIME,
                    EASStateInProgress.DEFAULT_MINIMUM_PRESENTATION_TIME);
            this.m_keyListenerArguments = new String[] { String.valueOf(Math.max(0, time)) };
        }
        catch (NumberFormatException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Invalid EASHostKeyListener minimum presentation time - " + e.getMessage());
            }
            this.m_keyListenerArguments = new String[] { String.valueOf(EASStateInProgress.DEFAULT_MINIMUM_PRESENTATION_TIME) };
        }
    }

    // Instance methods

    /**
     * Adds a listener for EAS events and notifies the newly-added listener of
     * an alert already in progress.
     * 
     * @param listener
     *            the {@link EASListener} implementation to be added
     */
    public void addListener(final EASListener listener)
    {
        super.addListener(listener);

        CallerContext context = super.m_callerContextManager.getCurrentContext();
        final EASEvent event = newEASEvent(EASState.getAlertStrategy().getReason());

        // runInContextAsync(Runnable) used to isolate the application listener
        // notification from the EAS implementation.
        context.runInContextAsync(new Runnable()
        {
            public void run()
            {
                listener.notify(event);
            }
        });
    }

    /**
     * Completes processing of the current alert in this state which includes
     * stopping the concurrent activities of:
     * <ul>
     * <li>the audio/video/text presentation timer</li>
     * <li>the periodic notification of EAS activity to registered listeners</li>
     * <li>the host key listener for an indefinite presentation</li>
     * </ul>
     * Transitions to {@link EASStateReceived} to process an overlapping alert
     * message; otherwise transitions to {@link EASStateNotInProgress} to
     * finalize processing of the current alert message. </p>
     * 
     * @see EASStateReceived#receiveAlert(EASMessage)
     * @see EASStateNotInProgress#completeAlert()
     */
    public synchronized void completeAlert()
    {
        if (log.isInfoEnabled())
        {
            log.info("completeAlert");
        }
        stopAlertPresentationTimer();
        stopKeyListenerApplication();
        stopEASListenerNotification();

        if (null != this.m_overlappingAlert)
        {
            if (log.isInfoEnabled())
            {
                log.info(formatLogMessage(m_overlappingAlert, "completeAlert - overlapping EAS alert exists - updating to received state and processing overlapping EAS alert"));
            }
            // Start processing the overlapping alert.
            EASState.s_easManagerContext.changeState(EASStateReceived.INSTANCE);
            EASState.s_easManagerContext.getCurrentState().receiveAlert(this.m_overlappingAlert);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("completeAlert - no overlapping EAS alert - updating to not in progress state and completing EAS alert");
            }
            // Complete processing the current alert.
            EASState.s_easManagerContext.changeState(EASStateNotInProgress.INSTANCE);
            EASState.s_easManagerContext.getCurrentState().completeAlert();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASState#getState()
     */
    public int getState()
    {
        return EASManager.EAS_MESSAGE_IN_PROGRESS_STATE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASState#getStateString()
     */
    public String getStateString()
    {
        return "EASStateInProgress";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASState#isAlertInProgress()
     */
    public boolean isAlertInProgress()
    {
        return true;
    }

    /**
     * Receives a new emergency alert message into the current state. The
     * <code>EAS_event_ID</code> of the incoming message is compared to that of
     * the in-progress message.
     * <ul>
     * <li>If the <code>EAS_event_ID</code>s match, the incoming message updates
     * the presentation time (i.e. <code>alert_message_time_remaining</code>) of
     * the in-progress alert and the state remains unchanged. This technique is
     * used to terminate an alert with an indefinite presentation time.</li>
     * <li>If the <code>EAS_event_ID</code>s don't match, the incoming alert
     * overlaps the in-progress alert, which will be overridden by the new alert
     * by transitioning to the {@link EASStateReceived} state.</li>
     * </ul>
     * 
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @see EASStateReceived#receiveAlert(EASMessage)
     */
    public void receiveAlert(final EASMessage message)
    {
        if (log.isInfoEnabled())
        {
            log.info(message.formatLogMessage("Receiving message..."));
        }

        EASMessage currentMessage = EASState.getAlertStrategy().getMessage();
        if (currentMessage.equals(message))
        {
            // Only update the presentation time of the current alert if the
            // EAS_event_ID matches.
            if (log.isInfoEnabled())
            {
                log.info(formatLogMessage(message, "receiveAlert - Updating presentation time of current EAS alert to:<" + message.getAlertMessageTimeRemaining()
                        + " secs>"));
            }
            final long presentationTime = message.getAlertMessageTimeRemaining() * 1000L;
            final long currentPresentationTime = currentMessage.getAlertMessageTimeRemaining() * 1000L;

            //update from indefinite to indefinite, ignore
            if (EASMessage.INDEFINITE_ALERT_MESSAGE_TIME_REMAINING == presentationTime && EASMessage.INDEFINITE_ALERT_MESSAGE_TIME_REMAINING ==  currentPresentationTime)
            {
                if (log.isInfoEnabled())
                {
                    log.info(formatLogMessage(message, "receiveAlert - indefinite to indefinite of same message, ignoring"));
                }
                return;
            }
            
            //current is not indefinite but new is
            if (presentationTime == EASMessage.INDEFINITE_ALERT_MESSAGE_TIME_REMAINING)
            {
                if (log.isInfoEnabled())
                {
                    log.info(formatLogMessage(message, "receiveAlert - presentation time is indefinite - stopping EAS alert presentation timer and starting EAS keylistener application"));
                }
                stopAlertPresentationTimer();
                startKeyListenerApplication();
            }
            else
            {
                //current may be indefinite and new is not
                if (log.isInfoEnabled())
                {
                    log.info(formatLogMessage(message, "receiveAlert - presentationTime is not indefinite - stopping EAS keylistener application and starting alert presentation timer"));
                }
                stopKeyListenerApplication();
                startAlertPresentationTimer(presentationTime);
            }
        }
        else
        {
            // Unconditionally stop presenting the current alert so we can
            // process the new (overlapping) alert.
            if (log.isInfoEnabled())
            {
                log.info(formatLogMessage(message, "receiveAlert - new EAS alert - stop processing current alert and process new alert..."));
            }
            stopPresentingAlert(message);
        }
    }

    /**
     * Registers an {@link EASHandler} instance. At most, only one instance can
     * be registered. Multiple calls of this method replace the previous
     * instance by a new one. By default, no instance is registered.
     * <p>
     * There's an alert in-progress so just set the pending handler to allow the
     * current handler to be notified to stop playing audio.
     */
    public void registerEASHandler(final EASHandler handler)
    {
        if (null == handler)
        {
            throw new IllegalArgumentException("EASHander reference must not be null");
        }

        synchronized (EASState.s_handlerMutexLock)
        {
            EASState.s_pendingHandler = new EASHandlerContext(handler, super.m_callerContextManager.getCurrentContext());
        }
    }

    /**
     * Starts presenting the current alert, which includes starting the
     * concurrent activities of:
     * <ul>
     * <li>the audio/video/text presentation of the alert itself</li>
     * <li>the periodic notification of EAS activity to registered listeners</li>
     * <li>the host key listener for an indefinite presentation</li>
     * </ul>
     * 
     * @see EASStateInProgress#stopPresentingAlert()
     */
    public void startPresentingAlert()
    {
        EASMessage currentAlert = EASState.getAlertStrategy().getMessage();
        if (log.isInfoEnabled())
        {
            log.info(formatLogMessage(currentAlert, "startPresentingAlert - starting EAS listener notification and starting presentation of alert"));
        }
        final long presentationTime = EASState.getAlertStrategy().getPresentationTime();
		// Added for findbugs issues fix - start
		synchronized(this)
        {
            this.m_stoppingPresentation = false;
        }
		// Added for findbugs issues fix - end
        startEASListenerNotification();
        EASState.getAlertStrategy().startPresentation();

        if (presentationTime == EASMessage.INDEFINITE_ALERT_MESSAGE_TIME_REMAINING)
        {
            if (log.isInfoEnabled())
            {
                log.info(formatLogMessage(currentAlert, "indefinite alert - starting EAS key listener application"));
            }
            startKeyListenerApplication();
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info(formatLogMessage(currentAlert, "not an indefinite alert - starting EAS presentation timer"));
            }
            startAlertPresentationTimer(presentationTime);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.dvb.application.AppStateChangeEventListener#stateChange(org.dvb.
     * application.AppStateChangeEvent)
     */
    public void stateChange(AppStateChangeEvent evt)
    {
        //the application may be restarting - ignore events from other app proxies
        if (evt.getAppID().equals(this.m_keyListenerAppId) && m_keyListenerProxy == evt.getSource())
        {
            boolean failed = evt.hasFailed();
            int newState = evt.getToState();
            int prevState = evt.getFromState();

            if (log.isInfoEnabled())
            {
                log.info("EAS key listener stateChange event: " + evt);
            }

            // If there is any failure while trying to start the application,
            // stop the application.
            if (failed && newState != AppProxy.DESTROYED && prevState != newState)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Failed to start the EAS host key listener application - stopping the application");
                }
                stopKeyListenerApplication();
            }

            // If the application has successfully gone from the STARTED state
            // to the PAUSED state,
            // stop the presentation gracefully -- it's an indication that the
            // user wants to dismiss
            // the indefinite alert presentation.
            else if (!failed && prevState == AppProxy.STARTED && newState == AppProxy.PAUSED)
            {
                if (log.isInfoEnabled())
                {
                    log.info("application event not failed and transitioned from started to pause - stop presenting EAS alert");
                }
                stopPresentingAlert();
            }

            // If the application has successfully gone to the DESTROYED state
            // the application has completely stopped and it's safe to restart
            // the application if necessary.
            // NOTE: there is no notification in the transition from DESTROYED to NOT_LOADED
            else if (!failed && newState == AppProxy.DESTROYED)
            {
                if (log.isInfoEnabled())
                {
                    log.info("application event not failed and transitioned to destroyed - updating listenerstopped to true");
                }
                this.m_keyListenerStopped.setTrue();
            }
        }
    }

    /**
     * Stops presenting the current alert which can originate from one of the
     * following "normal" activities:
     * <ol>
     * <li>disabling EAS processing by shutting down the stack, or</li>
     * <li>by switching to the Standby power mode, or</li>
     * <li>the presentation timer went off indicating the end of a finite alert
     * presentation, or</li>
     * <li>the EAS host key listener application terminated itself indicating
     * the end of an indefinite alert presentation.</li>
     * </ol>
     * 
     * @see EASStateInProgress#startPresentingAlert()
     */
    public synchronized void stopPresentingAlert()
    {
        stopPresentingAlert(null);
    }

    /**
     * Stops presenting the current alert which can originate from the following
     * activities:
     * <ol>
     * <li>disabling EAS processing by shutting down the stack, or</li>
     * <li>by switching to the Standby power mode, or</li>
     * <li>the presentation timer went off indicating the end of a finite alert
     * presentation, or</li>
     * <li>the EAS host key listener application terminated itself indicating
     * the end of an indefinite alert presentation.</li>
     * <li>an overlapping emergency alert message was received during the
     * presentation of the current alert.</li>
     * </ol>
     * To avoid potentially blocking timer or event notification threads,
     * stopping the presentation is spun off into an asynchronous system task.
     * 
     * @param overlappingAlert
     *            if not <code>null</code>, it's an overlapping
     *            {@link EASMessage} that was received while the current alert
     *            was being presented
     */
    public synchronized void stopPresentingAlert(final EASMessage overlappingAlert)
    {
        if (log.isInfoEnabled())
        {
            log.info("Stopping the alert presentation:<" + this.m_stoppingPresentation + ">, overlappingAlert:<"
                + (overlappingAlert != null) + ">");
        }

        final EASAlert alert = EASState.getAlertStrategy();
        if (!this.m_stoppingPresentation)
        {
            if (log.isInfoEnabled())
            {
                log.info(formatLogMessage(overlappingAlert, "stopPresentingAlert - not already stopping presentation - calling stopPresentation of alert on new systemcontext thread - overlapping alert:"));
            }
            this.m_stoppingPresentation = true;
            this.m_overlappingAlert = overlappingAlert;
            super.m_callerContextManager.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    alert.stopPresentation(null != overlappingAlert);
                }
            });
        }
    }

    /**
     * Returns a string representation of the receiver.
     * 
     * @return a string representation of the object.
     */
    public String toString()
    {
        EASAlert strategy = EASState.getAlertStrategy();
        StringBuffer buf = new StringBuffer("EASStateInProgress: message=[");
        buf.append((null != strategy) ? strategy.getMessage().toString() : "none");
        buf.append(']');
        return buf.toString();
    }

    /**
     * Unregisters the current registered {@link EASHandler} instance. If no
     * EASHandler instance has registered, do nothing.
     * <p>
     * There's an alert in-progress so just clear the pending handler to allow
     * the current handler to be notified to stop playing audio.
     */
    public void unregisterEASHandler()
    {
        synchronized (EASState.s_handlerMutexLock)
        {
            EASState.s_pendingHandler = null;
        }
    }

    /**
     * Starts the non-repeating presentation timer for the current alert. When
     * timer goes off, the presentation will be stopped.
     * 
     * @param presentationLength
     *            the length of time, in milliseconds, to present the alert
     */
    private void startAlertPresentationTimer(final long presentationLength)
    {
        try
        {
            final long presentationExpiry = System.currentTimeMillis() + presentationLength;

            // Start the timer that'll stop the presentation.
            EASState.s_timer.deschedule(this.m_presentationTimerSpec);
            this.m_presentationTimerSpec.setAbsoluteTime(presentationExpiry);
            this.m_presentationTimerSpec = EASState.s_timer.scheduleTimerSpec(this.m_presentationTimerSpec);
            // EASState.s_timer.scheduleTimerSpec(this.m_presentationTimerSpec);
            // // TODO: temporary work-around for OCORI-537
            if (log.isInfoEnabled())
            {
                log.info("startAlertPresentationTimer - scheduled presentation timer for:<" + new Date(presentationExpiry) + ">");
            }
        }
        catch (TVTimerScheduleFailedException e)
        {
            SystemEventUtil.logRecoverableError("Failed to schedule alert presentation timer", e);
        }
    }

    /**
     * Starts the periodic notification to registered {@link EASListener}
     * implementations that an alert is in progress.
     */
    private void startEASListenerNotification()
    {
        try
        {
            // Do the initial notification.
            notifyEASListeners(EASState.getAlertStrategy().getReason());

            // Start the repeated notifications.
            EASState.s_timer.deschedule(this.m_notificationTimerSpec);
            this.m_notificationTimerSpec.setDelayTime(EASStateInProgress.EAS_LISTENER_NOTIFICATION_INTERVAL);
            this.m_notificationTimerSpec.setRegular(true);
            this.m_notificationTimerSpec.setRepeat(true);
            this.m_notificationTimerSpec = EASState.s_timer.scheduleTimerSpec(this.m_notificationTimerSpec);
            // EASState.s_timer.scheduleTimerSpec(this.m_notificationTimerSpec);
            // // TODO: temporary work-around for OCORI-537
            if (log.isInfoEnabled())
            {
                log.info("Scheduled repeating EASListener notification timer");
            }
        }
        catch (TVTimerScheduleFailedException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Failed to schedule listener notification timer - " + e.getMessage());
            }
            SystemEventUtil.logRecoverableError("Failed to schedule EASListener notification timer - ", e);
        }
    }

    /**
     * Starts the <code>EASHostKeyListener</code> application if the host key
     * listener application hasn't already been started. A new proxy is obtained
     * on each start since {@link AppProxy#stop(boolean)} invalidates the prior
     * proxy.
     */
    private synchronized void startKeyListenerApplication()
    {
        if (null == this.m_keyListenerProxy)
        {
            this.m_keyListenerProxy = this.m_appsDatabase.getAppProxy(this.m_keyListenerAppId);
            if (null != this.m_keyListenerProxy)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Starting EAS host key listener application: 0x" + m_keyListenerAppId + ", args:"
                                + Arrays.asList(this.m_keyListenerArguments));
                }
                this.m_keyListenerStopInProgress = false;
                this.m_keyListenerProxy.addAppStateChangeEventListener(this);
                this.m_keyListenerProxy.start(this.m_keyListenerArguments);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("EAS host key listener application ID not defined or not found:<0x" + this.m_keyListenerAppId
                                + ">");
                }
            }
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("keyListenerProxy is not null, not starting key listener app");
            }
        }
    }

    /**
     * Stops the alert presentation timer.
     */
    private void stopAlertPresentationTimer()
    {
        EASState.s_timer.deschedule(this.m_presentationTimerSpec);
        if (log.isInfoEnabled())
        {
            log.info("Stopped presentation timer");
        }
    }

    /**
     * Stops the periodic notification to registered {@link EASListener}s that
     * an alert is in progress.
     */
    private void stopEASListenerNotification()
    {
        EASState.s_timer.deschedule(this.m_notificationTimerSpec);
        if (log.isInfoEnabled())
        {
            log.info("Stopped EAS Listener notification timer");
        }
    }

    /**
     * Stops the <code>EASHostKeyListener</code> application if the host key
     * listener application had been started. Note that stopping the application
     * is an asynchronous call.
     */
    private synchronized void stopKeyListenerApplication()
    {
        if (log.isInfoEnabled())
        {
            log.info("stopKeyListenerApplication - proxy: " + m_keyListenerProxy);
        }
        if (null != this.m_keyListenerProxy)
        {
            if (!this.m_keyListenerStopInProgress && AppProxy.DESTROYED != this.m_keyListenerProxy.getState())
            {
                try
                {
                    // Stop the application and wait for it to transition to the
                    // <code>NOT_LOADED</code> state.
                    if (log.isInfoEnabled())
                    {
                        log.info("stopKeyListenerApplication - stop not in progress and app not destroyed - Stopping key listener application");
                    }
                    this.m_keyListenerStopInProgress = true;
                    this.m_keyListenerStopped.setFalse();
                    this.m_keyListenerProxy.stop(true);
                    this.m_keyListenerStopped.waitUntilTrue();
                    if (log.isInfoEnabled())
                    {
                        log.info("stopKeyListenerApplication - application stopped");
                    }
                }
                catch (InterruptedException e)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("interrupted", e);
                    }
                    this.m_keyListenerStopped.setTrue();
                }
                finally
                {
                    this.m_keyListenerStopInProgress = false;
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("stopKeyListenerApplication - stop in progress or app destroyed - not stopping application");
                }
            }

            this.m_keyListenerProxy.removeAppStateChangeEventListener(this);
            this.m_keyListenerProxy = null;
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("stopKeyListenerApplication called but no key listener application started - ignoring");
            }
        }
    }
}
