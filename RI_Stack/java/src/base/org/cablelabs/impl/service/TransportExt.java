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

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIRequestor;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.NetworkCollection;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;
import javax.tv.service.transport.TransportStreamCollection;

import org.cablelabs.impl.util.CallbackList;

/**
 * Implementation specific extensions to <code>Transport</code>
 *
 * @author Todd Earles
 */
public abstract class TransportExt implements UniqueIdentifier, NetworkCollection, TransportStreamCollection
{
    /**
     * Create a snapshot of this <code>Transport</code> and associate it with
     * the specified SI cache.
     *
     * @param siCache
     *            The cache this snapshot is to be associated with
     * @return A copy of this object associated with <code>siCache</code>
     * @throws UnsupportedOperationException
     *             If creation of a snapshot is not supported
     */
    public abstract Transport createSnapshot(SICache siCache);

    /**
     * Returns the handle that identifies this <code>Transport</code> within the
     * SI database.
     *
     * @return The transport handle or null if not available via the SIDatabase.
     */
    public abstract TransportHandle getTransportHandle();

    /**
     * Returns the transportID for this <code>Transport</code>.
     *
     * @return The transportID.
     */
    public abstract int getTransportID();

    /**
     * Returns the specified <code>Network</code> in this <code>Transport</code>
     * .
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     *
     * @param locator
     *            Locator referencing the <code>Network</code> of interest.
     * @return The <code>Network</code> corresponding to the specified
     *         <code>locator</code>.
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid
     *             <code>Network</code>.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>.
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @see NetworkCollection#retrieveNetwork(Locator, SIRequestor)
     */
    public Network getNetwork(Locator locator) throws InvalidLocatorException, SecurityException, SIRequestException,
            InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveNetwork(locator, requestor);
            requestor.waitForCompletion();
        }

        // Return the results
        return (Network) requestor.getResults()[0];
    }

    /**
     * Returns an array of all the <code>Network</code> objects in this
     * <code>Transport</code>. The array will only contain <code>Network</code>
     * instances <code>n</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(n.getLocator())</code>. If no
     * <code>Network</code> instances meet this criteria, this method throws an
     * <code>SIRequestException</code> containing a
     * <code>SIRequestFailureType</code> of <code>DATA_UNAVAILABLE</code>.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     *
     * @return The <code>Network</code> objects available in this
     *         <code>Transport</code>.
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @see NetworkCollection#retrieveNetworks(SIRequestor)
     */
    public Network[] getNetworks() throws SIRequestException, InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveNetworks(requestor);
            requestor.waitForCompletion();
        }

        // Return the results
        return (Network[]) (requestor.getResults());
    }

    /**
     * Returns the <code>TransportStream</code> identified by the specified
     * transport stream locator.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     *
     * @param locator
     *            Locator referencing the <code>TransportStream</code> of
     *            interest.
     * @return The <code>TransportStream</code> corresponding to the specified
     *         <code>locator</code>.
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid
     *             <code>TransportStream</code>.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @see TransportStreamCollection#retrieveTransportStream
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

        // Return the results
        return (TransportStream) requestor.getResults()[0];
    }

    /**
     * Returns an array of the <code>TransportStream</code> objects in this
     * <code>Transport</code>. The array will only contain
     * <code>TransportStream</code> instances <code>ts</code> for which the
     * caller has <code>javax.tv.service.ReadPermission(ts.getLocator())</code>.
     * If no <code>TransportStream</code> instances meet this criteria, this
     * method throws an <code>SIRequestException</code> containing a
     * <code>SIRequestFailureType</code> of <code>DATA_UNAVAILABLE</code>.
     * <p>
     * This method delivers its results synchronously but may block waiting on
     * completion of an underlying asynchronous SI request.
     *
     * @return The <code>TransportStream</code> objects available in this
     *         <code>Transport</code>.
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @see TransportStreamCollection#retrieveTransportStreams
     */
    public TransportStream[] getTransportStreams() throws SIRequestException, InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveTransportStreams(requestor);
            requestor.waitForCompletion();
        }

        // Return the results
        return (TransportStream[]) (requestor.getResults());
    }

    /**
     * Sends a <code>ServiceDetailsChangeEvent</code> to the listeners that are
     * registered with this <code>Transport</code> object.
     *
     * @param event
     *            The <code>ServiceDetailsChangeEvent</code> to be sent to
     *            registered listeners.
     */
    public abstract void postServiceDetailsChangeEvent(ServiceDetailsChangeEvent event);

    /*
     * Following callbacks are invoked to notify of ServiceDetails
     * re-map when a Selection Provider re-maps an SPI Service or when OOB table
     * updates modifies ServiceDetails of a Service.
     */
    /**
     * The list of callbacks currently registered. A single list is kept
     * all callbacks get notified for any change to any
     * ServiceDetails.
     */
    public static final CallbackList callbacks = new CallbackList(ServiceDetailsCallback.class);

    /**
     * Add a {@link ServiceDetailsCallback} to the list of objects which are
     * synchronously notified of remap operations. Notification is given in
     * order from highest to lowest priority.
     *
     * @param callback
     *            The {@link ServiceDetailsCallback} to add.
     * @param priority
     *            The priority for this callback where a higher numerical value
     *            indicate a higher priority.
     */
    public static void addServiceDetailsCallback(ServiceDetailsCallback callback, int priority)
    {
        callbacks.addCallback(callback, priority);
    }

    /**
     * Remove the specified synchronous callback object (
     * {@link ServiceDetailsCallback}) from the list of registered callbacks.
     * Has no effect if the callback is not in the list.
     *
     * @param callback
     *            The {@link ServiceDetailsCallback} to remove.
     */
    public static void removeServiceDetailsCallback(ServiceDetailsCallback callback)
    {
        callbacks.removeCallback(callback);
    }
}
