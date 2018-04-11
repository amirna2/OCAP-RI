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

import java.net.Socket;

/**
 * Parameters used to support server-side transmission via HTTP.
 * 
 * See {@link org.cablelabs.impl.media.streaming.session.ContentRequest} for 
 * valid values for chunkedEncodingMode and frameTypesInTrickMode
 */
public class HNStreamParamsMediaServerHttp implements HNStreamParams
{
    private int m_connectionId;
    private String m_dlnaProfileId;
    private String m_mimeType;
    private int m_mpeSocket;
    private int m_chunkedEncodingMode;
    private long m_maxTrickModeBandwidth;
    private long m_currentDecodePTS;
    private int m_maxGOPsPerChunk;
    private int m_maxFramesPerGOP;
    private boolean m_useServerSidePacing;
    private int m_frameTypesInTrickModes;
    private int m_connectionStallingTimeoutMS;

    public HNStreamParamsMediaServerHttp(int connectionId, String dlnaProfileId, String mimeType,
            Socket socket, int chunkedEncodingMode, long maxTrickModeBandwidth, long currentDecodePTS,
            int maxGOPsPerChunk, int maxFramesPerGOP, boolean useServerSidePacing,
            int frameTypesInTrickModes, int connectionStallingTimeoutMS)
    {
        if (socket == null)
        {
            throw new IllegalArgumentException("socket is null");
        }
        else
        {
            m_mpeSocket = org.cablelabs.impl.net.Socket.getNativeHandle(socket);
        }

        m_connectionId = connectionId;
        m_dlnaProfileId = dlnaProfileId;
        m_mimeType = mimeType;
        m_chunkedEncodingMode = chunkedEncodingMode;
        m_maxTrickModeBandwidth = maxTrickModeBandwidth;
        m_currentDecodePTS = currentDecodePTS;
        m_maxGOPsPerChunk = maxGOPsPerChunk;
        m_maxFramesPerGOP = maxFramesPerGOP;
        m_useServerSidePacing = useServerSidePacing;
        m_frameTypesInTrickModes = frameTypesInTrickModes;
        m_connectionStallingTimeoutMS = connectionStallingTimeoutMS;
    }


    public int getStreamType()
    {
        return HNStreamType.HN_REQUEST_PARAM_TYPE_MEDIA_SERVER_HTTP;
    }
/*
    public int getConnectionId()
    {
        return m_connectionId;
    }
    
    public Socket getSocket()
    {
        return m_socket;
    }
*/
    public String toString()
    {
        return "HNStreamParamsMediaServerHttp - connectionId: " + m_connectionId + ", dlnaProfileId: " + m_dlnaProfileId +
                ", mimeType: " + m_mimeType + ", mpeSocket: " + m_mpeSocket;
    }
}
