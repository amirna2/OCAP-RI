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

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.TimeZone;
import org.cablelabs.lib.utils.ArgParser;

/**
 * UDPPerfReporter is a utility used by Xlets to send performance log messages
 * to the UDPPerfLogServer. The Xlet must provide the name of the config file
 * containing the ip address and port of the UDPPerfLogServer as an argument to
 * the constructor.
 * 
 * Performance log message format is a comma separated string of the following
 * format: "TEXT XLET, current time, Xlet name, start time stamp in
 * milliseconds, duration, 1/0 (for pass/fail), iteration". The send() method
 * inserts the first two fields. The UDPPerfReporter user must provide the rest.
 */
public class UDPPerfReporter
{
    public static final int PASS = 1;

    public static final int FAIL = 0;

    public static final String COMMA = new String(",");

    private int destport = 0;

    private InetAddress ipdest = null;

    private InetAddress senderip = null;

    private String senderipstr = null;

    private DatagramSocket sender = null;

    public UDPPerfReporter(String configFile)
    {
        System.out.println("UDPPerfReporter constructor");
        FileInputStream cfile = null;
        try
        {
            System.out.println("UDPPerfReporter parsing " + configFile);
            cfile = new FileInputStream(configFile);
            ArgParser fileOpts = new ArgParser(cfile);
            if (destport == 0) destport = fileOpts.getIntArg("PerfReporterPort");
            String ipdestArg = fileOpts.getStringArg("PerfReporter");
            System.out.println("UDPPerfReporter ipdestArg=" + ipdestArg);

            // If the value of PerfReporter is the empty string or null
            // assume performance logging is turned off.
            if (ipdestArg == null || ipdestArg.equals(""))
            {
                System.out.println("UDPPerfReporter is off");
            }
            else
            {
                // Only set ipdest if it's not already set
                if (ipdest == null) ipdest = getIpFromProperty(ipdestArg);
            }

            // If an invalid host name or ip address is given in the config file
            // assume that performance logging is turned off.
            if (ipdest != null)
            {
                System.out.println("UDPPerfReporter port=" + destport + " ip=" + ipdest);
            }
        }
        catch (IOException ioe)
        {
            System.out.println("UDPPerfReporter io error getting args from config file");
            ioe.printStackTrace();
        }
        catch (Exception e)
        {
            System.out.println("UDPPerfReporter error getting args from config file");
            e.printStackTrace();
        }
        try
        {
            cfile.close();
        }
        catch (IOException e)
        {
            System.out.println("UDPPerfReporter config file close failed");
            e.printStackTrace();
        }

        try
        {
            if (senderip == null) senderip = InetAddress.getLocalHost();
            if (senderipstr == null) senderipstr = senderip.getHostAddress();
        }
        catch (UnknownHostException e)
        {
            System.out.println("UDPPerfReporter exception getting localhost");
            senderipstr = "";
        }

        try
        {
            // Create a UDP socket if it's not already created and
            // logging is turned on
            if ((sender == null) && (ipdest != null)) sender = new DatagramSocket();
        }
        catch (SocketException e)
        {
            System.out.println("UDPPerfReporter socket exception creating socket");
        }
    }

    public int getDestPort()
    {
        return destport;
    }

    public void setDestPort(int destport)
    {
        this.destport = destport;
    }

    public InetAddress getIpDest()
    {
        return ipdest;
    }

    public void setIpDest(InetAddress ipdest)
    {
        this.ipdest = ipdest;
    }

    /**
     * getIpFromProperty returns the IP address that correspond to the value of
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
    public static InetAddress getIpFromProperty(String host)
    {
        try
        {
            if ((host == null) || (host.equals("")) || (host.toLowerCase().equals("localhost")))
            {
                return InetAddress.getLocalHost();
            }
            return InetAddress.getByName(host);
        }
        catch (UnknownHostException e)
        {
            System.out.println("UDPPerfReporter: getIPDestFromProperty unknown host=" + host);
            return null;
        }
    }

    /*
     * This send method sends a UDP formatted message to the performance log
     * server to be written into the log file by calling the unformatted send
     * method.
     * 
     * Performance log message format is a comma separated string of the
     * following format: "TEXT XLET, current time, Xlet name, start time stamp
     * (milliseconds), duration (milliseconds), PASS/FAIL, iteration". The
     * send() method inserts the first two fields. The UDPPerfReporter user must
     * provide the others.
     */
    public void send(String xletname, long start, long duration, int result, int iteration)
    {
        // If no log server is configured, do nothing
        if ((ipdest == null) || (sender == null))
        {
            /*
             * if (ipdest == null) System.out.println("send: ipdest is null");
             * else if (sender == null)
             * System.out.println("send: sender is null");
             */
            return;
        }
        send(new String(xletname + COMMA + start + COMMA + duration + COMMA + result + COMMA + iteration));
    }

    /*
     * This send method is the same as the version that takes 5 arguments, but
     * adds an addition 6th String argument that the user can use for additional
     * data.
     */
    public void send(String xletname, long start, long duration, int result, int iteration, String otherData)
    {
        // If no log server is configured, do nothing
        if ((ipdest == null) || (sender == null))
        {
            if (ipdest == null)
                System.out.println("send: ipdest is null");
            else if (sender == null) System.out.println("send: sender is null");
            return;
        }
        send(new String(xletname + COMMA + start + COMMA + duration + COMMA + result + COMMA + iteration + COMMA
                + otherData));
    }

    /*
     * This send method sends an unformatted UDP message to the performance log
     * server to be written into the log file.
     */
    public void send(String message)
    {
        // If no log server is configured, do nothing
        if ((ipdest == null) || (sender == null))
        {
            return;
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-6"));
        String datestr = new String("" + cal.get(Calendar.YEAR) + (cal.get(Calendar.MONTH) + 1)
                + cal.get(Calendar.DAY_OF_MONTH) + cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE)
                + cal.get(Calendar.SECOND));
        send((new String("TEST XLET," + datestr + COMMA + senderipstr + COMMA + message)).getBytes());
    }

    private void send(byte[] message)
    {
        try
        {
            String smessage = new String(message);
            // System.out.println("UDPPerfReporter creating packet "+ smessage);
            DatagramPacket packet = new DatagramPacket(message, message.length, ipdest, destport);
            // System.out.println("UDPPerfReporter packet created "+ smessage);
            if (sender == null)
            {
                System.out.println("UDPPerfReporter sender is null");
            }
            else
            {
                sender.send(packet);
                // System.out.println("UDPPerfReporter sent " + smessage);
            }
        }
        catch (IOException e)
        {
            System.out.println("UDPPerfReporter io exception " + e);
        }
        catch (Exception e)
        {
            System.out.println("UDPPerfReporter exception " + e);
        }
    }

    public void close()
    {
        try
        {
            if (sender != null) sender.close();
        }
        catch (Exception e)
        {
            System.out.println("UDPPerfReporter socket close failed" + e);
            e.printStackTrace();
        }
    }
}
