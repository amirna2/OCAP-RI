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

import java.util.ArrayList;

import javax.tv.service.Service;

import org.davic.mpeg.TransportStream;
import org.davic.net.tuning.IncorrectLocatorException;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NotOwnerException;
import org.davic.net.tuning.StreamNotFoundException;
import org.davic.resources.ResourceProxy;
import org.dvb.spi.selection.SelectionSession;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ResourceManager.Client;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.util.NativeHandle;

/**
 * Implementation specific extensions to <code>NetworkInterface</code>
 * 
 * @author Todd Earles
 * @author Jason Subbert
 */
public abstract class ExtendedNetworkInterface extends NetworkInterface implements NativeHandle
{
    /**
     * Reserve the NetworkInterface for <i>proxy</i>.
     * 
     * @param client
     *            the <code>ResourceClient</code> object that wants to use a
     *            scarce resource.
     * @return <code>true</code> if the reserve was successful,
     *         <code>false</code> if the reserve failed.
     */
    public abstract boolean reserve(Client client);

    /**
     * Release the <code>NetworkInterface</code>. A
     * <code>ResourceStatusEvent</code> is generated if release() is called by
     * the same proxy that currently has the <code>NetworkInterface</code>
     * reserved. No event is generated if the <code>ResourceProxy</code> does
     * not own the <code>NetworkInterface</code>.
     * 
     * @param proxy
     *            the <code>ResourceProxy</code> that owns the reservation
     * @throws NotOwnerException
     *             if the <code>ResourceProxy</code> does not own the
     *             <code>NetworkInterface</code>.
     */
    public abstract void release(ResourceProxy proxy) throws NotOwnerException;

    /**
     * Returns the object that currently has the <code>NetworkInterface</code>
     * reserved.
     * 
     * @return the <code>ResourceProxy</code> that currently has ownership of
     *         this interface.
     */
    public abstract Client getReservationOwner();

    /**
     * Tune the <code>NetworkInterface</code> to the transport stream pointed to
     * by <i>locator</i> assuming the reservation is still held by the calling
     * context. This call will result in the generation of a
     * <code>NetworkInterfaceTuningEvent</code>.
     * 
     * @param service
     *            the <code>Service</code> to tune to.
     * @param proxy
     *            the <code>ResourceProxy</code> that owns the reservation.
     * @param tuneToken
     *            the token that will be provided in NetworkInterfaceCallbacks
     *            associated with this tune
     * 
     * @throws StreamNotFoundException
     *             if the transport stream to be tuned cannot be found or cannot
     *             be tuned by this NetworkInterface.
     * @throws IncorrectLocatorException
     *             if the locator does not identify a tuneable transport stream.
     * @throws NotOwnerException
     *             if the calling context does not have the device reserved.
     */
    public abstract Object tune(final Service service, ResourceProxy proxy, Object tuneToken)
            throws NetworkInterfaceException;
    
    /**
     * Tune the <code>NetworkInterface</code> to the transport stream pointed to
     * by <i>locator</i> assuming the reservation is still held by the calling
     * context. This call will result in the generation of a
     * <code>NetworkInterfaceTuningEvent</code>.
     * @param locator
     *            the <code>TransportStream</code> to tune to.
     * @param proxy
     *            the <code>ResourceProxy</code> that owns the reservation.
     * @param tuneToken 
     *            the token that will be provided in NetworkInterfaceCallbacks
     *            associated with this tune
     * 
     * @throws StreamNotFoundException
     *             if the transport stream to be tuned cannot be found or cannot
     *             be tuned by this NetworkInterface.
     * @throws IncorrectLocatorException
     *             if the locator does not identify a tuneable transport stream.
     * @throws NotOwnerException
     *             if the calling context does not have the device reserved.
     */
    public abstract Object tune(final org.davic.net.Locator locator, ResourceProxy proxy, Object tuneToken)
            throws NetworkInterfaceException;
    
