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
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.ResourceUnavailableEvent;
import javax.media.StartEvent;
import javax.media.StopByRequestEvent;
import javax.media.Time;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.RecordingExt;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.media.player.PresentationModeControl;
import org.cablelabs.impl.media.player.PresentationModeListener;
import org.cablelabs.impl.media.player.ServicePlayer;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceContextResourceUsageImpl;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.SystemEventUtil;
import org.davic.net.tuning.NetworkInterface;
import org.ocap.dvr.TimeShiftListener;
import org.ocap.dvr.TimeShiftProperties;
import org.ocap.service.AlternativeContentErrorEvent;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingTerminatedEvent;

/**
 * A <code>ServiceContextDelegate</code> responsible for presenting an Recorded
 * services.
 */
public class RecordedServiceContextDelegate implements DVRServiceContextDelegate, ServiceContextDelegate,
        TimeShiftProperties
{
    private static final String PRESENTATION_MODE_CONTROL_CLASS_NAME = "org.cablelabs.impl.media.player.PresentationModeControl";

    private static final Logger log = Logger.getLogger(RecordedServiceContextDelegate.class);

    private static final NetworkInterface SPECIAL_NETWORK_INTERFACE = null;

    private static final int STATE_NOT_PRESENTING = 1;

    //presentation (and player) are starting
    private static final int STATE_PRESENTING_PENDING = 2;

    private static final int STATE_PRESENTING = 3;

    private static final int STATE_STOP_PENDING = 4;

    //must wait for player start to proceed with stopping
    private static final int STATE_STOP_PENDING_WAIT_PLAYER_START = 5;

    private ServiceMediaHandlerCreationDelegate creationDelegate;

    private ServiceContextDelegateListener serviceContextDelegateListener;

    private DVRServiceContextDelegateListener dvrServiceContextDelegateListener;

    private ServiceMediaHandler serviceMediaHandler;

    private PresentationModeListenerImpl presentationModeListener;

    private ControllerListener controllerListener;

    private static RecordingManager recordingManager;

    private AppDomain appDomain;

    private CallerContext ownerCallerContext;

    private Service service;
    private RecordingRequest recordingRequest;

    private int currentState = STATE_NOT_PRESENTING;

    // never null this out - even on destroy
    private Object lock;

    private final RecordingChangedListener recordingChangedListener = new RecordingChangedListenerImpl();

    private volatile boolean initialized;

    //recording playback listeners must be notified only once when either altcontent or normalcontent are posted
    private boolean recordingServiceContextNotified;

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
    private volatile long initialSequence;


    public synchronized void initialize(ServiceContextDelegateListener serviceContextDelegateListener,
            ServiceMediaHandlerCreationDelegate creationDelegate, AppDomain appDomain,
            PersistentVideoModeSettings videoModeSettings, Object lock)
    {
        synchronized (lock)
        {
            if (recordingManager == null)
            {
                recordingManager = ((org.cablelabs.impl.manager.RecordingManager) (ManagerManager.getInstance(org.cablelabs.impl.manager.RecordingManager.class))).getRecordingManager();
            }
            this.serviceContextDelegateListener = serviceContextDelegateListener;
            this.creationDelegate = creationDelegate;
            this.appDomain = appDomain;
            this.lock = lock;
            presentationModeListener = new PresentationModeListenerImpl();
            controllerListener = new ControllerListenerImpl();

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
                log.info(id + "prepareToPresent - service: " + newService + ", locators: " + Arrays.toString(newComponentLocators));
            }
            this.initialService = newService;
            this.initialComponentLocators = newComponentLocators;
            this.initialServiceContextResourceUsage = serviceContextResourceUsage;
            this.initialCallerContext = callerContext;
            this.initialSequence = sequence;
        }
    }

    public void present(long presentSequence)
    {
        assertInitialized();

        ServiceMediaHandler tempServiceMediaHandler = null;
        boolean servicesEqual;
        boolean callStopPresenting = false;
        int notPresentingSelectionFailedEventReason = -1;
        PresentationModeListener tempPresentationModeListener = null;
        AppDomain tempAppDomain = null;
        ControllerListener tempControllerListener = null;

        synchronized (lock)
        {
            if (presentSequence != initialSequence)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "ignoring present called with non-current sequence - current sequence: " + initialSequence + ", sequence parameter: " + presentSequence);
                }
                return;
            }

            if (log.isInfoEnabled())
            {
                log.info(id + "present - new service: " + initialService + ", locators: " + Arrays.toString(initialComponentLocators));
            }
            if (initialService == null)
            {
                throw new IllegalArgumentException("present called with a null service");
            }
            tempServiceMediaHandler = serviceMediaHandler;
            tempPresentationModeListener = presentationModeListener;
            tempAppDomain = appDomain;
            tempControllerListener = controllerListener;
            servicesEqual = initialService.equals(service);
        }

        if (servicesEqual)
        {
            if (initialComponentLocators == null || initialComponentLocators.length == 0)
            {
                //already presenting service, update mediatime from recorded service
                Time newMediaTime = ((RecordedService)initialService).getMediaTime();
                if (log.isDebugEnabled())
                {
                    log.debug(id + "no new locators - already presenting service - updating mediatime from recorded service to: " + newMediaTime);
                }
                ((ServicePlayer)serviceMediaHandler).setMediaTime(newMediaTime, false);
                // all select calls need a notification - may be altcontent or normalcontent - just respond with presenting
                notifyNoChange();
                return;
            }
            if (log.isDebugEnabled())
            {
                log.debug(id + "already presenting service - updating with new locators");
            }
            if (tempServiceMediaHandler != null)
            {
                try
                {
                    ((ServicePlayer)tempServiceMediaHandler).updateServiceContextSelection(initialComponentLocators);
                }
                catch (InvalidLocatorException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "Unable to select locators: " + Arrays.toString(initialComponentLocators), e);
                    }
                    // unable to present, notify missing handler
                    ((ServicePlayer) tempServiceMediaHandler).switchToAlternativeContent(AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                }
                catch (InvalidServiceComponentException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "Unable to select locators: " + Arrays.toString(initialComponentLocators), e);
                    }
                    // unable to present, notify missing handler
                    ((ServicePlayer) tempServiceMediaHandler).switchToAlternativeContent(AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                }
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "No ServiceMediaHandler - unable to update components");
                }
                callStopPresenting = true;
                notPresentingSelectionFailedEventReason = SelectionFailedEvent.OTHER;
            }
        }
        else
        {
            synchronized (lock)
            {
                try
                {
                    service = initialService;
                    recordingRequest = ((RecordedService)service).getRecordingRequest();
                    ownerCallerContext = initialCallerContext;

                    ServiceDetails serviceDetails = ((ServiceExt) service).getDetails();

                    serviceMediaHandler = creationDelegate.createServiceMediaHandler(serviceDetails,
                            initialComponentLocators, initialServiceContextResourceUsage, getBroadcastNetworkInterface(), null);
                    tempServiceMediaHandler = serviceMediaHandler;
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "setting currentState to PRESENTING_PENDING");
                    }
                    currentState = STATE_PRESENTING_PENDING;
                }
                catch (SIRequestException sire)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "unable to present", sire);
                    }
                    Class alternativeContentClass = AlternativeContentErrorEvent.class;
                    int reason = AlternativeContentErrorEvent.CONTENT_NOT_FOUND;
                    if (sire.getReason() == SIRequestFailureType.INSUFFICIENT_RESOURCES)
                    {
                        reason = SelectionFailedEvent.INSUFFICIENT_RESOURCES;
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to NOT_PRESENTING");
                        }
                        currentState = STATE_NOT_PRESENTING;
                        callStopPresenting = true;
                        notPresentingSelectionFailedEventReason = reason;
                    }
                    else
                    {
                        serviceMediaHandler = creationDelegate.createAlternativeContentMediaHandler(alternativeContentClass, reason,
                                initialServiceContextResourceUsage);
                        tempServiceMediaHandler = serviceMediaHandler;
                        if (log.isInfoEnabled())
                        {
                            log.info(id + "setting currentState to PRESENTING_PENDING");
                        }
                        currentState = STATE_PRESENTING_PENDING;
                    }
                }
                catch (InterruptedException e)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "unable to present", e);
                    }
                    serviceMediaHandler = creationDelegate.createAlternativeContentMediaHandler(
                            AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND, initialServiceContextResourceUsage);
                    tempServiceMediaHandler = serviceMediaHandler;
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "setting currentState to PRESENTING_PENDING");
                    }
                    currentState = STATE_PRESENTING_PENDING;
                }
            }
        }
        if (callStopPresenting)
        {
            stopPresentingWithReason(SelectionFailedEvent.class, notPresentingSelectionFailedEventReason);
        }
        else
        {
            if (tempServiceMediaHandler != null)
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
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + "no presentation mode control available");
                    }
                }
                // we can't currently present applications with recorded
                // services...call stopBoundApps to ensure other
                // applications are not
                // running
                tempAppDomain.stopBoundApps();

                tempServiceMediaHandler.addControllerListener(tempControllerListener);
                recordingManager.addRecordingChangedListener(recordingChangedListener);
                if (log.isInfoEnabled())
                {
                    log.info(id + "starting serviceMediaHandler: " + tempServiceMediaHandler + " - controls: "
                            + Arrays.toString(tempServiceMediaHandler.getControls()));
                }
                tempServiceMediaHandler.start();
            }
        }
    }

    public void stopPresenting(long presentSequence)
    {
        assertInitialized();
        synchronized(lock)
        {
            if (presentSequence != initialSequence)
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
        stopPresentingWithReason(PresentationTerminatedEvent.class, PresentationTerminatedEvent.USER_STOP);
    }

    public void stopPresentingAbstractService()
    {
        assertInitialized();
        // no-op (not an abstract service)
    }

    public synchronized void destroy()
    {
        if (initialized)
        {
            synchronized (lock)
            {
                serviceContextDelegateListener = null;
                creationDelegate = null;
                appDomain = null;
            }
        }
        initialized = false;
    }

    public void setDVRServiceContextDelegateListener(DVRServiceContextDelegateListener dvrServiceContextDelegateListener)
    {
        this.dvrServiceContextDelegateListener = dvrServiceContextDelegateListener;
    }

    public NetworkInterface getNetworkInterface()
    {
        assertInitialized();
        CallerContext tempOwnerCallerContext;
        synchronized (lock)
        {
            if ((currentState != STATE_PRESENTING) || (ownerCallerContext == null))
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + "not presenting or ownerCallerContext is null - returning null");
                }
                return null;
            }
            tempOwnerCallerContext = ownerCallerContext;
        }
        CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        boolean isCallerContextSameAsOwnerCallerContext = tempOwnerCallerContext.equals(callerContextManager.getCurrentContext());

        if (isCallerContextSameAsOwnerCallerContext)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "callerContext is same as owner caller context - returning special network interface");
            }
            // app callerContext with associated timeshift - return 'special' NI
            return SPECIAL_NETWORK_INTERFACE;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "caller context is different from owner caller context - returning broadcast network interface");
            }
            // other callerContext with associated timeshift - return 'real' NI
            return getBroadcastNetworkInterface();
        }
    }

    private ExtendedNetworkInterface getBroadcastNetworkInterface()
    {
        // delegate does not use a NetworkInterface
        return null;
    }

    public ServiceContentHandler[] getServiceContentHandlers()
    {
        assertInitialized();
        synchronized (lock)
        {
            // no apps for RecordedService
            if (serviceMediaHandler != null)
            {
                return new ServiceContentHandler[] { serviceMediaHandler };
            }

            return new ServiceContentHandler[] {};
        }
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

    public boolean canPresent(Service service)
    {
        boolean result = (service instanceof RecordedService);
        if (log.isDebugEnabled())
        {
            log.debug(id + "canPresent: " + service + ": " + result);
        }

        return result;
    }

    public TimeShiftWindowClient getTimeShiftWindowClient()
    {
        return null;
    }

    public void requestBuffering()
    {
        // no-op
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
        //reselect of currently presenting service - no need to notify recording presenting (did not leave the PRESENTING state)
    }

    private void notifyNormalContent()
    {
        assertInitialized();
        ServiceContextDelegateListener tempServiceContextDelegateListener;
        synchronized (lock)
        {
            tempServiceContextDelegateListener = serviceContextDelegateListener;
        }
        tempServiceContextDelegateListener.presentingNormalContent();
        notifyRecordingPresenting();
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
        notifyRecordingPresenting();
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
     * Notify the recording playback listener the first time either normalcontent or alternative content events are posted  
     */
    private void notifyRecordingPresenting() 
    {
        DVRServiceContextDelegateListener tempDVRServiceContextDelegateListener;
        ServiceMediaHandler tempServiceMediaHandler;
        Service tempService;
        RecordingExt tempRecording;
        synchronized (lock)
        {
            if (recordingServiceContextNotified)
            {
                //already notified
                return;
            }
            recordingServiceContextNotified = true;
            tempDVRServiceContextDelegateListener = dvrServiceContextDelegateListener;
            tempServiceMediaHandler = serviceMediaHandler;
            tempService = service;
            tempRecording = (RecordingExt) ((RecordedService) tempService).getRecordingRequest();
        }
        if (tempRecording != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "notifying recordedService is presenting");
            }
            tempDVRServiceContextDelegateListener.notifyRecordingServiceContextPresenting(tempRecording, tempServiceMediaHandler);
        }
        else
        {
            if (log.isErrorEnabled()) 
            {
                log.error(id + "attempted to notify recording presenting but recording not found for service: " + tempService);
            }
        }
    }
    
    private void releasePresentingResources()
    {
        synchronized (lock)
        {
            service = null;
            recordingRequest = null;
            ownerCallerContext = null;
        }
        recordingManager.removeRecordingChangedListener(recordingChangedListener);
    }

    private void stopPresentingWithReason(Class eventClass, int reason)
    {
        assertInitialized();
        ServiceContextDelegateListener tempServiceContextDelegateListener;
        ServiceMediaHandler tempServiceMediaHandler;
        PresentationModeListener tempPresentationModeListener;
        ControllerListener tempControllerListener;
        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "stopPresentingWithReason - type: " + eventClass.getName() + ", reason: " + reason);
            }
            //lock will be released in STOP_PENDING in order to release resources, and then state will transition to NOT_PRESENTING
            if (STATE_STOP_PENDING == currentState)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "stopPresentingWithReason called in STOP_PENDING - ignoring");
                }
            }
            if (STATE_STOP_PENDING_WAIT_PLAYER_START == currentState)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "stopPresentingWithReason called in STOP_PENDING_WAIT_PLAYER_START - not stopping until event received");
                }
                cachedStopPresentingEventClass = eventClass;
                cachedStopPresentingEventReason = reason;
                return;
            }
            tempServiceContextDelegateListener = serviceContextDelegateListener;
            tempServiceMediaHandler = serviceMediaHandler;
            tempPresentationModeListener = presentationModeListener;
            tempControllerListener = controllerListener;
            if (serviceMediaHandler != null)
            {
                int playerState = serviceMediaHandler.getState();
                if (serviceMediaHandler.getTargetState() == Controller.Started && playerState != Controller.Started)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + serviceMediaHandler + " target state is Started (" + Controller.Started + ") and current state is: " + playerState);
                    }
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "setting currentState to STOP_PENDING_WAIT_PLAYER_START");
                    }
                    currentState = STATE_STOP_PENDING_WAIT_PLAYER_START;
                    cachedStopPresentingEventClass = eventClass;
                    cachedStopPresentingEventReason = reason;
                    //when presentation mode event is received, come back and stop the player
                    return;
                }
            }
            //stopping player now, nulling ref 
            serviceMediaHandler = null;
            recordingServiceContextNotified = false;            
            if (log.isInfoEnabled())
            {
                log.info(id + "setting currentState to STOP_PENDING");
            }
            currentState = STATE_STOP_PENDING;
        }
        if (tempServiceMediaHandler != null)
        {
            try
            {
                PresentationModeControl presentationModeControl = (PresentationModeControl) tempServiceMediaHandler.getControl(PRESENTATION_MODE_CONTROL_CLASS_NAME);
                if (presentationModeControl != null)
                {
                    presentationModeControl.removePresentationModeListener(tempPresentationModeListener);
                }
                tempServiceMediaHandler.removeControllerListener(tempControllerListener);

                // Notify 'playerStopping' and set the serviceMediaHandler to
                // null before calling
                // notifyNotPresenting() to avoid duplicate notifications
                tempServiceContextDelegateListener.playerStopping(tempServiceMediaHandler);
                tempServiceMediaHandler.stop();
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

        releasePresentingResources();
        if (log.isInfoEnabled())
        {
            log.info(id + "setting currentState to NOT_PRESENTING");
        }
        synchronized (lock)
        {
            currentState = STATE_NOT_PRESENTING;
        }

        // Notify listeners that the presentation has been terminated (after
        // releasePresentingResources)
        notifyNotPresenting(eventClass, reason);
    }

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
            if (log.isDebugEnabled())
            {
                log.debug(id + msgPrefix + ": RecordedServiceContextDelegate not initialized");
            }
        }
        return initialized;
    }

    public void addTimeShiftListener(TimeShiftListener listener)
    {
        // no-op
    }

    public void removeTimeShiftListener(TimeShiftListener listener)
    {
        // no-op
    }

    //NOTE: not used (ServiceContext does not call the delegate to get minDuration)
    public long getMinimumDuration()
    {
        // no-op
        return 0;
    }

    public void setMinimumDuration(long minDuration)
    {
        // no-op
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
        if (log.isDebugEnabled())
        {
            log.debug(id + "updatePresentation - service: " + dvrPresentation);
        }

        long recordingStart;
        long recordingEnd;
        if (service != null)
        {
            RecordedService recordedService = (RecordedService) service;
            recordingStart = recordedService.getFirstMediaTime().getNanoseconds();
            recordingEnd = recordingStart + (recordedService.getRecordedDuration() * 1000000); // recorded
                                                                                               // duration
                                                                                               // is
                                                                                               // millis..
                                                                                               // multiply
                                                                                               // millis
                                                                                               // by
                                                                                               // 1000000
                                                                                               // to
                                                                                               // get
                                                                                               // nanos
                                                                                               // (NANOS
                                                                                               // PER
                                                                                               // MILLI)

            // Adjust mediaTime if outside the bounds of the recording
            long mediaTime = dvrPresentation.getMediaTime().getNanoseconds();

            if (mediaTime <= 0)
            {
                // zero or negative mediatime, present at the 'live point' (or
                // end of content if not an ongoing recorded service)
                dvrPresentation.setPresentation(new Time(Long.MAX_VALUE), dvrPresentation.getRate(),
                        dvrPresentation.getAction(), dvrPresentation.getPersistent());
            }
            else if (mediaTime > recordingEnd || mediaTime < recordingStart)
            {
                String actionMsg = (dvrPresentation.getAction() ? " setting media time to zero"
                        : "setting mediatime to live point");
                if (log.isDebugEnabled())
                {
                    log.debug(id + "mediaTime is out of recording bounds - mediaTime: " + mediaTime + ", recording start: "
                            + recordingStart + ", recording end: " + recordingEnd + actionMsg);
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

                if (log.isDebugEnabled())
                {
                    log.debug(id + "adjusted mediaTime: " + mediaTime + "ns");
                }
                dvrPresentation.setPresentation(new Time(mediaTime), dvrPresentation.getRate(),
                        dvrPresentation.getAction(), dvrPresentation.getPersistent());
            }
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn(id + "updatePresentation called but recorded service is null - unable to update: "
                        + dvrPresentation);
            }
        }
    }

    //NOTE: not used (ServiceContext does not call the delegate to get maxDuration)
    public long getMaximumDuration()
    {
        // no-op
        return 0;
    }

    public void setMaximumDuration(long maxDuration)
    {
        // no-op
    }

    //NOTE: not used (ServiceContext does not call the delegate to get last service buffered preference)
    public boolean getLastServiceBufferedPreference()
    {
        // no-op
        return false;
    }

    public void setLastServiceBufferedPreference(boolean buffer)
    {
        // no-op
    }

    //NOTE: not used (ServiceContext does not call the delegate to get the save preference)
    public boolean getSavePreference()
    {
        // no-op
        return false;
    }

    public void setSavePreference(boolean save)
    {
        // no-op
    }

    public NetworkInterface getNetworkInterface(boolean presentation)
    {
        assertInitialized();
        synchronized (lock)
        {
            if (currentState != STATE_PRESENTING)
            {
                return null;
            }
            if (presentation)
            {
                // presentation = true, return 'special' NI
                return SPECIAL_NETWORK_INTERFACE;
            }
            return getBroadcastNetworkInterface();
        }
    }

    public String toString()
    {
        return "RecordedServiceContextDelegate: " + id + "service: " + service + ", player: " + serviceMediaHandler + ", state: " + currentState;
    }

    class RecordingChangedListenerImpl implements RecordingChangedListener
    {
        /**
         * The recording changed listener is used to process events related to
         * the status of ongoing recordings. We monitor these events so that we
         * can detect changes to the status of the currently presenting recorded
         * service.
         */
        public void recordingChanged(RecordingChangedEvent e)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "received recording event " + e);
            }

            if (!checkInitialized("RecordingChangedEvent fired"))
                return;

            int tempCurrentState;
            RecordingRequest tempCurrentRecordingRequest;
            DVRServiceContextDelegateListener tempDVRServiceContextDelegateListener;
            synchronized (lock)
            {
                tempCurrentState = currentState;
                tempCurrentRecordingRequest = recordingRequest;
                tempDVRServiceContextDelegateListener = dvrServiceContextDelegateListener;
            }
            // Handle events while in the presenting state
            if (tempCurrentState == STATE_PRESENTING)
            {
                // Only interested in leaf recordings
                RecordingRequest recRequest = e.getRecordingRequest();
                if (recRequest instanceof LeafRecordingRequest)
                {
                    // FIXME(Todd): Will the following test match previous
                    // recordings of the same
                    // service.

                    // Only interested in the currently presenting service
                    if (tempCurrentRecordingRequest.getId() == recRequest.getId())
                    {
                        int state = e.getState();
                        // Deal with completed recordings
                        if (state == LeafRecordingRequest.COMPLETED_STATE)
                        {
                            tempDVRServiceContextDelegateListener.recordingStopped(RecordingTerminatedEvent.SCHEDULED_STOP);
                        }
                        if (state == LeafRecordingRequest.DELETED_STATE)
                        {
                            tempDVRServiceContextDelegateListener.recordingStopped(RecordingTerminatedEvent.SERVICE_VANISHED);
                            stopPresentingWithReason(PresentationTerminatedEvent.class, PresentationTerminatedEvent.SERVICE_VANISHED);
                        }

                        // Deal with failed or incomplete recordings
                        if (state == LeafRecordingRequest.FAILED_STATE
                                || state == LeafRecordingRequest.INCOMPLETE_STATE
                                || state == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE)
                        {
                            int reason;
                            //ok to call requested.getFailedException in failed, incomplete, in-prog with error states
                            int failedReason = ((RecordingFailedException) ((LeafRecordingRequest)recRequest).getFailedException()).getReason();
                            switch (failedReason)
                            {
                                case RecordingFailedException.ACCESS_WITHDRAWN:
                                case RecordingFailedException.CA_REFUSAL:
                                    reason = RecordingTerminatedEvent.ACCESS_WITHDRAWN;
                                    break;
                                case RecordingFailedException.RESOURCES_REMOVED:
                                case RecordingFailedException.INSUFFICIENT_RESOURCES:
                                case RecordingFailedException.OUT_OF_BANDWIDTH:
                                case RecordingFailedException.SPACE_FULL:
                                case RecordingFailedException.TUNED_AWAY:
                                case RecordingFailedException.TUNING_FAILURE:
                                    reason = RecordingTerminatedEvent.RESOURCES_REMOVED;
                                    break;
                                case RecordingFailedException.SERVICE_VANISHED:
                                case RecordingFailedException.CONTENT_NOT_FOUND:
                                case RecordingFailedException.RESOLUTION_ERROR:
                                    reason = RecordingTerminatedEvent.SERVICE_VANISHED;
                                    break;
                                case RecordingFailedException.USER_STOP:
                                    reason = RecordingTerminatedEvent.USER_STOP;
                                    break;
                                default:
                                    SystemEventUtil.logRecoverableError(new Exception(
                                            "Unknown recording failure reason " + failedReason));
                                    reason = RecordingTerminatedEvent.RESOURCES_REMOVED;
                                    break;
                            }
                            tempDVRServiceContextDelegateListener.recordingStopped(reason);
                        }
                    }
                }
            }
        }
    }

    private class ControllerListenerImpl implements ControllerListener
    {
        public void controllerUpdate(ControllerEvent event)
        {
            if (!checkInitialized("Controller Event fired")) return;

            ServiceMediaHandler tempServiceMediaHandler;
            int tempCurrentState;
            synchronized (lock)
            {
                tempServiceMediaHandler = serviceMediaHandler;
                tempCurrentState = currentState;
            }

            if (event.getSourceController() != tempServiceMediaHandler)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + "controllerUpdate - received controller event for non-current controller.  Ignoring: " + event + ", current handler: " + tempServiceMediaHandler);
                }
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug(id + "controllerUpdate - event: " + event + ", presenting: "
                        + (STATE_PRESENTING == tempCurrentState));
            }
            if (tempCurrentState == STATE_PRESENTING_PENDING || tempCurrentState == STATE_STOP_PENDING || tempCurrentState == STATE_STOP_PENDING_WAIT_PLAYER_START)
            {
                handleControllerEventWhenPending(event);
            }
            else
            {
                if (tempCurrentState == STATE_PRESENTING)
                {
                    handleControllerEventWhenPresenting(event);
                }
                else
                {
                        // we don't process a controllerEvent if we aren't
                        // pending or presenting..log it
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + "controllerUpdate - received controllerEvent but we're not presenting or pending: "
                                + event);
                    }
                }
            }
        }

        private void handleControllerEventWhenPending(ControllerEvent event)
        {
            ServiceMediaHandler tempServiceMediaHandler;
            ServiceContextDelegateListener tempServiceContextDelegateListener;
            synchronized (lock)
            {
                tempServiceMediaHandler = serviceMediaHandler;
                tempServiceContextDelegateListener = serviceContextDelegateListener;
            }
            if (event instanceof StartEvent)
            {
                if (tempServiceMediaHandler != null)
                {
                    tempServiceContextDelegateListener.playerStarted(tempServiceMediaHandler);
                }
                return;
            }
            if (event instanceof StopByRequestEvent)
            {
                if (tempServiceMediaHandler != null)
                {
                    tempServiceContextDelegateListener.playerStopping(tempServiceMediaHandler);
                }
                return;
            }
            if (event instanceof ResourceUnavailableEvent)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "controllerEvent - received resourceUnavailableEvent when pending: " + event);
                }
                stopPresentingWithReason(SelectionFailedEvent.class, SelectionFailedEvent.INSUFFICIENT_RESOURCES);
            }
            else if (event instanceof ControllerErrorEvent)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "controllerEvent - received ControllerErrorEvent when pending: " + event);
                }
                stopPresentingWithReason(SelectionFailedEvent.class, SelectionFailedEvent.OTHER);
            }
            // do not stop presentation due to ControllerClosedEvent or StopEvents
        }

        private void handleControllerEventWhenPresenting(ControllerEvent event)
        {
            ServiceMediaHandler tempServiceMediaHandler;
            ServiceContextDelegateListener tempServiceContextDelegateListener;
            synchronized (lock)
            {
                tempServiceMediaHandler = serviceMediaHandler;
                tempServiceContextDelegateListener = serviceContextDelegateListener;
            }
            if (event instanceof StartEvent)
            {
                if (tempServiceMediaHandler != null)
                {
                    tempServiceContextDelegateListener.playerStarted(tempServiceMediaHandler);
                }
                return;
            }
            if (event instanceof StopByRequestEvent)
            {
                if (tempServiceMediaHandler != null)
                {
                    tempServiceContextDelegateListener.playerStopping(tempServiceMediaHandler);
                }
                return;
            }
            if (event instanceof ResourceUnavailableEvent)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "controllerEvent - received resourceUnavailableEvent when presenting: " + event);
                }
                stopPresentingWithReason(PresentationTerminatedEvent.class,
                        PresentationTerminatedEvent.RESOURCES_REMOVED);
            }
            else if (event instanceof ControllerErrorEvent)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "controllerEvent - received ControllerErrorEvent when presenting: " + event);
                }
                stopPresentingWithReason(PresentationTerminatedEvent.class, PresentationTerminatedEvent.OTHER);
            }
            // do not stop presentation due to ControllerClosedEvent or StopEvents
        }
    }

    private class PresentationModeListenerImpl implements PresentationModeListener
    {
        public void normalContent()
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "presentationModeListener received normalContent notification");
            }
            if (!checkInitialized("Normal content notification fired in PresentationModeListenerImpl"))
                return;
            synchronized (lock)
            {
                int oldState = currentState;
                if (log.isInfoEnabled())
                {
                    log.info(id + "setting currentState to PRESENTING");
                }
                currentState = STATE_PRESENTING;
                if (oldState == STATE_STOP_PENDING_WAIT_PLAYER_START)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "presentationModeListener - received normalContent notification in STOP_PENDING_WAIT_PLAYER_START - stopping presentation");
                    }
                    stopPresentingWithReason(cachedStopPresentingEventClass, cachedStopPresentingEventReason);
                    return;
                }
            }

            notifyNormalContent();
        }

        public void alternativeContent(Class alternativeContentClass, int alternativeContentReasonCode)
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "presentationModeListener received alternativeContent notification - reason: " + alternativeContentReasonCode);
            }
            if (!checkInitialized("Alternative content notification fired in PresentationModeListenerImpl"))
                return;
            synchronized (lock)
            {
                int oldState = currentState;
                if (log.isInfoEnabled())
                {
                    log.info(id + "setting currentState to PRESENTING");
                }
                currentState = STATE_PRESENTING;
                if (oldState == STATE_STOP_PENDING_WAIT_PLAYER_START)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "presentationModeListener - received alternativeContent notification in STOP_PENDING_WAIT_PLAYER_START - stopping presentation");
                    }
                    stopPresentingWithReason(cachedStopPresentingEventClass, cachedStopPresentingEventReason);
                    return;
                }
            }
            notifyPresentingAlternativeContent(alternativeContentClass, alternativeContentReasonCode);
        }
    }
}
