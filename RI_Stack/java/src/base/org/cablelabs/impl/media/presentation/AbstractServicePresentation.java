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
package org.cablelabs.impl.media.presentation;

import java.util.LinkedList;

import javax.media.Time;
import javax.tv.locator.Locator;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.access.ComponentAuthorization;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.mpe.MediaAPIImpl;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.player.SessionChangeCallback;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.media.session.AlternativeContentSession;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.media.session.NoSourceException;
import org.cablelabs.impl.media.session.ServiceSession;
import org.cablelabs.impl.media.session.Session;
import org.cablelabs.impl.media.session.SessionListener;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.Arrays;
import org.davic.media.MediaFreezeException;
import org.dvb.media.DVBMediaSelectControl;
import org.dvb.media.PresentationChangedEvent;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.net.OcapLocator;
import org.ocap.service.AlternativeContentErrorEvent;
import org.ocap.service.S3DAlternativeContentErrorEvent;

abstract class AbstractServicePresentation extends AbstractVideoPresentation implements ServicePresentation
{
    private static final Logger log = Logger.getLogger(AbstractServicePresentation.class);

    private static final Logger performanceLog = Logger.getLogger("Performance.ServiceSelection");

    // state machine assumes stop events are synchronous (no callbacks)
    private static final int STATE_PRESENTATION_NOT_STARTED = 1;

    private static final int STATE_NORMAL_CONTENT_SESSION_STARTING = 2;

    private static final int STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION = 3;

    private static final int STATE_ALTERNATIVE_CONTENT_SESSION_STARTING = 4;

    private static final int STATE_NORMAL_CONTENT_SESSION_STARTED = 5;

    private static final int STATE_ALTERNATIVE_CONTENT_SESSION_STARTED = 6;

    //The active presentation is being updated (via MediaSelectControl or ServiceContext)
    private static final int STATE_NORMAL_CONTENT_SESSION_UPDATING = 7;

    private static final int STATE_PRESENTATION_SHUTDOWN = 8;

    /**
     * This holds {@link Selection}s that are queued by the
     * {@link #select(Selection)} method.
     */
    private SelectionQueue selectionQueue = new SelectionQueue();

    protected ServiceSession currentSession;

    // pendingSelection is set prior to a call to session.start or
    // session.select
    private Selection pendingSelection;

    // currentSelection is updated when content presenting is received
    private volatile Selection currentSelection;

    // will be null when not active (re-create each time to ensure we use the
    // correct video device)
    private AlternativeContentSession alternativeContentSession;

    //AlternativeContentErrorEvent or a subclass,
    // set the class so the correct event can be triggered when altcontent has started
    private Class alternativeContentClass;

    //AlternativeContentErrorEvent or subclass reason code (has to be a valid value for alternativeContentClass)
    // set the reason code so the correct event can be triggered when altcontent has started
    private int alternativeContentReasonCode;

    //alternative content mode - see alternative content mode constants in ServicePresentation
    private int alternativeContentMode;

    private final Object lock;

    private int state = STATE_PRESENTATION_NOT_STARTED;

    protected final SessionListenerImpl sessionListener;

    private static final int NO_NOTIFICATION = 1;

    private static final int ALTERNATIVE_CONTENT_NOTIFICATION = 2;

    private static final int NORMAL_CONTENT_NOTIFICATION = 3;

    private int lastNotification = NO_NOTIFICATION;

    private static final int WAIT_FOR_SESSION_CHANGE_TO_COMPLETE_TIMEOUT = 15000;

    protected final Time startMediaTime;
    
    protected final float startRate;
    
    //track attempts to create a session to prevent loops
    private int presentationChangedCount;

    /**
     * Construct a {@link AbstractVideoPresentation}.
     * 
     * @param servicePresentationContext
     *            {@link VideoPresentationContext} to use.
     * @param isShown
     *            indicates whether to show video initially.
     * @param initialSelection
     *            the selection activated when start() is called
     * @param bounds
     *            the scaling bounds
     * @param startMediaTime
     *            the initial requested mediatime
     * @param startRate 
     *            the initial requested rate
     */
    protected AbstractServicePresentation(ServicePresentationContext servicePresentationContext, boolean isShown,
            Selection initialSelection, ScalingBounds bounds, Time startMediaTime, float startRate)
    {
        super(servicePresentationContext, isShown, bounds);
        currentSelection = initialSelection;
        this.startMediaTime = startMediaTime;
        this.startRate = startRate;
        lock = servicePresentationContext.getLock();
        sessionListener = new SessionListenerImpl();
    }

