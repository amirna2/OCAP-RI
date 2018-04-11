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
package org.cablelabs.impl.ocap.hn.vpop;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.ContentRequest;
import org.cablelabs.impl.media.streaming.session.HNServerSessionManager;
import org.cablelabs.impl.media.streaming.session.VPOPStream;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescriptionVideoDevice;
import org.cablelabs.impl.ocap.hn.content.ContentItemImpl;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer.HTTPRequest;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.cds.HTTPRequestInterceptor;
import org.cablelabs.impl.util.MPEEnv;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;

/**
 * An HTTP request interceptor supporting VPOP ContentItems published in the CDS
 */
public class VPOPInterceptor implements HTTPRequestInterceptor
{
    private static final Logger log = Logger.getLogger(VPOPInterceptor.class);

    private static final String ENV_ALLOW_STREAMING_FROM_SELF = "OCAP.hn.server.vpop.allowStreamingFromSelf";
    private static final String ENV_ALLOW_STREAMING_FROM_SAME_SESSION = "OCAP.hn.server.vpop.allowStreamingFromSameSession";
    private static final boolean m_allowStreamingFromSelf = "true".equals(MPEEnv.getEnv(ENV_ALLOW_STREAMING_FROM_SELF));
    private static final boolean m_allowSameSessionException = "true".equals(MPEEnv.getEnv(ENV_ALLOW_STREAMING_FROM_SAME_SESSION));

    private static final Integer NO_CONNECTION_ID = new Integer(-1);

    private final Object m_lock = new Object();

    private VPOPStream m_currentVPOPStream = null;

