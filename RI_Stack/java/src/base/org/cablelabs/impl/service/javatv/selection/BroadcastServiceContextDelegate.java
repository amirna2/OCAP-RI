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
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceMediaHandler;
import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterfaceManager;
import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback;
import org.cablelabs.impl.davic.net.tuning.SharableNetworkInterfaceController;
import org.cablelabs.impl.davic.net.tuning.SharableNetworkInterfaceManager;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.player.MediaTimeBase;
import org.cablelabs.impl.media.player.PresentationModeControl;
import org.cablelabs.impl.media.player.PresentationModeListener;
import org.cablelabs.impl.media.player.ServicePlayer;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceContextResourceUsageImpl;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.SimpleCondition;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.media.AlternativeMediaPresentationEvent;
import org.ocap.media.NormalMediaPresentationEvent;
import org.ocap.service.AlternativeContentErrorEvent;

public class BroadcastServiceContextDelegate implements ServiceContextDelegate
{
    private static final Logger log = Logger.getLogger(BroadcastServiceContextDelegate.class);

    private static final String PRESENTATION_MODE_CONTROL_CLASS_NAME = "org.cablelabs.impl.media.player.PresentationModeControl";

    private static final int STATE_NOT_PRESENTING = 1;

    //attempting to acquire a tuner
    private static final int STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE = 2;

    //a tuner was acquired
    private static final int STATE_PRESENTING_PENDING_TUNE_COMPLETE = 3;

    //a request to present a service has proceeded far enough to create a player and call start on the player    
    private static final int STATE_PRESENTING_PENDING_PLAYER_STARTING = 4;

    private static final int STATE_PRESENTING = 5;

    //if player is in the transition to started, wait for player started notification prior to stopping player
    private static final int STATE_STOP_PENDING_WAIT_PLAYER_START = 6;

    //wait for player stopped notification prior to releasing resources
    private static final int STATE_STOP_PENDING_WAIT_PLAYER_STOP = 7;

    //the player is now stopped and resources can be released
    private static final int STATE_STOP_PENDING_PLAYER_NOT_STARTED = 8;

    //the priority to use when registering an NI callback
    public static final int NI_CALLBACK_PRIORITY = 20;

    // members referencing parameters passed-in to initialize
    private ServiceContextDelegateListener serviceContextDelegateListener;

    private ServiceMediaHandlerCreationDelegate creationDelegate;

    private AppDomain appDomain;

    // never null this out
    private volatile Object lock;

    private ControllerListener controllerListener;

    private PresentationModeListener presentationModeListener;

    //currently presenting service
    private Service service;

    //currently presenting component locators
    private Locator[] componentLocators;

    private ServiceContextResourceUsageImpl serviceContextResourceUsage;

    // members constructed during presentation
    private ServiceMediaHandler serviceMediaHandler;

    private int currentState = STATE_NOT_PRESENTING;

    private boolean initialized;
    
    private boolean presentationBlocked = true; // default to blocked

    private PersistentVideoModeSettings persistentVideoModeSettings;

    private SharableNetworkInterfaceController sharableNetworkInterfaceController;

    //if stopPresentingWithReason is called but the PresentationModeListener has not yet received an event, wait for the PresentationMode event 
    // then stop presenting with the cached class and reason 
    private Class cachedStopPresentingEventClass;
    private int cachedStopPresentingEventReason;
    
    //simple condition tracking if resources are released - no resources acquired initially, default to true
    private final SimpleCondition resourcesReleasedCondition = new SimpleCondition(true);
    
    //the tune instance returned from tuneOrShareFor - null if tuneOrShareFor fails or ResourceClient#release is called, in order
    //to prevent a call to SharableNetworkInterfaceController#release in those cases
    //this field is set non-null when not holding a lock
    private volatile Object tuneToken;
    
    //no need to use 'this' in the identity hash lookup, just a unique identifier
    private final String id = "Id: 0x" + Integer.toHexString(System.identityHashCode(new Object())).toUpperCase() + ": ";
    
