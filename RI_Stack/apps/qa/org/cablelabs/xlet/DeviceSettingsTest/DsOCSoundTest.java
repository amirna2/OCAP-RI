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
import java.util.Enumeration;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.ocap.hardware.IEEE1394Node;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.ui.event.OCRcEvent;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;

import org.ocap.hardware.device.AudioOutputPort;
import org.ocap.hardware.device.OCSound;

public class DsOCSoundTest extends Container implements Xlet, KeyListener, Driveable
{
    // The OCAP Xlet context.
    private XletContext m_ctx;

    // A HAVi Scene.
    private HScene m_scene;

    private static VidTextBox m_vbox;

    // Test runner
    private String m_appName = "DsOCSoundTest";

    private static String SECTION_DIVIDER = "==================================";

    // autoXlet
    private AutoXletClient m_axc = null;

    private static Logger m_log = null;

    private static Test m_test = null;

    // OCSound
    private static OCSound m_ocsound = null;

    private static float m_gain_level = 0.0f;

    private static AudioOutputPort m_base_audioOutputPorts[] = null;

    private static AudioOutputPort m_removed_audioOutputPorts = null;

    // Let the methods begin

    // @Override
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        m_scene.setVisible(false);
        // Clean up and dispose of resources.
        HScene tmp = m_scene;
        m_scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

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
        else if (m_ocsound == null)
        {
            m_ocsound = new OCSound();
            m_base_audioOutputPorts = m_ocsound.getAudioOutputs();
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

    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();

        switch (key)
        {
            case OCRcEvent.VK_1:
                print("Add Audio Output");
                testAddAudioOutput();
                break;
            case OCRcEvent.VK_2:
                print("Remove Audio Output");
                testRemoveAudioOutput();
                break;
            case OCRcEvent.VK_3:
                print("Set Muted");
                testMute(true);
                break;
            case OCRcEvent.VK_4:
                print("UnSet Muted");
                testMute(false);
                break;
            case OCRcEvent.VK_5:
                print("Change Gain Level to " + m_gain_level);
                testChangeGainLevel();
                break;

            case OCRcEvent.VK_INFO:
                printAllOCSound();
                break;
            case OCRcEvent.VK_EXIT:
                print("Not Exiting");
                break;
            default:
                break;
        }
    }

    private void testRemoveAudioOutput()
    {
        String msg = "Remove Audio Output Port";
        if (m_base_audioOutputPorts.length < 2)
        {
            print("Not enough ports to remove from: " + m_base_audioOutputPorts.length);
        }
        else if (null == m_removed_audioOutputPorts)
        {
            m_ocsound.removeAudioOutput(m_base_audioOutputPorts[1]);
            m_removed_audioOutputPorts = m_base_audioOutputPorts[1];
            // check if the array is one smaller then original
            int x = m_ocsound.getAudioOutputs().length - (m_base_audioOutputPorts.length - 1);
            notifyTestComplete(x, msg);
        }
    }

    private void testAddAudioOutput()
    {
        String msg = "Add Audio Output Port";
        if (m_removed_audioOutputPorts == null)
        {
            print("Must do a remove first.");
        }
        else
        {
            m_ocsound.addAudioOutput(m_removed_audioOutputPorts);
            m_removed_audioOutputPorts = null;
            // check if the array is same size as original
            int x = m_ocsound.getAudioOutputs().length - m_base_audioOutputPorts.length;
            notifyTestComplete(x, msg);
        }
    }

    private void testChangeGainLevel()
    {
        String msg = "Change Gain Level";
        float returned_level = m_ocsound.setLevel(m_gain_level);

        // check new level is correct
        notifyTestComplete(m_ocsound.getLevel() == m_gain_level ? 0 : 1, msg);

        m_gain_level += 0.1f;
        if (m_gain_level > 1.0f) m_gain_level = 0.0f;
    }

