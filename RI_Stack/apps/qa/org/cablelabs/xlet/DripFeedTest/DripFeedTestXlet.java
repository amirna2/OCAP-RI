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
package org.cablelabs.xlet.DripFeedTest;

import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.Vector;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.StartEvent;

import javax.tv.media.AWTVideoSize;
import javax.tv.media.AWTVideoSizeControl;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.media.DripFeedDataSource;
import org.dvb.media.VideoPresentationControl;
import org.dvb.media.BackgroundVideoPresentationControl;
import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.event.HRcEvent;

import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.TestResult;
import org.cablelabs.test.autoxlet.XletLogger;

// import org.cablelabs.xlet.DripFeedTest.BackgroundVideo;

/**
 * DripFeedTestXlet
 * 
 * @author Joshua Keplinger 2007/05 TK - Added scaled video (press SELECT key to
 *         toggle video bounds while in loop)
 * 
 */
public class DripFeedTestXlet extends Container implements Xlet, Driveable, UserEventListener
{

    public Vector images;

    public Player player;

    public DripFeedDataSource ds;

    public HScene scene;

    public RCKeyListener keyListener;

    public DripFeedPlayerListener playerListener;

    private int currImage = 0;

    private volatile boolean loop = false;

    private int iterations;

    // Objects used to integrate with AutoXlet testing framework
    private AutoXletClient axc;

    private Logger log;

    private Test test;

    // Bounds used in scaling video
    private static final Rectangle FULLSCREEN_BOUNDS = new Rectangle(0, 0, 640, 480);

    private static final Rectangle SCALED_BOUNDS = new Rectangle(0, 0, 320, 240);

    // private Image m_backgroundImage;

    // private Image m_bannerImage;

    private VideoPlayer m_videoPlayer;

    private boolean m_isFullScreen = false;

    private boolean m_hasTuned = false;

    private UserEvent ue;

    // Background Video presentation
    // private BackgroundVideo bgv;

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        loop = false;
        player.removeControllerListener(playerListener);
        player.close();

