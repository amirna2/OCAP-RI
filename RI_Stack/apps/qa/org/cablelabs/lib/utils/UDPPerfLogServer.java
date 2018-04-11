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
package org.cablelabs.lib.utils;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.FileWriter;
import java.io.IOException;

/**
 * UDPPerfLogServer runs on the network at the Headend. It listens on a UDP port
 * for performance log messages that it writes to a log file. The listening port
 * must be provided in the args passed to the constructor. The log file name may
 * also be specified in the args. A default log file name will be used if none
 * is provided.
 */
public class UDPPerfLogServer
{
    private int port = 0;

    private String logFileName = new String("perflog.txt");

    private PerfLog perflog;

    private DatagramSocket socket = null;

    private boolean noise = true;

    private int socktmo = 600000; // set default 10 minute timeout on receive()

    public UDPPerfLogServer(String[] args)
    {
        port = Integer.parseInt(args[0]);
        if (args.length >= 2)
        {
            logFileName = args[1];
        }
        if (args.length > 2)
        {
            noise = args[2].equals("quiet") ? false : true;
        }
        if (args.length > 3)
        {
            socktmo = Integer.parseInt(args[3]);
        }
        perflog = new PerfLog(logFileName);
        System.out.println("UDPPerfLogServer port=" + port + " log file=" + logFileName + " quiet = " + !noise
                + " timeout = " + socktmo);
    }

    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.out.println("Usage: UDPPerfLogServer <port> <log filename>");
            System.exit(-1);
        }
        UDPPerfLogServer server = new UDPPerfLogServer(args);
        server.execute();
    }

    public void execute()
    {
        try
        {
            System.out.println("UDPPerfLogServer port=" + port);
            socket = new DatagramSocket(port);
            byte[] buffer = new byte[1024];
            DatagramPacket in = new DatagramPacket(buffer, 1024);
            socket.setSoTimeout(socktmo);
            while (true)
            {
                // System.out.println("UDPPerfLogServer waiting for packet");
                try
                {
                    socket.receive(in);
                }
                catch (java.io.InterruptedIOException e)
                // catch (SocketTimeoutException e)
                {
                    System.out.println("\nUDPPerfLogServer timed out waiting for packet " + e);
                    perflog.write("\nUDPPerfLogServer timed out waiting for packet\n", noise);
                    break;
                }
                String logmsg = new String(buffer, 0, in.getLength());
                if (noise)
                {
                    System.out.println("UDPPerfLogServer received " + logmsg);
                }
                perflog.write(logmsg, noise);
            }
        }
        catch (Exception e)
        {
            System.out.println("UDPPerfLogServer exception " + e);
        }
        finally
        {
            try
            {
                socket.close();
                perflog.close();
            }
            catch (Exception e)
            {
                System.out.println("UDPPerfLogServer close failed " + e);
            }
        }
    }
}

/**
 * PerfLog writes performance data to a file. The file name is specified as an
 * argument to the constructor. The file will be created if it does not exist.
 * Performance log messages will be appended to the file if it does exist.
 */
class PerfLog
{
    private FileWriter fwriter = null;

    PerfLog(String filename)
    {
        System.out.println("PerfLog constructor filename=" + filename);
        try
        {
            fwriter = new FileWriter(filename, true);
            fwriter.write("PerfLog: starting \n");
        }
        catch (IOException e)
        {
            System.out.println("PerfLog failed to create writer");
        }
    }

    void write(String msg, boolean noise)
    {
        if (noise)
        {
            System.out.println("PerfLog write msg=" + msg);
        }
        if (fwriter != null)
        {
            try
            {
                fwriter.write(msg + "\n");
                fwriter.flush();
            }
            catch (IOException e)
            {
                System.out.println("PerfLog write failed");
            }

        }
    }

    void close()
    {
        System.out.println("PerfLog closing");
        try
        {
            fwriter.flush();
            fwriter.close();
        }
        catch (IOException ex)
        {
            System.out.println("PerfLog error closing log file");
        }
    }
}
