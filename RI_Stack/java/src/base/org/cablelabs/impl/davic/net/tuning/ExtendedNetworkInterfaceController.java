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

import java.util.Vector;

import javax.tv.service.Service;

import org.davic.mpeg.TransportStream;
import org.davic.net.Locator;
import org.davic.net.tuning.IncorrectLocatorException;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NoFreeInterfaceException;
import org.davic.net.tuning.NotOwnerException;
import org.davic.net.tuning.StreamNotFoundException;
import org.davic.resources.ResourceClient;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.ocap.resource.ApplicationResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.ExtendedReserve;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;

/**
 * The purpose of this class is to extend the functionality of the
 * NetworkInterfaceController by providing extra methods for reserving a
 * NetworkInterface. The extra methods allow for the specification of what
 * CallerContext on which the NetworkInterface will be reserved.
 * 
 */
public class ExtendedNetworkInterfaceController extends NetworkInterfaceController implements ExtendedReserve
{
    /**
     * 
     * @param rc
     */
    public ExtendedNetworkInterfaceController(ResourceClient rc)
    {
        super(rc);
    }

    /**
     * Releases the tuner.
     * <p>
     * This method causes a NetworkInterfaceReleasedEvent to be sent to the
     * listeners of the NetworkInterfaceManager.
     * 
     * @param usage
     *            the <code>ResourceUsage</code> that should own the network
     *            interface reservation.
     * @exception NotOwnerException
     *                raised if the controller does not currently have a network
     *                interface reserved
     */
    public synchronized void release(ResourceUsageImpl usage) throws NetworkInterfaceException
    {
        nim.release(this);
    }

    /**
     * @param context
     *            Specifies the CallerContext within which the NetworkInterface
     *            will be reserved.
     * @param nwif
     *            Specifies the NetworkInterface to attempt to reserve
     * @param requestData
     *            Used by the Resource Notification API in the requestRelease
     *            method of the ResourceClient interface. The usage of this
     *            parameter is optional and a null reference may be supplied.
     */
    public synchronized void reserve(CallerContext context, NetworkInterface nwif, Object requestData)
            throws NetworkInterfaceException
    {
        if (context == null)
        {
            throw new NullPointerException("null CallerContext");
        }

        context.checkAlive();

        // Create Resource Usage
        ApplicationResourceUsageImpl ru = new ApplicationResourceUsageImpl(context);
        if (ru != null) ru.set(this, false);

        reserve(ru, nwif, requestData);
    }

    /**
     * @param context
     *            Specifies the CallerContext within which the NetworkInterface
     *            will be reserved.
     * @param locator
     *            a Locator that points to a transport stream that the reserved
     *            network interface should be able to tune to
     * @param requestData
     *            Used by the Resource Notification API in the requestRelease
     *            method of the ResourceClient interface. The usage of this
     *            parameter is optional and a null reference may be supplied.
     */
    public synchronized void reserveFor(CallerContext context, Locator locator, Object requestData)
            throws NetworkInterfaceException
    {
        if (context == null)
        {
            throw new NullPointerException("null CallerContext");
        }

        context.checkAlive();

        // Create Resource Usage
        ApplicationResourceUsageImpl ru = new ApplicationResourceUsageImpl(context);
        if (ru != null) ru.set(this, false);

        reserveFor(ru, locator, requestData);
    }

    /**
     * @param usage
     *            the ResourceUsage that describes the proposed reservation
     * @param nwif
     *            Specifies the NetworkInterface to attempt to reserve
     * @param requestData
     *            Used by the Resource Notification API in the requestRelease
     *            method of the ResourceClient interface. The usage of this
     *            parameter is optional and a null reference may be supplied.
     */
    public void reserve(ResourceUsageImpl usage, NetworkInterface nwif, Object requestData)
            throws NetworkInterfaceException
    {
        if (usage == null)
        {
            throw new NullPointerException("null ResourceUsage");
        }

        super.reserve(usage, nwif, requestData);
    }

    /**
     * @param usage
     *            the ResourceUsage that describes the proposed reservation
     * @param locator
     *            a Locator that points to a transport stream that the reserved
     *            network interface should be able to tune to
     * @param requestData
     *            Used by the Resource Notification API in the requestRelease
     *            method of the ResourceClient interface. The usage of this
     *            parameter is optional and a null reference may be supplied.
     */
    public void reserveFor(ResourceUsageImpl usage, Locator locator, Object requestData)
            throws NetworkInterfaceException
    {
        if (usage == null)
        {
            throw new NullPointerException("null ResourceUsage");
        }

        super.reserveFor(usage, locator, requestData);
    }

