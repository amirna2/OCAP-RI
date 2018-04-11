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
package org.cablelabs.impl.media.streaming.session.data;

import java.net.URI;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;

public class HNStreamParamsMediaPlayerHttp implements HNStreamParams
{
    private static final Logger log = Logger.getLogger(HNStreamParamsMediaPlayerHttp.class);

    private int m_connectionId;
    private String m_uri;
    private String m_dlnaProfileId;
    private String m_mimeType;
    private String m_host;
    private int m_port;
    private String m_dtcpHost;
    private int m_dtcpPort;
    private boolean m_isTimeSeekSupported;
    private boolean m_isRangeSupported;
    private boolean m_isSenderPaced;
    private boolean m_isLimitedTimeSeekSupported;
    private boolean m_isLimitedByteSeekSupported;
    private boolean m_isPlayContainer; 
    private boolean m_isS0Increasing;
    private boolean m_isSnIncreasing; 
    private boolean m_isStreamingMode;
    private boolean m_isInteractiveMode; 
    private boolean m_isBackgroundMode;
    private boolean m_isHTTPStallingSupported;
    private boolean m_isDLNAV15;
    private boolean m_isLinkProtected; 
    private boolean m_isFullClearByteSeek;
    private boolean m_isLimitedClearByteSeek;
    private int m_playspeedsCnt;
    private float m_playspeeds[]; 
    
    public HNStreamParamsMediaPlayerHttp(int connectionId, URI requestURI, HNStreamProtocolInfo protocolInfo)
    {
        // Assign connection id
        m_connectionId = connectionId;

        // Formulate the request URI string from supplied URI
        StringBuffer sb = new StringBuffer(128);
        sb.append(requestURI.getPath());
        if (requestURI.getQuery() != null)
        {
            sb.append("?");
            sb.append(requestURI.getQuery());
        }
        m_uri = sb.toString();
        
        // Get host from URI
        m_host = requestURI.getHost();
        
        // Get port from URI
        m_port = requestURI.getPort();
        if (m_port == -1)
        {
            // Default HTTP port is 80.
            m_port = 80;
        }

        // Get profile id from protocol info
        m_dlnaProfileId = protocolInfo.getProfileId();

        // Assign mime type and DTCP host/port values, if available
        parseMimeType(protocolInfo.getMimeType());
        
        m_isTimeSeekSupported = protocolInfo.isTimeSeekSupported();
        m_isRangeSupported = protocolInfo.isNetworkRangeSupported();
        String flags = protocolInfo.getFlagsParam();
        m_isSenderPaced = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.SP_FLAG);
        m_isLimitedTimeSeekSupported = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.LOP_NPT);
        m_isLimitedByteSeekSupported = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.LOP_BYTES);
        m_isPlayContainer = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.PLAYCONTAINER_PARAM); 
        m_isS0Increasing = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.S0_INCREASING);
        m_isSnIncreasing = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.SN_INCREASING); 
        m_isStreamingMode = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.TM_S);
        m_isInteractiveMode = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.TM_I); 
        m_isBackgroundMode = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.TM_B);
        m_isHTTPStallingSupported = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.HTTP_STALLING);
        m_isDLNAV15 = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.DLNA_V15_FLAG);
        m_isLinkProtected = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.LP_FLAG); 
        m_isFullClearByteSeek = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.CLEARTEXTBYTESEEK_FULL_FLAG);
        m_isLimitedClearByteSeek = HNStreamProtocolInfo.isFlagSet(flags, HNStreamProtocolInfo.LOP_CLEARTEXTBYTES);
        m_playspeeds = protocolInfo.getPlayspeeds();
        if (m_playspeeds != null)
        {
            m_playspeedsCnt = m_playspeeds.length;
        }
    }

    public int getStreamType()
    {
        return HNStreamType.HN_REQUEST_PARAM_TYPE_MEDIA_PLAYER_HTTP;
    }

    /**
     * Accessor
     * 
     * @return connection id
     */
    public String toString()
    {
        return "HNStreamParamsMediaPlayerHttp - connectionId: " + m_connectionId + ", uri: " + m_uri +
                ", dlnaProfileId: " + m_dlnaProfileId + ", mimeType: " + m_mimeType +
                ", host: " + m_host + ", port: " + m_port +
                ", dtcpHost: " + m_dtcpHost + ", dtcpPort: " + m_dtcpPort;
    }

    /**
     * Assigns mimeType, dtcpHost and dtcpPort.
     */
    private void parseMimeType(String mimeType)
    {
        String[] components = Utils.split(mimeType, ";");
        m_mimeType = components[0];

        for (int i = 0; i < components.length; i++)
        {
            if (components[i].startsWith(HNStreamProtocolInfo.MIMETYPE_DTCP_CONTENT_FORMAT))
            {
                m_mimeType = components[i].substring(HNStreamProtocolInfo.MIMETYPE_DTCP_CONTENT_FORMAT.length());
            }
            else if (components[i].startsWith(HNStreamProtocolInfo.MIMETYPE_DTCP_HOST))
            {
                m_dtcpHost = components[i].substring(HNStreamProtocolInfo.MIMETYPE_DTCP_HOST.length());
            }
            else if (components[i].startsWith(HNStreamProtocolInfo.MIMETYPE_DTCP_PORT))
            {
                try
                {
                    int dtcpPort = Integer.parseInt(components[i].substring(HNStreamProtocolInfo.MIMETYPE_DTCP_PORT.length()));
                    if (dtcpPort > 0 && dtcpPort < 65536)
                    {
                        m_dtcpPort = dtcpPort;
                    }
                    else
                    {
                        throw new NumberFormatException("Bad TCP port number " + dtcpPort);
                    }
                }
                catch (NumberFormatException nfe)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unable to extract DTCP port value: " + nfe.getMessage());
                    }
                }
            }
        }
    }
}
