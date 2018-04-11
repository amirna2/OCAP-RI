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

package org.cablelabs.impl.davic.net.tuning;

import org.davic.net.Locator;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NotOwnerException;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.manager.CallerContext;

import java.util.Vector;

import javax.tv.service.Service;

/**
 * The purpose of this class is to extend the NetworkInterfaceManager class by
 * adding functionality to reserve and release <code>NetworkInterfaces</code>.
 * This allows the reserving and releasing of <code>NetworkInterfaces</code> to
 * be handled by a single class.
 */
public abstract class ExtendedNetworkInterfaceManager extends NetworkInterfaceManager
{
    /**
     * Reserve a <code>NetworkInterface</code> that can tune to the service
     * referenced by the locator parameter.
     * 
     * @param usage
     *            <code>ResourceUsage</code> object associated with the current
     *            operation
     * @param locator
     *            specifies a service that the <code>NetworkInterface</code>
     *            should be able to tune to.
     * @param proxy
     *            <code>NetworkInterfaceController</code> requesting the
     *            reservation
     * @param client
     *            <code>ResourceClient</code> associated with the
     *            <code>NetworkInterfaceController</code>
     * @param data
     *            object used by the resource notification API
     * @return <code>true</code> if the reservation succeeded <code>false</code>
     *         otherwise
     * @throws NetworkInterfaceException
     */
    public abstract boolean reserveFor(ResourceUsageImpl usage, Locator locator, ResourceProxy proxy,
            ResourceClient client, Object data) throws NetworkInterfaceException;

    /**
     * Returns the <code>SharableNetworkInterfaceManager</code> 
     * responsible for keeping track of sharable NIs
     * 
     * @return the <code>SharableNetworkInterfaceManager</code>
     */
    public abstract SharableNetworkInterfaceManager getSharableNetworkInterfaceManager();

    /**
     * Reserve the specified <code>NetworkInterface</code>.
     * 
     * @param usage
     *            <code>ResourceUsage</code> object associated with the current
     *            operation
     * @param nwif
     *            the <code>NetworkInterface</code> to reserve.
     * @param proxy
     *            <code>NetworkInterfaceController</code> requesting the
     *            reservation
     * @param client
     *            <code>ResourceClient</code> associated with the
     *            <code>NetworkInterfaceController</code>
     * @param data
     *            object used by the resource notification API
     * @return <code>true</code> if the reservation succeeded <code>false</code>
     *         otherwise
     * @throws NetworkInterfaceException
     */
    public abstract boolean reserve(ResourceUsageImpl usage, NetworkInterface nwif, NetworkInterfaceController proxy,
            ResourceClient client, Object data) throws NetworkInterfaceException;

    /**
     * Release the <code>NetworkInterface</code> reserved by the proxy
     * parameter.
     * 
     * @param proxy
     *            <code>ResourceProxy</code> that currently has a
     *            <code>NetworkInterface</code> reserved.
     * @throws NotOwnerException
     *             if the proxy parameter does not have a
     *             <code>NetworkInterface</code> reserved.
     */
    public abstract void release(ResourceProxy proxy) throws NotOwnerException;

    /**
     * Returns the <code>NetworkInterface</code> currently reserved by the
     * <code>ResourceProxy</code>
     * 
     * @param proxy
     *            the proxy object that we want to find a reserved
     *            NetworkInterface for.
     * @return the <code>NetworkInterface</code> reserved by the proxy or
     *         <code>null</code> if no interface is reserved.
     */
    public abstract ExtendedNetworkInterface getReservedNetworkInterface(ResourceProxy proxy);

    /**
     * Find a {@link ExtendedNetworkInterface} that is tuned to the
     * {@link javax.tv.service.transport.TransportStream} identified by the
     * {@link Locator}. The rules used to find an appropriate
     * {@link ExtendedNetworkInterface} are also based on who the caller is,
     * identified by a {@link CallerContext}. If an appropriate
     * {@link ExtendedNetworkInterface} is found, it is returned.
     * <p>
     * The rules that are applied are as follows:
     * <ul>
     * <li>When the {@link CallerContext} is for an <em>un</em>bound app, prefer
     * {@link ExtendedNetworkInterface}s in this order:</li>
     * <ol>
     * <li>Reserved by the app.</li>
     * <li>Unreserved.</li>
     * <li>Reserved by something other than the app.</li>
     * </ol>
     * <li>When the {@link CallerContext} is for a <em>bound</em> app, prefer
     * {@link ExtendedNetworkInterface}s in this order:</li>
     * <ol>
     * <li>Reserved by the app.</li>
     * <li>Reserved by the ServiceContext containing the app.</li>
     * <li>Unreserved.</li>
     * <li>Reserved by something other than app or ServiceContext.</li>
     * </ol>
     * </ul>
     * 
     * @param locator
     *            A {@link Locator} that must be carried by the returned
     *            {@link ExtendedNetworkInterface}.
     * @param cc
     *            The {@link CallerContext} of the app on whose behalf the
     *            {@link ExtendedNetworkInterface} is obtained.
     * @return Returns the matching {@link ExtendedNetworkInterface} if found;
     *         otherwise, null.
     */
    public abstract ExtendedNetworkInterface findNetworkInterface(Locator locator, CallerContext cc);

    /*
     * (non-Javadoc) Add a ResourceUsage to a currently reserved
     * networkInterface.
     * 
     * @param usage A{@link ResourceUsage} to add to the currently reserved
     * {@ink NetworkInterface}
     * 
     * @param proxy A{@link ResourceProxy} associated with the current
     * NetworkInterface reservation.
     * 
     * @return true for successful addition to the reservation; false otherwise.
     */
    public abstract boolean addResourceUsage(ResourceUsageImpl usage, NetworkInterfaceController proxy)
            throws NetworkInterfaceException;

    /*
     * (non-Javadoc) Release a ResourceUsage associated with a currently
     * reserved networkInterface.
     * 
     * @param usage A{@link ResourceUsage} to release from the currently
     * reserved {@ink NetworkInterface}
     * 
     * @param proxy A{@link ResourceProxy} associated with the current
     * NetworkInterface reservation.
     * 
     * @return true for successful addition to the reservation; false otherwise.
     */
    public abstract boolean releaseResourceUsage(ResourceUsageImpl usage, NetworkInterfaceController proxy)
            throws NetworkInterfaceException;

    /*
     * (non-Javadoc) Get the ResourceUsages associated with a currently reserved
     * networkInterface.
     * 
     * @param proxy A{@link ResourceProxy} associated with the current
     * NetworkInterface reservation.
     * 
     * @return a Vector if successful; null otherwise.
     */
    public abstract Vector getResourceUsages(NetworkInterfaceController proxy) throws NetworkInterfaceException;
    
    /*
     * (non-Javadoc) Set the ResourceUsages for the given
     * networkInterface.
     * 
     * @param proxy A{@link ResourceProxy} associated with the current
     * NetworkInterface reservation.
     * 
     * @param resource usages to set
     * 
     */
    public abstract void setResourceUsages(ResourceProxy proxy, Vector resUsages) throws NetworkInterfaceException;
    
    /*
     * Notify <code>ResourceOfferListener</code>s in prioritized order that the
     * network interface is available a can be reserved. <i>This method is not
     * part of the defined public API, but is present for the implementation
     * only.</i>
     * 
     * @param resource - the resource to deliver with the offer.
     */
    public abstract boolean offerAvailableResource(Object resource);
}
