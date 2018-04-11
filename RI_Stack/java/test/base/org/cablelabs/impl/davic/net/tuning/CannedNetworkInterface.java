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
import java.util.LinkedList;

import javax.tv.service.Service;

import org.davic.mpeg.TransportStream;
import org.davic.net.Locator;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NotOwnerException;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.application.AppID;
import org.dvb.spi.selection.SelectionSession;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceUsage;

import org.cablelabs.impl.davic.mpeg.TransportStreamExt;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ResourceManager.Client;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.service.javatv.transport.TransportStreamImpl;

/**
 * An implementation of the <code>NetworkInterface</code> with canned behavior.
 * This allows canned testing of anything that depends on the network interface.
 * 
 * @author Todd Earles
 */
public class CannedNetworkInterface extends ExtendedNetworkInterface
{
    private CannedSIDatabase sidb;

    /**
     * Construct a <code>NetworkInterface</code> with the given native tuner
     * handle.
     * 
     * @param tunerHandle
     *            The native tuner handle.
     */
    public CannedNetworkInterface(int tunerHandle)
    {
        this.tunerHandle = tunerHandle;
        listeners = new LinkedList();
        callbacks = new LinkedList();

        resclient = new DummyResourceClient();
        proxy = new DummyResourceProxy(resclient);
        usage = new ResourceUsageImpl(null, -1);
        dummyClient = new Client(resclient, proxy, usage,
                ((CallerContextManager) ManagerManager.getInstance(CallerContextManager.class)).getCurrentContext());
        sidb = (CannedSIDatabase) ((ServiceManager) ManagerManager.getInstance(ServiceManager.class)).getSIDatabase();
        TransportStream ts = sidb.transportStream7.getDavicTransportStream(this);
        cannedSetCurrentTransportStream(ts);
    }

    // Description copied from ExtendedNetworkInterface
    public boolean reserve(Client client)
    {

        switch (failReserveCounter)
        {
            case FAIL_BOTH:
            {
                return false;
            }
            case FAIL_FIRST:
            {
                if (counter == 0)
                {
                    counter++;
                    ResourceClient c = new DummyResourceClient();
                    this.client = dummyClient;
                    return false;
                }
                else
                {
                    this.client = client;
                    reserveCount++;
                    return true;
                }

            }
            case FAIL_SECOND:
            {
                if (counter == 1)
                {
                    this.client = dummyClient;
                    return false;
                }
                counter++;
            }
            default:
            {
                this.client = client;
                reserveCount++;
                return true;
            }
        }
    }

    // Description copied from ExtendedNetworkInterface
    public void release(ResourceProxy proxy) throws NotOwnerException
    {
        // if(client.proxy != proxy)
        // throw new NotOwnerException();

        // Mark this NI as not reserved
        client = null;

        ctx = null;
        reserveCount--;
    }

    // Description copied from ExtendedNetworkInterface
    public boolean forceReserve(Client oldOwner, Client newOwner) throws TuningResourceContention
    {
        // Not currently part of the canned environment.
        throw new UnsupportedOperationException();
    }

    // Description copied from ExtendedNetworkInterface
    public boolean requestReserve(Client oldOwner, Client newOwner) throws TuningResourceContention
    {
        // Not currently part of the canned environment.
        throw new UnsupportedOperationException();
    }

    // Description copied from ExtendedNetworkInterface
    public Client getReservationOwner()
    {
        return client;
    }

    // Description copied from ExtendedNetworkInterface
    public CallerContext getOwnerContext()
    {
        return client.context;
    }

    // Description copied from ExtendedNetworkInterface
    public Object tune(final org.davic.net.Locator davicLocator, ResourceProxy rproxy, Object tuneCookie) throws NotOwnerException
    {
        try
        {
            OcapLocator locator = (OcapLocator) davicLocator;
            ServiceHandle serviceHandle = sidb.getServiceBySourceID(locator.getSourceID());
            ServiceExt service = sidb.createService(serviceHandle);
            ServiceDetailsExt serviceDetails = ((ServiceDetailsExt) service.getDetails());
            TransportStream ts = ((TransportStreamImpl) serviceDetails.getTransportStream()).getDavicTransportStream(this);
            return tune(ts, rproxy, null);
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
            throw new NotOwnerException("Could not extract transport stream from locator");
        }
    }

