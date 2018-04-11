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
import org.ocap.ui.event.OCRcEvent;
import org.ocap.hardware.Host;
import org.ocap.hardware.IEEE1394Node;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.hardware.device.AudioOutputPort;
import org.ocap.hardware.device.FeatureNotSupportedException;
import org.ocap.hardware.device.HostSettings;
import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;

public class DsAudioOutputPortTest extends Container implements Driveable, KeyListener, Xlet
{
    // The OCAP Xlet context.
    private XletContext m_ctx;

    // A HAVi Scene.
    private HScene m_scene;

    private static VidTextBox m_vbox;

    // Test runner
    private String m_appName = "DsAudioOutputPortTest";

    private final static String SECTION_DIVIDER = "==================================";

    // autoXlet
    private AutoXletClient m_axc = null;

    private static Logger m_log = null;

    private static Test m_test = null;

    // Host info
    private HostSettings hostRef = null;

    // Audio Output Port stuff
    private int compressionNdx = 0;

    private final int compressionInt[] = { AudioOutputPort.COMPRESSION_NONE, AudioOutputPort.COMPRESSION_LIGHT,
            AudioOutputPort.COMPRESSION_MEDIUM, AudioOutputPort.COMPRESSION_HEAVY };

    private int encodingNdx = 0;

    private final int encodingInt[] = { AudioOutputPort.ENCODING_NONE, AudioOutputPort.ENCODING_DISPLAY,
            AudioOutputPort.ENCODING_PCM, AudioOutputPort.ENCODING_AC3 };

    private int dbNdx = 0;

    private final String dbStr[] = { "Minimum", "0.0", "Maximum" };

    private int stereoNdx = 0;

    private final int stereoInt[] = { AudioOutputPort.STEREO_MODE_MONO, AudioOutputPort.STEREO_MODE_STEREO,
            AudioOutputPort.STEREO_MODE_SURROUND };

    private float level = 0.0f;

    private boolean loopThru = false;

    private boolean muted = false;

    // Let the methods begin
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
        // get instance of HostSettings
        hostRef = (HostSettings) Host.getInstance();

