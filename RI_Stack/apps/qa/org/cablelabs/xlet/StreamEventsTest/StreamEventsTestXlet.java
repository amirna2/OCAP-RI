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

package org.cablelabs.xlet.StreamEventsTest;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.io.*;
import java.util.Vector;
import java.util.Enumeration;
import java.lang.Integer;

import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.*;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import javax.media.*;

import org.havi.ui.*;
import org.havi.ui.event.HRcEvent;
import org.ocap.net.OcapLocator;
import org.davic.net.tuning.*;
import org.davic.resources.*;
import org.dvb.dsmcc.*;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.*;

public class StreamEventsTestXlet extends Component implements Xlet, Driveable, KeyListener, ServiceContextListener,
        NetworkInterfaceListener, ResourceClient, StreamEventListener, NPTListener
{
    private VidTextBox vbox = null;

    private String s_log = "";

    private HScene scene = null;

    private ServiceContext service_context = null;

    private SIManager si_manager = null;

    private ServiceDomain sd = null;

    private static final int MAX_CONTENT = 1024;

    private static final String FILE_SYSTEM = "file_system";

    private static final String FREQUENCY = "frequency";

    private static final String PROG_NUM = "prog_num";

    private static final String QAM = "qam";

    private static final String CAROUSEL_ID = "carousel_id";

    private static final String IBOC = "IBOC";

    private static final String OOBOC = "OOBOC";

    private static final String CONFIG_FILE = "config_file";

    private static final String EVENT_PATH = "event_path_";

    private static final String EVENT_NAME = "event_name_";

    private static final String EVENT_ID = "event_id_";

    private String config_file_name = "";

    private Object tuning_finished = null; // tuning synchronization object

    private int index = 0;

    private String[] oc_contents = null;

    private OcapLocator ocap_loc = null;

    private int frequency = 0;

    private int prog_num = 0;

    private int qam = 0;

    private int carousel_id = 0;

    private String file_system = new String(IBOC);

    private boolean started = false;

    private boolean oob = false;

    final int MAX_TRIES = 10;

    private int tries = 0;

    private String carousel_mount = null; // carousel mount point

    private Vector event_files = new Vector();

    private Vector event_records = new Vector();

    private Vector event_objects = new Vector();

    private static final String DIVIDER = "\n***********************************************************************************\n";

    public void dispatchEvent(KeyEvent arg0, boolean arg1, int arg2)
    {
    }

    // /////////////////////////////////////////////////////////////////////////////
    // XLET METHODS //
    // /////////////////////////////////////////////////////////////////////////////

    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {
        index = 0;
        tuning_finished = new Object();

        // Set up the display.
        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        scene.setSize(640, 480);
        scene.add(this);
        vbox = new VidTextBox(50, 40, 540, 400, 14, 5000);
        scene.add(vbox);
        this.setVisible(true);
        scene.addKeyListener(this);
        scene.addKeyListener(vbox);

        try
        {
            // Get hostapp parameters.
            ArgParser args = new ArgParser((String[]) xletContext.getXletProperty(XletContext.ARGS));

            config_file_name = args.getStringArg(CONFIG_FILE);
            file_system = args.getStringArg(FILE_SYSTEM);
            if ((file_system.compareTo(OOBOC)) == 0)
            {
                oob = true;
            }

            frequency = args.getIntArg(FREQUENCY);
            prog_num = args.getIntArg(PROG_NUM);
            qam = args.getIntArg(QAM);
            carousel_id = args.getIntArg(CAROUSEL_ID);

            // Get config parameters.
            FileInputStream fis = new FileInputStream(config_file_name);
            ArgParser parser = new ArgParser(fis);
            for (int i = 0;; i++)
            {
                String path = parser.getStringArg(EVENT_PATH + i);
                String name = parser.getStringArg(EVENT_NAME + i);
                int id = parser.getIntArg(EVENT_ID + i);
                event_records.addElement(new EventInfo(path, name, id, false));
            }
        }
        catch (Exception e)
        {
            System.out.println("Caught exception e = " + e);
        }

        // Print hostapp and config parameters to the console.
        System.out.println(DIVIDER + "[STREAM_EVENTS_XLET]");
        System.out.println("  Config file:     " + config_file_name);
        System.out.println("  File system:     " + file_system);
        System.out.println("  Frequency:       " + frequency);
        System.out.println("  Program number:  " + prog_num);
        System.out.println("  QAM:             " + qam);
        System.out.println("  Carousel ID:     " + carousel_id);

        for (int i = 0; i < event_records.size(); i++)
        {
            System.out.println("  Event Path " + i + ":    " + ((EventInfo) (event_records.elementAt(i))).getPath());
            System.out.println("  Event Name " + i + ":    " + ((EventInfo) (event_records.elementAt(i))).getName());
            System.out.println("  Event Id " + i + ":      " + ((EventInfo) (event_records.elementAt(i))).getId());
        }

        System.out.println(DIVIDER);
        oc_contents = new String[MAX_CONTENT];
        vbox.write("Stream Events Test Xlet:\n");
    }

    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        scene.show();
        scene.requestFocus();

        // Tune to a carousel and read all the files on it.
        if (started != true)
        {
            startReading();
        }
    }

    public void pauseXlet()
    {
        System.out.println(DIVIDER + "Entered pauseXlet()" + DIVIDER);

        UnsubscribeAll();

        if (scene != null)
        {
            scene.setVisible(false);
        }
        if (service_context != null)
        {
            service_context.removeListener(this);
        }

        if (service_context != null)
        {
            service_context.removeListener(this);
            service_context.destroy();
        }
        si_manager = null;

        try
        {
            if (sd != null && sd.isAttached())
            {
                sd.detach();
            }
        }
        catch (Exception e)
        {
        }

        try
        {
            stopReading();
        }
        catch (Exception e)
        {
        };
    }

    public void destroyXlet(boolean param) throws XletStateChangeException
    {
        System.out.println(DIVIDER + "Entered destroyXlet()" + DIVIDER);

        UnsubscribeAll();

        if (scene != null)
        {
            scene.remove(this);
            scene.setVisible(false);
            HSceneFactory.getInstance().dispose(scene);
            scene = null;
        }

        if (service_context != null)
        {
            service_context.removeListener(this);
            service_context.destroy();
        }
        si_manager = null;

        try
        {
            if (sd != null && sd.isAttached())
            {
                sd.detach();
            }
        }
        catch (Exception e)
        {
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // STREAM EVENTS TEST METHODS //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * Impementation of ServiceContextListener interface notification of
     * ServiceContext event. Start reading inband carousel files, after tuning
     * is finished. This method gets called after tuning to a channel.
     */
    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        DSMCCObject dsmccObj = null;
        ServiceContentHandler[] schArray;

        if (event == null)
        {
            return;
        }

        try
        {
            ServiceContext sc = event.getServiceContext();
            if (sc == null)
            {
                return;
            }
            Service serv = sc.getService();

            if (event instanceof NormalContentEvent)
            {
                // Tune succeeded. Attach to a carousel and read the files.
                dsmccObj = carouselAttach(carousel_id);
                carousel_mount = dsmccObj.getAbsolutePath();
                scene.show();
                scene.requestFocus();
                viewOC(dsmccObj);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        testDSMCCStreamObject();

        testStreamEvents();
    }

    // Test the DSMCCStream object
    private void testDSMCCStreamObject()
    {
        String stream_path = "";

        // The path to the first DSMCCStreamEvent object in the 'event_records'
        // vector
        // is passed into the DSMCCStream object constructor. The
        // DSMCCStreamEvent class
        // is a subclass of DSMCCStream.
        if (event_records != null && event_records.size() > 0 && event_records.elementAt(0) != null)
        {
            stream_path = carousel_mount + ((EventInfo) (event_records.elementAt(0))).getPath();
        }

        try
        {
            DSMCCStream dsmccStream = new DSMCCStream(stream_path);
            System.out.println(DIVIDER + "[STREAM_EVENTS_XLET] DSMCCStream Object Data: \n" + "Stream Duration: "
                    + dsmccStream.getDuration() + "Current NPT:     " + dsmccStream.getNPT() + "NPT Rate:        "
                    + dsmccStream.getNPTRate().getNumerator() + " / " + dsmccStream.getNPTRate().getDenominator()
                    + "Stream Locator:  " + dsmccStream.getStreamLocator() + "Audio Stream:    "
                    + dsmccStream.isAudio() + "Data Stream:     " + dsmccStream.isData() + "Video Stream:    "
                    + dsmccStream.isVideo() + DIVIDER);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void testStreamEvents()
    {
        // Print a list of event files available on the carousel.
        if (event_files.size() <= 0)
        {
            System.out.println(DIVIDER + "[STREAM_EVENTS_XLET] No event files were found on carousel " + carousel_mount
                    + DIVIDER);
            vbox.write("No event files were found on carousel " + carousel_mount);
        }
        else
        {
            System.out.println(DIVIDER + "[STREAM_EVENTS_XLET] Event Files on Carousel:");
            vbox.write("EVENT FILES ON CAROUSEL:");

            for (int i = 0; i < event_files.size(); i++)
            {
                System.out.println("  " + event_files.elementAt(i));
                vbox.write("  " + (String) (event_files.elementAt(i)));
            }

            vbox.write(" ");
            System.out.println(DIVIDER);
        }

        // Subscribe to stream events listed in the config.properties file.
        for (int j = 0; j < event_records.size(); j++)
        {
            String event_name = ((EventInfo) (event_records.elementAt(j))).getName();
            String event_path = ((EventInfo) (event_records.elementAt(j))).getPath();

            try
            {
                event_objects.addElement(new DSMCCStreamEvent(carousel_mount, event_path));
                ((DSMCCStreamEvent) (event_objects.elementAt(j))).subscribe(event_name, this);

                ((EventInfo) (event_records.elementAt(j))).setSubscribed(true);
                String s_msg = "Subscribed to Stream Event: " + event_path + "/" + event_name;
                System.out.println(DIVIDER + "[STREAM_EVENTS_XLET] " + s_msg + DIVIDER);
                vbox.write(s_msg);
            }
            catch (Exception e)
            {
                System.out.println("[STREAM_EVENTS_XLET] Exception subscribing to stream event: " + event_path + "/"
                        + event_name);
                e.printStackTrace();
            }
        }

        // Print an event list for all the DSMCCStreamEvents objects we
        // subscribed to.
        Vector event_vector = new Vector();

        for (int k = 0; k < event_objects.size(); k++)
        {
            String[] event_list = ((DSMCCStreamEvent) (event_objects.elementAt(k))).getEventList();
            uniqueMerge(event_list, event_vector);
        }

        System.out.println(DIVIDER + "[STREAM_EVENTS_XLET] \nList of All Events in the DSMCCStreamEvent Objects:");
        vbox.write("\nLIST OF ALL EVENTS IN THE DSMCCStreamEvent OBJECTS:");
        for (int i = 0; i < event_vector.size(); i++)
        {
            System.out.println(event_vector.elementAt(i));
            vbox.write("  " + event_vector.elementAt(i));
        }
        System.out.println(DIVIDER);
    }

    // Merge String Array into Vector, leaving out duplicate Strings.
    private void uniqueMerge(String[] in, Vector out)
    {
        for (int i = 0; i < in.length; i++)
        {
            if (!out.contains(in[i]))
            {
                out.addElement(in[i]);
            }
        }
    }

    // Handle incoming stream events.
    public void receiveStreamEvent(StreamEvent se)
    {
        String name = se.getEventName();
        Integer Id = new Integer(se.getEventId());

        String s_msg = "~~~~~ RECEIVED STREAM EVENT ~~~~~" + "\n  'StreamEvent' Object Data: " + "\n  Name: " + name
                + "\n  NPT:  " + se.getEventNPT() + "\n  ID:   " + Id.toString() + "\n  Data: "
                + new String(se.getEventData());

        System.out.println(DIVIDER + "[STREAM_EVENTS_XLET] " + s_msg + "\n");
        vbox.write("\n" + s_msg);

        try
        {
            // Test method StreamEvent.getSource().
            DSMCCStreamEvent source = (DSMCCStreamEvent) se.getSource();

            // Test method DSMCCStreamEvent.getEventList().
            String event_names[] = source.getEventList();
            boolean matched_name = false;
            for (int i = 0; i < event_names.length; i++)
            {
                if (event_names[i].equals(name))
                {
                    matched_name = true;
                    break;
                }
            }

            if (!matched_name)
            {
                System.out.println("  \nERROR: StreamEvent.getSource().getEventList() failed.\n");
            }

            // Print the Event List from the DSMCCStreamEvent source object.
            System.out.println("\n  'DSMCCStreamEvent' Source Object Event List:\n");
            vbox.write("\n'DSMCCStreamEvent' Source Object Event List:");

            for (int i = 0; i < event_names.length; i++)
            {
                System.out.println("  " + event_names[i]);
                vbox.write("  " + event_names[i]);
            }
            System.out.println(DIVIDER);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // Test subscribing/unsubscribing to events listed in the config.properties
    // file.
    // The first event in config.properties corresponds to key press 1, etc.
    // Pressing the same key multiple times results in subscribing/unsubscribing
    // to/from the corresponding event.
    // Test case: unsubscribe from an event, then send it out from the
    // TSBroadcaster.
    public void keyPressed(KeyEvent e)
    {
        if (e == null) return;

        synchronized (e)
        {
            int key = e.getKeyCode();

            switch (key)
            {
                case HRcEvent.VK_1:
                    ProcessEvent(0);
                    break;

                case HRcEvent.VK_2:
                    ProcessEvent(1);
                    break;

                case HRcEvent.VK_3:
                    ProcessEvent(2);
                    break;

                case HRcEvent.VK_4:
                    ProcessEvent(3);
                    break;

                case HRcEvent.VK_5:
                    ProcessEvent(4);
                    break;

                case HRcEvent.VK_6:
                    ProcessEvent(5);
                    break;

                case HRcEvent.VK_7:
                    ProcessEvent(6);
                    break;

                case HRcEvent.VK_8:
                    ProcessEvent(7);
                    break;

                case HRcEvent.VK_9:
                    ProcessEvent(8);
                    break;

                case HRcEvent.VK_0:
                    ProcessEvent(9);
                    break;

                default:
                    break;
            }
        }
    }

    // Wrapper for method 'SubscribeUnsubscribe'.
    // This method maps HRcEvents (key presses) to event numbers.
    private void ProcessEvent(int event_index)
    {
        if (event_index >= event_records.size() || event_objects.elementAt(event_index) == null)
        {
            System.out.println(DIVIDER + "The xlet is not configured to recognize this event." + DIVIDER);
            return;
        }
        int event_id = ((EventInfo) (event_records.elementAt(event_index))).getId();
        boolean subscribed = ((EventInfo) (event_records.elementAt(event_index))).isSubscribed();
        String event_name = ((EventInfo) (event_records.elementAt(event_index))).getName();

        SubscribeUnsubscribe((DSMCCStreamEvent) (event_objects.elementAt(event_index)), event_id, subscribed,
                event_name);
        ((EventInfo) (event_records.elementAt(event_index))).setSubscribed((subscribed) ? false : true);
    }

    // Subscribe/unsubscribe to/from an event, depending on the value of event's
    // 'subscribed' parameter.
    private void SubscribeUnsubscribe(DSMCCStreamEvent event, int event_id, boolean subscribed, String event_name)
    {
        try
        {
            if (event != null && subscribed)
            {
                event.unsubscribe(event_id, this);
                String s_msg = "Unsubscribed from Stream Event: " + event_name;
                System.out.println(DIVIDER + "[STREAM_EVENTS_XLET] \n" + s_msg + DIVIDER);
                vbox.write("\n" + s_msg);
            }
            else if (event != null && !subscribed)
            {
                event.subscribe(event_name, this);
                String s_msg = "Subscribed to Stream Event: " + event_name;
                System.out.println(DIVIDER + "[STREAM_EVENTS_XLET] \n" + s_msg + DIVIDER);
                vbox.write("\n" + s_msg);
            }
        }
        catch (Exception e)
        {
            System.out.println("[STREAM_EVENTS_XLET] Exception unsubscribing from a stream event.");
            e.printStackTrace();
        }
    }

    // Unsubscribe from all events.
    private void UnsubscribeAll()
    {
        try
        {
            // Unsubscribe from stream events.
            for (int j = 0; j < event_objects.size(); j++)
            {
                String event_name = ((EventInfo) (event_records.elementAt(j))).getName();
                String event_path = ((EventInfo) (event_records.elementAt(j))).getPath();
                int event_id = ((EventInfo) (event_records.elementAt(j))).getId();

                try
                {
                    ((DSMCCStreamEvent) (event_objects.elementAt(j))).unsubscribe(event_id, this);
                    ((EventInfo) (event_records.elementAt(j))).setSubscribed(false);
                    String s_msg = "Unsubscribed from Stream Event: " + event_path + "/" + event_name;
                    System.out.println(DIVIDER + "[STREAM_EVENTS_XLET] \n" + s_msg + DIVIDER);
                    vbox.write("\n" + s_msg);
                }
                catch (Exception e)
                {
                    System.out.println("[STREAM_EVENTS_XLET] Exception unsubscribing from stream event: " + event_path
                            + "/" + event_name);
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("[STREAM_EVENTS_XLET] Exception unsubscribing from a stream event.");
            e.printStackTrace();
        }
    }

    // Handle NPTStatusEvents
    public void receiveNPTStatusEvent(NPTStatusEvent e)
    {
        Object stream = e.getSource();

        String s_msg = "Received NPT Status Event:" + "\n  Stream whose NPT status changed: " + stream.toString();

        System.out.println(DIVIDER + "[STREAM_EVENTS_XLET] \n" + s_msg + DIVIDER);

        vbox.write("\n" + s_msg);
    }

    // Handle NPTRateChangeEvents
    public void receiveRateChangedEvent(NPTRateChangeEvent e)
    {
        Object stream = e.getSource();
        NPTRate rate = e.getRate();

        String s_msg = "Received NPT Status Event:" + "\n  Stream whose NPT rate changed: " + stream.toString()
                + "\n  New rate: " + rate.getNumerator() + " / " + rate.getDenominator();

        System.out.println(DIVIDER + "[STREAM_EVENTS_XLET] \n" + s_msg + DIVIDER);

        vbox.write("\n" + s_msg);
    }

    // /////////////////////////////////////////////////////////////////////////////
    // OBJECT CAROUSEL VIEWING METHODS //
    // /////////////////////////////////////////////////////////////////////////////

    // Print out all the files stored on a carousel.
    public void viewOC(DSMCCObject parentDir)
    {
        String[] dirContents;
        String absPath;
        DSMCCObject dirItem;

        if (started == false) return;

        if (parentDir == null)
        {
            s_log = DIVIDER + "[STREAM_EVENTS_XLET] Invalid carouselID: " + carousel_id + DIVIDER;
            System.out.println(s_log);
            return;
        }

        if (parentDir.isDirectory() == false)
        {
            return;
        }
        dirContents = parentDir.list();
        if (dirContents == null)
        {
            return;
        }

        for (int ii = 0; ii < dirContents.length; ii++)
        {
            if (started == false) return;

            absPath = parentDir.getAbsolutePath();
            if (absPath == null)
            {
                absPath = "";
            }

            dirItem = new DSMCCObject(absPath + File.separator + dirContents[ii]);

            if (dirItem.isDirectory())
            {
                System.out.println("[STREAM_EVENTS_XLET] Entering Directory: " + parentDir.getAbsolutePath()
                        + File.separator + dirContents[ii]);

                byte[] buff = new byte[(int) dirItem.length()];

                try
                {
                    FileInputStream myFileIS = new FileInputStream(dirItem);
                    myFileIS.read(buff);
                    myFileIS.close();
                }
                catch (FileNotFoundException e)
                {
                }
                catch (SecurityException e)
                {
                }
                catch (IOException e)
                {
                }

                viewOC(dirItem);
            }
            else
            {
                byte[] buff = new byte[(int) dirItem.length()];

                try
                {
                    FileInputStream myFileIS = new FileInputStream(dirItem);
                    myFileIS.read(buff);
                    String file_path_name = new String(absPath + File.separator + dirContents[ii]);
                    System.out.println("[STREAM_EVENTS_XLET] Reading File: " + file_path_name);
                    myFileIS.close();

                    // vbox.write(file_path_name);

                    // Add event files to the 'event_files' vector.
                    for (int i = 0; i < event_records.size(); i++)
                    {
                        if (file_path_name.indexOf(((EventInfo) (event_records.elementAt(i))).getPath()) >= 0)
                        {
                            event_files.addElement(file_path_name);
                            break;
                        }
                    }
                }
                catch (FileNotFoundException e)
                {
                }
                catch (SecurityException e)
                {
                }
                catch (IOException e)
                {
                }

                oc_contents[index] = parentDir.getPath() + File.separator + dirContents[ii] + buff.length + " bytes ";
                index++;
            }
        }
    }

    // Start directory reading.
    public void startReading()
    {
        if (started != true && !oob)
        {
            started = true;
            setupIB(frequency, prog_num, qam);
        }
        else if (started != true && oob)
        {
            started = true;
            setupOOB(prog_num);
        }
    }

    // Attach to an OOB carousel and read all the files on it.
    private void setupOOB(int prog_num)
    {
        try
        {
            ocap_loc = new OcapLocator(-1, prog_num, -1);
            DSMCCObject dsmccObj = carouselAttach(carousel_id);

            scene.show();
            scene.requestFocus();

            viewOC(dsmccObj);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // Attach to a carousel.
    private DSMCCObject carouselAttach(int carouselID)
    {
        DSMCCObject dsmccObj = null;
        DSMCCObject mount_point = null;

        try
        {
            sd = new ServiceDomain();
            sd.attach(ocap_loc, carouselID);

            mount_point = sd.getMountPoint();
            dsmccObj = new DSMCCObject(mount_point, "/");

            if (dsmccObj == null)
            {
                return null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return dsmccObj;
    }

    // Tune inband.
    private void setupIB(int frequency, int prog_num, int qam)
    {
        Service service;
        boolean tune_failed = true;

        // Attempt tuning up to MAX_TRIES times, because tuning may fail,
        // if the SI data is not available yet.
        for (tries = 0; tries < MAX_TRIES && tune_failed; tries++)
        {
            System.out.println("Tuning attempt: (" + (tries + 1) + ")\n");

            try
            {
                service_context = ServiceContextFactory.getInstance().createServiceContext();
                si_manager = (SIManager) SIManager.createInstance();
                service_context.addListener(this);

                ocap_loc = new OcapLocator(frequency, prog_num, qam);
                service = si_manager.getService(ocap_loc);
                service_context.select(service);
                System.out.println("\nTune succeeded.\n");
                tune_failed = false;
            }
            catch (Exception e)
            {
                tune_failed = true;

                if (tries == MAX_TRIES - 1)
                {
                    System.out.println("\nCaught exception tuning:\n");
                    e.printStackTrace();
                }
                else
                {
                    try
                    {
                        Thread.sleep(2000);
                    }
                    catch (Exception e1)
                    {
                    }
                }
            }
        }
    }

    // Reset the TV screen.
    public void resetDisplay()
    {
        for (int ii = 0; ii < MAX_CONTENT; ii++)
        {
            oc_contents[ii] = new String("");
        }
        index = 0;
    }

    // Stop directory reading.
    public void stopReading() throws XletStateChangeException
    {
        if (started == true)
        {
            try
            {
                throw new XletStateChangeException("Pausing the xlet...");
            }
            catch (Exception e)
            {
            }
            started = false;
            resetDisplay();
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // EVENT HANDLING //
    // /////////////////////////////////////////////////////////////////////////////
    /**
     * This method inherits from org.davic.net.tuning.NetworkInterfaceLIstener,
     * and gets called when the tuning API generates an event for the
     * NetworkInterface object, which we have registered as a listener.
     */
    public void receiveNIEvent(NetworkInterfaceEvent event)
    {
    }

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
        synchronized (tuning_finished)
        {
            tuning_finished.notify();
        }
    }

    /**
     * This method gets called when the resource manager tells us we've lost
     * access to a resource, and we should clean up after ourselves.
     */
    public void notifyRelease(ResourceProxy proxy)
    {
        release(proxy);
    }

    public void keyTyped(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
    }

    // Helper class for storing event data.
    private class EventInfo
    {
        private String _path = "";

        private String _name = "";

        private int _id = 0;

        private boolean _subscribed = false;

        public EventInfo(String path, String name, int id, boolean subscribed)
        {
            _path = new String(path);
            _name = new String(name);
            _id = id;
            _subscribed = subscribed;
        }

        public String getPath()
        {
            return _path;
        }

        public String getName()
        {
            return _name;
        }

        public int getId()
        {
            return _id;
        }

        public boolean isSubscribed()
        {
            return _subscribed;
        }

        public void setPath(String path)
        {
            _path = new String(path);
        }

        public void setName(String name)
        {
            _name = new String(name);
        }

        public void setId(int id)
        {
            _id = id;
        }

        public void setSubscribed(boolean subscribed)
        {
            _subscribed = subscribed;
        }
    }
}
