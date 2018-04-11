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

/**
 * AutoTestDoneXlet sends a UDP message to an ant task when the auotmated
 * testing is done. This Xlet is designed to be the last automated Xlet run in
 * the XletDriver.xml file.
 */
public class AutoTestDoneXlet implements Xlet, Driveable
{
    private XletContext ctx;

    // Objects used to integrate with AutoXlet testing framework
    private AutoXletClient axc;

    private Test test;

    private Logger log;

    private String autotestserver;

    private Integer autotestport;

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("AutoTestDoneXlet initXlet");
        this.ctx = ctx;

        // Initialize AutoXlet framework client and grab logger and test objects
        axc = new AutoXletClient(this, ctx);
        test = axc.getTest();

        // If we have successfully connected, initialize our logger from the
        // AutoXletClient, else use a default constructed XletLogger which will
        // send all logging output to standard out.
        if (axc.isConnected())
            log = axc.getLogger();
        else
            log = new XletLogger();

        try
        {
            ArgParser xletArgs = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));

            String configFile = xletArgs.getStringArg("config_file");
            FileInputStream fis = new FileInputStream(configFile);
            ArgParser fopts = new ArgParser(fis);

            autotestserver = fopts.getStringArg("AutoTestServer");
            autotestport = fopts.getIntegerArg("AutoTestPort");
            if ((autotestserver != null) && (autotestport != null))
            {
                log.log("AutoTestDoneXlet args: autotest server:port=" + autotestserver + ":" + autotestport.intValue());
                System.out.println("AutoTestDoneXlet args: autotest server:port=" + autotestserver + ":"
                        + autotestport.intValue());
            }
        }
        catch (Exception e)
        {
            log.log("AutoTestDoneXlet error getting args");
            System.out.println("AutoTestDoneXlet error getting args");
            e.printStackTrace();
            throw new XletStateChangeException("AutoTestDoneXlet initXlet error reading args");
        }
        System.out.println("AutoTestDoneXlet initXlet done");
    }

    public void startXlet() throws XletStateChangeException
    {
        log.log("AutoTestDoneXlet startXlet");
        System.out.println("AutoTestDoneXlet startXlet");
        UDPLogger sender = null;
        try
        {
            if (autotestserver != null)
            {
                if (autotestport != null)
                {
                    sender = new UDPLogger(autotestserver, autotestport.intValue());
                    if (sender != null)
                        sender.send("AutoTestDone");
                    else
                        log.log("AutoTestDoneXlet sender is null");
                }
                else
                {
                    log.log("AutoTestDoneXlet autotestport is null");
                }
            }
            else
            {
                log.log("AutoTestDoneXlet autotestserver is null");
            }
        }
        catch (SocketException e)
        {
            log.log("AutoTestDoneXlet socket exception send failed");
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            log.log("AutoTestDoneXlet illegal arg send failed");
            e.printStackTrace();
        }
        if (sender != null) sender.close();

        test.assertTrue(true);

        // This Xlet is done
        // throw new
        // XletStateChangeException("AutoTestDoneXlet startXlet done");
        System.out.println("AutoTestDoneXlet startXlet done");
    }

    public void pauseXlet()
    {
        log.log("AutoTestDoneXlet pauseXlet");
    }

    public void destroyXlet(boolean b) throws XletStateChangeException
    {
        log.log("AutoTestDoneXlet destroyXlet");
        System.out.println("AutoTestDoneXlet destroyXlet - AutoXlet is Done!");
        ctx.notifyDestroyed();
    }

    public void dispatchEvent(KeyEvent arg0, boolean arg1, int arg2) throws RemoteException
    {
        log.log("AutoTestDoneXlet event received, but no events are needed by this Xlet");
    }
}
