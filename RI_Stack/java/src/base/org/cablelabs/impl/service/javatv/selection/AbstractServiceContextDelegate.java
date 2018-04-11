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

import javax.tv.locator.Locator;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.manager.application.InitialAutostartAppsStartedListener;
import org.cablelabs.impl.service.ServiceContextResourceUsageImpl;
import org.davic.net.tuning.NetworkInterface;
import org.ocap.service.AbstractService;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServicesDatabase;

/**
 * A <code>ServiceContextDelegate</code> responsible for presenting an
 * AbstractService.
 */
public class AbstractServiceContextDelegate implements ServiceContextDelegate
{
    private static final Logger log = Logger.getLogger(AbstractServiceContextDelegate.class);

    private ServiceContextDelegateListener serviceContextDelegateListener;

    private AppDomain appDomain;

    private static final int INVALID_ABSTRACT_SERVICE = -1;
    private int serviceID = INVALID_ABSTRACT_SERVICE;

    private boolean initialized;

    // do not null this out
    private Object lock;

    private static final ServicesDatabase servicesDatabase = ((ServiceManager) ManagerManager.getInstance(ServiceManager.class)).getServicesDatabase();
    private final InitialAutostartAppsStartedListener initialAutostartAppsStartedListener = new InitialAutostartAppsStartedListenerImpl();
    //no need to use 'this' in the identity hash lookup, just a unique identifier
    private final String id = "Id: 0x" + Integer.toHexString(System.identityHashCode(new Object())).toUpperCase() + ": ";

    //values provided in prepareToPresent
    private Service initialService;
    private long initialSequence;

    // TODO: OCAP spec says a service marked for removal should trigger
    // InvalidServiceComponentException, but this isn't possible
    // when the selection is via service instead of locator. Spec issue:
    // OCSPEC-212
    /**
     * Initialization method. Must be called before <link>@Present</link> is
     * called.
     * 
     * @param serviceContextDelegateListener
     *            the listener that will receive notifications from this
     *            delegate
     * @param creationDelegate
     *            responsible for creating the mediahandler (player)
     * @param appDomain
     *            app domain
     * @param persistentVideoModeSettings
     *            settings
     * @param lock
     *            the shared lock
     */
    public synchronized void initialize(ServiceContextDelegateListener serviceContextDelegateListener,
            ServiceMediaHandlerCreationDelegate creationDelegate, AppDomain appDomain,
            PersistentVideoModeSettings persistentVideoModeSettings, Object lock)
    {
        // NOTE: persistent video mode settings, owner caller context and
        // resourceusage not used
        synchronized (lock)
        {
            this.serviceContextDelegateListener = serviceContextDelegateListener;
            this.appDomain = appDomain;
            this.lock = lock;
            initialized = true;
            if (log.isDebugEnabled())
            {
                log.debug(id + "initialized");
            }
        }
    }

    public NetworkInterface getNetworkInterface()
    {
        // abstract service doesn't use this
        return null;
    }

    public Service getService()
    {
        assertInitialized();
        synchronized (lock)
        {
            if (serviceID != INVALID_ABSTRACT_SERVICE)
            {
                return servicesDatabase.getAbstractService(serviceID);
            }
        }
        
        return null;
    }

    public ServiceMediaHandler getServiceMediaHandler()
    {
        // abstract service doesn't use a ServiceMediaHandler
        return null;
    }

    public boolean isPresenting(Service abstractService)
    {
        assertInitialized();
        int tempServiceID;
        synchronized (lock)
        {
            tempServiceID = this.serviceID;
        }
        
        return tempServiceID != INVALID_ABSTRACT_SERVICE &&
               ((OcapLocator)abstractService.getLocator()).getSourceID() == tempServiceID;
    }

    public boolean canPresent(Service service)
    {
        boolean result = (service instanceof AbstractService);
        if (log.isDebugEnabled())
        {
            log.debug(id + "canPresent: " + service + ": " + result);
        }

        return result;
    }

    public void stopPresenting(long presentSequence)
    {
        assertInitialized();
        if (presentSequence != initialSequence)
        {
            //TODO: FAIL
            return;
        }
        if (log.isInfoEnabled())
        {
            log.info(id + "stopPresenting - sequence: " + presentSequence);
        }
        doStopPresenting(PresentationTerminatedEvent.USER_STOP);
    }

    /**
     * Stop presenting the abstract service
     */
    public void stopPresentingAbstractService()
    {
        if (log.isInfoEnabled())
        {
            log.info(id + "stopPresentingAbstractService");
        }
        assertInitialized();
        doStopPresenting(PresentationTerminatedEvent.SERVICE_VANISHED);
    }