        scene.setVisible(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#initXlet(javax.tv.xlet.XletContext)
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
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

        log.log(">>>> DripFeedTestXlet.initXlet() - Initializing....");

        String[] args = (String[]) ctx.getXletProperty(XletContext.ARGS);

        // Ripped out of ArgParser
        StringBuffer sb = new StringBuffer();
        Properties prop = new Properties();

        for (int ii = 0; ii < args.length; ii++)
        {
            sb.append(args[ii]);
            sb.append("\n");
        }

        try
        {
            prop.load(new ByteArrayInputStream((sb.toString()).getBytes()));
        }
        catch (IOException e)
        {
            throw new XletStateChangeException("Unable to load xlet arguments: " + e.getMessage());
        }

        Vector imagePaths = new Vector();
        String imageName = null;
        int i = 0;
        while ((imageName = prop.getProperty("FRAME" + i)) != null)
        {
            imagePaths.addElement(imageName);
            i++;
        }

        log.log(">>>> DripFeedTestXlet.initXlet() - Successfully retrieved arguments!");

        images = new Vector(imagePaths.size());
        loadImages(imagePaths);
        log.log(">>>> DripFeedTestXlet.initXlet() - Successfully loaded images!");
        iterations = Integer.parseInt(prop.getProperty("ITERATIONS", "50"));

        keyListener = new RCKeyListener();
        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.addKeyListener(keyListener);

        // scene.setBounds(SCALED_BOUNDS);
        scene.setVisible(true);
        scene.add(this);

        // Drip Feed data source
        ds = new DripFeedDataSource();

        try
        {
            player = Manager.createPlayer(ds);
        }
        catch (NoPlayerException e)
        {
            throw new XletStateChangeException("No Player available for DripFeedDataSource: " + e.getMessage());
        }
        catch (IOException e)
        {
            throw new XletStateChangeException("Unexpected exception creating Player: " + e.getMessage());
        }

        try
        {
            m_videoPlayer = new VideoPlayer(SCALED_BOUNDS);

        }
        catch (Throwable e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }

        log.log(">>>> DripFeedTestXlet.initXlet() - Successfully created dripfeed Player!");

        playerListener = new DripFeedPlayerListener();
        player.addControllerListener(playerListener);

        Object[] controls = ds.getControls();
        if (controls != null)
        {
            log.log(">>>>> DripFeedDataSource getControls[].length() = " + controls.length);
        }

        // Background Video Thread
        // bgv = new BackgroundVideo();

        // bgvThread.start();

        log.log(">>>> DripFeedTestXlet.initXlet() - Initialization complete!");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        log.log(">>>> DripFeedTestXlet.pauseXlet() - Pausing test xlet");
        loop = false;
        EventManager em = EventManager.getInstance();
        em.removeUserEventListener(this);

        player.stop();
        playerListener.started = false;
        scene.setVisible(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        log.log(">>>> DripFeedTestXlet.startXlet() - Starting dripfeed Player...");

        m_videoPlayer.resize(SCALED_BOUNDS);

        log.log(">>>> before player.start() ");
        player.start();
        log.log(">>>> after player.start() ");

        if (!playerListener.waitForStart())
            throw new XletStateChangeException("Player did not start within 10 seconds");

        scene.setVisible(true);

        log.log(">>>> DripFeedTestXlet.startXlet() - Setting bounds...");
        scene.show();
        scene.requestFocus();

        m_videoPlayer.tune(SCALED_BOUNDS);

        log.log(">>>> DripFeedTestXlet.startXlet() - Successfully started dripfeed Player!");
        log.log(">>>> DripFeedTestXlet.startXlet() - Displaying initial image...");
        log.log(">>>> DripFeedTestXlet.startXlet() - Successfully started!");
    }

    private void loadImages(Vector imagePaths)
    {
        for (int i = 0; i < imagePaths.size(); i++)
        {
            InputStream is = null;
            try
            {
                String imageName = (String) imagePaths.elementAt(i);

                log.log(">>>> DripFeedTestXlet.loadImages() - Loading image at " + imageName);

                // URL url = new URL(imageName);
                URL url = getClass().getResource(imageName);
                URLConnection conn = url.openConnection();
                int size = conn.getContentLength();
                if (size < 1)
                {
                    log.log("No data to load from " + imagePaths.elementAt(i));
                    continue;
                }
                byte[] feed = new byte[size];
                is = conn.getInputStream();
                is.read(feed);
                images.addElement(feed);
                log.log(">>>> DripFeedTestXlet.loadImages() - Successfully loaded image at " + imageName + "!");
            }
            catch (MalformedURLException e)
            {
                log.log("Unable to load " + imagePaths.elementAt(i) + "MalformedURLException: " + e.getMessage());
                continue;
            }

            catch (IOException e)
            {
                log.log("Unable to load " + imagePaths.elementAt(i) + "IOException: " + e.getMessage());
                continue;
            }
            catch (Exception e)
            {
                log.log("Unable to load " + imagePaths.elementAt(i) + ": " + e.getMessage());
                // e.printStackTrace();
                continue;
            }
            finally
            {
                try
                {
                    if (is != null) is.close();
                }
                catch (Exception e)
                {
                    log.log("Error closing inputstream: " + e.getMessage());
                }
            }
        }
    }

    private void updateImage()
    {
        log.log(">>>> DripFeedTestXlet.updateImage() - Displaying image " + currImage);
        ds.feed((byte[]) images.elementAt(currImage));
    }

    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
        keyListener.keyPressed(event);
    }

    public void userEventReceived(UserEvent e)
    {

        // Ignore if initial tune has not completed.
        if (!m_hasTuned)
        {
            return;
        }
        // Select key is pressed
        if (m_isFullScreen)
        {
            m_videoPlayer.setVideoBounds(SCALED_BOUNDS);
            m_isFullScreen = false;
        }
        else
        {
            m_videoPlayer.setVideoBounds(FULLSCREEN_BOUNDS);
            m_isFullScreen = true;
        }

        log.log(">>>> userEventReceived(UserEvent e)....");

        repaint();
    }

    public void logDF(String logMsg)
    {
        log.log(logMsg);
    }

    public void useAsGraphics()
    {
    }

    private class RCKeyListener implements KeyListener
    {

