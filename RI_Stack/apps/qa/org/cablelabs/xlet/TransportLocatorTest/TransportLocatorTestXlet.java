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

package org.cablelabs.xlet.TransportLocatorTest;

import java.awt.Component;
import java.awt.Graphics;
import java.io.FileInputStream;

import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.davic.mpeg.TransportStream;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.ocap.net.OcapLocator;
import org.ocap.si.ProgramAssociationTableManager;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;
import java.awt.event.KeyEvent;
import org.cablelabs.test.autoxlet.*;

public class TransportLocatorTestXlet extends Component implements Xlet, Driveable, NetworkInterfaceListener,
        ResourceClient, Runnable
{
    public void dispatchEvent(KeyEvent arg0, boolean arg1, int arg2)
    {
    }

    public void log(String s)
    {
        System.out.println(s);

        // Without the following check the same message is printed to console
        // twice, if the AutoXletClient is not connected.
        if (axc.isConnected())
        {
            logger.log(s + "\n");
        }
    }

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        // Set up the AutoXlet mechanism and populate our local Test and
        // Logger references.
        axc = new AutoXletClient(this, ctx);
        logger = axc.getLogger();
        test = axc.getTest();

        logger.log("*************************************************************");
        logger.log("*************************************************************");
        logger.log("****************** Transport Locator Test *******************");
        logger.log("*************************************************************");
        logger.log("*************************************************************");

        /*
         * Create a HAVI scene to handle basic xlet graphics stuff
         */
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setSize(640, 480);

        m_scene.add(this);
        _vbox = new VidTextBox(50, 40, 540, 400, 14, 5000);
        m_scene.add(_vbox);
        m_scene.addKeyListener(_vbox);
        // create a new worker thread
        thread = new Thread(this);

        // assuming the first argument is the config file
        String args[] = (String[]) ctx.getXletProperty(XletContext.ARGS);
        String configFile = args[0];
        System.out.println("config file = " + configFile);
        try
        {
            FileInputStream fis = new FileInputStream(configFile);
            ArgParser parser = new ArgParser(fis);
            for (int ii = 0; ii < MAX_TSINFO_COUNT; ii++)
            {
                int freq = parser.getIntArg(TRANSPORT_FREQUENCY + ii);
                int mod = parser.getIntArg(TRANSPORT_MODULATION + ii);
                int tsid = parser.getIntArg(TRANSPORT_TSID + ii);
                _ts_array[ii] = new TSInfo(freq, mod, tsid);
                _ts_number++;
            }
        }
        catch (Exception e)
        {
            System.out.println("caught exception e = " + e);
        }

        _vbox.write("TransportLocatorTest:");

        for (int ii = 0; ii < _ts_number; ii++)
        {
            _vbox.write("(" + ii + ") Frequency: " + _ts_array[ii].getFreq());
            _vbox.write("     QAM: " + _ts_array[ii].getMod());
            _vbox.write("     TSID to match: " + _ts_array[ii].getTsid());
        }
        _vbox.write(" ");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        // start the worker thread
        thread.start();
        m_scene.show();
        m_scene.requestFocus();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        // release the reserved NetworkInterface
        if (controller != null)
        {
            try
            {
                controller.release();
            }
            catch (NetworkInterfaceException e)
            {

            }
        }

        // stop the worker thread
        thread = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        // release the NetworkInterface
        if (controller != null)
        {
            try
            {
                controller.release();
            }
            catch (NetworkInterfaceException e)
            {

            }
        }

        // stop the worker thread
        thread = null;
    }

    /**
     * main worker thread function.
     */
    public void run()
    {
        log("TransportLocatorTest: Entered run()");

        NetworkInterfaceManager manager = NetworkInterfaceManager.getInstance();
        // get all the NetworkInterfaces on the system
        NetworkInterface interfaces[] = manager.getNetworkInterfaces();

        // locator used for tuning
        OcapLocator locator = null;

        // reserve and tune a NetworkInterface
        controller = new NetworkInterfaceController(this);

        for (int i = 0; i < interfaces.length; i++)
        {
            if (!interfaces[i].isReserved())
            {
                try
                {
                    controller.reserve(interfaces[i], null);
                }
                catch (NetworkInterfaceException e)
                {
                    // interface already reserved?
                    continue;
                }
                // successfully reserved the interface?
                System.out.println("Successfully reserved interface " + i);
                break;
            }
        }

        // check if we have a NetworkInterface reserved
        if (controller.getNetworkInterface() == null)
        {
            System.out.println("NetworkInterface not reserved for us!");
            return;
        }

        // add this app as a listener
        controller.getNetworkInterface().addNetworkInterfaceListener(this);

        for (int ii = 0; ii < _ts_number; ii++)
        {
            int tries = 0;
            tunedOK = false;

            _current_index = ii;
            locator = _ts_array[ii].getLocator();
            System.out.println("Tuning to locator: " + locator.toString());

            // Attempt tuning up to MAX_TUNING_TRIES times, because tuning may
            // fail,
            // if the SI data is not available yet.

            while (!tunedOK && (tries++ < TransportLocatorTestXlet.MAX_TUNING_TRIES))
            {
                synchronized (tuningSync)
                {
                    System.out.println("Tuning attempt: (" + tries + ")\n");

                    // tune the interface
                    try
                    {
                        controller.tune(locator);
                    }
                    catch (NetworkInterfaceException e)
                    {
                        if (tries == TransportLocatorTestXlet.MAX_TUNING_TRIES)
                        {
                            String msg = "Exception during tune: " + e.getMessage();
                            test.assertTrue(msg, false);
                            log(msg);
                        }
                    }

                    // wait for NetworkInterfaceTuningOverEvent
                    try
                    {
                        tuningSync.wait(1000);
                    }
                    catch (InterruptedException e)
                    {
                        String msg = "Caught exception during tuning wait: " + e.getMessage();
                        test.assertTrue(msg, false);
                        log(msg);
                    }
                }

                // wait for SI before attempting to tune again
                if (!tunedOK)
                {
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (Exception e2)
                    {
                    }
                }
            }

            if (!tunedOK)
            {
                s_log = "Test " + ii + ": TUNING FAILED\n";

                if (BAD_TSID == _ts_array[ii].getTsid())
                {
                    s_log = s_log + "Failed to tune to freq=" + _ts_array[ii].getFreq() + ", qam="
                            + _ts_array[ii].getMod() + " : \nTUNE FAILED - INCONCLUSIVE TEST";
                }
                else
                {
                    s_log = s_log + "Did not receive NetworkInterfaceTuningOverEvent when trying to tune to freq="
                            + _ts_array[ii].getFreq() + ", qam=" + _ts_array[ii].getMod()
                            + " : \nTUNE FAILED - INCONCLUSIVE TEST";
                }

                test.assertTrue(s_log, false);
                log(DIVIDER + s_log + DIVIDER);
                _vbox.write(s_log);
                _vbox.write(" ");
                continue;
            }

            ProgramAssociationTableManager patManager = ProgramAssociationTableManager.getInstance();
            PatRequestor requestor = new PatRequestor();

            // ///////////////////////////////////////////////////////////////////////
            // For right now, the ProgramAssociationTableManager can not accept
            // PAT request using a OcapLocator of the form
            // ocap://f=frequency.m=modulation.
            // The specification states that only 2 locator forms are valid and
            // this
            // is not one of them. However, it does seem acceptable that an
            // application
            // could request a PAT with a Frequency/Modulation form. Until the
            // specification changes, we will just need to sleep here instead of
            // requesting the PAT.
            // ///////////////////////////////////////////////////////////////////////
            TransportStream ts = null;
            int tsid = 0xFFFF;
            int attempt = 0;

            do
            {
                attempt++;

                // try to get the currently tuned transport-stream from the
                // interface
                ts = controller.getNetworkInterface().getCurrentTransportStream();
                if (ts != null)
                {
                    // get the transport-stream id
                    tsid = ts.getTransportStreamId();
                }

                // sleep for a bit to wait for the transport-stream id to become
                // valid
                // if its not yet valid
                if (tsid == 0xFFFF)
                {
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }
            while (tsid == 0xFFFF && attempt < 50);

            // get the transportstream id
            if (ts != null)
            {
                tsid = ts.getTransportStreamId();
                s_log = "Test " + ii + ":\nAcquired TSID for loc " + locator.toString() + "  : " + tsid + "\n"
                        + "Configured TSID for loc " + locator.toString() + ": " + _ts_array[ii].getTsid();
                log(DIVIDER + s_log + "\n");
                _vbox.write(s_log);

                if (tsid != _ts_array[ii].getTsid())
                {
                    if (BAD_TSID == _ts_array[ii].getTsid())
                    {
                        s_log = "TUNING FAILED for freq=" + _ts_array[ii].getFreq() + ", qam=" + _ts_array[ii].getMod()
                                + " : INCONCLUSIVE";
                        log(s_log + DIVIDER);
                        _vbox.write(s_log);
                        test.assertTrue(s_log, false);
                    }
                    else
                    {
                        s_log = "TSIDS DO NOT MATCH for freq=" + _ts_array[ii].getFreq() + ", qam="
                                + _ts_array[ii].getMod() + " : FAIL";
                        log(s_log + DIVIDER);
                        _vbox.write(s_log);
                        test.assertTrue(s_log, false);
                    }
                }
                else
                {
                    s_log = "TSIDS MATCH for freq=" + _ts_array[ii].getFreq() + ", qam=" + _ts_array[ii].getMod()
                            + " : PASS";
                    log(s_log + DIVIDER);
                    _vbox.write(s_log);
                    _ts_counter++;
                }
                _vbox.write(" ");
            }
            else
            {
                // error condition. The current transportstream is null.
                String msg = "The currently tuned transportStream is null";
                test.assertTrue(msg, false);
                log(msg);
            }
            m_scene.show();
        }

        // release the NetworkInterface
        try
        {
            controller.release();
        }
        catch (NetworkInterfaceException e)
        {
            // notify we caught an exception. For now write to the console.
            System.out.println("Caught exception during release e = " + e);
            log(e.toString());
        }

        if (_ts_counter == _ts_number)
        {
            log("*** Passed the test ***");
            test.assertTrue(true); // Passed the test.
        }
        if (!axc.isConnected())
        {
            logger.log(test.getTestResult());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.net.tuning.NetworkInterfaceListener#receiveNIEvent(org.davic
     * .net.tuning.NetworkInterfaceEvent)
     */
    public void receiveNIEvent(NetworkInterfaceEvent evt)
    {
        System.out.println("Received a NetworkInterfaceEvent");

        if (evt instanceof NetworkInterfaceTuningOverEvent)
        {
            System.out.println("Received NetworkInterfaceTuningOverEvent");

            if (((NetworkInterfaceTuningOverEvent) evt).getStatus() == NetworkInterfaceTuningOverEvent.FAILED)
            {
                System.out.println("\nReceived NetworkInterfaceTuningOverEvent.FAILED status");
                _ts_array[_current_index].setTsid(BAD_TSID);

                synchronized (tuningSync)
                {
                    tuningSync.notify();
                }
            }
            else
            {
                System.out.println("Received NetworkInterfaceTuningOverEvent.SUCCEEDED status");
                tunedOK = true;

                synchronized (tuningSync)
                {
                    tuningSync.notify();
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#notifyRelease(org.davic.resources.
     * ResourceProxy)
     */
    public void notifyRelease(ResourceProxy proxy)
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#release(org.davic.resources.ResourceProxy
     * )
     */
    public void release(ResourceProxy proxy)
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#requestRelease(org.davic.resources
     * .ResourceProxy, java.lang.Object)
     */
    public boolean requestRelease(ResourceProxy proxy, Object requestData)
    {
        if (controller != null && proxy == controller)
        {
            try
            {
                controller.release();
            }
            catch (NetworkInterfaceException e)
            {
                // eat this event
            }
        }
        return true;
    }

    public void paint(Graphics g)
    {
        m_scene.show();
    }

    class PatRequestor implements SIRequestor
    {
        public synchronized void notifyFailure(SIRequestFailureType reason)
        {
            done = true;
            success = false;
            this.reason = reason;
            notifyAll();
        }

        public synchronized void notifySuccess(SIRetrievable[] result)
        {
            done = true;
            success = true;
            notifyAll();
        }

        public synchronized void reset()
        {
            done = false;
            success = false;

        }

        boolean done = false;

        boolean success = false;

        SIRequestFailureType reason;
    }

    /**
     * object for notification when a tune completes.
     */
    Object tuningSync = new Object();

    /**
     * NIC used to reserve and tune a <code>NetworkInterface</code>
     */
    NetworkInterfaceController controller;

    /**
     * worker thread needed at this time so we don't block in initXlet or
     * startXlet
     */
    Thread thread = null;

    /**
     * array of ts info.
     */
    TSInfo _ts_array[] = new TSInfo[30];

    int _ts_number = 0;

    int _current_index = 0;

    VidTextBox _vbox;

    HScene m_scene;

    private static String TRANSPORT_FREQUENCY = "tsid_freq_";

    private static String TRANSPORT_MODULATION = "tsid_qam_";

    private static String TRANSPORT_TSID = "tsid_id_";

    private static int MAX_TSINFO_COUNT = 30;

    private static int BAD_TSID = 0xFFFFFFFF;

    private static final String DIVIDER = "\n------------------------------------------------------------------------\n";

    private static int MAX_TUNING_TRIES = 10;

    private String s_log = "";

    private AutoXletClient axc = null;

    private Logger logger = null;

    private Test test = null;

    private int _ts_counter = 0;

    private boolean tunedOK = false;
}
