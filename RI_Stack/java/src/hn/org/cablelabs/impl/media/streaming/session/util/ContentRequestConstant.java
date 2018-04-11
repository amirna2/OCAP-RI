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
package org.cablelabs.impl.media.streaming.session.util;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author Parthiban Balasubramanian
 * 
 */
public class ContentRequestConstant
{
    // ChunkedEncoding mode values
    public static final int HTTP_HEADER_CEM_GOP = 1;

    public static final int HTTP_HEADER_CEM_FRAME = 2;

    public static final int HTTP_HEADER_CEM_OTHER = 3;

    public static final int HTTP_HEADER_CEM_NONE = 0xFFFF;
    
    // ChunkedEncoding mode string values
    public static final String HTTP_HEADER_GOP_STR = "GOP";

    public static final String HTTP_HEADER_FRAME_STR = "Frame";
    
    public static final String HTTP_HEADER_OTHER_STR = "Other";

    // Frame types in trick mode
    public static final int HTTP_HEADER_TRICK_MODE_FRAME_TYPE_I = 1;

    public static final int HTTP_HEADER_TRICK_MODE_FRAME_TYPE_IP = 2;

    public static final int HTTP_HEADER_TRICK_MODE_FRAME_TYPE_ALL = 3;

    public static final int HTTP_HEADER_TRICK_MODE_FRAME_TYPE_NONE = 0xFFFF;

    // Frame types in trick mode string
    public static final String HTTP_HEADER_TRICK_MODE_FRAME_TYPE_I_STR = "I";

    public static final String HTTP_HEADER_TRICK_MODE_FRAME_TYPE_IP_STR = "IP";

    public static final String HTTP_HEADER_TRICK_MODE_FRAME_TYPE_ALL_STR = "all";
    
    public static final int HEX_RADIX = 16;

    // TODO: status codes are not consistently set (sometimes set when
    // HNStreamingException is thrown, sometimes not)

    // General purpose constants
    public static final int TS_PACKET_SIZE = 188;

    public static final long MILLIS_PER_SECOND = 1000L;

    public static final long NANOS_PER_SECOND = 1000000000L;

    public static final long NANOS_PER_MILLI = NANOS_PER_SECOND / MILLIS_PER_SECOND;

    // Header field strings
    //
    // OCAP namespace headers
    //
    public static final String CURRENT_DECODE_PTS = "CurrentDecodePTS.ochn.org";

    public static final String CHUNK_ENCODING_MODE = "ChunkEncodingMode.ochn.org";

    public static final String MAX_TRICK_MODE_BANDWIDTH = "MaxTrickModeBandwidth.ochn.org";

    public static final String MAX_GOPS_PER_CHUNK = "MaxGOPsPerChunk.ochn.org";

    public static final String MAX_FRAMES_PER_GOP = "MaxFramesPerGOP.ochn.org";

    public static final String SERVERSIDE_PACED_STREAMING = "ServersidePacedStreaming.ochn.org";

    public static final String FRAME_TYPES_IN_TRICK_MODE = "FrameTypesInTrickMode.ochn.org";

    public static final String FRAME_RATE_IN_TRICK_MODE = "FrameRateInTrickMode.ochn.org";

    // DLNA namespace headers
    //
    public static final String TIME_SEEK_RANGE_DLNA_ORG = "TimeSeekRange.dlna.org";

    public static final String PLAYSPEED_DLNA_ORG = "PlaySpeed.dlna.org";

    public static final String SCID_DLNA_ORG = "scid.dlna.org";

    public static final String GET_CONTENT_FEATURES_DLNA_ORG = "getcontentFeatures.dlna.org";

    public static final String CONTENT_FEATURES_DLNA_ORG = "contentFeatures.dlna.org";

    public static final String TRANSFER_MODE_DLNA_ORG = "transferMode.dlna.org";

    public static final String GET_AVAILABLE_SEEK_RANGE_DLNA_ORG = "getAvailableSeekRange.dlna.org";

    public static final String AVAILABLE_SEEK_RANGE_DLNA_ORG = "availableSeekRange.dlna.org";

    public static final String FRAME_RATE_IN_TRICK_MODE_DLNA_ORG = "FrameRateInTrickMode.dlna.org";

    // HTTP Headers
    //
    public static final String VARY_NAME = "Vary";

    public static final String RANGE = "Range";

    public static final String PRAGMA = "Pragma";

    public static final String CACHE_CONTROL = "Cache-control";

    public static final String CONNECTION = "Connection";

    // HTTP Headers
    //
    public static final String RANGE_DTCP_COM = "Range.dtcp.com";

    public static final String CONTENT_RANGE_DTCP_COM = "Content-Range.dtcp.com";

    // Constant values for headers
    //
    public static final String CHUNKED_VALUE = "chunked";

    public static final String STREAMING_VALUE = "Streaming";

    public static final String BACKGROUND_TRANSFER_VALUE = "Background";

    public static final String NO_CACHE = "no-cache";

    public static final String CONNECTION_CLOSE = "close";
    
    public static final String FRAMES = "frames";
    
    public static final String CHUNK = "chunk";
    
    public static final String BANDWIDTH = "bandwidth";
    
    public static final String PTS = "PTS";
    
    public static final String NPT = "npt";
    
    public static final String BYTES = "bytes";
    
    public static final String SPEED = "speed";
    
    public static final String GOPS = "gops";
    
    // Constants for pacing
    public static final String PACING = "pacing";
    
    public static final String PACING_YES = "YES";
    
    public static final String PACING_NO = "NO";
    
    

    // Server software property value
    public static final String SERVER_SOFTWARE_ID_PROP = "OCAP.hn.server.softwareId";

    public static final String SERVER_SOFTWARE_ID_DEFAULT = "OCAP-RI/1.0";

    // Chunk encoding property values
    public static final String CHUNK_ENCODING_PROP = "OCAP.hn.server.chunkEncodingMode";

    public static final String CHUNK_ENCODING_ALWAYS = "always";

    public static final String CHUNK_ENCODING_AS_APPROPRIATE = "as-appropriate";

    public static final String CHUNK_ENCODING_NEVER = "never";

    public static final String CHUNK_ENCODING_NOT_RECOGNIZED = "No Recognized Value Specified";

    // Lenient property values
    public static final String IS_LENIENT_PROP = "OCAP.hn.server.lenient.enabled";

    // FrameRate property value
    public static final String INCLUDE_DLNA_FRAME_RATE_IN_TRICK_MODE_PROP = "OCAP.hn.server.includeDLNAFrameRateInTrickModeHeader";

    public final static SimpleDateFormat RFC1123_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ", Locale.US);

    // String termination values
    public static final String SP = " ";

    public static final String CRLF = "\r\n";

    public static final String SERVER = "OCAP-RI HTTP Server";

    // Content Request Header names
    public static final String CR_HEADER_SERVER = "Server";

    public static final String CR_HEADER_DATE = "Date";

    public static final String CR_HEADER_CONTENT_TYPE = "Content-Type";

    public static final String CR_HEADER_TRANSFER_ENCODING = "Transfer-Encoding";

    public static final String CR_HEADER_CONTENT_LENGTH = "Content-Length";

    public static final String CR_HEADER_CONTENT_RANGE = "Content-Range";

    // TimeZone value
    public static final String TIMEZONE_GMT = "GMT";

    public static final String HTTP_REQUEST_TYPE_GET = "GET";

    public static final String HTTP_REQUEST_TYPE_PUT = "PUT";

    public static final String HTTP_REQUEST_TYPE_HEAD = "HEAD";

}
