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
import java.util.Enumeration;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.LocatorFactory;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.cablelabs.debug.Profile;
import org.cablelabs.impl.davic.mpeg.TransportStreamExt;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContext.Multicaster;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ResourceManager.Client;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceDetailsCallback;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.spi.SPIServiceDetails;
import org.cablelabs.impl.spi.ProviderInstance.SelectionSessionWrapper;
import org.cablelabs.impl.util.CallbackList;
import org.cablelabs.impl.util.EventCallback;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.LocatorFactoryImpl;
import org.cablelabs.impl.util.LocatorUtil;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;
import org.davic.mpeg.TransportStream;
import org.davic.net.Locator;
import org.davic.net.tuning.DeliverySystemType;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceTuningEvent;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.net.tuning.NotOwnerException;
import org.davic.net.tuning.StreamNotFoundException;
import org.davic.net.tuning.StreamTable;
import org.davic.resources.ResourceProxy;
import org.dvb.spi.selection.KnownServiceReference;
import org.dvb.spi.selection.SelectionSession;
import org.dvb.spi.selection.ServiceReference;
import org.ocap.net.OcapLocator;
import org.ocap.service.ServiceContextResourceUsage;

/**
 * Objects of this class represent physical network interfaces that can be used
 * for receiving broadcast transport streams.
 * 
 * @author Jason Subbert
 * @author Todd Earles (added re-tune support)
 */
public class NetworkInterfaceImpl extends ExtendedNetworkInterface
{
    private static final Logger performanceLog = Logger.getLogger("Performance.Tuning");
    /**
     * The native tuner handle, assigned by NetworkInterfaceManagerImpl at
     * construction.
     */
    private int tunerHandle;

    // for profiling
    private int channelTuneNativeTune = -1;

    /** An SIManager instance */
    private final SIManagerExt siManager = (SIManagerExt) SIManager.createInstance();

    /** Non-null if currently notifying callbacks on this thread */
    private ThreadLocal notifyingCallbacks = new ThreadLocal();

    // The amount of time to wait for a tune to complete.
    private final static int TIMEOUT_DEFAULT = 3000; // Per OCAP, tunes should
                                                     // take less than 3
                                                     // seconds.

    private final static String TIMEOUT = "OCAP.networkinterface.timeout";

    // Extract the timeout value from the mpeenv.ini, if specified.
    private final static int TUNER_DELAY = MPEEnv.getEnv(TIMEOUT, TIMEOUT_DEFAULT);
    
    private final String m_logPrefix;

    /**
     * Construct a NetworkInterfaceImpl for the specified native tuner handle
     * (tuner number).
     * 
     * @param tunerHandle
     *            the native
     */
    protected NetworkInterfaceImpl(int tunerHandle)
    {
        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        this.tunerHandle = tunerHandle;
        m_logPrefix = "Tuner " + tunerHandle + ": ";
    }

    /**
     * Returns the transport stream to which the network Interface is currently
     * tuned. Returns null if the network interface is not currently tuned to a
     * transport stream, e.g. because it is performing a tune action.
     * 
     * @return Transport stream to which the network interface is currently
     *         tuned
     */
    public TransportStream getCurrentTransportStream()
    {
        return stateMachine.getCurrentTransportStream();
    }

    /**
     * Returns the Locator of the transport stream to which the network
     * interface is connected. Returns null if the network interface is not
     * currently tuned to a transport stream.
     * 
     * @return Locator of the transport stream to which the network interface is
     *         tuned
     */
    public Locator getLocator()
    {
        return stateMachine.getCurrentLocator();
    }

    /**
     * @return true, if the network interface is reserved, otherwise false
     */
    public boolean isReserved()
    {
        final boolean reserved = reserve.getOwner() != null;
        if (log.isDebugEnabled()) 
        {
            log.debug(m_logPrefix + "isReserved: " + reserved);
        }
        return reserved;
    }

    /**
     * @return true, if the network interface is local (i.e. embedded in the
     *         receiver), otherwise false
     */
    public boolean isLocal()
    {
        return true;
    }

    /**
     * Lists the known transport streams that are accessible through this
     * network interface. If there are no such streams, returns an array with
     * length of zero.
     * 
     * @return array of transport streams accessible through this network
     *         interface
     */
    public TransportStream[] listAccessibleTransportStreams()
    {
        // TODO: this code should be commented in when there is a way to
        // get all the accessible TransportStreams.
        /*
         * int ids[] = getAllTransportStreamIds(); if (ids == null) { return
         * null; }
         * 
         * TransportStream[] streams = new TransportStream[ids.length];
         * 
         * for (int i = 0; i < ids.length; i++) { streams[i] = new
         * TransportStreamImpl(ids[i]); } return streams;
         */
        return new TransportStream[0];
    }

    /**
     * This method returns the type of the delivery system that this network
     * interface is connected to.
     * 
     * @return delivery system type
     */
    public int getDeliverySystemType()
    {
        return DeliverySystemType.CABLE_DELIVERY_SYSTEM;
    }

    /**
     * Adds a <code>NetworkInterfaceListener</code> for network interface
     * events. If the listener is already a registered listener, the listener is
     * added to the list again.
     * 
     * @param listener
     *            listener object to be registered to receive network interface
     *            events
     */
    public void addNetworkInterfaceListener(NetworkInterfaceListener listener)
    {
        addNetworkInterfaceListener(listener, ccm.getCurrentContext());
    }

    /**
     * Removes a registered listener of <code>NetworkInterface</code> events.
     * 
     * @param listener
     *            listener object to be removed so that it will not receive
     *            network interface events in future
     */
    public void removeNetworkInterfaceListener(NetworkInterfaceListener listener)
    {
        removeNetworkInterfaceListener(listener, ccm.getCurrentContext());
    }

    // Description copied from ExtendedNetworkInterface
    public int getTransportStreamFrequency()
    {
        return stateMachine.getTransportStreamFrequency();
    }

    // Description copied from ExtendedNetworkInterface
    public SelectionSession getCurrentSelectionSession()
    {
        return ( (stateMachine.tunedTSA == null)
                 ? null
                 : stateMachine.tunedTSA.session);
    }

    public Object getCurrentTuneToken()
    {        
        return (stateMachine.tuneRequest == null) ? null : stateMachine.tuneRequest.getToken();
    }
    
