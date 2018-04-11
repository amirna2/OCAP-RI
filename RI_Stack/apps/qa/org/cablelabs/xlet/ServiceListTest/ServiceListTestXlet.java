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
package org.cablelabs.xlet.ServiceListTest;

//Import Personal Java packages.
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Vector;

import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceIterator;
import javax.tv.service.navigation.ServiceList;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import org.davic.net.InvalidLocatorException;
import org.havi.ui.HScene;
import org.havi.ui.HStaticText;
import org.havi.ui.HVisible;
import org.ocap.net.OcapLocator;

import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.TestFailure;
import org.cablelabs.test.autoxlet.TestResult;
import org.cablelabs.test.autoxlet.XletLogger;

public class ServiceListTestXlet implements Xlet, Driveable
{
    private HScene scene;

    private boolean started = false;

    private SIManager siManager = null;

    private XletContext ctx;

    private Vector locators;

    private Logger log;

    private AutoXletClient axc;

    private Test test;

    private ServiceList list;

    /*
     * initXlet(XletContext) Required when implementing Xlet
     */
    public void initXlet(XletContext ctx)
    {
        axc = new AutoXletClient(this, ctx);
        test = axc.getTest();
        if (axc.isConnected())
            log = axc.getLogger();
        else
            log = new XletLogger();

        siManager = SIManager.createInstance();
        list = siManager.filterServices(null);

        scene = (HScene) javax.tv.graphics.TVContainer.getRootContainer(ctx);
        scene.setLocation(0, 0);
        scene.setSize(640, 480);

        locators = new Vector();

        this.ctx = ctx;

        log.log("Finished Init!");

    }

    /*
     * ######################################## pauseXlet() Required when
     * implementing Xlet
     */
    public void pauseXlet()
    {
        scene.setVisible(false);
    }

    /*
     * ######################################## startXlet() Required when
     * implementing Xlet
     */
    public void startXlet()
    {
        log.log("Starting Xlet...");
        if (!started)
        {
            loadConfigFile();
            if (axc.isConnected())
            {
                if (locators.size() > 0)
                    doListComparison();
                else
                    dumpServices();
            }
            else
            {
                if (locators.size() > 0)
                {
                    doListComparison();
                    displayServicesTestResults();
                }
                else
                    displayServices();
            }
        }
        if (!axc.isConnected())
        {
            scene.show();
            scene.validate();
        }
    }

    /*
     * ######################################## destoryXlet(boolean) Required
     * when implementing Xlet
     */
    public void destroyXlet(boolean unconditional)
    {
        scene.dispose();
    }

    private void doListComparison()
    {
        if (siManager != null)
        {
            log.log("Starting list comparison...");

            ServiceIterator it = list.createServiceIterator();
            while (it.hasNext())
            {
                Service service = it.nextService();
                OcapLocator locator = (OcapLocator) service.getLocator();
                test.assertTrue("Found an unexpected service: " + service.toString(), locators.contains(locator));
            }

            for (Enumeration e = locators.elements(); e.hasMoreElements();)
            {
                OcapLocator locator = (OcapLocator) e.nextElement();
                try
                {
                    test.assertNotNull("Did not find service with locator: " + locator.toString(),
                            list.findService(locator));
                }
                catch (javax.tv.locator.InvalidLocatorException ex)
                {
                    log.log(ex);
                    test.fail(ex.getMessage());
                }
            }

            log.log("List comparison complete!");
        }
        else
        {
            log.log("No services found");
            test.fail("No services found");
        }
    }

    private void dumpServices()
    {
        log.log("SourceIDs Found");
        ServiceIterator it = list.createServiceIterator();
        while (it.hasNext())
        {
            Service service = it.nextService();
            int sourceid = ((OcapLocator) service.getLocator()).getSourceID();
            log.log(Integer.toHexString(sourceid));
        }
    }

