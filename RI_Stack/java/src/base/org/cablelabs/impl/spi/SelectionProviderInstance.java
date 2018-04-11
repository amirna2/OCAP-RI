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

package org.cablelabs.impl.spi;

import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;

import javax.tv.service.SIChangeType;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.ServiceType;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.ServiceDetailsChangeEvent;

import org.davic.net.InvalidLocatorException;
import org.davic.net.Locator;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.dvb.spi.Provider;
import org.dvb.spi.ProviderFailedInstallationException;
import org.dvb.spi.ProviderRegistry;
import org.dvb.spi.selection.KnownServiceReference;
import org.dvb.spi.selection.LocatorScheme;
import org.dvb.spi.selection.SelectionProvider;
import org.dvb.spi.selection.SelectionProviderContext;
import org.dvb.spi.selection.SelectionSession;
import org.dvb.spi.selection.ServiceDescription;
import org.dvb.spi.selection.ServiceReference;
import org.dvb.spi.util.MultilingualString;
import org.ocap.net.OcapLocator;

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
import org.cablelabs.impl.util.LocatorUtil;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * An instance of {@link SelectionProviderInstance} is used to wrap and manage
 * an instance of {@link SelectionProvider}. An instance is created when a
 * SelectionProvider is registered via the ProviderRegistry and is disposed when
 * the provider is unregistered.
 * 
 * @author Todd Earles
 * @see SelectionProvider
 * @see ProviderRegistry
 * @see SIManager
 */
public class SelectionProviderInstance extends ProviderInstance implements SelectionProviderContext
{
    // used for log messages
    private static final Logger log = Logger.getLogger(SelectionProviderInstance.class);

    /** The provider managed by this provider instance */
    private final Provider provider;

    /** The provider name */
    private String providerName = null;

    /** The provider version */
    private String providerVersion = null;

    /** The SI cache */
    private final SICache siCache;

    /** The caller context to use when calling methods in the provider */
    private final CallerContext providerCC;

    /** True if this provider instance has been disposed */
    private boolean disposed = false;

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
    private Hashtable serviceByTDLocator = new Hashtable();

    /**
     * Maps each source ID to its corresponding Service object. The key is the
     * source ID and the value is the Service object. An entry is only added to
     * this hashtable if the provider specified a source ID in the transport
     * dependent locator used to add the service.
     */
    private Hashtable serviceBySourceID = new Hashtable();

    /**
     * Maps each service name to its corresponding Service object. The key is
     * the service name and the value is the Service object. An entry is only
     * added to this hashtable if the provider specified a service name in the
     * transport dependent locator used to add the service.
     */
    private Hashtable serviceByServiceName = new Hashtable();
    
    /**
     * The list of schemes supported by this provider. The entries in this array
     * are copies of the LocatorScheme instances supplied by the provider.
     * Copies are made so that methods may be called safely regardless of
     * caller context used.
     */
    private LocatorScheme[] schemes;
   
    private static ProviderRegistryExt providerRegistryExt = (ProviderRegistryExt) (ProviderRegistryExt.getInstance());
    
    /**
     * Construct a SelectionProviderInstance based on the given
     * {@link SelectionProvider}.
     * 
     * @param provider
     *            The SelectionProvider managed by this provider instance.
     */
    public SelectionProviderInstance(SelectionProvider provider)
    {
        this.provider = provider;
        this.providerName = provider.getName();
        this.providerVersion = provider.getVersion();

        // Get the SI cache
        ServiceManager serviceManager = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        siCache = serviceManager.getSICache();

        // Save provider's caller context
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        providerCC = ccm.getCurrentContext();
    }
    
    // Description copied from ProviderInstance
    // This is called by the ProviderRegistry at the time the SelectionProvider is registered
    public void init() throws ProviderFailedInstallationException
    {
        // Initialize the provider
        ((SelectionProvider) provider).init(this);
        
        LocatorScheme[] schemes = ((SelectionProvider) provider).getSupportedLocatorSchemes();
        this.schemes = new LocatorScheme[schemes.length];
        for (int i = 0; i < schemes.length; i++)
        {
            // Get the scheme name and verify it is valid for this platform
            String name = schemes[i].getScheme();
            if (log.isDebugEnabled())
            {
                log.debug("SelectionProviderInstance locatorScheme: " + name);
            }
            // Hold on to a copy of the LocatorScheme
            this.schemes[i] = new LocatorScheme(name, schemes[i].getProviderFirst());
        }
          
        // Register the schemes supported by this provider instance
        providerRegistryExt.registerProviderInstance(this, schemes);
        
        // Populate the service list
        updateServiceList(((SelectionProvider) provider).getServiceList(), false);
    }

