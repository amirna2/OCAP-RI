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

/**
 * File Name: OCVET72TestXlet.java
 *
 * This Xlet verifies that VK_RC_LOW_BATTERY, VK_USER and VK_CC have been added to 
 * the OCAP RemoteControl Key Event Codes as part of the OCVET-72 implementation. 
 * This xlet maps VK_RC_LOW_BATTERY, VK_USER and VK_CC to the remote control keys 
 * VK_1, VK_2 and VK_3, respectively. When one of the mapped keys is pressed a 
 * OCRcEvent keypress is generated for the corresponding, newly implemented, key. 
 * When the keyListener identifies one of these keyPresses a message is printed to the GUI.
 *
 *
 * Typical test run:
 *
 * 1. From $PLATFORMROOT enter:
 *    >./runRI.sh -setup -xlet OCVET72Test
 *
 * 2. Test Control:
 * 
 *   Key "INFO"   - print test usage info
 *   Key "1"      - generate a VK_RC_LOW_BATTERY keypress 
 *   Key "2"      - generate a VK_USER keypress
 *   Key "3"      - generate a VK_CC keypress
 *
 * 3. Sample Test Case:
 *
 *   - Press the key "1". Verify that the GUI shows that a VK_RC_LOW_BATTERY keypress was generated.
 *   - Press the key "2". Verify that the GUI shows that a VK_USER keypress was generated. 
 *   - Press the key "3". Verify that the GUI shows that a VK_CC keypress was generated.
 */

package org.cablelabs.xlet.OCVET72Test;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.event.HRcEvent;
import org.ocap.ui.event.OCRcEvent;

import org.cablelabs.test.autoxlet.*;
import org.cablelabs.lib.utils.VidTextBox;

public class OCVET72TestXlet extends Container implements Xlet, KeyListener, Driveable
{
    private String m_appName = "OCVET72Test";
    private HScene m_scene = null;
    private static VidTextBox m_vbox;

    // autoXlet
    private AutoXletClient m_axc = null;
    private static Logger m_log = null;
    private static Test m_test = null;
    private static Monitor m_eventMonitor = null;

    /**
     * Sets up the scene to display text strings.
     * 
     * @param context
     *            XletContext
     * @throws XletStateChangeException
     */
    public void initXlet(XletContext context) throws XletStateChangeException
    {
        System.out.println("[" + m_appName + "] : initXlet() - begin");

        // Set up the AutoXlet mechanism and populate local Test and Logger
        // references.
        m_axc = new AutoXletClient(this, context);
        m_test = m_axc.getTest();
        if (m_axc.isConnected())
        {
            m_log = m_axc.getLogger();
        }
        else
        {
            m_log = new XletLogger();
        }

        //used by eventDispatcher
	m_eventMonitor=new Monitor(); 

        // Setup the application graphical user interface
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_vbox = new VidTextBox(40, 50, 530, 370, 14, 5000);
        m_scene.add(m_vbox);
        m_scene.addKeyListener(this);
        m_scene.addKeyListener(m_vbox);

        debugLog(" initXlet() - end");
    }

    /**
     * @see javax.tv.xlet.Xlet#startXlet()
     * @throws XletStateChangeException
     */
    public void startXlet() throws XletStateChangeException
    {
        debugLog(" startXlet() - end");
        printTestUsage();

        m_scene.show();
        m_scene.requestFocus();

        debugLog(" startXlet() - end");
    }

    /**
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        m_scene.setVisible(false);
    }

    /**
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        if (m_scene != null)
        {
            m_scene.remove(this);
            m_scene.setVisible(false);
            HSceneFactory.getInstance().dispose(m_scene);
            m_scene = null;
        }
    }

    public void keyTyped(KeyEvent e)
    {
        //Do nothing. 
    }

    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();
        int xKey;

        switch (key)
        {
            // Print test usage.
            case OCRcEvent.VK_INFO:
                printTestUsage();
                break;
            //******** actual key press *************** 
            // For key press VK_1, 2 or 3, generate simulated key press for new OCVET-72 keys.

            // If VK_1 is pressed generate a VK_RC_LOW_BATTERY keyPress event.
            case OCRcEvent.VK_1:
                print("VK_1 key pressed - translates to VK_RC_LOW_BATTERY");
                xKey = OCRcEvent.VK_RC_LOW_BATTERY;
                generateKeyPress(xKey);
                break;
            // If VK_2 is pressed generate a VK_USER keyPress event.
            case OCRcEvent.VK_2:
                print("VK_2 key pressed - translates to VK_USER");
                xKey = OCRcEvent.VK_USER;
                generateKeyPress(xKey);
                break;
            // If VK_3 is pressed generate a VK_CC keyPress event.
            case OCRcEvent.VK_3:
                print("VK_3 key pressed - translates to VK_CC");
                xKey = OCRcEvent.VK_CC;
                generateKeyPress(xKey);
                break;
            //******** simulated key press ***************
            // Verify that simulated key press was generated.

            // Print message if simulated VK_CC is dispatched.
            case OCRcEvent.VK_CC:
                print("VK_CC key press generated");
                break;
            // Print message if simulated VK_USER is dispatched.
            case OCRcEvent.VK_USER:
                print("VK_USER key press generated");
                break;
            // Print message if simulated VK_RC_LOW_BATTERY is dispatched.
            case OCRcEvent.VK_RC_LOW_BATTERY:
                print("VK_RC_LOW_BATTERY key press generated");
                break;
            default:
                break;
        }
    }

    public void keyReleased(KeyEvent e)
    {
        // Do nothing.
    }

    /**
     * @param g
     *            Graphics
     */
    public void printTestUsage()
    {
        print("   *********** OCVET72 Test **********");
        print("   USAGE: (see readme.txt for additional details)");
        print("   Press \"INFO\" to print this test usage info");
        print("   Press \"1\" to test VK_RC_LOW_BATTERY");
        print("   Press \"2\" to test VK_USER");
        print("   Press \"3\" to test VK_CC");
        print("");
        print("  ******************************");
    }

    public void dispatchEvent(KeyEvent k, boolean b, int I)
    {
       // Do nothing
    }

    /**
     * logging function - allow messages to post to teraterm and autoxlet logs
     */
    private void debugLog(String msg)
    {
        m_log.log("[" + m_appName + "] :" + msg);
    }

    /**
     *printing function - allow messages to post in screen and log
     */
    private void print(String msg)
    {
        debugLog(msg);
        m_vbox.write("    " + msg);
    }

    public HScene getScene()
    {
        return m_scene;
    }

    /**
     * Generate keypress function- maps keys on remote to keys being 
     * impemented with OCVET-72: VK_USER, VK_LOW_RC_BATTERY and VK_CC.
     */

    public void generateKeyPress(int simulatedKey)
    {    
	//used by eventDispatcher    
        long currentTime = System.currentTimeMillis(); 
        // The OCAP RI GUI	
        HScene scene = HSceneFactory.getInstance().getDefaultHScene();  
    
        // Create a new OCRcEvent key press          
        KeyEvent k = new OCRcEvent(scene,                             
                           KeyEvent.KEY_PRESSED,
                           currentTime,
                           0,
			    // The key code for this KeyEvent
                           simulatedKey,
                           KeyEvent.CHAR_UNDEFINED);
        print("dispatch key code " + simulatedKey);
        // Dispatch the Event to the UI
	scene.dispatchEvent(k);                        
    }
}
