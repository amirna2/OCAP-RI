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

package org.cablelabs.test.media;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.net.URL;

import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.protocol.DataSource;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;

public class HNTestXlet extends Component implements Xlet
{
    private static final long serialVersionUID = 1;

    private static final Color TEXT_COLOR = new Color(255, 255, 230);

    private static final Color BG_COLOR = new Color(100, 100, 100);

    private static final Font FONT = new Font("tiresias", Font.PLAIN, 40);

    private static final Rectangle BOX_RECT = new Rectangle(115, 175, 410, 150);

    private HScene testScene;

    private static String testName = "HNTestXlet";

    private boolean testResult = false;

    private String testResultString = null;

    public void initXlet(XletContext c) throws XletStateChangeException
    {
        try
        {
            //
            // First we need to create a scene. This HAVi scene will handle all
            // of the basic setup for getting the xlet to be painted on the
            // screen
            testScene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());

            //
            // Since our xlet class is also a component we can simply
            // add this class to the scene and then it's paint method
            // will be called any time the xlet paints
            testScene.add(this);

            //
            // We now need to tell the component how big it is so that the paint
            // method knows when to call it. In this case we will make the
            // component
            // the entire size of the screen.
            setBounds(0, 0, 640, 480);

            setFont(FONT);

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

            URL remoteURL = new URL("http://127.0.0.1:8080/mpeg/traffic.mpg");
            MediaLocator ml = new MediaLocator(remoteURL);

            DataSource ds1 = Manager.createDataSource(ml);
            DataSource ds2 = Manager.createDataSource(remoteURL);

            if (null == ds1)
            {
                logTest("ds1 is null!");
                testResult = false;
            }
            else
            {
                logTest("ds1 is NOT null!");
                testResult = true;
            }

            if (null == ds2)
            {
                logTest("ds2 is null!");
                testResult = false;
            }
            else
            {
                logTest("ds2 is NOT null!");
                testResult = true;
            }

            try
            {
                Player p1 = Manager.createPlayer(ds1);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                testResult = false;
            }

            try
            {
                Player p2 = Manager.createPlayer(ds2);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                testResult = false;
            }

            try
            {
                Player p3 = Manager.createPlayer(ds1);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                testResult = false;
            }

            try
            {
                Player p4 = Manager.createPlayer(ds2);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                testResult = false;
            }

            //
            // When the app is finished loading and is ready to start
            // or the app is being unpaused this startXlet method is
            // called. It's important to properly handle all of your
            // visible components in the start and pause methods since
            // when those are called your app is working just after or
            // just before another application.
            testScene.setVisible(true);

            //
            // We also need to tell the application that our default
            // scene is going to have focus so all key events should
            // be sent to it
            testScene.requestFocus();

            repaint();
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
        //
        // When we pause the app we want to clear the screen so that
        // apps running after ours don't interfere with the graphics
        // that we have placed on the screen.
        testScene.setVisible(false);
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
            // hide myself so that the screen gets cleared
            testScene.setVisible(false);

            // cleanup so garbage collector can free resources
            testScene.removeAll();

            // release the resources
            testScene.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.Component#paint(java.awt.Graphics)
     */
    public void paint(Graphics g)
    {
        //
        // Draw a round rectangle behind the text
        g.setColor(BG_COLOR);
        g.fillRoundRect(BOX_RECT.x, BOX_RECT.y, BOX_RECT.width, BOX_RECT.height, 20, 20);

        //
        // Draw our text label
        g.setColor(TEXT_COLOR);

        // center the text
        int textHeight = getFontMetrics(FONT).getHeight();
        int x = (BOX_RECT.width - getFontMetrics(FONT).stringWidth(testName)) / 2;
        g.drawString(testName, BOX_RECT.x + x, BOX_RECT.y + textHeight + 10);

        if (testResult)
        {
            testResultString = "Test Passed!";
            g.setColor(new Color(16, 240, 16));
        }
        else
        {
            testResultString = "Test Failed!";
            g.setColor(new Color(240, 16, 16));
        }

        int xResult = (BOX_RECT.width - getFontMetrics(FONT).stringWidth(testResultString)) / 2;
        g.drawString(testResultString, BOX_RECT.x + xResult, BOX_RECT.y + textHeight + 60);
    }

    public void logTest(String s)
    {
        System.out.println(s);// "HNTestXlet: " + s);
    }
}