    /**
     * Find the <code>NetworkInterface</code> that is currently tuned to the
     * <code>Service</code> specified by the given <code>Locator</code>.
     * 
     * @param l
     *            A locator identifying the service being searched for.
     * @param enforceContext
     *            true if the <code>CallerContext</code> should be matched when
     *            searching for a <code>NetworkInterface</code>, false otherwise
     * @return The network interface currently tuned to the given service.
     */
    static public NetworkInterface getTunedNetworkInterface(Locator l, boolean enforceContext)
    {
        if (l == null) return null;

        // The network interface that is tuned to the specified service.
        NetworkInterface niForService = null;

        try
        {
            // Get the array of transport streams that carry the service
            TransportStream ts[] = StreamTable.getTransportStreams(l);

            // Find a network interface that is tuned to one of the transport
            // streams carrying the service.
            NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
            NetworkInterface ni[] = nim.getNetworkInterfaces();

            for (int i = 0; (i < ni.length) && (niForService == null); i++)
            {
                NetworkInterfaceImpl nwif = (NetworkInterfaceImpl) ni[i];

                // Get the transport stream
                TransportStream nits = nwif.getCurrentTransportStream();
                if (nits == null) continue;

                // Is the transport stream tuned by this network interface the
                // one
                // we are looking for?
                for (int j = 0; j < ts.length; j++)
                {
                    if (nits.equals(ts[j]))
                    {
                        if (enforceContext)
                        {
                            // check if the NI is reserved
                            if (nwif.isReserved())
                            {
                                CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

                                if (nwif.getOwnerContext() == ccm.getCurrentContext()
                                        && !(nwif.getResourceUsage().getResourceUsage() instanceof ServiceContextResourceUsage))
                                    niForService = nwif;
                            }
                            else
                                niForService = nwif;
                        }
                        else
                            niForService = nwif;
                    }
                }
            }
        }
        catch (NetworkInterfaceException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Could not get TransportStreams");
            }
        }
        return niForService;
    }

    /**
     * Add an <code>NetworkInterfaceListener</code> to this device for the given
     * calling context.
     * 
     * @param listener
     *            the <code>NetworkInterfaceListener</code> to be added.
     * @param context
     *            the context of the application installing the listener
     */
    private void addNetworkInterfaceListener(NetworkInterfaceListener listener, CallerContext context)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = getData(context);

            // Update listener/multicaster
            data.niListeners = EventMulticaster.add(data.niListeners, listener);

            // Manage context/multicaster
            niContexts = Multicaster.add(niContexts, context);
        }
    }

    /**
     * Remove a <code>NetworkInterfaceListener</code> from this device for the
     * given calling context.
     * 
     * @param listener
     *            the <code>NetworkInterfaceListener</code> to be removed.
     * @param context
     *            the context of the application removing the listener
     */
    private void removeNetworkInterfaceListener(NetworkInterfaceListener listener, CallerContext context)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = getData(context);

            // Update listener/multicaster
            data.niListeners = EventMulticaster.remove(data.niListeners, listener);
        }
    }

    /**
     * Cause the applications database to <i>remove</i> all listeners associated
     * with this <code>CallerContext</code>. This is done simply by setting the
     * reference to <code>null</code> and letting the garbage collector take
     * care of the rest.
     * 
     * @param c
     *            the <code>CallerContext</code> to remove
     */
    private void removeListeners(CallerContext c)
    {
        synchronized (lock)
        {
            c.removeCallbackData(this);
            niContexts = Multicaster.remove(niContexts, c);
        }
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
     * Notify <code>NetworkInterfaceListener</code>s of changes to the resource
     * reservation status of this screen device. <i>This method is not part of
     * the defined public API, but is present for the implementation only.</i>
     * 
     * @param e
     *            the event to deliver
     */
    private void notifyNetworkInterfaceListeners(NetworkInterfaceEvent e)
    {
        final NetworkInterfaceEvent event = e;
        CallerContext contexts = niContexts;

        if (contexts != null)
        {
            Runnable run = new Runnable()
            {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();

                    // Listeners are maintained in-context
                    Data data = (Data) ctx.getCallbackData(NetworkInterfaceImpl.this);

                    NetworkInterfaceListener l = data.niListeners;
                    if (l != null) l.receiveNIEvent(event);
                }
            };
            contexts.runInContext(run);
        }
    }

    // Description copied from ExtendedNetworkInterface
    public boolean reserve(Client newClient)
    {
        Client oldClient = null;
        synchronized (reserve)
        {
            // None of the reserve.take() operations inside this
            // synchronized block should fail.
            oldClient = reserve.getOwner();

            // Attempt to acquire if nobody has it reserved
            // Or previous owner has "expired"
            if ((oldClient == null || !oldClient.context.isAlive()) && reserve.take(oldClient, newClient))
            {
                stateMachine.handleInterfaceReserved();
                return true;
            }

            // If already reserved
            if (oldClient.equals(newClient) && reserve.take(oldClient, newClient))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to take the reservation from oldOwner.
     * 
     * @param oldClient
     *            the current owner of the <code>NetworkInterface</code>
     *            reservation.
     * @param newClient
     *            new owner of the <code>NetworkInterface</code> reservation.
     * @return <code>true</code>
     * @throws TuningResourceContention
     *             thrown if oldOwner does not match the real owner.
     */
    boolean requestReserve(Client oldClient, Client newClient) throws TuningResourceContention
    {
        synchronized (reserve)
        {
            // Attempt to take the resource.
            // If that failed, START OVER.
            if (!reserve.take(oldClient, newClient))
            {
                throw new TuningResourceContention();
            }

            // change the interface state
            synchronized (stateMachine)
            {
                stateMachine.handleInterfaceReleased();
                stateMachine.handleInterfaceReserved();
            }

            // success
            return true;
        }
    }

    // Description copied from ExtendedNetworkInterface
    public void release(ResourceProxy owner) throws NotOwnerException
    {
        synchronized (reserve)
        {
            Client currOwner = reserve.getOwner();
            // If the current owner is this context and proxy,
            // then release the reservation
            if (currOwner != null && currOwner.proxy == owner)
            {
                // Give up the resource
                if (reserve.take(currOwner, null))
                {
                    stateMachine.handleInterfaceReleased();
                    return;
                }
            }
            else
            {
                throw new NotOwnerException("Proxy does not have the NetworkInterface reserved");
            }
        }
    }

    /**
     * Forces oldClient to give up the NetworkInterface reservation. Will call
     * <code>ResourceClient.release()</code> and
     * <code>ResourceClient.notifyRelease()</code>.
     * 
     * @param oldClient
     *            the current owner of the <code>NetworkInterface</code>
     *            reservation.
     * @param newClient
     *            new owner of the <code>NetworkInterface</code> reservation.
     * @return <code>true</code>
     * @throws TuningResourceContention
     *             thrown if oldOwner does not match the real owner.
     */
    boolean forceReserve(Client oldClient, Client newClient) throws TuningResourceContention
    {
        // Force release on the old client
        oldClient.release();

        synchronized (reserve)
        {
            // Attempt to take the resource.
            // If that failed, START OVER.
            if (!reserve.take(oldClient, newClient)) throw new TuningResourceContention();

            // Notify client that it has lost its reservation.
            oldClient.notifyRelease();

            synchronized (stateMachine)
            {
                // change state to released
                stateMachine.handleInterfaceReleased();

                if (newClient != null)
                {
                    // change state to reserved
                    stateMachine.handleInterfaceReserved();
                }
            }
        }
        return true;
    }

    /**
     * Notify <code>NetworkInterfaceListener</code>s of the start or completion
     * of a tune operation. <i>This method is not part of the defined public
     * API, but is present for the implementation only.</i>
     * 
     */
    private void notifyNetworkInterfaceEvent(NetworkInterfaceEvent evt)
    {
        if (evt instanceof NetworkInterfaceTuningEvent)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Notify listeners of tuning start");
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Notify listeners of tuning over with status = "
                        + (((NetworkInterfaceTuningOverEvent) evt).getStatus() == NetworkInterfaceTuningOverEvent.SUCCEEDED ? "SUCCESS"
                                : "FAILED"));
            }
        }

        notifyNetworkInterfaceListeners(evt);
    }

    // Description copied from ExtendedNetworkInterface
    public Client getReservationOwner()
    {
        return reserve.getOwner();
    }

    // Description copied from ExtendedNetworkInterface
    public Object tune(final Service service, ResourceProxy proxy, Object tuneToken) 
        throws NetworkInterfaceException
    {
        org.davic.net.Locator locator = null;
        try 
        {
            locator = LocatorUtil.convertJavaTVLocatorToOcapLocator(service.getLocator());
        } 
        catch (InvalidLocatorException e) 
        {
          throw new org.davic.net.tuning.IncorrectLocatorException("Invalid locator");
        } 
        return tune(locator, proxy, tuneToken);
    }
    
    // Description copied from ExtendedNetworkInterface
    public Object tune(final Locator locator, ResourceProxy proxy, Object tuneToken) 
        throws NetworkInterfaceException
    {
        checkCallbackNotificationState();

        // As specified by I16 clause 16.2.1.1
        if (!(locator instanceof OcapLocator))
        {
            throw new org.davic.net.tuning.IncorrectLocatorException("Must use an OcapLocator");
        }
        ServiceDetails[] sd = null;
        
        int programNumber = ((OcapLocator) locator).getProgramNumber();
        if (programNumber == -1)
        {
            try
            {
                sd = siManager.getServiceDetails(locator);
                if (sd.length >= 1) programNumber = ((ServiceDetailsExt) sd[0]).getProgramNumber();
            }
            catch (Exception e)
            {
            }
        }

        // Get the DAVIC transport stream to be tuned
        TransportStream ts = null;
        SelectionSession session = null;
        Locator spiLocator = null;
        Service service = null;       
        org.davic.net.Locator loc = null;    
        if (LocatorUtil.isService(locator))
        {
            try
            {
                service = siManager.getService(locator);
                if (service instanceof SPIService)
                {
                    loc = (org.davic.net.Locator) service.getLocator();

                    SPIService spiService = (SPIService) service;                    
                    ServiceReference ref = spiService.getServiceReference();
                    ProviderInstance spm = (ProviderInstance) ((SPIService) service).getProviderInstance();
                    session = spm.newSession(((SPIService) service).getServiceReference(), (SPIService)service);
                    if(ref instanceof KnownServiceReference)
                    {
                    	// If service reference is known, locator is also known
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "Service reference is (known): " + ref);
                        }
                        // When service reference is known, select does not need to be called
                        //session.select();
                    	spiLocator = LocatorUtil.convertJavaTVLocatorToOcapLocator(((KnownServiceReference)ref).getActualLocation());
                    }
                    else
                    {       
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "Service reference is (unknown): " + ref);
                        }
                        // select returns the actual locator for unknown
                        // service references
                        spiLocator = session.select();
                    }
                    session.selectionReady(); 
                    
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "spiLocator: " + spiLocator);
                    }       
                    // Get details here for so we can retrieve the programNumber for SPIService
                    // Program number is used during tuning to provide to SI layer for PMT acquisition 
                    // Also , we need the program number for comparison when re-map occurs
                    try
                    {
                        sd = siManager.getServiceDetails(spiLocator);
                        if (sd.length >= 1) programNumber = ((ServiceDetailsExt) sd[0]).getProgramNumber();
                    }
                    catch (Exception e)
                    {
                        // Continue.
                    }      
                }
                else 
                {
                    loc = ((LocatorFactoryImpl) LocatorFactory.getInstance()).transformServiceToLocator((ServiceExt) service);
                }

            }
            catch (InvalidLocatorException e)
            {
                // We'll swallow the exception and continue tuning in case it's a
                // TransportStream locator.
            }
        }
        else
        {
            // Transport stream locator
            loc = locator;
        }
       
        try
        {
            Locator newLocator = spiLocator != null ? spiLocator : loc;
            javax.tv.service.transport.TransportStream jts = siManager.getTransportStream(newLocator);
            if (jts != null) ts = (TransportStreamExt)((org.cablelabs.impl.service.TransportStreamExt) jts).getDavicTransportStream(this);                          
        }
        catch (Exception e)
        {
            ts = null;
        }

        // Handle transport stream not mapped
        if (ts == null)
        {
            if (session != null)
            {
                session.destroy();
                session = null;
            }
                
            throw new StreamNotFoundException("Transport stream not mapped");
        }

        // Tune to the transport stream
        return tune(ts, service, proxy, programNumber, session, tuneToken);
    }

    // Description copied from ExtendedNetworkInterface
    public Object tune(final TransportStream ts, final ResourceProxy proxy, Object tuneToken) 
        throws NetworkInterfaceException
    {
        checkCallbackNotificationState();
        return tune(ts, null, proxy, -1, null, tuneToken);
    }

    // Description copied from ExtendedNetworkInterface
    private Object tune( final TransportStream ts, final Service service, final ResourceProxy proxy, 
                         final int programNumber, final SelectionSession session, Object tuneToken) 
        throws NetworkInterfaceException
    {
        final Exception[] runException = new Exception[1];
        final TransportStreamAdapter tsa = new TransportStreamAdapter(ts, service, programNumber, proxy, session, this.tunerHandle);
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "NetworkInterfaceImpl tune tsa:" + tsa);
        }
        final TuneRequest req = new TuneRequest(tsa, proxy, false, false, programNumber, tuneToken);
        withReservation(proxy, new Runnable()
        {
            public void run()
            {
                // Tune to the specified transport stream
                try
                {
                    doTune(req);
                }
                catch (Exception e)
                {
                    runException[0] = e;
                }
            }
        });
        return req.getToken();
    }

    /**
     * Inform the state machine that a tune exists and needs to be run. This
     * method assumes the caller is already holding the NI reservation.
     * 
     * @param req
     *            The tune request
     */
    private void doTune(TuneRequest req) throws NetworkInterfaceException
    {
        // Check that the transport stream we are about to tune is for this NI
        if (req.tsa.currentTS.getNetworkInterface() != this)
            throw new StreamNotFoundException("Stream not tuneable on this network interface");
        
        stateMachine.handleTuneInitiate(req);
    }

    /**
     * Perform a native tune.
     * 
     * @param req
     *            The tune request.
     * @return Return true if the request started successfully, false if it
     *         failed.
     */
    private boolean doNativeTune(TuneRequest req)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Calling native tune with retune=" + req.isRetuneRequest());
        }

        if (Profile.TUNING_LL && Profile.isProfiling())
        {
            Profile.popWhere();
            if (channelTuneNativeTune == -1)
            {
                channelTuneNativeTune = Profile.addLabel("Java nativeTune");
            }
            Profile.setWhere(channelTuneNativeTune);
        }
        
        if (performanceLog.isInfoEnabled())
        {
                performanceLog.info("Tuning Initiated: Tuner " +  tunerHandle +
                        ", Locator " + req.tsa.getLocator());
        }
        
        if (!nativeTune(tunerHandle, req, req.tsa.currentTS.getFrequency(), req.programNumber,
                req.tsa.currentTS.getModulationFormat()))
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Native tune call FAILED");
            }
            // something happened in the native call that caused
            // the tune to fail. Cancel the request and generate
            // a TuningOver event with a status of FAILED.
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Used by subclasses to execute code while holding the device reservation.
     * If the device is reserved, then <code>Runnable.run()</code> will be
     * executed. If the device is not currently reserved by the caller, then an
     * <code>NotOwnerException</code> will be thrown.
     * <p>
     * Note that no calls should be made to <i>unknown</i> (e.g., user-installed
     * listeners) from within the <code>Runnable.run()</code> method, as this
     * could present a potential for deadlock.
     * <p>
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     * 
     * @param run
     *            the <code>Runnable</code> to execute
     * @throws NotOwnerException
     *             if the calling context does not have the device reserved
     */
    private void withReservation(ResourceProxy proxy, Runnable run) throws NotOwnerException
    {
        if (!reserve.runWith(proxy, run)) 
        {
            if (log.isInfoEnabled())
            {
                // This is rare, just log it.
                log.info(m_logPrefix + " Reservation is not current! NetworkInterface: " + this + ", tuneRequest: " + getCurrentTuneToken()
                                      + ", owner: " + getReservationOwner());
            }
            throw new NotOwnerException("network interface is not reserved");
        }
    }

    /**
     * This contains the list of {@link NetworkInterfaceCallback} instances that
     * are invoked by this {@link NetworkInterfaceImpl}.
     */
    private CallbackList callbacks = new CallbackList(NetworkInterfaceCallback.class);

    // Description copied from ExtendedNetworkInterface
    public void addNetworkInterfaceCallback(NetworkInterfaceCallback callback, int priority)
    {
        callbacks.addCallback(callback, priority);
    }

    // Description copied from ExtendedNetworkInterface
    public void removeNetworkInterfaceCallback(NetworkInterfaceCallback callback)
    {
        callbacks.removeCallback(callback);
    }

    // Description copied from ExtendedNetworkInterface
    public boolean isTuning(final Object tuneToken) throws NotOwnerException
    {
        return stateMachine.isTuned(tuneToken);
    }

    public boolean isTuned(final Object tuneToken) throws NotOwnerException
    {
        return stateMachine.isTuned(tuneToken);
    }

    public boolean isSynced(final Object tuneToken) throws NotOwnerException
    {
        return stateMachine.isSynced(tuneToken);
    }

    /**
     * Invoke callbacks
     */
    protected void invokeCallbacks(String methodName, Object tuneToken)
    {        
        try
        {
            notifyingCallbacks.set(callbacks);
            callbacks.invokeCallbacks(NetworkInterfaceCallback.class.getMethod(methodName, new Class[] {
                    ExtendedNetworkInterface.class, Object.class }), new Object[] { this, tuneToken });
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
        finally
        {
            notifyingCallbacks.set(null);
        }
    }

    /**
     * Invoke callbacks
     */
    protected void invokeCallbacks(String methodName, Object tuneToken, boolean success)
    {
        try
        {
            notifyingCallbacks.set(callbacks);
            callbacks.invokeCallbacks(NetworkInterfaceCallback.class.getMethod(methodName, new Class[] {
                    ExtendedNetworkInterface.class, Object.class, boolean.class }), 
                    new Object[] { this, tuneToken, new Boolean(success) });
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
        finally
        {
            notifyingCallbacks.set(null);
        }
    }

    /**
     * Invoke callbacks
     */
    protected void invokeCallbacks(String methodName, Object tuneToken, boolean success, boolean synced)
    {
        try
        {
            notifyingCallbacks.set(callbacks);
            callbacks.invokeCallbacks(NetworkInterfaceCallback.class.getMethod(methodName, new Class[] {
                    ExtendedNetworkInterface.class, Object.class, boolean.class, boolean.class }), new Object[] { this,
                    tuneToken, new Boolean(success), new Boolean(synced) });
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
        finally
        {
            notifyingCallbacks.set(null);
        }
    }

    protected void checkCallbackNotificationState()
    {
        if (notifyingCallbacks.get() != null)
            throw new IllegalStateException("Cannot mutate NI while it is notifying callbacks");
    }

    /**
     * Native function call to start a tune operation.
     * 
     * @param tunerId
     *            the native tuner id of this interface
     * @param listener
     *            the object to be notified of tuning events
     * @param frequency
     *            the frequency of the desired stream
     * @param programNum
     *            the program number inside the stream to tune
     * @param qam
     *            the QAM mode to use for the desired stream
     * @return <code>true</code> if the tuning request was successful or
     *         <code>false</code> if the request failed.
     */
    protected native boolean nativeTune(int tunerId, EDListener listener, int frequency, int programNum, int qam);

    /**
     * This class represents the current state of the NetworkInterface and
     * handles state transitions.
     * 
     */
    class NIStateMachine
    {
        /**
         * internal states of the NetworkInterface. These are default
         * accessibility for testing purposes.
         */

        /**
         * The tuner is not tuned (a tune has not been performed on this tuner, a tune failed,
         * or the tuner has been un-tuned)
         */
        final static int NOT_TUNED = 0;

        /**
         * A platform tune request has been performed but a TUNE_STARTED hasn't been received
         */
        final static int TUNING = 1;

        /**
         * A MPE_TUNE_STARTED has been received after a native tune
         */
        final static int TUNE_STARTED = 2;

        /**
         * The tune is complete but the tuner is not currently synced
         */
        final static int TUNED_UNSYNC = 3;

        /**
         * The tune is complete and the tuner is currently synced
         */
        final static int TUNED_SYNC = 4;

        /**
         * the current state of the interface
         */
        private int state = NOT_TUNED;

        /**
         * TVTimer to manipulate the timeout.
         */
        private TVTimer timer = TVTimer.getTimer();

        /**
         * the current <code>TransportStreamAdapter</code> this interface is
         * tuned to.
         */
        private volatile TransportStreamAdapter tunedTSA;

        // See {@link NetworkInterfaceImpl#getCurrentLocator} for method level
        // details
        Locator getCurrentLocator()
        {
            TransportStreamAdapter tsa = tunedTSA;
            return (tsa == null) ? null : tsa.getLocator();
        }

        // See {@link NetworkInterfaceImpl#getCurrentTransportStream} for method
        // level details
        TransportStream getCurrentTransportStream()
        {
            // Get currently tuned transport stream
            TransportStreamAdapter tsa = tunedTSA;
            TransportStreamExt ts = (tsa == null) ? null : tsa.currentTS;

            // Return if not tuned or if tuned and TSID is known
            if (ts != null && ts.getTransportStreamId() != -1)
            {
                return ts;
            }
            // If no TSA, return null. Would through NullPointerException in the
            // try block below anyhow.
            if (ts == null)
            {
                return ts;
            }

            // Look up latest version of the TS in case the TSID is now known.
            // If a new TS cannot be obtained just return the one we have.
            TransportStreamExt newTS = null;
            try
            {
                javax.tv.service.transport.TransportStream jts = siManager.getTransportStream(ts.getLocator());
                if (jts == null)
                    return ts;
                else
                {
                    newTS = (TransportStreamExt) ((org.cablelabs.impl.service.TransportStreamExt) jts).getDavicTransportStream(NetworkInterfaceImpl.this);
                }
            }
            catch (Exception e)
            {
                return ts;
            }

            // Update tuned TSA if still tuned there
            synchronized (this)
            {
                if (tunedTSA.currentTS == ts)
                {
                    tunedTSA.currentTS = newTS;
                }
            }

            return newTS;
        }

        // See {@link NetworkInterfaceImpl#getTransportStreamFrequency} for
        // method level details
        int getTransportStreamFrequency()
        {
            TransportStreamAdapter tsa = tunedTSA;
            TransportStream ts = (tsa == null) ? null : tsa.currentTS;
            if (ts != null) return ((TransportStreamExt) ts).getFrequency();

            return -1;
        }

        /**
         * Return true if the tune request associated with the provided tuneToken is 
         * tuning or is pending to be tuned
         * 
         * @throws NotOwnerException if the provided tuneToken is not associated with 
         * an active TuneRequest
         */
        synchronized boolean isTuning(final Object tuneToken) throws NotOwnerException
        {
            if ((this.tuneRequest != null) && (tuneToken == this.tuneRequest.getToken()))
            {
                return (state == TUNING);
            }
            else if ((getPendingRequestForToken(tuneToken)) != null)
            {
                return true; // We're in line to be tuning (but not tuned)
            }
            else 
            {
                throw new NotOwnerException("isTuning - Tune request is not current or pending");
            }
        }
        
        /**
         * Return true if the tune request associated with the provided tuneToken is 
         * tuned
         * 
         * @throws NotOwnerException if the provided tuneToken is not associated with 
         * an active TuneRequest
         */
        synchronized boolean isTuned(final Object tuneToken) throws NotOwnerException
        {
            if ((this.tuneRequest != null) && (tuneToken == this.tuneRequest.getToken()))
            {
                return (state == TUNED_UNSYNC || state == TUNED_SYNC);
            }
            else if ((getPendingRequestForToken(tuneToken)) != null)
            {
                return false; // We're in line to be tuning (but not tuned)
            }
            else 
            {
                throw new NotOwnerException("isTuned - Tune request is not current or pending");
            }
        }
        
        /**
         * Return true if the tune request associated with the provided tuneToken is
         * synchronized
         * 
         * @throws NotOwnerException if the provided tuneToken is not associated with 
         * an active TuneRequest
         */
        synchronized boolean isSynced(final Object tuneToken) throws NotOwnerException
        {
            if ((this.tuneRequest != null) && (tuneToken == this.tuneRequest.getToken()))
            {
                return (state == TUNED_SYNC);
            }
            else if ((getPendingRequestForToken(tuneToken)) != null)
            {
                return false; // We're in line to be tuning (but not tuned)
            }
            else 
            {
                throw new NotOwnerException("isSynced - Tune request is not current or pending");
            }
        }

        private TuneRequest getPendingRequestForToken(final Object tuneToken)
        {
            if ((pendingRequest != null) && (pendingRequest.getToken() == tuneToken))
            {
                return pendingRequest;
            }
            
            // Check the ignored list
            Enumeration e = ignoredRequests.elements();
            while (e.hasMoreElements())
            {
                TuneRequest req = (TuneRequest)e.nextElement();
                if (req.getToken() == tuneToken)
                {
                    return req; // We're in line to be tuning (but will ultimately fail)
                }
            }
            
            return null;
        }
        
        /**
         * Handles state transitions and event generation when a tune is
         * started.
         * 
         * @param request
         *            the tune request
         */
        synchronized void handleTuneInitiate(final TuneRequest request)
        {
            switch (state)
            {
                case TUNING:
                    // We can't initiate a new tune until the current request is acknowledged
                    //  So just save off the request to process when we can
                    enqueuePendingRequest(request);
                    break;
                case TUNED_UNSYNC:
                case TUNED_SYNC:
                case TUNE_STARTED:
                case NOT_TUNED:
                    // We can supersede the current tune
                    if (tuneRequest != null)
                    {
                        cancelTimer(tuneRequest);
                    }
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "handleTuneInitiate: tuning");
                    }
                    changeState(TUNING);
                    tuneRequest = request;
                    startTuning(tuneRequest);
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "handleTuneInitiate: Invalid state: " + state);
                    }
                    throw new IllegalStateException("Invalid transaction or state");
            }
        } // END handleTuneInitiate()

        /**
         * Handles state transitions and event generation when an untune is
         * started.
         * 
         * @param untuneRequest
         *            the untune request
         */
        synchronized void handleUntune(final TuneRequest untuneRequest)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "handleUntune in state " + statename(state));
            }
            
            switch (state)
            {
                case TUNING:
                    // We can't invalidate the current tune request until the current acknowledged
                    //  So just save off the fact until we can process it
                    enqueuePendingRequest(untuneRequest);
                    break;
                case TUNED_UNSYNC:
                case TUNED_SYNC:
                case TUNE_STARTED:
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "handleUntune: untuning ");
                    }
                    
                    untune(tuneRequest); // Untune the current request
                    tuneRequest = null;
                    break;
                case NOT_TUNED:
                    if (log.isInfoEnabled())
                    {
                        log.info(m_logPrefix + "handleUntune: Unexpected in state " + statename(state) + ". Ignoring.");
                    }
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "handleUntune: Invalid state: " + state);
                    }
                    throw new IllegalStateException("Invalid transaction or state");
            }
        } // END handleUntuneInitiate()

        /**
         * The current tune request which is in progress, or "complete".
         */
        private TuneRequest tuneRequest = null;

        /**
         * The next tune request that we'll actually deal with, if we're
         * currently tuning.
         */
        private TuneRequest pendingRequest = null;

        /**
         * A list of tune requests that we're going to ignore.
         */
        private Vector ignoredRequests = new Vector();

        /**
         * Handles state transitions and event generation when the
         * <code>NetworkInterface</code> is reserved.
         * 
         */
        synchronized void handleInterfaceReserved()
        {
            // Nothing to do. The fact that the interface's reservation state has changed
            //  doesn't affect the tuner state
        }

        /**
         * Handles state transitions and event generation when the
         * <code>NetworkInterface</code> is released.
         */
        synchronized void handleInterfaceReleased()
        {
            // Nothing to do. The fact that the interface's reservation state has changed
            //  doesn't affect the tuner state
        }

        /**
         * Handle a TimerTrigger event.
         * 
         * @param request the request that timed out
         */
        synchronized void handleTimerTrigger(TuneRequest request)
        {
            // Make sure the event is the same one we're working on.
            if (request != tuneRequest)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "handleTimerTrigger: Request timing out is not current: " + request);
                }
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "handleTimerTrigger in state " + statename(state));
            }
            
            switch (state)
            {
                case TUNING: 
                    // We issued a native but haven't received TUNE_STARTED in a timely fashion.
                    // Treat this as a failed tune...                    
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "handleTimerTrigger: Considering unacknowledged native tune failed");
                    }
                    resetTSA();
                    changeState(NOT_TUNED);
                    // Note: we set the state prior to notification in case the callbacks
                    //       re-enter the StateMachine holding our monitor
                    notifyNetworkInterfaceCallbacksTuneComplete(request, false, false);
                    notifyNIListenersTuneComplete(request, false, false);
                    tuneRequest = null;

                    if (pendingRequest != null)
                    { // Note: pendingRequest should always be null in TUNE_STARTED
                        // The tune that just signaled "failed" has been superceeded by 
                        //  later requests (which are now enqueued)
                        processIgnoredRequests();

                        // The pending request is now current
                        tuneRequest = pendingRequest;
                        pendingRequest = null;
                        
                        // Assert: pendingRequest is null, ignoredRequests is empty, 
                        //         tuneRequest is updated (was the last one enqueued)
                        if (tuneRequest.isUntuneRequest())
                        {
                            // We don't need to signal. The untuned tune was signaled in processIgnoredRequests
                            tuneRequest = null;
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(m_logPrefix + "handleTimerTrigger: Initiating pending tune");
                            }
                            startTuning(tuneRequest);
                        }
                    }
                    break;
                case TUNE_STARTED:
                    // We should have received a SYNC/UNSYNC in a timely fashion and it didn't come
                    // in. Treat this as a failed tune...
                    
                    changeState(TUNED_UNSYNC);
                    // Note: we set the state prior to notification in case the callbacks
                    //       re-enter the StateMachine to check the state, holding our monitor
                    notifyNetworkInterfaceCallbacksTuneComplete(request, true, false);
                    notifyNIListenersTuneComplete(request, true, false);
                    break;
                case TUNED_UNSYNC: // The timer is cancelled before going into these states
                case NOT_TUNED:
                case TUNED_SYNC:
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "handleTimerTrigger: Unexpected in state " + this.statename(state) + ". Ignoring.");
                    }
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "handleTimerTrigger: Invalid state: " + state);
                    }
                    throw new IllegalStateException("Invalid transaction or state");
            }
        } // END handleTimerTrigger

        /**
         * Handle state changes for a TUNED_UNSYNC event.
         * 
         * @param request
         */
        synchronized void handleTuneStarted(final TuneRequest request)
        {
            // Make sure the event is the same one we're working on.
            if (request != tuneRequest)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "handleTuneStarted: Started request is not current: " + request);
                }
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "handleTuneStarted in state " + statename(state));
            }

            switch (state)
            {
                case TUNING:
                    // If any tune requests stacked up, this is the time to process them
                    if (pendingRequest != null)
                    { // Note: This essentially constitutes a substate of TUNING
                        // The tune that just signaled "started" has been superceeded by 
                        //  at least one tune
                        cancelTimer(request);
                        
                        changeState(NOT_TUNED);
                        // Note: we set the state prior to notification in case the callbacks
                        //       re-enter the StateMachine to check the state, holding our monitor
                        notifyNetworkInterfaceCallbacksTuneComplete(request, false, false);
                        notifyNIListenersTuneComplete(request, false, false);
                        tuneRequest = null;
                        
                        processIgnoredRequests();

                        // The pending request is now current
                        tuneRequest = pendingRequest;
                        pendingRequest = null;

                        // Assert: pendingRequest is null, ignoredRequests is empty, 
                        //         tuneRequest is updated (was the last one enqueued)
                        if (tuneRequest.isUntuneRequest())
                        {
                            // We don't need to signal. The untuned tune was signaled in processIgnoredRequests
                            tuneRequest = null;
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(m_logPrefix + "handleTuneStarted: Initiating pending tune");
                            }
                            startTuning(tuneRequest);
                        }
                    }
                    else
                    {
                        // Consider the current tune started
                        changeState(TUNE_STARTED);

                        if (performanceLog.isInfoEnabled())
                        {
                            performanceLog.info("Tune Started: Tuner "  +  request.tsa.tunerNum + 
                                                               ", f " + request.tsa.currentTS.getFrequency() +
                                                               ", p " + request.tsa.programNum +
                                                               ", m " + request.tsa.currentTS.getModulationFormat());
                        }
                        
                        updateTSA(request);

                        // Note: The timer should still be set and will go off if we don't
                        //       get TUNE_SYNC/UNSYNC in a reasonable period of time
                    }
                    
                    break;
                case TUNE_STARTED: // TUNE_STARTED is only legal immediately after tune initiation, per mpeos_media.h
                case TUNED_SYNC:
                case TUNED_UNSYNC:
                case NOT_TUNED:
                    if (log.isInfoEnabled())
                    {
                        log.info(m_logPrefix + "handleTuneStarted: Unexpected in state " + statename(state) + ". Ignoring.");
                    }
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "handleTuneStarted: Invalid state: " + state);
                    }
                    throw new IllegalStateException("Invalid transaction or state");
            }
        } // END handleTuneStarted()
        
        /**
         * Handle state changes for a TUNED_SYNC event.
         * 
         * @param request
         */
        synchronized void handleTuneSynced(TuneRequest request)
        {
            // Make sure the event is the same one we're working on.
            if (request != tuneRequest)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "handleTuneSynced: SYNCED request is not current: " + request);
                }
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "handleTuneSynced in state " + statename(state));
            }

            switch (state)
            {
                case TUNE_STARTED:
                    // We're tuned and synced. Consider the tune complete and successful
                    cancelTimer(request);
                    changeState(TUNED_SYNC);
                    // Note: we set the state prior to notification in case the callbacks
                    //       re-enter the StateMachine holding our monitor
                    notifyNetworkInterfaceCallbacksTuneComplete(request, true, true);
                    notifyNIListenersTuneComplete(request, true, true);
                    break;
                case TUNED_UNSYNC:
                    // We were unsynced, and now we're synced. Handle accordingly.
                    changeState(TUNED_SYNC);
                    invokeCallbacks("notifySyncAcquired", request.getToken());
                    break;
                case TUNED_SYNC: // Redundant
                case NOT_TUNED: // SYNC is not legal in any of these states, per mpeos_media.h
                case TUNING:
                    if (log.isInfoEnabled())
                    {
                        log.info(m_logPrefix + "handleTuneSynced: Unexpected in state " + statename(state) + ". Ignoring.");
                    }
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "handleTuneSynced: Invalid state: " + state);
                    }
                    throw new IllegalStateException("Invalid transaction or state");
            }
        } // END handleTuneSynced()

        /**
         * Handle state changes for a TUNED_UNSYNC event.
         * 
         * @param request
         */
        synchronized void handleTuneUnsync(TuneRequest request)
        {
            // Make sure the event is the same one we're working on.
            if (request != tuneRequest)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "handleTuneUnsync: UNSYNC request is not current: " + request);
                }
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "handleTuneUnsync in state " + statename(state));
            }
            
            switch (state)
            {
                case TUNE_STARTED:
                    // We're tuned and unsynced. Consider the tune complete but not (initially) successful
                    cancelTimer(request);
                    changeState(TUNED_UNSYNC);
                    // Note: we set the state prior to notification in case the callbacks
                    //       re-enter the StateMachine holding our monitor
                    notifyNetworkInterfaceCallbacksTuneComplete(request, true, false);
                    notifyNIListenersTuneComplete(request, true, false);
                    break;
                case TUNED_SYNC:
                    // We were synced, and now we're unsynced. Handle accordingly.
                    changeState(TUNED_UNSYNC);
                    
                    invokeCallbacks("notifySyncLost", request.getToken());
                    break;
                case TUNED_UNSYNC: // Redundant
                case NOT_TUNED: // UNSYNC is not legal in any of these states, per mpeos_media.h
                case TUNING:
                    if (log.isInfoEnabled())
                    {
                        log.info(m_logPrefix + "handleTuneUnsync: Unexpected in state " + statename(state) + ". Ignoring.");
                    }
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "handleTuneUnsync: Invalid state: " + state);
                    }
                    throw new IllegalStateException("Invalid transaction or state " + state);
            }
        } // END handleTuneUnsync()

        /**
         * Handle state changes for a tune failed.
         * 
         * @param request
         */
        synchronized void handleTuneFail(TuneRequest request)
        {
            // Make sure the event is the same one we're working on.
            if (request != tuneRequest)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "handleTuneFail: Failing request is not current: " + request);
                }
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "handleTuneFail in state " + statename(state));
            }

            switch (state)
            {
                case TUNING:
                case TUNE_STARTED:
                    // Technically, the platform should only indicate this in response to 
                    //  a native tune (not after TUNE_STARTED). But we'll handle it either before
                    //  or after TUNE_STARTED (for backward-compatibility)
                    cancelTimer(request);
                    resetTSA();
                    changeState(NOT_TUNED); 
                    // Note: we set the state prior to notification in case the callbacks
                    //       re-enter the StateMachine holding our monitor
                    notifyNetworkInterfaceCallbacksTuneComplete(request, false, false);
                    notifyNIListenersTuneComplete(request, false, false);
                    tuneRequest = null;

                    if (pendingRequest != null)
                    { // Note: pendingRequest should always be null in TUNE_STARTED
                        // The tune that just signaled "failed" has been superceeded by 
                        //  later requests (which are now enqueued)
                        processIgnoredRequests();

                        // The pending request is now current
                        tuneRequest = pendingRequest;
                        pendingRequest = null;
                        
                        // Assert: pendingRequest is null, ignoredRequests is empty, 
                        //         tuneRequest is updated (was the last one enqueued)
                        if (tuneRequest.isUntuneRequest())
                        {
                            // We don't need to signal. The untuned tune was signaled in processIgnoredRequests
                            tuneRequest = null;
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(m_logPrefix + "handleTuneFail: Initiating pending tune");
                            }
                            startTuning(tuneRequest);
                        }
                    }
                    break;
                case NOT_TUNED: // TUNE_FAIL is not legal in any of these states, per mpeos_media.h
                case TUNED_SYNC:
                case TUNED_UNSYNC:
                    if (log.isInfoEnabled())
                    {
                        log.info(m_logPrefix + "handleTuneFail: Unexpected in state " + statename(state) + ". Ignoring.");
                    }
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "handleTuneFail: Invalid state: " + state);
                    }
                    throw new IllegalStateException("Invalid transaction or state");
            }
        } // END handleTuneFail()

        private void enqueuePendingRequest(TuneRequest request)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Enqueuing pending request" + request);
            }
            if (pendingRequest != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Moving pending request to ignored list");
                }
                ignoredRequests.add(pendingRequest);
            }
            pendingRequest = request;
        }

        /**
         * Process any enqueued requests. If pending requests exist, it will
         * signal all intermediate requests as pending and then failed
         */
        private void processIgnoredRequests()
        {
            TuneRequest req = null;
            TuneRequest lastReq = null;

            // Walk all the requests that we're going to skip. Just send the
            // events and be done.
            while (!ignoredRequests.isEmpty())
            {
                // Remove the first element in the list.
                lastReq = req;
                req = (TuneRequest) ignoredRequests.remove(0);

                if (!req.isUntuneRequest())
                {
                    // Synthesize a tune pending event for each skipped tune
                    signalTuneStarted(req);
                    
                    // Notify the registered NICallback that this request is complete (but not successful)
                    notifyNetworkInterfaceCallbacksTuneComplete(req, false, false);
                    
                    // And a issue tune complete failed event.
                    NetworkInterfaceEvent evt = new ExtendedNetworkInterfaceTuningOverEvent(NetworkInterfaceImpl.this,
                            NetworkInterfaceTuningOverEvent.FAILED, req.getOwner());
                    notifyNetworkInterfaceEvent(evt);
                }
            }
            // IgnoredRequests is empty
        }
        
        private void resetTSA()
        {
            if (tunedTSA != null)
            {
                tunedTSA.dispose();
                tunedTSA = null;
            }
        }
        
        private void updateTSA(TuneRequest req)
        {
            if (req != null)
            {
                tunedTSA = req.tsa;
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "activating tunedTSA: " + tunedTSA);
                }
                tunedTSA.activate();
            }
        }

        /**
         * Start the timeout timer for a given request.
         * 
         * @param request
         */
        private void startTimer(TuneRequest request)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Starting timer");
            }
            try
            {
                request.spec = timer.scheduleTimerSpec(request.spec);
            }
            catch (TVTimerScheduleFailedException e)
            {
                SystemEventUtil.logRecoverableError("Unable to set timer", e);
            }
        }

        /**
         * Start the tune (and set associated timer/state)
         * 
         * @param request Tuning request to start
         */
        private void startTuning(final TuneRequest request)
        {
            startTimer(request);
            changeState(TUNING);
            if(!request.retune)
            {
                resetTSA();
            }
            signalTuneStarted(request);
            if (!doNativeTune(request))
            {
                CallerContext ctx = ccm.getSystemContext();
                ctx.runInContext(new Runnable()
                {
                    public void run()
                    {
                        handleTuneFail(request);
                    }
                } );
            }
        }

        /**
         * Untune, perform notifications, unset timer, and set state
         * 
         * @param request Tune request to untune
         */
        private void untune(final TuneRequest request)
        {
            if (request == null)
            {
                return;
            }
            cancelTimer(request);
            changeState(NOT_TUNED);
            resetTSA();
            invokeCallbacks("notifyUntuned", request.getToken());
        }
        
        /**
         * Cancel the timeout timer for a given request.
         * 
         * @param request
         */
        private void cancelTimer(TuneRequest request)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Cancelling timer");
            }
            timer.deschedule(request.spec);
        }

        /**
         * 
         * @param newState
         */
        void changeState(int newState)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Changing from state " + statename(state) + " to " + statename(newState));
            }
            state = newState;
        }

        String statename(int state)
        {
            switch (state)
            {
                case NOT_TUNED:
                    return "NOT_TUNED";
                case TUNING:
                    return "TUNING";
                case TUNE_STARTED:
                    return "TUNE_STARTED";
                case TUNED_SYNC:
                    return "TUNED_SYNC";
                case TUNED_UNSYNC:
                    return "TUNED_UNSYNC";
                default:
                    return "Unknown " + state;
            }
        }

        /**
         * Notify all listeners that a tune has started. Correctly notify the
         * listeners and callbacks based whether this tune is a true tune, or a
         * retune.
         * 
         * @param request
         *            The tune request in question.
         */
        private void signalTuneStarted(TuneRequest request)
        {
            if (request.isRetuneRequest())
            {
                invokeCallbacks("notifyRetunePending", request.getToken());
            }
            else
            {
                invokeCallbacks("notifyTunePending", request.getToken());
                NetworkInterfaceTuningEvent evt = new ExtendedNetworkInterfaceTuningEvent(NetworkInterfaceImpl.this,
                        request.getOwner());
                notifyNetworkInterfaceEvent(evt);
            }
        }

        /**
         * Send NIListeners notification that tune has completed.
         * 
         * @param request
         *            The tune request.
         * @param success
         *            Did this tune succeed?
         * @param synced
         *            Is the tuner ready?
         */
        private void notifyNIListenersTuneComplete(TuneRequest request, boolean success, boolean synced)
        {
            if (!request.isRetuneRequest())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Notifying NIListeners tune complete: "
                              + ((success && synced) ? "SUCCEEDED" : "FAILED") );
                }

                NetworkInterfaceTuningOverEvent evt = new ExtendedNetworkInterfaceTuningOverEvent(
                        NetworkInterfaceImpl.this, (success && synced) ? NetworkInterfaceTuningOverEvent.SUCCEEDED
                                : NetworkInterfaceTuningOverEvent.FAILED, request.getOwner());
                notifyNetworkInterfaceEvent(evt);
            }
        }

        /**
         * Send NetworkInterfaceCallbacks notification that tune has completed.
         * 
         * @param request
         *            The tune request.
         * @param success
         *            Did this tune succeed?
         * @param synced
         *            Is the tuner ready?
         */
        private void notifyNetworkInterfaceCallbacksTuneComplete(TuneRequest request, boolean success, boolean synced)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Notifying NetworkInterfaceCallbacks tune complete: " + request + " " + success + " "
                        + synced);
            }

            if (request.isRetuneRequest())
            {
                invokeCallbacks("notifyRetuneComplete", request.getToken(), success, synced);
            }
            else
            {
                invokeCallbacks("notifyTuneComplete", request.getToken(), success, synced);
            }
        }
    }

    /** 
     * This class represents a particular tune request. 
     * 
     * This class represents an ED listener object. A new
     * <code>NIListener</code> is created each time
     * {@link NetworkInterfaceImpl#tune(TransportStream,ResourceProxy, Object)} is
     * called. The advantage of this approach is that the ED listener is only
     * associated with a specific tune request and there is less chance that an
     * event from a previous tune could be associated with an event for the
     * current tune.
     * 
     */
    static int counter = 0;

    private class TuneRequest implements EDListener, TVTimerWentOffListener
    {
        /**
         * 
         * 
         * @param tsa
         * @param owner
         * @param retune
         * @param untune
         * @param programNumber
         * @param token
         */
        public TuneRequest(TransportStreamAdapter tsa, ResourceProxy owner, boolean retune, boolean untune, int programNumber, Object token)
        {
            if (log.isDebugEnabled())
            {
                log.debug("TuneRequest ctor... ");
            }
        	
            this.tsa = tsa;
            this.owner = owner;
            this.retune = retune;
            this.untune = untune;
            this.programNumber = programNumber;
            this.token = (token != null ? token : this);
            this.requestID = counter++; // Probably should be synchronized, but
                                        // only for debugging
            spec.setDelayTime(TUNER_DELAY);
            spec.addTVTimerWentOffListener(this);
            this.m_logPrefix = NetworkInterfaceImpl.this.m_logPrefix + "TuneRequest " + requestID + ": ";
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Constructed (timer set for " + TUNER_DELAY + "ms");
            }
        }

        public Object getToken()
        {
            return token;
        }

        public String toString()
        {
            return (this.requestID + " " + tsa + " " + retune);
        }

        public TransportStream getTransportStream()
        {
            return tsa.currentTS;
        }

        public ResourceProxy getOwner()
        {
            return owner;
        }

        public boolean isRetuneRequest()
        {
            return retune;
        }

        public boolean isUntuneRequest()
        {
            return untune;
        }

        private final String m_logPrefix;

        private final TransportStreamAdapter tsa;

        private final ResourceProxy owner;

        private final boolean retune;

        private final boolean untune;

        final int programNumber;

        private final Object token;

        private final int requestID;

        TVTimerSpec spec = new TVTimerSpec();

        public void asyncEvent(final int event, int eventData1, int eventData2)
        {
            // the creation of another thread could be removed when the native
            // code is changed
            // to use a java thread instead of an attached native thread. The
            // current native
            // thread does not check exceptions so we never see any problems
            // that happen during
            // event delivery.
            if (log.isDebugEnabled())
            {
                log.debug( m_logPrefix + "asyncEvent: Received event " + event 
                           + "(0x" + Integer.toHexString(eventData1)
                           + ",0x" + Integer.toHexString(eventData2) + ')' );
            }
            
            final TuneRequest req = this;
            switch (event)
            {
                // tune completed successfully
                case EventCallback.TUNE_SYNCED:
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "TUNE_SYNCED");
                    }
                    // tune was successful
                    stateMachine.handleTuneSynced(req);
                    break;
                case EventCallback.TUNE_UNSYNCED:
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "TUNE_UNSYCED");
                    }
                    // tune was successful
                    stateMachine.handleTuneUnsync(req);
                    break;
                case EventCallback.TUNE_FAIL:
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "TUNE_FAIL");
                    }
                    stateMachine.handleTuneFail(req);
                    break;
                case EventCallback.TUNE_ABORT:
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "TUNE_ABORT (handling as TUNE_FAIL)");
                    }
                    // Use the TUNE_FAIL handler
                    stateMachine.handleTuneFail(req);
                    break;
                case EventCallback.TUNE_STARTED:
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "TUNE_STARTED");
                    }
                    stateMachine.handleTuneStarted(req);
                    break;
                case EventCallback.TUNE_SHUTDOWN:
                    // do nothing with this event
                    // Should never be seen (ED eats them)
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "TUNE_SHUTDOWN");
                    }
                    break;
                default:
                    // this result should not happen.
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "Unknown event value " + event);
                    }
                    break;
            }
        }

        public void timerWentOff(TVTimerWentOffEvent e)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Timer went off " + this);
            }
            // Remove ourselves. This can only happen once per tune.
            spec.removeTVTimerWentOffListener(this);
            stateMachine.handleTimerTrigger(this);
        }
    }

    /**
     * A transport stream adapter which performs a re-tune whenever the
     * transport stream mapped by the tuned service changes.
     */
    private class TransportStreamAdapter
    {
        // The transport stream this object was constructed with
        private final TransportStreamExt originalTS;

        // The current (re-mapped) transport stream
        private TransportStreamExt currentTS;
        
        // The current session
        private SelectionSession session;

        // The service details locator
        private javax.tv.locator.Locator sdLocator = null;

        // Callback for notification of changes to service mapping
        private ServiceDetailsCallback sdcb = null;

        private ProviderInstance spi = null;

        private ResourceProxy proxy = null;
        
        private int tunerNum;
        
        private int programNum;
        
        ServiceDetailsExt sd = null;
        
        // Construct an instance
        public TransportStreamAdapter(TransportStream ts, Service service, int program_number, ResourceProxy proxy, SelectionSession session, int tunerNum)
        {
            // Save arguments
            originalTS = (TransportStreamExt) ts;
            this.session = session;
            this.tunerNum = tunerNum;
            this.proxy = proxy;
            
            // Set the current transport stream
            try
            {
                if(service instanceof SPIService)
                    sd = (SPIServiceDetails) (((SPIService)service).getDetails());
                else
                {
                    // In order to support handling of service re-map (via OOB SI table 
                    // updates) we need to have valid service details
                	sd = (ServiceDetailsExt) originalTS.getServiceDetails();
                    // If tuning to a service locator we can retrieve details for the locator
                	// If tuning to transport stream 'service' is null
                	if(sd == null && service != null)
                	{
                        sd = (ServiceDetailsExt) ((ServiceExt)service).getDetails();
                	}
                }
            }
            catch (Exception x)
            {
                SystemEventUtil.logRecoverableError("Cannot get ServiceDetails", x);
                return;
            }
                        
            if (sd == null)
            {
                // No remapping supported so use the original TS
                currentTS = originalTS;
            }
            else
            {                
                // Save the SD locator
                sdLocator = sd.getLocator();
                
                // Get the SPI for this SD
                if (sd instanceof SPIServiceDetails)
                {
                    SPIServiceDetails spisd = (SPIServiceDetails) sd;
                    spi = spisd.getProviderInstance();
                }
                programNum = program_number;
                
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "TSA programNum: " + programNum);
                }
                currentTS = originalTS;
            }
        }

        public void activate()
        {      
            if(sd == null)
            {
                return;
            }
            
            if(sdcb != null)
            {
                // Because of an existing issue OCORI-4104
                // cannot add a callback with same priority if one already exists.
                // So, remove the existing calback first and add new callback
                if (log.isWarnEnabled())
                {
                    log.warn(m_logPrefix + "ServiceDetailsCallback sdcb is not null. Removing callback. ");
                }
                TransportExt.removeServiceDetailsCallback(sdcb);
            }
            // Listen for changes to service mapping
            sdcb = new SDCBListener(this, sd.getID(), proxy);

            // TODO: Fix to use correct priority (OCORI-4104)
            // For now use tuner number as priority for ServiceDetailsCallback
            TransportExt.addServiceDetailsCallback(sdcb, tunerNum);
        }
        
        // Return the ServiceDetails locator. If not available, return the
        // TransportStream locator.
        public Locator getLocator()
        {
            return (sdLocator == null) ? currentTS.getLocator() : (Locator) sdLocator;
        }

        // Re-tune
        public void handleRetune(ServiceDetails sdetails, int mapState) throws NetworkInterfaceException
        {
		    // service is UNMAPPED
		    if(mapState == NetworkInterfaceImpl.SERVICE_UNMAPPED) 
		    {
		    	// Signal unTuned event
                TuneRequest untuneReq = new TuneRequest( this, reserve.getOwner().proxy, true, 
                                                         true, 0, stateMachine.tuneRequest.getToken() );

                stateMachine.handleUntune(untuneReq);
                
                currentTS = null;
                // If session is active, destroy the session
                if (session != null)
                {
                    session.destroy();
                    session = null;
                }
                return;		    	
		    }
		    // service is REMAPPED
            else if(mapState == NetworkInterfaceImpl.SERVICE_REMAPPED) 
            {
                // When a Service is RE_MAPPED
                // Get the latest transport stream based on current signaling.
                // Nothing to do if it did not change.
                TransportStreamExt ts = lookupTransportStream(sdetails);    
            
                // Re-mapping of Service within the same 
                // transport stream in now supported. 
                // But service details needs to have changed
                int pn = ((ServiceDetailsExt)sdetails).getProgramNumber();
                
                if (ts != null && ts.equals(currentTS) && programNum == pn) 
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "handleRetune service re-map pn: " + pn);
                    }
                    // If neither program number nor mode changed ignore it 
                    if (log.isInfoEnabled())
                    {
                        log.info(m_logPrefix + "handleRetune processing service re-map, but ServiceDetails unchanged..ignoring change for handle:0x" + Integer.toHexString(((ServiceDetailsExt)sdetails).getServiceDetailsHandle().getHandle()));
                    }
                    return;
                }
            
                currentTS = ts;
                if (session != null)
                {                    
                    session.destroy();
                    session = null;
                }
      
	            // If we have a ProviderInstance...
	            if (spi != null && session == null)
	            {
	                // Get the ServiceDetails used to create the TS
	                ServiceDetailsExt sd = (ServiceDetailsExt) ts.getServiceDetails();

	                // If there is actually a ServiceDetails available and it is an
	                // SPIServiceDetails...
	                if (sd != null && sd instanceof SPIServiceDetails)
	                {
	                    // Create a new session
	                	SPIService service = (SPIService)((SPIServiceDetails) sd).getService();
	                    session = (SelectionSessionWrapper) spi.newSession(((SPIService) service).getServiceReference(), (SPIService)service);
	                }
	            }
	            int programNumber = -1;
	            try
	            {
	                ServiceDetails[] sd = siManager.getServiceDetails(sdetails.getLocator());
	                if (sd.length >= 1) programNumber = ((ServiceDetailsExt) sd[0]).getProgramNumber();
	            }
	            catch (Exception e)
	            {
	            }
	            
	            TuneRequest req = new TuneRequest( this, reserve.getOwner().proxy, true, 
	                                               false, 
	                                               programNumber, stateMachine.tuneRequest.getToken() );

	            // Notify callbacks that a re-tune is about to start. Do not update
	            // state until callbacks are notified.
	            currentTS = ts;

	            // Perform the re-tune
	            doTune(req);
		    }
        } // END handleRetune()
        
        private TransportStreamExt lookupTransportStream(ServiceDetails sdetails)
        {
            ServiceDetailsExt sd = null;
            TransportStreamExt ts = null;

            // Get a fresh up-to-date instance of the service details and get
            // the current transport stream which carries it.
            try
            {
            	sd = (ServiceDetailsExt) siManager.getServiceDetails(sdetails.getLocator())[0];
                ts = (TransportStreamExt) sd.getDavicTransportStream(NetworkInterfaceImpl.this);
            }
            catch (Exception e)
            { /* leaves ts set to null */
            }

            return ts;
        }

        
        // Lookup the transport stream based on current signaling. Return null
        // if no transport stream is currently mapped.
        private TransportStreamExt lookupCurrentTransportStream()
        {
            ServiceDetailsExt sd = null;
            TransportStreamExt ts = null;

            // Get a fresh up-to-date instance of the service details and get
            // the current
            // transport stream which carries it.
            try
            {
                // TODO(Todd): Add getRefreshed() method to get up to date
                // version of object (same ID).
                sd = (ServiceDetailsExt) siManager.getServiceDetails(sdLocator)[0];
                ts = (TransportStreamExt) sd.getDavicTransportStream(NetworkInterfaceImpl.this);
            }
            catch (Exception e)
            { /* leaves ts set to null */
            }

            return ts;
        }

        // Release resources held by this TSA
        public void dispose()
        {
            if (session != null)
            {
                session.destroy();
            	session = null;
            }
            
            // Stop listening for SD changes
            if (sdcb != null)
            {
                TransportExt.removeServiceDetailsCallback(sdcb);
                sdcb = null;
            }
        }

        // Description copied from Object
        public String toString()
        {
            return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + "[sdLocator="
                    + sdLocator + ", originalTS=" + originalTS + ", currentTS=" + currentTS + ", session " + session +"]";
        }
    }

    private static int SERVICE_MAPPED=1;
    private static int SERVICE_REMAPPED=2;
    private static int SERVICE_UNMAPPED=3;

    /**
     * Listener for changes to service mapping.
     */
    private class SDCBListener implements ServiceDetailsCallback
    {
        // The object to notify when a change is detected
        private final TransportStreamAdapter tsa;

        // The unique ID of the ServiceDetails we are listening for
        private final Object sdID;

        // The resource proxy to hold while re-tuning
        private final ResourceProxy proxy;

        // Constructor
        public SDCBListener(TransportStreamAdapter tsa, Object sdID, ResourceProxy proxy)
        {
            this.tsa = tsa;
            this.sdID = sdID;
            this.proxy = proxy;
        }


        // Description copied from ServiceDetailsCallback
        public void notifyMapped(ServiceDetails sd)
        {
            handleChange(sd, NetworkInterfaceImpl.SERVICE_MAPPED);
        }

        // Description copied from ServiceDetailsCallback
        public void notifyRemapped(ServiceDetails sd)        
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "SDCBListener notifyRemapped");
            }
            handleChange(sd, NetworkInterfaceImpl.SERVICE_REMAPPED);
        }

        // Description copied from ServiceDetailsCallback
        public void notifyUnmapped(ServiceDetails sd)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "SDCBListener notifyUnmapped");
            }
            handleChange(sd, NetworkInterfaceImpl.SERVICE_UNMAPPED);
        }

        /**
         * Handle change to service mapping.
         */
        private void handleChange(final ServiceDetails sd, final int mapState)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "SDCBListener handleChange sdID: " + sdID + " sdID (new): " + ((ServiceDetailsExt) sd).getID());            
            }
            // Only deal with change if it is for the currently tuned
            // ServiceDetails.
        	
            if (((ServiceDetailsExt) sd).getID().equals(sdID))
            {
                // Perform the re-tune while holding the reservation
                try
                {
                    withReservation(proxy, new Runnable()
                    {
                        public void run()
                        {
                            synchronized (NetworkInterfaceImpl.this.stateMachine)
                            {
                                // Only re-tune if the change was for the
                                // currently-tuned TSA.
                                try
                                {
                                    if (tsa == NetworkInterfaceImpl.this.stateMachine.tunedTSA) 
                                    {
                                    	tsa.handleRetune(sd, mapState);
                                    }
                                }
                                catch (NetworkInterfaceException e)
                                {
                                    // Problem while re-tuning
                                    SystemEventUtil.logRecoverableError(e);
                                }
                            }
                        }
                    });
                }
                catch (NotOwnerException e)
                {
                    // Don't have the reservation so don't try to re-tune
                }
            }
        }
    }

    // Description copied from ExtendedNetworkInterface
    public ResourceUsageImpl getResourceUsage()
    {
        Client client = reserve.getOwner();
        return (client != null ? client.resusage : null);
    }

    // Description copied from ExtendedNetworkInterface
    public CallerContext getOwnerContext()
    {
        Client client = reserve.getOwner();
        return (client != null ? client.context : null);
    }

    /**
     * 
     * @author jspruiel
     * 
     *         Returns a list of usages.
     * @return list of usages
     */
    public ArrayList getResourceUsages()
    {
        Client client = reserve.getOwner();
        if (client != null)
        {
            return new ArrayList(client.resUsages);
        }
        return null;
    }

    /**
     * This class represents the <i>reservation</i> or ownership of a resource.
     * It maintains a single link to an owner, and provides three methods for
     * accessing the <i>owner</i>:
     * <ul>
     * <li> {@link #getOwner} queries the current owner
     * <li> {@link #take} attempts to reassign ownership
     * <li> {@link #runWith(Client, Runnable)} used to run code while maintaining
     * ownership
     * </ul>
     * <p>
     * To reserve the associated resource:
     * 
     * <pre>
     * while (!reserve.take(reserve.getOwner(), myOwner))
     * {
     *     // empty
     * }
     * </pre>
     * 
     * To execute while holding the resource:
     * 
     * <pre>
     * if (!reserve.runWith(myOwner, new Runnable()
     * {
     *     public void run()
     *     {
     *         // do stuff
     *     }
     * }))
     * {
     *     // failure condition - no longer have the resource
     * }
     * </pre>
     * 
     * @author Aaron Kamienski
     */
    private class Reservation
    {
        private Client owner;

        /**
         * Returns the current owner or <code>null</code> if nobody currently
         * owns the resource.
         */
        Client getOwner()
        {
            return owner;
        }

        /**
         * Runs the given <code>Runnable</code> while holding on to this
         * resource, if <i>proxy</i> is the <code>ResourceProxy</code> of the
         * current owner. Returns <code>true</code> if the <code>Runnable</code>
         * was successfully executed; <code>false</code> otherwise.
         * <p>
         * Generally, a failure to run the <code>Runnable</code> occurs when a
         * successful transfer of ownership (via {@link #take}) takes place on
         * another thread (since the <i>owner</i> was read by {@link #getOwner}
         * ).
         * 
         * @param proxy
         *            expected <code>ResourceProxy</code> of the current owner
         * @param run
         *            the <code>Runnable</code> to execute
         * @return <code>true</code> if the <code>Runnable</code> was
         *         successfully executed; <code>false</code> otherwise.
         */
        synchronized boolean runWith(ResourceProxy proxy, Runnable run)
        {
            if (owner != null && owner.proxy == proxy)
            {
                run.run();
                return true;
            }
            return false;
        }

        /**
         * Takes the resource reservation for the <i>newOwner</i>, if
         * <i>owner</i> correctly specifies the current owner. Returns
         * <code>true</code> if ownership was transferred; <code>false</code> if
         * <i>owner</i> was not the current owner.
         * <p>
         * Generally, a failure of ownership transfer occurs when another
         * successful transfer has taken place on another thread (since the
         * <i>owner</i> was read by {@link #getOwner}).
         * 
         * @param expectedOwner
         *            expected current owner
         * @param newOwner
         *            the owner to transfer resource ownership to
         * @return <code>true</code> if ownership was transferred;
         *         <code>false</code> if <i>owner</i> was not the current owner.
         */
        synchronized boolean take(Client expectedOwner, Client newOwner)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Reservation::take() Enter - owner: " + expectedOwner + " newOwner: " + newOwner);
            }
            // Compare using equals() instead of ==
            // so that equivalent objects compare the same.
            if ((owner == null) ? (expectedOwner == null) : owner.equals(expectedOwner))
            {
                if (expectedOwner != null)
                {
                    // Clear the reservation in the previous ResourceUsage
                    for (int ii = 0; ii < expectedOwner.resUsages.size(); ii++)
                    {
                        ((ResourceUsageImpl) expectedOwner.resUsages.get(ii)).set(expectedOwner.proxy, false);
                    }
                }

                owner = newOwner;

                if (owner != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "Before setting usage reservation");
                    }
                    // Set the reservation in the new ResourceUsage
                    ResourceUsageImpl eru = (ResourceUsageImpl) owner.resUsages.get(0);
                    eru.set(owner.proxy, true);
                }

                return true;
            }
            return false;
        }
    }

    /**
     * Resource reservation/ownership object.
     */
    private final Reservation reserve = new Reservation();

    /**
     * Holds context-specific data. Specifically the set of
     * <code>NetworkInterfaceListener</code>s.
     */
    private class Data implements CallbackData
    {
        public NetworkInterfaceListener niListeners;

        public void destroy(CallerContext cc)
        {
            removeListeners(cc);
        }

        public void active(CallerContext cc)
        { /* empty */
        }

        public void pause(CallerContext cc)
        { /* empty */
        }
    }

    /**
     * generic object for locking
     */
    private Object lock = new Object();

    /**
     * CallerContext multicaster for executing NetworkInterfaceListener.
     */
    private CallerContext niContexts = null;

    /**
     * reference to the CallerContextManager
     */
    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * NetworkInterface state machine object
     */
    NIStateMachine stateMachine = new NIStateMachine();

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(NetworkInterfaceImpl.class);

    // TODO: this needs to be implemented when the SI support is there
    // private native int[] getAllTransportStreamIds();

    // Description copied from ExtendedNet
    public int getHandle()
    {
        return tunerHandle;
    }

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }

}

/**
 * Exception thrown when reservation contention occurs between threads/apps.
 */
class TuningResourceContention extends Exception
{
    TuningResourceContention()
    {
        // empty
    }
}
