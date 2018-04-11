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

import java.io.IOException;

import javax.media.IncompatibleSourceException;
import javax.media.IncompatibleTimeBaseException;
import javax.media.TimeBase;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.media.player.AlternativeContentServiceMediaHandler;
import org.cablelabs.impl.media.player.ServicePlayer;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.SystemEventUtil;
import org.dvb.application.AppID;
import org.dvb.application.AppsDatabase;
import org.ocap.application.OcapAppAttributes;
import org.ocap.service.AlternativeContentErrorEvent;

/**
 * Supports creation of ServiceMediaHandlers - can be used by any {@link
 * ServiceContextDelegate#} to create a media handler.
 */
public class ServiceMediaHandlerCreationDelegate
{
    private static final Logger log = Logger.getLogger(ServiceMediaHandlerCreationDelegate.class);

    private ServiceContextExt serviceContextExt;

    private PersistentVideoModeSettings persistentVideoModeSettings;

    private Object lock;

    public ServiceMediaHandlerCreationDelegate(ServiceContextExt serviceContextExt,
            PersistentVideoModeSettings persistentVideoModeSettings, Object lock)
    {
        if (serviceContextExt == null || persistentVideoModeSettings == null || lock == null)
        {
            throw new IllegalArgumentException("null values not allowed in constructor: serviceContext"
                    + serviceContextExt + ", videosettings: " + persistentVideoModeSettings + ", lock: " + lock);
        }
        this.serviceContextExt = serviceContextExt;
        this.persistentVideoModeSettings = persistentVideoModeSettings;
        this.lock = lock;
    }

    public ServiceMediaHandler createAlternativeContentMediaHandler(Class alternativeContentClass, int alternativeContentReasonCode, ResourceUsageImpl resourceUsage)
    {
        ServiceMediaHandler handler = new AlternativeContentServiceMediaHandler(serviceContextExt.getCallerContext(), lock, resourceUsage, alternativeContentClass, alternativeContentReasonCode);

        if (log.isDebugEnabled())
        {
            log.debug("createAlternativeContentMediaHandler - reason: " + alternativeContentReasonCode);
        }
        try
        {
            //null servicedetails and locators, but must be called
            ((ServicePlayer)handler).setInitialSelection(null, null);
        }
        catch (InvalidLocatorException e)
        {
            //ignore
        }
        catch (InvalidServiceComponentException e)
        {
            //ignore
        }

        if (log.isDebugEnabled())
        {
            log.debug("service media handler created: " + handler);
        }

        applyVideoTransformation(handler);
        return handler;
    }

    /**
     * Create a ServiceMediaHandler for the provided serviceDetails and
     * component locators. Returns a not-yet-started ServiceMediaHandler
     * 
     * @param serviceDetails
     *            The service to present
     * @param componentLocators
     *            may be null if serviceDetails is provided
     * @param resourceUsage
     *            the resourceUsage
     * @param extendedNetworkInterface
     *            network interface
     * @param timeBase
     *            the timebase to set on the player (if null, no timebase is
     *            set)
     * @return The media handler used to present the media components for this
     *         service. If a media handler for presenting the media components
     *         cannot be created, a media handler which only presents
     *         AlternativeContent will be returned.
     */
    public ServiceMediaHandler createServiceMediaHandler(ServiceDetails serviceDetails, Locator[] componentLocators,
            ResourceUsageImpl resourceUsage, ExtendedNetworkInterface extendedNetworkInterface, TimeBase timeBase)
    {
        if (log.isInfoEnabled())
        {
            log.info("createServiceMediaHandler - details: " + serviceDetails + ", locators: "
                    + Arrays.toString(componentLocators) + ", networkInterface: " + extendedNetworkInterface);
        }

        try
        {
            ServiceMediaHandler handler = doCreateServiceMediaHandler(serviceDetails, resourceUsage,
                    extendedNetworkInterface, timeBase);

            //ensures all players created via ServiceContext have the initial selection set
            ((ServicePlayer) handler).setInitialSelection(serviceDetails, componentLocators);

            applyVideoTransformation(handler);

            return handler;
        }
        catch (Exception x)
        {
            SystemEventUtil.logRecoverableError(x);
            if (log.isDebugEnabled())
            {
                log.debug("unable to select components - creating alternativecontent mediahandler", x);
            }
            return createAlternativeContentMediaHandler(AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.MISSING_HANDLER, resourceUsage);
        }
    }

