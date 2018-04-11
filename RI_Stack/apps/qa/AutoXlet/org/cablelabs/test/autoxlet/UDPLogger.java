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

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.TimeZone;
import java.io.IOException;
import java.io.OutputStream;

/**
 * UDPLogger is a utility used by Xlets to send performance log messages to the
 * UDPLogServer. The Xlet must provide the name of the config file containing
 * the ip address and port of the UDPPerfLogServer as an argument to the
 * constructor.
 * 
 * Performance log message format is a comma separated string of the following
 * format: "TEXT XLET, current time, Xlet name, start time stamp in
 * milliseconds, duration, 1/0 (for pass/fail), iteration". The send() method
 * inserts the first two fields. The UDPPerfReporter user must provide the rest.
 */
public class UDPLogger extends OutputStream
{
    /**
     * Create a UDP log client. Log messages will be written to the specified
     * server on the given port
     * 
     * @param server
     *            the IP address or hostname of the log server
     * @param port
     *            the server port
     * @throws IllegalArgumentException
     *             if server IP or hostname is invalid
     * @throws SocketException
     *             if client was unable to connect to log server
     */
    public UDPLogger(String server, int port) throws IllegalArgumentException, SocketException
    {
        System.out.println("UDPLogger initializing: " + server + ":" + port);

        portdest = port;
        ipdest = getIPFromString(server);

        if (ipdest == null)
        {
            throw new IllegalArgumentException();
        }

        // Create a UDP socket
        sender = new DatagramSocket();
    }

    /**
     * getIPFromString returns the IP address that correspond to the value of
     * the String argument to the method. The argument may be 1. a machine name
     * 2. the ip address in dotted decimal form as a string 3. null 4. the empty
     * string 5. the word "localhost"
     * 
     * If the argument is 1 or 2, the ip address of the machine indicated is
     * returned. If the argument is 3, 4, or 5 the ip address of localhost is
     * returned.
     * 
     * @param host
     *            A String that contains a host name.
     * 
     * @return An InetAddress that corresponds to the given name.
     * 
     * @exception UnknownHostException
     *                If host cannot be resolved.
     */
    public static InetAddress getIPFromString(String host)
    {
        try
        {
            if (host == null || host.equals("") || host.toLowerCase().equals("localhost"))
            {
                return InetAddress.getLocalHost();
            }
            return InetAddress.getByName(host);
        }
        catch (UnknownHostException e)
        {
            System.out.println("UDPLogger: unknown host -- " + host);
            return null;
        }
    }

    /**
     * Send a message to the log server
     * 
     * @param message
     *            the message to be sent
     */
    public void send(String message)
    {
        final int MAX_DATA_SIZE = 512;

        byte[] bMessage = message.getBytes();

        try
        {
            int index = 0;

            // Split this message into 512 byte chunks
            for (int i = 0; i < bMessage.length / MAX_DATA_SIZE; ++i)
            {
                sender.send(new DatagramPacket((new String(bMessage, index, MAX_DATA_SIZE)).getBytes(), MAX_DATA_SIZE,
                        ipdest, portdest));

                index += MAX_DATA_SIZE;
            }

            // Now send the remaining bytes
            int remainingBytes = bMessage.length % MAX_DATA_SIZE;
            if (remainingBytes != 0)
            {
                sender.send(new DatagramPacket((new String(bMessage, index, remainingBytes).getBytes()),
                        remainingBytes, ipdest, portdest));
            }
        }
        catch (IOException e)
        {
            System.out.println("Error sending UDP message! " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close()
    {
        try
        {
            sender.close();
        }
        catch (Exception e)
        {
            System.out.println("UDPLogger socket close failed" + e);
            e.printStackTrace();
        }
    }

    public void write(int b) throws IOException
    {
        byte[] barray = new byte[1];
        barray[0] = (byte) b;

        send(new String(barray));
    }

    public void write(byte[] data) throws IOException
    {
        send(new String(data));
    }

    public void write(byte[] data, int offset, int size) throws IOException
    {
        send(new String(data, offset, size));
    }

    public void flush() throws IOException
    {
    }

    private InetAddress ipdest = null;

    private int portdest = 0;

    private DatagramSocket sender = null;
}
