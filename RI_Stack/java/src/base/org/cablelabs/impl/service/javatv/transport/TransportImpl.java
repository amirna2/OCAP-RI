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

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.transport.NetworkChangeEvent;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStreamChangeEvent;
import javax.tv.service.transport.TransportStreamChangeListener;

import org.cablelabs.impl.service.ListenerList;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportHandle;
import org.cablelabs.impl.service.TransportStreamExt;

/**
 * The <code>Transport</code> implementation.
 * 
 * @author Todd Earles
 */
public class TransportImpl extends TransportExt
{
    /**
     * Construct a <code>Transport</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param transportHandle
     *            The transport handle
     * @param deliverySystemType
     *            The delivery system type of this transport
     * @param transportID
     *            The ID for this transport
     * @param uniqueID
     *            The uniqueID to use for this instance or null one should be
     *            created.
     */
    public TransportImpl(SICache siCache, TransportHandle transportHandle, DeliverySystemType deliverySystemType,
            int transportID, Object uniqueID)
    {
        // Check all parameters
        if (siCache == null || transportHandle == null || deliverySystemType == null)
            throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        data = new Data();

        // Save all values
        data.siObjectID = (uniqueID == null) ? "TransportImpl" + transportHandle.getHandle() : uniqueID;
        data.transportHandle = transportHandle;
        data.deliverySystemType = deliverySystemType;
        data.transportID = transportID;

        // Register static listener with the cache
        siCache.addNetworkChangeListener(networkChangeListener);
        siCache.addTransportStreamChangeListener(transportStreamChangeListener);
        siCache.addServiceDetailsChangeListener(serviceDetailsChangeListener);
    }

    /**
     * Construct a <code>Transport</code>
     * 
     * @param siCache
     *            The cache this object belongs to
     * @param data
     *            The shared data
     * @throws IllegalArgumentException
     *             If <code>data</code> is null
     */
    protected TransportImpl(SICache siCache, Data data)
    {
        if (siCache == null || data == null) throw new IllegalArgumentException();

        // Save the cache this object belongs to
        this.siCache = siCache;

        // Set the data objects for this instance
        this.data = data;
    }

    // Description copied from Transport
    public Transport createSnapshot(SICache siCache)
    {
        return new TransportImpl(siCache, data);
    }

    // The data objects
    private final Data data;

    // Description copied from UniqueIdentifier
    public Object getID()
    {
        return data.siObjectID;
    }

    // Description copied from TransportExt
    public TransportHandle getTransportHandle()
    {
        return data.transportHandle;
    }

    // Description copied from Transport
    public void addServiceDetailsChangeListener(ServiceDetailsChangeListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("null listener not supported");
        }

