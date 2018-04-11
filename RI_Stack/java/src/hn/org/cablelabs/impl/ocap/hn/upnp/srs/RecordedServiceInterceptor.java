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
package org.cablelabs.impl.ocap.hn.upnp.srs;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.exception.HNStreamingRangeException;
import org.cablelabs.impl.media.streaming.session.ContentRequest;
import org.cablelabs.impl.media.streaming.session.HNServerSessionManager;
import org.cablelabs.impl.media.streaming.session.RecordingStream;
import org.cablelabs.impl.ocap.hn.transformation.NativeContentTransformation;
import org.cablelabs.impl.ocap.hn.transformation.OutputVideoContentFormatExt;
import org.cablelabs.impl.ocap.hn.transformation.TransformationManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer.HTTPRequest;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.cds.HTTPRequestInterceptor;
import org.cablelabs.impl.util.HNUtil;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.transformation.Transformation;
import org.ocap.hn.transformation.TransformationManager;
import org.ocap.shared.dvr.RecordingManager;

/**
 * An HTTP request interceptor supporting Recorded Services published in the CDS
 */
public class RecordedServiceInterceptor implements HTTPRequestInterceptor
{
    private static final Logger log = Logger.getLogger(RecordedServiceInterceptor.class);

    public boolean intercept( final Socket socket, final HTTPRequest httpRequest, final ContentEntry contentEntry, 
                              final URL requestURL, final URL effectiveURL) 
        throws HNStreamingException
    {
        final String path = effectiveURL.getPath();
        final String query = effectiveURL.getQuery();

        if (!path.startsWith(ContentDirectoryService.RECORDING_REQUEST_URI_PATH))
        { // Don't log here - this will happen all the time...
            return false;
        }
        
        // URL syntax check
        if (!query.startsWith(ContentDirectoryService.RECORDING_REQUEST_URI_ID_PREFIX))
        {
            if (log.isInfoEnabled())
            {
                log.info( "RecordedServiceInterceptor: Found request for recording with missing param: " 
                          + ContentDirectoryService.RECORDING_REQUEST_URI_ID_PREFIX );
            }
            return false;
        }
        String recordingIDStr = query.substring(ContentDirectoryService.RECORDING_REQUEST_URI_ID_PREFIX.length());
        recordingIDStr = recordingIDStr.substring(0, recordingIDStr.indexOf('&'));
        
        final int recordingID;
        try
        {
            recordingID = Integer.parseInt(recordingIDStr);
        }
        catch (NumberFormatException nfe)
        {
            if (log.isInfoEnabled())
            {
                log.info( "RecordedServiceInterceptor: Error parsing recording ID " + recordingIDStr, nfe); 
            }
            return false;
        }

        final RecordingContentItemLocal recordingContentItemLocal;
        final RecordingManager rm = RecordingManager.getInstance();
        
        try 
        {
            recordingContentItemLocal = (RecordingContentItemLocal)rm.getRecordingRequest(recordingID);
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info( "RecordedServiceInterceptor: Could not find RecordedService with ID " 
                          + recordingID + " for URI " + effectiveURL.toExternalForm(), e ); 
            }
            return false;
        }
        
        if (recordingContentItemLocal == null)
        {
            if (log.isInfoEnabled())
            {
                log.info( "RecordedServiceInterceptor: Could not find RecordedService with ID " 
                          + recordingID + " for URI " + effectiveURL.toExternalForm() ); 
            }
            return false;
        }
        
        if (contentEntry == null)
        {
            if (log.isInfoEnabled())
            {
                log.info( "RecordedServiceInterceptor: Content Entry is NULL"); 
            }
            return false;
        }

        if (log.isInfoEnabled())
        {
            log.info("RecordedServiceInterceptor: intercepting: " + httpRequest + ", content item: " + contentEntry);
        }
        ContentItem referencingContentItem = (ContentItem)contentEntry;

        // Look for a transformation query parameter (optional)
        NativeContentTransformation nativeTransform = HNUtil.getTransformationFromURIQuery(query);

        //acquire resources
        RecordingStream stream = null;
        ContentRequest request = new ContentRequest(socket, httpRequest, contentEntry, 
                                                    nativeTransform, effectiveURL.toExternalForm());
        if (request.isValidRequest())
        {
            try
            {
                stream = new RecordingStream( recordingContentItemLocal, referencingContentItem, 
                                              request );
            }
            catch (HNStreamingRangeException hnsre)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("RecordedServiceInterceptor: Unable to create recording stream", hnsre);
                }
                request.setStatus(ActionStatus.HTTP_RANGE_NOT_SATISFIABLE);
                request.sendHttpResponse();
                return true;
            }
            if (log.isInfoEnabled())
            {
                log.info("RecordedServiceInterceptor: stream created: " + stream);
            }
            try
            {
                request.setStreamingContext(stream.getContentLocationType(), stream.getFrameRateInTrickMode(),
                        stream.getFrameTypesInTrickMode(),
                        stream.getAvailableSeekStartTime(), stream.getAvailableSeekEndTime(),
                        stream.getAvailableSeekStartByte(true), stream.getAvailableSeekEndByte(true),
                        stream.getAvailableSeekStartByte(false), stream.getAvailableSeekEndByte(false),
                        stream.getStartByte(), stream.getEndByte(),
                        stream.getContentDescription(),
                        stream.getRequestedContentLength(),
                        stream.getContentDuration() );
            }
            catch (HNStreamingException hnse)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("RecordedServiceInterceptor: Unable to create recording stream", hnse);
                }
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
                log.debug("RecordedServiceInterceptor: checking authorization for connection id: " + connectionId+ 
                        " for content item: " + referencingContentItem);
            }
            InetAddress address = socket.getInetAddress();
            
                boolean authorized = serverSessionManager.authorize( address, requestURL, 
                                                                 connectionId, 
                                                                 referencingContentItem, 
                                                                 request.getRequestStrings(),
                                                                 request.getNetworkInterface() );
            if (authorized)
            {
                if (log.isInfoEnabled())
                {
                    log.info("RecordedServiceInterceptor: stream transmission authorized for request: " + request + 
                            " for content item: " + referencingContentItem + ", transmitting");
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
                        log.warn("RecordedServiceInterceptor: Problems trying to set infinite socket timeout", se);
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
                    log.info("RecordedServiceInterceptor: stream transmission not authorized for request: " + request);
                }
                request.setStatus(ActionStatus.HTTP_UNAUTHORIZED);
                request.sendHttpResponse();
                stream.stop(false);
            }
        }
        return true;
    }
}
