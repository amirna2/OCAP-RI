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

import java.util.Iterator;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.RatingDimension;
import javax.tv.service.SIException;
import javax.tv.service.SIManager;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDescription;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.NetworkCollection;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;
import javax.tv.service.transport.TransportStreamChangeListener;
import javax.tv.service.transport.TransportStreamCollection;

import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.TableChangeListener;

import org.cablelabs.impl.manager.service.SIRequestImpl;
import org.cablelabs.impl.manager.service.ServiceCollection;

/**
 * Resolve service information queries by returning already created SI objects
 * from an SI cache. If the requested SI object is not in the cache then it is
 * created and placed in the cache. If interest in an SI object is registered
 * with the cache, then it is automatically cached as soon as it is available on
 * the platform.
 * 
 * @author Todd Earles
 */
public interface SICache
{
    /**
     * Set the <code>SIDatabase</code> instance to be associated with this
     * instance of the <code>SICache</code>. This instance of
     * <code>SICache</code> should not be considered to be fully initialized
     * until this method is called. Attempts to access the
     * <code>SIDatabase</code> before this method is called should throw
     * <code>IllegalStateException</code>.
     * 
     * @param siDatabase
     *            The SIDatabase to associate with this SICache.
     * @throws IllegalStateException
     *             If the SIDatabase has already been set
     */
    public void setSIDatabase(SIDatabase siDatabase);

    /**
     * Get the <code>SIDatabase</code> instance associated with this instance of
     * the <code>SICache</code>.
     * 
     * @return The SIDatabase associated with this SICache.
     * @throws IllegalStateException
     *             If the SIDatabase has not been set
     */
    public SIDatabase getSIDatabase();

    /**
     * Provides the names of available rating dimensions in the local rating
     * region. A zero-length array is returned if no rating dimensions are
     * available.
     * 
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @return An array of strings representing the names of available rating
     *         dimensions in this rating region. If none are available, then an
     *         empty array is returned.
     * @see SIManager#getSupportedDimensions()
     * @see SIManager#setPreferredLanguage(String)
     */
    public String[] getSupportedDimensions(String language);

    /**
     * Reports the <code>RatingDimension</code> corresponding to the specified
     * string name.
     * 
     * @param name
     *            The name of the requested rating dimension.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @return The requested <code>RatingDimension</code>.
     * @throws SIException
     *             If <code>name</code> is not a supported rating dimension, as
     *             returned by <code>getSupportedDimensions()</code>.
     * @see SIManager#getRatingDimension
     * @see SIManager#setPreferredLanguage(String)
     */
    public RatingDimension getRatingDimension(String name, String language) throws SIException;

    /**
     * Get the specified rating dimension from the cache.
     * 
     * @param handle
     *            The handle to the rating dimension of interest
     * @return The rating dimension or null if it is not in the cache
     */
    public RatingDimension getCachedRatingDimension(RatingDimensionHandle handle);

    /**
     * Put the specified rating dimension in the cache.
     * 
     * @param handle
     *            The handle to the rating dimension to put in the cache
     * @param ratingDimension
     *            The rating dimension to put in the cache
     */
    public void putCachedRatingDimension(RatingDimensionHandle handle, RatingDimension ratingDimension);

    /**
     * Reports the various content delivery mechanisms currently available on
     * this platform. The implementation must be capable of supporting at least
     * one <code>Transport</code> instance.
     * 
     * @return An array of <code>Transport</code> objects representing the
     *         content delivery mechanisms currently available on this platform.
     *         If none are available, then an empty array is returned.
     * @see SIManager#getTransports
     */
    public Transport[] getTransports();

    /**
     * Get the specified transport from the cache.
     * 
     * @param handle
     *            The handle to the transport of interest
     * @return The transport or null if it is not in the cache
     */
    public Transport getCachedTransport(TransportHandle handle);

    /**
     * Put the specified transport in the cache.
     * 
     * @param handle
     *            The handle to the transport to put in the cache
     * @param transport
     *            The transport to put in the cache
     */
    public void putCachedTransport(TransportHandle handle, Transport transport);

    /**
     * Return an iterator to iterate over all cached Transport objects.
     */
    public Iterator getTransportCollection();

