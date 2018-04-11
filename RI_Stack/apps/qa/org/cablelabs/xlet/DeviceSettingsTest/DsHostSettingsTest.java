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

package org.cablelabs.xlet.DeviceSettingsTest;

import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.rmi.RemoteException;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HScreen;
import org.ocap.ui.event.OCRcEvent;
import org.ocap.hardware.Host;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.hardware.device.AudioOutputPort;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;

import org.ocap.hardware.device.HostSettings;
import org.ocap.hardware.device.FeatureNotSupportedException;

import java.util.Enumeration;
import java.util.Vector;

/*
 * Warning, this xlet must have
 * MonitorAppPermission("deviceController")
 */
public class DsHostSettingsTest extends Container implements Driveable, KeyListener, Xlet
{
    // The OCAP Xlet context.
    private XletContext m_ctx;

    // A HAVi Scene.
    private HScene m_scene;

    private static VidTextBox m_vbox;

    // Test runner
    private String m_appName = "DsHostSettingsTest";

    private static String SECTION_DIVIDER = "==================================";

    // autoXlet
    private AutoXletClient m_axc = null;

    private static Logger m_log = null;

    private static Test m_test = null;

    // Host info
    private HostSettings hostRef = null;

    private HScreen hscreenDef = null;

    private boolean powerModeLow = false;

    private static final String volumeRangeStr[] = { "Narrow", "Normal", "Wide" };

    private static final int volumeRangeInt[] = { HostSettings.RANGE_NARROW, HostSettings.RANGE_NORMAL,
            HostSettings.RANGE_WIDE };

    private static int vrx = 0;

    // Let the methods begin
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("[" + m_appName + "] : initXlet() - begin");

        // initialize AutoXlet
        m_axc = new AutoXletClient(this, ctx);
        m_test = m_axc.getTest();
        if (m_axc.isConnected())
            m_log = m_axc.getLogger();
        else
            m_log = new XletLogger();

        // store off our xlet context
        m_ctx = ctx;

        // get instance of HostSettings
        hostRef = (HostSettings) Host.getInstance();

        hscreenDef = HScreen.getDefaultHScreen();
        if (hscreenDef == null)
        {
            print("No default hscreen assigned");
        }

        // Setup the application graphical user interface.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_vbox = new VidTextBox(40, 50, 530, 370, 14, 5000);
        m_scene.add(m_vbox);
        m_scene.addKeyListener(this);
        m_scene.addKeyListener(m_vbox);

        final String DS_PROPERTY_NAME = "ocap.api.option.ds";
        final String DS_SPECIFICATION_VERSION = "1.0";

        // Test if the Device Settings Extension is System defined for stack.
        String ds_property = System.getProperty(DS_PROPERTY_NAME);
        if (ds_property == null)
        {
            debugLog(DS_PROPERTY_NAME + " Not defined.");
        }
        else if (!ds_property.equals(DS_SPECIFICATION_VERSION))
        {
            debugLog("Expected version " + DS_SPECIFICATION_VERSION + " Not system version " + ds_property);
        }

