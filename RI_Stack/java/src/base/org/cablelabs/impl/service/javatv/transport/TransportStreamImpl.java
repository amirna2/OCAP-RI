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

package org.cablelabs.impl.service.javatv.transport;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;

import org.davic.net.InvalidLocatorException;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.service.ServiceCollection;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.SIRequestorImpl;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.service.TransportStreamHandle;

/**
 * The <code>TransportStream</code> implementation.
 * 
 * @author Todd Earles
 */
public class TransportStreamImpl extends TransportStreamExt
{
    /**
     * Construct a <code>TransportStream</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param transportStreamHandle
     *            The transport stream handle
     * @param transport
     *            The transport that carries this transport stream
     * @param frequency
     *            The frequency that carries the transport stream
     * @param modulationFormat
     *            The modulation format used for the service
     * @param network
     *            The network that carries this transport stream
     * @param tsid
     *            The ID of this transport stream
     * @param description
     *            The textual name or description of this transport stream or
     *            the empty string if not available
     * @param serviceInformationType
     *            The SI format in which element was delivered
     * @param updateTime
     *            The time when this object was last updated from data in the
     *            broadcast
     * @param uniqueID
     *            The uniqueID to use for this instance or null one should be
     *            created.
     */
    public TransportStreamImpl(SICache siCache, TransportStreamHandle transportStreamHandle, Transport transport,
            int frequency, int modulationFormat, Network network, int tsid, String description,
            ServiceInformationType serviceInformationType, Date updateTime, Object uniqueID)
    {
        // Check all parameters
        if (siCache == null || transportStreamHandle == null || transport == null || network == null
                || description == null || serviceInformationType == null || updateTime == null
                || !(transport instanceof TransportExt)) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        data = new Data();
        sdsData = new SDSData();

        // Save all values
        data.siObjectID = (uniqueID == null) ? "TransportStreamImpl" + transportStreamHandle.getHandle() : uniqueID;
        data.transportStreamHandle = transportStreamHandle;
        data.transport = (TransportExt) transport;
        data.frequency = frequency;
        data.modulationFormat = modulationFormat;
        data.network = network;
        data.tsid = tsid;
        data.description = description;
        data.serviceInformationType = serviceInformationType;
        data.updateTime = updateTime;
        sdsData.serviceDetails = null;

        // Create ID for this SI object
        data.siObjectID = "TransportStreamImpl" + transportStreamHandle.getHandle();
    }

