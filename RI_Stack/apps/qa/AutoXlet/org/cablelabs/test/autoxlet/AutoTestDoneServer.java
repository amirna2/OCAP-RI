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
 * AutoTestDoneServer can run on the network at the Headend or simply on another
 * computer. It listens on a specified UDP port for an AutoTestDone message and
 * then exits. The listening port must be provided in the command line args. An
 * optional timeout value (in seconds) may also be specified on the command
 * line.
 * 
 * <pre>
 *  Usage:  AutoTestDoneServer <em>port</em> <em>timeout</em>
 * </pre>
 */
public class AutoTestDoneServer
{
    /**
     * Create a UDP log server which will listen for a packet on the given port.
     * 
     * @param port
     *            the port on which to listen for log packets
     * @throws IllegalArgumentException
     *             if the log file could not be opened
     */
    public AutoTestDoneServer(int port, int timeout) throws IOException
    {
        this.port = port;
        this.timeout = timeout;

        System.out.println("AutoTestDoneServer created!  Port = " + port + ", timeout = " + timeout);
    }

    /**
     * Begin listening for log packets
     */
    public void execute()
    {
        try
        {
            socket = new DatagramSocket(port);
            byte[] buffer = new byte[100];
            DatagramPacket in = new DatagramPacket(buffer, 100);
            int timeOutExpired = 0;
            if (timeout > 0)
            {
                socket.setSoTimeout(timeout * 1000);
            }
            try
            {
                socket.receive(in);
            }
            catch (java.io.InterruptedIOException to)
            {
                System.out.println("AutoTestDoneServer timed out waiting for test to finish");
                timeOutExpired = 1;
            }
            if (timeOutExpired == 0)
            {
                String logmsg = new String(buffer, 0, in.getLength());
                System.out.println("packet received=" + logmsg);
            }
        }
        catch (Exception e)
        {
            System.out.println("AutoTestDoneServer exception " + e);
        }
        finally
        {
            try
            {
                socket.close();
            }
            catch (Exception e)
            {
                System.out.println("AutoTestDoneServer close failed " + e);
            }
        }
    }

    public static void main(String[] args)
    {
        // Must have 1 or 2 args
        if ((args.length < 1) || (args.length > 2))
        {
            System.out.println("Usage: AutoTestDoneServer <port> <timeout>");
            System.exit(-1);
        }

        int port = 0;
        int timeout = 0;

        // Check for valid integer port number
        try
        {
            port = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException e)
        {
            System.out.println("AutoTestDoneServer: Invalid server port numnber!");
            System.exit(-1);
        }

        // Check for presence of optional timeout value
        if (args.length == 2)
        {
            // Check for valid integer timeout value.
            // Timeout value must be >= 0, 0 means no timeout, i.e., wait
            // forever
            try
            {
                timeout = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException e)
            {
                System.out.println("AutoTestDoneServer: Invalid timeout value, defaulting to no timeout");
                timeout = 0;
            }
            if (timeout < 0)
            {
                System.out.println("AutoTestDoneServer: Invalid negative timeout value, defaulting to no timeout");
                timeout = 0;
            }
        }
        // Attempt to start the server
        try
        {
            AutoTestDoneServer server = new AutoTestDoneServer(port, timeout);
            server.execute();
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }

    private int port = 0;

    private int timeout = 0;

    private DatagramSocket socket = null;

}
