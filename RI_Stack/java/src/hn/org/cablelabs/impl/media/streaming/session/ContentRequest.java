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
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescription;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentLocationType;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.media.streaming.session.util.ContentRequestConstant;
import org.cablelabs.impl.media.streaming.session.util.ContentRequestUtil;
import org.cablelabs.impl.ocap.hn.NetworkInterfaceImpl;
import org.cablelabs.impl.ocap.hn.content.ContentEntryImpl;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.transformation.NativeContentTransformation;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer.HTTPRequest;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.MPEEnv;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.content.ContentEntry;

public class ContentRequest
{
    private static final Logger log = Logger.getLogger(ContentRequest.class);

    // Data Associated with request
    //
    private HTTPRequest httpRequest;

    private ContentEntry contentEntry;
    
    /** The transformation associated with the content request (may be null) */
    private final NativeContentTransformation transformation;    

    private String effectiveURI;

    // Protocol info which is associated with content item
    private HNStreamProtocolInfo protocolInfo = null;
    
    // Values set based on what headers included in request
    // Items not requested will be set to default values
    private Integer connectionId = new Integer(0);

    // Indicates if this request is valid, set to false if problems encountered
    private boolean isValidRequest = true;
    
    // Flags which indicate if header was included in request
    private boolean rangeHeaderIncluded = false;
    private boolean dtcpRangeHeaderIncluded = false;
    private boolean timeSeekRangeHeaderIncluded = false;    
    private boolean contentFeaturesIncluded = false;
    private boolean playspeedHeaderIncluded = false;
    private boolean frameRateInTrickModeIncluded = false;
    private boolean getAvailableSeekRangeIncluded = false;
    
    // Optional OCAP HN Header values, set to default value if not requested
    private int currentDecodePTS = -1;
    private int chunkedEncodingMode = ContentRequestConstant.HTTP_HEADER_CEM_NONE;
    private int maxGOPsPerChunk = -1;
    private int maxFramesPerGOP = -1;
    private boolean serverSidePacedStreaming = false;
    private int frameTypesInTrickModeRequested = ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_NONE;
    private int frameTypesInTrickMode = ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_NONE;
    private long maxTrickModeBandwidth = -1;
    private int frameRateInTrickMode = -1;
    private int contentLocationType = -1;
    private HNStreamContentDescription contentDescription = null;

    // Required values for playback streaming
    //default start times to zero
    private long timeSeekStartMillis = -1;
    private String timeSeekStartNPT = null;
    private long timeSeekEndMillis = -1;
    private String timeSeekEndNPT = null;
    private String timeSeekDurationNPT = null;
    private long startByte = -1;
    private long endByte = -1;


    //will be set to the available seek end bytes - available seek start bytes - contentEntry.getContentSize would return -1
    // content may still be changing (in progress or tsb) - use contentEntry.getContentSize ==  -1 to determine if size changing 
    private long contentLengthBytes = -1; // For "ContentLength" header
    private long totalContentLength = -1; // For "ContentRange" header, instance-length field
    private long  dtcpContentLength = -1; // For "ContentRange.dtcp.com" header, instance-length field
    private long[] byteRange = null;
    private long[] dtcpRange = null;

    // Optional available seek range header values
    private int availableSeekRangeMode = 0; 
    private String availableSeekStartNPT = null;
    private String availableSeekEndNPT = null;
    private long availableSeekStartByte = -1;
    private long availableSeekEndByte = -1;
    private long availableDtcpSeekStartByte = -1;
    private long availableDtcpSeekEndByte = -1;
    private Socket socket = null;
    
    //default to 1.0
    private float rate = 1.0F;
    private String rateStr = "1";
    
    // Flag which indicates the response code should be partial content
    private boolean isPartialContent = false;

    // Transfer mode requested, can be either Streaming or Background
    private String transferMode = ContentRequestConstant.STREAMING_VALUE;
    
    // Configuration parameters which affect responses
    private static String configServerSoftwareId = null;

    // Specified option for chunk encoding.
    private static String chunkEncoding = ContentRequestConstant.CHUNK_ENCODING_ALWAYS;


    // Enable RI server to be lenient with requests which violate DLNA requirements
    // ignoring invalid headers and responding with content to GET request which
    // include those headers
    // specifically, if protocolInfo.isNetworkRangeSupported() returns false and a request includes a range header,
    // if configIsLenient is true, the request will behave as if the range header was not included
    private static boolean configIsLenient = true;

    // Enable RI server to include the DLNA header which indicates the server
    // will decimate/augment the response stream when trick play speeds are requested
    private static boolean includeDLNAFrameRateHdr = true;

    private int m_statusCode;
    private String m_reasonPhrase;
    private final Map m_headers = new HashMap();

    /**
     * Constructor 
     * @param httpRequest the upnp action to use to build the http response
     * @param contentEntry the entry to use to build the http response
     * @param transformation TODO
     * @param uri The URI to use for processing the request (in place of the httpRequest URI)
     */
    public ContentRequest(Socket socket, HTTPRequest httpRequest, ContentEntry contentEntry, 
                          NativeContentTransformation transformation, String uri)
    {
        this.socket = socket;
        this.httpRequest = httpRequest;
        this.contentEntry = contentEntry;
        this.transformation = transformation;
        this.effectiveURI = uri;
        
        m_headers.put(ContentRequestConstant.CR_HEADER_SERVER, ContentRequestConstant.SERVER);
        m_headers.put(ContentRequestConstant.CR_HEADER_DATE, ContentRequestConstant.RFC1123_FORMAT.format(new Date())
                + ContentRequestConstant.TIMEZONE_GMT);
        
        if (log.isInfoEnabled())
        {
            log.info("constructing contentRequest for action: " + httpRequest + ", entry: " + contentEntry);
        }

        // Initialize the value for the server response header field
        assignConfigValues();
        
        // Initialize response with header values which will be returned 
        // regardless if this is a valid or invalid request       
        
        if ((httpRequest == null) || (contentEntry == null))
        {
            if (log.isWarnEnabled())
            {
                log.warn("ContentRequest() - invalid request, null entry or action");
            }
            isValidRequest = false;
            return;
        }

        // Continue to process this request until it is determined to be invalid
        // Determine protocol id which is applicable to this request
        assignProtocolInfo();
        if (!isValidRequest)
        {
            if (log.isWarnEnabled())
            {
                log.warn("ContentRequest() - invalid request, problem with protocol info");
            }
            return;
        }

        // Assign connection id
        assignConnectionId();
        if (!isValidRequest)
        {
            if (log.isWarnEnabled())
            {
                log.warn("ContentRequest() - invalid request, problem with connection id");
            }
            return;
        }

        // Process headers which affect transfer mode, size and duration first
        assignRequestedStreamingValues();
        if (!isValidRequest)
        {
            if (log.isWarnEnabled())
            {
                log.warn("ContentRequest() - invalid request, problem with requested streaming values");
            }
            return;
        }

        // Process optional headers next
        assignOptionalHeaderValues();
        if (!isValidRequest)
        {
            if (log.isWarnEnabled())
            {
                log.warn("ContentRequest() - invalid request, problem with optional headers");
            }
        }
    }

    /**
     * Failure-path ContentRequest, used for returning invalid responses
     * 
     * @param socket the socket to return the response
     */
    public ContentRequest(Socket socket, ActionStatus status)
    {
        this.socket = socket;
        this.transformation = null;
        setStatus(status);
    }

