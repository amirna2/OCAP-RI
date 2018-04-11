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

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import org.havi.ui.HContainer;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.FillBuffer;
import org.cablelabs.lib.utils.UDPPerfReporter;
import org.cablelabs.test.autoxlet.*;

/**
 * UDPEchoClientXlet collects performance data on the time it takes to send a
 * UDP message from this Xlet to the HE and get a reply. The test assumes that
 * the UDPEchoServer is runing on the network at the HE. The tests are
 * configured by setting variables in the config.properties file. It logs the
 * performance results using the UDPPerfReporter, which requires that a
 * UPDPerfLogServer is setup to receive the performance counts.
 */
public class UDPEchoClientXlet extends HContainer implements Xlet, Driveable
{
    private static final String CONFIG_FILE = "config_file";

    private final int BUFFERSIZE = 256;

    private static final int FONT_SIZE = 16;

    private XletContext ctx;

    private String configFile = null;

    private UDPPerfReporter perflog;

    private InetAddress udpechoserver;

    private HScene scene;

    private String[] displaystr;

    private int x = 70;

    private int y = 40;

    private boolean started = false;

    private int udpechosendport;

    private int udpechorcvport;

    private int iterations;

    private int echoto;

    private FileInputStream cfile = null;

    private AutoXletClient axc = null;

    private Logger logger = null;

    private Test test = null;

