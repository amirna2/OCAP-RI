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

package org.cablelabs.xlet.SectionFilteringTest;

import java.io.*;
import java.util.*;

import javax.tv.xlet.*;
import javax.tv.service.*;
import javax.tv.service.selection.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.dvb.dsmcc.*;
import org.ocap.net.*;
import org.havi.ui.*;

import org.davic.net.Locator;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.davic.mpeg.TuningException;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.sections.*;

import org.ocap.si.Descriptor;
import org.ocap.si.PMTElementaryStreamInfo;
import org.ocap.si.PATProgram;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.ProgramMapTableManager;
import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramAssociationTableManager;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.*;

public class SectionFilteringTestXlet implements Xlet, AsynchronousLoadingEventListener, ServiceContextListener,
        SectionFilterListener, ResourceClient, Driveable, NetworkInterfaceListener
{
    private SectionFilterGroup filterGroup = null;

    private SimpleSectionFilter simpleFilter = null;

    private int i_source_id = 0;

    private int i_pid = 0;

    private boolean b_filter_by_tag = false;

    private int i_tag_id = 0;

    private int i_configured_tag = 0;

    private boolean b_section_received = false;

    private static final String SOURCE_ID = "source_id";

    private static final String PID = "pid";

    private static final String FILTER_BY_TAG = "filter_by_tag";

    private static final String TAG_ID = "tag_id";

    private static final String TAG = "tag";

    private static final String DIVIDER = "\n***********************************************************************\n";

    private HScene scene = null;

    private VidTextBox vbox = null;

    private static int vbox_size = 50000;

    private static int max_vbox_chars = vbox_size - 5000;

    private OcapLocator ocLocator = null;

    private static NetworkInterfaceController nic = null;

    private NetworkInterface ni = null;

    private ProgramMapTable pmt = null;

    private boolean tunedOK = false;

    private Object tuningSync = new Object();

    // AutoXlet
    private AutoXletClient axc = null;

    private Logger logger = null;

    private Test test = null;

    public void dispatchEvent(KeyEvent arg0, boolean arg1, int arg2)
    {
    }

    public void log(String s)
    {
        System.out.println(DIVIDER + s + DIVIDER);
        if (axc.isConnected())
        {
            logger.log(s + "\n");
        }
    }

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println(DIVIDER + "Entered initXlet()\n" + DIVIDER);

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
        logger.log("*********************** Section Filtering Test ************************");
        logger.log("***********************************************************************");
        logger.log("***********************************************************************");

        getHostappParams(ctx);

        try
        {
            ocLocator = new OcapLocator(i_source_id);
        }
        catch (Exception e)
        {
            String msg = "Failed to instantiate locator : " + e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
            throw new XletStateChangeException(e.getClass().getName() + " : " + e.getMessage());
        }

        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        scene.setSize(640, 480);
        vbox = new VidTextBox(50, 40, 540, 400, 14, vbox_size);
        scene.add(vbox);
        scene.addKeyListener(vbox);

        System.out.println(DIVIDER + "Exited initXlet()\n" + DIVIDER);
    }

    public synchronized void startXlet() throws XletStateChangeException
    {
        final int MAX_TRIES = 10;
        int tries = 0;
        System.out.println(DIVIDER + "Entered startXlet()\n" + DIVIDER);
        printParams();
        scene.show();
        scene.requestFocus();
        vbox.write("Section Filtering Test:");

        try
        {
            // reserve and tune a NetworkInterface
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

            nic.reserveFor(ocLocator, null);

            // check if we have a NetworkInterface reserved
            if (nic.getNetworkInterface() == null)
            {
                System.out.println("NetworkInterface not reserved for us!");
                return;
            }

            ni = nic.getNetworkInterface();

            // add this app as a listener
            nic.getNetworkInterface().addNetworkInterfaceListener(this);

            tries = 0;
            tunedOK = false;

            System.out.println("Tuning to locator: " + ocLocator.toString());
        }
        catch (Exception e)
        {
            String msg = "Exception reserving network interface: " + e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
        }

        // Attempt tuning up to MAX_TUNING_TRIES times, because tuning may fail,
        // if the SI data is not available yet.

        while (!tunedOK && (tries++ < MAX_TRIES))
        {
            synchronized (tuningSync)
            {
                System.out.println("Tuning attempt: (" + tries + ")\n");

                // tune the interface
                try
                {
                    nic.tune(ocLocator);
                }
                catch (NetworkInterfaceException e)
                {
                    if (tries == MAX_TRIES)
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

        if (!tunedOK) return;

        try
        {
            Thread.sleep(2000);
        }
        catch (Exception e3)
        {
        }

        if (b_filter_by_tag)
        {
            getPIDFromTagAndFilter();
        }
        else
        {
            Filter();
        }
    }

    private void getHostappParams(XletContext ctx)
    {
        try
        {
            ArgParser args = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));
            if (args == null)
            {
                log("No arguments specified.");
                throw new XletStateChangeException("No arguments specified.");
            }

            i_source_id = args.getIntArg(SOURCE_ID);
            i_pid = args.getIntArg(PID);

            String s_filter_by_tag = args.getStringArg(FILTER_BY_TAG);
            if (s_filter_by_tag.compareTo("true") == 0)
            {
                b_filter_by_tag = true;
            }

            i_tag_id = args.getIntArg(TAG_ID);
            i_configured_tag = args.getIntArg(TAG);
        }
        catch (Exception e)
        {
            String msg = "Failed to get hostapp parameters : " + e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
        }
    }

    private void printParams()
    {
        String s_params = "SourceID:       " + i_source_id + "\n";
        s_params = s_params + "PID:            " + i_pid + "\n";
        s_params = s_params + "Filter by tag:  " + b_filter_by_tag + "\n";
        s_params = s_params + "Tag ID:         " + i_tag_id + "\n";
        s_params = s_params + "Configured Tag: " + i_configured_tag + "\n";

        System.out.println(DIVIDER + s_params + DIVIDER);
    }

    private String byteArrayToString(byte[] byteArray)
    {
        String s_byte_array = "";
        for (int i = 0; i < byteArray.length; i++)
        {
            Byte b = new Byte(byteArray[i]);
            s_byte_array = s_byte_array + b.toString();
        }
        return s_byte_array;
    }

    public void pauseXlet()
    {
        System.out.println(DIVIDER + "Entered pauseXlet()\n" + DIVIDER);

        if (scene != null)
        {
            scene.removeKeyListener(vbox);
        }
        if (simpleFilter != null)
        {
            simpleFilter.removeSectionFilterListener(this);
        }
        if (filterGroup != null)
        {
            filterGroup.detach();
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
            String msg = "EXCEPTION in pauseXlet() : " + e.getMessage();
            log(msg);
            e.printStackTrace();
        }

        System.out.println(DIVIDER + "Exited pauseXlet()\n" + DIVIDER);
    }

    public void destroyXlet(boolean x) throws XletStateChangeException
    {
        System.out.println(DIVIDER + "Entered destroyXlet()\n" + DIVIDER);

        if (scene != null)
        {
            scene.removeKeyListener(vbox);
        }
        if (simpleFilter != null)
        {
            simpleFilter.removeSectionFilterListener(this);
        }
        if (filterGroup != null)
        {
            filterGroup.detach();
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
            String msg = "EXCEPTION in destroyXlet() : " + e.getMessage();
            log(msg);
            e.printStackTrace();
        }

        if (!b_section_received)
        {
            vbox.write("FAILED TO GET SECTION DATA.");
            test.fail("Test failed.");
        }

        System.out.println(DIVIDER + "Exited destroyXlet()\n" + DIVIDER);
    }

    public synchronized void receiveEvent(AsynchronousLoadingEvent e)
    {
        this.notify();
    }

    public synchronized void receiveServiceContextEvent(ServiceContextEvent e)
    {
        System.out.println("Service Context Select complete");
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
                tunedOK = true;

                synchronized (tuningSync)
                {
                    tuningSync.notify();
                }
            }
        }
    }

    public String format(String in)
    {
        String out = "";
        String substr = "";

        int line_length = 100;
        int num_of_lines = in.length() / line_length;
        if (num_of_lines > 100)
        {
            num_of_lines = 100;
        }

        int pos = 0;

        for (int i = 0; i < num_of_lines; i++)
        {
            substr = in.substring(pos, pos + line_length - 1);
            out = out + substr + "\n";
            pos = pos + line_length;
        }

        return (out + "...");
    }

    public void writeToVBox(String in)
    {
        String out = "";
        String substr = "";

        int line_length = 50;
        int num_of_lines = in.length() / line_length;
        int pos = 0;

        for (int i = 0; i < num_of_lines; i++)
        {
            substr = in.substring(pos, pos + line_length - 1);
            vbox.write(substr);
            pos = pos + line_length;
        }

        if (pos < in.length() - 1)
        {
            vbox.write(in.substring(pos, in.length() - 1));
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////
    // Section Filtering Methods
    // /////////////////////////////////////////////////////////////////////////////////

    private void getPIDFromTagAndFilter()
    {
        SIRequest pmt_request = ProgramMapTableManager.getInstance().retrieveInBand(new TestRequestor(this)
        {
            public void notifySuccess(SIRetrievable[] result)
            {
                pmt = (ProgramMapTable) (result[0]);
                i_pid = testXlet.getStreamPID(i_tag_id, i_configured_tag);
                System.out.println(DIVIDER + "Acquired PID: " + i_pid + DIVIDER);

                Filter();
            }

            public void notifyFailure(SIRequestFailureType reason)
            {
                pmt = null;
                log("PMT is NULL!");
            }
        }, ocLocator);
    }

    private void Filter()
    {
        try
        {
            filterGroup = new SectionFilterGroup(1); // Reserve one hardware
                                                     // section filter
            simpleFilter = filterGroup.newSimpleSectionFilter();
            simpleFilter.addSectionFilterListener(this);
            System.out.println(DIVIDER + "Created a simple filter\n" + DIVIDER);

            Object data = new Object();
            filterGroup.attach(ni.getCurrentTransportStream(), this, data);
            System.out.println(DIVIDER + "Attached filterGroup to Transport Stream\n" + DIVIDER);

            simpleFilter.startFiltering(null, i_pid);
            System.out.println(DIVIDER + "Started filtering\n" + DIVIDER);
        }
        catch (Exception e)
        {
            String msg = "EXCEPTION in Filter() : " + e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
        }
    }

    public void sectionFilterUpdate(SectionFilterEvent event)
    {
        System.out.println(DIVIDER + "Entered sectionFilterUpdate()\n" + DIVIDER);

        // Get the filter that the event was received from.
        SimpleSectionFilter filter = (SimpleSectionFilter) event.getSource();

        // Check that we've actually got a section.
        if (event instanceof SectionAvailableEvent)
        {
            try
            {
                // Get the sections from the filter.
                Section section = filter.getSection();

                // Get the section data. We could also choose to get some of
                // the section headers if we were more interested in those.
                byte[] sectionData = section.getData();

                String s_section_data = byteArrayToString(sectionData);

                log("SECTION DATA:\n" + format(s_section_data));

                if (s_section_data.length() > max_vbox_chars)
                {
                    s_section_data = s_section_data.substring(0, max_vbox_chars) + "...";
                }

                vbox.write("SECTION DATA:");
                writeToVBox(s_section_data);
                vbox.write("END OF SECTION DATA");

                b_section_received = true;

                // Passed the test.
                test.assertTrue(true);

                section.setEmpty();
            }
            catch (Exception e)
            {
                String msg = "EXCEPTION in sectionFilterUpdate() : " + e.getMessage();
                test.assertTrue(msg, false);
                log(msg);
                e.printStackTrace();
            }
        }
    }

    abstract class TestRequestor implements SIRequestor
    {
        public TestRequestor(SectionFilteringTestXlet testXlet)
        {
            this.testXlet = testXlet;
        }

        protected SectionFilteringTestXlet testXlet;
    }

    public synchronized int getStreamPID(int tag_id, int configured_tag)
    {
        int stream_pid = 0;
        String s_pmt = "";

        if (pmt == null)
        {
            log("PMT is NULL!");
            return stream_pid;
        }

        s_pmt = DIVIDER + "PMT:" + DIVIDER + "Program Number: " + pmt.getProgramNumber() + "\nPCR PID: "
                + pmt.getPcrPID();

        // Elementary Streams
        if (pmt.getPMTElementaryStreamInfoLoop() == null || pmt.getPMTElementaryStreamInfoLoop().length == 0)
        {
            s_pmt = s_pmt + ("\nNo Elementary Streams");
        }
        else
        {
            for (int i = 0; i < pmt.getPMTElementaryStreamInfoLoop().length; ++i)
            {
                PMTElementaryStreamInfo esInfo = pmt.getPMTElementaryStreamInfoLoop()[i];

                s_pmt = s_pmt
                        + ("\nElementary Stream " + i + "\n" + "    Stream Type:    " + esInfo.getStreamType() + "\n"
                                + "    Elementary PID: " + esInfo.getElementaryPID() + "\n" + "    Locator String: " + esInfo.getLocatorString());

                // Stream Descriptors
                if (esInfo.getDescriptorLoop() == null || esInfo.getDescriptorLoop().length == 0)
                {
                    s_pmt = s_pmt + ("\n    No descriptors");
                }
                else
                {
                    for (int j = 0; j < esInfo.getDescriptorLoop().length; ++j)
                    {
                        Descriptor desc = esInfo.getDescriptorLoop()[j];

                        s_pmt = s_pmt
                                + ("\n    Descriptor " + j + "\n" + "        Tag: " + desc.getTag() + "\n"
                                        + "        Content Length: " + desc.getContentLength());

                        String descContent = "";
                        for (int k = 0; k < desc.getContent().length; ++k)
                        {
                            int data = desc.getContent()[k] & 0xFF;
                            descContent += Integer.toHexString(data) + " ";
                        }
                        s_pmt = s_pmt + ("\n        " + descContent);

                        if ((desc.getTag() == tag_id) && (desc.getContent().length >= 2))
                        {
                            int acquired_tag = (desc.getContent()[0] & 0xFF) + (desc.getContent()[1] & 0xFF);
                            if (acquired_tag == configured_tag)
                            {
                                stream_pid = esInfo.getElementaryPID();
                            }
                        }
                    }
                }
            }
        }
        System.out.println(s_pmt);
        return stream_pid;
    }

    // /////////////////////////////////////////////////////////////////////////////////
    // ResourceClient Methods
    // /////////////////////////////////////////////////////////////////////////////////

    /**
     * This method gets called when the resource manager requests that we give
     * up a resource. We can refuse to do so, and that's what we do in this case
     * (even though we shouldn't).
     */
    public boolean requestRelease(ResourceProxy proxy, Object requestData)
    {
        return false;
    }

    /**
     * This method gets called when the resource manager informs us that we must
     * release a resource.
     */
    public void release(ResourceProxy proxy)
    {
    }

    /**
     * This method gets called when the resource manager tells us we've lost
     * access to a resource, and we should clean up after ourselves.
     */
    public void notifyRelease(ResourceProxy proxy)
    {
        release(proxy);
    }

    // /////////////////////////////////////////////////////////////////////////////////
}
