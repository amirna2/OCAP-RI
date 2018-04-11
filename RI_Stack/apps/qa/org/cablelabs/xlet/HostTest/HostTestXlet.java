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

package org.cablelabs.xlet.HostTest;

import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.ocap.hardware.Host;
import org.ocap.hardware.IEEE1394Node;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.ui.event.OCRcEvent;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Monitor;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.TestFailure;
import org.cablelabs.test.autoxlet.TestResult;
import org.cablelabs.test.autoxlet.XletLogger;

public class HostTestXlet extends Container implements Xlet, KeyListener, Driveable
{
    // The OCAP Xlet context.
    XletContext m_ctx;

    // A HAVi Scene.
    private HScene m_scene;

    private static VidTextBox m_vbox;

    // Host info
    private Host hostRef = null;

    // Test runner
    private String m_appName = "HostTestXlet";

    private int m_testNumber = 0;

    private Vector m_testList = new Vector();

    private static String SECTION_DIVIDER = "==================================";

    // autoXlet
    private AutoXletClient m_axc = null;

    private static Logger m_log = null;

    private static Test m_test = null;

    private static Monitor m_eventMonitor = null;

    /**
     * Initializes the OCAP Xlet.
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("[" + m_appName + "] : initXlet() - begin");

        // initialize AutoXlet
        m_axc = new AutoXletClient(this, ctx);
        m_test = m_axc.getTest();
        if (m_axc.isConnected())
        {
            m_log = m_axc.getLogger();
        }
        else
        {
            m_log = new XletLogger();
        }
        m_eventMonitor = new Monitor(); // used by event dispatcher

        // store off our xlet context
        m_ctx = ctx;

        // Setup the application graphical user interface.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_vbox = new VidTextBox(40, 50, 530, 370, 14, 5000);
        m_scene.add(m_vbox);
        m_scene.addKeyListener(this);
        m_scene.addKeyListener(m_vbox);

        debugLog(" initXlet() - end");
    }

    /**
     * Starts the OCAP Xlet.
     */
    public void startXlet() throws XletStateChangeException
    {
        debugLog(" startXlet() - begin");

        // get an instance of the Host
        hostRef = Host.getInstance();
        if (hostRef == null)
        {
            print(" COULD NOT ACCESS HOST INSTANCE!");
        }

        // run initial tests
        printTestList();
        printInfo();

        // Display the application.
        m_scene.show();
        m_scene.requestFocus();

        debugLog(" startXlet() - end");
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        m_scene.setVisible(false);
    }