    /**
     * Retrieves an array of all the <code>Network</code> objects containing the
     * specified <code>Transport</code>. The array will only contain
     * <code>Network</code> instances <code>n</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(n.getLocator())</code>. If no
     * <code>Network</code> instances meet this criteria, this method will
     * result in an <code>SIRequestFailureType</code> of
     * <code>DATA_UNAVAILABLE</code>.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param transport
     *            The transport contained by the <code>Network</code>s of
     *            interest.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see NetworkCollection#retrieveNetworks
     */
    public SIRequest retrieveNetworks(Transport transport, SIRequestor requestor);

    /**
     * Retrieves the <code>Network</code> identified by the specified network
     * locator.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param locator
     *            Locator referencing the <code>Network</code> of interest.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid service.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>
     * @see NetworkCollection#retrieveNetwork
     */
    public SIRequest retrieveNetwork(Locator locator, SIRequestor requestor) throws InvalidLocatorException;

    /**
     * Get the specified network from the cache.
     * 
     * @param handle
     *            The handle to the network of interest
     * @return The network or null if it is not in the cache
     */
    public Network getCachedNetwork(NetworkHandle handle);

    /**
     * Put the specified network in the cache.
     * 
     * @param handle
     *            The handle to the network to put in the cache
     * @param network
     *            The network to put in the cache
     */
    public void putCachedNetwork(NetworkHandle handle, Network network);

    /**
     * Enqueue the specified network request.
     * 
     * @param request
     *            The request to be enqueued.
     */
    public void enqueueNetworkRequest(final SIRequestImpl request);

    /**
     * Cancel the specified network request.
     * 
     * @param request
     *            The request to be canceled.
     * @return True if the request was canceled. False if the request was no
     *         longer pending.
     */
    public boolean cancelNetworkRequest(SIRequestImpl request);

    /**
     * Return an iterator to iterate over all cached Network objects.
     */
    public Iterator getNetworkCollection();

    /**
     * Retrieves an array of the <code>TransportStream</code> objects in the
     * specified <code>Transport</code>. The array will only contain
     * <code>TransportStream</code> instances <code>ts</code> for which the
     * caller has <code>javax.tv.service.ReadPermission(ts.getLocator())</code>.
     * If no <code>TransportStream</code> instances meet this criteria, this
     * method will result in an <code>SIRequestFailureType</code> of
     * <code>DATA_UNAVAILABLE</code>.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param transport
     *            The transport which carries the <code>TransportStream</code>s
     *            of interest.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see TransportStreamCollection#retrieveTransportStreams
     */
    public SIRequest retrieveTransportStreams(Transport transport, SIRequestor requestor);

    /**
     * Retrieves an array of <code>TransportStream</code> objects representing
     * the transport streams carried in this <code>Network</code>. Only
     * <code>TransportStream</code> instances <code>ts</code> for which the
     * caller has <code>javax.tv.service.ReadPermission(ts.getLocator())</code>
     * will be present in the array. If no <code>TransportStream</code>
     * instances meet this criteria or if this <code>Network</code> does not
     * aggregate transport streams, the result is an
     * <code>SIRequestFailureType</code> of <code>DATA_UNAVAILABLE</code>.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param network
     *            The network which carries the <code>TransportStream</code>s of
     *            interest.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see Network#retrieveTransportStreams
     */
    public SIRequest retrieveTransportStreams(Network network, SIRequestor requestor);

    /**
     * Retrieves the <code>TransportStream</code> identified by the specified
     * transport stream locator.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param locator
     *            Locator referencing the <code>TransportStream</code> of
     *            interest.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid service.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>
     * @see TransportStreamCollection#retrieveTransportStream
     */
    public SIRequest retrieveTransportStream(Locator locator, SIRequestor requestor) throws InvalidLocatorException;

    /**
     * Get the specified transport stream from the cache.
     * 
     * @param handle
     *            The handle to the transport stream of interest
     * @return The transport stream or null if it is not in the cache
     */
    public TransportStream getCachedTransportStream(TransportStreamHandle handle);

    /**
     * Put the specified transport stream in the cache.
     * 
     * @param handle
     *            The handle to the transport stream to put in the cache
     * @param transportStream
     *            The transport stream to put in the cache
     */
    public void putCachedTransportStream(TransportStreamHandle handle, TransportStream transportStream);

    /**
     * Enqueue the specified transport stream request.
     * 
     * @param request
     *            The request to be enqueued.
     */
    public void enqueueTransportStreamRequest(final SIRequestImpl request);

    /**
     * Cancel the specified transport stream request.
     * 
     * @param request
     *            The request to be canceled.
     * @return True if the request was canceled. False if the request was no
     *         longer pending.
     */
    public boolean cancelTransportStreamRequest(SIRequestImpl request);

