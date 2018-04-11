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

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.Transport;

import org.davic.net.InvalidLocatorException;

import org.cablelabs.impl.service.NetworkExt;
import org.cablelabs.impl.service.NetworkHandle;
import org.cablelabs.impl.service.NetworkLocator;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.TransportExt;

/**
 * The <code>Network</code> implementation.
 * 
 * @author Todd Earles
 */
public class NetworkImpl extends NetworkExt
{
    /**
     * Construct a <code>Network</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param networkHandle
     *            The network handle
     * @param transportext
     *            The transport which carries this network
     * @param networkID
     *            The ID of this network
     * @param name
     *            The name of this network or the empty string if not available
     * @param serviceInformationType
     *            The SI format in which element was delivered
     * @param updateTime
     *            The time when this object was last updated from data in the
     *            broadcast
     * @param uniqueID
     *            The uniqueID to use for this instance or null one should be
     *            created.
     */
    public NetworkImpl(SICache siCache, NetworkHandle networkHandle, Transport transport, int networkID, String name,
            ServiceInformationType serviceInformationType, Date updateTime, Object uniqueID)
    {
        // Check all parameters
        if (siCache == null || networkHandle == null || transport == null || name == null
                || serviceInformationType == null || updateTime == null || !(transport instanceof TransportExt))
            throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        data = new Data();

        // Save all values
        data.siObjectID = (uniqueID == null) ? "NetworkImpl" + networkHandle.getHandle() : uniqueID;
        data.networkHandle = networkHandle;
        data.transport = (TransportExt) transport;
        data.networkID = networkID;
        data.name = name;
        data.serviceInformationType = serviceInformationType;
        data.updateTime = updateTime;

        // Create ID for this SI object
        data.siObjectID = "NetworkImpl" + networkHandle.getHandle();
    }

    /**
     * Construct a <code>Network</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param data
     *            The shared data
     * @throws IllegalArgumentException
     *             If <code>data</code> is null
     */
    protected NetworkImpl(SICache siCache, Data data)
    {
        if (siCache == null || data == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        this.data = data;
    }

    // Description copied from Transport
    public Network createSnapshot(SICache siCache)
    {
        return new NetworkImpl(siCache, data);
    }

    // The data objects
    private final Data data;

    // Description copied from UniqueIdentifier
    public Object getID()
    {
        return data.siObjectID;
    }

    // Description copied from NetworkExt
    public NetworkHandle getNetworkHandle()
    {
        return data.networkHandle;
    }

    // Description copied from NetworkExt
    public Transport getTransport()
    {
        return data.transport;
    }

    // Description copied from Network
    public int getNetworkID()
    {
        return data.networkID;
    }

    // Description copied from Network
    public String getName()
    {
        return data.name;
    }

    // Description copied from Network
    public SIRequest retrieveTransportStreams(SIRequestor requestor)
    {
        // Call the cache to retrieve the transport streams
        return siCache.retrieveTransportStreams(this, requestor);
    }

    // Description copied from SIElement
    public Locator getLocator()
    {
        try
        {
            if (data.networkLocator == null)
            {
                data.networkLocator = new NetworkLocator(data.transport.getTransportID(), data.networkID);
            }
        }
        catch (InvalidLocatorException e)
        {
            // TODO(Todd): This should never happen so log it.
        }

        return data.networkLocator;
    }

    // Description copied from SIElement
    public boolean equals(Object obj)
    {
        // Make sure we have a good object
        if (this == obj) return true;
        if (obj == null || obj.getClass() != getClass()) return false;

        // Compare all other fields
        NetworkImpl o = (NetworkImpl) obj;
        return getID().equals(o.getID()) && getNetworkHandle().getHandle() == o.getNetworkHandle().getHandle()
                && getTransport().equals(o.getTransport()) && getNetworkID() == o.getNetworkID()
                && getName().equals(o.getName()) && getServiceInformationType().equals(o.getServiceInformationType())
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
                + ", locator=" + getLocator() + ", handle=" + getNetworkHandle() + ", networkID=" + getNetworkID()
                + ", name=" + getName() + ", serviceInformationType=" + getServiceInformationType() + ", updateTime="
                + getUpdateTime() + "]";
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

        /** The network handle */
        public NetworkHandle networkHandle;

        /** The transport that carries this network */
        public TransportExt transport;

        /** The ID of this network */
        public int networkID;

        /** The name of this network */
        public String name;

        /** The SI format in which element was delivered */
        public ServiceInformationType serviceInformationType;

        /**
         * The time when this object was last updated from data in the broadcast
         */
        public Date updateTime;

        /** The locator for this object */
        public NetworkLocator networkLocator = null;
    }
}
