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

package org.cablelabs.impl.manager.service;

import javax.tv.service.ReadPermission;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;

import org.apache.log4j.Logger;

import org.cablelabs.impl.service.NetworkExt;
import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SINotAvailableYetException;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportStreamHandle;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * An instance of <code>SIRequestTransportStreams</code> represents an
 * outstanding asynchronous request for a set of <code>TransportStream</code>
 * objects.
 * 
 * @author Todd Earles
 */
public class SIRequestTransportStreams extends SIRequestImpl
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIRequestTransportStreams.class);

    /** Identity of this object */
    protected String id = "@" + Integer.toHexString(System.identityHashCode(this));

    /**
     * Construct an <code>SIRequest</code> for all <code>TransportStream</code>
     * objects carried on the specified <code>Transport</code>.
     * 
     * @param siCache
     *            The <code>SICache</code> to which this request belongs.
     * @param transport
     *            <code>Transport</code> referencing the
     *            <code>TransportStream</code>s of interest.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     */
    public SIRequestTransportStreams(SICacheImpl siCache, Transport transport, SIRequestor requestor)
    {
        super(siCache, null, requestor);
        this.transport = transport;
        this.network = null;
        securityContext = SecurityUtil.getSecurityContext();
        // Expiration time is set temporarily to a large value
        // It will be reset in attemptDelivery() if the OOB SI
        // is found to be not fully acquired
        this.setExpirationTime(Long.MAX_VALUE);        
    }

    /**
     * Construct an <code>SIRequest</code> for all <code>TransportStream</code>
     * objects carried on the specified <code>Network</code>.
     * 
     * @param siCache
     *            The <code>SICache</code> to which this request belongs.
     * @param network
     *            <code>Network</code> referencing the
     *            <code>TransportStream</code>s of interest.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     */
    public SIRequestTransportStreams(SICacheImpl siCache, Network network, SIRequestor requestor)
    {
        super(siCache, null, requestor);
        this.transport = null;
        this.network = network;
        securityContext = SecurityUtil.getSecurityContext();
        // Expiration time is set temporarily to a large value
        // It will be reset in attemptDelivery() if the OOB SI
        // is found to be not fully acquired
        this.setExpirationTime(Long.MAX_VALUE);
    }

    // Description copied from SIRequest
    public synchronized boolean cancel()
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " Request canceled");
        }

        // Return if already canceled
        if (canceled)
            return false;

        // Cancel the request
        boolean result = siCache.cancelTransportStreamRequest(this);
        if (result == true)
            notifyFailure(SIRequestFailureType.CANCELED);
        canceled = true;
        return result;
    }

    // Description copied from SIRequestImpl
    public synchronized boolean attemptDelivery()
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " Attempting delivery");
        }

        // The request has already been canceled so consider the request
        // to be finished.
        if (canceled)
            return true;

        // Attempt delivery
        try
        {
            if(!siDatabase.isNITSVCTAcquired())
            {      
                siDatabase.waitForNITSVCT();

                long currentTime = System.currentTimeMillis();
                
                this.setExpirationTime(currentTime+sMgr.getRequestAsyncTimeout());
            }
            
            // Get the handles to all available transport streams carried by
            // the specified transport or network.
            TransportStreamHandle[] handles;
            if (transport != null)
            {
                // Based on transport
                handles = siDatabase.getTransportStreamsByTransport(((TransportExt) transport).getTransportHandle());
            }
            else
            {
                // Based on network
                handles = siDatabase.getTransportStreamsByNetwork(((NetworkExt) network).getNetworkHandle());
            }

            // Allocate an array large enough to hold all transport stream
            // objects to be returned.
            int returnCount = handles.length;
            TransportStream[] objects = new TransportStream[returnCount];

            // Process each transport stream handle
            for (int i = 0; i < returnCount; i++)
            {
                // Get the transport stream object from the cache if it is
                // present. Otherwise, create it from the database and add
                // it to the cache. Even if the transport stream object has
                // already
                // been retrieved and cached, the TSID may still be -1. This
                // means that
                // we have not yet tuned to this frequency and retrieved the
                // TSID from
                // the IB PAT. In that case, we will re-request the transport
                // stream
                // from native.
                objects[i] = siCache.getCachedTransportStream(handles[i]);
                if (objects[i] == null)
                {
                    objects[i] = siDatabase.createTransportStream(handles[i]);
                    siCache.putCachedTransportStream(handles[i], objects[i]);
                }
            }

            // Remove any transport streams the caller does not have
            // ReadPermission for.
            // If the pre-processed array has any elements but none of them pass
            // the permission check then fail with DATA_UNAVAILABLE.
            if (objects.length != 0)
            {
                objects = filterTransportStreamsByPermission(objects);

                if (objects.length == 0)
                {
                    notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
                    return true;
                }
            }

            // Notify the requestor
            notifySuccess(objects);
            return true;
        }
        catch (SINotAvailableYetException e)
        {
            // Try again later
            return false;
        }
        catch (SIDatabaseException e)
        {
            notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
            return true;
        }
        catch (Exception e)
        {
            notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
            return true;
        }
    }

    /**
     * Filter the list of transport streams based on permissions of the original
     * requestor.
     * 
     * @param transportStreams
     *            The array of transportStreams to filter
     * @return A new array of transport streams that pass the filter. If no
     *         transport streams pass the filter an empty array is returned.
     */
    private TransportStream[] filterTransportStreamsByPermission(TransportStream[] transportStreams)
    {
        // Iterate over all transport streams and mark whether the original
        // requestor has ReadPermission.
        int returnCount = 0;
        boolean[] passes = new boolean[transportStreams.length];
        for (int i = 0; i < transportStreams.length; i++)
        {
            passes[i] = SecurityUtil.hasPermission(new ReadPermission(transportStreams[i].getLocator()),
                    securityContext);
            if (passes[i]) returnCount++;
        }

        // Copy all transport streams that pass the filter to the return array
        TransportStream[] returnTransportStreams = new TransportStream[returnCount];
        for (int i = 0, j = 0; i < transportStreams.length; i++)
            if (passes[i]) returnTransportStreams[j++] = transportStreams[i];

        return returnTransportStreams;
    }

    // Description copied from Object
    public String toString()
    {
        // TODO(Todd): Include IdentityHashcode for Transport and Network
        return super.toString();
    }

    /** The transport whose transport streams are to be retrieved */
    private final Transport transport;

    /** The network whose transport streams are to be retrieved */
    private final Network network;

    /** The original requestors security context */
    private final Object securityContext;
}
