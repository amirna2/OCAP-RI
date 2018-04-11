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

import java.util.Date;

import javax.tv.locator.Locator;
import javax.tv.service.SIManager;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.ServiceType;
import javax.tv.service.guide.ProgramSchedule;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceComponentChangeEvent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.TransportStream;

import org.dvb.spi.selection.ServiceDescription;
import org.dvb.spi.selection.ServiceReference;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.service.SICacheImpl;
import org.cablelabs.impl.manager.service.SIRequestServiceComponents;
import org.cablelabs.impl.service.ListenerList;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.string.MultiString;

public class SPIServiceDetails extends ServiceDetailsExt
{
    /**
     * Construct a <code>SPIServiceDetails</code>. It is the callers
     * responsibility to ensure that this constructor is called only while
     * executing within the caller context of the provider who is making this
     * service available. This allows us to safely call methods on provider
     * supplied objects.
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param sourceID
     *            The source ID of the service
     * @param service
     *            The service to which this service details belongs
     * @param deliverySystemType
     *            The delivery system type of this transport
     * @param actualLocator
     *            The actual locator where the service is currently carried or
     *            null if not currently available.
     * @param description
     *            The service description or null if not available
     * @param uniqueID
     *            The uniqueID to use for this instance or null one should be
     *            created.
     */
    public SPIServiceDetails(SICache siCache, int sourceID, SPIService service, DeliverySystemType deliverySystemType,
            Locator actualLocator, ServiceDescription description, Object uniqueID)
    {
        // Check all parameters
        if (siCache == null || service == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        data = new Data();
        lsData = null;

        // Save all values
        data.siObjectID = (uniqueID == null) ? "SPIServiceDetails" + nextUnusedID++ : uniqueID;
        data.sourceID = sourceID;
        data.service = service;
        data.deliverySystemType = deliverySystemType;
        data.serviceDescription = description;
        data.updateTime = new Date();

        // Get the actual ServiceDetails if this service is currently mapped
        if (actualLocator != null)
        {
            SIManagerExt siManager = (SIManagerExt) SIManager.createInstance();
            try
            {
                data.mappedServiceDetails = (ServiceDetailsExt) siManager.getServiceDetails(actualLocator)[0];
            }
            catch (Exception x)
            {
                SystemEventUtil.logRecoverableError("Cannot get ServiceDetails for actualLocator", x);
            }
        }

        // Register static listener with the cache
        siCache.addServiceComponentChangeListener(serviceComponentChangeListener);
    }

    /**
     * Construct a <code>SPIServiceDetails</code>
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
    protected SPIServiceDetails(SICache siCache, Data data, LSData lsData)
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
        return new SPIServiceDetails(siCache, data, lsData);
    }

    // The data objects
    protected final Data data;

    private final LSData lsData;

    // Description copied from UniqueIdentifier
    public Object getID()
    {
        return data.siObjectID;
    }

    /**
     * Return the {@link SelectionProviderInstance} which provides this service.
     */
    public ProviderInstance getProviderInstance()
    {
        return data.service.getProviderInstance();
    }

    /**
     * Return the {@link ServiceReference} which created this service.
     */
    public ServiceReference getServiceReference()
    {
        return data.service.getServiceReference();
    }

    /**
     * Return true if this service details is mapped
     */
    public boolean isMapped()
    {
        return (data.mappedServiceDetails != null);
    }

    // Description copied from ServiceDetailsExt
    public ServiceDetailsHandle getServiceDetailsHandle()
    {
        //return null;
        if (data.mappedServiceDetails == null)
            return null;
        else
            return data.mappedServiceDetails.getServiceDetailsHandle();
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
        return new SPIServiceDetails(siCache, data, d);
    }

    // Description copied from ServiceDetailsExt
    public int getSourceID()
    {
        return data.sourceID;
    }

    public int getAppID()
    {
        // Fix me!!
        return -1;
    }

    // Description copied from ServiceDetailsExt
    public int getProgramNumber()
    {
        if (data.mappedServiceDetails == null)
            return -1;
        else
            return data.mappedServiceDetails.getProgramNumber();
    }

    // Description copied from ServiceDetailsExt
    public TransportStream getTransportStream()
    {
        if (data.mappedServiceDetails == null)
            return null;
        else
        {
            TransportStreamExt ts = (TransportStreamExt) data.mappedServiceDetails.getTransportStream();
            return (TransportStream) ts.createServiceDetailsSpecificVariant(this);
        }
    }

    // Description copied from ServiceDetailsExt
    public int getPcrPID()
    {
        if (data.mappedServiceDetails == null)
            return -1;
        else
            return data.mappedServiceDetails.getPcrPID();
    }
    
    public SIRequest retrievePcrPID(SIRequestor requestor)
    {
        return null;
    }

    // Description copied from ServiceDetails
    public ServiceType getServiceType()
    {
        return data.service.getServiceType();
    }

    // Description copied from ServiceDetails
    public SIRequest retrieveServiceDescription(SIRequestor requestor)
    {
        return failAsyncRequest(requestor);
    }

    // Description copied from ServiceDetailsExt
    public SIRequest retrieveDefaultMediaComponents(SIRequestor requestor)
    {
        if (data.mappedServiceDetails == null) return failAsyncRequest(requestor);

        SIRequestor r = new ComponentRequestor(this, requestor);
        return data.mappedServiceDetails.retrieveDefaultMediaComponents(r);
    }

    // Description copied from ServiceDetailsExt
    public SIRequest retrieveCarouselComponent(SIRequestor requestor)
    {
        if (data.mappedServiceDetails == null) return failAsyncRequest(requestor);

        SIRequestor r = new ComponentRequestor(this, requestor);
        return data.mappedServiceDetails.retrieveCarouselComponent(r);
    }

    // Description copied from ServiceDetailsExt
    public SIRequest retrieveCarouselComponent(SIRequestor requestor, int carouselID)
    {
        if (data.mappedServiceDetails == null) return failAsyncRequest(requestor);

        SIRequestor r = new ComponentRequestor(this, requestor);
        return data.mappedServiceDetails.retrieveCarouselComponent(r, carouselID);
    }

    // Description copied from ServiceDetailsExt
    public SIRequest retrieveComponentByAssociationTag(SIRequestor requestor, int associationTag)
    {
        if (data.mappedServiceDetails == null) return failAsyncRequest(requestor);

        SIRequestor r = new ComponentRequestor(this, requestor);
        return data.mappedServiceDetails.retrieveComponentByAssociationTag(r, associationTag);
    }

    // Description copied from ServiceDetails
    public SIRequest retrieveComponents(SIRequestor requestor)
    {
        if (data.mappedServiceDetails == null) return failAsyncRequest(requestor);

        SIRequestor r = new ComponentRequestor(this, requestor);
        return data.mappedServiceDetails.retrieveComponents(r);
    }

    /**
     * Retrieves an array of the <code>ServiceComponent</code> objects carried
     * in this <code>ServiceDetails</code> which match at least one of the
     * following criteria.
     * <ul>
     * <li>The component PID matches an entry in <code>pids</code>
     * <li>The component name matches an entry in <code>componentNames</code>
     * <li>The component tag matches an entry in <code>componentTags</code>
     * <li>The stream type matches an entry in <code>streamTypes</code> and its
     * corresponding entry in <code>indexes</code> or <code>languageCodes</code>.
     * </ul>
     * <p>
     * The array will only contain <code>ServiceComponent</code> instances
     * <code>c</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(c.getLocator())</code>. If no
     * <code>ServiceComponent</code> instances meet this criteria, this method
     * will result in an <code>SIRequestFailureType</code> of
     * <code>DATA_UNAVAILABLE</code>.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param pids
     *            An array of valid component PIDs. A value of null or an empty
     *            array indicate that component PIDs should not be used to
     *            select components.
     * @param componentNames
     *            An array of valid component names. A value of null or an empty
     *            array indicate that component names should not be used to
     *            select components.
     * @param componentTags
     *            An array of valid component tags. A value of null or an empty
     *            array indicate that component tags should not be used to
     *            select components.
     * @param streamTypes
     *            An array of valid stream types. A value of null or an empty
     *            array indicate that stream types should not be used to select
     *            components.
     * @param indexes
     *            Each entry in this array specifies an index for the
     *            corresponding entry in the <code>streamTypes</code> array.
     *            This index indicates which component of the given stream type
     *            is to be selected. An index of 0 specifies the first component
     *            with the given stream type, an index of 1 specifies the second
     *            component with the given stream type, and so forth. An index
     *            of -1 is treated as a value of 0 so it specifies the first
     *            component. If indexes is an empty array then the first
     *            component of each given stream type is selected.
     * @param languageCodes
     *            Each entry in this array specifies a language code for the
     *            corresponding entry in the <code>streamTypes</code> array. The
     *            first component with the specified language code and
     *            corresponding stream type is selected. If the language code is
     *            the empty string then the first component of the corresponding
     *            stream type is selected. If languageCodes is an empty array
     *            then the first component of each given stream type is
     *            selected.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @throws ArrayIndexOutOfBoundsException
     *             The <code>indexes</code> array or the
     *             <code>languageCodes</code> array is non-null and contains
     *             less entries than the corresponding <code>streamTypes</code>
     *             array.
     */
    public SIRequest retrieveComponents(int pids[], String[] componentNames, int[] componentTags, short[] streamTypes,
            int[] indexes, String[] languageCodes, final SIRequestor requestor)
    {
        // Fail the request if an actual ServiceDetails is not mapped
        if (data.mappedServiceDetails == null) return failAsyncRequest(requestor);

        // Create the requestor object which will receive the components
        SIRequestor componentRequestor = new ComponentRequestor(this, requestor);

        // Create the component request
        final SIRequestServiceComponents componentRequest = new SIRequestServiceComponents((SICacheImpl) siCache,
                data.mappedServiceDetails, pids, componentNames, componentTags, streamTypes, indexes, languageCodes,
                getPreferredLanguage(), componentRequestor);

        // Enqueue the component request with the SI cache
        siCache.enqueueServiceDetailsRequest(componentRequest);

        // Return the request to the caller
        return componentRequest;
    }

    /**
     * A component requestor which handles translating the results of the actual
     * component request. This translation is necessary to ensure that the
     * components returned to the caller are associated with this
     * SPIServiceDetails instead of the original broadcast ServiceDetails.
     */
    private static class ComponentRequestor implements SIRequestor
    {
        // The ServiceDetails which returned components should refer to
        private ServiceDetails details;

        // The wrapped requestor
        private SIRequestor requestor;

        // Construct the wrapper
        public ComponentRequestor(ServiceDetails details, SIRequestor requestor)
        {
            this.details = details;
            this.requestor = requestor;
        }

        // Description copied from SIRequestor
        public void notifyFailure(SIRequestFailureType reason)
        {
            // Pass on the failure reason to the original caller
            requestor.notifyFailure(reason);
        }

        // Description copied from SIRequestor
        public void notifySuccess(SIRetrievable[] result)
        {
            // Translate components and return them to the original caller
            ServiceComponentExt[] components = new ServiceComponentExt[result.length];
            for (int i = 0; i < result.length; i++)
            {
                ServiceComponentExt component = (ServiceComponentExt) result[i];
                components[i] = (ServiceComponentExt) component.createServiceDetailsSpecificVariant(details);
            }
            requestor.notifySuccess(components);
        }
    }

    // Description copied from ServiceDetails
    public ProgramSchedule getProgramSchedule()
    {
        if (data.mappedServiceDetails == null)
            return null;
        else
            return data.mappedServiceDetails.getProgramSchedule();
    }

    /**
     * Return an SIRequest that will immediately fail.
     * 
     * @return The SIRequest
     */
    private SIRequest failAsyncRequest(final SIRequestor requestor)
    {
        // Create the request object
        SIRequest request = new SIRequest()
        {
            public boolean cancel()
            {
                return false;
            }
        };

        // Invoke the failure
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ccm.getCurrentContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                requestor.notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
            }
        });

        return request;
    }

    // Description copied from ServiceDetails
    public String getLongName()
    {
        if (data.serviceDescription == null)
            return "";
        else
        {
            String language = (lsData == null) ? null : lsData.language;
            return data.service.getName(language);
        }
    }

    // Description copied from ServiceDetailsExt
    public MultiString getLongNameAsMultiString()
    {
        return null;
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
        return data.service.getServiceNumber();
    }

    // Description copied from ServiceMinorNumber
    public int getMinorNumber()
    {
        return data.service.getMinorNumber();
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
        SPIServiceDetails o = (SPIServiceDetails) obj;
        int[] ca = getCASystemIDs();
        int[] sdca = o.getCASystemIDs();
        if (ca.length != sdca.length) return false;
        for (int i = 0; i < ca.length; i++)
            if (ca[i] != sdca[i]) return false;

        // Compare all other fields
        return getID().equals(o.getID())
                && getLocator().equals(o.getLocator())
                && getSourceID() == o.getSourceID()
                && getProgramNumber() == o.getProgramNumber()
                && (getTransportStream() == o.getTransportStream() || (getTransportStream() != null && getTransportStream().equals(
                        o.getTransportStream())))
                && (getLongName() == o.getLongName() || (getLongName() != null && getLongName().equals(o.getLongName())))
                && getDeliverySystemType().equals(o.getDeliverySystemType())
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
        return ServiceInformationType.UNKNOWN;
    }

    // Description copied from SIRetrievable
    public Date getUpdateTime()
    {
        return data.updateTime;
    }

    // Description copied from CAIdentification
    public int[] getCASystemIDs()
    {
        if (data.mappedServiceDetails == null)
            return new int[0];
        else
            return data.mappedServiceDetails.getCASystemIDs();
    }

    // Description copied from CAIdentification
    public boolean isFree()
    {
        if (data.mappedServiceDetails == null)
            return true;
        else
            return data.mappedServiceDetails.isFree();
    }

    // Description copied from Object
    public String toString()
    {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + "[uniqueID=" + getID()
                + ", locator=" + getLocator() + ", sourceID=" + getSourceID() + ", programNumber=" + getProgramNumber()
                + ", longName=" + getLongName() + ", deliverySystemType=" + getDeliverySystemType()
                + ", serviceInformationType=" + getServiceInformationType() + ", updateTime=" + getUpdateTime()
                + ", caSystemIDs=" + Arrays.toString(getCASystemIDs()) + ", isFree=" + isFree() 
                + ", lsData" + ((lsData == null) ? "=null" : "[language=" + lsData.language + "]") + "]";
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

        /** The source ID of the service */
        public int sourceID;

        /** The service to which this service details belongs */
        public SPIService service;

        /** The delivery system type of this transport */
        public DeliverySystemType deliverySystemType;

        /** The service description */
        public ServiceDescription serviceDescription;

        /** The currently mapped ServiceDetails */
        public ServiceDetailsExt mappedServiceDetails;

        /** The time this object was created */
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

    // Next unused ID number
    private static int nextUnusedID = 1;

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
