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

package org.cablelabs.impl.service.javatv.navigation;

import java.util.Date;

import javax.tv.service.navigation.ServiceDescription;
import javax.tv.service.navigation.ServiceDetails;

import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServiceDescriptionExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.string.MultiString;

/**
 * The <code>ServiceDescription</code> implementation.
 * 
 * @author Todd Earles
 */
public class ServiceDescriptionImpl extends ServiceDescriptionExt
{
    /**
     * Construct a <code>ServiceDescription</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param serviceDetails
     *            The service details to which this description belongs
     * @param serviceDescription
     *            A textual description of the service or null if not available
     * @param updateTime
     *            The time when this object was last updated from data in the
     *            broadcast
     * @param uniqueID
     *            The uniqueID to use for this instance or null one should be
     *            created.
     */
    public ServiceDescriptionImpl(SICache siCache, ServiceDetails serviceDetails, MultiString serviceDescription,
            Date updateTime, Object uniqueID)
    {
        // Check all parameters
        // TODO(Todd): Check siCache for null when RecordedServiceImpl is fixed
        // to maintain its own implementation not based on this one.
        if (serviceDetails == null || updateTime == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        data = new Data();
        lsData = null;

        // Save all values
        data.siObjectID = (uniqueID == null) ? "ServiceDescription"
                + ((ServiceDetailsExt) serviceDetails).getServiceDetailsHandle().getHandle() : uniqueID;
        data.serviceDetails = serviceDetails;
        data.serviceDescription = serviceDescription;
        data.updateTime = updateTime;
    }

    /**
     * Construct a <code>ServiceDescription</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param data
     *            The shared data
     * @param lsData
     *            The language specific variant data or null if none
     * @throws IllegalArgumentException
     *             If <code>data</code> is null
     */
    protected ServiceDescriptionImpl(SICache siCache, Data data, LSData lsData)
    {
        if (siCache == null || data == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        this.data = data;
        this.lsData = lsData;
    }

    // Description copied from Transport
    public ServiceDescription createSnapshot(SICache siCache)
    {
        return new ServiceDescriptionImpl(siCache, data, lsData);
    }

    // The data objects
    private final Data data;

    private final LSData lsData;

    // Description copied from UniqueIdentifier
    public Object getID()
    {
        return data.siObjectID;
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
        return new ServiceDescriptionImpl(siCache, data, d);
    }

    // Description copied from ServiceDescriptionExt
    public ServiceDetails getServiceDetails()
    {
        return data.serviceDetails;
    }

    // Description copied from ServiceDescription
    public String getServiceDescription()
    {
        String language = (lsData == null) ? null : lsData.language;
        return (data.serviceDescription == null) ? "" : data.serviceDescription.getValue(language);
    }

    // Description copied from ServiceDescriptionExt
    public MultiString getServiceDescriptionAsMultiString()
    {
        return data.serviceDescription;
    }

    // Description copied from SIRetrievable
    public Date getUpdateTime()
    {
        return data.updateTime;
    }

    // Description copied from Object
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != getClass()) return false;

        ServiceDescriptionImpl o = (ServiceDescriptionImpl) obj;
        return getID().equals(o.getID())
                && (getServiceDescription() == o.getServiceDescription() || (getServiceDescription() != null && getServiceDescription().equals(
                        o.getServiceDescription()))) && getUpdateTime().equals(o.getUpdateTime())
                && getServiceDetails().equals(o.getServiceDetails());
    }

    // Description copied from Object
    public int hashCode()
    {
        return data.serviceDescription.hashCode() * 13 + getServiceDetails().hashCode();
    }

    // Description copied from Object
    public String toString()
    {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + "[uniqueID=" + getID()
                + ", serviceDescription=" + data.serviceDescription + ", updateTime=" + data.updateTime + ", lsData"
                + ((lsData == null) ? "=null" : "[language=" + lsData.language + "]") + "]";
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

        /** The service details to with this description belongs */
        public ServiceDetails serviceDetails;

        /** A textual description of the service */
        public MultiString serviceDescription;

        /**
         * The time when this object was last updated from data in the broadcast
         */
        public Date updateTime;
    }

    /**
     * The language specific variant data
     */
    static class LSData
    {
        /** The preferred language */
        public String language;
    }
}