    /**
     * Return an iterator to iterate over all cached TransportStream objects.
     */
    public Iterator getTransportStreamCollection();

    /**
     * Adds all broadcast services to the specified {@link ServiceCollection}.
     * Only <code>Service</code> instances for which the caller has
     * <code>javax.tv.service.ReadPermission</code> on the underlying locator
     * are added to the collection (this is enforced by
     * {@link ServiceCollection#add(Service)}.
     * 
     * @param collection
     *            The service collection to add all services to
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @see SIManager#filterServices(ServiceFilter)
     * @see SIManager#setPreferredLanguage(String)
     */
    public void getAllServices(ServiceCollection collection, String language);

    /**
     * Adds all broadcast services to the specified {@link ServiceCollection}.
     * Only <code>Service</code> instances for which the caller has
     * <code>javax.tv.service.ReadPermission</code> on the underlying locator
     * are added to the collection (this is enforced by
     * {@link ServiceCollection#add(Service)}.
     * 
     * @param collection
     *            The service collection to add all services to
     * @param ts
     *            The transport stream which the services belong to
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @see SIManager#filterServices(ServiceFilter)
     * @see SIManager#setPreferredLanguage(String)
     */
    public void getAllServicesForTransportStream(ServiceCollection collection, TransportStream ts, String language);

    /**
     * Provides the <code>Service</code> referred to by a given
     * <code>Locator</code>.
     * 
     * @param locator
     *            A locator specifying a service.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @return The <code>Service</code> object corresponding to the specified
     *         locator.
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid
     *             <code>Service</code>.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>
     * @see SIManager#getService
     * @see SIManager#setPreferredLanguage(String)
     */
    public Service getService(Locator locator, String language) throws InvalidLocatorException;

    /**
     * Provides the <code>Service</code> referred to by a given service number.
     * 
     * @param serviceNumber
     *            The service number for the service.
     * @param minorNumber
     *            The service minor number for the service. A value of -1
     *            indicates the one-part service number is defined by
     *            <code>serviceNumber</code>.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @return The <code>Service</code> object with the specified service
     *         number.
     * @throws SIDatabaseException
     *             If the service cannot be retrieved from the SI database
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(service.getLocator())</code>
     *             where <code>service</code> is the service to be returned.
     * @see SIManager#setPreferredLanguage(String)
     */
    public Service getService(int serviceNumber, int minorNumber, String language) throws SIDatabaseException;

    /**
     * Provides the <code>Service</code> referred to by a given app Id.
     * 
     * @param appId
     *            The app Id for the service (for DSG only).
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @return The <code>Service</code> object with the specified service
     *         number.
     * @throws SIDatabaseException
     *             If the service cannot be retrieved from the SI database
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(service.getLocator())</code>
     *             where <code>service</code> is the service to be returned.
     * @see SIManager#setPreferredLanguage(String)
     */
    public Service getService(int appId, String language) throws SIDatabaseException;

    /**
     * Get the specified service from the cache.
     * 
     * @param handle
     *            The handle to the service of interest
     * @return The service or null if it is not in the cache
     */
    public Service getCachedService(ServiceHandle handle);

    /**
     * Put the specified service in the cache.
     * 
     * @param handle
     *            The handle to the service to put in the cache
     * @param service
     *            The service to put in the cache
     */
    public void putCachedService(ServiceHandle handle, Service service);

    /**
     * Return an iterator to iterate over all cached Service objects.
     */
    public Iterator getServiceCollection();

    /**
     * Retrieves the <code>ServiceDetails</code> object specified by the given
     * <code>Service</code>.
     * <p>
     * If the content represented by this <code>Service</code> is delivered on
     * multiple transports there may be multiple <code>ServiceDetails</code> for
     * it. This method retrieves one of them based on availability or user
     * preferences.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param service
     *            The service for which details is to be retrieved.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param retrieveAll
     *            If true retrieve all service details. If false retrieve a
     *            single service details.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see SIManager#retrieveServiceDetails
     * @see Service#retrieveDetails
     * @see SIManager#setPreferredLanguage(String)
     */
    public SIRequest retrieveServiceDetails(Service service, String language, boolean retrieveAll, SIRequestor requestor);

    /**
     * Get the specified service details from the cache.
     * 
     * @param handle
     *            The handle to the service details of interest
     * @return The service details or null if it is not in the cache
     */
    public ServiceDetails getCachedServiceDetails(ServiceDetailsHandle handle);