    /**
     * Adds the usage to the reservation from the reserved NetworkInterface.
     * 
     * @param usage
     *            an ResourceUsageImpl that describes the proposed reservation
     * @exception IncorrectLocatorException
     *                raised if the locator does not references a broadcast
     *                transport stream
     * 
     * @param usage
     *            the ResourceUsage that describes the proposed add.
     */
    public void addResourceUsage(ResourceUsageImpl usage) throws NetworkInterfaceException
    {
        if (usage == null)
        {
            throw new NullPointerException("null ResourceUsage");
        }

        if (!nim.addResourceUsage(usage, this))
        {
            // at this point just throw an exception because add usasge failed.
            throw new NoFreeInterfaceException();
        }
    }

    /**
     * Gets the usage/usages from the reserved NetworkInterface.
     * 
     * 
     * @return Vector the Vector of ResourceUsage/ResourceUsages associated with
     *         the NetworkInterface reservation.
     */
    public Vector getResourceUsage() throws NetworkInterfaceException
    {
        return nim.getResourceUsages(this);
    }

    /**
     * Removes the usage association to the reservation from the reserved
     * NetworkInterface.
     * 
     * @param usage
     *            the ResourceUsage that describes the proposed release for the
     *            reserved NetworkInterface.
     */
    public void releaseResourceUsage(ResourceUsageImpl usage) throws NetworkInterfaceException
    {
        if (usage == null)
        {
            throw new NullPointerException("null ResourceUsage");
        }
        nim.releaseResourceUsage(usage, this);
    }

    /**
     * Tunes asynchronously to the given stream (specified by a Locator). This
     * method causes the NetworkInterfaceTuningEvent and the
     * NetworkInterfaceTuningOverEvent to be sent to the listeners of the
     * NetworkInterface reserved by this NetworkInterfaceController.
     * <p>
     * If tuning fails for one of the reasons which generate an exception, the
     * status of the network interface will be unchanged and no events
     * generated. If failure of tuning is reported by the event code of the
     * NetworkInterfaceTuningOverEvent then the state of the network interface
     * is not defined and it may be tuned to any transport stream or be left in
     * a state where it is not tuned to any transport stream.
     * 
     * @param locator
     *            The locator describing the transport stream to tune to
     * @exception StreamNotFoundException
     *                raised if the specified locator does not point to any
     *                known transport stream or the currently reserved
     *                NetworkInterface cannot tune to the specified transport
     *                stream
     * @return
     * @exception IncorrectLocatorException
     *                raised if locator does not references a broadcast
     *                transport stream
     * @exception NotOwnerException
     *                raised if no network interface is reserved at the moment
     */
    public synchronized Object extendedTune(org.davic.net.Locator locator) throws NetworkInterfaceException
    {
        if (locator == null)
        {
            throw new NullPointerException();
        }

        ExtendedNetworkInterface ni = nim.getReservedNetworkInterface(this);

        if (ni == null)
        {
            throw new NotOwnerException();
        }

        // tune to the TransportStream addressed by the locator
        return ni.tune(locator, this, null);
    }

    public synchronized Object extendedTune(Service service) throws NetworkInterfaceException
    {
        ExtendedNetworkInterface ni = nim.getReservedNetworkInterface(this);

        if (ni == null)
        {
            throw new NotOwnerException();
        }

        // tune to the Service 
        return ni.tune(service, this, null);
    }
    
    public synchronized Object extendedTune(Service service, Object tuneCookie) throws NetworkInterfaceException
    {
        ExtendedNetworkInterface ni = nim.getReservedNetworkInterface(this);

        if (ni == null)
        {
            throw new NotOwnerException();
        }

        // tune to the Service 
        return ni.tune(service, this, tuneCookie);
    }
    
    /**
     * Tunes asynchronously to the given transport stream.
     * <p>
     * This method causes the NetworkInterfaceTuningEvent and the
     * NetworkInterfaceTuningOverEvent to be sent to the listeners of the
     * NetworkInterface reserved by this NetworkInterfaceController. If tuning
     * fails for one of the reasons which generate an exception, the status of
     * the network interface will be unchanged and no events generated. If
     * failure of tuning is reported by the event code of the
     * NetworkInterfaceTuningOverEvent then the state of the network interface
     * is not defined and it may be tuned to any transport stream or be left in
     * a state where it is not tuned to any transport stream.
     * 
     * @param ts
     *            Transport stream object to tune to
     * @exception StreamNotFoundException
     *                raised if the specified transport stream is not associated
     *                with the currently reserved network interface
     * @exception NotOwnerException
     *                raised if no network interface is reserved at the moment
     */
    public synchronized Object extendedTune(TransportStream ts) throws NetworkInterfaceException
    {
        if (ts == null)
        {
            throw new NullPointerException("null TransportStream");
        }

        ExtendedNetworkInterface ni = nim.getReservedNetworkInterface(this);

        if (ni == null)
        {
            throw new NotOwnerException();
        }

        // tune the NetworkInterface
        return ni.tune(ts, this, null);
    }
}
