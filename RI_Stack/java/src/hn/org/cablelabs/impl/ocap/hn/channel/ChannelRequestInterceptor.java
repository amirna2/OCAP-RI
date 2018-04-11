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
package org.cablelabs.impl.ocap.hn.channel;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.ChannelStream;
import org.cablelabs.impl.media.streaming.session.ContentRequest;
import org.cablelabs.impl.media.streaming.session.HNServerSessionManager;
import org.cablelabs.impl.media.streaming.session.Stream;
import org.cablelabs.impl.ocap.hn.content.ChannelContentItemImpl;
import org.cablelabs.impl.ocap.hn.transformation.NativeContentTransformation;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer.HTTPRequest;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.cds.HTTPRequestInterceptor;
import org.cablelabs.impl.util.HNUtil;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;

/**
 * An HTTP request interceptor supporting ChannelContentItems published in the CDS
 */
public class ChannelRequestInterceptor implements HTTPRequestInterceptor
{
    private static final Logger log = Logger.getLogger(ChannelRequestInterceptor.class);

    private static final int HTTP_STATUS_UNAUTHORIZED = 401;

    //use different Stream implementations based on the presence or non-presence of the DVR extension
    String LIVE_STREAMING_TUNER = "org.cablelabs.impl.media.streaming.session.ChannelStream";

    String LIVE_STREAMING_TSB = "org.cablelabs.impl.media.streaming.session.TSBChannelStream";


    public boolean intercept( final Socket socket, final HTTPRequest httpRequest, final ContentEntry contentEntry, 
                              final URL requestURL, final URL effectiveURL ) 
        throws HNStreamingException
    {
        String requestPathAndQuery = effectiveURL.getPath() + '?' + effectiveURL.getQuery();

        // URL syntax check
        if (requestPathAndQuery.startsWith(ContentDirectoryService.CHANNEL_REQUEST_URI_PATH))
        { // This is definitely a ChannelRequest
            // TODO: Check the query params...
            if (log.isInfoEnabled())
            {
                log.info("ChannelRequestInterceptor: HTTP request prefix match: " + contentEntry);
            }
        }
        else
        { // The HTTP request prefix doesn't match. So this request isn't for a Channel...
            return false;
        }

        ChannelContentItemImpl channelContentItem;
        if (contentEntry instanceof ChannelContentItemImpl)
        {
            channelContentItem = (ChannelContentItemImpl) contentEntry;
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("ChannelRequestInterceptor: Request matched prefix but was not a ChannelContentItemImpl: " 
                         + requestPathAndQuery);
            }
            return false;
        }

        // Look for a transformation query parameter (optional)
        NativeContentTransformation nativeTransform 
                                = HNUtil.getTransformationFromURIQuery(effectiveURL.getQuery());

        if (log.isInfoEnabled())
        {
            log.info("ChannelRequestInterceptor: intercepting: " + httpRequest + ", content item: " + channelContentItem);
        }

        //requires live streaming Stream implementations to have a constructor which takes two parameters: ChannelContentItemImpl and a ContentRequest 
        Stream stream = null;
        ContentRequest request = new ContentRequest(socket, httpRequest, channelContentItem, 
                                                    nativeTransform, effectiveURL.toExternalForm());
        if (request.isValidRequest())
        {
            String streamClassName = null;
            try
            {
                if ((System.getProperty("ocap.api.option.dvr") != null) && 
                        (channelContentItem.getChannelType() == ContentItem.VIDEO_ITEM_BROADCAST))
                {       
                    streamClassName = LIVE_STREAMING_TSB;
                    Constructor constructor = Class.forName(LIVE_STREAMING_TSB).getConstructor(new Class[]{ChannelContentItemImpl.class, ContentRequest.class});
                    stream = (Stream)constructor.newInstance(new Object[]{channelContentItem, request});
                }
                else
                {
                    streamClassName = LIVE_STREAMING_TUNER;
                    // For Broadcast VOD content the implementation SHOULD not support random access or trick modes
                    // Instantiate ChannelStream stream type (this is done even when DVR extension is enabled) 
                    stream = new ChannelStream(channelContentItem, request);
                }
                
                if (log.isInfoEnabled())
                {
                    log.info("ChannelRequestInterceptor: stream created: " + stream);
                }

                request.setStreamingContext(stream.getContentLocationType(), stream.getFrameRateInTrickMode(),
                        stream.getFrameTypesInTrickMode(),
                        stream.getAvailableSeekStartTime(), stream.getAvailableSeekEndTime(),
                        stream.getAvailableSeekStartByte(true), stream.getAvailableSeekEndByte(true),
                        stream.getAvailableSeekStartByte(false), stream.getAvailableSeekEndByte(false),
                        stream.getStartByte(), stream.getEndByte(),
                        stream.getContentDescription(), stream.getRequestedContentLength(),
                        stream.getContentDuration() );
            }
            catch (HNStreamingException hnse)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("ChannelRequestInterceptor: Unable to create channel stream: " + streamClassName, hnse);
                }
                request.setStatus(ActionStatus.HTTP_UNAVAILABLE);
            }
            catch (ClassNotFoundException cnfe)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("ChannelRequestInterceptor: Unable to create channel stream: " + streamClassName, cnfe);
                }
                request.setStatus(ActionStatus.HTTP_SERVER_ERROR);
            }
            catch (NoSuchMethodException nsme)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("ChannelRequestInterceptor: Unable to create channel stream: " + streamClassName, nsme);
                }
                request.setStatus(ActionStatus.HTTP_SERVER_ERROR);
            }
            catch (InvocationTargetException ite)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("ChannelRequestInterceptor: Unable to create channel stream: " + streamClassName, ite.getTargetException());
                }
                request.setStatus(ActionStatus.HTTP_UNAVAILABLE);
            }
            catch (InstantiationException ie)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("ChannelRequestInterceptor: Unable to create channel stream: " + streamClassName, ie);
                }
                request.setStatus(ActionStatus.HTTP_SERVER_ERROR);
            }
            catch (IllegalAccessException iae)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("ChannelRequestInterceptor: Unable to create channel stream: " + streamClassName, iae);
                }
                request.setStatus(ActionStatus.HTTP_UNAUTHORIZED);
            }
        }

        //stream may not have been created, still send response
        if ((stream == null) || (!request.isValidRequest()) || (request.isHeadRequest()))
        {
            // Either request was invalid or HEAD, no streaming, so we are done
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
                log.debug("ChannelRequestInterceptor: checking authorization for connection id: " + connectionId+ 
                        " for content item: " + channelContentItem);
            }
            InetAddress address = socket.getInetAddress();
            boolean authorized = serverSessionManager.authorize( address, requestURL, 
                                                                 connectionId, 
                                                                 channelContentItem, 
                                                                 request.getRequestStrings(),
                                                                 request.getNetworkInterface() );
            if (authorized)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ChannelRequestInterceptor: stream transmission authorized for request: " + request + 
                            " for content item: " + channelContentItem + ", transmitting");
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
                        log.warn("ChannelRequestInterceptor: Problems trying to set infinite socket timeout", se);
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
                        log.info("Unable to open stream: " + stream + " - sending UNAVAILABLE response and stopping stream");
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
                    log.info("ChannelRequestInterceptor: stream transmission not authorized for request: " + request);
                }
                request.setStatus(ActionStatus.HTTP_UNAUTHORIZED);
                request.sendHttpResponse();
                stream.stop(false);
            }
        }
        return true;
    }
}
