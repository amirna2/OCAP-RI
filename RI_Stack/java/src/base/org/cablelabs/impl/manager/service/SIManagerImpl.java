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

package org.cablelabs.impl.manager.service;

import java.util.Iterator;
import java.util.List;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.RatingDimension;
import javax.tv.service.ReadPermission;
import javax.tv.service.SIException;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.PropertiesManager;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.javatv.navigation.ServiceListImpl;
import org.cablelabs.impl.spi.ProviderRegistryExt;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.spi.SPIServiceDetails;
import org.cablelabs.impl.util.LocatorUtil;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * Default implementation of <code>SIManager</code>.
 * 
 * @author Todd Earles
 */
public class SIManagerImpl extends SIManagerExt
{
    // Default language
    private static final String DEFAULT_LANGUAGE = null;

    // Current language for this SIManager instance
    private String language = DEFAULT_LANGUAGE;

    // The ServiceManager
    private final ServiceManager serviceManager;

    // The SI cache
    private final SICache siCache;

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIManagerImpl.class.getName());

    private static final String DELEGATE_PARAM_PREFIX = "OCAP.SIMgrDelegate";
    
    private static ProviderRegistryExt providerRegistryExt = (ProviderRegistryExt) (ProviderRegistryExt.getInstance());

    private final List siManagerDelegates;
    /**
     * Constructs an <code>SIManager</code> object.
     */
    public SIManagerImpl()
    {
        serviceManager = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        siCache = serviceManager.getSICache();
        siManagerDelegates = PropertiesManager.getInstance().getInstancesByPrecedence(DELEGATE_PARAM_PREFIX);
    }

    // Description copied from SIManager
    public void setPreferredLanguage(String language)
    {
        this.language = language;
    }

    // Description copied from SIManager
    public String getPreferredLanguage()
    {
        return language;
    }

    // Description copied from SIManager
    public void registerInterest(Locator locator, boolean active) throws InvalidLocatorException, SecurityException
    {
        // Check locator
        if (locator == null) throw new NullPointerException();

        // TODO(Todd): Should re-try acquisition of SI until interest is no
        // longer active.

        // Call cache to register interest in a signaled service
        siCache.registerInterest(locator, active);
    }

    // Description copied from SIManager
    public java.lang.String[] getSupportedDimensions()
    {
        return siCache.getSupportedDimensions(language);
    }

    // Description copied from SIManager
    public RatingDimension getRatingDimension(String name) throws SIException
    {
        if (name == null) throw new NullPointerException();
        return siCache.getRatingDimension(name, language);
    }

    // Description copied from SIManager
    public Transport[] getTransports()
    {
        return siCache.getTransports();
    }

    // Description copied from SIManager
    public SIRequest retrieveSIElement(Locator locator, final SIRequestor requestor) throws InvalidLocatorException,
            SecurityException
    {
        // Check for Null parameters
        if (requestor == null || locator == null) throw new NullPointerException("null parameters not allowed");

        // If this is a request for a provider supplied element, then handle it.
        if (LocatorUtil.isService(locator))
        {
            OcapLocator loc = LocatorUtil.convertJavaTVLocatorToOcapLocator(locator);
            SPIService service = getSPIService(loc);
            if (service != null)
            {
                // If providerFirst is false, then check for signaled SI
                // element(s) first.
                if (!service.getProviderFirst())
                {
                    try
                    {
                        // If the specified locator references a signaled
                        // service, then
                        // handle this as a request for signaled SI element(s).
                        siCache.getService(locator, language);
                        return siCache.retrieveSIElement(locator, language, requestor);
                    }
                    catch (Exception x)
                    {
                        // Did not find the signaled service so fall through and
                        // satisfy the
                        // request with SPI element(s).
                    }
                }

                // Handle the request for SPI service component(s) or service
                // details
                if (LocatorUtil.isServiceComponent(locator))
                {
                    // Handle request for SPI service component(s)
                    try
                    {
                        SPIServiceDetails details = (SPIServiceDetails) service.getDetails();
                        return details.retrieveComponents(loc.getPIDs(), loc.getComponentNames(),
                                loc.getComponentTags(), loc.getStreamTypes(), loc.getIndexes(), loc.getLanguageCodes(),
                                requestor);
                    }
                    catch (Exception x)
                    {
                        throw new InvalidLocatorException(loc, "Could not retrieve components due to " + x);
                    }
                }
                else
                {
                    // Handle request for SPI service details
                    return service.retrieveDetails(requestor);
                }
            }
            else
            { // It's a broadcast Service, but not SPI. Ask the cache to resolve it
                return siCache.retrieveSIElement(locator, language, requestor);
            }
        }

        // We're dealing with a non-broadcast non-SPI Service
        // Note: non-broadcast Locators should not try to resemble broadcast Locators. 
        
        // Walk through the SIManagerDelegates and see if any of them recognize the provided Locator
        Service serviceFromDelegate = null;
        for (Iterator iter = siManagerDelegates.iterator(); iter.hasNext();)
        {
            final SIManagerDelegate siDelegate = ((SIManagerDelegate) iter.next());
            serviceFromDelegate = siDelegate.getService(locator);
            if (serviceFromDelegate != null)
            {
                ServiceDetailsExt sd = null;
                try
                {
                    sd = (ServiceDetailsExt)(((ServiceExt)serviceFromDelegate).getDetails());
                    return sd.retrieveElementByLocator(requestor, locator);
                }
                catch (Exception e)
                { // No dice getting a SD that matches the locator. 
                    if (log.isDebugEnabled())
                    {
                        log.debug( "Error retrieving ServiceDetails for " + locator 
                                   + " on " + siDelegate + ", continuing...", e );
                    }
                }
            }
        } // END for (delegates)
        
        throw new InvalidLocatorException(locator, "Locator " + locator + " does not reference a retrievable SI object");
    }

    public Service getService(Locator locator) throws InvalidLocatorException, SecurityException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Constructing Service object for " + locator);
        }

        // Throw NullPointerException if we are not given any input parameters
        if (locator == null)
        {
            throw new NullPointerException("null locator not allowed");
        }

        // Walk through the SIManagerDelegates and see if any of them recognize the provided Locator
        for (Iterator iter = siManagerDelegates.iterator(); iter.hasNext();)
        {
            Service service = ((SIManagerDelegate) iter.next()).getService(locator);
            if (service != null)
            {
                return service;
            }
        }

        // delegates did not return a service..

        // Must be a proper broadcast Service locator
        if (!(LocatorUtil.isService(locator)))
        {
            throw new InvalidLocatorException(locator, "Locator was not an OCAP Service");
        }

        // Check for a provider supplied service and return it if one is found
        OcapLocator ocapLocator = LocatorUtil.convertJavaTVLocatorToOcapLocator(locator);
        SPIService spiService = getSPIService(ocapLocator);
        if (spiService != null)
        {
            // If providerFirst is false, then check for a signaled service
            // first.
            if (!spiService.getProviderFirst())
            {
                try
                {
                    return siCache.getService(locator, language);
                }
                catch (Exception x)
                {
                    // Did not find the signaled service so fall through and
                    // satisfy the
                    // request with the SPI service.
                }
            }
            return spiService;
        }

        // Construct the service object
        return siCache.getService(locator, language);
    }

    // Description copied from SIManagerExt
    public Service getService(int serviceNumber, int minorNumber) throws SIException
    {
        if (log.isDebugEnabled())
        {
            log.debug("1 SIManagerImpl::getService() called with " + serviceNumber + ", " + minorNumber + ", "
                    + language);
        }
        try
        {
            return siCache.getService(serviceNumber, minorNumber, language);
        }
        catch (SIDatabaseException ex)
        {
            throw new SIException("Cannot get service due to " + ex);
        }
    }

    // Description copied from SIManagerExt
    public Service getService(short majorChannelNumber, short minorChannelNumber) throws SIException
    {
        // Compute service number based on SCTE 65 channel number
        final int majorServiceNumber;
        final int minorServiceNumber;
        if (log.isDebugEnabled())
        {
            log.debug("2 SIManagerImpl::getService() called with " + majorChannelNumber + ", " + minorChannelNumber
                    + ", " + language);
        }

        if ((majorChannelNumber & 0x3f0) == 0x3f0)
        {
            // One part channel number
            majorServiceNumber = ((majorChannelNumber & 0x00f) << 10) + minorChannelNumber;
            minorServiceNumber = -1;
        }
        else
        {
            // Two part channel number
            majorServiceNumber = majorChannelNumber;
            minorServiceNumber = minorChannelNumber;
        }

        // Get the service
        try
        {
            return siCache.getService(majorServiceNumber, minorServiceNumber, language);
        }
        catch (SIDatabaseException ex)
        {
            throw new SIException("Cannot get service due to " + ex);
        }
    }

    // Description copied from SIManagerExt

    public Service getServiceByAppId(int appId) throws SIException
    {
        // Compute service number based on SCTE 65 channel number
        if (log.isDebugEnabled())
        {
            log.debug("2 SIManagerImpl::getServiceByAppId() called with " + appId + ", " + ", " + language);
        }

        // Get the service
        try
        {
            return siCache.getService(appId, language);
        }
        catch (SIDatabaseException ex)
        {
            throw new SIException("Cannot get service due to " + ex);
        }
    }

    /**
     * Return the provider supplied service if the specified locator refers to
     * one. Otherwise, return null.
     */
    private SPIService getSPIService(OcapLocator locator)
    {
        // Get scheme and ensure it is "ocap"
        String s = locator.toExternalForm();
        String scheme = s.substring(0, s.indexOf(':'));
        if (!scheme.equals("ocap")) return null;

        // Lookup the service based on the "ocap" scheme
        int sourceID = locator.getSourceID();
        String serviceName = locator.getServiceName();
        
        if (sourceID != -1)
            return (SPIService) providerRegistryExt.getService(scheme, sourceID, language);
        else if (serviceName != null)
            return (SPIService) providerRegistryExt.getService(scheme, serviceName, language);
        else
            return null;
    }

    // Description copied from SIManager
    public SIRequest retrieveServiceDetails(Locator locator, SIRequestor requestor) throws InvalidLocatorException,
            SecurityException
    {
        // Check requestor for null so it is detected right away
        if (requestor == null || locator == null) throw new NullPointerException("null parameters not allowed");

        // Must be a proper Service locator
        if (!(LocatorUtil.isService(locator)))
            throw new InvalidLocatorException(locator, "Locator was not an OCAP Service");

        // Check for a provider supplied service and return its service details
        // if found
        OcapLocator loc = LocatorUtil.convertJavaTVLocatorToOcapLocator(locator);
        SPIService spiService = getSPIService(loc);
        if (spiService != null)
        {
            // If providerFirst is false, then check for a signaled service
            // first.
            if (!spiService.getProviderFirst())
            {
                try
                {
                    Service service = siCache.getService(locator, language);
                    return siCache.retrieveServiceDetails(service, language, true, requestor);
                }
                catch (Exception x)
                {
                    // Did not find the signaled service so fall through and
                    // satisfy the
                    // request with the SPI service details.
                }
            }
            return spiService.retrieveDetails(requestor);
        }

        // Return all service details for this service
        Service service = siCache.getService(locator, language);
        return siCache.retrieveServiceDetails(service, language, true, requestor);
    }

    // Description copied from SIManager
    public SIRequest retrieveProgramEvent(Locator locator, SIRequestor requestor) throws InvalidLocatorException,
            SecurityException
    {
        // Check for Null parameters
        if (requestor == null || locator == null) throw new NullPointerException("null parameters not allowed");

        // Must be a proper Program Event locator
        if (!(LocatorUtil.isProgramEvent(locator)))
            throw new InvalidLocatorException(locator, "Locator was not an OCAP Program Event");

        // TODO(Todd): Implement when we support program events.
        SecurityUtil.checkPermission(new ReadPermission(locator));
        throw new InvalidLocatorException(locator,
                "retrieveProgramEvent is not currently implemented - no program events found for this locator");
    }

    // Description copied from SIManager
    public ServiceList filterServices(ServiceFilter filter)
    {
        // The collection of all services
        ServiceCollection collection = new ServiceCollection();

        // Add abstract services to the collection
        serviceManager.getServicesDatabase().getAbstractServices(collection);

        // Add provider supplied services marked as providerFirst to the
        // collection
        providerRegistryExt.getAllServices(collection, true, language);

        // Add broadcast services to the collection
        siCache.getAllServices(collection, language);

        // Add provider supplied services not marked as providerFirst to the
        // collection
        providerRegistryExt.getAllServices(collection, false, language);

        // Return the service list
        return new ServiceListImpl(collection.getServices(), filter);
    }

    // Description copied from SIManagerExt
    public SIRequest retrieveTransportStream(Locator locator, final SIRequestor requestor)
            throws InvalidLocatorException, SecurityException
    {
        // Check for a provider supplied service and return its transport stream
        // if found
        if (LocatorUtil.isService(locator))
        {
            // Get the transport stream
            OcapLocator loc = LocatorUtil.convertJavaTVLocatorToOcapLocator(locator);
            SPIService spiService = getSPIService(loc);
            final TransportStream[] ts = new TransportStream[] { null };
            if (spiService != null)
            {
                // If providerFirst is false, then check for a signaled service
                // first.
                if (!spiService.getProviderFirst())
                {
                    try
                    {
                        siCache.getService(locator, language);
                        return siCache.retrieveTransportStream(locator, requestor);
                    }
                    catch (Exception x)
                    {
                        // Did not find the signaled service so fall through and
                        // satisfy the
                        // request with the SPI transport stream.
                    }
                }

                // Get the transport stream from the SPI service details
                try
                {
                    ServiceDetailsExt details = (ServiceDetailsExt) spiService.getDetails();
                    ts[0] = details.getTransportStream();
                }
                catch (Exception x)
                {/* treat as if not mapped */
                }
            }

            // Only if we found a transport stream
            if (ts[0] != null)
            {
                // Create the request object
                SIRequest request = new SIRequest()
                {
                    public boolean cancel()
                    {
                        return false;
                    }
                };

                // Call the requestor with the transport stream
                CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                ccm.getCurrentContext().runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        requestor.notifySuccess(ts);
                    }
                });

                return request;
            }
        }

        return siCache.retrieveTransportStream(locator, requestor);
    }
}
