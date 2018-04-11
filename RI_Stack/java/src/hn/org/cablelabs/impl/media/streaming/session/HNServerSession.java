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

import java.net.Socket;

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.mpe.HNAPI;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackCopyControlInfo;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackParams;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackParamsMediaServerHttp;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescription;
import org.cablelabs.impl.media.streaming.session.data.HNStreamParamsMediaServerHttp;
import org.cablelabs.impl.ocap.hn.UsageTrackingContentItem;
import org.cablelabs.impl.ocap.hn.transformation.NativeContentTransformation;
import org.cablelabs.impl.util.SimpleCondition;

/**
 * Represents the server-side of a Home Networking client/server session.
 * 
 * Responds to requests from a client requesting transmission of a resource.
 */
public class HNServerSession
{
    private static final Logger log = Logger.getLogger(HNServerSession.class);

    //default duration opensession/requesttransmission will wait for a success event prior to timing out
    //configurable via OCAP.hn.session.timeout.millis
    private static final int DEFAULT_SESSION_TIMEOUT_MILLIS = 30000;
    private static final String SESSION_TIMEOUT_MILLIS_PARAM = "OCAP.hn.session.timeout.millis";
    
    private final EDListener internalListener;
    private EDListener listener;

    private volatile boolean transmissionInProgress;

    //result of open
    private int sessionId = -1;

    //result of transmit
    private int playbackId = -1;

    private Socket socket;

    protected UsageTrackingContentItem usageTrackingContentItem;

    protected String serverIdStr;

    private SimpleCondition initialEventCondition = new SimpleCondition(false);
    private boolean sessionOpen;
    
