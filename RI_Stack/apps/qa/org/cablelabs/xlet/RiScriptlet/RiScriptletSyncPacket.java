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

import org.apache.log4j.Logger;

/**
 * This class encapsulates the details of the TCP packets used by the RiScriptletSyncServer
 * and RiScriptletSyncClient
 * 
 * @author Steve Arendt
 */
public class RiScriptletSyncPacket
{
    private byte m_syncByte = -1;
    private byte m_commandTag;
    private short m_payloadSz;
    private byte[] m_payload;
    private byte m_errorField;
    
    public static final int HEADER_SZ = 5;
    public static final byte SYNC_BYTE = 0x47;
    
    public static final byte CMD_REGISTER = 0x01;
    public static final byte CMD_UNREGISTER = 0x02;
    public static final byte CMD_SYNC = 0x03;
    
    public static final byte ERROR_NONE = 0;
    public static final byte ERROR_INVALID_CMD = 1;
    public static final byte ERROR_INVALID_CLIENT_ID = 2;
    public static final byte ERROR_INVALID_SYNC_PT = 3;
    public static final byte ERROR_SYNC_DATA_ALREADY_SET = 4;

    private static final Logger m_log = Logger.getLogger(RiScriptletSyncPacket.class);

    public static void main(String[] args) 
    {
/*        RiScriptletSyncPacket syncPacket = new RiScriptletSyncPacket ((byte)4, 
                new byte[]{1,2,3,4,5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17});
        byte[] packetData = syncPacket.formatPacket();
        
        for (int i=0; i<packetData.length; i++)
        {
            System.out.println("packetData[" + i + "] = " + packetData[i]);
        }
        */
        
        RiScriptletSyncPacket syncPacket = new RiScriptletSyncPacket();
        byte[] headerData = new byte[] {0x47, 5, (byte)0x8C, (byte)0xFF};
        syncPacket.parseHeader(headerData);
        System.out.println ("commandTag = " + syncPacket.getCommandTag());
        System.out.println ("payloadSz = " + syncPacket.getPayloadSz());
    }
    
    public RiScriptletSyncPacket()
    {
    }

    public RiScriptletSyncPacket(byte commandTag)
    {
        m_syncByte = SYNC_BYTE;
        m_commandTag = commandTag;
        m_payloadSz = 0;
        m_payload = new byte[0];
        m_errorField = ERROR_NONE;
    }

    public RiScriptletSyncPacket(byte commandTag, byte[] payload)
    {
        m_syncByte = SYNC_BYTE;
        m_commandTag = commandTag;
        m_payloadSz = (short)payload.length;
        m_payload = payload;
        m_errorField = ERROR_NONE;
    }

    public RiScriptletSyncPacket(byte commandTag, byte[] payload, byte errorField)
    {
        m_syncByte = SYNC_BYTE;
        m_commandTag = commandTag;
        m_payloadSz = (short)payload.length;
        m_payload = payload;
        m_errorField = errorField;
    }

    public int parseHeader(byte[] headerData)
    {
        m_syncByte = headerData[0];
        m_commandTag = headerData[1];
        m_errorField = headerData[2];
        
        m_payloadSz = (short)((headerData[3] & 0x00FF) << 8);
        m_payloadSz += (short)(headerData[4] & 0x00FF);
        
        // check that syncByte and command tag are valid
        if (m_syncByte != SYNC_BYTE)
        {
            if (m_log.isErrorEnabled())
            {
                m_log.error("parseHeader detected bad syncByte: " + m_syncByte);
            }
            return -1;
        }
        
        if (m_commandTag != CMD_REGISTER && m_commandTag != CMD_UNREGISTER
                && m_commandTag != CMD_SYNC)
        {
            if (m_log.isErrorEnabled())
            {
                m_log.error("parseHeader detected bad commandTag: " + m_commandTag);
            }
            return -2;
        }
        
        return 0;
    }
    
