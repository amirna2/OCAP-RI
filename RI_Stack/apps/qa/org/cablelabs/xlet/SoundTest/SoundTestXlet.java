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
 * SoundTestXlet allows the user to play each of the supported audio encodings.
 */

package org.cablelabs.xlet.SoundTest;

// Must add org.cablelabs.lib.utils to OCAP.extensions in mpeenv.ini
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
import java.rmi.RemoteException;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.Player;
import javax.media.Time;
import javax.media.EndOfMediaEvent;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HSound;
import org.havi.ui.event.HRcEvent;
import org.ocap.ui.event.OCRcEvent;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;

public class SoundTestXlet implements Xlet, KeyListener, ControllerListener, Driveable
{

    Player player;

    private HScene scene;

    private HSound snd;

    private VidTextBox menu_box;

    private VidTextBox status_box;

    private boolean loop_mode = false;

    private final static String WHOAMI = "SoundTestXlet - ";

    private final static String RESOURCE_PATH = "org/cablelabs/xlet/SoundTest/";

    private final static String FILE_URL_PREFIX = "/";

    // "file://";

    private final static String filenameBase = "SoundTest";

    private final static String filenameMP1 = filenameBase + ".mp1";

    private final static String filenameMP2 = filenameBase + ".mp2";

    private final static String filenameMP3 = filenameBase + ".mp3";

    private final static String filenameAC3 = filenameBase + ".ac3";

    private final static String filenameAIFF = filenameBase + ".aiff";

    private final static int box_x = 50;

    private final static int box_w = 530;

    private final static int menu_box_y = 50;

    private final static int menu_box_h = 280; // for 10 lines

    private final static int status_box_y = menu_box_y + menu_box_h;

    private final static int status_box_h = 80;

    // if you change this, you must also change the on-screen text in
    // displayMenuHSound() and displayMenuJMF()
    private final static int MY_MENU_KEY = OCRcEvent.VK_COLORED_KEY_0;

    private final static int MENU_MAIN = 0;

    private final static int MENU_HSOUND = 1;

    private final static int MENU_JMF = 2;

    private final static int MENU_OTHER = 3;

    private int menu = MENU_MAIN;

    // Objects used to integrate with AutoXlet testing framework
    private AutoXletClient axc;

    private Logger log;

    private Test test;

    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {

        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        scene.addKeyListener(this);

        // menu
        menu_box = new VidTextBox(box_x, menu_box_y, box_w, menu_box_h, 14, 5000); // fontsize,
                                                                                   // buffersize
        if (menu_box == null)
        {
            System.err.println(WHOAMI + "ERROR - Failed to instantiate VidTextBox for menu");
        }
        scene.add(menu_box);

        // status
        status_box = new VidTextBox(box_x, status_box_y, box_w, status_box_h, 14, 5000); // fontsize,
                                                                                         // buffersize
        if (status_box == null)
        {
            System.err.println(WHOAMI + "ERROR - Failed to instantiate VidTextBox for status");
        }
        scene.add(status_box);

        scene.setLayout(new BorderLayout());
        scene.validate();

        // Initialize AutoXlet framework client and grab logger and test objects
        axc = new AutoXletClient(this, xletContext);
        test = axc.getTest();

        // If we have successfully connected, initialize our logger from the
        // AutoXletClient, else use a default constructed XletLogger which will
        // send all logging output to standard out.
        if (axc.isConnected())
        {
            log = axc.getLogger();
        }
        else
        {
            log = new XletLogger();
        }
    }

    public void startXlet()
    {
        System.out.println(WHOAMI + "startXlet");

        scene.setVisible(true);
        scene.show();
        scene.requestFocus();

        System.out.println("HSoundTestXlet - creating HSound");
        snd = new HSound();

        menu = MENU_MAIN;
        displayMenuMain();
    }

    public void pauseXlet()
    {
        System.out.println(WHOAMI + "pauseXlet");

        scene.setVisible(false);

        if (snd != null)
        {
            snd.dispose();
            snd = null;
        }

    }

    public void destroyXlet(boolean flag) throws XletStateChangeException
    {
        System.out.println(WHOAMI + "destroyXlet");

        if (scene != null)
        {
            scene.setVisible(false);
            scene.dispose();
            scene = null;
        }

        if (snd != null)
        {
            snd.dispose();
            snd = null;
        }

    }

