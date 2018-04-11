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

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.Socket;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import org.havi.ui.HContainer;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.FillBuffer;
import org.cablelabs.lib.utils.UDPPerfReporter;
import org.cablelabs.test.autoxlet.*;

/**
 * TCPEchoClientXlet collects performance data on the time it takes to send a
 * TCP message from this Xlet to the HE and get a reply. The test assumes that
 * the TCPEchoServer is running on the network at the HE. The tests are
 * configured by setting variables in the config.properties file. It logs the
 * performance results using the UDPPerfReporter, which requires that a
 * UPDPerfLogServer is setup to receive the performance counts.
 * 
 * This client also logs test results. Tests fail if there is an I/O exception
 * on send or receive or if the count of bytes written does not match the count
 * of bytes received.
 */
public class TCPEchoClientXlet extends HContainer implements Xlet, Driveable
{
    private static final String CONFIG_FILE = "config_file";

    private static final int BUFFERSIZE = 1024;

    private static final int FONT_SIZE = 16;

    private XletContext ctx;

    private String configFile = null;

    private UDPPerfReporter perflog;

    private InetAddress tcpechoserver;

    private HScene scene;

    private String[] displaystr;

    private int x = 70;

    private int y = 40;

    private boolean started = false;

    private int tcpechoport;

    private int iterations;

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
        scene.setFont(new Font("SansSerif", 0, FONT_SIZE));
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
        System.out.println("TCPEchoClientXlet initXlet");
        this.ctx = ctx;

        // Set up the AutoXlet mechanism and populate our local Test and
        // Logger references
        axc = new AutoXletClient(this, ctx);
        logger = axc.getLogger();
        test = axc.getTest();

        // Set up TCP performance reporting
        try
        {
            ArgParser xletArgs = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));
            configFile = xletArgs.getStringArg(CONFIG_FILE);
            perflog = new UDPPerfReporter(configFile);
        }
        catch (Exception e)
        {
            System.out.println("TCPEchoClientXlet error getting args from config file");
            e.printStackTrace();
            throw new XletStateChangeException(
                    "TCPEchoClientXlet initXlet error reading performance properties from config file");
        }

        // Get config args for TCP Performance tests
        try
        {
            cfile = new FileInputStream(configFile);
            ArgParser fileOpts = new ArgParser(cfile);
            String tcpechoserverArg = fileOpts.getStringArg("TCPEchoServer");
            tcpechoserver = UDPPerfReporter.getIpFromProperty(tcpechoserverArg);
            tcpechoport = fileOpts.getIntArg("TCPEchoPort");
            iterations = fileOpts.getIntArg("TCPIterations");
            System.out.println("TCPEchoClientXlet args: echo server=" + tcpechoserver + ":" + tcpechoport
                    + " iterations=" + iterations);
        }
        catch (Exception e)
        {
            System.out.println("TCPEchoClientXlet failed to get config args " + e);
            closeConfigFile();
            throw new XletStateChangeException(
                    "TCPEchoClientXlet initXlet error reading TCP properties from config file");
        }
        closeConfigFile();
    }

    public void startXlet() throws XletStateChangeException
    {
        int numbytes = 0;
        int offset = (int) '0';
        Socket socket = null;
        InputStream sin = null;
        OutputStream sout = null;

        if (!started)
        {
            initGUI();
        }
        displaystr = new String[iterations];
        scene.show();

        try
        {
            socket = new Socket(tcpechoserver, tcpechoport);
        }
        catch (ConnectException ce)
        {
            System.out.println("TCPEchoClientXlet TCPEchoServer not available " + ce);
            // Cannot continue without a socket
            destroyXlet(true);
        }
        catch (Exception e)
        {
            System.out.println("TCPEchoClientXlet failed creating socket " + e);
            destroyXlet(true);
        }

        try
        {
            sin = socket.getInputStream();
            sout = socket.getOutputStream();
        }
        catch (Exception e)
        {
            System.out.println("TCPEchoClientXlet exception getting input/output stream " + e);
            e.printStackTrace();
            destroyXlet(true);
        }

        byte[] inbuffer = new byte[BUFFERSIZE];
        int failed = 0;
        // Create and fill a buffer to send
        StringBuffer soutbuffer = FillBuffer.getFullBuffer(BUFFERSIZE);
        for (int i = 0; i < iterations && failed < 10; ++i)
        {
            System.out.println("TCPEchoClientXlet write/read loop count=" + i);
            // Insert the index into the buffer to make each buffer "somewhat"
            // unique
            int len = soutbuffer.length();
            soutbuffer.setCharAt(len - 1, (char) (i % 10 + offset));
            System.out.println("TCPEchoClientXlet output bytes=" + len);
            byte[] outbuffer = soutbuffer.toString().getBytes();
            try
            {
                long start = System.currentTimeMillis();
                sout.write(outbuffer);
                numbytes = sin.read(inbuffer);
                long stop = System.currentTimeMillis();

                // Log the time for each echo buffer to travel to the echo
                // server
                // and back
                perflog.send("TCPEchoClientXlet", start, (stop - start), UDPPerfReporter.PASS, i);
                displaystr[i] = new String("TCPEcho start=" + start + " duration=" + (stop - start) + " PASS"
                        + " iteration=" + (i + 1));
                scene.repaint();
                failed = 0; // restart test failed counter after each successful
                            // test
            }
            catch (IOException ioe)
            {
                // Log a performance failed message with start and duration 0
                perflog.send("TCPEchoClientXlet", 0, 0, UDPPerfReporter.FAIL, i);
                displaystr[i] = new String("TCPEcho start=0 duration=0 FAIL iteration=" + (i + 1));
                test.fail(displaystr[i]);
                if (!axc.isConnected()) logger.log(test.getTestResult());
                scene.repaint();
                System.out.println("TCPEchoClientXlet echo test failed" + ioe);
                failed++;
            }

            // Test that the echo was complete
            test.assertEquals("Number of bytes written and read differ, iteration = " + i, numbytes, len);
        }
        // 10 consecutive echo tests failed, quit trying
        if (failed >= 10)
        {
            System.out.println("TCPEchoClientXlet 10 TCP echo tests failed");
        }
        System.out.println("TCPEchoClientXlet closing socket in 3 seconds");
        try
        {
            Thread.sleep(3 * 1000);
        }
        catch (InterruptedException ignore)
        {
        }
        try
        {
            socket.close();
            sin.close();
            sout.close();
        }
        catch (IOException ioe)
        {
            System.out.println("TCPEchoClientXlet io exception closing socket " + ioe);
        }

        if (!axc.isConnected()) logger.log(test.getTestResult());
    }

    public void pauseXlet()
    {
        scene.setVisible(false);
    }

    public void destroyXlet(boolean b) throws XletStateChangeException
    {
        System.out.println("TCPEchoClientXlet destroyXlet");
        try
        {
            perflog.close();
            scene.setVisible(false);
            HSceneFactory.getInstance().dispose(scene);
        }
        catch (Exception e)
        {
            System.out.println("TCPEchoClientXlet destroyXlet Exception closing socket");
        }
        ctx.notifyDestroyed();
        throw new XletStateChangeException();
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
            System.out.println("TimePerf config file close failed " + io);
            io.printStackTrace();
        }
    }
}
