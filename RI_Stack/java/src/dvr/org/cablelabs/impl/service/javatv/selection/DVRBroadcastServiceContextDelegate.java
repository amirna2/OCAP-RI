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

package org.cablelabs.impl.service.javatv.selection;

import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.ResourceUnavailableEvent;
import javax.media.StopByRequestEvent;
import javax.media.Time;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceMediaHandler;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.DisableBufferingListener;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.manager.TimeShiftBuffer;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowStateChangedEvent;
import org.cablelabs.impl.manager.TimeShiftWindowChangedListener;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.TimerManager;
import org.cablelabs.impl.manager.recording.RecordingManagerInterface;
import org.cablelabs.impl.manager.timer.TimerMgr;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.player.MediaTimeBase;
import org.cablelabs.impl.media.player.PresentationModeControl;
import org.cablelabs.impl.media.player.PresentationModeListener;
import org.cablelabs.impl.media.player.ServicePlayer;
import org.cablelabs.impl.media.player.TSBServiceMediaHandler;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceContextResourceUsageImpl;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.MPEEnv;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NoFreeInterfaceException;
import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.TimeShiftEvent;
import org.ocap.dvr.TimeShiftListener;
import org.ocap.dvr.TimeShiftProperties;
import org.ocap.media.AlternativeMediaPresentationEvent;
import org.ocap.media.NormalMediaPresentationEvent;
import org.ocap.service.AlternativeContentErrorEvent;
import org.ocap.shared.dvr.RecordingTerminatedEvent;
import org.ocap.storage.ExtendedFileAccessPermissions;

