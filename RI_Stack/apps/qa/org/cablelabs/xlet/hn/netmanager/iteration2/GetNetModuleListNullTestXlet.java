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

package org.cablelabs.xlet.hn.netmanager.iteration2;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.ui.event.OCRcEvent;

/**
 * GetNetModuleListNullTestXlet - This test calls
 * <code>NetManager.getNetModuleList()</code> passing null into the method. All
 * of the <code>NetModules</code> on the network are returned in the form of a
 * <code>NetList</code>. Pressing the SELECT key on the remote control causes
 * the test to go out and query the <code>NetManager</code> for the list of
 * <code>NetModules</code> which are then printed out to the console. The test
 * will only display a failure message if it is unable to get a handle to the
 * <code>NetManager</code>. Analysis of the log output is required to determine
 * if the test was successful.
 * 
 * @author Ben Foran (Flashlight Engineering and Consulting)
 * 
 */
public class GetNetModuleListNullTestXlet extends Component implements Xlet, KeyListener
{
    private static final long serialVersionUID = 1;

    private static final Color TEXT_COLOR = new Color(255, 255, 230);

    private static final Color BG_COLOR = new Color(100, 100, 100);

    private static final Font FONT = new Font("tiresias", Font.PLAIN, 30);

    private static final Rectangle BOX_RECT = new Rectangle(95, 150, 450, 200);

    private HScene testScene;

    private static String testName = "GetNetModuleList(Null)TestXlet";

    private boolean testResult = false;

    private String testResultString = null;

    private NetManager instance = null;

    public void initXlet(XletContext c) throws XletStateChangeException
    {
        try
        {

            // setup scene
            testScene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
            testScene.add(this);
            setBounds(0, 0, 640, 480);
            setFont(FONT);

            // register as a key listener
            testScene.addKeyListener(this);

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
     */
    public void startXlet() throws XletStateChangeException
    {
        try
        {
            instance = NetManager.getInstance();
            if (instance == null)
            {
                testResult = false;
            }
            else
            {
                NetList netModuleList = instance.getNetModuleList(null);

                if (netModuleList == null)
                {
                    testResult = false;
                }
                else
                {
                    testResult = true;
                }
            }

            testScene.setVisible(true);
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
     * application to pause for another
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
            testScene.setVisible(false);

            testScene.removeAll();

            // release resources
            testScene.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    public void getNetModuleList()
    {
        NetList netModuleList = instance.getNetModuleList(null);

        System.out.println("GetNetModuleListNullTest: Print list of net modules");
        System.out.println("GetNetModuleListNullTest: net module list size - " + netModuleList.size());

        Enumeration netModuleListEnumeration = netModuleList.getElements();
        while (netModuleListEnumeration.hasMoreElements())
        {
            System.out.println("GetNetModuleListNullTest: netModule - "
                    + netModuleListEnumeration.nextElement().toString());
        }
    }

    /**
     * (non-Javadoc)
     * 
     * @see java.awt.Component#paint(java.awt.Graphics)
     */
    public void paint(Graphics g)
    {
        // Draw a round rectangle behind the text
        g.setColor(BG_COLOR);
        g.fillRoundRect(BOX_RECT.x, BOX_RECT.y, BOX_RECT.width, BOX_RECT.height, 20, 20);

        // Draw our text label
        g.setColor(TEXT_COLOR);

        // center the text
        int textHeight = getFontMetrics(FONT).getHeight();
        int x = (BOX_RECT.width - getFontMetrics(FONT).stringWidth(testName)) / 2;
        g.drawString(testName, BOX_RECT.x + x, BOX_RECT.y + textHeight + 5);

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
        g.drawString(testResultString, BOX_RECT.x + xResult, BOX_RECT.y + textHeight + 45);
    }

    public void keyPressed(KeyEvent keyEvent)
    {
        switch (keyEvent.getKeyCode())
        {
            case OCRcEvent.VK_ENTER:
                getNetModuleList();
                break;
            default:
                break;
        }
    }

    public void keyReleased(KeyEvent keyEvent)
    {
        // no op
    }

    public void keyTyped(KeyEvent keyEvent)
    {
        // no op
    }

}
