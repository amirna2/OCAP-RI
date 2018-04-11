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

import java.net.URI;

import javax.media.Time;
import javax.tv.service.navigation.ServiceComponent;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.mpe.HNAPI;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.presentation.PresentationContext;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.data.HNHttpHeaderAVStreamParameters;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackCopyControlInfo;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackParamsMediaPlayerHttp;
import org.cablelabs.impl.media.streaming.session.data.HNStreamParams;
import org.cablelabs.impl.media.streaming.session.data.HNStreamParamsMediaPlayerHttp;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.upnp.cm.ConnectionManagerService;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.SimpleCondition;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;

/**
 * Represents the client-side of a Home Networking client/server session.
 * 
 * Allows a client to request transmission of a resource from a remote server.
 */
public class HNClientSession
{
    private static final Logger log = Logger.getLogger(HNClientSession.class);

    private PresentationContext pc;

    private int reservationSessionId;

    private int transmissionSessionId;

    private boolean transmissionStarted;

    //default duration opensession/requesttransmission will wait for a success event prior to timing out
    //configurable via OCAP.hn.session.timeout.millis
    private static final int DEFAULT_SESSION_TIMEOUT_MILLIS = 30000;
    private static final String SESSION_TIMEOUT_MILLIS_PARAM = "OCAP.hn.session.timeout.millis";
    /**
     * Default DTCP descriptor data.
     */
    private static final byte[] DEFAULT_DTCP_DESCRIPTOR = {(byte) org.cablelabs.impl.signalling.DescriptorTag.DTCP, 4, 0, 0, 0, 0 };

    /** Lock to prevent concurrent parameters modifications. */
    private final Object parametersLock = new Object();

    /** Playback parameters. */
    private HNPlaybackParamsMediaPlayerHttp parameters;

    private final EDListener internalListener;
    private EDListener listener;

    private boolean sessionOpen;

    private SimpleCondition initialEventCondition = new SimpleCondition(false);
    private SimpleCondition contentPresentingCondition = new SimpleCondition(false);
    private int sourceConnectionId;
    private UPnPClientService sourceCMS;
    private boolean contentPresenting;
    
    private float currentGain;
    private boolean currentMuted;
    private boolean currentBlocked;

    private boolean initialSetComponentsCalled = false;
    
    private HNStreamProtocolInfo protocolInfo;
    private float currentRate;
    private float previousRate;
    

