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
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;

import org.apache.log4j.Logger;

import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SINotAvailableYetException;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServiceHandle;

/**
 * An instance of <code>SIRequestServiceDetails</code> represents an outstanding
 * asynchronous request for a <code>ServiceDetails</code> object.
 * 
 * @author Todd Earles
 */
public class SIRequestServiceDetails extends SIRequestImpl
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIRequestServiceDetails.class);

    /** Identity of this object */
    protected String id = "@" + Integer.toHexString(System.identityHashCode(this));

    /**
     * Construct an <code>SIRequest</code> for one or more
     * <code>ServiceDetails</code> objects specified by the given
     * <code>Service</code>. If the content represented by this
     * <code>Service</code> is delivered on multiple transports there may be
     * multiple <code>ServiceDetails</code> for it.
     * 
     * @param siCache
     *            The <code>SICache</code> to which this request belongs.
     * @param service
     *            <code>Service</code> referencing the service details of
     *            interest.
     * @param retrieveAll
     *            If true retrieve all service details. If false retrieve a
     *            single service details.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     */
    public SIRequestServiceDetails(SICacheImpl siCache, Service service, boolean retrieveAll, String language,
            SIRequestor requestor)
    {
        super(siCache, language, requestor);
        this.service = service;
        this.retrieveAll = retrieveAll;
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
        //if (LOGGING) log.debug(id + " Attempting delivery");

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
                // SI request time is 15 sec
                this.setExpirationTime(currentTime+sMgr.getRequestAsyncTimeout());
            }
            
            ServiceHandle sHandle = ((ServiceExt) service).getServiceHandle();
            // Get the handles to all available service details
            ServiceDetailsHandle[] handles = siDatabase.getServiceDetailsByService(sHandle);

            // Allocate an array large enough to hold all service details
            // objects to be returned.
            int returnCount = 1;
            if (retrieveAll) returnCount = handles.length;
            ServiceDetails[] objects = new ServiceDetails[returnCount];

            // Process each service details handle
            for (int i = 0; i < returnCount; i++)
            {
                // Get the service details object from the cache if it is
                // present. Otherwise, create it from the database and add
                // it to the cache.
                objects[i] = siCache.getCachedServiceDetails(handles[i]);
                if (objects[i] == null)
                {
                    objects[i] = siDatabase.createServiceDetails(handles[i]);
                    siCache.putCachedServiceDetails(handles[i], objects[i]);
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
    public Service getService()
    {
        return this.service;
    }

    // Description copied from Object
    public int getServiceHandle()
    {
        // return the native handle associated with this service
        return ((ServiceExt) service).getServiceHandle().getHandle();
    }

    // Description copied from Object
    public String toString()
    {
        // TODO(Todd): Include IdentityHashcode for Service
        return super.toString() + "[retrieveAll=" + retrieveAll + ", language=" + language + "]";
    }

    /** The service whose service details is to be retrieved */
    private final Service service;

    /** Retrieve all service details if true, otherwise retrieve one. */
    private final boolean retrieveAll;
}
