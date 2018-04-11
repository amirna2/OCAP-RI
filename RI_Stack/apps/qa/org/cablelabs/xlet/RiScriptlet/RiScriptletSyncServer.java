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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;


/**
 * This class is the RiScriptlet sync server.  It fields TCP requests from RiScriptletSyncClients
 * and acts to synchronize multiple clients at sync points.
 * 
 * @author Steve Arendt
 */
public class RiScriptletSyncServer extends Thread
{
    private static final Logger m_log = Logger.getLogger(RiScriptletSyncServer.class);

    private ServerSocket m_svrSocket;
    private int m_svrPort;
    
    // array of client conns -- the array index is the clientId -- the array in initialized when the
    // first client signs on and gives the expected number of clients
    private ClientConnectionThread[] m_clientThreads = null;
    
    // array of sync points that the clients have currently passed, with -1 being the starting state
    // the array index is the clientId
    private int[] m_lastSyncPts = null;
    
    // m_syncData are the data that are passed back to all the clients when a sync point is satisfied.  this
    // is a way for client to pass data to each other through sync points.  any
    // client can populate the syncdata array when it enters a sync point, and all
    // the clients receive the data from the sync point.
    private byte[] m_syncData = new byte[0];
    
    
    public static void main(String[] args) 
    {
        RiScriptletSyncServer syncSvr = null;

        try
        {
            syncSvr = new RiScriptletSyncServer(12345);
            syncSvr.start();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public RiScriptletSyncServer(int svrPort)
        throws IOException
    {
        m_svrPort = svrPort;
        m_svrSocket = new ServerSocket(m_svrPort);
    }
    
    public void run()
    {
        while (true) 
        {
            Socket connSocket = null;
            
            try 
            {
                if (m_log.isInfoEnabled())
                {
                    m_log.info("Accepting connections...");
                }
                
                connSocket = m_svrSocket.accept();
                ClientConnectionThread clientConnThread = new ClientConnectionThread (connSocket);
                clientConnThread.start();
            }
            catch (IOException ex) 
            {
                if (m_log.isErrorEnabled())
                {
                    m_log.error("Error in sync interface", ex);
                }
                
                try
                {
                    connSocket.close();
                }
                catch (IOException exx)
                {
                    // discard
                }
            }
        }                
    }
    
    public void exit ()
    {
        try
        {
            // close svr socket
            m_svrSocket.close();
        }
        catch (IOException ex)
        {
            // discard
        }
        
        // wait for thread to exit
        try
        {
            if (m_log.isInfoEnabled())
            {
                m_log.info("RiScriptletSyncServer: Calling join");
            }
            this.join();
            if (m_log.isInfoEnabled())
            {
                m_log.info("RiScriptletSyncServer: Exiting join");
            }
        }
        catch (InterruptedException ex)
        {
            // ignore
        }
        
        // exit client conns, exit call will wait for thread to exit
        for (int i=0; i<m_clientThreads.length; i++)
        {
            m_clientThreads[i].exit();
        }
        
    }
    
    private void initClientData(int expectedNumClients)
    {
        m_clientThreads = new ClientConnectionThread[expectedNumClients];
        
        m_lastSyncPts = new int[expectedNumClients];
        for (int i=0; i<expectedNumClients; i++)
        {
            m_lastSyncPts[i] = -1;
        }
    }
    
    private boolean checkSyncPoints(byte syncPt)
    {
        boolean allSynced = true;
        
        if (m_log.isInfoEnabled())
        {
            m_log.info("checkSyncPoints:  " + syncPt);
        }
        
        for (int i=0; i<m_lastSyncPts.length; i++)
        {
            if (m_lastSyncPts[i] > syncPt)
            {
                if (m_log.isErrorEnabled())
                {
                    m_log.error("checkSyncPoints: error");
                }
                
                // error
                return true;
            }
            
            if (m_lastSyncPts[i] < syncPt)
            {
                if (m_log.isInfoEnabled())
                {
                    m_log.info("checkSyncPoints: allSynced = false");
                }
                allSynced = false;
                break;
            }
        }
        
        if (allSynced)
        {
            if (m_log.isInfoEnabled())
            {
                m_log.info("checkSyncPoints: sending reply");
            }
            
            // send sync packet to all -- payload will contain syncId
            for (int i=0; i<m_clientThreads.length; i++)
            {
                RiScriptletSyncPacket packet = RiScriptletSyncPacket.formatSyncPacket((byte) i /* clientID */, syncPt, m_syncData);
                try
                {
                    m_clientThreads[i].send(packet);
                }
                catch (Exception ex)
                {
                    if (m_log.isErrorEnabled())
                    {
                        m_log.error("Error sending sync reply to client " + i + ": ", ex);
                    }
                }
            }
            
            // reset syncData field for use in next sync point
            m_syncData = new byte[0];
        }
        
        return false;
    }
    
    private class ClientConnectionThread extends Thread
    {
        RiScriptletSyncConn m_syncConn;
        
        public ClientConnectionThread (Socket connSocket)
            throws IOException
        {
            m_syncConn = new RiScriptletSyncConn(connSocket);
        }
        
        public RiScriptletSyncConn getConn()
        {
            return m_syncConn;
        }
        
        public void exit()
        {
            // closing the socket will get us out of the loop in the run method
            m_syncConn.closeConn();
            
            // wait for thread to exit
            try
            {
                if (m_log.isInfoEnabled())
                {
                    m_log.info("ClientConnectionThread: Calling join");
                }
                this.join();
                if (m_log.isInfoEnabled())
                {
                    m_log.info("ClientConnectionThread: Exiting join");
                }
            }
            catch (InterruptedException ex)
            {
                // ignore
            }
        }
        
        public void send(RiScriptletSyncPacket packet)
            throws Exception
        {
            m_syncConn.send(packet);
        }
        
        public String toString()
        {
            return m_syncConn.toString();
        }
        
        public void run()
        {
            if (m_log.isInfoEnabled())
            {
                m_log.info("ClientCommThread: run");
            }
            
            while (true)
            {
                try
                {
                    RiScriptletSyncPacket recvPacket = m_syncConn.receive(-1 /* Infinite timeout */);
                    
                    if (recvPacket != null)
                    {
                        RiScriptletSyncPacket sendPacket = processPacket(recvPacket);
                        
                        if (sendPacket != null)
                        {
                            m_syncConn.send(sendPacket);
                        }
                    }
                }
                catch (Exception ex)
                {
                    m_syncConn.closeConn();
                    if (m_log.isErrorEnabled())
                    {
                        m_log.error ("Error in sync server thread.", ex);
                    }
                    break;
                }
            }
        }
    }

    private RiScriptletSyncPacket processPacket(RiScriptletSyncPacket packet)
    {
        byte commandTag = packet.getCommandTag();
        byte[] payload = packet.getPayload();
        
        RiScriptletSyncPacket replyPacket = null;
        
        if (m_log.isInfoEnabled())
        {
            m_log.info("processPacket:  " + packet.toString());
        }
        
        switch (commandTag)
        {
            case RiScriptletSyncPacket.CMD_REGISTER:
            {
                byte clientId = payload[0];
                byte expectedNumClients = payload[1];
                
                if (m_log.isInfoEnabled())
                {
                    m_log.info("Received CMD_REGISTER: clientId = " + clientId + 
                            ", expectedNumClients = " + expectedNumClients);
                }
                
                if (m_clientThreads == null)
                {
                    initClientData(expectedNumClients);
                }
                
                // check that clientId is less that expectedNumClients
                if (clientId >= m_clientThreads.length)
                {
                    replyPacket = RiScriptletSyncPacket.formatRegisterPacket(clientId, expectedNumClients, RiScriptletSyncPacket.ERROR_INVALID_CLIENT_ID);
                }
                else
                {                
                    m_clientThreads[clientId] = (ClientConnectionThread)Thread.currentThread();
                    
                    replyPacket = RiScriptletSyncPacket.formatRegisterPacket(clientId, expectedNumClients);
                }
                
                break;
            }
            case RiScriptletSyncPacket.CMD_UNREGISTER:
            {
                byte clientId = payload[0];
                
                if (m_log.isInfoEnabled())
                {
                    m_log.info("Received CMD_UNREGISTER: clientId = " + clientId);
                }
                
                // check that clientId is less that expectedNumClients
                if (clientId >= m_clientThreads.length)
                {
                    replyPacket = RiScriptletSyncPacket.formatUnregisterPacket(clientId, RiScriptletSyncPacket.ERROR_INVALID_CLIENT_ID);
                }
                else
                {                
                    // do nothing
                    replyPacket = RiScriptletSyncPacket.formatUnregisterPacket(clientId);
                }
                
                break;
            }
            case RiScriptletSyncPacket.CMD_SYNC:
            {
                byte clientId = payload[0];
                byte syncPt = payload[1];
                byte[] syncData = new byte[0];
                if (payload.length > 2)
                {
                    syncData = new byte[payload.length - 2];
                    System.arraycopy(payload, 2, syncData, 0, payload.length - 2);
                }
                
                if (m_log.isInfoEnabled())
                {
                    m_log.info("Received CMD_SYNC: clientId = " + clientId + 
                            ", syncPt = " + syncPt + ", syncData.length = " + syncData.length);
                }
                
                // check that clientId is less that expectedNumClients
                if (clientId >= m_clientThreads.length)
                {
                    replyPacket = RiScriptletSyncPacket.formatSyncPacket(clientId, syncPt, syncData, RiScriptletSyncPacket.ERROR_INVALID_CLIENT_ID);
                }
                else if (syncData.length != 0 && m_syncData.length != 0)
                {
                    // error here if syncData already set by another client
                    replyPacket = RiScriptletSyncPacket.formatSyncPacket(clientId, syncPt, syncData, RiScriptletSyncPacket.ERROR_SYNC_DATA_ALREADY_SET);
                }
                else
                {                
                    m_lastSyncPts[clientId] = syncPt;
                    
                    // if there is syncData in this packet, set it in syncData field to be passed
                    // to other clients when sync is reached.
                    if (syncData.length != 0) 
                    {
                        m_syncData = syncData;
                    }
                    
                    if (checkSyncPoints (syncPt))
                    {
                        replyPacket = RiScriptletSyncPacket.formatSyncPacket(clientId, syncPt, syncData, RiScriptletSyncPacket.ERROR_INVALID_SYNC_PT);
                    }
                    else
                    {
                        replyPacket = null;  // sync replies will be sent back as a batch
                    }
                }
                
                
                break;
            }
            default:
                if (m_log.isErrorEnabled())
                {
                    m_log.error("Received UNKNOWN packet type " + commandTag);
                }
                
                replyPacket = new RiScriptletSyncPacket(commandTag,
                    new byte[0], RiScriptletSyncPacket.ERROR_INVALID_CMD);  
        }
        
        if (replyPacket != null)
        {
            if (m_log.isInfoEnabled())
            {
                m_log.info("processPacket:  replying with packet: " + replyPacket.toString());
            }
        }
        
        return replyPacket;
    }
    
    
}