    void initGUI()
    {
        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.setSize(640, 480);
        scene.setLayout(new BorderLayout());
        scene.setBackgroundMode(HScene.BACKGROUND_FILL);
        scene.setBackground(new Color(0, 0, 234).darker());
        scene.setForeground(new Color(234, 234, 234).darker());
        scene.setFont(new Font("SansSerif", Font.BOLD, FONT_SIZE));
        scene.add(this);
        scene.addNotify();
        scene.validate();

        scene.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                try
                {
                    destroyXlet(true);
                }
                catch (XletStateChangeException x)
                {
                }
            }
        });

        started = true;
    }

    public void paint(Graphics g)
    {
        FontMetrics fm = g.getFontMetrics();
        y = 40;
        for (int i = 0; i < displaystr.length; ++i)
        {
            if (displaystr[i] == null) break;
            g.drawString(displaystr[i], x, y);
            y = y + fm.getAscent() + 2;
        }
        super.paint(g);
    }

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("UDPEchoClientXlet initXlet");
        this.ctx = ctx;

        // Set up the AutoXlet mechanism and populate our local Test and
        // Logger references
        axc = new AutoXletClient(this, ctx);
        logger = axc.getLogger();
        test = axc.getTest();

        // Set up UDP performance reporting
        try
        {
            ArgParser xletArgs = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));
            configFile = xletArgs.getStringArg(CONFIG_FILE);
            perflog = new UDPPerfReporter(configFile);
        }
        catch (Exception e)
        {
            System.out.println("UDPEchoClientXlet error getting args from config file");
            e.printStackTrace();
            closeConfigFile();
            throw new XletStateChangeException(
                    "UDPEchoClientXlet initXlet error reading performance properties from config file");
        }

        // Get config parameters for UDP Performance tests
        try
        {
            cfile = new FileInputStream(configFile);
            ArgParser fileOpts = new ArgParser(cfile);
            String udpechoserverArg = fileOpts.getStringArg("UDPEchoServer");
            udpechoserver = UDPPerfReporter.getIpFromProperty(udpechoserverArg);
            udpechosendport = fileOpts.getIntArg("UDPEchoSendPort");
            udpechorcvport = fileOpts.getIntArg("UDPEchoReceivePort");
            iterations = fileOpts.getIntArg("UDPIterations");
            displaystr = new String[iterations];
            echoto = fileOpts.getIntArg("UDPEchoTimeout");
            System.out.println("UDPEchoClientXlet args: echo server=" + udpechoserver + ":" + udpechorcvport
                    + " destination port=" + udpechosendport);
        }
        catch (Exception e)
        {
            System.out.println("UDPEchoClientXlet failed to get config args " + e);
            closeConfigFile();
            throw new XletStateChangeException(
                    "UDPEchoClientXlet initXlet error reading UDP properties from config file");
        }
        closeConfigFile();
    }

    public void startXlet() throws XletStateChangeException
    {
        int offset = (int) '0';
        int failed = 0;

        if (!started)
        {
            initGUI();
        }
        displaystr = new String[iterations];
        scene.show();

        DatagramSocket sendsocket = null;
        DatagramSocket rcvsocket = null;
        try
        {
            sendsocket = new DatagramSocket();
            rcvsocket = new DatagramSocket(udpechorcvport);
            rcvsocket.setSoTimeout(echoto * 1000);
        }
        catch (Exception e)
        {
            System.out.println("UDPEchoClientXlet exception creating socket" + e);
            destroyXlet(true);
        }
        StringBuffer outbuffer = FillBuffer.getFullBuffer(BUFFERSIZE);
        for (int i = 0; i < iterations && failed < 10; ++i)
        {
            System.out.println("UDPEchoClientXlet write/read loop count=" + i);
            int len = outbuffer.length();
            outbuffer.setCharAt((len - 1), (char) (i % 10 + offset));
            byte[] outbytes = outbuffer.toString().getBytes();
            System.out.println("UDPEchoClientXlet output bytes=" + outbytes.length);
            DatagramPacket outpacket = new DatagramPacket(outbytes, outbytes.length, udpechoserver, udpechosendport);
            byte[] inbuffer = new byte[BUFFERSIZE];
            DatagramPacket inpacket = new DatagramPacket(inbuffer, BUFFERSIZE);
            try
            {
                long start = System.currentTimeMillis();
                sendsocket.send(outpacket);
                rcvsocket.receive(inpacket);
                long stop = System.currentTimeMillis();
                perflog.send("UDPEchoClientXlet", start, (stop - start), UDPPerfReporter.PASS, i);
                displaystr[i] = new String("UDPEcho start=" + start + " duration=" + (stop - start) + " PASS"
                        + " iteration=" + (i + 1));
                scene.repaint();
                failed = 0; // restart test failed counter after each successful
                            // test
            }
            catch (InterruptedIOException iioe)
            {
                perflog.send("UDPEchoClientXlet", 0, 0, UDPPerfReporter.FAIL, i);
                displaystr[i] = new String("UDPEcho start=0 duration=0 FAIL iteration=" + (i + 1));
                test.fail(displaystr[i]);
                if (!axc.isConnected()) logger.log(test.getTestResult());
                scene.repaint();
                System.out.println("UDPEchoClientXlet echo test receive timeout " + iioe);
                failed++;
            }
            catch (IOException ioe)
            {
                perflog.send("UDPEchoClientXlet", 0, 0, UDPPerfReporter.FAIL, i);
                System.out.println("UDPEchoClientXlet echo test failed " + ioe);
                failed++;
            }

            // Test that the echo was complete
            test.assertEquals("Number of bytes written and read differ, iteration = " + i, inpacket.getLength(), len);
        }
        // If 10 consecutive echo tests fail, quit trying
        if (failed >= 10)
        {
            System.out.println("UDPEchoClientXlet 10 UDP echo tests failed");
        }
        sendsocket.close();
        rcvsocket.close();

        if (!axc.isConnected()) logger.log(test.getTestResult());
    }

    public void pauseXlet()
    {
        scene.setVisible(false);
    }

    public void destroyXlet(boolean b) throws XletStateChangeException
    {
        try
        {
            perflog.close();
            scene.setVisible(false);
            HSceneFactory.getInstance().dispose(scene);
        }
        catch (Exception e)
        {
            System.out.println("UDPEchoClientXlet destroyXlet Exception closing socket");
        }
        ctx.notifyDestroyed();
    }

    public void dispatchEvent(KeyEvent arg0, boolean arg1, int arg2)
    {
        // Xlet does not use remote control events
    }

    void closeConfigFile()
    {
        try
        {
            cfile.close();
        }
        catch (IOException io)
        {
            System.out.println("UDPEchoClientXlet config file close failed " + io);
            io.printStackTrace();
        }
    }
}