    /**
     * All calls to update the selection via DvbMediaSelectControl (or
     * ServiceContext.select for a currently presenting service) will come
     * through here.
     * 
     * Selection trigger:
     * MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE_COMPONENTS
     * 
     * @param selection
     *            the selection containing new components to select
     */
    public void select(Selection selection)
    {
        synchronized (lock)
        {
            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                case STATE_PRESENTATION_SHUTDOWN:
                    throw new IllegalStateException("select called in unexpected state: " + stateToString(state));
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    // continue below
                    break;
                default:
                    throw new IllegalStateException("select called in unknown state: " + stateToString(state));
            }
            if (log.isInfoEnabled())
            {
                log.info("select - enqueuing selection: " + selection);
            }
            //update state to ensure waitForCurrentSelection waits until this selection is complete
            updateState(STATE_NORMAL_CONTENT_SESSION_UPDATING);
            selectionQueue.addSelection(selection);
            context.getTaskQueue().post(new SelectTask());
        }
    }

    public Selection getCurrentSelection()
    {
        synchronized (lock)
        {
            return currentSelection;
        }
    }

    public void reselect(MediaPresentationEvaluationTrigger trigger)
    {
        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("reselect(" + trigger + ") - current state: " + stateToString(state) + ", current selection: "
                        + currentSelection);
            }
            // if we are reselecting due to altcontent, always bypass the PMT
            // pid comparison logic
            // if in NORMAL_CONTENT_SESSION_STARTING, we don't yet have a
            // currentSelection - base the new selection on the pending
            // selection instead
            boolean bypassPMTCheck = false;
            Selection activeSelection = null;
            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                {
                    //if the NetworkConditionMonitor receives a tune sync when shut down (due to service remap), proceed,
                    //otherwise, ignore
                    if (!SelectionTrigger.RESOURCES.equals(trigger))
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("ignoring reselect in: " + stateToString(state));
                        }
                        return;
                    }
                    activeSelection = (pendingSelection != null ? pendingSelection : currentSelection);
                    //continue below
                    break;
                }
                case STATE_PRESENTATION_SHUTDOWN:
                    if (log.isWarnEnabled())
                    {
                        log.warn("ignoring reselect in: " + stateToString(state));
                    }
                    return;
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                    // continue below
                    activeSelection = pendingSelection;
                    break;
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                    bypassPMTCheck = true;
                    // we may have a pending selection or a current selection...
                    activeSelection = (pendingSelection != null ? pendingSelection : currentSelection);
                    // continue below
                    break;
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    activeSelection = currentSelection;
                    // continue below
                    break;
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                    bypassPMTCheck = true;
                    // we may have a pending selection or a current selection...
                    activeSelection = (pendingSelection != null ? pendingSelection : currentSelection);
                    // continue below
                    break;
                default:
                    throw new IllegalStateException("reselect called in unknown state: " + stateToString(state));
            }

            // Determine components/locators for new selection.
            // get the current components before calling getServiceComponents
            // depending on when reselect is called, currentComponents may be
            // null
            ServiceComponentExt[] currentComponents = activeSelection.getCurrentComponents();
            ServiceComponentExt[] newComponents = activeSelection.getServiceComponents();
            ServiceDetailsExt serviceDetails = activeSelection.getServiceDetails();
            Locator[] locators = activeSelection.getLocators();

            // If re-selecting for a Service selection using default components,
            // log if we couldn't find components (will proceed to alt content)
            if (serviceDetails != null && activeSelection.isDefault()
                    && (newComponents.length == 0 && !serviceDetails.isAnalog()))
            {
                if (log.isWarnEnabled())
                {
                    log.warn("could not retrieve default components for current selection: " + activeSelection);
                }
            }

            // Create new Selection from current one
            Selection newSelection = new Selection(trigger, serviceDetails, locators);
            //ensure CA authorization is preserved if it was set in the previous selection
            //media access authorization will be re-set
            newSelection.setConditionalAccessAuthorization(activeSelection.getConditionalAccessComponentAuthorization());
            if (bypassPMTCheck)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("bypassing PMT check");
                }
            }
            // A reselection due to a PMT change should only proceed if the PIDs
            // (components) currently being decoded are changed by the PMT change or if there is a change in CCI. If we're in
            // alternative content, don't perform this check.
            if (!bypassPMTCheck && trigger == MediaPresentationEvaluationTrigger.PMT_CHANGED)
            {
                boolean cciUpdated = isCCIUpdated();
                // If the length differs, they must be different.
                boolean componentsUpdated = currentComponents == null || (currentComponents.length != newComponents.length);
                //if CCI hasn't changed, check components
                // If the length is the same, then they might be identical.
                if (!cciUpdated && !componentsUpdated)
                {
                    // Check each component in the current Selection to see if
                    // it exists in
                    // new Selection. Comparison is based on PIDs.
                    for (int i = 0; !componentsUpdated && i < currentComponents.length; ++i)
                    {
                        // Check for a matching component in the new selection.
                        boolean found = false; // assume not found until proven
                                               // otherwise
                        int curPID = currentComponents[i].getPID();
                        for (int j = 0; !found && j < newComponents.length; ++j)
                        {
                            found = (curPID == newComponents[j].getPID());
                        }
                        componentsUpdated  = !found;
                    }
                }
                if (cciUpdated || componentsUpdated)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("reselect due to component or CCI update - components updated: " + componentsUpdated + ", cci updated: " + cciUpdated);
                    }
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("skipping PMT-based reselect because components and CCI did not change");
                    }
                    return;
                }

            }
            if (log.isInfoEnabled())
            {
                log.info("reselect - enqueuing selection: " + newSelection);
            }

            selectionQueue.addSelection(newSelection);
            context.getTaskQueue().post(new SelectTask());
        }
    }

    //default implementation
    protected boolean isCCIUpdated()
    {
        return false;
    }

    public void freeze() throws MediaFreezeException
    {
        synchronized (lock)
        {
            waitForSessionChangeToComplete();
            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                case STATE_PRESENTATION_SHUTDOWN:
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                    throw new MediaFreezeException("presentation not started - current state: " + stateToString(state));
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    try
                    {
                        currentSession.freeze();
                    }
                    catch (MPEException x)
                    {
                        throw new MediaFreezeException(x.toString());
                    }
                    break;
                default:
                    throw new IllegalStateException("freeze called in unknown state: " + stateToString(state));
            }
        }
    }

    public void resume() throws MediaFreezeException
    {
        synchronized (lock)
        {
            waitForSessionChangeToComplete();
            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                case STATE_PRESENTATION_SHUTDOWN:
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                    throw new MediaFreezeException("presentation not started - current state: " + stateToString(state));
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    try
                    {
                        currentSession.resume();
                    }
                    catch (MPEException x)
                    {
                        throw new MediaFreezeException(x.toString());
                    }
                    break;
                default:
                    throw new IllegalStateException("resume called in unknown state: " + stateToString(state));
            }
        }
    }

    public void swap(ServicePresentation other)
    {
        // gather presentation state needed to the update other presentation and
        // update this presentation while holding this lock,
        // but update the other presentation via acceptSwappedSettings(which
        // will acquire its own lock)
        VideoDevice thisDevice = null;
        ScalingBounds thisBounds = null;
        boolean thisShow = false;

        boolean updateOtherDevice = false;
        synchronized (lock)
        {
            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                case STATE_PRESENTATION_SHUTDOWN:
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                    throw new IllegalStateException("swap called in unexpected state: " + stateToString(state));
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    if (other instanceof AbstractServicePresentation)
                    {
                        updateOtherDevice = true;
                        thisDevice = getVideoDevice();
                        thisBounds = getBounds();
                        thisShow = getShowVideo();
                        AbstractServicePresentation otherPres = (AbstractServicePresentation) other;
                        acceptSwappedSettings(otherPres.getVideoDevice(), otherPres.getBounds(),
                                otherPres.getShowVideo());
                    }
                    else
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Unable to swap - passed-in presentation is not an AbstractServicePresentation: "
                                    + other);
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("swap called in unknown state: " + stateToString(state));
            }
        }
        // outside of lock
        if (updateOtherDevice)
        {
            ((AbstractServicePresentation) other).acceptSwappedSettings(thisDevice, thisBounds, thisShow);
        }
    }

    public int getAspectRatio()
    {
        synchronized (lock)
        {
            return getMediaAPI().getAspectRatio(getVideoDevice().getHandle());
        }
    }

    public int getActiveFormatDefinition()
    {
        synchronized (lock)
        {
            return getMediaAPI().getActiveFormatDefinition(getVideoDevice().getHandle());
        }
    }

    public int getDecoderFormatConversion()
    {
        synchronized (lock)
        {
            return getMediaAPI().getDFC(getVideoDevice().getHandle());
        }
    }

    public boolean checkDecoderFormatConversion(int dfc)
    {
        synchronized (lock)
        {
            return getMediaAPI().checkDFC(getVideoDevice().getHandle(), dfc);
        }
    }

    protected int getAlternativeContentReasonCode()
    {
        synchronized(lock)
        {
            return alternativeContentReasonCode;
        }
    }

    public int getSessionHandle()
    {
        synchronized (lock)
        {
            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                case STATE_PRESENTATION_SHUTDOWN:
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                    return Session.INVALID;
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    return currentSession.getNativeHandle();
                default:
                    throw new IllegalStateException("getSessionHandle called in unknown state: " + stateToString(state));
            }
        }
    }

    //The Context.notifyStart call is made in presentPendingSelection if the trigger is new_selected_service, ensuring
    //the player transitions to the started state.
    protected void doStart()
    {
        synchronized (lock)
        {
            if (Asserting.ASSERTING)
            {
                Assert.preCondition(currentSession == null);
                Assert.preCondition(currentSelection != null);
            }
            if (log.isInfoEnabled())
            {
                log.info("doStart - clock mediatime: " + startMediaTime + ", clock rate: " + startRate);
            }

            startNewSessionAsync(currentSelection, MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE, startMediaTime, startRate, false, false);
        }
    }

    protected void releaseResources(boolean shuttingDown)
    {
        super.releaseResources(shuttingDown);
    }

    //called by the presentation implementations - will perform a state check and call the overridden doStopInternal if needed
    protected void doStop(boolean shuttingDown)
    {
        if (log.isDebugEnabled())
        {
            log.debug("doStop - shutting down: " + shuttingDown);
        }
        boolean callStopInternal;
        synchronized(lock)
        {
            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                    //already stopped - only stop if we are shutting down
                    callStopInternal = shuttingDown;
                    break;
                case STATE_PRESENTATION_SHUTDOWN:
                    //already shutdown
                    callStopInternal = false;
                    break;
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    callStopInternal = true;
                    // continue below
                    break;
                default:
                    throw new IllegalStateException("doStopInternal called in unknown state: " + stateToString(state));
            }
            if (log.isInfoEnabled())
            {
                log.info("doStop - calling doStopInternal: " + callStopInternal);
            }
            if (callStopInternal)
            {
                doStopInternal(shuttingDown);
            }
        }
    }

    protected void doStopInternal(boolean shuttingDown)
    {
        synchronized (lock)
        {
            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                    if (shuttingDown)
                    {
                        updateState(STATE_PRESENTATION_SHUTDOWN);
                    }
                    // continue below
                    break;
                case STATE_PRESENTATION_SHUTDOWN:
                    //ignore - already shutdown
                    return;
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    if (shuttingDown)
                    {
                        updateState(STATE_PRESENTATION_SHUTDOWN);
                    }
                    else
                    {
                        updateState(STATE_PRESENTATION_NOT_STARTED);
                    }
                    // continue below
                    break;
                default:
                    throw new IllegalStateException("doStopInternal called in unknown state: " + stateToString(state));
            }
            // may have been starting alt content, so stop if we are now started
            if (alternativeContentSession != null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("doStopInternal - stopping alternativeContentSession and setting to null");
                }
                alternativeContentSession.stop(false);
                alternativeContentSession = null;
            }
            // we may not have a currentSession
            if (currentSession != null)
            {
                ((SessionChangeCallback) context).notifyStoppingSession(currentSession.getNativeHandle());
                if (log.isInfoEnabled())
                {
                    log.info("doStopInternal - shutting down: " + shuttingDown + " - stopping currentSession and setting to null: " + currentSession);
                }
                //hold the last frame if this stop is not due to a call to presentation.stop()
                currentSession.stop(!shuttingDown);
                currentSession = null;
                if (log.isDebugEnabled())
                {
                    log.debug("doStopInternal - sessionStarted flag set to false");
                }
            }
        }
    }

    public String toString()
    {
        return "AbstractServicePresentation: " + Integer.toHexString(hashCode()) + ", state: " + state;
    }

    public Selection waitForCurrentSelection()
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("entering waitForCurrentSelection");
            }
            waitForSessionChangeToComplete();
            if (log.isDebugEnabled())
            {
                log.debug("leaving waitForCurrentSelection - returning: " + currentSelection);
            }
            return currentSelection;
        }
    }

    protected void startNewSession(final Selection selection, final MediaPresentationEvaluationTrigger trigger,
                                   final Time mediaTime, final float rate, boolean mediaTimeChangeRequest, boolean rateChangeRequest)
    {
        int statePriorToMediaAuthorization = 0;
        synchronized (lock)
        {
            if (selection == null || trigger == null)
            {
                // we didn't start, so fail
                closePresentation("selection or trigger null - unable to start session", null);
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug("startNewSession - trigger: " + trigger + ", mediatime: " + mediaTime + ", rate: " + rate + ", current state: " + stateToString(state) + ", selection: " + selection);
            }

            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                    // continue below
                    break;
                case STATE_PRESENTATION_SHUTDOWN:
                    if (log.isInfoEnabled())
                    {
                        log.info("ignoring startNewSession in " + stateToString(state));
                    }
                    return;
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                    // may be called by the session start logic trying to initiate start of a new session due to native failure
                    //continue below (don't wait for session change)
                    break;
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                    waitForSessionChangeToComplete();
                    break;
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                    break;
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    //don't wait for completion here - in startNewSession due to an update
                    break;
                default:
                    throw new IllegalStateException("startNewSession called in unknown state: " + state);
            }
            //NOTE: state may have changed due to waitForSessionChange

            // select has (possibly updated) locators, just set the trigger
            selection.setTrigger(trigger);

            try
            {
                updateSelectionDetails(selection, mediaTime, rate);

                //resource availability verification requires resource monitoring to be initialized
                //always do this even if components aren't valid
                if (log.isDebugEnabled())
                {
                    log.debug("initializing resource monitoring");
                }

                initializeResources(selection);
                //perform resource validation followed by component validation
                boolean resourcesValid = validateResources();
                if (log.isInfoEnabled())
                {
                    log.info("resourcesValid: " + resourcesValid);
                }
                boolean componentsValid = false;
                if (resourcesValid)
                {
                    componentsValid = validateComponents(selection);
                    if (log.isInfoEnabled())
                    {
                        log.info("componentsValid: " + componentsValid);
                    }
                }
                //went to altcontent in validate resources or validate components
                if (!componentsValid || !resourcesValid)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("session not valid or resources not available - not starting session");
                    }
                    //use requested mediatime and rate
                    context.clockSetRate(rate, false);
                    context.clockSetMediaTime(mediaTime, mediaTimeChangeRequest);
                    return;
                }

                //determine conditional access authorization
                if (conditionalAccessAuthorizationRequired())
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("CA authorization is required for presentation");
                    }
                    updateConditionalAccessAuthorization(selection);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("CA authorization is not required for presentation - not updating CA authorization");
                    }
                }

                if (conditionalAccessAuthorizationRequired() && !selection.getConditionalAccessComponentAuthorization().isFullAuthorization())
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info("CA authorization is required but CA is not fully authorized");
                    }
                    //if already in alternative content due to conditional access, send authorization
                    if (state == STATE_ALTERNATIVE_CONTENT_SESSION_STARTED && alternativeContentReasonCode == AlternativeContentErrorEvent.CA_REFUSAL)
                    {
                        currentSelection = selection;
                        ServicePresentationContext ctx = (ServicePresentationContext)context;
                        ctx.notifyMediaAuthorization(currentSelection.getConditionalAccessComponentAuthorization());
                    }
                    else
                    {
                        pendingSelection = selection;
                        switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CA_REFUSAL);
                    }
                    //use requested mediatime and rate
                    context.clockSetRate(rate, false);
                    context.clockSetMediaTime(mediaTime, mediaTimeChangeRequest);
                    return;
                }
            }
            catch (Throwable t)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("exception starting new session", t);
                }
                closePresentation("Unable to start new session: " + t.getMessage(), t);
                return;
            }
            if (!rateChangeRequest)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("session start was not due to a rate change request - verifying media authorization");
                }
                statePriorToMediaAuthorization = state;
                updateState(STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION);
            }
            //set pending selection before releasing this lock
            pendingSelection = selection;
        }

        //don't hold the lock during this call to check media access authorization
        //if change is due to rate change, do not check authorization
        if (!rateChangeRequest)
        {
            //perform media access authorization
            if (mediaAccessAuthorizationRequired())
            {
                if (log.isInfoEnabled())
                {
                    log.info("Media access authorization is required for presentation");
                }
                updateMediaAccessAuthorization(selection);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Media access authorization is not required for presentation - not updating media access authorization");
                }
            }
        }

        boolean presentationChanged = false;
        Time presentationChangedTime = null;
        float presentationChangedRate = 0.0F;
        
        synchronized(lock)
        {
            //lock re-acquired - check state - presentation may be shut down
            if (state == STATE_PRESENTATION_SHUTDOWN)
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("presentation shut down - not initiating presentation of pending selection");
                }
                return;
            }
            //if trigger was rate change, state wasn't changed to starting authorization
            if (!rateChangeRequest)
            {
                if (state != STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("selection request started while verify media access authorization - no longer processing selection request: " + selection);
                    }
                    return;
                }
                updateState(statePriorToMediaAuthorization);
            }
            try
            {
                if (mediaAccessAuthorizationRequired() && !selection.getMediaAccessComponentAuthorization().isFullAuthorization())
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Media access authorization is required but media access is not fully authorized");
                    }

                    //if already in alternative content due to rating, send authorization
                    if (state == STATE_ALTERNATIVE_CONTENT_SESSION_STARTED && alternativeContentReasonCode == AlternativeContentErrorEvent.RATING_PROBLEM)
                    {
                        currentSelection = selection;
                        pendingSelection = null;
                        ServicePresentationContext ctx = (ServicePresentationContext)context;
                        ctx.notifyMediaAuthorization(currentSelection.getMediaAccessComponentAuthorization());
                    }
                    else
                    {
                        switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.RATING_PROBLEM);
                    }
                    //use requested mediatime and rate
                    context.clockSetRate(rate, false);
                    context.clockSetMediaTime(mediaTime, mediaTimeChangeRequest);
                    return;
                }

                //at this point, components and resources are valid and presentation is not blocked due to CA or MAH
                if (log.isDebugEnabled())
                {
                    log.debug("pendingSelection updated to: " + pendingSelection);
                }
                if (MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE_COMPONENTS.equals(pendingSelection.getTrigger()) ||
                        SelectionTrigger.SERVICE_CONTEXT_RESELECT.equals(pendingSelection.getTrigger()))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("new selected service components - updating session with pending selection: " + pendingSelection);
                    }
                    //session already exists - update the presentation
                    updateState(STATE_NORMAL_CONTENT_SESSION_STARTING);
                    //startNewSession & update is due to NEW_SELECTED_SERVICE_COMPONENTS, not a mediatime change - no need to call clockSetMediaTime
                    doUpdateSession(pendingSelection);
                }
                else if (MediaPresentationEvaluationTrigger.USER_RATING_CHANGED.equals(pendingSelection.getTrigger()) && state == STATE_NORMAL_CONTENT_SESSION_STARTED)
                {
                    //applications may trigger reselection due to user rating but authorization will be null
                    //unless mediaAccessAuthorizationRequired returns true
                    if (mediaAccessAuthorizationRequired())
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("user rating change - media access authorization was full auth and presenting normal content - notifying media authorization");
                        }
                        ServicePresentationContext ctx = (ServicePresentationContext)context;
                        ctx.notifyMediaAuthorization(pendingSelection.getMediaAccessComponentAuthorization());
                    }
                    else
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("user rating change - authorization not required - not notifying media authorization");
                        }
                    }
                    currentSelection = pendingSelection;
                    pendingSelection = null;
                    //startNewSession & update is due to RATING trigger, not a mediatime change - no need to call clockSetMediaTime
                }
                else
                {
                    //always stop a prior session if it is active
                    doStop(false);
                    //update state to STARTING immediately after stopping
                    updateState(STATE_NORMAL_CONTENT_SESSION_STARTING);
                    //doCreateSession will provide an updated requested mediatime in the CreateSessionResult if necessary (in that case, a null session will be provided)...
                    // if presentation point changes, re-validate
                    CreateSessionResult result = doCreateSession(selection, mediaTime, rate);
                    if (result.transitionToAlternativeContent)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("startNewSession - transition to alternative content");
                        }
                        switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, result.alternativeContentClass, result.alternativeContentErrorCode);
                    }
                    if (result.presentationRequestChanged)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("startNewSession - presentation request changed - new time: " + result.mediaTime + ", rate: " + result.rate);
                        }
                        presentationChanged = true;
                        presentationChangedTime = result.mediaTime;
                        presentationChangedRate = result.rate;
                    }
                    else
                    {
                        currentSession = result.session;
                        //presentation did not change, use requested mediatime and rate
                        presentPendingSelection(mediaTime, rate, mediaTimeChangeRequest, trigger);
                    }
                }
            }
            catch (NoSourceException x)
            {
                // The source to be used was removed before the session could be
                // started.
                ((ServicePresentationContext) context).notifyNoSource(x.toString(), x);
                closePresentation("Unable to start new session: " + x.getMessage(), x);
            }
            catch (Throwable t)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("exception starting new session", t);
                }
                closePresentation("Unable to start new session: " + t.getMessage(), t);
            }
        }

        //must call while not holding the lock to prevent deadlocks
        //release monitoring - start a new session with a different mediatime and rate (may or may not re-register resource monitors)
        if (presentationChanged)
        {
            if (presentationChangedCount > 0)
            {
                closePresentation("Unable to start new session - failed to create an updated session", new RuntimeException());
            }
            else
            {
                presentationChangedCount++;
                releaseResources(false);
                //presentation request was changed, run startNewSession in PRESENTATION_NOT_STARTED state
                updateState(STATE_PRESENTATION_NOT_STARTED);
                startNewSession(selection, selection.getTrigger(), presentationChangedTime, presentationChangedRate, mediaTimeChangeRequest, rateChangeRequest);
            }
        }
        else
        {
            presentationChangedCount = 0;
        }
    }

    /**
     * Asynchronously perform session validation - start the decode session if validation succeeds or switch to alternative content 
     *
     * @param selection
     * @param trigger
     * @param mediaTime
     * @param rate
     * @param mediaTimeChangeRequest
     * @param rateChangeRequest
     */
    protected void startNewSessionAsync(final Selection selection, final MediaPresentationEvaluationTrigger trigger,
                                        final Time mediaTime, final float rate, final boolean mediaTimeChangeRequest, final boolean rateChangeRequest)
    {
        if (log.isDebugEnabled())
        {
            log.debug("startNewSessionAsync - prior to run - trigger: " + trigger + ", mediatime: " + mediaTime + ", rate: " + rate + ", current state: " + stateToString(state) + ", selection: " + selection);
        }
        ((CallerContextManager) ManagerManager.getInstance(CallerContextManager.class)).getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                if (log.isDebugEnabled())
                {
                    log.debug("startNewSessionAsync - trigger: " + trigger + ", mediatime: " + mediaTime + ", rate: " + rate + ", current state: " + stateToString(state) + ", selection: " + selection);
                }
                startNewSession(selection, trigger, mediaTime, rate, mediaTimeChangeRequest, rateChangeRequest);
            }
        });
    }

    private void presentPendingSelection(Time mediaTime, float rate, boolean mediaTimeChangeRequest, MediaPresentationEvaluationTrigger trigger) throws MPEException, NoSourceException
    {
        synchronized(lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("presentPendingSelection - pending selection: " + pendingSelection + ", mediatime: " + mediaTime + ", rate: " + rate + ", mediatime change request: " + mediaTimeChangeRequest + ", trigger: " + trigger);
            }
            
            if (performanceLog.isInfoEnabled())
            {
                performanceLog.info("Decode: Locator " +  pendingSelection.getServiceDetails().getLocator().toExternalForm());
            }
            
            //doStartSession must ensure the clock mediatime and rate are correct, even if a regular session wasn't started
            doStartSession(currentSession, pendingSelection, mediaTime, rate, mediaTimeChangeRequest, trigger);
            
            if (log.isInfoEnabled())
            {
                log.info("currentSession set to: " + currentSession);
            }
            //doStartSession may have resulted in alternative content (current session would be defined but not started)
            if (isSessionStarted())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("notifying starting session");
                }
                // Notify the context of session startup.
                // ServicePresentationContext is a SessionChangeCallback
                ((SessionChangeCallback) context).notifyStartingSession(currentSession.getNativeHandle());
                //if presenting a pending service and trigger is new service, notify started (transition player to started state)
                if (MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE.equals(pendingSelection.getTrigger()))
                {
                    context.notifyStarted();
                }
                //on initial selection, if the start rate does not match the rate provided by the session,
                // update the player rate and post a RateChangeEvent
                if (trigger == MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE && startRate != currentSession.getRate())
                {
                    context.clockSetRate(currentSession.getRate(), true);
                }

            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("session was not started (altcontent was started) - not notifying starting session");
                }
            }
        }
    }

    /**
     * Subclasses requiring authorization via a registered MediaAccessHandler must authorize the selection with the MediaAccessHandler and then
     * update the authorization on the selection
     *
     * Presentation of all broadcast components must be authorized by a registered MediaAccessHandler
     * (presentation of unbuffered or buffered broadcast services (live point and tsb presentation, live point in-progress recordings and broadcast presentations)
     *
     * @param selection the selection to authorize and update
     */
    protected void updateMediaAccessAuthorization(Selection selection)
    {
        // default implementation
    }

    /**
     * Subclasses requiring authorization via conditional access must determine access provided by the CA system and
     * update the authorization on the selection
     *
     * Presentation of broadcast components -at the live point- must be authorized by the CA system
     * including presentation of unbuffered broadcast services (live point tsb presentation,
     * live point in-progress recordings and broadcast presentations)
     *
     * Override by subclasses if needed (default conditional access authorization required: false)
     *
     * @param selection the selection to authorize and update
     */
    protected void updateConditionalAccessAuthorization(Selection selection) throws MPEException
    {
        // default implementation
    }

    protected void updateSelectionDetails(Selection selection, Time mediaTime, float rate)
    {
        // default is a no-op
    }

    protected boolean componentsValid(Selection selection)
    {
        synchronized (lock)
        {
            try
            {
                ServiceComponentExt[] components = selection.getServiceComponents();
                // treat analog or component length > 0 as valid
                return !selection.isDigital() || components != null && components.length > 0;
            }
            catch (Throwable th)
            {
                return false;
            }
        }
    }

    /**
     * Validate components, and generates an
     * {@link AlternativeContentErrorEvent} if required
     * <p/>
     * 
     * @param selection
     *            the selection containing components
     * 
     * @return true if the selection's components are valid
     */
    protected boolean validateComponents(Selection selection)
    {
        synchronized (lock)
        {
            if (!componentsPresentable(selection))
            {
                if (log.isInfoEnabled())
                {
                    log.info("initial components not presentable - selection: " + selection);
                }
                switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                return false;
            }
            return true;
        }
    }

    /**
     * Validate resources (if applicable), and generate an
     * {@link AlternativeContentErrorEvent} if required
     * 
     * @return true if resources were validated
     */
    protected boolean validateResources()
    {
        // no-op, override if necessary (and post events in this method if
        // needed)
        return true;
    }

    protected void initializeResources(Selection selection)
    {
        // no-op, override if necessary
    }

    /**
     * Block until the session start finishes, due to successful start or error.
     * 
     */
    protected void waitForSessionChangeToComplete()
    {
        synchronized (lock)
        {
            long startTime = System.currentTimeMillis();
            int waitMillis = 30;
            // if we don't receive an event within 15 seconds, unblock and log
            // the warning (we don't expect this, but it will
            // allow the stack to continue)
            int loopCount = 0;
            while (true)
            {
                loopCount++;
                // log every 1500 ms
                if (loopCount % 50 == 0)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("waitForCurrentSessionChangeToComplete - waiting - current state: "
                                + stateToString(state));
                    }
                }

                switch (state)
                {
                    case STATE_PRESENTATION_NOT_STARTED:
                    case STATE_PRESENTATION_SHUTDOWN:
                        return;
                    case STATE_NORMAL_CONTENT_SESSION_STARTING:
                    case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                    case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                    case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                        // keep waiting
                        break;
                    case STATE_NORMAL_CONTENT_SESSION_STARTED:
                    case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                        return;
                    default:
                        throw new IllegalStateException("waitForSessionChangeToComplete called in unexpected state: "
                                + stateToString(state));
                }

                try
                {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime > WAIT_FOR_SESSION_CHANGE_TO_COMPLETE_TIMEOUT)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("waitForSessionChangeToComplete timed out - current state: "
                                    + stateToString(state) + ", current session: " + currentSession, new RuntimeException());
                        }
                        return;
                    }
                    lock.wait(waitMillis);
                }
                catch (InterruptedException ex)
                {
                    // ignore
                }
            }
        }
    }

    protected String eventToString(int event)
    {
        return MediaAPIImpl.eventToString(event);
    }

    public void switchToAlternativeContent(int alternativeContentMode, Class alternativeContentClass, int alternativeContentReasonCode)
    {
        boolean alreadyStartingAlternativeContent = false;
        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("switchToAlternativeContent - class: " + alternativeContentClass.getName() + ", reason: " + alternativeContentReasonCode +
                        ", alternativeContent mode: " + alternativeContentMode + ", current state: "
                        + stateToString(state));
            }
            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                    //continue below
                    break;
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                    // don't wait - may enter here on initial attempt to present
                    // the selection
                    // keep current session active but stop decoding

                    // we are going from normalcontent to alternative content
                    // due to issues with the network or stream - post
                    // PresentationChangedEvent controllerEvent
                    if (alternativeContentReasonCode == AlternativeContentErrorEvent.CONTENT_NOT_FOUND
                            || alternativeContentReasonCode == AlternativeContentErrorEvent.TUNING_FAILURE)
                    {
                        ((ServicePresentationContext) context).notifyPresentationChanged(PresentationChangedEvent.STREAM_UNAVAILABLE);
                    }
                    //continue below
                    break;
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                    alreadyStartingAlternativeContent = true;
                    break;
                //a transition to altcontent while verifying authorization should stop the current selection and transition to altcontent
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    // transition from normalcontent to alternative content
                    // due to issues with the network or stream - post
                    // PresentationChangedEvent controllerEvent
                    if (alternativeContentReasonCode == AlternativeContentErrorEvent.CONTENT_NOT_FOUND
                            || alternativeContentReasonCode == AlternativeContentErrorEvent.TUNING_FAILURE)
                    {
                        ((ServicePresentationContext) context).notifyPresentationChanged(PresentationChangedEvent.STREAM_UNAVAILABLE);
                    }
                    //continue below
                    break;
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                    //may require decoder stop
                    //not valid to go from current altcontent state with keepDecoderAlive=false to altcontent keepdecoderalive=true (need a reselect to start up the decode session)
                    //continue below
                    break;
                case STATE_PRESENTATION_SHUTDOWN:
                    if (log.isWarnEnabled())
                    {
                        log.warn("ignoring switchToAlternativeContent in " + stateToString(state));
                    }
                    return;
                default:
                    throw new IllegalStateException("switchToAlternativeContent called in unknown state: " + state);
            }
        
            //starting altcontent with a mode with keepdecoderalive=true when already presenting altcontent with keepdecoderalive=false is not valid - reselect instead
            //will start altcontent but only if class, reason or mode have changed
            if (state == STATE_ALTERNATIVE_CONTENT_SESSION_STARTED && (this.alternativeContentReasonCode == alternativeContentReasonCode) && 
                    (this.alternativeContentClass != null && this.alternativeContentClass.equals(alternativeContentClass)) &&
                    this.alternativeContentMode == alternativeContentMode)
            {
                if (log.isInfoEnabled())
                {
                    log.info("switchToAlternativeContent called but already presenting alternative content with same reason, class and mode - ignoring");
                }
                return;
            }
            
            //may have waited for a session change - check current state to see if a notification is needed
            boolean classChanged = (this.alternativeContentClass == null || (!(this.alternativeContentClass.equals(alternativeContentClass))));
            boolean reasonChanged = (this.alternativeContentReasonCode != alternativeContentReasonCode);
            //only post altcontent event if we aren't already in altcontent for the current reason or class
            boolean notify = (state != STATE_ALTERNATIVE_CONTENT_SESSION_STARTED || classChanged || reasonChanged);

            this.alternativeContentMode = alternativeContentMode;
            this.alternativeContentClass = alternativeContentClass;
            this.alternativeContentReasonCode = alternativeContentReasonCode;

            if (log.isDebugEnabled()) 
            {
                log.debug("notifying alternative content: " + notify + ", class: " + alternativeContentClass + ", reason: " + alternativeContentReasonCode);
            }
            //requires decoder to already be active (current session not null)
            if (ALTERNATIVE_CONTENT_MODE_RENDER_BLACK == alternativeContentMode)
            {
                //no alternativecontentsession (decode session is active)
                if (log.isInfoEnabled())
                {
                    log.info("blocking presentation: " + currentSession);
                }
                //block is synchronous - notify altcontent immediately after blocking presentation
                currentSession.blockPresentation(true);
                if (notify)
                {
                    notifyAlternativeContent(alternativeContentClass, alternativeContentReasonCode);
                    updateState(STATE_ALTERNATIVE_CONTENT_SESSION_STARTED);
                }
            }
            //requires decoder to already be active (current session not null)
            else if (ALTERNATIVE_CONTENT_MODE_RENDER_VIDEO == alternativeContentMode)
            {
                if (log.isInfoEnabled())
                {
                    log.info("unblocking presentation: " + currentSession);
                }
                //ensure current session is not blocked
                currentSession.blockPresentation(false);
                if (notify)
                {
                    notifyAlternativeContent(alternativeContentClass, alternativeContentReasonCode);
                    updateState(STATE_ALTERNATIVE_CONTENT_SESSION_STARTED);
                }
            }
            else if (ALTERNATIVE_CONTENT_MODE_STOP_DECODE == alternativeContentMode)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("stopping decode of current session and starting altcontent session");
                }
                if (notify  && !alreadyStartingAlternativeContent)
                {
                    updateState(STATE_ALTERNATIVE_CONTENT_SESSION_STARTING);
                    if (alternativeContentSession != null)
                    {
                        alternativeContentSession.stop(false);
                        alternativeContentSession = null;
                    }
                    if (isSessionStarted())
                    {
                        currentSession.stop(false);
                    }
                    //altcontent notification and transition to altcontent started will be handled on reception of still frame decode notification
                    alternativeContentSession = new AlternativeContentSession(lock, sessionListener, getVideoDevice());
                    updateState(STATE_ALTERNATIVE_CONTENT_SESSION_STARTING);
                    alternativeContentSession.start();
                }
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unexpected alternative content mode: " + alternativeContentMode);
                }
            }
        }
    }

    protected void notifyAlternativeContent(Class alternativeContentClass, int alternativeContentReasonCode)
    {
        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("notifying alternative content - reason: " + alternativeContentReasonCode);
            }
            ((ServicePresentationContext) context).notifyAlternativeContent(alternativeContentClass, alternativeContentReasonCode);
        }
        this.alternativeContentClass = alternativeContentClass;
        this.alternativeContentReasonCode = alternativeContentReasonCode;
        lastNotification = ALTERNATIVE_CONTENT_NOTIFICATION;
    }

    /**
     * Handle an ED event generated by the current session. This contains common
     * event handling code for all subclasses. Subclasses should override this
     * to customize event handling.
     * 
     * @param session
     *            the session
     * @param event
     *            - MPE event code
     * @param data1
     *            - first event parameter
     * @param data2
     *            - second event parameter
     */
    protected void handleSessionEventAsync(Session session, int event, int data1, int data2)
    {
        if (log.isInfoEnabled())
        {
            log.info("handleSessionEventAsync - event: "
                    + eventToString(event) + ", data1: " + data1 + ", data2: " + data2 + ", selection: "
                    + (pendingSelection != null ? "pending selection: " + pendingSelection : "current selection: "
                            + currentSelection) + ", session: " + session + ", current state: " + stateToString(state));
        }

        synchronized (lock)
        {
            // These events are not dependent on the current session.
            // We want to know about them, even if session isn't current
            // because they are providing info about video format, which
            // carries over across sessions.
            ServicePresentationContext ctx = (ServicePresentationContext) context;
            switch (event)
            {
                case MediaAPI.Event.ACTIVE_FORMAT_CHANGED:
                    ctx.notifyActiveFormatChanged(data1);
                    return;
                case MediaAPI.Event.ASPECT_RATIO_CHANGED:
                    ctx.notifyAspectRatioChanged(data1);
                    return;
                case MediaAPI.Event.DFC_CHANGED:
                    ctx.notifyDecoderFormatConversionChanged(data1);
                    return;
                default:
                    // ignore other events
            }

            // All other event types are session-specific and thus
            // are only relevant for current session and pending selection.
            //as this is an async event, the session reference passed in may have been null..no-op in that case
            if (session == null || (!(session.equals(currentSession) || (session.equals(alternativeContentSession)))))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ignoring event for non-current session - event session: " + session
                            + ", current session: " + currentSession + " altcontent session: "
                            + alternativeContentSession);
                }
                return;
            }

            ComponentAuthorization mediaAccessComponentAuthorization = null;
            ComponentAuthorization conditionalAccessComponentAuthorization = null;
            if (mediaAccessAuthorizationRequired())
            {
                // event may received while we have a pending selection, or just
                // a current selection
                mediaAccessComponentAuthorization = pendingSelection != null ? pendingSelection.getMediaAccessComponentAuthorization()
                        : currentSelection.getMediaAccessComponentAuthorization();
            }
            if (conditionalAccessAuthorizationRequired())
            {
                // event may received while we have a pending selection, or just
                // a current selection
                conditionalAccessComponentAuthorization = pendingSelection != null ? pendingSelection.getConditionalAccessComponentAuthorization()
                        : currentSelection.getConditionalAccessComponentAuthorization();
            }
            switch (event)
            {
                // session successfully started ...
                case MediaAPI.Event.CONTENT_PRESENTING:
                    switch (state)
                    {
                        case STATE_PRESENTATION_NOT_STARTED:
                        case STATE_PRESENTATION_SHUTDOWN:
                            if (log.isWarnEnabled())
                            {
                                log.warn("handleSessionEventAsync called in unexpected state: " + stateToString(state));
                            }
                            return;
                        case STATE_NORMAL_CONTENT_SESSION_STARTING:
                        case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                        //may receive content presenting due to 2d/3d transition
                        case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                        case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                            break;
                        //now possible to get content presenting from altcontent (recovery due to 3d transition)
                        case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                            //supported format change...success with decoder alive? unblock
                            if (data1 == MediaAPI.Event.CONTENT_PRESENTING_2D_SUCCESS || data1 == MediaAPI.Event.CONTENT_PRESENTING_3D_SUCCESS)
                            {
                                if (ALTERNATIVE_CONTENT_MODE_RENDER_BLACK == alternativeContentMode || ALTERNATIVE_CONTENT_MODE_RENDER_VIDEO == alternativeContentMode)
                                {
                                    unblockPresentationAndNotifyNormalContent();
                                }
                                else
                                {
                                    if (log.isDebugEnabled())
                                    {
                                        log.debug("received CONTENT_PRESENTING 2d/3d success in ALTERNATIVE_CONTENT_SESSION_STARTED but decoder wasn't alive - reselecting");
                                    }
                                    reselect(SelectionTrigger.FORMAT);
                                }
                            }
                            else if (data1 == MediaAPI.Event.CONTENT_PRESENTING_3D_FORMAT_NOT_CONFIRMED)
                            {
                                //3d format not supported is only provided if the decode session is active...just 'switch' to altcontent (post the event)
                                switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_RENDER_VIDEO, S3DAlternativeContentErrorEvent.class, S3DAlternativeContentErrorEvent.S3D_FORMAT_NOT_SUPPORTED);
                            }
                            else
                            {
                                if (log.isWarnEnabled())
                                {
                                    log.warn("received CONTENT_PRESENTING but for unexpected reason in ALTERNATIVE_CONTENT_SESSION_STARTED - ignoring");
                                }
                            }
                            return;
                            //transitions in format (2D to 3D or 3D to another 3D format) can be received in normal content started state
                        case STATE_NORMAL_CONTENT_SESSION_STARTED:
                            if (data1 == MediaAPI.Event.CONTENT_PRESENTING_2D_SUCCESS || data1 == MediaAPI.Event.CONTENT_PRESENTING_3D_SUCCESS)
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("already presenting normal content, ignoring 2d or 3d success notification");
                                }
                                //already in normalcontent, just a change in format, no-op
                                return;
                            }
                            // continue below
                            break;
                        default:
                            throw new IllegalStateException("handleSessionEventAsync called in unknown state: "
                                    + stateToString(state));
                    }

                    //trigger altcontent if needed on CONTENT_PRESENTING notification
                    switch (data1)
                    {
                        case MediaAPI.Event.CONTENT_PRESENTING_2D_SUCCESS:
                        case MediaAPI.Event.CONTENT_PRESENTING_3D_SUCCESS:
                            //continue below if current state is not NORMAL_CONTENT_STARTED
                            break;
                        case MediaAPI.Event.CONTENT_PRESENTING_3D_FORMAT_NOT_CONFIRMED:
                            //may recover due to change in the stream format (to 2D or supported 3D format)
                            //keep the decode session active
                            switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_RENDER_VIDEO, S3DAlternativeContentErrorEvent.class, S3DAlternativeContentErrorEvent.S3D_FORMAT_NOT_SUPPORTED);
                            return;
                        default:
                            if (log.isWarnEnabled())
                            {
                                log.warn("Unexpected data1 value: " + data1 + " - ignoring");
                            }
                            return;
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("CONTENT_PRESENTING reason: " + contentPresentingToString(data1));
                    }

                    // received a content presenting notification - update

                    // currentSelection & null out pendingSelection
                    //content presenting may come after not presenting - only update current selection if pendingselection is not null
                    if (pendingSelection != null)
                    {
                        currentSelection = pendingSelection;
                        pendingSelection = null;
                        if (log.isDebugEnabled())
                        {
                            log.debug("setting pendingSelection as currentSelection - new currentSelection: " + currentSelection);
                        }
                    }

                    if (mediaAccessAuthorizationRequired())
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("media access authorization was required - notifying full authorization: "
                                    + mediaAccessComponentAuthorization.isFullAuthorization());
                        }
                        ctx.notifySessionComplete(currentSession.getNativeHandle(), mediaAccessComponentAuthorization.isFullAuthorization());
                        ctx.notifyMediaAuthorization(mediaAccessComponentAuthorization);
                        if (mediaAccessComponentAuthorization.isFullAuthorization())
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("unblocking presentation: " + currentSession);
                            }
                            currentSession.blockPresentation(false);
                            updateState(STATE_NORMAL_CONTENT_SESSION_STARTED);
                            if (lastNotification != NORMAL_CONTENT_NOTIFICATION)
                            {
                                notifyNormalContent(ctx);
                            }
                            else
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("not notifying NormalContent - lastNotification: " + lastNotification
                                            + ", altContentReasonCode: " + alternativeContentReasonCode);
                                }
                            }

                            processPendingRequest();
                        }
                        else
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("not full authorization - switching to alternative content - selection: "
                                        + currentSelection);
                            }
                            switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.RATING_PROBLEM);
                        }
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("media access authorization was not required");
                        }
                        // TODO: post mediapresented event (no auth)
                        // normalcontent - auth not required, post
                        // sessioncomplete successful
                        ctx.notifySessionComplete(currentSession.getNativeHandle(), true);

                        // authorization not required - update state & send
                        // NormalContent
                        updateState(STATE_NORMAL_CONTENT_SESSION_STARTED);
                        if (lastNotification != NORMAL_CONTENT_NOTIFICATION)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("unblocking presentation: " + currentSession);
                            }
                            currentSession.blockPresentation(false);
                            notifyNormalContent(ctx);
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("not notifying NormalContent - lastNotification: " + lastNotification
                                        + ", altContentReasonCode: " + alternativeContentReasonCode);
                            }
                        }
                        processPendingRequest();
                    }
                    if (SelectionTrigger.SERVICE_CONTEXT_RESELECT.equals(currentSelection.getTrigger()))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("current selection trigger is SERVICE_CONTEXT_RESELECT - calling notifyNormalContent");
                        }
                        ctx.notifyNormalContent();
                    }
                    if (MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE.equals(currentSelection.getTrigger()))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("current selection trigger is NEW_SELECTED_SERVICE - firing presentation start-related events");
                        }
                        startPresentation();
                    }
                    if (MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE_COMPONENTS.equals(currentSelection.getTrigger()))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("current selection trigger is NEW_SELECTED_SERVICE_COMPONENTS - calling notifyMediaSelectSucceeded");
                        }
                        ctx.notifyMediaSelectSucceeded(currentSelection.getLocators());
                    }
                    //the trigger has been used to post events..null it out now..
                    currentSelection.setTrigger(null);

                    break;
                case MediaAPI.Event.STILL_FRAME_DECODED:
                    switch (state)
                    {
                        case STATE_PRESENTATION_NOT_STARTED:
                        case STATE_PRESENTATION_SHUTDOWN:
                        case STATE_NORMAL_CONTENT_SESSION_STARTING:
                        //a transition to altcontent while verifying authorization will transition the state to altcontent starting - unexpected state
                        case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                            if (log.isWarnEnabled())
                            {
                                log.warn("handleSessionEventAsync called in unexpected state: " + stateToString(state));
                            }
                            return;
                        case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                            // continue below
                            break;
                        case STATE_NORMAL_CONTENT_SESSION_STARTED:
                        case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                        case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                            if (log.isWarnEnabled())
                            {
                                log.warn("handleSessionEventAsync called in unexpected state: " + stateToString(state));
                            }
                            return;
                        default:
                            throw new IllegalStateException("handleSessionEventAsync called in unknown state: "
                                    + stateToString(state));
                    }
                    // there may be a pending selection - if so, set current
                    // selection to pending selection
                    // may be needed to support mediaselectcontrol, etc.
                    if (pendingSelection != null)
                    {
                        currentSelection = pendingSelection;
                        pendingSelection = null;
                    }

                    // set 'presenting' flag and post mediapresented event (we
                    // may enter altcontent on start)
                    if (MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE.equals(currentSelection.getTrigger()))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("current selection trigger is NEW_SELECTED_SERVICE - firing presentation start-related events");
                        }
                        startPresentation();
                    }

                    // alternative content
                    updateState(STATE_ALTERNATIVE_CONTENT_SESSION_STARTED);
                    // content presenting - sessionComplete, but we're
                    // presenting alternative content
                    ctx.notifySessionComplete(alternativeContentSession.getNativeHandle(), false);
                    if (alternativeContentReasonCode == AlternativeContentErrorEvent.CA_REFUSAL || alternativeContentReasonCode == AlternativeContentErrorEvent.RATING_PROBLEM)
                    {
                        boolean mediaPresentationEventSent = false;
                        //only send one event - CA if not full auth, result of MAH otherwise
                        if (conditionalAccessAuthorizationRequired())
                        {
                            if (!conditionalAccessComponentAuthorization.isFullAuthorization())
                            {
                                ctx.notifyMediaAuthorization(conditionalAccessComponentAuthorization);
                                mediaPresentationEventSent = true;
                            }
                        }
                        if (mediaAccessAuthorizationRequired())
                        {
                            if (!mediaPresentationEventSent)
                            {
                                ctx.notifyMediaAuthorization(mediaAccessComponentAuthorization);
                            }
                        }
                    }
                    else
                    {
                        //post altmediapresentationevent with no reasons
                        OcapLocator locator = currentSelection.getServiceDetails() == null ? null : (OcapLocator) currentSelection.getServiceDetails().getService().getLocator();
                        ctx.notifyNoReasonAlternativeMediaPresentation(currentSelection.getElementaryStreams(getNetworkInterface(),
                                currentSelection.getCurrentComponents()), locator, 
                                currentSelection.getTrigger(), currentSelection.isDigital());
                    }
                    if (MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE_COMPONENTS.equals(currentSelection.getTrigger()))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("current selection trigger is NEW_SELECTED_SERVICE_COMPONENTS - calling notifyMediaSelectFailed");
                        }
                        ctx.notifyMediaSelectFailed(currentSelection.getLocators());
                    }
                    //altcontent but need to transition the player to started state even though the decode session was not started...
                    if (MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE.equals(currentSelection.getTrigger()))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("current selection trigger is NEW_SELECTED_SERVICE - calling notifyStarted");
                        }
                        ctx.notifyStarted();
                    }
                    notifyAlternativeContent(alternativeContentClass, alternativeContentReasonCode);

                    //the trigger has been used to post events..null it out now (may receive content_presenting due to 3d format issue
                    //and the events should not be resent)
                    currentSelection.setTrigger(null);

                    processPendingRequest();
                    break;
                case MediaAPI.Event.S3D_FORMAT_CHANGED:
                    // data1 contains s3dTransitionType -- these are defined in S3DSignalingChangedEvent
                    // if a format change is unsupported (will result in 3d altcontent), both s3d_format_changed as well as
                    // a content_presenting or content_not_presenting event will be received
                    switch (state)
                    {
                        case STATE_PRESENTATION_NOT_STARTED:
                        case STATE_PRESENTATION_SHUTDOWN:
                        case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                            if (log.isWarnEnabled())
                            {
                                log.warn("handleSessionEventAsync called in unexpected state: " + stateToString(state));
                            }
                            return;
                        case STATE_NORMAL_CONTENT_SESSION_STARTING:
                        case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                        case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                        case STATE_NORMAL_CONTENT_SESSION_STARTED:
                        case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                            // continue below
                            break;
                        default:
                            throw new IllegalStateException("handleSessionEventAsync called in unknown state: "
                                    + stateToString(state));
                    }
                    ctx.notify3DFormatChanged(data1);
                    return;
                case MediaAPI.Event.CONTENT_NOT_PRESENTING:
                    //data1 contains the reason
                    switch (state)
                    {
                        case STATE_PRESENTATION_NOT_STARTED:
                        case STATE_PRESENTATION_SHUTDOWN:
                        case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                            if (log.isWarnEnabled())
                            {
                                log.warn("handleSessionEventAsync called in unexpected state: " + stateToString(state));
                            }
                            return;
                        case STATE_NORMAL_CONTENT_SESSION_STARTING:
                        case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                            // continue below
                            break;
                        case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                            if (log.isWarnEnabled())
                            {
                                log.warn("handleSessionEventAsync called in unexpected state: " + stateToString(state));
                            }
                            return;
                        case STATE_NORMAL_CONTENT_SESSION_STARTED:
                        case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                            // continue below
                            break;
                        default:
                            throw new IllegalStateException("handleSessionEventAsync called in unknown state: "
                                    + stateToString(state));
                    }
                    if (log.isDebugEnabled())
                    {
                        log.debug("CONTENT_NOT_PRESENTING reason: " + contentNotPresentingToString(data1));
                    }
                    //not presenting, switch to alternative content
                    switch (data1)
                    {
                        case MediaAPI.Event.CONTENT_NOT_PRESENTING_NO_DATA:
                            switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_RENDER_BLACK, AlternativeContentErrorEvent.class,
                                    AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                            break;
                        case MediaAPI.Event.CONTENT_NOT_PRESENTING_3D_DISPLAY_DEVICE_NOT_CAPABLE:
                            switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_RENDER_BLACK, S3DAlternativeContentErrorEvent.class,
                                    S3DAlternativeContentErrorEvent.S3D_NOT_SUPPORTED);
                            break;
                        case MediaAPI.Event.CONTENT_NOT_PRESENTING_3D_NO_CONNECTED_DISPLAY_DEVICE:
                            switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_RENDER_BLACK, S3DAlternativeContentErrorEvent.class,
                                    S3DAlternativeContentErrorEvent.S3D_NO_HDMI_CONNECTION);
                            break;
                        default:
                            if (log.isWarnEnabled())
                            {
                                log.warn("unexpected reason associated with CONTENT_NOT_PRESENTING event - ignoring");
                            }
                            break;
                    }
                    break;
                case MediaAPI.Event.FAILURE_UNKNOWN:
                    handleFailureEventAsync(event, data1, data2);
                    break;
                    // session ed queue termination
                case MediaAPI.Event.QUEUE_TERMINATED:
                    // ignoring
                    break;
                default:
                    if (log.isDebugEnabled())
                    {
                        log.debug("handleSessionEventAsync - received unexpected event - code: " + event + ", data1: "
                                + data1 + ", data2: " + data2);
                    }
            }
        }
    }

    protected void handleFailureEventAsync(int event, int data1, int data2)
    {
        if (log.isInfoEnabled()) 
        {
            log.info("handleFailureEventAsync: " + event + ", data1: " + data1 + ", data2: " + data2);
        }
        synchronized(getLock())
        {
            ServicePresentationContext ctx = (ServicePresentationContext) context;
            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                case STATE_PRESENTATION_SHUTDOWN:
                    if (log.isWarnEnabled())
                    {
                        log.warn("handleFailureEventAsync called in unexpected state: " + stateToString(state));
                    }
                    break;
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    ctx.notifySessionComplete(currentSession.getNativeHandle(), false);
                    if (MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE_COMPONENTS.equals(pendingSelection.getTrigger()))
                    {
                        ctx.notifyMediaSelectFailed(pendingSelection.getLocators());
                    }
                    closePresentation("received failure event: " + event, null);
                    break;
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                    closePresentation("received failure event: " + event, null);
                    // not processing pending requests, presentation
                    // closed
                    break;
                default:
                    throw new IllegalStateException("handleFailureEventAsync called in unknown state: "
                            + stateToString(state));
            }
        }
    }

    private void unblockPresentationAndNotifyNormalContent()
    {
        if (log.isInfoEnabled())
        {
            log.info("unblocking presentation and notifying NormalContent: " + currentSession);
        }
        currentSession.blockPresentation(false);
        //not in alternative content any longer - update keepdecoderalive flag
        notifyNormalContent((ServicePresentationContext) context);
        updateState(STATE_NORMAL_CONTENT_SESSION_STARTED);
    }

    abstract ExtendedNetworkInterface getNetworkInterface();

    /**
     * Subclasses requiring authorization via a registered MediaAccessHandler must authorize the selection with the MediaAccessHandler and then
     * update the authorization on the selection
     *
     * Presentation of all broadcast components must be authorized by a registered MediaAccessHandler
     * including presentation of unbuffered or buffered broadcast services (live point and tsb presentation, live point
     * in-progress recordings and broadcast presentations)
     *  
     * Override by subclasses if needed (default media access authorization required: false)
     * 
     * @return false
     */
    protected boolean mediaAccessAuthorizationRequired()
    {
        return false;
    }

    /**
     * Subclasses requiring authorization via conditional access must determine access provided by the CA system and
     * update the authorization on the selection
     *
     * Presentation of broadcast components -at the live point- must be authorized by the CA system
     * including presentation of unbuffered broadcast services (live point tsb presentation,
     * live point in-progress recordings and broadcast presentations)
     *
     * Override by subclasses if needed (default conditional access authorization required: false)
     *
     * @return false
     */
    protected boolean conditionalAccessAuthorizationRequired()
    {
        return false;
    }

    protected void notifyNormalContent(ServicePresentationContext ctx)
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("calling notifyNormalContent");
            }
            ctx.notifyNormalContent();
            lastNotification = NORMAL_CONTENT_NOTIFICATION;
        }
    }

    /**
     * Return if current session is started (a normal content decode session has
     * been initiated).  Should be called to guard calls on the Session (get/setSessionRate or get/setMediaTime)
     * 
     * This method does not call {@link #waitForSessionChangeToComplete()}
     * 
     * @return true if decode session has been started
     */
    protected boolean isSessionStarted()
    {
        synchronized (lock)
        {
            return currentSession != null && currentSession.isDecodeInitiated();
        }
    }

    /**
     * Create a new session.
     *
     * If the process of creating a new session resulted in an update to the clock mediatime or rate, the returning CreateSessionResult 
     * should have presentationPointChanged set to 'true'
     */
    protected abstract CreateSessionResult doCreateSession(Selection selection, Time mediaTime, float rate)
            throws NoSourceException, MPEException;

    protected abstract void doStartSession(ServiceSession session, Selection selection, Time mediaTime, float rate, boolean mediaTimeChangeRequest, MediaPresentationEvaluationTrigger trigger) throws MPEException, NoSourceException;
    
    /**
     * Update currently presenting session.
     * 
     * @param selection
     * @throws NoSourceException
     * @throws MPEException
     */
    protected abstract void doUpdateSession(Selection selection) throws NoSourceException, MPEException;

    private void processPendingRequest()
    {
        synchronized (lock)
        {
            if (!selectionQueue.isEmpty())
            {
                if (log.isInfoEnabled())
                {
                    log.info("processPendingRequests - enqueueing new select task");
                }
                context.getTaskQueue().post(new SelectTask());
            }
        }
    }

    private void updateState(int newState)
    {
        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("updateState - from: " + stateToString(state) + " to " + stateToString(newState));
            }
            state = newState;
            lock.notifyAll();
        }
    }

    /**
     * Verify components are presentable (valid default or explicit components,
     * and service components are retrievable.
     * 
     * @param selection
     *            the selection to evaluate
     * 
     * @return true if acceptable components and retrievable
     */
    protected boolean componentsPresentable(Selection selection)
    {
        synchronized (lock)
        {
            final boolean componentsValidResult = componentsValid(selection);
            if (log.isDebugEnabled())
            {
                log.debug("verifying components valid - selection: " + selection +
                        ", components: " + Arrays.toString(selection.getCurrentComponents()) + ", result: " + componentsValidResult);
            }

            if (!componentsValidResult)
            {
                return false;
            }

            final boolean acceptableComponents = selection.isAcceptableComponents();
            if (log.isDebugEnabled())
            {
                log.debug("verifying components acceptable - selection: " + selection +
                        ", components: " + Arrays.toString(selection.getCurrentComponents()) + ", result: " + acceptableComponents);
            }
            return acceptableComponents;
        }
    }

    private void acceptSwappedSettings(VideoDevice videoDevice, ScalingBounds scalingBounds, boolean showVideo)
    {
        synchronized (lock)
        {
            waitForSessionChangeToComplete();
            switch (state)
            {
                case STATE_PRESENTATION_NOT_STARTED:
                case STATE_PRESENTATION_SHUTDOWN:
                case STATE_NORMAL_CONTENT_SESSION_STARTING:
                case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                    throw new IllegalStateException("acceptSwappedSettings in unexpected state: "
                            + stateToString(state));
                case STATE_NORMAL_CONTENT_SESSION_STARTED:
                case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                    currentSession.setVideoDevice(videoDevice);
                    setBounds(scalingBounds);
                    setShowVideo(showVideo);
                    break;
                default:
                    throw new IllegalStateException("acceptSwappedSettings in unexpected state: "
                            + stateToString(state));
            }
        }
    }

    private String stateToString(int state)
    {
        switch (state)
        {
            case STATE_PRESENTATION_NOT_STARTED:
                return "STATE_PRESENTATION_NOT_STARTED";
            case STATE_PRESENTATION_SHUTDOWN:
                return "STATE_PRESENTATION_SHUTDOWN";
            case STATE_NORMAL_CONTENT_SESSION_STARTING:
                return "STATE_NORMAL_CONTENT_SESSION_STARTING";
            case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                return "STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION";
            case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                return "STATE_ALTERNATIVE_CONTENT_SESSION_STARTING";
            case STATE_NORMAL_CONTENT_SESSION_STARTED:
                return "STATE_NORMAL_CONTENT_SESSION_STARTED";
            case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                return "STATE_ALTERNATIVE_CONTENT_SESSION_STARTED";
            case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                return "STATE_NORMAL_CONTENT_SESSION_UPDATING";
            default:
                return "Unknown state: " + state;
        }
    }

    private String contentNotPresentingToString(int reason)
    {
        switch (reason)
        {
            case MediaAPI.Event.CONTENT_NOT_PRESENTING_3D_DISPLAY_DEVICE_NOT_CAPABLE:
                return "3D_DISPLAY_DEVICE_NOT_CAPABLE";
            case MediaAPI.Event.CONTENT_NOT_PRESENTING_NO_DATA:
                return "NO_DATA";
            case MediaAPI.Event.CONTENT_NOT_PRESENTING_3D_NO_CONNECTED_DISPLAY_DEVICE:
                return "NO_CONNECTED_DISPLAY_DEVICE";
            default:
                return "Content not presenting unknown reason: " + reason;
        }
    }

    private String contentPresentingToString(int reason)
    {
        switch (reason)
        {
            case MediaAPI.Event.CONTENT_PRESENTING_2D_SUCCESS:
                return "2D_SUCCESS";
            case MediaAPI.Event.CONTENT_PRESENTING_3D_SUCCESS:
                return "3D_SUCCESS";
            case MediaAPI.Event.CONTENT_PRESENTING_3D_FORMAT_NOT_CONFIRMED:
                return "3D_FORMAT_NOT_SUPPORTED";
            default:
                return "Content presenting unknown reason: " + reason;
        }
    }

    /**
     * This is responsible for processing a {@link Selection} from the
     * {@link SelectionQueue}) for this {@link ServicePresentation} instance.
     * This will exit early in these cases:
     * <ul>
     * <li>A session change is in progress. (A new SelectTask is queued when
     * session change is complete.)</li>
     * <li>The presentation has stopped. (All pending selection requests are
     * failed.)</li>
     * <li>There are no queued Selections.<\li>
     * </ul>
     * exit early if there
     * <p/>
     * PRECONDITIONS
     * <ul>
     * <li>no pending selection if session change is not in progress</li>
     * </ul>
     */
    class SelectTask implements Runnable
    {
        public void run()
        {
            synchronized (lock)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("select task running - current state: " + stateToString(state));
                }
                switch (state)
                {
                    case STATE_PRESENTATION_SHUTDOWN:
                        if (log.isDebugEnabled())
                        {
                            log.debug("not executing select task in " + stateToString(state));
                        }
                        return;
                    case STATE_NORMAL_CONTENT_SESSION_STARTING:
                    case STATE_ALTERNATIVE_CONTENT_SESSION_STARTING:
                    case STATE_NORMAL_CONTENT_SESSION_STARTING_MEDIA_ACCESS_AUTHORIZATION:
                        // allow the starting task to finish, this task will be
                        // re-executed when the session is started
                        return;
                    case STATE_PRESENTATION_NOT_STARTED:
                    case STATE_NORMAL_CONTENT_SESSION_STARTED:
                    case STATE_ALTERNATIVE_CONTENT_SESSION_STARTED:
                    case STATE_NORMAL_CONTENT_SESSION_UPDATING:
                        // continue below
                        break;
                    default:
                        throw new IllegalStateException("SelectTask.run called in unknown state: "
                                + stateToString(state));
                }

                // If there aren't any pending requests, return silently.
                // (They may have already been processed when a prior selection
                // completed.)
                if (selectionQueue.isEmpty())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Selection queue is empty - no task to process");
                    }
                    return;
                }

                // Remove selection from the head of the queue.
                Selection selection = selectionQueue.removeSelection();
                try
                {
                    Time clockMediaTime = context.getClock().getMediaTime();
                    float clockRate = context.getClock().getRate();
                    if (log.isDebugEnabled())
                    {
                        log.debug("Select task - starting new session - clock mediaTime: " + clockMediaTime
                                + ", clock rate: " + clockRate);
                    }
                    // start the session with player clock's mediatime & rate
                    startNewSessionAsync(selection, selection.getTrigger(), clockMediaTime, clockRate, false, false);
                }
                // If an error occurs when trying to start the session,
                // it is fatal to the presentation.
                catch (Exception x)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Exception triggered in reselect..stopping current session", x);
                    }
                    try
                    {
                        doStop(true);
                    }
                    finally
                    {
                        releaseResources(true);
                    }
                    // Notify context that selection failed.
                    ((ServicePresentationContext) context).notifyMediaSelectFailed(selection.getLocators());
                }
            }
        }
    }

    /**
     * This is a FIFO queue of {@link Selection} objects, which are queued by
     * the {@link DVBMediaSelectControl}.
     */
    class SelectionQueue
    {
        private LinkedList list = new LinkedList();

        void addSelection(Selection req)
        {
            list.addLast(req);
        }

        Selection removeSelection()
        {
            return (Selection) list.removeFirst();
        }

        boolean isEmpty()
        {
            return list.isEmpty();
        }

        /**
         * Flush all pending requests from the queue and fail them.
         */
        void flush()
        {
            // De-queue and fail any pending selection requests.
            while (!isEmpty())
            {
                Selection selection = removeSelection();
                // constructor accepted a ServicePresentationContext, so cast is
                // safe
                ((ServicePresentationContext) context).notifyMediaSelectFailed(selection.getLocators());
            }
        }
    }

    class SessionListenerImpl implements SessionListener
    {
        public void handleSessionEvent(final Session session, final int event, final int data1, final int data2)
        {
        // Get the event off of the ED event queue quickly by placing it
        // on the task queue for this context.  Context and its task queue are both final, no need to acquire locks to post runnable
            context.getTaskQueue().post(new Runnable()
            {
                public void run()
                {
                    // Delegate to abstract method to handle the event.
                    handleSessionEventAsync(session, event, data1, data2);
                }
            });
        }
    }

    /**
     * Holder object containing the result of the doCreateSession call.  Provides the new session or updated mediatime and rate.
     *
     * If presentationRequestChanged is true, the session field will be null and an updated mediatime and rate should be used.
     * If presentationRequestChanged is false, the session will not be null.
     */
    protected class CreateSessionResult {
        private boolean presentationRequestChanged;
        Time mediaTime;
        float rate;
        ServiceSession session;
        boolean transitionToAlternativeContent = false;
        int alternativeContentErrorCode;
        Class alternativeContentClass;

        CreateSessionResult(boolean presentationRequestChanged, Time mediaTime, float rate, ServiceSession session)
        {
            this.presentationRequestChanged = presentationRequestChanged;
            this.mediaTime = mediaTime;
            this.rate = rate;
            this.session = session;
        }
        
        CreateSessionResult(Class alternativeContentClass, int alternativeContentErrorCode) 
        {
            transitionToAlternativeContent = true;
            this.alternativeContentClass = alternativeContentClass;
            this.alternativeContentErrorCode = alternativeContentErrorCode;
        }
    }
}