    // Description copied from ProviderInstance
    public void dispose()
    {
        providerRegistryExt.unregisterProviderInstance(this, schemes);
        
        disposed = true;
        
        // Send notification indicating removal of each service previously
        // supplied by this provider.
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
     * Return the application supplied provider
     */
    public Provider getProvider()
    {
        return provider;
    }

    /**
     * Return provider name.
     */
    public String getProviderName()
    {
        return providerName;
    }

    /**
     * Return provider version.
     */
    public String getProviderVersion()
    {
        return providerVersion;
    }
    
    public boolean equals(Object obj)
    {
        return obj != null && (obj instanceof SelectionProviderInstance) && ((SelectionProviderInstance) obj).provider == provider;
    }
    
    public int hashCode()
    {
        return System.identityHashCode(provider);
    }
    
    // Description copied from SelectionProviderContext
    public void serviceListChanged(ServiceReference[] serviceReferences)
    {
        updateServiceList(serviceReferences, false);
    }

    // Description copied from SelectionProviderContext
    public void serviceDescriptionAvailable(ServiceReference[] serviceReferences)
    {
        // TODO(Todd): The semantics for this method do not seem clear. Until
        // the real intentions
        // are clarified this method just re-generates the full service list
        // based on current
        // information from the provider. No need to switch to the provider
        // caller context since
        // this method is called by the provider.
        updateServiceList(((SelectionProvider) provider).getServiceList(), false);
    }

    // Description copied from SelectionProviderContext
    public void updateService(ServiceReference service)
    {
        synchronized (listLock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("SelectionProviderInstance updateService service: " + service);
            }

            // Make sure the TD service is present in the service list
            if (serviceByTDLocator.get(service.getLocator().toExternalForm()) == null)
            {
                throw new IllegalArgumentException("Unknown service: " + service);
            }

            // Update the service list
            updateServiceList(new ServiceReference[] { service }, true);
        }
    }

