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
import java.util.Hashtable;
import java.util.Enumeration;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Dimension;
import java.rmi.RemoteException;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HScreen;
import org.ocap.hardware.Host;
import org.ocap.hardware.device.HostSettings;
import org.ocap.hardware.device.VideoOutputSettings;
import org.ocap.hardware.device.VideoOutputConfiguration;
import org.ocap.hardware.device.VideoOutputPortListener;
import org.ocap.hardware.device.DynamicVideoOutputConfiguration;
import org.ocap.hardware.device.FixedVideoOutputConfiguration;
import org.ocap.hardware.device.AudioOutputPort;
import org.ocap.hardware.device.VideoResolution;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.ui.event.OCRcEvent;
import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;
import org.dvb.media.VideoFormatControl;

public class DsVideoOutputSettingsTest extends Container implements Driveable, Xlet, KeyListener
{
    // The OCAP Xlet context.
    private XletContext m_ctx;

    // A HAVi Scene.
    private HScene m_scene;

    private static VidTextBox m_vbox;

    // Test runner
    private String m_appName = "DsVideoOutputSettingsTest";

    private static String SECTION_DIVIDER = "==================================";

    // autoXlet
    private AutoXletClient m_axc = null;

    private static Logger m_log = null;

    private static Test m_test = null;

    // Host info
    private HostSettings hostRef = null;

    private HScreen hscreenDef = null;

    // Video Output Settings stuff
    private final static String pcStr = "Product Code";

    private final static String snStr = "Serial Number";

    private final static String mnStr = "Manufacturer Name";

    private final static String mwStr = "Manufacture Week";

    private final static String myStr = "Manufacture Year";

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
            debugLog("No default hscreen assigned");
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

    // 
    // display info on xlet usage
    // 
    private void printTestList()
    {
        print("");
        print(SECTION_DIVIDER);
        print("Press <1> to Display Main Video Output Port");
        print("Press <2> to Display All Video Output Configurations");
        print("Press <3> to Change Main Video Configuration with Listener");
        print("Press <INFO> Display Test Options");
    }

    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();
        switch (key)
        {
            case OCRcEvent.VK_1:
                print("Display Main Video Output Port");
                testDisplayMainVideoOutputPort();
                break;
            case OCRcEvent.VK_2:
                print("Display All Video Output Configurations");
                testDisplayAllVideoOutputConfig();
                break;
            case OCRcEvent.VK_3:
                print("Change Main Video Configuration with Listener");
                testChangeConfig();
                break;
            case OCRcEvent.VK_INFO:
                printTestList();
                break;
            default:
                break;
        }
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

    boolean didChange; // must be global for VideoOutputPortListener to see.

    private void testChangeConfig()
    {
        didChange = false;
        VideoOutputPort vop = hostRef.getMainVideoOutputPort(hscreenDef);
        if (vop != null && vop instanceof VideoOutputSettings)
        {
            VideoOutputSettings vops = (VideoOutputSettings) vop;
            VideoOutputConfiguration current = vops.getOutputConfiguration();
            VideoOutputPortListener listener;
            vops.addListener(listener = new VideoOutputPortListener()
            {
                public void configurationChanged(VideoOutputPort source, VideoOutputConfiguration oldConfig,
                        VideoOutputConfiguration newConfig)
                {
                    didChange = true; // change exception occurred
                    print("Video Output Port Listener called for Configuration Changed");
                }

                public void connectionStatusChanged(VideoOutputPort source, boolean status)
                {
                    print("Video Output Port Listener called for Connection Status Changed");
                }

                public void enabledStatusChanged(VideoOutputPort source, boolean status)
                {
                    print("Video Output Port Listener called for Enabled Status Changed");
                }
            });

            // try to change configuration
            try
            {
                vops.setOutputConfiguration(current); // set video to current

                try
                {
                    Thread.sleep(300);
                }
                catch (InterruptedException ie)
                {
                }
            }
            catch (java.lang.SecurityException se)
            {
                print("Security Exception caught " + se); // need
                                                          // MonitorAppPermission("deviceController")
            }
            catch (org.ocap.hardware.device.FeatureNotSupportedException fnse)
            {
                print("Feature Not Supported Exception caught " + fnse);
            }
            catch (java.lang.IllegalArgumentException iae)
            {
                print("Illegal Argument Exception caught " + iae);
            }
            // remove listener
            vops.removeListener(listener);

            notifyTestComplete(didChange ? 0 : 1, "Did not receive configurationChanged exception");
        }
    }

