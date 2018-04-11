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

package org.cablelabs.impl.ocap.hn;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.tv.service.SIChangeType;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.ServiceType;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.ServiceDetailsChangeEvent;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.service.ServiceCollection;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceDetailsCallback;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.ProviderRegistryExt;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.spi.SPIServiceDetails;
import org.cablelabs.impl.util.LocatorUtil;
import org.cablelabs.impl.util.SystemEventUtil;
import org.davic.net.InvalidLocatorException;
import org.davic.net.Locator;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.dvb.spi.selection.KnownServiceReference;
import org.dvb.spi.selection.LocatorScheme;
import org.dvb.spi.selection.SelectionSession;
import org.dvb.spi.selection.ServiceDescription;
import org.dvb.spi.selection.ServiceReference;
import org.dvb.spi.util.MultilingualString;
import org.ocap.hn.service.ServiceResolutionHandler;
import org.ocap.net.OcapLocator;

/**
 * Used to wrap and manage an application suppled ServiceResolutionHandler.
 * Implemented as a ProviderInstance
 */
public class ServiceResolutionHandlerProxy extends ProviderInstance
{
    // used for log messages
    private static final Logger log = Logger.getLogger(ServiceResolutionHandlerProxy.class);
    
    /** The ServiceResolutionHandler managed by this proxy */
    private final ServiceResolutionHandler handler;

    /** The SI cache */
    private final SICache siCache;

    /** The caller context to use when calling methods in the provider */
    private final CallerContext handlerCC;

    /** True if the handler has been disposed */
    private boolean disposed = false;

    /**
     * The locator scheme supported by this handler. 
     */
    private LocatorScheme scheme;
    
    private ServiceDescription description;
    
    /**
     * Object used to provide synchronized access to the service list. The
     * service list consist of the following fields.
     * <ul>
     * <li> {@link #serviceByTDLocator}
     * <li> {@link #serviceBySourceID}
     * <li> {@link #serviceByServiceName}
     * </ul>
     */
    private Object listLock = new Object();

    /**
     * Maps each transport-dependent locator to its corresponding Service
     * object. The key is the transport-dependent locator string and the value
     * is the Service object.
     */
    private final Hashtable serviceByTDLocator = new Hashtable();

    /**
     * Maps each source ID to its corresponding Service object. The key is the
     * source ID and the value is the Service object. An entry is only added to
     * this hashtable if the provider specified a source ID in the transport
     * dependent locator used to add the service.
     */
    private final Hashtable serviceBySourceID = new Hashtable();

    /**
     * Maps each service name to its corresponding Service object. The key is
     * the service name and the value is the Service object. An entry is only
     * added to this hashtable if the provider specified a service name in the
     * transport dependent locator used to add the service.
     */
    private final Hashtable serviceByServiceName = new Hashtable();

    
    private static ProviderRegistryExt providerRegistryExt = (ProviderRegistryExt) (ProviderRegistryExt.getInstance());
    
    /**
     * Construct a ServiceResolutionHandlerInstance based on the given handler.
     * 
     * @param provider
     *            The ServiceResolutionHandler managed by this Instance.
     */
    public ServiceResolutionHandlerProxy(ServiceResolutionHandler srh)
    {
        // Save the handler
        this.handler = srh;
        
        // Get the SI cache
        ServiceManager serviceManager = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        siCache = serviceManager.getSICache();

        // Save provider's caller context
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        handlerCC = ccm.getCurrentContext();
        
        init();
    }

    /**
     * Return the application supplied ServiceResolutionHandler
     */
    public ServiceResolutionHandler getHandler()
    {
        return handler;
    }
    
    /**
     * Return the ServiceResolutionHandler context
     */
    public CallerContext getHandlerCC()
    {
        return handlerCC;
    }
    
    /**
     * Initialize this instance.
     * This method is inherited from ProviderInstance. In the case of ServiceResolutionHandlerProxy it is only 
     * called from the constructor. In the case of SelectionProviderInstance the method is called explicitly during
     * initialization by the ProviderRegistry. 
     */
    public void init()
    {
        // ServiceResolutionHandler is registered as 'providerFirst' for "ocap" scheme.
        // If a SPI SelectionProvider is registered at a later time they both
        // co-exist as providers for this scheme. However in that case, SPI provider is added
        // before the ServiceResolutionHandler in the registry.
        this.scheme = new LocatorScheme("ocap", true); 
        
        providerRegistryExt.registerProviderInstance(this, new LocatorScheme[]{scheme});
    }