    /**
     * Update the list of services available via this provider.
     * 
     * @param references
     *            The list of service references to be updated.
     * @param partial
     *            True if "references" is a partial list of service references.
     *            False if "references" is a full list of service references. A
     *            full list totally replaces the current list of services. A
     *            partial list causes only the specified services to be
     *            replaced.
     */
    private void updateServiceList(ServiceReference[] references, boolean partial)
    {
        if (log.isDebugEnabled())
        {
            log.debug("SelectionProviderInstance updateServiceList..references: " + references);
        }
        
        synchronized (listLock)
        {
            // If a partial list of references was passed, then use the current
            // service list.
            // Otherwise, create a new service list.
            Hashtable newServiceByTDLocator;
            Hashtable newServiceBySourceID;
            Hashtable newServiceByServiceName;
            
            if (partial)
            {
                newServiceByTDLocator = serviceByTDLocator;
                newServiceBySourceID = serviceBySourceID;
                newServiceByServiceName = serviceByServiceName;
            }
            else
            {
                newServiceByTDLocator = new Hashtable();
                newServiceBySourceID = new Hashtable();
                newServiceByServiceName = new Hashtable();
            }

            // Get the service descriptions for the specified service
            // references. No need to switch
            // to the provider caller context since all calls to this method are
            // initiated by the
            // provider.
            ServiceDescription[] descriptions = ((SelectionProvider) provider).getServiceDescriptions(references);

            
            // Process each service reference
            for (int i = 0; i < references.length; i++)
            {
                ServiceReference ref = references[i];
                if (log.isDebugEnabled())
                {
                    log.debug("SelectionProviderInstance updateServiceList..ServiceReference: " + ref);
                }

                if (!isValidReference(ref))
                    continue;

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
                            + references[i]);
                }

                // Get the transport dependent and actual locators
                OcapLocator tdLocator;
                OcapLocator actualLocator = null;
                try
                {
                    tdLocator = new OcapLocator(tdLocatorString);
                    if (references[i] instanceof KnownServiceReference)
                    {
                        String s = ((KnownServiceReference) references[i]).getActualLocation().toExternalForm();
                        actualLocator = new OcapLocator(s);
                        if (log.isDebugEnabled())
                        {
                            log.debug("SelectionProviderInstance updateServiceList..actualLocator: " + actualLocator);
                        }
                    }
                }
                catch (InvalidLocatorException x)
                {
                    // Skip this service reference if we cannot construct the
                    // locators
                    SystemEventUtil.logRecoverableError("Cannot construct locator", x);
                    continue;
                }

                // Get the TD service from the old service list
                SPIService oldTDService = (SPIService) serviceByTDLocator.get(tdLocatorString);
                if (log.isDebugEnabled())
                {
                    log.debug("SelectionProviderInstance updateServiceList..tdLocatorString: " + tdLocatorString + ", oldTDService: " + oldTDService);
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
                    // Remove the old source ID mapping from the master list
                    if (oldSourceID != -1) 
                    {
                        serviceBySourceID.remove(new Integer(oldSourceID));
                    }

                    // Remove the old service name mapping
                    String oldServiceName = ((OcapLocator) oldTDService.getLocator()).getServiceName();
                    // Remove the old service name mapping from the master list
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
                            log.debug("SelectionProviderInstance updateServiceList..caught exception: ", x);
                        }
                    }
                }

                // Get the providerFirst flag for this service
                boolean providerFirst = false;
                for (int s = 0; s < schemes.length; s++)
                {
                    if (tdLocatorString.startsWith(schemes[s].getScheme()))
                    {
                        providerFirst = schemes[s].getProviderFirst();
                        break;
                    }
                }

                // Create the TD service and add it to the new service list.
                ServiceDescription desc = descriptions[i];
                SPIService newTDService = new SPIService(siCache, this, providerFirst, references[i],
                        (desc == null) ? ServiceType.UNKNOWN : desc.getServiceType(), -1, -1, tdLocator, actualLocator,
                        desc, tdServiceUID, serviceDetailsUID);
                newServiceByTDLocator.put(tdLocatorString, newTDService);

                if (log.isDebugEnabled())
                {
                    log.debug("SelectionProviderInstance updateServiceList..newTDService: " + newTDService);            
                }
                
                // Add the new source ID mappings
                int newSourceID = ((OcapLocator) newTDService.getLocator()).getSourceID();
                if (newSourceID != -1)
                {
                    newServiceBySourceID.put(new Integer(newSourceID), newTDService);
                }

                // Add the new service name mappings
                String newServiceName = ((OcapLocator) newTDService.getLocator()).getServiceName();
                if (newServiceName != null)
                {
                    newServiceByServiceName.put(newServiceName, newTDService);
                }
                                     
                // Post change events if any
                postEvent(oldTDService, newTDService);
            }

            // Remove all entries still in the old service list. This is only
            // done if a new service
            // list is being created.
            if (!partial)
            {                
                Enumeration e = serviceByTDLocator.elements();                
                while (e.hasMoreElements())
                {
                    postEvent((SPIService) e.nextElement(), null);   
                }
                
                // Make the new service list the current one
                serviceByTDLocator = newServiceByTDLocator;
                serviceBySourceID = newServiceBySourceID;
                serviceByServiceName = newServiceByServiceName;                
            }
        }
    }
    
    /**
     * Checks the ServiceReference for compliance with ECN 1102
     * <p>
     * From the ECN: This subsection complies with [DVB-GEM 1.0.2] Section 14.9
     * which and defines a standardized textual representation for transport
     * independent locators. The following assertions are made for transport
     * independent and transport dependent locators.
     * <ul>
     * <li>A transport independent locator SHALL be based on a source_id term.</li>
     * 
     * <li>A transport dependent locator SHALL be based on a source_id,
     * service_name, or frequency.program_number terms.</li>
     * 
     * <li>All service objects in the SI database returned the from
     * javax.tv.service.SIManager SHALL be transport dependent.</li>
     * </ul>
     * <p>
     * In addition, this section extends [DVB-GEM 1.0.2] and defines an actual
     * locator format used in the [DVB-MHP 1.1] provider SPI. The format of
     * these locators SHALL contain a frequency and program_number term in order
     * to be considered properly formatted.
     * 
     * Example valid TI locator:
     * <ul>
     * <li>ocap://0x0b12
     * <dd>Identify by source ID</li>
     * </ul>
     * 
     * Example valid TD locators:
     * <ul>
     * <li>ocap://0x0b12
     * <dd>Identify by source ID</li>
     * <li>ocap://n=Fox
     * <dd>Identify by source name</li>
     * <li>ocap://f=0x2254600.0x01
     * <dd>Identify by frequency and program number</li>
     * </ul>
     * 
     * Example invalid TI or TD locators:
     * <ul>
     * <li>ocap://f=0x2254600.0x01.m=0x0A
     * <dd>Identify by frequency and program number with modulation format
     * <li>ocap://oobfdc.0x01
     * <dd>Identify out-of-band servce by program number</li>
     * </ul>
     * 
     * @param ref
     *            the ServiceReference to be checked.
     * @return null if getting a valid ref is impossible otherwise a valid ref.
     * @throws InvalidLocatorException
     */
    private boolean isValidReference(ServiceReference ref)
    {
        // OCAP uses a single scheme "ocap" to refer to both the transport
        // independent and
        // the transport dependent versions of the service. If a provider tries
        // to register
        // a service which implies a mapping from a transport independent
        // service to a
        // transport dependent service, then log the error and register only the
        // transport
        // dependent service.
        String tiLocatorString = ref.getServiceIdentifier();
        String tdLocatorString = ref.getLocator().toExternalForm();
        if (!tiLocatorString.equals(tdLocatorString))
        {
            SystemEventUtil.logEvent("OCAP does not allow mapping from transport independent to transport dependent service: "
                    + ref);
        }

        // start testing the tiLocator
        OcapLocator tiLoc;
        OcapLocator tdLoc;
        try
        {
            tiLoc = new OcapLocator(tiLocatorString);
            tdLoc = new OcapLocator(ref.getLocator().toExternalForm());
        }
        catch (InvalidLocatorException e)
        {
            SystemEventUtil.logEvent("InvalidLocatorException thrown in SelectionProviderInstance.isValidReference: "
                    + e.getMessage());
            // if we have a poorly formed locator, it is not valid.
            return false;
        }
        // See if we have a valid TI locator:
        // 
        // if a source id is not defined then the TI locator is not valid and
        // the ServiceReference is not valid.
        if (tiLoc.getSourceID() == -1)
        {
            return false;
        }

        // See if we have a valid TD locator
        if (tdLoc.getFrequency() != -1) // if frequency is defined, then the
                                        // locator is in
                                        // frequency.program[m=modulation] form
        {
            return true;
        }
        else if (tdLoc.getServiceName() != null)
        {
            return true;
        }
        else if (tdLoc.getSourceID() != -1)
        {
            return true;
        }
        else
        {
            return false;
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
            throw new IllegalArgumentException("SelectionProviderInstance postEvent called with invalid arguments");
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
            if(newService != null)
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
            log.debug("SelectionProviderInstance postEvent..newService: " + newService);     
        }
        if (log.isDebugEnabled())
        {
            log.debug("SelectionProviderInstance postEvent..oldService: " + oldService); 
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
                    log.debug("SelectionProviderInstance postEvent..oldService == newService...");
                }
                return;
            }
            
            if (log.isDebugEnabled())
            {
                log.debug("SelectionProviderInstance postEvent..SIChangeType.MODIFY");     
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

    public Service getServiceByLocator(String scheme, OcapLocator locator, String language)
    {
        // Check parameters
        if (scheme == null) 
        {
            throw new IllegalArgumentException("SelectionProviderInstance getServiceByLocator called with invalid scheme - null");
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
            throw new IllegalArgumentException("SelectionProviderInstance getService called with invalid scheme - null");
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
            throw new IllegalArgumentException("SelectionProviderInstance getService called with invalid arguments");
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
    /**
     * Get the service long name from the specified service description using
     * the specified language.
     * 
     * @param description
     *            The provider supplied service description. It is the callers
     *            responsibility to ensure that this parameter is not null.
     * @param preferredLanguage
     *            The preferred language to use in looking up the service long
     *            name
     * @return The service long name or "" if one is not found
     */
    public String getLongName(final ServiceDescription description, final String preferredLanguage)
    {
        final String[] longName = new String[] { "" };
        CallerContext.Util.doRunInContextSync(providerCC, new Runnable()
        {
            public void run()
            {
                MultilingualString ms = description.getLongName(preferredLanguage);
                if (ms != null)
                {
                    longName[0] = ms.getString();
                }
            }
        });

        return longName[0];
    }

    /**
     * Calls the {@link SelectionProvider#newSession(ServiceReference)} method
     * of the SelectionProvider managed by this provider instance.
     * 
     * @throws IllegalStateException
     *             If this provider instance has been disposed.
     */
    public SelectionSession newSession(final ServiceReference ref, final SPIService service)
    {
        synchronized (listLock)
        {
            // Check whether this provider instance has been disposed
            if (disposed) throw new IllegalStateException("This provider instance has been disposed");

            // Call newSession() method on the provider
            final SelectionSession[] session = new SelectionSession[] { null };
            if (CallerContext.Util.doRunInContextSync(providerCC, new Runnable()
            {
                public void run()
                {
                    session[0] = ((SelectionProvider) provider).newSession(ref);
                }
            }))
            {
                // Return a session wrapper so calls to the session end up
                // calling the provider from
                // the correct caller context.                
                return new SPISelectionSession(session[0], service);
            }
            else
            {
                // Return null if the session could not be setup
                return null;
            }
        }
    }

    // Session wrapper
    public class SPISelectionSession extends SelectionSessionWrapper
    {
        private SelectionSession session;
        private SPIService service;
        private org.davic.net.Locator mappedLocator = null;
        private ServiceExt mappedService = null;
        private ServiceDetailsExt mappedDetails = null;

        // The real session returned by the providers' newSession() method
        // Construct a session wrapper
        public SPISelectionSession(SelectionSession session, SPIService service) 
        {
            this.session = session;   
            this.service = service;
            if (log.isDebugEnabled())
            {
                log.debug("SPISelectionSession ctor session: " + session);
            }
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
                            log.debug("SPISelectionSession ctor caught exception", e);
                        }
                    }
                    this.mappedLocator = spiLocator;
            }
        }

        // Description copied from SelectionSession
        public Locator select()
        {
            if (log.isDebugEnabled())
            {
                log.debug("SPISelectionSession select session: " + session);
            }
            // Call select() method on the providers' session
            final Locator[] locator = new Locator[] { null };
            CallerContext.Util.doRunInContextSync(providerCC, new Runnable()
            {
                public void run()
                {
                    locator[0] = session.select();
                }
            });
            
            if (this.mappedLocator == null)
            {
                this.mappedLocator = locator[0];
            }
            
            // Return the locator
            return locator[0];
        }
        
        // Description copied from SelectionSession
        public void destroy()
        {
            // Call destroy() method on the providers' session
            CallerContext.Util.doRunInContextSync(providerCC, new Runnable()
            {
                public void run()
                {
                    session.destroy();
                }
            });            
        }

        // Description copied from SelectionSession
        public void selectionReady()
        {
            // Call selectionReady() method on the providers' session
            CallerContext.Util.doRunInContextSync(providerCC, new Runnable()
            {
                public void run()
                {
                    session.selectionReady();
                }
            });
        }

        // Description copied from SelectionSession
        public float setRate(final float newRate)
        {
            // Call setRate() method on the providers' session
            final float[] rate = new float[] { Float.NEGATIVE_INFINITY };
            CallerContext.Util.doRunInContextSync(providerCC, new Runnable()
            {
                public void run()
                {
                    rate[0] = session.setRate(newRate);
                }
            });

            // Return the rate
            return rate[0];
        }

        // Description copied from SelectionSession
        public long setPosition(final long newPosition)
        {
            // Call setPosition() method on the providers' session
            final long[] position = new long[] { -1 };
            CallerContext.Util.doRunInContextSync(providerCC, new Runnable()
            {
                public void run()
                {
                    position[0] = session.setPosition(newPosition);
                }
            });

            // Return the locator
            return position[0];
        }
        
        public SPIService getSPIService()
        {
            return this.service;
        }
        
        public org.davic.net.Locator getMappedLocator()
        {
            if (this.mappedLocator == null)
            {
                this.mappedLocator = select();
            }
            return this.mappedLocator;
        }
        
        public ServiceExt getMappedService()
        {
            if (log.isDebugEnabled())
            {
                log.debug("getMappedService: " + this.mappedService );
            }
            if (this.mappedService == null)
            {
                try
                {
                    this.mappedService = (ServiceExt) SIManager.createInstance().getService(select());
                }
                catch (SecurityException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("SPISelectionSession getMappedService caught exception", e);
                    }
                }
                catch (javax.tv.locator.InvalidLocatorException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("SPISelectionSession getMappedService caught exception", e);
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
                        log.debug("SPISelectionSession getMappedService caught exception", e);
                    }
                }
                catch (InterruptedException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("SPISelectionSession getMappedService caught exception", e);
                    }
                }
            }
            return this.mappedDetails;
        }            
    }

}
