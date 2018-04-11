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

import org.apache.log4j.Logger;
import org.davic.mpeg.TransportStream;
import org.davic.net.Locator;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceReleasedEvent;
import org.davic.net.tuning.NetworkInterfaceReservedEvent;
import org.davic.net.tuning.NotOwnerException;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.cablelabs.impl.util.EventMulticaster;
import org.ocap.resource.ApplicationResourceUsage;
import org.ocap.service.ServiceContextResourceUsage;

import org.cablelabs.impl.davic.mpeg.TransportStreamExt;
import org.cablelabs.impl.davic.resources.ExtendedResourceServer;
import org.cablelabs.impl.davic.resources.ResourceOfferListener;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ResourceManager;
import org.cablelabs.impl.manager.CallerContext.Multicaster;
import org.cablelabs.impl.manager.ResourceManager.Client;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;

import java.util.Vector;
import java.util.ArrayList;

import javax.tv.service.Service;

import org.cablelabs.impl.util.SystemEventUtil;

/**
 * A network interface manager is an object that keeps track of broadcast
 * network interfaces that are connected to the receiver.
 * <p>
 * There is only one instance of the network interface manager in a receiver and
 * this can be retrieved using the getInstance method.
 */
public class NetworkInterfaceManagerImpl extends ExtendedNetworkInterfaceManager implements ExtendedResourceServer
{

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(NetworkInterfaceManager.class.getName());
    
    private SharableNetworkInterfaceManager sharableNIManager = null;

    /* For javadoc to hide the non-public constructor. */
    protected NetworkInterfaceManagerImpl()
    {
        setInterfaces();
    }

    public SharableNetworkInterfaceManager getSharableNetworkInterfaceManager()
    { 
        if(sharableNIManager == null)
        {
            sharableNIManager = new SharableNetworkInterfaceManager();
        }
        return sharableNIManager;
    }
    
    /**
     * Set the list of network interfaces
     */
    protected void setInterfaces()
    {
        // Native tuner handles are assigned from 1..n.
        int numTuners = getNumberOfTuners();
        networkInterfaces = new ExtendedNetworkInterface[numTuners];
        if (numTuners == 0)
        {
            SystemEventUtil.logCatastrophicError("No tuners available", new Exception("No tuners available"));
        }
        else
        {
            for (int i = 0; i < numTuners; ++i)
                networkInterfaces[i] = new NetworkInterfaceImpl(i + 1);
        }
        if (log.isInfoEnabled()) 
        {
            log.info("number of tuners created: " + numTuners);
        }
    }

    /**
     * Returns the instance of the NetworkInterfaceManager
     * 
     * @return network interface manager
     */
    public static NetworkInterfaceManager getInstance()
    {
        return instance;
    }

    /**
     * Returns all network interfaces.
     * <p>
     * If there are no network interfaces, returns an array with the length of
     * zero.
     * 
     * @return an array containing all network interfaces
     */
    public NetworkInterface[] getNetworkInterfaces()
    {
        NetworkInterface array[] = new NetworkInterface[networkInterfaces.length];
        System.arraycopy(networkInterfaces, 0, array, 0, networkInterfaces.length);
        return array;
    }

    /**
     * Returns the NetworkInterface with which the specified TransportStream
     * object is associated. It neither tunes nor reserves the NetworkInterface.
     * 
     * @param ts
     *            Transport stream object
     * @return network interface that is associated with the transport stream
     */
    public NetworkInterface getNetworkInterface(TransportStream ts)
    {
        return (ts instanceof TransportStreamExt) ? ((TransportStreamExt) ts).getNetworkInterface() : null; // E.g.,
                                                                                                            // would
                                                                                                            // be
                                                                                                            // null
                                                                                                            // for
                                                                                                            // PODExtendedChannel
    }

    /**
     * Registers a resource status listener to receive resource status events
     * 
     * @param listener
     *            listener to be registered
     */
    public void addResourceStatusEventListener(ResourceStatusListener listener)
    {
        addResourceStatusEventListener(listener, ccm.getCurrentContext());
    }

    /**
     * Removes the registration of a registered listener so that it will not
     * receive resource status events any more
     * 
     * @param listener
     *            listener whose registration is to be removed
     */
    public void removeResourceStatusEventListener(ResourceStatusListener listener)
    {
        removeResourceStatusEventListener(listener, ccm.getCurrentContext());
    }