    private void applyVideoTransformation(ServiceMediaHandler handler)
    {
        CallerContextManager ccManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        // Apply the default video transformation (ECN 1040)
        CallerContext callerContext = ccManager.getCurrentContext();
        boolean applyVT = false;
        OcapAppAttributes appAttr = null;
        AppsDatabase db = (AppsDatabase) callerContext.get(AppsDatabase.class);

        // Get the AppAttributes associated with the AppID. These will
        // be an instance of OcapAppAttributes
        if (db != null)
        {
            appAttr = (OcapAppAttributes) db.getAppAttributes((AppID) callerContext.get(CallerContext.APP_ID));
        }
        // If the caller is running in this service context and is
        // service_bound,
        // then video transformation should not be applied.
        boolean serviceBound = false;
        if (appAttr != null)
        {
            serviceBound = (this == callerContext.get(CallerContext.SERVICE_CONTEXT) && appAttr.getIsServiceBound());
        }

        if (log.isDebugEnabled())
        {
            log.debug("serviceBound flag: " + serviceBound);
        }

        // If the current caller is not running within this service context or
        // if its not service bound
        // then apply the video transformation
        if ((this != callerContext.get(CallerContext.SERVICE_CONTEXT) || !serviceBound))
        {
            if (log.isDebugEnabled())
            {
                log.debug("setting applyVT to true");
            }
            // 'ComponentParent' and 'ComponentRectangle' parameters of
            // 'persistentVideoModeSettings'
            // are set to null in this case
            applyVT = true;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("not setting applyVT to true");
            }

        }

        // Check for persistent video settings enabled.
        if ((persistentVideoModeSettings.isEnabled()) || applyVT)
        {
            if (log.isDebugEnabled())
            {
                log.debug("applying persistent settings");
            }

            ((ServicePlayer) handler).setInitialVideoSize(persistentVideoModeSettings.getVideoTransformation(),
                    persistentVideoModeSettings.getComponentParent(),
                    persistentVideoModeSettings.getComponentRectangle());
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("persistent settings not enabled and not applyVT");
            }
        }
    }

    /**
     * Create datasource and serviceMediaHandler, connect datasource
     * 
     * @param serviceDetails
     *            service details used to create the handler and datasource
     * 
     * @param resourceUsage
     *            the resourceUsage
     * @param networkInterface
     *            networkInterface
     * @param timeBase
     *            timeBase
     * @return service media handler
     * 
     * @throws IOException
     *             if dataSource unable to connect
     * @throws IncompatibleSourceException
     *             if handler and datasource are not compatible
     * @throws IllegalArgumentException
     *             in some cases if dataSource cannot be created
     * @throws IncompatibleTimeBaseException
     *             if timebase is unsupported by the player clock
     */
    private ServiceMediaHandler doCreateServiceMediaHandler(ServiceDetails serviceDetails,
            ResourceUsageImpl resourceUsage, ExtendedNetworkInterface networkInterface, TimeBase timeBase)
            throws IOException, IncompatibleSourceException, IncompatibleTimeBaseException
    {
        ServiceManager serviceManager = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        ServiceDataSource dataSrc = serviceManager.createServiceDataSource(serviceContextExt,
                serviceDetails.getService());
        dataSrc.setService(serviceDetails.getService());
        dataSrc.connect();

        // Create the ServiceMediaHandler and initialize it.
        ServiceMediaHandler handler = serviceManager.createServiceMediaHandler(dataSrc, serviceContextExt, lock, resourceUsage);
        //all players created via this mechanism are implementations of ServicePlayer
        ((ServicePlayer)handler).setNetworkInterface(networkInterface);
        if (log.isDebugEnabled())
        {
            log.debug("service media handler created: " + handler);
        }
        // setTimeBase must be called prior to setSource, since setSource will
        // start the clock - only set if not null
        if (timeBase != null)
        {
            handler.setTimeBase(timeBase);
        }

        handler.setSource(dataSrc);

        return handler;
    }
}