    // assumes permission checks were performed outside the delegate
    public ServiceContentHandler[] getServiceContentHandlers()
    {
        assertInitialized();
        AppDomain tempAppDomain;
        synchronized (lock)
        {
            // for an abstractservice, the only service content handlers are
            // held by
            // the appdomain
            tempAppDomain = appDomain;
        }
        return tempAppDomain.getServiceContentHandlers();
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
                log.info(id + "prepareToPresent - service: " + newService + ", current sequence: " + initialSequence + ", new sequence: " + sequence);
            }
            this.initialService = newService;
            this.initialSequence = sequence;
            //component locators unused
            //serviceContext resource usage unused
            //callerContext is unused
        }
    }

    public void present(long presentSequence)
    {
        assertInitialized();
        if (presentSequence != initialSequence)
        {
            //TODO: FAIL
            return;
        }
        if (!(initialService instanceof AbstractService))
        {
            throw new IllegalArgumentException("service is not abstract: " + initialService);
        }
        if (log.isInfoEnabled())
        {
            log.info(id + "present - sequence: " + presentSequence);
        }
        doPresent(((OcapLocator)initialService.getLocator()).getSourceID());
    }

    public synchronized void destroy()
    {
        if (initialized)
        {
            synchronized (lock)
            {
                serviceContextDelegateListener = null;
                appDomain = null;
            }
        }
        initialized = false;
    }

    private void doStopPresenting(int reason)
    {
        assertInitialized();
        // acquire refs while holding the lock but don't call into
        // servicesdatabase while holding the lock
        AppDomain tempAppDomain;
        int tempAbstractServiceID;
        synchronized (lock)
        {
            tempAppDomain = appDomain;
            tempAbstractServiceID = serviceID;
            serviceID = INVALID_ABSTRACT_SERVICE;
        }
        servicesDatabase.removeSelectedService(tempAbstractServiceID);
        if (log.isDebugEnabled())
        {
            log.debug(id + "service removed from database, about to stop appDomain");
        }

        tempAppDomain.stop();
        if (log.isDebugEnabled())
        {
            log.debug(id + "appDomain stopped, notifying not presenting");
        }
        notifyNotPresenting(PresentationTerminatedEvent.class, reason);
    }

    private synchronized void assertInitialized()
    {
        if (!initialized)
        {
            throw new IllegalStateException("initialize not called before calling present");
        }
    }

    /**
     * Helper method supporting service presentation
     * 
     * @param newAbstractServiceID
     *            the service to present
     * 
     * @throws IllegalArgumentException
     *             if abstract service is null
     */
    // NOTE: AbstractServices can only present a single service/locator - passing
    // in locators doesn't make sense
    private void doPresent(int newAbstractServiceID)
    {
        if (newAbstractServiceID == INVALID_ABSTRACT_SERVICE)
        {
            throw new IllegalArgumentException("abstractService may not be null");
        }
        ServiceContextFactory serviceContextFactory = ServiceContextFactory.getInstance();
        ServiceContext[] serviceContexts = serviceContextFactory.getServiceContexts();
        // verify service is not already being presented by a servicecontext
        // if service is already being presented, trigger selectionFailed
        // (reason not defined in spec, so we're using insufficient resources)
        // must be in systemcontext to see all service contexts, relying on that
        // being the case here
        for (int i = 0; i < serviceContexts.length; i++)
        {
            Service serviceToExamine = serviceContexts[i].getService();
            if (log.isDebugEnabled())
            {
                log.debug(id + "Examining: " + serviceContexts[i] + ", service: " + serviceToExamine);
            }
            // serviceToExamine may be null (for example, this servicecontext is
            // not yet presenting)
            if (serviceToExamine != null &&
                ((OcapLocator)serviceToExamine.getLocator()).getSourceID() == newAbstractServiceID)
            {
                if (log.isInfoEnabled())
                {
                    log.info(id + "abstract service already being presented - unable to select service ID: " + serviceToExamine);
                }
                notifyNotPresenting(SelectionFailedEvent.class, SelectionFailedEvent.INSUFFICIENT_RESOURCES);
                return;
            }
        }

        // should NOT get here if service is marked (should have thrown an
        // exception instead - see TODO above)
        if (servicesDatabase.isServiceMarked(newAbstractServiceID))
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "service marked for removal - not presenting ID: " + newAbstractServiceID);
            }
            notifyNotPresenting(SelectionFailedEvent.class, SelectionFailedEvent.CONTENT_NOT_FOUND);
            return;
        }

        AbstractService service;
        if ((service = servicesDatabase.addSelectedService(newAbstractServiceID)) == null)
        {
            notifyNotPresenting(SelectionFailedEvent.class, SelectionFailedEvent.INSUFFICIENT_RESOURCES);
            if (log.isInfoEnabled())
            {
                log.info(id + "unable to add service to database. ID: " + newAbstractServiceID);
            }
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug(id + "service added to database. ID: " + newAbstractServiceID);
        }

        AppDomain tempAppDomain;
        synchronized (lock)
        {
            tempAppDomain = appDomain;
            serviceID = newAbstractServiceID;
        }
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + "about to select details in appDomain - service ID: " + newAbstractServiceID);
            }
            ServiceDetails details = ((ServiceExt)service).getDetails();
            // stop bound apps before selecting
            tempAppDomain.stopBoundApps();
            tempAppDomain.select(details, initialAutostartAppsStartedListener);
            // successful select
            if (log.isDebugEnabled())
            {
                log.debug(id + "service selected, will notify normalcontent when all autostart apps are started - service ID: " + newAbstractServiceID);
            }
        }
        catch (SIRequestException sire)
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "unable to present - request exception", sire);
            }
            notifyNotPresenting(SelectionFailedEvent.class, SelectionFailedEvent.CONTENT_NOT_FOUND);
        }
        catch (InterruptedException ie)
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "unable to present - interrupted", ie);
            }
            notifyNotPresenting(SelectionFailedEvent.class, SelectionFailedEvent.CONTENT_NOT_FOUND);
        }
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

    private void notifyNotPresenting(Class eventClass, int reasonCode)
    {
        ServiceContextDelegateListener tempServiceContextDelegateListener;
        synchronized (lock)
        {
            tempServiceContextDelegateListener = serviceContextDelegateListener;
        }
        tempServiceContextDelegateListener.notPresenting(eventClass, reasonCode);
    }

    public String toString()
    {
        return "AbstractServiceContextDelegate: " + id + "service ID: " + serviceID;
    }

    private class InitialAutostartAppsStartedListenerImpl implements InitialAutostartAppsStartedListener
    {
        public void initialAutostartAppsStarted()
        {
            if (log.isInfoEnabled())
            {
                log.info(id + "received initialAutostartAppsStarted notification - posting normalcontent event");
            }
            notifyNormalContent();
        }
    }
}
