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
import javax.tv.service.navigation.ServiceDetails;

import org.apache.log4j.Logger;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SINotAvailableYetException;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.util.LocatorUtil;

/**
 * An instance of <code>SIRequestServices</code> represents an
 * outstanding asynchronous request for a set of <code>service</code>
 * objects.
 */
public class SIRequestServices extends SIRequestImpl
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIRequestServices.class);

    /**
     * Construct an <code>SIRequest</code> for all <code>Service</code>
     * s carried by the specified <code>locator</code> sourceID.
     * 
     * @param siCache
     *            The <code>SICache</code> to which this request belongs.
     * @param locator
     *            <code>Locator</code> referencing the service of
     *            interest.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     */
    public SIRequestServices(SICacheImpl siCache, Locator locator, String language,
            SIRequestor requestor) throws InvalidLocatorException
    {
        super(siCache, language, requestor);
        // Get the source_id of the service
        OcapLocator ocapLocator = LocatorUtil.convertJavaTVLocatorToOcapLocator(locator);
        sourceID = ocapLocator.getSourceID();     
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
        if (canceled) return false;

        // Cancel the request
        boolean result = siCache.cancelServicesRequest(this);
        if (result == true) notifyFailure(SIRequestFailureType.CANCELED);
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
        if (canceled) return true;

        // Attempt delivery
        try
        {
            if(!siDatabase.isOOBAcquired())
            {
                siDatabase.waitForOOB();

                long currentTime = System.currentTimeMillis();
                this.setExpirationTime(currentTime+sMgr.getRequestAsyncTimeout());
            }
            
            // Get the handles to all available service details for this sourceID
            ServiceDetailsHandle[] detailsHandles = siDatabase.getServiceDetailsBySourceID(sourceID);       

            // Allocate an array large enough to hold all service details
            int returnCount = detailsHandles.length;
            ServiceDetails[] objects = new ServiceDetails[returnCount];

            if (log.isInfoEnabled())
            {
                log.info("getServiceDetailsBySourceID returned " + returnCount);
            }

            // Process each service handle
            for (int i = 0; i < returnCount; i++)
            {
                // Get the service details object from the cache if it is
                // present. Otherwise, create it from the database and add
                // it to the cache.
                objects[i] = siCache.getCachedServiceDetails(detailsHandles[i]);
                if (objects[i] == null)
                {
                    objects[i] = siDatabase.createServiceDetails(detailsHandles[i]);
                    siCache.putCachedServiceDetails(detailsHandles[i], objects[i]);
                }

                // Create language specific instance if required
                if (language != null)
                {
                    ServiceDetailsExt sd = (ServiceDetailsExt) objects[i];
                    objects[i] = (ServiceDetails) sd.createLanguageSpecificVariant(language);
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

    // Description copied from Object
    public String toString()
    {
        // TODO: Implement...
        return super.toString();
    }

    /**
     * The sourceID of a service. 
     */
    private int sourceID = -1;
}