    public HNServerSession(String serverIdStr)
    {
        this.serverIdStr = serverIdStr;
        internalListener = new EDListener()
        {
            public void asyncEvent(int eventCode, int eventData1, int eventData2)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("asyncEvent - received: " + eventCode + ", " + eventData1 + ", " + eventData2);
                }
                //a delegating listener may not yet be registered - track open state
                if (HNAPI.Event.HN_EVT_SESSION_OPENED == eventCode)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("received session opened event");
                    }
                    sessionOpen = true;
                }
                if (HNAPI.Event.HN_EVT_SESSION_CLOSED == eventCode)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("received shutdown or closed event");
                    }
                    sessionOpen = false;
                }
                if (!initialEventCondition.getState())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("setting initial event condition to true");
                    }
                    initialEventCondition.setTrue();
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

    public void setListener(EDListener listener)
    {
        this.listener = listener;
    }
    
    /**
     * Process a client-initiated request to authorize a session prior to
     * transmission.
     * <p>
     * If a NetAuthorizationHandler is registered, the NetAuthorizationHandler
     * will determine if this request is authorized.
     * 
     * @param socket the socket 
     * @param chunkedEncodingMode requested mode for chunk encoding, GOP, Frame, or other  
     * @param dlnaProfileId the profile ID associated with the content to be streamed 
     * @param maxTrickModeBandwidth max bits per sec to be sent in trick mode, -1 unspecified 
     * @param currentDecodePTS render's current decoder timestamp, -1 unspecified
     * @param maxGOPsPerChunk requested max number of GOPs included in each chunk, -1 unspecified
     * @param maxFramesPerGOP requested max number of frames per GOP, -1 unspecified
     * @param useServerSidePacing requested server side pacing
     * @param frameTypesInTrickModes requested frame type for trick modes
     * @param connectionId the connection id
     * @param usageTrackingContentItem the content item to transmit
     * @throws HNStreamingException if the session could not be opened
     */
    public void openSession(Socket socket, int chunkedEncodingMode, String dlnaProfileId, String mimeType,
            long maxTrickModeBandwidth, long currentDecodePTS, int maxGOPsPerChunk, int maxFramesPerGOP, 
            boolean useServerSidePacing, int frameTypesInTrickModes,
            int connectionId, UsageTrackingContentItem usageTrackingContentItem) throws HNStreamingException
    {
        try
        {
            this.socket = socket;

            int connectionStallingTimeoutMS = HNServerSessionManager.getInstance().getConnectionStallingTimeoutMS();
        
            HNStreamParamsMediaServerHttp params = new HNStreamParamsMediaServerHttp(connectionId, dlnaProfileId,
                mimeType, socket, chunkedEncodingMode, maxTrickModeBandwidth, currentDecodePTS,
                maxGOPsPerChunk, maxFramesPerGOP, useServerSidePacing, frameTypesInTrickModes, 
                connectionStallingTimeoutMS);

            if (log.isDebugEnabled()) 
            {
                log.debug("calling nativeStreamOpen - params: " + params);
            }
            sessionId = HNAPIImpl.nativeStreamOpen(internalListener, params);
    
            // Update the usage count of the item associated with this session
            this.usageTrackingContentItem = usageTrackingContentItem;
    
            usageTrackingContentItem.incrementInUseCount();
    
            waitForInitialOpen();
        }
        catch (Throwable t)
        {
            throw new HNStreamingException("Unable to open stream", t);
        }
    }
    
    /**
     * Transmit the requested resource (request is already authorized)
     * 
     * @param contentLocationType
     * @param contentDescription
     * @param rate
     * @param startBytePosition
     * @param endBytePosition
     * @param startTime 
     * @param endTime 
     * @param hnPlaybackCopyControlInfo array containing copycontrol info - may be null
     * @param transform 
     * @param useTimeOffsetValues 
     * @throws HNStreamingException
     */
    public void transmit(int contentLocationType, HNStreamContentDescription contentDescription, 
                         float rate, boolean useTimeOffsetValues,
                         long startBytePosition, long endBytePosition,
                         long startTime, long endTime, 
                         HNPlaybackCopyControlInfo[] hnPlaybackCopyControlInfo, 
                         NativeContentTransformation transform) throws HNStreamingException
    {
        HNPlaybackParams params = new HNPlaybackParamsMediaServerHttp(contentLocationType,
                                    contentDescription, rate, 
                                    useTimeOffsetValues, startBytePosition, 
                                    endBytePosition, startTime,
                                    endTime, hnPlaybackCopyControlInfo, transform);
        // *TODO* - isBlocked is hard coded to false, use default mute and gain
        try
        {
            HNAPI.Playback playback = HNAPIImpl.nativePlaybackStart(sessionId, params,  rate, false, false, 0.0F);
            playbackId = playback.handle;
    
            if (log.isInfoEnabled())
            {
                log.info("transmit - rate: " + rate + ", start byte: " + startBytePosition + ", end byte: " + endBytePosition  + ", description: " + contentDescription);
            }
            setTransmissionInProgress();
        }
        catch (Throwable t)
        {
            throw new HNStreamingException("Unable to start playback", t);   
        }
    }

    /**
     * Process a client-initiated request to release the reservation
     * (transmission stopped if applicable and resources released).
     * 
     * If session is complete, decrement usage tracking.  Always update transmittionInProgress flag.
     */
    public void releaseResources(boolean closeSocket, boolean sessionComplete)
    {
        if (log.isInfoEnabled())
        {
            log.info("releaseResources - stop and close session: "+ sessionId);
        }
        
        stop();

        if (sessionId != -1)
        {
            HNAPIImpl.nativeStreamClose(sessionId);
            sessionId = -1;
        }

        if (sessionComplete && (null != usageTrackingContentItem))
        {
            // Decrement the in use count of item
            usageTrackingContentItem.decrementInUseCount();

            // Set the current item to null
            usageTrackingContentItem = null;
        }

        // Not closing socket, so just null out reference (new request may come in on same socket)
        if (!closeSocket)
        {
            if (log.isInfoEnabled())
            {
                log.info("releaseResources - not closing socket but setting to null");
            }
            socket = null;
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("releaseResources - not setting socket to null since " + 
                        "socket will be closed when associated stream is stopped");
            }            
        }
        listener = null;
        setTransmissionNoLongerInProgress();
    }
    
    public void stop()
    {
        if (playbackId != -1)
        {
            if (log.isInfoEnabled())
            {
                log.info("playback started - calling stopPlayback");
            }
            HNAPIImpl.nativePlaybackStop(playbackId, HNAPI.MediaHoldFrameMode.HN_MEDIA_STOP_MODE_BLACK);
            playbackId = -1;
        }
    }

    public Time getMediaTime()
    {
        //TODO Is this valid for the server?
        //return new Time(HNAPIImpl.nativePlayerPlaybackGetTime(playbackId).getNanoseconds());
        throw new UnsupportedOperationException();
    }

    public void updateEndPosition(long newEndPosition)
    {
        HNAPIImpl.nativeServerUpdateEndPosition(playbackId, newEndPosition);
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
            throw new HNStreamingException("session open event not received in: " + initialOpenTimeoutMillis + " millis");
        }
    }
            
    private synchronized void setTransmissionNoLongerInProgress()
    {
        if (log.isDebugEnabled())
        {
            log.debug("setTransmissionNoLongerInProgress");
        }
        if (transmissionInProgress)
        {
            transmissionInProgress = false;
            notifyAll();
        }
    }

    // Added for findbugs issues fix - start
    // Synchronizing transmissionInProgress variable
    private synchronized void setTransmissionInProgress()
    {
        if (log.isDebugEnabled())
        {
            log.debug("setTransmissionInProgress");
        }
        transmissionInProgress = true;
    } 

    // Added for findbugs issues fix - end
    public synchronized void waitForTransmissionToComplete()
    {
        if (log.isInfoEnabled())
        {
            log.info("waitForTransmissionToComplete");
        }
        
        while (transmissionInProgress)
        {
            try
            {
                wait();
            }
            catch (Exception e)
            {
                // ignore
            }
        }

        if (log.isInfoEnabled())
        {
            log.info("waitForTransmissionToComplete - " +
            "saw transmission complete");
        }
    }
    
    public String toString()
    {
        return "HNServerSession - socket: " + socket + ", sessionId: " + sessionId + ", playback handle: " + playbackId;
    }
}