    private void displayServices()
    {
        HStaticText banner = new HStaticText("SourceIDs Found:", 0, 40, 640, 40);
        banner.setBackground(Color.darkGray);
        banner.setForeground(Color.green.darker());
        banner.setBackgroundMode(HVisible.BACKGROUND_FILL);
        scene.add(banner);

        int x = 40;
        int y = 80;
        int width = 80;
        int height = 40;
        int counter = 0;

        ServiceIterator it = list.createServiceIterator();
        while (it.hasNext())
        {
            counter++;
            Service service = it.nextService();
            int sourceid = ((OcapLocator) service.getLocator()).getSourceID();
            HStaticText text = new HStaticText(Integer.toHexString(sourceid), x, y, width, height);
            text.setBackground(Color.darkGray);
            text.setForeground(Color.green.darker());
            text.setBackgroundMode(HVisible.BACKGROUND_FILL);
            scene.add(text);
            y += 45;
            if (counter > 7)
            {
                counter = 0;
                x += 85;
                y = 80;
            }
        }
    }

    private void displayServicesTestResults()
    {

        scene.setLayout(new FlowLayout());
        scene.setFont(new Font("SansSerif", Font.BOLD, 20));
        scene.setBackground(Color.black);

        Color errColor = Color.orange.darker();
        Color okColor = Color.green.darker();

        HStaticText banner = new HStaticText("ServiceList Test Xlet", 0, 40, 640, 35);
        banner.setBackground(Color.darkGray);
        banner.setForeground(Color.green.darker());
        banner.setBackgroundMode(HVisible.BACKGROUND_FILL);
        scene.add(banner);

        TestResult results = test.getTestResult();
        if (results.wasSuccessful())
        {
            HStaticText text = new HStaticText("Passed!", 250, 100, 100, 35);
            text.setBackground(Color.darkGray);
            text.setForeground(okColor);
            text.setBackgroundMode(HVisible.BACKGROUND_FILL);
            scene.add(text);
        }
        else
        {

            Enumeration e = results.failures();
            while (e.hasMoreElements())
            {
                TestFailure failure = (TestFailure) e.nextElement();
                HStaticText text = new HStaticText(failure.toString());
                text.setBackground(Color.darkGray);
                text.setBackground(errColor);
                text.setBackgroundMode(HVisible.BACKGROUND_FILL);
                scene.add(text);
            }
        }

        log.log(test.getTestResult());

        started = true;
    }

    private void loadConfigFile()
    {
        try
        {
            String[] args = (String[]) ctx.getXletProperty(XletContext.ARGS);
            if (args == null)
            {
                log.log("No arguments found");
                return;
            }
            if (args.length == 0)
            {
                log.log("No sourceID file specified, continuing without sourceID list");
                return;
            }
            for (int i = 0; i < args.length; i++)
            {
                log.log(args[i]);
            }

            ArgParser parser = new ArgParser(args);
            String filename = parser.getStringArg("config_file");
            log.log("Attempting to open file: " + filename);

            File configFile = new File(filename);
            if (configFile.exists())
            {
                log.log("Reading from file: " + filename);
                BufferedReader reader = new BufferedReader(new FileReader(configFile));
                String sourceID = reader.readLine();
                while (sourceID != null)
                {
                    try
                    {
                        OcapLocator value = new OcapLocator(Integer.parseInt(sourceID, 16));
                        locators.addElement(value);
                        log.log("Adding locator: " + value.toExternalForm());
                    }
                    catch (InvalidLocatorException e)
                    {
                        log.log(e);
                    }
                    sourceID = reader.readLine();

                }// close while
                log.log("Finished reading from file: " + filename);
                reader.close();
            }// close if
        }// close try
        catch (Exception ex)
        {
            log.log("Unable to load config file, continuing without sourceID list");
            log.log(ex);
        }
    }// close loadConfigFile()

    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
        log.log("Received key event, but ignoring it since this test runs alone");

    }
} // end class