    /**
     * Sets the values necessary to begin streaming the content.
     * 
     * @param contentLocationType
     *            - Content Location type
     * @param frameRateInTrickMode
     *            - FrameRate in TrickMode for the content
     * @param frameTypesInTrickMode
     *            - FrameTypes in TrickMode for the content
     * @param availableSeekStartTime
     *            - Available Start time for the content
     * @param availableSeekEndTime
     *            - Available End time for the content
     * @param availableSeekStartByte
     *            - Encrypted Recording Start byte (DTCP Encrypted Position)
     * @param availableSeekEndByte
     *            - Encrypted Recording End byte (DTCP Encrypted Position)
     * @param availableDtcpSeekStartByte
     *            - ClearText Start Byte (Non-Encrypted Position)
     * @param availableDtcpSeekEndByte
     *            - ClearText End Byte (Non-Encrypted Position)
     * @param startByte
     *            - Requested start byte
     * @param endByte
     *            - Requested end byte
     * @param contentDescription
     *            - Requested content description
     * @param inContentLengthBytes
     *            - When Stream cannot calculate the length, a value < 1 should
     *            be sent in and this method will simply take the difference
     *            between the start and end bytes should it need calculation.
     *            Currently only RecordingStreams can have this value
     *            pre-determined. All other known implementations are currently
     *            sending -1.
     * @throws HNStreamingException
     */
    public void setStreamingContext(int contentLocationType, int frameRateInTrickMode,
                                    int frameTypesInTrickMode,
                                    Time availableSeekStartTime, Time availableSeekEndTime,
                                    long availableSeekStartByte, long availableSeekEndByte,
                                    long availableDtcpSeekStartByte, long availableDtcpSeekEndByte,
                                    long startByte, long endByte,
                                    HNStreamContentDescription contentDescription,
                                    long inContentLengthBytes,
                                    long inContentDuration )
    throws HNStreamingException
    {
        if (!isValidRequest)
        {
            if (log.isErrorEnabled())
            {
                log.error("setStreamingContext - not a valid request - ignoring");
            }
            return;
        }

        if (log.isInfoEnabled()) 
        {
            log.info("setStreamingContext - contentLocationType: " + contentLocationType +
                     ", frameRateInTrickMode: " + frameRateInTrickMode +
                     ", frameTypesInTrickMode: " + frameTypesInTrickMode +
                     ", availableSeekStartTime:" + availableSeekStartTime +
                     ", availableSeekEndTime: " + availableSeekEndTime +
                     ", availableSeekStartByte: " + availableSeekStartByte +
                     ", availableSeekEndByte: " + availableSeekEndByte +
                     ", availableDtcpSeekStartByte: " + availableDtcpSeekStartByte +
                     ", availableDtcpSeekEndByte: " + availableDtcpSeekEndByte +
                     ", startByte: " + startByte + ", endByte: " + endByte +
                     ", inContentLengthBytes: " + inContentLengthBytes +
                     ", inContentDuration: " + inContentDuration);
        }
        this.frameRateInTrickMode = frameRateInTrickMode;
        this.frameTypesInTrickMode = frameTypesInTrickMode;
        this.availableSeekStartByte = availableSeekStartByte;
        this.availableSeekEndByte = availableSeekEndByte;
        this.availableDtcpSeekStartByte = availableDtcpSeekStartByte;
        this.availableDtcpSeekEndByte = availableDtcpSeekEndByte;
        this.contentLocationType = contentLocationType;
        this.contentDescription = contentDescription;
        
        // If start byte is -1, leave content length at default value
        //handle positive and negative rates
        if (rate >= 0)
        {
            if (startByte == -1)
            {
                startByte = availableSeekStartByte;
            }
            if (endByte == -1 || endByte > availableSeekEndByte)
            {
                endByte = availableSeekEndByte;
            }
            if (startByte != -1)
            {
                if (inContentLengthBytes < 1)
                {
                    contentLengthBytes = endByte - startByte + 1;
                }
                else
                {
                    contentLengthBytes = inContentLengthBytes;
                }
            }
        }
        else
        {
            if (startByte == -1)
            {
                startByte = availableSeekEndByte;
            }
            if (endByte == -1 || endByte < availableSeekStartByte)
            {
                endByte = availableSeekStartByte;
            }
            if (startByte != -1)
            {
                if (inContentLengthBytes < 1)
                {
                    contentLengthBytes = startByte - endByte + 1;
                }
                else
                {
                    contentLengthBytes = inContentLengthBytes;
                }
            }
        }
        if (log.isInfoEnabled())
        {
            log.info("contentLengthBytes: " + contentLengthBytes);
        }

        totalContentLength = availableSeekEndByte - availableSeekStartByte + 1;
        if (log.isInfoEnabled()) 
        {
            log.info("totalContentLength: " + totalContentLength);
        }

        if (protocolInfo.isLinkProtected())
        {
            startByte = availableDtcpSeekStartByte;
            endByte = availableDtcpSeekEndByte;
            dtcpContentLength = availableDtcpSeekEndByte - availableDtcpSeekStartByte + 1;
        }
        if (log.isInfoEnabled())
        {
            log.info("dtcpContentLength: " + dtcpContentLength);
        }
        
        if (getAvailableSeekRangeIncluded)
        {
            // Set mode for available seek range response
            if ((contentLocationType == HNStreamContentLocationType.HN_CONTENT_LOCATION_LOCAL_TSB) ||
                (contentLocationType == HNStreamContentLocationType.HN_CONTENT_LOCATION_VIDEO_DEVICE))
            {
                // DLNA Requirement [7.4.16.2]: If a Content Source uses the "Limited Random Access Data
                // Availability" model under Mode=0, then the following must be true.
                // The s0 data boundary must map to a beginning that must change with time.
                availableSeekRangeMode = 0;
            }
            else
            {
                // DLNA Requirement [7.4.16.4]: If a Content Source uses the "Limited Random Access Data
                // Availability" model under Mode=1, then the following must be true.
                // The s0 data boundary must map to a fixed and non-changing beginning
                availableSeekRangeMode = 1;                
            }
        }
        
        // If seek start and/or end was provided, format into npt
        if (availableSeekStartTime != null)
        {
            availableSeekStartNPT = ContentRequestUtil.formatNPT(availableSeekStartTime.getNanoseconds()
                    / ContentRequestConstant.NANOS_PER_MILLI);
        }
        if (availableSeekEndTime != null)
        {
            availableSeekEndNPT = ContentRequestUtil.formatNPT(availableSeekEndTime.getNanoseconds()
                    / ContentRequestConstant.NANOS_PER_MILLI);
        }
        
        // Format start, end and duration into NPT format if provided
        if (timeSeekStartMillis != -1)
        {
            timeSeekStartNPT = ContentRequestUtil.formatNPT(timeSeekStartMillis);
        }
        if (timeSeekEndMillis != -1)
        {
            timeSeekEndNPT = ContentRequestUtil.formatNPT(timeSeekEndMillis);
        }
        
        //duration needs to be in the form: duration = hours ":" minutes ":" seconds
        if (inContentDuration > 0)
        {
            timeSeekDurationNPT = ContentRequestUtil.formatNPT(inContentDuration);
        }
        else
        { // Formulate the duration from the time-seek values
            timeSeekDurationNPT = ContentRequestUtil.formatNPT(timeSeekEndMillis - timeSeekStartMillis);
        }
            
        //startByte was not provided by a range header, provide it (calculated from timeseekrange header)
        if (timeSeekRangeHeaderIncluded && !rangeHeaderIncluded)
        {
            this.startByte = startByte;
            this.endByte = endByte;
            if (log.isDebugEnabled()) 
            {
                log.debug("timeseek range header included but range not included - startByte: " + startByte + " startNanos: " + getTimeSeekStartNanos() + ", endByte: " + endByte + ", endNanos: " + getTimeSeekEndNanos());
            }
        }        
        
        // Now have enough information to validate and assign streaming values, including some optional fields 
        validateStreamingValues();
    }
    
    public boolean isValidRequest()
    {
        return isValidRequest;
    }
    
    public void sendHttpResponse()
    {
        // Formulate the http response to this http request
        if(isValidRequest)
        {
            formulateResponse();
        }
        
        // Send the response directly on socket
        // NOTE: there currently is no support for chunked responses, 
        // should only be a problem if this is a really big response or using really small chunks
        try
        {
            OutputStream os = socket.getOutputStream();
            StringBuffer sb = new StringBuffer(256);
            sb.append(getStatusLine());
            sb.append(getHeaders());
            sb.append(ContentRequestConstant.CRLF);
            os.write(sb.toString().getBytes());
        }
        catch (IOException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("sendHttpResponse() - " + 
                        "error sending response for request: " + getHeaders(), e);
            }                
        }

        StringBuffer buffer = new StringBuffer();
        if (log.isInfoEnabled())
        {
            buffer.append("content response - status line: ");
            buffer.append(getStatusLine());
            buffer.append(" ");
            buffer.append(", headers: ").append(getHeaders());
            buffer.append(", port: ");
            buffer.append(socket.getPort());
        }

