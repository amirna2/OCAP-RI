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
 * Created on May 19, 2006
 */
package org.cablelabs.xlet.classloader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.lang.DVBClassLoader;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HScreen;
import org.havi.ui.HState;
import org.havi.ui.HStaticText;
import org.havi.ui.HVisible;
import org.havi.ui.event.HRcEvent;
import org.ocap.ui.event.OCRcEvent;

/**
 * Integration test Xlet for ClassLoader support. This is intended to provide
 * testing for the following:
 * <ul>
 * <li>System-created class loaders that use the application's current
 * transport.
 * <li>Application-created class loaders.
 * <li>Performance benchmarking of class loaders under different situations.
 * </ul>
 * 
 * @author Aaron Kamienski
 */
public class ClassLoaderXlet implements Xlet, Runnable, KeyListener
{
    private XletContext xc;

    private HScene scene;

    private HStaticText display;

    private StringBuffer text = new StringBuffer(256);

    private boolean started;

    private Thread thread;

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        xc = ctx;
    }

    public void startXlet() throws XletStateChangeException
    {
        if (!started)
        {
            HSceneFactory hsf = HSceneFactory.getInstance();
            scene = hsf.getFullScreenScene(HScreen.getDefaultHScreen().getDefaultHGraphicsDevice());
            scene.setFont(new Font("SansSerif", Font.PLAIN, 16));
            scene.setBackgroundMode(HScene.BACKGROUND_FILL);
            scene.setBackground(Color.darkGray.darker());
            scene.setForeground(Color.white.darker());
            scene.addKeyListener(this);
            scene.setLayout(new BorderLayout());
            display = new HStaticText();
            display.setVerticalAlignment(HVisible.VALIGN_BOTTOM);
            display.setHorizontalAlignment(HVisible.HALIGN_LEFT);
            scene.add(display);
            scene.add(new Filler(50), BorderLayout.NORTH);
            scene.add(new Filler(50), BorderLayout.EAST);
            scene.add(new Filler(50), BorderLayout.SOUTH);
            scene.add(new Filler(50), BorderLayout.WEST);
            scene.validate();

            started = true;
        }

        startTest();

        scene.setVisible(true);
        scene.show();
        scene.requestFocus();
    }

    public void pauseXlet()
    {
        stopTest();

        HScene tmp = scene;
        if (tmp != null) tmp.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        stopTest();

        HScene tmp = scene;
        scene = null;
        if (tmp != null)
        {
            tmp.setVisible(false);
            tmp.dispose();
        }
    }

    public void keyPressed(KeyEvent e)
    {
        switch (e.getKeyCode())
        {
            case KeyEvent.VK_PAUSE:
                pauseXlet();
                xc.notifyPaused();
                break;
            case OCRcEvent.VK_EXIT:
                try
                {
                    destroyXlet(true);
                }
                catch (XletStateChangeException ex)
                { /* ignored */
                }
                xc.notifyDestroyed();
                break;
            case HRcEvent.VK_PLAY:
                startTest();
                break;
            case HRcEvent.VK_STOP:
                stopTest();
                break;
        }
    }

    public void keyReleased(KeyEvent e)
    {
        // Does nothing
    }

    public void keyTyped(KeyEvent e)
    {
        // Does nothing
    }

    private synchronized void startTest()
    {
        if (thread == null)
        {
            log("Starting ClassLoader tests...");
            thread = new Thread(this, "ClassLoaderTest");
            thread.start();
        }
        // else do nothing
    }

    private synchronized void stopTest()
    {
        if (thread != null)
        {
            log("Stopping tests.");
            thread.interrupt();
            thread = null;
        }
    }

    private synchronized boolean continueTest()
    {
        return Thread.currentThread() == thread;
    }

    private synchronized void logTest(String msg)
    {
        if (thread == Thread.currentThread()) log(msg);
    }

    private void log(String msg)
    {
        text.append('\n').append(msg);
        display.setTextContent(text.toString(), HState.ALL_STATES);
        System.out.println("[CLASSLOADERTEST]" + msg);
    }

    public void run()
    {
        parseArgs();

        synchronized (this)
        {
            if (thread == Thread.currentThread()) thread = null;
        }
    }

    private void parseArgs()
    {
        if (!parseArgs("dvb.caller.parameters") && !parseArgs(XletContext.ARGS))
        {
            log("No arguments");
        }
    }

    private String toString(Throwable e)
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bos);

        e.printStackTrace(out);
        out.flush();

        return bos.toString();
    }

    private boolean parseArgs(String type)
    {
        String[] args = (String[]) xc.getXletProperty(type);
        if (args == null || args.length == 0) return false;
        try
        {
            parseArgs(args);
        }
        catch (Throwable e)
        {
            log("Could not parseArgs/execute tests");
            log(toString(e));
        }
        return true;
    }

    private void parseArgs(String[] args)
    {
        for (int i = 0; continueTest() && i < args.length; ++i)
        {
            if ("-time".equals(args[i]))
            {
                // -time <classloader> <index>
                // Where <classloader> is AppClassLoader or
                // DVBClassLoader(<urls>)
                String clName = args[++i];
                String indexName = args[++i];

                ClassLoader cl;
                int index = Integer.decode(indexName).intValue();
                if ("AppClassLoader".equalsIgnoreCase(clName))
                {
                    cl = getClass().getClassLoader();
                }
                else if (clName.startsWith("DVBClassLoader(") && clName.endsWith(")"))
                {
                    // Use the app class loader as parent (default)
                    cl = createDVBClassLoader(clName, false, null);
                }
                else if (clName.startsWith("DVBClassLoader(") && clName.endsWith("):app"))
                {
                    // Explicitly use the app class loader as parent
                    cl = createDVBClassLoader(clName, true, getClass().getClassLoader());
                }
                else if (clName.startsWith("DVBClassLoader(") && clName.endsWith("):null"))
                {
                    // Explicitly specify no parent
                    // This is the preferred method as it prevents another CL
                    // from loading the class
                    cl = createDVBClassLoader(clName, true, null);
                }
                else if (clName.startsWith("DVBClassLoader(") && clName.endsWith("):system"))
                {
                    // Explicitly use system class loader as parent
                    cl = createDVBClassLoader(clName, true, DVBClassLoader.class.getClassLoader());
                }
                else
                {
                    throw new IllegalArgumentException("Cannot parse -time " + args[i - 1]);
                }

                logTest("Timing " + clName + " [" + index + "]...");
                try
                {
                    ClassLoadTime time = new ClassLoadTime(cl, "cablelabs.Class", index);

                    logTest("   elapsed time = " + time.getElapsedTime() + " ms");
                    logTest("    total bytes = " + time.getTotalBytes());
                    logTest("      bytes/sec = " + time.getBytesPerSec() + " bps");
                }
                catch (Exception e)
                {
                    logTest(toString(e));
                }
            }
        }
    }

    private DVBClassLoader createDVBClassLoader(String str, boolean useParent, ClassLoader parent)
    {
        str = str.substring("DVBClassLoader(".length(), str.lastIndexOf(")"));
        StringTokenizer tok = new StringTokenizer(str, ",");

        URL urls[] = new URL[tok.countTokens()];
        for (int i = 0; i < urls.length; ++i)
        {
            String url = tok.nextToken();
            try
            {
                urls[i] = new URL(url);
            }
            catch (MalformedURLException e)
            {
                throw new IllegalArgumentException("Invalid URL " + url);
            }
        }

        return useParent ? DVBClassLoader.newInstance(urls, parent) : DVBClassLoader.newInstance(urls);
    }

    class Filler extends Component
    {
        public Filler(int space)
        {
            this.space = space;
        }

        public Dimension getPreferredSize()
        {
            return new Dimension(space, space);
        }

        public Dimension getMaximumSize()
        {
            return getPreferredSize();
        }

        public Dimension getMinimumSize()
        {
            return getPreferredSize();
        }

        private final int space;
    }

}