    /**
     * Put the specified service details in the cache.
     * 
     * @param handle
     *            The handle to the service details to put in the cache
     * @param serviceDetails
     *            The service details to put in the cache
     */
    public void putCachedServiceDetails(ServiceDetailsHandle handle, ServiceDetails serviceDetails);

    /**
     * Enqueue the specified service details request.
     * 
     * @param request
     *            The request to be enqueued.
     */
    public void enqueueServiceDetailsRequest(final SIRequestImpl request);

    /**
     * Cancel the specified service details request.
     * 
     * @param request
     *            The request to be canceled.
     * @return True if the request was canceled. False if the request was no
     *         longer pending.
     */
    public boolean cancelServiceDetailsRequest(SIRequestImpl request);

    /**
     * Return an iterator to iterate over all cached ServiceDetails objects.
     */
    public Iterator getServiceDetailsCollection();

    /**
     * Retrieves the <code>ServiceDescription</code> for the specified
     * <code>ServiceDetails</code>.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param serviceDetails
     *            The service details for which a description is to be
     *            retrieved.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see ServiceDetails#retrieveServiceDescription
     * @see SIManager#setPreferredLanguage(String)
     */
    public SIRequest retrieveServiceDescription(ServiceDetails serviceDetails, String language, SIRequestor requestor);

    /**
     * Get the specified service description from the cache.
     * 
     * @param handle
     *            The handle to the service details for the service description
     *            of interest
     * @return The service description or null if it is not in the cache
     */
    public ServiceDescription getCachedServiceDescription(ServiceDetailsHandle handle);

    /**
     * Put the specified service description in the cache.
     * 
     * @param handle
     *            The handle to the service details for the service description
     *            to put in the cache
     * @param serviceDescription
     *            The service description to put in the cache
     */
    public void putCachedServiceDescription(ServiceDetailsHandle handle, ServiceDescription serviceDescription);

    /**
     * Return an iterator to iterate over all cached ServiceDescription objects.
     */
    public Iterator getServiceDescriptionCollection();

    /**
     * Retrieves the <code>ProgramAssociationTable</code> corresponding to the
     * specified <code>Locator</code>.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param locator
     *            An OcapLocator referencing an Ocap service by SourceID or
     *            Frequency/Program#
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws InvalidLocatorException
     *             If <code>locator</code> is not an OcapLocator or does not
     *             reference a valid OCAP service by SourceID or
     *             Frequency/Program#
     */
    public SIRequest retrieveProgramAssociationTable(Locator locator, SIRequestor requestor);

    /**
     * Get the specified PAT from the cache.
     * 
     * @param handle
     *            The handle to the PAT of interest
     * @return The PAT or null if it is not in the cache
     */
    public ProgramAssociationTable getCachedProgramAssociationTable(ProgramAssociationTableHandle handle);

    /**
     * Put the specified PAT in the cache.
     * 
     * @param handle
     *            The handle associated with the PAT put in the cache
     * @param programAssociationTable
     *            The PAT to put in the cache
     */
    public void putCachedProgramAssociationTable(ProgramAssociationTableHandle handle,
            ProgramAssociationTable programAssociationTable);

    /**
     * Retrieves the <code>ProgramMapTable</code> corresponding to the specified
     * <code>Locator</code>.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param locator
     *            An OcapLocator referencing an Ocap service by SourceID or
     *            Frequency/Program#
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws InvalidLocatorException
     *             If <code>locator</code> is not an OcapLocator or does not
     *             reference a valid OCAP service by SourceID or
     *             Frequency/Program#
     */
    public SIRequest retrieveProgramMapTable(Locator locator, SIRequestor requestor);
    
    public SIRequest retrieveProgramMapTable(Service service, SIRequestor requestor);

    /**
     * Get the specified PMT from the cache.
     * 
     * @param handle
     *            The handle of the PMT of interest
     * @return The PMT or null if it is not in the cache
     */
    public ProgramMapTable getCachedProgramMapTable(ProgramMapTableHandle handle);

    /**
     * Put the specified PMT in the cache.
     * 
     * @param handle
     *            The handle associated with the PMT put in the cache
     * @param programMapTable
     *            The PMT to put in the cache
     */
    public void putCachedProgramMapTable(ProgramMapTableHandle handle, ProgramMapTable programMapTable);

