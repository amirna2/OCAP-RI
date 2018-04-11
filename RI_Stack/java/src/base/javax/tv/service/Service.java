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

package javax.tv.service;

import javax.tv.locator.Locator;

/**
 * The <code>Service</code> interface represents an abstract view on what is
 * generally referred to as a television "service" or "channel". It may
 * represent an MPEG-2 program, DVB service, an ATSC virtual channel, SCTE
 * virtual channel, etc. It represents the basic information associated with a
 * service, such as its name or number, which is guaranteed to be available on
 * the receiver.
 * <P>
 * 
 * Internal to the receiver, each service is uniquely identified by information
 * that may include system type, network ID, transport stream ID, service
 * number, service number, or other information. This identification is
 * encapsulated by the Locator object.
 * <P>
 * 
 * Note that a <code>Service</code> object may represent multiple instances of
 * the same content delivered over different media (e.g., the same service may
 * be delivered over a terrestrial and cable network). A
 * <code>ServiceDetails</code> object represents a specific instance of such
 * content which is bound to a specific delivery mechanism.
 * <P>
 * 
 * The information available through this object, i.e., the service name,
 * service number, etc., represents information that is stored in the receiver
 * and is not necessarily the same as what is broadcast in any broadcast service
 * information protocol. For example, a receiver implementation may let the end
 * user edit this information according to the user's preferences.
 * <P>
 * 
 * A <code>Service</code> object may optionally implement an interface that
 * supports service numbers. Each <code>Service</code> object must provide
 * either a service name (via the <code>getName</code> method) or a service
 * number (via the <code>ServiceNumber</code> interface).
 * 
 * @see #getName
 * @see ServiceNumber
 * @see javax.tv.service.navigation.ServiceDetails
 * @see <a
 *      href="../../../overview-summary.html#guidelines-opinterfaces">Optionally
 *      implemented interfaces</a>
 */
public interface Service
{

    /**
     * This method retrieves additional information about the
     * <code>Service</code>. This information is retrieved from the broadcast
     * service information.
     * <P>
     * 
     * Note that if the content represented by this <code>Service</code> is
     * delivered on multiple transport-dependent streams there may be multiple
     * <code>ServiceDetails</code> for it. This method retrieves one of them
     * based on availability or user preferences. If access to all possible
     * <code>ServiceDetails</code> is required, the service <code>Locator</code>
     * can be transformed to transport-dependent <code>Locator</code> instances
     * and <code>ServiceDetails</code> can be retrieved for each.
     * <P>
     * 
     * This method returns data asynchronously.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @see javax.tv.locator.Locator
     * 
     * @see javax.tv.service.navigation.ServiceDetails
     */
    public abstract SIRequest retrieveDetails(SIRequestor requestor);

    /**
     * Returns a short service name or acronym. For example, in ATSC systems the
     * service name is provided by the the PSIP VCT; in DVB systems, this
     * information is provided by the DVB Service Descriptor or the Multilingual
     * Service Name Descriptor. The service name may also be user-defined.
     * 
     * @return A string representing this service's short name. If the short
     *         name is unavailable, the string representation of the service
     *         number is returned.
     */
    public abstract String getName();

    /**
     * This method indicates whether the service represented by this
     * <code>Service</code> object is available on multiple transports, (e.g.,
     * the same content delivered over terrestrial and cable network).
     * 
     * @return <code>true</code> if multiple transports carry the same content
     *         identified by this <code>Service</code> object;
     *         <code>false</code> if there is only one instance of this service.
     */
    public boolean hasMultipleInstances();

    /**
     * Returns the type of this service, (for example, "digital
     * television", "digital radio", "NVOD", etc.) These values can be mapped to
     * the ATSC service type in the VCT table and the DVB service type in the
     * service descriptor.
     * 
     * @return Service type of this <code>Service</code>.
     */
    public ServiceType getServiceType();

    /**
     * Reports the <code>Locator</code> of this <code>Service</code>. Note that
     * if the resulting locator is transport-dependent, it will also correspond
     * to a <code>ServiceDetails</code> object.
     * 
     * @return A locator referencing this <code>Service</code>.
     * 
     * @see javax.tv.service.navigation.ServiceDetails
     */
    public Locator getLocator();

    /**
     * Tests two <code>Service</code> objects for equality. Returns
     * <code>true</code> if and only if:
     * <ul>
     * <li><code>obj</code>'s class is the same as the class of this
     * <code>Service</code>, and
     * <p>
     * <li><code>obj</code>'s <code>Locator</code> is equal to the
     * <code>Locator</code> of this <code>Service</code> (as reported by
     * <code>Service.getLocator()</code>, and
     * <p>
     * <li><code>obj</code> and this object encapsulate identical data.
     * </ul>
     * 
     * @param obj
     *            The object against which to test for equality.
     * 
     * @return <code>true</code> if the two <code>Service</code> objects are
     *         equal; <code>false</code> otherwise.
     */
    public boolean equals(Object obj);

    /**
     * Reports the hash code value of this <code>Service</code>. Two
     * <code>Service</code> objects that are equal will have identical hash
     * codes.
     * 
     * @return The hash code value of this <code>Service</code>.
     */
    public int hashCode();
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
