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
 * File Name: PowerKeyTestXlet.java
 * 
 * 
 * This Xlet tests the power key event listener by allowing
 * the registration and unregistration of event listeners 
 * and by displaying the results on the screen. 
 * 
 * Usage:
 * 
 * Key "INFO"   - Print test usage info
 * Key "1"      - register a listener 
 * Key "2"      - unregister a listener
 * Key "3"      - perform synchronous call to get power status
 * Key "Power"  - remote/front-panel toggles the power state
 *   
 * 
 * Typical test run:
 * 
 * 1. Load the xlet and press key "1" to register the listener.
 * 2. Press the "Power" key and verify that the xlet receives
 *    power key events. The values displayed on the screen should
 *    toggle between LOW and FULL power. The cached value should 
 *    be opposite the event value which means that the application 
 *    was in one state before it received the notification of the
 *    state change event. 
 * 3. Unregister the listener by pressing the key "2". Verify that 
 *    no state changes happen when the power key is pressed. 
 * 4. Use the key "3" to retrieve the current state from the STB 
 *    via the synchronous call. With this key both the current state
 *    and the event state should match because there was no state 
 *    change at the system level. This is just a call to get the 
 *    actual status.
 *    
 * Logs are also available to confirm the behaviors described above.
 * If necessary, enable the MPE logs by changing the mpeenv.ini file 
 * and enabling the logs (ALL DEBUG) in the UTIL module. This will 
 * show the consistency between the mpe layer and the notification 
 * events received by the registered client.
 * 
 * Date: 13 Septebmer 2005 
 * Author: Francesco Dorigo
 * @author francesco 
 */

package org.cablelabs.xlet.PowerKeyTest;

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

import org.ocap.hardware.Host;
import org.ocap.hardware.PowerModeChangeListener;
import org.cablelabs.test.autoxlet.*;
import org.cablelabs.lib.utils.VidTextBox;

public class PowerKeyTestXlet extends Container implements Xlet, KeyListener, Driveable
{
    private String m_appName = "PowerKeyTest";
    private HScene m_scene = null;
    private static VidTextBox m_vbox;
    
    private PowerChangeListenerTest pmclt = null;
    private Host host = null;
    private int currentPowerMode;
    private int previousPowerMode;

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

        m_eventMonitor=new Monitor(); //used by eventDispatcher

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

