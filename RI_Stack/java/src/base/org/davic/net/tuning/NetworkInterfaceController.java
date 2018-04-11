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

package org.davic.net.tuning;

import org.davic.mpeg.TransportStream;
import org.davic.net.Locator;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.net.tuning.TunerPermission;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterfaceManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.resource.ApplicationResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * NetworkInterfaceController represents a controller that can be used for
 * tuning a network interface. Applications may create a network interface
 * controller object and use it to attempt to reserve the capability to tune a
 * network interface.
 * <p>
 * The capability to tune a network interface is a resource and the network
 * interface controller acts as a resource proxy for this resource.
 */

public class NetworkInterfaceController implements ResourceProxy
{
    /**
     * Creates a NetworkInterfaceController
     * 
     * @param rc
     *            The ResourceClient that the controller is associated with
     */
    public NetworkInterfaceController(ResourceClient rc)
    {
        // client that is notified if NetworkInterface access is lost
        client = rc;
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
     * @exception IncorrectLocatorException
     *                raised if locator does not references a broadcast
     *                transport stream
     * @exception NotOwnerException
     *                raised if no network interface is reserved at the moment
     */
    public synchronized void tune(org.davic.net.Locator locator) throws NetworkInterfaceException
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
        ni.tune(locator, this, null);
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
    public synchronized void tune(TransportStream ts) throws NetworkInterfaceException
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
        ni.tune(ts, this, null);
    }

    /**
     * Tries to reserve exclusively the control over the specified network
     * interface.
     * <p>
     * If the reservation succeeds, a NetworkInterfaceReservedEvent is sent to
     * the listeners of the NetworkInterfaceManager. If this
     * NetworkInterfaceController has currently reserved another
     * NetworkInterface, then it will either release that NetworkInterface and
     * reserve an appropriate one, or throw an exception. If a NetworkInterface
     * that is able to tune to the specified transport stream is currently
     * reserved by this NetworkInterfaceController, then this method does
     * nothing.
     * 
     * @param ni
     *            Network Interface to be reserved
     * @param requestData
     *            Used by the Resource Notification API in the requestRelease
     *            method of the ResourceClient interface. The usage of this
     *            parameter is optional and a null reference may be supplied.
     * @exception NoFreeInterfaceException
     *                raised if the requested network interface can not be
     *                reserved
     * @exception SecurityException
     *                raised if the application does not have an instance of
     *                <code>TunerPermission</code>
     */
    public synchronized void reserve(NetworkInterface nwif, Object requestData) throws NetworkInterfaceException
    {
        ExtendedNetworkInterface ni = (ExtendedNetworkInterface) nwif;
        if (ni == null)
        {
            throw new NullPointerException("null NetworkInterface");
        }

        // security check
        checkPermission();

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(org.cablelabs.impl.manager.CallerContextManager.class);
        CallerContext cc = ccm.getCurrentContext();
        // Create Application Resource Usage
        ApplicationResourceUsageImpl ru = new ApplicationResourceUsageImpl(cc);
        if (ru != null) ru.set(this, false);

        reserve(ru, nwif, requestData);
    }

    /**
     * Tries to reserve exclusively the control over a network interface that
     * can receive the transport stream specified by the locator parameter.
     * <p>
     * The specific network interface is selected by the method implementation.
     * <p>
     * If the reservation succeeds, a NetworkInterfaceReservedEvent is sent to
     * the listeners of the NetworkInterfaceManager. If this
     * NetworkInterfaceController has currently reserved another
     * NetworkInterface, then it will either release that NetworkInterface and
     * reserve an appropriate one, or throw an exception. If a NetworkInterface
     * that is able to tune to the specified transport stream is currently
     * reserved by this NetworkInterfaceController, then this method does
     * nothing.
     * 
     * @param locator
     *            a Locator that points to a transport stream that the reserved
     *            network interface should be able to tune to
     * @param requestData
     *            Used by the Resource Notification API in the requestRelease
     *            method of the ResourceClient interface. The usage of this
     *            parameter is optional and a null reference may be supplied.
     * @exception NoFreeInterfaceException
     *                raised if a network interface can not be reserved
     * @exception StreamNotFoundException
     *                raised if the specified locator does not point to any
     *                known transport stream
     * @exception IncorrectLocatorException
     *                raised if the locator does not references a broadcast
     *                transport stream
     * @exception SecurityException
     *                raised if the application does not have an instance of
     *                <code>TunerPermission</code>
     * 
     */
    public synchronized void reserveFor(org.davic.net.Locator locator, Object requestData)
            throws NetworkInterfaceException
    {
        if (locator == null)
        {
            throw new NullPointerException("null Locator");
        }
        if (!(locator instanceof org.ocap.net.OcapLocator))
        {
            throw new IncorrectLocatorException();
        }

        // security check
        checkPermission();

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(org.cablelabs.impl.manager.CallerContextManager.class);
        CallerContext cc = ccm.getCurrentContext();
        // Create Application Resource Usage
        ApplicationResourceUsageImpl ru = new ApplicationResourceUsageImpl(cc);
        if (ru != null) ru.set(this, false);

        reserveFor(ru, locator, requestData);
    }

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
    public synchronized void release() throws NetworkInterfaceException
    {
        nim.release(this);
    }