    /**
     * Retrieves an array of the <code>ServiceComponent</code> objects carried
     * in the specified <code>ServiceDetails</code>. The array will only contain
     * <code>ServiceComponent</code> instances <code>c</code> for which the
     * caller has <code>javax.tv.service.ReadPermission(c.getLocator())</code>.
     * If no <code>ServiceComponent</code> instances meet this criteria, this
     * method will result in an <code>SIRequestFailureType</code> of
     * <code>DATA_UNAVAILABLE</code>.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param serviceDetails
     *            The service details for which components are to be retrieved.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see ServiceDetails#retrieveComponents
     * @see SIManager#setPreferredLanguage(String)
     */
    public SIRequest retrieveServiceComponents(ServiceDetails serviceDetails, String language, SIRequestor requestor);

    /**
     * Retrieves an array of the <code>ServiceComponent</code> objects which
     * represent the default media components carried in the specified
     * <code>ServiceDetails</code>.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param serviceDetails
     *            The service details for which components are to be retrieved.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see ServiceDetailsExt#retrieveDefaultMediaComponents
     * @see SIManager#setPreferredLanguage(String)
     */
    public SIRequest retrieveDefaultMediaComponents(ServiceDetails serviceDetails, String language,
            SIRequestor requestor);

    /**
     * Retrieves the <code>ServiceComponent</code> object which represents the
     * default object carousel for the specified <code>ServiceDetails</code>.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param serviceDetails
     *            The service details for which components are to be retrieved.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see ServiceDetailsExt#retrieveDefaultMediaComponents
     * @see SIManager#setPreferredLanguage(String)
     */
    public SIRequest retrieveCarouselComponent(ServiceDetails serviceDetails, String language, SIRequestor requestor);

    /**
     * Retrieves the <code>ServiceComponent</code> object which represents the
     * object carousel with the specified carousel ID for the specified
     * <code>ServiceDetails</code>.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param serviceDetails
     *            The service details for which components are to be retrieved.
     * @param carouselID
     *            The carousel ID of the object carousel to retrieve
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see ServiceDetailsExt#retrieveDefaultMediaComponents
     * @see SIManager#setPreferredLanguage(String)
     */
    public SIRequest retrieveCarouselComponent(ServiceDetails serviceDetails, int carouselID, String language,
            SIRequestor requestor);

    /**
     * Retrieves the <code>ServiceComponent</code> object which corresponds to
     * the specified association tag.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param serviceDetails
     *            The service details where the search for the component is to
     *            start
     * @param associationTag
     *            The association tag of the service component to retrieve
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see ServiceDetailsExt#retrieveComponentByAssociationTag
     * @see SIManager#setPreferredLanguage(String)
     */
    public SIRequest retrieveComponentByAssociationTag(ServiceDetails serviceDetails, int associationTag,
            String language, SIRequestor requestor);

    /**
     * Get the specified service component from the cache.
     * 
     * @param handle
     *            The handle to the service component of interest
     * @return The service component or null if it is not in the cache
     */
    public ServiceComponent getCachedServiceComponent(ServiceComponentHandle handle);

    /**
     * Put the specified service component in the cache.
     * 
     * @param handle
     *            The handle to the service component to put in the cache
     * @param serviceComponent
     *            The service component to put in the cache
     */
    public void putCachedServiceComponent(ServiceComponentHandle handle, ServiceComponent serviceComponent);

    /**
     * Retrieves the PCR Pid which corresponds to
     * the specified ServiceDetails.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param serviceDetails
     *            The service details where the search for the PCR Pid is to
     *            start
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see ServiceDetailsExt#retrievePCRPid
     * @see SIManager#setPreferredLanguage(String)
     */
    public SIRequest retrievePCRPid(ServiceDetails serviceDetails, String language, SIRequestor requestor);
    
    /**
     * Get the PCR Pid associated with the ServiceDetails.
     * 
     * @param handle
     *            The handle to the ServiceDetails of interest
     * @return The PCRPidElement or null if it is not in the cache
     */    
    public PCRPidElement getCachedPCRPid(ServiceDetailsHandle handle);
    
    /**
     * Put the specified PCR Pid in the cache.
     * 
     * @param handle
     *            The handle to the service details to put in the cache
     * @param pcrPid
     *            The PCR Pid to put in the cache
     */
    public void putCachedPCRPid(ServiceDetailsHandle handle, PCRPidElement pcrPid);

    
    /**
     * Remove the PCR Pid for the given ServiceDetails from SI cache.
     * 
     * @param handle
     *            The handle to the service details 
     */
    public void flushCachedPCRPid(ServiceDetailsHandle handle);
    
