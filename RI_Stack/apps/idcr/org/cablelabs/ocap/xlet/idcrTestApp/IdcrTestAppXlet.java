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

// Declare package.
package org.cablelabs.ocap.xlet.idcrTestApp;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.ocap.ui.event.OCRcEvent;

/*
 *  class IdcrTestAppXlet
 *
 * This sample application demonstrates how to draw simple graphics on the screen and
 * handle remote control key presses 
 *
 */
public class IdcrTestAppXlet extends Component implements Xlet, KeyListener
{
    private static final long serialVersionUID = 1;

    private static final Font FONT = new Font("sansserif", Font.BOLD, 32);

    private static final int TEXT_START = 350;

    private HScene m_scene;

    private int m_numObjects = 4; // Initial number of objects

    private int m_diameter = 40; // Initial diameter of objects

    public void initXlet(XletContext c) throws XletStateChangeException
    {
        try
        {
            // create the scene
            m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());

            // We extend component, so add to the scene
            m_scene.add(this);

            // Set component bounds
            setBounds(0, 0, 640, 480);

            // Set font used for text
            setFont(FONT);

            // Register a key listener
            m_scene.addKeyListener(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /**
     * startXlet
     * 
     * Called by the system when the app is suppose to actually start.
     * 
     */
    public void startXlet() throws XletStateChangeException
    {
        try
        {
            // Make the scene visible
            m_scene.setVisible(true);

            // Make this scene gain focus
            m_scene.requestFocus();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /**
     * pauseXlet
     * 
     * Called by the system when the user has performed an action requiring this
     * application to pause for another ,
     */
    public void pauseXlet()
    {
        // Make the scene invisible since we're pausing
        m_scene.setVisible(false);
    }

    /**
     * destroyXlet
     * 
     * Called by the system when the application needs to exit and clean up.
     * 
     */
    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        try
        {
            // Hide screen
            m_scene.setVisible(false);

            // Remove key listener
            m_scene.removeKeyListener(this);

            // Remove everything else
            m_scene.removeAll();

            // Give up resources
            m_scene.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    public void paint(Graphics g)
    {
        int i;
        int x;
        int circleSpace;
        int space;

        // Draw the text
        g.setColor(Color.green);
        g.drawString("Press 1-9 to change number of circles", 0, TEXT_START);
        FontMetrics fm = g.getFontMetrics(FONT);
        int h = fm.getHeight();
        g.drawString("Volume up increases circle size", 0, TEXT_START + h);
        g.drawString("Volume down decreases circle size", 0, TEXT_START + h + h);

        // draw circle objects according to number key pressed
        circleSpace = m_numObjects * m_diameter;
        space = (640 - circleSpace) / (m_numObjects + 1);

        g.setColor(Color.red);
        for (i = 0; i < m_numObjects; i++)
        {
            x = space * (i + 1) + m_diameter * i;
            g.fillOval(x, 100, m_diameter, m_diameter);
        }

    }

    public void keyTyped(KeyEvent e)
    {
        // Do nothing
    }

    public void keyReleased(KeyEvent e)
    {
        // Do nothing
    }

    public void keyPressed(KeyEvent e)
    {
        message("~~~~ IdcrTestApp recieved awt event" + e.getKeyCode());

        // Handle the key event
        boolean repaint = true;
        switch (e.getKeyCode())
        {
            case OCRcEvent.VK_1:
                m_numObjects = 1;
                break;
            case OCRcEvent.VK_2:
                m_numObjects = 2;
                break;
            case OCRcEvent.VK_3:
                m_numObjects = 3;
                break;
            case OCRcEvent.VK_4:
                m_numObjects = 4;
                break;
            case OCRcEvent.VK_5:
                m_numObjects = 5;
                break;
            case OCRcEvent.VK_6:
                m_numObjects = 6;
                break;
            case OCRcEvent.VK_7:
                m_numObjects = 7;
                break;
            case OCRcEvent.VK_8:
                m_numObjects = 8;
                break;
            case OCRcEvent.VK_9:
                m_numObjects = 9;
                break;
            case OCRcEvent.VK_0:
                m_numObjects = 0;
                break;
            case OCRcEvent.VK_VOLUME_DOWN:
                m_diameter -= 10;
                if (m_diameter <= 0) m_diameter = 5;
                break;
            case OCRcEvent.VK_VOLUME_UP:
                m_diameter += 10;
                break;
            default:
                repaint = false;
                break;
        }

        // We have to repaint the screen to handle any changes
        if (repaint) // TODO: should be able to repaint any time, but breaks
                     // idcr
            repaint();// demo app work this out
    }

    protected void message(String s)
    {
        System.out.println(s);
    }

}
