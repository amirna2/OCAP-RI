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

package org.cablelabs.impl.service.javatv.service;

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.ServiceType;

import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.util.string.MultiString;

/**
 * The <code>Service</code> implementation.
 * 
 * @author Todd Earles
 */
public class ServiceImpl extends ServiceExt
{
    /**
     * Construct a <code>Service</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param serviceHandle
     *            The service handle or null if not available via the SIDatabase
     * @param serviceName
     *            The service name or null if not available
     * @param hasMultipleInstances
     *            True if the service is carried on multiple transports
     * @param serviceType
     *            The type of service
     * @param serviceNumber
     *            The service number
     * @param minorNumber
     *            The service minor number
     * @param locator
     *            The locator to the service
     * @param uniqueID
     *            The uniqueID to use for this instance or null one should be
     *            created.
     */
    public ServiceImpl(SICache siCache, ServiceHandle serviceHandle, MultiString serviceName,
            boolean hasMultipleInstances, ServiceType serviceType, int serviceNumber, int minorNumber, Locator locator,
            Object uniqueID)
    {
        // Check all parameters
        if (siCache == null || serviceType == null || locator == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        data = new Data();
        lsData = null;
        locData = null;

        // Save all values
        data.siObjectID = (uniqueID == null) ? "ServiceImpl" + serviceHandle.getHandle() : uniqueID;
        data.serviceHandle = serviceHandle;
        data.serviceName = serviceName;
        data.hasMultipleInstances = hasMultipleInstances;
        data.serviceType = serviceType;
        data.serviceNumber = serviceNumber;
        data.minorNumber = minorNumber;
        data.locator = locator;
    }

    /**
     * Construct a <code>Service</code>
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
    protected ServiceImpl(SICache siCache, Data data, LSData lsData, LocData locData)
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
        return new ServiceImpl(siCache, data, lsData, locData);
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

    // Description copied from ServiceExt
    public ServiceHandle getServiceHandle()
    {
        return data.serviceHandle;
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
        return new ServiceImpl(siCache, data, d, locData);
    }

    // Description copied from LocatorVariant
    public Object createLocatorSpecificVariant(Locator locator)
    {
        if (locator == null) throw new NullPointerException();
        LocData d = new LocData();
        d.locator = locator;
        return new ServiceImpl(siCache, data, lsData, d);
    }

    // Description copied from Service
    public SIRequest retrieveDetails(SIRequestor requestor)
    {
        // Call the SICache to do the real work
        return siCache.retrieveServiceDetails(this, getPreferredLanguage(), false, requestor);
    }

    // Description copied from Service
    public String getName()
    {
        if (data.serviceName == null)
        {
            return "";
        }
        else
        {
            String language = (lsData == null) ? null : lsData.language;
            return data.serviceName.getValue(language);
        }
    }

    // Description copied from ServiceExt
    public MultiString getNameAsMultiString()
    {
        return data.serviceName;
    }

    // Description copied from Service
    public boolean hasMultipleInstances()
    {
        return data.hasMultipleInstances;
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
        ServiceImpl o = (ServiceImpl) obj;
        return getID().equals(o.getID())
                && (getServiceHandle() == o.getServiceHandle() || (getServiceHandle() != null && getServiceHandle().equals(
                        o.getServiceHandle())))
                && (getName() == o.getName() || (getName() != null && getName().equals(o.getName())))
                && hasMultipleInstances() == o.hasMultipleInstances() && getServiceType().equals(o.getServiceType())
                && getServiceNumber() == o.getServiceNumber() && getMinorNumber() == o.getMinorNumber()
                && getLocator().equals(o.getLocator());
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
                + ", locator=" + getLocator() + ", handle=" + getServiceHandle() + ", name=" + getName()
                + ", serviceType=" + getServiceType() + ", serviceNumber=" + getServiceNumber() + ", minorNumber="
                + getMinorNumber() + ", lsData" + ((lsData == null) ? "=null" : "[language=" + lsData.language + "]")
                + ", locData" + ((locData == null) ? "=null" : "[locator=" + locData.locator + "]") + "]";
    }

    public void registerForPSIAcquisition()
    {
        // DSG specific
        siCache.getSIDatabase().registerForPSIAcquisition(data.serviceHandle);
    }

    public void unregisterForPSIAcquisition()
    {
        // DSG specific
        siCache.getSIDatabase().unregisterForPSIAcquisition(data.serviceHandle);
    }

    /** The SI cache */
    protected final SICache siCache;

    /**
     * The shared data for the invariant and all variants of this object
     */
    static class Data
    {
        /** Object ID for this SI object */
        public Object siObjectID;

        /** The service handle */
        public ServiceHandle serviceHandle;

        /** The service name */
        public MultiString serviceName;

        /** True if the service is carried on multiple transports */
        public boolean hasMultipleInstances;

        /** The type of service */
        public ServiceType serviceType;

        /** The service number */
        public int serviceNumber;

        /** The service minor number */
        public int minorNumber;

        /** The service locator */
        public Locator locator;
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
}
