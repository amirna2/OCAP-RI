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

import javax.tv.service.*;
import javax.tv.locator.*;

/**
 * This interface represents a collection of bouquets on a
 * <code>Transport</code>. In DVB SI, this information is contained in the BAT
 * tables. <code>BouquetCollection</code> may be optionally implemented by
 * <code>Transport</code> objects, depending on the SI data carried on that
 * transport.
 * 
 * @see <a
 *      href="../../../../overview-summary.html#guidelines-opinterfaces">Optionally
 *      implemented interfaces</a>
 */
public interface BouquetCollection extends Transport
{

    /**
     * Retrieves the specified <code>Bouquet</code> from the collection.
     * <p>
     * 
     * This method delivers its results asynchronously.
     * 
     * @param locator
     *            A <code>Locator</code> referencing the <code>Bouquet</code> of
     *            interest.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid bouquet.
     * 
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>.
     * 
     * @see Bouquet
     * @see javax.tv.service.ReadPermission
     */
    public SIRequest retrieveBouquet(Locator locator, SIRequestor requestor) throws InvalidLocatorException,
            SecurityException;

    /**
     * 
     * Retrieves an array of all the <code>Bouquet</code> objects in this
     * <code>BouquetCollection</code>. This array will only contain
     * <code>Bouquet</code> instances <code>b</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(b.getLocator())</code>. If no
     * <code>Bouquet</code> instances meet this criteria, this method will
     * result in an <code>SIRequestFailureType</code> of
     * <code>DATA_UNAVAILABLE</code>.
     * <p>
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
     * @see Bouquet
     * @see javax.tv.service.ReadPermission
     */
    public SIRequest retrieveBouquets(SIRequestor requestor);

    /**
     * Registers a <code>BouquetChangeListener</code> to be notified of changes
     * to a <code>Bouquet</code> that is part of this
     * <code>BouquetCollection</code>. Subsequent notification is made via
     * <code>BouquetChangeEvent</code> with this <code>BouquetCollection</code>
     * as the event source and an <code>SIChangeType</code> of <code>ADD</code>,
     * <code>REMOVE</code> or <code>MODIFY</code>. Only changes to
     * <code>Bouquet</code> instances <code>b</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(b.getLocator())</code> will be
     * reported.
     * <p>
     * 
     * This method is only a request for notification. No guarantee is provided
     * that the SI database will detect all, or even any, SI changes or whether
     * such changes will be detected in a timely fashion.
     * <p>
     * 
     * If the specified <code>BouquetChangeListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            A <code>BouquetChangeListener</code> to be notified about
     *            changes related to <code>Bouquet</code> carried on this
     *            <code>Transport</code>.
     * 
     * @see BouquetChangeEvent
     * @see javax.tv.service.ReadPermission
     **/
    public void addBouquetChangeListener(BouquetChangeListener listener);

    /**
     * Called to unregister an <code>BouquetChangeListener</code>. If the
     * specified <code>BouquetChangeListener</code> is not registered, no action
     * is performed.
     * 
     * @param listener
     *            A previously registered listener.
     */
    public void removeBouquetChangeListener(BouquetChangeListener listener);

}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