    /**
     * Tune the <code>NetworkInterface</code> to the transport stream pointed to
     * by <i>ts</i> assuming the reservation is still held by the calling
     * context. This call will result in the generation of a
     * <code>NetworkInterfaceTuningEvent</code>.
     * @param ts
     *            the <code>TransportStream</code> to tune to.
     * @param proxy
     *            the <code>ResourceProxy</code> that owns the reservation.
     * @param tuneToken 
     *            the token that will be provided in NetworkInterfaceCallbacks
     *            associated with this tune
     * 
     * @throws StreamNotFoundException
     *             if the transport stream to be tuned cannot be found or cannot
     *             be tuned by this NetworkInterface.
     * @throws NotOwnerException
     *             if the calling context does not have the device reserved.
     */
    public abstract Object tune(final TransportStream ts, ResourceProxy proxy, Object tuneToken) 
            throws NetworkInterfaceException;

    /**
     * Returns the <code>ResourceUsage</code> that describes the reservation or
     * pending reservation of this <code>NetworkInterface</code>
     * 
     * @return <code>ResourceUsage</code> or null
     */
    public abstract ResourceUsageImpl getResourceUsage();

    /**
     * Returns the list of all usages from the NetworkInterface.
     * In cases where the NI is being shared, the list will have more than
     * 1 ResourceUsage in it. This list will never contain a SharedResourceUsage.
     *         
     * @return list of ResourceUsages
     */
    public abstract ArrayList getResourceUsages();
    
    /**
     * Returns the <code>CallerCotext</code> associated with the reservation or
     * pending reservation of this <code>NetworkInterface</code>
     * 
     * @return <code>CallerContext</code> or null
     */
    public abstract CallerContext getOwnerContext();

    /**
     * Add a {@link NetworkInterfaceCallback} to the list of objects which are
     * synchronously notified of tuning operations. Notification is given in
     * order from highest to lowest priority.
     * 
     * @param callback
     *            The {@link NetworkInterfaceCallback} to add.
     * @param priority
     *            The priority for this callback where a higher numerical value
     *            indicate a higher priority.
     */
    public abstract void addNetworkInterfaceCallback(NetworkInterfaceCallback callback, int priority);

    /**
     * Remove the specified synchronous callback object (
     * {@link NetworkInterfaceCallback}) from the list of registered callbacks.
     * Has no effect if the callback is not in the list.
     * 
     * @param callback
     *            The {@link NetworkInterfaceCallback} to remove.
     */
    public abstract void removeNetworkInterfaceCallback(NetworkInterfaceCallback callback);

    /**
     * Return whether this network interface is currently tuning.
     * 
     * @param tuneReq
     *            the tune request
     *            
     * @throws NotOwnerException
     *             if the tune request is not the current tuning request
     *             
     * @return True if currently tuning; otherwise, false.
     */
    public abstract boolean isTuning(final Object tuneReq) throws NotOwnerException;

    /**
     * Return whether this network interface is currently tuned.
     * 
     * @param tuneReq
     *            the tune request
     *            
     * @throws NotOwnerException
     *             if the tune request is not the current tuning request
     * @return True if the NI is tuned, false otherwise.
     */
    public abstract boolean isTuned(final Object tuneReq) throws NotOwnerException;

    /**
     * Return whether this network interface is currently synced and ready to
     * receive data.
     * @param tuneReq
     *            the tune request
     *            
     * @throws NotOwnerException
     *             if the tune request is not the current tuning request
     * @return True if synced, false otherwise.
     */
    public abstract boolean isSynced(final Object tuneReq) throws NotOwnerException;

    /**
     * Returns the frequency of the transport stream to which this network
     * interface is currently tuned
     * 
     * @return the frequency of the tuned transport stream or -1 if not
     *         currently tuned
     */
    public abstract int getTransportStreamFrequency();

    /**
     * Returns the current SelectionSession for the tuned TransportStream.
     * 
     * @return the current SelectionSession or null if one is not available
     */
    public abstract SelectionSession getCurrentSelectionSession();
    
    /**
     * Returns the current tune request (tune cookie) for the tuned TransportStream.
     * 
     * @return the current tune request or null if one is not available
     */
    public abstract Object getCurrentTuneToken();
}
