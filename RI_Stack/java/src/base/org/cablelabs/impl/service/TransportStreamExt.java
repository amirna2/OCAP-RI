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

package org.cablelabs.impl.service;

import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;

import org.apache.log4j.Logger;
import org.davic.net.Locator;
import org.davic.net.tuning.NetworkInterface;


/**
 * Implementation specific extensions to <code>TransportStream</code>
 * 
 * @author Todd Earles
 */
public abstract class TransportStreamExt implements UniqueIdentifier, TransportStream, ServiceDetailsVariant
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(TransportStreamExt.class.getName());

    /**
     * Create a snapshot of this <code>TransportStream</code> and associate it
     * with the specified SI cache.
     * 
     * @param siCache
     *            The cache this snapshot is to be associated with
     * @return A copy of this object associated with <code>siCache</code>
     * @throws UnsupportedOperationException
     *             If creation of a snapshot is not supported
     */
    public abstract TransportStream createSnapshot(SICache siCache);

    /**
     * Returns the handle that identifies this <code>TransportStream</code>
     * within the SI database.
     * 
     * @return The transport stream handle or null if not available via the
     *         SIDatabase.
     */
    public abstract TransportStreamHandle getTransportStreamHandle();

    /**
     * Returns the frequency that carries this transport stream.
     * 
     * @return The frequency that carries this transport stream.
     */
    public abstract int getFrequency();

    /**
     * Returns the modulation format for this transport stream.
     * 
     * @return The modulation format for this transport stream. The value 255
     *         indicates that this transport stream carries an analog service.
     */
    public abstract int getModulationFormat();

    /**
     * Returns the <code>Transport</code> to which this
     * <code>TransportStream</code> belongs.
     * 
     * @return The transport to which this transport stream belongs.
     */
    public abstract Transport getTransport();
    
    /**
     * Returns the <code>Network</code> to which this
     * <code>TransportStream</code> belongs.
     * 
     * @return The network to which this transport stream belongs.
     */
    public abstract Network getNetwork();

    /**
     * Returns the <code>ServiceDetails</code> to which this
     * <code>TransportStream</code> instance is restricted or null if not
     * restricted.
     * 
     * @return The service details to which this transport stream is restricted.
     */
    public abstract ServiceDetails getServiceDetails();

    /**
     * Returns the <code>ServiceDetails</code> instance for each service carried
     * within this transport stream.
     * 
     * @return An array of <code>ServiceDetails</code> for all services within
     *         this transport stream or an empty array if there are none.
     */
    public abstract ServiceDetails[] getAllServiceDetails();

    /**
     * Get the DAVIC version of this transport stream.
     * 
     * @param ni
     *            The network interface to which this transport stream belongs.
     * @return The DAVIC transport stream
     */
    public org.davic.mpeg.TransportStream getDavicTransportStream(NetworkInterface ni)
    {
        return new DavicTransportStream(ni);
    }

    public int getTransportStreamID()
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveTsID(requestor);
            requestor.waitForCompletion();
        }
        
        // Return the results
        TsIDElement[] arr = new TsIDElement[1];
        try
        {
            arr[0]  = (TsIDElement) requestor.getResults()[0];
            return arr[0].getIntValue();
        } catch (SIRequestException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("TransportStreamExt getTransportStreamID caught SIRequestException" );
            }
        }
        
        return -1;
    }
    
    public abstract SIRequest retrieveTsID(SIRequestor requestor);
    
    /**
     * The DAVIC version of this transport stream
     */
    protected class DavicTransportStream extends org.cablelabs.impl.davic.mpeg.TransportStreamExt
    {
        /**
         * Construct a DAVIC transport stream
         */
        public DavicTransportStream(NetworkInterface ni)
        {
            if (ni == null) throw new IllegalArgumentException();
            this.ni = ni;
        }

        // Description copied from org.davic.mpeg.TransportStream
        public int getTransportStreamId()
        {
            return TransportStreamExt.this.getTransportStreamID();
        }
        
        public SIRequest retrieveTsID(SIRequestor requestor)
        {
            return TransportStreamExt.this.retrieveTsID(requestor);
        }
        
        // Description copied from org.davic.mpeg.TransportStream
        public org.davic.mpeg.Service retrieveService(int serviceId)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Searching for service with service ID " + serviceId);
            }

            // Get all service details for this transport stream
            ServiceDetails[] sda = getAllServiceDetails();

            // Find the service details with the given DAVIC service ID. The
            // DAVIC service ID is
            // the MPEG program number.
            for (int i = 0; i < sda.length; i++)
            {
                ServiceDetailsExt sd = (ServiceDetailsExt) sda[i];
                if (log.isDebugEnabled())
                {
                    log.debug("Checking " + sd);
                }
                if (sd.getProgramNumber() == serviceId)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Service ID matched");
                    }
                    return sd.getDavicService(this);
                }
            }

            // Return null if not found
            return null;
        }

        // Description copied from org.davic.mpeg.TransportStream
        public org.davic.mpeg.Service[] retrieveServices()
        {
            // Get the JavaTV transport dependent services
            ServiceDetails[] details = TransportStreamExt.this.getAllServiceDetails();
            int len = details.length;
            if (len == 0) return null;

            // Create a DAVIC service for each JavaTV service
            org.davic.mpeg.Service[] services = null;
            int validServices = 0;
            for (int i = 0; i < len; i++)
            {
                // If PMT is not acquired then treat it as a program not carried
                // in this transport stream. Check for pcrPID (for digital
                // services)
                if (!((ServiceDetailsExt) details[i]).isAnalog()
                        && (((ServiceDetailsExt) details[i]).getPcrPID() != -1))
                {
                    validServices++;
                    // if (Logging.LOGGING)
                    // log.debug("PcrPID: " +
                    // ((ServiceDetailsExt)details[i]).getPcrPID());
                }
            }

            // populate services carried in the transport stream
            services = new org.davic.mpeg.Service[validServices];
            for (int i = 0, j = 0; i < len; i++)
            {
                if (!((ServiceDetailsExt) details[i]).isAnalog()
                        && (((ServiceDetailsExt) details[i]).getPcrPID() != -1))
                {
                    services[j++] = ((ServiceDetailsExt) details[i]).getDavicService(this);
                }
            }

            // Return the array of DAVIC services
            return services;
        }

        // Description copied from
        // org.cablelabs.impl.davic.mpeg.TransportStreamExt
        public NetworkInterface getNetworkInterface()
        {
            return ni;
        }

        // Description copied from
        // org.cablelabs.impl.davic.mpeg.TransportStreamExt
        public int getFrequency()
        {
            return TransportStreamExt.this.getFrequency();
        }

        // Description copied from
        // org.cablelabs.impl.davic.mpeg.TransportStreamExt
        public int getModulationFormat()
        {
            return TransportStreamExt.this.getModulationFormat();
        }

        // Description copied from
        // org.cablelabs.impl.davic.mpeg.TransportStreamExt
        public Transport getTransport()
        {
            return TransportStreamExt.this.getTransport();
        }

        // Description copied from
        // org.cablelabs.impl.davic.mpeg.TransportStreamExt
        public TransportStreamHandle getTransportStreamHandle()
        {
            return TransportStreamExt.this.getTransportStreamHandle();
        }
        
        // Description copied from
        // org.cablelabs.impl.davic.mpeg.TransportStreamExt
        public ServiceDetails getServiceDetails()
        {
            return TransportStreamExt.this.getServiceDetails();
        }

        // Description copied from
        // org.cablelabs.impl.davic.mpeg.TransportStreamExt
        public Locator getLocator()
        {
            return (org.davic.net.Locator) TransportStreamExt.this.getLocator();
        }

        // Description copied from Object
        public boolean equals(Object obj)
        {
            // Make sure we have a good object
            if (this == obj) return true;
            if (obj == null || obj.getClass() != getClass()) return false;

            // Compare all other fields
            DavicTransportStream o = (DavicTransportStream) obj;
            int tsid1 = getTransportStreamId();
            int tsid2 = o.getTransportStreamId();
            return getNetworkInterface().equals(o.getNetworkInterface()) && getFrequency() == o.getFrequency()
                    && getModulationFormat() == o.getModulationFormat()
                    && (tsid1 == tsid2 || tsid1 == -1 || tsid2 == -1);
        }

        // Description copied from Object
        public int hashCode()
        {
            return TransportStreamExt.this.getFrequency() ^ ni.hashCode();
        }

        // Description copied from Object
        public String toString()
        {
            return super.toString() + "[ni=" + ni + ", frequency=" + TransportStreamExt.this.getFrequency()
                    + ", modulationFormat=" + TransportStreamExt.this.getModulationFormat() + "]";
        }

        /** The network interface which carries this transport stream */
        private final NetworkInterface ni;

    }
}