    private final static int NUM_MENU_LINES_TO_ERASE = 12;

    private void eraseMenu()
    {
        // erase the previous menu -- do it the hard way.
        for (int i = 0; i < NUM_MENU_LINES_TO_ERASE; i++)
        {
            displayMenuLine(" ");
        }
    }

    /*
     * The VidTextBox that is used for the Menus draws text at the last line and
     * scrolls the text upward. There is no way (that I know of) to erase the
     * VidTextBox so that text is draw from the top line. So, All menus must
     * have the same number of lines. That is some of the menus display lines
     * containing only a space character. If you need more menu lines, you must
     * make the VidTextBox for the menu larger, and make make the status box
     * smaller and move it down.
     */

    private void displayMenuMain()
    {
        eraseMenu();
        // 123456789.123456789.123456789.987654321.987654321
        displayMenuLine("+---------------- SoundTestXlet ----------------+");
        displayMenuLine("+----------------   Main Menu   ----------------+");
        displayMenuLine("     1 - HSound Tests");
        displayMenuLine("     2 - JMF    Tests");
        displayMenuLine("     3 - Other  Tests");
        displayMenuLine(" ");
        displayMenuLine(" ");
        displayMenuLine(" ");
        displayMenuLine(" ");
        displayMenuLine(" ");

        // erase the status line
        if (status_box != null)
        {
            status_box.write(" ");
        }

        scene.repaint();
    }

    private void displayMenuHSound()
    {
        eraseMenu();
        // 123456789.123456789.123456789.987654321.987654321
        displayMenuLine("+---------------- SoundTestXlet ----------------+");
        displayMenuLine("+---------------- HSound Tests  ----------------+");
        displayMenuLine("     1 - MPEG-1 Layer-1   (" + filenameMP1 + ")");
        displayMenuLine("     2 - MPEG-1 Layer-2   (" + filenameMP2 + ")");
        displayMenuLine("     3 - MPEG-1 Layer-3   (" + filenameMP3 + ")");
        displayMenuLine("     4 - AC3    (" + filenameAC3 + ")");
        displayMenuLine("     5 - AIFF   (" + filenameAIFF + ")");
        displayMenuLine("     6 - GetTime");
        displayMenuLine("     7 - SetTime (+ 5 secs.)");
        displayMenuLine("     9 - Stop");
        displayMenuLine("     0 - Toggle Loop mode");
        displayMenuLine("Red Circle - Go to Main Menu");

        scene.repaint();
    }

    private void displayMenuJMF()
    {
        eraseMenu();
        // 123456789.123456789.123456789.987654321.987654321
        displayMenuLine("+---------------- SoundTestXlet ----------------+");
        displayMenuLine("+----------------    JMF Tests    ----------------+");
        displayMenuLine("     1 - MPEG-1 Layer-1   (" + filenameMP1 + ")");
        displayMenuLine("     2 - MPEG-1 Layer-2   (" + filenameMP2 + ")");
        displayMenuLine("     3 - MPEG-1 Layer-3   (" + filenameMP3 + ")");
        displayMenuLine("     4 - AC3    (" + filenameAC3 + ")");
        displayMenuLine("     5 - AIFF   (" + filenameAIFF + ")");
        displayMenuLine("     6 - GetTime");
        displayMenuLine("     7 - SetTime (+ 5 secs.)");
        displayMenuLine("     9 - Stop");
        displayMenuLine("     0 - Toggle Loop mode");
        displayMenuLine("Red Circle - Go to Main Menu");
        displayMenuLine(" ");

        scene.repaint();
    }

    private void displayMenuOther()
    {
        eraseMenu();
        // 123456789.123456789.123456789.987654321.987654321
        displayMenuLine("+---------------- SoundTestXlet ----------------+");
        displayMenuLine("+----------------   Other Tests   ----------------+");
        displayMenuLine("     1 - Run Automated Test Cases");
        displayMenuLine(" ");
        displayMenuLine(" ");
        displayMenuLine(" ");
        displayMenuLine("Red Circle - Go to Main Menu");

        scene.repaint();
    }

    private void displayMenuLine(String msg)
    {
        if (menu_box != null)
        {
            menu_box.write(msg);
        }
        System.out.println(WHOAMI + msg);
    }

