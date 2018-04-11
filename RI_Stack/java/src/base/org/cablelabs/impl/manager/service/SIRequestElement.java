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
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;

import org.apache.log4j.Logger;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.service.NetworkLocator;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.util.LocatorUtil;

/**
 * An instance of <code>SIRequestElement</code> represents an outstanding
 * asynchronous request for one or more SI elements.
 * 
 * @author Todd Earles
 */
public class SIRequestElement extends SIRequestImpl
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIRequestElement.class);

    /** Identity of this object */
    protected String id = "@" + Integer.toHexString(System.identityHashCode(this));

    /**
     * Construct an <code>SIRequest</code> for the specified SI element(s).
     * 
     * @param siCache
     *            The <code>SICache</code> to which this request belongs.
     * @param locator
     *            A locator that identifies one or more SI elements.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @throws InvalidLocatorException
     *             If the specified <code>locator</code> does not reference an
     *             SI element.
     */
    public SIRequestElement(SICacheImpl siCache, Locator locator, String language, SIRequestor requestor)
            throws InvalidLocatorException
    {
        super(siCache, language, requestor);

        if (log.isDebugEnabled())
        {
            log.debug(id + " SIRequestElement ctor...");
        }

        // Make sure we can construct a request object for the specified
        // locator. This ensures that the locator is valid.
        if (locator instanceof NetworkLocator)
        {
            // Create a request for a network
            initialRequest = new SIRequestNetwork(siCache, locator, new PassThroughRequestor());
        }
        else if (locator instanceof OcapLocator)
        {
            // Save the locator so we can get component information later
            ocapLocator = (OcapLocator) locator;            

            if (ocapLocator.getSourceID() != -1
            		|| (ocapLocator.getServiceName() != null)
                    || ((ocapLocator.getModulationFormat() == 255) 
                    || ((ocapLocator.getModulationFormat() != 255) && (ocapLocator.getProgramNumber() != -1))))
            {
            	// If sourceId locator it can have multiple handles
            	// Also make sure this is not a service component locator
                if (ocapLocator.getSourceID() != -1
                		&& LocatorUtil.isService(ocapLocator) 
                		&& !(LocatorUtil.isServiceComponent(ocapLocator)))
                {      
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + " Creating SIRequestServices...");
                    }
                    initialRequest = new SIRequestServices(siCache, locator, 
                    		language, new ServiceDetailsRequestor());
                }
                else
                {
                    Service service = siCache.getService(locator, language);
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + " Creating SIRequestServiceDetails...");
                    }
                    initialRequest = new SIRequestServiceDetails(siCache, service, true, language,
                            new ServiceDetailsRequestor());               	
                }
            }
            else
            {
                // Try to create a request for a transport stream
                initialRequest = new SIRequestTransportStream(siCache, locator, new PassThroughRequestor());
            }
        }
        else
            throw new InvalidLocatorException(locator, "Locator does not reference a retrievable SI object");
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

        // Cancel the subordinate request if one is pending   
        if (currentRequest != null)
            currentRequest.cancel();

        // Mark as canceled and send notification
        notifyFailure(SIRequestFailureType.CANCELED);
        canceled = true;
        return true;
    }

    // Description copied from SIRequestImpl
    public synchronized boolean attemptDelivery()
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " Attempting delivery");
        }

        // This method will only be called one time. The implementation of
        // this class must ensure that delivery is guaranteed.

        // The request has already been canceled so consider the request
        // to be finished.
        if (canceled)
            return true;

        // Enqueue the initial subordinate request and attempt its delivery
        if (initialRequest instanceof SIRequestNetwork)
            siCache.enqueueNetworkRequest(initialRequest);
        else if (initialRequest instanceof SIRequestTransportStream)
            siCache.enqueueTransportStreamRequest(initialRequest);
        else if(initialRequest instanceof SIRequestServiceDetails)
        	siCache.enqueueServiceDetailsRequest(initialRequest);  
        else if(initialRequest instanceof SIRequestServices)
        	siCache.enqueueServicesRequest(initialRequest);
        else
        {
            // Should never get here because the constructor must have created
            // a request. This is just for safety.
            notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
            return true;
        }

        // Mark that the initial subordinate request is now enqueued. Return
        // an indication that the request has not yet been satisfied.
        currentRequest = initialRequest;
        return false;
    }

    /**
     * This requestor is called with the results of a network or transport
     * stream lookup. Just pass through the failure or success notification to
     * the original requestor.
     */
    class PassThroughRequestor implements SIRequestor
    {
        // Description copied from SIRequestor
        public void notifyFailure(SIRequestFailureType reason)
        {
            // If the request has already been canceled just return.
            // Otherwise, pass on the failure.
			// Added for findbugs issues fix - using cached copy
        	boolean l_cancelled;
        	synchronized(SIRequestElement.this)
        	{
        		l_cancelled = canceled;
        	}
            if (l_cancelled)
                return;
            else
                SIRequestElement.this.notifyFailure(reason);
        }

        // Description copied from SIRequestor
        public void notifySuccess(SIRetrievable[] result)
        {
            // If the request has already been canceled just return.
            // Otherwise, pass on the failure.
			// Added for findbugs issues fix - using cached copy
        	boolean l_cancelled;
        	synchronized(SIRequestElement.this)
        	{
        		l_cancelled = canceled;
        	}
            if (l_cancelled)
                return;
            else
                SIRequestElement.this.notifySuccess(result);
        }
    }

    /**
     * This requestor is called with the results of a lookup of all service
     * details for a service.
     */
    class ServiceDetailsRequestor implements SIRequestor
    {
        // Description copied from SIRequestor
        public void notifyFailure(SIRequestFailureType reason)
        {
            // If the request has already been canceled just return.
            // Otherwise, pass on the failure.
			// Added for findbugs issues fix - using cached copy
        	boolean l_cancelled;
        	synchronized(SIRequestElement.this)
        	{
        		l_cancelled = canceled;
        	}
            if (l_cancelled)
                return;
            else
                SIRequestElement.this.notifyFailure(reason);
        }

        // Description copied from SIRequestor
        public void notifySuccess(SIRetrievable[] result)
        {
            // If the request has already been canceled just return.
			// Added for findbugs issues fix - using cached copy
        	boolean l_cancelled;
        	synchronized(SIRequestElement.this)
        	{
        		l_cancelled = canceled;
        	}
            if (l_cancelled) return;

            // Just pass on the array if it is empty
            if (result.length == 0)
            {
                SIRequestElement.this.notifySuccess(result);
                return;
            }

            // If components are specified, then retrieve the components.
            // Otherwise, return the service details we already have.
            ServiceDetails serviceDetails = (ServiceDetails) result[0];
            int[] pids = ocapLocator.getPIDs();
            String[] componentNames = ocapLocator.getComponentNames();
            int[] componentTags = ocapLocator.getComponentTags();
            short[] streamTypes = ocapLocator.getStreamTypes();
            if ((pids.length != 0) || (componentNames.length != 0) || (componentTags.length != 0)
                    || (streamTypes.length != 0))
            {
                // Retrieve the components
                int[] indexes = ocapLocator.getIndexes();
                String[] languageCodes = ocapLocator.getLanguageCodes();
			// Added for findbugs issues fix
			// Synchronizing on proper object           
                SIRequestImpl l_currentRequest;
                synchronized(SIRequestElement.this)
                {
                currentRequest = new SIRequestServiceComponents(siCache, serviceDetails, pids, componentNames,
                        componentTags, streamTypes, indexes, languageCodes, language, requestor);
                	l_currentRequest = currentRequest;
                }
                siCache.enqueueServiceDetailsRequest(l_currentRequest);
            }
            else
            {
                // No component(s) specified by the locator so return the
                // service details
                SIRequestElement.this.notifySuccess(result);
            }
        }
    }

    /**
     * This requestor is called with the results of a lookup of service
     * details for a sourceId.
     */
    class ServiceDetailsForSourceIdRequestor implements SIRequestor
    {
        // Description copied from SIRequestor
        public void notifyFailure(SIRequestFailureType reason)
        {
            // If the request has already been canceled just return.
            // Otherwise, pass on the failure.
            boolean l_cancelled;
            synchronized(SIRequestElement.this)
            {
                l_cancelled = canceled;
            }
            if (l_cancelled)
                return;
            else
                SIRequestElement.this.notifyFailure(reason);
        }

        // Description copied from SIRequestor
        public void notifySuccess(SIRetrievable[] result)
        {
            // If the request has already been canceled just return.
            boolean l_cancelled;
            synchronized(SIRequestElement.this)
            {
                l_cancelled = canceled;
            }
            if (l_cancelled) return;

            // Just pass on the array if it is empty
            if (result.length == 0)
            {
                SIRequestElement.this.notifySuccess(result);
                return;
            }
            SIRequestElement.this.notifySuccess(result);

        }
    }
    
    // Description copied from Object
    public String toString()
    {
        return super.toString() + "[" + ocapLocator + ", language=" + language + "]";
    }

    /** The OCAP locator if one is specified */
    private OcapLocator ocapLocator = null;

    /** The initial subordinate request */
    private SIRequestImpl initialRequest = null;
    
    /** The current outstanding subordinate request */
    private SIRequestImpl currentRequest = null;
}