        host = Host.getInstance();
        currentPowerMode = host.getPowerMode();
        previousPowerMode = currentPowerMode;

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
        // TODO Auto-generated method stub
    }

    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();

        switch (key)
        {
            // Remote/front-panel toggle of the power state.
            case HRcEvent.VK_INFO:
                printTestUsage();
                break;

            case HRcEvent.VK_POWER:
                print("POWER key pressed" +listenerRegistered());
                if (pmclt == null)
                {
                    getSynchronousPowerState();
                }

                // native code is bypassed when run in automation mode, 
                // hence test assertion does not make sense here.
                //m_test.assertTrue("test failed; currentPowerMode (" +currentPowerMode +") should NOT equal previousPowerMode (" +previousPowerMode +")", currentPowerMode!=previousPowerMode);
                if (currentPowerMode == previousPowerMode) 
                {
                    print("test failed; currentPowerMode (" +currentPowerMode +") should NOT equal previousPowerMode (" +previousPowerMode +")");
                }
                break;

            // Register a listener.
            case HRcEvent.VK_1:
                print("VK_1 key pressed");
                addPowerKeyListener();
                break;

            // Unregister a listener.
            case HRcEvent.VK_2:
                print("VK_2 key pressed");
                removePowerKeyListener();
                break;

            // Perform synchronous call to get power status
            case HRcEvent.VK_3:
                print("VK_3 key pressed" +listenerRegistered());
                getSynchronousPowerState();

                // Verify, that the current power state and the event power
                // state match.
                m_test.assertTrue("test failed; currentPowerMode (" +currentPowerMode +") does NOT equal previousPowerMode (" +previousPowerMode +")", currentPowerMode==previousPowerMode);
                break;

            default:
                break;
        }
    }

    public void keyReleased(KeyEvent e)
    {
        // TODO Auto-generated method stub
    }

    /**
     * @param g
     *            Graphics
     */
    public void printTestUsage()
    {
        print("   *********** Power Key Test **********");
        print("   USAGE: (see readme.txt for additioanl details)");
        print("   Press \"INFO\" to print this test usage info");
        print("   Press \"1\" to register the event listener");
        print("   Press \"2\" to un-register the event listener");
        print("   Press \"3\" to perform synchronous call to get current power mode");
        print("   Press \"POWER\" either on the remote or the front panel to toggle the power mode");
        print("   If there is a registered listener, the screen should be updated with the values of the power state change events.");
        print("   If no listener has been registered, then no event should be received.");
        print("   The synchronous call (key 3) should always report the new power state");

        print("");
        print("   Cached Power Mode = " +translatePowerLevel(previousPowerMode) +" (" +previousPowerMode+")");
        print("   Current Power Mode = " +translatePowerLevel(currentPowerMode) +" (" +previousPowerMode+")");

        print("  ******************************");
    }

    public String listenerRegistered()
    {
        if (pmclt == null)
        {
            return("  (No PowerModeChangeListener registered)");
        }
        return("  (PowerModeChangeListener registered)");
    }

    private void addPowerKeyListener()
    {
        if (pmclt == null)
        {
            pmclt = new PowerChangeListenerTest();
            host.addPowerModeChangeListener(pmclt);
        }
    }

    private void removePowerKeyListener()
    {
        if (pmclt != null)
        {
            host.removePowerModeChangeListener(pmclt);
            pmclt = null;
        }
    }


    private void getSynchronousPowerState()
    {
        previousPowerMode = currentPowerMode;
        currentPowerMode = host.getPowerMode();

        print("   Synchronous Power Change Call: " +translatePowerLevel(currentPowerMode) +" (" +currentPowerMode+")" +";    previousPowerMode="+previousPowerMode);

    }


    public String translatePowerLevel(int powerMode)
    {
        String returnStr="LOW POWER";

        switch (powerMode)
        {
            case Host.FULL_POWER:
                returnStr="FULL POWER";
                break;
            case Host.LOW_POWER:
                returnStr="LOW POWER";
                break;
            default:
                break;
        }
        return returnStr;
    }

    public void dispatchEvent(KeyEvent e, boolean useMonitor, int timeout)
    {
        if (useMonitor)
        {
            m_eventMonitor.setTimeout(timeout);
            synchronized (m_eventMonitor)
            {
                keyPressed(e);
                m_eventMonitor.waitForReady();
            }
        }
        else
        {
            keyPressed(e);
        }
    }


    //
    // logging function - allow messages to post to teraterm and autoxlet logs
    //
    private void debugLog(String msg)
    {
        m_log.log("[" + m_appName + "] :" + msg);
    }


    //
    // printing function - allow messages to post in screen and log
    //
    private void print(String msg)
    {
        //CHEIRE:m_log.log("\t" + msg);
        debugLog(msg);
        m_vbox.write("    " + msg);
    }

    public HScene getScene()
    {
        return m_scene;
    }




    private class PowerChangeListenerTest implements PowerModeChangeListener
    {
        /**
         * Called when the power mode changes (for example from full to low
         * power).
         * 
         *@see Host#FULL_POWER
         *@see Host#LOW_POWER
         */
        public void powerModeChanged(int newPowerMode)
        {
            previousPowerMode = currentPowerMode;
            currentPowerMode = newPowerMode;

            print("   in powerModeChanged(): Power Change Async Event received: " +translatePowerLevel(currentPowerMode) +" (" +currentPowerMode+")" +";    previousPowerMode="+previousPowerMode);


            // native code is bypassed when run in automation mode, hence
            // test assertion does not make sense here.
            //m_test.assertTrue("test failed; currentPowerMode (" +currentPowerMode +") should NOT equal previousPowerMode (" +previousPowerMode +")", currentPowerMode!=previousPowerMode);

            if (currentPowerMode == previousPowerMode) 
            {
                print("in powerModeChanged(): test failed; currentPowerMode (" +currentPowerMode +") should NOT equal previousPowerMode (" +previousPowerMode +")");
            }
        }
    }
}