    /**
     * Retrieves the TSID associated with the given
     * transport stream.
     * 
     * @param transportStream
     *            The transport stream where the search is to start
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @see SIManager#setPreferredLanguage(String)
     */
    public SIRequest retrieveTsID(TransportStream transportStream, String language, SIRequestor requestor);
    
    /**
     * Get the tsid associated with the TransportStreamHandle.
     * 
     * @param handle
     *            The handle to the TransportStreamHandle of interest
     * @return The TsIDElement or null if it is not in the cache
     */    
    public TsIDElement getCachedTsID(TransportStreamHandle handle);
    
    /**
     * Put the specified PCR Pid in the cache.
     * 
     * @param handle
     *            The handle to the service details to put in the cache
     * @param serviceComponent
     *            The PCR Pid to put in the cache
     */
    public void putCachedTsID(TransportStreamHandle handle, TsIDElement tsid);
    
    /**
     * Remove the TsID for the given transport stream handle from SI cache.
     * 
     * @param handle
     *            The handle to the transport stream 
     */
    public void flushCachedTsID(TransportStreamHandle handle);
    
    /**
     * Return an iterator to iterate over all cached ServiceComponent objects.
     */
    public Iterator getServiceComponentCollection();

    /**
     * Retrieves the <code>SIElement</code> corresponding to the specified
     * <code>Locator</code>. If the locator identifies more than one
     * <code>SIElement</code>, all matching <code>SIElements</code> are
     * retrieved.
     * <p>
     * If the locator is a <code>NetworkLocator</code>, the corresponding
     * <code>Network</code> object is returned.
     * <p>
     * If the locator is an <code>OcapLocator</code> which identifies a
     * transport stream (but no service), the corresponding
     * <code>TransportStream</code> object is returned.
     * <p>
     * If the locator is an <code>OcapLocator</code> which identifies a service
     * (but no service component), a <code>ServiceDetails</code> object is
     * returned for each transport that carries the service.
     * <p>
     * If the locator is an <code>OcapLocator</code> which identifies one or
     * more service components, a <code>ServiceComponent</code> object is
     * returned for each service component identified.
     * <p>
     * This method delivers its results asynchronously.
     * 
     * @param locator
     *            A locator referencing one or more SIElements.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid
     *             <code>SIElement</code>.
     * @throws NullPointerException
     *             The <code>SIRequestor</code> object is null.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>
     * @see SIManager#retrieveSIElement
     * @see SIManager#setPreferredLanguage(String)
     */
    public SIRequest retrieveSIElement(Locator locator, String language, SIRequestor requestor)
            throws InvalidLocatorException;

    /**
     * Registers interest in <code>SIElement</code>s corresponding to the
     * specified <code>Locator</code>. If the locator identifies more than one
     * <code>SIElement</code>, interest is registered for all matching
     * <code>SIElement</code>s.
     * <p>
     * If the locator is a <code>NetworkLocator</code>, interest is registered
     * for each <code>SIElement</code> that is part of the network. This
     * includes all <code>ServiceDetails</code>, <code>ServiceDescription</code>
     * and <code>ServiceComponent</code> objects carried by the network.
     * <p>
     * If the locator is an <code>OcapLocator</code> and identifies a transport
     * stream (but no service), interest is registered for each
     * <code>SIElement</code> that is part of the transport stream. This
     * includes all <code>ServiceDetails</code>, <code>ServiceDescription</code>
     * and <code>ServiceComponent</code> objects carried by the transport
     * stream.
     * <p>
     * If the locator is an <code>OcapLocator</code> and identifies a service
     * (but no service components), interest is registered for each
     * <code>SIElement</code> that is part of the service. This includes the
     * <code>Service</code> object and the <code>ServiceDetails</code> for each
     * transport where the service is carried. It also includes the
     * <code>ServiceDescription</code> and all <code>ServiceComponent</code>s
     * for each <code>ServiceDetails</code> identified.
     * <p>
     * If the locator is an <code>OcapLocator</code> and identifies one or more
     * service components, interest is registered for each
     * <code>ServiceComponent</code> identified.
     * <p>
     * If a tuner is available, an attempt is made to tune to the service
     * specified by <code>locator</code>. If an unused tuner cannot be found
     * then no tune is performed.
     * <p>
     * Note that this method returns immediately and that there is no indication
     * of the completion of any resulting caching operations. Since it is only a
     * hint for cache optimization, no specific behavior for this method is
     * guaranteed.
     * 
     * @param locator
     *            A locator referencing the <code>SIElement</code>s for which
     *            complete information is desired.
     * @param active
     *            A flag indicating whether this interest is active or not. A
     *            value of <code>true</code> means that the application is
     *            interested in the <code>SIElement</code>s; <code>false</code>
     *            means that the application wants to cancel a previously shown
     *            interest for the <code>SIElement</code>s.
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid
     *             <code>SIElement</code>.
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)<code>
     * @see SIManager#registerInterest
     */
    public void registerInterest(Locator locator, boolean active) throws InvalidLocatorException;

