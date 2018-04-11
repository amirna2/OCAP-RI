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

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.InterruptedException;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;

public class AutoTestDone
{
    public static void main(String[] args) throws Exception
    {
        autotestserver = defaultServer;
        autotestport = defaultPort;
        autotestmessage = defaultMessage;
        if (0 == args.length)
        {
            System.out.println("AutoTestDone - server, port and message not specified, using default values");
        }
        else if (1 == args.length)
        {
            autotestserver = args[0];
            System.out.println("AutoTestDone - port and message not specified, using default values");
        }
        else if (2 == args.length)
        {
            autotestserver = args[0];
            autotestport = args[1];
            System.out.println("AutoTestDone - message not specified, using default value");
        }
        else
        {
            autotestserver = args[0];
            autotestport = args[1];
            autotestmessage = args[2];
        }
        if (args.length > 3)
        {
            System.out.println("AutoTestDone - extra command line args ignored");
        }
        System.out.println("AutoTestDone - sending '" + autotestmessage + "' to " + autotestserver + " at port "
                + autotestport);

        try
        {
            sender = new UDPLogger(autotestserver, Integer.parseInt(autotestport));
            if (sender != null)
            {
                sender.send(autotestmessage);
                sender.close();
            }
            else
            {
                System.out.println("AutoTestDone - new UDPLogger() returned null");
            }
        }
        catch (SocketException e)
        {
            System.out.println("AutoTestDone - socket exception");
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("AutoTestDone - illegal arg");
            e.printStackTrace();
        }
    }

    private final static String defaultServer = "localhost"; // default
                                                             // destination if
                                                             // not specified on
                                                             // command line

    private final static String defaultPort = "8031"; // default port # if not
                                                      // specified on command
                                                      // line

    private final static String defaultMessage = "AutoTestDone"; // default
                                                                 // message if
                                                                 // not
                                                                 // specified on
                                                                 // command line

    private static UDPLogger sender = null;

    private static String autotestserver;

    private static String autotestport;

    private static String autotestmessage;
}
