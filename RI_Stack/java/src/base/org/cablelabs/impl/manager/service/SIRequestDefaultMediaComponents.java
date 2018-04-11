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
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.StreamType;

import org.apache.log4j.Logger;

import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SINotAvailableYetException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceComponentHandle;

/**
 * An instance of <code>SIRequestDefaultMediaComponents</code> represents an
 * outstanding asynchronous request for the set of <code>ServiceComponent</code>
 * objects which represent the default media components for a service.
 * 
 * @author Todd Earles
 */
public class SIRequestDefaultMediaComponents extends SIRequestServiceComponents
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIRequestDefaultMediaComponents.class);

    /** Identity of this object */
    protected String id = "@" + Integer.toHexString(System.identityHashCode(this));

    /**
     * Construct an <code>SIRequest</code> for the <code>ServiceComponent</code>
     * s which represent the default media components for the specified
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
    public SIRequestDefaultMediaComponents(SICacheImpl siCache, ServiceDetails serviceDetails, String language,
            SIRequestor requestor)
    {
        super(siCache, serviceDetails, language, requestor);
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
            // Get the handles to all available service components
            ServiceComponentHandle[] handles = siDatabase.getServiceComponentsByServiceDetails(sdHandle);

            ServiceComponentExt[] components = new ServiceComponentExt[handles.length];

            // Process each service component handle
            for (int i = 0; i < handles.length; i++)
            {
                // Get the service component object from the cache if it is
                // present. Otherwise, create it from the database and add
                // it to the cache.
                components[i] = (ServiceComponentExt) siCache.getCachedServiceComponent(handles[i]);
                if (components[i] == null)
                {
                    components[i] = siDatabase.createServiceComponent(handles[i]);
                    siCache.putCachedServiceComponent(handles[i], components[i]);
                }

                // Create language specific instance if required
                if (language != null)
                {
                    components[i] = (ServiceComponentExt) components[i].createLanguageSpecificVariant(language);
                }
                
                // Return service details specific variant of these components
                // The ServiceComponents may be common to one or more ServiceDetails.                
                ServiceComponentExt component = (ServiceComponentExt) components[i];
                components[i] = (ServiceComponentExt) component.createServiceDetailsSpecificVariant(details);
            }

            ServiceComponentExt[] sortedComponents = sortComponents(components);

            ServiceComponentExt videoComponent = null;
            ServiceComponentExt audioComponent = null;

            // The default audio and video are the first ones found in the list
            // of prioritized service components. Look for them.
            int numDefaultComponents = 0;
            for (int i = 0; i < sortedComponents.length && (videoComponent == null || audioComponent == null); i++)
            {
                if (videoComponent == null && sortedComponents[i].getStreamType().equals(StreamType.VIDEO))
                {
                    numDefaultComponents++;
                    videoComponent = sortedComponents[i];
                }
                else if (audioComponent == null && sortedComponents[i].getStreamType().equals(StreamType.AUDIO))
                {
                    numDefaultComponents++;
                    audioComponent = sortedComponents[i];
                }
            }

            // Add the default audio last and the video first to the array of
            // default components, if they were found. If neither were found
            // return an array of size 0.
            ServiceComponentExt[] defaultComponents = new ServiceComponentExt[numDefaultComponents];
            if (audioComponent != null)
            {
                defaultComponents[--numDefaultComponents] = audioComponent;
            }
            if (videoComponent != null)
            {
                defaultComponents[--numDefaultComponents] = videoComponent;
            }

            // Notify the requestor
            notifySuccess(defaultComponents);
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
}