        debugLog(" initXlet() - end");

    }

    public void pauseXlet()
    {
        m_scene.setVisible(false);
    }

    public void startXlet() throws XletStateChangeException
    {
        debugLog(" startXlet() - begin");

        // Display the application.
        m_scene.show();
        m_scene.requestFocus();
        // Describe tests
        printTestList();

        debugLog(" startXlet() - end");
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        m_scene.setVisible(false);
        // Clean up and dispose of resources.
        HScene tmp = m_scene;
        m_scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    private void testSetMainVideoOutputPort()
    {
        if (hostRef instanceof Host)
        {
            VideoOutputPort mainvop = hostRef.getMainVideoOutputPort(hscreenDef);
            Host host = (Host) hostRef;

            // convert enum to array by adding enum objects to vector then
            // converting vector to array
            Enumeration enm = host.getVideoOutputPorts();
            Vector v = new Vector();
            while (enm.hasMoreElements())
                v.addElement(enm.nextElement());

            // must have 2 or more video ports to go on
            if (v.size() > 1)
            {
                // convert vector to array
                VideoOutputPort[] vop = (VideoOutputPort[]) v.toArray(new VideoOutputPort[v.size()]);

                int x = 0;
                try
                {
                    // swap video[0] with video[1]
                    if (mainvop.equals(vop[0])) x = 1;

                    hostRef.setMainVideoOutputPort(hscreenDef, vop[x]);

                    // now test if there was a change
                    hscreenDef = HScreen.getDefaultHScreen();
                    mainvop = hostRef.getMainVideoOutputPort(hscreenDef);
                    notifyTestComplete(hscreenDef.equals(vop[x]) ? 0 : 1,
                            "VideoOutputPort didn't change for default HScreen");
                }
                catch (FeatureNotSupportedException e)
                {
                    debugLog("testSetMainVideoOutputPort() - caught FeatureNotSupportedException " + e);
                    notifyTestComplete(1, "FeatureNotSupportedException");
                }
            }
            else
                notifyTestComplete(1, "Not enough Video Output Ports to test with.");
        }
    }

    private void testGetMainVideoOutputPort()
    {
        String vopType;
        VideoOutputPort vop = hostRef.getMainVideoOutputPort(hscreenDef);
        if (null != vop)
        {
            switch (vop.getType())
            {
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_1394:
                    vopType = "AV_OUTPUT_PORT_TYPE_1394";
                    break;
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_BB:
                    vopType = "AV_OUTPUT_PORT_TYPE_BB";
                    break;
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO:
                    vopType = "AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO";
                    break;
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_DVI:
                    vopType = "AV_OUTPUT_PORT_TYPE_DVI";
                    break;
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_RF:
                    vopType = "AV_OUTPUT_PORT_TYPE_RF";
                    break;
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_SVIDEO:
                    vopType = "AV_OUTPUT_PORT_TYPE_SVIDEO";
                    break;
                default:
                    vopType = "<unknown>";
                    break;
            }
            print("Main Video Output Port type is " + vopType);
            notifyTestComplete(0, "testGetMainVideoOutputPort");
        }
        else
        {
            notifyTestComplete(1, "No Main Video Output Port");
        }

    }

    private void testGetAudioOutputs()
    {
        try
        {
            // get count of audio output ports
            Enumeration enm = hostRef.getAudioOutputs();
            int count = 0;
            while (enm.hasMoreElements())
            {
                AudioOutputPort aop = (AudioOutputPort) enm.nextElement();
                ++count;
            }

            // must have 1 or more audio output ports
            notifyTestComplete(count > 0 ? 0 : 1, "Audio Output Port count is zero");
        }
        catch (IllegalArgumentException iae)
        {
            debugLog(" testGetAudioOutputs() - caught IllegalArgumentException " + iae);
            notifyTestComplete(1, "Illegal Argument Exception");
        }
        catch (SecurityException se)
        {
            debugLog(" testGetAudioOutputs() - caught SecurityException exception: " + se);
            notifyTestComplete(1, "Security Exception");
        }
    }

    private void testSetPowerModeLow()
    {
        try
        {
            hostRef.setPowerMode(Host.LOW_POWER);
            powerModeLow = true;
            notifyTestComplete(0, "");
        }
        catch (IllegalArgumentException iae)
        {
            debugLog(" testSetPowerModeLow() - caught IllegalArgumentException " + iae);
            notifyTestComplete(1, "Illegal Argument Exception");
        }
        catch (SecurityException se)
        {
            debugLog(" testSetPowerModeLow() - caught SecurityException exception: " + se);
            notifyTestComplete(1, "Security Exception");
        }
    }

    private void testSetPowerModeFull()
    {
        try
        {
            hostRef.setPowerMode(Host.FULL_POWER);
            powerModeLow = false;
            notifyTestComplete(0, "");
        }
        catch (IllegalArgumentException iae)
        {
            debugLog(" testSetPowerModeFull() - caught IllegalArgumentException " + iae);
            notifyTestComplete(1, "Illegal Argument Exception");
        }
        catch (SecurityException se)
        {
            debugLog(" testSetPowerModeFull() - caught SecurityException exception: " + se);
            notifyTestComplete(1, "Security Exception");
        }
    }

    private void testSetMuteKey(boolean keyEnable)
    {
        try
        {
            hostRef.setSystemMuteKeyControl(keyEnable);
            notifyTestComplete(0, "");
        }
        catch (IllegalArgumentException iae)
        {
            debugLog(" testSetMuteKey() - caught IllegalArgumentException " + iae);
            notifyTestComplete(1, "Illegal Argument Exception");
        }
        catch (SecurityException se)
        {
            debugLog(" testSetMuteKey() - caught SecurityException exception: " + se);
            notifyTestComplete(1, "Security Exception");
        }
    }

    private void testSetSystemVolumeKeyControl(boolean keyEnable)
    {
        try
        {
            hostRef.setSystemVolumeKeyControl(keyEnable);
            notifyTestComplete(0, "");
        }
        catch (IllegalArgumentException iae)
        {
            debugLog(" testSetSystemVolumeKeyControl() - caught IllegalArgumentException " + iae);
            notifyTestComplete(1, "Illegal Argument Exception");
        }
        catch (SecurityException se)
        {
            debugLog(" testSetSystemVolumeKeyControl() - caught SecurityException exception: " + se);
            notifyTestComplete(1, "Security Exception");
        }
    }

    private void testSetSystemVolumeRange()
    {
        try
        {
            hostRef.setSystemVolumeRange(volumeRangeInt[vrx]);
            vrx = (++vrx == volumeRangeInt.length ? 0 : vrx); // bump index
            notifyTestComplete(0, "");
        }
        catch (IllegalArgumentException iae)
        {
            debugLog(" testSetSystemVolumeRange() - caught IllegalArgumentException " + iae);
            notifyTestComplete(1, "Illegal Argument Exception");
        }
        catch (SecurityException se)
        {
            debugLog(" testSetSystemVolumeRange() - caught SecurityException exception: " + se);
            notifyTestComplete(1, "Security Exception");
        }
    }

    /*
     * !!!!For AutoXlet automation framework!!!!! Method used to send completion
     * of events
     */
    public void notifyTestComplete(int result, String reason)
    {
        m_log.log("Test completed; result=" + (result == 0 ? "Passed" : "Failed"));
        m_test.assertTrue("Test failed:" + reason, result == 0);
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
        m_log.log(msg);
        m_vbox.write("    " + msg);
    }

    public void keyReleased(KeyEvent arg0)
    {
    }

    public void keyTyped(KeyEvent arg0)
    {
    }

    public void dispatchEvent(KeyEvent event, boolean useMonitor, int timeout) throws RemoteException
    {
        keyPressed(event);
    }

    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();

        // when power mode is low, then any key press will wake us up
        if (powerModeLow) testSetPowerModeFull();

        switch (key)
        {
            case OCRcEvent.VK_1:
                print("Get Audio Output Ports List");
                testGetAudioOutputs();
                break;
            case OCRcEvent.VK_2:
                print("Get Main Video Output Port");
                testGetMainVideoOutputPort();
                break;
            case OCRcEvent.VK_3:
                print("Set Main Video Output Port");
                testSetMainVideoOutputPort();
                break;
            case OCRcEvent.VK_4:
                print("Set Power Mode to Low...bye bye");
                testSetPowerModeLow();
                break;
            case OCRcEvent.VK_5:
                print("Set Power Mode to Full");
                testSetPowerModeFull();
                break;
            case OCRcEvent.VK_6:
                print("Set System Mute Key Control - Disable key - (Press Mute key)");
                testSetMuteKey(false);
                break;
            case OCRcEvent.VK_7:
                print("Set System Mute Key Control - Enable key - (Press Mute key)");
                testSetMuteKey(true);
                break;
            case OCRcEvent.VK_8:
                print("Set System Volume Key Control - Enable key - (Press Volume key)");
                testSetSystemVolumeKeyControl(true);
                break;
            case OCRcEvent.VK_9:
                print("Set System Volume Key Control - Disable key - (Press Volume key)");
                testSetSystemVolumeKeyControl(false);
                break;
            case OCRcEvent.VK_0:
                print("Set System Volume Range " + volumeRangeStr[vrx]);
                testSetSystemVolumeRange();
                break;

            case OCRcEvent.VK_INFO:
                printTestList();
                break;
            case OCRcEvent.VK_EXIT:
                print("Not Exiting");
                break;
            case OCRcEvent.VK_MUTE:
                print("MUTE key pressed");
                break;
            case OCRcEvent.VK_VOLUME_DOWN:
            case OCRcEvent.VK_VOLUME_UP:
                print("Volume key pressed");
                break;
            default:
                break;
        }
    }

    // 
    // display info on xlet usage
    // 
    private void printTestList()
    {
        print("");
        print(SECTION_DIVIDER);
        print("Press <1> to Get Audio Outputs List");
        print("Press <2> to Get Main Video Output Port");
        print("Press <3> to Set Main Video Output Port");
        print("Press <4> to Set Power Mode Low");
        print("Press <5> to Set Power Mode Full");
        print("Press <6> to Set System Mute Key Control - Disable Mute key");
        print("Press <7> to Set System Mute Key Control - Enable Mute key");
        print("Press <8> to Set System Volume Key Control - Enable Volume key");
        print("Press <9> to Set System Volume Key Control - Disable Volume key");
        print("Press <0> to Cycle thru Set System Volume Range Narrow, Normal, Wide [" + volumeRangeStr[vrx] + "]");
        print("Press <INFO> Display HostSettings Options");
        print("Press <MUTE> for Mute key detected message");
        print("Press <VOLUME> for Volume key detected message");
    }

}// end of all

