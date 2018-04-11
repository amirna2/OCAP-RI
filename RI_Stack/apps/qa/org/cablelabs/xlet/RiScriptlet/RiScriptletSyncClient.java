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

import java.net.InetAddress;
import java.net.Socket;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * This class acts as a client to make calls to the RiScriptlet sync server
 * 
 * @author Steve Arendt
 */
public class RiScriptletSyncClient
{
    private String m_syncSvrHost;
    private int m_syncSvrPort;

    private byte m_clientId;
    
    private RiScriptletSyncConn m_conn;


    private static final Logger m_log = Logger.getLogger(RiScriptletSyncClient.class);

    // main for testing
    public static void main(String[] args) 
    {
        RiScriptletSyncClient syncClient = null;

        try
        {
            syncClient = new RiScriptletSyncClient("127.0.0.1", 12345);

            syncClient.register((byte)0 /* clientId */, (byte) 1 /* expectedNumClients */, -1);

            Thread.currentThread().sleep(5000);
            
            try
            {
                syncClient.sync((byte)1, 3000);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }

            syncClient.unregister(5000);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Created a new RiScriptletSyncClient instance
     * 
     * @param syncSvrHost
     *            The hostname or IP addr for the RiScriptlet acting as the sync server.
     * @param syncSvrPort
     *            The TCP port for the RiScriptlet acting as the sync server.
     * 
     * @throws Exception
     *            If a connection cannot be established, then an Exception is thrown
     */
    public RiScriptletSyncClient (String syncSvrHost, int syncSvrPort)
    {
        m_syncSvrHost = syncSvrHost;
        m_syncSvrPort = syncSvrPort;
        
        m_conn = new RiScriptletSyncConn();
    }
    
    /**
     * Registers with the RiScriptletSyncServer by opening a connection to
     * it and sending it's clientId
     * 
     * @param clientId
     *            The clientId for this script.
     * @param expectedNumClients
     *            The expected number of sync clients in the group of simultaneous
     *            script.  This is used by the sync server to wait for all the clients
     *            to register before satisfying any sync requests.
     * @param timeoutMillis
     *            The timeout in millisecs for the TCP comm.
     * 
     * @throws Exception
     *            If a connection cannot be established, then an Exception is thrown
     */
    public void register (byte clientId, byte expectedNumClients, int timeoutMillis)
        throws Exception
    {
        m_clientId = clientId;

        m_conn.openConn(m_syncSvrHost, m_syncSvrPort);
        
        RiScriptletSyncPacket packet = RiScriptletSyncPacket.formatRegisterPacket(m_clientId, 
                expectedNumClients);
        
        RiScriptletSyncPacket replyPacket = m_conn.sendReceive(packet, timeoutMillis);
        
        if (replyPacket.getErrorField() != RiScriptletSyncPacket.ERROR_NONE)
        {
            // GORP: handle this: close conn?
            if (m_log.isErrorEnabled())
            {
                m_log.error("Error " + replyPacket.getErrorField() + "(" + replyPacket.getErrorDesc() + ") received during register");
            }
            throw new Exception ("Error " + replyPacket.getErrorField() + "(" + replyPacket.getErrorDesc() + ") received during register");
        }
    }

    /**
     * Un-registers with the RiScriptletSyncServer by closing its connection.
     * 
     * @param timeoutMillis
     *            The timeout in millisecs for the TCP comm.
     */
    public void unregister(int timeoutMillis)
    {
        try
        {
            RiScriptletSyncPacket packet = RiScriptletSyncPacket.formatUnregisterPacket(m_clientId);

            RiScriptletSyncPacket replyPacket = m_conn.sendReceive(packet, timeoutMillis);
            
            if (replyPacket.getErrorField() != RiScriptletSyncPacket.ERROR_NONE)
            {
                if (m_log.isErrorEnabled())
                {
                    m_log.error("Error " + replyPacket.getErrorField() + "(" + replyPacket.getErrorDesc() + ") received during unregister");
                }
            }
        }
        catch (Exception ex)
        {
            if (m_log.isErrorEnabled())
            {
                m_log.error("Error during unregister: ", ex);
            }
        }

        m_conn.closeConn();
    }

    /**
     * Synchronizes with other scripts at the specified syncId.  This call
     * blocks until all the scripts make a sync call with the same syncId.
     * A script can optionally pass in a byte[] which will then be passed
     * to all the scripts when the return from the sync call; this is a 
     * way of passing info from one script to another.  Only one script can 
     * pass in data for a sync point.
     * 
     * @param syncId
     *            The syncId for the sync point.
     * @param timeoutMillis
     *            The timeout in millisecs for the TCP comm.
     * 
     * @return a byte[] containing the data from the sync point
     *
     * @throws Exception
     *            If a comm error or other error occurs, then an Exception is thrown
     */
    public byte[] sync(byte syncId, int timeoutMillis)
        throws Exception
    {
        RiScriptletSyncPacket packet = RiScriptletSyncPacket.formatSyncPacket(m_clientId, syncId, new byte[0]);
                
        RiScriptletSyncPacket replyPacket = m_conn.sendReceive(packet, timeoutMillis);
        if (replyPacket.getErrorField() != RiScriptletSyncPacket.ERROR_NONE)
        {
            // GORP: handle this: close conn?
            if (m_log.isErrorEnabled())
            {
                m_log.error("Error " + replyPacket.getErrorField() + "(" + replyPacket.getErrorDesc() + ") received during sync");
            }
            throw new Exception ("Error " + replyPacket.getErrorField() + "(" + replyPacket.getErrorDesc() + ") received during sync");
        }
        
        return replyPacket.getPayload();
    }

    /**
     * Synchronizes with other scripts at the specified syncId.  This call
     * blocks until all the scripts make a sync call with the same syncId.
     * A script can optionally pass in a byte[] which will then be passed
     * to all the scripts when the return from the sync call; this is a 
     * way of passing info from one script to another.  Only one script can 
     * pass in data for a sync point.
     * 
     * @param syncId
     *            The syncId for this sync call.
     * @param timeoutMillis
     *            The timeout in millisecs for the TCP comm.
     * @param data
     *            The data to be passed to the other scripts from the sync call.
     * 
     * @return a byte[] containing the data from the sync point
     *
     * @throws Exception
     *            If a comm error or other error occurs, then an Exception is thrown
     */
    public byte[] sync(byte syncId, byte[] data, int timeoutMillis)
        throws Exception
    {
        RiScriptletSyncPacket syncPacket = RiScriptletSyncPacket.formatSyncPacket(m_clientId, syncId, data);

        RiScriptletSyncPacket syncPacketRcv = m_conn.sendReceive(syncPacket, timeoutMillis);
        return syncPacketRcv.getPayload();
    }

}