    public byte[] formatPacket()
    {
        byte[] packetData = new byte[HEADER_SZ + m_payloadSz];
        packetData[0] = SYNC_BYTE;
        packetData[1] = m_commandTag;
        packetData[2] = m_errorField;
        
        packetData[3] = (byte) ((m_payloadSz >> 8) & 0x000000FF);
        packetData[4] = (byte) (m_payloadSz & 0x000000FF);
        
        System.arraycopy(m_payload, 0, packetData, HEADER_SZ, m_payloadSz);
        
        return packetData;
    }
    
    public byte getCommandTag()
    {
        return m_commandTag;
    }

    public void setCommandTag (byte commandTag)
    {
        m_commandTag = commandTag;
    }

    public short getPayloadSz()
    {
        return m_payloadSz;
    }

    public byte[] getPayload()
    {
        return m_payload;
    }

    public void setPayload (byte[] payload)
    {
        m_payloadSz = (short)payload.length;
        m_payload = payload;
    }
    
    public byte getErrorField()
    {
        return m_errorField;
    }
    
    public String toString()
    {
        String desc = getCmdDesc() + ", " + getErrorDesc() + ", payload: {";
        for (int i=0; i<m_payload.length; i++)
        {
            desc += "0x" + Integer.toHexString( ((int)m_payload[i]) & 0x0000000FF );
            if (i != m_payload.length-1)
            {
                desc += ", ";
            }
        }
        
        desc += "}";
        return desc;      
   }
    
    public String getCmdDesc()
    {
        switch (m_commandTag)
        {
            case CMD_REGISTER:
                return "CMD_REGISTER";
            case CMD_UNREGISTER:
                return "CMD_UNREGISTER";
            case CMD_SYNC:
                return "CMD_SYNC";
            default:
                return "UNKNOWN CMD";
        }
    }
    
    public String getErrorDesc()
    {
        switch (m_errorField)
        {
            case ERROR_NONE:
                return "ERROR_NONE";
            case ERROR_INVALID_CMD:
                return "ERROR_INVALID_CMD";
            case ERROR_INVALID_CLIENT_ID:
                return "ERROR_INVALID_CLIENT_ID";
            case ERROR_INVALID_SYNC_PT:
                return "ERROR_INVALID_SYNC_PT";
            case ERROR_SYNC_DATA_ALREADY_SET:
                return "ERROR_SYNC_DATA_ALREADY_SET";
            default:
                return "UNKNOWN ERROR";
        }
    }
    
    public static RiScriptletSyncPacket formatRegisterPacket(byte clientId, byte expectedNumClients)
    {
        return formatRegisterPacket(clientId, expectedNumClients, ERROR_NONE);
    }

    public static RiScriptletSyncPacket formatRegisterPacket(byte clientId, byte expectedNumClients, byte errorField)
    {
        return new RiScriptletSyncPacket(RiScriptletSyncPacket.CMD_REGISTER, 
                new byte[] {clientId, expectedNumClients}, errorField);
    }

    public static RiScriptletSyncPacket formatUnregisterPacket(byte clientId)
    {
        return formatUnregisterPacket(clientId, ERROR_NONE);
    }

    public static RiScriptletSyncPacket formatUnregisterPacket(byte clientId, byte errorField)
    {
        return new RiScriptletSyncPacket(RiScriptletSyncPacket.CMD_UNREGISTER, 
                new byte[] {clientId}, errorField);
    }

    public static RiScriptletSyncPacket formatSyncPacket(byte clientId, byte syncPt, byte[] syncData)
    {
        return formatSyncPacket(clientId, syncPt, syncData, ERROR_NONE);
    }

    public static RiScriptletSyncPacket formatSyncPacket(byte clientId, byte syncPt, byte[] syncData, byte errorField)
    {
        byte[] payload = new byte[syncData.length + 2];
        payload[0] = clientId;
        payload[1] = syncPt;
        System.arraycopy(syncData, 0, payload, 2, syncData.length);
        
        return new RiScriptletSyncPacket(RiScriptletSyncPacket.CMD_SYNC, payload, errorField);
    }

}
