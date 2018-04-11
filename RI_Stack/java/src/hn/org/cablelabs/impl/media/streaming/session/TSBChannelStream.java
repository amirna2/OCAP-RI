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
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.media.Time;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.LocatorFactory;
import javax.tv.service.SIManager;

import javax.tv.service.Service;
import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.TimeShiftBuffer;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowChangedListener;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.TimeShiftWindowStateChangedEvent;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.mpe.HNAPI;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.exception.HNStreamingRangeException;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackCopyControlInfo;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescription;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescriptionTSB;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentLocationType;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.media.streaming.session.util.ContentRequestConstant;
import org.cablelabs.impl.media.streaming.session.util.StreamUtil;
import org.cablelabs.impl.ocap.hn.ContentServerNetModuleImpl;
import org.cablelabs.impl.ocap.hn.NetResourceUsageImpl;
import org.cablelabs.impl.ocap.hn.content.ChannelContentItemImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.cm.ConnectionCompleteListener;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.util.LocatorFactoryImpl;
import org.cablelabs.impl.util.SimpleCondition;
import org.cablelabs.impl.util.TimeTable;
import org.davic.net.Locator;
import org.davic.net.tuning.NoFreeInterfaceException;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.StreamingActivityListener;
import org.ocap.hn.resource.NetResourceUsage;
import org.ocap.net.OcapLocator;

public class TSBChannelStream implements Stream, TimeShiftWindowChangedListener
{
    private static final Logger log = Logger.getLogger(TSBChannelStream.class);

    // Tri-state start-up condition
    // 1. success 
    // 2. failure due to tuning (retry) 
    // 3. failure due to other reasons (non re-try)
    private static final int START_UP_SUCCESS = 0;
    private static final int START_UP_RETRY = 1;
    private static final int START_UP_FAIL = 2;

    private static final long BUFFERING_START_TIMEOUT_MILLIS = 30000;

    private static final int STATE_INIT = 1;
    private static final int STATE_TRANSMITTING = 2;
    private static final int STATE_STOPPED = 3;

    private final SimpleCondition m_tswInitializedCondition = new SimpleCondition(false);

    private final ChannelContentItemImpl m_contentItem;
    private final ContentRequest request;
    private final HNServerSession session;
    private final Integer connectionId;
    private final Socket socket;
    private final String url;

    private final String m_logPrefix;
    
    //track current transmitting segment and byte range
    private final TransmitInfo transmitInfo;
    private final HNStreamProtocolInfo protocolInfo;
    private final float requestedRate;
    private final int requestedFrameTypes;
    private final int contentLocationType;

    private final ConnectionCompleteListener connectionCompleteListener;

    private int m_startUpError = START_UP_SUCCESS;
    
    //fields that are assigned during initialization but not in the constructor
    private TimeShiftWindowClient m_tswClient = null;
    private OcapLocator resolvedLocator = null;
    private String m_bufferingStartupError = null;

    private final Object lock = new Object();
    
    private int currentState = STATE_INIT;

    /**
     * Constructor - creates and opens an HNServerSession.
     * 
     * The ChannelRequestInterceptor uses reflection to construct this Stream implementation (or the non-dvr live streaming implementation)
     * 
     * @param channelItem the ChannelContentItemImpl
     * @param request the content request
     * @throws HNStreamingException if unable to build the stream
     */
    public TSBChannelStream(ChannelContentItemImpl channelItem, ContentRequest request) throws HNStreamingException, HNStreamingRangeException
    {
        m_logPrefix = "TSBCS 0x" + Integer.toHexString(hashCode()) + ": ";
        this.m_contentItem = channelItem;
        this.request = request;
        this.url = request.getURI();
        
        if (log.isInfoEnabled())
        {
            log.info("constructing TSBChannelStream - id: " + request.getConnectionId() + ", url: " + url);
        }
        
        // Following two initialization steps should be repeated till 
        // attempts to resolve locator and initialize TSW succeed or fail.
        // A re-try is attempted if the call to initTSW() results in a tune 
        // failed but notification from SRH requires tune to be re-attempted.
        // Need to also re-retrieve the tuning locator
        do
        {
            if(m_startUpError == START_UP_RETRY)
            {
                if (log.isInfoEnabled())
                {
                    log.info("TSBChannelStream: retrying resolve locator and initTSW..");
                }
                // Reset the condition variable
                m_tswInitializedCondition.setFalse();
            }
            // Resolve tuning locator
            resolvedLocator = resolveLocator(m_contentItem.getChannelLocator());
            try
            {
                //verify retrieval does not fail - no need to hold a ref to the service - pass in the channellocator
                Service service = SIManager.createInstance().getService(m_contentItem.getChannelLocator());
                initTSW(request, service);
            }
            catch (InvalidLocatorException e1)
            {
                throw new HNStreamingException("Unable to verify service after resolving channel locator: " + m_contentItem.getChannelLocator(), e1);
            }
        }
        while (m_startUpError == START_UP_RETRY);
           
        if(m_startUpError == START_UP_FAIL)
        {
            if (log.isDebugEnabled())
            {
                log.debug( m_logPrefix + "TSBChannelStream failed to initialize" );
            }
            throw new IllegalArgumentException("TSBChannelStream failed to initialize");
        }

        contentLocationType = HNStreamContentLocationType.HN_CONTENT_LOCATION_LOCAL_TSB;

        protocolInfo = request.getProtocolInfo();
        requestedRate = request.getRate();
        requestedFrameTypes = request.getRequestedFrameTypesInTrickMode();

        //both range and timeseekrange may be open-ended (end time -1)
        if (request.isRangeHeaderIncluded() || request.isDtcpRangeHeaderIncluded())
        {
            //restricted range format (7.4.38.3) means start is required...http range start must be be less than end
            final long requestedStartBytePosition;
            //requestedEndBytePosition may be -1
            final long requestedEndBytePosition;
            requestedStartBytePosition = request.getStartBytePosition();
            //requestedEndBytePosition may be -1
            requestedEndBytePosition = request.getEndBytePosition();

            if (log.isInfoEnabled())
            {
                log.info("range header provided - startNanos: " + request.getTimeSeekStartNanos() + ", requested startByte: " + requestedStartBytePosition +
                        ", endNanos: " + request.getTimeSeekEndNanos() + ", requested endByte: " + requestedEndBytePosition + ", rate: " + requestedRate);
            }
            transmitInfo = getTransmitInfoForRange(requestedStartBytePosition, requestedEndBytePosition, requestedRate);
        }
        else if (request.isTimeSeekRangeHeaderIncluded())
        {
            if (log.isInfoEnabled()) 
            {
                log.info("timeSeekRange header provided - resolving start time position for startNanos: " + request.getTimeSeekStartNanos() + 
                        ", endNanos: " + request.getTimeSeekEndNanos() + ", rate: " + requestedRate);
            }
            //timeseekrange format (7.4.40.3) start required, end optional, start must be less than end in forward scan, 
            //end must be less than start in backward scan 
            //timeseekrange header includes bytes= field, need to resolve byte offsets for the given
            //time values
            long requestedStartTimePosition = request.getTimeSeekStartNanos();
            long requestedStartBytePosition = getByteOffsetForCombinedTSBsNanos(request.getTimeSeekStartNanos());
            //time seek end may be -1

            long requestedEndTimePosition = request.getTimeSeekEndNanos() > -1 ? request.getTimeSeekEndNanos() : -1;
            long requestedEndBytePosition = request.getTimeSeekEndNanos() > -1 ? getByteOffsetForCombinedTSBsNanos(request.getTimeSeekEndNanos()) : request.getTimeSeekEndNanos();
            //normalize positions based on time and rate                             
            if (log.isInfoEnabled()) 
            {
                log.info("timeSeekRange header provided - resolved time positions for startNanos: " + request.getTimeSeekStartNanos() + ", start time: " + requestedStartTimePosition +
                        ", endNanos: " + request.getTimeSeekEndNanos() + ", end time: " + requestedEndTimePosition + ", rate: " + requestedRate);
            }
            if (log.isInfoEnabled())
            {
                log.info("timeSeekRange header provided - requested startByte: " + requestedStartBytePosition
                        + ", requested endByte: " + requestedEndBytePosition);
            }

            transmitInfo = getTransmitInfoForTimeRange(requestedStartTimePosition, requestedEndTimePosition, requestedStartBytePosition, requestedEndBytePosition, requestedRate);
        }
        else
        {
            //no requested start or end byte position
            transmitInfo = new TransmitInfo(requestedRate);

            if (log.isInfoEnabled()) 
            {
                log.info("both range and timeSeekRangeHeader not provided - rate: " + requestedRate);
            }
        }
        if (log.isInfoEnabled())
        {
            log.info("initial transmit info: " + transmitInfo);
        }

        connectionId = request.getConnectionId();
        session = new HNServerSession(MediaServer.getServerIdStr());

        socket = request.getSocket();
        
        //listener released in session#releaseResources - no need to hold a reference 
        session.setListener(new EDListenerImpl());

        connectionCompleteListener = new ConnectionCompleteListenerImpl(connectionId);
        MediaServer.getInstance().getCMS().addConnectionCompleteListener(connectionCompleteListener);
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
    
    // Initialize resource usage and acquire TimeShiftWindow
    public final void initTSW(final ContentRequest request, Service service)
    {        
        TimeShiftManager tsm = (TimeShiftManager) ManagerManager.getInstance(TimeShiftManager.class);
        
        //
        // Buffering discovering/initiation
        //        
        NetResourceUsage resourceUsage = new NetResourceUsageImpl(request.getRequestInetAddress(),
                                            NetResourceUsage.USAGE_TYPE_PRESENTATION,
                                            resolvedLocator,
                                            null );
        if (log.isDebugEnabled())
        {
            log.debug("ocapLocator: " + resolvedLocator + ", service: " + service);
        }
        
        try
        {
            m_tswClient  = tsm.getTSWByService( service,
                                                 TimeShiftManager.TSWUSE_BUFFERING
                                                 | TimeShiftManager.TSWUSE_NETPLAYBACK, 
                                                 resourceUsage, this, TimeShiftManager.LISTENER_PRIORITY_LOW );
        }
        catch (NoFreeInterfaceException nfie)
        {
            throw new IllegalArgumentException( "Failed to acquire a NI: "
                                                + nfie.toString());
        }

        int tswState = m_tswClient.getState();
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "constructor: Got TSW in state " + TimeShiftManager.stateString[tswState] );
        }
        
