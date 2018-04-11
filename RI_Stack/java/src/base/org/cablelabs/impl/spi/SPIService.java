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

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.ServiceType;
import javax.tv.service.navigation.DeliverySystemType;

import org.dvb.spi.selection.SelectionProvider;
import org.dvb.spi.selection.ServiceDescription;
import org.dvb.spi.selection.ServiceReference;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.util.string.MultiString;

/**
 * A {@link Service} made available via a {@link SelectionProvider}.
 * 
 * @author Todd Earles
 */
public class SPIService extends ServiceExt
{
    /**
     * Construct a <code>SPIService</code>. It is the callers responsibility to
     * ensure that this constructor is called only while executing within the
     * caller context of the provider who is making this service available. This
     * allows us to safely call methods on provider supplied objects.
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param providerInstance
     *            The {@link SelectionProviderInstance} which provides this
     *            service
     * @param providerFirst
     *            If true, then this service overrides any implementation
     *            provided service with the same identification. If false, then
     *            this service is hidden by any implementation provided service
     *            with the same identification.
     * @param serviceReference
     *            The {@link ServiceReference} which created this service
     * @param serviceType
     *            The type of service
     * @param serviceNumber
     *            The service number
     * @param minorNumber
     *            The service minor number
     * @param locator
     *            The locator to the service
     * @param actualLocator
     *            The locator to where this service is currently mapped or null
     *            if not currently mapped.
     * @param description
     *            The service description or null if not available
     * @param uniqueID
     *            The uniqueID to use for this instance or null one should be
     *            created.
     * @param serviceDetailsUniqueID
     *            The uniqueID to use when creating the associated
     *            ServiceDetails or null if this is not a transport dependent
     *            service.
     */
    public SPIService(SICache siCache, ProviderInstance providerInstance, boolean providerFirst,
            ServiceReference serviceReference, ServiceType serviceType, int serviceNumber, int minorNumber,
            OcapLocator locator, OcapLocator actualLocator, ServiceDescription description, Object uniqueID,
            Object serviceDetailsUniqueID)
    {
        // Check all parameters
        if (siCache == null || providerInstance == null || serviceReference == null || serviceType == null
                || locator == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        data = new Data();
        lsData = null;
        locData = null;

        // Save all values
        data.providerInstance = providerInstance;
        data.providerFirst = providerFirst;
        data.serviceReference = serviceReference;
        data.siObjectID = (uniqueID == null) ? "SPIService" + nextUnusedID++ : uniqueID;
        data.serviceType = ServiceType.UNKNOWN;
        data.serviceNumber = serviceNumber;
        data.minorNumber = minorNumber;
        data.locator = locator;
        data.serviceDescription = description;
        if (description != null)
        {
            data.serviceType = serviceType;
        }

        // Create the associated ServiceDetails
        data.serviceDetails = new SPIServiceDetails(siCache, data.locator.getSourceID(), this,
                (description == null) ? DeliverySystemType.UNKNOWN : description.getDeliverySystemType(),
                actualLocator, description, serviceDetailsUniqueID);
    }
  
    /**
     * Construct a <code>SPIService</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param data
     *            The shared data
     * @param lsData
     *            The language specific variant data or null if none
     * @param locData
     *            The locator specific variant data or null if none
     * @throws IllegalArgumentException
     *             If <code>data</code> is null
     */
    protected SPIService(SICache siCache, Data data, LSData lsData, LocData locData)
    {
        if (siCache == null || data == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        this.data = data;
        this.lsData = lsData;
        this.locData = locData;
    }

    // Description copied from Transport
    public Service createSnapshot(SICache siCache)
    {
        return new SPIService(siCache, data, lsData, locData);
    }

    // The data objects
    private final Data data;

    private final LSData lsData;

    private final LocData locData;

    // Description copied from UniqueIdentifier
    public Object getID()
    {
        return data.siObjectID;
    }

    /**
     * Return the {@link ProviderInstance} which provides this service.
     */
    public ProviderInstance getProviderInstance()
    {
        return data.providerInstance;
    }
    
    /**
     * Return true if this service should override any implementation provided
     * service with the same identification. Otherwise, return false.
     */
    public boolean getProviderFirst()
    {
        return data.providerFirst;
    }

    /**
     * Return the {@link ServiceReference} which created this service.
     */
    public ServiceReference getServiceReference()
    {
        return data.serviceReference;
    }

    // Description copied from ServiceExt
    public ServiceHandle getServiceHandle()
    {
        return null;
    }

    // Description copied from LanguageVariant
    public String getPreferredLanguage()
    {
        if (lsData == null)
            return null;
        else
            return lsData.language;
    }

    // Description copied from LanguageVariant
    public Object createLanguageSpecificVariant(String language)
    {
        LSData d = new LSData();
        d.language = language;
        return new SPIService(siCache, data, d, locData);
    }

    // Description copied from LocatorVariant
    public Object createLocatorSpecificVariant(Locator locator)
    {
        if (locator == null) throw new NullPointerException();
        LocData d = new LocData();
        d.locator = locator;
        return new SPIService(siCache, data, lsData, d);
    }

    // Description copied from Service
    public SIRequest retrieveDetails(final SIRequestor requestor)
    {
        // Create the request object
        SIRequest request = new SIRequest()
        {
            public boolean cancel()
            {
                return false;
            }
        };

        // Return the ServiceDetails
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ccm.getCurrentContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                // Call the requestor with the result
                SPIServiceDetails details = (SPIServiceDetails) data.serviceDetails.createLanguageSpecificVariant(getPreferredLanguage());
                requestor.notifySuccess(new SPIServiceDetails[] { details });
            }
        });

