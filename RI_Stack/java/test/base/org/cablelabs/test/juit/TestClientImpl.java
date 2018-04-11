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

package org.cablelabs.test.juit;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

import org.dvb.test.DVBTest;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HState;
import org.havi.ui.HStaticText;
import org.havi.ui.HVisible;
import org.havi.ui.event.HRcEvent;

import com.sun.tck.mhp.TestClient;

public class TestClientImpl implements TestClient, KeyListener
{

    private HScene scene;

    private HStaticText screenMessage;

    private int responseKey;

    public TestClientImpl()
    {
        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.setSize(640, 480);
        scene.addKeyListener(this);
        scene.setLayout(null);
        scene.removeAll();

        Color bgcolor = new Color(0x08, 0x04, 0x08);

        screenMessage = new HStaticText("", 0, 360, 660, 40);
        screenMessage.setForeground(Color.white);
        screenMessage.setBackground(bgcolor);
        screenMessage.setBackgroundMode(HVisible.BACKGROUND_FILL);

        HStaticText yesno = new HStaticText("(1 = Yes, 3 = No)", 0, 400, 660, 40);
        yesno.setForeground(Color.white);
        yesno.setBackground(bgcolor);
        yesno.setBackgroundMode(HVisible.BACKGROUND_FILL);

        scene.add(screenMessage);
        scene.add(yesno);
    }

    /**
     * Prompts the user for a response to the provided message. This will return
     * quietly if the user responds positively, and will throw an IOException if
     * the user responds negatively or no response is entered within 30 seconds.
     */
    public synchronized void prompt(String id, int controlCode, String message) throws IOException
    {
        log(id, message + " (1 = Yes, 3 = No)");
        screenMessage.setTextContent(id + ": " + message, HState.NORMAL_STATE);
        scene.show();
        scene.requestFocus();
        try
        {
            wait(30000);
        }
        catch (InterruptedException ex)
        {
            scene.setVisible(false);
            throw new IOException("Unexpectedly interrupted while waiting for user input");
        }

        scene.setVisible(false);

        if (responseKey != HRcEvent.VK_1)
        {
            throw new IOException("User notified failure of test case or did not respond");
        }

    }

    /**
     * Logs output as an integer.
     */
    public void log(String id, int no)
    {
        log(id, "" + no);
    }

    /**
     * Logs output as a String.
     */
    public void log(String id, String message)
    {
        System.out.println("********************************************************************************");
        System.out.println("TestClient log: " + id + ": " + message);
        System.out.println("********************************************************************************");
    }

    /**
     * Logs to the console that the test has terminated with the provided code.
     */
    public void terminate(String id, int terminationCondition)
    {
        String message;
        switch (terminationCondition)
        {
            case DVBTest.PASS:
                message = "Test terminated with the following code: PASS";
                break;
            case DVBTest.FAIL:
                message = "Test terminated with the following code: FAIL";
                break;
            case DVBTest.UNRESOLVED:
                message = "Test terminated with the following code: UNRESOLVED";
                break;
            case DVBTest.UNTESTED:
                message = "Test terminated with the following code: UNTESTED";
                break;
            case DVBTest.HUMAN_INTERVENTION:
                message = "Test terminated with the following code: HUMAN_INTERVENTION";
                break;
            case DVBTest.OPTION_UNSUPPORTED:
                message = "Test terminated with the following code: UNSUPPORTED";
                break;
            default:
                message = "Test terminated with the following code: UNKNOWN (" + terminationCondition + ")";
        }
        log(id, message);
        // throw new IOException(message);
    }

    public void keyTyped(KeyEvent event)
    {
        // no-op

    }

    public synchronized void keyPressed(KeyEvent event)
    {
        int keyCode = event.getKeyCode();

        if (keyCode == HRcEvent.VK_1 || keyCode == HRcEvent.VK_3)
        {
            responseKey = keyCode;
            notify();
        }

    }

    public void keyReleased(KeyEvent event)
    {
        // no-op

    }

}
