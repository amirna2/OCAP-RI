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
package org.cablelabs.xlet.UDPEchoTest;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.*;

/**
 * UDPEchoServer listens on a port for UDP clients to send messages that it
 * simple echos back. UDPEchoServer requires the client to pass in the reply ip
 * address and port as well as the listening port as command line arguments.
 */
public class UDPEchoServer
{
    int clientport;

    int serverport;

    InetAddress clientip = null;

    UDPEchoServer(InetAddress clientip, int clientport, int serverport)
    {
        this.clientip = clientip;
        this.clientport = clientport;
        this.serverport = serverport;
    }

    public static void main(String[] args)
    {
        InetAddress clientiparg = null;
        int clientportarg = 0;
        int serverportarg = 0;

        System.out.println("UDPEchoServer main");

        // Get args for UDP Performance tests
        if (args.length < 3)
        {
            System.out.println("Usage: UDPEchoServer <client ip address> <client port> <server port>");
            System.exit(-1);
        }

        // If we have a 4th arg, use it to direct sysout to a file
        if (args.length == 4)
        {
            try
            {
                File outFile = new File(args[3]);
                if (outFile.exists())
                {
                    outFile.delete();
                }
                FileOutputStream fileStream = new FileOutputStream(outFile);
                PrintStream prtStream = new PrintStream(fileStream, true);
                System.setOut(prtStream);
                System.setErr(prtStream);
            }
            catch (IOException ioe)
            {
                // try writing to syserr, but probably won't work
                System.err.println("Unable to access output file " + args[3] + " Exception: " + ioe.getMessage());
            }
            catch (SecurityException se)
            {
                // try writing to syserr, but probably won't work
                System.err.println("Unable to redirect output to " + args[3] + " Exception: " + se.getMessage());
            }
        }

        // Get the client ip address from args[0]
        try
        {
            clientiparg = InetAddress.getByName(args[0]);
            System.out.println("UDPEchoServer client ip=" + clientiparg);
        }
        catch (UnknownHostException e)
        {
            System.out.println("Bad client address = " + e.getMessage());
            System.exit(-1);
        }

        // Get the client port from args[1]
        try
        {
            clientportarg = Integer.parseInt(args[1]);
            System.out.println("UDPEchoServer client port=" + clientportarg);
        }
        catch (NumberFormatException e)
        {
            System.out.println("UDPEchoServer bad client port number");
            System.exit(-1);
        }

        try
        {
            serverportarg = Integer.parseInt(args[2]);
            System.out.println("UDPEchoServer server port=" + serverportarg);
        }
        catch (NumberFormatException e)
        {
            System.out.println("UDPEchoServer bad port number");
            System.exit(-1);
        }

        UDPEchoServer echoserver = new UDPEchoServer(clientiparg, clientportarg, serverportarg);
        echoserver.startEchoServer();
    }

    void startEchoServer()
    {
        DatagramSocket rcvsocket = null;
        DatagramSocket sendsocket = null;

        try
        {
            System.out.println("UDPEchoServer creating send and receive sockets");
            rcvsocket = new DatagramSocket(serverport);
            sendsocket = new DatagramSocket();
        }
        catch (SocketException e)
        {
            System.out.println("UDPEchoServer socket exception" + e);
            System.exit(-1);
        }

        // Receive and echo packets forever
        while (true)
        {
            System.out.println("UDPEchoServer write/read loop");
            byte[] buffer = new byte[1024];
            try
            {
                DatagramPacket rcvpacket = new DatagramPacket(buffer, buffer.length);
                rcvsocket.receive(rcvpacket);
                String pktStr = new String(rcvpacket.getData());
                System.out.println("UDPEchoServer data=" + pktStr);
                System.out.println("UDPEchoServer data length=" + buffer.length);
                if ((pktStr.trim()).equalsIgnoreCase("Dielikeadog!!!"))
                {
                    break;
                }
                DatagramPacket replypacket = mkReplyPacket(rcvpacket);
                sendsocket.send(replypacket);
                System.out.println("UDPEchoServer echo done");
            }
            catch (IOException e)
            {
                System.out.println("UDPEchoServer datagram send/receive exception" + e);
                e.printStackTrace();
            }
        }
    }

    // Make the reply packet from the data in the packet received
    DatagramPacket mkReplyPacket(DatagramPacket packet)
    {
        byte[] data = packet.getData();
        int length = packet.getLength();
        return new DatagramPacket(data, length, clientip, clientport);
    }
}