    /**
     * Registers a <code>NetworkChangeListener</code> to be notified of changes
     * to any <code>Network</code>. Subsequent notification is made via
     * <code>NetworkChangeEvent</code> with the <code>Network</code> as the
     * event source and an <code>SIChangeType</code> of <code>ADD</code>,
     * <code>REMOVE</code> or <code>MODIFY</code>. Only changes to
     * <code>Network</code> instances <code>n</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(n.getLocator())</code> will be
     * reported.
     * <p>
     * This method is only a request for notification. No guarantee is provided
     * that the SI database will detect all, or even any, SI changes or whether
     * such changes will be detected in a timely fashion.
     * 
     * If the specified <code>NetworkChangeListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            A <code>NetworkChangeListener</code> to be notified about
     *            changes related to a <code>Network</code>.
     * @see NetworkCollection#addNetworkChangeListener
     */
    public void addNetworkChangeListener(NetworkChangeListener listener);

    /**
     * Called to unregister a <code>NetworkChangeListener</code>. If the
     * specified <code>NetworkChangeListener</code> is not registered, no action
     * is performed.
     * 
     * @param listener
     *            A previously registered listener.
     * @see NetworkCollection#removeNetworkChangeListener
     */
    public void removeNetworkChangeListener(NetworkChangeListener listener);

    /**
     * Registers a <code>TransportStreamChangeListener</code> to be notified of
     * changes to any <code>TransportStream</code>. Subsequent notification is
     * made via <code>TransportStreamChangeEvent</code> with the
     * <code>TransportStream</code> as the event source and an
     * <code>SIChangeType</code> of <code>ADD</code>, <code>REMOVE</code> or
     * <code>MODIFY</code>. Only changes to <code>TransportStream</code>
     * instances <code>ts</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(ts.getLocator())</code> will be
     * reported.
     * <p>
     * This method is only a request for notification. No guarantee is provided
     * that the SI database will detect all, or even any, SI changes or whether
     * such changes will be detected in a timely fashion.
     * 
     * If the specified <code>TransportStreamChangeListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            A <code>TransportStreamChangeListener</code> to be notified
     *            about changes related to a <code>TransportStream</code>.
     * @see TransportStreamCollection#addTransportStreamChangeListener
     */
    public void addTransportStreamChangeListener(TransportStreamChangeListener listener);

    /**
     * Called to unregister a <code>TransportStreamChangeListener</code>. If the
     * specified <code>TransportStreamChangeListener</code> is not registered,
     * no action is performed.
     * 
     * @param listener
     *            A previously registered listener.
     * @see TransportStreamCollection#removeTransportStreamChangeListener
     */
    public void removeTransportStreamChangeListener(TransportStreamChangeListener listener);

    /**
     * Registers a <code>ServiceDetailsChangeListener</code> to be notified of
     * changes to any <code>ServiceDetails</code>. Subsequent notification is
     * made via <code>ServiceDetailsChangeEvent</code> with the
     * <code>ServiceDetails</code> as the event source and an
     * <code>SIChangeType</code> of <code>ADD</code>, <code>REMOVE</code> or
     * <code>MODIFY</code>. Only changes to <code>ServiceDetails</code>
     * <code>sd</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(sd.getLocator())</code> will be
     * reported.
     * <p>
     * This method is only a request for notification. No guarantee is provided
     * that the SI database will detect all, or even any, SI changes or whether
     * such changes will be detected in a timely fashion.
     * 
     * If the specified <code>ServiceDetailsChangeListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            A <code>ServiceDetailsChangeListener</code> to be notified
     *            about changes related to a <code>ServiceDetails</code>.
     * @see Transport#addServiceDetailsChangeListener
     */
    public void addServiceDetailsChangeListener(ServiceDetailsChangeListener listener);