        if (log.isInfoEnabled()) 
        {
            log.info("response sent: " + buffer.toString());
        }
    }
    
    public String toString()
    {
        return "ContentRequest{" + "request=" + httpRequest + ", connectionId=" + connectionId + '}';
    }
    
    //
    // Action related get methods 
    //
    public Socket getSocket()
    {
        return socket;
    }
    
    public boolean isHeadRequest()
    {
        return httpRequest.isHead();
    }
    
    public InetAddress getRequestInetAddress()
    {
        return socket.getInetAddress();
    }
    
    public NetworkInterface getNetworkInterface()
    {
        final InetAddress localAddress = getSocket().getLocalAddress();

        // Create a NI who's getInetAddress() returns the socket the request was received on
        return new NetworkInterfaceImpl(localAddress);
    }

    /**
     * Return the request line plus the headers in a String array
     */
    public String [] getRequestStrings()
    {
        return httpRequest.getRequest();
    }
    
    /**
     * Return the effective URI to be used for the request (this may not be the one in the
     * HTTPRequest
     */
    public String getURI()
    {
        return effectiveURI;
    }
    
    public NativeContentTransformation getTransformation()
    {
        return transformation;
    }
    
    //
    // Streaming Parameter related get methods
    //
    public Integer getConnectionId()
    {
        return connectionId;
    }

    public HNStreamProtocolInfo getProtocolInfo()
    {
        return protocolInfo;
    }
    
    public boolean isRangeHeaderIncluded()
    {
        return rangeHeaderIncluded;
    }

    public boolean isDtcpRangeHeaderIncluded()
    {
        return dtcpRangeHeaderIncluded;
    }

    public boolean isTimeSeekRangeHeaderIncluded()
    {
        return timeSeekRangeHeaderIncluded;
    }
    
    public long getTimeSeekStartNanos()
    {
        return timeSeekStartMillis * ContentRequestConstant.NANOS_PER_MILLI;
    }

    /**
     * Return timeSeekEndNanos or -1 if not provided
     * 
     * @return nanos or -1
     */
    public long getTimeSeekEndNanos()
    {
        return (timeSeekEndMillis > -1 ? timeSeekEndMillis * ContentRequestConstant.NANOS_PER_MILLI : timeSeekEndMillis);
    }

    //not valid if timeseekrange header provided until after setStreamingContext has been called and validation is complete
    public long getStartBytePosition()
    {
        return startByte;
    }

    //not valid if timeseekrange header provided until after setStreamingContext has been called and validation is complete
    public long getEndBytePosition()
    {
        return endByte;
    }

    public float getRate()
    {
        return rate;
    }

    //
    // Optional header value get methods
    //
    public int getChunkedEncodingMode()
    {
        return chunkedEncodingMode;
    }
    
    public long getMaxTrickModeBandwidth()
    {
        return maxTrickModeBandwidth;
    }
    
    public int getCurrentDecodePTS()
    {
        return currentDecodePTS;
    }
    
    public int getMaxGOPsPerChunk()
    {
        return maxGOPsPerChunk;
    }
    
    public int getMaxFramesPerGOP()
    {
        return maxFramesPerGOP;
    }
    
    public boolean isUseServerSidePacing()
    {
        return serverSidePacedStreaming;
    }
    
    public int getRequestedFrameTypesInTrickMode()
    {
        return frameTypesInTrickModeRequested;
    }
    
    private void assignConfigValues()
    {
        // If the software id is null, never initialized config values so do it now
        if (configServerSoftwareId == null)
        {
            // Get the server software id string to include as the value for the SERVER header in response
            configServerSoftwareId = MPEEnv.getEnv(ContentRequestConstant.SERVER_SOFTWARE_ID_PROP,
                    ContentRequestConstant.SERVER_SOFTWARE_ID_DEFAULT);
            if (log.isInfoEnabled())
            {
                log.info("assignConfigValues() - server software id set to: " + configServerSoftwareId);
            }
            
            // Get the chunk encoding preference.
            String prop = MPEEnv.getEnv(ContentRequestConstant.CHUNK_ENCODING_PROP);
            chunkEncoding = ContentRequestConstant.CHUNK_ENCODING_NOT_RECOGNIZED;
            if ((prop != null) && (prop.trim().equalsIgnoreCase(ContentRequestConstant.CHUNK_ENCODING_ALWAYS)))
            {
                chunkEncoding = ContentRequestConstant.CHUNK_ENCODING_ALWAYS;
            }
            else if ((prop.trim().equalsIgnoreCase(ContentRequestConstant.CHUNK_ENCODING_AS_APPROPRIATE)))
            {
                chunkEncoding = ContentRequestConstant.CHUNK_ENCODING_AS_APPROPRIATE;
            }
            else if ((prop.trim().equalsIgnoreCase(ContentRequestConstant.CHUNK_ENCODING_NEVER)))
            {
                chunkEncoding = ContentRequestConstant.CHUNK_ENCODING_NEVER;
            }
            if (log.isInfoEnabled())
            {
                log.info("assignConfigValues() - chunkEncoding set to: " + chunkEncoding);
            }
            
            // Enable GET requests with non-DLNA compliant headers to NOT be ignored 
            prop = MPEEnv.getEnv(ContentRequestConstant.IS_LENIENT_PROP);
            if ((prop != null) && (prop.trim().equalsIgnoreCase("false")))
            {
                configIsLenient = false;
            }
            if (log.isDebugEnabled())
            {
                log.debug("assignConfigValues() - isLenient set to: " + configIsLenient);
            }            
            
            // Disable DLNA frame rate header in HTTP responses
            prop = MPEEnv.getEnv(ContentRequestConstant.INCLUDE_DLNA_FRAME_RATE_IN_TRICK_MODE_PROP);
            if ((prop != null) && (prop.trim().equalsIgnoreCase("false")))
            {
                includeDLNAFrameRateHdr = false;
            }
            if (log.isDebugEnabled())
            {
                log.debug("assignConfigValues() - includeDLNAFrameRateHdr set to: " + 
                        includeDLNAFrameRateHdr);
            }            
        }   
    }

    private void assignProtocolInfo()
    {
        // Need to determine profile id of this content item that was requested
        String uriProfile = (String) ContentRequestUtil.getQueryParameters(getURI()).get("profile");
        if (uriProfile != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("assignProtocolInfo() - profile id included in request URL: " + uriProfile);
            }            
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("assignProtocolInfo() - no profile id included in request URL: " + effectiveURI);
            }            
        }

        // Get all supported protocols for this content item
        HNStreamProtocolInfo[] protocolInfos = ((ContentEntryImpl)contentEntry).getProtocolInfo();
        if ((protocolInfos == null) || (protocolInfos.length <= 0))
        {
            if (log.isErrorEnabled())
            {
                log.error("assignProtocolInfo() - no associated protocol info for content entry");
            }
            setStatus(ActionStatus.HTTP_SERVER_ERROR);
            return;
        }
        
        // Find protocol info which matches uri profile id if not null
        if (uriProfile != null)
        {
            for (int i = 0; i < protocolInfos.length; i++)
            {
                if (protocolInfos[i].getProfileId().equals(uriProfile))
                {
                    protocolInfo = updateHost(protocolInfos[i]);
                    break;
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("assignProtocolInfo() - protocol info " + i + " - " + protocolInfos[i].getProfileId() +
                                " does not match " + uriProfile);
                    }                                
                }
            }            
        }
        
        // If no matching protocol was found, use first one  
        if (protocolInfo == null)
        {
            // Assign to first protocol info in list
            protocolInfo = updateHost(protocolInfos[0]);
            if (log.isWarnEnabled())
            {
                log.warn("assignProtocolInfo() - No matching protocol found for URL: " + effectiveURI +
                         ", using protocol info: " + protocolInfo.getAsString());
            }
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("assignProtocolInfo() - using protocol info: " + protocolInfo.getAsString());
        }
    }

    public String getHeaderValue(final String name)
    {
        final String result = httpRequest.getHeader(name);
        if (result == null || result.trim().equals(""))
        {
            return null;
        }
        return result;
    }

    /**
     * Determines if request included scid header with connection value and set
     * connection id to this value.  If request did not include scid header, 
     * get next available connection id to use from CMS.
     */
    private void assignConnectionId()
    {
        // Assign connection based on id supplied in request or retrieved from CMS
        // populate any headers if provided
        String value = getHeaderValue(ContentRequestConstant.SCID_DLNA_ORG);

        // connection id is optional
        if ((value != null) && !value.trim().equals(""))
        {
            try
            {
                connectionId = new Integer(value);
            }
            catch (NumberFormatException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("assignConnectionId() - invalid connection id provided: " + value);
                }
                setStatus(ActionStatus.HTTP_BAD_REQUEST);
                return;
            }
        }

        // If no connection id was specified, get the next available connection
        // id in order to manage
        // this session even though client doesn't care if this isn't HEAD
        // DLNA says no scid in HEAD response
        if ((0 == connectionId.intValue()) && !isHeadRequest())
        {
            connectionId = new Integer(MediaServer.getInstance().getCMS().getNextConnectionID());
            if (log.isInfoEnabled())
            {
                log.info("connection id was not provided - using local connection id: " + this);
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("connection id was provided: " + this);
            }
        }
    }
    
    /**
     * Parses optional header fields in request and assigns related streaming values 
     * to be passed to platform.
     */
    private void assignOptionalHeaderValues()
    {
        // Assign maxTrickBandwidth value if included in request
        try
        {
            Long maxTrickModeBandwidth = ContentRequestUtil.parseMaxTrickModeBandwidth(getHeaderValue(ContentRequestConstant.MAX_TRICK_MODE_BANDWIDTH));
            if (maxTrickModeBandwidth != null)
            {
                this.maxTrickModeBandwidth = maxTrickModeBandwidth.longValue();
            }
        }
        catch (HNStreamingException e)
        {
            // Based on OCAP HNP Spec 5.6.1 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of max GOP OCAP header: " +
                         getHeaderValue(ContentRequestConstant.MAX_GOPS_PER_CHUNK));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }

        // Assign decode PTS value if included in request
        try
        {
            Integer currentDecodePTS = ContentRequestUtil.parseCurrentDecodePTS(getHeaderValue(ContentRequestConstant.CURRENT_DECODE_PTS));
            if (currentDecodePTS != null)
            {
                this.currentDecodePTS = currentDecodePTS.intValue();
            }
        }
        catch (HNStreamingException e)
        {
            // Based on OCAP HNP Spec 5.6.1 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of max GOP OCAP header: " +
                        getHeaderValue(ContentRequestConstant.MAX_GOPS_PER_CHUNK));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }

        // Determine if max gops was included in request
        try
        {
            Integer maxGop = ContentRequestUtil.parseMaxGOPsPerChunk(getHeaderValue(ContentRequestConstant.MAX_GOPS_PER_CHUNK));
            if (maxGop != null)
            {
                maxGOPsPerChunk = maxGop.intValue();
            }            
        }
        catch (HNStreamingException e)
        {
            // Based on OCAP HNP Spec 5.6.1 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of max GOP OCAP header: " +
                        getHeaderValue(ContentRequestConstant.MAX_GOPS_PER_CHUNK));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }

        // Assign max frames per GOP if included in request
        try
        {
            Integer maxFramesPerGOP = ContentRequestUtil.parseMaxFramesPerGOP(getHeaderValue(ContentRequestConstant.MAX_FRAMES_PER_GOP));
            if (maxFramesPerGOP != null)
            {
                this.maxFramesPerGOP = maxFramesPerGOP.intValue();
            }
        }
        catch (HNStreamingException e)
        {
            // Based on OCAP HNP Spec 5.6.1 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of max GOP OCAP header: " +
                        getHeaderValue(ContentRequestConstant.MAX_GOPS_PER_CHUNK));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }

        // Set server side pacing flag based on value in request
        try
        {
            Boolean serverSidePacing = ContentRequestUtil.parseServerSidePacing(getHeaderValue(ContentRequestConstant.SERVERSIDE_PACED_STREAMING));
            if (serverSidePacing != null)
            {
                serverSidePacedStreaming = serverSidePacing.booleanValue();
            }
        }
        catch (HNStreamingException e)
        {
            // Based on OCAP HNP Spec 5.6.1 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of max GOP OCAP header: " +
                        getHeaderValue(ContentRequestConstant.MAX_GOPS_PER_CHUNK));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }

        // Handle trick mode types
        try
        {
            Integer requestedFrameTypes = ContentRequestUtil.parseFrameTypesInTrickMode(getHeaderValue(ContentRequestConstant.FRAME_TYPES_IN_TRICK_MODE));
            if (requestedFrameTypes != null)
            {
                frameTypesInTrickModeRequested = requestedFrameTypes.intValue();
                
            }            
        }
        catch (HNStreamingException e)
        {
            // Based on OCAP HNP Spec 5.6.1 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of frame types in trick mode OCAP header: " +
                        getHeaderValue(ContentRequestConstant.FRAME_TYPES_IN_TRICK_MODE));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }

        // Set flag if frame rate in trick mode was requested
        frameRateInTrickModeIncluded = (getHeaderValue(ContentRequestConstant.FRAME_RATE_IN_TRICK_MODE) != null);  

        // Determine if request included getContentFeatures 
        try
        {
            if (ContentRequestUtil.parseContentFeaturesDlnaOrg(getHeaderValue(ContentRequestConstant.GET_CONTENT_FEATURES_DLNA_ORG)))
            {
                contentFeaturesIncluded = true;
            }            
        }
        catch (HNStreamingException e)
        {
            // Based on DLNA 7.4.26.5 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of getContentFeatures header: " +
                        getHeaderValue(ContentRequestConstant.GET_CONTENT_FEATURES_DLNA_ORG));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }

        // Verify this protocol type supports available seek range prior to setting flag.
        // It is only supported under the following conditions:
        //
        // DLNA Requirement [7.4.36.4]: If an HTTP Server Endpoint supports either the Range HTTP
        // header or the TimeSeekRange.dlna.org HTTP header with the 
        // "Limited Random Access Data Availability" model for a content binary, 
        // the HTTP Server Endpoint MUST support the getAvailableSeekRange.dlna.org HTTP header field.
        //
        // Set flag if getAvailableSeekRange header included
        try
        {
            if (protocolInfo.isAvailableSeekSupported())
            {
                if (ContentRequestUtil.parseAvailableSeekRangeDlnaOrg(getHeaderValue(ContentRequestConstant.GET_AVAILABLE_SEEK_RANGE_DLNA_ORG)))
                {
                    getAvailableSeekRangeIncluded = true;
                }
            }
        }
        catch (HNStreamingException e)
        {
            // Based on DLNA 7.4.36.6 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of getAvailableTimeSeek header: " +
                        getHeaderValue(ContentRequestConstant.GET_AVAILABLE_SEEK_RANGE_DLNA_ORG));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
        }
    }
    
    /**
     * Determine parameter values which should be included in response and/or sent down to the 
     * platform to satisfy this content request.
     * 
     * Not rejecting request in cases where protocol info params do not indicate for specific headers
     * such as when only 1x playspeed is supported and a playspeed header is included.
     * 
     *  This is based on DLNA Reqmt 7.4.26.7 - comment:
     *      These guidelines do not define interoperability guidelines for a scenario where an
     *      HTTP Client Endpoint attempts to use an optional transport layer feature when the 4th
     *      field does not indicate support for the transport layer feature.
     */
    private void assignRequestedStreamingValues()
    {
        // Determine validity of playspeed for this request
        //
        // Not rejecting request when only 1x playspeed is supported and a playspeed header is included.
        //
        //  This is based on DLNA Reqmt 7.4.26.7 - comment:
        //      These guidelines do not define interoperability guidelines for a scenario where an
        //     HTTP Client Endpoint attempts to use an optional transport layer feature when the 4th
        //     field does not indicate support for the transport layer feature.
        try
        {
            Float playSpeed = ContentRequestUtil.parsePlaySpeedDlnaOrg(getHeaderValue(ContentRequestConstant.PLAYSPEED_DLNA_ORG));
            if (playSpeed != null)
            {
                rate = playSpeed.floatValue();
                rateStr = getHeaderValue(ContentRequestConstant.PLAYSPEED_DLNA_ORG);

                // Verify this rate is supported
                if (!protocolInfo.isValidPlaySpeed(rate))
                {
                    // Based on DLNA 7.4.70.5 Return error response with error code 406
                    if (log.isWarnEnabled())
                    {
                        log.warn("assignRequestedStreamingValues() - invalid playspeed of " +
                                rateStr + " requested, protocol info is: " + protocolInfo.getAsString());
                    }
                    setStatus(ActionStatus.HTTP_NOT_ACCEPTABLE);
                    return;
                }
                playspeedHeaderIncluded = true;
            }
        }
        catch (HNStreamingException e)
        {
            // Based on DLNA 7.4.70.3 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of playspeed header: " +
                        getHeaderValue(ContentRequestConstant.PLAYSPEED_DLNA_ORG));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }

        // Determine validity of time seek range for this request
        //
        // Rejecting GET request when protocol info op_param val a indicates timeSeekRange is not supported 
        // and a timeSeekRange header is included.
        //
        // This is based on DLNA Reqmt 7.3.33.2:
        //      HTTP Server Endpoints use the 406 (Not Acceptable) status code to indicate
        //      that an HTTP request can never be satisfied with the specified HTTP headers.        
        try
        {
            Integer[] timeSeekRange = ContentRequestUtil.parseTimeSeekRangeDlnaOrg(getHeaderValue(ContentRequestConstant.TIME_SEEK_RANGE_DLNA_ORG));
            if (timeSeekRange != null)
            {
                // Make sure protocol info indicates support for time seek header
                // Note - protocol info will not be null since it was set in assignProtocolInfo()
                if (!protocolInfo.isTimeSeekSupported())
                {
                    // If this is a GET request, reject it
                    if (!httpRequest.isHead())
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("assignRequestedStreamingValues() - time seek range header not supported for this profile: " +
                                    getHeaderValue(ContentRequestConstant.TIME_SEEK_RANGE_DLNA_ORG));
                        }
                        setStatus(ActionStatus.HTTP_NOT_ACCEPTABLE);
                        return;    
                    }
                    // *TODO* - reverted MPEOS change, need to support this for RI player until change is put back in
                    // This is a head request, don't return error but don't include time seek header
                    else
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("assignRequestedStreamingValues() - head request - " + 
                                    "time seek range header not supported for this profile: " +
                                    getHeaderValue(ContentRequestConstant.TIME_SEEK_RANGE_DLNA_ORG));
                        }                        
                    }
                }
                else
                {
                    // Range header always takes precedence over TimeSeekRange.dlna.org, as indicated in DLNA 7.4.71
                    timeSeekStartMillis = timeSeekRange[0].intValue();
                    timeSeekEndMillis = timeSeekRange[1].intValue();
                    if (!isValidTimeSeekRange())
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("assignRequestedStreamingValues() - invalid time seek range " + timeSeekStartMillis + ", " + timeSeekEndMillis);
                        }
                        setStatus(ActionStatus.HTTP_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    // Set flag to indicate time seek range header was included
                    timeSeekRangeHeaderIncluded = true;

                    // Don't set partial content flag according to DLNA 7.40.4.7                    
                }
            }            
        }
        catch (HNStreamingException e)
        {
            // Based on DLNA 7.4.40.9 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of time seek range header: " +
                        getHeaderValue(ContentRequestConstant.TIME_SEEK_RANGE_DLNA_ORG));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }
        
        // Determine validity of byte range for this request
        //
        // Rejecting request when protocol info op_param val b indicates Range is not supported 
        // and a Range header is included.
        //
        // This is based on DLNA Reqmt 7.3.33.2:
        //      HTTP Server Endpoints use the 406 (Not Acceptable) status code to indicate
        //      that an HTTP request can never be satisfied with the specified HTTP headers.        
        try
        {
            byteRange = ContentRequestUtil.parseRange(getHeaderValue(ContentRequestConstant.RANGE));
            
            // Verify this range is supported
            if (byteRange != null)        
            {
                // Make sure protocol info indicates support for range header
                // Note - protocol info will not be null since it was set in assignProtocolInfo()
                if (!protocolInfo.isNetworkRangeSupported())
                {
                    // Check to see if server is not configured to be lenient and if this should not be ignored
                    if (!configIsLenient)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("assignRequestedStreamingValues() - network range not supported - range header requeseted: " +
                                    getHeaderValue(ContentRequestConstant.RANGE));
                        }
                        setStatus(ActionStatus.HTTP_NOT_ACCEPTABLE);
                        return;
                    }
                    else
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("assignRequestedStreamingValues() - network range not supported - server is configured to be lenient, " +
                                    "ignoring invalid DLNA Range header: " + getHeaderValue(ContentRequestConstant.RANGE));
                        }
                    }
                }
                else
                {
                
                    if (!isValidByteRange(byteRange))
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("assignRequestedStreamingValues() - invalid byte range " +
                                    Arrays.toString(byteRange));
                        }
                        // Based on DLNA 7.4.38.6 Return error response 400 Bad request
                        setStatus(ActionStatus.HTTP_BAD_REQUEST);
                        return;
                    }
                    rangeHeaderIncluded = true;
                    startByte = byteRange[0];
                    endByte = byteRange[1];
                    if (log.isDebugEnabled()) 
                    {
                        log.debug("range header provided - start byte updated to: " + startByte + ", end byte updated to: " + endByte);
                    }
    
                    // Set flag so return code is 206 - partial content
                    isPartialContent = true;
                }
            }            
        }
        catch (HNStreamingException e)
        {
            // Based on DLNA 7.4.38.6 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of range header: " +
                        getHeaderValue(ContentRequestConstant.RANGE));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }

        // Determine validity of dtcp byte range for this request
        //
        // Rejecting request when protocol info op_param val b indicates Range is not supported 
        // and a Range header is included.
        //
        // This is based on DLNA Reqmt 7.3.33.2:
        //      HTTP Server Endpoints use the 406 (Not Acceptable) status code to indicate
        //      that an HTTP request can never be satisfied with the specified HTTP headers.        
        try
        {
            dtcpRange = ContentRequestUtil.parseRange(getHeaderValue(ContentRequestConstant.RANGE_DTCP_COM));
            if (dtcpRange != null)        
            {
                // Make sure protocol info indicates support for range header
                // Note - protocol info will not be null since it was set in assignProtocolInfo()
                if (!protocolInfo.isCleartextRangeSupported())
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("assignRequestedStreamingValues() - dtcp range header not supported for this profile: " +
                                getHeaderValue(ContentRequestConstant.RANGE_DTCP_COM));
                    }
                    setStatus(ActionStatus.HTTP_NOT_ACCEPTABLE);
                    return;                    
                }

                if (rangeHeaderIncluded)
                {
                    // DLNA LinkProtection 7.4.13.9
                    if (log.isWarnEnabled())
                    {
                        log.warn("Both " + ContentRequestConstant.RANGE + " and " + ContentRequestConstant.RANGE_DTCP_COM + " are set");
                    }
                    setStatus(ActionStatus.HTTP_NOT_ACCEPTABLE);
                    return;                    
                }
                else
                {
                    if (!isValidByteRange(dtcpRange))
                    {
                        throw new HNStreamingException("assignRequestedStreamingValues() " +
                            "- invalid dtcp range " + Arrays.toString(dtcpRange));
                    }

                    startByte = dtcpRange[0];
                    endByte = ContentRequestUtil.getAlignedEndByte(startByte,dtcpRange[1]);

                    if (log.isInfoEnabled())
                    {
                        log.info(ContentRequestConstant.RANGE_DTCP_COM + " header provided");
                    }
                    
                    dtcpRangeHeaderIncluded = true;

                    // Per DLNA Link Protection 7.4.13.11, respond with HTTP 200 OK,
                    // not HTTP 206 Partial Content. Do _not_ set isPartialContent flag.
                }
            }            
        }
        catch (HNStreamingException e)
        {
            // Based on DLNA 7.4.38.6 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of range header: " +
                        getHeaderValue(ContentRequestConstant.RANGE_DTCP_COM));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }

        // According to DLNA 7.4.71.2 - Range header field must take the highest precedence and the server
        // must ignore the other time seek and play speed fields.
        if (rangeHeaderIncluded)
        {
            playspeedHeaderIncluded = false;
            rate = 1.0F;

            timeSeekRangeHeaderIncluded = false;                      
            timeSeekStartMillis = -1;
            timeSeekEndMillis = -1;
        }

        try
        {
            // Assign chunk encoding mode to use for this request
            Integer chunkedEncodingMode = ContentRequestUtil.parseChunkedEncodingMode(getHeaderValue(ContentRequestConstant.CHUNK_ENCODING_MODE));
            if (chunkedEncodingMode != null)
            {
                this.chunkedEncodingMode = chunkedEncodingMode.intValue();
            }            
        }
        catch (HNStreamingException e)
        {
            // Based on OCAP HNP Spec 5.6.1 Return error response 400 Bad request
            if (log.isWarnEnabled())
            {
                log.warn("assignRequestedStreamingValues() - invalid syntax of chunk encoding OCAP header: " +
                        getHeaderValue(ContentRequestConstant.CHUNK_ENCODING_MODE));
            }
            setStatus(ActionStatus.HTTP_BAD_REQUEST);
            return;
        }
        
        // Validate the requested transferMode, use default streaming unless requested otherwise
        transferMode = ContentRequestConstant.STREAMING_VALUE;
        String requestedTransferMode = getHeaderValue(ContentRequestConstant.TRANSFER_MODE_DLNA_ORG);
        if ((requestedTransferMode != null) && (!requestedTransferMode.equals(ContentRequestConstant.STREAMING_VALUE)))
        {
            // If connection stalling is supported, background mode streaming may also be supported
            if ((requestedTransferMode.equals(ContentRequestConstant.BACKGROUND_TRANSFER_VALUE))
                    && (protocolInfo.isBackgroundTransferSupported()))
            {
                // Currently there is no logic which differentiates a streaming transfer
                // from a background transfer so just need to update the response header
                // to reflect what was requested
                transferMode = ContentRequestConstant.BACKGROUND_TRANSFER_VALUE;                
            }
            else
            {
                // Based on DLNA 7.4.49.4 Return error response 406 Not Acceptable.
                if (log.isWarnEnabled())
                {
                    log.warn("assignRequestedStreamingValues() - unsupported option in transferMode header: " + 
                            requestedTransferMode);
                }
                setStatus(ActionStatus.HTTP_NOT_ACCEPTABLE);
                return;                
            }
        }

        // Determine if chunked encoding is appropriate for this request.  
        // Send the response in chunked mode when the RI cannot calculate 
        // the content length ahead of time.
        chunkedEncodingMode = ContentRequestConstant.HTTP_HEADER_CEM_NONE;
        
        //Based on DLNA 7.5.4.3.2.25.2 && 7.5.4.3.2.33.4 HTTP Server Endpoints 
        // shall not use Chunked Transfer Coding in response to HTTP/1.0 GET 
        // requests and the HTTP Server Endpoint shall respond with 
        // error code 406 (Not Acceptable).
        if (isClientHttp1_0() && !configIsLenient && protocolInfo.useChunkEncoding()) {
            setStatus(ActionStatus.HTTP_NOT_ACCEPTABLE);
            return;
        }
        if (chunkEncoding.equals(ContentRequestConstant.CHUNK_ENCODING_AS_APPROPRIATE))
        {
            int rateValue = new Float(rate).intValue();
            if ((protocolInfo.useChunkEncoding()) || (playspeedHeaderIncluded && rateValue != 1))
            {
                chunkedEncodingMode = ContentRequestConstant.HTTP_HEADER_CEM_OTHER;
            }
            else
            {
                chunkedEncodingMode = ContentRequestConstant.HTTP_HEADER_CEM_NONE;
            }
        }
        else if (chunkEncoding.equals(ContentRequestConstant.CHUNK_ENCODING_ALWAYS))
        {
            chunkedEncodingMode = ContentRequestConstant.HTTP_HEADER_CEM_OTHER;
        }
        else if (chunkEncoding.equals(ContentRequestConstant.CHUNK_ENCODING_NEVER))
        {
            chunkedEncodingMode = ContentRequestConstant.HTTP_HEADER_CEM_NONE;
        }
    }
    
    private void validateStreamingValues()
    {
        if (timeSeekRangeHeaderIncluded)
        {
            // Verify requested range is valid and set byte range values to include in response
            //startByte, endByte were provided by setStreamingContext - validate them
            if (!isValidContentTimeSeekRange(startByte, endByte))
            {
                // Based on DLNA 7.4.40.8 Return error response 416 - Requested Range Not Satisfiable
                setStatus(ActionStatus.HTTP_RANGE_NOT_SATISFIABLE);
                return;
            }
        }

        if (rangeHeaderIncluded)
        {
            // Verify requested range is valid and sent total byte range
            if (!isValidContentByteRange(byteRange, true))
            {
                // Based on DLNA 7.4.38.6 Return error response 400 - Bad Request
                setStatus(ActionStatus.HTTP_BAD_REQUEST);
            }
        }

        if (dtcpRangeHeaderIncluded)
        {
            // Verify requested range is valid and sent total byte range
            if (!isValidContentByteRange(dtcpRange, false))
            {
                // Based on DLNA LinkProtection 7.4.13.7 Return error response 416
                setStatus(ActionStatus.HTTP_RANGE_NOT_SATISFIABLE);
            }
        }
    }

    private HNStreamProtocolInfo updateHost(HNStreamProtocolInfo protocolInfoToUpdate)
    {
        return new HNStreamProtocolInfo(MediaServer.substitute(protocolInfoToUpdate.getAsString(), MediaServer.HOST_PLACEHOLDER, socket.getLocalAddress().getHostAddress()));
    }
    
    /**
     * Create HTTP response to be sent for this request. 
     */
    private void formulateResponse()
    {
        // No response to create if prereqs are not satisfied
        if(protocolInfo == null || connectionId == null)
        {
            return;
        }
        
         // Include HTTP status header
        if(m_statusCode == 0)
        {
            if (isPartialContent)
            {
                setStatus(ActionStatus.HTTP_PARTIAL_CONTENT);
            }
            else
            {
                setStatus(ActionStatus.HTTP_OK);
            }
        }
        
        // Include content type header (host placeholder already replaced with correct address)
        if(protocolInfo != null)
        {
            addHeader(ContentRequestConstant.CR_HEADER_CONTENT_TYPE, protocolInfo.getMimeType());
        }

        // Include connection id if not zero
        if (connectionId.intValue() > 0)
        {
            addHeader(ContentRequestConstant.SCID_DLNA_ORG, connectionId.toString());
        }

        // Include the DLNA transfer mode header field, value is always streaming since other options,
        // background or interactive, are not supported
        addHeader(ContentRequestConstant.TRANSFER_MODE_DLNA_ORG, transferMode);
        
        // DLNA Requirement [7.4.51.4]: If an HTTP Server Endpoint transfers an Audio or AV content
        // binaries that permits variable play speed and time-based seek operations for
        // cacheable content transported in an HTTP/1.1 GET response, then the HTTP Server
        // Endpoint must include a "Vary" HTTP header.
        // The "Vary" header must list either or both of the following two arguments to inform
        // caches of the corresponding supported operations:
        // - TimeSeekRange.dlna.org
        // - PlaySpeed.dlna.org
        // the Vary header has to be included whenever the server responds to a
        // client request for the CDS object. The Vary header has to be included even if the
        // request does not include Time Seek or Play Speed headers.
        //if time seek or play speed are supported, add the vary header
        boolean varyTimeSeekRange = protocolInfo.isTimeSeekSupported();
        boolean varyPlaySpeed = (protocolInfo.getPlayspeeds() != null);
        
        if (varyPlaySpeed || varyTimeSeekRange)
        {
            StringBuffer newHeader = new StringBuffer();
            if (varyTimeSeekRange)
            {
                newHeader.append(ContentRequestConstant.TIME_SEEK_RANGE_DLNA_ORG);
                if (varyPlaySpeed)
                {
                    newHeader.append(", ");
                    newHeader.append(ContentRequestConstant.PLAYSPEED_DLNA_ORG);
                }
            }
            else
            {
                if (varyPlaySpeed)
                {
                    newHeader.append(ContentRequestConstant.PLAYSPEED_DLNA_ORG);
                }
            }
            
            addHeader(ContentRequestConstant.VARY_NAME, newHeader.toString());
        }

        if (protocolInfo.isLinkProtected())
        {
            // DLNA LinkProtection [7.4.9] and [7.4.10]
            addHeader(ContentRequestConstant.PRAGMA, ContentRequestConstant.NO_CACHE);
            if (isClientHttp1_0() == false)
            {
                addHeader(ContentRequestConstant.CACHE_CONTROL, ContentRequestConstant.NO_CACHE);
            }
        }

        // *TODO* - video device case here?
        // NOTE: Not including CACHE-CONTROL nor PRAGMA headers since default caching rules
        // are sufficient.  Also a transfer is required to be cacheable when TimeSeekRange
        // or Playspeed headers are included in request, see DLNA Requirement [7.4.51.3]:
        // If an HTTP Server Endpoint transfers Audio or AV content binaries
        // using HTTP/1.1 GET responses that include one or both of these HTTP headers, then
        // such transfers should be marked as cacheable.

        // Include encoding header field to indicate chunk encoding
        if (chunkedEncodingMode != ContentRequestConstant.HTTP_HEADER_CEM_NONE)
        {
            if (log.isDebugEnabled())
            {
                log.debug("formulateResponse() - setting transfer encoding mode to chunked");
            }
            addHeader(ContentRequestConstant.CR_HEADER_TRANSFER_ENCODING, ContentRequestConstant.CHUNKED_VALUE);
        }
        else // Include content length if not chunked encoding
        {
            if (log.isDebugEnabled())
            {
                log.debug("formulateResponse() - including content length since cem is: " + chunkedEncodingMode);
            }
            addHeader(ContentRequestConstant.CR_HEADER_CONTENT_LENGTH, String.valueOf(contentLengthBytes));
        }
        
        // Include content features if requested
        if (contentFeaturesIncluded)
        {
            //DLNA Requirement [7.4.26.6]: The value of the contentFeatures.dlna.org HTTP header must be
            //the same value as the fourth field of the content's res@protocolInfo value, as
            //described in the 7.3.30 MM protocolinfo values: 4th Field guideline.
            addHeader(ContentRequestConstant.CONTENT_FEATURES_DLNA_ORG, protocolInfo.getFourthField(false));
        }

        // Include available time seek if requested
        if (getAvailableSeekRangeIncluded)
        {
            // Include AvailableSeekRange header in response formatting as specified in DLNA 7.4.36.8:
            // 
            // availableSeekRange.dlna.org: 1 npt=00:05.30.12-00:10:34 bytes=214748364-224077003
            //
            StringBuffer sb = new StringBuffer(32);
            boolean timeSeekRange = protocolInfo.isTimeSeekSupported();
            if(timeSeekRange)
            {
                sb.append(availableSeekRangeMode);
                sb.append(" ");
                sb.append("npt=");
                sb.append(availableSeekStartNPT);
                sb.append("-");
                sb.append(availableSeekEndNPT);                
            }
            boolean byteSeekRange = protocolInfo.isNetworkRangeSupported();
            // This needs to be conditional based on whether opParam b-val is set (if byte seek is supported)
            if(byteSeekRange)
            {
                sb.append(" bytes=");
                sb.append(availableSeekStartByte);
                sb.append("-");
                sb.append(availableSeekEndByte);            
                
                if (protocolInfo.isLinkProtected())
                {
                    // TODO OCORI-4485 This violates the syntax of DLNA 7.4.36.8
                    // TODO but is expected by some of the LPTT tests.
                    sb.append(" cleartextbytes=");
                    sb.append(availableDtcpSeekStartByte);
                    sb.append("-");
                    sb.append(availableDtcpSeekEndByte);
                }
            }   
            addHeader(ContentRequestConstant.AVAILABLE_SEEK_RANGE_DLNA_ORG, sb.toString());                
        }

        // Add time seek range if value was requested
        if (timeSeekRangeHeaderIncluded)
        {
            // Include TimeSeekRange header in response formatting as specified in DLNA 7.4.40.5:
            // 
            // TimeSeekRange.dlna.org : npt=335.1-336.1/40445.4 bytes=1539686400-1540210688/304857907200
            //
            StringBuffer sb = new StringBuffer(32);
            
            sb.append("npt=");
            sb.append(timeSeekStartNPT);
            sb.append("-");
            if (timeSeekEndNPT != null)
            {
                sb.append(timeSeekEndNPT);
            }
            else
            {
                //timeseek end not available, use available seek end
                sb.append(availableSeekEndNPT);
            }
            sb.append("/");
            if (timeSeekDurationNPT != null)
            {
                sb.append(timeSeekDurationNPT);
            }
            else
            {
                sb.append("*");
            }
            if(startByte != -1)
            {
                sb.append(" bytes=");
                sb.append(startByte);
                sb.append("-");
                if (endByte != -1)
                {
                    sb.append(endByte);
                }
                else
                {
                    // For in-progress recordings/live streaming the available 
                    // seek end byte may be unknown (-1)
                    if(availableSeekEndByte != -1)
                    {
                        //end byte not available, use available seek end byte
                        sb.append(availableSeekEndByte);
                    }
                }                
                sb.append("/");
                //size may not yet be set..use that instead of totalContentLength, which is set to the current known value
                if ((contentEntry.getContentSize() == -1) 
                        || (totalContentLength <= 0) )
                {
                    // For in-progress recordings/live streaming the content 
                    // length may be unknown 
                    sb.append("*");
                }
                else
                {
                    sb.append(totalContentLength);
                }
            }

            if (!dtcpRangeHeaderIncluded || (dtcpRangeHeaderIncluded && protocolInfo.isLimitedRADA()))
            {
                addHeader(ContentRequestConstant.TIME_SEEK_RANGE_DLNA_ORG, sb.toString());
            }

            if (protocolInfo.isLinkProtected() && !dtcpRangeHeaderIncluded)
            {
                // Per DLNA Volume 3: Link Protection, requirement [7.4.8.1]
                addDtcpContentRangeHeader();
            }
         }

        // Add range if value was requested
        if (rangeHeaderIncluded)
        {
            if(startByte != -1)
            {
                StringBuffer sb = new StringBuffer("bytes ");
                sb.append(String.valueOf(startByte)).append("-");
                
                //-1 totalContentLength results in * for content range last field
                //-1 in endBytes, use totalContentLength, which won't be negative 1 
                if (endByte != -1)
                {
                    sb.append(String.valueOf(endByte)).append("/");
                }
                else
                {
                    // For in-progress recordings/live streaming the content 
                    // length may be unknown
                    if(totalContentLength > 0)
                    {
                        //subtract one from content length for end range
                        sb.append(String.valueOf(totalContentLength - 1)).append("/");
                    }
                }
                if(totalContentLength > 0)
                {
                    sb.append(String.valueOf(totalContentLength));
                }
                addHeader(ContentRequestConstant.CR_HEADER_CONTENT_RANGE, sb.toString());                
            }
        }

        if (dtcpRangeHeaderIncluded)
        {
            addDtcpContentRangeHeader();
        }

        // Add play speed if header was included
        if (playspeedHeaderIncluded && !dtcpRangeHeaderIncluded)
        {
            // Include Playspeed header in response formatting as specified in DLNA 7.4.70.3:
            // 
            // PlaySpeed.dlna.org : speed=-1/2
            //
            StringBuffer sb = new StringBuffer(32);
            
            //sb.append("speed=");
            sb.append(rateStr);
            
            addHeader(ContentRequestConstant.PLAYSPEED_DLNA_ORG, sb.toString());
            // Include OCAP HN frame rate header if the HTTPRequest contains
            // the OCAP HN Frame Rate in Trick Mode header
            if (httpRequest.getHeader(ContentRequestConstant.FRAME_RATE_IN_TRICK_MODE) != null && (rate != 1.0))
            {
                // Include FrameRateInTrickMode DLNA HN header formatting as specified in DLNA 7.5.4.3.3.16.10
                //
                // FrameRateInTrickMode.dlna.org : rate=15                
                sb = new StringBuffer(32);
                sb.append("rate=");
                sb.append(frameRateInTrickMode);

                addHeader(ContentRequestConstant.FRAME_RATE_IN_TRICK_MODE, sb.toString());
            }
            // Include DLNA frame rate header if not disabled and trick mode speed requested
            else if ((includeDLNAFrameRateHdr) && (rate != 1.0))
            {
                // Include FrameRateInTrickMode DLNA HN header formatting as specified in DLNA 7.5.4.3.3.16.10
                //
                // FrameRateInTrickMode.dlna.org : rate=15                
                sb = new StringBuffer(32);
                sb.append("rate=");
                sb.append(frameRateInTrickMode);

                addHeader(ContentRequestConstant.FRAME_RATE_IN_TRICK_MODE_DLNA_ORG, sb.toString());
            }
        }

        // Include max GOP header if applicable
        if (maxGOPsPerChunk != -1)
        {
            // Include MaxGOPsPerChunk OCAP HN header formatting as specified in HNP 5.6.1.4
            //
            // MaxGOPsPerChunk.ochn.org : gops=1
            //
            StringBuffer sb = new StringBuffer(32);
            
            sb.append("gops=");
            sb.append(maxGOPsPerChunk);
            
            addHeader(ContentRequestConstant.MAX_GOPS_PER_CHUNK, sb.toString());
        }
        
        if (frameTypesInTrickModeRequested != ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_NONE)
        {
            // Include FrameTypesInTrickMode OCAP HN header formatting as specified in HNP 5.6.1.7
            //
            // FrameTypesInTrickMode.ochn.org : frames=all
            //
            StringBuffer sb = new StringBuffer(32);
            
            sb.append("frames=");
            sb.append(ContentRequestUtil.formatFrameTypesInTrickMode(frameTypesInTrickMode));
            
            addHeader(ContentRequestConstant.FRAME_TYPES_IN_TRICK_MODE, sb.toString());
        }
        
        if (frameRateInTrickModeIncluded)
        {
            // Include FrameRateInTrickMode OCAP HN header formatting as specified in HNP 5.6.1.8
            //
            // FrameRateInTrickMode.ochn.org : rate=15
            //
            StringBuffer sb = new StringBuffer(32);
            sb.append("rate=");
            sb.append(frameRateInTrickMode);

            addHeader(ContentRequestConstant.FRAME_RATE_IN_TRICK_MODE, sb.toString());
        }

        if (addServersidePacedStreamingHeader())
        {
            // add ServersidePacedStreaming.ochn.org header to response
            addHeader(ContentRequestConstant.SERVERSIDE_PACED_STREAMING, "YES");
        }
            
            
    }

    private boolean addServersidePacedStreamingHeader()
    {
        Boolean serverSidePacing = null;
        try
        {
            serverSidePacing = ContentRequestUtil.parseServerSidePacing(getHeaderValue(ContentRequestConstant.SERVERSIDE_PACED_STREAMING));
        }
        catch (Exception e)
        {
            // some unexpected issue,,issue warning and treat as if no header sent
            if (log.isWarnEnabled())
            {
                log.warn("addServersidePacedStreamingHeader() - " + 
                        "unexpected exception checking for header", e);
            }                
        }

        if (serverSidePacing == null)
        {
            // no header in incoming request
            return false; 
        }
        boolean serverSidePacedStreaming = serverSidePacing.booleanValue();

        // does incoming request have SERVERSIDE_PACED_STREAMING header set to YES and
        // does platform support timestamping for this content request 
        if (HNAPIImpl.nativeServerGetServerSidePacingRestampFlag(contentLocationType,
            contentDescription, getProtocolInfo().getProfileId(),
            getProtocolInfo().getMimeType(), transformation) && 
            serverSidePacedStreaming)
        {
            return true;
        }

        return false;
    }
    
    /**
     * Determines if this request came from an HTTP 1.0 client by looking at
     * ? header in request.  If header values contain 1.0, it indicates a 1.0 client.
     * 
     * @return  true if client is HTTP 1.0, false otherwise
     */
    private boolean isClientHttp1_0()
    {
        boolean isClientHttp1_0 = false;
        if (httpRequest.isHttp10())
        {
            isClientHttp1_0 = true;
        }
        return isClientHttp1_0;
    }
    
    /**
     * Determines if requested byte range is valid for supplied content entry.
     * @param range the range to validate (index 1 will contain -1 if end was not present
     * 
     * @return true if valid, false otherwise
     */
    private boolean isValidByteRange(long[] range)
    {
        //restricted range format (7.4.38.3) start is required...http range start must be be less than end
        
        // Already determined that range header is syntax is correct when parsing
        // Rate is always 1 when Range header is supplied
        // Determine if the byte positions are valid
        if (range[0] == -1)
        {
            if (log.isWarnEnabled())
            {
                log.warn("isValidByteRange() - Per DLNA [7.4.38.3], first byte pos" +
                        " of range header must be specified");
            }
            return false;
        }
        else if (range[1] != -1 && (range[0] > range[1]))
        {
            if (log.isWarnEnabled())
            {
                log.warn("isValidByteRange() - start " + range[0]+ " greater than end " +
                        range[1]);
            }
            return false;
        }

        return true;
    }
    
    /**
     * Determines if requested byte range is valid for supplied content entry.
     * @param range the range to validate
     * @param encrypted true, validate against network domain (post-DTCP/IP).
     *                  false, validate against cleartext domain (pre-DTCP/IP).
     * 
     * @return true if valid, false otherwise
     */
    private boolean isValidContentByteRange(long[] range, boolean encrypted)
    {
        final long availableSeekStart;
        final long availableSeekEnd;

        // If range header is included startByte is set to byteRange[0]
        // and endByte is set to byteRange[1]
        if(startByte == -1 || endByte == -1)
        {
            // Its conceivable that byte range values are -1
            // for certain types of content and they are
            // considered valid
            return true;
        }
        
        if (encrypted)
        {
            availableSeekStart = this.availableSeekStartByte;
            availableSeekEnd = this.availableSeekEndByte;
        }
        else
        {
            availableSeekStart = this.availableDtcpSeekStartByte;
            availableSeekEnd = this.availableDtcpSeekEndByte;
        }

        if (range[0] < availableSeekStart || range[0] > availableSeekEnd)
        {
            if (log.isWarnEnabled())
            {
                log.warn("isValidContentByteRange() - start byte " + range[0] +
                        " is outside of available seek range (" +
                        availableSeekStart + ", " + availableSeekEnd + ")");
            }
            return false;
        }

        return true;
    }

    /**
     * Determines if requested time seek range is valid for supplied content entry.
     * 
     * @param startByte start byte position provided by setStreamingContext
     * @param endByte end byte position provided by setStreamingContext (may be -1 if not provided) 
     * 
     * @return true if valid, false otherwise
     */
    private boolean isValidContentTimeSeekRange(long startByte, long endByte)
    {
        if(startByte == -1 || endByte == -1)
        {
            // Its conceivable that one or both of these values are -1
            // for certain types of content. Hence
            // we don't need to validate the byte offsets here
            // TODO: This function really needs validate 
            // time seek range...
            return true;
        }
        
        final long availableSeekStart;
        final long availableSeekEnd;

        if (protocolInfo.isLinkProtected())
        {
            availableSeekStart = availableDtcpSeekStartByte;
            availableSeekEnd = availableDtcpSeekEndByte;
        }
        else
        {
            availableSeekStart = availableSeekStartByte;
            availableSeekEnd = availableSeekEndByte;
        }

        boolean valid = (startByte >= availableSeekStart);
        if (rate > 0.0F && endByte != -1)
        {
            valid = valid && (endByte <= availableSeekEnd);
        }
        if (rate < 0.0F && availableSeekEnd != -1)
        {
            // Available seek end byte may be -1 for
            // for certain content types (e.g in-progress 
            // recordings, live streaming etc.)
            valid = valid && (startByte <= availableSeekEnd);
        }
        if (!valid)
        {
            if (log.isWarnEnabled())
            {
                log.warn("isValidContentTimeSeekRange() - invalid byte range - start byte " + startByte + ", end byte: " + endByte + ", rate: " + rate + 
                        ", availableSeekStartByte: " + availableSeekStart + ", availableSeekEndByte: " + availableSeekEnd);
            }
            return false;
        }

        return true;
    }

    /**
     * Determine if time seek range included in request is valid.
     * Also initialize parameters needed to include in the request
     * and to initiate platform streaming.
     * 
     * @return  true if valid requested time seek range, false otherwise
     */
    private boolean isValidTimeSeekRange()
    {
        //timeseekrange format (7.4.40.3) start required, end optional, start must be less than end in forward scan, end must be less than start in backward scan
        if (timeSeekStartMillis == -1)
        {
            if (log.isWarnEnabled())
            {
                log.warn("isValidTimeSeekRange() - invalid range, start was -1");
            }
            return false;
        }
        //Already determined that range header is syntax is correct when parsing
        if (rate > 0.0F)
        {
            if (timeSeekEndMillis != -1 && (timeSeekStartMillis > timeSeekEndMillis))
            {
                if (log.isWarnEnabled())
                {
                    log.warn("isValidTimeSeekRange() - " + 
                        "invalid range, start is greater than end for positive rate: " + rate + ", start: " + timeSeekStartMillis + ", end: " + timeSeekEndMillis);
                }
                return false;
            }
        }

        if (rate < 0.0F)
        {
            if (timeSeekEndMillis != -1 && (timeSeekStartMillis < timeSeekEndMillis))
            {
                if (log.isWarnEnabled())
                {
                    log.warn("isValidTimeSeekRange() - " + 
                        "invalid range, start is less than than end for negative rate: " + rate + ", start: " + timeSeekStartMillis + ", end: " + timeSeekEndMillis);
                }
                return false;
            }
        }
        
        // Determine if this requirement applies:
        // DLNA [7.4.36.3]: 
        // If the HTTP Client Endpoint includes both the TimeSeekRange.dlna.org HTTP header 
        // field and the PlaySpeed.dlna.org HTTP header field in an HTTP GET request for a 
        // content binary in case of the "Limited Random Access Data Availability" model, 
        // it must specify the end position of the request range in the range specifier of the 
        // TimeSeekRange.dlna.org HTTP header. This guideline applies to both positive speed 
        // value (forward scan mode) and negative speed value (backward scan mode).
        //
        if ((playspeedHeaderIncluded) && (protocolInfo.isLimitedRADA()) && 
                (timeSeekEndMillis == -1))
        {
            if (log.isWarnEnabled())
            {
                log.warn("isValidTimeSeekRange() - " + 
                    "invalid range, limited RADA with playspeed must include end time");
            }
            return false;            
        }
        return true;
    }
    
    //
    // Parse methods to extract values from request headers
    //
    private void addDtcpContentRangeHeader()
    {
        if(startByte == -1 || endByte == -1)
        {
            // Its conceivable that one or both of these values are -1
            // for certain types of content. Hence
            // we don't need to validate the byte offsets here
            return;
        }
        StringBuffer sb = new StringBuffer(64);
        sb.append("bytes " + startByte + "-");

        if (dtcpRangeHeaderIncluded)
        {
            if (endByte != -1)
            {
                sb.append(endByte);
            }
            else
            {
                if(dtcpContentLength > 0)
                {
                    sb.append(dtcpContentLength - 1);
                }
            }
            sb.append("/");

            // Validate content length before appending
            // For some types of content the length
            // maybe unknown
            if (dtcpContentLength > 0)
            {
                sb.append(dtcpContentLength);
            }
            else
            {
                sb.append("*");
            }
        }
        else
        {
            // TODO: These are not DTCP values
            // Why are we including these???
            if(contentLengthBytes > 0)
            {
                sb.append(startByte + contentLengthBytes - 1);
                sb.append('/');
                sb.append(contentLengthBytes);
            }
        }
        addHeader(ContentRequestConstant.CONTENT_RANGE_DTCP_COM, sb.toString());
    }
    
    public void setStatus(ActionStatus status)
    {
        m_statusCode = status.getCode();
        m_reasonPhrase = status.getDescription();
        
        // The validity of the request and the status
        // set are strongly correlated, and a consistent state
        // can be managed from this single mutator.
        if(!ActionStatus.HTTP_OK.equals(status) ||
                !ActionStatus.HTTP_PARTIAL_CONTENT.equals(status) ||
                !ActionStatus.HTTP_CONTINUE.equals(status))
        {
            isValidRequest = false;
        }
    }
    
    private String getStatusLine()
    {
        StringBuffer sb = new StringBuffer();
        // Always return the highest supported http version in the response.
        sb.append(MediaServer.HIGHEST_SUPPORTED_HTTP_VER);
        sb.append(ContentRequestConstant.SP).append(m_statusCode).append(ContentRequestConstant.SP).append(m_reasonPhrase).append(ContentRequestConstant.CRLF);
        
        return sb.toString();
    }
    
    public void addHeader(String key, String value)
    {
        m_headers.put(key, value);
    }
    
    public String getHeaders()
    {
        StringBuffer sb = new StringBuffer();
        for(Iterator i = m_headers.entrySet().iterator(); i.hasNext();)
        {
            Map.Entry entry = (Map.Entry)i.next();
            sb.append(entry.getKey() + ": " + entry.getValue() + ContentRequestConstant.CRLF);
        }
        return sb.toString();
    }
}