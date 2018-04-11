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

import javax.tv.service.SIManager;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.transport.NetworkChangeListener;
import javax.tv.service.transport.ServiceDetailsChangeListener;
import javax.tv.service.transport.TransportStreamChangeListener;

import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.TableChangeListener;

import org.cablelabs.impl.manager.CallerContext;

/**
 * The SI database provide synchronous access to the native SI for the platform.
 * Handles are used to identify SI objects (e.g. transport streams, services
 * etc.) at the native level so that entire objects need not be transfered to
 * the Java layer unless actually required.
 * <p>
 * Handles for a given SI type are unique system-wide. For example, handle
 * values used for services may overlap with those used for service components.
 * However, handles for service components must be unique across all services on
 * all transports.
 * 
 * @author Todd Earles
 */
public interface SIDatabase
{
    /**
     * Set the <code>SICache</code> instance to be associated with this instance
     * of the <code>SIDatabase</code>. This instance of <code>SIDatabase</code>
     * should not be considered to be fully initialized until this method is
     * called. Attempts to access the <code>SICache</code> before this method is
     * called should throw <code>IllegalStateException</code>.
     * 
     * @param siCache
     *            The SICache to associate with this SIDatabase.
     * @throws IllegalStateException
     *             If the SICache has already been set
     */
    public void setSICache(SICache siCache);

    /**
     * Get the <code>SICache</code> instance associated with this instance of
     * the <code>SIDatabase</code>.
     * 
     * @return The SICache associated with this SIDatabase.
     * @throws IllegalStateException
     *             If the SICache has not been set
     */
    public SICache getSICache();

    /**
     * Provides a list of handles of available rating dimensions in the local
     * rating region. A zero-length array is returned if no rating dimensions
     * are available.
     * 
     * @return An array of handles representing the available rating dimensions
     *         in this rating region.
     * @throws SINotAvailableException
     *             If the list of rating dimensions is not currently available.
     * @throws SINotAvailableYetException
     *             If the list of rating dimensions will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getSupportedDimensions
     */
    public RatingDimensionHandle[] getSupportedDimensions() throws SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the handle to the rating dimension with the given name
     * 
     * @param name
     *            The name for the rating dimension
     * @return The handle to the rating dimension
     * @throws SIRequestInvalidException
     *             If <code>name</code> is not a valid rating dimension name
     * @throws SINotAvailableException
     *             If the rating dimension is not currently available.
     * @throws SINotAvailableYetException
     *             If the rating dimension will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getRatingDimension
     */
    public RatingDimensionHandle getRatingDimensionByName(String name) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException;