    /**
     * Called to unregister a <code>ServiceDetailsChangeListener</code>. If the
     * specified <code>ServiceDetailsChangeListener</code> is not registered, no
     * action is performed.
     * 
     * @param listener
     *            A previously registered listener.
     * @see Transport#removeServiceDetailsChangeListener
     */
    public void removeServiceDetailsChangeListener(ServiceDetailsChangeListener listener);

    /**
     * Registers a <code>ServiceComponentChangeListener</code> to be notified of
     * changes to any <code>ServiceComponent</code>. Subsequent notification is
     * made via <code>ServiceComponentChangeEvent</code> with the
     * <code>ServiceComponent</code> as the event source and an
     * <code>SIChangeType</code> of <code>ADD</code>, <code>REMOVE</code> or
     * <code>MODIFY</code>. Only changes to <code>ServiceComponent</code>
     * instances <code>c</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(c.getLocator())</code> will be
     * reported.
     * <p>
     * This method is only a request for notification. No guarantee is provided
     * that the SI database will detect all, or even any, SI changes or whether
     * such changes will be detected in a timely fashion.
     * 
     * If the specified <code>ServiceComponentChangeListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            A <code>ServiceComponentChangeListener</code> to be notified
     *            about changes related to a <code>ServiceComponent</code>.
     * @see ServiceDetails#addServiceComponentChangeListener
     */
    public void addServiceComponentChangeListener(ServiceComponentChangeListener listener);

    /**
     * Called to unregister a <code>ServiceComponentChangeListener</code>. If
     * the specified <code>ServiceComponentChangeListener</code> is not
     * registered, no action is performed.
     * 
     * @param listener
     *            A previously registered listener.
     * @see ServiceDetails#removeServiceComponentChangeListener
     */
    public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener);

    /**
     * Registers a <code>TableChangeListener</code> to be notified of changes to
     * any <code>ProgramAssociationTable</code>. Subsequent notification is made
     * via <code>SIChangeEvent</code> with the <code>SICache</code> as the event
     * source and an <code>SIChangeType</code> of <code>ADD</code>,
     * <code>REMOVE</code> or <code>MODIFY</code>.
     * <p>
     * 
     * If the specified <code>TableChangeListener</code> is already registered,
     * no action is performed.
     * 
     * @param listener
     *            A <code>TableChangeListener</code> to be notified about
     *            changes related to a <code>ProgramAssociationTable</code>.
     */
    public void addPATChangeListener(TableChangeListener listener);

    /**
     * Called to unregister a <code>TableChangeListener</code>. If the specified
     * <code>TableChangeListener</code> is not registered, no action is
     * performed.
     * 
     * @param listener
     *            A previously registered listener.
     */
    public void removePATChangeListener(TableChangeListener listener);

    /**
     * Registers a <code>TableChangeListener</code> to be notified of changes to
     * any <code>ProgramMapTable</code>. Subsequent notification is made via
     * <code>SIChangeEvent</code> with the <code>SICache</code> as the event
     * source and an <code>SIChangeType</code> of <code>ADD</code>,
     * <code>REMOVE</code> or <code>MODIFY</code>.
     * <p>
     * 
     * If the specified <code>TableChangeListener</code> is already registered,
     * no action is performed.
     * 
     * @param listener
     *            A <code>TableChangeListener</code> to be notified about
     *            changes related to a <code>ProgramMapTable</code>.
     */
    public void addPMTChangeListener(TableChangeListener listener);

    /**
     * Called to unregister a <code>TableChangeListener</code>. If the specified
     * <code>TableChangeListener</code> is not registered, no action is
     * performed.
     * 
     * @param listener
     *            A previously registered listener.
     */
    public void removePMTChangeListener(TableChangeListener listener);

    /**
     * Transition this cache into the destroyed state. All periodic operations
     * are terminated. All outstanding requests are failed and all cached SI
     * objects are disposed. Any subsequent calls to any method defined by this
     * interface shall throw {@link IllegalStateException}.
     */
    public void destroy();
    
    /**
     * This indicates the number of milliseconds after which an asynchronous request will be
     * forced to fail. The default value of 15 seconds (15000) can be overridden
     * by setting the system property named
     * <code>OCAP.sicache.asyncTimeout</code>.
     * 
     * @return The SI request timeout
     */ 
    public int getRequestAsyncTimeout();
}