    private void testDisplayAllVideoOutputConfig()
    {
        VideoOutputPort vop = hostRef.getMainVideoOutputPort(hscreenDef);
        if (vop != null && vop instanceof VideoOutputSettings)
        {
            VideoOutputSettings vops = (VideoOutputSettings) vop;
            VideoOutputConfiguration[] voc = vops.getSupportedConfigurations();
            if (voc != null)
            {
                for (int x = 0; x < voc.length; x++)
                {
                    String name = voc[x].getName();
                    print("  Name of Video Output Configuration " + x + " is " + name);
                    if (voc[x] instanceof DynamicVideoOutputConfiguration)
                    {
                        print("  Dynamic Video Output Configuration for VOC " + x);
                        DynamicVideoOutputConfiguration dvoc = (DynamicVideoOutputConfiguration) voc[x];
                        Enumeration enm = dvoc.getInputResolutions();
                        int vrCount = 0;
                        while (enm.hasMoreElements())
                        {
                            VideoResolution vr = (VideoResolution) enm.nextElement();
                            ++vrCount;
                            print("  Video Input Resolution " + vrCount);
                            print("     Aspect Ratio " + vr.getAspectRatio());
                            print("     Frame Rate " + vr.getRate());
                            print("     Scan Mode " + lookUpScanMode(vr.getScanMode()));
                            Dimension pr = vr.getPixelResolution();
                            print("     Pixel Resolution height/width " + pr.height + "/" + pr.width);
                        }
                    }

                    if (voc[x] instanceof FixedVideoOutputConfiguration)
                    {
                        print("  Fixed Video Output Configuration for VOC " + x);
                        FixedVideoOutputConfiguration fvoc = (FixedVideoOutputConfiguration) voc[x];
                        VideoResolution vr = fvoc.getVideoResolution();
                        print("     Aspect Ratio " + vr.getAspectRatio());
                        print("     Frame Rate " + vr.getRate());
                        print("     Scan Mode " + lookUpScanMode(vr.getScanMode()));
                        Dimension pr = vr.getPixelResolution();
                        print("     Pixel Resolution height/width " + pr.height + "/" + pr.width);
                    }
                }// for
            }
            else
                m_test.assertTrue("  No Video Output Configurations", false);

            VideoOutputConfiguration current = vops.getOutputConfiguration();
            if (current != null)
                print("  Name of Current Video Output Configuration is " + current.getName());
            else
                m_test.assertTrue(" No current video output configuration", false);

            notifyTestComplete(voc != null ? 0 : 1, "No Video Output Configurations returned");
        }
    }

    private void testDisplayMainVideoOutputPort()
    {
        VideoOutputPort vop = hostRef.getMainVideoOutputPort(hscreenDef);
        if (vop != null && vop instanceof VideoOutputSettings)
        {
            VideoOutputSettings vops = (VideoOutputSettings) vop;
            print("Main Video Output Port type is " + lookUpVopType(vop.getType()));
            print("  Display Connected is " + vops.isDisplayConnected());
            print("  Display Aspect Ratio is " + lookUpAspectRatio(vops.getDisplayAspectRatio()));
            print("  Content Protected is " + vops.isContentProtected());
            print("  Dynamic Configuration Support is " + vops.isDynamicConfigurationSupported());

            AudioOutputPort aop = vops.getAudioOutputPort();
            m_test.assertNotNull("  Main Video Output Port's Audio Port is null", aop);

            Hashtable hash = vops.getDisplayAttributes();
            if (hash != null)
            {
                String mname = (String) hash.get(mnStr); // manuf name
                Short pc = (Short) hash.get(pcStr); // product code
                Integer sn = (Integer) hash.get(snStr); // serial number
                Byte mw = (Byte) hash.get(mwStr); // manuf week
                Byte my = (Byte) hash.get(myStr); // manuf year from 1990
                if (mname != null) print("  Manufacture Name " + mname);
                if (pc != null) print("  Product code " + pc);
                if (sn != null) print("  Serial Number " + sn);
                if (mw != null) print("  Manufacture Week " + mw);
                if (my != null) print("  Manufacture Year " + (1990 + my.intValue()));
            }
            else
                print("  getDisplayAttributes() returned no data.");
            notifyTestComplete(0, "OK");
        }
        else
            notifyTestComplete(1, "No Main Video Output Port returned");
    }

    private String lookUpAspectRatio(int ratio)
    {
        switch (ratio)
        {
            case VideoFormatControl.ASPECT_RATIO_16_9:
                return "16x9";

            case VideoFormatControl.ASPECT_RATIO_2_21_1:
                return "2x21x1";

            case VideoFormatControl.ASPECT_RATIO_4_3:
                return "4x3";

            default:
                return "<unknown>";
        }
    }

    private String lookUpVopType(int type)
    {
        switch (type)
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

    private String lookUpScanMode(int sm)
    {
        switch (sm)
        {
            case VideoResolution.SCANMODE_INTERLACED:
                return "INTERLACED";

            case VideoResolution.SCANMODE_PROGRESSIVE:
                return "PROGRESSIVE";

            case VideoResolution.SCANMODE_UNKNOWN:
                return "UNKNOWN";

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
}
