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

import javax.tv.service.SIException;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.StreamType;

import org.davic.mpeg.ElementaryStream;
import org.davic.net.tuning.NetworkInterface;

import org.cablelabs.impl.util.string.MultiString;

/**
 * Implementation specific extensions to <code>ServiceComponent</code>
 * 
 * @author Todd Earles
 */
public abstract class ServiceComponentExt implements UniqueIdentifier, ServiceComponent, LanguageVariant,
        ServiceDetailsVariant
{
    /**
     * Constant to use for a component tag when one is not defined.
     */
    public final static long COMPONENT_TAG_UNDEFINED = 100000000L;

    /**
     * Constant to use for a association tag when one is not defined.
     */
    public final static long ASSOCIATION_TAG_UNDEFINED = 100000000L;

    /**
     * Constant to use for a carousel ID when one is not defined.
     */
    public final static long CAROUSEL_ID_UNDEFINED = 100000000L;

    /**
     * Create a snapshot of this <code>ServiceComponent</code> and associate it
     * with the specified SI cache.
     * 
     * @param siCache
     *            The cache this snapshot is to be associated with
     * @return A copy of this object associated with <code>siCache</code>
     * @throws UnsupportedOperationException
     *             If creation of a snapshot is not supported
     */
    public abstract ServiceComponent createSnapshot(SICache siCache);

    /**
     * Returns the handle that identifies this <code>ServiceComponent</code>
     * within the SI database.
     * 
     * @return The service component handle or null if not available via the
     *         SIDatabase.
     */
    public abstract ServiceComponentHandle getServiceComponentHandle();

    /**
     * Same as {@link ServiceComponent#getName()} except this method returns all
     * language variants as a {@link MultiString}.
     * 
     * @return The multi-string or null if not available.
     */
    public abstract MultiString getNameAsMultiString();

    /**
     * Returns the PID of the elementary stream that carries this service
     * component.
     * 
     * @return The PID for this component
     */
    public abstract int getPID();

    /**
     * Returns the component tag for this service component.
     * 
     * @return The component tag
     * @throws SIException
     *             If no component tag is found for this component
     */
    public abstract int getComponentTag() throws SIException;

    /**
     * Returns the association tag for this service component.
     * 
     * @return The association tag
     * @throws SIException
     *             If no association tag is found for this component
     */
    public abstract int getAssociationTag() throws SIException;

    /**
     * Returns the carousel ID for the carousel associated with this service
     * component.
     * 
     * @return The carousel ID
     * @throws SIException
     *             If no carousel is associated with this component
     */
    public abstract int getCarouselID() throws SIException;

    /**
     * Get the elementary stream type as defined in the PMT.
     * 
     * @return The stream type
     */
    public abstract short getElementaryStreamType();

    /**
     * Returns whether this component represents a media stream.
     * 
     * @return True if this component is a media stream; otherwise, false.
     */
    public boolean isMediaStream()
    {
        StreamType streamType = getStreamType();
        return isMediaStream(streamType);
    }

    /**
     * Tests the stream type to see if it is a media stream type.
     * 
     * @param streamType
     * @return
     */
    public boolean isMediaStream(StreamType streamType)
    {
        return streamType == StreamType.AUDIO || streamType == StreamType.VIDEO;
    }

    /**
     * Returns the {@link ServiceDetails} this object was constructed with or
     * null if no {@link ServiceDetails} was specified or this is the invariant
     * version of the object.
     * 
     * @return The {@link ServiceDetails}.
     */
    public abstract ServiceDetails getServiceDetails();

    /**
     * Get the DAVIC version of the service which carries this component.
     * 
     * @param ni
     *            The <code>NetworkInterface</code> the returned DAVIC service
     *            should be associated with or null if not associated with a
     *            network interface.
     * @return The DAVIC service
     */
    public org.davic.mpeg.Service getDavicService(NetworkInterface ni)
    {
        ServiceDetailsExt details = (ServiceDetailsExt) getServiceDetails();
        if (ni == null)
        {
            // TODO(Todd): This is required because the MediaAccessHandler takes
            // DAVIC elementary streams and must be called for services which
            // are
            // not associated with a network interface (e.g. a RecordedService
            // for a completed recording). If the MAH is changed to take JavaTV
            // components we can remove this special-case code.

            // This DAVIC service is not associated with a network interface
            return details.getDavicService(null);
        }
        else
        {
            // This DAVIC service is associated with a network interface
            TransportStreamExt ts = (TransportStreamExt) details.getTransportStream();
            return details.getDavicService(ts.getDavicTransportStream(ni));
        }
    }

    /**
     * Get the DAVIC version of this service component.
     * 
     * @param service
     *            The DAVIC service containing the service component
     * @return The DAVIC service component
     */
    public ElementaryStream getDavicElementaryStream(org.davic.mpeg.Service service)
    {
        return new DavicElementaryStream(service);
    }

    /**
     * The DAVIC version of this service component
     */
    protected class DavicElementaryStream extends org.cablelabs.impl.davic.mpeg.ElementaryStreamExt
    {
        /**
         * Construct a DAVIC elementary stream
         */
        public DavicElementaryStream(org.davic.mpeg.Service service)
        {
            if (service == null) throw new IllegalArgumentException();
            this.service = service;
        }

        // Description copied from ElementaryStream
        public org.davic.mpeg.Service getService()
        {
            return service;
        }

        // Description copied from ElementaryStream
        public int getPID()
        {
            return ServiceComponentExt.this.getPID();
        }

        // Description copied from ElementaryStream
        public Integer getAssociationTag()
        {
            try
            {
                int associationTag = ServiceComponentExt.this.getAssociationTag();
                return new Integer(associationTag);
            }
            catch (SIException e)
            {
                return null;
            }
        }

        public short getElementaryStreamType()
        {
            return ServiceComponentExt.this.getElementaryStreamType();
        }

        // Description copied from Object
        public boolean equals(Object obj)
        {
            // Make sure we have a good object
            if (this == obj) return true;
            if (obj == null || obj.getClass() != getClass()) return false;

            // Compare all other fields
            DavicElementaryStream o = (DavicElementaryStream) obj;
            return getService().equals(o.getService()) && getPID() == o.getPID()
                    && getAssociationTag().equals(o.getAssociationTag());
        }

        // Description copied from Object
        public int hashCode()
        {
            return getPID() ^ service.hashCode();
        }

        // Description copied from Object
        public String toString()
        {
            return super.toString() + "[service=" + service + ", pid=" + getPID() + ", associationTag="
                    + getAssociationTag() + ", streamType: " + getElementaryStreamType() + "]";
        }

        /** The DAVIC service which carries this elementary stream */
        private final org.davic.mpeg.Service service;
    }
}
