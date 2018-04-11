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
package org.cablelabs.xlet.GraphicSpeedTest;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.ocap.ui.event.OCRcEvent;

import org.cablelabs.lib.utils.VideoPlayer;
import org.cablelabs.lib.utils.ArgParser;
import java.io.*;

public class GraphicSpeedTestXlet implements Xlet, KeyListener
{
    public static final String IMAGE_PATH = "/org/cablelabs/xlet/GraphicSpeedTest/images/";

    private static final String GRAPHICS_SOURCE = "graphic_sourceid";

    private static final String CONFIG_FILE = "config_file";

    private String _config_file = null;

    private static int _sourceID;

    private final static String INFO_PIC = "spriteUIInfo.png";

    private final static Point INFO_LOC = new Point(0, 27);

    private final static Point START_LOC = new Point(-50, 170);

    private final static Point END_LOC = new Point(620, 170);

    private final static int DELTA_X = 15; // pix per frame

    private final static Color TEXT_COLOR = new Color(220, 220, 220);

    private final static String FRAMES[] = { "spriteAxRunning_01.png", "spriteAxRunning_02.png",
            "spriteAxRunning_03.png", "spriteAxRunning_04.png", "spriteAxRunning_05.png", "spriteAxRunning_06.png",
            "spriteAxRunning_07.png", "spriteAxRunning_08.png", "spriteAxRunning_09.png", "spriteAxRunning_10.png",
            "spriteAxRunning_11.png", "spriteAxRunning_12.png" };

    private HScene m_scene;

    private Anim m_animation;

    private Display m_display;

    private long m_timeStamp = 0;

    private VideoPlayer m_videoPlayer;

    /*
     * Signals the Xlet to initialize itself and enter the Paused state.
     */
    public void initXlet(XletContext context) throws XletStateChangeException
    {
        // set up the scene
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.addKeyListener(this);

        m_animation = new Anim();
        m_animation.setVisible(false);

        m_display = new Display();
        m_display.setBounds(0, 50, 640, 100);
        m_display.setForeground(Color.blue.darker());

        m_scene.add(m_animation);
        m_scene.add(m_display);

        m_videoPlayer = new VideoPlayer();

        try
        {
            ArgParser args = new ArgParser((String[]) context.getXletProperty(context.ARGS));
            _config_file = args.getStringArg(CONFIG_FILE);
            FileInputStream _fis = new FileInputStream(_config_file);
            try
            {
                ArgParser fopt = new ArgParser(_fis);
                _sourceID = fopt.getIntArg(GRAPHICS_SOURCE);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("\n\n*****************************************************************");
        System.out.println("AnimSpeedTestXlet - _SourceID = " + _sourceID);
        System.out.println("\n\n*****************************************************************");
    }

    /* end cma add */

    /*
     * Signals the Xlet to start providing service and enter the Active state.
     */
    public void startXlet() throws XletStateChangeException
    {
        System.out.println("####### AnimSpeedTest.startXlet()");

        // show the graphics
        m_scene.show();
        m_scene.requestFocus();

    }

    /*
     * Signals the Xlet to stop providing service and enter the Paused state.
     */
    public void pauseXlet()
    {
    }

    /*
     * Signals the Xlet to terminate and enter the Destroyed state.
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        m_scene.dispose();
    }

    /*
     * handles the key press to start the animation
     */
    public void keyPressed(KeyEvent e)
    {
        // move this to release handling
    }

    /*
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent e)
    {
        // no op
    }

    /*
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent e)
    {
        int key = e.getKeyCode();

        switch (key)
        {
            case OCRcEvent.VK_PLAY:
                // Start!!
                startAnim();
                break;

            case OCRcEvent.VK_ENTER:
                if (m_videoPlayer != null)
                {
                    if (m_videoPlayer.isPlaying())
                        m_videoPlayer.stop();
                    else
                        m_videoPlayer.tune(_sourceID);
                }
                break;

            case OCRcEvent.VK_INFO:
                // toggle the infomation display
                m_display.setVisible(!m_display.isVisible());
                break;

            default:
                break;
        }
    }

    private void startAnim()
    {
        System.out.println("startAnim");

        // start the timer...
        m_timeStamp = System.currentTimeMillis();

        // call paint method to begin
        m_animation.setVisible(true);
    }

    private void doneAnimating(int frameCount)
    {
        System.out.println("doneAnimating");
        m_animation.setVisible(false);

        long timeTook = System.currentTimeMillis() - m_timeStamp;
        System.out.println("Time took: " + timeTook);
        System.out.println("Total frames shown: " + frameCount);

        m_display.setInfo(timeTook, frameCount);
        m_display.repaint();

        System.out.println();
    }

    /*
     * 
     * @author hiroyo
     */
    private class Anim extends Component
    {
        private Image m_imageList[];

        private boolean m_isAnimating = false;

        public Anim()
        {
            m_imageList = new Image[FRAMES.length];

            MediaTracker tracker = new MediaTracker(this);

            for (int i = 0; i < FRAMES.length; ++i)
            {
                String path = IMAGE_PATH + FRAMES[i];

                URL url = this.getClass().getResource(path);

                Image image = java.awt.Toolkit.getDefaultToolkit().getImage(url);

                tracker.addImage(image, 0);

                m_imageList[i] = image;
            }

            try
            {
                tracker.waitForAll();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            setBounds(START_LOC.x, START_LOC.y, 640 - START_LOC.x, m_imageList[0].getHeight(this));

        }

        public void paint(Graphics g)
        {
            Toolkit toolkit = Toolkit.getDefaultToolkit();

            int index = 0;
            int x = 0;
            int counter = 0;

            m_isAnimating = true;

            int width = m_imageList[0].getWidth(this);
            int height = m_imageList[0].getHeight(this);

            while (true)
            {

                g.drawImage(m_imageList[index], x, 0, this);

                // sync up the graphics
                toolkit.sync();

                if (x > END_LOC.x)
                {
                    // done
                    break;
                }

                // clean up for the next frame
                g.clearRect(x, 0, width, height);

                ++index;
                ++counter;

                if (index == m_imageList.length) index = 0;

                x += DELTA_X;
            }

            doneAnimating(counter);

            m_isAnimating = false;
        }
    }

    /*
     * 
     * @author hiroyo
     */
    private class Display extends Component
    {

        private long m_timeTook;

        private int m_numFrames;

        private double m_frameRate;

        public void setInfo(long timeTook, int numFrames)
        {
            m_timeTook = timeTook;
            m_numFrames = numFrames;

            m_frameRate = (double) (m_numFrames * 1000) / (double) m_timeTook;

            System.out.println("Frame Rate: " + m_frameRate);
        }

        public void paint(Graphics g)
        {
            g.fillRect(0, 0, getBounds().width, getBounds().height);

            g.setColor(TEXT_COLOR);

            g.drawString("Press PLAY to start animation, Press SELECT to toggle video", 120, 18);

            String str = "Time took: " + m_timeTook + " miliseconds";

            g.drawString(str, 220, 45);

            str = "Total frames shown: " + m_numFrames;

            g.drawString(str, 220, 65);

            str = "Frame rate: " + m_frameRate;

            g.drawString(str, 220, 85);
        }
    }

}
