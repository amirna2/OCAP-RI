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
package org.cablelabs.xlet.TCPEchoTest;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Exception;
import java.io.IOException;

/**
 * TCPEchoServer listens on a port for TCP clients to connect and send a message
 * that it simply echos back. This server will keep the connection open and echo
 * messages until the client drops the connection. Each connection is run in its
 * own thread so that multiple clients can use this service simultaneously. The
 * TCPEchoServer requires a listening port passed in as a command line argument.
 */
public class TCPEchoServer
{
    final static int TCPECHOPORT = 7; // Need to run as root to use the
                                      // well-known

    // Echo port. This port must be changed
    // with a command line arg.

    public static void main(String args[])
    {

        int port = TCPECHOPORT;
        ServerSocket server_socket = null;

        try
        {
            port = Integer.parseInt(args[0]);
        }
        catch (Exception e)
        {
            System.out.println("Usage: TCPEchoServer <listening port>");
            System.exit(-1);
        }

        try
        {
            server_socket = new ServerSocket(port);
            System.out.println("TCPEchoServer Server waiting for client on port " + server_socket.getLocalPort());
        }
        catch (IOException e)
        {
            System.out.println("TCPEchoServer failed to create ServerSocket" + e);
            System.exit(-1);
        }

        while (true)
        {
            try
            {
                Socket socket = server_socket.accept();
                System.out.println("TCPEchoServer New connection accepted " + socket.getInetAddress() + ":"
                        + socket.getPort());

                // Start a thread for echoing to this client
                Thread tcpecho = new Thread(new TCPEcho(socket));
                tcpecho.start();
            }
            catch (Exception e)
            {
                System.out.println("TCPEchoServer socket creation failed" + e);
            }
        }

    }
}

/**
 * TCPEcho gets the Input and Output streams for the socket established for one
 * echo client. Reads the input stream and immediately writes it out to the
 * client.
 * 
 * TCPEcho implements Runnable so that each echo client runs in a dedicated
 * thread.
 * 
 */
class TCPEcho implements Runnable
{
    InputStream in;

    OutputStream out;

    Socket socket;

    byte[] buffer = new byte[1024];

    TCPEcho(Socket socket)
    {
        this.socket = socket;
    }

    public void run()
    {
        System.out.println("TCPEcho New client, socket, and thread");
        try
        {
            socket.setSoTimeout(1000);
            in = socket.getInputStream();
            out = socket.getOutputStream();
        }
        catch (IOException e)
        {
            System.out.println("TCPEcho read/write failed" + e);
            return;
        }

        // echo server infinite loop
        // read one buffer, echo it back, and start over
        while (true)
        {
            try
            {
                int bytes = in.read(buffer);
                if (bytes == -1)
                {
                    break;
                }
                out.write(buffer);
                System.out.println("TCPEchoServer echoed " + bytes + " bytes");
            }
            catch (IOException e)
            {
                System.out.println("TCPEcho read/write failed\n" + e);
                break;
            }
        }

        // connection closed by client
        try
        {
            socket.close();
            System.out.println("TCPEcho Connection closed by client");
        }
        catch (IOException e)
        {
            System.out.println("TCPEcho IOException" + e);
        }
    }
}