    public HNClientSession(PresentationContext pc)
    {
        this.pc = pc;

        internalListener = new EDListener()
        {
            public void asyncEvent(int eventCode, int eventData1, int eventData2)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("asyncEvent - received: " + eventCode + ", " + eventData1 + ", " + eventData2);
                }
                //a delegating listener may not yet be registered - track open state
                if (MediaAPI.Event.CONTENT_PRESENTING == eventCode)
                {
                    contentPresenting = true;
                }
                if (HNAPI.Event.HN_EVT_SESSION_OPENED == eventCode)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("received session opened event");
                    }
                    contentPresenting = false;
                    sessionOpen = true;
                }
                if (HNAPI.Event.HN_EVT_SESSION_CLOSED == eventCode)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("received shutdown or closed event");
                    }
                    contentPresenting = false;
                    sessionOpen = false;
                }
                if (HNAPI.Event.HN_EVT_INACTIVITY_TIMEOUT == eventCode)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("received inactivity event");
                    }
                    // Got timeout so need to re-issue request to server
                    contentPresenting = false;
                }
                if (!initialEventCondition.getState())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("setting initial event condition to true");
                    }
                    initialEventCondition.setTrue();
                }
                if (contentPresenting && !contentPresentingCondition.getState())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("setting initial content presenting condition to true");
                    }
                    contentPresentingCondition.setTrue();
                }

                EDListener delegatingListener = listener;
                if (delegatingListener != null)
                {
                    delegatingListener.asyncEvent(eventCode, eventData1, eventData2);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("no delegating listener registered to notify of event: " + toString());
                    }
                }
            }
        };
    }

    /**
     * Request reservation for a resource for later transmission.
     *
     * This method blocks until {@link HNAPI.Event#HN_EVT_SESSION_OPENED} is received
     * or a timeout is encountered (configurable via OCAP.hn.opensession.timeout.millis property, default 30 seconds).
     * 
     * 
     * @param requestURI
     *            the URI representing the requested resource
     * @param sessionProtocolInfo
     *            selected (RemoteService) or created (JMF) protocolInfo
     * @param sourceConnectionId
     *            the connection id provided by the remote server
     * @param remoteCMS the server a ConnectionComplete will be sent to
     * 
     * @throws HNStreamingException
     *             if the session could not be opened
     */
    public void openSession(URI requestURI, HNStreamProtocolInfo sessionProtocolInfo, int sourceConnectionId, 
                            UPnPClientService remoteCMS) 
    throws HNStreamingException
    {
        if (log.isDebugEnabled())
        {
            log.debug("openSession - requestURI: " + requestURI + ", protocolInfo: " + protocolInfo + ", instance: " + toString());
        }

        try
        {
            // Save a reference to remote connection manager service action to notify when connection is complete
            sourceCMS = remoteCMS;
            this.sourceConnectionId = sourceConnectionId;
    
            // Save reference to protocol info to determine if connection stalling is supported
            protocolInfo = sessionProtocolInfo;

            HNStreamParams params = new HNStreamParamsMediaPlayerHttp(sourceConnectionId, requestURI, protocolInfo);

            reservationSessionId = HNAPIImpl.nativeStreamOpen(internalListener, params);
            //block until the session opened event is received or timeout is reached
            waitForInitialOpen();
        }
        catch (Throwable t)
        {
            throw new HNStreamingException("Unable to open stream", t);
        }
    }

    public boolean isPresenting()
    {
        return contentPresenting;
    }

    public int getSourceConnectionId()
    {
        return sourceConnectionId;
    }

    private void waitForInitialOpen() throws HNStreamingException
    {
        if (log.isDebugEnabled())
        {
            log.debug("waitForInitialOpen");
        }
        String initialOpenTimeoutMillisString = PropertiesManager.getInstance().getProperty(SESSION_TIMEOUT_MILLIS_PARAM, null);
        int initialOpenTimeoutMillis = (initialOpenTimeoutMillisString != null ? Integer.parseInt(initialOpenTimeoutMillisString) : DEFAULT_SESSION_TIMEOUT_MILLIS);

        try
        {
            initialEventCondition.waitUntilTrue(initialOpenTimeoutMillis);
            if (log.isDebugEnabled())
            {
                log.debug("after wait for initial event condition: " + toString());
            }
        }
        catch (InterruptedException e)
        {
            throw new HNStreamingException("session open event not received");
        }

        if (!sessionOpen)
        {
            throw new HNStreamingException("session open event not received");
        }
    }

    private void waitForContentPresenting() throws HNStreamingException
    {
        if (log.isDebugEnabled())
        {
            log.debug("waitForContentPresenting");
        }
        String initialContentPresentingTimeoutMillisString = PropertiesManager.getInstance().getProperty(SESSION_TIMEOUT_MILLIS_PARAM, null);
        int initialContentPresentingTimeoutMillis = (initialContentPresentingTimeoutMillisString != null ? Integer.parseInt(initialContentPresentingTimeoutMillisString) : DEFAULT_SESSION_TIMEOUT_MILLIS);

        try
        {
            contentPresentingCondition.waitUntilTrue(initialContentPresentingTimeoutMillis);
            if (log.isDebugEnabled())
            {
                log.debug("after wait for initial content presenting condition: " + toString());
            }
        }
        catch (InterruptedException e)
        {
            throw new HNStreamingException("initial content presenting event not received");
        }

        if (!contentPresenting)
        {
            throw new HNStreamingException("initial content presenting event not received");
        }
    }

    /**
     * Set HN stream components and DTCP descriptor.
     *
     * @param restartPlayback true if playback should be re-initialized
     * @param components service components.
     * @param cciDescriptors
     * @throws org.cablelabs.impl.media.streaming.exception.HNStreamingException if transmission could not be re-initialized or components set
     */
    public void setComponents(boolean restartPlayback, ServiceComponent[] components, HNPlaybackCopyControlInfo[] cciDescriptors) throws HNStreamingException
    {
        synchronized (parametersLock)
        {
            parameters.setAVStreamParameters(new HNHttpHeaderAVStreamParameters(components));
            if (restartPlayback && !initialSetComponentsCalled)
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("setComponents: " + Arrays.toString(components) + " - restarting playback");
                }
                HNAPIImpl.nativePlaybackStop(transmissionSessionId, HNAPI.MediaHoldFrameMode.HN_MEDIA_STOP_MODE_BLACK);
                transmissionStarted = false;
                
                //pass in null for CCI descriptors if there are none
                requestTransmissionAndDecode(parameters.getVideoDevice(), parameters.getInitialMediaTimeNS(), currentBlocked,
                        currentMuted, currentGain, parameters.getRequestedRate(), cciDescriptors);
            }
            else
            {
                if (transmissionStarted)
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info("setComponents: " + Arrays.toString(components) + "- not restarting, changing playback pids");
                    }
                    try
                    {
                        HNAPIImpl.nativePlayerPlaybackChangePIDs(transmissionSessionId, new HNHttpHeaderAVStreamParameters(components));
                        HNAPIImpl.nativePlayerPlaybackUpdateCCI(transmissionSessionId, (cciDescriptors != null && cciDescriptors.length > 0 ? cciDescriptors : null));
                    }
                    catch (Throwable t)
                    {
                        throw new HNStreamingException("Unable to open stream", t);
                    }
                }
                else
                {
                    if (log.isWarnEnabled()) 
                    {
                        log.warn("setComponents: " + Arrays.toString(components) + ", not S0 increasing but transmission not started - ignoring");
                    }
                }
            }
            initialSetComponentsCalled = true;
        }
    }

    /**
     * Request transmission and begin decoding when stream arrives.
     * 
     * Expects playbackstop to already have been called if not initial playback.
     * 
     * @param videoDevice
     *            the video device to use when decoding
     * @param startTimeNanos
     * @param rate
         *            the initial rate
     * @param cciDescriptors
     * @throws HNStreamingException
     *             if the request to transmit fails
     */
    public void requestTransmissionAndDecode(int videoDevice, long startTimeNanos, boolean initialBlockingState, boolean muted,
                                             float initialGain, float rate, HNPlaybackCopyControlInfo[] cciDescriptors)
            throws HNStreamingException
    {
        if (log.isInfoEnabled())
        {
            log.info("requestTransmissionAndDecode - startTimeNanos: " + startTimeNanos + ", rate: " + rate);
        }
        //initialize contentpresenting condition to false in order to block until it is received
        try
        {
            contentPresentingCondition.setFalse();
            // TODO: how/do we get blocking flag?
            if (sessionOpen)
            {
                synchronized (parametersLock)
                {
                    if (parameters == null)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("requestTransmissionAndDecode() - parameter null, creating new");
                        }
                        //if not provided, construct a default playbackparams instance
                        // constructor for avstreamparams object
                        // not requesting specific pids
                        parameters = new HNPlaybackParamsMediaPlayerHttp(new HNHttpHeaderAVStreamParameters(),
                                videoDevice, initialBlockingState, muted, initialGain, rate, startTimeNanos,
                                (cciDescriptors != null && cciDescriptors.length > 0 ? cciDescriptors : null));
                    }
                    else
                    {
                        parameters.setRequestedRate(rate);
                        parameters.setInitialMediaTimeNS(startTimeNanos);
                        parameters.setPlaybackCopyControl((cciDescriptors != null && cciDescriptors.length > 0 ? cciDescriptors : null));
                    }
                }
                currentBlocked = initialBlockingState;
                currentMuted = muted;
                currentGain = initialGain;
    
                HNAPI.Playback playback = HNAPIImpl.nativePlaybackStart(reservationSessionId, parameters, rate, currentBlocked, 
                        currentMuted, currentGain);
                transmissionSessionId = playback.handle;
                sourceConnectionId = HNAPIImpl.nativePlayerGetConnectionId(reservationSessionId);
    
                transmissionStarted = true;
                if (!contentPresentingCondition.getState())
                {
                    waitForContentPresenting();
                }
                // TODO: refactor so that the presentation context is not exposed to
                // the session layer.
                currentRate = playback.rate;
                pc.clockSetRate(playback.rate, false);
                //update mediatime as well
                pc.clockSetMediaTime(new Time(startTimeNanos), false);
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("requesting transmission but previous reservation failed - ignoring");
                }
            }
        }
        catch (MPEMediaError e)
        {
            throw new HNStreamingException("error while trying to present session", e);
        }
    }

    /**
     * No-op - does not de-authorize and tear down the connection.
     * 
     * Close session will both close the session and stop playback if necessary.
     */
    public void releaseResources()
    {
        if (log.isInfoEnabled())
        {
            log.info("releaseResources");
        }
    }

    public void closeSession()
    {
        if (log.isInfoEnabled())
        {
            log.info("closeSession - session open: " + sessionOpen);
        }
        if (sessionOpen)
        {
            if (transmissionStarted)
            {
                if (log.isInfoEnabled())
                {
                    log.info("releaseResources - transmission started - stopping playback");
                }
                HNAPIImpl.nativePlaybackStop(transmissionSessionId, HNAPI.MediaHoldFrameMode.HN_MEDIA_STOP_MODE_BLACK);
                transmissionStarted = false;
            }
            ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
            sm.getSIDatabase().unregisterForHNPSIAcquisition(reservationSessionId);
            HNAPIImpl.nativeStreamClose(reservationSessionId);
            sessionOpen = false;
            
            // Notify remote connection manager that connection is complete
            notifyRemoteCMSConnectionComplete();
            
            sourceConnectionId = 0;
            reservationSessionId = 0;
        }
    }

    public float getRate()
    {
        if (transmissionStarted)
        {
            return HNAPIImpl.nativePlayerPlaybackGetRate(transmissionSessionId);
        }
        if (log.isWarnEnabled())
        {
            log.warn("transmission not started - returning default rate");
        }
        return 1.0F;
    }

    public Time getMediaTime()
    {
        Time mediaTime = null;
        
        // Look for special case where playback is paused and connection stalling is not supported
        if ((currentRate == 0.0) && (!protocolInfo.isConnectionStallingSupported()))

        {
            // Use the initial time when rate was set to zero
            mediaTime = new Time(parameters.getInitialMediaTimeNS());
        }
        else
        {
            mediaTime = HNAPIImpl.nativePlayerPlaybackGetTime(transmissionSessionId);
        }
        return mediaTime;
    }

    public float setRate(float rate) throws HNStreamingException
    {
        //may be called at BOS/EOS or while streaming..
        Time playTime = getMediaTime();
       
        if (currentRate == rate)
        {
            log.info("setRate: already at requested rate " + rate);
            return currentRate;
        }
        
         // Determine if content supports connection stalling
        boolean isConnectionStallingSupported = protocolInfo.isConnectionStallingSupported();
        
        // Save current rate as previous rate if not paused
        if (currentRate != 0.0)
        {
            previousRate = currentRate;
        }

        if (log.isInfoEnabled())
        {
            log.info("setRate: called with new rate " + rate + ", current rate: " + currentRate +
                    ", previous rate: " + previousRate + ", stalling supported? " +
                    protocolInfo.isConnectionStallingSupported() + ", transmission started? " +
                    transmissionStarted);
        }
        
        // Clear flags which indicate pausing or resuming
        boolean pausing = false;
        boolean resuming = false;

        // Need to make a rate change on current playback 
        if (transmissionStarted)
        {
            // Should playback be paused?
            if ((rate == 0.0) && (currentRate != 0.0))
            {
                // Cannot pause if connection stalling is not supported
                if(isConnectionStallingSupported)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("setRate() - attempting to pause playback");
                    }                                        
                    // Try to pause this playback
                    try
                    {
                        HNAPIImpl.nativePlayerPlaybackPause(transmissionSessionId);
                        if (log.isInfoEnabled())
                        {
                            log.info("setRate() - paused playback");
                        }    
                        pausing = true;
                    }
                    catch (HNStreamingException e)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("setRate() - unable to pause playback: ", e);
                        }        
                        throw e;
                    }                                        
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("setRate:  connectionStalling is not supported.. cannot pause!");
                    }
                    return currentRate;
                }
            }
            // Should playback be resumed?
            else if ((rate != 0.0) && (currentRate == 0.0) && (rate == previousRate) && 
                    (isConnectionStallingSupported))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("setRate() - attempting to resume playback");
                }                                        
                // Try to resume this playback
                try
                {
                    HNAPIImpl.nativePlayerPlaybackResume(transmissionSessionId);
                
                    if (log.isInfoEnabled())
                    {
                        log.info("setRate() - resumed playback");
                    }    
                    resuming = true;
                }
                catch (HNStreamingException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("setRate() - unable to resume playback: ", e);
                    }                     
                    throw e;
                }                            
            }
            
            if ((!pausing) && (!resuming)) 
            {
                // Not pausing or resuming. 
                if(!protocolInfo.isTimeSeekSupported() && !protocolInfo.isNetworkRangeSupported())
                {
                    // These fields are set to 'false' by the server
                    // to indicate time seek range, network byte range request 
                    // are not reported (e.g VPOP). Return from here
                    if (log.isInfoEnabled())
                    {
                        log.info("setRate: protocolInfo.isTimeSeekSupported and protocolInfo.isNetworkRangeSupported are false..!");
                    }
                    return currentRate;
                }
                
                // Not pausing or resuming an active transmission, need to stop playback
                if (log.isInfoEnabled())
                {
                    log.info("setRate() - transmission started & not pausing or resuming - stopping playback");
                }
              
                // If new rate = 0, hold frame to simulate pause since not supported, 
                // otherwise clear since starting new playback
                int holdFrame = HNAPI.MediaHoldFrameMode.HN_MEDIA_STOP_MODE_BLACK;
                if (rate == 0)
                {
                    holdFrame = HNAPI.MediaHoldFrameMode.HN_MEDIA_STOP_MODE_HOLD_FRAME;
                }
                
                // Stop playback displaying last frame as appropriate
                HNAPIImpl.nativePlaybackStop(transmissionSessionId, holdFrame);
                // Set flag to since transmission is going to be restarted
                transmissionStarted = false;
                try
                {
                    requestTransmissionAndDecode(parameters.getVideoDevice(), playTime.getNanoseconds(), currentBlocked,
                            currentMuted, currentGain, rate, parameters.getPlaybackCopyControl());
                    return rate;
                }
                catch (HNStreamingException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("setRate() - problems changing rate - returning current rate: ", e);
                    }
                    throw e;
                }
            }
            // Pausing or resuming active playback
            else 
            {
                if (log.isDebugEnabled())
                {
                    log.debug("setRate() - not requesting transmission & decode, just adjusting parameter values");
                }                                        
                
                // Not starting another transmission or decode, just need to adjust rate related parameters
                parameters.setRequestedRate(rate);
                parameters.setInitialMediaTimeNS(playTime.getNanoseconds());
                currentRate = rate;
                
                // TODO: refactor so that the presentation context is not exposed to
                // the session layer.
                pc.clockSetRate(rate, false); 
                //update mediatime as well
                pc.clockSetMediaTime(new Time(playTime.getNanoseconds()), false);
            }
        }
        return rate;
    }

    public void setMediaTime(Time mediaTime)
    {
        //may be called at BOS/EOS or while streaming..

        // Need to stop current playback
        if (log.isDebugEnabled())
        {
            log.debug("setMediaTime: " + mediaTime);
        }
        
        if (transmissionStarted)
        {
            if (log.isInfoEnabled())
            {
                log.info("setMediaTime - transmission started - stopping playback");
            }
            HNAPIImpl.nativePlaybackStop(transmissionSessionId, 
                    HNAPI.MediaHoldFrameMode.HN_MEDIA_STOP_MODE_HOLD_FRAME);
            transmissionStarted = false;
        }
            
        
        // Re-initiate new playback with new rate and time
        try
        {
            requestTransmissionAndDecode(parameters.getVideoDevice(), mediaTime.getNanoseconds(), currentBlocked, 
                currentMuted, currentGain, getRate(), parameters.getPlaybackCopyControl());
        }
        catch (HNStreamingException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("setMediaTime() - problems changing mediatime: ", e);
            }
        }
    }

    public int getNativeHandle()
    {
        return reservationSessionId;
    }

    public void setMute(boolean mute)
    {
        currentMuted = mute;
        HNAPIImpl.nativePlayerPlaybackSetMute(transmissionSessionId, currentMuted);
    }

    public float setGain(float gain)
    {
        currentGain = gain;
        return HNAPIImpl.nativePlayerPlaybackSetGain(transmissionSessionId, currentGain);
    }

    public void blockPresentation(boolean blockPresentation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("blockPresentation: " + blockPresentation);
        }
        currentBlocked = blockPresentation;
        HNAPIImpl.nativePlayerPlaybackBlockPresentation(transmissionSessionId, currentBlocked);
    }

    public boolean isReserved()
    {
        return sessionOpen;
    }

    public void setListener(EDListener listener)
    {
        this.listener = listener;
    }

    public String toString()
    {
        return "HNClientSession - handle: " + reservationSessionId + ", session open: " +
                sessionOpen + ", transmission started: " + transmissionStarted + ", " + super.toString();
    }
    
    /**
     * Notify the remote Connection Manager Service that the connection is complete
     * and can be closed down.
     */
    private void notifyRemoteCMSConnectionComplete()
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyRemoteCMSConnectionComplete() - called");
        }
        if (sourceCMS != null)
        {
            // Invoke the connection complete action
            UPnPAction ccAction = sourceCMS.getAction(ConnectionManagerService.CONNECTION_COMPLETE);
            if (ccAction != null)
            {
                UPnPActionInvocation ccInvocation = new UPnPActionInvocation(
                                                            new String[]{Integer.toString(sourceConnectionId)}, ccAction);
                
                // Invoke the action but don't supply handler since there is nothing to be done
                if (log.isDebugEnabled())
                {
                    log.debug("notifyRemoteCMSConnectionComplete() - invoking connection complete on remote CMS");
                }
                sourceCMS.postActionInvocation(ccInvocation, null);
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("notifyRemoteCMSConnectionComplete() - remote CMS had no action named: " +
                    ConnectionManagerService.CONNECTION_COMPLETE);
                }
            }            
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyRemoteCMSConnectionComplete() - remote CMS was null");
            }
        }
    }
}