public class DVRBroadcastServiceContextDelegate implements DVRServiceContextDelegate, ServiceContextDelegate,
        TimeShiftProperties
{
    private static final Logger log = Logger.getLogger(DVRBroadcastServiceContextDelegate.class);

    //never set to null, assigned in initialize method
    static RecordingManagerInterface recordingManager;

    private static final NetworkInterface SPECIAL_NETWORK_INTERFACE = null;

    private static final int DEFAULT_BUFFERING_AUTOSTART_DELAY = 1000;

    private static long BUFFERING_AUTOSTART_DELAY = MPEEnv.getEnv("OCAP.dvr.tsb.bufferingAutostartDelay",
            DEFAULT_BUFFERING_AUTOSTART_DELAY);

    private static final String PRESENTATION_MODE_CONTROL_CLASS_NAME = "org.cablelabs.impl.media.player.PresentationModeControl";

    //initial state, not presenting any service
    private static final int STATE_NOT_PRESENTING = 1;

    //attempting to acquire a tsw
    private static final int STATE_PRESENTING_PENDING_WAIT_TSW_READY = 2;

    //a tsw is available and ready to present
    private static final int STATE_PRESENTING_PENDING_TSW_READY = 3;

    //a request to present a service has proceeded far enough to create a player and call start on the player    
    private static final int STATE_PRESENTING_PENDING_PLAYER_STARTING = 4;

    private static final int STATE_PRESENTING = 5;

    //if player is in the transition to started, wait for player started notification prior to stopping player
    private static final int STATE_STOP_PENDING_WAIT_PLAYER_START = 6;

    //wait for player stopped notification prior to releasing resources
    private static final int STATE_STOP_PENDING_WAIT_PLAYER_STOP = 7;

    //the player is now stopped and resources can be released
    private static final int STATE_STOP_PENDING_PLAYER_NOT_STARTED = 8;

    private static final String PRESENT_LIVE_FROM_BUFFER_PARAM = "OCAP.dvr.presentLiveFromBuffer";

    // members referencing parameters passed-in to initialize
    private ServiceContextDelegateListener serviceContextDelegateListener;

    private DVRServiceContextDelegateListener dvrServiceContextDelegateListener;

    private ServiceMediaHandlerCreationDelegate creationDelegate;

    private AppDomain appDomain;

    private PersistentVideoModeSettings persistentVideoModeSettings;

    // never null this out
    private volatile Object lock;

    // members constructed in initialize
    private final DisableBufferingListener disableBufferingListener = new DisableBufferingListenerImpl();

    private TVTimer tvTimer;

    private TVTimerSpec tvTimerSpec;

    private TimeShiftWindowChangedListener timeShiftWindowChangedListener;

    private ControllerListener controllerListener;

    private PresentationModeListener presentationModeListener;

    //currently presenting service
    private Service service;

    //currently presenting component locators
    private Locator[] componentLocators;

    private ServiceContextResourceUsageImpl serviceContextResourceUsage;

    // members constructed during presentation
    private TimeShiftWindowClient timeShiftWindowClient;

    private ServiceMediaHandler serviceMediaHandler;

    // timeshiftproperties state
    private long minDuration;

    private long maxDuration;

    private boolean bufferLastService;

    private int currentState = STATE_NOT_PRESENTING;

    private boolean initialized;

    private BufferingRequest bufferingRequest;

    private boolean presentationBlocked = true; // default to blocked

    // remember if the last event was a 'not found' event (we only send 'found'
    // if we've previously sent 'not found')
    private boolean lastTimeShiftEventNoTimeShiftBuffer;
    private boolean tvTimerSpecIsScheduled = false;
    private TVTimerWentOffListener tvTimerWentOffListener;

    //if stopPresentingWithReason is called but the PresentationModeListener has not yet received an event, wait for the PresentationMode event 
    // then stop presenting with the cached class and reason 
    private Class cachedStopPresentingEventClass;
    private int cachedStopPresentingEventReason;
    //no need to use 'this' in the identity hash lookup, just a unique identifier
    private final String id = "Id: 0x" + Integer.toHexString(System.identityHashCode(new Object())).toUpperCase() + ": ";

    //values provided in prepareToPresent
    private Service initialService;
    private Locator[] initialComponentLocators;
    private ServiceContextResourceUsageImpl initialServiceContextResourceUsage;
    private CallerContext initialCallerContext;
    private volatile long initialSequence = -1;

    public synchronized void initialize(ServiceContextDelegateListener serviceContextDelegateListener,
            ServiceMediaHandlerCreationDelegate creationDelegate, AppDomain appDomain,
            PersistentVideoModeSettings persistentVideoModeSettings, Object lock)
    {
        CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext systemContext = callerContextManager.getSystemContext();
        synchronized (lock)
        {
            if (recordingManager == null)
            {
                recordingManager = (RecordingManagerInterface) ((org.cablelabs.impl.manager.RecordingManager) ManagerManager.getInstance(org.cablelabs.impl.manager.RecordingManager.class)).getRecordingManager();

            }
            this.serviceContextDelegateListener = serviceContextDelegateListener;
            this.creationDelegate = creationDelegate;
            this.appDomain = appDomain;
            this.persistentVideoModeSettings = persistentVideoModeSettings;
            this.lock = lock;
            tvTimer = ((TimerManager) TimerMgr.getInstance()).getTimer(systemContext);
            tvTimerSpec = new TVTimerSpec();
            //don't add the tvtimerlistener - the listener constructor is provided a
            // reference to the timeshiftwindowclient so stale notifications are ignored
            tvTimerSpec.setAbsolute(false); // Always a delay
            tvTimerSpec.setTime(BUFFERING_AUTOSTART_DELAY); // Always the same
                                                            // delay
            tvTimerSpec.setRepeat(false); // Only once

            initialized = true;
            if (log.isDebugEnabled())
            {
                log.debug(id + "initialized");
            }
        }
    }

    public void prepareToPresent(Service newService, Locator[] newComponentLocators, ServiceContextResourceUsageImpl serviceContextResourceUsage,
                                 CallerContext callerContext, long sequence)
    {
        Assert.lockHeld(lock);
        synchronized(lock)
        {
            //may be called without a stop, if selecting the same service with different locators
            if (log.isInfoEnabled())
            {
                log.info(id + "prepareToPresent - service: " + newService + ", locators: " + Arrays.toString(newComponentLocators) + ", current state: " + stateToString(currentState) + ", current sequence: " + initialSequence + ", new sequence: " + sequence);
            }
            this.initialService = newService;
            this.initialComponentLocators = newComponentLocators;
            this.initialServiceContextResourceUsage = serviceContextResourceUsage;
            this.initialCallerContext = callerContext;
            this.initialSequence = sequence;
            timeShiftWindowChangedListener = new TimeShiftWindowChangedListenerImpl(initialSequence);
            controllerListener = new ControllerListenerImpl(initialSequence);
            presentationModeListener = new PresentationModeListenerImpl(initialSequence);
        }
    }

    public void present(long presentSequence)
    {
        assertInitialized();

        ServiceMediaHandler tempServiceMediaHandler;
        DVRServiceContextDelegateListener tempDvrServiceContextDelegateListener;
        Locator[] tempInitialComponentLocators;

        boolean callNotifyNoChange = false;
        boolean callUpdateServiceContextSelection = false;
        boolean enableInitialPlayerBuffering = false;
        boolean notifyTimeShiftEvent = false;

        boolean callStopPresenting = false;
        int stopPresentingReason = -1;

        boolean callStartPlayerAndApps = false;

        synchronized (lock)
        {
            if (initialSequence != presentSequence)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "ignoring present called with non-current sequence - current sequence: " + initialSequence + ", sequence parameter: " + presentSequence);
                }
                return;
            }

            if (log.isInfoEnabled())
            {
                log.info(id + "present - sequence: " + presentSequence);
            }
            if (initialService == null)
            {
                throw new IllegalArgumentException("present called with a null service");
            }
            tempServiceMediaHandler = serviceMediaHandler;
            tempInitialComponentLocators = initialComponentLocators;
            tempDvrServiceContextDelegateListener = dvrServiceContextDelegateListener;
            if (initialService.equals(service))
            {
                if (serviceMediaHandler == null)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "No player - unable to select components");
                    }
                    callStopPresenting = true;
                    stopPresentingReason = SelectionFailedEvent.OTHER;
                }
                else if (initialComponentLocators == null || initialComponentLocators.length == 0)
                {
                    callNotifyNoChange = true;
                }
                else
                {
                    callUpdateServiceContextSelection = true;
                }
            }
            else
            {
                try
                {
                    service = initialService;
                    componentLocators = (Locator[]) Arrays.copy(initialComponentLocators, Locator.class);
                    serviceContextResourceUsage = initialServiceContextResourceUsage;
                    //get a ref to the TSWclient here
    
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "setting currentState to STATE_PRESENTING_PENDING_WAIT_TSW_READY");
                    }
                    currentState = STATE_PRESENTING_PENDING_WAIT_TSW_READY;
    
                    //retrieve the TSW while holding the lock
                    // We want to ensure we have a use registered - the
                    // presentation layer will register a live use
                    // if appropriate
                    int timeShiftUses = TimeShiftManager.TSWUSE_NIRES;
                    TimeShiftManager timeShiftManager = (TimeShiftManager) ManagerManager.getInstance(TimeShiftManager.class);
                    timeShiftWindowClient = timeShiftManager.getTSWByDuration(initialService, minDuration,
                            maxDuration, timeShiftUses, serviceContextResourceUsage,
                            timeShiftWindowChangedListener, TimeShiftManager.LISTENER_PRIORITY_LOW);
                    // Make sure we've actually got one, otherwise we're done.
                    if (timeShiftWindowClient == null)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "present - getTSWByDuration returned null.  Could not select");
                        }
                        callStopPresenting = true;
                        stopPresentingReason = SelectionFailedEvent.INSUFFICIENT_RESOURCES;
                    }
                    else
                    {
                        int timeShiftWindowClientState = timeShiftWindowClient.getState();

                        if (log.isDebugEnabled())
                        {
                            log.debug(id + "present - got TSW: " + timeShiftWindowClient + " in state "
                                    + TimeShiftManager.stateString[timeShiftWindowClientState] + " ("
                                    + timeShiftWindowClientState + ")");
                        }

                        callStartPlayerAndApps = ((timeShiftWindowClientState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER)
                                || timeShiftWindowClientState == TimeShiftManager.TSWSTATE_BUFFERING
                                || timeShiftWindowClientState == TimeShiftManager.TSWSTATE_BUFF_PENDING
                                || timeShiftWindowClientState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER);

                        if (callStartPlayerAndApps)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_PRESENTING_PENDING_TSW_READY");
                            }
                            currentState = STATE_PRESENTING_PENDING_TSW_READY;

                            if (log.isInfoEnabled())
                            {
                                log.info(id + "timeShiftWindowClient available for use - calling startPlayerAndApps");
                            }

                            //enable buffering if ready to buffer, buff pending or buffering
                            if (((timeShiftWindowClientState == TimeShiftManager.TSWSTATE_BUFFERING) ||
                                    timeShiftWindowClientState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER ||
                                    timeShiftWindowClientState == TimeShiftManager.TSWSTATE_BUFF_PENDING
                            )
                                    && (minDuration > 0))
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug(id + "present - starting a player - buffering, ready to buffer or buff pending and minDuration > 0 - enable buffering");
                                }
                                enableInitialPlayerBuffering = true;
                            }
                        }

                        recordingManager.addDisableBufferingListener(disableBufferingListener);
    
                        // if initial state is not ready and we have a
                        // minduration set, notify no time shift buffer and set
                        // flag
                        if (timeShiftWindowClientState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER
                                && minDuration > 0)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "present - TSW in NOT_READY_TO_BUFFER state and minDuration > 0 - notify NO_TIME_SHIFT_BUFFER");
                            }
                            // notify buffer not found and set flag
                            notifyTimeShiftEvent = true;
                            lastTimeShiftEventNoTimeShiftBuffer = true;
                        }
                    }
                }
                catch (NoFreeInterfaceException e)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "present - no Free Interface: " + e.getMessage());
                    }
                    callStopPresenting = true;
                    stopPresentingReason = SelectionFailedEvent.INSUFFICIENT_RESOURCES;
                }
            }
        }

        if (callStopPresenting)
        {
            stopPresentingWithReason(SelectionFailedEvent.class, stopPresentingReason, presentSequence);
        }
        else if (callNotifyNoChange)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "no new locators - already presenting service");
            }
            // all select calls need a notification - may be altcontent or normalcontent - just respond with noChange
            notifyNoChange();
        }
        else if (callUpdateServiceContextSelection)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + "already presenting service - selecting new locators");
                }
                ((ServicePlayer)tempServiceMediaHandler).updateServiceContextSelection(tempInitialComponentLocators);
            }
            catch (InvalidLocatorException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "Unable to select locators: " + Arrays.toString(tempInitialComponentLocators), e);
                }
                stopPresentingWithReason(SelectionFailedEvent.class, SelectionFailedEvent.INSUFFICIENT_RESOURCES, presentSequence);
            }
            catch (InvalidServiceComponentException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "Unable to select locators: " + Arrays.toString(tempInitialComponentLocators), e);
                }
                if (log.isWarnEnabled())
                {
                    log.warn(id + "Unable to select locators: " + Arrays.toString(tempInitialComponentLocators), e);
                }
                stopPresentingWithReason(SelectionFailedEvent.class, SelectionFailedEvent.INSUFFICIENT_RESOURCES, presentSequence);
            }
        }
        else
        {
            if (notifyTimeShiftEvent)
            {
                tempDvrServiceContextDelegateListener.notifyTimeShiftEvent(TimeShiftEvent.NO_TIME_SHIFT_BUFFER);
            }

            if (callStartPlayerAndApps)
            {
                startPlayerAndApps(presentSequence, enableInitialPlayerBuffering);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "timeShiftWindowClient not yet available for use - waiting for a TimeShiftWindow event");
                }
            }
        }
    }

    public void stopPresenting(long presentSequence)
    {
        assertInitialized();
        synchronized(lock)
        {
            if (initialSequence != presentSequence)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "ignoring stopPresenting called with non-current sequence - current sequence: " + initialSequence + ", sequence parameter: " + presentSequence);
                }
                return;
            }
            if (log.isInfoEnabled())
            {
                log.info(id + "stopPresenting - sequence: " + presentSequence);
            }
        }
        stopPresentingWithReason(PresentationTerminatedEvent.class, PresentationTerminatedEvent.USER_STOP, presentSequence);
    }

    public void stopPresentingAbstractService()
    {
        assertInitialized();
        // no-op in all but abstractservicecontextdelegate
    }

    /**
     * Release references and stop last channel buffering if applicable.
     */
    public synchronized void destroy()
    {
        if (log.isInfoEnabled())
        {
            log.info(id + "destroy");
        }

        synchronized (lock)
        {
            if (initialized)
            {
                    // reset state to default values
                service = null;
                componentLocators = null;

                if (log.isInfoEnabled())
                {
                    log.info(id + "setting currentState to STATE_NOT_PRESENTING");
                }
                currentState = STATE_NOT_PRESENTING;

                // initialized check prevents us from worrying about coming
                // through destroy twice
                recordingManager.removeDisableBufferingListener(disableBufferingListener);

                presentationBlocked = true; // default to blocked
            }
            stopLastChannelBuffering();
        }
        initialized = false;
    }

    public boolean canPresent(Service service)
    {
        return true;
    }

    public TimeShiftWindowClient getTimeShiftWindowClient()
    {
        assertInitialized();
        synchronized (lock)
        {
            return timeShiftWindowClient;
        }
    }

    /**
     * Request buffering
     */
    public void requestBuffering()
    {
        assertInitialized();
        TimeShiftWindowClient tempTimeShiftWindowClient;
        long tempInitialSequence;
                
        synchronized (lock)
        {
            tempTimeShiftWindowClient = timeShiftWindowClient;
            tempInitialSequence = initialSequence;
        }

        // Turn buffering on.
        if (tempTimeShiftWindowClient != null)
        {
            int timeShiftWindowClientState = tempTimeShiftWindowClient.getState();
            if (timeShiftWindowClientState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER
                    || timeShiftWindowClientState == TimeShiftManager.TSWSTATE_BUFF_PENDING
                    || timeShiftWindowClientState == TimeShiftManager.TSWSTATE_BUFFERING)
            {
                setBuffering(true, true, tempInitialSequence);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + "requestBuffering - timeShiftWindowClient not in state allowing attachment for buffering: "
                            + tempTimeShiftWindowClient);
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "requestBuffering - timeshiftWindowClient null - ignoring request to enable buffering");
            }
        }
    }

    /**
     * Assuming this is called in the process of setting up a JMF player (after
     * we have a timeShiftWindowClient)
     * 
     * @param dvrPresentation
     *            the DVRPresentation to update
     */
    public void updatePresentation(DVRPresentation dvrPresentation)
    {
        assertInitialized();
        if (log.isInfoEnabled())
        {
            log.info(id + "updatePresentation: " + dvrPresentation);
        }

        TimeShiftWindowClient tempTimeShiftWindowClient;
        synchronized (lock)
        {
            tempTimeShiftWindowClient = timeShiftWindowClient;
        }

        if (tempTimeShiftWindowClient != null)
        {
            TimeShiftBuffer firstTSB = tempTimeShiftWindowClient.getFirstTSB();
            TimeShiftBuffer lastTSB = tempTimeShiftWindowClient.getLastTSB();
            if (firstTSB == null || lastTSB == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "updatePresentation - first or last tsb were null - first: " + firstTSB + ", last: "
                            + lastTSB + ", unable to update presentation");
                }
                return;
            }
            long tsbStart = firstTSB.getTSWStartTimeOffset() + firstTSB.getContentStartTimeInMediaTime();
            long tsbEnd = lastTSB.getTSWStartTimeOffset() + lastTSB.getContentEndTimeInMediaTime();

            // Adjust mediaTime if outside the bounds of the TSB.
            long mediaTime = dvrPresentation.getMediaTime().getNanoseconds();
            if (mediaTime <= 0)
            {
                // zero or negative mediatime, present from live point
                dvrPresentation.setPresentation(new Time(Long.MAX_VALUE), dvrPresentation.getRate(),
                        dvrPresentation.getAction(), dvrPresentation.getPersistent());
            }
            else if (mediaTime > tsbEnd || mediaTime < tsbStart)
            {
                String actionMsg = (dvrPresentation.getAction() ? " setting media time to zero"
                        : "setting mediatime to live point");
                if (log.isInfoEnabled())
                {
                    log.info(id + "mediaTime is out of TSB bounds - mediaTime: " + mediaTime + ", tsb start: " + tsbStart
                            + ", tsb end: " + tsbEnd + actionMsg);
                }

                if (dvrPresentation.getAction())
                {
                    // set media time to the begining
                    mediaTime = 0;
                }
                else
                {
                    // set media time to live point
                    mediaTime = Long.MAX_VALUE;
                }

                if (log.isInfoEnabled())
                {
                    log.info(id + "adjusted mediaTime: " + mediaTime + "ns");
                }
                dvrPresentation.setPresentation(new Time(mediaTime), dvrPresentation.getRate(),
                        dvrPresentation.getAction(), dvrPresentation.getPersistent());
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "updatePresentation called but tsb is null - setting presentation media time to live point");
            }
            dvrPresentation.setPresentation(new Time(Long.MAX_VALUE), dvrPresentation.getRate(),
                    dvrPresentation.getAction(), dvrPresentation.getPersistent());
        }
    }

    public void setDVRServiceContextDelegateListener(DVRServiceContextDelegateListener dvrServiceContextDelegateListener)
    {
        synchronized (lock)
        {
            this.dvrServiceContextDelegateListener = dvrServiceContextDelegateListener;
        }
    }

    public NetworkInterface getNetworkInterface()
    {
        assertInitialized();
        if (log.isDebugEnabled())
        {
            log.debug(id + "getNetworkInterface");
        }
        CallerContext tempOwnerCallerContext;
        long tempMinDuration;
        synchronized (lock)
        {
            if (currentState != STATE_PRESENTING)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + "not presenting or ownerCallerContext is null - returning null");
                }
                return null;
            }
            tempOwnerCallerContext = initialCallerContext;
            tempMinDuration = minDuration;
        }
        CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        boolean isCallerContextSameAsOwnerCallerContext = tempOwnerCallerContext.equals(callerContextManager.getCurrentContext());
        boolean isTimeShiftEnabled = tempMinDuration > 0;

        if (isTimeShiftEnabled)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "timeShift is enabled");
            }
            if (isCallerContextSameAsOwnerCallerContext)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + "callerContext is same as owner caller context - returning special network interface");
                }
                // app callerContext with associated timeshift - return
                // 'special' NI
                return SPECIAL_NETWORK_INTERFACE;
            }
            else
            {
                // other callerContext with associated timeshift - return 'real'
                // NI
                if (log.isDebugEnabled())
                {
                    log.debug(id + "caller context is different from owner caller context - returning broadcast network interface");
                }
                return getBroadcastNetworkInterface();
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "timeShift is not enabled - returning broadcast network interface");
            }
            // no associated timeshift - return 'real' NI
            return getBroadcastNetworkInterface();
        }
    }

    private ExtendedNetworkInterface getBroadcastNetworkInterface()
    {
        if (!checkInitialized("getBroadcastNetworkInterface")) return null;
        if (isNotPresenting("getBroadcastNetworkInterface")) return null;

        TimeShiftWindowClient tempTimeShiftWindowClient;
        synchronized (lock)
        {
            tempTimeShiftWindowClient = timeShiftWindowClient;
        }
        if (tempTimeShiftWindowClient != null)
        {
            ExtendedNetworkInterface ni = tempTimeShiftWindowClient.getNetworkInterface();
            if (ni == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "getBroadcastNetworkInterface - network interface was null");
                }
            }
            if (log.isDebugEnabled())
            {
                log.debug(id + "getBroadcastNetworkInterface returning: " + ni);
            }
            return ni;
        }
        if (log.isInfoEnabled())
        {
            log.info(id + "getBroadcastNetworkInterface - timeShiftWindowClient was null");
        }
        return null;
    }

    public ServiceContentHandler[] getServiceContentHandlers()
    {
        assertInitialized();
        AppDomain tempAppDomain;
        ServiceMediaHandler tempServiceMediaHandler;
        synchronized (lock)
        {
            tempAppDomain = appDomain;
            tempServiceMediaHandler = serviceMediaHandler;
        }

        ServiceContentHandler[] appServiceContentHandlers = tempAppDomain.getServiceContentHandlers();
        ServiceContentHandler[] serviceContentHandlersCopy = new ServiceContentHandler[appServiceContentHandlers.length + 1];
        System.arraycopy(appServiceContentHandlers, 0, serviceContentHandlersCopy, 1, appServiceContentHandlers.length);
        serviceContentHandlersCopy[0] = tempServiceMediaHandler;
        return serviceContentHandlersCopy;
    }

    public Service getService()
    {
        assertInitialized();
        synchronized (lock)
        {
            return service;
        }
    }

    public ServiceMediaHandler getServiceMediaHandler()
    {
        assertInitialized();
        synchronized (lock)
        {
            return serviceMediaHandler;
        }
    }

    /**
     * Report if this delegate is currently presenting the service.
     * 
     * @param service
     *            to evaluate
     * 
     * @return true if the passed-in service is the same service that is currently presenting
     */
    public boolean isPresenting(Service service)
    {
        assertInitialized();
        Service tempService;
        synchronized (lock)
        {
            tempService = this.service;
        }
        return tempService != null && tempService.equals(service);
    }

    private String stateToString(int state)
    {
        switch (currentState)
        {
            case STATE_PRESENTING:
                return "STATE_PRESENTING";
            case STATE_PRESENTING_PENDING_WAIT_TSW_READY:
                return "STATE_PRESENTING_PENDING_WAIT_TSW_READY";
            case STATE_PRESENTING_PENDING_TSW_READY:
                return "STATE_PRESENTING_PENDING_TSW_READY";
            case STATE_PRESENTING_PENDING_PLAYER_STARTING:
                return "STATE_PRESENTING_PENDING_PLAYER_STARTING";
            case STATE_NOT_PRESENTING:
                return "STATE_NOT_PRESENTING";
            case STATE_STOP_PENDING_WAIT_PLAYER_START:
                return "STATE_STOP_PENDING_WAIT_PLAYER_START";
            case STATE_STOP_PENDING_WAIT_PLAYER_STOP:
                return "STATE_STOP_PENDING_WAIT_PLAYER_STOP";
            case STATE_STOP_PENDING_PLAYER_NOT_STARTED:
                return "STATE_STOP_PENDING_PLAYER_NOT_STARTED";
            default:
                return "UNEXPECTED STATE: " + state;
        }
    }

    /**
     * Adds a listenter for time-shift events related to this
     * <code>TimeShiftProperties</code>.
     * <p/>
     * param listener The listener to add.
     */
    public void addTimeShiftListener(TimeShiftListener addingTimeShiftListener)
    {
        // TODO: notify that we are buffering or buffer not found when added?
    }

    /**
     * Removes a listener for time-shift events related to this
     * <code>TimeShiftProperties</code>
     */
    public void removeTimeShiftListener(TimeShiftListener removingTimeShiftListener)
    {
        // no-op
    }

    //NOTE: not used (ServiceContext does not call the delegate to get last service buffered preference)
    public boolean getLastServiceBufferedPreference()
    {
        assertInitialized();
        synchronized (lock)
        {
            return bufferLastService;
        }
    }

    /**
     * Gets the maximum content buffering duration. If this method is called
     * before <code>setMaximumDuration</code> has ever been called, or if
     * content buffering is disabled for this <code>ServiceContext</code> the
     * value returned SHALL be 0.
     * 
     * @return The maximum content buffering durat ion in seconds.
     */
    //NOTE: not used (ServiceContext does not call the delegate to get maxDuration)
    public long getMaximumDuration()
    {
        assertInitialized();
        synchronized (lock)
        {
            return maxDuration;
        }
    }

    /**
     * Gets the minimum content buffering duration. If this method is called
     * before setMinimumDuration has ever been called, or if content buffering
     * is disabled for this <code>ServiceContext</code> the value returned SHALL
     * be 0.
     * 
     * @return The minimum content buffering duration in seconds.
     */
    //NOTE: not used (ServiceContext does not call the delegate to get minDuration)
    public long getMinimumDuration()
    {
        synchronized (lock)
        {
            return minDuration;
        }
    }

    /**
     * Gets the save time-shift contents at service change preference.
     * 
     * @return True if save time-shift contents at service selection preference
     *         is enabled, otherwise returns false.
     */
    //NOTE: not used (ServiceContext does not call the delegate to get the save preference)
    public boolean getSavePreference()
    {
        assertInitialized();
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "getLastServiceBufferedPreference: " + bufferLastService);
            }
            return false;
        }
    }

    /**
     * Sets a preference to buffer the last service. This method has no effect
     * if the size of the time-shift buffer associated with the
     * <code>ServiceContext</code> object implementing this interface is set to
     * zero.
     * 
     * @param buffer
     *            If true the implementation will buffer the service selected by
     *            the <code>ServiceContext</code> object implementing this
     *            interface, based on time-shift buffer availability; see the
     *            OCAP DVR API specification time-shift buffer requirements. If
     *            false the last service will not be bufferred.
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             <code>ServiceContextPermission("*","own")</code> for the
     *             <code>ServiceContext</code> object that implements this
     *             <code>TimeShiftProperties</code>.
     */
    public void setLastServiceBufferedPreference(boolean buffer) throws SecurityException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + "setLastServiceBufferedPreference: " + buffer);
        }

        synchronized (lock)
        {
            bufferLastService = buffer;
        }
        if (!buffer)
        {
            stopLastChannelBuffering();
        }
    }

    /**
     * Sets the maximum duration of content that MAY be buffered for this
     * <code>ServiceContext</code>. Informs the implementation that storing more
     * content than this is not needed by the application owning this
     * <code>ServiceContext</code>. </p>
     * <p>
     * This method MAY be called at any time regardless of service context
     * state.
     * </p>
     * <p/>
     * 
     * @param maxDuration
     *            Maximum duration in seconds.
     * 
     * @throws IllegalArgumentException
     *             if the parameter is less than the duration set by the
     *             <code>setMinimumDuration</code> method, or if the parameter
     *             is less than the duration returned by
     *             {@link OcapRecordingManager#getSmallestTimeShiftDuration}.
     * @throws SecurityException
     *             if the calling application does not have
     *             <code>ServiceContextPermission("*","own")</code> for the
     *             <code>ServiceContext</code> object that implements this
     *             <code>TimeShiftProperties</code>.
     */
    public void setMaximumDuration(long maxDuration) throws SecurityException
    {

        assertInitialized();
        TimeShiftWindowClient tempTimeShiftWindowClient;
        long oldMaxDuration;
        synchronized (lock)
        {
            tempTimeShiftWindowClient = timeShiftWindowClient;
            oldMaxDuration = this.maxDuration;
            this.maxDuration = maxDuration;
        }
        if (tempTimeShiftWindowClient != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "setMaximumDuration(" + maxDuration + "): was " + oldMaxDuration);
            }
            tempTimeShiftWindowClient.setDesiredDuration(maxDuration);
        }
    }

    /**
     * Sets the minimum duration of content that SHALL be buffered for this
     * <code>ServiceContext</code>. Setting the minimum duration to 0 disables
     * time shifting on the ServiceContext. </p>
     * <p>
     * This method MAY be called at any time regardless of service context
     * state. However, enabling time-shifting or changing the minimum duration
     * SHALL NOT take affect until the <code>ServiceContext</code> is in the not
     * presenting state, presentation pending state, or a new service is
     * selected. If the same service is selected it is implementation dependent
     * regarding whether time-shift enabling takes affect during the selection.
     * </p>
     * <p>
     * Disabling time shifting by setting the minimum duration to 0 SHOULD take
     * effect immediately.
     * </p>
     * <p>
     * When enabling of time shifting by changing the minimum duration from zero
     * to a positive value takes effect, a TimeShiftControl SHALL be added to
     * the associated JMF player. When time shifting is disabled by changing the
     * minimum duration to zero any existing TimeShiftControl SHALL be removed
     * from the associated JMF player.
     * </p>
     * <p>
     * An increase in minimum duration MUST NOT cause any loss of previously
     * buffered content for the current service.
     * </p>
     * <p/>
     * 
     * @param minDuration
     *            Minimum duration in seconds.
     * 
     * @throws IllegalArgumentException
     *             If the parameter is greater than the current value and Host
     *             device does not have enough space to meet the request, or if
     *             the parameter is greater than the maximum duration set by the
     *             <code>setMaximumDuration</code> method, or if the parameter
     *             is less than the duration returned by
     *             {@link OcapRecordingManager#getSmallestTimeShiftDuration}.
     * @throws SecurityException
     *             if the calling application does not have
     *             <code>ServiceContextPermission("*","own")</code> for the
     *             <code>ServiceContext</code> object that implements this
     *             <code>TimeShiftProperties</code>.
     */
    public void setMinimumDuration(long minDuration) throws SecurityException
    {

        assertInitialized();
        TimeShiftWindowClient tempTimeShiftWindowClient;
        DVRServiceContextDelegateListener tempDVRServiceContextDelegateListener;
        long oldMinDuration;
        int tempCurrentState;
        long tempInitialSequence;

        synchronized (lock)
        {
            tempTimeShiftWindowClient = timeShiftWindowClient;
            oldMinDuration = this.minDuration;
            tempCurrentState = currentState;
            tempDVRServiceContextDelegateListener = dvrServiceContextDelegateListener;
            tempInitialSequence = initialSequence;
            this.minDuration = minDuration;
        }
        // If we are tuned, let's set our current buffering requirements
        if (tempTimeShiftWindowClient != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "setMinimumDuration(" + minDuration + ")");
            }
            if (oldMinDuration == 0 && minDuration > 0 && tempCurrentState == STATE_PRESENTING)
            {
                int timeShiftWindowClientState = tempTimeShiftWindowClient.getState();
                if (timeShiftWindowClientState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER
                        || timeShiftWindowClientState == TimeShiftManager.TSWSTATE_BUFF_PENDING
                        || timeShiftWindowClientState == TimeShiftManager.TSWSTATE_BUFFERING)
                {
                    setBuffering(true, true, tempInitialSequence);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + "setMinimumDuration - timeShiftWindowClient not in state allowing attachment for buffering: "
                                + tempTimeShiftWindowClient);
                    }
                }
                tempTimeShiftWindowClient.setMinimumDuration(minDuration);
            }
            else
            {
                if (oldMinDuration > 0 && minDuration == 0)
                {
                    // min duration was not zero and now is...notify recording
                    // stopped
                    tempDVRServiceContextDelegateListener.recordingStopped(RecordingTerminatedEvent.USER_STOP);
                    setBuffering(false, true, tempInitialSequence);
                }
            }
        }
    }

    /*
     * Required by TimeShiftProperties interface, but a no-op, since the
     * presentations are stored in the DVRServiceContext implementation.
     * 
     * DVRPresentation mediaTime will be updated with valid mediatimes (if
     * necessary) in updatePresentation
     */
    public void setPresentation(Service service, Time time, float rate, boolean action, boolean persistent)
    {
        // no-op
    }

    public NetworkInterface getNetworkInterface(boolean presentation)
    {
        // presentation == false means return NI corresponding to live content,
        // true means return NI corresponding
        // to time-shifted content
        if (log.isDebugEnabled())
        {
            log.debug(id + "getNetworkInterface - presentation: " + presentation);
        }
        synchronized(lock)
        {
            if (currentState != STATE_PRESENTING)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + "getNetworkInterface(boolean) - not presenting - returning null");
                }
                return null;
            }
        }
        if (presentation)
        {
            // presentation = true, return 'special' NI
            return SPECIAL_NETWORK_INTERFACE;
        }
        return getBroadcastNetworkInterface();
    }

    /**
     * Sets a preference to retain the time-shift contents for the
     * <code>ServiceContext</code> when a new service is selected. When enabled
     * the time-shift contents are saved back to the value returned by the
     * <code>getMaxTimeShiftDuration</code> method.
     * 
     * @param save
     *            If true the implementation will retain the time-shift contents
     *            for the <code>ServiceContext</code> when a new service is
     *            selected. If false the time-shift contents are flushed when a
     *            new service is selected.
     * 
     * @throws IllegalArgumentException
     *             if the parameter is true and the Host device does not have
     *             the hardware resources to support the preference.
     * @throws SecurityException
     *             if the calling application does not have
     *             <code>ServiceContextPermission("*","own")</code> for the
     *             <code>ServiceContext</code> object that implements this
     *             <code>TimeShiftProperties</code>.
     */
    public void setSavePreference(boolean save)
    {
        // no-op
    }

    // asumes lock is held
    private synchronized void assertInitialized()
    {
        if (!initialized)
        {
            throw new IllegalStateException("initialize not called before calling present");
        }
    }

    // Similar to assertInitialized() but provides an error message and
    // initalization flag for initialization
    private synchronized boolean checkInitialized(String msgPrefix)
    {
        if (!initialized)
        {
            if (log.isInfoEnabled())
            {
                log.info(id + msgPrefix + ": DVRBroadcastServiceContextDelegate not initialized");
            }
        }
        return initialized;
    }

    private boolean isNotPresenting(String msgPrefix)
    {
        boolean notPresenting;
        synchronized (lock)
        {
            notPresenting = (currentState == STATE_NOT_PRESENTING);
        }
        if (notPresenting)
        {
            if (log.isInfoEnabled())
            {
                log.info(id + msgPrefix + ": DVRBroadcastServiceContextDelegate not presenting");
            }
        }
        return notPresenting;
    }

    /**
     * Stop presenting and release resources.  This method is responsible for stopping the player and releasing 
     * resources once the player is stopped.
     *
     * The method may be called by the implementation multiple times through a number of state transitions.  
     *
     * If the method is called when a player is in a transition to the Started state, the delegate will transition 
     * to the STATE_STOP_PENDING_WAIT_PLAYER_START state and expect to be called again when a StartEvent 
     * ControllerEvent is received and the delegate is in the STATE_PRESENTING state.
     *
     * If the method is called when a player is in the Started state, the delegate will transition to the 
     * STATE_STOP_PENDING_WAIT_PLAYER_STOP state, call stop on the player, and expect to be called again when a 
     * StopEvent ControllerEvent is received and the delegate is in the STATE_NOT_PRESENTING state.
     *
     * @param eventClass the ServiceContextEvent class to use as the not presenting class
     * @param reason the ServiceContextEvent class reason to use as the not presenting reason
     * @param presentSequence the sequence related to the present request being stopped - ignore if stale
     */
    private void stopPresentingWithReason(Class eventClass, int reason, long presentSequence)
    {
        assertInitialized();
        ServiceContextDelegateListener tempServiceContextDelegateListener;
        ServiceMediaHandler tempServiceMediaHandler;
        PresentationModeListener tempPresentationModeListener;
        ControllerListener tempControllerListener;

        boolean stopPlayer = false;
        boolean releaseResources = false;

        //if releaseResources is going to be called, reset the sequence so a follow-on call to stopPresenting or present will be no-op'd
        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "stopPresentingWithReason - type: " + eventClass.getName() + ", reason: " + reason + ", " +
                        "current state: " + stateToString(currentState));
            }

            if (initialSequence != presentSequence)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "sequence changed - not stopping - sequence expected: " + presentSequence + " - is now: " + initialSequence);
                }
                return;
            }

            cachedStopPresentingEventClass = eventClass;
            cachedStopPresentingEventReason = reason;

            tempServiceContextDelegateListener = serviceContextDelegateListener;
            tempServiceMediaHandler = serviceMediaHandler;
            tempPresentationModeListener = presentationModeListener;
            tempControllerListener = controllerListener;

            switch (currentState)
            {
                case STATE_PRESENTING:
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "setting currentState to STATE_STOP_PENDING_WAIT_PLAYER_STOP");
                    }
                    currentState = STATE_STOP_PENDING_WAIT_PLAYER_STOP;
                    stopPlayer = true;
                    break;
                case STATE_PRESENTING_PENDING_WAIT_TSW_READY:
                    //stop called while trying to acquire the tsw - change the state to stop pending wait tsw ready
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                    }
                    currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                    releaseResources = true;
                    break;
                case STATE_PRESENTING_PENDING_TSW_READY:
                    //a tsw was acquired - ok to proceed
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                    }
                    currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                    releaseResources = true;
                    break;
                case STATE_PRESENTING_PENDING_PLAYER_STARTING:
                    //acquired resources, player created and player start called but PresentationModeListener notification not yet received - 
                    //check if in a transition to Started in order to wait for the Started notification prior to stopping player
                    int playerCurrentState = serviceMediaHandler.getState();
                    int playerTargetState = serviceMediaHandler.getTargetState();
                    if (log.isInfoEnabled())
                    {
                        log.info(id + serviceMediaHandler + " - player target state: " + playerTargetState + ", player current state: " + playerCurrentState);
                    }
                    if (playerTargetState == Controller.Started && playerCurrentState != Controller.Started)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_STOP_PENDING_WAIT_PLAYER_START");
                        }
                        currentState = STATE_STOP_PENDING_WAIT_PLAYER_START;
                    }
                    else
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_STOP_PENDING_WAIT_PLAYER_STOP");
                        }
                        currentState = STATE_STOP_PENDING_WAIT_PLAYER_STOP;
                        //ok to stop player - not in a transition to started
                        stopPlayer = true;
                    }
                    break;
                case STATE_NOT_PRESENTING:
                    //may be called after prepareToPresent is called but before present is called - post notPresenting
                    //when present is later called, no-op it
                    releaseResources = true;
                    break;
                case STATE_STOP_PENDING_WAIT_PLAYER_START:
                case STATE_STOP_PENDING_WAIT_PLAYER_STOP:
                    //already stopping - wait for player start or stop before stopping
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + "ignoring stopPresentingWithReason called in state: " + stateToString(currentState));
                    }
                    break;
                case STATE_STOP_PENDING_PLAYER_NOT_STARTED:
                    //receiving stopPresentingWithReason with a stopped player, release resources
                    releaseResources = true;
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "stopPresentingWithReason called in unexpected state: " + stateToString(currentState));
                    }
            }
        }

        if (stopPlayer)
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "stopping player: " + tempServiceMediaHandler);
            }
            if (tempServiceMediaHandler != null)
            {
                tempServiceContextDelegateListener.playerStopping(tempServiceMediaHandler);
                tempServiceMediaHandler.stop();
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "expected to stop player but serviceMediaHandler null");
                }
            }
        }

        if (releaseResources)
        {
            //if an (already-stopped_ player exists, close it
            if (tempServiceMediaHandler != null)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "releasing resources - player not null - calling deallocate and close on the player: " + tempServiceMediaHandler);
                }
                try
                {
                    PresentationModeControl presentationModeControl = (PresentationModeControl)
                            tempServiceMediaHandler.getControl(PRESENTATION_MODE_CONTROL_CLASS_NAME);
                    if (presentationModeControl != null)
                    {
                        presentationModeControl.removePresentationModeListener(tempPresentationModeListener);
                    }
                    tempServiceMediaHandler.removeControllerListener(tempControllerListener);
                    // Notify 'playerStopping' and set the serviceMediaHandler to
                    // null before calling
                    // notifyNotPresenting() to avoid duplicate notifications
                    tempServiceMediaHandler.deallocate();
                    tempServiceMediaHandler.close();
                }
                // catch Exception (particularly runtime exceptions), because we
                // still want to release and notify below
                catch (Exception e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "Unable to stop player", e);
                    }
                }
            }

            //will release presenting resources once the notification of player stop has been received
            releasePresentingResources();
            // Notify listeners that the presentation has been terminated, only once resources were released
            notifyNotPresenting(eventClass, reason);
        }
    }

    /**
     * Release any resources acquired during presentation process. If
     * setLastServiceBufferedPreference(true) was called, and we were buffering,
     * request buffering via RecordingManager.
     * 
     * May be called in STATE_NOT_PRESENTING or STATE_STOP_PENDING_PLAYER_NOT_STARTED
     *
     */
    private void releasePresentingResources()
    {
        boolean tempBufferLastService;
        int tempCurrentState;
        Service tempService;
        BufferingRequest tempBufferingRequest;

        boolean callRequestBuffering = false;
        boolean callReleaseTSW = false;
        long newMaxDuration;
        long tempMinDuration;
        TimeShiftWindowClient tempTimeShiftWindowClient;

        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "releasePresentingResources - current state: " + stateToString(currentState));
            }
            if (!checkInitialized("releasePresentingResources"))
            {
                return;
            }
            tempBufferLastService = bufferLastService;
            tempCurrentState = currentState;
            tempMinDuration = minDuration;
            newMaxDuration = maxDuration;
            tempBufferingRequest = bufferingRequest;
            tempTimeShiftWindowClient = timeShiftWindowClient;

            if (minDuration > 0 && bufferLastService)
            {
                if (newMaxDuration == 0)
                {
                    newMaxDuration = minDuration;
                }
                if (tempBufferingRequest == null)
                {
                    tempBufferingRequest = recordingManager.createBufferingRequest(service, minDuration,
                            newMaxDuration, new ExtendedFileAccessPermissions(true, true, true, true, true, true, null,
                            null), initialCallerContext);
                    callRequestBuffering = true;
                    bufferingRequest = tempBufferingRequest;
                }
            }
            tempService = service;

            if (timeShiftWindowClient != null)
            {
                // Clear out the TimeShiftWindowClient
                if (log.isDebugEnabled())
                {
                    log.debug(id + "releasePresentingResources - releasing timeshiftwindowclient: " + timeShiftWindowClient);
                }
                callReleaseTSW = true;

                // update state
                timeShiftWindowClient = null;
            }
            minDuration = 0;
            maxDuration = 0;
            bufferLastService = false;
            service = null;
            serviceMediaHandler = null;

            if (log.isInfoEnabled())
            {
                log.info(id + "setting currentState to STATE_NOT_PRESENTING");
            }
            currentState = STATE_NOT_PRESENTING;
            if (log.isInfoEnabled())
            {
                log.info(id + "resetting sequence");
            }
            initialSequence = -1;

            componentLocators = null;
            // resetting to blocked
            presentationBlocked = true;
            try
            {
                if (tvTimerSpecIsScheduled)
                {
                    tvTimerSpec.removeTVTimerWentOffListener(tvTimerWentOffListener);
                    tvTimer.deschedule(tvTimerSpec);
                    tvTimerSpecIsScheduled = false;
                }
            }
            catch (Throwable t)
            {
                // ignore
            }
            timeShiftWindowChangedListener = null;
            controllerListener = null;
            presentationModeListener = null;
            tvTimerWentOffListener = null;
        }

        if (callReleaseTSW)
        {
            tempTimeShiftWindowClient.release();
        }

        // Remove any scheduled buffer enablers that are around - may be re-added if last channel buffering is enabled
        recordingManager.removeDisableBufferingListener(disableBufferingListener);

        // If last channel buffering has been requested, do it here.
        // We only change the buffer request if we've started buffering on the
        // current stream.
        if (tempMinDuration > 0 && tempBufferLastService && (tempCurrentState == STATE_PRESENTING))
        {
            // if we're going to request buffering, re-add the listener (will be
            // removed in destroy)
            recordingManager.addDisableBufferingListener(disableBufferingListener);

            if (tempBufferingRequest != null)
            {
                if (callRequestBuffering)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + "releasePresentingResources - Creating new BufferingRequest");
                    }
                    recordingManager.requestBuffering(tempBufferingRequest);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + "releasePresentingResources - Changing BufferingRequest to new service");
                    }
                    tempBufferingRequest.setService(tempService);
                    try
                    {
                        if (newMaxDuration != tempBufferingRequest.getMaxDuration())
                        {
                            tempBufferingRequest.setMaxDuration(newMaxDuration);
                        }
                        if (tempMinDuration != tempBufferingRequest.getMinimumDuration())
                        {
                            tempBufferingRequest.setMinimumDuration(tempMinDuration);
                        }
                    }
                    catch (IllegalArgumentException e)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "releasePresentingResources - unable to set min&max duration on buffer request");
                        }
                    }
                }
            }
        }
    }

    /**
     * Turn buffering on or off.
     *
     * Will not allow buffering if minDuration <= zero.
     *
     * @param enableBuffering
     *            The Mode we want buffering to be in (on or off).
     * @param notifyJMF
     * @param presentSequence
     */
    private void setBuffering(boolean enableBuffering, boolean notifyJMF, long presentSequence)
    {
        ServiceMediaHandler tempServiceMediaHandler;
        boolean attachedForBuffering;
        boolean bufferingEnabled = recordingManager.isBufferingEnabled();
        long tempMinDuration;
        boolean failed = true;
        TimeShiftWindowClient tempTimeShiftWindowClient;
        int tempCurrentState;
                
        synchronized (lock)
        {
            //enable buffering check should check for a race - disable buffering can be called at any time
            if (enableBuffering)
            {
                if (initialSequence != presentSequence)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "sequence changed - ignoring setBuffering(true) - sequence expected: " + presentSequence + " - is now: " + initialSequence);
                    }
                    return;
                }
            }
            tempCurrentState = currentState;
            tempMinDuration = minDuration;
            tempTimeShiftWindowClient = timeShiftWindowClient;
            if (tempTimeShiftWindowClient == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "setBuffering: " + enableBuffering + ", notify: " + notifyJMF
                            + " called with null timeshiftwindowclient - ignoring");
                }
                return;
            }
            tempServiceMediaHandler = serviceMediaHandler;
            // bitwise operation
            attachedForBuffering = (timeShiftWindowClient.getUses() & TimeShiftManager.TSWUSE_BUFFERING) != 0;
            if ((enableBuffering && attachedForBuffering) || (!enableBuffering && !attachedForBuffering))
            {
                return;
            }
        }

        if (log.isInfoEnabled())
        {
            log.info(id + "setBuffering: " + enableBuffering + ", notify: " + notifyJMF + ", tswclient: "
                    + tempTimeShiftWindowClient + ", recordingManagerBufferingEnabled: " + bufferingEnabled
                    + ", serviceMediaHandler: " + tempServiceMediaHandler);
        }

        //we may get a request to enable buffering from presentation code if presenting live from the buffer, even if minduration is zero..
        //allow it
        boolean isPresentLiveFromBuffer = "true".equalsIgnoreCase(PropertiesManager.getInstance().getProperty(PRESENT_LIVE_FROM_BUFFER_PARAM, "false"));

        if (enableBuffering && ((isPresentLiveFromBuffer || (tempMinDuration > 0))) && bufferingEnabled)
        {
            try
            {
                tempTimeShiftWindowClient.attachFor(TimeShiftManager.TSWUSE_BUFFERING);
                failed = false;
            }
            catch (IllegalStateException e)
            {
                // can happen due to race conditions
                if (log.isInfoEnabled())
                {
                    log.info(id + "Unable to attach for buffering in time shift window client: " + e.getMessage());
                }
            }
            catch (IllegalArgumentException e)
            {
                // can happen due to race conditions
                if (log.isInfoEnabled())
                {
                    log.info(id + "Unable to attach for buffering in time shift window client: " + e.getMessage());
                }
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "Unable to attach for bufering in time shift window client", e);
                }
            }
        }
        // don't check for recordingmanager bufferingEnabled...
        else if (!enableBuffering)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + "detaching for buffering");
                }
                tempTimeShiftWindowClient.detachFor(TimeShiftManager.TSWUSE_BUFFERING);
                failed = false;
            }
            catch (IllegalStateException e)
            {
                // can happen due to race conditions
                if (log.isInfoEnabled())
                {
                    log.info(id + "Detach for buffering failed.  Ignoring: " + e.getMessage());
                }
            }
            catch (IllegalArgumentException e)
            {
                // can happen due to race conditions
                if (log.isInfoEnabled())
                {
                    log.info(id + "Detach for buffering failed.  Ignoring: " + e.getMessage());
                }
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "Detach for buffering failed. Ignoring", e);
                }
            }
        }

        if (!failed && notifyJMF && (tempCurrentState == STATE_PRESENTING))
        {
            if (tempServiceMediaHandler instanceof TSBServiceMediaHandler)
            {
                ((TSBServiceMediaHandler) tempServiceMediaHandler).setBufferingMode(enableBuffering);
            }
        }
    }

    private void stopLastChannelBuffering()
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + "stopLastChannelBuffering");
        }
        BufferingRequest tempBufferingRequest;
        synchronized (lock)
        {
            tempBufferingRequest = bufferingRequest;
            bufferingRequest = null;
        }
        if (tempBufferingRequest != null)
        {
            recordingManager.cancelBufferingRequest(tempBufferingRequest);
        }
    }

    private void notifyNoChange()
    {
        assertInitialized();
        ServiceContextDelegateListener tempServiceContextDelegateListener;
        synchronized (lock)
        {
            tempServiceContextDelegateListener = serviceContextDelegateListener;
        }
        tempServiceContextDelegateListener.presentingNoChange();
    }

    private void notifyPresentingNormalContent()
    {
        assertInitialized();
        ServiceContextDelegateListener tempServiceContextDelegateListener;
        synchronized (lock)
        {
            tempServiceContextDelegateListener = serviceContextDelegateListener;
        }
        tempServiceContextDelegateListener.presentingNormalContent();
    }

    private void notifyPresentingAlternativeContent(Class alternativeContentClass, int alternativeContentErrorEventReasonCode)
    {
        assertInitialized();
        ServiceContextDelegateListener tempServiceContextDelegateListener;
        synchronized (lock)
        {
            tempServiceContextDelegateListener = serviceContextDelegateListener;
        }
        tempServiceContextDelegateListener.presentingAlternativeContent(alternativeContentClass, alternativeContentErrorEventReasonCode);
    }

    private void notifyNotPresenting(Class eventClass, int reasonCode)
    {
        assertInitialized();
        ServiceMediaHandler tempServiceMediaHandler;
        ServiceContextDelegateListener tempServiceContextDelegateListener;
        synchronized (lock)
        {
            tempServiceMediaHandler = serviceMediaHandler;
            tempServiceContextDelegateListener = serviceContextDelegateListener;
        }
        if (tempServiceMediaHandler != null)
        {
            tempServiceContextDelegateListener.playerStopping(tempServiceMediaHandler);
        }
        tempServiceContextDelegateListener.notPresenting(eventClass, reasonCode);
    }

    class DisableBufferingListenerImpl implements DisableBufferingListener
    {
        public void notifyBufferingDisabledStateChange(boolean enabled)
        {
            if (!checkInitialized("BufferingDisabledStateChange notification fired"))
                return;

            if (log.isInfoEnabled())
            {
                log.info(id + "notifyBufferingDisabledStateChanged - enabled: " + enabled);
            }

            //may be called while not presenting - pass in initial sequence - it's ignored
            long tempInitialSequence;
            synchronized(lock)
            {
                tempInitialSequence = initialSequence;
            }
            if (!enabled)
            {
                // If buffering has been marked as disabled
                // shut down service buffering and notify JMF
                setBuffering(false, true, tempInitialSequence);
            }
        }
    }

    class TimeShiftWindowChangedListenerImpl implements TimeShiftWindowChangedListener
    {
        private final long presentSequence;

        public TimeShiftWindowChangedListenerImpl(long presentSequence)
        {
            this.presentSequence = presentSequence;
        }

        public void tswStateChanged(TimeShiftWindowClient tswc, TimeShiftWindowStateChangedEvent e)
        {
            if (!checkInitialized("tswStateChanged fired")) return;
            
            DVRServiceContextDelegateListener tempDvrServiceContextDelegateListener;
            TimeShiftWindowClient tempTimeShiftWindowClient;

            boolean notifyTimeShiftBufferFound = false;
            boolean notifyNoTimeShiftBuffer = false;

            boolean callSetBuffering = false;
            boolean setBufferingEnableBuffering = false;
            boolean setBufferingNotifyJMF = false;

            boolean callStartPlayerAndApps = false;
            boolean enableInitialPlayerBuffering = false;

            boolean callRecordingStopped = false;
            int recordingStoppedReason = 0;

            boolean callStopPresentingWithReason = false;
            Class stopPresentingClass = null;
            int stopPresentingReason = 0;

            synchronized (lock)
            {
                int newState = e.getNewState();
                int oldState = e.getOldState();
                int changeReason = e.getReason();

                tempDvrServiceContextDelegateListener = dvrServiceContextDelegateListener;
                
                if (log.isInfoEnabled())
                {
                    log.info(id + "TSWCL - got new TSW state " + TimeShiftManager.stateString[newState] + " (" + newState
                            + ") from " + TimeShiftManager.stateString[oldState] + " (" + oldState + "), reason: "
                            + TimeShiftManager.reasonString[changeReason] + " for " + tswc + " in state: " + stateToString(currentState));
                }

                if (initialSequence != presentSequence)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "sequence changed - ignoring timeShiftWindowChanged event - sequence expected: " + presentSequence + " - is now: " + initialSequence);
                    }
                    return;
                }

                // notify (only if we have a minDuration set and last event was
                // NO_TSB)
                if (newState == TimeShiftManager.TSWSTATE_BUFFERING && minDuration > 0 && lastTimeShiftEventNoTimeShiftBuffer)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "TSWCL - BUFFERING after notification of NO_TIME_SHIFT_BUFFER - notify TIME_SHIFT_BUFFER_FOUND");
                    }
                    notifyTimeShiftBufferFound = true;
                    lastTimeShiftEventNoTimeShiftBuffer = false;
                }

                if (newState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER && minDuration > 0 
                        && !lastTimeShiftEventNoTimeShiftBuffer)
                {
                    // if we receive a TUNE_PENDING to NOT_READY_TO_BUFFER
                    // transition notification, proceed
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "TSWCL - NOT_READY_TO_BUFFER from TUNE_PENDING with min duration > 0 - notify NO_TIME_SHIFT_BUFFER");
                    }
                    notifyNoTimeShiftBuffer = true;
                    lastTimeShiftEventNoTimeShiftBuffer = true;
                }

                switch (newState)
                {
                    case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
                        if (oldState == TimeShiftManager.TSWSTATE_TUNE_PENDING)
                        {
                            // don't trigger startPlayer for service remap
                            if (changeReason == TimeShiftManager.TSWREASON_SERVICEREMAP)
                            {
                                // service remap, re-enable buffering but don't
                                // notify jmf
                                if (minDuration > 0)
                                {
                                    callSetBuffering = true;
                                    setBufferingEnableBuffering = true;
                                    setBufferingNotifyJMF = false;
                                }
                            }
                            else
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info(id + "TSWCL - READY_TO_BUFFER from TUNE_PENDING");
                                }
                                if (log.isInfoEnabled())
                                {
                                    log.info(id + "setting currentState to STATE_PRESENTING_PENDING_TSW_READY");
                                }
                                
                                currentState = STATE_PRESENTING_PENDING_TSW_READY;
    
                                if (log.isInfoEnabled())
                                {
                                    log.info(id + "TSWCL - READY_TO_BUFFER from TUNE_PENDING - calling startPlayerAndApps");
                                }
                                callStartPlayerAndApps = true;
                                if (minDuration > 0)
                                {
                                    if (log.isInfoEnabled())
                                    {
                                        log.info(id + "TSWCL - READY_TO_BUFFER from TUNE_PENDING and minduration > 0 - enable buffering");
                                    }
                                    enableInitialPlayerBuffering = true;
                                }

                            }
                        }
                        // re-enable buffering if we were in buffshutdown or
                        // not_ready_to_buffer with a minduration > 0, regardless of
                        // reason
                        if ((oldState == TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN || oldState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER)
                                && minDuration > 0)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(id + "TSWCL - READY_TO_BUFFER from BUFF_SHUTDOWN or NOT_READY_TO_BUFFER and min duration > 0- enabling buffering");
                            }
                            callSetBuffering = true;
                            setBufferingEnableBuffering = true;
                            setBufferingNotifyJMF = true;
                        }
                        break;
                    case TimeShiftManager.TSWSTATE_BUFFERING:
                        break;
                    case TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER:
                        if (oldState == TimeShiftManager.TSWSTATE_TUNE_PENDING)
                        {
                            // if we receive a TUNE_PENDING to NOT_READY_TO_BUFFER
                            // transition notification, proceed
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_PRESENTING_PENDING_TSW_READY");
                            }
                            currentState = STATE_PRESENTING_PENDING_TSW_READY;
                            
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "TSWCL - NOT_READY_TO_BUFFER from TUNE_PENDING - calling startPlayerAndApps");
                            }
                            callStartPlayerAndApps = true;
                        }
                        else
                        {
                            // Nothing to do. If we were attached for buffering, we
                            // would have transitioned
                            // through BUFFSHUTDOWN before getting here. We don't
                            // want to attach in
                            // this state - only when going to READY_TO_BUFFER
                        }
                        break;
                    case TimeShiftManager.TSWSTATE_IDLE:
                        if (log.isDebugEnabled())
                        {
                            log.debug(id + "TSWCL - IDLE");
                        }
                        //presentation will stop due to idle if necessary
                        //if starting up, stop presentation
                        if (currentState == STATE_PRESENTING_PENDING_WAIT_TSW_READY)
                        {
                            // should be no free interface, but always stop (there
                            // is no started player, so stop)
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                            }
                            currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                            callStopPresentingWithReason = true;
                            stopPresentingClass = SelectionFailedEvent.class;
                            stopPresentingReason = SelectionFailedEvent.INSUFFICIENT_RESOURCES;
                        }
                        // if we're presenting, the presentation layer will cause us
                        // to go to not presenting
                        break;
                    case TimeShiftManager.TSWSTATE_INTSHUTDOWN:
                        // Lost the interface, release resources, no signalling
                        // (detach for buffering and live playback, but don't stop
                        // presenting)
                        if (log.isDebugEnabled())
                        {
                            log.debug(id + "TSWCL - INTSHUTDOWN");
                        }
                        if (minDuration > 0)
                        {
                            // only notify for intlost and servicevanished reasons
                            // (other reasons are SDV, no signaling)
                            if (changeReason == TimeShiftManager.TSWREASON_INTLOST)
                            {
                                callRecordingStopped = true;
                                recordingStoppedReason = RecordingTerminatedEvent.RESOURCES_REMOVED;
                            }
                            if (changeReason == TimeShiftManager.TSWREASON_SERVICEVANISHED)
                            {
                                callRecordingStopped = true;
                                recordingStoppedReason = RecordingTerminatedEvent.SERVICE_VANISHED;
                            }
                        }
    
                        if ((tswc.getUses() & TimeShiftManager.TSWUSE_BUFFERING) != 0)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(id + "TSWCL - INTSHUTDOWN AND buffering use registered");
                            }
                            // Else, if we're doing buffered playback, just clear
                            // the buffering state.
                            // TODO: Need to inform JMF that we no longer have an
                            // NI, so that it won't attempt
                            // to fast forward or play past the end of the buffer.
                            callSetBuffering = true;
                            setBufferingEnableBuffering = false;
                            setBufferingNotifyJMF = false;
                        }
                        break;
                    case TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN:
                        if (log.isDebugEnabled())
                        {
                            log.debug(id + "TSWCL - BUFF_SHUTDOWN");
                        }
                        if (changeReason == TimeShiftManager.TSWREASON_SYNCLOST)
                        {
                            callRecordingStopped = true;
                            recordingStoppedReason = RecordingTerminatedEvent.RESOURCES_REMOVED;
                        }
                        if (changeReason == TimeShiftManager.TSWREASON_ACCESSWITHDRAWN)
                        {
                            callRecordingStopped = true;
                            recordingStoppedReason = RecordingTerminatedEvent.ACCESS_WITHDRAWN;
                        }
    
                        // always detach for buffering
                        // to fast forward or play past the end of the buffer (don't
                        // notify JMF because we still have buffered content)
                        callSetBuffering = true;
                        setBufferingEnableBuffering = false;
                        setBufferingNotifyJMF = false;
                        break;
                    case TimeShiftManager.TSWSTATE_RESERVE_PENDING:
                    case TimeShiftManager.TSWSTATE_TUNE_PENDING:
                    case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                        if (log.isDebugEnabled())
                        {
                            log.debug(id + "TSWCL - " + TimeShiftManager.stateString[newState] + " - ignoring transition");
                        }
                        break;
                    default:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "Unexpected state: " + newState);
                        }
                        break;
                }
            }
            if (notifyTimeShiftBufferFound)
            {
                tempDvrServiceContextDelegateListener.notifyTimeShiftEvent(TimeShiftEvent.TIME_SHIFT_BUFFER_FOUND);
            }
            if (notifyNoTimeShiftBuffer)
            {
                tempDvrServiceContextDelegateListener.notifyTimeShiftEvent(TimeShiftEvent.NO_TIME_SHIFT_BUFFER);
            }
            if (callSetBuffering)
            {
                setBuffering(setBufferingEnableBuffering, setBufferingNotifyJMF, presentSequence);
            }
            if (callRecordingStopped)
            {
                tempDvrServiceContextDelegateListener.recordingStopped(recordingStoppedReason);
            }
            if (callStopPresentingWithReason)
            {
                stopPresentingWithReason(stopPresentingClass, stopPresentingReason, presentSequence);
            }
            if (callStartPlayerAndApps)
            {
                startPlayerAndApps(presentSequence, enableInitialPlayerBuffering);
            }
        }

        public void tswCCIChanged(TimeShiftWindowClient tswc, CopyControlInfo cci)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "TSWCL - CCI Changed: " + cci + " - ignoring");
            }
        }
    }

    /**
     * Start player and applications.  May be called from STATE_PRESENTING_PENDING_TSW_READY
     */
    public void startPlayerAndApps(long presentSequence, boolean enableInitialPlayerBuffering)
    {
        ServiceDetails tempServiceDetails = null;
        ServiceMediaHandler tempServiceMediaHandler = null;
        PresentationModeListener tempPresentationModeListener;
        PersistentVideoModeSettings tempPersistentVideoModeSettings = null;
        ControllerListener tempControllerListener;
        boolean tempPresentationBlocked = false;
        AppDomain tempAppDomain = null;

        boolean callStopPresenting = false;
        boolean startPlayerAndApps = false;

        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "startPlayerAndApps - enableInitialPlayerBuffering: " + enableInitialPlayerBuffering + ", current state: " + stateToString(currentState) + ", tsw: " + timeShiftWindowClient);
            }

            if (initialSequence != presentSequence)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "sequence changed - not starting player and apps - sequence expected: " + presentSequence + " - is now: " + initialSequence);
                }
                return;
            }

            //if a stop was called after the lock was released in present or notifyTuneComplete, stop presentation
            switch (currentState)
            {
                case STATE_PRESENTING:
                case STATE_PRESENTING_PENDING_WAIT_TSW_READY:
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "ignoring unexpected startPlayerAndApps call in state: " + stateToString(currentState));
                    }
                    break;
                case STATE_PRESENTING_PENDING_TSW_READY:
                    startPlayerAndApps = true;
                    //ok (still verify tsw is ready
                    break;
                case STATE_PRESENTING_PENDING_PLAYER_STARTING:
                case STATE_NOT_PRESENTING:
                case STATE_STOP_PENDING_WAIT_PLAYER_START:
                case STATE_STOP_PENDING_WAIT_PLAYER_STOP:
                case STATE_STOP_PENDING_PLAYER_NOT_STARTED:
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "ignoring unexpected startPlayerAndApps call in state: " + stateToString(currentState));
                    }
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "ignoring unexpected startPlayerAndApps call in state: " + stateToString(currentState));
                    }
            }

            if (startPlayerAndApps)
            {
                tempServiceMediaHandler = null;
                tempPresentationModeListener = presentationModeListener;
                tempPersistentVideoModeSettings = persistentVideoModeSettings;
                tempPresentationBlocked = presentationBlocked;
                tempAppDomain = appDomain;
                tempControllerListener = controllerListener;

                try
                {
                    tempServiceDetails = ((ServiceExt) service).getDetails();
                    ExtendedNetworkInterface networkInterface = getBroadcastNetworkInterface();
                    if (networkInterface != null)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "creating serviceMediaHandler");
                        }
                        serviceMediaHandler = creationDelegate.createServiceMediaHandler(tempServiceDetails, componentLocators,
                            serviceContextResourceUsage, networkInterface, new MediaTimeBase());
                        //ensure the control is enabled before the player is available to applications, even though buffering
                        // may not yet have been started
                        if (enableInitialPlayerBuffering)
                        {
                            if (serviceMediaHandler instanceof TSBServiceMediaHandler)
                            {
                                ((TSBServiceMediaHandler) serviceMediaHandler).setBufferingMode(true);
                            }
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_PRESENTING_PENDING_PLAYER_STARTING");
                        }
                        currentState = STATE_PRESENTING_PENDING_PLAYER_STARTING;
                        tempServiceMediaHandler = serviceMediaHandler;
                    }
                    else
                    {
                        if (log.isWarnEnabled()) 
                        {
                            log.warn(id + "no networkInterface available");
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                        }
                        currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                        callStopPresenting = true;
                    }
                }
                catch (Exception e)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "unable to retrieve details - creating altcontent handler");
                    }
                    serviceMediaHandler = creationDelegate.createAlternativeContentMediaHandler(
                            AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND, serviceContextResourceUsage);
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "setting currentState to STATE_PRESENTING_PENDING_PLAYER_STARTING");
                    }
                    currentState = STATE_PRESENTING_PENDING_PLAYER_STARTING;
                    tempServiceMediaHandler = serviceMediaHandler;
                }
                if (!callStopPresenting)
                {
                    registerPlayerListeners(tempServiceMediaHandler,  tempPresentationModeListener, tempControllerListener);
                }
            }
        }

        if (callStopPresenting)
        {
            stopPresentingWithReason(SelectionFailedEvent.class, SelectionFailedEvent.INSUFFICIENT_RESOURCES, presentSequence);
        }
        else
        {
            if (tempServiceMediaHandler != null)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "starting serviceMediaHandler: " + tempServiceMediaHandler);
                }
                tempServiceMediaHandler.start();

                // Start applications if enabled.
                if (tempServiceDetails != null && tempAppDomain != null && tempPersistentVideoModeSettings != null && tempPersistentVideoModeSettings.isAppsEnabled()
                        && !tempPresentationBlocked)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + "appsEnabled and presentation not blocked - calling appDomain.select with service details: "
                                + tempServiceDetails);
                    }
                    tempAppDomain.select(tempServiceDetails, null);
                }
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "expected to start player but no player available");
                }
            }
        }
    }

    private void registerPlayerListeners(ServiceMediaHandler tempServiceMediaHandler,
                                         PresentationModeListener tempPresentationModeListener, ControllerListener tempControllerListener)
    {
        PresentationModeControl presentationModeControl = (PresentationModeControl) tempServiceMediaHandler.getControl(PRESENTATION_MODE_CONTROL_CLASS_NAME);
        if (presentationModeControl != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "registering listener on presentation mode control");
            }
            presentationModeControl.addPresentationModeListener(tempPresentationModeListener);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "no presentation mode control available, unable to add presentationModeListener");
            }
        }

        tempServiceMediaHandler.addControllerListener(tempControllerListener);
    }

    private void registerTimerWentOffListener(long sequence)
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "registerTimerWentOffListener - setting timer to enable buffering in "
                        + BUFFERING_AUTOSTART_DELAY + " milliseconds - sequence: " + sequence);
            }

            if (tvTimerSpecIsScheduled)
            {
                try
                {
                    tvTimer.deschedule(tvTimerSpec); // Remove anything that's
                                                     // there.
                    tvTimerSpecIsScheduled = false;
                    // Remove ourselves from the timer specs.
                    tvTimerSpec.removeTVTimerWentOffListener(tvTimerWentOffListener);
                }
                catch (Exception e)
                {
                    // no-op
                }
            }

            try
            {
                //listener will remove itself when triggered
                tvTimerWentOffListener = new TVTimerWentOffListenerImpl(sequence);
                tvTimerSpec.addTVTimerWentOffListener(tvTimerWentOffListener);
                tvTimerSpec = tvTimer.scheduleTimerSpec(tvTimerSpec);
                tvTimerSpecIsScheduled = true;
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "registerTimerWentOffListener - caught exception while attempting to schedule Delayed Buffering enabler", e);
                }
            }
        }
    }

    private void blockApplicationPresentation(boolean presentationBlocked)
    {
        boolean oldPresentationBlocked;
        boolean presentationBlockChanged;
        AppDomain tempAppDomain;
        Service tempService;
        synchronized (lock)
        {
            oldPresentationBlocked = this.presentationBlocked;
            presentationBlockChanged = (presentationBlocked != oldPresentationBlocked);
            this.presentationBlocked = presentationBlocked;
            tempAppDomain = appDomain;
            tempService = service;
        }
        // default presentationBlocked value of true allows this to go through
        // and then set presentationBlocked member
        // if we become unblocked
        if (presentationBlockChanged)
        {
            if (presentationBlocked)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "presentation is blocked - stopping appDomain");
                }
                tempAppDomain.stop();
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "presentation is not blocked - calling appDomain.select for service: " + tempService);
                }
                try
                {
                    tempAppDomain.select(((ServiceExt) tempService).getDetails(), null);
                }
                catch (SIRequestException sire)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "Exception selecting appDomain", sire);
                    }
                }
                catch (InterruptedException ie)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "Exception selecting appDomain", ie);
                    }
                }
            }
        }
    }

    public String toString()
    {
        // don't acquire lock here (may not be initialized when toString is
        // called)
        return "DVRBroadcastServiceContextDelegate: " + id + "service: " + service + ", player: " + serviceMediaHandler + ", state: "
                + stateToString(currentState);
    }

    private class TVTimerWentOffListenerImpl implements TVTimerWentOffListener
    {
        private final long presentSequence;

        TVTimerWentOffListenerImpl(long presentSequence)
        {
            synchronized (lock)
            {
                this.presentSequence = presentSequence;
            }
        }

        public void timerWentOff(TVTimerWentOffEvent tvTimerWentOffEvent)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "enablebuffering timer fired");
            }

            try
            {
                if (!checkInitialized("TVTimerWentOffListener fired"))
                    return;
                if (isNotPresenting("TVTimerWentOffListener fired"))
                    return;

                boolean callSetBuffering = false;
                boolean setBufferingEnableBuffering = false;
                boolean setBufferingNotifyJMF = false;

                synchronized (lock)
                {
                    // Remove listener from the timer spec
                    TVTimerSpec timerSpec = tvTimerWentOffEvent.getTimerSpec();
                    timerSpec.removeTVTimerWentOffListener(this);
                    tvTimerSpecIsScheduled = false;

                    if (initialSequence != presentSequence)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "sequence changed - ignoring timer notification - sequence expected: " + presentSequence + " - is now: " + initialSequence);
                        }
                        return;
                    }
                    if (currentState == STATE_PRESENTING)
                    {
                        // if minduration was set to zero before the timer went off,
                        // call setbuffering false
                        if (minDuration > 0)
                        {
                            int timeShiftWindowClientState = timeShiftWindowClient.getState();
                            if (timeShiftWindowClientState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER
                                    || timeShiftWindowClientState == TimeShiftManager.TSWSTATE_BUFF_PENDING
                                    || timeShiftWindowClientState == TimeShiftManager.TSWSTATE_BUFFERING)
                            {
                                callSetBuffering = true;
                                setBufferingEnableBuffering = true;
                                setBufferingNotifyJMF = true;
                            }
                            else
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug(id + "timeShiftWindowClient not in state allowing attachment for buffering");
                                }
                            }
                        }
                        else
                        {
                            callSetBuffering = true;
                            setBufferingEnableBuffering = false;
                            setBufferingNotifyJMF = true;
                        }

                    }
                }

                if (callSetBuffering)
                {
                    setBuffering(setBufferingEnableBuffering, setBufferingNotifyJMF, presentSequence);
                }
                else
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "timerWentOff - presentation state has changed - not enabling buffering");
                    }
                }
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "TimerWentOffListener - caught exception while starting buffering: " + e.getMessage(), e);
                }
            }
        }
    }

    private class ControllerListenerImpl implements ControllerListener
    {
        private final long presentSequence;

        ControllerListenerImpl(long presentSequence)
        {
            this.presentSequence = presentSequence;
        }

        public void controllerUpdate(ControllerEvent event)
        {
            if (!checkInitialized("Controller Event fired"))
            {
                return;
            }

            ServiceMediaHandler tempServiceMediaHandler;
            boolean callEnforceBlocking = false;
            boolean callStopPresentingWithReason = false;
            int stopPresentingReason = 0;
            Class stopPresentingClass = null;

            synchronized (lock)
            {
                tempServiceMediaHandler = serviceMediaHandler;

                if (log.isInfoEnabled())
                {
                    log.info(id + "controllerUpdate - event: " + event + ", presenting: "
                            + (STATE_PRESENTING == currentState) + ", state: " + stateToString(currentState));
                }

                if (initialSequence != presentSequence)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "sequence changed - ignoring controller event - sequence expected: " + presentSequence + " - is now: " + initialSequence);
                    }
                    return;
                }

                if (event.getSourceController() != tempServiceMediaHandler)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "ignoring controllerUpdate for non-current controller - event: " + event + ", current handler: " +
                                tempServiceMediaHandler + ", listener: " + this + ", state: " + stateToString(currentState));
                    }
                    return;
                }

                //check for ControllerErrorEvent or ResourceUnavailableEvent in all states and stop presentation with correct reason (PTE for started, SFE otherwise)
                if (event instanceof ControllerErrorEvent)
                {
                    if (currentState == STATE_PRESENTING)
                    {
                        if (event instanceof ResourceUnavailableEvent)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn(id + "received resourceUnavailableEvent");
                            }
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                            }
                            currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                            callStopPresentingWithReason = true;
                            stopPresentingClass = PresentationTerminatedEvent.class;
                            stopPresentingReason = PresentationTerminatedEvent.RESOURCES_REMOVED;
                        }
                        else
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn(id + "received ControllerErrorEvent");
                            }
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                            }
                            currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                            callStopPresentingWithReason = true;
                            stopPresentingClass = PresentationTerminatedEvent.class;
                            stopPresentingReason = PresentationTerminatedEvent.OTHER;
                        }
                    }
                    else
                    {
                        if (event instanceof ResourceUnavailableEvent)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn(id + "received resourceUnavailableEvent");
                            }
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                            }
                            currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                            callStopPresentingWithReason = true;
                            stopPresentingClass = SelectionFailedEvent.class;
                            stopPresentingReason = SelectionFailedEvent.INSUFFICIENT_RESOURCES;
                        }
                        else
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn(id + "received ControllerErrorEvent");
                            }
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                            }
                            currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                            callStopPresentingWithReason = true;
                            stopPresentingClass = SelectionFailedEvent.class;
                            stopPresentingReason = SelectionFailedEvent.OTHER;
                        }
                    }
                }
                else
                {
                    //handle other events 
                    switch (currentState)
                    {
                        case STATE_PRESENTING:
                            // handle presentation event (block/unblock apps based on
                            // normal/alternative media presentation event
                            callEnforceBlocking = true;
                            break;
                        case STATE_PRESENTING_PENDING_WAIT_TSW_READY:
                        case STATE_PRESENTING_PENDING_TSW_READY:
                        case STATE_PRESENTING_PENDING_PLAYER_STARTING:
                            // handle presentation event (block/unblock apps based on
                            // normal/alternative media presentation event
                            callEnforceBlocking = true;
                            break;
                        case STATE_NOT_PRESENTING:
                            if (log.isWarnEnabled())
                            {
                                log.warn(id + "ignoring unexpected controllerUpdate in state: " + stateToString(currentState));
                            }
                            break;
                        case STATE_STOP_PENDING_WAIT_PLAYER_START:
                            //handled in PresentationModeListener, since that listener causes the transition to the PRESENTING state, which 
                            //is what stopPresentingWithReason expects in order to initiate a player stop
                            break;
                        case STATE_STOP_PENDING_WAIT_PLAYER_STOP:
                            //if a StopByRequestEvent or ControllerClosedEvent are received, the player is now stopped - call stopPresentingWithReason 
                            //in the STATE_STOP_PENDING_PLAYER_NOT_STARTED state in order to release resources
                            if (event instanceof StopByRequestEvent || event instanceof ControllerClosedEvent)
                            {
                                //received the stop event - change state and post the event
                                if (log.isInfoEnabled())
                                {
                                    log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                                }
                                currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                                callStopPresentingWithReason = true;
                                stopPresentingReason = cachedStopPresentingEventReason;
                                stopPresentingClass = cachedStopPresentingEventClass;
                            }
                            break;
                        case STATE_STOP_PENDING_PLAYER_NOT_STARTED:
                            //there may be in-flight events - ignore
                            if (log.isDebugEnabled())
                            {
                                log.debug(id + "ignoring controllerUpdate in state: " + stateToString(currentState));
                            }
                            break;
                        default:
                            if (log.isWarnEnabled())
                            {
                                log.warn(id + "ignoring unexpected controllerUpdate in state: " + stateToString(currentState));
                            }
                    }
                }
            }

            if (callEnforceBlocking)
            {
                enforceBlocking(event);
            }

            if (callStopPresentingWithReason)
            {
                stopPresentingWithReason(stopPresentingClass, stopPresentingReason, presentSequence);
            }
        }

        /**
         * Enforce blocking
         *
         * @param event
         *            the controllerEvent to examine
         */
        private void enforceBlocking(ControllerEvent event)
        {
            PersistentVideoModeSettings tempPersistentVideoModeSettings;
            ResourceUsageImpl tempResourceUsage;
            synchronized (lock)
            {
                tempPersistentVideoModeSettings = persistentVideoModeSettings;
                tempResourceUsage = serviceContextResourceUsage;
            }
            // Nothing to do if apps are not enabled
            if (!tempPersistentVideoModeSettings.isAppsEnabled())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + "enforceBlocking - persistentVideoModeSettings apps not enabled");
                }
                return;
            }

            // Determine whether the presentation is currently blocked. Return
            // if this
            // event does not indicate the blocking state.
            if (event instanceof AlternativeMediaPresentationEvent)
            {
                // block here unless our resourceUsage is an EAS resourceUsage
                blockApplicationPresentation(!(tempResourceUsage.isResourceUsageEAS()));
            }
            else if (event instanceof NormalMediaPresentationEvent)
            {
                // always call blockApplicationPresentation if apps are enabled
                blockApplicationPresentation(false);
            }
        }
    }

    private class PresentationModeListenerImpl implements PresentationModeListener
    {
        private final long presentSequence;

        public PresentationModeListenerImpl(long presentSequence)
        {
            this.presentSequence = presentSequence;
        }

        public void normalContent()
        {
            if (!checkInitialized("Normal content notification fired in PresentationModeListenerImpl"))
            {
                return;
            }

            boolean notifyNormalContent = false;
            boolean callStopPresentingWithReason = false;
            int stopPresentingReason = 0;
            Class stopPresentingClass = null;
            boolean registerListener = false;

            synchronized (lock)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "presentationModeListener received normalContent notification - current state: " + stateToString(currentState));
                }

                if (initialSequence != presentSequence)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "sequence changed - ignoring normalContent - sequence expected: " + presentSequence + " - is now: " + initialSequence);
                    }
                    return;
                }

                switch (currentState)
                {
                    case STATE_PRESENTING:
                        //standard notification when recovering from altcontent while presenting, continue below
                        notifyNormalContent = true;
                        registerListener = minDuration > 0;
                        break;
                    case STATE_PRESENTING_PENDING_WAIT_TSW_READY:
                    case STATE_PRESENTING_PENDING_TSW_READY:
                    case STATE_PRESENTING_PENDING_PLAYER_STARTING:
                        //initial notification
                        notifyNormalContent = true;
                        registerListener = minDuration > 0;
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_PRESENTING");
                        }
                        currentState = STATE_PRESENTING;
                        break;
                    case STATE_NOT_PRESENTING:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected normalContent notification in state: " + stateToString(currentState));
                        }
                        break;
                    case STATE_STOP_PENDING_WAIT_PLAYER_START:
                        //stop called before player was started
                        notifyNormalContent = true;
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_PRESENTING");
                        }
                        currentState = STATE_PRESENTING;
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "normalContent notification in : " + stateToString(currentState) + " - player is now started - will call stopPresentingWithReason");
                        }
                        callStopPresentingWithReason = true;
                        stopPresentingClass = cachedStopPresentingEventClass;
                        stopPresentingReason = cachedStopPresentingEventReason;
                        break;
                    case STATE_STOP_PENDING_WAIT_PLAYER_STOP:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected normalContent notification in state: " + stateToString(currentState));
                        }
                        break;
                    case STATE_STOP_PENDING_PLAYER_NOT_STARTED:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected normalContent notification in state: " + stateToString(currentState));
                        }
                        break;
                    default:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected normalContent notification in state: " + stateToString(currentState));
                        }
                }

            }

            if (notifyNormalContent)
            {
                // must select appDomain synchronously prior to notifying apps of
                // NormalContent
                // it is ok to call appDomain.select from this listener because

                // there will be no notification of
                // normal content unless the selection is authorized

                //only unblock applications if not now stopping presentation
                if (!callStopPresentingWithReason)
                {
                    blockApplicationPresentation(false);
                }

                //post the event
                notifyPresentingNormalContent();

                // register a listener to start buffering (will no-op if we are
                // not waiting to buffer)

                // If we want to enable buffering, start a Timer to get to the
                // time when we want to do the
                // enable. Only do this if buffering is not enabled already.

                if (registerListener)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "registering timer to enable buffering");
                    }
                    registerTimerWentOffListener(presentSequence);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + "not registering timer to enable buffering");
                    }
                }
            }

            if (callStopPresentingWithReason)
            {
                stopPresentingWithReason(stopPresentingClass, stopPresentingReason, presentSequence);
            }
        }

        public void alternativeContent(Class alternativeContentClass, int alternativeContentReasonCode)
        {
            if (!checkInitialized("Alternative content notification fired in PresentationModeListenerImpl"))
            {
                return;
            }

            boolean notifyAlternativeContent = false;
            boolean callStopPresentingWithReason = false;
            int stopPresentingReason = 0;
            Class stopPresentingClass = null;

            synchronized (lock)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "presentationModeListener received alternativeContent notification - current state: " +
                            stateToString(currentState));
                }

                if (initialSequence != presentSequence)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "sequence changed - ignoring alternativeContent - sequence expected: " + presentSequence + " - is now: " + initialSequence);
                    }
                    return;
                }

                switch (currentState)
                {
                    case STATE_PRESENTING:
                        //standard notification when recovering from altcontent while presenting, continue below
                        notifyAlternativeContent = true;
                        break;
                    case STATE_PRESENTING_PENDING_WAIT_TSW_READY:
                    case STATE_PRESENTING_PENDING_TSW_READY:
                    case STATE_PRESENTING_PENDING_PLAYER_STARTING:
                        //initial notification
                        notifyAlternativeContent = true;
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_PRESENTING");
                        }
                        currentState = STATE_PRESENTING;
                        break;
                    case STATE_NOT_PRESENTING:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected alternativeContent notification in state: " + stateToString(currentState));
                        }
                        break;
                    case STATE_STOP_PENDING_WAIT_PLAYER_START:
                        //stop called before player was started
                        notifyAlternativeContent = true;
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_PRESENTING");
                        }
                        currentState = STATE_PRESENTING;
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "alternativeContent notification in : " + stateToString(currentState) + " - player is now started - will call stopPresentingWithReason");
                        }
                        callStopPresentingWithReason = true;
                        stopPresentingClass = cachedStopPresentingEventClass;
                        stopPresentingReason = cachedStopPresentingEventReason;
                        break;
                    case STATE_STOP_PENDING_WAIT_PLAYER_STOP:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected alternativeContent notification in state: " + stateToString(currentState));
                        }
                        break;
                    case STATE_STOP_PENDING_PLAYER_NOT_STARTED:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected alternativeContent notification in state: " + stateToString(currentState));
                        }
                        break;
                    default:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected alternativeContent notification in state: " + stateToString(currentState));
                        }
                }
            }

            if (notifyAlternativeContent)
            {
                notifyPresentingAlternativeContent(alternativeContentClass, alternativeContentReasonCode);
            }

            if (callStopPresentingWithReason)
            {
                stopPresentingWithReason(stopPresentingClass, stopPresentingReason, presentSequence);
            }
        }
    }
}