    private void displayStatus(String msg)
    {
        if (status_box != null)
        {
            status_box.write(msg);
        }
        scene.repaint();
        System.out.println(WHOAMI + msg);
    }

    public void keyTyped(KeyEvent e)
    {
        System.out.println(WHOAMI + " received key typed event " + e);
    }

    public void keyReleased(KeyEvent e)
    {
        System.out.println(WHOAMI + " received key released event " + e);
    }

    public void keyPressed(KeyEvent e)
    {
        System.out.println(WHOAMI + " received key pressed event " + e);
        if (e.getKeyCode() == MY_MENU_KEY)
        {
            menu = MENU_MAIN;
            displayMenuMain();
            e.consume();
            return;
        }

        if (menu == MENU_MAIN)
        {
            doMainMenu(e);
        }
        else if (menu == MENU_HSOUND)
        {
            doHSound(e);
        }
        else if (menu == MENU_JMF)
        {
            doJMF(e);
        }
        else
        {
            doOther(e);
        }
    }

    /**
     * Main Menu
     */
    private void doMainMenu(KeyEvent e)
    {
        int key = e.getKeyCode();

        if (key == HRcEvent.VK_1)
        {
            menu = MENU_HSOUND;
            displayMenuHSound();
            e.consume();
        }
        else if (key == HRcEvent.VK_2)
        {
            menu = MENU_JMF;
            displayMenuJMF();
            e.consume();
        }
        else if (key == HRcEvent.VK_3)
        {
            menu = MENU_OTHER;
            displayMenuOther();
            e.consume();
        }
        else
        {
            menu_box.keyPressed(e);
        }
    }

    /**
     * HSound tests
     */
    private void doHSound(KeyEvent e)
    {
        int key = e.getKeyCode();

        if (key == HRcEvent.VK_1)
        {
            displayStatus("1 - MPEG-1 Layer 1   (" + filenameMP1 + ")   Loop " + (loop_mode ? "On" : "Off"));
            hsoundPlay(filenameMP1);
            e.consume();
        }
        else if (key == HRcEvent.VK_2)
        {
            displayStatus("2 - MPEG-1 Layer 2   (" + filenameMP2 + ")   Loop " + (loop_mode ? "On" : "Off"));
            hsoundPlay(filenameMP2);
            e.consume();
        }
        else if (key == HRcEvent.VK_3)
        {
            displayStatus("3 - MPEG-1 Layer 3   (" + filenameMP3 + ")   Loop " + (loop_mode ? "On" : "Off"));
            hsoundPlay(filenameMP3);
            e.consume();
        }
        else if (key == HRcEvent.VK_4)
        {
            displayStatus("4 - AC3   (" + filenameAC3 + ")   Loop " + (loop_mode ? "On" : "Off"));
            hsoundPlay(filenameAC3);
            e.consume();
        }
        else if (key == HRcEvent.VK_5)
        {
            displayStatus("5 - AIFF   (" + filenameAIFF + ")   Loop " + (loop_mode ? "On" : "Off"));
            hsoundPlay(filenameAIFF);
            e.consume();
        }
        else if (key == HRcEvent.VK_9)
        {
            displayStatus("9 - Stop");
            hsoundStop();
            e.consume();
        }
        else if (key == HRcEvent.VK_0)
        {
            if (loop_mode == true)
            {
                loop_mode = false;
                displayStatus("Loop Off");
            }
            else
            {
                loop_mode = true;
                displayStatus("Loop On");
            }
            e.consume();
        }
        else
        {
            menu_box.keyPressed(e);
        }

    }

    private void hsoundStop()
    {
        if (snd != null)
        {
            snd.stop();
            snd = null;
        }
    }

    private void hsoundPlay(String filename)
    {

        String path = RESOURCE_PATH + filename;

        try
        {
            System.out.println(WHOAMI + "calling HSound.load(" + path + ")");
            if (snd != null)
            {
                snd.load(path);
            }

            if (loop_mode == false)
            {
                System.out.println(WHOAMI + "calling HSound.play()");
                if (snd != null)
                {
                    snd.play();
                }
            }
            else
            {
                System.out.println(WHOAMI + "calling HSound.loop()");
                if (snd != null)
                {
                    snd.loop();
                }
            }
        }
        catch (java.io.IOException e)
        {
            System.out.println(WHOAMI + "HSound - IOException - path = " + path);
        }
        catch (java.lang.SecurityException e)
        {
            System.out.println(WHOAMI + "HSound - SecurityException - path = " + path);
        }
    }

