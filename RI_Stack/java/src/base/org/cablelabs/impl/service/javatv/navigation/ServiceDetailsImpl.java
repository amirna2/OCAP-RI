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

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.ServiceType;
import javax.tv.service.guide.ProgramSchedule;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceComponentChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.TransportStream;

import org.cablelabs.impl.service.ListenerList;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.string.MultiString;

/**
 * The <code>ServiceDetails</code> implementation.
 * 
 * @author Todd Earles
 */
public class ServiceDetailsImpl extends ServiceDetailsExt
{
    /**
     * Construct a <code>ServiceDetails</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param serviceDetailsHandle
     *            The service details handle or null if not available via the
     *            SIDatabase
     * @param sourceID
     *            The source ID of the service
     * @param programNumber
     *            The program number for the service
     * @param transportStream
     *            The transport stream that contains this service details or
     *            null if not currently carried.
     * @param longName
     *            The full service name or null if not available
     * @param service
     *            The service to which this service details belongs
     * @param deliverySystemType
     *            The delivery system type of this transport
     * @param serviceInformationType
     *            The SI format in which element was delivered
     * @param updateTime
     *            The time when this object was last updated from data in the
     *            broadcast
     * @param caSystemIDs
     *            The array of CA system IDs associated with the service (0
     *            length if none)
     * @param isFree
     *            True if the service is not protected by CA
     * @param pcrPID
     *            The PCR PID or 0x1FFF if none defined
     * @param uniqueID
     *            The uniqueID to use for this instance or null one should be
     *            created.
     */
    public ServiceDetailsImpl(SICache siCache, ServiceDetailsHandle serviceDetailsHandle, int sourceID,
            int programNumber, TransportStream transportStream, MultiString longName, Service service,
            DeliverySystemType deliverySystemType, ServiceInformationType serviceInformationType, Date updateTime,
            int[] caSystemIDs, boolean isFree, int pcrPID, Object uniqueID)
    {
        // Check all parameters
        if (siCache == null || service == null || deliverySystemType == null || serviceInformationType == null
                || updateTime == null || caSystemIDs == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        data = new Data();
        lsData = null;

        // Save all values
        data.siObjectID = (uniqueID == null) ? "ServiceDetailsImpl" + serviceDetailsHandle.getHandle() : uniqueID;
        data.serviceDetailsHandle = serviceDetailsHandle;
        data.sourceID = sourceID;
        data.programNumber = programNumber;
        data.transportStream = transportStream;
        data.longName = longName;
        data.service = service;
        data.deliverySystemType = deliverySystemType;
        data.serviceInformationType = serviceInformationType;
        data.updateTime = updateTime;
        data.caSystemIDs = caSystemIDs;
        data.isFree = isFree;
        data.pcrPID = pcrPID;

        // Register static listener with the cache
        siCache.addServiceComponentChangeListener(serviceComponentChangeListener);
    }

    /**
     * Construct a <code>ServiceDetails</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param serviceDetailsHandle
     *            The service details handle or null if not available via the
     *            SIDatabase
     * @param sourceID
     *            The source ID of the service
     * @param appID
     *            The app ID of the service (DSG only)
     * @param programNumber
     *            The program number for the service
     * @param transportStream
     *            The transport stream that contains this service details or
     *            null if not currently carried.
     * @param longName
     *            The full service name or null if not available
     * @param service
     *            The service to which this service details belongs
     * @param deliverySystemType
     *            The delivery system type of this transport
     * @param serviceInformationType
     *            The SI format in which element was delivered
     * @param updateTime
     *            The time when this object was last updated from data in the
     *            broadcast
     * @param caSystemIDs
     *            The array of CA system IDs associated with the service (0
     *            length if none)
     * @param isFree
     *            True if the service is not protected by CA
     * @param pcrPID
     *            The PCR PID or 0x1FFF if none defined
     * @param uniqueID
     *            The uniqueID to use for this instance or null one should be
     *            created.
     */
    public ServiceDetailsImpl(SICache siCache, ServiceDetailsHandle serviceDetailsHandle, int sourceID, int appID,
            int programNumber, TransportStream transportStream, MultiString longName, Service service,
            DeliverySystemType deliverySystemType, ServiceInformationType serviceInformationType, Date updateTime,
            int[] caSystemIDs, boolean isFree, int pcrPID, Object uniqueID)
    {
        // Check all parameters
        if (siCache == null || service == null || deliverySystemType == null || serviceInformationType == null
                || updateTime == null || caSystemIDs == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        data = new Data();
        lsData = null;

        // Save all values
        data.siObjectID = (uniqueID == null) ? "ServiceDetailsImpl" + serviceDetailsHandle.getHandle() : uniqueID;
        data.serviceDetailsHandle = serviceDetailsHandle;
        data.sourceID = sourceID;
        data.appID = appID;
        data.programNumber = programNumber;
        data.transportStream = transportStream;
        data.longName = longName;
        data.service = service;
        data.deliverySystemType = deliverySystemType;
        data.serviceInformationType = serviceInformationType;
        data.updateTime = updateTime;
        data.caSystemIDs = caSystemIDs;
        data.isFree = isFree;
        data.pcrPID = pcrPID;

        // Register static listener with the cache
        siCache.addServiceComponentChangeListener(serviceComponentChangeListener);
    }

    /**
     * Construct a <code>ServiceDetails</code>
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
    protected ServiceDetailsImpl(SICache siCache, Data data, LSData lsData)
    {
        if (siCache == null || data == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        this.data = data;
        this.lsData = lsData;
    }

    // Description copied from Transport
    public ServiceDetails createSnapshot(SICache siCache)
    {
        return new ServiceDetailsImpl(siCache, data, lsData);
    }

    // The data objects
    protected final Data data;

    private final LSData lsData;

    // Description copied from UniqueIdentifier
    public Object getID()
    {
        return data.siObjectID;
    }

    // Description copied from ServiceDetailsExt
    public ServiceDetailsHandle getServiceDetailsHandle()
    {
        return data.serviceDetailsHandle;
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
        return new ServiceDetailsImpl(siCache, data, d);
    }

    // Description copied from ServiceDetailsExt
    public int getSourceID()
    {
        return data.sourceID;
    }

    public int getAppID()
    {
        return data.appID;
    }

    // Description copied from ServiceDetailsExt
    public int getProgramNumber()
    {
        return data.programNumber;
    }

    // Description copied from ServiceDetailsExt
    public TransportStream getTransportStream()
    {
        return data.transportStream;
    }

    // Description copied from ServiceDetailsExt
    public SIRequest retrievePcrPID(SIRequestor requestor)
    {
        return siCache.retrievePCRPid(this, getPreferredLanguage(), requestor);
    }
    
    // Description copied from ServiceDetailsExt
    public SIRequest retrieveDefaultMediaComponents(SIRequestor requestor)
    {
        return siCache.retrieveDefaultMediaComponents(this, getPreferredLanguage(), requestor);
    }

    // Description copied from ServiceDetailsExt
    public SIRequest retrieveCarouselComponent(SIRequestor requestor)
    {
        return siCache.retrieveCarouselComponent(this, getPreferredLanguage(), requestor);
    }

    // Description copied from ServiceDetailsExt
    public SIRequest retrieveCarouselComponent(SIRequestor requestor, int carouselID)
    {
        return siCache.retrieveCarouselComponent(this, carouselID, getPreferredLanguage(), requestor);
    }

    // Description copied from ServiceDetailsExt
    public SIRequest retrieveComponentByAssociationTag(SIRequestor requestor, int associationTag)
    {
        return siCache.retrieveComponentByAssociationTag(this, associationTag, getPreferredLanguage(), requestor);
    }

    // Description copied from ServiceDetails
    public SIRequest retrieveServiceDescription(SIRequestor requestor)
    {
        return siCache.retrieveServiceDescription(this, getPreferredLanguage(), requestor);
    }

    // Description copied from ServiceDetails
    public ServiceType getServiceType()
    {
        return data.service.getServiceType();
    }

    // Description copied from ServiceDetails
    public SIRequest retrieveComponents(SIRequestor requestor)
    {
        return siCache.retrieveServiceComponents(this, getPreferredLanguage(), requestor);
    }

    // Description copied from ServiceDetails
    public ProgramSchedule getProgramSchedule()
    {
        // TODO(Todd): Add support for program schedule
        return null;
    }

    // Description copied from ServiceDetails
    public String getLongName()
    {
        String language = (lsData == null) ? null : lsData.language;
        return (data.longName == null) ? "" : data.longName.getValue(language);
    }

    // Description copied from ServiceDetailsExt
    public MultiString getLongNameAsMultiString()
    {
        return data.longName;
    }

    // Description copied from ServiceDetails
    public Service getService()
    {
        return data.service;
    }

    // Description copied from ServiceDetails
    public void addServiceComponentChangeListener(ServiceComponentChangeListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("null listener not supported");
        }

        serviceComponentChangeListenerList.addListener(getID(), listener);
    }

    // Description copied from ServiceDetails
    public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("null listener not supported");
        }

        serviceComponentChangeListenerList.removeListener(getID(), listener);
    }

    // Description copied from ServiceNumber
    public int getServiceNumber()
    {
        return ((ServiceExt) data.service).getServiceNumber();
    }

    // Description copied from ServiceMinorNumber
    public int getMinorNumber()
    {
        return ((ServiceExt) data.service).getMinorNumber();
    }

    // Description copied from ServiceDetails
    public DeliverySystemType getDeliverySystemType()
    {
        return data.deliverySystemType;
    }

    // Description copied from SIElement
    public Locator getLocator()
    {
        // Get the locator from the service object
        return data.service.getLocator();
    }

    // Description copied from SIElement
    public boolean equals(Object obj)
    {
        // Make sure we have a good object
        if (this == obj) return true;
        if (obj == null || obj.getClass() != getClass()) return false;

        // Compare CA system IDs
        ServiceDetailsImpl o = (ServiceDetailsImpl) obj;
        int[] ca = getCASystemIDs();
        int[] sdca = o.getCASystemIDs();
        if (ca.length != sdca.length) return false;
        for (int i = 0; i < ca.length; i++)
            if (ca[i] != sdca[i]) return false;

        // Compare all other fields
        return getID().equals(o.getID())
                && (getServiceDetailsHandle() == o.getServiceDetailsHandle() || (getServiceDetailsHandle() != null && getServiceDetailsHandle().equals(
                        o.getServiceDetailsHandle())))
                && getLocator().equals(o.getLocator())
                && getSourceID() == o.getSourceID()
                && getProgramNumber() == o.getProgramNumber()
                && (getTransportStream() == o.getTransportStream() || (getTransportStream() != null && getTransportStream().equals(
                        o.getTransportStream())))
                && (getLongName() == o.getLongName() || (getLongName() != null && getLongName().equals(o.getLongName())))
                && getService().equals(o.getService()) && getDeliverySystemType().equals(o.getDeliverySystemType())
                && getServiceInformationType().equals(o.getServiceInformationType())
                && getUpdateTime().equals(o.getUpdateTime()) && isFree() == o.isFree() && getPcrPID() == o.getPcrPID();
    }

    // Description copied from SIElement
    public int hashCode()
    {
        return getLocator().hashCode();
    }

    // Description copied from SIElement
    public ServiceInformationType getServiceInformationType()
    {
        return data.serviceInformationType;
    }

    // Description copied from SIRetrievable
    public Date getUpdateTime()
    {
        return data.updateTime;
    }

    // Description copied from CAIdentification
    public int[] getCASystemIDs()
    {
        return data.caSystemIDs;
    }

    // Description copied from CAIdentification
    public boolean isFree()
    {
        return data.isFree;
    }

    // Description copied from Object
    public String toString()
    {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + "[uniqueID=" + getID()
                + ", locator=" + getLocator() + ", handle=" + getServiceDetailsHandle() + ", sourceID=" + getSourceID()
                + ", programNumber=" + getProgramNumber() + ", appID=" + getAppID() + ", longName=" + getLongName()
                + ", deliverySystemType=" + getDeliverySystemType() + ", serviceInformationType="
                + getServiceInformationType() + ", updateTime=" + getUpdateTime() + ", caSystemIDs="
                + Arrays.toString(getCASystemIDs()) + ", isFree=" + isFree() + ", lsData"
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

        /** The service details handle */
        public ServiceDetailsHandle serviceDetailsHandle;

        /** The source ID of the service */
        public int sourceID;

        /** The app ID of the service (for DSG only) */
        public int appID;

        /** The program number for the service */
        public int programNumber;

        /**
         * The handle to the transport stream which contains this service
         * details
         */
        public TransportStream transportStream;

        /** The full service name */
        public MultiString longName;

        /** The service to which this service details belongs */
        public Service service;

        /** The delivery system type of this transport */
        public DeliverySystemType deliverySystemType;

        /** The SI format in which element was delivered */
        public ServiceInformationType serviceInformationType;

        /**
         * The time when this object was last updated from data in the broadcast
         */
        public Date updateTime;

        /** The array of CA system IDs associated with the service */
        public int[] caSystemIDs;

        /** True if the service is not protected by CA */
        public boolean isFree;

        /** The PCR PID for this service details */
        public int pcrPID;
    }

    /**
     * The language specific variant data
     */
    static class LSData
    {
        /** The preferred language */
        public String language;
    }

    /** Listener list for service component change listeners */
    private static ListenerList serviceComponentChangeListenerList = new ListenerList(
            ServiceComponentChangeListener.class);

    /** Service component change listener */
    static ServiceComponentChangeListener serviceComponentChangeListener = new ServiceComponentChangeListener()
    {
        public void notifyChange(ServiceComponentChangeEvent event)
        {
            ServiceComponentExt sc = (ServiceComponentExt) event.getServiceComponent();
            ServiceDetailsExt sd = (ServiceDetailsExt) sc.getServiceDetails();
            Object id = sd.getID();
            serviceComponentChangeListenerList.postEvent(id, event);
        }
    };
}
