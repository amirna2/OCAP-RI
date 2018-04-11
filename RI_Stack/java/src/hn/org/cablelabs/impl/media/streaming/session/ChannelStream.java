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

package org.cablelabs.impl.media.streaming.session;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import javax.media.Time;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.LocatorFactory;
import javax.tv.service.SIManager;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.StreamType;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterfaceManager;
import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback;
import org.cablelabs.impl.davic.net.tuning.SharableNetworkInterfaceController;
import org.cablelabs.impl.davic.net.tuning.SharableNetworkInterfaceManager;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.pod.CASession;
import org.cablelabs.impl.manager.pod.CASessionListener;
import org.cablelabs.impl.media.access.CASessionMonitor;
import org.cablelabs.impl.media.mpe.HNAPI;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.service.ServiceChangeEvent;
import org.cablelabs.impl.service.ServiceChangeListener;
import org.cablelabs.impl.service.ServiceChangeMonitor;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackCopyControlInfo;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescription;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescriptionTuner;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentLocationType;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.media.streaming.session.util.ContentRequestConstant;
import org.cablelabs.impl.media.streaming.session.util.StreamUtil;
import org.cablelabs.impl.ocap.hn.ContentServerNetModuleImpl;
import org.cablelabs.impl.ocap.hn.NetResourceUsageImpl;
import org.cablelabs.impl.ocap.hn.content.ChannelContentItemImpl;
import org.cablelabs.impl.ocap.hn.transformation.NativeContentTransformation;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.cm.ConnectionCompleteListener;
import org.cablelabs.impl.pod.mpe.CASessionEvent;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.CountingSemaphore;
import org.cablelabs.impl.util.LocatorFactoryImpl;
import org.cablelabs.impl.util.MediaStreamType;
import org.cablelabs.impl.util.SimpleCondition;
import org.davic.net.Locator;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.StreamingActivityListener;
import org.ocap.hn.resource.NetResourceUsage;
import org.ocap.net.OcapLocator;
import org.ocap.si.PATProgram;
import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramAssociationTableManager;

public class ChannelStream implements Stream
{
    private static final Logger log = Logger.getLogger(ChannelStream.class);

    //initial state including successful tuning and resource monitoring
    //ServiceChangeMonitor, CASessionMonitor, NetworkInterfaceCallback and ConnectionCompleteListener may all receive notifications in this state
    private static final int STATE_INIT = 1;
    //state entered prior to TRANSMITTING state due to sync loss, retune, etc
    private static final int STATE_INIT_SUSPENDED = 2;
    //state while transmitting
    private static final int STATE_TRANSMITTING = 3;
    //state entered after TRANSMITTING state due to sync loss, retune, etc
    private static final int STATE_TRANSMISSION_SUSPENDED = 4;
    //state when streaming has stopped (terminal)
    private static final int STATE_STOPPED = 5;

    private static final long TUNE_TIMEOUT_MILLIS = 30000;

    //the priority to use when registering an NI callback
    private static final int NI_CALLBACK_PRIORITY = 15;

    private final Object m_lock = new Object();
    private final SimpleCondition m_tuneTimeoutCondition = new SimpleCondition(false);

    //ensure the stream stays active during session transitions due to retune, sync/unsync.  
    //Max value as there may be multiple not-streaming conditions at the same time  
    private final CountingSemaphore m_sessionTracker = new CountingSemaphore(Integer.MAX_VALUE);

    //session/content description can change due to retune
    private HNServerSession m_session;
    private HNStreamContentDescription m_contentDescriptionTuner;
    private int m_currentState = STATE_INIT;

    private final ChannelContentItemImpl m_contentItem;
    private final Socket m_socket;
    private final String m_url;
    private final HNStreamProtocolInfo m_protocolInfo;
    private final Integer m_connectionId;

    //re-initialized each time session is changed
    //NOTE: CASessionChange and ServiceChange notifications are handled async
    private final CASessionMonitor m_caSessionMonitor;
    private final ServiceChangeMonitor m_serviceChangeMonitor;

    //not re-initialized with session changes
    private final ConnectionCompleteListener m_connectionCompleteListener;
    //sync/unsync and tuneComplete notifications are ran async, notifyRetunePending and ResourceClient#release cannot be ran async
    //notifications are received via an ED listener asyncEvent, and these notifications may result in calls which can result in other
    //ED listener asyncEvent notifications, which would cause a queue lockup...avoid that by calling those methods async
    private final NetworkInterfaceCallback m_networkInterfaceCallback;

    private final SharableNetworkInterfaceController m_sharableNetworkInterfaceController;
    private final ExtendedNetworkInterface m_networkInterface;

    //result of tuneOrShareFor
    private boolean m_tuneSuccess = false;

    //token associated with the tune
    private Object m_tuneInstance;
    
    private Integer SUSPENDED_REASON_SYNC = new Integer(1);
    private Integer SUSPENDED_REASON_RETUNE = new Integer(2);
    private Integer SUSPENDED_REASON_CA = new Integer(3);
    private Integer SUSPENDED_REASON_PMT = new Integer(4);

    //as suspension reasons are set, 
    private final Set m_suspendedReasons = new HashSet();
    
    //constructor args
    private final int m_chunkedEncodingMode;
    private final long m_maxTrickModeBandwidth;
    private final int m_currentDecodePTS;
    private final int m_maxGOPsPerChunk;
    private final int m_maxFramesPerGOP;
    private final boolean m_useServerSidePacing;
    private final NativeContentTransformation m_transformation;

    /**
     * Constructor - creates and opens an HNServerSession
     * 
     * @param request the contentRequest associated with this stream
     * @param contentItem the content item to stream
     * @throws HNStreamingException if the session could not be opened 
     */
    public ChannelStream(ChannelContentItemImpl contentItem, ContentRequest request) 
        throws HNStreamingException
    {
        m_contentItem = contentItem;
        m_url = request.getURI();
        m_chunkedEncodingMode = request.getChunkedEncodingMode();
        m_maxTrickModeBandwidth = request.getMaxTrickModeBandwidth();
        m_currentDecodePTS = request.getCurrentDecodePTS();
        m_maxGOPsPerChunk = request.getMaxGOPsPerChunk();
        m_maxFramesPerGOP = request.getMaxFramesPerGOP();
        m_useServerSidePacing = request.isUseServerSidePacing();
        m_socket = request.getSocket();
        m_protocolInfo = request.getProtocolInfo();
        m_connectionId = request.getConnectionId();
        m_transformation = request.getTransformation();

        if (log.isInfoEnabled())
        {
            log.info("constructing ChannelStream - id: " + m_connectionId + ", url: " + m_url);
        }

        // We ignore rate and range requests (it's a non-random-access service)
        
        if (request.isRangeHeaderIncluded())
        {
            if (log.isInfoEnabled()) 
            {
                log.info( "Ignoring range request (requested startByte: " + request.getStartBytePosition() 
                          + ", requested endByte: " + request.getEndBytePosition() 
                          + ", rate: " + request.getRate() );
            }
        }
        else if (request.isTimeSeekRangeHeaderIncluded())
        {
            if (log.isInfoEnabled()) 
            {
                log.info( "Ignoring timeSeekRange (requested startNanos: " + request.getTimeSeekStartNanos() + 
                          ", endNanos: " + request.getTimeSeekEndNanos() 
                          + ", rate: " + request.getRate() );
            }
        }
        
        m_serviceChangeMonitor = new ServiceChangeMonitor(m_lock, new ServiceChangeListenerImpl());
        m_caSessionMonitor = new CASessionMonitor(m_lock, new CASessionListenerImpl());
        m_connectionCompleteListener = new ConnectionCompleteListenerImpl();
        m_session = new HNServerSession(MediaServer.getServerIdStr());

        //listener released in session#releaseResources - no need to hold a reference 
        m_session.setListener(new EDListenerImpl());

        m_tuneTimeoutCondition.setFalse();

        //resolveLocator also potentially causes the service to become available from SI - must be called prior to calling tuneOrShareFor
        OcapLocator resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());

        // The locator in is only used for resource usage creation
        // Use channel locator (Not resolved locator), in order to be told of remaps
        InetAddress requestInetAddress = request.getRequestInetAddress();
        OcapLocator channelLocator = m_contentItem.getChannelLocator();
        if (log.isInfoEnabled())
        {
            log.info("channelLocator: " + channelLocator + ", request inetAddress: " + requestInetAddress);
        }
        //now that resolveLocator has been called, the service for the channel locator should be resolvable via SI, even if it was an SPI service
        ServiceExt service;
        try
        {
            //verify retrieval does not fail - no need to hold a ref to the service
            service = (ServiceExt) SIManager.createInstance().getService(channelLocator);
        }
        catch (InvalidLocatorException e1)
        {
            throw new HNStreamingException("Unable to verify service after resolving channel locator: " + m_contentItem.getChannelLocator(), e1);
        }

        ResourceClientImpl networkInterfaceResourceClient = new ResourceClientImpl();

        ExtendedNetworkInterfaceManager networkInterfaceManager = (ExtendedNetworkInterfaceManager) ExtendedNetworkInterfaceManager.getInstance();
        SharableNetworkInterfaceManager sharableNetworkInterfaceManager = networkInterfaceManager.getSharableNetworkInterfaceManager();
        m_sharableNetworkInterfaceController = sharableNetworkInterfaceManager.createSharableNetworkInterfaceController(networkInterfaceResourceClient);
        m_networkInterfaceCallback = new NetworkInterfaceCallbackImpl();
        if (log.isInfoEnabled())
        {
            log.info("networkInterfaceController: " + m_sharableNetworkInterfaceController);
        }

        NetResourceUsageImpl resourceUsage = new NetResourceUsageImpl(requestInetAddress,
                NetResourceUsage.USAGE_TYPE_PRESENTATION,
                channelLocator,
                null);