        public void keyPressed(KeyEvent event)
        {
            int keyCode = event.getKeyCode();

            switch (keyCode)
            {
                case HRcEvent.VK_RIGHT:
                    log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - VK_RIGHT pressed");
                    if (loop)
                    {
                        log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - Image loop currently running, "
                                + "press VK_STOP to stop the image loop");
                    }
                    else
                    {
                        currImage = (currImage == images.size() - 1) ? 0 : currImage + 1;
                        updateImage();
                    }
                    break;
                case HRcEvent.VK_LEFT:
                    log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - VK_LEFT pressed");
                    if (loop)
                    {
                        log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - Image loop currently running, "
                                + "press VK_STOP to stop the image loop");
                    }
                    else
                    {
                        currImage = (currImage > 0) ? currImage - 1 : images.size() - 1;
                        updateImage();
                    }
                    break;
                case HRcEvent.VK_PLAY:
                    log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - VK_PLAY pressed");
                    if (loop)
                    {
                        log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - Image loop already started");
                    }
                    else
                    {
                        log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - Starting image loop...");
                        loop = true;
                        ImageLooper looper = new ImageLooper();
                        new Thread(looper).start();
                    }
                    break;
                case HRcEvent.VK_STOP:
                    log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - VK_STOP pressed");
                    if (loop)
                    {
                        log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - Stopping image loop");
                        loop = false;
                    }
                    else
                    {
                        log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - Image loop already stopped");
                    }
                    break;

                case HRcEvent.VK_ENTER:
                    log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - VK_ENTER pressed");
                    userEventReceived(ue); // Toggle video bounds (full <- ->
                                           // scaled)
                    break;

                case HRcEvent.VK_1:
                    log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - VK_1 pressed");
                    // bgv.backgroundVideoInit();
                    // bgv.useAsBackground(); // Use as the background
                    break;

                case HRcEvent.VK_2:
                    log.log(">>>> DripFeedTestXlet$RCKeyListener.keyPressed() - VK_2 pressed");
                    useAsGraphics(); // Use as a graphics
                    break;