    //values provided in prepareToPresent
    private Service initialService;
    private Locator[] initialComponentLocators;
    private ServiceContextResourceUsageImpl initialServiceContextResourceUsage;
    private volatile long initialSequence = -1;
    //set to true if stopping due to a ResourceClient#release call
    private boolean stopDueToRelease;

    public synchronized void initialize(ServiceContextDelegateListener serviceContextDelegateListener,
            ServiceMediaHandlerCreationDelegate creationDelegate, AppDomain appDomain,
            PersistentVideoModeSettings persistentVideoModeSettings, Object lock)
    {
        synchronized (lock)
        {
            this.serviceContextDelegateListener = serviceContextDelegateListener;
            this.creationDelegate = creationDelegate;
            this.appDomain = appDomain;
            this.persistentVideoModeSettings = persistentVideoModeSettings;
            this.lock = lock;

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
            this.initialSequence = sequence;
            //callerContext is unused
            controllerListener = new ControllerListenerImpl(sequence);
            presentationModeListener = new PresentationModeListenerImpl(sequence);
        }
    }
    
    public void present(long presentSequence)
    {
        assertInitialized();
        ServiceMediaHandler tempServiceMediaHandler;
        boolean callStopPresenting = false;
        boolean callNotifyNoChange = false;
        boolean callUpdateServiceContextSelection = false;
        int stopPresentingReason = -1;
        SharableNetworkInterfaceController tempSharableNetworkInterfaceController = null;
        NetworkInterfaceCallback tempNetworkInterfaceCallback = null;
        Locator[] tempInitialComponentLocators;

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
                service = initialService;
                componentLocators = (Locator[]) Arrays.copy(initialComponentLocators, Locator.class);
    
                serviceContextResourceUsage = initialServiceContextResourceUsage;
                NetworkInterfaceResourceClient networkInterfaceResourceClient = new NetworkInterfaceResourceClient(presentSequence);
                ExtendedNetworkInterfaceManager networkInterfaceManager = (ExtendedNetworkInterfaceManager) ExtendedNetworkInterfaceManager.getInstance();
                SharableNetworkInterfaceManager sharableNetworkInterfaceManager = networkInterfaceManager.getSharableNetworkInterfaceManager();
                sharableNetworkInterfaceController = sharableNetworkInterfaceManager.createSharableNetworkInterfaceController(networkInterfaceResourceClient);
                tempSharableNetworkInterfaceController = sharableNetworkInterfaceController;
                tempNetworkInterfaceCallback = new NetworkInterfaceCallbackImpl(presentSequence);
                if (log.isDebugEnabled())
                {
                    log.debug(id + "sharableNetworkInterfaceController created");
                }
                if (log.isInfoEnabled())
                {
                    log.info(id + "setting currentState to STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE");
                }
                currentState = STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE;
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
            try
            {
                // We want to (ok "need to") do this without holding the lock (we may get
                //  NICallbacks before this returns) - don't use the service member, the lock isn't held here
                Object tempTuneToken = tempSharableNetworkInterfaceController.tuneOrShareFor(serviceContextResourceUsage, initialService, null,
                        tempNetworkInterfaceCallback, NI_CALLBACK_PRIORITY);
                tuneToken = tempTuneToken;
                resourcesReleasedCondition.setFalse();
                if (log.isDebugEnabled())
                {
                    log.debug(id + "updating resourcesReleasedCondition to false");
                }
                if (log.isInfoEnabled())
                {
                    log.info(id + "tuneOrShareFor on the SharableNetworkInterfaceController for service: " + initialService + " - returned tuneInstance: " + tempTuneToken);
                }
                ExtendedNetworkInterface networkInterface = tempSharableNetworkInterfaceController.getNetworkInterface();
                if (tempTuneToken == null || networkInterface == null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "stopping selection - null tuneInstance or null NI returned by the sharableNetworkInterfaceController - tuneInstance: " + tempTuneToken + ", NI: " + networkInterface);
                    }
                    //no need to change the state (no tune-related notifications will be received)
                    stopPresentingWithReason(SelectionFailedEvent.class, SelectionFailedEvent.INSUFFICIENT_RESOURCES, presentSequence);
                }
                else
                {
                    //returned from tuneOrShareFor without throwing an exception or returning null - if tuned, no callback will be received, start the player
                    //ok to start apps if not tuning (synced or unsynced)
                    //if not tuned, an NI callback will be received
                    if (networkInterface.isTuned(tempTuneToken))
                    {
                        if (log.isInfoEnabled()) 
                        {
                            log.info(id + "networkInterface already tuned - no need to wait for tune complete - starting player and apps");
                        }
                        //startPlayerAndApps will verify the NI is tuned if in PRESENTING_PENDING_WAIT_TUNE_COMPLETE
                        //otherwise, startPlayerAndApps expects the state to be PRESENTING_PENDING_TUNE_COMPLETE
                        startPlayerAndApps(networkInterface, tempTuneToken, presentSequence);
                    }
                }
            }
            catch (NetworkInterfaceException e)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "Unable to tune", e);
                }
                stopPresentingWithReason(SelectionFailedEvent.class, SelectionFailedEvent.INSUFFICIENT_RESOURCES, presentSequence);
            }
            catch (Throwable t)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "Unable to tune", t);
                }
                stopPresentingWithReason(SelectionFailedEvent.class, SelectionFailedEvent.OTHER, presentSequence);
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

        if (initialized)
        {
            synchronized (lock)
            {
                // reset state to default values
                service = null;
                componentLocators = null;

                if (log.isInfoEnabled())
                {
                    log.info(id + "setting currentState to STATE_NOT_PRESENTING");
                }
                currentState = STATE_NOT_PRESENTING;

                presentationBlocked = true; // default to blocked
            }
        }
        initialized = false;
    }

    public boolean canPresent(Service service)
    {
        return true;
    }

    public NetworkInterface getNetworkInterface()
    {
        assertInitialized();
        SharableNetworkInterfaceController tempSharableNetworkInterfaceController = null;
        synchronized (lock)
        {
            if (sharableNetworkInterfaceController != null)
            {
                tempSharableNetworkInterfaceController = sharableNetworkInterfaceController;
            }
        }
        if (tempSharableNetworkInterfaceController == null)
        {
            return null;
        }
        return tempSharableNetworkInterfaceController.getNetworkInterface();
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
            case STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE:
                return "STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE";
            case STATE_PRESENTING_PENDING_TUNE_COMPLETE:
                return "STATE_PRESENTING_PENDING_TUNE_COMPLETE";
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
                case STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE:
                    //stop called while trying to acquire the tuner - ok to release
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                    }
                    currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                    releaseResources = true;
                    break;
                case STATE_PRESENTING_PENDING_TUNE_COMPLETE:
                    //a tuner was acquired - ok to proceed
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
     * Release any resources acquired during presentation process.
     *
     * May be called in STATE_NOT_PRESENTING or STATE_STOP_PENDING_PLAYER_NOT_STARTED
     */
    private void releasePresentingResources()
    {
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
            service = null;
            componentLocators = null;
            //don't call release if stop is due to ResourceClient#release
            if (sharableNetworkInterfaceController != null)
            {
                if (!stopDueToRelease)
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info(id + "releasePresentingResources - releasing SharableNetworkInterfaceController - tuneInstance: " + tuneToken);
                    }
                    try
                    {
                        //no need to hold a reference to the NICallback
                        // or remove it because release does the work to ensure it will be released when necessary

                        //release while holding the lock, so the state can be updated to NOT_PRESENTING after release has 
                        //made the NI available
                        sharableNetworkInterfaceController.release();
                    }
                    catch (NetworkInterfaceException e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "unable to release NI" , e);
                        }
                    }
                }
                else
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info(id + "releasePresentingResources - due to release - not calling release on the SharableNetworkInterfaceController");
                    }
                }
            }
            else
            {
                if (log.isInfoEnabled()) 
                {
                    log.info(id + "releasePresentingResources - no SharableNetworkInterfaceController");
                }
            }
            sharableNetworkInterfaceController = null;
            tuneToken = null;
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
            serviceMediaHandler = null;
            // resetting to blocked
            presentationBlocked = true;
            // resetting condition
            if (log.isDebugEnabled())
            {
                log.debug(id + "updating resourcesReleasedCondition to true");
            }
            resourcesReleasedCondition.setTrue();
            stopDueToRelease = false;
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

    /**
     * Start player and applications.  May be called from STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE if NI is tuned with the tune token
     * or from STATE_PRESENTING_PENDING_TUNE_COMPLETE
     * @param networkInterface the NI which must be tuned
     * @param tempTuneToken the tune token which must be associated with the tuned NI
     * @param presentSequence the sequence at the time startPlayerAndApps was called - sequence may have changed
     */
    public void startPlayerAndApps(ExtendedNetworkInterface networkInterface, Object tempTuneToken, long presentSequence)
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
                log.debug(id + "startPlayerAndApps - current state: " + stateToString(currentState) + ", networkInterface: " + networkInterface + ", tuneInstance parameter: " + tempTuneToken + ", current tune instance: " + tuneToken);
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
                    //unexpected
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "ignoring unexpected startPlayerAndApps call in state: " + stateToString(currentState));
                    }
                    break;
                case STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE:
                    //ok - already tuned  - no tunecomplete notification if isTuned(token) returns true
                    startPlayerAndApps = true;
                    break;
                case STATE_PRESENTING_PENDING_TUNE_COMPLETE:
                    startPlayerAndApps = true;
                    //ok (still verify isTuned(token)
                    break;
                case STATE_PRESENTING_PENDING_PLAYER_STARTING:
                    if (log.isInfoEnabled()) 
                    {
                        log.info(id + "ignoring startPlayerAndApps call in state: " + stateToString(currentState));
                    }
                    break;
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
                //ok to continue - will change state to player starting while holding the lock
                try
                {
                    //it is possible to get a tuneComplete/success notification -after- NI.isTuned returned false - check the NI 
                    //from the shared NI controller against the NI parameter to confirm (NI from shared NI controller will be null if NI is no longer available)
                    NetworkInterface currentNetworkInterface = (sharableNetworkInterfaceController != null ? sharableNetworkInterfaceController.getNetworkInterface() : null);
                    boolean allowStart = (currentNetworkInterface != null && currentNetworkInterface.equals(networkInterface)) && (tempTuneToken != null && 
                            tempTuneToken.equals(tuneToken)) && networkInterface.isTuned(tempTuneToken);
                    if (!allowStart)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "startPlayerAndApps called but networkInterface is not tuned or NI or tuneInstance are null - networkInterface: " + 
                                    networkInterface + ", tuneInstance: " + tuneToken);
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                        }
                        currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                        callStopPresenting = true;
                    }
                }
                catch (NetworkInterfaceException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "startPlayerAndApps called but networkInterface is not tuned");
                    }
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                    }
                    currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                    callStopPresenting = true;
                }

                if (!callStopPresenting)
                {
                    tempPresentationModeListener = presentationModeListener;
                    tempPersistentVideoModeSettings = persistentVideoModeSettings;
                    tempPresentationBlocked = presentationBlocked;
                    tempAppDomain = appDomain;
                    tempControllerListener = controllerListener;
        
                    try
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "creating serviceMediaHandler service: " + service);
                        }

                        tempServiceDetails = ((ServiceExt) service).getDetails();
                        // we can only get here if we registered a listener on the NI
                        serviceMediaHandler = creationDelegate.createServiceMediaHandler(tempServiceDetails, componentLocators,
                                serviceContextResourceUsage, networkInterface, new MediaTimeBase());
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_PRESENTING_PENDING_PLAYER_STARTING");
                        }
                        currentState = STATE_PRESENTING_PENDING_PLAYER_STARTING;
                        tempServiceMediaHandler = serviceMediaHandler;
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
        return "BroadcastServiceContextDelegate: " + id + "service: " + service + ", player: " + serviceMediaHandler + ", state: " 
                + stateToString(currentState);
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
            boolean callStopPresenting = false;
            int stopPresentingReason = 0;
            Class stopPresentingClass = null;
            
            synchronized (lock)
            {
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

                tempServiceMediaHandler = serviceMediaHandler;

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
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                            }
                            currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                            callStopPresenting = true;
                            stopPresentingClass = PresentationTerminatedEvent.class;
                            stopPresentingReason = PresentationTerminatedEvent.RESOURCES_REMOVED;
                        }
                        else
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                            }
                            currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                            callStopPresenting = true;
                            stopPresentingClass = PresentationTerminatedEvent.class;
                            stopPresentingReason = PresentationTerminatedEvent.OTHER;
                        }
                    }
                    else
                    {
                        if (event instanceof ResourceUnavailableEvent)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                            }
                            currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                            callStopPresenting = true;
                            stopPresentingClass = SelectionFailedEvent.class;
                            stopPresentingReason = SelectionFailedEvent.INSUFFICIENT_RESOURCES;
                        }
                        else
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                            }
                            currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                            callStopPresenting = true;
                            stopPresentingClass = SelectionFailedEvent.class;
                            stopPresentingReason = SelectionFailedEvent.OTHER;
                        }
                    }
                }
                else
                {
                    //enable/disable app based on normal/altmediapresentation event
                    //call stopPresenting if we are waiting for the player stop
                    switch (currentState)
                    {
                        case STATE_PRESENTING:
                            // handle presentation event (block/unblock apps based on
                            // normal/alternative media presentation event
                            callEnforceBlocking = true;
                            break;
                        case STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE:
                        case STATE_PRESENTING_PENDING_TUNE_COMPLETE:
                            if (log.isWarnEnabled())
                            {
                                log.warn(id + "ignoring unexpected controllerUpdate in state: " + stateToString(currentState));
                            }
                            break;
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
                                callStopPresenting = true;
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
            
            if (callStopPresenting)
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
            boolean callStopPresenting = false;
            int stopPresentingReason = 0;
            Class stopPresentingClass = null;
            
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
                        break;
                    case STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE:
                    case STATE_PRESENTING_PENDING_TUNE_COMPLETE:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected normalContent notification in state: " + stateToString(currentState));
                        }
                        break;
                    case STATE_PRESENTING_PENDING_PLAYER_STARTING:
                        //initial notification
                        notifyNormalContent = true;
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
                        callStopPresenting = true;
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
                if (!callStopPresenting)
                {
                    blockApplicationPresentation(false);
                }

                //post the event
                notifyPresentingNormalContent();
            }

            if (callStopPresenting)
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
            boolean callStopPresenting = false;
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
                    case STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE:
                    case STATE_PRESENTING_PENDING_TUNE_COMPLETE:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected alternativeContent notification in state: " + stateToString(currentState));
                        }
                        break;
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
                        callStopPresenting = true;
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

            if (callStopPresenting)
            {
                stopPresentingWithReason(stopPresentingClass, stopPresentingReason, presentSequence);
            }
        }
    }

    class NetworkInterfaceResourceClient implements ResourceClient
    {
        private final long presentSequence;

        public NetworkInterfaceResourceClient(long presentSequence)
        {
            this.presentSequence = presentSequence;
        }

        public void notifyRelease(ResourceProxy proxy)
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "notifyRelease: " + proxy);
            }
        }

        public void release(ResourceProxy proxy)
        {
            boolean callStopPresenting = false;
            int stopPresentingReason = 0;
            Class stopPresentingClass = null;
            //the NI has been released - unset the tuneToken so release won't be called on the SharableNetworkInterfaceController
            synchronized (lock)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "release: " + proxy + ", current state: " + stateToString(currentState));
                }

                if (initialSequence != presentSequence)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "sequence changed - ignoring release - sequence expected: " + presentSequence + " - is now: " + initialSequence);
                    }
                    return;
                }
                stopDueToRelease = true;

                switch (currentState)
                {
                    //let the stopPresenting code stop the player and change the state
                    case STATE_PRESENTING:
                        tuneToken = null;
                        callStopPresenting = true;
                        stopPresentingClass = PresentationTerminatedEvent.class;
                        stopPresentingReason = PresentationTerminatedEvent.RESOURCES_REMOVED;
                        break;
                    case STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE:
                        //stopPresenting has not yet been called, and tuneComplete has not yet been called
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                        }
                        currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                        tuneToken = null;
                        callStopPresenting = true;
                        stopPresentingClass = SelectionFailedEvent.class;
                        stopPresentingReason = SelectionFailedEvent.INSUFFICIENT_RESOURCES;
                        break;
                    case STATE_PRESENTING_PENDING_TUNE_COMPLETE:
                        //tunecomplete has been received but the player has not yet been started - change the state and call stopPresenting
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                        }
                        currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                        tuneToken = null;
                        callStopPresenting = true;
                        stopPresentingClass = SelectionFailedEvent.class;
                        stopPresentingReason = SelectionFailedEvent.INSUFFICIENT_RESOURCES;
                        break;
                    case STATE_PRESENTING_PENDING_PLAYER_STARTING:
                        //player is starting - call stop presenting without changing state
                        tuneToken = null;
                        callStopPresenting = true;
                        stopPresentingClass = SelectionFailedEvent.class;
                        stopPresentingReason = SelectionFailedEvent.INSUFFICIENT_RESOURCES;
                        break;
                    case STATE_NOT_PRESENTING:
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "ignoring release in unexpected state: " + stateToString(currentState));
                        }
                        break;
                    case STATE_STOP_PENDING_WAIT_PLAYER_START:
                    case STATE_STOP_PENDING_WAIT_PLAYER_STOP:
                    case STATE_STOP_PENDING_PLAYER_NOT_STARTED:
                        //player is stopping - call stop presenting without changing state
                        tuneToken = null;
                        callStopPresenting = true;
                        //use cached class and reset reason based on class - may be stopping before starting
                        stopPresentingClass = cachedStopPresentingEventClass;
                        if (SelectionFailedEvent.class.equals(cachedStopPresentingEventClass))
                        {
                            stopPresentingReason = SelectionFailedEvent.INSUFFICIENT_RESOURCES;
                        }
                        else
                        {
                            stopPresentingReason = PresentationTerminatedEvent.RESOURCES_REMOVED;
                        }
                        break;
                    default:
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "ignoring release in state: " + stateToString(currentState));
                        }
                }
            }

            //stop and wait for condition before returning
            if (callStopPresenting)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "release - calling stopPresentingWithReason");
                }
                stopPresentingWithReason(stopPresentingClass, stopPresentingReason, presentSequence);
                try
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "release - waiting for resourcesReleasedCondition");
                    }
                    resourcesReleasedCondition.waitUntilTrue();
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "release - resourcesReleasedCondition true - returning");
                    }
                }
                catch (InterruptedException e)
                {
                    //ignore
                }
            }
        }

        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            synchronized (lock)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "requestRelease - returning false: " + proxy + ", current state: " + stateToString(currentState));
                }
                return false;
            }
        }
    }

    class NetworkInterfaceCallbackImpl implements NetworkInterfaceCallback
    {
        private final long presentSequence;

        public NetworkInterfaceCallbackImpl(long presentSequence)
        {
            this.presentSequence = presentSequence;
        }

        public void notifyTunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            //ignore
        }

        public void notifyTuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, final boolean success, boolean isSynced)
        {
            boolean callStartPlayerAndApps = false;
            boolean callStopPresenting = false;
            int stopPresentingReason = 0;
            Class stopPresentingClass = null;
            ExtendedNetworkInterface tempNetworkInterface = null;
            Object tempTuneToken = null;

            synchronized(lock)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "notifyTuneComplete - success: " + success + ", isSynced: " + isSynced + ", NI: " + ni + ", tuneInstance parameter: " + tuneInstance + ", current state: " + stateToString(currentState) + ", current tuneInstance: " + tuneToken);
                }

                if (initialSequence != presentSequence)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "sequence changed - ignoring notifyTuneComplete - sequence expected: " + presentSequence + " - is now: " + initialSequence);
                    }
                    return;
                }

                //this shouldn't happen since sequence check above should guard against handling stale notifications
                if (tuneInstance != tuneToken)
                {
                    if (log.isWarnEnabled()) 
                    {
                        log.warn("ignoring notifyTuneComplete for non-current tuneInstance - tuneInstance parameter: " + tuneInstance + ", current tuneInstance: " + tuneToken);
                    }
                    return;
                }

                switch (currentState)
                {
                    case STATE_PRESENTING:
                        //already presenting - should not receive this
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected notifyTuneComplete in state: " + stateToString(currentState));
                        }
                        break;
                    case STATE_PRESENTING_PENDING_WAIT_TUNE_COMPLETE:
                        //success: change state to tune complete and start player
                        //failure: change state to player not started and call stoppresenting
                        if (success)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_PRESENTING_PENDING_TUNE_COMPLETE");
                            }
                            currentState = STATE_PRESENTING_PENDING_TUNE_COMPLETE;
                            tempNetworkInterface = sharableNetworkInterfaceController.getNetworkInterface();
                            tempTuneToken = tuneToken;
                            callStartPlayerAndApps = true;
                        }
                        else
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "setting currentState to STATE_STOP_PENDING_PLAYER_NOT_STARTED");
                            }
                            currentState = STATE_STOP_PENDING_PLAYER_NOT_STARTED;
                            callStopPresenting = true;
                            stopPresentingClass = SelectionFailedEvent.class;
                            stopPresentingReason = SelectionFailedEvent.INSUFFICIENT_RESOURCES;
                        }
                        break;
                    case STATE_PRESENTING_PENDING_TUNE_COMPLETE:
                        //already received tuneComplete - should not receive this
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected notifyTuneComplete in state: " + stateToString(currentState));
                        }
                        break;
                    case STATE_PRESENTING_PENDING_PLAYER_STARTING:
                    case STATE_NOT_PRESENTING:
                    case STATE_STOP_PENDING_WAIT_PLAYER_START:
                    case STATE_STOP_PENDING_WAIT_PLAYER_STOP:
                    case STATE_STOP_PENDING_PLAYER_NOT_STARTED:
                        if (log.isWarnEnabled())
                        {
                            log.warn(id + "ignoring unexpected notifyTuneComplete in state: " + stateToString(currentState));
                        }
                        break;
                    default:
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "ignoring notifyTuneComplete in state: " + stateToString(currentState));
                        }
                }
            }
            
            if (callStartPlayerAndApps)
            {
                final ExtendedNetworkInterface tempFinalNetworkInterface = tempNetworkInterface;
                final Object tempFinalTuneToken = tempTuneToken;
                CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                callerContextManager.getSystemContext().runInContextAsync(
                    new Runnable()
                    {
                        public void run()
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "notifyTuneComplete - starting player and apps");
                            }
                            startPlayerAndApps(tempFinalNetworkInterface, tempFinalTuneToken, presentSequence);
                        }
                    });
            }
            
            if (callStopPresenting)
            {
                final Class tempStopPresentingClass = stopPresentingClass;
                final int tempStopPresentingReason = stopPresentingReason;
                
                CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                callerContextManager.getSystemContext().runInContextAsync(
                    new Runnable()
                    {
                        public void run()
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(id + "notifyTuneComplete - stopping presentation");
                            }
                            stopPresentingWithReason(tempStopPresentingClass, tempStopPresentingReason, presentSequence);
                        }
                    });
            }    
        }

        public void notifyRetunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            //ignore
        }

        public void notifyRetuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success, boolean isSynced)
        {
            //ignore
        }

        public void notifyUntuned(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            //ignore
        }

        public void notifySyncAcquired(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            //ignore
        }

        public void notifySyncLost(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            //ignore
        }
    }
}
