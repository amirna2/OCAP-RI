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

package org.cablelabs.xlet.PropertiesTest;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.security.AccessControlException;
import java.util.Vector;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;

import org.cablelabs.test.autoxlet.*;

/**
 * Class PropertiesTestXlet gets values of application properties, and outputs
 * them to a console window and to a TV screen.
 */
public class PropertiesTestXlet extends Container implements Xlet, Driveable
{
    /**
     * Sets up the scene to display text strings.
     * 
     * @param context
     *            XletContext
     */
    public void initXlet(XletContext context) throws XletStateChangeException
    {
        // Set up the AutoXlet mechanism and populate our local Test and
        // Logger references
        axc = new AutoXletClient(this, context);
        logger = axc.getLogger();
        test = axc.getTest();

        // Set up the scene to display text strings.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setVisible(false);
        m_scene.add(this);

        ocapPropTable = new Vector();

        setBounds(0, 0, 640, 480);
        setBackground(Color.blue);
        setForeground(Color.white);
        setFont(FONT);

        // Determine my app type
        String aidStr = (String) context.getXletProperty("dvb.app.id");
        int aid = Integer.parseInt(aidStr, 16);

        if (aid >= 0 && aid <= 0x3fff)
        {
            myAppType = UNSIGNED_APP;
            myAppTypeStr = "unsigned app (aid=" + aidStr + ")";
        }
        else if (aid >= 0x4000 && aid <= 0x5fff)
        {
            myAppType = SIGNED_APP;
            myAppTypeStr = "signed app (aid=" + aidStr + ")";
        }
        else if (aid >= 0x6000 && aid <= 0x6fff)
        {
            myAppType = MONAPP_WITH_PRF_APP;
            myAppTypeStr = "monitor app (aid=" + aidStr + ")";
        }
        else if (aid >= 0x7000 && aid <= 0x7fff)
        {
            myAppType = MONAPP_WITH_PRF_APP;
            myAppTypeStr = "super host app (aid=" + aidStr + ")";
        }
        else
            logger.log("Invalid AppID!" + Integer.toHexString(aid));
    }