    private void testMute(boolean m)
    {
        String msg = "Change Mute";
        m_ocsound.setMuted(m);
        // check if muted is correct
        notifyTestComplete(m_ocsound.isMuted() == m ? 0 : 1, msg);
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

    // 
    // display info on xlet usage
    // 
    private void printTestList()
    {
        print("");
        print(SECTION_DIVIDER);
        print("Press <1> to Add Audio Output");
        print("Press <2> to Remove Audio Output");
        print("Press <3> to Set Muted");
        print("Press <4> to UnSet Muted");
        print("Press <5> to Change Gain Level (0.1)");
        print("Press <INFO> display OCSound Info");
    }

    private void printAllOCSound()
    {
        print("Gain Level = " + m_ocsound.getLevel());
        print("Muted is " + m_ocsound.isMuted());

        AudioOutputPort[] aop = m_ocsound.getAudioOutputs();
        print("Number of Audio Output Ports = " + aop.length);
        for (int x = 0; x < aop.length; x++)
        {
            print("*** Port " + x + " ***");
            print("  Stereo Mode = " + lookupStereoMode(aop[x].getStereoMode()));
            print("  Compression = " + lookupCompression(aop[x].getCompression()));
            print("  Encoding = " + lookupEncoding(aop[x].getEncoding()));
            print("  dB Gain = " + aop[x].getDB());
            print("  Max dB = " + aop[x].getMaxDB());
            print("  Min dB = " + aop[x].getMinDB());
            print("  Level = " + aop[x].getLevel());
            print("  Optimal Level = " + aop[x].getOptimalLevel());
            print("  Muted = " + aop[x].isMuted());
            print("  Loop Thru = " + aop[x].isLoopThru());
            printVideoOutputInformation(aop[x].getConnectedVideoOutputPorts());
            print(SECTION_DIVIDER);
        }
    }

    private void printVideoOutputInformation(java.util.Enumeration voPorts)
    {
        print("  VideoOutputPort Information");
        while (null != voPorts && voPorts.hasMoreElements())
        {
            VideoOutputPort port = (VideoOutputPort) voPorts.nextElement();
            print("    " + port);
            print("      Status          = " + (port.status() ? "enabled" : "disabled"));
            print("      Port Type       = " + lookupPortType(port.getType()));
            if (port.getType() == VideoOutputPort.AV_OUTPUT_PORT_TYPE_1394)
            {
                print1394Information(port);
            }
            print("      DTCP            = " + port.queryCapability(VideoOutputPort.CAPABILITY_TYPE_DTCP));
            print("      HDCP            = " + port.queryCapability(VideoOutputPort.CAPABILITY_TYPE_HDCP));
            print("      Rez Restriction = "
                    + port.queryCapability(VideoOutputPort.CAPABILITY_TYPE_RESOLUTION_RESTRICTION));
        }
    }

    private String lookupPortType(int portType)
    {
        switch (portType)
        {
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_1394:
                return "AV_OUTPUT_PORT_TYPE_1394";
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_BB:
                return "AV_OUTPUT_PORT_TYPE_BB";
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO:
                return "AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO";
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_DVI:
                return "AV_OUTPUT_PORT_TYPE_DVI";
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_RF:
                return "AV_OUTPUT_PORT_TYPE_RF";
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_SVIDEO:
                return "AV_OUTPUT_PORT_TYPE_SVIDEO";
            default:
                return "<unknown>";
        }
    }

    private void print1394Information(VideoOutputPort port)
    {
        print("      IEEE 1394 Node Information");
        IEEE1394Node[] nodes = port.getIEEE1394Node();
        for (int i = 0; i < nodes.length; i++)
        {
            print("        Model Name    = " + nodes[i].getModelName());
            print("        Vendor Name   = " + nodes[i].getVendorName());
            short[] types = nodes[i].getSubunitType();
            String typesString = "";
            for (int j = 0; j < types.length; j++)
                typesString += types[j] + ",";
            print("        Subunit Types = " + typesString);
        }
    }

    private String lookupEncoding(int encode)
    {
        switch (encode)
        {
            case AudioOutputPort.ENCODING_AC3:
                return "AC3";
            case AudioOutputPort.ENCODING_DISPLAY:
                return "Display";
            case AudioOutputPort.ENCODING_NONE:
                return "None";
            case AudioOutputPort.ENCODING_PCM:
                return "PCM";
            default:
                return "<unknown>";
        }
    }

    private String lookupCompression(int compression)
    {
        switch (compression)
        {
            case AudioOutputPort.COMPRESSION_HEAVY:
                return "Heavy";
            case AudioOutputPort.COMPRESSION_LIGHT:
                return "Light";
            case AudioOutputPort.COMPRESSION_MEDIUM:
                return "Medium";
            case AudioOutputPort.COMPRESSION_NONE:
                return "None";
            default:
                return "<unknown>";
        }
    }

    private String lookupStereoMode(int stereoMode)
    {
        switch (stereoMode)
        {
            case AudioOutputPort.STEREO_MODE_MONO:
                return "Mono";
            case AudioOutputPort.STEREO_MODE_STEREO:
                return "Stereo";
            case AudioOutputPort.STEREO_MODE_SURROUND:
                return "Surround";
            default:
                return "<unknown>";
        }
    }

    /*
     * !!!!For AutoXlet automation framework!!!!! Method used to send completion
     * of events
     */
    public void notifyTestComplete(int result, String reason)
    {
        String testResult = "PASSED";
        if (result != 0)
        {
            testResult = "FAILED: " + reason;
        }
        m_log.log("Test completed; result=" + testResult);
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
}