    /**
     * JMF tests
     */
    private void doJMF(KeyEvent e)
    {
        int key = e.getKeyCode();

        if (key == HRcEvent.VK_1)
        {
            displayStatus("1 - MPEG-1 Layer 1   (" + filenameMP1 + ")");
            jmfPlay(filenameMP1);
            e.consume();
        }
        else if (key == HRcEvent.VK_2)
        {
            displayStatus("2 - MPEG-1 Layer 2   (" + filenameMP2 + ")");
            jmfPlay(filenameMP2);
            e.consume();
        }
        else if (key == HRcEvent.VK_3)
        {
            displayStatus("3 - MPEG-1 Layer 3   (" + filenameMP3 + ")");
            jmfPlay(filenameMP3);
            e.consume();
        }
        else if (key == HRcEvent.VK_4)
        {
            displayStatus("4 - AC3   (" + filenameAC3 + ")");
            jmfPlay(filenameAC3);
            e.consume();
        }
        else if (key == HRcEvent.VK_5)
        {
            displayStatus("5 - AIFF   (" + filenameAIFF + ")");
            jmfPlay(filenameAIFF);
            e.consume();
        }
        else if (key == HRcEvent.VK_6)
        {
            displayStatus("GetTime");
            jmfGetMediaTime();
            e.consume();
        }
        else if (key == HRcEvent.VK_7)
        {
            displayStatus("SetTime");
            jmfSetMediaTime();
            e.consume();
        }
        else if (key == HRcEvent.VK_9)
        {
            displayStatus("Stop");
            jmfStop();
            e.consume();
        }
        else if (key == HRcEvent.VK_0)
        {
            if (loop_mode == true)
            {
                loop_mode = false;
                displayStatus("Loop Off");
            }
            else
            {
                loop_mode = true;
                displayStatus("Loop On");
            }
            e.consume();
        }
        else
        {
            menu_box.keyPressed(e);
        }

    }

    private void jmfStop()
    {
        if (player != null)
        {
            player.stop();
            // player.removeControllerListener(ControllerListener listener);
            player.deallocate();
            player.close();
            player = null;
        }
    }

    private void jmfGetMediaTime()
    {
        if (player != null)
        {
            Time mediaTime = player.getMediaTime();
            displayStatus("MediaTime = " + mediaTime.getSeconds());
        }
    }

    private void jmfSetMediaTime()
    {
        if (player != null)
        {
            int incrementSecs = 5;
            Time mediaTime = player.getMediaTime();
            Time newTime = new Time(mediaTime.getSeconds() + incrementSecs);
            player.setMediaTime(newTime);
            mediaTime = player.getMediaTime();
            displayStatus("MediaTime (seconds) = " + mediaTime.getSeconds());
        }
    }

    private void jmfPlay(String filename)
    {

        String location = FILE_URL_PREFIX + RESOURCE_PATH + filename;

        try
        {
            // Creating a URL for the location
            System.out.println(WHOAMI + "jmf_play - creating URL (" + location + ")");
            URL url = getClass().getResource(filename);// new URL (location);

            // Create a player using the URL
            jmfStop(); // Clear the existing memory assignment
            player = Manager.createPlayer(url);
            if (player != null)
            {
                player.addControllerListener(this);
                player.start();
            }
            else
            {
                System.out.println(WHOAMI + "jmf_play - ERROR - (player == null)");
            }

        }
        catch (java.lang.SecurityException e)
        {
            System.out.println(WHOAMI + "jmf_play - SecurityException - " + " -- location = " + filename);
        }
        catch (java.lang.Exception e)
        {
            System.out.println(WHOAMI + "jmf_play - Exception " + e + " -- location = " + location);
        }

    }

