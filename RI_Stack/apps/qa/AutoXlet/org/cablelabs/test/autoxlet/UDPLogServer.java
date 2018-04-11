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
package org.cablelabs.test.autoxlet;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.IllegalArgumentException;

/**
 * UDPLogServer can run on the network at the Headend or simply on another
 * computer. It listens on a specified UDP port for log messages that it writes
 * to a log file. The listening port and log filename must be provided in the
 * command line args.
 * 
 * <pre>
 *  Usage:  UDPLogServer <em>port</em> <em>filename</em>
 * </pre>
 */
public class UDPLogServer
{
    /**
     * Create a UDP log server which will listen for log packets on the given
     * port. Log messages will be written to the specified logfile.
     * 
     * @param port
     *            the port on which to listen for log packets
     * @param logfile
     *            the local file to which log messages will be written
     * @throws IllegalArgumentException
     *             if the log file could not be opened
     */
    public UDPLogServer(int port, String logfile) throws IOException
    {
        if (logfile == null) throw new IllegalArgumentException();

        this.port = port;

        fwriter = new FileWriter(logfile, false);

        System.out.println("UDPLogServer created!  Port = " + port + "   LogFile = " + logfile);
    }

    /**
     * Begin listening for log packets
     */
    public void execute()
    {
        try
        {
            socket = new DatagramSocket(port);
            byte[] buffer = new byte[576];
            DatagramPacket in = new DatagramPacket(buffer, 576);
            while (true)
            {
                socket.receive(in);
                String logmsg = new String(buffer, 0, in.getLength());
                fwriter.write(logmsg);
                fwriter.flush();
            }
        }
        catch (Exception e)
        {
            System.out.println("UDPLogServer exception " + e);
        }
        finally
        {
            try
            {
                socket.close();
                fwriter.close();
            }
            catch (Exception e)
            {
                System.out.println("UDPLogServer close failed " + e);
            }
        }
    }

    public static void main(String[] args)
    {
        // Must have 2 args
        if (args.length != 2)
        {
            System.out.println("Usage: UDPLogServer <port> <log filename>");
            System.exit(-1);
        }

        int port = 0;

        // Check for valid integer port number
        try
        {
            port = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException e)
        {
            System.out.println("UDPLogServer:  Invalid server port numnber!");
            System.exit(-1);
        }

        // Attempt to start the server
        try
        {
            UDPLogServer server = new UDPLogServer(port, args[1]);
            server.execute();
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }

    private int port = 0;

    private DatagramSocket socket = null;

    private FileWriter fwriter = null;

}