    /**
     * Add a <code>ResourceStatusListener</code> to this device for the given
     * calling context.
     * 
     * @param listener
     *            the <code>ResourceStatusListener</code> to be added to this
     *            device.
     * @param context
     *            the context of the application installing the listener
     */
    private void addResourceStatusEventListener(ResourceStatusListener listener, CallerContext context)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = getData(context);

            // Update listener/multicaster
            data.rsListeners = EventMulticaster.add(data.rsListeners, listener);

            // Manage context/multicaster
            rsContexts = Multicaster.add(rsContexts, context);
        }
    }

    /**
     * Remove a <code>ResourceStatusListener</code> from this device for the
     * given calling context.
     * 
     * @param listener
     *            the <code>ResourceStatusListener</code> to be removed to this
     *            device.
     * @param ctx
     *            the context of the application removing the listener
     */
    private void removeResourceStatusEventListener(ResourceStatusListener listener, CallerContext ctx)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = (Data) ctx.getCallbackData(this);

            // Remove the given listener from the set of listeners
            if (data != null && data.rsListeners != null)
            {
                data.rsListeners = EventMulticaster.remove(data.rsListeners, listener);
            }
        }
    }

    /**
     * Notify <code>ResourceStatusListener</code>s of changes to the resource
     * reservation status of this network interface device. <i>This method is
     * not part of the defined public API, but is present for the implementation
     * only.</i>
     * 
     * @param e
     *            the event to deliver
     */
    void notifyResourceStatusListener(ResourceStatusEvent e)
    {
        // Do not lock during this call!
        // We simply want whatever we read at the time of access.

        final ResourceStatusEvent event = e;
        CallerContext contexts = rsContexts;
        if (contexts != null)
        {
            contexts.runInContext(new Runnable()
            {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();

                    // Listeners are maintained in-context
                    Data data = (Data) ctx.getCallbackData(NetworkInterfaceManagerImpl.this);
                    if (null != data)
                    {
                        ResourceStatusListener l = data.rsListeners;
                        if (l != null)
                        {
                            l.statusChanged(event);
                        }
                    }

                }
            });
        }
    }

    /*
     * Private class to represent a instance of a ResourceOfferListener and its
     * associated priority.
     */
    private class NIReceiver
    {
        private ResourceOfferListener rol = null;

        private int priority = 0;

        NIReceiver(ResourceOfferListener rol, int priority)
        {
            this.rol = rol;
            this.priority = priority;
        }

        public ResourceOfferListener getROL()
        {
            return this.rol;
        }

        public int getPriority()
        {
            return this.priority;
        }
    }

    /*
     * Add entries to the array list sorted by priority lower the int priority
     * in the NEReceiver, the higher the priority.
     */
    private void sortIn(NIReceiver nir)
    {
        if (log.isDebugEnabled())
        {
            log.debug("sortIn() - Enter - First Check if listener already exists.\n");
        }
        //
        // First make sure this isn't the same listener.
        //
        for (int ii = 0; ii < this.offerListeners.size(); ii++)
        {
            NIReceiver curNirNode = (NIReceiver) this.offerListeners.get(ii);
            if ((nir.getROL()).equals(curNirNode.getROL()))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("sortIn() - found duplicate entry - not adding.\n");
                }
                return;
            }
        }

        // Insert the new entry after all previous entries with
        // the same priority and before lower priority entries.
        if (log.isDebugEnabled())
        {
            log.debug("sortIn() - size = " + this.offerListeners.size());
        }
        boolean added = false;
        for (int ii = 0; ii < this.offerListeners.size(); ii++)
        {
            NIReceiver node = (NIReceiver) this.offerListeners.get(ii);
            if (log.isDebugEnabled())
            {
                log.debug("sortIn() - newEntry priority = " + nir.getPriority() + " Entry(" + ii + ") priority = "
                        + node.getPriority());
            }
            if (nir.getPriority() < node.getPriority())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("sortIn() - adding new entry at location - " + ii + "\n");
                }
                this.offerListeners.add(ii, nir);
                added = true;
                return;
            }
        }

        // If I reached the end of the list without adding the receiver,
        // just add it to the end of the list.
        if (!added)
        {
            if (log.isDebugEnabled())
            {
                log.debug("sortIn() - priority lower than other existing entries - adding to end of list.\n");
            }
            this.offerListeners.add(nir);
        }
    }

    /*
     * Registers a resource status listener to receive resource status events
     * 
     * @param listener listener to be registered
     * 
     * @param priority the priority of the offer - lower the int, higher the
     * priority.
     */
    public void addResourceOfferListener(ResourceOfferListener listener, int priority)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addResourceOfferEventListener() - Enter");
        }

        if (listener == null || priority < 0) return;

        NIReceiver nir = new NIReceiver(listener, priority);
        synchronized (lock)
        {
            sortIn(nir);
        }
    }

    /*
     * Removes the registration of a registered listener so that it will not
     * receive resource offers events any more
     * 
     * @param listener listener whose registration is to be removed
     * 
     * @param priority priority of offer
     */
    public void removeResourceOfferListener(ResourceOfferListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removeResourceOfferEventListener() - Enter");
        }

        if (listener == null) return;

        synchronized (lock)
        {
            for (int ii = 0; ii < this.offerListeners.size(); ii++)
            {
                NIReceiver nir = (NIReceiver) this.offerListeners.get(ii);
                if (listener == nir.getROL())
                {
                    this.offerListeners.remove(ii);
                }
            }
        }
    }

    /*
     * Notify <code>ResourceOfferListener</code>s in prioritized order that the
     * network interface is available a can be reserved. <i>This method is not
     * part of the defined public API, but is present for the implementation
     * only.</i>
     * 
     * @param resource - the resource to deliver with the offer.
     */
    public boolean offerAvailableResource(Object resource)
    {
        // Do not lock during this call!
        // We simply want whatever we read at the time of access.
        final NetworkInterface netInterface = (NetworkInterface) resource;
        boolean rtCode = false;

        // Make the offer. If false is returned try the next offeree,
        // else stop offering.
        for (int ii = 0; ii < offerListeners.size(); ii++)
        {
            NIReceiver nir = (NIReceiver) offerListeners.get(ii);
            ResourceOfferListener rol = nir.getROL();
            rtCode = rol.offerResource(netInterface);
            if (false == rtCode)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("offerAvailableResource - Receiver didn't want NI going to next receiver...");
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("offerAvailableResource Receiver: " + ii + " accepted NI...");
                }
                return true;
            }
        }
        return false;
    }

    public boolean reserveFor(ResourceUsageImpl usage, Locator locator, ResourceProxy proxy,
            ResourceClient client, Object data) throws NetworkInterfaceException
    {
        if (log.isInfoEnabled())
        {
            log.info(" reserveFor enter- ");
        }
        
        Client newClient = null;
        if (usage instanceof ApplicationResourceUsage)
            newClient = new Client(client, proxy, usage, ccm.getCurrentContext());
        else
            newClient = new Client(client, proxy, usage, ccm.getSystemContext());
        ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);

        // Determine if reservation is even allowed
        if (!rm.isReservationAllowed(newClient, NetworkInterfaceController.class.getName())) return false;

        // if there are no interfaces to reserve, then no need to continue
        if (networkInterfaces.length == 0)
        {
            return false;
        }

        while (true)
        {
            try
            {
                Client clientArray[];
                // synchronized (reserveLock)
                // {
                // first go through the list of interfaces to see if the passed
                // in controller
                // already has a network interface reserved.
                if (getReservedNetworkInterface(newClient) != null)
                {
                    return true;
                }
                
                // TODO: one should check that the interface can tune to
                // the service referenced by the locator, but at
                // this time all services can be tuned by any locator.

                // look for a free interface
                for (int i = 0; i < networkInterfaces.length; i++)
                {
                    if (!networkInterfaces[i].isReserved())
                    {
                        // reserve this interface and return;
                        if (networkInterfaces[i].reserve(newClient))
                        {
                            // reservation succeeded so register callback data
                            // if this is an
                            // explicit reservation
                            if (isExplicitReservation(newClient)) getData(newClient.context);
                            notifyResourceStatusListener(new NetworkInterfaceReservedEvent(networkInterfaces[i]));
                            return true;
                        }
                        else
                        {
                            // TODO: throw TuningResourceContention?
                        }
                    }
                }

                // there aren't any free interfaces. Try negotiation then
                // contention.
                clientArray = new Client[networkInterfaces.length];
                for (int i = 0; i < networkInterfaces.length; i++)
                {
                    clientArray[i] = networkInterfaces[i].getReservationOwner();
                }
                // }

                return negotiateRelease(newClient, clientArray, data) || forceRelease(newClient, clientArray);
            }
            catch (TuningResourceContention e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("TuningResourceContention has occurred.");
                }
                // Start over!
                continue;
            }
        }
    }
    
    public synchronized boolean addResourceUsage(ResourceUsageImpl usage, NetworkInterfaceController proxy)
            throws NetworkInterfaceException
    {
        if (log.isDebugEnabled())
        {
            log.debug("NetworkInterface addResourceUsage Enter usage: " + usage + " proxy: " + proxy);
        }

        // Does the calling proxy correspond to the reserved proxy.
        // Get the NI for this proxy.
        ExtendedNetworkInterface eni = getReservedNetworkInterface(proxy);
        if (eni == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("addResourceUsage - NIC doesn't own a NetworkInterface!!!");
            }
            return false;
        }

        Client resOwner = eni.getReservationOwner();

        // Verify that the proxy is the same that already has the
        // current reservation.

        if (true != resOwner.proxy.equals(proxy))
        {
            if (log.isErrorEnabled())
            {
                log.error("proxy: " + (proxy != null ? proxy : null) + " does not equal owning proxy");
            }
            return false;
        }

        if (true == resOwner.addResourceUsage(usage))
        {
            resOwner.dumpUsages();
            return true;
        }
        return false;
    }

    public synchronized boolean releaseResourceUsage(ResourceUsageImpl usage, NetworkInterfaceController proxy)
            throws NetworkInterfaceException
    {
        // Get the reserved networkInterface. It must have been reserved
        // already;
        // otherwise the release call doesn't make sense. Fail if the proxy
        // doesn't
        // have a reservation.
        ExtendedNetworkInterface eni = getReservedNetworkInterface(proxy);
        if (null == eni)
        {
            if (log.isErrorEnabled())
            {
                log.error("releaseResourceUsage - NetworkInterfaceController(proxy: " + (proxy != null ? proxy : null)
                        + ")doesn't own a NetworkInterface.");
            }
            return false;
        }

        // Get the ResourceManager client object, - the owner of the
        // NetworkInterface. Fail if there is no owner.
        Client resOwner = eni.getReservationOwner();
        if (null != resOwner)
        {
            // Release the usage. Dump the usages here so we
            // can follow which usages are still attached to the
            // NetworkInterface.
            if (true == resOwner.releaseResourceUsage(usage))
            {
                resOwner.dumpUsages();
                return true;
            }
        }
        return false;
    }

    public synchronized void setResourceUsages(ResourceProxy proxy, Vector resUsages)
    {
        // Set the resource usages for the given proxy
        ExtendedNetworkInterface eni = getReservedNetworkInterface(proxy);
        if (null == eni)
        {
            if (log.isErrorEnabled())
            {
                log.error("setResourceUsages - NetworkInterfaceController(Client: " + (proxy != null ? proxy : null)
                        + ") doesn't own a NetworkInterface.");
            }
            return;
        }

        // Get the ResourceManager client object, - the owner of the
        // NetworkInterface. Fail if there is no owner.
        Client resOwner = eni.getReservationOwner();
        if (null != resOwner)
        {
            resOwner.setResourceUsages(resUsages);
            resOwner.dumpUsages();
        }
    }
    
    public synchronized Vector getResourceUsages(NetworkInterfaceController proxy)
    {
        // Get the reserved networkInterface. It must have been reserved
        // already;
        // otherwise the getter call doesn't make sense. Fail if the proxy
        // doesn't
        // have a reservation.
        ExtendedNetworkInterface eni = getReservedNetworkInterface(proxy);
        if (null == eni)
        {
            if (log.isErrorEnabled())
            {
                log.error("getResourceUsages - NetworkInterfaceController(proxy: " + (proxy != null ? proxy : null)
                        + ")doesn't own a NetworkInterface.");
            }
            return null;
        }

        // Get the ResourceManager client object, - the owner of the
        // NetworkInterface. Fail if there is no owner.
        Client resOwner = eni.getReservationOwner();
        if (null != resOwner)
        {
            return (Vector) resOwner.resUsages.clone();
        }
        return null;
    }

    // description in ExtendedNetworkInterfaceManagerImpl
    public boolean reserve(ResourceUsageImpl usage, NetworkInterface nwif, NetworkInterfaceController proxy,
            ResourceClient client, Object data) throws NetworkInterfaceException
    {

        Client newClient = new Client(client, proxy, usage,
                ((CallerContextManager) ManagerManager.getInstance(CallerContextManager.class)).getCurrentContext());
        ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);

        // Determine if reservation is even allowed
        if (!rm.isReservationAllowed(newClient, NetworkInterfaceController.class.getName())) return false;

        while (true)
        {
            try
            {
                Client clientArray[];

                // synchronized(reserveLock)
                // {
                // first check if we currently have the requested
                // NetworkInterface reserved
                Client owner = ((ExtendedNetworkInterface) nwif).getReservationOwner();
                if (owner != null && owner.proxy == proxy)
                {
                    return true;
                }

                // go through the list of interfaces to see if the passed in
                // controller already has a network interface reserved. If it
                // does
                // then release the interface.
                if (getReservedNetworkInterface(newClient) != null)
                {
                    release(proxy);
                }

                // if not, then try to reserve the interface and do
                // negotiation/contention if necessary.

                if (((ExtendedNetworkInterface) nwif).reserve(newClient))
                {
                    // reservation succeeded so register callback data if this
                    // is an
                    // explicit reservation
                    if (isExplicitReservation(newClient)) getData(newClient.context);
                    notifyResourceStatusListener(new NetworkInterfaceReservedEvent(nwif));
                    return true;
                }

                clientArray = new Client[] { ((ExtendedNetworkInterface) nwif).getReservationOwner() };
                // }
                return negotiateRelease(newClient, clientArray, data) || forceRelease(newClient, clientArray);
            }
            catch (TuningResourceContention e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("TuningResourceContention has occurred.");
                }
                // Start over!
                continue;
            }
            catch (NotOwnerException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("NetworkInterface unexpectedly released.");
                }
                // Start over!
                continue;
            }
        }
    }

    private boolean negotiateRelease(Client newClient, Client clientArray[], Object data)
            throws TuningResourceContention
    {
        ExtendedNetworkInterface targetInterface = null;
        Client releasingClient = requestRelease(clientArray, newClient, data);

        if (log.isDebugEnabled())
        {
            log.debug("negotiateRelease - releasingClient = " + releasingClient);
        }

        if (releasingClient != null)
        {
            // Found a releasing client - first attempt to get the NI associated
            // with
            // the releasing client. If that NI is not null - the client didn't
            // release
            // the NI but returned true.
            for (int i = 0; i < clientArray.length; i++)
            {
                if (releasingClient.equals(clientArray[i]))
                {
                    targetInterface = getReservedNetworkInterface(clientArray[i]);
                    if (log.isDebugEnabled())
                    {
                        log.debug("negotiateRelease - found target client - NI = " + targetInterface);
                    }
                    break;
                }
            }

            if (targetInterface == null)
            {
                // NI is null, so find the un-reserved NI and reserve it.
                for (int i = 0; i < networkInterfaces.length; i++)
                {
                    if (networkInterfaces[i].getReservationOwner() == null)
                    {
                        targetInterface = networkInterfaces[i];
                        if (((NetworkInterfaceImpl) targetInterface).requestReserve(null, newClient))
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("negotiateRelease - found unreservd NI - reserving SUCCESS ");
                            }
                            // reservation succeeded so register callback data
                            // if this is an
                            // explicit reservation
                            if (isExplicitReservation(newClient)) getData(newClient.context);
                            notifyResourceStatusListener(new NetworkInterfaceReservedEvent(targetInterface));
                            return true;
                        }
                    }
                }
            }
            else
            {
                // Just replace the NI reservation we found with the new one.
                if (((NetworkInterfaceImpl) targetInterface).requestReserve(releasingClient, newClient))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("negotiateRelease - returned true but no NI unreserved - just replace the client returned");
                    }
                    notifyResourceStatusListener(new NetworkInterfaceReleasedEvent(targetInterface));
                    notifyResourceStatusListener(new NetworkInterfaceReservedEvent(targetInterface));
                    return true;
                }
            }
        }
        return false;
    }

    // description in ExtendedNetworkInterfaceManagerImpl
    public void release(ResourceProxy proxy) throws NotOwnerException
    {
        // if the proxy is the current owner then release the networkInterface
        // if not, then throw an exception.
        // synchronized(reserveLock)
        // {
        for (int i = 0; i < networkInterfaces.length; i++)
        {
            final ExtendedNetworkInterface targetInterface = networkInterfaces[i];
            final Client owner = targetInterface.getReservationOwner();
            if (owner != null && owner.proxy == proxy)
            {
                targetInterface.release(owner.proxy);

                // We will perform this notification async - since we may be on
                // a
                // system thread that doesn't expect to have it's thread
                // re-entering
                // another part of the stack
                ccm.getSystemContext().runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        if (!offerAvailableResource(targetInterface))
                        {
                            // Only send the resource released event if
                            // the offers are rejected.
                            notifyResourceStatusListener(new NetworkInterfaceReleasedEvent(targetInterface));
                        }
                    }
                });
                return;
            }
        }
        // }
        // if proxy doesn't own the reservation on any interfaces, throw a
        // NotOwnerException.
        throw new NotOwnerException();
    }

    // description in ExtendedNetworkInterfaceManagerImpl
    public ExtendedNetworkInterface getReservedNetworkInterface(ResourceProxy proxy)
    {
        // synchronized(reserveLock)
        // {
        ExtendedNetworkInterface returnInterface = null;
        for (int i = 0; i < networkInterfaces.length; i++)
        {
            Client owner = networkInterfaces[i].getReservationOwner();
            if (owner != null && owner.proxy == proxy)
            {
                returnInterface = networkInterfaces[i];
                break;
            }
        }
        return returnInterface;
        // }
    }

    /**
     * This method implements the rules for <code>NetworkInterface</code>
     * reservations when an application exits. The rules are: <li>If a
     * <code>NetowrkInterface</code> is explicitly reserved by an application,
     * then the reservation is implicitly released when the application exits.
     * <li>If a <code>NetworkInterface</code> is implicitly reserved, the
     * reservation is maintained until granted to another application. The
     * effective priority for resource contention purposes is zero.
     * 
     * @param cc
     *            <code>CallerContext</code> of the application that exited.
     */
    private void updateReservations(CallerContext cc)
    {
        // if the proxy is the current owner then release the networkInterface
        // if not, then throw an exception.
        synchronized (lock)
        {
            for (int i = 0; i < networkInterfaces.length; i++)
            {
                ExtendedNetworkInterface targetInterface = networkInterfaces[i];
                Client owner = targetInterface.getReservationOwner();
                if (owner != null && owner.context == cc)
                {
                    if (owner.resusage instanceof ApplicationResourceUsage)
                    {
                        try
                        {
                            targetInterface.release(owner.proxy);
                            notifyResourceStatusListener(new NetworkInterfaceReleasedEvent(targetInterface));
                        }
                        catch (NotOwnerException e)
                        {
                            // do nothing
                        }
                    }
                    else if (owner.resusage instanceof ServiceContextResourceUsage)
                    {
                        // continue to hold reservation but priority should be 0
                        owner.resusage.setPriority(0);
                    }
                }
            }
        }
    }

    /**
     * Requests that the current resource owner, <i>oldClient</i>, release it's
     * reservation, and if possible takes the reservation for the new owner,
     * <i>newClient</i>.
     * 
     * @param oldClient
     *            the current owner
     * @param newClient
     *            the new owner
     * @return <code>true</code> if the current owner released the resource to
     *         the new owner
     * 
     * @throws TuningResourceContention
     *             if <i>oldClient</i> isn't the current owner at the time the
     *             reservation attempt is made
     */
    private Client requestRelease(Client oldClients[], Client newClient, Object data)
    {
        ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);

        // Ask each of the oldClients to release their reservation
        return rm.negotiateRelease(oldClients, data);
    }

    /**
     * Iterates over all the NetworkInterfaces and returns <code>true</code> if
     * the passed in client currently owns a reservation.
     * 
     * @param client
     *            find if this <code>Client</code> is currently a
     *            <code>NetworkInterface</code> owner.
     * @return <code>true</code> if the passed in client currently owns a
     *         <code>NetworkInterface</code>.
     */
    private ExtendedNetworkInterface getReservedNetworkInterface(Client client)
    {
        // synchronized(reserveLock)
        // {
        ExtendedNetworkInterface returnInterface = null;
        for (int i = 0; i < networkInterfaces.length; i++)
        {
            if (client.equals(networkInterfaces[i].getReservationOwner())) returnInterface = networkInterfaces[i];
        }
        return returnInterface;
        // }
    }

    /**
     * Attempts to force the current owner, <i>oldClient</i>, to release the
     * resource, if the requestor, <i>newClient</i>, has higher priority. Calls
     * the {@link ResourceManager#prioritizeContention} method to sort the
     * owners by priority. Based on the priorities returned:
     * <ol>
     * <li>If <i>newClient</i> has higher priority than <i>oldClient</i>, then
     * <i>newClient</i> gains the reservation.
     * <li>If <i>oldClient</i> is not specified, then it loses its reservation.
     * <li>If <i>newClient</i> is not specified, then it doesn't gain the
     * reservation.
     * </ol>
     * 
     * @param oldClient
     *            the current owner
     * @param newClient
     *            the new owner
     * @return <code>true</code> if the new owner was able to reserve the
     *         resource; <code>false</code> otherwise.
     * 
     * @throws TuningResourceContention
     *             if <i>oldClient</i> isn't the current owner at the time the
     *             reservation attempt is made
     */
    private boolean forceRelease(Client newClient, Client clientArray[]) throws TuningResourceContention
    {
        boolean found;

        ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);
        // we have contention...
        // Ask manager to prioritize
        Client priority[] = rm.prioritizeContention(newClient, clientArray);

        // first find if each of the owners is actually in the prioritized list
        // if not, then their NetworkInterface reservation should be revoked and
        // we
        // should start over if newClient is supposed to reserve an interface.
        boolean interfaceReleased = false;
        for (int i = 0; i < clientArray.length; i++)
        {
            found = false;
            for (int j = 0; j < priority.length; j++)
            {
                if (clientArray[i].equals(priority[j])) found = true;
            }

            if (!found)
            {
                // clientArray[i] was not found in the prioritized list, so the
                // interface owned by that client should be released.
                ExtendedNetworkInterface exni = getReservedNetworkInterface(clientArray[i]);
                if (exni != null)
                {
                    ((NetworkInterfaceImpl) exni).forceReserve(clientArray[i], null);
                    notifyResourceStatusListener(new NetworkInterfaceReleasedEvent(exni));
                    interfaceReleased = true;
                }
                else
                {
                    // something changed as clientArray[i] is not an owner
                    throw new TuningResourceContention();
                }
            }
        }

        // if we free up any NetworkInterfaces, we may be able to allocate it
        // to newClient if newClient is in the top n positions in the
        // prioritized list
        // where n is the number of NetworkInterfaces
        if (interfaceReleased)
        {
            for (int i = 0; i < clientArray.length && i < priority.length; i++)
            {
                if (priority[i].equals(newClient))
                {
                    // jumps back to reserve() or reserveFor() and newClient
                    // will
                    // get the reservation in those functions
                    throw new TuningResourceContention();
                }
            }
        }

        // look through the top n positions in the prioritized array. (n =
        // number of interfaces)
        // newClient gets a reservation if it is in that list
        found = false;
        for (int i = 0; i < clientArray.length && i < priority.length; i++)
        {
            if (priority[i].equals(newClient))
            {
                found = true;
                break;
            }
        }

        // newClient was found so try to reserve it a NetworkInterface
        if (found)
        {
            return reallocateNetworkInterface(newClient, clientArray, priority);
        }

        // newClient doesn't get an interface
        return false;
    }

    private boolean reallocateNetworkInterface(Client client, Client currentOwners[], Client priority[])
            throws TuningResourceContention
    {
        Client oldClient = null;
        ExtendedNetworkInterface targetInterface = null;

        // find the lowest priority owner. This owner will have their
        // reservation
        // revoked and the network interface will be allocated to client.
        for (int i = priority.length - 1; i >= 0; i--)
        {
            if ((targetInterface = getReservedNetworkInterface(priority[i])) != null)
            {
                oldClient = priority[i];
                break;
            }
        }

        if (oldClient == null)
        {
            // this shouldn't happen, but no one in the priority array owns a
            // network interface.
            throw new TuningResourceContention();
        }

        // If newClient is highest priority, then it gets the resource
        // If oldClient is highest priority, then it keeps the resource

        // Find newClient
        int newPriority;
        for (newPriority = 0; newPriority < priority.length; ++newPriority)
            if (client.equals(priority[newPriority])) break;
        // Find oldClient
        int oldPriority;
        for (oldPriority = 0; oldPriority < priority.length; ++oldPriority)
            if (oldClient.equals(priority[oldPriority])) break;

        // New owner is either newClient or nobody
        Client owner = (newPriority < priority.length && newPriority < oldPriority) ? client : null;

        // Force release
        if (oldPriority >= priority.length // oldClient not in prioritized list
                || owner != null) // newClient has priority
        {
            if (((NetworkInterfaceImpl) targetInterface).forceReserve(oldClient, client))
            {
                notifyResourceStatusListener(new NetworkInterfaceReleasedEvent(targetInterface));
                notifyResourceStatusListener(new NetworkInterfaceReservedEvent(targetInterface));
                return true;
            }
        }
        return false;
    }

    /**
     * Cleans up after a CallerContext is destroyed, forgetting any listeners
     * previously associated with it.
     * 
     * @param ctx
     *            the context to forget
     */
    private void cleanup(CallerContext ctx)
    {
        synchronized (lock)
        {
            // Remove ctx from the set of contexts with listeners
            rsContexts = Multicaster.remove(rsContexts, ctx);
        }

    }

    /**
     * Returns </code>true</code> if the reserving Client is an explicit
     * reservation. e.g. <code>reseve()</code> or <code>reserveFor()</code> is
     * called directly by the application.
     * 
     * @param client
     *            <code>Client</code> reserving the NetworkInterface.
     * @return <code>true</code> if this is an explicit reservation, otherwise
     *         <code>false</code>.
     */
    private boolean isExplicitReservation(Client client)
    {
        return client.resusage instanceof ApplicationResourceUsage;
    }

    /**
     * Access this device's global data object associated with current context.
     * If none is assigned, then one is created.
     * <p>
     * Synchronizes on the internal object {@link #lock}.
     * 
     * @param ctx
     *            the context to access
     * @return the <code>Data</code> object
     */
    private Data getData(CallerContext ctx)
    {
        synchronized (lock)
        {
            Data data = (Data) ctx.getCallbackData(this);
            if (data == null)
            {
                data = new Data();
                ctx.addCallbackData(data, this);
            }
            return data;
        }
    }

    /**
     * Holds context-specific data. Specifically the set of
     * <code>ResourceStatusListener</code>s.
     */
    private class Data implements CallbackData
    {
        public ResourceStatusListener rsListeners = null;

        public void destroy(CallerContext ctx)
        {
            updateReservations(ctx);
            cleanup(ctx);
        }

        public void active(CallerContext ctx)
        {
        }

        public void pause(CallerContext ctx)
        {
        }
    }

    // private Object reserveLock = new Object();

    /**
     * Multicaster for ResourceStatusListener.
     */
    private CallerContext rsContexts;

    /**
     * Reference to the CallerContextManager singleton.
     */
    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * Private lock.
     */
    private Object lock = new Object();

    /**
     * Native method to get the number of tuners available on the system
     * 
     * @return number of tuners
     */
    private native int getNumberOfTuners();

    /**
     * List of NetworkInterfaces.
     */
    protected ExtendedNetworkInterface[] networkInterfaces = null;

    /**
     * Singleton instance of NetworkInterfaceManager.
     */
    private static NetworkInterfaceManager instance = new NetworkInterfaceManagerImpl();

    /**
     * Collection to hold the ResourceOfferListeners.
     */
    protected ArrayList offerListeners = new ArrayList();

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }

    public ExtendedNetworkInterface findNetworkInterface(Locator locator, CallerContext cc)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