    /*
     * JMF ControllerListener
     */
    public void controllerUpdate(ControllerEvent event)
    {
        System.out.println(WHOAMI + "controllerUpdate - " + event.toString());
        if (event instanceof EndOfMediaEvent)
        {
            System.out.println(WHOAMI + "controllerUpdate - EndOfMediaEvent");
            if (loop_mode == true)
            {
                Player p = (Player) event.getSourceController();
                p.setMediaTime(new Time(0));
                p.start();
            }
        }
        /*
         * else if (event instanceof RealizeCompleteEvent) { System.out.println
         * (WHOAMI + "controllerUpdate - RealizeCompleteEvent"); } else if
         * (event instanceof PrefetchCompleteEvent) { System.out.println (WHOAMI
         * + "controllerUpdate - PrefetchCompleteEvent"); } else if (event
         * instanceof StartEvent) { System.out.println (WHOAMI +
         * "controllerUpdate - StartEvent"); } else if (event instanceof
         * StopEvent) { System.out.println (WHOAMI +
         * "controllerUpdate - StopEvent"); } else if (event instanceof
         * ControllerErrorEvent) { System.out.println (WHOAMI +
         * "controllerUpdate - ControllerErrorEvent"); } // else if (event
         * instanceof DataLostErrorEvent) // { // System.out.println (WHOAMI +
         * "controllerUpdate - DataLostErrorEvent"); // } else if (event
         * instanceof ResourceUnavailableEvent) { System.out.println (WHOAMI +
         * "controllerUpdate - ResourceUnavailableEvent"); } else if (event
         * instanceof InternalErrorEvent) { System.out.println (WHOAMI +
         * "controllerUpdate - InternalErrorEvent"); } else if (event instanceof
         * RateChangeEvent) { System.out.println (WHOAMI +
         * "controllerUpdate - RateChangeEvent"); } else if (event instanceof
         * MediaTimeSetEvent) { System.out.println (WHOAMI +
         * "controllerUpdate - MediaTimeSetEvent"); } else if (event instanceof
         * TransitionEvent) { System.out.println (WHOAMI +
         * "controllerUpdate - TransitionEvent"); } else if (event instanceof
         * ControllerClosedEvent) { System.out.println (WHOAMI +
         * "controllerUpdate - ControllerClosedEvent"); }
         */
        else
        {
            System.out.println(WHOAMI + "controllerUpdate - Unknown Event - " + event.toString());
        }
    }

    /**
     * Other tests
     */
    private void doOther(KeyEvent e)
    {
        int key = e.getKeyCode();

        if (key == HRcEvent.VK_1)
        {
            runAutomatedTests();
            e.consume();
        }
        // TODO

        /*
         * int key = e.getKeyCode();
         * 
         * if (key == HRcEvent.VK_1) { displayStatus ("1 - Xxxx"); // TODO
         * e.consume(); } else if (key == HRcEvent.VK_2) { displayStatus
         * ("2 - Xxxx"); // TODO e.consume(); } else if (key == HRcEvent.VK_3) {
         * displayStatus ("3 - Xxxx"); // TODO e.consume(); } else {
         * menu_box.keyPressed(e); }
         */

    }

    private void pauseForCompletion()
    {
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException exc)
        {
        }
    }

    private void runAutomatedTests()
    {
        testPlayAIFFWithJMF();
        pauseForCompletion();

        testPlayHSoundAIFF();
        pauseForCompletion();

        testPlayJMFThenStopAIFF();
        pauseForCompletion();

        testPlayHSoundThenStopAIFF();
        pauseForCompletion();

        testPlayHSoundAIFFTwice();
    }

    private void testPlayHSoundThenStopAIFF()
    {
        hsoundPlay(filenameAIFF);
        hsoundStop();
        // we're just checking for a crash here
        test.assertTrue(true);
    }

    private void testPlayJMFThenStopAIFF()
    {
        jmfPlay(filenameAIFF);
        jmfStop();
        // we're just checking for a crash here
        test.assertTrue(true);
    }

    private void testPlayAIFFWithJMF()
    {
        jmfPlay(filenameAIFF);
        // we're just checking for a crash here
        test.assertTrue(true);
    }

    private void testPlayHSoundAIFF()
    {
        hsoundPlay(filenameAIFF);
        // we're just checking for a crash here
        test.assertTrue(true);
    }

    private void testPlayHSoundAIFFTwice()
    {
        hsoundPlay(filenameAIFF);
        hsoundPlay(filenameAIFF);
        // we're just checking for a crash here
        test.assertTrue(true);
    }

    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
        keyPressed(event);
    }

}