    /**
     * Returns the network interface associated with this controller.
     * 
     * @return the network interface associated with this controller or null if
     *         no network interface has been reserved.
     */
    public NetworkInterface getNetworkInterface()
    {
        return nim.getReservedNetworkInterface(this);
    }

    /**
     * Returns the resource client that is associated with this
     * NetworkInterfaceController. This method implements getClient method of
     * org.davic.resources.ResourceProxy.
     * 
     * @return the resource client associated with this controller
     */
    public ResourceClient getClient()
    {
        return client;
    }

    /**
     * Tries to reserve exclusively the control over the specified network
     * interface.
     * <p>
     * If the reservation succeeds, a NetworkInterfaceReservedEvent is sent to
     * the listeners of the NetworkInterfaceManager. If this
     * NetworkInterfaceController has currently reserved another
     * NetworkInterface, then it will either release that NetworkInterface and
     * reserve an appropriate one, or throw an exception. If a NetworkInterface
     * that is able to tune to the specified transport stream is currently
     * reserved by this NetworkInterfaceController, then this method does
     * nothing.
     * 
     * @param usage
     *            an ExtendedResourceUsage that describes the proposed
     *            reservation
     * @param ni
     *            Network Interface to be reserved
     * @param requestData
     *            Used by the Resource Notification API in the requestRelease
     *            method of the ResourceClient interface. The usage of this
     *            parameter is optional and a null reference may be supplied.
     * @exception NoFreeInterfaceException
     *                raised if the requested network interface can not be
     *                reserved
     * @exception SecurityException
     *                raised if the application does not have an instance of
     *                <code>TunerPermission</code>
     */
    protected synchronized void reserve(ResourceUsageImpl usage, NetworkInterface nwif, Object data)
            throws NetworkInterfaceException
    {

        ExtendedNetworkInterface ni = (ExtendedNetworkInterface) nwif;
        if (ni == null)
        {
            throw new NullPointerException("null NetworkInterface");
        }

        if (usage == null)
        {
            throw new NullPointerException("null ResourceUsage");
        }

        // check if the application has TunerPermission
        checkPermission();

        if (!nim.reserve(usage, nwif, this, client, data)) throw new NoFreeInterfaceException();
    }

    /**
     * Tries to reserve exclusively the control over a network interface that
     * can receive the transport stream specified by the locator parameter.
     * <p>
     * The specific network interface is selected by the method implementation.
     * <p>
     * If the reservation succeeds, a NetworkInterfaceReservedEvent is sent to
     * the listeners of the NetworkInterfaceManager. If this
     * NetworkInterfaceController has currently reserved another
     * NetworkInterface, then it will either release that NetworkInterface and
     * reserve an appropriate one, or throw an exception. If a NetworkInterface
     * that is able to tune to the specified transport stream is currently
     * reserved by this NetworkInterfaceController, then this method does
     * nothing.
     * 
     * @param usage
     *            an ExtendedResourceUsage that describes the proposed
     *            reservation
     * @param locator
     *            a Locator that points to a transport stream that the reserved
     *            network interface should be able to tune to
     * @param requestData
     *            Used by the Resource Notification API in the requestRelease
     *            method of the ResourceClient interface. The usage of this
     *            parameter is optional and a null reference may be supplied.
     * @exception NoFreeInterfaceException
     *                raised if a network interface can not be reserved
     * @exception StreamNotFoundException
     *                raised if the specified locator does not point to any
     *                known transport stream
     * @exception IncorrectLocatorException
     *                raised if the locator does not references a broadcast
     *                transport stream
     * @exception SecurityException
     *                raised if the application does not have an instance of
     *                <code>TunerPermission</code>
     * 
     */
    protected synchronized void reserveFor(ResourceUsageImpl usage, Locator locator, Object data)
            throws NetworkInterfaceException
    {
        if (locator == null)
        {
            throw new NullPointerException("null Locator");
        }

        if (usage == null)
        {
            throw new NullPointerException("null ResourceUsage");
        }

        if (!(locator instanceof org.ocap.net.OcapLocator))
        {
            throw new IncorrectLocatorException();
        }

        // check that the locator references a broadcast transport stream
        OcapLocator ocapLoc = (OcapLocator) locator;
        if (ocapLoc.getSourceID() == -1 && ocapLoc.getFrequency() == -1 && ocapLoc.getProgramNumber() != -1)
        {
            throw new IncorrectLocatorException("locator does not reference a broadcast transport stream");
        }
        // check that the locator points to a known transport stream
        TransportStream streams[] = StreamTable.getTransportStreams(locator);
        if (streams == null) throw new StreamNotFoundException("locator does not reference a known TransportStream");

        // check if the application has TunerPermission
        checkPermission();

        if (!nim.reserveFor(usage, locator, this, client, data))
        {
            // at this point just throw an exception because reservation failed.
            throw new NoFreeInterfaceException();
        }
    }

    private void checkPermission() throws SecurityException
    {
        SecurityUtil.checkPermission(new TunerPermission("Permission check"));
    }

    /**
     * reference to the NetworkInterfaceManager/
     */
    protected ExtendedNetworkInterfaceManager nim = (ExtendedNetworkInterfaceManager) NetworkInterfaceManager.getInstance();

    /**
     * the resource client that created this controller
     */
    private ResourceClient client;

}
