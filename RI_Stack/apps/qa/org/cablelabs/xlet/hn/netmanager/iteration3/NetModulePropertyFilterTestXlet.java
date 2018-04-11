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

package org.cablelabs.xlet.hn.netmanager.iteration3;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Properties;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.NetModule;
import org.ocap.hn.PropertyFilter;

/**
 * NetModulePropertyFilterTestXlet - This test calls the
 * <code>NetManager.getNetModuleList()</code> passing null into the method. All
 * of the <code>NetModules</code> on the network are returned in the form of a
 * <code>NetList</code>. Then the test creates a <code>PropertyFilter</code> and
 * calls the <code>PropertyFilter.accept()</code> method passing in each of the
 * <code>NetModules</code>. A message is printed to the console indicating if
 * each <code>NetModule</code> was accepted by the <code>PropertyFilter</code>.
 * The test will fail if it is unable to get a handle on the
 * <code>NetManager</code> or there are no <code>NetModules</code> present on
 * the network. Analysis of the log output is required to determine if the test
 * was successful.
 * 
 * @author Ben Foran (Flashlight Engineering and Consulting)
 * 
 */
public class NetModulePropertyFilterTestXlet extends Component implements Xlet
{
    private static final long serialVersionUID = 1;

    private static final Color TEXT_COLOR = new Color(255, 255, 230);

    private static final Color BG_COLOR = new Color(100, 100, 100);

    private static final Font FONT = new Font("tiresias", Font.PLAIN, 30);

    private static final Rectangle BOX_RECT = new Rectangle(95, 150, 450, 200);

    private HScene testScene;

    private static String testName = "NetModulePropertyFilterTestXlet";

    private boolean testResult = false;

    private boolean displayResult = false;

    private String testResultString = null;

    public void initXlet(XletContext c) throws XletStateChangeException
    {
        try
        {
            // setup the scene
            testScene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
            testScene.add(this);
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
     */
    public void startXlet() throws XletStateChangeException
    {
        try
        {
            testScene.setVisible(true);
            testScene.requestFocus();

            repaint();

            NetManager instance = NetManager.getInstance();
            if (instance == null)
            {
                testResult = false;
            }
            else
            {
                int retryCount = 10;

                NetList netModuleList = instance.getNetModuleList(null);

                while (netModuleList.size() == 0 && retryCount > 0)
                {
                    System.out.println("NetModulePropertyFilterTestXlet: NetModule list empty - sleeping for 30 seconds");

                    // sleep for 30 seconds before checking to see if there are
                    // devices
                    Thread.sleep(30000);

                    netModuleList = instance.getNetModuleList(null);

                    retryCount--;

                    System.out.println("NetModulePropertyFilterTestXlet: retry count - " + retryCount);
                }

                if (netModuleList.size() == 0)
                {
                    testResult = false;
                    displayResult = true;
                }
                else
                {
                    Properties netModuleProperties = new Properties();

                    netModuleProperties.put(NetModule.PROP_NETMODULE_TYPE,
                            "urn:schemas-upnp-org:service:ScheduledRecording:1");

                    PropertyFilter netModulePropertyFilter = new PropertyFilter(netModuleProperties);

                    // Verify that the property filter was successfully created.
                    if (!netModulePropertyFilter.contains(NetModule.PROP_NETMODULE_TYPE))
                    {
                        System.out.println("NetModulePropertyFilterTestXlet: PropertyFilter NOT created successfully");

                        testResult = false;
                        displayResult = true;

                    }
                    else
                    {
                        System.out.println("NetModulePropertyFilterTestXlet: PropertyFilter created successfully");

                        Enumeration netModuleListEnumeration = netModuleList.getElements();

                        while (netModuleListEnumeration.hasMoreElements())
                        {
                            NetModule netModule = (NetModule) netModuleListEnumeration.nextElement();

                            System.out.println("NetModulePropertyFilterTestXlet: device - " + netModule.toString()
                                    + " accepted by filter - " + netModulePropertyFilter.accept(netModule));
                        }

                        testResult = true;
                        displayResult = true;
                    }
                }
            }

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

        g.setColor(TEXT_COLOR);

        // center the text
        int textHeight = getFontMetrics(FONT).getHeight();
        int x = (BOX_RECT.width - getFontMetrics(FONT).stringWidth(testName)) / 2;
        g.drawString(testName, BOX_RECT.x + x, BOX_RECT.y + textHeight + 5);

        if (displayResult)
        {
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
        else
        {
            String deviceWaitString = "Waiting for NetModules ...";
            int xResult = (BOX_RECT.width - getFontMetrics(FONT).stringWidth(deviceWaitString)) / 2;
            g.drawString(deviceWaitString, BOX_RECT.x + xResult, BOX_RECT.y + textHeight + 45);
        }
    }
}
