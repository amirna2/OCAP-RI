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

import javax.media.Time;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.AlternativeContentEvent;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextDestroyedEvent;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.GraphicsManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.OcapMain;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceContextFactoryExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.javatv.selection.ServiceContextCallback;
import org.cablelabs.impl.util.SystemEventUtil;
import org.ocap.system.EASEvent;
import org.ocap.system.EASListener;

/**
 * A concrete instance of this abstract class represents the strategy to use for
 * presenting a type of EAS alert (e.g. details channel, text+audio, text-only).
 * 
 * @author Dave Beidle
 * @version $Revision$
 * @see EASAlertDetailsChannel
 * @see EASAlertTextAudio
 * @see EASAlertTextOnly
 */
abstract class EASAlert
{
    /**
     * A static inner class that provides the concrete instance of an
     * {@link EASAlert} strategy that handles an emergency alert message with no
     * presentable content.
     */
    static class EASAlertEmpty extends EASAlert
    {
        /**
         * Constructs a new instance of the receiver with a reference to the
         * given message.
         * 
         * @param state
         *            a {@link EASState} reference for method callbacks
         * @param message
         *            a parsed and validated instance of {@link EASMessage}
         */
        public EASAlertEmpty(EASState state, EASMessage message)
        {
            super(state, message);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#getReason()
         */
        public int getReason()
        {
            return EASEvent.EAS_TEXT_DISPLAY;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#processAlert()
         */
        public void processAlert()
        {
            SystemEventUtil.logEvent(super.m_easMessage.formatLogMessage("No valid EAS content to present"));
            EASState.s_easManagerContext.changeState(EASStateNotInProgress.INSTANCE);
            EASState.s_easManagerContext.getCurrentState().completeAlert();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#startPresentation()
         */
        public void startPresentation()
        {
            // Intentionally do nothing -- no content to present.
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASAlert#stopPresentation(boolean
         * )
         */
        public void stopPresentation(boolean force)
        {
            // Intentionally do nothing -- no content to stop presenting.
        }

        /**
         * Returns a string representation of the receiver.
         * 
         * @return a string representation of the object.
         */
        public String toString()
        {
            return "EASAlertEmpty";
        }
    }

    /**
     * An inner class that encapsulates tuning and presentation of EAS details
     * channel and audio override services. Some operations occur asynchronously
     * so a call back mechanism is used to notify the <code>EASAlert</code>
     * strategy when selection has completed and is presenting, when the
     * presentation has terminated, or when service selection has failed.
     */
    class EASTuner implements ServiceContextCallback, ServiceContextListener
    {
        private EASAlert m_alertCallback;

        private boolean m_forceTune;

        private ServiceMediaHandler m_mediaHandler;

        private ServiceContextExt m_serviceContext;

        /**
         * Constructs a new instance of the receiver.
         */
        public EASTuner(final EASAlert callback)
        {
            this.m_alertCallback = callback;
        }

        /**
         * Indicates whether a forced tune to an EAS details channel or audio
         * override source was done to present the EAS content.
         * 
         * @return <code>true</code> if an force tune was done to present EAS
         *         content; otherwise <code>false</code>
         */
        public boolean isForceTune()
        {
            return this.m_forceTune;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.service.javatv.selection.ServiceContextCallback
         * #notifyPlayerStarted(javax.tv.service.selection.ServiceContext,
         * javax.tv.service.selection.ServiceMediaHandler)
         */
        public void notifyPlayerStarted(ServiceContext sc, ServiceMediaHandler player)
        {
            if (null == this.m_mediaHandler)
            {
                this.m_mediaHandler = player;
                this.m_mediaHandler.setMediaTime(new Time(Double.POSITIVE_INFINITY)); // jump
                                                                                      // to
                                                                                      // "live"
                                                                                      // point
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.service.javatv.selection.ServiceContextCallback
         * #notifyStoppingPlayer(javax.tv.service.selection.ServiceContext,
         * javax.tv.service.selection.ServiceMediaHandler)
         */
        public void notifyStoppingPlayer(ServiceContext sc, ServiceMediaHandler player)
        {
            // intentionally left empty
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.service.selection.ServiceContextListener#
         * receiveServiceContextEvent
         * (javax.tv.service.selection.ServiceContextEvent)
         */
        public void receiveServiceContextEvent(ServiceContextEvent e)
        {
            if (e instanceof AlternativeContentEvent)
            {
                // Presenting black screen (probably) while ServiceContext
                // recovers content.
                if (log.isInfoEnabled())
                {
                    log.info("Alternative content presenting");
                }
                this.m_alertCallback.presentationStarted();
            }
            else if (e instanceof NormalContentEvent)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Presentation started");
                }
                this.m_alertCallback.presentationStarted();
            }
            else if (e instanceof PresentationTerminatedEvent)
            {
                // Presentation terminated.
                if (log.isInfoEnabled())
                {
                    log.info("Presentation terminated, reason:<" + ((PresentationTerminatedEvent) e).getReason() + ">");
                }
                try
                {
                    releaseServiceContext();
                }
                catch (Exception exception)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("PresentationTerminatedEvent received - problem releasing ServiceContext", exception);
                    }
                }
                this.m_alertCallback.presentationTerminated();
            }
            else if (e instanceof SelectionFailedEvent)
            {
                // Selection failed due to an unrecoverable error.
                if (log.isWarnEnabled())
                {
                    log.warn("Selection failed, reason:<" + ((SelectionFailedEvent) e).getReason() + ">");
                }
                try
                {
                    releaseServiceContext();
                }
                catch (Exception exception)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("SelectionFailedEvent received - problem releasing ServiceContext", exception);
                    }
                }
                this.m_alertCallback.presentationFailed();
            }
            else if (e instanceof ServiceContextDestroyedEvent)
            {
                // Presentation destroyed.
                if (log.isWarnEnabled())
                {
                    log.warn("EAS service context destroyed");
                }
                try
                {
                    releaseServiceContext();
                }
                catch (Exception exception)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("ServiceContextDestroyedEvent received - problem releasing ServiceContext", exception);
                    }
                }
                this.m_alertCallback.presentationFailed();
            }
            else
            {
                // Intentionally do nothing -- ignore all other events.
                if (log.isWarnEnabled())
                {
                    log.warn("Ignoring unexpected service context event:<" + e.getClass().getName() + ">");
                }
            }
        }

        /**
         * Selects the given <code>service</code> for presenting EAS content
         * (details channel or audio-only), which may originate from an OOB
         * source ID, or an IB frequency/program/modulation triplet.
         * <p>
         * An attempt is first made to reuse a service context that is already
         * presenting the EAS service. If such a service context is found, the
         * presentation is advanced to the "live" point to ensure the EAS
         * content is being presented. All other service contexts that are
         * presenting a non-abstract service are stopped.
         * <p>
         * If a service context cannot be reused, a new service context is
         * created and a forced tune initiated to select the EAS service. This
         * operation completes asynchronously; a {@link NormalContentEvent} is
         * posted when selection completes and the content is presenting.
         * 
         * @param easService
         *            the service containing EAS content
         * @return <code>true</code> if a service context was selected and
         *         reused because it's already presenting the EAS service;
         *         <code>false</code> if an asynchronous forced tune is in
         *         progress on a new service context
         */
        public boolean select(final Service easService)
        {
            if (log.isInfoEnabled())
            {
                log.info("EAS alert - select service: " + easService);
            }
            ServiceContext[] serviceContexts = EASAlert.this.m_serviceContextFactory.getAllServiceContexts();
            boolean serviceAlreadyTuned = false;
            this.m_forceTune = false;
            this.m_serviceContext = null;

            // Try to use an existing service context, stopping all other
            // non-abstract services in the process...
            if (log.isInfoEnabled())
            {
                log.info("examining " + serviceContexts.length + " servicecontexts for requested service");
            }
            for (int i = 0; i < serviceContexts.length; ++i)
            {
                ServiceContextExt context = (ServiceContextExt) serviceContexts[i];
                if (log.isInfoEnabled())
                {
                    log.info("examining: " + context);
                }
                if (null != context && !context.isDestroyed() && context.getService() != null)
                {
                    ServiceExt serviceExt = (ServiceExt) context.getService();
                    ServiceExt easServiceExt = (ServiceExt)easService;
                    try
                    {
                        //compare service from servicedetails, not services (which may have different locators but represent the same service details)
                        if (easServiceExt.getDetails().getService().equals(serviceExt.getDetails().getService()))
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("Reusing existing servicecontext: " + context);
                            }
                            serviceAlreadyTuned = true;
                            this.m_serviceContext = context;
                            this.m_serviceContext.addListener(this);
                            this.m_mediaHandler = this.m_serviceContext.addServiceContextCallback(this, Integer.MAX_VALUE);
                            if (null != this.m_mediaHandler)
                            {
                                this.m_mediaHandler.setMediaTime(new Time(Double.POSITIVE_INFINITY)); // jump to "live" point
                            }
                        }
                    }
                    catch (SIRequestException e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("SIRequest exception - unable to retrieve service details - easService: " + easService + ", serviceContext service: " + serviceExt);
                        }
                    }
                    catch (InterruptedException e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Interrupted exception - unable to retrieve service details - easService: " + easService + ", serviceContext service: " + serviceExt);
                        }
                    }
                }
            }

            // otherwise, create a new service context and forcibly tune it to
            // the EAS service.
            if (null == this.m_serviceContext)
            {
                try
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Creating new servicecontext for: " + easService);
                    }
                    this.m_serviceContext = (ServiceContextExt) EASAlert.this.m_serviceContextFactory.createServiceContext();
                    this.m_serviceContext.addListener(this);
                    this.m_forceTune = true;

                    // Notify native that we are going to display the details
                    // channel. TODO: only for analog service?
                    displayAlert(true);

                }
                catch (InsufficientResourcesException e)
                {
                    SystemEventUtil.logRecoverableError("Failed to create an EAS service context", e);
                    this.m_alertCallback.presentationFailed();
                }
            }
            // Select and present the service (asynchronous call) - may already be presenting, but will update resource usage
            this.m_serviceContext.forceEASTune(easService);

            return serviceAlreadyTuned;
        }

        public void stop()
        {
            if (log.isInfoEnabled())
            {
                log.info("stop");
            }
            try
            {
                releaseServiceContext();
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled()) 
                {
                    log.warn("stop - problem releasing ServiceContext", e);
                }
            }
            m_alertCallback.presentationTerminated();
        }

        /**
         * If isForceTune is true, stops the current service context from presenting content and
         * releases any resources used in the presentation.
         * <p>
         * This operation completes asynchronously; a
         * {@link PresentationTerminatedEvent} is posted when the context stops.
         * <p>
         * If an {@link IllegalStateException} occurs, it indicates that the
         * service context was destroyed. This scenario is interpreted as the
         * presentation was stopped and resources were released. In the case,
         * the alert strategy is notified of presentation termination.
         */
        private void releaseServiceContext()
        {
            if (!m_serviceContext.isDestroyed())
            {
                //allow changes to ServiceContext and Player media time - must be called prior to calling SC.stop or destroy
                m_serviceContext.unforceEASTune();

                if (log.isInfoEnabled())
                {
                    log.info("releaseServiceContext");
                }

                //remove listeners prior to calling serviceContext.stop to avoid the presentationterminated notification from 
                //coming through this code again when PresentationTerminatedEvent is received by the ServiceContextListener
                m_serviceContext.removeServiceContextCallback(this);
                m_serviceContext.removeListener(this);

                try
                {
                    if (isForceTune())
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("was forceTune - stopping serviceContext");
                        }
                        //EAS created the ServiceContext - stop it
                        m_serviceContext.stop();
                    }
                }
                catch (IllegalStateException e)
                {
                    SystemEventUtil.logRecoverableError("EAS service context was destroyed before it could be stopped", e);
                }
                if (isForceTune())
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("releaseServiceContext - was forceTune - destroying serviceContext");
                    }
                    //EAS created the ServiceContext - stop it
                    m_serviceContext.destroy();
                }
            }

            m_serviceContext = null;
            m_mediaHandler = null;
        }
    }

    // Class Constants

    // Class Constants

    // Class Constants

    private static final Logger log = Logger.getLogger(EASAlert.class.getName());

    // Class Methods

    static
    {
        OcapMain.loadLibrary();
    }

    // Instance Fields

    protected final EASMessage m_easMessage;

    protected final EASState m_easState;

    protected final GraphicsManager m_graphicsManager;

    protected final ServiceContextFactoryExt m_serviceContextFactory;

    protected final SIManager m_siManager;

    // Constructors

    /**
     * Constructs a new instance of the receiver with a reference to the given
     * message.
     * 
     * @param state
     *            a {@link EASState} reference for method callbacks
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     */
    public EASAlert(final EASState state, final EASMessage message)
    {
        this.m_easMessage = message;
        this.m_easState = state;
        this.m_graphicsManager = (GraphicsManager) ManagerManager.getInstance(GraphicsManager.class);
        this.m_serviceContextFactory = (ServiceContextFactoryExt) ServiceContextFactory.getInstance();
        this.m_siManager = SIManager.createInstance();
    }

    // Instance methods

    /**
     * Returns the {@link EASMessage} instance currently being processed by this
     * alert strategy.
     * 
     * @return a reference to the current {@link EASMessage} being processed
     */
    public EASMessage getMessage()
    {
        return this.m_easMessage;
    }

    /**
     * Returns the time, in milliseconds, that the alert should be presented to
     * the user. This default implementation returns the
     * <code>alert_message_time_remaining</code> field value converted to
     * milliseconds.
     * <p>
     * Subclasses should override this method to return the length of an audio
     * or video stream if it is known.
     * 
     * @return the presentation of the alert, in milliseconds, or 0 if the
     *         presentation time is indefinite.
     */
    public long getPresentationTime()
    {
        return this.m_easMessage.getAlertMessageTimeRemaining() * 1000L;
    }

    /**
     * Returns the EAS event {@link EASEvent#getReason() reason} to use for
     * {@link EASListener} notifications and warnings.
     * 
     * @return either {@link EASEvent#EAS_DETAILS_CHANNEL} or
     *         {@link EASEvent#EAS_TEXT_DISPLAY} depending on the alert
     *         strategy.
     */
    public abstract int getReason();

    /**
     * Indicates whether a forced tune to an EAS details channel or audio
     * override source was done to present the EAS content. This default
     * implementation always returns false.
     * 
     * @return <code>true</code> if an force tune was done to present EAS
     *         content; otherwise <code>false</code>
     */
    public boolean isForceTune()
    {
        return false;
    }

    /**
     * Call back method indicating that the presentation failed to start. This
     * is usually due to a forced tune failure. This default implementation does
     * nothing.
     */
    public void presentationFailed()
    {
        // intentionally do nothing
    }

    /**
     * Call back method indicating that the presentation has started. This
     * default implementation transitions to the next state.
     */
    public void presentationStarted()
    {
        if (log.isInfoEnabled())
        {
            log.info("presentationStarted");
        }
        if (null != EASState.s_easManagerContext)
        {
            if (log.isInfoEnabled())
            {
                log.info("changing state to in progress and calling startPresentingAlert");
            }
            EASState.s_easManagerContext.changeState(EASStateInProgress.INSTANCE);
            EASState.s_easManagerContext.getCurrentState().startPresentingAlert();
        }
    }

    /**
     * Call back method indicating that the presentation has terminated. This
     * default implementation completes alert processing if the
     * {@link EASManagerImpl} context exists.
     * <p>
     * NOTE: the {@link EASState#s_easManagerContext} is <code>null</code> only
     * during unit testing.
     */
    public void presentationTerminated()
    {
        if (log.isInfoEnabled())
        {
            log.info("presentationTerminated");
        }
        if (null != EASState.s_easManagerContext)
        {
            final EASState currentState = EASState.s_easManagerContext.getCurrentState();
            if (log.isInfoEnabled())
            {
                log.info("calling completeAlert on: " + currentState);
            }
            currentState.completeAlert();
        }
    }

    /**
     * Processes the alert by acquiring any resources (e.g. tuner, decoder)
     * required by the alert strategy to present the alert. These resources may
     * be acquired asynchronously, synchronously, or not at all as needed by the
     * strategy implementation.
     */
    public abstract void processAlert();

    /**
     * Starts the alert presentation.
     */
    public abstract void startPresentation();

    /**
     * Stops the alert presentation, possibly blocking the current thread if the
     * presentation hasn't completed.
     */
    public void stopPresentation()
    {
        stopPresentation(false);
    }

    /**
     * Stops the alert presentation, possibly blocking the current thread if the
     * presentation hasn't completed. However, the presentation is
     * unconditionally stopped if <code>force</code> is <code>true</code>.
     * 
     * @param force
     *            <code>true</code> if the presentation should be stopped
     *            unconditionally
     */
    public abstract void stopPresentation(final boolean force);

    /**
     * Enables/disables displaying the alert via the native subsystem.
     * <p>
     * Originally reported in enableTV Bugzilla issue #1532, this was introduced
     * as a PowerTV fix to enable the native subsystem to switch the IEEE-1394
     * source from digital output to analog output to properly display the EAS
     * text scroll and when doing an EAS force-tune. This may be necessary if
     * the digital output is incapable of displaying graphics or analog services
     * (e.g. SA-3250HD).
     * <p>
     * The fix was refactored and applied in response to enableTV Bugzilla issue
     * #4226. The SA-8300HD was referenced as another device requiring this fix.
     * 
     * @param enable
     *            <code>true</code> when the EAS message is about to be
     *            displayed, and <code>false</code> when the message
     *            presentation is complete.
     */
    protected void displayAlert(final boolean enable)
    {
        nativeDisplayAlert(enable);
    }

    /**
     * Enables/disables displaying the alert via the native subsystem. Performs
     * any tasks in native code that need to be done immediately before and
     * after an alert is displayed.
     * 
     * @param enable
     *            <code>true</code> when the EAS message is about to be
     *            displayed, and <code>false</code> when the message
     *            presentation is complete.
     */
    private native void nativeDisplayAlert(boolean enable);
}
