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
package org.cablelabs.xlet.VideoPresentationTest;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.media.Player;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.AlternativeContentEvent;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.media.BackgroundVideoPresentationControl;
import org.dvb.media.VideoFormatControl;
import org.dvb.media.VideoTransformation;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoConfiguration;
import org.havi.ui.HVideoDevice;
import org.ocap.net.OcapLocator;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;

/**
 * VideoPresentationTestXlet
 * 
 * @author Joshua Keplinger
 * 
 */
public class VideoPresentationTestXlet implements Xlet, Driveable
{

    private XletContext xctx;

    // Objects used to integrate with AutoXlet testing framework
    private AutoXletClient axc;

    private Logger log;

    private Test test;

    private ServiceContext sctx;

    private SCListener listener;

    private BackgroundVideoPresentationControl bvpControl;

    private VideoFormatControl vfControl;

    private Vector testData;

    private int testsRunCount;

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#initXlet(javax.tv.xlet.XletContext)
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        xctx = ctx;

        // Initialize AutoXlet framework client and grab logger and test objects
        axc = new AutoXletClient(this, ctx);
        test = axc.getTest();

        // If we have successfully connected, initialize our logger from the
        // AutoXletClient, else use a default constructed XletLogger which will
        // send all logging output to standard out.
        if (axc.isConnected())
            log = axc.getLogger();
        else
            log = new XletLogger();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        try
        {
            ArgParser args = new ArgParser((String[]) xctx.getXletProperty(XletContext.ARGS));
            String xml = args.getStringArg("test_config");
            log.log("Loading " + xml);
            testData = TestDataFactory.getTestData(xml);

            sctx = ServiceContextFactory.getInstance().createServiceContext();
            listener = new SCListener();
            sctx.addListener(listener);

            // Run the tests
            runTests();

            // Print the test results
            printResults();

            // Set the video config back to default
            HVideoDevice vidDev = HScreen.getDefaultHScreen().getDefaultHVideoDevice();
            vidDev.setVideoConfiguration(vidDev.getDefaultConfiguration());

            // Shut down the service context
            sctx.removeListener(listener);
            sctx.destroy();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new XletStateChangeException("Cannot continue without test values: " + ex.getMessage());
        }
    }

    private void runTests()
    {
        for (int i = 0; i < testData.size(); i++)
        {
            TestData td = (TestData) testData.elementAt(i);

            if (!selectService(td.sourceId))
            {
                log.log("Service selection failed");
                continue;
            }

            if (!findAndSetVideoConfiguration(td))
            {
                log.log("Unable to set VideoConfiguration");
                continue;
            }

            // if(!selectService(td.sourceId))
            // {
            // log.log("Service selection failed");
            // continue;
            // }

            if (!getControls())
            {
                log.log("Unable to get controls");
                continue;
            }

            if (runTest(td)) testsRunCount++;
        }
    }

    private void printResults()
    {
        log.log("****************************************************");
        log.log("  >>  VideoPresentationTest Results  <<");
        log.log("Number of Tests loaded: " + testData.size());
        log.log("Number of Tests ran: " + testsRunCount);
        log.log("Results of Tests ran...");
        log.log(test.getTestResult());
        log.log("****************************************************");
    }

    private boolean selectService(int sourceId)
    {
        SIManager sim = SIManager.createInstance();
        Service svc = null;
        try
        {
            OcapLocator loc = new OcapLocator(sourceId);
            svc = sim.getService(loc);
        }
        catch (org.davic.net.InvalidLocatorException ex)
        {
            log.log(ex);
            return false;
        }
        catch (javax.tv.locator.InvalidLocatorException ex)
        {
            log.log(ex);
            return false;
        }
        sctx.stop();
        if (!listener.waitForStop())
        {
            log.log("ServiceContext did not stop");
            return false; // Didn't stop, try again with the next service
        }
        log.log("Selecting " + svc);

        sctx.select(svc);
        if (!listener.waitForPresenting())
        {
            log.log("Service selection never completed");
            return false;
        }
        log.log("Service selection succeeded for " + svc);
        return true;
    }

    private boolean runTest(TestData td)
    {
        if (td.afd != vfControl.getActiveFormatDefinition())
        {
            log.log("AFD does not match, skipping configuration test");
            return false;
        }
        if (td.ar != vfControl.getAspectRatio())
        {
            log.log("Aspect ratio does not match, skipping configuration test");
            return false;
        }

        VideoTransformation vt = vfControl.getVideoTransformation(td.dfc);
        if (vt == null)
        {
            log.log("Unable to get VideoTransformation for DFC = " + getDFC(td.dfc));
            return false;
        }

        if (!bvpControl.setVideoTransformation(vt))
        {
            log.log("Unable to set VideoTransformation = " + vt);
            return false;
        }

        testVideoAreas(td);

        return true;
    }

    private boolean getControls()
    {
        ServiceContentHandler[] handlers = sctx.getServiceContentHandlers();
        Player player = null;
        for (int i = 0; i < handlers.length; i++)
        {
            if (handlers[i] instanceof Player) player = (Player) handlers[i];
        }
        if (player == null)
        {
            log.log("Unable to get Player");
            return false;
        }
        bvpControl = (BackgroundVideoPresentationControl) player.getControl(BackgroundVideoPresentationControl.class.getName());
        if (bvpControl == null)
        {
            log.log("Unable to get BackgroundVideoPresentationControl");
            return false;
        }
        vfControl = (VideoFormatControl) player.getControl(VideoFormatControl.class.getName());
        if (vfControl == null)
        {
            log.log("Unable to get VideoFormatControl");
            return false;
        }

        return true;
    }

    private boolean findAndSetVideoConfiguration(TestData td)
    {
        HVideoConfiguration[] vidConfigs = HScreen.getDefaultHScreen().getDefaultHVideoDevice().getConfigurations();
        HVideoConfiguration config = null;
        for (int i = 0; i < vidConfigs.length; i++)
        {
            Dimension sar = vidConfigs[i].getPixelResolution();
            Dimension par = vidConfigs[i].getPixelAspectRatio();
            if (sar.width == td.scrWidth && sar.height == td.scrHeight && par.width == td.parWidth
                    && par.height == td.parHeight)
            {
                config = vidConfigs[i];
                break;
            }
        }
        if (config == null)
        {
            log.log("Unable to find matching VideoConfiguration [parWidth=" + td.parWidth + ",parHeight="
                    + td.parHeight + ",scrWidth=" + td.scrWidth + ",scrHeight=" + td.scrHeight + "]");
            return false;
        }
        try
        {
            HScreen.getDefaultHScreen().getDefaultHVideoDevice().setVideoConfiguration(config);
        }
        catch (SecurityException e)
        {
            log.log("SecurityException while setting HVideoConfiguration " + config);
            log.log(e);
            return false;
        }
        catch (HPermissionDeniedException e)
        {
            log.log("HPermissionDeniedException while setting HVideoConfiguration " + config);
            log.log(e);
            return false;
        }
        catch (HConfigurationException e)
        {
            log.log("HConfigurationException while setting HVideoConfiguration " + config);
            log.log(e);
            return false;
        }

        return true;
    }

    private void testVideoAreas(TestData td)
    {
        // Check active video area
        HScreenRectangle rect = bvpControl.getActiveVideoArea();
        test.assertEquals("X is incorrect for active video area, using " + td, td.ava.x, rect.x, 0.0001f);
        test.assertEquals("Y is incorrect for active video area, using " + td, td.ava.y, rect.y, 0.0001f);
        test.assertEquals("Width is incorrect for active video area, using " + td, td.ava.width, rect.width, 0.0001f);
        test.assertEquals("Height is incorrect for active video area, using " + td, td.ava.height, rect.height, 0.0001f);

        // Check total video area
        rect = bvpControl.getTotalVideoArea();
        test.assertEquals("X is incorrect for total video area, using " + td, td.ava.x, rect.x, 0.0001f);
        test.assertEquals("Y is incorrect for total video area, using " + td, td.ava.y, rect.y, 0.0001f);
        test.assertEquals("Width is incorrect for total video area, using " + td, td.ava.width, rect.width, 0.0001f);
        test.assertEquals("Height is incorrect for total video area, using " + td, td.ava.height, rect.height, 0.0001f);

        // Check active video area onscreen
        rect = bvpControl.getActiveVideoAreaOnScreen();
        test.assertEquals("X is incorrect for active video area onscreen, using " + td, td.ava.x, rect.x, 0.0001f);
        test.assertEquals("Y is incorrect for active video area onscreen, using " + td, td.ava.y, rect.y, 0.0001f);
        test.assertEquals("Width is incorrect for active video area onscreen, using " + td, td.ava.width, rect.width,
                0.0001f);
        test.assertEquals("Height is incorrect for active video area onscreen, using " + td, td.ava.height,
                rect.height, 0.0001f);

        // Check total video area onscreen
        rect = bvpControl.getTotalVideoAreaOnScreen();
        test.assertEquals("X is incorrect for total video area onscreen, using " + td, td.ava.x, rect.x, 0.0001f);
        test.assertEquals("Y is incorrect for total video area onscreen, using " + td, td.ava.y, rect.y, 0.0001f);
        test.assertEquals("Width is incorrect for total video area onscreen, using " + td, td.ava.width, rect.width,
                0.0001f);
        test.assertEquals("Height is incorrect for total video area onscreen, using " + td, td.ava.height, rect.height,
                0.0001f);
    }

    private String getDFC(int dfc)
    {
        Field[] constants = VideoFormatControl.class.getFields();
        for (int i = 0; i < constants.length; i++)
        {
            Field constant = constants[i];
            try
            {
                if (constant.getName().startsWith("DFC") && dfc == constant.getInt(VideoFormatControl.class))
                    return constant.getName();
            }
            catch (IllegalArgumentException e)
            {
                // Bad name, try the next one
            }
            catch (IllegalAccessException e)
            {
                // Visibility problem, try the next one
            }
        }

        return null;
    }

    private class SCListener implements ServiceContextListener
    {

        private boolean presenting;

        public synchronized void receiveServiceContextEvent(ServiceContextEvent e)
        {
            if (e instanceof NormalContentEvent || e instanceof AlternativeContentEvent)
            {
                System.out.println("Received " + e.getClass().getName());
                presenting = true;
                notify();
            }
            else if (e instanceof PresentationTerminatedEvent)
            {
                presenting = false;
                notify();
            }
        }

        public synchronized boolean waitForPresenting()
        {
            try
            {
                if (!presenting) wait(60000);
            }
            catch (InterruptedException ex)
            {
                log.log("Unexpectedly interrupted: " + ex);
            }
            return presenting;
        }

        public synchronized boolean waitForStop()
        {
            try
            {
                if (presenting) wait(5000);
            }
            catch (InterruptedException ex)
            {
                log.log("Unexpectedly interrupted: " + ex);
            }
            return !presenting;
        }

    }

    private class VideoResourceClient implements ResourceClient
    {

        private HVideoDevice vidDev;

        public VideoResourceClient(HVideoDevice vidDev)
        {
            this.vidDev = vidDev;
        }

        public void notifyRelease(ResourceProxy proxy)
        {

        }

        public void release(ResourceProxy proxy)
        {
            vidDev.releaseDevice();
        }

        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            return false;
        }

    }

    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {

    }

}
