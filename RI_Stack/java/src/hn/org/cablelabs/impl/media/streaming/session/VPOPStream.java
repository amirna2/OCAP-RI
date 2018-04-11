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

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.mpe.HNAPI;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackCopyControlInfo;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescription;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescriptionVideoDevice;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentLocationType;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.media.streaming.session.util.ContentRequestConstant;
import org.cablelabs.impl.media.streaming.session.util.StreamUtil;
import org.cablelabs.impl.ocap.hn.ContentServerNetModuleImpl;
import org.cablelabs.impl.ocap.hn.content.ContentItemImpl;
import org.cablelabs.impl.ocap.hn.transformation.NativeContentTransformation;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.cm.ConnectionCompleteListener;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.StreamingActivityListener;

public class VPOPStream implements Stream
{
    private static final Logger log = Logger.getLogger(VPOPStream.class);

    private static final int STATE_INIT = 1;
    private static final int STATE_TRANSMITTING = 2;
    private static final int STATE_STOPPED = 3;

    private final HNStreamContentDescriptionVideoDevice m_videoDeviceLocation;
    private final ContentItemImpl m_contentItem;
    private final HNServerSession m_session;
    private final Socket m_socket;
    private final String m_url;
    private final int m_contentLocationType;
    private final HNStreamProtocolInfo m_protocolInfo;
    private final Integer m_connectionId;

    private final ConnectionCompleteListener m_connectionCompleteListener;

    private final Object m_lock = new Object();
    
    private int m_currentState = STATE_INIT;
    private final NativeContentTransformation m_transformation;

    /**
     * Constructor - creates and opens an HNServerSession
     * 
     * @param videoDeviceLocation the HNStreamContentDescription identifying the stream source
     * @param request the contentRequest associated with this stream
     * @param contentItem the content item to stream
     * @throws HNStreamingException if the session could not be opened 
     */
    public VPOPStream( HNStreamContentDescriptionVideoDevice videoDeviceLocation, 
                       ContentRequest request, ContentItemImpl contentItem ) 
        throws HNStreamingException
    {
        m_videoDeviceLocation = videoDeviceLocation;
        m_contentItem = contentItem;
        m_url = request.getURI();
        m_transformation = request.getTransformation();
        m_contentLocationType = HNStreamContentLocationType.HN_CONTENT_LOCATION_VIDEO_DEVICE;        

        if (log.isInfoEnabled())
        {
            log.info("constructing VPOPStream - id: " + request.getConnectionId() + ", url: " + m_url);
        }

        m_protocolInfo = request.getProtocolInfo();
        
        // We ignore rate and range requests for VPOP service (it's a non-random-access service)
        
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
        
        m_connectionId = request.getConnectionId();
        m_session = new HNServerSession(MediaServer.getServerIdStr());

        m_socket = request.getSocket();

        //listener released in session#releaseResources - no need to hold a reference 
        m_session.setListener(new EDListenerImpl());

        m_connectionCompleteListener = new ConnectionCompleteListenerImpl();
        MediaServer.getInstance().getCMS().addConnectionCompleteListener(m_connectionCompleteListener);
    }
    
    public void open(ContentRequest request) throws HNStreamingException
    {
        m_session.openSession( m_socket, request.getChunkedEncodingMode(),
                m_protocolInfo.getProfileId(), 
                m_protocolInfo.getContentFormat(),
                request.getMaxTrickModeBandwidth(), request.getCurrentDecodePTS(), 
                request.getMaxGOPsPerChunk(), request.getMaxFramesPerGOP(), 
                request.isUseServerSidePacing(), request.getRequestedFrameTypesInTrickMode(), 
                m_connectionId.intValue(), m_contentItem );
    }

    public void transmit() throws HNStreamingException
    {
        synchronized(m_lock)
        {
            if (m_currentState != STATE_INIT)
            {
                throw new IllegalArgumentException("transmit called in incorrect state: " + m_currentState);
            }
            m_currentState = STATE_TRANSMITTING;
        }

        MediaServer.getInstance().getCMS().registerLocalConnection(m_protocolInfo, m_connectionId.intValue(), -1);

        // Notify when content begins streaming (notifying prior to streaming start)
        if (m_contentItem != null)
        {
            ContentServerNetModuleImpl server = (ContentServerNetModuleImpl)m_contentItem.getServer();
            if (server != null)
            {
                server.notifyStreamStarted(m_contentItem, m_connectionId.intValue(),
                    m_url, null, StreamingActivityListener.CONTENT_TYPE_LIVE_RESOURCES);        

                // VideoDevice-based content is always streamed at the live point
                m_session.transmit(m_contentLocationType, m_videoDeviceLocation, 1.0f, false, -1, -1, 
                    -1, -1, new HNPlaybackCopyControlInfo[] {new HNPlaybackCopyControlInfo((short)-1, true,false,(byte)0)}, m_transformation);
                m_session.waitForTransmissionToComplete();
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("transmit() - null content item server");
                }
                throw new IllegalArgumentException("Content item server was null");                
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("transmit() - null content item");
            }
            throw new IllegalArgumentException("Content item was null");
        }
    }
    
    public void stop(boolean closeSocket)
    {
        //may be called in any state, but no-op if already stopped
        synchronized(m_lock)
        {
            if (m_currentState == STATE_STOPPED)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring stop when already stopped");
                }
                return;
            }
            m_currentState = STATE_STOPPED;
        }
        
        m_session.releaseResources(closeSocket, true);
        
        //release listener
        MediaServer.getInstance().getCMS().removeConnectionCompleteListener(m_connectionCompleteListener);
        if (log.isInfoEnabled())
        {
            log.info("VPOPStream stop() - closeSocket: " + closeSocket);
        }
        if (closeSocket)
        {
            if (m_socket != null)
            {
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("VPOPStream stop() - closing socket..");
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
        return m_contentLocationType;
    }
    
    public String toString()
    {
        return "VPOPStream - connectionId: " + m_connectionId + ", url: " + m_url;
    }

    private void releaseAndDeauthorize(final boolean deauthorizeNow, final int resultCode, boolean closeSocket)
    {
        //may come in from an asyncEvent or a notifyComplete - no-op if already stopped
        synchronized(m_lock)
        {
            if (m_currentState == STATE_STOPPED)
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
            log.info("releaseAndDeauthorize - now: " + deauthorizeNow + " - id: " + m_connectionId + ", url: " + m_url);
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
        return m_videoDeviceLocation;
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
                releaseAndDeauthorize(true, HttpURLConnection.HTTP_OK, true);
            }
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
            if (log.isDebugEnabled())
            {
                log.debug("asyncEvent: " + eventCode + ", data1: " + eventData1 + ", data2: " + eventData2);
            }

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
    
    // Although VPOP does not support trick modes it is still operating under Limited RADA Mode 0.
    // 
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

    public int getFrameTypesInTrickMode() throws HNStreamingException
    {
        // No trick mode support since this stream is always presenting at the
        // live point (no random access)
        return ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_NONE;
    }

    public long getStartByte()
    {
        // This stream is always presenting at the live point (no random access)
        return -1;
    }
}
