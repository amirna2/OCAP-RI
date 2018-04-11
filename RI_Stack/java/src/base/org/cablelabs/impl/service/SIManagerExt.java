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

package org.cablelabs.impl.service;

import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIElement;
import javax.tv.service.SIException;
import javax.tv.service.SIManager;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.TransportStream;

/**
 * Implementation specific extensions to <code>SIManager</code>
 * 
 * @author Todd Earles
 */
public abstract class SIManagerExt extends SIManager
{
    /**
     * Retrieves the <code>TransportStream</code> object corresponding to the
     * given <code>Locator</code>.
     * <p>
     * Note that the locator may point to an SI element lower in the hierarchy
     * than a transport stream (such as a service component event). In such a
     * case, the <code>TransportStream</code> for the service that the service
     * component is part of will be returned.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param locator
     *            A locator referencing a TransportStream
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid
     *             <code>TransportStream</code>.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @see TransportStream
     * @see javax.tv.service.ReadPermission
     */
    public abstract SIRequest retrieveTransportStream(Locator locator, SIRequestor requestor)
            throws InvalidLocatorException, SecurityException;

    /**
     * Returns the <code>TransportStream</code> object corresponding to the
     * given <code>Locator</code>.
     * <p>
     * Note that the locator may point to an SI element lower in the hierarchy
     * than a transport stream (such as a service component event). In such a
     * case, the <code>TransportStream</code> for the service that the service
     * component is part of will be returned.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     * 
     * @param locator
     *            A locator referencing a TransportStream
     * @return The <code>TransportStream</code> corresponding to the specified
     *         <code>locator</code> or null if the transport stream is not
     *         currently mapped.
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid
     *             <code>TransportStream</code>.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>.
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     */
    public TransportStream getTransportStream(Locator locator) throws InvalidLocatorException, SecurityException,
            SIRequestException, InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveTransportStream(locator, requestor);
            requestor.waitForCompletion();
        }

        TransportStream ts = null;
        if (requestor.getResults().length > 0) ts = (TransportStream) (requestor.getResults()[0]);

        return ts;
    }

    /**
     * Returns the <code>Service</code> object corresponding to the given
     * service number.
     * 
     * @param serviceNumber
     *            The service number for the service.
     * @param minorNumber
     *            The service minor number for the service. A value of -1
     *            indicates the one-part service number is defined by
     *            <code>serviceNumber</code>.
     * @return The <code>Service</code> object with the specified service
     *         number.
     * @throws SIException
     *             If the service cannot be retrieved
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(service.getLocator())</code>
     *             where <code>service</code> is the service to be returned.
     */
    public abstract Service getService(int serviceNumber, int minorNumber) throws SIException;

    /**
     * Returns the <code>Service</code> object corresponding to the given
     * channel number.
     * 
     * @param majorChannelNumber
     *            The major_channel_number as defined by SCTE 65.
     * @param minorChannelNumber
     *            The minor_channel_number as defined by SCTE 65.
     * @return The <code>Service</code> object with the specified service
     *         number.
     * @throws SIException
     *             If the service cannot be retrieved
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(service.getLocator())</code>
     *             where <code>service</code> is the service to be returned.
     */
    public abstract Service getService(short majorChannelNumber, short minorChannelNumber) throws SIException;

    /**
     * Returns the <code>Service</code> object corresponding to the given
     * application Id (for DSG only).
     * 
     * @param appId
     *            The appId specific to the DSG tunnel
     * @return The <code>Service</code> object.
     * @throws SIException
     *             If the service cannot be retrieved
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(service.getLocator())</code>
     *             where <code>service</code> is the service to be returned.
     */
    public abstract Service getServiceByAppId(int appId) throws SIException;

    /**
     * Returns the <code>ServiceDetails</code> object corresponding to the given
     * <code>Locator</code>.
     * <p>
     * Note that the locator may point to an SI element lower in the hierarchy
     * than a service (such as a program event). In such a case, the
     * <code>ServiceDetails</code> for the service that the program event is
     * part of will be returned.
     * <p>
     * If a transport-independent locator is provided, one or more
     * <code>ServiceDetails</code> objects may be returned. However, it is
     * permissible in this case for this method to always retrieve a single
     * <code>ServiceDetails</code> object, as determined by the implementation,
     * user preferences, or availability. To obtain all of the corresponding
     * <code>ServiceDetails</code> objects, the application may transform the
     * transport-independent locator into multiple transport-dependent locators
     * and retrieve a <code>ServiceDetails</code> object for each.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     * 
     * @param locator
     *            A locator referencing a Service
     * @return The <code>ServiceDetails</code> corresponding to the specified
     *         <code>locator</code>.
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid
     *             <code>Service</code>.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>.
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @see SIManager#retrieveServiceDetails(Locator, SIRequestor)
     */
    public ServiceDetails[] getServiceDetails(Locator locator) throws InvalidLocatorException, SecurityException,
            SIRequestException, InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveServiceDetails(locator, requestor);
            requestor.waitForCompletion();
        }

        // Return the results
        return (ServiceDetails[]) (requestor.getResults());
    }

    /**
     * Returns the <code>SIElement</code> corresponding to the specified
     * <code>Locator</code>. If the locator identifies more than one
     * <code>SIElement</code>, all matching <code>SIElements</code> are
     * retrieved.
     * <p>
     * For example, multiple <code>SIElement</code> objects are retrieved when
     * the locator represents identical content delivered over different media
     * (terrestrial, satellite, cable, etc.) or a specific program event made
     * available at different times, possibly on different services.
     * <p>
     * This call retrieves various types of <code>SIElement</code> instances
     * according to the locator specified. For example, if the locator is a
     * transport-dependent locator to a service (and therefore to a
     * <code>ServiceDetails</code> -- see {@link Service#getLocator} for more
     * information), a <code>ServiceDetails</code> object is retrieved; if the
     * locator represents a program event, a <code>ProgramEvent</code> object is
     * retrieved; if the locator represents a service component, a
     * <code>ServiceComponent</code> is retrieved.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     * 
     * @param locator
     *            A locator referencing one or more SIElements.
     * @return The <code>SIElement</code> objects corresponding to the specified
     *         <code>locator</code>.
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid
     *             <code>SIElement</code>.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>.
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @see SIManager#retrieveSIElement(Locator, SIRequestor)
     */
    public SIElement[] getSIElement(Locator locator) throws InvalidLocatorException, SecurityException,
            SIRequestException, InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveSIElement(locator, requestor);
            requestor.waitForCompletion();
        }

        // Return the results
        return (SIElement[]) requestor.getResults();
    }

    /**
     * Returns the complete set of <code>SIElement</code> instances
     * corresponding to the array of <code>Locator</code> instances passed to
     * this method. Each entry in the <code>Locator</code> array is processed as
     * defined by {@link #getSIElement(Locator)} and entries in this array may
     * identify a mixture of SIElement types.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     * 
     * @param locators
     *            An array of locators which each reference one or more
     *            SIElements.
     * @return The <code>SIElement</code> objects corresponding to the specified
     *         <code>locators</code>.
     * @throws InvalidLocatorException
     *             If one or more entries in <code>locators</code> does not
     *             reference a valid <code>SIElement</code>.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>.
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @see SIManagerExt#getSIElement(Locator)
     */
    public SIElement[] getSIElements(Locator[] locators) throws InvalidLocatorException, SecurityException,
            SIRequestException, InterruptedException
    {
        Vector results = new Vector();

        // Process each locator
        for (int i = 0; i < locators.length; i++)
        {
            // Get the SIElements for the next locator
            SIElement[] elements = getSIElement(locators[i]);

            // Add each SIElement to the result list
            for (int n = 0; n < elements.length; n++)
                results.add(elements[n]);
        }

        // Return the results
        return (SIElement[]) results.toArray(new SIElement[results.size()]);
    }
}
