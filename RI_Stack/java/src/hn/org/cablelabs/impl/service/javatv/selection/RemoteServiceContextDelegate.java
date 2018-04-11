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
import javax.tv.locator.Locator;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.player.PresentationModeControl;
import org.cablelabs.impl.media.player.PresentationModeListener;
import org.cablelabs.impl.media.player.ServicePlayer;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceContextResourceUsageImpl;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.util.Arrays;
import org.davic.net.tuning.NetworkInterface;
import org.ocap.hn.service.RemoteService;
import org.ocap.service.AlternativeContentErrorEvent;

public class RemoteServiceContextDelegate implements ServiceContextDelegate
{
    private static final Logger log = Logger.getLogger(RemoteServiceContextDelegate.class);

    private static final String PRESENTATION_MODE_CONTROL_CLASS_NAME = "org.cablelabs.impl.media.player.PresentationModeControl";

    private static final int STATE_NOT_PRESENTING = 1;

    //presentation (and player) are starting
    private static final int STATE_PRESENTING_PENDING = 2;

    private static final int STATE_PRESENTING = 3;

    private static final int STATE_STOP_PENDING = 4;

    //must wait for player start to proceed with stopping
    private static final int STATE_STOP_PENDING_WAIT_PLAYER_START = 5;
    
    private ServiceMediaHandlerCreationDelegate creationDelegate;

    private ServiceContextDelegateListener serviceContextDelegateListener;

    private ServiceMediaHandler serviceMediaHandler;

    private Object lock;

    private Service service;

    private int currentState = STATE_NOT_PRESENTING;

    private ControllerListener controllerListener;

    private AppDomain appDomain;

    private PresentationModeListenerImpl presentationModeListener;

    private boolean initialized;

    //if stopPresentingWithReason is called but the PresentationModeListener has not yet received an event, wait for the PresentationMode event 
    // then stop presenting with the cached class and reason 
    private Class cachedStopPresentingEventClass;
    private int cachedStopPresentingEventReason;
    //no need to use 'this' in the identity hash lookup, just a unique identifier
    private final String id = "Id: 0x" + Integer.toHexString(System.identityHashCode(new Object())).toUpperCase().toUpperCase() + ": ";

    //values provided in prepareToPresent
    private Service initialService;
    private Locator[] initialComponentLocators;
    private ServiceContextResourceUsageImpl initialServiceContextResourceUsage;
    private volatile long initialSequence;

    public synchronized void initialize(ServiceContextDelegateListener serviceContextDelegateListener,
            ServiceMediaHandlerCreationDelegate creationDelegate, AppDomain appDomain,
            PersistentVideoModeSettings videoModeSettings, Object lock)
    {
        synchronized (lock)
        {
            this.serviceContextDelegateListener = serviceContextDelegateListener;
            this.creationDelegate = creationDelegate;
            this.lock = lock;
            controllerListener = new ControllerListenerImpl();
            this.appDomain = appDomain;
            presentationModeListener = new PresentationModeListenerImpl();
            initialized = true;
            if (log.isDebugEnabled())
            {
                log.debug(id + "initialized");
            }
        }
        // Init any managers for HN? Do we listen to anything?
        // DeviceEventListener
        // NetModuleEvent, DeviceEvent
    }

    public boolean canPresent(Service service)
    {
        boolean result = (service instanceof RemoteService);
        if (log.isDebugEnabled())
        {
            log.debug(id + "canPresent: " + service + ": " + result);
        }

        return result;
    }

    public synchronized void destroy()
    {
        if (initialized)
        {
            // Added for findbugs issues fix - start
            // This synchronization block is commented as it is empty.
            // synchronized (lock)
            // {
                // release resources
            // }
            // Added for findbugs issues fix - end
        }
        initialized = false;
    }

    public Service getService()
    {
        assertInitialized();
        synchronized (lock)
        {
            return service;
        }
    }

    public NetworkInterface getNetworkInterface()
    {
        // abstract service doesn't use this
        return null;
    }

