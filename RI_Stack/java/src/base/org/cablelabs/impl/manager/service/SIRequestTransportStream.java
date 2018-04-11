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

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;

import org.apache.log4j.Logger;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SINotAvailableYetException;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportHandle;
import org.cablelabs.impl.service.TransportStreamHandle;
import org.cablelabs.impl.util.LocatorUtil;

/**
 * An instance of <code>SIRequestTransportStream</code> represents an
 * outstanding asynchronous request for a <code>TransportStream</code> object.
 * 
 * @author Todd Earles
 */
public class SIRequestTransportStream extends SIRequestImpl
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIRequestTransportStream.class);

    /** Identity of this object */
    protected String id = "@" + Integer.toHexString(System.identityHashCode(this));

    /**
     * Construct an <code>SIRequest</code> for the <code>TransportStream</code>
     * that carries the specified service.
     * 
     * @param siCache
     *            The <code>SICache</code> to which this request belongs.
     * @param locator
     *            Transport stream locator referencing the
     *            <code>TransportStream</code> of interest.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @throws InvalidLocatorException
     *             If the CABLE transport cannot be found or the specified
     *             <code>locator</code> does not reference a transport stream.
     */
    public SIRequestTransportStream(SICacheImpl siCache, Locator locator, SIRequestor requestor)
            throws InvalidLocatorException
    {
        super(siCache, null, requestor);

        // Ocap locators always refer to the CABLE transport. Find the transport
        // ID for the CABLE transport.
        Transport transports[] = siCache.getTransports();
        for (int i = 0; i < transports.length; i++)
            if (transports[i].getDeliverySystemType() == DeliverySystemType.CABLE)
                transportID = ((TransportExt) transports[i]).getTransportID();
        if (transportID == 0) throw new InvalidLocatorException(locator, "Cable transport not found");

        // Get the frequency or source_id of the transport stream
        OcapLocator ocapLocator = LocatorUtil.convertJavaTVLocatorToOcapLocator(locator);
        sourceID = ocapLocator.getSourceID();
        frequency = ocapLocator.getFrequency();
        mode = ocapLocator.getModulationFormat();
        if (mode == -1)
        {
            // An unspecified/unknown modulation mode of -1
            // implies a QAM 256 modulation per OCAP spec
            // See Javadoc for org.ocap.net.OcapLocator.getModulationFormat()
            mode = 0x10;
        }

        serviceName = ocapLocator.getServiceName();
        if (serviceName != null)
        {
            Service s = siCache.getService(locator, null);
            ocapLocator = (OcapLocator) s.getLocator();
            sourceID = ocapLocator.getSourceID();
            frequency = ocapLocator.getFrequency();
        }
        if (frequency == -1 && sourceID == -1)
            throw new InvalidLocatorException(locator, "Locator does not specify a transport stream");
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
            // Get the handle to the specified transport
            TransportHandle transportHandle = siDatabase.getTransportByID(transportID);

            if(!siDatabase.isNITSVCTAcquired())
            {
                siDatabase.waitForNITSVCT();

                long currentTime = System.currentTimeMillis();
                // SI request time is 15 sec
                this.setExpirationTime(currentTime+sMgr.getRequestAsyncTimeout());
            }
            
            // Get the handle to the specified transport stream
            TransportStreamHandle tsHandle = null;
            if (frequency != -1)
            {
                tsHandle = siDatabase.getTransportStreamByID(transportHandle, frequency, mode, -1);
            }
            else if (sourceID != -1) 
            {
                tsHandle = siDatabase.getTransportStreamBySourceID(sourceID);
            }

            // Get the transport stream object from the cache if it is
            // present. Otherwise, create it from the database and add
            // it to the cache. Even if the transport stream object has already
            // been retrieved and cached, the TSID may still be -1. This means
            // that
            // we have not yet tuned to this frequency and retrieved the TSID
            // from
            // the IB PAT. In that case, we will re-request the transport stream
            // from native.
            TransportStream[] objects = new TransportStream[1];
            objects[0] = siCache.getCachedTransportStream(tsHandle);
            if (objects[0] == null)
            {
                objects[0] = siDatabase.createTransportStream(tsHandle);
                siCache.putCachedTransportStream(tsHandle, objects[0]);
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

    // Description copied from Object
    public String toString()
    {
        // TODO(Todd): Include IdentityHashcode for Transport
        return super.toString() + "[transportID=" + Integer.toHexString(transportID) + ", frequency="
                + Integer.toHexString(frequency) + ", sourceID=" + Integer.toHexString(sourceID) + ", serviceName="
                + serviceName + "]";
    }

    /** The CABLE transport ID */
    private int transportID;

    /** The frequency of the transport stream */
    private int frequency = -1;

    /**
     * The sourceID of a service. We are looking for the transport stream
     * associated with this service
     */
    private int sourceID = -1;

    private int mode = -1;

    /**
     * The service name of a service. We are looking for the transport stream
     * associated with this service
     */
    private String serviceName = null;
}