    /**
     * Gets values of application properties, and outputs them to a console
     * window.
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        if (!started)
        {
            // addOcapPropElement("user.dir",null,ALL_PERMISSIONS_APP_PERMISSIONS);
            addOcapPropElement("user.dir", null, UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("ocap.j.location", null, UNSIGNED_APP_PERMISSIONS);

            addOcapPropElement("havi.specification.vendor", "DVB", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("havi.specification.name", "MHP", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("havi.specification.version", "1.1", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("havi.implementation.vendor", "CableLabs", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("havi.implementation.name", "OCAP HAVi", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("havi.implementation.version", null, UNSIGNED_APP_PERMISSIONS);

            addOcapPropElement("dvb.persistent.root", null, SIGNED_APP_PERMISSIONS);
            addOcapPropElement("dvb.returnchannel.timeout", "10000", UNSIGNED_APP_PERMISSIONS);

            addOcapPropElement("mhp.profile.enhanced_broadcast", "YES", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("mhp.eb.version.major", "1", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("mhp.eb.version.minor", "0", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("mhp.eb.version.micro", "3", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("mhp.profile.interactive_broadcast", "YES", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("mhp.ib.version.major", "1", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("mhp.ib.version.minor", "0", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("mhp.ib.version.micro", "3", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("mhp.profile.internet_access", null, UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("mhp.ia.version.major", null, UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("mhp.ia.version.minor", null, UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("mhp.ia.version.micro", null, UNSIGNED_APP_PERMISSIONS);

            addOcapPropElement("ocap.profile", "OCAP 1.1", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("ocap.version", "1.3.1", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("ocap.version.update", "", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("ocap.hardware.serialnum", null, MONAPP_WITH_PRF_APP_PERMISSIONS);
            addOcapPropElement("ocap.hardware.vendor_id", null, MONAPP_WITH_PRF_APP_PERMISSIONS);
            addOcapPropElement("ocap.hardware.version_id", null, MONAPP_WITH_PRF_APP_PERMISSIONS);
            addOcapPropElement("ocap.hardware.createdate", null, MONAPP_WITH_PRF_APP_PERMISSIONS);
            addOcapPropElement("ocap.memory.video", null, MONAPP_WITH_PRF_APP_PERMISSIONS);
            addOcapPropElement("ocap.memory.total", null, MONAPP_WITH_PRF_APP_PERMISSIONS);
            addOcapPropElement("ocap.system.highdef", null, UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("ocap.api.option.dvr", "1.0", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("ocap.api.option.dvr.update", "I09", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("ocap.api.option.hn", "3.0", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("ocap.api.option.hn.update", "I11", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("ocap.api.option.ds", "5.0", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("ocap.api.option.fp", "1.0", UNSIGNED_APP_PERMISSIONS);
            addOcapPropElement("ocap.api.option.fp.update", "I05", UNSIGNED_APP_PERMISSIONS);

            // ECR OCAP1.0.2-N-08.1216-3
            // additional platform specific system properties
            String apsspStr[] = { "ocap.hardware.version", "ocap.hardware.model_id", "ocap.software.model_id",
                    "ocap.software.vendor_id", "ocap.software.version" };
            for (int x = 0; x < apsspStr.length; x++)
                addOcapPropElement(apsspStr[x], null, MONAPP_WITH_PRF_APP_PERMISSIONS);

            started = true;
        }
        logger.log("*************************************************************");
        logger.log("*************************************************************");
        logger.log("***************** OCAP Required Properties ******************");
        logger.log("*************************************************************");
        logger.log("*************************************************************");

        for (int i = 0; i < ocapPropTable.size(); ++i)
        {
            String str = (String) (ocapPropTable.elementAt(i));
            int colonIndex = str.indexOf(':');
            String key = str.substring(0, colonIndex);
            String value = str.substring(colonIndex + 1);

            logger.log(key + ":" + value);

            if (value.startsWith("FAIL"))
            {
                failureCount++;
            }
        }

        m_scene.show();
        m_scene.requestFocus();

        if (!axc.isConnected()) logger.log(test.getTestResult());
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
        started = false;
    }

    private void addOcapPropElement(String key, String testValue, int requiredPermission)
    {
        if (key == null) return;

        String propValue;
        try
        {
            propValue = System.getProperty(key);

            // Make sure we were supposed to have permission to access this prop
            if ((myAppType & requiredPermission) == 0)
            {
                test.fail("This app should not have permission to read " + key);
                propValue = "FAIL-should NOT have permission to read " + propValue;
            }
            else
            {
                test.assertTrue(true); // Passed permission test

                // For some properties, we know what we expect the value to be.
                // For others, we don't know, so we won't test the value
                if (testValue != null && propValue != null)
                {
                    test.assertTrue("Properties do not match! " + key + " -- Expected: " + testValue + "  Actual: "
                            + propValue, propValue.equals(testValue));
                    if (!testValue.equals(propValue))
                    {
                        propValue = "FAIL-got " + propValue + ",expected " + testValue;
                    }
                }
            }

            ocapPropTable.addElement(key + ":" + propValue);
        }
        catch (AccessControlException ace)
        {
            // Make sure we were not supposed to have permission to access this
            // prop
            if ((myAppType & requiredPermission) != 0)
            {
                test.fail("This app should have permission to read " + key);
                ocapPropTable.addElement(key + ":" + "FAIL-access to " + key + " denied");
            }
            else
                test.assertTrue(true); // passed the test
        }
    }

    /**
     * Outputs values of application properties to a TV screen.
     * 
     * @param g
     *            Graphics
     */
    public void paint(Graphics g)
    {
        int x = 25;
        int y = 15;
        int horiz_offset = 225;
        int vert_offset = 14;

        if (failureCount == 0)
        {
            g.drawString("***************** " + myAppTypeStr + " : PASS ******************", 150, y);
            System.out.println("***************** " + myAppTypeStr + " : PASS ******************");
        }
        else
        {
            g.drawString("***************** " + myAppTypeStr + " : " + failureCount + " FAILURES ******************",
                    150, y);
            System.out.println("***************** " + myAppTypeStr + " : " + failureCount
                    + " FAILURES ******************");
        }
        y = y + (vert_offset * 2);

        for (int i = 0; i < ocapPropTable.size(); ++i)
        {
            String str = (String) (ocapPropTable.elementAt(i));
            int colonIndex = str.indexOf(':');
            String key = str.substring(0, colonIndex);
            String value = str.substring(colonIndex + 1);

            setForeground(Color.white);
            g.drawString(key + ":", x, y);
            setForeground(Color.green);
            g.drawString(value, horiz_offset, y);
            y += vert_offset;

            if (value.startsWith("FAIL"))
            {
                System.out.println("Incorrect value returned for " + key + ": " + value);
            }
        }

    }

    public void dispatchEvent(KeyEvent arg0, boolean arg1, int arg2)
    {
        // Xlet does not use remote control events
    }

    private static final Font FONT = new Font("tiresias", Font.PLAIN, 14);

    private HScene m_scene = null;

    private Vector ocapPropTable = null;

    private AutoXletClient axc = null;

    private Logger logger = null;

    private Test test = null;

    private int myAppType;

    private String myAppTypeStr = "";

    private int failureCount = 0;

    private boolean started = false;

    // These values used to identify this particular application by inspecting
    // the appID. Each type has all the permissions of the previous types.
    private static final int UNSIGNED_APP = 1;

    private static final int SIGNED_APP = (1 << 1) | 1;

    private static final int MONAPP_WITH_PRF_APP = (1 << 2) | (1 << 1) | 1;

    // These values represent the permissions granted to each level of app. Each
    // level includes the permissions granted to the previous level
    private static final int UNSIGNED_APP_PERMISSIONS = 1;

    private static final int SIGNED_APP_PERMISSIONS = (1 << 1);

    private static final int MONAPP_WITH_PRF_APP_PERMISSIONS = (1 << 2);

    private static final int ALL_PERMISSIONS_APP_PERMISSIONS = (1 << 3);
}