    // Description copied from ExtendedNetworkInterface
    public Object tune(final TransportStream ts, ResourceProxy proxy, Object tuneCookie) throws NotOwnerException
    {
        if (failTuneCounter > 0)
        {
            failTuneCounter--;
            throw new NotOwnerException();
        }
        if (failTuneCounter == -1)
        {
            failTuneCounter = 0;
        }

        cannedSetCurrentTransportStream(ts);
        if (stall == STALL_BEFORE_TUNING)
        {
            stall = 0;
            return new Object();
        }
        else if (stall == STALL_WHILE_TUNING)
        {
            cannedSendEvent(new ExtendedNetworkInterfaceTuningEvent(this, client.proxy));
            stall = 0;
            return new Object();
        }
        else if (stall == STALL_BOTH)
        {
            stall--;
            return new Object();
        }
        cannedSendEvent(new ExtendedNetworkInterfaceTuningEvent(this, client.proxy));
        cannedSendEvent(new ExtendedNetworkInterfaceTuningOverEvent(this,
                ExtendedNetworkInterfaceTuningOverEvent.SUCCEEDED, client.proxy));
        return new Object();
    }

    public int getTransportStreamFrequency()
    {
        if (ts != null) return ((TransportStreamExt) ts).getFrequency();

        return -1;
    }

    public SelectionSession getCurrentSelectionSession()
    {
        return ss;
    }

    public void cannedSetCurrentSelectionSession(SelectionSession ss)
    {
        this.ss = ss;
    }

    // Description copied from ExtendedNetworkInterface
    public ResourceUsageImpl getResourceUsage()
    {
        return (client != null ? client.resusage : null);
    }


    public ArrayList getResourceUsages()
    {
        return new ArrayList();
    }
    // Description copied from NativeHandle
    public int getHandle()
    {
        return tunerHandle;
    }

    // Description copied from NetworkInterface
    public void addNetworkInterfaceListener(NetworkInterfaceListener nil)
    {
        if (!listeners.contains(nil)) listeners.add(nil);
    }

    // Description copied from NetworkInterface
    public void removeNetworkInterfaceListener(NetworkInterfaceListener nil)
    {
        listeners.remove(nil);
    }

    // Description copied from NetworkInterface
    public synchronized boolean isReserved()
    {
        return (client != null);
    }

    public TransportStream getCurrentTransportStream()
    {
        return ts;
    }

    /**
     * Sends an event to all of the listeners using the current context.
     * 
     * @param event
     *            The event to send to the listeners.
     */
    public void cannedSendEvent(final NetworkInterfaceEvent event)
    {
        CallerContextManager ccManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext ctx = ccManager.getCurrentContext();
        ctx.runInContext(new Runnable()
        {
            public void run()
            {
                NetworkInterfaceListener[] nilArray = (NetworkInterfaceListener[]) listeners.toArray(new NetworkInterfaceListener[] {});
                for (int i = 0; i < nilArray.length; i++)
                {
                    nilArray[i].receiveNIEvent(event);
                }
            }
        });
    }

    /**
     * Sets all of the failure values and the stall value to default or zero.
     */
    public void cannedResetFailures()
    {
        failReserveCounter = 0;
        failTuneCounter = 0;
        stall = 0;
    }

    /**
     * Set the point at which the reserve fails. Possible values are:<br />
     * <code.CannedNetworkInterface.FAIL_FIRST</code> - Fail the first time
     * through a selection attempt.<br />
     * <code>CannedNetworkInterface.FAIL_SECOND</code> - Fail the second time
     * through a selection attempt (i.e. when the ServiceContext is attempting
     * to fall back to the previous service.) Note: this has no effect if a
     * service was not previously selected.<br />
     * <code>CannedNetworkInterface.FAIL_BOTH</code> - Fail both times through a
     * selection attempt. Same effect as using <code>FAIL_FIRST</code> if no
     * previous service was selected.<br />
     * 
     * @param count
     */
    public void cannedSetFailReserve(int count)
    {
        failReserveCounter = count;
        if (count == FAIL_BOTH || count == FAIL_FIRST) client = dummyClient;
        counter = 0;
    }

    /**
     * Set the point at which the tune fails. Possible values are:<br />
     * <code.CannedNetworkInterface.FAIL_FIRST</code> - Fail the first time
     * through a selection attempt.<br />
     * <code>CannedNetworkInterface.FAIL_SECOND</code> - Fail the second time
     * through a selection attempt (i.e. when the ServiceContext is attempting
     * to fall back to the previous service.) Note: this has no effect if a
     * service was not previously selected.<br />
     * <code>CannedNetworkInterface.FAIL_BOTH</code> - Fail both times through a
     * selection attempt. Same effect as using <code>FAIL_FIRST</code> if no
     * previous service was selected.<br />
     * 
     * @param count
     *            The point at which to cause a failure
     */
    public void cannedSetFailTune(int count)
    {
        failTuneCounter = count;
    }

    /**
     * Sets the flag for forcing a stall in the event sending for a tune. Valid
     * values are <code>CannedNetworkInterface.STALL_BEFORE_TUNING</code> and
     * <code>CannedNetworkInterface.STALL_WHILE_TUNING</code>
     * 
     * @param when
     *            The step in tuning to stall the event delivery.
     */
    public void cannedStallTune(int when)
    {
        stall = when;
    }

