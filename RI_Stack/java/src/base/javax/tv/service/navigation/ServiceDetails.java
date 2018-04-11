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

package javax.tv.service.navigation;

import javax.tv.service.*;
import javax.tv.service.guide.ProgramSchedule;

/**
 * This interface provides access to service meta-data. It provides more
 * information about a <code>Service</code> object and represents a specific
 * instance of a service bound to a transport stream.
 * <p>
 * 
 * A <code>ServiceDetails</code> object may optionally implement the
 * <code>ServiceNumber</code> interface to report service numbers as assigned by
 * the broadcaster of the service.
 * <p>
 * 
 * A <code>ServiceDetails</code> object may optionally implement the
 * <code>ServiceProviderInformation</code> interface to report information
 * concerning the service provider.
 * 
 * @see javax.tv.service.Service
 * 
 * @see javax.tv.service.ServiceNumber
 * 
 * @see ServiceProviderInformation
 * 
 * @see <a
 *      href="../../../../overview-summary.html#guidelines-opinterfaces">Optionally
 *      implemented interfaces</a>
 */
public interface ServiceDetails extends SIElement, CAIdentification
{

    /**
     * Retrieves a textual description of this service if available. This method
     * delivers its results asynchronously.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @see ServiceDescription
     */
    public SIRequest retrieveServiceDescription(SIRequestor requestor);

    /**
     * Returns the type of this service, for example, "digital
     * television", "digital radio", "NVOD", etc. These values can be mapped to
     * the ATSC service type in the VCT table and the DVB service type in the
     * Service Descriptor.
     * 
     * @return Service type of this service.
     */
    public ServiceType getServiceType();

    /**
     * Retrieves an array of elementary components which are part of this
     * service. The array will only contain <code>ServiceComponent</code>
     * instances <code>c</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(c.getLocator())</code>. If no
     * <code>ServiceComponent</code> instances meet this criteria, this method
     * will result in an <code>SIRequestFailureType</code> of
     * <code>DATA_UNAVAILABLE</code>.
     * 
     * This method delivers its results asynchronously.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @see ServiceComponent
     * @see javax.tv.service.ReadPermission
     */
    public SIRequest retrieveComponents(SIRequestor requestor);

    /**
     * Returns a schedule of program events associated with this service.
     * 
     * @return The program schedule for this service, or <code>null</code> if no
     *         schedule is available.
     */
    public ProgramSchedule getProgramSchedule();

    /**
     * Called to obtain a full service name. For example, this information may
     * be delivered in the ATSC Extended Channel Name Descriptor, the DVB
     * Service Descriptor or the DVB Multilingual Service Name Descriptor.
     * 
     * @return A string representing the full service name, or an empty string
     *         if the name is not available.
     */
    public String getLongName();

    /**
     * Returns the <code>Service</code> this <code>ServiceDetails</code> object
     * is associated with.
     * 
     * @return The <code>Service</code> to which this
     *         <code>ServiceDetails</code> belongs.
     */
    public Service getService();

    /**
     * Registers a <code>ServiceComponentChangeListener</code> to be notified of
     * changes to a <code>ServiceComponent</code> that is part of this
     * <code>ServiceDetails</code>. Subsequent notification is made via
     * <code>ServiceComponentChangeEvent</code> with this
     * <code>ServiceDetails</code> instance as the event source and an
     * <code>SIChangeType</code> of <code>ADD</code>, <code>REMOVE</code> or
     * <code>MODIFY</code>. Only changes to <code>ServiceComponent</code>
     * instances <code>c</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(c.getLocator())</code> will be
     * reported.
     * <p>
     * 
     * This method is only a request for notification. No guarantee is provided
     * that the SI database will detect all, or even any, SI changes or whether
     * such changes will be detected in a timely fashion.
     * <p>
     * 
     * If the specified <code>ServiceComponentChangeListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            A <code>ServiceComponentChangeListener</code> to be notified
     *            about changes related to a <code>ServiceComponent</code> in
     *            this <code>ServiceDetails</code>.
     * 
     * @see ServiceComponentChangeEvent
     * @see javax.tv.service.ReadPermission
     */
    public abstract void addServiceComponentChangeListener(ServiceComponentChangeListener listener);

    /**
     * Called to unregister an <code>ServiceComponentChangeListener</code>. If
     * the specified <code>ServiceComponentChangeListener</code> is not
     * registered, no action is performed.
     * 
     * @param listener
     *            A previously registered listener.
     */
    public abstract void removeServiceComponentChangeListener(ServiceComponentChangeListener listener);

    /**
     * Reports the type of mechanism by which this service was delivered.
     * 
     * @return The delivery system type of this service.
     */
    public DeliverySystemType getDeliverySystemType();
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
