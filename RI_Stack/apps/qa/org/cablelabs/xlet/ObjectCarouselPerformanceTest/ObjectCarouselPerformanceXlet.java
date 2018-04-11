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

package org.cablelabs.xlet.ObjectCarouselPerformanceTest;

import java.awt.*;
import java.io.*;
import java.util.*;

import org.havi.ui.*;
import javax.tv.xlet.*;
import javax.tv.service.*;
import javax.tv.service.selection.*;

import org.dvb.dsmcc.*;
import org.ocap.*;
import org.ocap.net.*;
import org.cablelabs.lib.utils.ArgParser;

import org.ocap.application.*;
import org.dvb.application.*;

import org.cablelabs.lib.utils.VidTextBox;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import org.havi.ui.event.HRcEvent;

import org.davic.net.Locator;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.cablelabs.test.autoxlet.*;

public class ObjectCarouselPerformanceXlet extends Component implements Xlet, KeyListener,
        AsynchronousLoadingEventListener, ServiceContextListener, Driveable, NetworkInterfaceListener
{
    private HScene scene = null;

    private ServiceContext sc = null;

    private String s_config_file_name = "";

    private int i_source_id = 0;

    private int i_carousel_id = 0;

    private boolean b_oobOC = false;

    private boolean b_use_nsap = false;

    private byte[] ba_nsap_addr = null;

    private boolean b_read_concurrently = false;

    private boolean b_prefetch = false;

    private boolean b_synch_load = false;

    private boolean b_asynch_load = false;

    private int i_block_size = 0;

    private int i_total_iter = 0;

    public String s_prefix = "";

    private ServiceDomain sd = null;

    private static final String DIVIDER = "***********************************************************************\n";

    private boolean b_tunedOK = false;

    private Object tuningSync = new Object();

    private String a_files[] = null;

    private OcapLocator oc_locator = null;

    private Vector v_results_console = new Vector();

    private int i_num_of_files = 0;

    private FileReader a_read_threads[] = null;

    private VidTextBox vbox = null;

    private NetworkInterfaceController nic = null;

    private Object iteration_complete_monitor = null;

    private int i_iteration_complete_counter = 0;

    private AutoXletClient axc = null;

    private Logger logger = null;

    private Test test = null;

    private String s_log = "";

    private boolean b_test_passed = true;

    private boolean b_tune_failed = true;

    public void dispatchEvent(KeyEvent arg0, boolean arg1, int arg2)
    {
    }

    public void log(String s)
    {
        System.out.println(s);
        if (axc.isConnected())
        {
            logger.log(s + "\n");
        }
    }

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        // Set up the AutoXlet mechanism and populate local Test and Logger
        // references.
        axc = new AutoXletClient(this, ctx);
        test = axc.getTest();
        if (axc.isConnected())
        {
            logger = axc.getLogger();
        }
        else
        {
            logger = new XletLogger();
        }

        logger.log("***********************************************************************");
        logger.log("***********************************************************************");
        logger.log("****************** Object Carousel Performance Test *******************");
        logger.log("***********************************************************************");
        logger.log("***********************************************************************");

        getHostappParams(ctx);
        getConfigParams();

        if (b_read_concurrently)
        {
            a_read_threads = new FileReader[i_num_of_files];
        }
        else
        {
            a_read_threads = new FileReader[1];
        }

        iteration_complete_monitor = new Object();

        try
        {
            // Instantiate locator.
            if (b_use_nsap)
            {
                ba_nsap_addr = toNsap(i_source_id, i_carousel_id, b_oobOC);
                oc_locator = new OcapLocator(i_source_id);
            }
            else if (b_oobOC && !b_use_nsap)
            {
                oc_locator = new OcapLocator(-1, i_source_id, -1);
            }
            else
            {
                oc_locator = new OcapLocator(i_source_id);
            }
        }
        catch (Exception e)
        {
            String msg = "Failed to instantiate locator : " + e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
            throw new XletStateChangeException(e.getClass().getName() + " : " + e.getMessage());
        }

        printParams();

        try
        {
            // Instantiate this as an app manager.
            AppManagerProxy amp = AppManagerProxy.getInstance();
            amp.setAppFilter(new MyAppFilter());
        }
        catch (Exception e)
        {
            String msg = "Failed to set app filter: " + e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
        }

        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());

        scene.setSize(640, 480);
        scene.add(this);
        vbox = new VidTextBox(50, 40, 540, 400, 14, 10000);
        scene.add(vbox);
        this.setVisible(true);
        scene.addKeyListener(this);
        scene.addKeyListener(vbox);
    }

    public synchronized void startXlet() throws XletStateChangeException
    {
        long l_iterationStartTime = 0;
        long l_iterationEndTime = 0;
        int i_start_iter = 0;
        int i_curr_iter = 0;

        sd = new ServiceDomain();

        // Inband NSAP
        if (b_use_nsap && !b_oobOC)
        {
            tuneByNetworkInterface();
        }

        // Inband OCAPLocator
        else if (!b_use_nsap && !b_oobOC)
        {
            tuneByServiceContext();
        }

        this.setVisible(true);
        scene.show();
        scene.requestFocus();

        if (b_oobOC)
        {
            vbox.write("Object Carousel Performance OOB Test:");
        }
        else
        {
            vbox.write("Object Carousel Performance Inband Test:");
        }

        // Read OC files
        try
        {
            for (i_curr_iter = i_start_iter; i_curr_iter < i_total_iter; i_curr_iter++)
            {
                i_iteration_complete_counter = 0;
                v_results_console.removeAllElements();
                l_iterationStartTime = System.currentTimeMillis();

                s_prefix = mountCarousel();

                if (b_read_concurrently)
                {
                    // Start file-reading threads, one thread per file,
                    // to read files concurrently.
                    for (int i = 0; i < i_num_of_files; i++)
                    {
                        a_read_threads[i] = new FileReader(a_files[i], i_curr_iter);
                    }
                }
                else
                {
                    // Read files sequentially.
                    a_read_threads[0] = new FileReader(a_files, i_curr_iter);
                }

                // Wait for all the threads from the current iteration to finish
                // running.
                synchronized (iteration_complete_monitor)
                {
                    try
                    {
                        iteration_complete_monitor.wait();
                    }
                    catch (InterruptedException e)
                    {
                    }
                }

                l_iterationEndTime = System.currentTimeMillis();
                v_results_console.addElement("ITERATION TOTAL: " + (i_curr_iter + 1) + ", Start Time: "
                        + l_iterationStartTime + ", End Time: " + l_iterationEndTime + ", Total Time: "
                        + (l_iterationEndTime - l_iterationStartTime));

                vbox.write("TOTAL ITERATION TIME: " + (l_iterationEndTime - l_iterationStartTime));

                System.out.println(DIVIDER + "Detaching\n" + DIVIDER);
                sd.detach();

                i_start_iter = i_curr_iter;
                log_perf_data(i_curr_iter);
            } // END OF ITERATION.
        }
        catch (Exception e)
        {
            String msg = "Exception in startXlet() : " + e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
        }

        // Clean up the service context.
        if (!b_use_nsap && !b_oobOC)
        {
            sc.destroy();
        }

        // Test passed.
        if (b_test_passed)
        {
            test.assertTrue(true);
        }
        // Test failed.
        else
        {
            test.assertTrue(false);
        }
    }

    public void pauseXlet()
    {
        for (int i = 0; a_read_threads != null && i < a_read_threads.length && a_read_threads[i] != null; i++)
        {
            a_read_threads[i].interrupt();
        }

        try
        {
            if (nic != null)
            {
                nic.release();
            }
        }
        catch (Exception e)
        {
        }

        scene.setVisible(false);
        this.setVisible(false);
    }

    public void destroyXlet(boolean x) throws XletStateChangeException
    {
        for (int i = 0; a_read_threads != null && i < a_read_threads.length && a_read_threads[i] != null; i++)
        {
            a_read_threads[i].interrupt();
        }

        try
        {
            if (nic != null)
            {
                nic.release();
            }
        }
        catch (Exception e)
        {
        }

        if (scene != null)
        {
            scene.remove(this);
            scene.setVisible(false);
            HSceneFactory.getInstance().dispose(scene);
            scene = null;
        }
    }

    public void tuneByNetworkInterface()
    {
        final int MAX_TRIES = 20;
        int i_tries = 0;

        try
        {
            // Reserve and tune a NetworkInterface
            nic = new NetworkInterfaceController(new ResourceClient()
            {
                public boolean requestRelease(ResourceProxy proxy, Object requestData)
                {
                    return false;
                }

                public void release(ResourceProxy proxy)
                {
                }

                public void notifyRelease(ResourceProxy proxy)
                {
                    throw new RuntimeException("Lost the tuner");
                }
            });

            nic.reserveFor(oc_locator, null);

            // Check if we have a NetworkInterface reserved
            if (nic.getNetworkInterface() == null)
            {
                System.out.println("NetworkInterface not reserved for us!");
                return;
            }

            // Add this app as a listener
            nic.getNetworkInterface().addNetworkInterfaceListener(this);

            i_tries = 0;
            b_tunedOK = false;

            System.out.println("Tuning to locator: " + oc_locator.toString());
        }
        catch (Exception e)
        {
            String msg = "Exception reserving network interface: " + e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
        }

        // Attempt tuning up to MAX_TUNING_TRIES times, because tuning may fail,
        // if the SI data is not available yet.

        while (!b_tunedOK && (i_tries++ < MAX_TRIES))
        {
            synchronized (tuningSync)
            {
                System.out.println("Tuning attempt: (" + i_tries + ")\n");

                // Tune the interface
                try
                {
                    nic.tune(oc_locator);
                }
                catch (NetworkInterfaceException e)
                {
                    if (i_tries == MAX_TRIES)
                    {
                        String msg = "Exception during tune: " + e.getMessage();
                        test.assertTrue(msg, false);
                        log(msg);
                    }
                }

                // Wait for NetworkInterfaceTuningOverEvent
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

            // Wait for SI before attempting to tune again
            if (!b_tunedOK)
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
    }

    public void tuneByServiceContext()
    {
        SIManager siManager = (SIManager) SIManager.createInstance();
        Service service = null;
        final int MAX_TRIES = 20;
        int i_tries = 0;

        // Attempt tuning up to MAX_TRIES times, because tuning may fail,
        // if the SI data is not available yet.
        for (i_tries = 0; i_tries < MAX_TRIES && b_tune_failed; i_tries++)
        {
            System.out.println("Tuning attempt: (" + (i_tries + 1) + ")\n");

            try
            {
                sc = ServiceContextFactory.getInstance().createServiceContext();
                sc.addListener(this);
                service = siManager.getService(oc_locator);
                sc.select(service);
                System.out.println("\nTune succeeded.\n");
                b_tune_failed = false;
            }
            catch (Exception e1)
            {
                b_tune_failed = true;

                if (i_tries == MAX_TRIES - 1)
                {
                    String msg = "Failed to tune : " + e1.getMessage();
                    test.assertTrue(msg, false);
                    log(msg);
                    e1.printStackTrace();
                }
                else
                {
                    try
                    {
                        Thread.sleep(2000);
                    }
                    catch (Exception e2)
                    {
                    }
                }
            }
        }

        try
        {
            this.wait(30 * 1000);
        }
        catch (Exception e3)
        {
        }
        sc.removeListener(this);
    }

    public String mountCarousel()
    {
        long l_mountStartTime = 0;
        long l_mountEndTime = 0;
        final int MAX_TRIES = 20;
        int i_tries = 0;

        boolean mount_failed = true;
        System.out.println(DIVIDER + "Mounting carousel: " + i_carousel_id + " : " + i_source_id + "\n" + DIVIDER);
        l_mountStartTime = System.currentTimeMillis();

        for (i_tries = 0; i_tries < MAX_TRIES && mount_failed; i_tries++)
        {
            System.out.println("Carousel mount attempt: (" + (i_tries + 1) + ")\n");

            try
            {
                if (b_use_nsap)
                {
                    sd.attach(ba_nsap_addr);
                }
                else
                {
                    sd.attach(oc_locator, i_carousel_id);
                }
                mount_failed = false;
            }
            catch (Exception e1)
            {
                mount_failed = true;

                if (i_tries == MAX_TRIES - 1)
                {
                    String msg = "Failed to mount carousel : " + e1.getMessage();
                    test.assertTrue(msg, false);
                    log(msg);
                    e1.printStackTrace();
                }
                else
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
        }

        l_mountEndTime = System.currentTimeMillis();
        v_results_console.addElement("Carousel Mount Time: " + (l_mountEndTime - l_mountStartTime));

        return sd.getMountPoint().getPath();
    }

    // Get xlet parameters from hostapp file.
    private void getHostappParams(XletContext ctx)
    {
        final String CONFIG_FILE = "config_file";
        final String FILE_SYSTEM = "file_system";
        final String SOURCE_ID = "source_id";
        final String CAROUSEL_ID = "carousel_id";
        final String USE_NSAP = "use_nsap_address";
        final String READ_CONCURRENTLY = "read_concurrently";
        final String PREFETCH = "prefetch";
        final String SYNCH_LOAD = "synch_load";
        final String ASYNCH_LOAD = "asynch_load";
        final String BLOCK_SIZE = "block_size";
        final String ITERATIONS = "iterations";
        final String OOBOC = "OOBOC";

        String s_file_system = "InbandOC";
        String s_use_nsap = "false";
        String s_read_concurrently = "false";
        String s_prefetch = "false";
        String s_synch_load = "false";
        String s_asynch_load = "false";

        try
        {
            // Get parameter values from hostapp.properties
            ArgParser args = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));
            if (args == null)
            {
                log("No arguments specified.");
                throw new XletStateChangeException("No arguments specified.");
            }

            s_config_file_name = args.getStringArg(CONFIG_FILE);

            s_file_system = args.getStringArg(FILE_SYSTEM);
            if (s_file_system.compareTo(OOBOC) == 0)
            {
                b_oobOC = true;
            }

            i_source_id = args.getIntArg(SOURCE_ID);
            i_carousel_id = args.getIntArg(CAROUSEL_ID);

            s_use_nsap = args.getStringArg(USE_NSAP);
            if (s_use_nsap.compareTo("true") == 0)
            {
                b_use_nsap = true;
            }

            s_read_concurrently = args.getStringArg(READ_CONCURRENTLY);
            if (s_read_concurrently.compareTo("true") == 0)
            {
                b_read_concurrently = true;
            }

            s_prefetch = args.getStringArg(PREFETCH);
            if (s_prefetch.compareTo("true") == 0)
            {
                b_prefetch = true;
            }

            s_synch_load = args.getStringArg(SYNCH_LOAD);
            if (s_synch_load.compareTo("true") == 0)
            {
                b_synch_load = true;
            }

            s_asynch_load = args.getStringArg(ASYNCH_LOAD);
            if (s_asynch_load.compareTo("true") == 0)
            {
                b_asynch_load = true;
            }

            if (b_synch_load && b_asynch_load)
            {
                s_log = "CONFIGURATION ERROR: parameters " + SYNCH_LOAD + " and " + ASYNCH_LOAD
                        + " cannot be both true.";
                log(s_log);
                throw new XletStateChangeException(s_log);
            }

            i_block_size = args.getIntArg(BLOCK_SIZE);

            i_total_iter = args.getIntArg(ITERATIONS);
            if (i_total_iter < 1)
            {
                String msg = "Iterations must be greater than 0.";
                test.assertTrue(msg, false);
                throw new Exception(msg);
            }
        }
        catch (Exception e)
        {
            String msg = "Failed to get hostapp parameters : " + e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
        }
    }

    // Get xlet parameters from config file.
    private void getConfigParams()
    {
        final String OC_FILE_ARG = "oc_perf_inb_";
        final String OOBOC_FILE_ARG = "oc_perf_oob_";
        FileInputStream fis_read = null;

        try
        {
            fis_read = new FileInputStream(s_config_file_name);
            ArgParser config_args = new ArgParser(fis_read);

            // Get names of files to read from config file.

            String strOCType = OC_FILE_ARG;
            if (b_oobOC)
            {
                strOCType = OOBOC_FILE_ARG;
            }

            i_num_of_files = countFiles(strOCType);

            if (i_num_of_files == 0)
            {
                String msg = "No files specified.";
                test.assertTrue(msg, false);
                log(msg);
                throw new Exception(msg);
            }

            a_files = new String[i_num_of_files];

            for (int i = 0; i < i_num_of_files; i++)
            {
                String str_source_id = config_args.getStringArg(strOCType + i);
                a_files[i] = str_source_id;
            }

            fis_read.close();
        }
        catch (Exception e)
        {
            String msg = "Failed to get config parameters : " + e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
        }
    }

    // Print object carousel parameters to console.
    private void printParams()
    {
        String s_params = "FS:                ";
        if (b_oobOC)
        {
            s_params += "OOB ";
        }
        s_params = s_params + "OC\n";

        if (b_oobOC)
        {
            s_params = s_params + "Program Number:    " + i_source_id + "\n";
        }
        else
        {
            s_params = s_params + "SourceID:          " + i_source_id + "\n";
        }

        s_params = s_params + "CarouselID:        " + i_carousel_id + "\n";

        if (b_use_nsap)
        {
            s_params = s_params + "Using NSAP:        " + byteArrayToString(ba_nsap_addr) + "\n";
        }
        else
        {
            s_params = s_params + "Locator:           " + oc_locator.toString() + "\n";
        }

        s_params = s_params + "Concurrent:        ";
        if (b_read_concurrently)
        {
            s_params = s_params + "True\n";
        }
        else
        {
            s_params = s_params + "False\n";
        }

        s_params = s_params + "Prefetch:          ";
        if (b_prefetch)
        {
            s_params = s_params + "True\n";
        }
        else
        {
            s_params = s_params + "False\n";
        }

        s_params = s_params + "Synchronous Load:  ";
        if (b_synch_load)
        {
            s_params = s_params + "True\n";
        }
        else
        {
            s_params = s_params + "False\n";
        }

        s_params = s_params + "Asynch Dir Load:   ";
        if (b_asynch_load)
        {
            s_params = s_params + "True\n";
        }
        else
        {
            s_params = s_params + "False\n";
        }

        s_params = s_params + "Blocksize:         " + i_block_size + "\n";

        s_params = s_params + "Iterations:        " + i_total_iter + "\n";

        s_log = DIVIDER + s_params + DIVIDER;

        log(s_log);
    }

    // Count the number of files to read from the carousel, listed in config
    // file.
    private int countFiles(String strOCType)
    {
        int i_file_num = -1;
        try
        {
            FileInputStream fis_count = new FileInputStream(s_config_file_name);
            byte[] buffer = new byte[fis_count.available() + 1];
            fis_count.read(buffer);
            String str_config_file = new String(buffer);

            while (str_config_file.indexOf(strOCType + ++i_file_num) != -1);

            fis_count.close();
        }
        catch (Exception e)
        {
            String msg = "Failed to count files to read in config.properties : " + e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
        }
        return i_file_num;
    }

    private byte[] toNsap(int sourceId, int carouselId, boolean bOOB) throws Exception
    {
        byte[] nsap = { 0x00, // AFI
                0x00, // Type
                0x00, 0x00, 0x00, 0x00, // carouselId
                0x01, // specifierType
                0x00, 0x10, 0x00, // specifierData
                0x00, 0x00, // transport_stream_id
                0x00, 0x00, // original_network_id
                0x00, 0x00, // service_id
                (byte) 0x7F, -1, -1, -1 // reserved
        };
        nsap[2] = (byte) ((carouselId >> 24) & 0xFF);
        nsap[3] = (byte) ((carouselId >> 16) & 0xFF);
        nsap[4] = (byte) ((carouselId >> 8) & 0xFF);
        nsap[5] = (byte) (carouselId & 0xFF);
        nsap[14] = (byte) ((sourceId >> 8) & 0xFF);
        nsap[15] = (byte) (sourceId & 0xFF);
        if (bOOB)
        {
            nsap[16] = (byte) -1;
        };

        return nsap;
    }

    private String byteArrayToString(byte[] byteArray)
    {
        String s_byte_array = "";
        for (int i = 0; i < byteArray.length; i++)
        {
            String s_hex = Integer.toHexString(byteArray[i]);
            if (s_hex.length() == 8)
            {
                s_byte_array = s_byte_array + s_hex.substring(6) + " ";
            }
            else
            {
                s_byte_array = s_byte_array + s_hex + " ";
            };
        }
        return s_byte_array;
    }

    // Log object carousel performance data to console.
    public synchronized void log_perf_data(int iteration)
    {
        // try { this.wait(3 * 1000); } catch (Exception e) {}

        Enumeration en = v_results_console.elements();

        s_log = "\n" + DIVIDER + "Performance Comparison Data for Iteration " + (iteration + 1) + "\n" + DIVIDER;

        while (en.hasMoreElements())
        {
            s_log = s_log + (String) en.nextElement() + "\n";
        }
        s_log = s_log + "\n";

        log(s_log);
    }

    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();
        if (key == HRcEvent.VK_LEFT || key == HRcEvent.VK_RIGHT || key == HRcEvent.VK_UP || key == HRcEvent.VK_DOWN)
        {
        }
    }

    public void keyTyped(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
        int key = e.getKeyCode();
        if (key == HRcEvent.VK_LEFT || key == HRcEvent.VK_RIGHT || key == HRcEvent.VK_UP || key == HRcEvent.VK_DOWN)
        {
        }
    }

    public synchronized void receiveEvent(AsynchronousLoadingEvent e)
    {
        this.notify();
    }

    public void receiveNIEvent(NetworkInterfaceEvent evt)
    {
        System.out.println("Received a NetworkInterfaceEvent");

        if (evt instanceof NetworkInterfaceTuningOverEvent)
        {
            System.out.println("Received NetworkInterfaceTuningOverEvent");

            if (((NetworkInterfaceTuningOverEvent) evt).getStatus() == NetworkInterfaceTuningOverEvent.FAILED)
            {
                System.out.println("\nReceived NetworkInterfaceTuningOverEvent.FAILED status");

                synchronized (tuningSync)
                {
                    tuningSync.notify();
                }
            }
            else
            {
                System.out.println("Received NetworkInterfaceTuningOverEvent.SUCCEEDED status");
                b_tunedOK = true;

                synchronized (tuningSync)
                {
                    tuningSync.notify();
                }
            }
        }
    }

    public synchronized void receiveServiceContextEvent(ServiceContextEvent e)
    {
        System.out.println("Service Context Select complete");
        this.notify();
    }

    private class MyAppFilter extends AppsDatabaseFilter
    {
        public boolean accept(AppID appid)
        {
            return false;
        }
    }

    // ******************************************************************************//
    // ******************************************************************************//
    // **************************** FileReader Class
    // ****************************//
    // ******************************************************************************//
    // ******************************************************************************//

    // Read a file from an object carousel.
    public class FileReader extends Thread implements AsynchronousLoadingEventListener
    {
        private String s_object_names[] = null;

        private String s_object_path_name = new String("");

        private long l_start_time = 0;

        private long l_end_time = 0;

        private int i_iteration = -1;

        public String s_results = new String("");

        private final String FAILED = "FAILED";

        public FileReader(String objectName, int iteration)
        {
            s_object_names = new String[1];
            s_object_names[0] = objectName;
            i_iteration = iteration;
            start(); // Start the thread.
        }

        public FileReader(String objectNames[], int iteration)
        {
            s_object_names = new String[objectNames.length];
            for (int i = 0; i < objectNames.length; i++)
            {
                s_object_names[i] = objectNames[i];
            }

            i_iteration = iteration;
            start(); // Start the thread.
        }

        public void run()
        {
            DSMCCObject dsmcc_object = null;

            for (int i = 0; i < s_object_names.length; i++)
            {
                try
                {
                    s_object_path_name = s_prefix + "/" + s_object_names[i];
                    dsmcc_object = new DSMCCObject(s_object_path_name);

                    if (!dsmcc_object.exists())
                    {
                        v_results_console.addElement(s_object_path_name + ", READ " + FAILED);
                        s_results = (i_iteration + 1) + ", " + s_object_names[i] + ", DOES NOT EXIST";
                    }

                    else if (!dsmcc_object.canRead())
                    {
                        v_results_console.addElement((i_iteration + 1) + ", " + s_object_path_name + ", READ " + FAILED);
                        s_results = (i_iteration + 1) + ", " + s_object_names[i] + ", IS NOT READABLE";
                    }

                    else if (dsmcc_object.isFile())
                    {
                        processFile(dsmcc_object, s_object_names[i]);
                    }

                    else if (dsmcc_object.isDirectory())
                    {
                        processDirectory(dsmcc_object, s_object_names[i]);
                    }
                }
                catch (Exception e)
                {
                    String msg = "Exception thrown for file " + s_object_path_name + " : " + e.getMessage();
                    test.assertTrue(msg, false);
                    System.out.println(msg); // not writing to 'logger'
                                             // concurrently.
                    e.printStackTrace();
                }

                synchronized (vbox)
                {
                    vbox.write(s_results);

                    if (s_results.endsWith(FAILED) || s_results.length() == 0)
                    {
                        // Let the automation framework know, that a file-read
                        // failure occurred.
                        test.fail(s_results);
                        b_test_passed = false;
                    }
                }

                ++i_iteration_complete_counter;

                // Current iteration is complete.
                if (i_iteration_complete_counter == i_num_of_files)
                {
                    synchronized (iteration_complete_monitor)
                    {
                        iteration_complete_monitor.notify();
                    }
                }
            }
        }

        public void processFile(DSMCCObject f, String name) throws InvalidFormatException, InterruptedIOException,
                FileNotFoundException, IOException, NotLoadedException
        {
            boolean b_load_file = false;
            int i_bs = 0;
            byte[] a_block = null;
            int i_bytes_read = 0;
            long l_bytes_in_file = 0;

            if (!f.isFile())
            {
                v_results_console.addElement("TEST ERROR: " + s_object_path_name + " IS NOT A FILE");
                return;
            }

            l_start_time = System.currentTimeMillis();

            // NOTE: the total amount of time to prefetch/load and read a file
            // is
            // always the same. Depending on whether the Prefetch and/or Load
            // are ON or OFF, the latency shifts among Prefetch, Load and Read.

            // Prefetch file (Quick. Latency shifts to Read.)
            if (b_prefetch)
            {
                byte _priority = 0x1;
                f.prefetch(s_object_path_name, _priority);
            }

            // Load file (Slow.)
            if (b_synch_load)
            {
                b_load_file = true;
                f.synchronousLoad();
            }

            // Read file.
            FileInputStream fis = new FileInputStream(f);

            if (i_block_size > 0)
            {
                i_bs = i_block_size;
            }
            else
            {
                i_bs = fis.available();
            }
            a_block = new byte[i_bs];

            for (long j = 0;; j += i_bs)
            {
                i_bytes_read = fis.read(a_block);
                if (i_bytes_read == -1)
                {
                    break;
                }
                l_bytes_in_file = l_bytes_in_file + i_bytes_read;
            }

            fis.close();
            l_end_time = System.currentTimeMillis();

            if (l_bytes_in_file > 0)
            {
                v_results_console.addElement(s_object_path_name + ", Bytes read: " + l_bytes_in_file + ", Read Time: "
                        + (l_end_time - l_start_time));

                s_results = s_results + (i_iteration + 1) + ", " + name + ",\n   Bytes read: " + l_bytes_in_file
                        + ", Time: " + (l_end_time - l_start_time);
            }
            else
            {
                v_results_console.addElement(s_object_path_name + ", READ " + FAILED);
                s_results = (i_iteration + 1) + ", " + name + ", READ " + FAILED;
            }

            if (b_load_file && !b_prefetch)
            {
                f.unload();
            }
        }

        public void processDirectory(DSMCCObject dir, String name) throws InvalidFormatException,
                InterruptedIOException, FileNotFoundException, IOException, NotLoadedException
        {
            if (!dir.isDirectory())
            {
                v_results_console.addElement("TEST ERROR: " + s_object_path_name + " IS NOT A DIRECTORY");
                return;
            }

            l_start_time = System.currentTimeMillis();

            // Prefetch directory (quick).
            if (b_prefetch)
            {
                byte _priority = 0x1;
                dir.prefetch(s_object_path_name, _priority);
            }

            // Load directory (slow).
            if (b_synch_load)
            {
                dir.synchronousLoad();

                l_end_time = System.currentTimeMillis();

                listDir(dir, name, new String(", Load/Prefetch Time: " + (l_end_time - l_start_time)));

                if (!b_prefetch)
                {
                    dir.unload();
                }
            }

            else if (b_asynch_load)
            {
                dir.asynchronousLoad(this);
                listDir(dir, name, "");
            }

            else
            {
                listDir(dir, name, "");
            }
        }

        // List directory contents.
        private void listDir(DSMCCObject obj, String name, String loadTime)
        {
            if (obj == null || loadTime == null || !obj.isDirectory())
            {
                return;
            }

            String[] dirContents = obj.list();
            String dirList = "";

            for (int i = 0; i < dirContents.length; i++)
            {
                dirList = dirList + "\n    " + dirContents[i];
            }

            if (dirList.length() > 0)
            {
                v_results_console.addElement(s_object_path_name + loadTime + "\n    DIRECTORY CONTENTS:" + dirList);

                s_results = s_results + (i_iteration + 1) + ", " + name + loadTime + "\n    DIRECTORY CONTENTS:"
                        + dirList;
            }
        }

        public synchronized void receiveEvent(AsynchronousLoadingEvent e)
        {
            System.out.println("\n" + DIVIDER + "Asynchronous Load Complete for " + s_object_path_name + "\n" + DIVIDER);

            // TODO: Output to the screen.
            // TODO: Unload directories loaded asynchronously.

            this.notify();
        }
    }
}
