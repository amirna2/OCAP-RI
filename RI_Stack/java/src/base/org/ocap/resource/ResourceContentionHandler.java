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

package org.ocap.resource;

/**
 * <P>
 * A class implementing this interface decides which application shall be
 * allowed to reserve a resource.
 * </P>
 * <P>
 * An application which has a MonitorAppPermission("handler.resource") may have
 * a class implementing this interface, and may set an instance of it in the
 * ResourceContentionManager. The
 * {@link ResourceContentionHandler#resolveResourceContention} method decides
 * the how to resolve resource conflicts between the new request and existing
 * resource allocations. See the {@link ResourceContentionManager} for the details.
 * </P>
 */
public interface ResourceContentionHandler
{

    /**
     * This method notifies the ResourceContentionHandler that one to many
     * resource contentions have occurred between one or more applications and
     * system modules, except the Emergency Alert System (EAS) module. EAS
     * system module resource requests SHALL be given the highest priority by
     * the implementation and resource requests by this module SHALL not be
     * reported to the ResourceContentionHandler. In the case of one
     * application, the same application is conflicting with itself and a
     * registered ResourceContentionHandler SHALL be notified in this case.
     * <p>
     * This method notifies the ResourceContentionHandler that one to many
     * resource contentions have occurred between two or more applications. Each
     * entry in the currentReservations indicates a set of resources reserved by
     * an application for a single activity such as a resource usage by a single
     * service context. There may be multiple entries in this list from a single
     * application. An entry may correspond to a current resource usage or
     * resource reservations for a future activity. A prioritized array of
     * {@link ResourceUsage} instances is returned. The array is in priority
     * order from highest to lowest indicating the priority order to be followed
     * by the implementation while resolving the conflicts. When this method
     * returns the implementation will iterate through each entry in the array
     * in the order of priority, awarding resources as required by the activity
     * represented by the resourceUsage. The ResourceContentionHandler may use
     * information such as Application Priority to prioritize the array of
     * ResourceUsages returned. When the value returned is not null the
     * ResourceContentionHandler MAY return an array containing all of the
     * <code>ResourceUsage</code> objects passed to it, or it MAY return a
     * subset of those objects.
     * </p>
     *
     * @param newRequest
     *            The resource usage object containing the attributes of the
     *            resource[s] request.
     *
     * @param currentReservations
     *            The resource usage objects currently owned by applications
     *            which are in conflict with the newRequest. A ResourceUsage
     *            associated with a current reseravation may belong to an
     *            application that has been destroyed. Use of the AppID
     *            contained within such a ResourceUsage with any of the methods
     *            in <code>org.dvb.application.AppsDatabase</code> MAY cause a
     *            failure status to be returned.
     *
     * @return A prioritized array of resource usage objects.The first entry has
     *         the highest priority. This function returns null if the
     *         contention handler wants the implementation to resolve the
     *         conflict.
     */
    public ResourceUsage[] resolveResourceContention(ResourceUsage newRequest, ResourceUsage[] currentReservations);

    /**
     * Warns the resource contention handler of an impending contention with a
     * presenting ServiceContext (e.g., scheduled recording as defined by the
     * OCAP DVR specification). If a ResourceContentionHandler is registered the
     * implementation SHALL call this method as defined by the
     * {@link ResourceContentionManager#setWarningPeriod} method.
     *
     * @param newRequest
     *            The resource usage object containing the attributes of the
     *            resource[s] request.
     *
     * @param currentReservations
     *            The resource usage objects currently owned by applications
     *            which are in conflict with the newRequest. A ResourceUsage
     *            associated with a current reseravation may belong to an
     *            application that has been destroyed. Use of the AppID
     *            contained within such a ResourceUsage with any of the methods
     *            in <code>org.dvb.application.AppsDatabase</code> MAY cause a
     *            failure status to be returned.
     */
    public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations);
}
