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

package org.cablelabs.xlet.hn.srs.iteration1;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.ocap.hn.content.MetadataIdentifiers;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.recording.NetRecordingSpec;

/**
 * This test is for Iteraton 1 features: MetadataNode, MetadataIdentifiers and
 * NetRecordingSpec
 * 
 * @author Dan Woodard
 * 
 */
public class Test1 extends Component implements Xlet
{

    private static final Color TEXT_COLOR = new Color(255, 255, 230);

    private static final Color BG_COLOR = new Color(100, 100, 100);

    private static final Font FONT = new Font("tiresias", Font.PLAIN, 30);

    private static final Rectangle BOX_RECT = new Rectangle(95, 150, 450, 200);

    private HScene testScene;

    private static String testName = "Test1";

    private boolean testResult = false;

    private boolean displayResult = true;

    private String testResultString = null;

    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        // TODO Auto-generated method stub

    }

    public void initXlet(XletContext arg0) throws XletStateChangeException
    {
        System.out.println("Test1: initXlet");
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

    public void pauseXlet()
    {
        // TODO Auto-generated method stub

    }

    public void startXlet() throws XletStateChangeException
    {
        testScene.setVisible(true);
        testScene.requestFocus();

        repaint();

        System.out.println("Test1: startXlet");

        if (runTest1())
        {
            this.testResult = true;

            System.out.println("Test1: MetadataNode,NetRecordingSpec test PASSED");
        }
        else
        {
            this.testResult = false;

            System.out.println("Test1: MetadataNode,NetRecordingSpec test FAILED");
        }

        if (testResult == true && runTest2())
        {
            System.out.println("Test1: MetadataIdentifiers test PASSED");
        }
        else
        {
            this.testResult = false;

            System.out.println("Test1: MetadataIdentifiers test FAILED");
        }

        repaint();
    }

    /**
     * tests MetadataIdentifiers
     * 
     * @return true if passed
     */
    private boolean runTest2()
    {
        boolean result = true;

        String[] sa = MetadataIdentifiers.getIdentifiers();

        if (sa.length != MetadataIdentifiers.getNumberOfIdentifiers())
        {
            System.out.println("Test1: ERROR MetadataIdentifiers num of ids does not match length of getIdentifiers() array");
            result = false;
        }

        for (int i = 0; i < sa.length; i++)
        {
            if (MetadataIdentifiers.contains(sa[i]))
            {
                System.out.println("Test1: ERROR MetadataIdentifiers does not contain " + sa[i]);
                result = false;
                break;
            }
        }

        return result;
    }

    /**
     * Tests NetRecordingSpec and MetadataNode
     * 
     * @return true for pass, false for fail
     */
    private boolean runTest1()
    {
        boolean result = true;

        System.out.println("Test1: create NetRecordingSpec()");
        NetRecordingSpec spec = new NetRecordingSpec();// this will create an
                                                       // empty MetadataNode
        MetadataNode node = spec.getMetadata();

        if (node == null)
        {
            System.out.println("Test1: FAILED TO GET MetadataNode");
            return false;
        }
        System.out.println("Test1: got MetadataNode from NetRecordingSpec");

        System.out.println("Test1: create NetRecordingSpec(MetadataNode)");
        NetRecordingSpec spec2 = new NetRecordingSpec(node);// this will create
                                                            // an empty
                                                            // MetadataNode

        if (spec2.getMetadata() == node)
        {
            System.out.println("Test1: MetadataNode in NetRecordingSpec is the same instance as passed into constructor");
        }

        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        String key4 = "key4";
        String key5 = "key5";

        Object badobj = new Object();
        String obj1 = "obj1";
        String obj2 = "obj2";
        String obj3 = "obj3";
        String obj4 = "obj4";

        MetadataNode node2 = node.createMetadataNode("inputkey");

        try
        {
            node.addMetadata(key1, badobj);
            System.out.println("Test1: ERROR: did not verify value checking with caught IllegalArgumentException;");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("Test1: verified value checking with caught IllegalArgumentException;");
        }

        try
        {

            node2.addMetadata(key3, obj3);
            System.out.println("Test1: added key3");
            node2.addMetadata(key4, obj4);
            System.out.println("Test1: added key4");
            node.addMetadata(key1, obj1);
            System.out.println("Test1: added key1");
            node.addMetadata(key2, obj2);
            System.out.println("Test1: added key2");
            node.addMetadata(key5, node2);
            System.out.println("Test1: added key5");
            System.out.println("Test1: verified addMetadata;");
        }
        catch (IllegalArgumentException e1)
        {
            System.out.println("Test1: ERROR unexpected IllegalArgumentException " + e1);
            result = false;
        }

        if (node2.getParentNode() != node)
        {
            System.out.println("Test1: ERROR parent node incorrect");
            result = false;
        }

        if (!node2.getKey().equals(key5))
        {
            System.out.println("Test1: ERROR node2 key incorrect");
            result = false;
        }

        String[] keys = node2.getKeys();

        Set s = new HashSet();

        for (int i = 0; i < keys.length; i++)
        {
            s.add(keys[i]);
        }

        if (s.contains(key3) && s.contains(key4))
        {
            System.out.println("Test1: node2 contains the correct keys");
        }
        else
        {
            System.out.println("Test1: ERROR node2 does not contain the correct keys");
            result = false;
        }

        keys = node.getKeys();

        s.clear();

        for (int i = 0; i < keys.length; i++)
        {
            s.add(keys[i]);
        }

        if (s.contains(key1) && s.contains(key2) && s.contains(key5))
        {
            System.out.println("Test1: node contains the correct keys");
        }
        else
        {
            System.out.println("Test1: ERROR node does not contain the correct keys");
            result = false;
        }

        if (node.getMetadata(key1) == obj1 && node.getMetadata(key2) == obj2 & node.getMetadata(key5) == node2)
        {
            System.out.println("Test1: node contains the correct values");

        }
        else
        {
            System.out.println("Test1: ERROR node does not contain the correct values");
            result = false;
        }

        Enumeration enm = node.getMetadata();

        if (!enm.hasMoreElements())
        {
            System.out.println("Test1: ERROR node does not contain any values");
            result = false;
        }

        s.clear();

        while (enm.hasMoreElements())
        {
            s.add(enm.nextElement());
        }

        if (s.contains(obj1) && s.contains(obj2) && s.contains(node2))
        {
            System.out.println("Test1: node enumerates the correct values");
        }
        else
        {
            System.out.println("Test1: ERROR node does not enumerate the correct values");
            result = false;
        }

        return result;

    }

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
            String deviceWaitString = "Waiting for devices ...";
            int xResult = (BOX_RECT.width - getFontMetrics(FONT).stringWidth(deviceWaitString)) / 2;
            g.drawString(deviceWaitString, BOX_RECT.x + xResult, BOX_RECT.y + textHeight + 45);
        }
    }
}