        return request;
    }

    /**
     * Get the service name.
     * 
     * @param preferredLanguage
     *            The preferred language or null if no preference
     * @return The service name
     */
    public String getName(String preferredLanguage)
    {
        if (data.serviceDescription == null)
            return "";
        else
            return data.providerInstance.getLongName(data.serviceDescription, preferredLanguage);
    }

    // Description copied from Service
    public String getName()
    {
        String language = (lsData == null) ? null : lsData.language;
        return getName(language);
    }

    // Description copied from ServiceExt
    public MultiString getNameAsMultiString()
    {
        return null;
    }

    // Description copied from Service
    public boolean hasMultipleInstances()
    {
        return false;
    }

    // Description copied from Service
    public ServiceType getServiceType()
    {
        return data.serviceType;
    }

    // Description copied from Service
    public Locator getLocator()
    {
        if (locData == null)
            return data.locator;
        else
            return locData.locator;
    }

    // Description copied from Service
    public boolean equals(Object obj)
    {
        // Make sure we have a good object
        if (this == obj) return true;
        if (obj == null || obj.getClass() != getClass()) return false;

        // Compare all other fields
        SPIService o = (SPIService) obj;
        return getID().equals(o.getID())
                && (getName() == o.getName() || (getName() != null && getName().equals(o.getName())))
                && hasMultipleInstances() == o.hasMultipleInstances() && getServiceType().equals(o.getServiceType())
                && getServiceNumber() == o.getServiceNumber() && getMinorNumber() == o.getMinorNumber()
                && getLocator().equals(o.getLocator());
    }

    public void registerForPSIAcquisition()
    {
        // TODO Auto-generated method stub
    }

    public void unregisterForPSIAcquisition()
    {
        // TODO Auto-generated method stub
    }

    // Description copied from Service
    public int hashCode()
    {
        return getLocator().hashCode();
    }

    // Description copied from ServiceNumber
    public int getServiceNumber()
    {
        return data.serviceNumber;
    }

    // Description copied from ServiceMinorNumber
    public int getMinorNumber()
    {
        return data.minorNumber;
    }

    // Description copied from Object
    public String toString()
    {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + "[uniqueID=" + getID()
                + ", locator=" + getLocator() + ", name=" + getName() + ", serviceType=" + getServiceType()
                + ", serviceNumber=" + getServiceNumber() + ", minorNumber=" + getMinorNumber() + ", serviceDetails="
                + data.serviceDetails + ", lsData"
                + ((lsData == null) ? "=null" : "[language=" + lsData.language + "]") + ", locData"
                + ((locData == null) ? "=null" : "[locator=" + locData.locator + "]") + "]";
    }

    /** The SI cache */
    protected final SICache siCache;

    /**
     * The shared data for the invariant and all variants of this object
     */
    static class Data
    {
        /** The ProviderInstance which provides this service */
        public ProviderInstance providerInstance;

        /** The provider for this service */
        public Object provider;
        
        /** The providerFirst flag for this service */
        public boolean providerFirst;

        /** The ServiceReference which created this service */
        public ServiceReference serviceReference;

        /** Object ID for this SI object */
        public Object siObjectID;

        /** The type of service */
        public ServiceType serviceType;

        /** The service number */
        public int serviceNumber;

        /** The service minor number */
        public int minorNumber;

        /** The service locator */
        public OcapLocator locator;

        /** The service description */
        public ServiceDescription serviceDescription;

        /** The ServiceDetails associated with this service */
        public SPIServiceDetails serviceDetails;
    }

    /**
     * The language specific variant data
     */
    static class LSData
    {
        /** The preferred language */
        public String language;
    }

    /**
     * The locator specific variant data
     */
    static class LocData
    {
        /** The locator to use instead of the invariant locator */
        public Locator locator;
    }

    // Next unused ID number
    private static int nextUnusedID = 1;
}
