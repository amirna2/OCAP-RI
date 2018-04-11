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
package org.cablelabs.xlet.SoundContention;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.rmi.RemoteException;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.ocap.ui.event.OCRcEvent;

import org.havi.ui.HSound; // tkn

/**
 * SoundContention1 comunicates with SoundContention2 using Inter Xlet
 * Communication and SimpleVO. If we have focus key events will be displayed. On
 * an enter event the SimpleVO value will be set and SoundContention2 will be
 * notified and display our key event.
 * 
 * When SoundContention2 gets an enter event the SimpleVO value will be set and
 * SoundContention1 will be notified and display SoundContention's key event.
 */
public class SoundContention1 extends Component implements Xlet, ValueChangedListener, UserEventListener,
        FocusListener, KeyListener
{
    private static final long serialVersionUID = 1;

    // private static final long serialVersionUID = 0x6545;

    // The Xlets Application Context
    private XletContext context;

    // The top-level HAVi Scene.
    private HScene m_scene;

    private HSound snd;

    private SoundTest soundtest;

    private boolean noError = false;

    private boolean jmfMode; // JMF mode flag

    private String playerMode; // One of HSound and JMF

    private boolean testRC; // Resource Contention test flag

    private String rcMode; // Sring for RC mode on/off

    private String audioFiles[] = { "08_11025_Mono.AIF", "08_22050_Mono.AIF", "08_22050_Stereo.AIF",
            "16_22050_Mono.AIF", "16_22050_Stereo.AIF", "16_44100_Mono.AIF", "16_44100_Stereo.AIF", "Frieds.aiff" };

    private String audioF; // an audio file

    private int keyNum;

    // The exported object used to communicate with IXCSample1
    private SimpleVO exporter;

    private boolean hasFocus = false;

    // Our local key code
    private int keyCode = -1;

    /**
     * The OCAP Xlet initialization entry point.
     * 
     * @param cntx
     *            The Xlet context.
     * 
     * @see javax.tv.xlet.Xlet#initXlet(javax.tv.xlet.XletContext)
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        try
        {
            context = ctx;
            jmfMode = true;
            testRC = true;
            snd = new HSound();
            soundtest = new SoundTest();
            exporter = new SimpleVO(this);
            exporter.exportMe(ctx);
            org.dvb.io.ixc.IxcRegistry.bind(ctx, "Player1", this);
            exporter.addValueChangedListener(this);
            m_scene = HSceneFactory.getInstance().getDefaultHScene();
            // Set the size of the scene
            m_scene.setSize(320, 480);
            m_scene.setLocation(new Point(0, 0));
            setSize(320, 480);
            m_scene.add(this);

            EventManager em = EventManager.getInstance();

            UserEventRepository repo = new UserEventRepository("keys");
            repo.addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, KeyEvent.KEY_PRESSED, OCRcEvent.VK_LEFT, 0, 0L));

            em.addUserEventListener(this, repo);

            addFocusListener(this);
            addKeyListener(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /**
     * The OCAP Xlet entry point for starting the application.
     * 
     * @throws XletStateChangeException
     *             This exception is thrown if the Xlet can not be successfully
     *             started.
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        try
        {
            m_scene.show();

            // AppID fullAppId = new AppID(1, 1992); // 6546 = 0x1992
            AppID fullAppId = new AppID(1, 2);
            // Get app proxy
            AppProxy proxy = AppsDatabase.getAppsDatabase().getAppProxy(fullAppId);
            proxy.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /**
     * The OCAP Xlet entry point for pausing the application.
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        m_scene.setVisible(false);
        if (snd != null)
        {
            snd.dispose();
            snd = null;
        }
    }

    /**
     * The OCAP Xlet entry point for destroying the application.
     * 
     * @throws XletStateChangeException
     *             This exception is thrown if the Xlet can not be successfully
     *             destoryed.
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        try
        {
            exporter.removeValueChangedListener(this);
            EventManager em = EventManager.getInstance();
            em.removeUserEventListener(this);
            removeFocusListener(this);
            removeKeyListener(this);
            m_scene.dispose();
            m_scene = null;
            soundtest.jmfStop();
            if (snd != null)
            {
                snd.dispose();
                snd = null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /*
     * @see java.awt.Component#paint(java.awt.Graphics)
     */
    public void paint(Graphics g)
    {
        Font font = new Font("tirasias", Font.PLAIN, 14);

        g.setFont(font);
        g.setColor(Color.darkGray);
        g.fillRect(0, 0, 320, 480);
        g.setColor(Color.white);

        int y = 40;

        if (jmfMode == true)
        {
            playerMode = "JMF Player";
        }
        else
        {
            playerMode = "HSound Player";
        }
        if (testRC == true)
        {
            rcMode = ", RC Test On";
        }
        else
        {
            rcMode = ", RC Test Off";
        }
        audioF = null;
        g.drawString("PLAYER 1 - " + playerMode + rcMode, 20, y);
        y += 30;
        g.drawString("Left Arrow - Focus on Player 1", 20, y);
        // y += 20;
        // g.drawString("and play SoundTest.aiff", 20, y);

        y += 30;
        g.drawString("Menu - Toggle between", 20, y);
        y += 20;
        g.drawString("HSound / JMF Players", 20, y);

        y += 30;
        g.drawString("0 - Toggle Resource Contention", 20, y);
        y += 20;
        g.drawString("RC Test On issues Player Busy if applicable", 20, y);
        y += 20;
        g.drawString("RC Test Off stops the player before restart", 20, y);

        y += 30;
        g.drawString("1 - 8 bit 11.025 KHz Mono", 20, y);
        y += 20;
        g.drawString("2 - 8 Bit 22.050 KHz Mono", 20, y);
        y += 20;
        g.drawString("3 - 8 Bit 22.050 KHz Stereo", 20, y);
        y += 20;
        g.drawString("4 - 16 Bit 22.050 KHz Mono", 20, y);
        y += 20;
        g.drawString("5 - 16 Bit 22.050 KHz Stereo", 20, y);
        y += 20;
        g.drawString("6 - 16 Bit 44.100 KHz Mono", 20, y);
        y += 20;
        g.drawString("7 - 16 Bit 44.100 KHz Stereo", 20, y);
        y += 20;
        g.drawString("8 - 16 bit Frieds.aiff", 20, y);
        y += 30;

        String keyName = KeyCodeHelper.getKeyName(keyCode);
        // g.drawString("Key: " + keyName, 20, y);
        keyNum = keyCode - 49;
        if (keyName == "Menu")
        {
            y += 20;
            if (jmfMode == true)
            {
                jmfMode = false;
                g.drawString("Changed the player to HSound mode", 20, y);
            }
            else
            {
                jmfMode = true;
                g.drawString("Changed the player to JMF mode", 20, y);
            }
            return;
        }
        else if (keyName == "0")
        {
            y += 20;
            if (testRC)
            {
                testRC = false;
                g.drawString("Changed RC to Off", 20, y);
            }
            else
            {
                testRC = true;
                g.drawString("Changed RC to On", 20, y);
            }
            return;
        }
        else if (keyNum >= 0 && keyNum <= 7)
        {

            y += 20;
            audioF = audioFiles[keyNum];
            g.drawString("Playing " + audioF, 20, y);

        }
        else if (keyName == "9")
        {
            soundtest.runAutomatedTests(snd, testRC);
        }

        if (hasFocus)
        {

            g.setColor(Color.white);
            g.drawRect(0, 0, 319, 479);
            g.drawRect(1, 1, 317, 477);
            if (audioF != null)
            {
                try
                {
                    if (jmfMode)
                    {
                        soundtest.jmfPlay(audioF, testRC);
                    }
                    else
                    {
                        soundtest.hsoundPlay(audioF, snd);
                    }

                    noError = true;

                }
                catch (Exception e)
                { // Currently, SoundManager does not check this contention
                    y += 20; // and this will be ignored.
                    g.drawString("Device busy or not availablr. ", 20, y);
                }
            }
        }
    }

    /**
     * When the remote keyCode value is changed update our local keyCode and
     * repaint.
     * 
     * @see org.cablelabs.xlet.ixcSample.ValueChangedListener#valueChanged(org.cablelabs.xlet.ixcSample.SimpleRemote)
     */
    public void valueChanged() throws RemoteException
    {
        keyCode = exporter.getValue();
        repaint();
    }

    /**
     * @return our XletContext from initXlet
     */
    public XletContext getContext()
    {
        return context;
    }

    /**
     * On VK_LEFT (Left Arrow) event request focus.
     * 
     * @see org.dvb.event.UserEventListener#userEventReceived(org.dvb.event.UserEvent)
     */
    public void userEventReceived(UserEvent e)
    {
        requestFocus();
    }

    /**
     * On focusGained remember that we have focus so we can paint ourselves with
     * a border.
     * 
     * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
     */
    public void focusGained(FocusEvent arg0)
    {

        hasFocus = true;
        if (noError)
        {
            if (jmfMode)
            {
                soundtest.jmfStop();
            }
            else
            {
                soundtest.hsoundStop(snd); // Stop the player before starting
                                           // another
            }
        }
        repaint();
    }

    /**
     * On focusLost remember that we do not have focus so we can paint ourselves
     * without a border.
     * 
     * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
     */
    public void focusLost(FocusEvent arg0)
    {
        hasFocus = false;
        if (noError)
        {
            if (jmfMode)
            {
                soundtest.jmfStop();
            }
            else
            {
                soundtest.hsoundStop(snd); // Stop the player before starting
                                           // another
            }
        }
        // repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent arg0)
    {
    }

    /**
     * On VK_ENTER set remote key code. Otherwise set local key code and
     * repaint.
     * 
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent event)
    {
        if (event.getKeyCode() == KeyEvent.VK_ENTER)
        {
            // On enter set the remote keyCode.
            try
            {
                exporter.setValue(keyCode);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            // Set our local keyCode
            keyCode = event.getKeyCode();
            repaint();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent arg0)
    {

    }

}