                default:
                    break;
            }
        }

        public void keyReleased(KeyEvent event)
        {

        }

        public void keyTyped(KeyEvent event)
        {

        }

    }

    private class DripFeedPlayerListener implements ControllerListener
    {

        public boolean started = false;

        public synchronized void controllerUpdate(ControllerEvent event)
        {
            if (event instanceof StartEvent)
            {
                started = true;
                notifyAll();
            }
        }

        public synchronized boolean waitForStart()
        {
            try
            {
                if (!started) wait(10000);
            }
            catch (InterruptedException e)
            {
            }

            return started;
        }

    }

    private class ImageLooper implements Runnable
    {

        public void run()
        {
            test.getTestResult().clearTestResults();
            for (int i = 0; i < iterations; i++)
            {
                if (!loop)
                    break;
                else
                {
                    synchronized (this)
                    {
                        try
                        {
                            currImage = i % images.size();
                            updateImage();
                            wait(1000);
                            test.assertTrue(true);
                        }
                        catch (InterruptedException e)
                        {
                            return;
                        }
                        catch (Exception e)
                        {
                            test.fail("Exception thrown while feeding Player: " + e.getMessage());
                        }
                    } // synchronized(this)
                } // else
            } // for loop
            loop = false;
            log.log(">>>> DripFeedTestXlet$ImageLooper.run() - Image loop complete!");
            TestResult result = test.getTestResult();
            log.log("Number of tests run: " + result.runCount());
            log.log(result);
        } // run()
    }

    /**
     * Tunes to the specified source id and handles video resizing
     */
    private class VideoPlayer implements ServiceContextListener
    {
        private ServiceContext m_serviceContext = null;

        private AWTVideoSizeControl m_videoSizeCtrl = null;

        private Rectangle m_videoBounds;

        public VideoPlayer(Rectangle initialBounds) throws InsufficientResourcesException
        {
            m_serviceContext = ServiceContextFactory.getInstance().createServiceContext();
            m_serviceContext.addListener(this);
            m_videoBounds = initialBounds;
            log.log(">>>> videoPlayer() - videoPlayer");
        }

        public void cleanup()
        {
            m_serviceContext.removeListener(this);
            m_serviceContext.destroy();
        }

        /**
         * Tunes to the sourceID and start with videoBounds
         */
        public void tune(Rectangle videoBounds)
        {
            // Remember the next video bounds... the video bounds will be set at
            // the tune complete.
            m_videoBounds = videoBounds;
            m_hasTuned = true;
            updateImage();
            log.log(">>>> tune() - tune");
        }

        /**
         * Tunes to the sourceID. Video bounds remains the same.
         */
        public void tune()
        {
            tune(m_videoBounds);
        }

        /**
         * Stop the service context
         */
        public void stop()
        {
            if (m_serviceContext != null)
            {
                m_serviceContext.stop();
            }
        }

        /**
         * Set the video bounds to "rect"
         */
        public void setVideoBounds(Rectangle rect)
        {

            if (m_videoSizeCtrl != null)
            {
                Rectangle src = new Rectangle(0, 0, 640, 480);
                AWTVideoSize preferredSize = new AWTVideoSize(src, rect);
                AWTVideoSize scaledSize = m_videoSizeCtrl.checkSize(preferredSize);
                if (!scaledSize.equals(preferredSize))
                {
                    System.out.println("Unsupported scaling factor: " + preferredSize + ".");
                    System.out.println("Video will be scaled to: " + scaledSize + ".");
                }
                m_videoSizeCtrl.setSize(scaledSize);
                log.log(">>>> setVideoBounds() - setVideoBounds");
            }
        }

        /**
         * Notifies the ServiceContextListener of an event generated by a
         * ServiceContext.
         * 
         * Parameters: e - The generated event.
         */

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            if (ev != null)
            {
                if (ev instanceof NormalContentEvent)
                {
                    ServiceContentHandler[] schArray = ev.getServiceContext().getServiceContentHandlers();

                    if (schArray[0] != null && schArray[0] instanceof Player)
                    {
                        m_hasTuned = true;
                        Player player = (Player) schArray[0];
                        // Keep the video size control
                        m_videoSizeCtrl = (AWTVideoSizeControl) player.getControl("javax.tv.media.AWTVideoSizeControl");

                        // Set the video bounds
                        setVideoBounds(m_isFullScreen ? FULLSCREEN_BOUNDS : SCALED_BOUNDS);
                        log.log(">>>> receiveServiceContextEvent() - receiveServiceContextEvent");
                    }
                }
            }
            log.log(">>>> Exiting receiveServiceContextEvent() - receiveServiceContextEvent");
        }

        public void resize(Rectangle size)
        {

            ServiceContextFactory serviceContextFactory = null;
            // javax.tv.service.selection.ServiceContext m_serviceContext =
            // null;
            log.log(">>>> in video resize, size=" + size);
            try
            {
                serviceContextFactory = ServiceContextFactory.getInstance();

                // serviceContext =
                // serviceContextFactory.getServiceContext(context);
                m_serviceContext = serviceContextFactory.createServiceContext();

            }
            catch (Exception ex)
            {

                ex.printStackTrace();

            }
            if (m_serviceContext != null)
            {

                ServiceContentHandler[] serviceContentHandler = m_serviceContext.getServiceContentHandlers();

                // Player player = null;

                if (serviceContentHandler.length > 0)
                {
                    // player = (Player) serviceContentHandler[0];
                }

                if (player != null)
                {

                    m_videoSizeCtrl = (AWTVideoSizeControl) player.getControl("javax.tv.media.AWTVideoSizeControl");
                    Rectangle src = new Rectangle(0, 0, 720, 480); // source
                                                                   // video is
                                                                   // 720 x 480
                    // Rectangle src = new Rectangle(0, 0, 640, 480); // source
                    // video is 640 x 480
                    AWTVideoSize preferredSize = new AWTVideoSize(src, size);
                    m_videoSizeCtrl.setSize(preferredSize);
                    AWTVideoSize scaledSize = m_videoSizeCtrl.checkSize(preferredSize);
                    if (!scaledSize.equals(preferredSize))
                    {
                        log.log(">>>> Unsupported scaling factor: " + preferredSize);
                        log.log(">>>> Video will be scaled to: " + scaledSize);
                    }
                    log.log(">>>> video resized using " + size);
                    log.log(">>>> video preferred size " + preferredSize);
                    AWTVideoSize getSize = m_videoSizeCtrl.getSize();
                    log.log(">>>> getSize " + getSize);

                    // ======= debug statements
                    VideoPresentationControl vPC = (VideoPresentationControl) player.getControl("org.dvb.media.VideoPresentationControl");
                    float[] vPCV = vPC.getVerticalScalingFactors();
                    float[] vPCH = vPC.getHorizontalScalingFactors();
                    log.log(">>>> vertical scaling factor: " + vPCV);
                    log.log(">>>> horizontal scaling factor: " + vPCH);
                    // ======= end of debug statements

                }
            }
            log.log(">>>> Leaving video resize");
        }
    }

}
