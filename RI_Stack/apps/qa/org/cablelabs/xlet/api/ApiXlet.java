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
package org.cablelabs.xlet.api;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Enumeration;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HScreen;
import org.havi.ui.HStaticText;
import org.ocap.application.AppManagerProxy;
import org.ocap.system.RegisteredApiManager;

import org.cablelabs.lib.utils.XaitGen.XAITGenerator;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.TestFailure;
import org.cablelabs.test.autoxlet.TestResult;
import org.cablelabs.test.autoxlet.XletLogger;

/**
 * Simple Xlet that demonstrates the registration of an API. When destroyed() it
 * will unregister the API.
 * 
 * @author Aaron Kamienski
 */
public class ApiXlet implements Xlet, Driveable
{
    protected XletContext ctx;

    protected AutoXletClient axc;

    protected Logger log;

    protected Test test;

    private boolean started;

    static final String APINAME = "org.cablelabs.api.Api";

    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
    }

    public synchronized void initXlet(XletContext xc)
    {
        this.ctx = xc;

        // Initialize AutoXlet framework client and grab logger and test objects
        axc = new AutoXletClient(this, ctx);
        test = axc.getTest();

        // If we have successfully connected, initialize our logger from the
        // AutoXletClient, else use a default constructed XletLogger which will
        // send all logging output to standard out.
        if (axc.isConnected())
        {
            log = axc.getLogger();
        }
        else
        {
            log = new XletLogger();
        }
    }

    public synchronized void startXlet() throws XletStateChangeException
    {
        if (!started)
        {

            try
            {
                //
                // the xait needs to be signalled in order for the
                // api to be stored
                //
                // if OCAP.xait.ignore is set to true in the mpeenv.ini
                // file, this xait will be ignored and the test will fail
                //
                XAITGenerator xaitGen = new XAITGenerator();
                AppManagerProxy appMgrProxy = AppManagerProxy.getInstance();
                appMgrProxy.registerUnboundApp(xaitGen.generate());

                RegisteredApiManager ram = RegisteredApiManager.getInstance();

                String version;
                String currVersion = ram.getVersion(APINAME);
                if (currVersion == null) version = "0";
                {
                    try
                    {
                        int number = Integer.parseInt(currVersion);
                        version = "" + (number + 1);
                    }
                    catch (Exception e)
                    {
                        version = "0";
                    }
                }

                registerAPI(ram, version);
                //
                // we need an assertion here so that the test
                // registers a test as being run
                //
                test.assertTrue(true);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                test.fail("Could not register API - " + e.getMessage());
            }

            started = true;
        }

        showResults();
    }

    protected void registerAPI(RegisteredApiManager ram, String version) throws Exception
    {
        ram.register(APINAME, version, new File("test-scdf.xml"), (short) 150);
    }

    private void showResults()
    {
        Color color;
        String text;
        TestResult results = test.getTestResult();
        log.log(results);
        if (results.wasSuccessful())
        {
            // all the tests passed - background color should be green
            color = Color.green.darker();
            text = "All " + results.runCount() + " tests passed";
        }
        else
        {
            // some of the tests failed - background should be red
            color = Color.red.darker();
            StringBuffer sb = new StringBuffer();
            // iterate over the list of failures and draw them on the screen
            for (Enumeration e = results.failures(); e.hasMoreElements();)
            {
                sb.append(((TestFailure) e.nextElement()).toString()).append('\n');
            }
            text = sb.toString();
        }

        HStaticText screenText = new HStaticText();
        screenText.setBackgroundMode(HStaticText.BACKGROUND_FILL);
        screenText.setForeground(new Color(234, 234, 234));
        screenText.setBackground(color);
        screenText.setTextContent(text, HStaticText.ALL_STATES);
        screenText.setFont(new Font("SansSerif", 0, 20));

        HScene scene = HSceneFactory.getInstance().getFullScreenScene(
                HScreen.getDefaultHScreen().getDefaultHGraphicsDevice());
        scene.setLayout(new BorderLayout());
        scene.add(screenText);
        scene.validate();
        scene.show();
    }

    public synchronized void pauseXlet()
    {
        // does nothing
    }

    public synchronized void destroyXlet(boolean forced) throws XletStateChangeException
    {
        if (forced)
        {
            RegisteredApiManager ram = RegisteredApiManager.getInstance();

            try
            {
                ram.unregister(APINAME);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                test.fail("Could not unregister API - " + e.getMessage());
                throw new XletStateChangeException(e.getMessage());
            }
        }
    }
}