    public ServiceMediaHandler getServiceMediaHandler()
    {
        assertInitialized();
        synchronized (lock)
        {
            return serviceMediaHandler;
        }
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
            this.initialSequence = sequence;
            //callerContext is unused
        }
    }

    public void present(long presentSequence)
    {
        assertInitialized();

        boolean servicesEqual;
        ServiceMediaHandler tempServiceMediaHandler;
        PresentationModeListener tempPresentationModeListener;
        AppDomain tempAppDomain;
        ControllerListener tempControllerListener;

        synchronized (lock)
        {
            long tempInitialSequence = initialSequence;
            if (presentSequence != tempInitialSequence)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "ignoring present called with non-current sequence - current sequence: " + tempInitialSequence + ", sequence parameter: " + presentSequence);
                }
                return;
            }

            if (log.isInfoEnabled())
            {
                log.info(id + "presenting - sequence: " + presentSequence);
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
            log.debug(id + "already presenting service");
            //no component-specific locator support
            // all select calls need a notification - may be altcontent or normalcontent - just respond with presenting
            notifyNoChange();
            return;
        }

        boolean callStopPresenting = false;
        synchronized (lock)
        {
            try
            {
                service = initialService;
                ServiceDetails serviceDetails = ((ServiceExt) initialService).getDetails();
                serviceMediaHandler = creationDelegate.createServiceMediaHandler(serviceDetails, initialComponentLocators,
                        initialServiceContextResourceUsage, null, null);
                tempServiceMediaHandler = serviceMediaHandler;
                if (log.isInfoEnabled())
                {
                    log.info(id + "setting currentState to PRESENTING_PENDING");
                }
                currentState = STATE_PRESENTING_PENDING;
            }
            catch (SIRequestException sire)
            {
                if (sire.getReason() == SIRequestFailureType.INSUFFICIENT_RESOURCES)
                {
                    callStopPresenting = true;
                }
                else
                {
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
            catch (InterruptedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + " doPresent - interruptedException");
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
        if (callStopPresenting)
        {
            stopPresentingWithReason(SelectionFailedEvent.class, SelectionFailedEvent.INSUFFICIENT_RESOURCES);
        }
        else
        {
            if (tempServiceMediaHandler != null)
            {
                PresentationModeControl presentationModeControl = (PresentationModeControl) tempServiceMediaHandler.getControl(PRESENTATION_MODE_CONTROL_CLASS_NAME);
                if (presentationModeControl != null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(id + "registering listener on presentation mode control");
                    }
                    presentationModeControl.addPresentationModeListener(tempPresentationModeListener);
                }
                else
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(id + "no presentation mode control available");
                    }
                }

                // we can't currently present applications with remote
                // services...call stopBoundApps to ensure other
                // applications are not
                // running
                if (log.isDebugEnabled())
                {
                    log.debug(id + "stopping bound apps");
                }
                tempAppDomain.stopBoundApps();

                tempServiceMediaHandler.addControllerListener(tempControllerListener);
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
            long tempInitialSequence = initialSequence;
            if (presentSequence != tempInitialSequence)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(id + "ignoring stopPresenting called with non-current sequence - current sequence: " + tempInitialSequence + ", sequence parameter: " + presentSequence);
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
                log.debug(id + msgPrefix + ": RemoteServiceContextDelegate not initialized");
            }
        }
        return initialized;
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
            if (log.isDebugEnabled())
            {
                log.debug(id + "stopPresentingWithReason: " + eventClass + ", " + reason);
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
        // Notify listeners that the presentation has been terminated (after
        // releasePresentingResources)
        if (log.isInfoEnabled())
        {
            log.info(id + "setting currentState to NOT_PRESENTING");
        }
        synchronized (lock)
        {
            currentState = STATE_NOT_PRESENTING;
        }
        notifyNotPresenting(eventClass, reason);
    }

    private void releasePresentingResources()
    {
        //only resource to release is a service reference that needs to be nulled out
        synchronized (lock)
        {
            service = null;
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

    private void notifyNormalContent()
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
        ServiceContextDelegateListener tempServiceContextDelegateListener;
        synchronized (lock)
        {
            tempServiceContextDelegateListener = serviceContextDelegateListener;
        }
        tempServiceContextDelegateListener.presentingAlternativeContent(alternativeContentClass, alternativeContentErrorEventReasonCode);
    }

    private void notifyNotPresenting(Class className, int reasonCode)
    {
        ServiceContextDelegateListener tempServiceContextDelegateListener;
        synchronized (lock)
        {
            tempServiceContextDelegateListener = serviceContextDelegateListener;
        }
        tempServiceContextDelegateListener.notPresenting(className, reasonCode);
    }

    public String toString()
    {
        return "RemoteServiceContextDelegate: " + id + "service: " + service + ", player: " + serviceMediaHandler + ", state: " + currentState;
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
                    tempServiceContextDelegateListener.playerStopping((tempServiceMediaHandler));
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
                    tempServiceContextDelegateListener.playerStopping((tempServiceMediaHandler));
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
            if (!checkInitialized("Alternative content notification fired in PresentationModeListenerImpl")) return;
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