        switch (tswState)
        {
            case TimeShiftManager.TSWSTATE_BUFFERING:
            {
                m_tswClient.attachFor(TimeShiftManager.TSWUSE_BUFFERING);
                m_tswInitializedCondition.setTrue();
                m_startUpError = START_UP_SUCCESS;
                break;
            }
            case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
            {
                m_tswClient.attachFor(TimeShiftManager.TSWUSE_BUFFERING);
                // FALL THROUGH
            }
            case TimeShiftManager.TSWSTATE_RESERVE_PENDING:
            case TimeShiftManager.TSWSTATE_TUNE_PENDING:
            case TimeShiftManager.TSWSTATE_BUFF_PENDING:
            {
                if (log.isDebugEnabled())
                {
                    log.debug( m_logPrefix + "constructor: Waiting for TSW to become BUFFERING..." );
                }

                try
                {
                    m_tswInitializedCondition.waitUntilTrue(BUFFERING_START_TIMEOUT_MILLIS);
                }
                catch (InterruptedException e)
                {
                    throw new IllegalArgumentException("Failed to wait for stream startup");
                }
                
                if (!m_tswInitializedCondition.getState())
                {
                    if (log.isInfoEnabled())
                    {
                        log.info( m_logPrefix + "constructor: Timed out waiting for TSB to start buffering" );
                    }

                    // notify ServiceResolutionHandler
                    // (via ChannelContentItem) that tune failed
                    // If it returns TRUE tune needs to be re-attempted
                    boolean retry = m_contentItem.notifyTuningFailed();
                    if(retry)
                    {
                        m_startUpError = START_UP_RETRY;
                    }
                    else
                    {
                        m_startUpError = START_UP_FAIL;
                    }
                    m_bufferingStartupError = "Timed out waiting for TSB to start buffering";
                }               
                break;
            }
            case TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER:
            case TimeShiftManager.TSWSTATE_IDLE:
            {
                if (log.isInfoEnabled())
                {
                    log.info( m_logPrefix + "Got TSW in " + TimeShiftManager.stateString[tswState] + " state.." );
                }

                // notify ServiceResolutionHandler
                // (via ChannelContentItem) that tune failed
                // If it returns TRUE tune needs to be re-attempted
                boolean retry = m_contentItem.notifyTuningFailed();
                if(retry)
                {
                    m_startUpError = START_UP_RETRY;
                }
                else
                {
                    m_startUpError = START_UP_FAIL;
                }
                m_bufferingStartupError = "Failed to acquire TSB";
            }               
            break;
            default:
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "constructor: Got a TSW in an unusable state" );
                }
                throw new IllegalArgumentException( "Failed to get a TSW (" 
                                                    + TimeShiftManager.stateString[tswState] 
                                                    + ')');                
            }
        } // END switch (m_tswClient.getState())
    }
    
    public void open(ContentRequest request) throws HNStreamingException
    {
        session.openSession(socket, request.getChunkedEncodingMode(),
                protocolInfo.getProfileId(), 
                protocolInfo.getContentFormat(),
                request.getMaxTrickModeBandwidth(), request.getCurrentDecodePTS(), 
                request.getMaxGOPsPerChunk(), request.getMaxFramesPerGOP(), request.isUseServerSidePacing(), 
                request.getRequestedFrameTypesInTrickMode(), connectionId.intValue(), m_contentItem);
    }

    public void transmit() throws HNStreamingException
    {
        synchronized(lock)
        {
            if (currentState != STATE_INIT)
            {
                throw new IllegalStateException("transmit called in incorrect state: " + currentState);
            }
    
            currentState = STATE_TRANSMITTING;
        }

        MediaServer.getInstance().getCMS().registerLocalConnection(protocolInfo, connectionId.intValue(), -1);
        //notify when content begins streaming (notifying prior to streaming start)
        ((ContentServerNetModuleImpl) m_contentItem.getServer()).notifyStreamStarted(m_contentItem, connectionId.intValue(),
            url, m_tswClient.getNetworkInterface(), StreamingActivityListener.CONTENT_TYPE_LIVE_RESOURCES);        

        //transmit blocks
        session.transmit(contentLocationType, transmitInfo.getContentDescription(), requestedRate, transmitInfo.handleInTime,
                transmitInfo.getTransmitStartBytePosition(), transmitInfo.getTransmitEndBytePosition(), 
                transmitInfo.getTransmitStartTimePosition(), transmitInfo.getTransmitEndTimePosition(), 
                null, request.getTransformation());
        session.waitForTransmissionToComplete();
    }
    
    public void stop(boolean closeSocket)
    {
        //may be called in any state, but no-op if already stopped
        synchronized(lock)
        {
            if (currentState == STATE_STOPPED)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring stop when already stopped");
                }
                return;
            }
            currentState = STATE_STOPPED;
        }

        session.releaseResources(closeSocket, true);

        //notify after streaming has ended
        //TODO: support end reason timeout (must be provided in asyncEvent and would override TSW-provided reasons)
        //other end reasons will be due to hitting end of file notification on last tsb with a tsw reason mapped 
        ((ContentServerNetModuleImpl) m_contentItem.getServer()).notifyStreamEnded(m_contentItem, connectionId.intValue(),
            StreamingActivityListener.ACTIVITY_END_USER_STOP, StreamingActivityListener.CONTENT_TYPE_LIVE_RESOURCES);        
        
        //release listener
        MediaServer.getInstance().getCMS().removeConnectionCompleteListener(connectionCompleteListener);

        // Release our interest in the TSW
        if (m_tswClient != null)
        {
            m_tswClient.release();
        }
        if (log.isInfoEnabled())
        {
            log.info("TSBChannelStream stop() - closeSocket: " + closeSocket);
        }
        if (closeSocket)
        {
            if (socket != null)
            {
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("TSBChannelStream stop() - closing socket..");
                    }
                    socket.close();
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
    }
    
    public Integer getConnectionId()
    {
        return connectionId;
    }
    
    public String getURL()
    {
        return url;
    }
    
    public Time getAvailableSeekStartTime() throws HNStreamingException
    {
        return new Time(m_tswClient.getFirstTSB().getContentStartTimeInMediaTime());
    }
    
    public Time getAvailableSeekEndTime()
    {
        //add absolute durations without including TSW start time offset
        long result = 0L;
        for (Enumeration tsbs = m_tswClient.elements();tsbs.hasMoreElements();)
        {
            TimeShiftBuffer tsb = (TimeShiftBuffer)tsbs.nextElement();
            //duration is nanos
            result += tsb.getDuration();
        }
        return new Time(result);
    }
    
    public int getFrameRateInTrickMode() throws HNStreamingException
    {
        return HNAPIImpl.nativeServerGetFrameRateInTrickMode(contentLocationType, transmitInfo.contentDescription, 
                protocolInfo.getProfileId(), protocolInfo.getContentFormat(), request.getTransformation(), requestedRate);
    }

    public int getFrameTypesInTrickMode() throws HNStreamingException
    {
        int requestedFrameTypes = request.getRequestedFrameTypesInTrickMode();
        // Set frame types to what was requested
        int frameTypes = requestedFrameTypes;

        // If a specific type was requested, determine if platform supports
        if (requestedFrameTypes != ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_NONE)
        {
            // Set the frame types to what was requested
            int supportedFrameTypes = ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_NONE;
            try
            {
                // Ask platform what trick mode frame types are supported for this content
                supportedFrameTypes = HNAPIImpl.nativeServerGetFrameTypesInTrickMode(contentLocationType, 
                        transmitInfo.contentDescription, protocolInfo.getProfileId(), 
                        protocolInfo.getContentFormat(), request.getTransformation(), 
                        requestedRate); 
            }
            catch (MPEMediaError err)
            {
                throw new HNStreamingException("Failed to retrieve frame type in trick mode", err);
            }
            // If platform doesn't support what was requested, frame type
            // will be whatever platform can support
            if (supportedFrameTypes < requestedFrameTypes)
            {
                frameTypes = supportedFrameTypes;
            }
        }
        return frameTypes;
    }

    public int getContentLocationType()
    {
        return contentLocationType;
    }
    
    public long getAvailableSeekStartByte(boolean encrypted) throws HNStreamingException
    {
        long nanoseconds = getAvailableSeekStartTime().getNanoseconds();
        HNStreamContentDescription thisContentDescription = new HNStreamContentDescriptionTSB(m_tswClient.getFirstTSB().getNativeTSBHandle());
        try
        {
            long tempPosition = HNAPIImpl.nativeServerGetNetworkBytePositionForMediaTimeNS(contentLocationType, 
                                                thisContentDescription, protocolInfo.getProfileId(),
                                                protocolInfo.getContentFormat(), 
                                                request.getTransformation(), nanoseconds);
            if(tempPosition < 0)
            {
                return -1;
            }
            if (protocolInfo.isLinkProtected() && encrypted)
            {
                return StreamUtil.getDTCPEncryptedStartByte(tempPosition);
            }
            else
            {
                return tempPosition;
            }
        }
        catch (MPEMediaError err)
        {
            throw new HNStreamingException("Unable to retrieve network byte position for mediatime nanos: " + nanoseconds);
        }
    }
    
    public long getAvailableSeekEndByte(boolean encrypted) throws HNStreamingException
    {
        //add up network bytes for each segment in the tsb 
        long result = 0L;
        TimeShiftBuffer[] tsbs = getTSBs();
        for (int i = 0;i<tsbs.length;i++)
        {
            long tempSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType,
                    new HNStreamContentDescriptionTSB(tsbs[i].getNativeTSBHandle()), protocolInfo.getProfileId(),
                                                      protocolInfo.getContentFormat(), 
                                                      request.getTransformation());
            if(tempSize < 0)
            {
                // For live streaming content the platform may return -1 as size
                // Hence just return an unknown end byte offset of -1
                return -1;
            }
            if(protocolInfo.isLinkProtected() && encrypted)
            {
                result += StreamUtil.getDTCPEncryptedSize(tempSize);
            }
            else
            {
                result += tempSize;
            }
        }
        //seek position is size - 1
        return result - 1;
    }
    
    public long getStartByte()
    {
        return transmitInfo.requestedStartBytePosition;
    }
    
    public long getEndByte()
    {
        return transmitInfo.requestedEndBytePosition;
    }

    public boolean isTransmitting(ContentItem contentItem)
    {
        synchronized(lock)
        {
            return (currentState == STATE_TRANSMITTING && m_contentItem.equals(contentItem));
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
    
    public String toString()
    {
        return "TSBChannelStream 0x" + Integer.toHexString(hashCode()) 
                + ":[connectionId " + connectionId 
                + ", url: " + url
                + ", buffStarted " + m_tswInitializedCondition.getState()
                + ", buffStartErr " + m_bufferingStartupError
                + ( m_tswClient == null ? "tswc null" : " tswc 0x" + Integer.toHexString(m_tswClient.hashCode()) )
                + ", transmitInfo: " + transmitInfo + "]";
    }
    
    private TimeShiftBuffer[] getTSBs() throws HNStreamingException
    {
        if (m_tswClient == null)
        {
            throw new HNStreamingException("getTSBs called but tswClient was null");
        }
        Enumeration tsbs = m_tswClient.elements();
        List list = new ArrayList();
        while (tsbs.hasMoreElements())
        {
            list.add(tsbs.nextElement());
        }
        
        if (list.isEmpty())
        {
          throw new HNStreamingException("no TSBs available");
        }
        
        return (TimeShiftBuffer[])list.toArray(new TimeShiftBuffer[0]);
    }
    
    /*
      Create a TransmitInfo - combinedTSBsStartByte will be less than combinedTSBsEndByte if rate was negative) - from timeseekrange
      endByte may be -1
     */
    private TransmitInfo getTransmitInfoForRange(long combinedTSBsStartByte, long combinedTSBSEndByte, 
            float rate) throws HNStreamingException, HNStreamingRangeException
    {
        TimeShiftBuffer[] segments= getTSBs();
        long combinedTSBsSize = 0L;
        for (int i = 0;i<segments.length;i++)
        {
            HNStreamContentDescription thisDescription = new HNStreamContentDescriptionTSB(segments[i].getNativeTSBHandle());

            long thisTSBSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, thisDescription,
                    protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                    request.getTransformation());
            long combinedSize;
            if(thisTSBSize > 0)
            {
                combinedSize = combinedTSBsSize + thisTSBSize;
            }
            else
            {
                combinedSize = combinedTSBsSize;
            }

            if (combinedSize >= combinedTSBsStartByte)
            {
                //current segment contains needed start byte offset
                long thisStartByteOffset = combinedTSBsStartByte - combinedTSBsSize;
                if (log.isInfoEnabled()) 
                {
                    log.info("getTransmitInfoForRange - combined TSBs start byte: " + combinedTSBsStartByte + ", segment index to use: " + i + 
                            ", byte offset in to segment: " + thisStartByteOffset + ", combinedTSBs end byte: " + combinedTSBSEndByte + ", rate: " + rate);
                }
                return new TransmitInfo(segments[i], combinedTSBsSize + thisStartByteOffset, thisStartByteOffset, combinedTSBSEndByte, rate);
            }
            if(thisTSBSize > 0)
            {
                combinedTSBsSize += thisTSBSize;
            }
        }
        throw new HNStreamingRangeException("segment not found for start byte: " + combinedTSBsStartByte + ", combined TSBs size: " + combinedTSBsSize);
    }
    
    private TransmitInfo getTransmitInfoForTimeRange(long tsbStartTime, long tsbEndTime, long combinedTSBsStartByte, long combinedTSBSEndByte, float rate)
            throws HNStreamingException, HNStreamingRangeException
    {
        TimeShiftBuffer[] segments= getTSBs();
        long combinedTSBsDuration = 0L;
        long combinedTSBsSize = 0L;
        for (int i = 0;i<segments.length;i++)
        {
            long thisTsbDurationNanos = segments[i].getDuration();
            HNStreamContentDescription thisDescription = new HNStreamContentDescriptionTSB(segments[i].getNativeTSBHandle());

            long thisTSBSize;
            if(combinedTSBsStartByte > 0)
            {       
                thisTSBSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, thisDescription,
                        protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                        request.getTransformation());
            }
            else
            {
                thisTSBSize = 0;
            }

            if (tsbStartTime <= (combinedTSBsDuration + thisTsbDurationNanos))
            {
                //current segment contains needed start time offset
                TimeShiftBuffer thisSegment = (TimeShiftBuffer) segments[i];
                long thisStartTimeOffset = tsbStartTime - combinedTSBsDuration;
                long thisStartByteOffset = combinedTSBsStartByte - combinedTSBsSize;
                if (log.isInfoEnabled())
                {
                    log.info("getTransmitInfoForTimeRange - tsb start time: " + tsbStartTime + ", segment index to use: " + i +
                            ", time offset in to segment: " + thisStartTimeOffset + ", tsb end time: " + tsbEndTime + ", rate: " + rate);
                }
                return new TransmitInfo(thisSegment, combinedTSBsDuration + thisStartTimeOffset, thisStartTimeOffset,
                        rate, tsbEndTime, combinedTSBsSize + thisStartByteOffset, thisStartByteOffset, combinedTSBSEndByte);
            }
            combinedTSBsDuration += thisTsbDurationNanos;
            combinedTSBsSize += thisTSBSize;
        }
        throw new HNStreamingRangeException("segment not found for time: " + tsbStartTime + ", tsb duration: " + combinedTSBsDuration);
    }    
    
    private TimeShiftBuffer getTSB(long startOffsetNanos) throws HNStreamingException
    {
        TimeShiftBuffer service = null;
    
        TimeShiftBuffer[] services = getTSBs();
        if (log.isDebugEnabled())
        {
            log.debug("getTSB - evaluating segments - count: " + services.length + 
            ", looking for nanos offset: " + startOffsetNanos);
        }
        long combinedTSBsDurationNanos = 0L;
        for (int i = 0; i < services.length; i++)
        {
            long thisTSBDurationNanos = services[i].getDuration();
            combinedTSBsDurationNanos += thisTSBDurationNanos;
            if (log.isDebugEnabled())
            {
                log.debug("getTSB - current duration nanos: " + combinedTSBsDurationNanos);
            }
            if (startOffsetNanos <= combinedTSBsDurationNanos)
            {
                service = services[i];
                break;
            }
            /*
            else if(startOffsetNanos > combinedTSBsDurationNanos)
            {
                // This means the requested offset is greater than the
                // combined TSB durations. Best we can do is to move to live point
                if (log.isInfoEnabled())
                {
                    log.info("getTSB - current duration nanos: " + combinedTSBsDurationNanos + "is less than startOffsetNanos: " + startOffsetNanos + ".. switch to live");
                }
                service = services[i];
                break;
            }
            */
        }
        if (service == null)
        {
            throw new HNStreamingException("getTSB - Unable to find segment for offset nanos: " + startOffsetNanos + ", combined TSBs nanos: " + combinedTSBsDurationNanos);
        }
        if (log.isDebugEnabled())
        {
            log.debug("getTSB - returning: " + service);
        }
        return service;
    }
    
    // Determine byte offset into the combined TSB content (including all segments)
    private long getByteOffsetForCombinedTSBsNanos(long nanos) throws HNStreamingException, HNStreamingRangeException
    {
        try
        {
            TimeShiftBuffer[] segments = getTSBs();
            TimeShiftBuffer thisSegment = getTSB(nanos);
            long combinedTSBsSize = 0L;
            long combinedTSBsNanos = 0L;
            for (int i = 0;i<segments.length;i++)
            {
                HNStreamContentDescription thisDescription = new HNStreamContentDescriptionTSB(segments[i].getNativeTSBHandle());

                long thisTSBSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, thisDescription,
                        protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                        request.getTransformation());
                // If native returns unknown size for this content, byte offset cannot
                // be determined. Return from here.
                if(thisTSBSize < 0)
                {
                    return -1;
                }
                if (segments[i] == thisSegment)
                {
                    long thisNanosOffset = nanos - combinedTSBsNanos;
                    long thisByteOffset = HNAPIImpl.nativeServerGetNetworkBytePositionForMediaTimeNS(contentLocationType, thisDescription,
                            protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                            request.getTransformation(), thisNanosOffset);
                    // If native returns unknown byte offset for given time, overall byte offset cannot
                    // be determined. Return from here.
                    if(thisByteOffset < 0)
                    {
                        return -1; 
                    }
                    return combinedTSBsSize + thisByteOffset;
                }
                combinedTSBsSize += thisTSBSize;
                //tsb duration is nanos
                combinedTSBsNanos += segments[i].getDuration();
            }
        }
        catch (MPEMediaError err)
        {
            throw new HNStreamingException("Unable to get byte offset for combined TSB nanos: " + nanos, err);
        }
        throw new HNStreamingRangeException("getByteOffsetForCombinedTSBsNanos - segment not found for nanos: " + nanos);
    }
    
    // Determine time offset into the combined TSB content (including all segments)
    private long getTimeOffsetForCombinedTSBsNanos(long nanos) throws HNStreamingException, HNStreamingRangeException
    {
        try
        {
            TimeShiftBuffer[] segments = getTSBs();
            TimeShiftBuffer thisSegment = getTSB(nanos);
            long combinedTSBsNanos = 0L;
            for (int i = 0;i<segments.length;i++)
            {
                if (segments[i] == thisSegment)
                {
                    long thisNanosOffset = nanos - combinedTSBsNanos;
                    return thisNanosOffset;
                }
                //tsb duration is nanos
                combinedTSBsNanos += segments[i].getDuration();
            }
        }
        catch (MPEMediaError err)
        {
            throw new HNStreamingException("Unable to get time offset for combined TSB nanos: " + nanos, err);
        }
        throw new HNStreamingRangeException("getTimeOffsetForCombinedTSBsNanos - segment not found for nanos: " + nanos);
    }

    private long getCombinedTSBsStartByteOffsetForSegment(TimeShiftBuffer segment) throws HNStreamingException
    {
        TimeShiftBuffer[] segments = getTSBs();
        long combinedTSBsSize = 0L;
        for (int i = 0;i<segments.length;i++)
        {
            long thisTSBSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType,
                    new HNStreamContentDescriptionTSB(segments[i].getNativeTSBHandle()),
                                                      protocolInfo.getProfileId(),
                                                      protocolInfo.getContentFormat(),
                                                      request.getTransformation());
            // If content size returned by native for this segment
            // is unknown (-1) return from here.
            // This may happen for certain types of content (e.g. live
            // streaming, in-progress recording etc.)
            if(thisTSBSize < 0)
            {
                return -1;
            }
            if (segments[i] == segment)
            {
                return combinedTSBsSize;
            }
            combinedTSBsSize += thisTSBSize;
        }
        throw new HNStreamingException("segment: " + segment + " not found for: " + m_contentItem);
    }

    private long getCombinedTSBsStartTimeOffsetForSegment(TimeShiftBuffer segment) throws HNStreamingException
    {
        TimeShiftBuffer[] segments = getTSBs();
        long combinedTSBsDuration = 0L;
        for (int i = 0;i<segments.length;i++)
        {
            long thisTSBDuration = segments[i].getDuration();
            if (segments[i] == segment)
            {
                return combinedTSBsDuration;
            }
            combinedTSBsDuration += thisTSBDuration;
        }
        throw new HNStreamingException("segment: " + segment + " not found for: " + m_contentItem);
    }
    
    private void releaseAndDeauthorize(boolean deauthorizeNow, final int resultCode, boolean closeSocket)
    {
        //may come in from an asyncEvent or a notifyComplete - no-op if already stopped
        synchronized(lock)
        {
            if (currentState == STATE_STOPPED)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring releaseAndDeauthorize when already stopped");
                }
                return;
            }
            //don't change state here
        }

        if (log.isInfoEnabled()) 
        {
            log.info("releaseAndDeauthorize - now: " + deauthorizeNow + " - id: " + connectionId + ", url: " + url);
        }
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

        HNStreamContentDescription thisContentDescription =
             new HNStreamContentDescriptionTSB(m_tswClient.getFirstTSB().getNativeTSBHandle());
        return thisContentDescription;

    }
 

    class TransmitInfo
    {
        private final boolean forwardScan;

        //start/end byte positions from the request, may span segments, end may be -1 if provided
        private final long requestedStartBytePosition;
        private final long requestedEndBytePosition;
    
        private final boolean handleInTime;
        private final long requestedStartTimePosition;
        private final long requestedEndTimePosition;
        
        TimeShiftBuffer currentSegment;
        long currentSegmentStartByteOffset;
        long currentSegmentStartTimeOffset;

        int segmentIndex;

        HNStreamContentDescription contentDescription;
        
        boolean requestIsSatisfied = false;
        private long copyControlInfoRestrictedSegmentEndByte = -1;
        private long copyControlInfoRestrictedSegmentEndTime = -1;
        //single (program-level copycontrolinfo entry) - may be null
        HNPlaybackCopyControlInfo hnPlaybackCopyControlInfo = null;

        //Constructor supporting range requests (ranges provided by timeseekrange or range headers)
        TransmitInfo(TimeShiftBuffer currentSegment, long startByteOffsetIntoCombinedTSBs, 
                long currentSegmentStartByteOffset, long requestedEndBytePosition, 
                float requestedRate) throws HNStreamingException
        {
            this.currentSegment = currentSegment;
            TimeShiftBuffer[] tsbs = getTSBs();
            for (int i=0;i<tsbs.length;i++)
            {
                if (tsbs[i] == currentSegment)
                {
                    segmentIndex = i;
                    break;
                }
            }
            handleInTime = false;
            requestedStartBytePosition = startByteOffsetIntoCombinedTSBs;
            this.requestedEndBytePosition = requestedEndBytePosition;
            this.currentSegmentStartByteOffset = currentSegmentStartByteOffset;
            currentSegmentStartTimeOffset = -1;
            this.requestedStartTimePosition = -1;
            this.requestedEndTimePosition = -1;
            contentDescription = new HNStreamContentDescriptionTSB(currentSegment.getNativeTSBHandle());
            forwardScan = (requestedRate > 0.0F);
            updateCopyControlValues(currentSegment, currentSegmentStartByteOffset);
        }
        
        //Constructor supporting time requests (provided by timeseekrange)
        TransmitInfo(TimeShiftBuffer currentSegment, long startTimeOffset,
                     long currentSegmentOffset, float requestedRate, long requestedEndTime,
                     long startByteOffsetIntoCombinedTSBs, 
                     long currentSegmentStartByteOffset, long requestedEndBytePosition) throws HNStreamingException
        {
            this.currentSegment = currentSegment;
            TimeShiftBuffer[] tsbs = getTSBs();
            for (int i=0;i<tsbs.length;i++)
            {
                if (tsbs[i] == currentSegment)
                {
                    segmentIndex = i;
                    break;
                }
            }
            handleInTime = true; 
            requestedStartTimePosition = startTimeOffset;
            requestedEndTimePosition = requestedEndTime;
            currentSegmentStartTimeOffset = currentSegmentOffset;         
            requestedStartBytePosition = startByteOffsetIntoCombinedTSBs;
            this.requestedEndBytePosition = requestedEndBytePosition;
            this.currentSegmentStartByteOffset = currentSegmentStartByteOffset;      
            contentDescription = new HNStreamContentDescriptionTSB(currentSegment.getNativeTSBHandle());
            forwardScan = (requestedRate > 0.0F);
            updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);
        }

        //Constructor supporting requests without ranges (transmit all available content)
        TransmitInfo(float requestedRate) throws HNStreamingException
        {
            //default, start at live point, provide requested start time position
            TimeShiftBuffer[] tsbs = getTSBs();
            handleInTime = true;
            segmentIndex = tsbs.length - 1;
            currentSegment = tsbs[segmentIndex];
            currentSegmentStartByteOffset = -1;
            requestedStartBytePosition = -1;
            requestedEndBytePosition = -1;
            currentSegmentStartTimeOffset = currentSegment.getDuration();
            requestedStartTimePosition = getCombinedTSBsStartTimeOffsetForSegment(currentSegment) + currentSegmentStartTimeOffset;
            requestedEndTimePosition = -1;
            contentDescription = new HNStreamContentDescriptionTSB(currentSegment.getNativeTSBHandle());
            //expect rate to be 1.0..negative rate would require a range..
            forwardScan = (requestedRate > 0.0F);
            updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);
        }

        //evaluate CCI in the direction of play to find the CopyControlInfo in effect at the current start offset (or null)
        //calculate an updated byte end position if CCI changes occur in the direction of play prior to requested end position
        final void updateCopyControlValues(TimeShiftBuffer currentSegment, long currentSegmentStartByteOffset) throws HNStreamingException
        {
            copyControlInfoRestrictedSegmentEndByte = -1;
            hnPlaybackCopyControlInfo = null;
            TimeTable timeTable = currentSegment.getCCITimeTable();
            if (log.isDebugEnabled())
            {
                log.debug("updateCopyControlValues - current segment: " + currentSegment + ", duration: " + currentSegment.getDuration() +
                        ", start byte offset: " + currentSegmentStartByteOffset + ", CCI: " + timeTable + ", forward scan: " + forwardScan);
            }

            try
            {
                //evaluate CCI entries in direction of play
                Enumeration copyControlInfoEntries = forwardScan ? timeTable.elements() : timeTable.reverseElements();
                CopyControlInfo lastCopyControlInfo = null;

                //walk the elements in the direction of play and find the first CCI entry after the start position, as well as the
                //CCI entry representing the start position if one exists
                CopyControlInfo thisCopyControlInfo = null;
                while (copyControlInfoEntries.hasMoreElements())
                {
                    thisCopyControlInfo = (CopyControlInfo) copyControlInfoEntries.nextElement();
                    long thisCopyControlInfoBytePosition = HNAPIImpl.nativeServerGetNetworkBytePositionForMediaTimeNS(
                            contentLocationType, contentDescription,
                            protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                            request.getTransformation(), thisCopyControlInfo.getTimeNanos());
                    //CCI past the start position has been found...lastCopyControlInfo represents the starting position copycontrol info

                    if (forwardScan && (thisCopyControlInfoBytePosition > currentSegmentStartByteOffset))
                    {
                        //if thisCopyControlInfo byte position is within the requested range, restrict the end position to that value
                        //when end of content is received, initiate streaming with updated CCI
                        long startByteOffset = getCombinedTSBsStartByteOffsetForSegment(currentSegment);
                        if (requestedEndBytePosition == -1 || (startByteOffset + thisCopyControlInfoBytePosition) < requestedEndBytePosition)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("updateCopyControlValues - forward scan and copycontrol byte position within requested range: " + (startByteOffset + thisCopyControlInfoBytePosition));
                            }
                            copyControlInfoRestrictedSegmentEndByte = thisCopyControlInfoBytePosition - 1;
                        }
                        break;
                    }

                    if (!forwardScan && (thisCopyControlInfoBytePosition < currentSegmentStartByteOffset))
                    {
                        //if thisCopyControlInfo byte position is within the requested range, restrict the end position to that value
                        //when end of content is received, initiate streaming with updated CCI
                        long startByteOffset = getCombinedTSBsStartByteOffsetForSegment(currentSegment);
                        if (requestedEndBytePosition == -1 || (startByteOffset + thisCopyControlInfoBytePosition) > requestedEndBytePosition)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("updateCopyControlValues - reverse scan and copycontrol byte position within requested range: " + (startByteOffset + thisCopyControlInfoBytePosition));
                            }
                            copyControlInfoRestrictedSegmentEndByte = thisCopyControlInfoBytePosition;
                            //CCI in effect is thisCopyControlInfo (reverse scan)
                            lastCopyControlInfo = thisCopyControlInfo;
                        }
                        break;
                    }

                    //track last known copycontrolinfo to support passing copy control info to transmit call
                    lastCopyControlInfo = thisCopyControlInfo;
                }
                //set copycontrolinfo if lastCopyControlInfo exists (copycontrol in effect at start position)
                if (lastCopyControlInfo != null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("updateCopyControlValues - CCI in effect at start position: " + lastCopyControlInfo);
                    }
                    //using -1 for pid when isProgram=true (CCI from cablecard is program-wide)
                    hnPlaybackCopyControlInfo = new HNPlaybackCopyControlInfo((short)-1, true, false, lastCopyControlInfo.getCCI());
                }
            }
            catch (MPEMediaError err)
            {
                throw new HNStreamingException("Unable to update copy control values", err);
            }
        }
        
        final void updateCopyControlValuesForTime(TimeShiftBuffer currentSegment, long currentSegmentStartTimeOffset) throws HNStreamingException
        {
            copyControlInfoRestrictedSegmentEndTime = -1;
            hnPlaybackCopyControlInfo = null;
            TimeTable timeTable = currentSegment.getCCITimeTable();
            try
            {
                //evaluate CCI entries in direction of play
                Enumeration copyControlInfoEntries = forwardScan ? timeTable.elements() : timeTable.reverseElements();
                CopyControlInfo lastCopyControlInfo = null;

                //walk the elements in the direction of play and find the first CCI entry after the start position, as well as the
                //CCI entry representing the start position if one exists
                CopyControlInfo thisCopyControlInfo;
                while (copyControlInfoEntries.hasMoreElements())
                {
                    thisCopyControlInfo = (CopyControlInfo) copyControlInfoEntries.nextElement();

                    long thisCopyControlInfoTimePosition = thisCopyControlInfo.getTimeNanos();
                    
                    //CCI past the start position has been found...lastCopyControlInfo represents the starting position copycontrol info

                    if (forwardScan && (thisCopyControlInfoTimePosition > currentSegmentStartTimeOffset))
                    {
                        //if thisCopyControlInfo time position is within the requested range, restrict the end position to that value
                        //when end of content is received, initiate streaming with updated CCI
                        long startTimeOffset = getCombinedTSBsStartTimeOffsetForSegment(currentSegment);
                        if (requestedEndTimePosition == -1 || (startTimeOffset + thisCopyControlInfoTimePosition) < requestedEndTimePosition)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("updateCopyControlValuesForTime - forward scan and copycontrol time position within requested range: " + (startTimeOffset + thisCopyControlInfoTimePosition));
                            }
                            //thisCopyControlInfoBytePosition is the first byte where different CCI should be applied
                            //so the end byte for current segment is one byte just before that new CCI position
                            copyControlInfoRestrictedSegmentEndTime = thisCopyControlInfoTimePosition - 1;
                        }
                        break;
                    }

                    if (!forwardScan && (thisCopyControlInfoTimePosition < currentSegmentStartTimeOffset))
                    {
                        //if thisCopyControlInfo byte position is within the requested range, restrict the end position to that value
                        //when end of content is received, initiate streaming with updated CCI
                        long startTimeOffset = getCombinedTSBsStartTimeOffsetForSegment(currentSegment);
                        if (requestedEndTimePosition == -1 || (startTimeOffset + thisCopyControlInfoTimePosition) > requestedEndTimePosition)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("updateCopyControlValuesForTime - reverse scan and copycontrol byte position within requested range: " + (startTimeOffset + thisCopyControlInfoTimePosition));
                            }
                            copyControlInfoRestrictedSegmentEndTime = thisCopyControlInfoTimePosition;
                            //CCI in effect is thisCopyControlInfo (reverse scan)
                            lastCopyControlInfo = thisCopyControlInfo;
                        }
                        break;
                    }

                    //track last known copycontrolinfo to support passing copy control info to transmit call
                    lastCopyControlInfo = thisCopyControlInfo;
                }
                //set copycontrolinfo if lastCopyControlInfo exists (copycontrol in effect at start position)
                if (lastCopyControlInfo != null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("updateCopyControlValuesForTime - CCI in effect at start position: " + lastCopyControlInfo);
                    }
                    //using -1 for pid when isProgram=true (CCI from cablecard is program-wide)
                    hnPlaybackCopyControlInfo = new HNPlaybackCopyControlInfo((short)-1, true, false, lastCopyControlInfo.getCCI());
                }
            }
            catch (MPEMediaError err)
            {
                throw new HNStreamingException("Unable to update copy control values", err);
            }
        }
        
        void endOfContent()
        {
            try
            {
                //assert positive rate 
                TimeShiftBuffer[] segments = getTSBs();
                if(!handleInTime)
                {
                    long combinedTSBsStartByteOffsetForSegment = getCombinedTSBsStartByteOffsetForSegment(segments[segmentIndex]);
                    long segmentLength = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType,
                            new HNStreamContentDescriptionTSB(segments[segmentIndex].getNativeTSBHandle()),
                                                              protocolInfo.getProfileId(), 
                                                              protocolInfo.getContentFormat(), 
                                                              request.getTransformation());
                    //determine if all requested bytes have been sent
                    //update segment start offset if restriction exists
                    if (copyControlInfoRestrictedSegmentEndByte != -1)
                    {
                        //same segment, update offset
                        currentSegmentStartByteOffset = copyControlInfoRestrictedSegmentEndByte + 1;
                        updateCopyControlValues(currentSegment, currentSegmentStartByteOffset);
                        if (log.isInfoEnabled())
                        {
                            log.info("endofcontent - was CCI restricted - request not yet satisfied - updated to: " + transmitInfo);
                        }
                    }
                    else if (requestedEndBytePosition == -1 && segmentIndex == (segments.length - 1))
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("endofcontent - no requested end position and all segments transmitted - request satisfied - rate: " + requestedRate + ", requestedStartBytePosition: " +
                                requestedStartBytePosition + ", requestedEndBytePosition: " + requestedEndBytePosition);
                        }
                    }
                    else if (requestedEndBytePosition > -1 && combinedTSBsStartByteOffsetForSegment + segmentLength >= requestedEndBytePosition)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("endofcontent - all bytes sent - request satisfied - rate: " + requestedRate + ", requestedStartBytePosition: " +
                                requestedStartBytePosition + ", requestedEndBytePosition: " + requestedEndBytePosition);
                        }
                    }
                    else if (segmentIndex + 1 < segments.length)
                    {
                        currentSegment = segments[++segmentIndex];
                        contentDescription = new HNStreamContentDescriptionTSB(currentSegment.getNativeTSBHandle());
                        //update start offset to 0L
                        currentSegmentStartByteOffset = 0L;
                        updateCopyControlValues(currentSegment, currentSegmentStartByteOffset);
                        if (log.isInfoEnabled()) 
                        {
                            log.info("endofcontent - request not yet satisfied - next segment: " + currentSegment + ", rate: " + requestedRate + ", requestedStartBytePosition: " +
                                requestedStartBytePosition + ", requestedEndBytePosition: " + requestedEndBytePosition + ", segment offset: " + currentSegmentStartByteOffset);
                        }
                    }
                }
                else
                {
                    long combinedTSBsStartTimeOffsetForSegment = getCombinedTSBsStartTimeOffsetForSegment(segments[segmentIndex]);
                    long segmentLength = segments[segmentIndex].getDuration();
                    //determine if all requested bytes have been sent
                    //update segment start offset if restriction exists
                    if (copyControlInfoRestrictedSegmentEndTime != -1)
                    {
                        //same segment, update offset
                        currentSegmentStartTimeOffset = copyControlInfoRestrictedSegmentEndTime + 1;
                        updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);
                        if (log.isInfoEnabled())
                        {
                            log.info("endofcontent - was CCI restricted - request not yet satisfied - updated to: " + transmitInfo);
                        }
                    }
                    else if (requestedEndTimePosition == -1 && segmentIndex == (segments.length - 1))
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("endofcontent - no requested end position and all segments transmitted - request satisfied - rate: " + requestedRate + ", requestedStartTimePosition: " +
                                requestedStartTimePosition + ", requestedEndTimePosition: " + requestedEndTimePosition);
                        }
                    }
                    else if (requestedEndTimePosition > -1 && combinedTSBsStartTimeOffsetForSegment + segmentLength >= requestedEndTimePosition)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("endofcontent - all bytes sent - request satisfied - rate: " + requestedRate + ", requestedStartTimePosition: " +
                                requestedStartTimePosition + ", requestedEndTimePosition: " + requestedEndTimePosition);
                        }
                    }
                    else if (segmentIndex + 1 < segments.length)
                    {
                        currentSegment = segments[++segmentIndex];
                        contentDescription = new HNStreamContentDescriptionTSB(currentSegment.getNativeTSBHandle());
                        //update start offset to 0L
                        currentSegmentStartTimeOffset = 0L;
                        updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);
                        currentSegmentStartByteOffset = 0L;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("endofcontent - request not yet satisfied - next segment: " + currentSegment + ", rate: " + requestedRate + ", requestedStartTimePosition: " +
                                requestedStartTimePosition + ", requestedEndTimePosition: " + requestedEndTimePosition + ", segment offset: " + currentSegmentStartTimeOffset);
                        }
                    }                        
                }
            }
            catch (HNStreamingException e)
            {
                if (log.isWarnEnabled()) 
                {
                    log.warn("Problem updating state due to end of content notification", e);
                }
                requestIsSatisfied = true;
            }
        }
            
        public void beginningOfContent()
        {
            try
            {
                //assert negative rate
                TimeShiftBuffer[] segments = getTSBs();                
                if(!handleInTime)
                {
                    long combinedTSBsStartByteOffsetForSegment = getCombinedTSBsStartByteOffsetForSegment(segments[segmentIndex]);
                    //determine if all requested bytes have been sent
                    //update segment start offset if restriction exists
                    if (copyControlInfoRestrictedSegmentEndByte != -1)
                    {
                        //same segment, update offset
                        currentSegmentStartByteOffset = copyControlInfoRestrictedSegmentEndByte - 1;
                        updateCopyControlValues(currentSegment, currentSegmentStartByteOffset);
                        if (log.isInfoEnabled())
                        {
                            log.info("beginningofcontent - was CCI restricted - request not yet satisfied - updated to: " + transmitInfo);
                        }
                    }
                    else if (requestedEndBytePosition == -1 && segmentIndex == 0)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("beginningofcontent - no requested end position and all segments transmitted - request satisfied - rate: " + requestedRate + ", requestedStartBytePosition: " +
                                requestedStartBytePosition + ", requestedEndBytePosition: " + requestedEndBytePosition);
                        }
                    }
                    else if (requestedEndBytePosition > -1 && combinedTSBsStartByteOffsetForSegment <= requestedEndBytePosition)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("beginningofcontent - all bytes sent - request satisfied - rate: " + requestedRate + ", requestedStartBytePosition: " +
                                requestedStartBytePosition + ", requestedEndBytePosition: " + requestedEndBytePosition);
                        }
                    }
                    else if (segmentIndex - 1 > -1)
                    {
                        currentSegment = segments[--segmentIndex];
                        contentDescription = new HNStreamContentDescriptionTSB(currentSegment.getNativeTSBHandle());
                        //update start offset to content length - 1
                        long  byteOffset = HNAPIImpl.nativeServerGetNetworkContentItemSize(
                                contentLocationType, contentDescription,
                                protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                                request.getTransformation());
                        if(byteOffset > 0)
                        {
                            currentSegmentStartByteOffset = byteOffset -1;
                        }
                        else
                        {
                            currentSegmentStartByteOffset = -1;
                        }
                        updateCopyControlValues(currentSegment, currentSegmentStartByteOffset);
                        if (log.isInfoEnabled()) 
                        {
                            log.info("beginningofcontent - request not yet satisfied - next segment: " + currentSegment + ", rate: " + requestedRate + ", requestedStartBytePosition: " +
                                requestedStartBytePosition + ", requestedEndBytePosition: " + requestedEndBytePosition + ", segment offset: " + currentSegmentStartByteOffset);
                        }
                    }
                }
                else
                {
                    long combinedTSBsStartTimeOffsetForSegment = getCombinedTSBsStartTimeOffsetForSegment(segments[segmentIndex]);
                    //update segment start offset if restriction exists
                    if (copyControlInfoRestrictedSegmentEndTime != -1)
                    {
                        //same segment, update offset
                        currentSegmentStartTimeOffset = copyControlInfoRestrictedSegmentEndTime - 1;
                        updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);
                        if (log.isInfoEnabled())
                        {
                            log.info("beginningofcontent - was CCI restricted - request not yet satisfied - updated to: " + transmitInfo);
                        }
                    }
                    else if (requestedEndTimePosition == -1 && segmentIndex == 0)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("beginningofcontent - no requested end position and all segments transmitted - request satisfied - rate: " + requestedRate + ", requestedStartTimePosition: " +
                                requestedStartTimePosition + ", requestedEndTimePosition: " + requestedEndTimePosition);
                        }
                    }
                    else if (requestedEndTimePosition > -1 && combinedTSBsStartTimeOffsetForSegment <= requestedEndTimePosition)
                    {
                        requestIsSatisfied = true;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("beginningofcontent - all bytes sent - request satisfied - rate: " + requestedRate + ", requestedStartTimePosition: " +
                                requestedStartTimePosition + ", requestedEndTimePosition: " + requestedEndTimePosition);
                        }
                    }
                    else if (segmentIndex - 1 > -1)
                    {
                        currentSegment = segments[--segmentIndex];
                        contentDescription = new HNStreamContentDescriptionTSB(currentSegment.getNativeTSBHandle());
                        //update start offset to content length - 1
                        currentSegmentStartTimeOffset = currentSegment.getDuration() - 1;
                        updateCopyControlValuesForTime(currentSegment, currentSegmentStartTimeOffset);
                        currentSegmentStartByteOffset = -1;
                        if (log.isInfoEnabled()) 
                        {
                            log.info("beginningofcontent - request not yet satisfied - next segment: " + currentSegment + ", rate: " + requestedRate + ", requestedStartTimePosition: " +
                                requestedStartTimePosition + ", requestedEndTimePosition: " + requestedEndTimePosition + ", segment offset: " + currentSegmentStartTimeOffset);
                        }
                    }                    
                }
            }
            catch (HNStreamingException e)
            {
                if (log.isWarnEnabled()) 
                {
                    log.warn("Problem updating state due to beginning of content notification", e);
                }
                requestIsSatisfied = true;
            }
            
        }
        
        long getTransmitStartBytePosition()
        {
            return currentSegmentStartByteOffset;
        }
        
        long getTransmitEndBytePosition() throws HNStreamingException
        {
            //restriction exists, pass the restriction end byte
            if (copyControlInfoRestrictedSegmentEndByte != -1)
            {
                return copyControlInfoRestrictedSegmentEndByte;
            }
            //no end specified, may be ongoing, just pass -1 to the transmit call
            if (requestedEndBytePosition == -1)
            {
                return -1;
            }
            long combinedTSBsStartByteOffsetForSegment = getCombinedTSBsStartByteOffsetForSegment(currentSegment);
            long segmentSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(
                    contentLocationType, contentDescription, protocolInfo.getProfileId(), 
                    protocolInfo.getContentFormat(), request.getTransformation());
            if(segmentSize < 0)
            {
                return -1;
            }
            if (forwardScan)
            {
                //if end is after end of segment, -1, otherwise, calculate offset
                return requestedEndBytePosition > combinedTSBsStartByteOffsetForSegment + segmentSize ? 
                        -1 : requestedEndBytePosition - combinedTSBsStartByteOffsetForSegment;
            }
            else
            {
                //if end is before start of segment, use -1, otherwise, calculate offset
                return requestedEndBytePosition < combinedTSBsStartByteOffsetForSegment ? 
                        -1 : requestedEndBytePosition - combinedTSBsStartByteOffsetForSegment;
            }
        }

        long getTransmitStartTimePosition()
        {
            return this.currentSegmentStartTimeOffset;
        }
        
        long getTransmitEndTimePosition() 
        {
            //restriction exists, pass the restriction end time
            if (copyControlInfoRestrictedSegmentEndTime != -1)
            {
                return copyControlInfoRestrictedSegmentEndTime;
            }
            //no end specified, may be ongoing, just pass -1 to the transmit call
            if (requestedEndTimePosition == -1)
            {
                return -1;
            }

            //end position exists...determine if end is within current segment
            long tsbStartTimeOffsetForSegment;
            try 
            {
                tsbStartTimeOffsetForSegment = getCombinedTSBsStartTimeOffsetForSegment(currentSegment);
                long segmentDuration = currentSegment.getDuration();
                if (log.isInfoEnabled())
                {
                    log.info("getTransmitEndTimePosition  tsbStartTimeOffsetForSegment: " + tsbStartTimeOffsetForSegment
                             + ", segmentDuration: " + segmentDuration + ", requestedEndTimePosition: " + requestedEndTimePosition);
                }
                if (forwardScan)
                {
                    //if end is after end of segment, -1, otherwise, calculate offset
                    return requestedEndTimePosition > tsbStartTimeOffsetForSegment + segmentDuration ?
                            -1 : requestedEndTimePosition - tsbStartTimeOffsetForSegment;
                }
                else
                {
                    //if end is before start of segment, use -1, otherwise, calculate offset
                    return requestedEndTimePosition < tsbStartTimeOffsetForSegment ?
                            -1 : requestedEndTimePosition - tsbStartTimeOffsetForSegment;
                }
            } 
            catch (HNStreamingException e) 
            {
                if (log.isWarnEnabled()) 
                {
                    log.warn("getTransmitEndTimePosition() failed", e);
                }
                return -1;
            }
        }

        
        boolean isRequestSatisfied()
        {
            return requestIsSatisfied;
        }

        public String toString()
        {
            return "TransmitInfo - segment index: " + segmentIndex + ", start byte offset into segment: " +
                    currentSegmentStartByteOffset + ", requested start: " + requestedStartBytePosition + ", requested end: " + requestedEndBytePosition +
                    ", CCI-restricted end of segment: " + copyControlInfoRestrictedSegmentEndByte + ", rate: " + requestedRate + ", CCI: " + hnPlaybackCopyControlInfo;
        }
        
        public HNStreamContentDescription getContentDescription()
        {
            return contentDescription;
        }
    }

    class ConnectionCompleteListenerImpl implements ConnectionCompleteListener
    {
        private final Integer connectionId;

        ConnectionCompleteListenerImpl(Integer connectionId)
        {
            this.connectionId = connectionId;
        }

        public void notifyComplete(int connectionId)
        {
            if (this.connectionId.intValue() == connectionId)
            {
                if (log.isInfoEnabled())
                {
                    log.info("connectioncomplete: " + connectionId);
                }
                // TODO: Provide a proper result code
                releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, true);
            }
        }

        public String toString()
        {
            return "ConnectionCompleteListenerImpl - id: " + connectionId;
        }
    }

    protected class EDListenerImpl implements EDListener
    {
        public void asyncEvent(int eventCode, int eventData1, int eventData2)
        {
            if (log.isDebugEnabled())
            {
                log.debug("asyncEvent: " + eventCode + ", data1: " + eventData1 + ", data2: " + eventData2);
            }

            switch (eventCode)
            {
                case HNAPI.Event.HN_EVT_END_OF_CONTENT:
                    if (log.isDebugEnabled())
                    {
                        log.debug("endofcontent event received");
                    }
                    handleEndOfContent();
                    break;
                case HNAPI.Event.HN_EVT_BEGINNING_OF_CONTENT:
                    if (log.isDebugEnabled())
                    {
                        log.debug("beginningofcontent event received");
                    }
                    handleBeginningOfContent();
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
                    releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, false);
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
        
        private void handleEndOfContent()
        {
            transmitInfo.endOfContent();
            if (transmitInfo.isRequestSatisfied())
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("end of content - request satisfied - releasing session and scheduling deauthorize");
                }
                releaseAndDeauthorize(false, HttpURLConnection.HTTP_OK, false);
            }
            else
            {
                if (log.isDebugEnabled()) 
                {
                    log.debug("end of content - request not satisfied - stopping session: " + session);
                }
                //stop old transmission but don't release resources
                session.stop();
                        
                if (log.isDebugEnabled()) 
                {
                    log.debug("calling session transmit with new parameters: " + session);
                }
                try
                {
                    session.transmit(contentLocationType, transmitInfo.getContentDescription(), requestedRate, transmitInfo.handleInTime,
                            transmitInfo.getTransmitStartBytePosition(), transmitInfo.getTransmitEndBytePosition(),
                            transmitInfo.getTransmitStartTimePosition(), transmitInfo.getTransmitEndTimePosition(), null, request.getTransformation());
                }
                catch (HNStreamingException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unable to transmit: " + m_contentItem, e);
                    }
                    releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, false);
                }
            }
        }
        
        private void handleBeginningOfContent()
        {
            transmitInfo.beginningOfContent();
            if (transmitInfo.isRequestSatisfied())
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("beginning of content - request satisfied - releasing session and scheduling deauthorize");
                }
                releaseAndDeauthorize(false, HttpURLConnection.HTTP_OK, false);
            }
            else
            {
                if (log.isDebugEnabled()) 
                {
                    log.debug("beginning of content - request not satisfied - stopping session: " + session);
                }
                //stop old transmission but don't release resources
                session.stop();
                        
                if (log.isDebugEnabled()) 
                {
                    log.debug("calling session transmit with new parameters: " + session);
                }
                try
                {
                    session.transmit(contentLocationType, transmitInfo.contentDescription, requestedRate, transmitInfo.handleInTime,
                            transmitInfo.getTransmitStartBytePosition(), transmitInfo.getTransmitEndBytePosition(),
                            transmitInfo.getTransmitStartTimePosition(), transmitInfo.getTransmitEndTimePosition(), null, request.getTransformation());
                }
                catch (HNStreamingException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unable to transmit: " + m_contentItem, e);
                    }
                    releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, false);
                }
            }
        }
    }

    public void tswStateChanged(TimeShiftWindowClient tswc, 
                                TimeShiftWindowStateChangedEvent tswce)
    {
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "tswStateChanged(" 
                       + TimeShiftManager.stateString[tswce.getOldState()] 
                       + " -> " + TimeShiftManager.stateString[tswce.getNewState()] 
                       + ')' );
        }
        switch (tswce.getNewState())
        {
            case TimeShiftManager.TSWSTATE_BUFFERING:
            {
                if (log.isInfoEnabled())
                {
                    log.info( m_logPrefix + "tswStateChanged(" 
                              + TimeShiftManager.stateString[tswce.getNewState()] +
                              "): attaching for buffering" );
                }
                // We may already be attached - if so, this is harmless
                m_tswClient.attachFor(TimeShiftManager.TSWUSE_BUFFERING);
                
                m_bufferingStartupError = null;
                m_tswInitializedCondition.setTrue();
                m_startUpError = START_UP_SUCCESS;
                break;
            }
            case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
            {
                if (log.isInfoEnabled())
                {
                    log.info( m_logPrefix + "tswStateChanged(" 
                              + TimeShiftManager.stateString[tswce.getNewState()] +
                              "): attaching for buffering" );
                }
                m_tswClient.attachFor(TimeShiftManager.TSWUSE_BUFFERING);
                break;
            }
            case TimeShiftManager.TSWSTATE_INTSHUTDOWN:
            case TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN:
            {
                // Going into restart/shutdown cycle. We don't have any
                // dependencies on the buffering session, so just detach
                if ((m_tswClient.getUses() & TimeShiftManager.TSWUSE_BUFFERING) != 0)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info( m_logPrefix + "tswStateChanged(" 
                                + TimeShiftManager.stateString[tswce.getNewState()] +
                                "): Restart/shutdown cycle - detaching for buffering" );
                    }
                    
                    m_tswClient.detachFor(TimeShiftManager.TSWUSE_BUFFERING);
                }
                break;
            }
            case TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER:
            case TimeShiftManager.TSWSTATE_IDLE:
            {
                if (!m_tswInitializedCondition.getState())
                {
                    if (log.isInfoEnabled())
                    {
                        log.info( m_logPrefix + "tswStateChanged(" 
                                  + TimeShiftManager.stateString[tswce.getNewState()] +
                                  "): Failed to acquire a TSW" );
                    }

                    int reason = tswce.getReason();
                    if (log.isInfoEnabled())
                    {
                        log.info( m_logPrefix + "tswStateChanged(reason=" 
                                  + reason +")" );
                    }
                    if(reason == TimeShiftManager.TSWREASON_TUNEFAILURE 
                            || reason == TimeShiftManager.TSWREASON_SYNCLOST
                            || reason == TimeShiftManager.TSWREASON_NOCOMPONENTS)
                    {
                        // notify ServiceResolutionHandler
                        // (via ChannelContentItem) that tune failed
                        // If it returns TRUE tune needs to be re-attempted
                        boolean retry = m_contentItem.notifyTuningFailed();
                        if(retry)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info( m_logPrefix + "tswStateChanged(" 
                                        + TimeShiftManager.stateString[tswce.getNewState()] +
                                        "): Re-try tuning.." );
                            }
                            m_startUpError = START_UP_RETRY;
                        }
                        else
                        {
                            m_startUpError = START_UP_FAIL;
                        }
                    }
                    m_bufferingStartupError = "Failed to initiate buffering";
                    m_tswInitializedCondition.setTrue();
                }
                break;
            }
            case TimeShiftManager.TSWSTATE_RESERVE_PENDING:
            case TimeShiftManager.TSWSTATE_TUNE_PENDING:
            case TimeShiftManager.TSWSTATE_BUFF_PENDING:
            {
                // These are no-op transitions...
                break;
            }
            default:
            {
                if (log.isWarnEnabled())
                {
                    log.warn( m_logPrefix + "tswStateChanged(" 
                            + TimeShiftManager.stateString[tswce.getNewState()] +
                            "): UNHANDLED STATE" );
                }
                break;
            }
        } // END switch (tswState)
    } // END tswStateChanged()

    /**
     * {@inheritDoc}
     */
    public void tswCCIChanged(TimeShiftWindowClient tswc, CopyControlInfo tswcci)
    {
        //notification is for 'live point' - if currently streaming the last segment, update the restricted segment end byte 
        // and update the end stream position
        if (log.isInfoEnabled())
        {
            log.info("received tswCCIChanged notification: " + tswcci);
        }
        //if transmitinfo has no current CCI restriction and no explicit end byte position and the segment being presented is the last segment, and presenting at a positive rate, update end position 

        //if an end range value was requested, validation logic ensured that end value was within the available byte range
        //only need to check for end value of -1
        
        //end of content will be received for the updated end position and updated CCI will then be sent
        // set the restriction (requires tsb's CCITimeTable to have been updated)
        try
        {
            TimeShiftBuffer[] segments = getTSBs();
            if ((transmitInfo.currentSegment == segments[segments.length - 1]) && (transmitInfo.forwardScan))
            {
                if(!transmitInfo.handleInTime)
                {
                    if( (transmitInfo.copyControlInfoRestrictedSegmentEndByte == -1) 
                            && (transmitInfo.getTransmitEndBytePosition() == -1) )
                    {
                        // If we received this indication, there should be at least one CCI entry
                        final TimeTable tsbTimeTable = transmitInfo.currentSegment.getCCITimeTable();
                        final CopyControlInfo latestTSBCCI = (CopyControlInfo)tsbTimeTable.getLastEntry();
                        if (latestTSBCCI == null)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("tswCCIChanged received, but no CCI found in the tsb segment!");
                            }
                        }
                        else
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("tswCCIChanged notification is for active segment - updating CCI restriction end byte position and updating updateEndBytePosition ("
                                        + latestTSBCCI + ')');
                            }
                            try
                            {
                                long newEndPosition = HNAPIImpl.nativeServerGetNetworkBytePositionForMediaTimeNS(
                                        contentLocationType, transmitInfo.getContentDescription(),
                                        protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                                        request.getTransformation(), latestTSBCCI.getTimeNanos());
                                transmitInfo.copyControlInfoRestrictedSegmentEndByte = newEndPosition;
                                session.updateEndPosition(newEndPosition);
                            }
                            catch (MPEMediaError err)
                            {
                                if (log.isWarnEnabled())
                                {
                                    log.warn("Unable to retrieve byte position for cci time: " + latestTSBCCI.getTimeMillis() + "ms", err);
                                }
                            }
                        }                    
                    }
                    else
                    {
                        if( (transmitInfo.copyControlInfoRestrictedSegmentEndTime == -1) 
                                && (transmitInfo.getTransmitEndTimePosition() == -1) )
                        {
                            // If we received this indication, there should be at least one CCI entry
                            final TimeTable tsbTimeTable = transmitInfo.currentSegment.getCCITimeTable();
                            final CopyControlInfo latestTSBCCI = (CopyControlInfo)tsbTimeTable.getLastEntry();
                            if (latestTSBCCI == null)
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("tswCCIChanged received, but no CCI found in the tsb segment!");
                                }
                            }
                            else
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("tswCCIChanged notification is for active segment - updating CCI restriction end byte position and updating updateEndTimePosition ("
                                            + latestTSBCCI + ')');
                                }
                                try
                                {
                                    long newEndPosition = transmitInfo.currentSegment.getDuration();
                                    transmitInfo.copyControlInfoRestrictedSegmentEndTime = newEndPosition;
                                    session.updateEndPosition(newEndPosition);
                                }
                                catch (MPEMediaError err)
                                {
                                    if (log.isWarnEnabled())
                                    {
                                        log.warn("Unable to retrieve byte position for cci time: " + latestTSBCCI.getTimeMillis() + "ms", err);
                                    }
                                }
                            }  
                        }
                    }
                }
            }
        }
        catch (HNStreamingException hnse)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to process tswCCIChanged notification", hnse);
            }
        }
    }

} // END class ChannelStream
