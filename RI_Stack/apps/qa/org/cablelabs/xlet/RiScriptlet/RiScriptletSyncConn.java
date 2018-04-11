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

// Declare package.
package org.cablelabs.xlet.RiScriptlet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.log4j.Logger;

/**
 * This class encapsulates the details of the TCP connection used by the RiScriptletSyncServer
 * and RiScriptletSyncClient
 * 
 * @author Steve Arendt
 */
public class RiScriptletSyncConn
{
    private Socket m_connSocket;
    private OutputStream m_outStream;
    private InputStream m_inStream;
    
    private static final Logger m_log = Logger.getLogger(RiScriptletSyncConn.class);


    public RiScriptletSyncConn()
    {
    }
    
    public RiScriptletSyncConn(Socket connSocket)
        throws IOException
    {
        m_connSocket = connSocket;
        m_outStream = m_connSocket.getOutputStream();
        m_inStream = new BufferedInputStream(m_connSocket.getInputStream());
    }
    
    public String toString()
    {
        String desc = "RiScriptletSyncConn: dest endpt = " + m_connSocket.getInetAddress().toString() + ":" + m_connSocket.getPort() + 
                ", local endpt = " + m_connSocket.getLocalAddress() + ":" + m_connSocket.getLocalPort();
        return desc;
    }
    
    public void openConn(String syncSvrHost, int syncSvrPort)
        throws IOException
    {
        if (m_log.isDebugEnabled())
        {
            m_log.debug("Opening sync connection");
        }

        m_connSocket = new Socket(InetAddress.getByName(syncSvrHost), syncSvrPort);

        m_outStream = m_connSocket.getOutputStream();
        m_inStream = new BufferedInputStream(m_connSocket.getInputStream());
        
        if (m_log.isDebugEnabled())
        {
            m_log.debug("Sync connection opened");
        }

    }

    public void closeConn()
    {
        if (m_connSocket != null) 
        {
            if (m_log.isDebugEnabled())
            {
                m_log.debug("Closing sync connection");
            }

            try
            {
                m_connSocket.close();
            }
            catch (IOException ex) 
            {
                if (m_log.isErrorEnabled())
                {
                    m_log.error("Error closing sync connection", ex);
                }
            }
        }

        m_connSocket = null;
        m_outStream = null;
        m_inStream = null;
    }

    public boolean isOpen()
    {
        return (m_connSocket != null);
    }

    public RiScriptletSyncPacket sendReceive(RiScriptletSyncPacket syncPacketSend, int timeoutMillis)
        throws Exception
    {
        
        RiScriptletSyncPacket syncPacketRcv = null;
            
        send (syncPacketSend);
            

        while (true)
        {
            syncPacketRcv = receive (timeoutMillis);
            if (isReply(syncPacketSend, syncPacketRcv))
            {
                break;
            }
        }
                                            
        return syncPacketRcv;
    }


    /**
     * GORP: fill in
     *          * 
     * @return the byte[] received from the sync server
     */
    public RiScriptletSyncPacket receive(int timeoutMillis)
        throws Exception
    {
        if (!isOpen())
        {
            throw new Exception ("Socket not open");
        }

        byte[] headerBuf = new byte[RiScriptletSyncPacket.HEADER_SZ];
        

        if (m_log.isDebugEnabled())
        {
            m_log.debug("Receiving header bytes.");
        }
        
        // first receive header: first byte is a sync byte, second is command tag, and last two are length
        for (int i=0; i<RiScriptletSyncPacket.HEADER_SZ; i++)
        {
           readByte(headerBuf, i, timeoutMillis);
        }

        if (m_log.isDebugEnabled())
        {
            m_log.debug("Finished receiving header bytes.");
        }
        
        RiScriptletSyncPacket syncPacket = new RiScriptletSyncPacket();
        int nReturn = syncPacket.parseHeader(headerBuf);
        if (nReturn != 0)
        {
            if (m_log.isErrorEnabled())
            {
                m_log.error("Error during socket read: problem parsing header");
            }
            
            return null;
        }

        byte commandTag = syncPacket.getCommandTag();
        short payloadLength = syncPacket.getPayloadSz();
        if (m_log.isDebugEnabled())
        {
            m_log.debug("Header: commandTag = " + commandTag + ", payloadLength = " + payloadLength);
        }

        // now that we know how big the payload is, allocate buffer
        byte[] payload = new byte[payloadLength];

        int index = 0;
        while (index < payloadLength)
        {
            readByte(payload, index, timeoutMillis);

            index++;
        }
            
        syncPacket.setPayload(payload);

        if (m_log.isDebugEnabled())
        {
            m_log.debug("Packet read complete: " + syncPacket);
        }
        
        return syncPacket;
    }
    
    /**
     * Sends a byte[] to the sync server
     * 
     * @param data
     *            the data to send
     */
    public void send(RiScriptletSyncPacket packet)
        throws Exception
    {
        if (!isOpen())
        {
            throw new Exception ("Socket not open");
        }
            
        byte[] data = packet.formatPacket();
        
        if (m_log.isDebugEnabled())
        {
            m_log.debug("Sending packet: " + packet);
        }

        m_outStream.write(data);
    }
    
    private void readByte (byte[] data, int index, int timeoutMillis)
        throws Exception
    {
        int periodMillis = 100;
        boolean infiniteTimeout = (timeoutMillis < 0);
        
        int numReps = timeoutMillis / periodMillis;        
        if (timeoutMillis > numReps*periodMillis || numReps == 0)
        {
            numReps++;
        }
        
        for (int i=0; i<numReps; i++)
        {
            if (infiniteTimeout || m_inStream.available() != 0)
            {
                int numBytesRead = m_inStream.read(data, index, 1);
                if (numBytesRead <= 0)
                {
                    // error or socket closure occurred -- so close socket and return
                    closeConn();
                    throw new Exception ("Error reading byte");
                }
                
                return;
            }
            
            try
            {
                Thread.currentThread().sleep(periodMillis);
            }
            catch (InterruptedException ex)
            {
                // discard
            }
        }
        
        throw new Exception ("Timeout reading byte");
    }


    private boolean isReply(RiScriptletSyncPacket packetSend, RiScriptletSyncPacket packetRcv)
    {
        byte commandTagSent = packetSend.getCommandTag();
        byte commandTagRcv = packetRcv.getCommandTag();
        if (commandTagSent != commandTagRcv)
        {
            return false;
        }
        
        if (commandTagSent == RiScriptletSyncPacket.CMD_SYNC)
        {
            byte syncIdSent = packetSend.getPayload()[1];
            byte syncIdRcv = packetRcv.getPayload()[1];
            if (syncIdSent != syncIdRcv)
            {
                return false;
            }
        }
        
        return true;
    }

}