    /**
     * Construct a <code>TransportStream</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param data
     *            The shared data
     * @param sdsData
     *            The service details specific variant data or null if none
     * @throws IllegalArgumentException
     *             If <code>data</code> is null
     */
    protected TransportStreamImpl(SICache siCache, Data data, SDSData sdsData)
    {
        if (siCache == null || data == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        this.data = data;
        this.sdsData = sdsData;
    }

    // Description copied from Transport
    public TransportStream createSnapshot(SICache siCache)
    {
        return new TransportStreamImpl(siCache, data, sdsData);
    }

    // The data objects
    private final Data data;

    private final SDSData sdsData;

    // Description copied from UniqueIdentifier
    public Object getID()
    {
        return data.siObjectID;
    }

    // Description copied from TransportStreamExt
    public ServiceDetails getServiceDetails()
    {
        return sdsData.serviceDetails;
    }

    // Description copied from ServiceDetailsVariant
    public Object createServiceDetailsSpecificVariant(ServiceDetails serviceDetails)
    {
        SDSData d = new SDSData();
        d.serviceDetails = serviceDetails;
        return new TransportStreamImpl(siCache, data, d);
    }

    // Description copied from TransportStreamExt
    public TransportStreamHandle getTransportStreamHandle()
    {
        return data.transportStreamHandle;
    }

    // Description copied from TransportStreamExt
    public Transport getTransport()
    {
        return data.transport;
    }

    // Description copied from TransportStreamExt
    public int getFrequency()
    {
        return data.frequency;
    }

    // Description copied from TransportStreamExt
    public int getModulationFormat()
    {
        return data.modulationFormat;
    }

    // Description copied from TransportStreamExt
    public Network getNetwork()
    {
        return data.network;
    }

    // Description copied from TransportStreamExt
    public ServiceDetails[] getAllServiceDetails()
    {
        try
        {
            // TODO(Todd): Add a method to the cache that allows retrieval
            // of all service details for a specific transport stream. Use it
            // here
            // instead of having to iterate through all services.

            // Get the list of all JavaTV services
            ServiceCollection collection = new ServiceCollection();
            siCache.getAllServices(collection, "");
            Vector services = collection.getServices();

            // Get the JavaTV transport dependent services and only keep those
            // carried by this transport stream.
            Vector results = new Vector();
            Enumeration e = services.elements();
            while (e.hasMoreElements())
            {
                ServiceExt service = (ServiceExt) e.nextElement();
                ServiceDetailsExt details = (ServiceDetailsExt) service.getDetails();
                int frequency = ((TransportStreamExt) details.getTransportStream()).getFrequency();
                if (frequency == getFrequency()) results.add(details);
            }

            // Return the array of services carried on this transport stream
            return (ServiceDetails[]) results.toArray(new ServiceDetails[0]);
        }
        catch (Exception e)
        {
            // No services available
            return new ServiceDetails[0];
        }
    }


    public SIRequest retrieveTsID(SIRequestor requestor)
    {
        return siCache.retrieveTsID(this, null, requestor);
    }
    
    // Description copied from TransportStream
    public String getDescription()
    {
        return data.description;
    }

    // Description copied from SIElement
    public Locator getLocator()
    {
        try
        {
            if (data.transportStreamLocator == null)
                data.transportStreamLocator = new OcapLocator(data.frequency, data.modulationFormat);
        }
        catch (InvalidLocatorException e)
        {
            // TODO(Todd): This should never happen so log it.
        }

        return data.transportStreamLocator;
    }

    // Description copied from SIElement
    public boolean equals(Object obj)
    {
        // Make sure we have a good object
        if (this == obj) return true;
        if (obj == null || obj.getClass() != getClass()) return false;

        // Compare all other fields
        TransportStreamImpl o = (TransportStreamImpl) obj;
        int tsid1 = getTransportStreamID();
        int tsid2 = o.getTransportStreamID();
        return getID().equals(o.getID())
                && getTransportStreamHandle().getHandle() == o.getTransportStreamHandle().getHandle()
                && getTransport().equals(o.getTransport()) && getFrequency() == o.getFrequency()
                && getModulationFormat() == o.getModulationFormat() && getNetwork().equals(o.getNetwork())
                && (tsid1 == tsid2 || tsid1 == -1 || tsid2 == -1) && getDescription().equals(o.getDescription())
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
                + ", locator=" + getLocator() + ", handle=" + getTransportStreamHandle() + ", frequency="
                + getFrequency() + ", modulationFormat=" + getModulationFormat() + ", tsid=" + data.tsid
                + ", description=" + getDescription() + ", serviceInformationType=" + getServiceInformationType()
                + ", updateTime=" + getUpdateTime() + ", sdsData"
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

        /** The transport stream handle */
        public TransportStreamHandle transportStreamHandle;

        /** The transport that carries this transport stream */
        public TransportExt transport;

        /** The frequency that carries this transport stream */
        public int frequency;

        /** The modulation format used for the service */
        public int modulationFormat;

        /** The network that carries this transport stream */
        public Network network;

        /** The ID of this transport stream */
        public int tsid;

        /** The textual name or description of this transport stream */
        public String description;

        /** The SI format in which element was delivered */
        public ServiceInformationType serviceInformationType;

        /**
         * The time when this object was last updated from data in the broadcast
         */
        public Date updateTime;

        /** The locator for this object */
        public OcapLocator transportStreamLocator = null;
    }

    /**
     * The service details specific variant data
     */
    static class SDSData
    {
        /** The service details */
        public ServiceDetails serviceDetails;
    }
}
