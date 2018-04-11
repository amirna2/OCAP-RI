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

import javax.tv.service.Service;

import org.cablelabs.impl.davic.net.tuning.SharableNetworkInterfaceManager.ProxyClient;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NotOwnerException;
import org.davic.resources.ResourceProxy;

public interface SharableNetworkInterfaceController extends ResourceProxy
{   
    public abstract void checkPermission() throws SecurityException;
    
    public abstract ExtendedNetworkInterface getNetworkInterface();
    
    public abstract void setProxyClient(ProxyClient pc);
    
    public abstract ProxyClient getProxyClient();
    
    /**
     * Releases the tuner.
     * <p>
     * This method causes a NetworkInterfaceReleasedEvent to be sent to the
     * listeners of the NetworkInterfaceManager.
     * 
     * @exception NotOwnerException
     *                raised if the controller does not currently have a network
     *                interface reserved
     */
    public abstract void release() throws NetworkInterfaceException;
    
    /**
     * This method is used to acquire a share-able NetworkInterface which is already
     * tuned to the designated Service or acquire a NetworkInterface and
     * initiate a tune to the designated Service.
     * 
     * When this method finds an NI which is already tuned to the provided
     * Service and can be shared, this method will associate the shared 
     * NI with this NetworkInterfaceController. If no NetworkInterface can be 
     * found/used for sharing, this method will attempt to reserve an NI, associate 
     * it with this NetworkInterfaceController, and initiate a tune.
     * 
     * In either of the above cases, the NetworkInterfaceController will
     * unconditionally release any existing NetworkInterface reservation and 
     * disassociate the current NI from the NetworkInterfaceController. And upon 
     * successful return of the call, the NetworkInterfaceCallback will be registered 
     * with the associated NI and tuneInstance will be returned. The state of the 
     * associated NetworkInterface will need to be established prior to use by the 
     * caller (e.g. the NI may be in the process of tuning or may not be synced - 
     * @see ExtendedNetworkInterface.isTuning() and 
     * @see ExtendedNetworkInterface.isSynced()). Any NI acquired via this
     * method will be considered share-able. NetworkInterfaces's acquired via 
     * NetworkInterfaceController.reserve()/reserveFor() will never be considered 
     * share-able.
     * 
     * If no NetworkInterface can be shared or acquired, this method will
     * throw a NetworkInterfaceException.
     * 
     * @param usage
     *            the ResourceUsage that describes the proposed reservation
     * @param service
     *            a Service object representing the Service requested for
     *            reserving and/or sharing.
     * @param requestData
     *            Used by the Resource Notification API in the requestRelease
     *            method of the ResourceClient interface. The usage of this
     *            parameter is optional and a null reference may be supplied.
     * @param niCallback
     *            The {@link NetworkInterfaceCallback} to register when a
     *            share-able NI is found.
     * @param priority
     *            The priority for the callback (a higher numerical value
     *            indicates a higher priority).
     * @return An object indicating the tune instance that was able to be shared
     *         or null of no NetworkInterface was found to be sharable.
     * @throws NetworkInterfaceException
     *             if no NetworkInterface could be found for sharing or no
     *             NetworkInterface could be reserved.
     */
    public abstract Object tuneOrShareFor( ResourceUsageImpl usage, Service service, Object requestData, 
                                     NetworkInterfaceCallback niCallback, int priority )
            throws NetworkInterfaceException;
    /**
     * This method is used to share an already-tuned NetworkInterface. 
     * 
     * When it's verified that the designated NI is tuned to the provided Service 
     * and is share-able, this method will associate the shared NI with this 
     * NetworkInterfaceController. The NetworkInterfaceCallback will be registered 
     * with the associated NI and tuneInstance will be returned. The state of the 
     * associated NetworkInterface will need to be established prior to use by the 
     * caller (e.g. the NI may be in the process of tuning or may not be synced - 
     * @see ExtendedNetworkInterface.isTuning() and 
     * @see ExtendedNetworkInterface.isSynced()). 
     * 
     * This method will unconditionally release any existing NetworkInterface 
     * reservation and disassociate the current NI from the NetworkInterfaceController. 
     * 
     * If the designated NetworkInterface cannot be shared, the method will throw 
     * NetworkInterfaceException.
     * 
     * @param usage
     *            the ResourceUsage that describes the proposed reservation
     * @param service
     *            a Service object representing the Service requested for reserving
     *            and/or sharing.
     * @param nwif
     *            the particular NetworkInterface requested for sharing
     * @param requestData
     *            Used by the Resource Notification API in the requestRelease
     *            method of the ResourceClient interface. The usage of this
     *            parameter is optional and a null reference may be supplied.
     * @param niCallback
     *            The {@link NetworkInterfaceCallback} to register when a share-able
     *            NI is found.
     * @param priority
     *            The priority for the callback (a higher numerical value
     *            indicates a higher priority).
     * @return
     *            An object indicating the tune instance that was able to be
     *            shared or null of no NetworkInterface was found to be sharable. 
     * @throws NetworkInterfaceException
     *            if no NEtworkInterface could be found for sharing or no NetworkInterface
     *            could be reserved.
     */
    public abstract Object shareFor( ResourceUsageImpl usage, Service service, 
                            NetworkInterface nwif, Object requestData, 
                            NetworkInterfaceCallback niCallback, int priority )
            throws NetworkInterfaceException;
}