    /**
     * Destroys the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             destroyed.
     */
    public void destroyXlet(boolean forced) throws XletStateChangeException
    {
        m_scene.setVisible(false);

        // Clean up and dispose of resources.
        HScene tmp = m_scene;
        m_scene = null;
        hostRef = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    private String lookupPowerMode(int powerMode)
    {
        switch (powerMode)
        {
            case Host.FULL_POWER:
                return "FULL_POWER";
            case Host.LOW_POWER:
                return "LOW_POWER";
            default:
                return "<unknown>";
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
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_HDMI:
                return "AV_OUTPUT_PORT_TYPE_HDMI";
            case VideoOutputPort.AV_OUTPUT_PORT_TYPE_INTERNAL:
                return "AV_OUTPUT_PORT_TYPE_INTERNAL";
            default:
                return "<unknown>";
        }
    }

    private void printInfo()
    {
        print(SECTION_DIVIDER);

        print("Host information:");

        if (hostRef == null)
        {
            print(" COULD NOT ACCESS HOST INSTANCE!");
            return;
        }

        print("  ID             = '" + hostRef.getID() + "'");
        print("  PowerMode      = " + hostRef.getPowerMode() + " (" + lookupPowerMode(hostRef.getPowerMode()) + ")");
        print("  ReverseChanMac = " + hostRef.getReverseChannelMAC());
        print("  ACOutlet       = "
                + (hostRef.isACOutletPresent() ? (hostRef.getACOutlet() ? "<present-ON>" : "<present-OFF>")
                        : "<not present>"));
        print("  RFBypass       = "
                + (hostRef.getRFBypassCapability() ? (hostRef.getRFBypass() ? "<controllable-ENABLED>"
                        : "<controllable-DISABLED>") : "<not controllable>"));
        printVideoOutputInformation();
        print(SECTION_DIVIDER);
        return;
    }

    private void setACOutlet(boolean enable)
    {
        if ((hostRef != null) && (hostRef.isACOutletPresent())) hostRef.setACOutlet(enable);

    }

    private void setRFBypass(boolean enable)
    {
        if ((hostRef != null) && (hostRef.getRFBypassCapability())) hostRef.setRFBypass(enable);

    }

    // 
    // Key Handling methods
    // 
    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();

        switch (key)
        {
            case OCRcEvent.VK_0:
                printInfo();
                break;
            case OCRcEvent.VK_1:
                print("Turn AC-Outlet ON");
                setACOutlet(true);
                printInfo();
                break;
            case OCRcEvent.VK_2:
                print("Turn AC-Outlet OFF");
                setACOutlet(false);
                printInfo();
                break;
            case OCRcEvent.VK_4:
                print("Enable RF-Bypass");
                setRFBypass(true);
                printInfo();
                break;
            case OCRcEvent.VK_5:
                print("Disable RF-Bypass");
                setRFBypass(false);
                printInfo();
                break;
            case OCRcEvent.VK_9:
                runTests();
                break;
            case OCRcEvent.VK_EXIT:
                print("Exiting");
                // TODO: exit app
                break;
            case OCRcEvent.VK_INFO:
                printTestList();
                break;

            default:
                // don't worry about the rest of the keys
                break;
        }
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyTyped(KeyEvent e)
    {
    }

    //
    // For autoxlet automation framework (Driveable interface)
    // 
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

    private void runTests()
    {
        print("Running tests...");
        // Test AC Outlet support
        if (hostRef.isACOutletPresent())
        {
            print("AC Outlet present. Testing...");
            boolean oldState = hostRef.getACOutlet();
            hostRef.setACOutlet(true);
            m_test.assertTrue("AC Outlet should be enabled", hostRef.getACOutlet());
            hostRef.setACOutlet(false);
            m_test.assertFalse("AC Outlet should be disabled", hostRef.getACOutlet());
            hostRef.setACOutlet(oldState);
            print("AC Outlet testing complete!");
        }
        else
            print("AC Outlet not present. Tests skipped.");
        // Test RF bypass support
        if (hostRef.getRFBypassCapability())
        {
            boolean oldState = hostRef.getRFBypass();
            hostRef.setRFBypass(true);
            m_test.assertTrue("RF bypass should be enabled", hostRef.getRFBypass());
            hostRef.setRFBypass(false);
            m_test.assertFalse("RF bypass should be disabled", hostRef.getRFBypass());
            hostRef.setRFBypass(oldState);
        }
        else
            print("RF bypass not available. Tests skipped.");

        print("Testing complete!");

        TestResult result = m_test.getTestResult();
        if (result.wasSuccessful())
            print("All " + result.runCount() + " tests passed");
        else
        {
            Enumeration failures = result.failures();
            while (failures.hasMoreElements())
            {
                TestFailure failure = (TestFailure) failures.nextElement();
                print(failure.toString());
                print(failure.trace());
            }
        }

        m_eventMonitor.notifyReady();
    }

    private void printVideoOutputInformation()
    {
        print("  VideoOutputPort Information");
        Enumeration voPorts = hostRef.getVideoOutputPorts();
        while (voPorts.hasMoreElements())
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

    // 
    // display info on xlet usage
    // 
    private void printTestList()
    {
        print("");
        print(SECTION_DIVIDER);

        print("Press <0> for current Host information");
        print("Press <1> to enable AC-Outlet (if Host supports it)");
        print("Press <2> to disable AC-Outlet (if Host supports it)");
        print("Press <4> to enable RF-Bypass (if Host supports it)");
        print("Press <5> to disable RF-Bypass (if Host supports it)");
        print("Press <9> to run tests");
        print("Press <EXIT> to exit this application");

        // print(SECTION_DIVIDER);
        // for (int i = 0; i < m_testList.size(); i++)
        // {
        // print("Test " +i +" : " +(String)m_testList.elementAt(i));
        // }

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
        m_log.log("\t" + msg);
        m_vbox.write("    " + msg);
    }

}