        try
        {
            if (log.isInfoEnabled())
            {
                log.info("calling tuneOrShareFor for service: " + service);
            }
            // We want to (ok "need to") do this without holding the lock (we may get
            //  NICallbacks before this returns) - don't use the service member, the lock isn't held here
            m_tuneInstance = m_sharableNetworkInterfaceController.tuneOrShareFor(resourceUsage, service, null, m_networkInterfaceCallback, NI_CALLBACK_PRIORITY);
            m_networkInterface = m_sharableNetworkInterfaceController.getNetworkInterface();
            if (log.isInfoEnabled())
            {
                log.info("tuneOrShareFor on the SharableNetworkInterfaceController for service: " + service + " - returned tuneInstance: " + m_tuneInstance);
            }
            //NI should never be null or idle - don't check tuner state while holding lock
            if (m_tuneInstance == null || m_networkInterface == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("networkInterface is null or idle");
                }
                throw new HNStreamingException("Unable to access a network interface");
            }
            else
            {
                //returned from tuneOrShareFor without throwing an exception or returning null - if tuned, no callback will be received, start the player
                // Going to try to tune
                //ok to start streaming if not tuning (synced or unsynced)
                //if not tuned, an NI callback will be received
                if (m_networkInterface.isTuned(m_tuneInstance))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("networkInterface already tuned");
                    }
                    CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                    callerContextManager.getSystemContext().runInContextAsync(new Runnable()
                    {
                        public void run()
                        {
                            m_tuneSuccess = true;
                            m_tuneTimeoutCondition.setTrue();
                        }
                    });
                }
            }
        }
        catch (NetworkInterfaceException e1)
        {
            throw new HNStreamingException("tuneOrShareFor failed", e1);
        }
        
        try
        {
            m_tuneTimeoutCondition.waitUntilTrue(TUNE_TIMEOUT_MILLIS);
        }
        catch (InterruptedException e)
        {
            //ignore   
        }

        if (!m_tuneTimeoutCondition.getState() || !m_tuneSuccess)
        {
            throw new HNStreamingException("Unable to reserve and tune");
        }

        try
        {
            //use resolved locator during resource monitoring (transport dependent service details)
            //tuneOrShareFor must be called prior to beginResourceMonitoring due to the need to use the NetworkInterface
            //transmit can only be called after beginResourceMonitoring has been called, as the CASession is set up here
            Integer result = beginResourceMonitoring(resolvedLocator);
            if (result != null)
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("beginResourceMonitoring returned: " + suspendedReasonToString(result));
                }
                if (log.isInfoEnabled())
                {
                    log.info("changing state to INIT_SUSPENDED");
                }
                m_currentState = STATE_INIT_SUSPENDED;
                m_suspendedReasons.add(result);
            }
            MediaServer.getInstance().getCMS().addConnectionCompleteListener(m_connectionCompleteListener);
        }
        catch (HNStreamingException hnse)
        {
            if (log.isInfoEnabled()) 
            {
                log.info("unable to begin resource monitoring - stopping");
            }
            //tune succeeded but resource monitoring failed - clean up
            stop(false);
            //then throw
            throw hnse;
        }
    }

    public void open(ContentRequest request) throws HNStreamingException
    {
        m_session.openSession(m_socket, m_chunkedEncodingMode,
                m_protocolInfo.getProfileId(), 
                m_protocolInfo.getContentFormat(),
                m_maxTrickModeBandwidth, m_currentDecodePTS,
                m_maxGOPsPerChunk, m_maxFramesPerGOP,
                m_useServerSidePacing, getFrameTypesInTrickMode(), 
                m_connectionId.intValue(), m_contentItem );
    }
    
    public int getFrameTypesInTrickMode() throws HNStreamingException
    {
        // No trick mode support since this stream is always presenting at the
        // live point (no random access)
        return ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_NONE;
    }
    
    public void transmit() throws HNStreamingException
    {
        synchronized (m_lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("transmit: " + m_contentItem + ", current state: " + stateToString(m_currentState));
            }
            switch (m_currentState)
            {
                case STATE_INIT:
                    //default case
                    MediaServer.getInstance().getCMS().registerLocalConnection(m_protocolInfo, m_connectionId.intValue(), -1);

                    // Notify when content begins streaming (notifying prior to streaming start)
                    ContentServerNetModuleImpl server = (ContentServerNetModuleImpl)m_contentItem.getServer();
                    if (server != null)
                    {
                        server.notifyStreamStarted(m_contentItem, m_connectionId.intValue(),
                                m_url, null, StreamingActivityListener.CONTENT_TYPE_LIVE_RESOURCES);

                        m_session.transmit(HNStreamContentLocationType.HN_CONTENT_LOCATION_TUNER, m_contentDescriptionTuner, 1.0f, false, -1, -1,
                                -1, -1, new HNPlaybackCopyControlInfo[]{new HNPlaybackCopyControlInfo((short) -1, true, false, (byte) 0)}, m_transformation);
                        //acquire the semaphore prior to calling tracksessioncompletion
                        m_sessionTracker.acquire();
                        trackSessionCompletion(m_session);

                        if (log.isInfoEnabled())
                        {
                            log.info("changing state to TRANSMITTING");
                        }
                        m_currentState = STATE_TRANSMITTING;
                    }
                    else
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("transmit() - null content item server");
                        }
                        throw new IllegalArgumentException("Content item server was null");
                    }
                    break;
                case STATE_INIT_SUSPENDED:
                    //normal path when some condition occurs prior to transmit - update to TRANSMISSION_SUSPENDED - reasons don't change
                    if (log.isInfoEnabled())
                    {
                        log.info("changing state to TRANSMISSION_SUSPENDED");
                    }
                    m_currentState = STATE_TRANSMISSION_SUSPENDED;
                    break;
                case STATE_TRANSMITTING:
                    //ignore
                    if (log.isInfoEnabled())
                    {
                        log.info("ignoring transmit in TRANSMITTING state");
                    }
                    break;
                case STATE_TRANSMISSION_SUSPENDED:
                    //ignore
                    if (log.isInfoEnabled())
                    {
                        log.info("ignoring transmit in TRANSMISSION_SUSPENDED state");
                    }
                    break;
                case STATE_STOPPED:
                    //no-op
                    if (log.isInfoEnabled())
                    {
                        log.info("ignoring transmit in STOPPED state");
                    }
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn("notifyCASessionChange called in unexpected state: " + stateToString(m_currentState));
                    }
            }
        }

        //block until semaphore count is zero
        m_sessionTracker.await();
    }
    
    public void stop(boolean closeSocket)
    {
        //may be called in any state, but no-op if already stopped
        synchronized(m_lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("stop - closeSocket: " + closeSocket + ", current state: " + m_currentState);
            }
            
            if (m_currentState == STATE_STOPPED)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring stop in STOPPED state");
                }
                return;
            }
            //streaming complete
            //clean up monitors - shutting down
            m_caSessionMonitor.cleanup();
            m_serviceChangeMonitor.cleanup();

            if (log.isInfoEnabled())
            {
                log.info("stop - releasing resources - session complete");
            }
            releaseResources(closeSocket, true);

            //stopping the stream - close the counting semaphore
            m_sessionTracker.releaseAll();

            //release listener
            MediaServer.getInstance().getCMS().removeConnectionCompleteListener(m_connectionCompleteListener);
            
            if (closeSocket)
            {
                if (m_socket != null)
                {
                    try
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("stop - closing socket..");
                        }
                        m_socket.close();
                    }
                    catch (IOException e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Unable to close socket", e);
                        }
                    }
                }
            }
            if (log.isInfoEnabled())
            {
                log.info("changing state to STOPPED");
            }
            m_currentState = STATE_STOPPED;
        }
    }

    /**
     * Monitor resources
     *
     * @throws HNStreamingException if service details could not be retrived or decrypt session could not be started
     * @param resolvedLocator the transport-dependent locator
     * @return Integer suspended reason, or null if successful
     */
    private Integer beginResourceMonitoring(OcapLocator resolvedLocator) throws HNStreamingException
    {
        //ensure the content description is always set as it is used to respond to the request.  If no CA session could be initiated, use zero as the LTSID
        
        //may be called from INIT, INIT_SUSPENDED or TRANSMIT_SUSPENDED states - caller enforces this contract
        synchronized(m_lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("beginResourceMonitoring - resolved locator: " + resolvedLocator + ", current state: " + 
                        stateToString(m_currentState) + ", current suspended reasons: " + m_suspendedReasons);
            }
            
            //try to retrieve components prior to attempting to start the decrypt session..if components could not be retrieved, return false to caller
            ServiceDetailsExt serviceDetails;
            try
            {
                SIManagerExt siManager = (SIManagerExt) SIManager.createInstance();
                serviceDetails = (ServiceDetailsExt)siManager.getServiceDetails(resolvedLocator)[0];
                if (log.isDebugEnabled()) 
                {
                    log.debug("beginResourceMonitoring - retrieved service details: " + serviceDetails);
                }
            }
            catch (InvalidLocatorException ile)
            {
                throw new HNStreamingException("unable to retrieve service details for locator: " + resolvedLocator, ile);
            }
            catch (SIRequestException sire)
            {
                throw new HNStreamingException("unable to retrieve service details or components for locator: " + resolvedLocator, sire);
            }
            catch (InterruptedException ie)
            {
                throw new HNStreamingException("unable to retrieve service details or components for locator: " + resolvedLocator, ie);
            }

            ServiceComponentExt[] components;
            // Retrieve PCR pid for serviceDetails
            int pcrPid = serviceDetails.getPcrPID();
            if (log.isDebugEnabled()) 
            {
                log.debug("beginResourceMonitoring - retrieved PCR pid: " + pcrPid);
            }
            if ((pcrPid == -1) || (pcrPid == 0x1FFF))
            { 
                throw new HNStreamingException("Invalid PCR pid for serviceDetails.." );
            }
            
            try
            {
                // Include all service components (not just default)
                components = getComponents(serviceDetails);
                if (log.isDebugEnabled()) 
                {
                    log.debug("beginResourceMonitoring - retrieved components: " + Arrays.toString(components));
                }
            }
            catch (InterruptedException e)
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("interrupted retrieving default media components for service details: " + serviceDetails);
                }
                //no CA session, use zero for LTSID
                if (log.isDebugEnabled())
                {
                    log.debug("beginResourceMonitoring - calling cleanup on CASessionMonitor");
                }
                m_caSessionMonitor.cleanup();
                m_contentDescriptionTuner = new HNStreamContentDescriptionTuner(m_networkInterface.getHandle(), resolvedLocator.getFrequency(), (short)0);
                return SUSPENDED_REASON_PMT;    
            }
            catch (SIRequestException e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("SI exception retrieving default media components for service details: " + serviceDetails, e);
                }

                //no CA session, use zero for LTSID
                if (log.isDebugEnabled())
                {
                    log.debug("beginResourceMonitoring - calling cleanup on CASessionMonitor");
                }
                m_caSessionMonitor.cleanup();
                m_contentDescriptionTuner = new HNStreamContentDescriptionTuner(m_networkInterface.getHandle(), resolvedLocator.getFrequency(), (short)0);
                return SUSPENDED_REASON_PMT;
            }

            try
            {
                //create a new CA session if in state INIT or INIT_SUSPENDED, or in TRANSMISSION_SUSPENDED with suspended reasons PMT or RETUNE..otherwise, don't create a new CA session
                boolean newCASession;
                switch (m_currentState)
                {
                    case STATE_INIT:
                    case STATE_INIT_SUSPENDED:
                        newCASession = true;
                        break;
                    case STATE_TRANSMITTING:
                        //unexpected
                        if (log.isWarnEnabled()) 
                        {
                            log.warn("beginResourceMonitoring called in TRANSMITTING state");
                        }
                        newCASession = false;
                        break;
                    case STATE_TRANSMISSION_SUSPENDED:
                        newCASession = m_suspendedReasons.contains(SUSPENDED_REASON_PMT) || m_suspendedReasons.contains(SUSPENDED_REASON_RETUNE);
                        break;
                    case STATE_STOPPED:
                        //unexpected
                        if (log.isWarnEnabled())
                        {
                            log.warn("beginResourceMonitoring called in TRANSMITTING state");
                        }
                        newCASession = false;
                        break;
                    default:
                        if (log.isWarnEnabled()) 
                        {
                            log.warn("beginResourceMonitoring called in unknown state: " + stateToString(m_currentState));
                        }
                        newCASession = false;
                }

                if (log.isDebugEnabled())
                {
                    log.debug("beginResourceMonitoring - calling cleanup on ServiceChangeMonitor");
                }
                //always clean up service change monitor, as it may be monitoring for a previous service (changes due to remap for example)
                //CA and servicechange monitor cleanup no-ops if not already initialized
                m_serviceChangeMonitor.cleanup();
                //only clean up and create a new CA session monitor if current suspended reason includes PMT or RETUNE (as a PMT or RETUNE may result in changed components)
                if (newCASession)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("beginResourceMonitoring - calling cleanup on CASessionMonitor");
                    }
                    m_caSessionMonitor.cleanup();
                }
    
                //initialize with new details
                if (log.isDebugEnabled()) 
                {
                    log.debug("beginResourceMonitoring - initializing ServiceChangeMonitor");
                }
                m_serviceChangeMonitor.initialize(serviceDetails);
                if (newCASession)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("beginResourceMonitoring - calling startDecryptSession on CASessionMonitor");
                    }
                    m_caSessionMonitor.startDecryptSession(serviceDetails, m_networkInterface, components);
                    int compsLength = components.length;
                    int pmtPid = getPMTPid(serviceDetails);
                    // Create new arrays to include PCR and PMT pids
                    int pidArray[] = new int[compsLength + 2];
                    short elemStreamTypesArray[] = new short[compsLength + 2];
                    short mediaStreamTypes[] = new short[compsLength + 2];

                    // Copy audio/video pids
                    for(int i=0; i<compsLength; i++)
                    {
                        pidArray[i] = components[i].getPID();
                        elemStreamTypesArray[i] = components[i].getElementaryStreamType();
                        mediaStreamTypes[i] = streamTypeToMediaStreamType(components[i].getStreamType());
                    }
                    // Add PCR pid
                    pidArray[compsLength] = pcrPid;
                    elemStreamTypesArray[compsLength] = MediaStreamType.PCR;
                    mediaStreamTypes[compsLength] = MediaStreamType.PCR;
                    // Add PMT pid
                    pidArray[(compsLength)+1] = pmtPid;
                    elemStreamTypesArray[(compsLength)+1] = MediaStreamType.PMT;
                    mediaStreamTypes[(compsLength)+1] = MediaStreamType.PMT;
                    
                    m_contentDescriptionTuner = new HNStreamContentDescriptionTuner(m_networkInterface.getHandle(), resolvedLocator.getFrequency(), m_caSessionMonitor.getLTSID(),
                                                                                    pidArray, elemStreamTypesArray, mediaStreamTypes);
                }
    
                if (!(m_caSessionMonitor.getLastCASessionEventID() == CASessionEvent.EventID.FULLY_AUTHORIZED))
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info("CASessionMonitor last session event ID was not fully authorized result: " + m_caSessionMonitor.getLastCASessionEventID());
                    }
                    //a CASessionMonitor exists, either from a previous call to beginResourceMonitoring or this call - contentDescriptionTuner is unchanged
                    return SUSPENDED_REASON_CA;
                }
                
                if (log.isDebugEnabled())
                {
                    log.debug("beginResourceMonitoring - content description: " + m_contentDescriptionTuner);
                }
                return null;
            }
            catch (MPEException mpee)
            {
                throw new HNStreamingException("unable to start decrypt session", mpee);
            }
        }
    }

    private void trackSessionCompletion(final HNServerSession session)
    {
        new Thread(new Runnable()
        {
            public void run()
            {
                session.waitForTransmissionToComplete();
                m_sessionTracker.release();
            }
        }).start();
    }

    /**
     * Release all resources
     * 
     * Called by stop and internally during transitions between sessions in TRANSMISSION_SUSPENDED prior to new session startup.
     * 
     * @param closeSocket parameter to pass in to HNServerSession#releaseResources
     * @param sessionComplete parameter to pass in to HNServerSession#releaseResources 
     */
    private void releaseResources(boolean closeSocket, boolean sessionComplete)
    {
        synchronized(m_lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("releaseResources - closeSocket: " + closeSocket + ", sessionComplete: " + sessionComplete + ", current state: " + stateToString(m_currentState));
            }
            
            if (m_currentState == STATE_STOPPED)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring releaseResources in STOPPED state");
                }
                return;
            }

            //m_session is never null
            m_session.releaseResources(closeSocket, sessionComplete);
    
            //maintain the NI and callback if session is not complete
            if (sessionComplete)
            {
                if (log.isInfoEnabled())
                {
                    log.info("releaseResources - sessionComplete - releasing networkinterfacecontroller and removing callback");
                }
                try
                {
                    if(m_sharableNetworkInterfaceController != null)
                    {
                        m_sharableNetworkInterfaceController.release();
                    }
                }
                catch (NetworkInterfaceException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("unable to release sharableNetworkInterfaceController: " + m_sharableNetworkInterfaceController, e);
                    }
                }
                if (m_networkInterface != null)
                {
                    m_networkInterface.removeNetworkInterfaceCallback(m_networkInterfaceCallback);
                }
            }
        }
    }

    public Integer getConnectionId()
    {
        return m_connectionId;
    }
    
    public String getURL()
    {
        return m_url;
    }
    
    public int getContentLocationType()
    {
        return HNStreamContentLocationType.HN_CONTENT_LOCATION_TUNER;
    }
    
    public String toString()
    {
        return "ChannelStream - connectionId: " + m_connectionId + ", url: " + m_url;
    }

    //don't call while holding m_lock
    private void releaseAndDeauthorize(final boolean deauthorizeNow, final int resultCode, boolean closeSocket)
    {
        //may come in from an asyncEvent, a resourceClient release, or a notifyComplete - no-op if already stopped
        synchronized(m_lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("releaseAndDeauthorize - now: " + deauthorizeNow + " - id: " + m_connectionId + ", url: " + m_url + ", current state: " + stateToString(m_currentState));
            }

            if (m_currentState == STATE_STOPPED)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring releaseAndDeauthorize in STOPPED state");
                }
                return;
            }
        }
        
        // Need to call before stream stops and connection id is reset.
        // For cases where this method is called without the ConnectionManagerService knowledge.
        MediaServer.getInstance().getCMS().removeConnectionInfo(m_connectionId.intValue());
        HNServerSessionManager.getInstance().release(this, closeSocket);
        if (deauthorizeNow)
        {
            HNServerSessionManager.getInstance().deauthorize(this, resultCode);
        }
        else
        {
            HNServerSessionManager.getInstance().scheduleDeauthorize(this, resultCode);
        }
    }

    public HNStreamContentDescription getContentDescription()
    {
        return m_contentDescriptionTuner;
    } 

    private ServiceComponentExt[] getComponents(ServiceDetails serviceDetails) throws InterruptedException,
            SIRequestException
    {
        if (serviceDetails == null)
        {
            return new ServiceComponentExt[0];
        }
        Service service = serviceDetails.getService();
        if (service instanceof SPIService)
        {
            ProviderInstance spi = ((SPIService) service).getProviderInstance();
            ProviderInstance.SelectionSessionWrapper session = (ProviderInstance.SelectionSessionWrapper) spi.getSelectionSession((SPIService)service);
            serviceDetails = session.getMappedDetails();
        }

        ServiceComponent[] comps = ((ServiceDetailsExt) serviceDetails).getComponents();
        if (comps == null || comps.length == 0)
        {
            return new ServiceComponentExt[0];
        }
        return (ServiceComponentExt[]) Arrays.copy(comps, ServiceComponentExt.class);
    }

    class ConnectionCompleteListenerImpl implements ConnectionCompleteListener
    {
        public void notifyComplete(int connectionId)
        {
            if (m_connectionId.intValue() == connectionId)
            {
                if (log.isInfoEnabled())
                {
                    log.info("connectioncomplete: " + connectionId);
                }
            }
            releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, true);
        }

        public String toString()
        {
            return "ConnectionCompleteListenerImpl - id: " + m_connectionId;
        }
    }

    protected class EDListenerImpl implements EDListener
    {
        public void asyncEvent(int eventCode, int eventData1, int eventData2)
        {
            synchronized (m_lock)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("asyncEvent: " + eventCode + ", data1: " + eventData1 + ", data2: " + eventData2 + ", current state: " + stateToString(m_currentState));
                }
            }
            //no need to acquire m_lock's monitor - called methods are responsible for checking state
            switch (eventCode)
            {
                case HNAPI.Event.HN_EVT_END_OF_CONTENT:
                    if (log.isDebugEnabled())
                    {
                        log.debug("endofcontent event received - ignoring");
                    }
                    break;
                case HNAPI.Event.HN_EVT_BEGINNING_OF_CONTENT:
                    if (log.isDebugEnabled())
                    {
                        log.debug("beginningofcontent event received - ignoring");
                    }
                    break;
                case HNAPI.Event.HN_EVT_SESSION_OPENED:
                    if (log.isDebugEnabled())
                    {
                        log.debug("session opened event received - ignoring");
                    }
                    break;
                case MediaAPI.Event.CONTENT_PRESENTING:
                    if (log.isDebugEnabled())
                    {
                        log.debug("content presenting event received = ignoring");
                    }
                    break;
                case HNAPI.Event.HN_EVT_PLAYBACK_STOPPED:
                    if (log.isInfoEnabled())
                    {
                        log.info("playback stopped event received - releasing and scheduling deauthorize");
                    }
                    releaseAndDeauthorize(false, HttpURLConnection.HTTP_OK, false);
                    break;
                case HNAPI.Event.HN_EVT_INACTIVITY_TIMEOUT:
                    if (log.isInfoEnabled())
                    {
                        log.info("playback timeout event received - releasing and scheduling deauthorize");
                    }
                    releaseAndDeauthorize(false, HttpURLConnection.HTTP_OK, true);
                    break;
                case MediaAPI.Event.FAILURE_UNKNOWN:
                    if (log.isInfoEnabled())
                    {
                        log.info("failure unknown: " + eventData1 + ", " + eventData2+ ", releasing and deauthorizing session");
                    }
                    releaseAndDeauthorize(true, HttpURLConnection.HTTP_INTERNAL_ERROR, false);
                    break;
                case HNAPI.Event.HN_EVT_SESSION_CLOSED:
                    if (log.isDebugEnabled())
                    {
                        log.debug("session closed event received - ignoring");
                    }
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unhandled event " + eventCode + "(" + eventData1 + ", " + eventData2 + ") - ignoring");
                    }
                    // ignore
            }
        }
    }
    
    // DLNA Requirement [7.4.16.3]: If using "Limited Random Access Data Availability" Mode=0, 
    // then a Content Source must use increasing values for npt-start-time and first-byte-pos
    // when reporting the available random access data range.
    //
    // But currently not supporting time seek, range or available seek headers due to:
    //
    // DLNA Requirement [7.4.36.4]: If an HTTP Server Endpoint supports either the Range HTTP
    // header or the TimeSeekRange.dlna.org HTTP header with the 
    // "Limited Random Access Data Availability" model for a content binary, 
    // the HTTP Server Endpoint MUST support the getAvailableSeekRange.dlna.org HTTP header field.
    //
    // Since none of these headers are available there is no way to retrieve npt-start-time or
    // first-byte-pos so these can remain constant values and still meet DLNA requirements.
    //
    public long getAvailableSeekEndByte(boolean encrypted) throws HNStreamingException
    {
         // This stream is always presenting at the live point (no random access)
        return -1;
    }

    public Time getAvailableSeekEndTime() throws HNStreamingException
    {
        // This stream is always presenting at the live point (no random access)
        return null;
    }

    public long getAvailableSeekStartByte(boolean encrypted) throws HNStreamingException
    {
        // This stream is always presenting at the live point (no random access)
        return -1;
    }

    public Time getAvailableSeekStartTime() throws HNStreamingException
    {
        // This stream is always presenting at the live point (no random access)
        return null;
    }

    public long getEndByte()
    {
        // This stream is always presenting at the live point (no random access)
        return -1;
    }

    public boolean isTransmitting(ContentItem contentItem)
    {
        synchronized (m_lock)
        {
            return (m_currentState == STATE_TRANSMITTING && m_contentItem.equals(contentItem));
        }
    }

    /**
     * @see org.cablelabs.impl.media.streaming.session.Stream#getRequestedContentLength()
     */
    public long getRequestedContentLength()
    {
        return -1;
    }

    /**
     * @see org.cablelabs.impl.media.streaming.session.Stream#getContentDuration()
     */
    public long getContentDuration()
    {
        return -1;
    }
    
    public int getFrameRateInTrickMode() throws HNStreamingException
    {
        // This stream is always presenting at the live point (no random access)
        return 0;
    }

    public long getStartByte()
    {
        // This stream is always presenting at the live point (no random access)
        return -1;
    }

    // If tuning locator is unknown resolve the locator by invoking ServiceResolutionHandler
    private OcapLocator resolveLocator(OcapLocator channelLocator) throws HNStreamingException
    {
        if (log.isDebugEnabled()) 
        {
            log.debug("resolveLocator: " + channelLocator);
        }
        
        ServiceExt service = null;
        OcapLocator result;

        //see if the Service is already in the SI cache
        SIManager siMgr = SIManager.createInstance();
        try
        {
            service = (ServiceExt)siMgr.getService(channelLocator);
        }
        catch (javax.tv.locator.InvalidLocatorException e)
        {
            // If this is an SPI locator, an invalid locator exception maybe thrown here but we
            // can resolve the locator by calling the ServiceResolutionHandler
            // So..continue
        }

        // The service returned by SIManager will be a regular service or an SPIService
        //If it isn't an SPIService, ensure the resulting locator is transport dependent 
        //by calling transformServiceToLocator.
        //at this point, service won't be null for a regular service, only an SPIService
        if(service != null && !(service instanceof SPIService))
        {
            if (log.isInfoEnabled())
            {
                log.info("resolveLocator - service returned by SIManager for locator was not an SPIService: " + service);
            }

            // This method returns a fpq form locator - input may be a non-SPIService sourceid or fpq locator 
            Locator transformedServiceLocator = ((LocatorFactoryImpl) LocatorFactory.getInstance()).transformServiceToLocator(service);
            try
            {
                result = new OcapLocator(transformedServiceLocator.toExternalForm());
            }
            catch (org.davic.net.InvalidLocatorException e)
            {
                // This should not happen..
                throw new HNStreamingException("Unable to construct locator from external form: " + transformedServiceLocator.toExternalForm(), e);
            }
        }
        else
        {
            if (log.isInfoEnabled()) 
            {
                log.info("resolveLocator - no service returned by SIManager for locator, or returned service was an SPIService");
            }
            
            //SPIService returned by SIManager, or failed to retrieve service
            //resolve tuning locator and re-retrieve service
            Locator tuningLocator = m_contentItem.getTuningLocator();
            if (tuningLocator == null)
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("resolveLocator - tuning locator was null - resolving tuning locator");
                }
                boolean resolved = m_contentItem.resolveTuningLocator();
    
                if(resolved)
                {
                    tuningLocator = m_contentItem.getTuningLocator();
                    try
                    {
                        result = new OcapLocator(tuningLocator.toExternalForm());
                        if (log.isInfoEnabled())
                        {
                            log.info("resolveLocator - tuning locator was null but resolved to: " + result);
                        }
                    }
                    catch (org.davic.net.InvalidLocatorException e)
                    {
                        // This should not happen..
                        throw new HNStreamingException("Unable to construct locator from external form: " + tuningLocator.toExternalForm(), e);
                    }
                }
                else
                {
                    throw new HNStreamingException("resolveLocator - resolveTuningLocator failed");
                }
            }
            else
            {
                try
                {
                    result = new OcapLocator(tuningLocator.toExternalForm());
                    if (log.isInfoEnabled())
                    {
                        log.info("resolveTuningLocator - tuning locator was not null - returning: " + result);
                    }
                }
                catch (org.davic.net.InvalidLocatorException e)
                {
                    // This should not happen..
                    throw new HNStreamingException("Unable to construct locator from external form: " + tuningLocator.toExternalForm(), e);
                }
            }
        }
        return result;
    }

    HNServerSession resumeTransmissionWithNewSession() throws HNStreamingException
    {
        //requires caller to ensure correct state - no state checks here
        synchronized (m_lock)
        {
            HNServerSession newSession = new HNServerSession(MediaServer.getServerIdStr());
            newSession.setListener(new EDListenerImpl());

            newSession.openSession(m_socket, m_chunkedEncodingMode,
                    m_protocolInfo.getProfileId(),
                    m_protocolInfo.getContentFormat(),
                    m_maxTrickModeBandwidth, m_currentDecodePTS,
                    m_maxGOPsPerChunk, m_maxFramesPerGOP,
                    m_useServerSidePacing, getFrameTypesInTrickMode(),
                    m_connectionId.intValue(), m_contentItem);
            newSession.transmit(HNStreamContentLocationType.HN_CONTENT_LOCATION_TUNER, m_contentDescriptionTuner, 1.0f, false, -1, -1,
                    -1, -1, new HNPlaybackCopyControlInfo[]{new HNPlaybackCopyControlInfo((short) -1, true, false, (byte) 0)}, m_transformation);
            if (log.isInfoEnabled())
            {
                log.info("changing state to TRANSMITTING");
            }
            m_currentState = STATE_TRANSMITTING;
            return newSession;
        }
    }

    class NetworkInterfaceCallbackImpl implements NetworkInterfaceCallback
    {
        public void notifyTunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            //ignore
            if (log.isDebugEnabled())
            {
                log.debug("notifyTunePending - ignoring");
            }
        }

        public void notifyTuneComplete(ExtendedNetworkInterface ni, final Object tuneInstance, final boolean success, final boolean isSynced)
        {
            CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            callerContextManager.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    handleNotifyTuneCompleteAsync(tuneInstance, success, isSynced);
                }
            });
        }

        private void handleNotifyTuneCompleteAsync(Object tuneInstance, boolean success, boolean isSynced)
        {
            synchronized (m_lock)
            {
                //ignore notifications for non-current tune instances
                if (m_tuneInstance != tuneInstance)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("notifyTuneComplete - ignoring notification for non-current tune instance - tune instance parameter: " +
                                tuneInstance + ", tuneInstance member: " + m_tuneInstance + ", current state: " + stateToString(m_currentState));
                    }
                    return;
                }

                if (log.isInfoEnabled())
                {
                    log.info("notifyTuneComplete - current state: " + stateToString(m_currentState));
                }

                switch (m_currentState)
                {
                    case STATE_INIT:
                        if (success)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("notifyTuneComplete - success - synced: " + isSynced);
                            }
                            m_tuneSuccess = true;
                            m_tuneTimeoutCondition.setTrue();
                        }
                        else
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("notifyTuneComplete - failed");
                            }
                            m_tuneSuccess = false;
                            m_tuneTimeoutCondition.setTrue();
                        }
                        break;
                    case STATE_INIT_SUSPENDED:
                        //ignore
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyTuneComplete - ignoring notification in INIT_SUSPENDED");
                        }
                        break;
                    case STATE_TRANSMITTING:
                        //ignore
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyTuneComplete - ignoring notification in TRANSMITTING");
                        }
                        break;
                    case STATE_TRANSMISSION_SUSPENDED:
                        //ignore
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyTuneComplete - ignoring notification in TRANSMISSION_SUSPENDED");
                        }
                        break;
                    case STATE_STOPPED:
                        //no-op
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyTuneComplete - ignoring notification in STOPPED");
                        }
                        break;
                    default:
                        if (log.isWarnEnabled())
                        {
                            log.warn("notifyTuneComplete called in unexpected state: " + stateToString(m_currentState));
                        }
                }
            }
        }

        //must not be ran async
        public void notifyRetunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            synchronized (m_lock)
            {
                //ignore notifications for non-current tune instances
                if (m_tuneInstance != tuneInstance)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("notifyRetunePending - ignoring notification for non-current tune instance - tune instance parameter: " +
                                tuneInstance + ", tuneInstance member: " + m_tuneInstance + ", current state: " + stateToString(m_currentState));
                    }
                    return;
                }

                if (log.isInfoEnabled())
                {
                    log.info("notifyRetunePending - current state: " + stateToString(m_currentState));
                }

                switch (m_currentState)
                {
                    case STATE_INIT:
                        //change to init suspended, no need to release resources or re-initialize monitoring
                        if (log.isInfoEnabled())
                        {
                            log.info("changing state to INIT_SUSPENDED");
                        }
                        m_currentState = STATE_INIT_SUSPENDED;
                        m_suspendedReasons.add(SUSPENDED_REASON_RETUNE);
                        break;
                    case STATE_INIT_SUSPENDED:
                        m_suspendedReasons.add(SUSPENDED_REASON_RETUNE);
                        break;
                    case STATE_TRANSMITTING:
                        //release resources and resume monitoring
                        //increment the counting semaphore to block the shutdown of the transmit call...then release the non-NI resources
                        m_sessionTracker.acquire();
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyRetunePending - releasing resources");
                        }
                        releaseResources(false, false);
                        if (log.isInfoEnabled())
                        {
                            log.info("changing state to TRANSMISSION_SUSPENDED");
                        }
                        m_currentState = STATE_TRANSMISSION_SUSPENDED;
                        m_suspendedReasons.add(SUSPENDED_REASON_RETUNE);
                        break;
                    case STATE_TRANSMISSION_SUSPENDED:
                        m_suspendedReasons.add(SUSPENDED_REASON_RETUNE);
                        break;
                    case STATE_STOPPED:
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyRetunePending - ignoring notification in STOPPED");
                        }
                        //no-op
                        break;
                    default:
                        if (log.isWarnEnabled())
                        {
                            log.warn("notifyRetunePending called in unexpected state: " + stateToString(m_currentState));
                        }
                }
            }
        }

        public void notifyRetuneComplete(ExtendedNetworkInterface ni, final Object tuneInstance, final boolean success, final boolean isSynced)
        {
            CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            callerContextManager.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    handleNotifyRetuneCompleteAsync(tuneInstance, success, isSynced);
                }
            });
        }

        private void handleNotifyRetuneCompleteAsync(Object tuneInstance, boolean success, boolean isSynced)
        {
            boolean deauthorize = false;

            synchronized (m_lock)
            {
                //ignore notifications for non-current tune instances
                if (m_tuneInstance != tuneInstance)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("notifyRetuneComplete - ignoring notification for non-current tune instance - tune instance parameter: " +
                                tuneInstance + ", tuneInstance member: " + m_tuneInstance + ", current state: " + stateToString(m_currentState));
                    }
                    return;
                }

                if (log.isInfoEnabled()) 
                {
                    log.info("notifyRetuneComplete - success: " + success + ", isSynced: " + isSynced  + ", current state: " + stateToString(m_currentState));
                }

                switch (m_currentState)
                {
                    case STATE_INIT:
                        //ignore
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyRetuneComplete - ignoring notification in INIT");
                        }
                        break;
                    case STATE_INIT_SUSPENDED:
                        if (m_suspendedReasons.size() == 1 && m_suspendedReasons.contains(SUSPENDED_REASON_RETUNE))
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("changing state to INIT");
                            }
                            m_currentState = STATE_INIT;
                        }
                        m_suspendedReasons.remove(SUSPENDED_REASON_RETUNE);
                        break;
                    case STATE_TRANSMITTING:
                        //ignore
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyRetuneComplete - ignoring notification in TRANSMITTING");
                        }
                        break;
                    case STATE_TRANSMISSION_SUSPENDED:
                        //re-initialize transmission if success and synced...release if retune failed
                        //only proceed if we are synced and retune completed and not otherwise suspended.
                        if (m_suspendedReasons.size() == 1 && m_suspendedReasons.contains(SUSPENDED_REASON_RETUNE))
                        {
                            if (success)
                            {
                                if (isSynced)
                                {
                                    try
                                    {
                                        if (log.isInfoEnabled())
                                        {
                                            log.info("notifyRetuneComplete - success and synced - resuming transmission");
                                        }
                                        OcapLocator resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());
                                        Integer result = beginResourceMonitoring(resolvedLocator);
                                        if (result == null)
                                        {
                                            m_session = resumeTransmissionWithNewSession();
                                            trackSessionCompletion(m_session);
                                        }
                                        else
                                        {
                                            if (log.isInfoEnabled())
                                            {
                                                log.info("beginResourceMonitoring returned: " + suspendedReasonToString(result));
                                            }
                                            //no need to change state
                                            m_suspendedReasons.add(result);
                                        }
                                    }
                                    catch (HNStreamingException e)
                                    {
                                        if (log.isWarnEnabled())
                                        {
                                            log.warn("notifyRetuneComplete - failed to resume transmission - calling releaseAndDeauthorize", e);
                                        }
                                        deauthorize = true;
                                    }
                                }
                                else
                                {
                                    if (log.isInfoEnabled())
                                    {
                                        log.info("notifyRetuneComplete - success but not synced - will wait for sync notification to resume transmission");
                                    }
                                }
                            }
                            else
                            {
                                if (log.isWarnEnabled())
                                {
                                    log.warn("notifyRetuneComplete - not successful - calling releaseAndDeauthorize");
                                }
                                deauthorize = true;
                            }
                        }
                        m_suspendedReasons.remove(SUSPENDED_REASON_RETUNE);
                        break;
                    case STATE_STOPPED:
                        //no-op
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyRetuneComplete - ignoring notification in STOPPED");
                        }
                        break;
                    default:
                        if (log.isWarnEnabled())
                        {
                            log.warn("notifyRetuneComplete called in unexpected state: " + stateToString(m_currentState));
                        }
                }
            }

            if (deauthorize)
            {
                if (log.isInfoEnabled())
                {
                    log.info("notifyRetuneComplete - calling releaseAndDeauthorize");
                }
                releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, false);
            }
        }

        //no need to run async
        public void notifyUntuned(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            boolean deauthorize = false;
            synchronized (m_lock)
            {
                //ignore notifications for non-current tune instances
                if (m_tuneInstance != tuneInstance)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("notifyUntuned - ignoring notification for non-current tune instance - tune instance parameter: " +
                                tuneInstance + ", tuneInstance member: " + m_tuneInstance + ", current state: " + stateToString(m_currentState));
                    }
                    return;
                }

                if (log.isInfoEnabled())
                {
                    log.info("notifyUntuned - current state: " + stateToString(m_currentState));
                }

                switch (m_currentState)
                {
                    case STATE_INIT:
                        deauthorize = true;
                        break;
                    case STATE_INIT_SUSPENDED:
                        deauthorize = true;
                        break;
                    case STATE_TRANSMITTING:
                        deauthorize = true;
                        break;
                    case STATE_TRANSMISSION_SUSPENDED:
                        deauthorize = true;
                        break;
                    case STATE_STOPPED:
                        //no-op
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyUntuned - ignoring notification in STOPPED");
                        }
                        break;
                    default:
                        if (log.isWarnEnabled())
                        {
                            log.warn("notifyUntuned called in unexpected state: " + stateToString(m_currentState));
                        }
                }
            }

            if (deauthorize)
            {
                if (log.isInfoEnabled())
                {
                    log.info("notifyUntuned - calling releaseAndDeauthorize");
                }
                releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, false);
            }
        }

        public void notifySyncAcquired(ExtendedNetworkInterface ni, final Object tuneInstance)
        {
            CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            callerContextManager.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    handleNotifySyncAcquiredAsync(tuneInstance);
                }
            });
        }

        private void handleNotifySyncAcquiredAsync(Object tuneInstance)
        {
            boolean deauthorize = false;
            synchronized (m_lock)
            {
                //ignore notifications for non-current tune instances
                if (m_tuneInstance != tuneInstance)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("notifySyncAcquired - ignoring notification for non-current tune instance - tune instance parameter: " +
                                tuneInstance + ", tuneInstance member: " + m_tuneInstance + ", current state: " + stateToString(m_currentState));
                    }
                    return;
                }

                if (log.isInfoEnabled())
                {
                    log.info("notifySyncAcquired - current state: " + stateToString(m_currentState));
                }

                switch (m_currentState)
                {
                    case STATE_INIT:
                        //ignore
                        if (log.isInfoEnabled())
                        {
                            log.info("notifySyncAcquired - ignoring notification in INIT");
                        }
                        break;
                    case STATE_INIT_SUSPENDED:
                        if (m_suspendedReasons.size() == 1 && m_suspendedReasons.contains(SUSPENDED_REASON_SYNC))
                        {
                            //only in suspended due to sync - update to init
                            if (log.isInfoEnabled())
                            {
                                log.info("changing state to INIT");
                            }
                            m_currentState = STATE_INIT;
                        }
                        m_suspendedReasons.remove(SUSPENDED_REASON_SYNC);
                        break;
                    case STATE_TRANSMITTING:
                        //ignore
                        if (log.isInfoEnabled())
                        {
                            log.info("notifySyncAcquired - ignoring notification in TRANSMITTING");
                        }
                        break;
                    case STATE_TRANSMISSION_SUSPENDED:
                        if (m_suspendedReasons.size() == 1 && m_suspendedReasons.contains(SUSPENDED_REASON_SYNC))
                        {
                            //attempt to re-initialize transmission
                            try
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("notifySyncAcquired - resuming transmission");
                                }
                                OcapLocator resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());
                                Integer result = beginResourceMonitoring(resolvedLocator);
                                if (result == null)
                                {
                                    m_session = resumeTransmissionWithNewSession();
                                    trackSessionCompletion(m_session);
                                }
                                else
                                {
                                    if (log.isInfoEnabled())
                                    {
                                        log.info("beginResourceMonitoring returned: " + suspendedReasonToString(result));
                                    }
                                    //no need to change state
                                    m_suspendedReasons.add(result);
                                }
                            }
                            catch (HNStreamingException e)
                            {
                                if (log.isWarnEnabled())
                                {
                                    log.warn("notifySyncAcquired - unable to resume transmission with new session", e);
                                }
                                deauthorize = true;
                            }
                        }
                        m_suspendedReasons.remove(SUSPENDED_REASON_SYNC);
                        break;
                    case STATE_STOPPED:
                        //no-op
                        if (log.isInfoEnabled())
                        {
                            log.info("notifySyncAcquired - ignoring notification in STOPPED");
                        }
                        break;
                    default:
                        if (log.isWarnEnabled())
                        {
                            log.warn("notifySyncAcquired called in unexpected state: " + stateToString(m_currentState));
                        }
                }
            }
            if (deauthorize)
            {
                if (log.isInfoEnabled())
                {
                    log.info("notifySyncAcquired - calling releaseAndDeauthorize");
                }
                releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, false);
            }
        }

        public void notifySyncLost(ExtendedNetworkInterface ni, final Object tuneInstance)
        {
            CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            callerContextManager.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    handleNotifySyncLostAsync(tuneInstance);
                }
            });
        }

        private void handleNotifySyncLostAsync(Object tuneInstance)
        {
            synchronized (m_lock)
            {
                //ignore notifications for non-current tune instances
                if (m_tuneInstance != tuneInstance)
                {
                    if (log.isDebugEnabled()) 
                    {
                        log.debug("notifySyncLost - ignoring notification for non-current tune instance - tune instance parameter: " + 
                                tuneInstance + ", tuneInstance member: " + m_tuneInstance + ", current state: " + stateToString(m_currentState));
                    }
                    return;
                }

                if (log.isInfoEnabled())
                {
                    log.info("notifySyncLost - current state: " + stateToString(m_currentState));
                }

                switch (m_currentState)
                {
                    case STATE_INIT:
                        //no need to re-initialize monitoring/update session tracker or call release resources, just transition to suspended state
                        if (log.isInfoEnabled())
                        {
                            log.info("changing state to INIT_SUSPENDED");
                        }
                        m_currentState = STATE_INIT_SUSPENDED;
                        m_suspendedReasons.add(SUSPENDED_REASON_SYNC);
                        break;
                    case STATE_INIT_SUSPENDED:
                        //no need to change state, just add reason
                        m_suspendedReasons.add(SUSPENDED_REASON_SYNC);
                        break;
                    case STATE_TRANSMITTING:
                        //stop transmission and transition to suspended
                        //increment the counting semaphore to block the shutdown of the transmit call...then release the non-NI resources
                        m_sessionTracker.acquire();
                        //will recover due to sync acquired
                        if (log.isInfoEnabled())
                        {
                            log.info("notifySyncLost - releasing resources");
                        }
                        releaseResources(false, false);
                        if (log.isInfoEnabled())
                        {
                            log.info("changing state to TRANSMISSION_SUSPENDED");
                        }
                        m_currentState = STATE_TRANSMISSION_SUSPENDED;
                        m_suspendedReasons.add(SUSPENDED_REASON_SYNC);
                        break;
                    case STATE_TRANSMISSION_SUSPENDED:
                        //no need to change state, just add reason
                        m_suspendedReasons.add(SUSPENDED_REASON_SYNC);
                        break;
                    case STATE_STOPPED:
                        //no-op
                        if (log.isInfoEnabled())
                        {
                            log.info("notifySyncLost - ignoring notification in STOPPED");
                        }
                        break;
                    default:
                        if (log.isWarnEnabled())
                        {
                            log.warn("notifySyncLost called in unexpected state: " + stateToString(m_currentState));
                        }
                }
            }
        }
    }

    class ResourceClientImpl implements ResourceClient
    {
        public void notifyRelease(ResourceProxy proxy)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyRelease: " + proxy);
            }
        }

        //must not be ran async
        public void release(ResourceProxy proxy)
        {
            if (log.isDebugEnabled())
            {
                log.debug("release: " + proxy);
            }
            boolean deauthorize = false;
            synchronized(m_lock)
            {
                switch (m_currentState)
                {
                    case STATE_INIT:
                        deauthorize = true;
                        break;
                    case STATE_INIT_SUSPENDED:
                        deauthorize = true;
                        break;
                    case STATE_TRANSMITTING:
                        deauthorize = true;
                        break;
                    case STATE_TRANSMISSION_SUSPENDED:
                        deauthorize = true;
                        break;
                    case STATE_STOPPED:
                        if (log.isInfoEnabled())
                        {
                            log.info("release - ignoring notification in STOPPED");
                        }
                        break;
                    default:
                        if (log.isWarnEnabled())
                        {
                            log.warn("release called in unexpected state: " + stateToString(m_currentState));
                        }
                }
            }
            if (deauthorize)
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("release - calling releaseAndDeauthorize");
                }
                releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, false);
            }
        }

        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            if (log.isInfoEnabled())
            {
                log.info("requestRelease, returning false - proxy: " + proxy);
            }
            return false;
        }
    }

    private class CASessionListenerImpl implements CASessionListener
    {
        public void notifyCASessionChange(CASession session, final CASessionEvent event)
        {
            CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            callerContextManager.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    handleNotifyCASessionChangeAsync(event);
                }
            });
        }

        private void handleNotifyCASessionChangeAsync(CASessionEvent event)
        {
            boolean deauthorize = false;
            synchronized (m_lock)
            {
                if (log.isInfoEnabled())
                {
                    log.info("notifyCASessionChange - event: 0x" + Integer.toHexString(event.getEventID()) + ", current state: " + stateToString(m_currentState));
                }
                switch (m_currentState)
                {
                    case STATE_INIT:
                        //re-initialize monitoring, don't begin transmission - transmit has not yet been called
                        try
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("notifyCASessionChange - re-initializing resource monitoring");
                            }
                            OcapLocator resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());
                            Integer result = beginResourceMonitoring(resolvedLocator);
                            if (result != null)
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("beginResourceMonitoring returned: " + suspendedReasonToString(result));
                                }
                                if (log.isInfoEnabled())
                                {
                                    log.info("changing state to INIT_SUSPENDED");
                                }
                                m_currentState = STATE_INIT_SUSPENDED;
                                m_suspendedReasons.add(result);
                            }
                        }
                        catch (HNStreamingException e)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("notifyCASessionChange - unable to re-initialize resource monitoring", e);
                            }
                            deauthorize = true;
                        }
                        break;
                    case STATE_INIT_SUSPENDED:
                        //re-initialize monitoring because CA may now be not fully authorized and then fail, don't begin transmission - transmit has not yet been called
                        try
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("notifyCASessionChange - re-initializing resource monitoring");
                            }
                            OcapLocator resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());
                            Integer result = beginResourceMonitoring(resolvedLocator);
                            if (result != null)
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("beginResourceMonitoring returned: " + suspendedReasonToString(result));
                                }
                                //no need to change state, just add reason
                                m_suspendedReasons.add(result);
                            }
                        }
                        catch (HNStreamingException e)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("notifyCASessionChange - unable to re-initialize resource monitoring", e);
                            }
                            deauthorize = true;
                        }
                        break;
                    case STATE_TRANSMITTING:
                        //release resources and re-initialize transmission
                        m_sessionTracker.acquire();
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyCASessionChange - releasing resources");
                        }
                        releaseResources(false, false);
                        if (log.isInfoEnabled())
                        {
                            log.info("changing state to TRANSMISSION_SUSPENDED");
                        }
                        m_currentState = STATE_TRANSMISSION_SUSPENDED;
                        m_suspendedReasons.add(SUSPENDED_REASON_CA);
                        try
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("notifyCASessionChange - resuming transmission");
                            }
                            OcapLocator resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());
                            Integer result = beginResourceMonitoring(resolvedLocator);
                            if (result == null)
                            {
                                m_session = resumeTransmissionWithNewSession();
                                trackSessionCompletion(m_session);
                                //success - remove CA suspended reason
                                m_suspendedReasons.remove(SUSPENDED_REASON_CA);
                            }
                            else
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("beginResourceMonitoring returned: " + suspendedReasonToString(result));
                                }
                                //no need to change state
                                //if reason was not CA, remove CA reason added above
                                if (!SUSPENDED_REASON_CA.equals(result))
                                {
                                    m_suspendedReasons.remove(SUSPENDED_REASON_CA);
                                }
                                //add new result
                                m_suspendedReasons.add(result);
                            }
                        }
                        catch (HNStreamingException e)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("notifyCASessionChange - unable to resume transmission with new session", e);
                            }
                            deauthorize = true;
                        }
                        break;
                    case STATE_TRANSMISSION_SUSPENDED:
                        //attempt monitoring and transmission if only suspended reason is CA
                        if (m_suspendedReasons.size() == 1 && m_suspendedReasons.contains(SUSPENDED_REASON_CA))
                        {
                            try
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("notifyCASessionChange - re-initializing resource monitoring");
                                }
                                OcapLocator resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());
                                Integer result = beginResourceMonitoring(resolvedLocator);
                                //re-attempted monitoring and no failure (CA or PMT) - if only current reason for failure was CA, resume transmission
                                if (result == null)
                                {
                                    m_session = resumeTransmissionWithNewSession();
                                    trackSessionCompletion(m_session);
                                    m_suspendedReasons.remove(SUSPENDED_REASON_CA);
                                }
                                else
                                {
                                    if (log.isInfoEnabled())
                                    {
                                        log.info("beginResourceMonitoring returned: " + suspendedReasonToString(result));
                                    }
                                    //no need to change state
                                    if (!SUSPENDED_REASON_CA.equals(result))
                                    {
                                        //remove CA reason if not current reason
                                        m_suspendedReasons.remove(SUSPENDED_REASON_CA);
                                    }
                                    m_suspendedReasons.add(result);
                                }
                            }
                            catch (HNStreamingException e)
                            {
                                if (log.isWarnEnabled())
                                {
                                    log.warn("notifyCASessionChange - unable to re-initialize resource monitoring", e);
                                }
                                deauthorize = true;
                            }
                        }
                        break;
                    case STATE_STOPPED:
                        //no-op
                        if (log.isInfoEnabled())
                        {
                            log.info("notifyCASessionChange - ignoring notification in STOPPED");
                        }
                        break;
                    default:
                        if (log.isWarnEnabled()) 
                        {
                            log.warn("notifyCASessionChange called in unexpected state: " + stateToString(m_currentState));
                        }
                }
            }

            if (deauthorize)
            {
                if (log.isInfoEnabled())
                {
                    log.info("notifyCASessionChange - calling releaseAndDeauthorize");
                }
                releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, false);
            }
        }
    }

    private class ServiceChangeListenerImpl implements ServiceChangeListener
    {
        public void serviceChangeEvent(final int event)
        {
            CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            callerContextManager.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    handleServiceChangeEventAsync(event);
                }
            });
        }

        private void handleServiceChangeEventAsync(int event)
        {
            boolean deauthorize = false;
            synchronized (m_lock)
            {
                if (log.isInfoEnabled())
                {
                    log.info("serviceChangeEvent: " + serviceChangeEventToString(event) + ", current state: " + stateToString(m_currentState));
                }

                switch (m_currentState)
                {
                    case STATE_INIT:
                        switch (event)
                        {
                            case ServiceChangeEvent.PMT_REMOVED:
                                //change to suspended - already monitoring - don't release resources or re-initialize monitoring
                                if (log.isInfoEnabled())
                                {
                                    log.info("changing state to INIT_SUSPENDED");
                                }
                                m_currentState = STATE_INIT_SUSPENDED;
                                m_suspendedReasons.add(SUSPENDED_REASON_PMT);
                                break;
                            case ServiceChangeEvent.PMT_CHANGED:
                                //re-initialize monitoring but don't begin transmission
                                try
                                {
                                    if (log.isInfoEnabled())
                                    {
                                        log.info("serviceChangeEvent - re-initializing resource monitoring");
                                    }
                                    if (log.isInfoEnabled())
                                    {
                                        log.info("changing state to INIT_SUSPENDED");
                                    }
                                    m_currentState = STATE_INIT_SUSPENDED;
                                    m_suspendedReasons.add(SUSPENDED_REASON_PMT);
                                    OcapLocator resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());
                                    Integer result = beginResourceMonitoring(resolvedLocator);
                                    if (result == null)
                                    {
                                        if (log.isInfoEnabled())
                                        {
                                            log.info("changing state to INIT");
                                        }
                                        m_currentState = STATE_INIT;
                                        m_suspendedReasons.remove(SUSPENDED_REASON_PMT);
                                    }
                                    else
                                    {
                                        if (log.isInfoEnabled())
                                        {
                                            log.info("beginResourceMonitoring returned: " + suspendedReasonToString(result));
                                        }
                                        //no need to change state
                                        if (!SUSPENDED_REASON_PMT.equals(result))
                                        {
                                            //remove PMT reason added above if not current reason
                                            m_suspendedReasons.remove(SUSPENDED_REASON_PMT);
                                        }
                                        //add result (may be CA or PMT)
                                        m_suspendedReasons.add(result);
                                    }
                                }
                                catch (HNStreamingException e)
                                {
                                    if (log.isWarnEnabled())
                                    {
                                        log.warn("serviceChangeEvent - unable to re-initialize resource monitoring", e);
                                    }
                                    deauthorize = true;
                                }
                                break;
                            default:
                                if (log.isWarnEnabled())
                                {
                                    log.warn("serviceChangeEvent - unknown event type - ignoring: " + event);
                                }
                        }
                        break;
                    case STATE_INIT_SUSPENDED:
                        switch (event)
                        {
                            case ServiceChangeEvent.PMT_REMOVED:
                                //add reason - monitoring/transmission will resume when another notification is received
                                m_suspendedReasons.add(SUSPENDED_REASON_PMT);
                                break;
                            case ServiceChangeEvent.PMT_CHANGED:
                                //TODO: not adding PMT as suspended reason if already suspended for other reasons...ok?? others??
                                //if only reason suspended is PMT, attempt monitoring
                                if (m_suspendedReasons.size() == 1 && m_suspendedReasons.contains(SUSPENDED_REASON_PMT))
                                {
                                    //re-initiate monitoring and if successful, go back to INIT - don't begin transmission
                                    try
                                    {
                                        if (log.isInfoEnabled())
                                        {
                                            log.info("serviceChangeEvent - resuming transmission");
                                        }
                                        OcapLocator resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());
                                        Integer result = beginResourceMonitoring(resolvedLocator);
                                        if (result == null)
                                        {
                                            if (log.isInfoEnabled())
                                            {
                                                log.info("changing state to INIT");
                                            }
                                            m_currentState = STATE_INIT;
                                            m_suspendedReasons.remove(SUSPENDED_REASON_PMT);
                                        }
                                        else
                                        {
                                            if (log.isInfoEnabled())
                                            {
                                                log.info("beginResourceMonitoring returned: " + suspendedReasonToString(result));
                                            }
                                            //no need to change state, just add reason (may be CA or PMT)
                                            m_suspendedReasons.add(result);
                                        }
                                    }
                                    catch (HNStreamingException e)
                                    {
                                        if (log.isWarnEnabled())
                                        {
                                            log.warn("serviceChangeEvent - unable to resume transmission with new session", e);
                                        }
                                        deauthorize = true;
                                    }
                                }
                                break;
                            default:
                                if (log.isWarnEnabled())
                                {
                                    log.warn("serviceChangeEvent - unknown event type - ignoring: " + event);
                                }
                        }
                        break;
                    case STATE_TRANSMITTING:
                        switch (event)
                        {
                            case ServiceChangeEvent.PMT_REMOVED:
                                //may be recovered by reception of PMT_CHANGED
                                //increment the semaphore count
                                m_sessionTracker.acquire();
                                if (log.isInfoEnabled())
                                {
                                    log.info("serviceChangeEvent - releasing resources");
                                }
                                releaseResources(false, false);
                                if (log.isInfoEnabled())
                                {
                                    log.info("changing state to TRANSMISSION_SUSPENDED");
                                }
                                m_currentState = STATE_TRANSMISSION_SUSPENDED;
                                m_suspendedReasons.add(SUSPENDED_REASON_PMT);
                                break;
                            case ServiceChangeEvent.PMT_CHANGED:
                                //re-start transmission with updated PMT
                                //may be received after a PMT REMOVE, or may just a change in components
                                //shut down the old transmission
                                m_sessionTracker.acquire();
                                if (log.isInfoEnabled())
                                {
                                    log.info("serviceChangeEvent - releasing resources");
                                }
                                releaseResources(false, false);
                                if (log.isInfoEnabled())
                                {
                                    log.info("changing state to TRANSMISSION_SUSPENDED");
                                }
                                m_currentState = STATE_TRANSMISSION_SUSPENDED;
                                m_suspendedReasons.add(SUSPENDED_REASON_PMT);
                                try
                                {
                                    if (log.isInfoEnabled())
                                    {
                                        log.info("serviceChangeEvent - resuming transmission");
                                    }
                                    OcapLocator resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());
                                    Integer result = beginResourceMonitoring(resolvedLocator);
                                    if (result == null)
                                    {
                                        m_session = resumeTransmissionWithNewSession();
                                        trackSessionCompletion(m_session);
                                        m_suspendedReasons.remove(SUSPENDED_REASON_PMT);
                                    }
                                    else
                                    {
                                        if (log.isInfoEnabled())
                                        {
                                            log.info("beginResourceMonitoring returned: " + suspendedReasonToString(result));
                                        }
                                        if (log.isInfoEnabled())
                                        {
                                            log.info("changing state to INIT_SUSPENDED");
                                        }
                                        //no need to change state
                                        if (!SUSPENDED_REASON_PMT.equals(result))
                                        {
                                            //remove PMT reason added above if not current reason
                                            m_suspendedReasons.remove(SUSPENDED_REASON_PMT);
                                        }
                                        m_suspendedReasons.add(result);
                                    }
                                }
                                catch (HNStreamingException e)
                                {
                                    if (log.isWarnEnabled())
                                    {
                                        log.warn("serviceChangeEvent - unable to resume transmission with new session", e);
                                    }
                                    deauthorize = true;
                                }
                                break;
                            default:
                                if (log.isWarnEnabled())
                                {
                                    log.warn("serviceChangeEvent - unknown event type - ignoring: " + event);
                                }
                        }
                        break;
                    case STATE_TRANSMISSION_SUSPENDED:
                        switch (event)
                        {
                            case ServiceChangeEvent.PMT_REMOVED:
                                m_suspendedReasons.add(SUSPENDED_REASON_PMT);
                                break;
                            case ServiceChangeEvent.PMT_CHANGED:
                                //if only reason suspended is PMT, attempt resource monitoring and transmission
                                if (m_suspendedReasons.size() == 1 && m_suspendedReasons.contains(SUSPENDED_REASON_PMT))
                                {
                                    //re-initiate monitoring and go back to TRANSMITTING
                                    try
                                    {
                                        if (log.isInfoEnabled())
                                        {
                                            log.info("serviceChangeEvent - resuming transmission");
                                        }
                                        OcapLocator resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());
                                        Integer result = beginResourceMonitoring(resolvedLocator);
                                        if (result == null)
                                        {
                                            m_session = resumeTransmissionWithNewSession();
                                            trackSessionCompletion(m_session);
                                            m_suspendedReasons.remove(SUSPENDED_REASON_PMT);
                                        }
                                        else
                                        {
                                            if (log.isInfoEnabled())
                                            {
                                                log.info("beginResourceMonitoring returned: " + suspendedReasonToString(result));
                                            }
                                            //no need to change state
                                            if (!SUSPENDED_REASON_PMT.equals(result))
                                            {
                                                //remove PMT reason added above if not current reason
                                                m_suspendedReasons.remove(SUSPENDED_REASON_PMT);
                                            }
                                            m_suspendedReasons.add(result);
                                        }
                                    }
                                    catch (HNStreamingException e)
                                    {
                                        if (log.isWarnEnabled())
                                        {
                                            log.warn("serviceChangeEvent - unable to resume transmission with new session", e);
                                        }
                                        deauthorize = true;
                                    }
                                }
                                break;
                            default:
                                if (log.isWarnEnabled())
                                {
                                    log.warn("serviceChangeEvent - unknown event type - ignoring: " + event);
                                }
                        }
                            
                        //nothing to do - monitoring/transmission will resume when another notification is received
                        if (log.isInfoEnabled())
                        {
                            log.info("serviceChangeEvent - ignoring notification in TRANSMISSION_SUSPENDED");
                        }
                        
                        break;
                    case STATE_STOPPED:
                        //no-op
                        if (log.isInfoEnabled())
                        {
                            log.info("serviceChangeEvent - ignoring notification in STOPPED");
                        }
                        break;
                    default:
                        if (log.isWarnEnabled())
                        {
                            log.warn("serviceChangeEvent called in unexpected state: " + stateToString(m_currentState));
                        }
                        break;
                }
            }
            if (deauthorize)
            {
                if (log.isInfoEnabled())
                {
                    log.info("serviceChangeEvent - calling releaseAndDeauthorize");
                }
                releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, false);
            }
        }
    }

    private String suspendedReasonToString(Integer result)
    {
        if (SUSPENDED_REASON_PMT.equals(result))
        {
            return "SUSPENDED_REASON_PMT";
        }
        if (SUSPENDED_REASON_CA.equals(result))
        {
            return "SUSPENDED_REASON_CA";
        }
        if (SUSPENDED_REASON_RETUNE.equals(result))
        {
            return "SUSPENDED_REASON_RETUNE";
        }
        if (SUSPENDED_REASON_SYNC.equals(result))
        {
            return "SUSPENDED_REASON_SYNC";
        }
        return "UNKNOWN SUSPENDED REASON: " + result;
    }

    private String serviceChangeEventToString(int event)
    {
        switch (event)
        {
            case ServiceChangeEvent.PMT_CHANGED:
                return "PMT CHANGED (add or modify)";
            case ServiceChangeEvent.PMT_REMOVED:
                return "PMT REMOVED";
            default:
                return "Unknown event: " + event;
        }
    }

    private String stateToString(int currentState)
    {
        switch (currentState)
        {
            case STATE_INIT:
                return "INIT";
            case STATE_INIT_SUSPENDED:
                return "INIT_SUSPENDED";
            case STATE_STOPPED:
                return "STOPPED";
            case STATE_TRANSMITTING:
                return "TRANSMITTING";
            case STATE_TRANSMISSION_SUSPENDED:
                return "TRANSMISSION_SUSPENDED";
            default:
                return "Unknown state: " + currentState;
        }
    }

    public static short streamTypeToMediaStreamType(StreamType strType)
    {
        if (strType == StreamType.VIDEO)
        {
            return MediaStreamType.VIDEO;
        }
        if (strType == StreamType.AUDIO)
        {
            return MediaStreamType.AUDIO;
        }
        if (strType == StreamType.DATA)
        {
            return MediaStreamType.DATA;
        }
        if (strType == StreamType.SUBTITLES)
        {
            return MediaStreamType.SUBTITLES;
        }
        if (strType == StreamType.SECTIONS)
        {
            return MediaStreamType.SECTIONS;
        }
        return MediaStreamType.UNKNOWN;
    } // END streamTypeToMediaStreamType()

    private int getPMTPid(ServiceDetailsExt serviceDetails)
    {
        //
        // Get the PMT PID 
        //
        TransportStreamExt tsExt = (TransportStreamExt) serviceDetails.getTransportStream();

        OcapLocator transportDepLocator;
        try 
        {
            transportDepLocator = new OcapLocator(tsExt.getFrequency(), serviceDetails.getProgramNumber(),
                    tsExt.getModulationFormat());
        } 
        catch (org.davic.net.InvalidLocatorException e) 
        {
            if (log.isWarnEnabled())
            {
                log.warn("getPMTPid: Could not create OcapLocator for serviceDetails.");
            }
            return -1;
        }
        
        ProgramAssociationTableManager patm = ProgramAssociationTableManager.getInstance();

        class PATRequestor implements SIRequestor
        {
            public ProgramAssociationTable pat = null;
            private final SimpleCondition patAcquired = new SimpleCondition(false);

            public void notifyFailure(SIRequestFailureType reason)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getPMTPid: PATRequestor::notifyFailure called");
                }
                patAcquired.setTrue();
            }

            public void notifySuccess(SIRetrievable[] result)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getPMTPid: PATRequestor::notifySuccess called");
                }
                if (result != null)
                    pat = (ProgramAssociationTable) result[0];
                patAcquired.setTrue();
            }

            public ProgramAssociationTable getPAT()
            {
                try
                {
                    patAcquired.waitUntilTrue(10000);
                }
                catch (InterruptedException e)
                {
                }

                return pat;
            }
        } // END class PATRequestor

        PATRequestor patRequestor = new PATRequestor();

        // Retrieve In-band PAT
        SIRequest siRequest = patm.retrieveInBand(patRequestor, transportDepLocator);

        int pmtPid = -1;
        if (patRequestor.getPAT() != null)
        {
            PATProgram[] programs = patRequestor.getPAT().getPrograms();
            for (int i = 0; i < programs.length; i++)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getPMTPid: Program[" + i + "]: "
                            + programs[i].getProgramNumber());
                }
                if (programs[i].getProgramNumber() == serviceDetails.getProgramNumber())
                {
                    pmtPid = programs[i].getPID();
                    break;
                }
            }
        }

        if (pmtPid == -1)
        {
            if (log.isWarnEnabled())
            {
                log.warn("getPMTPid: Could not find the PMT PID for " + transportDepLocator);
            }
        }
        else
        {
            return pmtPid;
        }

        return -1;
    }
}