    public boolean intercept( final Socket socket, final HTTPRequest httpRequest, final ContentEntry contentEntry, 
                              final URL requestURL, final URL effectiveURL ) 
        throws HNStreamingException
    {
        final String query = effectiveURL.getQuery();
        
        // example URI: http://__host_port__/ocaphn/vpop?displaysource=primary&profile=MPEG_TS_SD_NA_ISO&mime=video/mpeg
        //        path: /ocaphn/vpop
        //       query: displaysource=primary&profile=MPEG_TS_SD_NA_ISO&mime=video/mpeg
        if (!effectiveURL.getPath().startsWith(ContentDirectoryService.VPOP_REQUEST_URI_PATH))
        {
            return false;
        }
        
        // URL syntax check
        if (!query.startsWith(ContentDirectoryService.VPOP_REQUEST_URI_SOURCE_PREFIX))
        {
            if (log.isInfoEnabled())
            {
                log.info( "intercepting: Found request for vpop with missing param: " 
                          + ContentDirectoryService.VPOP_REQUEST_URI_SOURCE_PREFIX );
            }
            return false;
        }
        
        String sourceDisplay = query.substring(ContentDirectoryService.VPOP_REQUEST_URI_SOURCE_PREFIX.length());
        sourceDisplay = sourceDisplay.substring(0, sourceDisplay.indexOf('&'));
        
        if (log.isDebugEnabled())
        {
            log.debug( "intercepting: Found display source: " +  sourceDisplay);
        }
        
        final ContentRequest request = new ContentRequest(socket, httpRequest, contentEntry, null, effectiveURL.toExternalForm());
        
        InetAddress address = request.getRequestInetAddress();
        
        // Reject requests from self.
        if(!m_allowStreamingFromSelf && sameHost(address))
        {
            if (log.isInfoEnabled())
            {
                log.info( "intercepting: Rejecting request from self (" + address + ')');
            }
            request.setStatus(ActionStatus.HTTP_UNAUTHORIZED);
            request.sendHttpResponse();
            return true;
        }
        
        final ContentItem referencingContentItem = (ContentItem) contentEntry;
        
        if (log.isInfoEnabled())
        {
            log.info("intercepting: " + httpRequest + ",content item: " + referencingContentItem);
        }

        HNStreamContentDescriptionVideoDevice vpopContentDescription 
            = ContentDirectoryService.getContentDescriptionForDisplay(sourceDisplay);
        
        if (vpopContentDescription == null)
        {
            if (log.isInfoEnabled())
            {
                log.info( "intercepting: Display \"" + sourceDisplay + "\" not recognized. Giving up.");
            }
            return false;
        }
        if (log.isDebugEnabled())
        {
            log.debug( "intercepting: Video device: 0x" 
                       +  Integer.toHexString(vpopContentDescription.getVideoDeviceHandle()));
        }
        
        final ContentItemImpl vpopContentItem = (ContentItemImpl)referencingContentItem;

        //acquire resources
        
        // Process head request with temporary VPOPStream
        if(request.isHeadRequest())
        {
            VPOPStream headStream = new VPOPStream(vpopContentDescription, request, vpopContentItem);
            
            request.setStreamingContext( headStream.getContentLocationType(), 
                    headStream.getFrameRateInTrickMode(), 
                    headStream.getFrameTypesInTrickMode(), 
                    headStream.getAvailableSeekStartTime(), 
                    headStream.getAvailableSeekEndTime(),
                    headStream.getAvailableSeekStartByte(true),
                    headStream.getAvailableSeekEndByte(true),
                    headStream.getAvailableSeekStartByte(false),
                    headStream.getAvailableSeekEndByte(false),
                    headStream.getStartByte(), 
                    headStream.getEndByte(),
                    headStream.getContentDescription(),
                    headStream.getRequestedContentLength(),
                    headStream.getContentDuration() );
            
            request.sendHttpResponse();
            headStream.stop(false);
            
            return true;
        }
        VPOPStream stream = null;
        if (request.isValidRequest())
        {
            try
            {                
                // Check if currently streaming
                synchronized(m_lock)
                {
                    if (!isVPOPRequestAllowed(request, vpopContentItem))
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("intercepting: Rejecting request, VPOP stream is already active," +
                                    "response: " + ActionStatus.HTTP_UNAVAILABLE.getCode());
                        }
                        request.setStatus(ActionStatus.HTTP_UNAVAILABLE);
                        request.sendHttpResponse();                        
                        return true;
                    }
                    //not transmitting, vpop stream was already cleaned up
                    m_currentVPOPStream = new VPOPStream( vpopContentDescription, request,
                                                          vpopContentItem );
                    stream = m_currentVPOPStream;
                }
                

                if (log.isInfoEnabled())
                {
                    log.info("stream created: " + stream);
                }
                request.setStreamingContext( stream.getContentLocationType(),
                        stream.getFrameRateInTrickMode(),
                        stream.getFrameTypesInTrickMode(),
                        stream.getAvailableSeekStartTime(),
                        stream.getAvailableSeekEndTime(),
                        stream.getAvailableSeekStartByte(true),
                        stream.getAvailableSeekEndByte(true),
                        stream.getAvailableSeekStartByte(false),
                        stream.getAvailableSeekEndByte(false),
                        stream.getStartByte(),
                        stream.getEndByte(),
                        stream.getContentDescription(),
                        stream.getRequestedContentLength(),
                        stream.getContentDuration() );
            }
            catch (HNStreamingException hnse)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to create VPOP stream", hnse);
                }
            }
        }

        //stream may not have been created, still send response
        if ((stream == null) || (!request.isValidRequest()))
        {
            request.setStatus(ActionStatus.HTTP_BAD_REQUEST);
            // Either request was invalid no streaming, so we are done
            request.sendHttpResponse();
            if (stream != null)
            {
                stream.stop(false);
            }
        }
        else
        {
            HNServerSessionManager serverSessionManager = HNServerSessionManager.getInstance();
            Integer connectionId = request.getConnectionId();
            if (log.isDebugEnabled())
            {
                log.debug("intercept: checking authorization for connection id: " + connectionId+ " for content item: " + referencingContentItem);
            }

            boolean authorized = serverSessionManager.authorize( address, requestURL, 
                                                                 connectionId, 
                                                                 referencingContentItem, 
                                                                 request.getRequestStrings(),
                                                                 request.getNetworkInterface() );
            if (authorized)
            {
                if (log.isInfoEnabled())
                {
                    log.info("stream transmission authorized for request: " + request + " for content item: " + referencingContentItem + ", transmitting");
                }

                String sessionID =
                    request.getSocket().getInetAddress().getHostAddress()
                    + requestURL.toString();
                serverSessionManager.addActiveSession(sessionID, connectionId);

                // Remove socket timeout in order to support inactivity during streaming
                try
                {
                    request.getSocket().setSoTimeout(0);                    
                }
                catch (SocketException se)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Problems trying to set infinite socket timeout", se);
                    }
                }
               
                try
                {
                    stream.open(request);
                    // Session has been successfully opened, send HTTP response prior to sending content
                    request.sendHttpResponse();
                    serverSessionManager.transmit(stream);
                }
                catch (HNStreamingException hnse)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Unable to open stream - stopping");
                    }
                    request.setStatus(ActionStatus.HTTP_UNAVAILABLE);
                    request.sendHttpResponse();
                    stream.stop(false);
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("stream transmission not authorized for request: " + request);
                }
                request.setStatus(ActionStatus.HTTP_UNAUTHORIZED);
                request.sendHttpResponse();
                stream.stop(false);
            }
        }
        return true;
    }
    
    public Integer getConnectionId()
    {
        synchronized(m_lock)
        {
            return m_currentVPOPStream != null ? m_currentVPOPStream.getConnectionId() : NO_CONNECTION_ID;
        }
    }

    private boolean sameHost(InetAddress address)
    {
        try
        {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements())
            {
                NetworkInterface ni = (NetworkInterface)e.nextElement();
                Enumeration ie = ni.getInetAddresses();
                while(ie.hasMoreElements())
                {
                    InetAddress i = (InetAddress)ie.nextElement();
                    if(address.equals(i))
                    {
                        return true;
                    }
                }
            }
        }
        catch (SocketException e1)
        {
            return false;
        }
        return false;
    }

    private boolean isVPOPRequestAllowed(final ContentRequest request,
                                         final ContentItemImpl vpopContentItem)
    {
        if (m_currentVPOPStream == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("isVPOPRequestAllowed: no VPOP stream");
            }
            return true;
        }
        if (m_currentVPOPStream.isTransmitting(vpopContentItem))
        {
            if (m_allowSameSessionException &&
                (request.getConnectionId().intValue() == getConnectionId().intValue()))
            {
                if (log.isInfoEnabled())
                {
                    log.info("isVPOPRequestAllowed:stream request on same session");
                }
                m_currentVPOPStream.stop(false);
                return true;
            }
            return false;
           
        }

        if (log.isInfoEnabled())
        {
            log.info("isVPOPRequestAllowed:stream not transmitting");
        }
        
        return true;
    }

}