    /**
     * Creates the <code>RatingDimension</code> corresponding to the specified
     * rating dimension handle.
     * 
     * @param ratingDimensionHandle
     *            The handle to the rating dimension
     * @return The requested <code>RatingDimensionExt</code>.
     * @throws SIRequestInvalidException
     *             If <code>ratingDimensionHandle</code> is not a valid rating
     *             dimension handle.
     * @throws SINotAvailableException
     *             If the requested rating dimension is not currently available.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getRatingDimension
     */
    public RatingDimensionExt createRatingDimension(RatingDimensionHandle ratingDimensionHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException;

    /**
     * Provides a list of handles to all transports available on the platform. A
     * zero-length array is returned if no transports are available.
     * 
     * @return An array of transport handles
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getTransports
     */
    public TransportHandle[] getAllTransports() throws SILookupFailedException;

    /**
     * Provides the handle to the transport with the given transport ID.
     * 
     * @param transportID
     *            The transport ID
     * @return The handle to the transport
     * @throws SIRequestInvalidException
     *             If the <code>transportID</code> does not identify a
     *             transport.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SIManager#retrieveSIElement
     */
    public TransportHandle getTransportByID(int transportID) throws SIRequestInvalidException, SILookupFailedException;

    /**
     * Creates the <code>Transport</code> corresponding to the specified
     * transport handle.
     * 
     * @param transportHandle
     *            The handle to the transport
     * @return The requested <code>TransportExt</code>.
     * @throws SIRequestInvalidException
     *             If <code>transportHandle</code> is not a valid transport
     *             handle.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getTransports
     */
    public TransportExt createTransport(TransportHandle transportHandle) throws SIRequestInvalidException,
            SILookupFailedException;

    /**
     * Provides a list of handles to all networks containing the specified
     * transport. A zero-length array is returned if no networks are available.
     * 
     * @param transportHandle
     *            The handle to the transport
     * @return An array of network handles
     * @throws SIRequestInvalidException
     *             If <code>transportHandle</code> is not a valid transport
     *             handle.
     * @throws SINotAvailableException
     *             If the list of networks is not currently available.
     * @throws SINotAvailableYetException
     *             If the list of networks will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveNetworks
     */
    public NetworkHandle[] getNetworksByTransport(TransportHandle transportHandle) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException;

    /**
     * Provides the handle to the network with the given network ID.
     * 
     * @param transportHandle
     *            The handle to the transport
     * @param networkID
     *            The network ID
     * @return The handle to the network
     * @throws SIRequestInvalidException
     *             If <code>transportHandle</code> is not a valid transport
     *             handle or the <code>networkID</code> does not identify a
     *             network which carries the specified transport.
     * @throws SINotAvailableException
     *             If the network is not currently available.
     * @throws SINotAvailableYetException
     *             If the network will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SIManager#retrieveSIElement
     */
    public NetworkHandle getNetworkByID(TransportHandle transportHandle, int networkID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Creates the <code>Network</code> corresponding to the specified network
     * handle.
     * 
     * @param networkHandle
     *            The handle to the network
     * @return The requested <code>NetworkExt</code>.
     * @throws SIRequestInvalidException
     *             If <code>networkHandle</code> is not a valid network handle.
     * @throws SINotAvailableException
     *             If the requested network is not currently available.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveNetworks
     */
    public NetworkExt createNetwork(NetworkHandle networkHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException;

    /**
     * Provides a list of handles to all transport streams carried by the
     * specified transport. A zero-length array is returned if no transport
     * streams are available.
     * 
     * @param transportHandle
     *            The handle to the transport
     * @return An array of transport stream handles
     * @throws SIRequestInvalidException
     *             If <code>transportHandle</code> is not a valid transport
     *             handle.
     * @throws SINotAvailableException
     *             If the list of transport streams is not currently available.
     * @throws SINotAvailableYetException
     *             If the list of transport stream will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveTransportStreams
     */
    public TransportStreamHandle[] getTransportStreamsByTransport(TransportHandle transportHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides a list of handles to all transport streams carried by the
     * specified network. A zero-length array is returned if no transport
     * streams are available.
     * 
     * @param networkHandle
     *            The handle to the network
     * @return An array of transport stream handles
     * @throws SIRequestInvalidException
     *             If <code>networkHandle</code> is not a valid network handle.
     * @throws SINotAvailableException
     *             If the list of transport streams is not currently available.
     * @throws SINotAvailableYetException
     *             If the list of transport streams will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveTransportStreams
     */
    public TransportStreamHandle[] getTransportStreamsByNetwork(NetworkHandle networkHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the handle to the transport stream with the given frequency and
     * transport stream ID.
     * 
     * @param transportHandle
     *            The handle to the transport
     * @param frequency
     *            The frequency that carries the transport stream. For the
     *            out-of-band transport stream, use -1.
     * @param modulationFormat
     *            The modulation mode of the transport stream.
     * @param tsID
     *            The transport stream ID. If the tsID is not known, use -1 to
     *            retrieve the handle to the first transport stream found with
     *            the given frequency.
     * @return The handle to the transport stream
     * @throws SIRequestInvalidException
     *             If <code>transportHandle</code> is not a valid transport
     *             handle or the <code>frequency</code> and <code>tsID</code> do
     *             not identify a transport stream on the specified transport.
     * @throws SINotAvailableException
     *             If the transport stream is not currently available.
     * @throws SINotAvailableYetException
     *             If the transport stream will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SIManager#retrieveSIElement
     */
    public TransportStreamHandle getTransportStreamByID(TransportHandle transportHandle, int frequency,
            int modulationFormat, int tsID) throws SIRequestInvalidException, SINotAvailableException,
            SINotAvailableYetException, SILookupFailedException;

    /**
     * Provides the transport stream associated with the service specified by
     * the given SourceID
     * 
     * @param sourceID
     *            The sourceID of the service
     * @return The handle to the transport stream
     * @throws SIRequestInvalidException
     *             If <code>sourceID</code> does not represent a valid service
     * @throws SINotAvailableException
     *             If the transport stream is not currently available.
     * @throws SINotAvailableYetException
     *             If the transport stream will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     */
    public TransportStreamHandle getTransportStreamBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException;

    /**
     * Provides the transport stream associated with the service specified by
     * the given frequency and program number
     * 
     * @param frequency
     *            The frequency of the service
     * @param modulationFormat
     *            The modulation format used to carry the service (-1 if
     *            unspecified)
     * @param programNumber
     *            The program number of the service
     * @return The handle to the transport stream
     * @throws SIRequestInvalidException
     *             If <code>sourceID</code> does not represent a valid service
     * @throws SINotAvailableException
     *             If the transport stream is not currently available.
     * @throws SINotAvailableYetException
     *             If the transport stream will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     */
    public TransportStreamHandle getTransportStreamByProgramNumber(int frequency, int modulationFormat,
            int programNumber) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Creates the <code>TransportStream</code> corresponding to the specified
     * transport stream handle.
     * 
     * @param transportStreamHandle
     *            The handle to the transport stream
     * @return The requested <code>TransportStreamExt</code>.
     * @throws SIRequestInvalidException
     *             If <code>transportStreamHandle</code> is not a valid
     *             transport stream handle.
     * @throws SINotAvailableException
     *             If the requested transport stream is not currently available.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveTransportStreams
     */
    public TransportStreamExt createTransportStream(TransportStreamHandle transportStreamHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException;

    /**
     * Provides a list of handles to all services available on the platform. A
     * zero-length array is returned if no services are available.
     * 
     * @return An array of service handles
     * @throws SINotAvailableException
     *             If the list of services is not currently available.
     * @throws SINotAvailableYetException
     *             If the list of services will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getAllServices
     */
    public ServiceHandle[] getAllServices() throws SILookupFailedException, SINotAvailableException,
            SINotAvailableYetException;

    /**
     * Provides the handle to the service with the given source ID
     * 
     * @param sourceID
     *            The source ID for the service
     * @return The handle to the service
     * @throws SIRequestInvalidException
     *             If <code>sourceID</code> is not a valid source ID for a
     *             service
     * @throws SINotAvailableException
     *             If the service is not currently available.
     * @throws SINotAvailableYetException
     *             If the service will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getService
     */
    public ServiceHandle getServiceBySourceID(int sourceID) throws SIRequestInvalidException, SINotAvailableException,
            SINotAvailableYetException, SILookupFailedException;
    
    /**
     * Provides the handle to the service with the given service name
     * 
     * @param serviceName
     *            The service name for the service
     * @return The handle to the service
     * @throws SIRequestInvalidException
     *             If <code>serviceName</code> is not a valid service name for a
     *             service
     * @throws SINotAvailableException
     *             If the service is not currently available.
     * @throws SINotAvailableYetException
     *             If the service will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getService
     */
    public ServiceHandle getServiceByServiceName(String serviceName) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException;

    /**
     * Provides the handle to the service with the given service number and
     * service minor number.
     * 
     * @param serviceNumber
     *            The service number for the service
     * @param minorNumber
     *            The service minor number for the service. A value of -1
     *            indicates the one-part service number is defined by
     *            <code>serviceNumber</code>.
     * @return The handle to the service
     * @throws SIRequestInvalidException
     *             A service could not be found with the specified service
     *             number and minor number.
     * @throws SINotAvailableException
     *             If the service is not currently available.
     * @throws SINotAvailableYetException
     *             If the service will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     */
    public ServiceHandle getServiceByServiceNumber(int serviceNumber, int minorNumber)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the handle to the service with the given app Id.
     * 
     * @param appId
     *            The appId for the service (DSG only)
     * @return The handle to the service
     * @throws SIRequestInvalidException
     *             A service could not be found with the specified service
     *             number and minor number.
     * @throws SINotAvailableException
     *             If the service is not currently available.
     * @throws SINotAvailableYetException
     *             If the service will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     */
    public ServiceHandle getServiceByAppId(int appId) throws SIRequestInvalidException, SINotAvailableException,
            SINotAvailableYetException, SILookupFailedException;

    /**
     * Provides the handle to the service with the given program number
     * 
     * @param frequency
     *            The frequency that carries the service. Specify -1 for the
     *            out-of-band channel
     * @param modulationFormat
     *            The modulation format used to carry the service (-1 if
     *            unspecified)
     * @param programNumber
     *            The program number of the service
     * @return The handle to the service
     * @throws SIRequestInvalidException
     *             No service found with the specified <code>frequency</code>,
     *             <code>programNumber</code> and <code>modultionFormat</code>.
     *             service
     * @throws SINotAvailableException
     *             If the service is not currently available.
     * @throws SINotAvailableYetException
     *             If the service will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getService
     */
    public ServiceHandle getServiceByProgramNumber(int frequency, int modulationFormat, int programNumber)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Creates the <code>Service</code> corresponding to the specified service
     * handle.
     * 
     * @param serviceHandle
     *            The handle to the service
     * @return The requested <code>ServiceExt</code>.
     * @throws SIRequestInvalidException
     *             If <code>serviceHandle</code> is not a valid service handle.
     * @throws SINotAvailableException
     *             If the requested service is not currently available.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getService
     */
    public ServiceExt createService(ServiceHandle serviceHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException;

    /**
     * Provides a list of handles to all service details which represent the
     * specified service. A zero-length array is returned if no service details
     * are available.
     * 
     * @param serviceHandle
     *            The handle to the service
     * @return An array of service details handles
     * @throws SIRequestInvalidException
     *             If <code>serviceHandle</code> is not a valid service handle.
     * @throws SINotAvailableException
     *             If the list of service details is not currently available.
     * @throws SINotAvailableYetException
     *             If the list of service details will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveServiceDetails
     */
    public ServiceDetailsHandle[] getServiceDetailsByService(ServiceHandle serviceHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides a list of service details handles to all service details which represent the
     * specified sourceId. A zero-length array is returned if no service details
     * are available.
     * 
     * @param sourceId
     *            The sourceId of the service
     * @return An array of service details handles
     * @throws SIRequestInvalidException
     *             If <code>serviceHandle</code> is not a valid service handle.
     * @throws SINotAvailableException
     *             If the list of service details is not currently available.
     * @throws SINotAvailableYetException
     *             If the list of service details will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveServiceDetails
     */
    public ServiceDetailsHandle[] getServiceDetailsBySourceID(int sourceId)
    throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
    SILookupFailedException;

    /**
     * Creates the <code>ServiceDetails</code> corresponding to the specified
     * service details handle.
     * 
     * @param serviceDetailsHandle
     *            The handle to the service details
     * @return The requested <code>ServiceDetailsExt</code>.
     * @throws SIRequestInvalidException
     *             If <code>serviceDetailsHandle</code> is not a valid service
     *             details handle.
     * @throws SINotAvailableException
     *             If the requested service details is not currently available.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveServiceDetails
     */
    public ServiceDetailsExt createServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException;

    /**
     * Creates the <code>ServiceDescription</code> corresponding to the
     * specified service details handle.
     * 
     * @param serviceDetailsHandle
     *            The handle to the service details
     * @return The requested <code>ServiceDescriptionExt</code>.
     * @throws SIRequestInvalidException
     *             If <code>serviceDetailsHandle</code> is not a valid service
     *             details handle.
     * @throws SINotAvailableException
     *             If the requested service description is not currently
     *             available.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveServiceDescription
     */
    public ServiceDescriptionExt createServiceDescription(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException;

    /**
     * Provides the PAT handle associated with the specified transport stream
     * frequency and transport stream ID
     * 
     * @param frequency
     *            The frequency that carries the transport stream. For the
     *            out-of-band transport stream, use -1.
     * @param modulationFormat
     *            The modulation format used to carry the service (-1 if
     *            unspecified)
     * @param tsID
     *            The transport stream ID. If the tsID is not known, use -1 to
     *            retrieve the handle to the first transport stream found with
     *            the given frequency
     * @return The handle to the PAT
     * @throws SIRequestInvalidException
     *             If <code>transportHandle</code> is not a valid transport
     *             handle or the <code>frequency</code> and <code>tsID</code> do
     *             not identify a transport stream on the specified transport.
     * @throws SINotAvailableException
     *             If the PAT is not currently available.
     * @throws SINotAvailableYetException
     *             If the PAT will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     */
    public ProgramAssociationTableHandle getProgramAssociationTableByID(int frequency, int modulationFormat, int tsID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the PAT handle associated with the service specified by the
     * given SourceID
     * 
     * @param sourceID
     *            The sourceID of the service
     * @return The handle to the PAT
     * @throws SIRequestInvalidException
     *             If <code>sourceID</code> does not represent a valid service
     * @throws SINotAvailableException
     *             If the PAT is not currently available.
     * @throws SINotAvailableYetException
     *             If the PAT will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     */
    public ProgramAssociationTableHandle getProgramAssociationTableBySourceID(int sourceID)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the PMT handle associated with the given service information
     * (frequency, modulation, program number)
     * 
     * @param frequency
     *            The frequency that carries the service. Specify -1 for the
     *            out-of-band channel
     * @param programNumber
     *            The program number of the service
     * @param modulationFormat
     *            The modulation format used to carry the service (-1 if
     *            unspecified)
     * @return The handle to the PMT
     * @throws SIRequestInvalidException
     *             No service found with the specified <code>frequency</code>,
     *             <code>programNumber</code> and <code>modultionFormat</code>.
     *             service
     * @throws SINotAvailableException
     *             If the PMT is not currently available.
     * @throws SINotAvailableYetException
     *             If the PMT will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getService
     */
    public ProgramMapTableHandle getProgramMapTableByProgramNumber(int frequency, int modulationFormat,
            int programNumber) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the PMT handle associated with the given service information
     * (source ID)
     * 
     * @param sourceID
     *            The source ID for the service
     * @return The handle to the PMT
     * @throws SIRequestInvalidException
     *             If <code>sourceID</code> is not a valid source ID for a
     *             service
     * @throws SINotAvailableException
     *             If the PMT is not currently available.
     * @throws SINotAvailableYetException
     *             If the PMT will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#getService
     */
    public ProgramMapTableHandle getProgramMapTableBySourceID(int sourceID) throws SIRequestInvalidException,
            SINotAvailableException, SINotAvailableYetException, SILookupFailedException;

    public ProgramMapTableHandle getProgramMapTableByService(int serviceHandle) throws SIRequestInvalidException,
    SINotAvailableException, SINotAvailableYetException, SILookupFailedException;

    /**
     * Creates the <code>ProgramAssociationTable</code> corresponding to the
     * specified PAT handle
     * 
     * @param patHandle
     *            The handle to the PAT
     * @return The requested <code>ProgramAssociationTable</code>
     * @throws SIRequestInvalidException
     *             If <code>transportStreamHandle</code> is not a valid
     *             transport stream handle.
     * @throws SINotAvailableException
     *             If the requested PAT is not currently available.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveProgramAssociationTable
     */
    public ProgramAssociationTable createProgramAssociationTable(ProgramAssociationTableHandle patHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException;

    /**
     * Creates the <code>ProgramMapTable</code> corresponding to the specified
     * PMT handle
     * 
     * @param pmtHandle
     *            The handle to the PMT
     * @return The requested <code>ProgramMapTable</code>
     * @throws SIRequestInvalidException
     *             If <code>serviceHandle</code> is not a valid service handle.
     * @throws SINotAvailableException
     *             If the requested PMT is not currently available.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveProgramMapTable
     */
    public ProgramMapTable createProgramMapTable(ProgramMapTableHandle pmtHandle) throws SIRequestInvalidException,
            SINotAvailableException, SILookupFailedException;

    /**
     * Provides a list of handles to all service components carried by a
     * transport-specific service. A zero-length array is returned if no service
     * components are available.
     * 
     * @param serviceDetailsHandle
     *            The handle to the service details
     * @return An array of service component handles
     * @throws SIRequestInvalidException
     *             If <code>serviceDetailsHandle</code> is not a valid service
     *             details handle.
     * @throws SINotAvailableException
     *             The service may have components but the list of those service
     *             components is not currently available (e.g. because the
     *             service is not currently tuned).
     * @throws SINotAvailableYetException
     *             If the list of service components will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveServiceComponents
     */
    public ServiceComponentHandle[] getServiceComponentsByServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the handle to the service component with the given component
     * PID.
     * 
     * @param serviceDetailsHandle
     *            The handle to the service details
     * @param pid
     *            The service component PID
     * @return The handle to the service component
     * @throws SIRequestInvalidException
     *             If <code>serviceDetailsHandle</code> is not a valid service
     *             details handle or the <code>pid</code> does not identify a
     *             service component within the service.
     * @throws SINotAvailableException
     *             If the list of service components is not currently available.
     * @throws SINotAvailableYetException
     *             If the service component will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveServiceComponents
     */
    public ServiceComponentHandle getServiceComponentByPID(ServiceDetailsHandle serviceDetailsHandle, int pid)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the handle to the service component with the given component
     * tag.
     * 
     * @param serviceDetailsHandle
     *            The handle to the service details
     * @param tag
     *            The service component tag
     * @return The handle to the service component
     * @throws SIRequestInvalidException
     *             If <code>serviceDetailsHandle</code> is not a valid service
     *             details handle or the <code>tag</code> does not identify a
     *             service component within the service.
     * @throws SINotAvailableException
     *             If the service component is not currently available.
     * @throws SINotAvailableYetException
     *             If the service component will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveServiceComponents
     */
    public ServiceComponentHandle getServiceComponentByTag(ServiceDetailsHandle serviceDetailsHandle, int tag)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the handle to the service component with the given component
     * name.
     * 
     * @param serviceDetailsHandle
     *            The handle to the service details
     * @param name
     *            The service component name
     * @return The handle to the service component
     * @throws SIRequestInvalidException
     *             If <code>serviceDetailsHandle</code> is not a valid service
     *             details handle or the <code>name</code> does not identify a
     *             service component within the service.
     * @throws SINotAvailableException
     *             If the service component is not currently available.
     * @throws SINotAvailableYetException
     *             If the service component will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveServiceComponents
     */
    public ServiceComponentHandle getServiceComponentByName(ServiceDetailsHandle serviceDetailsHandle, String name)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the handle to the service component which carries the default
     * object carousel for the specified service.
     * 
     * @param serviceDetailsHandle
     *            The handle to the service details
     * @return The handle to the service component which carries the carousel.
     * @throws SIRequestInvalidException
     *             If <code>serviceDetailsHandle</code> is not a valid service
     *             details handle or there is no default carousel for this
     *             service.
     * @throws SINotAvailableException
     *             If the service component is not currently available.
     * @throws SINotAvailableYetException
     *             If the service component will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveCarouselComponent
     */
    public ServiceComponentHandle getCarouselComponentByServiceDetails(ServiceDetailsHandle serviceDetailsHandle)
            throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the handle to the service component which carries the object
     * carousel with the specified carousel ID for the specified service.
     * 
     * @param serviceDetailsHandle
     *            The handle to the service details
     * @param carouselID
     *            The carousel ID of the object carousel
     * @return The handle to the service component which carries the carousel.
     * @throws SIRequestInvalidException
     *             If <code>serviceDetailsHandle</code> is not a valid service
     *             details handle or the <code>carouselID</code> does not
     *             identify a carousel carried by this service.
     * @throws SINotAvailableException
     *             If the service component is not currently available.
     * @throws SINotAvailableYetException
     *             If the service component will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveCarouselComponent
     */
    public ServiceComponentHandle getCarouselComponentByServiceDetails(ServiceDetailsHandle serviceDetailsHandle,
            int carouselID) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Provides the handle to the service component specified by the given
     * association tag.
     * 
     * @param serviceDetailsHandle
     *            The handle to the service details where the search is to start
     * @param associationTag
     *            The association tag of the service component
     * @return The handle to the service component with the given association
     *         tag.
     * @throws SIRequestInvalidException
     *             If <code>serviceDetailsHandle</code> is not a valid service
     *             details handle or the <code>associationTag</code> does not
     *             identify a service component.
     * @throws SINotAvailableException
     *             If the service component is not currently available.
     * @throws SINotAvailableYetException
     *             If the service component will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveComponentByAssociationTag
     */
    public ServiceComponentHandle getComponentByAssociationTag(ServiceDetailsHandle serviceDetailsHandle,
            int associationTag) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
            SILookupFailedException;

    /**
     * Retrieves the PCR Pid associated with the given
     * ServiceDetails.
     * 
     * @param serviceDetailsHandle
     *            The handle to the service details where the search is to start
     * @return The PCR Pid
     * @throws SIRequestInvalidException
     *             If <code>serviceDetailsHandle</code> is not a valid service
     *             details handle 
     * @throws SINotAvailableException
     *             If the PCR Pid is not currently available.
     * @throws SINotAvailableYetException
     *             If the PCR Pid will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     *             
     * Note: PCR Pid is found in the Program Map Table. Hence the above SI exceptions
     *       reflect the state of PMT acquisition.
     */
    public int getPCRPidForServiceDetails(ServiceDetailsHandle serviceDetailsHandle) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
    SILookupFailedException;
    
    /**
     * Retrieves the TSID associated with the given
     * transport stream.
     * 
     * @param tsHandle
     *            The handle to the transport stream where the search is to start
     * @return The TsID
     * @throws SIRequestInvalidException
     *             If <code>TransportStreamHandle</code> is not a valid 
     * @throws SINotAvailableException
     *             If the tsid is not currently available.
     * @throws SINotAvailableYetException
     *             If the tsid will be available shortly.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     *             
     * Note: PCR Pid is found in the Program Map Table. Hence the above SI exceptions
     *       reflect the state of PMT acquisition.
     */
    public int getTsIDForTransportStreamHandle(TransportStreamHandle tsHandle) throws SIRequestInvalidException, SINotAvailableException, SINotAvailableYetException,
    SILookupFailedException;
    
    /**
     * Creates the <code>ServiceComponent</code> corresponding to the specified
     * service component handle.
     * 
     * @param serviceComponentHandle
     *            The handle to the service component
     * @return The requested <code>ServiceDescriptionExt</code>.
     * @throws SIRequestInvalidException
     *             If <code>serviceDetailsHandle</code> is not a valid service
     *             details handle.
     * @throws SINotAvailableException
     *             If the requested service description is not currently
     *             available.
     * @throws SILookupFailedException
     *             If the SI lookup could not be performed.
     * @see SICache#retrieveServiceDescription
     */
    public ServiceComponentExt createServiceComponent(ServiceComponentHandle serviceComponentHandle)
            throws SIRequestInvalidException, SINotAvailableException, SILookupFailedException;

    /**
     * Registers a <code>SIChangedListener</code> to be notified of availability
     * of new service information in this SI database. Subsequent notification
     * is made via <code>SIChangedEvent</code> with this <code>SIDatabase</code>
     * as the event source.
     * <p>
     * If the specified <code>SIChangedListener</code> is already registered, no
     * action is performed.
     * 
     * @param listener
     *            A <code>SIChangedListener</code to be notified about newly
     *            acquired SI.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void addSIAcquiredListener(SIChangedListener listener, CallerContext context);

    public void addSIChangedListener(SIChangedListener listener, CallerContext context);

    /**
     * Called to unregister an <code>SIChangedListener</code>. If the specified
     * <code>SIChangedListener</code> is not registered, no action is performed.
     * 
     * @param listener
     *            A previously registered listener.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void removeSIAcquiredListener(SIChangedListener listener, CallerContext context);

    public void removeSIChangedListener(SIChangedListener listener, CallerContext context);

    /**
     * Registers a <code>NetworkChangeListener</code> to be notified of changes
     * to any <code>Network</code>. Subsequent notification is made via
     * <code>NetworkChangeEvent</code> with the <code>Network</code> as the
     * event source and an <code>SIChangeType</code> of <code>ADD</code>,
     * <code>REMOVE</code> or <code>MODIFY</code>. This method is only a request
     * for notification. No guarantee is provided that the SI database will
     * detect all, or even any, SI changes or whether such changes will be
     * detected in a timely fashion.
     * 
     * If the specified <code>NetworkChangeListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            A <code>NetworkChangeListener</code> to be notified about
     *            changes related to a <code>Network</code>.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void addNetworkChangeListener(NetworkChangeListener listener, CallerContext context);

    /**
     * Called to unregister a <code>NetworkChangeListener</code>. If the
     * specified <code>NetworkChangeListener</code> is not registered, no action
     * is performed.
     * 
     * @param listener
     *            A previously registered listener.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void removeNetworkChangeListener(NetworkChangeListener listener, CallerContext context);

    /**
     * Registers a <code>TransportStreamChangeListener</code> to be notified of
     * changes to any <code>TransportStream</code>. Subsequent notification is
     * made via <code>TransportStreamChangeEvent</code> with the
     * <code>TransportStream</code> as the event source and an
     * <code>SIChangeType</code> of <code>ADD</code>, <code>REMOVE</code> or
     * <code>MODIFY</code>. This method is only a request for notification. No
     * guarantee is provided that the SI database will detect all, or even any,
     * SI changes or whether such changes will be detected in a timely fashion.
     * 
     * If the specified <code>TransportStreamChangeListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            A <code>TransportStreamChangeListener</code> to be notified
     *            about changes related to a <code>TransportStream</code>.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void addTransportStreamChangeListener(TransportStreamChangeListener listener, CallerContext context);

    /**
     * Called to unregister a <code>TransportStreamChangeListener</code>. If the
     * specified <code>TransportStreamChangeListener</code> is not registered,
     * no action is performed.
     * 
     * @param listener
     *            A previously registered listener.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void removeTransportStreamChangeListener(TransportStreamChangeListener listener, CallerContext context);

    /**
     * Registers a <code>ServiceDetailsChangeListener</code> to be notified of
     * changes to any <code>ServiceDetails</code>. Subsequent notification is
     * made via <code>ServiceDetailsChangeEvent</code> with the
     * <code>ServiceDetails</code> as the event source and an
     * <code>SIChangeType</code> of <code>ADD</code>, <code>REMOVE</code> or
     * <code>MODIFY</code>. This method is only a request for notification. No
     * guarantee is provided that the SI database will detect all, or even any,
     * SI changes or whether such changes will be detected in a timely fashion.
     * 
     * If the specified <code>ServiceDetailsChangeListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            A <code>ServiceDetailsChangeListener</code> to be notified
     *            about changes related to a <code>ServiceDetails</code>.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void addServiceDetailsChangeListener(ServiceDetailsChangeListener listener, CallerContext context);

    /**
     * Called to unregister a <code>ServiceDetailsChangeListener</code>. If the
     * specified <code>ServiceDetailsChangeListener</code> is not registered, no
     * action is performed.
     * 
     * @param listener
     *            A previously registered listener.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void removeServiceDetailsChangeListener(ServiceDetailsChangeListener listener, CallerContext context);

    /**
     * Registers a <code>ServiceComponentChangeListener</code> to be notified of
     * changes to any <code>ServiceComponent</code>. Subsequent notification is
     * made via <code>ServiceComponentChangeEvent</code> with the
     * <code>ServiceComponent</code> as the event source and an
     * <code>SIChangeType</code> of <code>ADD</code>, <code>REMOVE</code> or
     * <code>MODIFY</code>. This method is only a request for notification. No
     * guarantee is provided that the SI database will detect all, or even any,
     * SI changes or whether such changes will be detected in a timely fashion.
     * 
     * If the specified <code>ServiceComponentChangeListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            A <code>ServiceComponentChangeListener</code> to be notified
     *            about changes related to a <code>ServiceComponent</code>.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void addServiceComponentChangeListener(ServiceComponentChangeListener listener, CallerContext context);

    /**
     * Called to unregister a <code>ServiceComponentChangeListener</code>. If
     * the specified <code>ServiceComponentChangeListener</code> is not
     * registered, no action is performed.
     * 
     * @param listener
     *            A previously registered listener.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener, CallerContext context);

    /**
     * Registers a <code>TableChangeListener</code> to be notified of changes to
     * any <code>ProgramAssociationTable</code>. Subsequent notification is made
     * via <code>SIChangeEvent</code> with the <code>SIDatabase</code> as the
     * event source and an <code>SIChangeType</code> of <code>ADD</code>,
     * <code>REMOVE</code> or <code>MODIFY</code>.
     * 
     * If the specified <code>TableChangeListener</code> is already registered,
     * no action is performed.
     * 
     * @param listener
     *            A <code>TableChangeListener</code> to be notified about
     *            changes related to a <code>ProgramAssociationTable</code>.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void addPATChangeListener(TableChangeListener listener, CallerContext context);

    /**
     * Called to unregister a PAT <code>TableChangeListener</code>. If the
     * specified <code>TableChangeListener</code> is not registered, no action
     * is performed.
     * 
     * @param listener
     *            A previously registered listener.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void removePATChangeListener(TableChangeListener listener, CallerContext context);

    /**
     * Registers a <code>TableChangeListener</code> to be notified of changes to
     * any <code>ProgramMapTable</code>. Subsequent notification is made via
     * <code>SIChangeEvent</code> with the <code>SIDatabase</code> as the event
     * source and an <code>SIChangeType</code> of <code>ADD</code>,
     * <code>REMOVE</code> or <code>MODIFY</code>.
     * 
     * If the specified <code>TableChangeListener</code> is already registered,
     * no action is performed.
     * 
     * @param listener
     *            A <code>TableChangeListener</code> to be notified about
     *            changes related to a <code>ProgramMapTable</code>.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void addPMTChangeListener(TableChangeListener listener, CallerContext context);

    /**
     * Called to unregister a PMT <code>TableChangeListener</code>. If the
     * specified <code>TableChangeListener</code> is not registered, no action
     * is performed.
     * 
     * @param listener
     *            A previously registered listener.
     * @param context
     *            The caller context to use when invoking the listener
     */
    public void removePMTChangeListener(TableChangeListener listener, CallerContext context);

    /**
     * Called to register interest for PSI acquisition when DSG application
     * tunnel is in the process of being accessed.
     * 
     * @param serviceHandle
     *            A handle that uniquely identifies a DSG app tunnel.
     */
    public void registerForPSIAcquisition(ServiceHandle serviceHandle);

    /**
     * Called to unregister interest for PSI acquisition (DSG application tunnel
     * specific)
     * 
     * @param serviceHandle
     *            A handle that uniquely identifies a DSG app tunnel.
     */
    public void unregisterForPSIAcquisition(ServiceHandle serviceHandle);

    /**
     * Called to register interest for PSI acquisition when HN session is in the
     * process of being accessed.
     * 
     * @param session
     *            A handle that uniquely identifies a HN session.
     * @return 
     *            A corresponding service handle for HN session.
     * @throws
     *            Exception if an error occured.
     */
    public ServiceDetailsHandle registerForHNPSIAcquisition(int session) throws SIDatabaseException;

    /**
     * Called to unregister interest for PSI acquisition (HN session specific)
     * 
     * @param session
     *            A handle that uniquely identifies a HN session.
     */
    public void unregisterForHNPSIAcquisition(int session);
    
    /**
     * Indicates if OOB SI has been acquired
     * 
     */    
    public boolean isOOBAcquired();
    
    /**
     * Force wait until OOB SI tables are acquired
     * 
     */ 
    public void waitForOOB();
    
    /**
     * Indicates if SI tables NIT, SVCT have been acquired
     * 
     */    
    public boolean isNITSVCTAcquired();
    
    /**
     * Force wait until OOB SI tables NIT and SVCT are acquired
     * 
     */ 
    public void waitForNITSVCT();
    
    /**
     * Force wait until OOB SI table NTT is acquired
     * 
     */ 
    public void waitForNTT();
}
