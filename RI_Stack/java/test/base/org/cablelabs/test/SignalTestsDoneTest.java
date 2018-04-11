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

/* 

 */

package org.cablelabs.test;

import org.cablelabs.impl.util.MPEEnv;
import junit.framework.*;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.IOException;

/**
 * SignalTestsDone sends a UDP message to an ant task when the auotmated testing
 * is done. This fake JUnit test should be added as the last test in the suite.
 */

// Fake test class

public class SignalTestsDoneTest extends TestCase
{
    public void testSignalTestsDone()
    {
        String doneServer = MPEEnv.getEnv("JunitDoneServer");
        int donePort = MPEEnv.getEnv("JunitDonePort", 8031);
        String JunitName = MPEEnv.getEnv("MainClassArgs.0", "Junit tests");

        System.out.println("\nSignalTestsDoneTest() - sending to " + doneServer + " at port " + donePort + "\n");

        if (doneServer != null)
        {
            try
            {
                NotifyDone(JunitName, doneServer, donePort);
            }
            catch (Exception e)
            {
                System.out.println("Error sending notification of JUnit completion : " + e.getMessage());
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("SignalTestsDoneTest() - JunitDoneServer is null");
        }
    }

    /*  ********** Boilerplate ************* */

    public SignalTestsDoneTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SignalTestsDoneTest.class);
        return suite;
    }

    /**
     * Send "Junit done" message to a server.
     * 
     * @param message
     *            message to send
     * @param server
     *            the IP address of the log server
     * @param port
     *            the server port
     * @throws IllegalArgumentException
     *             if server IP or hostname is invalid
     * @throws SocketException
     *             if client was unable to connect to log server
     */
    public void NotifyDone(String message, String server, int port) throws IllegalArgumentException, SocketException,
            UnknownHostException
    {
        byte[] bMessage = message.getBytes();
        InetAddress ipdest = InetAddress.getByName(server);
        DatagramSocket sender = new DatagramSocket();

        if (ipdest == null)
        {
            throw new IllegalArgumentException();
        }

        try
        {
            sender.send(new DatagramPacket(bMessage, bMessage.length, ipdest, port));
        }
        catch (IOException e)
        {
            System.out.println("Error sending UDP message! " + e.getMessage());
            e.printStackTrace();
        }
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
}
