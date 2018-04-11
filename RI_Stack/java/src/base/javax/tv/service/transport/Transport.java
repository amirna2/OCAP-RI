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

package javax.tv.service.transport;

import javax.tv.service.navigation.DeliverySystemType;

/**
 * This interface represents an individual content delivery mechanism. A
 * <code>Transport</code> serves as an access point for acquiring information
 * about services and their groupings.
 * <p>
 * 
 * A <code>Transport</code> may expose various types of entities (e.g. bouquets,
 * networks and/or transport streams) by optionally implementing additional
 * interfaces (i.e. <code>BouquetCollection</code>,
 * <code>NetworkCollection</code>, and/or <code>TransportStreamCollection</code>
 * ), depending on the particular SI format used and the presence of optional
 * elements and tables in the SI data being broadcast.
 * <p>
 * 
 * @see BouquetCollection
 * @see NetworkCollection
 * @see TransportStreamCollection
 * 
 * @see <a
 *      href="../../../../overview-summary.html#guidelines-opinterfaces">Optionally
 *      implemented interfaces</a>
 */
public interface Transport
{

    /**
     * Registers a <code>ServiceDetailsChangeListener</code> to be notified of
     * changes to <code>ServiceDetails</code> that are carried on this
     * <code>Transport</code>. Subsequent notification is made via
     * <code>ServiceDetailsChangeEvent</code> with this <code>Transport</code>
     * instance as the event source and an <code>SIChangeType</code> of
     * <code>ADD</code>, <code>REMOVE</code> or <code>MODIFY</code>. Only
     * changes to <code>ServiceDetails</code> <code>sd</code> for which the
     * caller has <code>javax.tv.service.ReadPermission(sd.getLocator())</code>
     * will be reported.
     * <p>
     * 
     * This method is only a request for notification. No guarantee is provided
     * that the SI database will detect all, or even any, SI changes or whether
     * such changes will be detected in a timely fashion. Applications may
     * indicate <code>ServiceDetails</code> of particular interest via the
     * method {@link javax.tv.service.SIManager#registerInterest}.
     * <p>
     * 
     * If the specified <code>ServiceDetailsChangeListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            An <code>ServiceDetailsChangeListener</code> to be notified
     *            about changes related to <code>ServiceDetails</code> carried
     *            on this <code>Transport</code>.
     * 
     * @see ServiceDetailsChangeEvent
     * @see javax.tv.service.SIManager#registerInterest
     * @see javax.tv.service.ReadPermission
     */
    public void addServiceDetailsChangeListener(ServiceDetailsChangeListener listener);

    /**
     * Called to unregister an <code>ServiceDetailsChangeListener</code>. If the
     * specified <code>ServiceDetailsChangeListener</code> is not registered, no
     * action is performed.
     * 
     * @param listener
     *            A previously registered listener.
     */
    public void removeServiceDetailsChangeListener(ServiceDetailsChangeListener listener);

    /**
     * Reports the type of mechanism by which this <code>Transport</code>
     * delivers content.
     * 
     * @return The delivery system type of this transport.
     */
    public DeliverySystemType getDeliverySystemType();

}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
