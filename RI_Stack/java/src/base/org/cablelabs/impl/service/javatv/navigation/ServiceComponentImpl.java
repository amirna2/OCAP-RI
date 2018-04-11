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
import javax.tv.service.SIException;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.StreamType;

import org.davic.net.InvalidLocatorException;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.service.SIRequestImpl;
import org.cablelabs.impl.service.OcapLocatorImpl;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceComponentHandle;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.string.MultiString;
import org.apache.log4j.Logger;

/**
 * The <code>ServiceComponent</code> implementation.
 * 
 * @author Todd Earles
 */
public class ServiceComponentImpl extends ServiceComponentExt
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(ServiceComponentImpl.class);
    
    /**
     * Construct a <code>ServiceComponent</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param serviceComponentHandle
     *            The service component handle
     * @param serviceDetails
     *            The service details to which this component belongs
     * @param pid
     *            The PID of the elementary stream that carries this component
     * @param componentTag
     *            The component tag for this service component or
     *            COMPONENT_TAG_UNDEFINED if there is no component tag for this
     *            component.
     * @param associationTag
     *            The association tag for this service component or
     *            ASSOCIATION_TAG_UNDEFINED if there is no association tag for
     *            this component.
     * @param carouselID
     *            The carousel ID for the carousel associated with this
     *            component or CAROUSEL_ID_UNDEFINED if there is no carousel
     *            associated with this component.
     * @param componentName
     *            The component name or null if not available
     * @param associatedLanguage
     *            The language used in the elementary stream
     * @param streamType
     *            The elementary stream type of this component as defined in the
     *            PMT
     * @param service
     *            The service to which this service component belongs
     * @param serviceInformationType
     *            The SI format in which element was delivered
     * @param updateTime
     *            The time when this object was last updated from data in the
     *            broadcast
     * @param uniqueID
     *            The uniqueID to use for this instance or null one should be
     *            created.
     */
    public ServiceComponentImpl(SICache siCache, ServiceComponentHandle serviceComponentHandle,
            ServiceDetails serviceDetails, int pid, long componentTag, long associationTag, long carouselID,
            MultiString componentName, String associatedLanguage, short streamType,
            ServiceInformationType serviceInformationType, Date updateTime, Object uniqueID)
    {
        // Check all parameters
        if (siCache == null || serviceComponentHandle == null || serviceDetails == null || associatedLanguage == null
                || serviceInformationType == null || updateTime == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        data = new Data();
        lsData = null;
        sdsData = null;

        // Save all values
        data.siObjectID = (uniqueID == null) ? "ServiceComponentImpl" + serviceComponentHandle.getHandle() : uniqueID;
        data.serviceComponentHandle = serviceComponentHandle;
        data.serviceDetails = serviceDetails;
        data.pid = pid;
        data.componentTag = componentTag;
        data.associationTag = associationTag;
        data.carouselID = carouselID;
        data.componentName = componentName;
        data.associatedLanguage = associatedLanguage;
        data.streamType = streamType;
        data.serviceInformationType = serviceInformationType;
        data.updateTime = updateTime;
        
        if (log.isDebugEnabled())
        {
            log.debug("ServiceComponentImpl serviceDetails: " + serviceDetails);
        }
    }

    /**
     * Construct a <code>ServiceComponent</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param data
     *            The shared data
     * @param lsData
     *            The language specific variant data or null if none
     * @param sdsData
     *            The service details specific variant data or null if none
     * @throws IllegalArgumentException
     *             If <code>data</code> is null
     */
    protected ServiceComponentImpl(SICache siCache, Data data, LSData lsData, SDSData sdsData)
    {
        if (siCache == null || data == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        this.data = data;
        this.lsData = lsData;
        this.sdsData = sdsData;
    }

    // Description copied from Transport
    public ServiceComponent createSnapshot(SICache siCache)
    {
        return new ServiceComponentImpl(siCache, data, lsData, sdsData);
    }

    // The data objects
    private final Data data;

    protected final LSData lsData;

    protected final SDSData sdsData;

    // Description copied from UniqueIdentifier
    public Object getID()
    {
        return data.siObjectID;
    }

    // Description copied from ServiceComponentExt
    public ServiceComponentHandle getServiceComponentHandle()
    {
        return data.serviceComponentHandle;
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
        return new ServiceComponentImpl(siCache, data, d, sdsData);
    }

    // Description copied from ServiceDetailsVariant
    public Object createServiceDetailsSpecificVariant(ServiceDetails serviceDetails)
    {
        SDSData d = new SDSData();
        d.serviceDetails = serviceDetails;
        return new ServiceComponentImpl(siCache, data, lsData, d);
    }

    // Description copied from ServiceComponentExt
    public ServiceDetails getServiceDetails()
    {
        if(sdsData != null)
        {
            return sdsData.serviceDetails;
        }
        
        return data.serviceDetails;
    }

    // Description copied from ServiceComponentExt
    public int getPID()
    {
        return data.pid;
    }

    // Description copied from ServiceComponentExt
    public int getComponentTag() throws SIException
    {
        if (data.componentTag == COMPONENT_TAG_UNDEFINED)
            throw new SIException("Component tag not defined");
        else
            return (int) data.componentTag;
    }

    // Description copied from ServiceComponentExt
    public int getAssociationTag() throws SIException
    {
        if (data.associationTag == ASSOCIATION_TAG_UNDEFINED)
            throw new SIException("Association tag not defined");
        else
            return (int) data.associationTag;
    }

    // Description copied from ServiceComponentExt
    public int getCarouselID() throws SIException
    {
        if (data.carouselID == CAROUSEL_ID_UNDEFINED)
            throw new SIException("Carousel ID not defined");
        else
            return (int) data.carouselID;
    }

    // Description copied from ServiceComponentExt
    public short getElementaryStreamType()
    {
        return data.streamType;
    }

    // Description copied from ServiceComponent
    public String getName()
    {
        String language = (lsData == null) ? null : lsData.language;
        return (data.componentName == null) ? "" : data.componentName.getValue(language);
    }

    // Description copied from ServiceExt
    public MultiString getNameAsMultiString()
    {
        return data.componentName;
    }

    // Description copied from ServiceComponent
    public String getAssociatedLanguage()
    {
        return data.associatedLanguage;
    }

    // Description copied from ServiceComponent
    public StreamType getStreamType()
    {
        switch (data.streamType)
        {
            case org.ocap.si.StreamType.MPEG_1_VIDEO:
            case org.ocap.si.StreamType.MPEG_2_VIDEO:
            case org.ocap.si.StreamType.VIDEO_DCII:
            case org.ocap.si.StreamType.AVC_VIDEO:
                return StreamType.VIDEO;
            case org.ocap.si.StreamType.MPEG_1_AUDIO:
            case org.ocap.si.StreamType.MPEG_2_AUDIO:
            case org.ocap.si.StreamType.ATSC_AUDIO:
            case org.ocap.si.StreamType.AAC_ADTS_AUDIO:
            case org.ocap.si.StreamType.AAC_AUDIO_LATM:
            case org.ocap.si.StreamType.ENHANCED_ATSC_AUDIO:
                return StreamType.AUDIO;
            case org.ocap.si.StreamType.MPEG_PRIVATE_DATA:
            case org.ocap.si.StreamType.DSM_CC:
            case org.ocap.si.StreamType.DSM_CC_MPE:
            case org.ocap.si.StreamType.DSM_CC_UN:
            case org.ocap.si.StreamType.DSM_CC_STREAM_DESCRIPTORS:
            case org.ocap.si.StreamType.DSM_CC_SECTIONS:
            case org.ocap.si.StreamType.METADATA_PES:
                return StreamType.DATA;
            case org.ocap.si.StreamType.MPEG_PRIVATE_SECTION:
            case org.ocap.si.StreamType.SYNCHRONIZED_DOWNLOAD:
            case org.ocap.si.StreamType.METADATA_SECTIONS:
            case org.ocap.si.StreamType.METADATA_DATA_CAROUSEL:
            case org.ocap.si.StreamType.METADATA_OBJECT_CAROUSEL:
            case org.ocap.si.StreamType.METADATA_SYNCH_DOWNLOAD:
                return StreamType.SECTIONS;
            case org.ocap.si.StreamType.STD_SUBTITLE:
                return StreamType.SUBTITLES;
            default:
                return StreamType.UNKNOWN;
        }
    }

    // Description copied from ServiceComponent
    public Service getService()
    {
        // TODO(Todd): This method should be removed from ServiceComponentExt
        // and all sub-classes (including this one).
        return data.serviceDetails.getService();
    }

    // Description copied from SIElement
    public Locator getLocator()
    {        
        // Only create the object once - reuse on subsequent calls
        if (locator == null)
        {
            try
            {
                // Create the service component locator
                String url = getServiceDetails().getLocator().toExternalForm();
              
                // We want to eliminate non-ocap locators here
                // (eg: HN contentItem locators start with 'http://' )
                // In that case we will not create ServiceComponent locators
                if (url.startsWith("ocap:"))
                {
                    url += ".+0x" + Integer.toHexString(data.pid);
                    if(getServiceDetails().getLocator() instanceof OcapLocatorImpl)
                        locator = new OcapLocatorImpl(url);
                    else
                        locator = new OcapLocator(url);
                    if (log.isDebugEnabled())
                    {
                        log.debug("ServiceComponentImpl getLocator locator: " + locator);
                    }
                }
                // TODO(Todd): Create the "tag" form of the locator if a
                // component tag is available
                
                //if (LOGGING) 
                //    log.debug("ServiceComponentImpl getLocator url: " + url);
            }
            catch (InvalidLocatorException e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        }

        return locator;
    }

    // Description copied from SIElement
    public boolean equals(Object obj)
    {
        // Make sure we have a good object
        if (this == obj) return true;
        if (obj == null || obj.getClass() != getClass()) return false;

        // Compare fields
        ServiceComponentImpl o = (ServiceComponentImpl) obj;
        return getID().equals(o.getID())
                && getServiceComponentHandle().getHandle() == o.getServiceComponentHandle().getHandle()
                && getServiceDetails().equals(o.getServiceDetails()) && getPID() == o.getPID()
                && data.componentTag == o.data.componentTag && data.associationTag == o.data.associationTag
                && data.carouselID == o.data.carouselID
                && (getName() == o.getName() || (getName() != null && getName().equals(o.getName())))
                && getAssociatedLanguage().equals(o.getAssociatedLanguage())
                && getStreamType().equals(o.getStreamType())
                && getServiceInformationType().equals(o.getServiceInformationType())
                && getUpdateTime().equals(o.getUpdateTime());
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

    // Description copied from Object
    public String toString()
    {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + "[uniqueID=" + getID()
                /*+ ", locator=" + getLocator() */
                + ", handle=" + getServiceComponentHandle() + ", pid=" + getPID()
                + ", componentTag=" + data.componentTag + ", associationTag=" + data.associationTag + ", carouselID="
                + data.carouselID + ", componentName=" + getName() + ", associatedLanguage=" + getAssociatedLanguage()
                + ", streamType=" + getStreamType() + ", serviceInformationType=" + getServiceInformationType()
                + ", updateTime=" + getUpdateTime() + ", lsData"
                + ((lsData == null) ? "=null" : "[language=" + lsData.language + "]") + ", sdsData"
                + ((sdsData == null) ? "=null" : "[serviceDetails=" + sdsData.serviceDetails + "]") + "]";
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

        /** The service component handle */
        public ServiceComponentHandle serviceComponentHandle;

        /** The service details to with this component belongs */
        public ServiceDetails serviceDetails;

        /** The PID of the elementary stream that carries this component */
        public int pid;

        /**
         * The component tag for this service component. The actual value is
         * only 32 bits. However, we store it in a long (64 bits) so we have
         * room for the "UNDEFINED" value.
         */
        public long componentTag;

        /**
         * The association tag for this service component. The actual value is
         * only 32 bits. However, we store it in a long (64 bits) so we have
         * room for the "UNDEFINED" value.
         */
        public long associationTag;

        /**
         * The carousel ID for the carousel associated with this service
         * component The actual value is only 32 bits. However, we store it in a
         * long (64 bits) so we have room for the "UNDEFINED" value.
         */
        public long carouselID;

        /** The component name */
        public MultiString componentName;

        /** The language used in the elementary stream */
        public String associatedLanguage;

        /** The elementary stream type of this component as defined in the PMT */
        public short streamType;

        /** The SI format in which element was delivered */
        public ServiceInformationType serviceInformationType;

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

    /**
     * The service details specific variant data
     */
    static class SDSData
    {
        /** The service details */
        public ServiceDetails serviceDetails;
    }
    /** The locator for this service component(is dependent on ServiceDetails) */
    protected Locator locator = null;
}
