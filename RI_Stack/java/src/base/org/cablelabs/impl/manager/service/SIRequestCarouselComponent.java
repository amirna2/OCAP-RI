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

import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDetails;

import org.apache.log4j.Logger;

import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SINotAvailableYetException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceComponentHandle;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;

/**
 * An instance of <code>SIRequestCarouselComponent</code> represents an
 * outstanding asynchronous request for a <code>ServiceComponent</code> object
 * which represents an object carousel.
 * 
 * @author Todd Earles
 */
public class SIRequestCarouselComponent extends SIRequestImpl
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIRequestCarouselComponent.class);

    /**
     * Construct an <code>SIRequest</code> for the <code>ServiceComponent</code>
     * which represents the default object carousel for the specified
     * <code>ServiceDetails</code>.
     * 
     * @param siCache
     *            The <code>SICache</code> to which this request belongs.
     * @param serviceDetails
     *            <code>ServiceDetails</code> referencing the service details of
     *            interest.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     */
    public SIRequestCarouselComponent(SICacheImpl siCache, ServiceDetails serviceDetails, String language,
            SIRequestor requestor)
    {
        super(siCache, language, requestor);
        sdHandle = ((ServiceDetailsExt) serviceDetails).getServiceDetailsHandle();
        details = serviceDetails;
        haveCarouselID = false;
        carouselID = 0;
    }

    /**
     * Construct an <code>SIRequest</code> for the <code>ServiceComponent</code>
     * which represents the object carousel with the specified carousel ID for
     * the specified <code>ServiceDetails</code>.
     * 
     * @param siCache
     *            The <code>SICache</code> to which this request belongs.
     * @param serviceDetails
     *            <code>ServiceDetails</code> referencing the service details of
     *            interest.
     * @param carouselID
     *            The carousel ID of the object carousel to retrieve
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     */
    public SIRequestCarouselComponent(SICacheImpl siCache, ServiceDetails serviceDetails, int carouselID,
            String language, SIRequestor requestor)
    {
        super(siCache, language, requestor);
        sdHandle = ((ServiceDetailsExt) serviceDetails).getServiceDetailsHandle();
        details = serviceDetails;
        haveCarouselID = true;
        this.carouselID = carouselID;
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
        boolean result = siCache.cancelServiceDetailsRequest(this);
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
            // Get the handle to the service component which carries the
            // carousel
            ServiceComponentHandle handle;
            if (haveCarouselID)
                handle = siDatabase.getCarouselComponentByServiceDetails(sdHandle, carouselID);
            else
                handle = siDatabase.getCarouselComponentByServiceDetails(sdHandle);

            // Get the service component object from the cache if it is
            // present. Otherwise, create it from the database and add
            // it to the cache.
            ServiceComponentExt[] objects = new ServiceComponentExt[1];
            objects[0] = (ServiceComponentExt) siCache.getCachedServiceComponent(handle);
            if (objects[0] == null)
            {
                objects[0] = siDatabase.createServiceComponent(handle);
                siCache.putCachedServiceComponent(handle, objects[0]);
            }

            // Create language specific instance if required
            if (language != null)
            {
                ServiceComponentExt sd = (ServiceComponentExt) objects[0];
                objects[0] = (ServiceComponentExt) sd.createLanguageSpecificVariant(language);
            }

            // Return ServiceDetailsSpecific ServiceComponent 
            {
                ServiceComponentExt scExt = (ServiceComponentExt) objects[0];
                objects[0] = (ServiceComponentExt) scExt.createServiceDetailsSpecificVariant(details);
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
        // TODO(Todd): Include IdentityHashcode for ServiceDetails
        return super.toString() + "[haveCarouselID=" + haveCarouselID + ", carouselID="
                + Integer.toHexString(carouselID) + ", language=" + language + "]";
    }

    /** The service details whose service components are to be retrieved */
    private final ServiceDetailsHandle sdHandle;
    private final ServiceDetails details;

    /** True if a carousel ID has been specified in the request */
    private final boolean haveCarouselID;

    /** The carousel ID */
    private final int carouselID;
}