        serviceDetailsChangeListenerList.addListener(getID(), listener);
    }

    // Description copied from Transport
    public void removeServiceDetailsChangeListener(ServiceDetailsChangeListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("null listener not supported");
        }

        serviceDetailsChangeListenerList.removeListener(getID(), listener);
    }

    // Description copied from TransportExt
    public void postServiceDetailsChangeEvent(ServiceDetailsChangeEvent event)
    {
        if (event == null)
        {
            throw new NullPointerException("null event not supported");
        }

        serviceDetailsChangeListenerList.postEvent(getID(), event);
    }

    // Description copied from Transport
    public DeliverySystemType getDeliverySystemType()
    {
        return data.deliverySystemType;
    }

    // Description copied from TransportExt
    public int getTransportID()
    {
        return data.transportID;
    }

    // Description copied from Object
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != getClass()) return false;

        TransportImpl o = (TransportImpl) obj;
        return getID().equals(o.getID()) && getTransportHandle().getHandle() == o.getTransportHandle().getHandle()
                && getDeliverySystemType() == o.getDeliverySystemType() && getTransportID() == o.getTransportID();
    }

    // Description copied from Object
    public int hashCode()
    {
        return data.transportID * 31 + data.transportHandle.getHandle();
    }

    // Description copied from NetworkCollection
    public SIRequest retrieveNetwork(Locator locator, SIRequestor requestor) throws InvalidLocatorException,
            SecurityException
    {
        // Call the cache to retrieve the network
        return siCache.retrieveNetwork(locator, requestor);
    }

    // Description copied from NetworkCollection
    public SIRequest retrieveNetworks(SIRequestor requestor)
    {
        // Call the cache to retrieve the networks
        return siCache.retrieveNetworks(this, requestor);
    }

    // Description copied from NetworkCollection
    public void addNetworkChangeListener(NetworkChangeListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("null listener not supported");
        }

        networkChangeListenerList.addListener(getID(), listener);
    }

    // Description copied from NetworkCollection
    public void removeNetworkChangeListener(NetworkChangeListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("null listener not supported");
        }

        networkChangeListenerList.removeListener(getID(), listener);
    }

    // Description copied from TransportStreamCollection
    public SIRequest retrieveTransportStream(Locator locator, SIRequestor requestor) throws InvalidLocatorException,
            SecurityException
    {
        // Call the cache to retrieve the transport stream
        return siCache.retrieveTransportStream(locator, requestor);
    }

    // Description copied from TransportStreamCollection
    public SIRequest retrieveTransportStreams(SIRequestor requestor)
    {
        // Call the cache to retrieve the transport streams
        return siCache.retrieveTransportStreams(this, requestor);
    }

    // Description copied from TransportStreamCollection
    public void addTransportStreamChangeListener(TransportStreamChangeListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("null listener not supported");
        }

        transportStreamChangeListenerList.addListener(getID(), listener);
    }

    // Description copied from TransportStreamCollection
    public void removeTransportStreamChangeListener(TransportStreamChangeListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("null listener not supported");
        }

        transportStreamChangeListenerList.removeListener(getID(), listener);
    }

    // Description copied from Object
    public String toString()
    {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + "[uniqueID=" + getID()
                + ", deliverySystemType=" + getDeliverySystemType() + ", handle=" + getTransportHandle()
                + ", transportID=" + Integer.toHexString(getTransportID()) + "]";
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

        /** The transport handle */
        public TransportHandle transportHandle;

        /** The delivery system type of this transport */
        public DeliverySystemType deliverySystemType;

        /** The transportID for this transport */
        public int transportID;
    }

    /** Listener list for network change listeners */
    private static ListenerList networkChangeListenerList = new ListenerList(NetworkChangeListener.class);

    /** Listener list for transport stream change listeners */
    private static ListenerList transportStreamChangeListenerList = new ListenerList(
            TransportStreamChangeListener.class);

    /** Listener list for service details change listeners */
    private static ListenerList serviceDetailsChangeListenerList = new ListenerList(ServiceDetailsChangeListener.class);

    /** Network change listener */
    static NetworkChangeListener networkChangeListener = new NetworkChangeListener()
    {
        public void notifyChange(NetworkChangeEvent event)
        {
            Transport transport = event.getTransport();
            Object id = ((TransportExt) transport).getID();
            networkChangeListenerList.postEvent(id, event);
        }
    };

    /** Transport stream change listener */
    static TransportStreamChangeListener transportStreamChangeListener = new TransportStreamChangeListener()
    {
        public void notifyChange(TransportStreamChangeEvent event)
        {
            Transport transport = event.getTransport();
            Object id = ((TransportExt) transport).getID();
            transportStreamChangeListenerList.postEvent(id, event);
        }
    };

    /** Service details change listener */
    static ServiceDetailsChangeListener serviceDetailsChangeListener = new ServiceDetailsChangeListener()
    {
        public void notifyChange(ServiceDetailsChangeEvent event)
        {
            ServiceDetailsExt sd = (ServiceDetailsExt) event.getServiceDetails();
            TransportStreamExt ts = (TransportStreamExt) sd.getTransportStream();
            Transport transport = ts.getTransport();
            Object id = ((TransportExt) transport).getID();
            serviceDetailsChangeListenerList.postEvent(id, event);
        }
    };
}