    /**
     * Dispose of this instance.
     */
    public void dispose()
    {
        disposed = true;
        
        providerRegistryExt.unregisterProviderInstance(this, new LocatorScheme[]{scheme});
        
        // Send notification indicating removal of each service previously
        // supplied by this handler.
        synchronized (listLock)
        {
            Enumeration e = serviceByTDLocator.elements();
            while (e.hasMoreElements())
            {
                SPIService service = (SPIService) e.nextElement();
                this.postEvent(service, null);
            }
        }
    }

    /**
     * Use identity hash code for application supplied ServiceResolutionHandler.
     */
    public int hashCode()
    {
        return System.identityHashCode(handler);
    }
    
    public boolean equals(Object obj)
    {
        return obj != null && (obj instanceof ServiceResolutionHandlerProxy) && ((ServiceResolutionHandlerProxy) obj).handler == handler;
    }

    // Called when ChannelContentItem is created
    public void addService(ServiceReference service)
    {        
        synchronized (listLock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("ServiceResolutionHandlerProxy addService service: " + service);
            }
            
            // Update the service list
            updateServiceList(service);
        }
    }
    
    // Called when ChannelContentitem tuning locator is set to null
    public void removeService(ServiceReference ref)
    {
        // This method is called when ServiceResolutionHandler 
        // sets the tuning locator to null
        synchronized (listLock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("ServiceResolutionHandlerProxy removeService service: " + ref);
            }

            // Make sure the TD service is present in the service list
            if (serviceByTDLocator.get(ref.getLocator().toExternalForm()) == null)
            {
                throw new IllegalArgumentException("Unknown service: " + ref);
            }

            // Remove the service from hash tables and signal event
            {
                String tdLocatorString = ref.getLocator().toExternalForm();
                SPIService tdService = (SPIService) serviceByTDLocator.get(tdLocatorString);
                
                if (tdService != null)
                {
                    // Remove the TD service from the service list
                    serviceByTDLocator.remove(tdLocatorString);

                    // Remove the source ID mapping
                    int oldSourceID = ((OcapLocator) tdService.getLocator()).getSourceID();
                    if (oldSourceID != -1) 
                    {
                        serviceBySourceID.remove(new Integer(oldSourceID));
                    }

                    // Remove the service name mapping
                    String oldServiceName = ((OcapLocator) tdService.getLocator()).getServiceName();
                    if (oldServiceName != null) 
                    {
                        serviceByServiceName.remove(oldServiceName);
                    }
                }
                this.postEvent(tdService, null);
            }
        }
    }

    // Called when ChannelContentitem tuning locator is resolved
    public void updateService(ServiceReference service)
    {
        // This method is called when ServiceResolutionHandler resolves
        // a ChannelContentItem's tuning locator
        synchronized (listLock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("ServiceResolutionHandlerProxy updateService service: " + service);
            }

            // Make sure the TD service is present in the service list
            if (serviceByTDLocator.get(service.getLocator().toExternalForm()) == null)
            {
                throw new IllegalArgumentException("Unknown service: " + service);
            }
            
            // Update the service list
            updateServiceList(service);
        }
    }

    /**
     * Update the list of services available via this provider.
     * 
     * @param reference
     *            The service reference to be updated.
     */
    private void updateServiceList(ServiceReference reference)
    {
        if (log.isDebugEnabled())
        {
            log.debug("ServiceResolutionHandlerProxy updateServiceList..reference: " + reference);
        }
        synchronized (listLock)
        {            
            if (log.isDebugEnabled())
            {
                log.debug("ServiceResolutionHandlerProxy updateServiceList..");
            }
            
            // Process the service reference
            if(reference != null)
            {
                ServiceReference ref = reference;
                if (log.isDebugEnabled())
                {
                    log.debug("ServiceResolutionHandlerProxy updateServiceList..ServiceReference: " + ref);
                }

                // OCAP uses a single scheme "ocap" to refer to both the
                // transport independent and
                // the transport dependent versions of the service. If a
                // provider tries to register
                // a service which implies a mapping from a transport
                // independent service to a
                // transport dependent service, then log the error and register
                // only the transport
                // dependent service.
                String tiLocatorString = ref.getServiceIdentifier();
                String tdLocatorString = ref.getLocator().toExternalForm();
                if (!tiLocatorString.equals(tdLocatorString))
                {
                    SystemEventUtil.logEvent("OCAP does not allow mapping from transport independent to transport dependent service: "
                            + reference);
                }

                // Get the transport dependent and actual locators
                OcapLocator tdLocator = null;
                OcapLocator actualLocator = null;
                try
                {
                    tdLocator = new OcapLocator(tdLocatorString);
                    if (reference instanceof KnownServiceReference)
                    {
                        String s = ((KnownServiceReference) reference).getActualLocation().toExternalForm();
                        actualLocator = new OcapLocator(s);
                        if (log.isDebugEnabled())
                        {
                            log.debug("ServiceResolutionHandlerProxy updateServiceList..actualLocator: " + actualLocator);
                        }
                    }
                }
                catch (InvalidLocatorException x)
                {
                    // Skip this service reference if we cannot construct the
                    // locators
                    SystemEventUtil.logRecoverableError("Cannot construct locator", x);
                }

                // Get the TD service from the old service list
                SPIService oldTDService = (SPIService) serviceByTDLocator.get(tdLocatorString);
                if (log.isDebugEnabled())
                {
                    log.debug("ServiceResolutionHandlerProxy updateServiceList..tdLocatorString: " + tdLocatorString + " ..oldTDService: " + oldTDService);
                }

                // Processing required only if TD service is present in the old
                // service list
                Object tdServiceUID = null;
                Object serviceDetailsUID = null;
                if (oldTDService != null)
                {
                    // Remove the TD service from the old service list
                    serviceByTDLocator.remove(tdLocatorString);

                    // Remove the old source ID mapping
                    int oldSourceID = ((OcapLocator) oldTDService.getLocator()).getSourceID();
                    if (oldSourceID != -1) 
                    {
                        serviceBySourceID.remove(new Integer(oldSourceID));
                    }

                    // Remove the old service name mapping
                    String oldServiceName = ((OcapLocator) oldTDService.getLocator()).getServiceName();
                    if (oldServiceName != null) 
                    {
                        serviceByServiceName.remove(oldServiceName);
                    }

                    // Get the TD unique ID
                    tdServiceUID = oldTDService.getID();

                    // Get the ServiceDetails unique ID from the old TD service
                    // if present
                    try
                    {
                        if (oldTDService != null)
                        {
                            SPIServiceDetails sd = (SPIServiceDetails) oldTDService.getDetails();
                            serviceDetailsUID = sd.getID();
                        }
                    }
                    catch (Exception x)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("ServiceResolutionHandlerProxy updateServiceList..caught exception", x);            
                        }
                    }
                }

                // Get the providerFirst flag for this service
                boolean providerFirst = true;

                // Create the TD service and add it to the new service list.
                ServiceDescription desc = null;
                SPIService newTDService = new SPIService(siCache, this, providerFirst, reference,
                        (desc == null) ? ServiceType.UNKNOWN : desc.getServiceType(), -1, -1, tdLocator, actualLocator,
                        desc, tdServiceUID, serviceDetailsUID);
                serviceByTDLocator.put(tdLocatorString, newTDService);
                if (log.isDebugEnabled())
                {
                    log.debug("ServiceResolutionHandlerProxy updateServiceList..newTDService: " + newTDService);            
                }

                // Add the new source ID mappings
                int newSourceID = ((OcapLocator) newTDService.getLocator()).getSourceID();
                if (newSourceID != -1)
                {
                    serviceBySourceID.put(new Integer(newSourceID), newTDService);
                }

                // Add the new service name mappings
                String newServiceName = ((OcapLocator) newTDService.getLocator()).getServiceName();
                if (newServiceName != null)
                {
                    serviceByServiceName.put(newServiceName, newTDService);
                }
                
                // Post change events if any
                postEvent(oldTDService, newTDService);
            }
        }
    }
    
    /**
     * Send an SIChangeEvent if the transport dependent service was added,
     * modified or removed.
     * 
     * @param oldService
     *            The service as it was before the change. Null indicates the
     *            service did not exist before the change.
     * @param newService
     *            The service as it is after the change. Null indicates the
     *            service no longer exists.
     */
    private void postEvent(SPIService oldService, SPIService newService)
    {
        // Check parameters
        if (oldService == null && newService == null) 
        {
            throw new IllegalArgumentException("ServiceResolutionHandlerProxy postEvent called with invalid arguments");
        }

        // Lookup old and new service details
        SPIServiceDetails oldServiceDetails = null;
        SPIServiceDetails newServiceDetails = null;
        try
        {
            if (oldService != null) 
            {
                oldServiceDetails = (SPIServiceDetails) oldService.getDetails();
            }
            if (newService != null) 
            {
                newServiceDetails = (SPIServiceDetails) newService.getDetails();
            }
        }
        catch (Exception x)
        {
            SystemEventUtil.logRecoverableError("Cannot get ServiceDetails", x);
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug("ServiceResolutionHandlerProxy postEvent..newService: " + newService);     
        }
        if (log.isDebugEnabled())
        {
            log.debug("ServiceResolutionHandlerProxy postEvent..oldService: " + oldService); 
        }

        // Set parameters based on old and new service details
        SIChangeType changeType = null;
        boolean oldMapped = false;
        boolean newMapped = false;
        SPIServiceDetails details;
        if (oldService == null)
        {
            changeType = SIChangeType.ADD;
            newMapped = newServiceDetails.isMapped();
            details = newServiceDetails;
        }
        else if (newService == null)
        {
            changeType = SIChangeType.REMOVE;
            oldMapped = oldServiceDetails.isMapped();
            details = oldServiceDetails;
        }
        else
        {
            // Compare the ServiceDetails to determine if callback needs to be notified
            if(oldServiceDetails.equals(newServiceDetails))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ServiceResolutionHandlerProxy postEvent..oldService == newService...");
                }
                return;
            }
            
            if (log.isDebugEnabled())
            {
                log.debug("ServiceResolutionHandlerProxy postEvent..SIChangeType.MODIFY");     
            }

            changeType = SIChangeType.MODIFY;
            oldMapped = oldServiceDetails.isMapped();
            newMapped = newServiceDetails.isMapped();
            details = newServiceDetails;
        }

        // Notify synchronous callback listeners
        if (oldMapped || newMapped)
        {
            String methodName;
            if (oldMapped)
            {
                if (newMapped)
                {
                    methodName = "notifyRemapped";
                }
                else
                {
                    methodName = "notifyUnmapped";
                }
            }
            else
            {
                methodName = "notifyMapped";
            }
            try
            {
                TransportExt.callbacks.invokeCallbacks(ServiceDetailsCallback.class.getMethod(methodName,
                    new Class[] { ServiceDetails.class }), new Object[] { details });
            }
            catch (Exception e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        }

        // Notify asynchronous event listeners. The ServiceDetailsChangeEvent is
        // posted to the
        // transport which carries this ServiceDetails. If the ServiceDetails is
        // not currently
        // carried in a transport, then no event is sent.
        TransportStreamExt ts = (TransportStreamExt) details.getTransportStream();
        if (ts != null)
        {
            TransportExt transport = (TransportExt) ts.getTransport();
            ServiceDetailsChangeEvent event = new ServiceDetailsChangeEvent(transport, changeType, details);
            transport.postServiceDetailsChangeEvent(event);
        }
    }

    class ChannelContentServiceDescription implements ServiceDescription
    {
        private Service service;

        ChannelContentServiceDescription(Service s)
        {
            service = s;
        }

        public DeliverySystemType getDeliverySystemType()
        {
            return DeliverySystemType.UNKNOWN;
        }

        public MultilingualString getLongName(String perferredLanguage)
        {
            return new MultilingualString(service.getName(), "eng");
        }

        public ServiceType getServiceType()
        {
            return service.getServiceType();
        }
    }

    /**
     * Provides the {@link Service} referred to by the given scheme and source
     * ID. A match will be found only if the provider registered the service
     * with a transport dependent locator containing the specified source ID.
     * 
     * @param scheme
     *            The scheme containing the service being looked for
     * @param sourceID
     *            The source ID of the service being looked for
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @return The service object or null if no service was found.
     * @throws IllegalArgumentException
     *             If scheme is null
     */
    public Service getService(String scheme, int sourceID, String language)
    {
        // Check parameters
        if (scheme == null) 
        {
            throw new IllegalArgumentException("ServiceResolutionHandlerProxy getService called with invalid scheme");
        }

        synchronized (listLock)
        {
            SPIService service = (SPIService) serviceBySourceID.get(new Integer(sourceID));
            if (service != null)
            {
                return (Service) service.createLanguageSpecificVariant(language);
            }
        }
        return null;
    }

    /**
     * Provides the {@link Service} referred to by the given scheme and service
     * name. A match will be found only if the provider registered the service
     * with a transport dependent locator containing the specified service name
     * or provided a {@link ServiceDescription} which answers with the specified
     * service name when its getLongName() method is called with the specified
     * preferred language.
     * 
     * @param scheme
     *            The scheme containing the service being looked for
     * @param serviceName
     *            The service name of the service being looked for
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @return The service object or null if no service was found.
     * @throws IllegalArgumentException
     *             If scheme or serviceName is null
     */
    public Service getService(String scheme, String serviceName, String language)
    {
        // Check parameters
        if (scheme == null || serviceName == null) 
        {
            throw new IllegalArgumentException("ServiceResolutionHandlerProxy getService called with invalid arguments");
        }

        synchronized (listLock)
        {
            SPIService service = (SPIService) serviceByServiceName.get(serviceName);
            if (service != null)
            {
                return (Service) service.createLanguageSpecificVariant(language);
            }

            // Iterate over all services checking for a match using the
            // specified language
            Enumeration e2 = serviceByTDLocator.elements();
            while (e2.hasMoreElements())
            {
                service = (SPIService) e2.nextElement();
                if (serviceName.equals(service.getName(language)))
                {
                    return (Service) service.createLanguageSpecificVariant(language);
                }
            }
        }
        return null;
    }

    /**
     * Adds all provider supplied services to the specified
     * {@link ServiceCollection}.
     * 
     * @param collection
     *            The service collection to add all services to
     * @param providerFirst
     *            Only add services to the collection whose
     *            {@link SPIService#getProviderFirst()} method returns the same
     *            value as this parameter.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     */
    public void getAllServices(ServiceCollection collection, boolean providerFirst, String language)
    {
        synchronized (listLock)
        {
            Enumeration e = serviceByTDLocator.elements();
            while (e.hasMoreElements())
            {
                SPIService spi = (SPIService) e.nextElement();
                if (spi.getProviderFirst() == providerFirst)
                {
                    spi = (SPIService) spi.createLanguageSpecificVariant(language);
                    if (log.isDebugEnabled())
                    {
                        log.debug("SelectionProviderInstance adding service: " + spi);     
                    }
                    collection.add(spi);
                }                    
            }
        }
    }
    
    public Service getServiceByLocator(String scheme, OcapLocator locator, String language)
    {
        // Check parameters
        if (scheme == null)
        {
            throw new IllegalArgumentException("ServiceResolutionHandlerProxy getService called with invalid arguments");
        }
        synchronized (listLock)
        {
            Enumeration e = serviceByTDLocator.elements();
            while (e.hasMoreElements())
            {
                SPIService spi = (SPIService) e.nextElement();
                if(spi.getLocator().equals(locator))
                {
                    return (Service) spi.createLanguageSpecificVariant(language);
                }                        
            }
            return null;            
        }
    }
    
    public void getAllProviderServices(ServiceCollection collection, boolean providerFirst, String language)
    {
        synchronized (listLock)
        {
            Enumeration e = serviceByTDLocator.elements();
            while (e.hasMoreElements())
            {
                SPIService spi = (SPIService) e.nextElement();
                if (spi.getProviderFirst() == providerFirst)
                {
                    spi = (SPIService) spi.createLanguageSpecificVariant(language);
                    if (log.isDebugEnabled())
                    {
                        log.debug("SelectionProviderInstance adding service: " + spi);     
                    }
                    collection.add(spi);
                }                    
            }
        }
    }
    
    /*
     * Create a new SelectionSession
     */
    public SelectionSession newSession(ServiceReference ref, SPIService service) 
    {        
        // Check whether this handler has been unregistered
        if (disposed) 
        {
            throw new IllegalStateException("This handler has been disposed");
        }
        
        // create new session
        SelectionSession session = null;

        session = new SRHSelectionSession(service);
          
        return session;
    }
    
    /*
     * @Override(non-Javadoc)
     * @see org.cablelabs.impl.spi.ProviderInstance#getSelectionSession(org.cablelabs.impl.spi.SPIService)
     */
    public SelectionSession getSelectionSession(SPIService service) 
    {
        NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
        NetworkInterface[] nis = nim.getNetworkInterfaces();
        for(int i=0; i<nis.length; i++)
        {     
            // If the tuner is tuned to the SPIService locator
            // return the selection session associated with the tuner
            if(service.getLocator().equals(nis[i].getLocator()))
            {
                ExtendedNetworkInterface eni = (ExtendedNetworkInterface)nis[i];
                if (log.isDebugEnabled())
                {
                    log.debug("getSelectionSession: ni[" + i + "].getLocator(): " + nis[i].getLocator());
                }
                if (log.isDebugEnabled())
                {
                    log.debug("getSelectionSession: eni.getCurrentSelectionSession(): " + eni.getCurrentSelectionSession());
                } 
                return eni.getCurrentSelectionSession();
            }           
        }
        return null;
    }

    /*
     * @Override(non-Javadoc)
     */
    public String getLongName(ServiceDescription description, String preferredLanguage) 
    {
        return null;
    }
    
    public class SRHSelectionSession extends SelectionSessionWrapper
    {
        private SPIService service;
        private org.davic.net.Locator mappedLocator = null;
        private ServiceExt mappedService = null;
        private ServiceDetailsExt mappedDetails = null;

        // The real session returned by the providers' newSession() method
        // Construct a session wrapper
        public SRHSelectionSession(SPIService service) 
        {  
            this.service = service;
            ServiceReference ref = ((SPIService)service).getServiceReference();
            if(ref instanceof KnownServiceReference)
            {
                    Locator spiLocator = null;
                    try 
                    {
                        spiLocator = LocatorUtil.convertJavaTVLocatorToOcapLocator(((KnownServiceReference)ref).getActualLocation());
                    } catch (javax.tv.locator.InvalidLocatorException e) 
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("SRHSelectionSession caught InvalidLocatorException", e);
                        }
                    }
                    this.mappedLocator = spiLocator;
            }
        }

        // Description copied from SelectionSession
        public Locator select()
        {   
            // Return the mapped locator
            return this.mappedLocator;
        }
        
        // Description copied from SelectionSession
        public void destroy()
        {
          
        }

        // Description copied from SelectionSession
        public void selectionReady()
        {

        }

        // Description copied from SelectionSession
        public float setRate(final float newRate)
        {
            // Return the rate
            return 0;
        }

        // Description copied from SelectionSession
        public long setPosition(final long newPosition)
        {
            return 0;
        }
        
        public SPIService getSPIService()
        {
            return this.service;
        }
        
        public org.davic.net.Locator getMappedLocator()
        {
            return this.mappedLocator;
        }
        
        public ServiceExt getMappedService()
        {
            if (this.mappedService == null)
            {
                try
                {
                    this.mappedService = (ServiceExt) SIManager.createInstance().getService(this.mappedLocator);
                }
                catch (SecurityException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("SRHSelectionSession getMappedService caught exception", e);
                    }
                }
                catch (javax.tv.locator.InvalidLocatorException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("SRHSelectionSession getMappedService caught exception", e);
                    }
                }
            }
            if (log.isDebugEnabled())
            {
                log.debug("getMappedService: " + this.mappedService );
            }
            
            return this.mappedService;
        }   
        
        public ServiceDetailsExt getMappedDetails()
        {
            if (this.mappedDetails == null)
            {
                ServiceExt s = getMappedService();
                try
                {
                    this.mappedDetails = (ServiceDetailsExt) s.getDetails();
                }
                catch (SIRequestException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("SRHSelectionSession getMappedService caught exception ", e);
                    }
                }
                catch (InterruptedException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("SRHSelectionSession getMappedService caught exception ", e);
                    }
                }
            }
            return this.mappedDetails;
        }            
    }
}
