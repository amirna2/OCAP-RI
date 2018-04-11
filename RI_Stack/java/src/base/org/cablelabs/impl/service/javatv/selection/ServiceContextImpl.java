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

import java.awt.Container;
import java.awt.Rectangle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectPermission;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextDestroyedEvent;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceContextPermission;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceContextResourceUsageImpl;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.CallbackList;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;
import org.davic.net.tuning.NetworkInterface;
import org.dvb.media.VideoTransformation;
import org.ocap.service.AlternativeContentErrorEvent;

public class ServiceContextImpl implements ServiceContextExt
{
    /*
     * Callback mechanism: send presenting(NORMAL_PRESENTATION) to represent a
     * successful start, even when stop is being called to interrupt an
     * initiating presentation send notPresenting(NORMAL_PRESENTATION) to
     * represent a successful stop
     */

    // NOTE: destroy and stop are asynchronous but a PENDING state for those
    // transitions is not required
    private static final int STATE_NOT_PRESENTING = 1;

    private static final int STATE_PRESENTING_PENDING = 2;

    private static final int STATE_PRESENTING = 3;

    private static final int STATE_NEW_SELECT_STOP_PENDING = 4;

    //for purposes of modeling the states and events described in the ServiceContext class javadoc,
    //the ServiceContext 'STOPPED' state is represented by both STATE_USER_STOP_PENDING and STATE_NOT_PRESENTING
    //(no INTERRUPTED ServiceContext event will be posted if select is called in STATE_USER_STOP_PENDING)
    private static final int STATE_USER_STOP_PENDING = 5;

    private static final int STATE_DESTROYED = 6;

    private static final String[] STATE_NAMES = { "NOT USED(0)", "NOT_PRESENTING", "PRESENTING_PENDING", "PRESENTING",
            "NEW_SELECT_STOP_PENDING", "USER_STOP_PENDING", "DESTROYED" };

    private static final Logger log = Logger.getLogger(ServiceContextImpl.class);

    private static final Logger performanceLog = Logger.getLogger("Performance.ServiceSelection");

    // NOTE: current use of locks is to prevent concurrent access to delegate or
    // currentState fields

    // NOTE: JMF uses the Context's lock (if context is not null and lock is not
    // null) in the AbstractPlayer class, because that
    // class calls back into service context..share the lock used here with the
    // context
    protected final Object mutex = new ServiceContextLock();

    private final Object callerContextMutex = new Object();

    private final CallerContextManager callerContextManager;

    //keep a reference to all callercontexts which add ServiceContextListeners, so individual multicasters
    //can be released when the ServiceContext is destroyed
    private final Set callerContexts = new HashSet();
    //the EventMulticaster which holds references to all callercontexts so ServiceContextListeners can be notified on the
    //correct callercontext
    private CallerContext callerContextMulticaster;

    // these fields (and delegate) can be modified by one thread and examined by
    // another...use the mutex to protect access
    private int currentState = STATE_NOT_PRESENTING;

    // failure due to SelectionFailedEvent requires posting
    // PresentationTerminatedEvent based on state when failing select was called
    private int stateAtTimeOfSelection = currentState;

    private boolean destroyWhenIdle;

    // delegate will be nulled out each time we're no longer attempting to
    // present
    private ServiceContextDelegate delegate = null;

    private final List serviceContextDelegates = new ArrayList();

    private final CallbackList serviceContextCallbacks;

    // track the last successful service presentation (locators or service) and
    // in some failures to select, present this service)
    private Service lastPresentingService;

    private Locator[] lastPresentingLocators;

    // track the service attempting to be presented (so we can mark as
    // lastPresentingService if successful)
    private Service presentationPendingService;

    private Locator[] presentationPendingLocators;

    private final VideoTransformation platformDefaultVideoTransformation = new VideoTransformation();

    /**
     * Persistent scaled video mode settings associated with service context -
     * note it's final now, so appropriate defaults should be set if necessary
     * also, the field being null meant apps could start...
     */
    private final PersistentVideoModeSettings persistentSettings;

    private AppDomain appDomain;

    private final ServiceContextDelegateListenerImpl serviceContextDelegateListener;

    private final ServiceMediaHandlerCreationDelegate creationDelegate;

    private CallerContext selectingCallerContext;

    private final CallerContext creatingCallerContext;

    private ServiceContextResourceUsageImpl resourceUsage;
    // increment when select is called while a presentation is pending
    // interrupted events will be posted when servicecontxt state transitions out of pending
    private int selectCalledWhilePendingCount;
    
    //static - incremented each time a ServiceContext is constructed
    //updated in the constructor while holding mutex
    private static int serviceContextId = 0;

    //not static - this instance's id - assigned in the constructor while holding mutex
    private final int instanceServiceContextId;

    private long currentPresentingSequence = 0L;

    // NOTE: permission exceptions thrown in preference to state exceptions
    public ServiceContextImpl(boolean destroyWhenIdle)
    {
        synchronized (mutex)
        {
            serviceContextId++;
            instanceServiceContextId = serviceContextId;
        }

        this.destroyWhenIdle = destroyWhenIdle;
        serviceContextDelegateListener = new ServiceContextDelegateListenerImpl();

        // NOTE: appDomain must be created early on in the lifecycle in order
        // for auto start apps to successfully start
        callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        creatingCallerContext = callerContextManager.getCurrentContext();

        final AppDomain[] appDomainHolder = new AppDomain[1];
        CallerContext.Util.doRunInContextSync(callerContextManager.getSystemContext(), new Runnable()
        {
            public void run()
            {
                ApplicationManager appManager = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
                appDomainHolder[0] = appManager.createAppDomain(ServiceContextImpl.this);
            }
        });
        appDomain = appDomainHolder[0];

        persistentSettings = new PersistentVideoModeSettings();

        creationDelegate = new ServiceMediaHandlerCreationDelegate(this, persistentSettings, mutex);

        serviceContextCallbacks = new CallbackList(ServiceContextCallback.class);
        if (log.isInfoEnabled())
        {
            log.info("ServiceContext instance created: " + getDiagnosticInfo());
        }
    }

