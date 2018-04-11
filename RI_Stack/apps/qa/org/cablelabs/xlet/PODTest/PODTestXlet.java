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

package org.cablelabs.xlet.PODTest;

import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.Calendar;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HScreen;

import org.ocap.hardware.pod.POD;
import org.ocap.hardware.pod.HostParamHandler;
import org.ocap.hardware.pod.PODApplication;
import org.ocap.system.SystemModuleHandler;
import org.ocap.system.SystemModuleRegistrar;
import org.ocap.system.SystemModule;
import org.ocap.ui.event.OCRcEvent;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;


/*
 * This test exercises the public APIs for org.ocap.hardware.pod.POD
 * 
 * Warning, this xlet must have
 * MonitorAppPermission("podApplication")
 */
public class PODTestXlet extends Container implements Xlet, Driveable, KeyListener, SystemModuleHandler,
        HostParamHandler
{

    /**
     * Added to silence the compiler
     */
    private static final long serialVersionUID = -3599127111275002970L;

    /*
     * A HAVi Scene and widget to display test results
     */
    private HScene m_scene;

    private HScreen hscreenDef = null;

    private static VidTextBox m_vbox;

    private static String SECTION_DIVIDER = "==================================";

    /*
     * Test runner name for logging
     */
    private String m_appName = "PODTest";

    /*
     * autoXlet variables
     */
    private AutoXletClient m_axc = null;

    private static Logger m_log = null;

    private static Test m_test = null;

    SystemModule m_module = null;

    private byte m_echoTxn = -1;

    private final int m_dataRqst = 0x9F9A02; // SAS_data_rqst

    private final int m_dataAv = 0x9F9A03; // SAS_data_av

    private final int m_dataCnf = 0x9F9A04; // SAS_data_cnf

    private final int m_serverQuery = 0x9F9A05; // SAS_server_query

    private final int m_serverReply = 0x9F9A06; // SAS_server_reply

    // App ID for Private Host Application that is required by,
    // and hard coded into, the HPNX-Pro synchronous test (SAS) application,
    // aka "01 DECAF 4 A DEAF CAB."
    // Note, this is not an OCAP AppID either in class or content.
    private final byte[] m_hostAppID = { (byte) 0x01, (byte) 0xDE, (byte) 0xCA, (byte) 0xF4, (byte) 0xAD, (byte) 0xEA,
            (byte) 0xFC, (byte) 0xAB };

    // Txn ID in m_dataAvail and m_replyData.
    // Note - What happens when this hard coded value equals the txn id
    // of a different txn created by the application on the HPNX-Pro?
    private final byte m_txn = 0x77;

    // Response to SAS data request.
    private final byte[] m_dataAvail = { 0x00, m_txn };

    // Response to the SAS Server Query.
    // byte[0] = TXN, byte[1,2] = message length, byte[3,length-1] = data
    private final byte[] m_replyData = { m_txn, 0x00, 0x05, (byte) 0x98, 0x76, 0x54, 0x32, 0x10 };

    /*
     * Names for each of the possible features. See CableCARD Interface 2.0
     * Specification OC-SP-CCIF2.0-I20-091211 Table 9.15-2
     */
    String featureID[] = { "Reserved", // 0x00
            "RF Output Channel", // 0x01
            "Parental Control PIN", // 0x02
            "Parental Control Settings", // 0x03
            "Purchase PIN", // 0x04
            "Time Zone", // 0x05
            "Daylight Savings Control", // 0x06
            "AC Outlet", // 0x07
            "Language", // 0x08
            "Rating Region", // 0x09
            "Reset PINS", // 0x0A
            "Cable URL", // 0x0B
            "EAS location code", // 0x0C
            "VCT ID", // 0x0D
            "Turn-on Channel", // 0x0E
            "Terminal Association", // 0x0F
            "Download Group-ID", // 0x10
            "Zip Code" // 0x11
    };

    /*
     * Expected bytes returned for each feature. Derived empirically. Only for
     * supported features.
     */
    int expectedFeatureBytes[][] = { {}, // "Reserved", 0x00
            { 0x03, 0x02 }, // "RF Output Channel" 0x01
            { 0x04, 0x31, 0x32, 0x33, 0x34 }, // "Parental Control PIN" 0x02
            { 0x00, 0x00, 0x00 }, // "Parental Control Settings" 0x03
            {}, // "Purchase PIN" 0x04
            { 0xFE, 0x5C }, // "Time Zone" 0x05
            { 0x02 }, // "Daylight Savings Control" 0x06
            {}, // "AC Outlet" 0x07
            { 0x45, 0x4E, 0x47 }, // "Language" 0x08
            { 0x01 }, // "Rating Region" 0x09
            { 0x00 }, // "Reset PINS" 0x0A
            { 0x00 }, // "Cable URL" 0x0B
            { 0x00, 0x0C, 0x00 }, // "EAS location code" 0x0C
            { 0x00, 0x01 }, // "VCT ID" 0x0D
            { 0x00, 0x00 }, // "Turn-on Channel" 0x0E
    // "Terminal Association", // 0x0F
    // "Download Group-ID", // 0x10
    // "Zip Code" // 0x11
    };

    /**
     * Initializes the OCAP Xlet.
     * 
     * @param ctx
     *            the context for this Xlet A reference to the context is stored
     *            for further need. This is the place where any initialization
     *            should be done, unless it takes a lot of time or resources.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             initialized.
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("[" + m_appName + "] : initXlet() - begin");
        Calendar now = Calendar.getInstance();

        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);
        System.out.println("[" + m_appName + "] time: " + hour + ":" + minute + ":" + second);

        // initialize AutoXlet and create a logger
        m_axc = new AutoXletClient(this, ctx);
        m_test = m_axc.getTest();
        if (m_axc.isConnected())
            m_log = m_axc.getLogger();
        else
            m_log = new XletLogger();

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

        InetAddress ip = null;

        try
        {
            ip = InetAddress.getLocalHost();
        }
        catch (Exception e)
        {
            debugLog("getByName() threw an exception!");
            m_log.log(e);
            return;
        }

        debugLog(" IpAddress: " + ip);
        debugLog(" IpAddress.getHostName: " + ip.getHostName());
        debugLog(" initXlet() - end");

    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        m_scene.setVisible(false);
    }

    /**
     * Starts the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             started.
     */
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

    /**
     * Destroys the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             destroyed.
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        m_scene.setVisible(false);
        // Clean up and dispose of resources.
        HScene tmp = m_scene;
        m_scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    /*
     * Method used to send completion of events !!!!For AutoXlet automation
     * framework!!!!!
     */
    public void notifyTestComplete(int result, String reason)
    {
        m_log.log("Test completed; result=" + (result == 0 ? "Passed" : "Failed"));
        m_test.assertTrue("Test failed:" + reason, result == 0);
    }

    /*
     * logging function - allow messages to post to teraterm and autoxlet logs
     * !!!!For AutoXlet automation framework!!!!!
     */
    private void debugLog(String msg)
    {
        m_log.log("[" + m_appName + "] :" + msg);
    }

    /*
     * printing function - allow messages to post in screen and log
     */
    private void print(String msg)
    {
        m_log.log(msg);
        m_vbox.write("    " + msg);
        // System.out.println (msg);
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

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();
        switch (key)
        {
            case OCRcEvent.VK_MENU:
                print("calling testSendAPDU...");
                testSendAPDU();
                break;
            case OCRcEvent.VK_1:
                print("getInstance ...");
                testGetInstance();
                break;
            case OCRcEvent.VK_2:
                print("isReady...");
                testIsReady();
                break;
            case OCRcEvent.VK_3:
                print("getManufacturerID ...");
                testGetManufacturerID();
                break;
            case OCRcEvent.VK_4:
                print("getVersionNumber ...");
                testGetVersionNumber();
                break;
            case OCRcEvent.VK_5:
                print("getApplications ...");
                testGetApplications();
                break;
            case OCRcEvent.VK_6:
                print("getHostFeatureList...");
                testGetHostFeatureList();
                break;
            case OCRcEvent.VK_7:
                print("getHostParam ...");
                testGetHostParam();
                break;
            case OCRcEvent.VK_8:
                print("setHostParamHandler...");
                testSetHostParamHandler();
                break;
            case OCRcEvent.VK_9:
                print("updateHostParam...");
                testUpdateHostParam();
                break;
            case OCRcEvent.VK_INFO:
                printTestList();
                break;
            case OCRcEvent.VK_EXIT:
                print("Not Exiting");
                break;
            case OCRcEvent.VK_0:
            case OCRcEvent.VK_MUTE:
            case OCRcEvent.VK_VOLUME_DOWN:
            case OCRcEvent.VK_VOLUME_UP:
            default:
                break;
        }
    }

    /*
     * usage. Refreshed by pressing the "info" key.
     */
    private void printTestList()
    {
        print("");
        print(SECTION_DIVIDER);
        print("org.ocap.system.SystemModule;");
        print("Press <MENU> sendAPDU");
        print(SECTION_DIVIDER);
        print("org.ocap.hardware.pod.POD");
        print("Press <1> getInstance()");
        print("Press <2> isReady()");
        print("Press <3> getManufacturerID()");
        print("Press <4> getVersionNumber()");
        print("Press <5> getApplications()");
        print("Press <6> getHostFeatureList()");
        print("Press <7> getHostParam( - all of them - )");
        print("Press <8> setHostParamHandler");
        print("Press <9> updateHostParam( - all of them - )");
        print(SECTION_DIVIDER);
        print("Press <INFO> for the list of tests");
    }

    /*
     * Implements the HostParamHandletInterface
     */
    public boolean notifyUpdate(int fID, byte[] value)
    {
        String nameID = fID > featureID.length ? "Reserved" : featureID[fID];
        print("   " + nameID + " notifyUpdate Called");
        return true;
    }

    /*
     * Add new tests below here
     */

    /*
     * Test sending an APDU.
     */
    private void testSendAPDU()
    {

        // Define an APDU to send.
        int SAS_ASYNC_MSG_APDU = 0x9F9A07;

        // Dummy data to send with apdu.
        byte[] dataByte = { 0x01, 0x00, 0x04, 0x01, 0x23, 0x45, 0x67 };

        if (null == m_module)
        {
            SystemModuleRegistrar smr = SystemModuleRegistrar.getInstance();
            try
            {
                smr.registerSASHandler(this, m_hostAppID);
            }
            catch (Exception e)
            {
                print("   registerSASHandler(...) threw an exception.");
                m_log.log(e);
                return;
            }
        }

        // Ensure that system returned a SystemModule.
        for (int i = 0; m_module == null; i++)
        {

            if (i >= 10)
            {
                print("   m_module == null?  No SystemModule created.");
                return;
            }

            try
            {
                Thread.sleep(1000);
            }
            catch (Exception e)
            {
            }
        }

        // Call sendAPDU
        try
        {
            m_module.sendAPDU(SAS_ASYNC_MSG_APDU, dataByte);
            // -S1 Verify sendAPDU did not throw exception
        }
        catch (Exception e)
        {
            print("   sendAPDU(...) threw an exception.");
            m_log.log(e);
            return;
        }

        // As POD applications are vendor specific, it is meaningless to
        // determine whether APDU really worked.
    }

    public synchronized void receiveAPDU(int apduTag, int length, byte[] data)
    {
        // Incoming Data Request; outgoing Data Available
        if (apduTag == m_dataRqst)
        {
            int txnId = (m_txn & 0xFF) >> 0;
            print("<--- data_rqst() " + "---> data_av(data, txn 0x" + Integer.toHexString(txnId) + ")");
            m_module.sendAPDU(m_dataAv, m_dataAvail);
        }

        // Incoming Data Available, outgoing Data Confirm & Query
        if (apduTag == m_dataAv)
        {
            byte status = (byte) data[0];
            int txnId = (data[1] & 0xFF) >> 0;
            byte[] txn = { data[1] };

            if (status == 0)
            {
                m_echoTxn = data[1];
                print("<--- data_av(data, txn 0x" + Integer.toHexString(txnId) + ") " + "---> data_cnf(data, txn 0x"
                        + Integer.toHexString(txnId) + ") " + "---> server_query(txn 0x" + Integer.toHexString(txnId)
                        + ")");
                m_module.sendAPDU(m_dataCnf, txn);
                m_module.sendAPDU(m_serverQuery, txn);
            }
            if (status == 1)
            {
                print("<--- data_av(no data, txn 0x" + Integer.toHexString(txnId) + ") "
                        + "---> data_cnf(no data, txn 0x" + Integer.toHexString(txnId) + ")");
                m_module.sendAPDU(m_dataCnf, txn);
            }
        }

        // Incoming Data Confirmation, No outgoing message
        if (apduTag == m_dataCnf)
        {
            int txnId = (data[0] & 0xFF) >> 0;
            print("<--- data_cnf(txn 0x" + Integer.toHexString(txnId) + ")");
        }

        // Incoming Server Query, outgoing Server Reply
        if (apduTag == m_serverQuery)
        {
            int txnId = (data[0] & 0xFF) >> 0;
            print("<--- server_query(txn 0x" + Integer.toHexString(txnId) + ") " + "---> server_reply(data, txn 0x"
                    + Integer.toHexString(txnId) + ")");
            m_module.sendAPDU(m_serverReply, m_replyData);
        }

        // Incoming Server Reply, verify Reply echos data sent to card.
        if (apduTag == m_serverReply)
        {
            int txnId = (data[0] & 0xFF) >> 0;
            print("<--- server_reply(txn 0x" + Integer.toHexString(txnId) + ")");

            // Transaction numbers of Reply and Sent data match.
            if (data[0] == m_echoTxn)
            {

                StringBuffer sentData = new StringBuffer();
                for (int i = 3; i < data.length; i++)
                {
                    sentData.append(Integer.toHexString((m_replyData[i] & 0xFF) >> 0));
                }
                StringBuffer receivedData = new StringBuffer();
                for (int i = 3; i < data.length; i++)
                {
                    receivedData.append(Integer.toHexString((data[i] & 0xFF) >> 0));
                }

                if (!receivedData.toString().equals(sentData.toString()))
                {
                    print("SAS Resource did not correctly echo data. " + receivedData.toString());
                }
                else
                {
                    print("SAS Resource correctly echoed data. " + receivedData.toString());
                }

                try
                {
                    destroyXlet(true);
                }
                catch (Exception e)
                {
                    m_log.log("destroy() threw " + e);
                }
            }
        }
        notifyAll();
    }

    public synchronized void sendAPDUFailed(int apduTag, byte[] data)
    {
        print("sendAPDUFailed for APDU(" + apduTag + ") called");
        notifyAll();
    }

    public synchronized void notifyUnregister()
    {
        print("notifyUnregister called");
        notifyAll();
    }

    public synchronized void ready(SystemModule module)
    {

        if (module == null)
        {
            print("SystemModule NULL in ready()?!");
            try
            {
                destroyXlet(true);
            }
            catch (Exception e)
            {
                m_log.log("destroyXlet() threw " + e);
            }
        }

        m_module = module;
        notifyAll();
    }

    /*
     * test the method org.ocap.hardware.pod.POD.getApplications()
     */
    private void testGetApplications()
    {
        int expectedNumberOfApps = 0;
        PODApplication[] apps;
        int actualNumberOfApps;
        try
        {
            POD pod = POD.getInstance();
            apps = pod.getApplications();
            print("   " + expectedNumberOfApps + " applications expected");
            actualNumberOfApps = apps == null ? 0 : apps.length;
            print("   " + actualNumberOfApps + " applications found");
        }
        catch (java.lang.IllegalStateException ise)
        {
            print("   getApplications threw an java.lang.IllegalStateException");
            ise.printStackTrace();
            m_log.log(ise);
            return;
        }
        catch (java.lang.SecurityException e)
        {
            print("   getApplications threw java.lang.SecurityException");
            m_log.log(e);
            return;
        }
        if (apps == null && expectedNumberOfApps > 0)
        {
            print("   getApplications unexpectedly returned null");
            return;
        }
        if (expectedNumberOfApps != actualNumberOfApps)
        {
            print("   getApplications returned " + actualNumberOfApps + " elements, expected " + expectedNumberOfApps);
        }
        for (int i = 0; i <= actualNumberOfApps - 1; i++)
        {
            print("   " + apps[i].getName());
            print("   " + apps[i].getType());
            print("   " + apps[i].getVersionNumber());
            print("   " + apps[i].getURL());
        }
    }

    /*
     * test the method org.ocap.hardware.pod.POD.getInstance()
     */
    private void testGetInstance()
    {
        try
        {
            POD pod = POD.getInstance();
        }
        catch (java.lang.SecurityException e)
        {
            print("   getInstance threw java.lang.SecurityException");
            m_log.log(e);
            return;
        }
        print("   instance successfully returned by org.ocap.hardware.pod.POD.getInstance()");
    }

    /*
     * test the method org.ocap.hardware.pod.POD.isReady()
     */
    private void testIsReady()
    {
        boolean ir = false;
        try
        {
            POD pod = POD.getInstance();
            ir = pod.isReady();
        }
        catch (java.lang.SecurityException e)
        {
            print("   isReady threw java.lang.SecurityException ");
            return;
        }
        print("   " + ir + " returned by org.ocap.hardware.pod.POD.isReady()");
    }

    /*
     * test the method org.ocap.hardware.pod.POD.getManufacturerID()
     */
    private void testGetManufacturerID()
    {
        int id = 0;
        try
        {
            POD pod = POD.getInstance();
            id = pod.getManufacturerID();
        }
        catch (java.lang.IllegalStateException e)
        {
            print("   getManufacturerID threw java.lang.IllegalStateException");
            m_log.log(e);
            return;
        }
        catch (java.lang.SecurityException e)
        {
            print("   getInstance threw java.lang.SecurityException");
            m_log.log(e);
            return;
        }
        print("   " + id + " returned by org.ocap.hardware.pod.POD.getManufacturerID()");
    }

    /*
     * test the method org.ocap.hardware.pod.POD.getVersionNumber()
     */
    private void testGetVersionNumber()
    {
        int vn = 0;
        try
        {
            POD pod = POD.getInstance();
            vn = pod.getVersionNumber();
        }
        catch (java.lang.IllegalStateException e)
        {
            print("   getVersionNumber threw java.lang.IllegalStateException");
            m_log.log(e);
            return;
        }
        catch (java.lang.SecurityException e)
        {
            print("   getInstance threw java.lang.SecurityException");
            m_log.log(e);
            return;
        }
        print("   " + vn + " returned by org.ocap.hardware.pod.POD.getVersionNumber()");
    }

    /*
     * test the method org.ocap.hardware.pod.POD.getHostFeatureList()
     */
    private void testGetHostFeatureList()
    {
        int[] features = null;
        int expectedNumberOfFeatures = 12;
        int actualNumberOfFeatures = 0;
        POD pod = null;
        try
        {
            pod = POD.getInstance();
            features = pod.getHostFeatureList();
            actualNumberOfFeatures = features.length;
            print("   ... " + actualNumberOfFeatures + " features found");
        }
        catch (java.lang.IllegalStateException e)
        {
            print("   getHostFeatureList threw java.lang.IllegalStateException");
            m_log.log(e);
            return;
        }
        catch (java.lang.SecurityException e)
        {
            print("   getHostFeatureList threw java.lang.SecurityException");
            m_log.log(e);
            return;
        }
        if (features == null && expectedNumberOfFeatures > 0)
        {
            print("   getHostFeatureList unexpectedly returned null");
            return;
        }
        if (expectedNumberOfFeatures != actualNumberOfFeatures)
        {
            print("   getHostFeatureList returned " + actualNumberOfFeatures + " elements, expected "
                    + expectedNumberOfFeatures);
            return;
        }
        for (int i = 0; i < expectedNumberOfFeatures; i++)
        {
            String nameID = features[i] > featureID.length ? "Reserved" : featureID[features[i]];
            print("   " + features[i] + " " + nameID);
        }
    }

    /*
     * test the method org.ocap.hardware.pod.POD.getHostParam()
     */
    private void testGetHostParam()
    {
        int[] features = null;
        int expectedNumberOfFeatures = 12;
        int actualNumberOfFeatures = 0;
        POD pod = null;
        try
        {
            pod = POD.getInstance();
            features = pod.getHostFeatureList();
            actualNumberOfFeatures = features.length;
            print("   ... " + actualNumberOfFeatures + " features found");
        }
        catch (java.lang.IllegalStateException e)
        {
            print("   getHostParam  threw java.lang.IllegalStateException");
            m_log.log(e);
            return;
        }
        catch (java.lang.SecurityException e)
        {
            print("    getHostParam threw java.lang.SecurityException");
            m_log.log(e);
            return;
        }
        if (features == null && expectedNumberOfFeatures > 0)
        {
            print("    getHostParam unexpectedly returned null");
            return;
        }
        if (expectedNumberOfFeatures != actualNumberOfFeatures)
        {
            print("    getHostParam returned " + actualNumberOfFeatures + " elements, expected "
                    + expectedNumberOfFeatures);
            return;
        }
        for (int i = 0; i < expectedNumberOfFeatures; i++)
        {
            String nameID = features[i] > featureID.length ? "Reserved" : featureID[features[i]];
            byte fID[] = pod.getHostParam(features[i]);
            if (fID.length == 0)
            {
                print("   FAIL: Generic feature " + nameID + " is not supported");
                continue;
            }
            int expectedNBytes = expectedFeatureBytes[features[i]].length;
            if (expectedNBytes != fID.length)
            {
                print("   FAIL: " + nameID + " found " + fID.length + " bytes, expected " + expectedNBytes + " bytes");
            }
            else
            {
                boolean noErrs = true;
                for (int j = 0; j < expectedNBytes; j++)
                {
                    if (fID[j] != (byte) expectedFeatureBytes[features[i]][j])
                    {
                        print("   FAIL: " + nameID + " found " + fID[j] + " byte, expected "
                                + expectedFeatureBytes[features[i]][j] + " byte");
                        noErrs = false;
                    }
                }
                if (noErrs)
                {
                    print("      PASS: " + nameID + " found " + fID.length + " bytes, expected " + expectedNBytes
                            + " bytes");
                }
                else
                {
                    print("   FAIL: " + nameID + " Number of bytes correct but 1 or more bytes not as expected");
                }
            }
        }
    }

    /*
     * test the method org.ocap.hardware.pod.POD.updateHostParam()
     */
    private void testUpdateHostParam()
    {
        int[] features = null;
        int expectedNumberOfFeatures = 12;
        int actualNumberOfFeatures = 0;
        POD pod = null;
        try
        {
            pod = POD.getInstance();
            features = pod.getHostFeatureList();
            actualNumberOfFeatures = features.length;
            print("   ... " + actualNumberOfFeatures + " features found");
        }
        catch (java.lang.IllegalStateException e)
        {
            print("   getHostFeatureList threw java.lang.IllegalStateException");
            m_log.log(e);
            return;
        }
        catch (java.lang.SecurityException e)
        {
            print("   getHostFeatureList threw java.lang.SecurityException");
            m_log.log(e);
            return;
        }
        if (features == null && expectedNumberOfFeatures > 0)
        {
            print("   getHostFeatureList unexpectedly returned null");
            return;
        }
        if (expectedNumberOfFeatures != actualNumberOfFeatures)
        {
            print("   getHostFeatureList returned " + actualNumberOfFeatures + " elements, expected "
                    + expectedNumberOfFeatures);
            return;
        }
        for (int i = 0; i < expectedNumberOfFeatures; i++)
        {
            String nameID = features[i] > featureID.length ? "Reserved" : featureID[features[i]];
            // attempt to increase each byte by 1
            int expectedNBytes = expectedFeatureBytes[features[i]].length;
            byte[] newBytes = new byte[expectedNBytes];
            for (int j = 0; j < expectedNBytes; j++)
            {
                newBytes[j] = (byte) (expectedFeatureBytes[features[i]][j] < 0Xff ? expectedFeatureBytes[features[i]][j]++
                        : 0x00);
            }
            try
            {
                boolean result = pod.updateHostParam(features[i], newBytes);
                print("   " + result + " " + features[i] + " " + nameID);
            }
            catch (java.lang.IllegalArgumentException iae)
            {
                print("   updateHostParam threw java.lang.IllegalArgumentException");
                m_log.log(iae);
            }
        }
    }

    private void testSetHostParamHandler()
    {
        try
        {
            POD pod = POD.getInstance();
            pod.setHostParamHandler(this);
        }
        catch (java.lang.SecurityException e)
        {
            print("   setHostParamHandler threw java.lang.SecurityException");
            m_log.log(e);
            return;
        }
        print("   using the test programs notifyUpdate method");
    }
}// end of all

