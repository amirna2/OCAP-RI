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

package org.cablelabs.xlet.MAHTest;

import org.cablelabs.xlet.DvrTest.*;

import java.util.*;
import java.awt.event.*;
import org.havi.ui.*;
import org.ocap.net.OcapLocator;
import org.dvb.ui.DVBColor;
import javax.tv.util.*;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import java.io.*;
import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.*;

public class MAHTestRunnerXlet extends DVRTestRunnerXlet implements Xlet, KeyListener, Driveable
{
    public void initXlet(XletContext ctx)
    {
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

        m_log.log("MAHTestRunnerXlet.initXlet()");

        // store off our xlet context
        m_xctx = ctx;
        /*
         * Grab valid service locators from Xlet params
         */
        m_serviceLocators = retrieveDvrLocators(ctx);

        /*
         * Establish self as RC key listener
         */
        System.out.println("Setting up key listener and havi interface");
        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_vbox = new VidTextBox(50, 50, 530, 370, 14, 5000);
        m_vbox.setBackground(new DVBColor(128, 128, 128, 155));
        m_vbox.setForeground(new DVBColor(200, 200, 200, 255));

        scene.add(m_vbox);
        scene.addKeyListener(this);
        scene.addKeyListener(m_vbox);

        m_timer = TVTimer.getTimer();

        System.out.println("Setup test case list");
        m_testList = new Vector();

        m_loopingTests = new Vector();

        // setup a monitor that will be used by the event dispatcher
        // to synchronize testcase launch events
        m_eventMonitor = new Monitor();
    }

    /**
     * implementation of Xlet interface
     */
    public void startXlet()
    {
        m_log.log("MAHTestRunnerXlet.startXlet()");
        /*
         * Request UI keys
         */
        System.out.println("MAHTestRunnerXlet:startXlet()");
        scene.show();
        scene.requestFocus();
        stopTest = false;
        log("Make a group selection");
        printTestGroups();
    }

    /**
     * implementation of Xlet interface
     */
    public void destroyXlet(boolean x)
    {
        System.out.println("MAHTestRunnerXlet: destroyXlet");
        // hide scene
        scene.setVisible(false);
        // dispose of self
        HScene tmp = scene;
        scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    /**
     * Read MAH source ids from the config file, and build an array of OCAP
     * Locators which can be used for tuning and scheduling.
     * 
     * @param ctx
     * @return
     */
    public synchronized Vector retrieveDvrLocators(XletContext ctx)
    {
        Vector locators = new Vector();
        FileInputStream fis_count = null;
        FileInputStream fis_read = null;

        try
        {
            // Get path name of config file.
            ArgParser xlet_args = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));
            String str_config_file_name = xlet_args.getStringArg(CONFIG_FILE);

            fis_count = new FileInputStream(str_config_file_name);
            byte[] buffer = new byte[fis_count.available() + 1];
            fis_count.read(buffer);
            String str_config_file = new String(buffer);
            fis_read = new FileInputStream(str_config_file_name);
            ArgParser config_args = new ArgParser(fis_read);

            String idName = null;
            String str = config_args.getStringArg(DVR_IF_FPQ);
            if (str.compareTo("TRUE") == 0)
            {
                idName = DVR_FPQ;
                System.out.println("Using Frequency, Progm QAM");
            }
            else
            {
                idName = DVR_SOURCE_ID;
                System.out.println("Using Source Id");
            }
            // Count MAH source ids in config file.
            int chan_count = -1;
            while (str_config_file.indexOf(idName + ++chan_count) != -1);
            fis_count.close();

            // Read MAH channel from config file.
            if (idName.compareTo(DVR_SOURCE_ID) == 0)
            {
                for (int i = 0; i < chan_count; i++)
                {
                    System.out.println("Using Source Id");
                    String str_source_id = config_args.getStringArg(DVR_SOURCE_ID + i);
                    locators.addElement((Object) (new OcapLocator("ocap://" + str_source_id)));
                    System.out.println("MAH Tests locator: " + locators.elementAt(i));
                }
            }
            else
            {
                for (int i = 0; i < chan_count; i++)
                {
                    int frequency = 0;
                    int programNum = 0;
                    int qam = 0;
                    System.out.println("Using Frequency, Progm QAM");
                    String str_fpq = config_args.getStringArg(DVR_FPQ + i);
                    StringTokenizer st = new StringTokenizer(str_fpq, ",");
                    String elem = st.nextToken();
                    System.out.println("Using freq " + elem);
                    frequency = Integer.parseInt(elem);
                    elem = st.nextToken();
                    programNum = Integer.parseInt(elem);
                    elem = st.nextToken();
                    qam = Integer.parseInt(elem);

                    System.out.println(" Channel- freq: " + frequency + " qam :" + qam + " pid :" + programNum);
                    locators.addElement((Object) new OcapLocator(frequency, programNum, qam));
                    System.out.println("MAH Tests locator: " + locators.elementAt(i));
                }
            }
            fis_read.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return locators;
    }

    static public XletContext getContext()
    {
        return m_xctx;
    }

    public void getTestSuite(int suite)
    {
        if (m_testList.size() != 0)
        {
            m_testList.removeAllElements();
        }
        switch (suite)
        {
            case 0:
                addTests(new TestServiceBlocking(m_serviceLocators).getTests());
                addTests(new TestOverrideServiceBlocking(m_serviceLocators).getTests());
                break;
            case 1:

                break;
            case 2:

                break;
        }
    }

    public void printTestGroups()
    {
        for (int i = 0; i < m_testGroups.length; i++)
        {
            log("Group " + ((int) (i + 1)) + ": " + m_testGroups[i]);
        }
    }

    protected String[] m_testGroups = { "Service Based Testing", "Ratings Based Testing", "Stress Tests" };
}