    public void setAvailableServiceContextDelegates(List serviceContextDelegates)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setAvailableServiceContextDelegates: " + serviceContextDelegates);
        }
        synchronized (mutex)
        {
            for (Iterator iter = serviceContextDelegates.iterator(); iter.hasNext();)
            {
                ServiceContextDelegate delegate = (ServiceContextDelegate) iter.next();
                delegate.initialize(serviceContextDelegateListener, creationDelegate, appDomain, persistentSettings,
                        mutex);
            }
            this.serviceContextDelegates.addAll(serviceContextDelegates);
        }
    }

    // temporary implementation - doesn't acquire resources forcefully
    public void forceEASTune(Service service)
    {
        if (log.isInfoEnabled())
        {
            log.info("forceEASTune - service - " + service + ": " + getDiagnosticInfo());
        }
        synchronized(mutex)
        {
            selectingCallerContext = callerContextManager.getCurrentContext();
            stateAtTimeOfSelection = currentState;
        }
        doSelect(service, true);
    }

    public void setDestroyWhenIdle(boolean destroyWhenIdle)
    {
        if (log.isInfoEnabled())
        {
            log.info("setDestroyWhenIdle: " + destroyWhenIdle + ": " + getDiagnosticInfo());
        }
        // ignore calls in other states
        boolean destroyNow;
        synchronized (mutex)
        {
            this.destroyWhenIdle = destroyWhenIdle;
            destroyNow = destroyWhenIdle && (STATE_NOT_PRESENTING == currentState);
        }
        if (destroyNow)
        {
            if (log.isInfoEnabled())
            {
                log.info("destroy when idle true - currently idle - destroying: " + getDiagnosticInfo());
            }
            destroy();
        }
    }

    public boolean isPresenting()
    {
        synchronized (mutex)
        {
            // report that the SC is presenting in all states except:
            // STATE_PRESENTING_PENDING (initiating presentation from
            // NOT_PRESENTING)
            // STATE_NOT_PRESENTING
            // STATE_DESTROYED
            return (STATE_PRESENTING == currentState || STATE_NEW_SELECT_STOP_PENDING == currentState || STATE_USER_STOP_PENDING == currentState);
        }
    }

    public boolean isDestroyed()
    {
        synchronized (mutex)
        {
            return (STATE_DESTROYED == currentState);
        }
    }

    public AppDomain getAppDomain()
    {
        synchronized (mutex)
        {
            assertServiceContextNotDestroyed();
            return appDomain;
        }
    }

    public CallerContext getCallerContext()
    {
        synchronized (mutex)
        {
            assertServiceContextNotDestroyed();
            return selectingCallerContext;
        }
    }

    public CallerContext getCreatingContext()
    {
        return creatingCallerContext;
    }

    public ServiceMediaHandler addServiceContextCallback(ServiceContextCallback callback, int priority)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addServiceContextCallback - callback: " + callback + ", priority: " + priority + ": "
                    + getDiagnosticInfo());
        }
        ServiceContextDelegate tempDelegate;
        synchronized (mutex)
        {
            assertServiceContextNotDestroyed();
            tempDelegate = delegate;
            serviceContextCallbacks.addCallback(callback, priority);
        }
        return (isPresenting() ? tempDelegate.getServiceMediaHandler() : null);
    }

    public void removeServiceContextCallback(ServiceContextCallback callback)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removeServiceContextCallback: " + callback + ": " + getDiagnosticInfo());
        }
        synchronized (mutex)
        {
            // Its ok to call this method when ServiceContext is in DESTROYED
            // state
            serviceContextCallbacks.removeCallback(callback);
        }
    }

    public void stopAbstractService()
    {
        if (log.isInfoEnabled())
        {
            log.info("stopAbstractService:" + getDiagnosticInfo());
        }
        // TODO: does stopAbstractService require a permission check?
        callerContextManager.getSystemContext().runInContext(new Runnable()
        {
            public void run()
            {
                assertServiceContextNotDestroyed();

                ServiceContextDelegate tempDelegate;
                boolean callDelegateStopPresentingAbstractService = false;
                synchronized (mutex)
                {
                    tempDelegate = delegate;
                    switch (currentState)
                    {
                        case STATE_NOT_PRESENTING:
                        case STATE_USER_STOP_PENDING:
                        case STATE_NEW_SELECT_STOP_PENDING:
                            if (log.isDebugEnabled())
                            {
                                log.debug("stopAbstractService called in NOT_PRESENTING, NEW_SELECT_STOP_PENDING or STOP_PENDING: "
                                        + getDiagnosticInfo());
                            }
                            break;
                        case STATE_PRESENTING_PENDING:
                        case STATE_PRESENTING:
                            if (log.isInfoEnabled())
                            {
                                log.info("stopAbstractService called in PRESENTING_PENDING or PRESENTING: "
                                        + getDiagnosticInfo());
                            }
                            callDelegateStopPresentingAbstractService = true;
                            break;
                        default:
                            throw new IllegalStateException("stopAbstractService - unexpected state " + currentState
                                    + ": " + getDiagnosticInfo());
                    }
                }
                if (callDelegateStopPresentingAbstractService)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("calling stopPresentingAbstractService on: " + tempDelegate + ": " + getDiagnosticInfo());
                    }
                    tempDelegate.stopPresentingAbstractService();
                }
            }
        });
    }

    public NetworkInterface getNetworkInterface()
    {
        ServiceContextDelegate tempDelegate;
        synchronized (mutex)
        {
            assertServiceContextNotDestroyed();
            tempDelegate = delegate;
        }
        return tempDelegate == null ? null : tempDelegate.getNetworkInterface();
    }

    public void setDefaultVideoTransformation(VideoTransformation videoTransformation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setDefaultVideoTransformation: " + videoTransformation + ": " + getDiagnosticInfo());
        }
        assertServiceContextNotDestroyed();
        // Check for persistent settings class available.
        if (videoTransformation == null)
        { // If null is passed, revert to platform default
            persistentSettings.setVideoTransformation(platformDefaultVideoTransformation);
        }
        else
        {
            persistentSettings.setVideoTransformation(videoTransformation);
        }

        persistentSettings.setComponent(null, null);
    }

    public void unforceEASTune()
    {
        ServiceContextResourceUsageImpl tempResourceUsage;
        boolean updateResourceUsage = false;
        synchronized(mutex)
        {
            if (log.isInfoEnabled())
            {
                log.info("unforce EAS tune (" + STATE_NAMES[currentState] + ") - " + getDiagnosticInfo());
            }
            tempResourceUsage = resourceUsage;
            //in which states should we handle this?
            switch (currentState)
            {
                case STATE_NOT_PRESENTING:
                    break;
                case STATE_PRESENTING_PENDING:
                case STATE_USER_STOP_PENDING:
                case STATE_NEW_SELECT_STOP_PENDING:
                case STATE_PRESENTING:
                    updateResourceUsage = true;
                    // fall through to run code outside of monitor
                    break;
                default:
                    throw new IllegalStateException("unforceEASTune called in unknown state " + currentState
                            + ": " + getDiagnosticInfo());
            }
        }
        if (updateResourceUsage)
        {
            tempResourceUsage.setResourceUsageEAS(false);
        }
    }

    public void select(Service service) throws SecurityException
    {
        synchronized(mutex)
        {
            selectingCallerContext = callerContextManager.getCurrentContext();
            stateAtTimeOfSelection = currentState;
        }
        doSelect(service, false);
    }

    /**
     * Select service
     * 
     * @param service
     *            the service to select
     * @param isEAS present for EAS
     * @throws SecurityException
     */
    private void doSelect(final Service service, final boolean isEAS) throws SecurityException
    {
        assertServiceContextNotDestroyed();
        synchronized (mutex)
        {
            if (log.isInfoEnabled())
            {
                log.info("select service - " + service + ": " + getDiagnosticInfo());
            }
            if (!isEAS && resourceUsage != null && resourceUsage.isResourceUsageEAS())
            {
                throw new IllegalStateException("ServiceContext is presenting EAS: " + getDiagnosticInfo());
            }
            stateAtTimeOfSelection = currentState;
            //if select is called while a selection is pending, increment the count
            if (currentState == STATE_NEW_SELECT_STOP_PENDING || currentState == STATE_PRESENTING_PENDING)
            {
                //update interrupted count that will be posted when notPresenting is received
                selectCalledWhilePendingCount++;
                if (log.isDebugEnabled())
                {
                    log.debug("incrementing selectCalledWhilePendingCount to: " + selectCalledWhilePendingCount);
                }
            }
        }
        // throw exceptions if possible before changing the state of the
        // currently selected service
        if (service == null)
        {
            throw new NullPointerException("specified service is null");
        }

        Locator serviceLocator = service.getLocator();
        if (serviceLocator == null)
        {
            throw new NullPointerException("service locator is null");
        }

        checkSelectPermission(serviceLocator);

        //there is always a delegate, as a default is always registered
        final ServiceContextDelegate newDelegate = lookupDelegate(service);
        callerContextManager.getSystemContext().runInContext(new Runnable()
        {
            public void run()
            {
                doSelect(service, null, newDelegate, isEAS);
            }
        });
    }

    public void select(Locator[] componentLocators) throws InvalidLocatorException, InvalidServiceComponentException,
            SecurityException
    {
        synchronized(mutex)
        {
            selectingCallerContext = callerContextManager.getCurrentContext();
            stateAtTimeOfSelection = currentState;
        }
        doSelect(componentLocators, false);
    }

    /**
     * Select locators
     * 
     * @param componentLocators
     *            the locators to select
     * 
     * @param isEAS present for EAS
     * @throws InvalidLocatorException
     *             thrown if locators are invalid
     * @throws InvalidServiceComponentException
     *             if locators not all part of same service
     * @throws SecurityException
     *             if select permission not granted
     * @throws NullPointerException
     *             if null component locators or zero-length array
     */
    private void doSelect(final Locator[] componentLocators, final boolean isEAS) throws InvalidLocatorException, InvalidServiceComponentException,
            SecurityException
    {
        assertServiceContextNotDestroyed();
        synchronized (mutex)
        {
            if (log.isInfoEnabled())
            {
                log.info("select locators (" + STATE_NAMES[currentState] + ") - " + Arrays.toString(componentLocators)
                        + ": " + getDiagnosticInfo());
            }
            //if select is called while a selection is pending, increment the count
            if (currentState == STATE_NEW_SELECT_STOP_PENDING || currentState == STATE_PRESENTING_PENDING)
            {
                //update interrupted count that will be posted when notPresenting is received
                selectCalledWhilePendingCount++;
                if (log.isDebugEnabled())
                {
                    log.debug("incrementing selectCalledWhilePendingCount to: " + selectCalledWhilePendingCount);
                }
            }
        }
        // throw exceptions if possible before changing the state of the
        // currently selected service
        if (componentLocators == null)
        {
            throw new NullPointerException("select componentLocator[]- null array");
        }
        if (componentLocators.length == 0)
        {
            throw new IllegalArgumentException("select componentLocator[] - empty array");
        }
        if (componentLocators[0] == null)
        {
            throw new NullPointerException("select componentLocator[] - no componentLocators specified");
        }

        // Get the service object and make sure all locators are part of the
        // same service.
        SIManager siManager = SIManager.createInstance();
        final Service service = siManager.getService(componentLocators[0]);
        Locator serviceLocator = service.getLocator();
        if (serviceLocator == null)
        {
            throw new InvalidServiceComponentException(componentLocators[0], "No service");
        }

        // examine remaining services to make sure all componentLocators are
        // part of the same service
        for (int i = 1; i < componentLocators.length; i++)
        {
            if (!service.equals(siManager.getService(componentLocators[i])))
            {
                throw new InvalidServiceComponentException(componentLocators[i],
                        "Components not all part of the same service");
            }
        }

        checkSelectPermission(serviceLocator);
        //there is always a delegate, as a default is always registered
        final ServiceContextDelegate newDelegate = lookupDelegate(service);
        callerContextManager.getSystemContext().runInContext(new Runnable()
        {
            public void run()
            {
                doSelect(service, componentLocators, newDelegate, isEAS);
            }
        });
    }

    public void stop() throws SecurityException
    {
        if (log.isInfoEnabled())
        {
            log.info("stop: " + getDiagnosticInfo());
        }
        checkServiceContextPermission("stop");
        assertServiceContextNotDestroyed();
        assertResourceUsageIsNotEAS();
        callerContextManager.getSystemContext().runInContext(new Runnable()
        {
            public void run()
            {

                boolean callDelegateStopPresenting = false;
                ServiceContextDelegate tempDelegate;
                long tempSequence;
                synchronized (mutex)
                {
                    tempDelegate = delegate;
                    tempSequence = currentPresentingSequence;
                    switch (currentState)
                    {
                        case STATE_NOT_PRESENTING:
                        case STATE_USER_STOP_PENDING:
                            if (log.isDebugEnabled())
                            {
                                log.debug("stop called in NOT_PRESENTING or STOP_PENDING: " + getDiagnosticInfo());
                            }
                            break;
                        case STATE_NEW_SELECT_STOP_PENDING:
                            if (log.isDebugEnabled())
                            {
                                log.debug("stop called in NEW_SELECT_STOP_PENDING: " + getDiagnosticInfo());
                            }
                            if (log.isInfoEnabled())
                            {
                                log.info("setting state to STOP_PENDING: " + getDiagnosticInfo());
                            }
                            currentState = STATE_USER_STOP_PENDING;
                            break;
                        case STATE_PRESENTING_PENDING:
                        case STATE_PRESENTING:
                            if (log.isInfoEnabled())
                            {
                                log.info("stop called in PRESENTING_PENDING or PRESENTING: " + getDiagnosticInfo());
                            }
                            // transition to stop pending, so when the
                            // notPresenting notification is triggered, we know
                            // it was user-initiated (select while presenting
                            // does not transition to stop pending, so we can
                            // differentiate between a stop of the delegate to
                            // select a new service vs. a user-initiated stop)
                            if (log.isInfoEnabled())
                            {
                                log.info("setting state to STOP_PENDING: " + getDiagnosticInfo());
                            }
                            currentState = STATE_USER_STOP_PENDING;
                            callDelegateStopPresenting = true;
                            break;
                        default:
                            throw new IllegalStateException("stop - unexpected state: " + getDiagnosticInfo());
                    }
                }
                if (callDelegateStopPresenting)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("stop - calling stopPresenting on: " + tempDelegate + ": " + getDiagnosticInfo());
                    }
                    tempDelegate.stopPresenting(tempSequence);
                }
            }
        });
    }

    /**
     * Stops presentation if one is ongoing, and posts a
     * ServiceContextDestroyedEvent in all cases except when this ServiceContext
     * is already DESTROYED.
     * 
     * @throws SecurityException
     */
    public void destroy() throws SecurityException
    {
        if (log.isInfoEnabled())
        {
            log.info("destroy: " + getDiagnosticInfo());
        }
        checkServiceContextPermission("destroy");
        assertResourceUsageIsNotEAS();
        callerContextManager.getSystemContext().runInContext(new Runnable()
        {
            public void run()
            {
                boolean callCleanup = false;
                boolean callDelegateStopPresenting = false;
                ServiceContextDelegate tempDelegate;
                long tempSequence;
                synchronized (mutex)
                {
                    tempDelegate = delegate;
                    tempSequence = currentPresentingSequence;
                    switch (currentState)
                    {
                        case STATE_NOT_PRESENTING:
                            if (log.isInfoEnabled())
                            {
                                log.info("destroy called in NOT_PRESENTING: " + getDiagnosticInfo());
                            }
                            if (log.isInfoEnabled())
                            {
                                log.info("setting state to DESTROYED: " + getDiagnosticInfo());
                            }
                            currentState = STATE_DESTROYED;
                            if (log.isInfoEnabled())
                            {
                                log.info("posting ServiceContextDestroyedEvent: " + getDiagnosticInfo());
                            }
                            postEvent(new ServiceContextDestroyedEvent(ServiceContextImpl.this));
                            callCleanup = true;
                            break;
                        case STATE_NEW_SELECT_STOP_PENDING:
                        case STATE_USER_STOP_PENDING:
                            if (log.isInfoEnabled())
                            {
                                log.info("destroy called in STOP_PENDING or NEW_SELECT_STOP_PENDING: "
                                        + getDiagnosticInfo());
                            }
                            if (log.isInfoEnabled())
                            {
                                log.info("setting state to DESTROYED: " + getDiagnosticInfo());
                            }
                            currentState = STATE_DESTROYED;
                            // will clean up when we get the notPresenting
                            break;
                        case STATE_PRESENTING_PENDING:
                        case STATE_PRESENTING:
                            if (log.isInfoEnabled())
                            {
                                log.info("destroy called in PRESENTING_PENDING or PRESENTING: " + getDiagnosticInfo());
                            }
                            if (log.isInfoEnabled())
                            {
                                log.info("setting state to DESTROYED: " + getDiagnosticInfo());
                            }
                            // TODO: stopPresenting may throw a
                            // runtimeException, but we still want
                            // destroy to run..
                            // do we just call stop from inside destroy,
                            // or nest try blocks in order to report
                            // exceptions?

                            // setting to DESTROYED state..when we get the call
                            // in notPresenting, destroy the delegate,
                            // and post the destroyed event and null out the
                            // delegate and call cleanup
                            currentState = STATE_DESTROYED;
                            callDelegateStopPresenting = true;
                            break;
                        case STATE_DESTROYED:
                            if (log.isDebugEnabled())
                            {
                                log.debug("destroy called in DESTROYED: " + getDiagnosticInfo());
                            }
                            break;
                        default:
                            throw new IllegalStateException("destroy - unexpected state " + currentState + ": "
                                    + getDiagnosticInfo());
                    }
                }
                if (callDelegateStopPresenting)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("destroy - calling stopPresenting on: " + tempDelegate + ": " + getDiagnosticInfo());
                    }
                    tempDelegate.stopPresenting(tempSequence);
                }
                if (callCleanup)
                {
                    // Call cleanup method with 'cleanupCallerContextData' flag set to false 
                    cleanup(false);
                }
            }
        });
    }

    public ServiceContentHandler[] getServiceContentHandlers() throws SecurityException
    {
        checkServiceContextPermission("getServiceContentHandlers");
        assertServiceContextNotDestroyed();
        ServiceContextDelegate tempDelegate;
        synchronized (mutex)
        {
            switch (currentState)
            {
                case STATE_NOT_PRESENTING:
                case STATE_PRESENTING_PENDING:
                case STATE_USER_STOP_PENDING:
                case STATE_NEW_SELECT_STOP_PENDING:
                    return new ServiceContentHandler[] {};
                case STATE_PRESENTING:
                    tempDelegate = delegate;
                    // fall through to run code outside of monitor
                    break;
                default:
                    throw new IllegalStateException("getServiceContentHandlers called in unknown state " + currentState
                            + ": " + getDiagnosticInfo());
            }
        }
        return tempDelegate.getServiceContentHandlers();
    }

    public Service getService()
    {
        assertServiceContextNotDestroyed();
        ServiceContextDelegate tempDelegate;
        synchronized (mutex)
        {
            if (!isPresenting())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getService - not presenting - returning null:" + getDiagnosticInfo());
                }
                return null;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getService - presenting - returning " + delegate.getService() + ":"
                            + getDiagnosticInfo());
                }
                tempDelegate = delegate;
            }
        }
        // returning outside of monitor
        return tempDelegate.getService();
    }

    public void setPersistentVideoMode(boolean enabled)
    {
        assertServiceContextNotDestroyed();
        if (log.isDebugEnabled())
        {
            log.debug("setPersistentVideoMode: " + enabled + ": " + getDiagnosticInfo());
        }
        persistentSettings.setEnabled(enabled);
    }

    public void setApplicationsEnabled(boolean enabled)
    {
        assertServiceContextNotDestroyed();
        if (log.isDebugEnabled())
        {
            log.debug("setApplicationsEnabled: " + enabled + ": " + getDiagnosticInfo());
        }
        persistentSettings.setAppsEnabled(enabled);

        // Call application domain to finish operation.
        // TODO(afh): need to ask Pat Ladd if this is immediate or not...
        // appDomain.stop(); appDomain.select(appsEnabled);
    }

    public boolean isAppsEnabled()
    {
        assertServiceContextNotDestroyed();
        return persistentSettings.isAppsEnabled();
    }

    public boolean isPersistentVideoMode()
    {
        assertServiceContextNotDestroyed();
        return persistentSettings.isEnabled();
    }

    // TODO: craig mentioned this method may go away - implement?
    public void swapSettings(ServiceContext sourceServiceContext, boolean audioUse, boolean swapAppSettings)
            throws IllegalArgumentException
    {
        // no-op
    }

    public void setInitialBackground(VideoTransformation videoTransformation)
    {
        assertServiceContextNotDestroyed();
        if (log.isDebugEnabled())
        {
            log.debug("setInitialBackground: " + videoTransformation + ": " + getDiagnosticInfo());
        }
        persistentSettings.setVideoTransformation(videoTransformation);

        // QUESTION: does this "clear" the component settings?
        // persistentSettings.setComponent(null, null);
    }

    public void setInitialComponent(Container container, Rectangle rectangle)
    {
        assertServiceContextNotDestroyed();
        if (log.isDebugEnabled())
        {
            log.debug("setInitialComponent - container: " + container + ", rectangle: " + rectangle + ": " + getDiagnosticInfo());
        }
        persistentSettings.setComponent(container, rectangle);
    }

    public void addListener(ServiceContextListener listener)
    {
        assertServiceContextNotDestroyed();
        // Check for Null parameters
        if (listener == null)
        {
            throw new NullPointerException("null parameters not allowed");
        }
        if (log.isDebugEnabled())
        {
            log.debug("addListener: " + listener + ": " + getDiagnosticInfo());
        }

        synchronized (callerContextMutex)
        {
            CallerContext callerContext = callerContextManager.getCurrentContext();
            CCData data = getCCData(callerContext);
            data.listeners = EventMulticaster.add(data.listeners, listener);
        }
    }

    public void removeListener(ServiceContextListener listener)
    {
        assertServiceContextNotDestroyed();
        // NOTE: EventMulticaster.remove(null) won't trigger an NPE, but will
        // return null, so we don't want to allow it..
        // throw an NPE here?
        if (listener == null)
        {
            throw new NullPointerException("null parameters not allowed");
        }

        if (log.isDebugEnabled())
        {
            log.debug("removeListener: " + listener + ": " + getDiagnosticInfo());
        }

        // Remove the listener from the list of listeners for this caller
        // context.
        synchronized (callerContextMutex)
        {
            CallerContext callerContext = callerContextManager.getCurrentContext();
            CCData data = getCCData(callerContext);
            data.listeners = EventMulticaster.remove(data.listeners, listener);
        }
    }

    protected ServiceContextDelegate getDelegate()
    {
        synchronized (mutex)
        {
            return delegate;
        }
    }

    protected void cleanup(boolean cleanupCallerContextData)
    {
        synchronized (mutex)
        {
            if (log.isDebugEnabled())
            {
                log.debug("cleanup: " + getDiagnosticInfo());
            }
            appDomain.destroy();
            delegate = null;
            synchronized(callerContextMutex)
            {
                // Callercontext specific listeners are cleaned up after
                // notification is complete in postEvent() method
                if(cleanupCallerContextData)
                {
                    //set each CCData's EventMulticaster/listener to null
                    for (Iterator iter = callerContexts.iterator();iter.hasNext();)
                    {
                        CallerContext callerContext = (CallerContext) iter.next();
                        CCData data = getCCData(callerContext);
                        data.listeners = null;
                    }
                }
                callerContexts.clear();
                callerContextMulticaster = null;
            }            	
            lastPresentingLocators = null;
            lastPresentingService = null;
            presentationPendingLocators = null;
            presentationPendingService = null;
        }
    }

    protected void assertServiceContextNotDestroyed()
    {
        synchronized (mutex)
        {
            if (STATE_DESTROYED == currentState)
            {
                throw new IllegalStateException("ServiceContext has been destroyed: " + getDiagnosticInfo());
            }
        }
    }

    private void assertResourceUsageIsNotEAS()
    {
        synchronized(mutex)
        {
            if (resourceUsage != null && resourceUsage.isResourceUsageEAS())
            {
                throw new IllegalStateException("ServiceContext is presenting EAS: " + getDiagnosticInfo());
            }
        }
    }

    // NOTE: assume we're already in SystemContext
    private void doSelect(Service service, Locator[] componentLocators, final ServiceContextDelegate newDelegate, boolean isEAS)
    {
        assertServiceContextNotDestroyed();

        boolean callDelegatePresent = false;
        boolean callDelegateStopPresenting = false;
        ServiceContextDelegate tempDelegate;
        long tempCurrentPresentingSequence;
        long tempNewPresentingSequence = -1;
        synchronized (mutex)
        {
            if (log.isDebugEnabled())
            {
                log.debug("doSelect ("
                        + STATE_NAMES[currentState]
                        + ") - "
                        + service
                        + ", "
                        + (componentLocators == null ? "no locators" : "locators: "
                                + Arrays.toString(componentLocators)) + ": " + getDiagnosticInfo());
            }
            tempDelegate = delegate;
            tempCurrentPresentingSequence = currentPresentingSequence;
            // set pending service/locators at this point
            presentationPendingService = service;
            if (componentLocators != null)
            {
                presentationPendingLocators = (Locator[]) Arrays.copy(componentLocators, Locator.class);
            }
            else
            {
                presentationPendingLocators = null;
            }

            if (performanceLog.isInfoEnabled())
            {
                performanceLog.info("Service Selection Started:ServiceContext " + instanceServiceContextId + ", Locator " + service.getLocator().toExternalForm());
            }

            switch (currentState)
            {
                case STATE_NOT_PRESENTING:
                    if (log.isInfoEnabled())
                    {
                        log.info("select called in NOT PRESENTING: " + getDiagnosticInfo());
                    }
                    if (log.isInfoEnabled())
                    {
                        log.info("setting state to PRESENTING_PENDING: " + getDiagnosticInfo());
                    }
                    currentState = STATE_PRESENTING_PENDING;
                    //create the ResourceUsage
                    resourceUsage = new ServiceContextResourceUsageImpl(selectingCallerContext, this, service, isEAS);
                    //increment presenting sequence since delegate#present is going to be called
                    tempNewPresentingSequence = ++currentPresentingSequence;
                    callDelegatePresent = true;
                    newDelegate.prepareToPresent(service, componentLocators, resourceUsage, selectingCallerContext, tempNewPresentingSequence);
                    break;
                case STATE_NEW_SELECT_STOP_PENDING:
                    if (log.isInfoEnabled())
                    {
                        log.info("select called in NEW_SELECT_STOP_PENDING: " + getDiagnosticInfo());
                    }
                    //update the resourceUsage EAS flag
                    resourceUsage.setRequestedService(service);
                    resourceUsage.setResourceUsageEAS(isEAS);
                    //interrupted event will be posted when notPresenting notification is received
                    break;
                case STATE_USER_STOP_PENDING:
                    resourceUsage.setRequestedService(service);
                    //update the resourceUsage EAS flag
                    resourceUsage.setResourceUsageEAS(isEAS);
                    if (log.isInfoEnabled())
                    {
                        log.info("select called in STOP PENDING: " + getDiagnosticInfo());
                    }
                    if (log.isInfoEnabled())
                    {
                        log.info("setting state to NEW_SELECT_STOP_PENDING: " + getDiagnosticInfo());
                    }
                    currentState = STATE_NEW_SELECT_STOP_PENDING;
                    break;
                // stop the current service if there is one
                case STATE_PRESENTING_PENDING:
                    // no state change needed
                    if (log.isInfoEnabled())
                    {
                        log.info("select called in PRESENTING PENDING: " + getDiagnosticInfo());
                    }
                    if (log.isInfoEnabled())
                    {
                        log.info("setting state to NEW_SELECT_STOP_PENDING: " + getDiagnosticInfo());
                    }
                    // transition to new_select_stop_pending, present when the notPresenting callback is received
                    currentState = STATE_NEW_SELECT_STOP_PENDING;
                    resourceUsage.setRequestedService(service);
                    //update the resourceUsage EAS flag
                    resourceUsage.setResourceUsageEAS(isEAS);
                    callDelegateStopPresenting = true;
                    //interrupted event will be posted when notPresenting notification is received
                    break;
                case STATE_PRESENTING:
                    if (log.isInfoEnabled())
                    {
                        log.info("select called in PRESENTING: " + getDiagnosticInfo());
                    }
                    if (delegate.isPresenting(service))
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("already presenting service - not stopping delegate before initiating selection with possibly new locators: "
                                    + getDiagnosticInfo());
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info("setting state to PRESENTING_PENDING: " + getDiagnosticInfo());
                        }
                        // presenting...currently presenting this service? If so, don't stop it
                        currentState = STATE_PRESENTING_PENDING;
                        resourceUsage.setRequestedService(service);
                        //update the resourceUsage EAS flag
                        resourceUsage.setResourceUsageEAS(isEAS);
                        // call delegate.present without calling delegate.stop
                        //DO NOT increment currentPresentingSequence since delegate#present is going to be called with the already-presenting service
                        tempNewPresentingSequence = currentPresentingSequence;
                        callDelegatePresent = true;
                        newDelegate.prepareToPresent(service, componentLocators, resourceUsage, selectingCallerContext, tempNewPresentingSequence);
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("not already presenting requested service: " + getDiagnosticInfo());
                        }

                        if (log.isInfoEnabled())
                        {
                            log.info("setting state to NEW_SELECT_STOP_PENDING: " + getDiagnosticInfo());
                        }
                        // transition to new_select_stop_pending, so we can
                        // present when we get the notPresenting callback
                        currentState = STATE_NEW_SELECT_STOP_PENDING;
                        resourceUsage.setRequestedService(service);
                        //update the resourceUsage EAS flag
                        resourceUsage.setResourceUsageEAS(isEAS);

                        // different service - stop the current service (will
                        // start the new one from notPresenting)
                        callDelegateStopPresenting = true;
                    }
                    break;
                default:
                    throw new IllegalStateException("select called in unexpected state: " + getDiagnosticInfo());
            }

            if (callDelegatePresent)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("present using delegate " + newDelegate + ": " + getDiagnosticInfo());
                }
                delegate = newDelegate;
            }
        }

        if (callDelegateStopPresenting)
        {
            if (log.isInfoEnabled())
            {
                log.info("doSelect - calling stopPresenting: " + tempDelegate + ": " + getDiagnosticInfo());
            }
            tempDelegate.stopPresenting(tempCurrentPresentingSequence);
        }

        //present while not holding the lock - enqueue on the systemcontext pool, not system-1
        if (callDelegatePresent)
        {
            if (log.isInfoEnabled())
            {
                log.info("doSelect - calling present: " + getDiagnosticInfo());
            }
            final long finalTempNewPresentingSequence = tempNewPresentingSequence;
            CallerContext.Util.doRunInContextSync(callerContextManager.getSystemContext(), new Runnable()
            {
                public void run()
                {
                    newDelegate.present(finalTempNewPresentingSequence);
                }
            });
        }
    }

    private void checkSelectPermission(Locator serviceLocator)
    {
        // Make sure we have permission to perform the service selection
        if (isContextOwned())
        {
            SecurityUtil.checkPermission(new SelectPermission(serviceLocator, "own"));
        }
        else
        {
            SecurityUtil.checkPermission(new SelectPermission(serviceLocator, "*"));
        }
    }

    protected void checkServiceContextPermission(String permission)
    {
        if (isContextOwned())
        {
            SecurityUtil.checkPermission(new ServiceContextPermission(permission, "own"));
        }
        else
        {
            SecurityUtil.checkPermission(new ServiceContextPermission(permission, "*"));
        }
    }

    /**
     * Look up delegate
     * 
     * @param service
     *            passed in to delegate to determine if delegate can select the
     *            service
     * 
     * @return delegate or null if no delegate can select the service
     */
    protected ServiceContextDelegate lookupDelegate(Service service)
    {
        List delegateList;
        synchronized (mutex)
        {
            delegateList = new ArrayList(serviceContextDelegates);
        }

        ServiceContextDelegate delegateToUse = null;
        for (Iterator iter = delegateList.iterator(); iter.hasNext();)
        {
            ServiceContextDelegate delegate = (ServiceContextDelegate) iter.next();
            if (delegate.canPresent(service))
            {
                delegateToUse = delegate;
                break;
            }
        }

        return delegateToUse;
    }

    /**
     * Context is owned if the currentContext is the same instance as the
     * CallerContext used at ServiceContext construction or this ServiceContext
     * instance is the same instance returned by
     * currentContext.get(CallerContext.SERVICE_CONTEXT)
     *
     * @return true if this context is owned, otherwise false
     */
    private boolean isContextOwned()
    {
        synchronized (mutex)
        {
            CallerContext currentContext = callerContextManager.getCurrentContext();

            if (creatingCallerContext == currentContext)
            {
                return true;
            }

            // Is an owner if running within this service context (within the
            // selected service)
            if (this == currentContext.get(CallerContext.SERVICE_CONTEXT))
            {
                return true;
            }
            if (log.isDebugEnabled())
            {
                log.debug("context not owned - returning false - callercontext: " + currentContext + ", creating callerContext: " + creatingCallerContext + ": " + getDiagnosticInfo());
            }
        }
        // Not the owner
        return false;
    }

    /**
     * Attempt to re-select previously selected service. If unable to reselect,
     * give up (destroying if destroyOnIdle set).
     */
    private void reselectLastService()
    {
        boolean reselectFailed = false;
        Locator[] locatorsToSelect = null;
        Service serviceToSelect = null;

        synchronized (mutex)
        {
            // check for locators first..if they're set, reselect using them
            if (lastPresentingLocators != null)
            {
                locatorsToSelect = lastPresentingLocators;
                lastPresentingLocators = null;
            }
            else if (lastPresentingService != null)
            {
                serviceToSelect = lastPresentingService;
                lastPresentingService = null;
            }
        }
        if (locatorsToSelect != null)
        {
            try
            {
                if (log.isInfoEnabled())
                {
                    log.info("reselectLastService - locators: " + Arrays.toString(locatorsToSelect)
                            + getDiagnosticInfo());
                }
                // using private select method to ensure selectingCallerContext member is not updated
                doSelect(locatorsToSelect, false);
            }
            catch (InvalidServiceComponentException isce)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to reselect using locators: " + Arrays.toString(locatorsToSelect) + ": " + getDiagnosticInfo(), isce);
                }
                reselectFailed = true;
            }
            catch (InvalidLocatorException ile)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to reselect using locators: " + Arrays.toString(locatorsToSelect) + ": " + getDiagnosticInfo(), ile);
                }
                reselectFailed = true;
            }
        }
        else if (serviceToSelect != null)
        {
            if (log.isInfoEnabled())
            {
                log.info("reselectLastService: " + serviceToSelect + ": " + getDiagnosticInfo());
            }
            // using private select method to ensure selectingCallerContext member is not updated
            doSelect(serviceToSelect, false);
        }
        else
        {
            // no previous service to select, set state to NOT_PRESENTING and
            // check for destroy on idle
            if (log.isDebugEnabled())
            {
                log.debug("no previous presenting service to reselect: " + getDiagnosticInfo());
            }
            reselectFailed = true;
        }

        if (reselectFailed)
        {
            if (log.isInfoEnabled())
            {
                log.info("setting state to NOT_PRESENTING: " + getDiagnosticInfo());
            }
            currentState = STATE_NOT_PRESENTING;
            if (destroyWhenIdle)
            {
                destroy();
            }
        }
    }

    /**
     * Post the specified event to all listeners. Creates a ServiceContextEvent
     * of the type provided by class.
     * 
     * The event must provide a two-argument constructor (ServiceContext, int)
     * 
     * @param serviceContextEventClass
     *            a subclass of ServiceContextEvent
     * @param reasonCode
     *            the reason
     */
    private void notifyPresentationStopped(Class serviceContextEventClass, int reasonCode)
    {
        if (!(PresentationTerminatedEvent.class.equals(serviceContextEventClass) || SelectionFailedEvent.class.equals(serviceContextEventClass)))
        {
            if (log.isWarnEnabled())
            {
                log.warn("postEvent called with unexpected class - ignoring: " + serviceContextEventClass
                        + ", reason: " + reasonCode + ": " + getDiagnosticInfo());
            }
            return;
        }

        // construct the event and call postEvent
        try
        {
            Constructor c = serviceContextEventClass.getDeclaredConstructor(new Class[] { ServiceContext.class,
                    int.class });
            ServiceContextEvent event = (ServiceContextEvent) c.newInstance(new Object[] { this,
                    new Integer(reasonCode) });
            postEvent(event);
        }
        catch (NoSuchMethodException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("unable to get constructor taking a serviceContext and int for class: "
                        + serviceContextEventClass.getName() + ", reason: " + reasonCode + ": " + getDiagnosticInfo(), e);
            }
        }
        catch (InvocationTargetException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("unable to call new instance on constructor taking an int for class: "
                        + serviceContextEventClass.getName() + ", reason: " + reasonCode + ": " + getDiagnosticInfo(), e);
            }
        }
        catch (IllegalAccessException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("unable to call new instance on constructor taking an int for class: "
                        + serviceContextEventClass.getName() + ", reason: " + reasonCode + ": " + getDiagnosticInfo(), e);
            }
        }
        catch (InstantiationException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("unable to call new instance on constructor taking an int for class: "
                        + serviceContextEventClass.getName() + ", reason: " + reasonCode + ": " + getDiagnosticInfo(), e);
            }
        }
    }

    /**
     * Post the ServiceContextEvent
     * 
     * Note: this method can be called -after- the SC is in the DESTROYED state 
     * in order to notify apps of PresentationTerminatedEvent as well as
     * ServiceContextDestroyedEvent 
     * 
     * @param event
     *            the event to post
     */
    protected void postEvent(final ServiceContextEvent event)
    {
        if (log.isDebugEnabled())
        {
            log.debug("postEvent called with " + event + ": " + getDiagnosticInfo());
        }

        if (event instanceof NormalContentEvent)
        {
            if (performanceLog.isInfoEnabled())
            {
                performanceLog.info("Presentation Started: ServiceContext " + instanceServiceContextId);
            }

        }

        // Notify all listeners. Use a local copy of the callerContextMulticaster so it
        // does not
        // change while we are using it.
        CallerContext callerContext;
        synchronized (callerContextMutex)
        {
            callerContext = callerContextMulticaster;
        }
        if (callerContext != null)
        {
            // Execute the runnable in each caller context in callerContextMulticaster
            callerContext.runInContext(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CallerContext cc = callerContextManager.getCurrentContext();
                    CCData data = getCCData(cc);
                    if ((data != null) && (data.listeners != null))
                    {
                        data.listeners.receiveServiceContextEvent(event);
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("data or listeners was null - not notifying of event: " + event + ": "
                                    + getDiagnosticInfo());
                        }
                    }

                    // If the service context has been destroyed, then discard
                    // the ccData
                    // for this caller context. This is done so that the ccData
                    // does not
                    // cause anything it refers to (like this ServiceContext) to
                    // be held onto.
                    if (event instanceof ServiceContextDestroyedEvent)
                    {
                        synchronized (callerContextMutex)
                        {
                            //set each CCData's EventMulticaster/listener to null
                            if (data != null)
                            {
                                data.listeners = null;
                            }
                            cc.removeCallbackData(callerContextMutex);
                        }
                    }
                }
            });
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("No caller context list - unable to post event: " + event + ": " + getDiagnosticInfo());
            }
        }
    }

    /**
     * Send SelectionFailedEvent.INTERRUPTED for each time the event was received.
     */
    private void postInterruptionEvents()
    {
        int pendingCount;
        synchronized(mutex)
        {
            pendingCount = selectCalledWhilePendingCount;
            selectCalledWhilePendingCount = 0;
        }
        if (pendingCount > 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("posting interruption events - count: " + pendingCount + " and resetting selectCalledWhilePendingCount to zero: " + getDiagnosticInfo());
            }
            for (int i=0; i < pendingCount; i++)
            {
                if (log.isInfoEnabled())
                {
                    log.info("posting SelectionFailedEvent(interrupted): " + getDiagnosticInfo());
                }
                postEvent(new SelectionFailedEvent(ServiceContextImpl.this, SelectionFailedEvent.INTERRUPTED));
            }
        }
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context. Create one if this caller context does not have one yet.
     * 
     * @param callerContext
     *            the caller context whose data object is to be returned
     * 
     * @return the data object for the specified caller context
     */
    private CCData getCCData(CallerContext callerContext)
    {
        synchronized (callerContextMutex)
        {
            // Retrieve the data for the caller context
            CCData data = (CCData) callerContext.getCallbackData(callerContextMutex);

            // If a data block has not yet been assigned to this caller context
            // then allocate one and add this caller context to
            // callerContextMulticaster.
            if (data == null)
            {
                data = new CCData();
                callerContext.addCallbackData(data, callerContextMutex);
                // NOTE: it's safe to call add with a null callerContextMulticaster
                // here..
                callerContextMulticaster = CallerContext.Multicaster.add(callerContextMulticaster, callerContext);
                callerContexts.add(callerContext);
            }
            return data;
        }
    }

    protected final String getDiagnosticInfo()
    {
        ServiceContextDelegate delegateLocal = delegate;
        String state = STATE_NAMES[currentState];
        return "Id:[" + instanceServiceContextId + "] [" + state + " - " + (delegateLocal == null ? "no delegate" : delegateLocal.toString()) + "]";
    }

    // Do not acquire a lock in toString (delegate.toString() should also not
    // acquire a lock)
    public String toString()
    {
        return super.toString() + ": " + getDiagnosticInfo();
    }

    private class ServiceContextDelegateListenerImpl implements ServiceContextDelegateListener
    {
        private boolean lastPresentationNormalContent = false;
        private Class lastAlternativeContentClass = null;
        private int NOT_ALTERNATIVE_CONTENT = -1;
        private int lastAlternativeContentReason = NOT_ALTERNATIVE_CONTENT;

        public void presentingAlternativeContent(Class alternativeContentClass, int alternativeContentErrorEventReasonCode)
        {
            synchronized (mutex)
            {
                if (log.isInfoEnabled())
                {
                    log.info("delegate callback - presentingAlternativeContent - class: " + alternativeContentClass.getName() + ", reason  " + alternativeContentErrorEventReasonCode
                            + ": " + getDiagnosticInfo());
                }
                switch (currentState)
                {
                    case STATE_DESTROYED:
                        if (log.isWarnEnabled())
                        {
                            log.warn("ignoring presentingAlternativeContent in DESTROYED: " + getDiagnosticInfo());
                        }
                        // don't change state
                        return;
                    case STATE_PRESENTING_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received presentingAlternativeContent in PRESENTING_PENDING: " + getDiagnosticInfo());
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info("setting state to PRESENTING: " + getDiagnosticInfo());
                        }

                        // always change state before notifying
                        currentState = STATE_PRESENTING;
                        if (log.isInfoEnabled())
                        {
                            log.info("posting AlternativeContentErrorEvent - class: " + alternativeContentClass.getName() + ", reason "
                                    + alternativeContentErrorEventReasonCode + ": " + getDiagnosticInfo());
                        }
                        decrementSelectCalledWhilePending();
                        // support non normal-presentation reason code
                        // notification when presenting
                        postEvent(buildAlternativeContentEvent(alternativeContentClass, alternativeContentErrorEventReasonCode));

                        if (log.isDebugEnabled())
                        {
                            log.debug("setting lastpresentingservice & locators to: " + presentationPendingService
                                    + ", " + Arrays.toString(presentationPendingLocators) + ": " + getDiagnosticInfo());
                        }
                        lastPresentingService = presentationPendingService;
                        lastPresentingLocators = presentationPendingLocators;
                        // null out pending services
                        presentationPendingService = null;
                        presentationPendingLocators = null;

                        lastAlternativeContentClass = alternativeContentClass;
                        lastAlternativeContentReason = alternativeContentErrorEventReasonCode;
                        lastPresentationNormalContent = false;

                        resourceUsage.setRequestedService(lastPresentingService);
                        break;
                    //previous presentation progressed far enough to result in a successful session start before stop was called - post normalcontent
                    case STATE_NEW_SELECT_STOP_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received presentingAlternativeContent in NEW_SELECT_STOP_PENDING: " + getDiagnosticInfo());
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info("posting NormalContentEvent: " + getDiagnosticInfo());
                        }
                        decrementSelectCalledWhilePending();
                        postEvent(new NormalContentEvent(ServiceContextImpl.this));
                        break;
                    //unexpected presenting notification in NOT PRESENTING or USER STOP PENDING
                    case STATE_NOT_PRESENTING:
                    case STATE_USER_STOP_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received unexpected presentingAlternativeContent in NOT_PRESENTING or STOP_PENDING (late notification) - ignoring: " + getDiagnosticInfo());
                        }
                        break;
                    case STATE_PRESENTING:
                        if (log.isInfoEnabled())
                        {
                            log.info("Received presentingAlternativeContent in PRESENTING state - reason "
                                    + alternativeContentErrorEventReasonCode + ": " + getDiagnosticInfo());
                        }

                        // support non normal-presentation reason code
                        // notification when presenting
                        if (log.isInfoEnabled())
                        {
                            log.info("posting AlternativeContentErrorEvent class:" + alternativeContentClass.getName()  + ", reason: "
                                    + alternativeContentErrorEventReasonCode + ": " + getDiagnosticInfo());
                        }
                        postEvent(buildAlternativeContentEvent(alternativeContentClass, alternativeContentErrorEventReasonCode));
                        lastAlternativeContentClass = alternativeContentClass;
                        lastAlternativeContentReason = alternativeContentErrorEventReasonCode;
                        lastPresentationNormalContent = false;

                        break;
                    default:
                        throw new IllegalStateException("received presentingAlternativeContent in unknown state: " + currentState + ": "
                                + getDiagnosticInfo());
                }
            }
        }

        //must be a alternativecontenterrorevent or sublcass
        private AlternativeContentErrorEvent buildAlternativeContentEvent(Class alternativeContentClass, int alternativeContentErrorEventReasonCode)
        {
            try
            {
                Constructor constructor = alternativeContentClass.getDeclaredConstructor(new Class[]{ServiceContext.class, int.class});
                return (AlternativeContentErrorEvent) constructor.newInstance(new Object[]{ServiceContextImpl.this, new Integer(alternativeContentErrorEventReasonCode)});
            }
            catch (NoSuchMethodException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to construct alternative content constructor for class: " + alternativeContentClass.getName() + ": " + getDiagnosticInfo(), e);
                }
            }
            catch (InvocationTargetException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to construct altcontent event for class: " + alternativeContentClass.getName() + ": " + getDiagnosticInfo(), e);
                }
            }
            catch (InstantiationException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to construct altcontent event for class: " + alternativeContentClass.getName() + ": " + getDiagnosticInfo(), e);
                }
            }
            catch (IllegalAccessException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to construct altcontent event for class: " + alternativeContentClass.getName() + ": " + getDiagnosticInfo(), e);
                }
            }
            throw new IllegalArgumentException("Unable to construct class: " + alternativeContentClass.getName() + ": " + getDiagnosticInfo());
        }

        public void presentingNormalContent()
        {
            synchronized (mutex)
            {
                if (log.isInfoEnabled())
                {
                    log.info("delegate callback - presentingNormalContent: " + getDiagnosticInfo());
                }
                switch (currentState)
                {
                    case STATE_DESTROYED:
                        if (log.isWarnEnabled())
                        {
                            log.warn("ignoring presentingNormalContent in DESTROYED: " + getDiagnosticInfo());
                        }
                        // don't change state
                        return;
                    case STATE_PRESENTING_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received presentingNormalContent in PRESENTING_PENDING: " + getDiagnosticInfo());
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info("setting state to PRESENTING: " + getDiagnosticInfo());
                        }

                        // always change state before notifying
                        currentState = STATE_PRESENTING;
                        if (log.isInfoEnabled())
                        {
                            log.info("posting NormalContentEvent: " + getDiagnosticInfo());
                        }
                        decrementSelectCalledWhilePending();
                        postEvent(new NormalContentEvent(ServiceContextImpl.this));

                        if (log.isDebugEnabled())
                        {
                            log.debug("setting lastpresentingservice & locators to: " + presentationPendingService
                                    + ", " + Arrays.toString(presentationPendingLocators) + ": " + getDiagnosticInfo());
                        }
                        lastPresentingService = presentationPendingService;
                        lastPresentingLocators = presentationPendingLocators;
                        // null out pending services
                        presentationPendingService = null;
                        presentationPendingLocators = null;

                        lastAlternativeContentClass = null;
                        lastAlternativeContentReason = NOT_ALTERNATIVE_CONTENT;
                        lastPresentationNormalContent = true;

                        resourceUsage.setRequestedService(lastPresentingService);
                        break;
                    //previous presentation progressed far enough to result in a successful session start before stop was called - post normalcontent
                    case STATE_NEW_SELECT_STOP_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received presentingNormalContent in NEW_SELECT_STOP_PENDING: " + getDiagnosticInfo());
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info("posting NormalContentEvent: " + getDiagnosticInfo());
                        }
                        decrementSelectCalledWhilePending();
                        postEvent(new NormalContentEvent(ServiceContextImpl.this));
                        break;
                    //unexpected presenting notification in NOT PRESENTING or USER STOP PENDING
                    case STATE_NOT_PRESENTING:
                    case STATE_USER_STOP_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received unexpected presentingNormalContent in NOT_PRESENTING or STOP_PENDING (late notification) - ignoring: " + getDiagnosticInfo());
                        }
                        break;
                    case STATE_PRESENTING:
                        if (log.isInfoEnabled())
                        {
                            log.info("Received presentingNormalContent in PRESENTING state: " + getDiagnosticInfo());
                        }
                        if ((null == lastAlternativeContentClass && NOT_ALTERNATIVE_CONTENT == lastAlternativeContentReason))
                        {
                            // we don't expect successful presenting call in
                            // PRESENTING unless we're recovering from
                            // altContentErrorEvent
                            throw new IllegalStateException(
                                    "received presentingNormalContent (NORMAL_PRESENTATION) in PRESENTING: "
                                            + getDiagnosticInfo());
                        }

                        if (log.isInfoEnabled())
                        {
                            log.info("posting NormalContentEvent: " + getDiagnosticInfo());
                        }
                        postEvent(new NormalContentEvent(ServiceContextImpl.this));

                        lastAlternativeContentClass = null;
                        lastAlternativeContentReason = NOT_ALTERNATIVE_CONTENT;
                        lastPresentationNormalContent = true;

                        break;
                    default:
                        throw new IllegalStateException("received presentingNormalContent in unknown state: " + currentState + ": "
                                + getDiagnosticInfo());
                }
            }
        }

        private void decrementSelectCalledWhilePending()
        {
            synchronized(mutex)
            {
                //update interrupted count that will be posted when notPresenting is received
                if (selectCalledWhilePendingCount > 0)
                {
                    selectCalledWhilePendingCount--;
                }
                if (log.isDebugEnabled())
                {
                    log.debug("decrementing selectCalledWhilePendingCount to: " + selectCalledWhilePendingCount);
                }
            }
        }

        public void presentingNoChange()
        {
            synchronized (mutex)
            {
                if (log.isInfoEnabled())
                {
                    log.info("delegate callback - presentingNoChange - reason: " + getDiagnosticInfo());
                }
                switch (currentState)
                {
                    case STATE_DESTROYED:
                        if (log.isWarnEnabled())
                        {
                            log.warn("ignoring presentingNoChange in DESTROYED: " + getDiagnosticInfo());
                        }
                        // don't change state
                        return;
                    case STATE_PRESENTING_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received presentingNoChange in PRESENTING_PENDING: " + getDiagnosticInfo());
                        }

                        // always change state before notifying
                        currentState = STATE_PRESENTING;

                        if (lastPresentationNormalContent)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("no change - re-posting NormalContentEvent: " + getDiagnosticInfo());
                            }
                            decrementSelectCalledWhilePending();
                            postEvent(new NormalContentEvent(ServiceContextImpl.this));
                        }
                        else
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("no change - re-posting AlternativeContentErrorEvent - class: " + lastAlternativeContentClass + ", reason: " + lastAlternativeContentReason + ": " + getDiagnosticInfo());
                            }
                            decrementSelectCalledWhilePending();
                            postEvent(buildAlternativeContentEvent(lastAlternativeContentClass, lastAlternativeContentReason));
                        }

                        if (log.isDebugEnabled())
                        {
                            log.debug("setting lastpresentingservice & locators to: " + presentationPendingService
                                    + ", " + Arrays.toString(presentationPendingLocators) + ": " + getDiagnosticInfo());
                        }
                        lastPresentingService = presentationPendingService;
                        lastPresentingLocators = presentationPendingLocators;
                        // null out pending services
                        presentationPendingService = null;
                        presentationPendingLocators = null;

                        resourceUsage.setRequestedService(lastPresentingService);
                        break;
                    //previous presentation progressed far enough to result in a successful session start before stop was called - post normalcontent
                    case STATE_NEW_SELECT_STOP_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received presentingNoChange in NEW_SELECT_STOP_PENDING: " + getDiagnosticInfo());
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info("posting NormalContentEvent: " + getDiagnosticInfo());
                        }
                        decrementSelectCalledWhilePending();
                        postEvent(new NormalContentEvent(ServiceContextImpl.this));
                        break;
                    //unexpected presentingNoChange notification in NOT PRESENTING or USER STOP PENDING
                    case STATE_NOT_PRESENTING:
                    case STATE_USER_STOP_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received unexpected presentingNoChange in NOT_PRESENTING or STOP_PENDING (late notification) - ignoring: " + getDiagnosticInfo());
                        }
                        break;
                    case STATE_PRESENTING:
                        if (log.isInfoEnabled())
                        {
                            log.info("Received presentingNoChange in PRESENTING state: " + getDiagnosticInfo());
                        }

                        if (lastPresentationNormalContent)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("no change - re-posting NormalContentEvent: " + getDiagnosticInfo());
                            }
                            postEvent(new NormalContentEvent(ServiceContextImpl.this));
                        }
                        else
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("no change - re-posting AlternativeContentErrorEvent - class: " + lastAlternativeContentClass + ", reason: " + lastAlternativeContentReason + ": " + getDiagnosticInfo());
                            }
                            postEvent(buildAlternativeContentEvent(lastAlternativeContentClass, lastAlternativeContentReason));
                        }
                        break;
                    default:
                        throw new IllegalStateException("received presentingNoChange in unknown state: " + currentState + ": "
                                + getDiagnosticInfo());
                }
            }
        }

        // TODO: callback is playerStopping, but nuke that on the interface..no
        // need for stopped
        // TODO: assign a bug
        // assuming we're in systemcontext
        public void notPresenting(Class eventClass, int reasonCode)
        {
            // if state before failed selection was NOT PRESENTING or
            // PRESENTATION PENDING, return to NOT PRESENTING state and post a
            // PresentationTerminatedEvent
            // if state before failed selection was PRESENTING, attempt to
            // return to previous state, or PTE if not possible
            boolean callReselectLastService = false;
            boolean callDelegateDestroy = false;
            boolean callCleanup = false;
            boolean callDoSelect = false;
            Locator[] tempLocatorsToPresent = null;
            Service tempServiceToPresent = null;
            ServiceContextDelegate tempDelegate;
            int entryState;
            boolean isPresentationTerminatedEvent = PresentationTerminatedEvent.class.equals(eventClass);
            boolean isSelectionFailedEvent = SelectionFailedEvent.class.equals(eventClass);
            boolean postServiceContextDestroyedEvent = false;
            boolean notifyPresentationTerminatedAfterSelectionFailed = false;
            int tempStateAtTimeOfSelection;
            synchronized (mutex)
            {
                if (log.isInfoEnabled())
                {
                    log.info("delegate callback - notPresenting - " + eventClass + ", reason: " + reasonCode + ": "
                            + getDiagnosticInfo());
                }
                if (!(isPresentationTerminatedEvent || isSelectionFailedEvent))
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("received notPresenting call with unexpected class: " + eventClass + ", ignoring: " + getDiagnosticInfo());
                    }
                    return;
                }
                tempDelegate = delegate;
                entryState = currentState;
                tempStateAtTimeOfSelection = stateAtTimeOfSelection;
                if (isSelectionFailedEvent
                        && (tempStateAtTimeOfSelection == STATE_NOT_PRESENTING || tempStateAtTimeOfSelection == STATE_PRESENTING_PENDING))
                {
                    notifyPresentationTerminatedAfterSelectionFailed = true;
                }

                switch (currentState)
                {
                    // will be in DESTROYED state when we get the callback if we
                    // were presenting
                    case STATE_DESTROYED:
                        if (log.isInfoEnabled())
                        {
                            log.info("received notPresenting in DESTROYED: " + getDiagnosticInfo());
                        }

                        // received a selectionfailed in the destroyed state

                        // post the event provided by the delegate, destroy the
                        // delegate and post destroyed event
                        notifyPresentationStopped(eventClass, reasonCode);
                        callDelegateDestroy = true;
                        postServiceContextDestroyedEvent = true;
                        callCleanup = true;
                        break;
                    case STATE_NOT_PRESENTING:
                        if (log.isWarnEnabled())
                        {
                            log.warn("received notPresenting in NOT_PRESENTING: " + getDiagnosticInfo());
                        }
                        break;
                    case STATE_NEW_SELECT_STOP_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received notPresenting in NEW_SELECT_STOP_PENDING: " + getDiagnosticInfo());
                        }
                        //previous presentation is stopped - post interruption events and select the pending selection
                        //note the reason for the failure is ignored (the selection was interrupted, post an interruption event)
                        postInterruptionEvents();

                        // now that we've received the notPresenting
                        // notification (from stopping the delegate prior to reselect)
                        // trigger the selection
                        if (log.isInfoEnabled())
                        {
                            log.info("setting state to NOT_PRESENTING: " + getDiagnosticInfo());
                        }

                        currentState = STATE_NOT_PRESENTING;
                        if (log.isInfoEnabled())
                        {
                            log.info("initiating selection of the pending service: " + presentationPendingService
                                    + ", " + Arrays.toString(presentationPendingLocators) + ": " + getDiagnosticInfo());
                        }
                        callDoSelect = true;
                        tempServiceToPresent = presentationPendingService;
                        tempLocatorsToPresent = presentationPendingLocators;
                        break;
                    // if the user stopped, we'd instead be in the
                    // STATE_USER_STOP_PENDING state when we receive the not
                    // presenting
                    // notification
                    case STATE_PRESENTING_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received notPresenting in PRESENTING_PENDING: " + getDiagnosticInfo());
                        }
                        if (isPresentationTerminatedEvent && PresentationTerminatedEvent.USER_STOP == reasonCode)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("ignoring PresentationTerminatedEvent.USER_STOP in PRESENTING_PENDING: " + getDiagnosticInfo());
                            }
                        }
                        else
                        {
                            // this is a normal error state from inability to
                            // select..fire events and reselect last presented
                            // service
                            presentationPendingService = null;
                            presentationPendingLocators = null;
                            if (log.isInfoEnabled())
                            {
                                log.info("posting event type: " + eventClass.getName() + ", reason " + reasonCode
                                        + ": " + getDiagnosticInfo());
                            }

                            notifyPresentationStopped(eventClass, reasonCode);

                            if (log.isInfoEnabled())
                            {
                                log.info("setting state to NOT_PRESENTING: " + getDiagnosticInfo());
                            }
                            currentState = STATE_NOT_PRESENTING;

                            // only reselect if SelectionFailed event was
                            // received
                            callReselectLastService = eventClass.equals(SelectionFailedEvent.class);
                            if (callReselectLastService)
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("initiating reselection of the last presented service: "
                                            + getDiagnosticInfo());
                                }
                            }
                        }
                        break;
                    // we've received an error while presenting
                    case STATE_PRESENTING:
                        if (log.isInfoEnabled())
                        {
                            log.info("received notPresenting in PRESENTING: " + getDiagnosticInfo());
                        }
                        presentationPendingService = null;
                        presentationPendingLocators = null;
                        if (log.isInfoEnabled())
                        {
                            log.info("setting state to NOT_PRESENTING: " + getDiagnosticInfo());
                        }
                        currentState = STATE_NOT_PRESENTING;

                        if (log.isInfoEnabled())
                        {
                            log.info("posting event type: " + eventClass.getName() + ", reason " + reasonCode + ": "
                                    + getDiagnosticInfo());
                        }
                        notifyPresentationStopped(eventClass, reasonCode);
                        if (destroyWhenIdle)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("setting state to DESTROYED: " + getDiagnosticInfo());
                            }
                            currentState = STATE_DESTROYED;
                            callDelegateDestroy = true;
                            if (log.isInfoEnabled())
                            {
                                log.info("posting ServiceContextDestroyedEvent: " + getDiagnosticInfo());
                            }
                            postServiceContextDestroyedEvent = true;
                            callCleanup = true;
                        }
                        // ((ServiceContextResourceUsageImpl)
                        // resourceUsage).setRequestedService(null);
                        break;
                    // user-initiated stop
                    case STATE_USER_STOP_PENDING:
                        if (log.isInfoEnabled())
                        {
                            log.info("notPresenting called in STOP_PENDING: " + getDiagnosticInfo());
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info("setting state to NOT_PRESENTING: " + getDiagnosticInfo());
                        }

                        currentState = STATE_NOT_PRESENTING;
                        if (log.isInfoEnabled())
                        {
                            log.info("posting event type: " + eventClass.getName() + ", reason " + reasonCode + ": "
                                    + getDiagnosticInfo());
                        }
                        notifyPresentationStopped(eventClass, reasonCode);
                        if (destroyWhenIdle)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("setting state to DESTROYED: " + getDiagnosticInfo());
                            }
                            currentState = STATE_DESTROYED;
                            callDelegateDestroy = true;
                            if (log.isInfoEnabled())
                            {
                                log.info("posting ServiceContextDestroyedEvent: " + getDiagnosticInfo());
                            }
                            postServiceContextDestroyedEvent = true;
                            callCleanup = true;
                        }
                        // ((ServiceContextResourceUsageImpl)
                        // resourceUsage).setRequestedService(null);
                        break;
                    default:
                        throw new IllegalStateException("unknown state: " + getDiagnosticInfo());
                }
            }
            if (callReselectLastService)
            {
                reselectLastService();
            }
            if (callDoSelect)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("received notPresenting in " + STATE_NAMES[entryState] + " - calling doSelect: " + getDiagnosticInfo());
                }
                ServiceContextDelegate newDelegate = lookupDelegate(tempServiceToPresent);
                if (newDelegate == null)
                {
                    //TODO: what to do here?
                }
                doSelect(tempServiceToPresent, tempLocatorsToPresent, newDelegate, false);
            }
            if (notifyPresentationTerminatedAfterSelectionFailed)
            {
                if (log.isInfoEnabled())
                {
                    log.info("received selectionFailedEvent following select called in "
                            + STATE_NAMES[tempStateAtTimeOfSelection] + ", will post presentationTerminated event: " + getDiagnosticInfo());
                }

                postPresentationTerminatedEventFollowingSelectionFailedEvent(reasonCode);
            }
            if (callDelegateDestroy)
            {
                // assuming destroy is synchronous
                if (log.isInfoEnabled())
                {
                    log.info("notPresenting - calling destroy on: " + tempDelegate + ": " + getDiagnosticInfo());
                }
                tempDelegate.destroy();
            }
            if (postServiceContextDestroyedEvent)
            {
                if (log.isInfoEnabled())
                {
                    log.info("posting ServiceContextDestroyedEvent: " + getDiagnosticInfo());
                }
                postEvent(new ServiceContextDestroyedEvent(ServiceContextImpl.this));
            }
            if (callCleanup)
            {
                cleanup(true);
            }
        }

        public void playerStarted(ServiceMediaHandler handler)
        {
            if (log.isDebugEnabled())
            {
                log.debug("playerStarted - player: " + handler + ": " + getDiagnosticInfo());
            }
            try
            {
                serviceContextCallbacks.invokeCallbacks(ServiceContextCallback.class.getMethod("notifyPlayerStarted",
                        new Class[]{ServiceContext.class, ServiceMediaHandler.class}), new Object[]{
                        ServiceContextImpl.this, handler});
            }
            catch (Exception e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        }

        public void playerStopping(ServiceMediaHandler handler)
        {
            if (log.isDebugEnabled())
            {
                log.debug("playerStopping - player: " + handler + ": " + getDiagnosticInfo());
            }
            try
            {
                // TODO: the inconsistent naming convention of notifications
                // (started/stopping)..make them consistent?
                serviceContextCallbacks.invokeCallbacks(ServiceContextCallback.class.getMethod("notifyStoppingPlayer",
                        new Class[]{ServiceContext.class, ServiceMediaHandler.class}), new Object[]{
                        ServiceContextImpl.this, handler});
            }
            catch (Exception e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        }

        private void postPresentationTerminatedEventFollowingSelectionFailedEvent(int selectionFailedEventReasonCode)
        {
            int presentationTerminatedEventReasonCode;
            switch (selectionFailedEventReasonCode)
            {
                // won't be triggered (transition instead to alternative
                // content), included for completeness
                case SelectionFailedEvent.CA_REFUSAL:
                    presentationTerminatedEventReasonCode = PresentationTerminatedEvent.ACCESS_WITHDRAWN;
                    break;
                // won't be triggered (transition instead to alternative
                // content), included for completeness
                case SelectionFailedEvent.CONTENT_NOT_FOUND:
                    presentationTerminatedEventReasonCode = PresentationTerminatedEvent.SERVICE_VANISHED;
                    break;
                case SelectionFailedEvent.INSUFFICIENT_RESOURCES:
                    presentationTerminatedEventReasonCode = PresentationTerminatedEvent.RESOURCES_REMOVED;
                    break;
                // won't be triggered (transition instead to alternative
                // content), included for completeness
                case SelectionFailedEvent.MISSING_HANDLER:
                    presentationTerminatedEventReasonCode = PresentationTerminatedEvent.RESOURCES_REMOVED;
                    break;
                // won't be triggered (transition instead to alternative
                // content), included for completeness
                case SelectionFailedEvent.TUNING_FAILURE:
                    presentationTerminatedEventReasonCode = PresentationTerminatedEvent.TUNED_AWAY;
                    break;
                case SelectionFailedEvent.OTHER:
                    presentationTerminatedEventReasonCode = PresentationTerminatedEvent.OTHER;
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn("ignoring selectionFailedEvent code: " + selectionFailedEventReasonCode + ": " + getDiagnosticInfo());
                    }
                    return;
            }
            notifyPresentationStopped(PresentationTerminatedEvent.class, presentationTerminatedEventReasonCode);
        }
    }

    /**
     * Per caller context data
     */
    private class CCData implements CallbackData
    {
        /**
         * CallbackData The listeners is used to keep track of all objects that
         * have registered to be notified of service context events.
         */
        public volatile ServiceContextListener listeners;

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            synchronized (callerContextMutex)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("destroy callerContext: " + cc + ": " + getDiagnosticInfo());
                }
                // TODO(Todd): There may be a race condition here if we ever
                // attempt to send an event after the ccdata object is
                // removed from the callercontext. If this happens, the code
                // that sends the event will call getCCData() which will
                // create a new ccdata object. This new object would have no
                // registered listeners so no events get sent. This
                // additional ccdata object may also be leaked.

                // Remove this caller context from the list then throw away
                // the CCData for it.
                // NOTE: it's safe to call remove with callerContextMulticaster null
                // here
                callerContextMulticaster = CallerContext.Multicaster.remove(callerContextMulticaster, cc);
                cc.removeCallbackData(callerContextMutex);
                listeners = null;

                // Check if the callerContext (cc) is same as the ownerCC?
                // Reset the video transformation to platform default
                persistentSettings.setVideoTransformation(platformDefaultVideoTransformation);
            }
        }
    }

    private final class ServiceContextLock
    {
        //no impl, just created to differentiate from other locks
    }
}