        debugLog(" initXlet() - end");
    }

    public void pauseXlet()
    {
        debugLog(" pauseXlet");
        m_scene.setVisible(false);
    }

    public void startXlet() throws XletStateChangeException
    {
        debugLog(" startXlet() - begin");

        // Display the application.
        m_scene.show();
        m_scene.requestFocus();
        // Describe tests
        printAllInfo();

        debugLog(" startXlet() - end");

    }

    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
        keyPressed(event);
    }

    public void keyReleased(KeyEvent arg0)
    {
    }

    public void keyTyped(KeyEvent arg0)
    {
    }

    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();

        switch (key)
        {
            case OCRcEvent.VK_1:
                print("Display All Audio Output Ports");
                testDisplayAllAudioOutputPorts();
                break;
            case OCRcEvent.VK_2:
                print("Set Compression " + lookupCompression(compressionInt[compressionNdx]));
                testSetCompression();
                break;
            case OCRcEvent.VK_3:
                print("Set DB " + dbStr[dbNdx]);
                testSetDB();
                break;
            case OCRcEvent.VK_4:
                print("Set Encoding " + lookupEncoding(encodingInt[encodingNdx]));
                testSetEncoding();
                break;
            case OCRcEvent.VK_5:
                print("Set Level " + level);
                testSetLevel();
            case OCRcEvent.VK_6:
                print("Set Loop Thru " + loopThru);
                testSetLoopThru();
                break;
            case OCRcEvent.VK_7:
                print("Set Muted " + muted);
                testSetMuted();
                break;
            case OCRcEvent.VK_8:
                print("Set Stereo Mode " + lookupStereoMode(stereoInt[stereoNdx]));
                testSetStereoMode();
                break;

            case OCRcEvent.VK_INFO:
                printAllInfo();
                break;
            case OCRcEvent.VK_EXIT:
                print("Not Exiting");
                break;
            default:
                break;
        }
    }

    // 
    // display info on xlet usage
    // 
    private void printAllInfo()
    {
        print("");
        print(SECTION_DIVIDER);
        print("Press <1> to Display All Audio Output Ports");
        print("Press <2> to Set Compression " + lookupCompression(compressionInt[compressionNdx]));
        print("Press <3> to Set DB " + dbStr[dbNdx]);
        print("Press <4> to Set Encoding " + lookupEncoding(encodingInt[encodingNdx]));
        print("Press <5> to Set Level " + level);
        print("Press <6> to Set loopThru " + loopThru);
        print("Press <7> to Set Muted " + muted);
        print("Press <8> to Set Stereo Mode " + lookupStereoMode(stereoInt[stereoNdx]));
        print("Press <INFO> display Info");
    }

    private void testSetStereoMode()
    {
        // get enumeration of audio output ports
        Enumeration enm = hostRef.getAudioOutputs();
        int x = 0;
        while (enm.hasMoreElements())
        {
            try
            {
                AudioOutputPort aop = (AudioOutputPort) enm.nextElement();
                ++x;
                print("*** Port " + x + " ***");
                aop.setStereoMode(stereoInt[stereoNdx]);
                m_test.assertTrue("setStereoMode() failed " + aop.getStereoMode() + " vs " + stereoInt[stereoNdx],
                        aop.getStereoMode() == stereoInt[stereoNdx]);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" testSetStereoMode - caught IllegalArgumentException " + iae);
                notifyTestComplete(1, "Illegal Argument Exception");
            }
            catch (FeatureNotSupportedException fnse)
            {
                debugLog(" testSetStereoMode - caught FeatureNotSupportedException exception: " + fnse);
                notifyTestComplete(1, "FeatureNotSupportedException");
            }
        }// while
        stereoNdx = ++stereoNdx == stereoInt.length ? 0 : stereoNdx;
        // must have 1 or more audio output ports
        notifyTestComplete(x > 0 ? 0 : 1, "Audio Output Port count is zero");
    }

    private void testSetMuted()
    {
        Enumeration enm = hostRef.getAudioOutputs();
        int x = 0;
        while (enm.hasMoreElements())
        {
            // get enumeration of audio output ports
            AudioOutputPort aop = (AudioOutputPort) enm.nextElement();
            ++x;
            print("*** Port " + x + " ***");
            aop.setMuted(muted);
            m_test.assertTrue("setMuted() failed", aop.isMuted() == muted);
        }// while
        muted = !muted;
        // must have 1 or more audio output ports
        notifyTestComplete(x > 0 ? 0 : 1, "Audio Output Port count is zero");
    }

    private void testSetLoopThru()
    {
        // get enumeration of audio output ports
        Enumeration enm = hostRef.getAudioOutputs();
        int x = 0;
        while (enm.hasMoreElements())
        {
            try
            {
                AudioOutputPort aop = (AudioOutputPort) enm.nextElement();
                ++x;
                print("*** Port " + x + " ***");
                aop.setLoopThru(loopThru);
                m_test.assertTrue("setLoopThru() failed", aop.isLoopThru() == loopThru);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" testSetLoopThru - caught IllegalArgumentException " + iae);
                notifyTestComplete(1, "Illegal Argument Exception");
            }
            catch (FeatureNotSupportedException fnse)
            {
                debugLog(" testSetLoopThru - caught FeatureNotSupportedException exception: " + fnse);
                notifyTestComplete(1, "FeatureNotSupportedException");
            }
        }// while
        loopThru = !loopThru;
        // must have 1 or more audio output ports
        notifyTestComplete(x > 0 ? 0 : 1, "Audio Output Port count is zero");
    }

    private void testSetLevel()
    {
        Enumeration enm = hostRef.getAudioOutputs();
        int x = 0;
        while (enm.hasMoreElements())
        {
            // get enumeration of audio output ports
            AudioOutputPort aop = (AudioOutputPort) enm.nextElement();
            ++x;
            print("*** Port " + x + " ***");
            aop.setLevel(level);
            m_test.assertTrue("setLevel() failed " + aop.getLevel() + " vs " + level, aop.getLevel() == level);
        }// while
        // increase level upto 1.0 by 0.1 amount
        level = (level + 0.1f) > 1.0 ? 0.0f : (level + 0.1f);
        // must have 1 or more audio output ports
        notifyTestComplete(x > 0 ? 0 : 1, "Audio Output Port count is zero");
    }

    private void testSetEncoding()
    {
        // get enumeration of audio output ports
        Enumeration enm = hostRef.getAudioOutputs();
        int x = 0;
        while (enm.hasMoreElements())
        {
            try
            {
                AudioOutputPort aop = (AudioOutputPort) enm.nextElement();
                ++x;
                print("*** Port " + x + " ***");
                aop.setEncoding(encodingInt[encodingNdx]);
                m_test.assertTrue("setEncoding() failed " + aop.getEncoding() + " vs " + encodingInt[encodingNdx],
                        aop.getEncoding() == encodingInt[encodingNdx]);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" testSetEncoding - caught IllegalArgumentException " + iae);
                notifyTestComplete(1, "Illegal Argument Exception");
            }
            catch (FeatureNotSupportedException fnse)
            {
                debugLog(" testSetEncoding - caught FeatureNotSupportedException exception: " + fnse);
                notifyTestComplete(1, "FeatureNotSupportedException");
            }
        }// while

        encodingNdx = ++encodingNdx == encodingInt.length ? 0 : encodingNdx;

        // must have 1 or more audio output ports
        notifyTestComplete(x > 0 ? 0 : 1, "Audio Output Port count is zero");
    }

    private void testSetDB()
    {
        float dodb;
        Enumeration enm = hostRef.getAudioOutputs();
        int x = 0;
        while (enm.hasMoreElements())
        {
            // get enumeration of audio output ports
            AudioOutputPort aop = (AudioOutputPort) enm.nextElement();
            if (dbNdx == 0)
                dodb = aop.getMinDB();
            else if (dbNdx == 2)
                dodb = aop.getMaxDB();
            else
                dodb = 0.0f;
            ++x;
            print("*** Port " + x + " ***");
            aop.setDB(dodb);
            m_test.assertTrue("setDB() failed " + aop.getDB() + " vs " + dodb, aop.getDB() == dodb);
        }// while
        dbNdx = ++dbNdx == dbStr.length ? 0 : dbNdx;
        // must have 1 or more audio output ports
        notifyTestComplete(x > 0 ? 0 : 1, "Audio Output Port count is zero");
    }

    private void testSetCompression()
    {
        // get enumeration of audio output ports
        Enumeration enm = hostRef.getAudioOutputs();
        int x = 0;
        while (enm.hasMoreElements())
        {
            try
            {
                AudioOutputPort aop = (AudioOutputPort) enm.nextElement();
                ++x;
                print("*** Port " + x + " ***");
                aop.setCompression(compressionInt[compressionNdx]);
                m_test.assertTrue("setCompression() failed " + aop.getCompression() + " vs "
                        + compressionInt[compressionNdx], aop.getCompression() == compressionInt[compressionNdx]);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" testSetCompression() - caught IllegalArgumentException " + iae);
                notifyTestComplete(1, "Illegal Argument Exception");
            }
            catch (FeatureNotSupportedException fnse)
            {
                debugLog(" testSetCompression() - caught FeatureNotSupportedException exception: " + fnse);
                notifyTestComplete(1, "FeatureNotSupportedException");
            }
        }// while

        compressionNdx = ++compressionNdx == compressionInt.length ? 0 : compressionNdx;

        // must have 1 or more audio output ports
        notifyTestComplete(x > 0 ? 0 : 1, "Audio Output Port count is zero");
    }

    private void testDisplayAllAudioOutputPorts()
    {
        Enumeration enm = hostRef.getAudioOutputs();
        int x = 0;
        while (enm.hasMoreElements())
        {
            try
            {
                // get enumeration of audio output ports
                AudioOutputPort aop = (AudioOutputPort) enm.nextElement();
                ++x;
                print("*** Port " + x + " ***");
                // print("  Stereo Mode = " +
                // lookupStereoMode(aop.getStereoMode()));
                // print("  Compression = " +
                // lookupCompression(aop.getCompression()));
                // print("  Encoding = " + lookupEncoding(aop.getEncoding()));
                // print("  dB Gain = " + aop.getDB());
                // print("  Max dB = " + aop.getMaxDB());
                // print("  Min dB = " + aop.getMinDB());
                // print("  Level = " + aop.getLevel());
                // print("  Optimal Level = " + aop.getOptimalLevel());
                // print("  Muted = " + aop.isMuted());
                // print("  Loop Thru = " + aop.isLoopThru());
                printVideoOutputInformation(aop.getConnectedVideoOutputPorts());
                print(SECTION_DIVIDER);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" testDisplayAllAudioOutputPorts() - caught IllegalArgumentException " + iae);
                notifyTestComplete(1, "Illegal Argument Exception");
            }
            catch (SecurityException se)
            {
                debugLog(" testDisplayAllAudioOutputPorts() - caught SecurityException exception: " + se);
                notifyTestComplete(1, "Security Exception");
            }
        }// while
        // must have 1 or more audio output ports
        notifyTestComplete(x > 0 ? 0 : 1, "Audio Output Port count is zero");
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