    public int cannedGetReserveCount()
    {
        return reserveCount;
    }

    /**
     * Steals the tuner away from the ServiceContext that currently owns the
     * ResourceClient.
     */
    public void cannedStealTuner()
    {
        Client temp = client;
        if (!temp.requestRelease(null))
        {
            temp.release();
            temp.notifyRelease();
        }
    }

    public int cannedGetListenerCount()
    {
        return listeners.size();
    }

    public void cannedSetCurrentTransportStream(TransportStream ts)
    {
        this.ts = ts;
    }

    public LinkedList cannedGetCallbacks()
    {
        return callbacks;
    }

    public void cannedClearCallbacks()
    {
        callbacks.clear();
    }

    public Client cannedGetClient()
    {
        return client;
    }

    private int tunerHandle;

    private LinkedList listeners;

    private Client client;

    private Client dummyClient;

    private ResourceProxy proxy;

    private ResourceClient resclient;

    private ResourceUsageImpl usage;

    private CallerContext ctx;

    private int reserveCount = 0;

    private int failReserveCounter = 0;

    private int failTuneCounter = 0;

    private int stall = 0;

    private int counter;

    private TransportStream ts;

    private LinkedList callbacks;

    private SelectionSession ss;

    public static final int STALL_BEFORE_TUNING = 1;

    public static final int STALL_WHILE_TUNING = 2;

    public static final int STALL_BOTH = 3;

    public static final int FAIL_FIRST = 1;

    public static final int FAIL_SECOND = -1;

    public static final int FAIL_BOTH = 2;

    public boolean transferReservation(Client oldOwner, Client newOwner)
    {
        /*
         * this.proxy = newOwner.proxy; this.client = newOwner.client;
         * this.usage = newOwner.resusage; this.ctx = newOwner.context;
         */
        return true;
    }

    public void addNetworkInterfaceCallback(NetworkInterfaceCallback callback, int priority)
    {
        if (!callbacks.contains(callback)) callbacks.add(callback);
    }

    public void removeNetworkInterfaceCallback(NetworkInterfaceCallback callback)
    {
        callbacks.remove(callback);
    }
    /*
    public boolean isTuning()
    {
        return false;
    }

    public boolean isSynced()
    {
        return true;
    }

    public boolean isTuned()
    {
        return true;
    }
    */

    public Object getCurrentTuneToken() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isTuning(Object tuneReq) throws NotOwnerException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isTuned(Object tuneReq) throws NotOwnerException {
        // TODO Auto-generated method stub
        return true;
    }

    public boolean isSynced(Object tuneReq) throws NotOwnerException {
        // TODO Auto-generated method stub
        return true;
    }

    private class DummyResourceProxy implements ResourceProxy
    {
        ResourceClient resclient;

        public DummyResourceProxy(ResourceClient client)
        {
            resclient = client;
        }

        public ResourceClient getClient()
        {
            return resclient;
        }

    }

    private class DummyResourceClient implements ResourceClient
    {

        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            return false;
        }

        public void release(ResourceProxy proxy)
        {
        }

        public void notifyRelease(ResourceProxy proxy)
        {
        }

    }

    private class DummyResourceUsage implements ResourceUsage
    {

        public CallerContext getContext()
        {
            return ((CallerContextManager) ManagerManager.getInstance(CallerContextManager.class)).getSystemContext();
        }

        public int getPriority()
        {
            return 256;
        }

        public boolean set(String type, ResourceProxy proxy)
        {
            // TODO (Josh) Implement
            return false;
        }

        public boolean set(ResourceProxy proxy, boolean reserve)
        {
            // TODO (Josh) Implement
            return false;
        }

        public boolean remove(ResourceProxy proxy)
        {
            // TODO (Josh) Implement
            return false;
        }

        public ResourceUsage getResourceUsage()
        {
            return this;
        }

        public boolean isEquals(ResourceUsageImpl usage)
        {
            if (this == (ResourceUsage) usage) return true;
            return false;
        }

        public boolean isReserved(ResourceProxy proxy)
        {
            // TODO (Josh) Implement
            return false;
        }

        public AppID getAppID()
        {
            // TODO (Josh) Implement
            return null;
        }

        public String[] getResourceNames()
        {
            // TODO (Josh) Implement
            return null;
        }

        public ResourceProxy getResource(String resourceName)
        {
            // TODO (Josh) Implement
            return null;
        }

        public void setPriority(int priority)
        {
            // do nothing for now
        }
    }

    public Object tune(Service service, ResourceProxy proxy, Object tuneCookie)
            throws NetworkInterfaceException {
        // TODO Auto-generated method stub
        return null;
    }
}
