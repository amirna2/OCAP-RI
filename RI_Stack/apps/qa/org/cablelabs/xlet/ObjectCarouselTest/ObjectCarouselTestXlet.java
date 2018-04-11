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

package org.cablelabs.xlet.ObjectCarouselTest;

import java.awt.*;
import java.io.*;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.*;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;
import java.util.Enumeration;
import org.havi.ui.*;
import org.havi.ui.event.HRcEvent;
import org.ocap.net.OcapLocator;
import org.dvb.dsmcc.*;
import javax.media.*;
import org.davic.net.tuning.*;
import org.davic.resources.*;
import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.*;

public class ObjectCarouselTestXlet extends Component implements Xlet, Driveable, KeyListener, ServiceContextListener,
        NetworkInterfaceListener, ResourceClient
{
    private VidTextBox vbox = null;

    private AutoXletClient axc = null;

    private Logger logger = null;

    private Test test = null;

    private String s_log = "";

    private int file_counter = 0;

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

    private long reading_start = 0;

    private long reading_stop = 0;

    private long read_time = 0;

    final int MAX_TRIES = 10;

    private int tries = 0;

    private static final String DIVIDER = "\n***********************************************************************************\n";

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

    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {
        // Set up the AutoXlet mechanism and populate our local Test and
        // Logger references.
        axc = new AutoXletClient(this, xletContext);
        logger = axc.getLogger();
        test = axc.getTest();

        index = 0;
        tuning_finished = new Object();

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
            vbox.write("ObjectCarousel Test Xlet:");

            ArgParser args = new ArgParser((String[]) xletContext.getXletProperty(XletContext.ARGS));

            file_system = args.getStringArg(FILE_SYSTEM);
            if ((file_system.compareTo(OOBOC)) == 0)
            {
                oob = true;
            }

            frequency = args.getIntArg(FREQUENCY);
            prog_num = args.getIntArg(PROG_NUM);
            qam = args.getIntArg(QAM);
            carousel_id = args.getIntArg(CAROUSEL_ID);
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
        }

        System.out.println(DIVIDER + "ObjectCarouselXlet - file_system = " + file_system);
        System.out.println("ObjectCarouselXlet - frequency = " + frequency);
        System.out.println("ObjectCarouselXlet - prog_num = " + prog_num);
        System.out.println("ObjectCarouselXlet - qam = " + qam);
        System.out.println("ObjectCarouselXlet - carousel_id = " + carousel_id + DIVIDER);

        oc_contents = new String[MAX_CONTENT];
    }

    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        scene.show();
        scene.requestFocus();

        logger.log("*************************************************************");
        logger.log("*************************************************************");
        logger.log("******************* Object Carousel Test ********************");
        logger.log("*************************************************************");
        logger.log("*************************************************************");

        if (started != true)
        {
            startReading();
        }

        if (!axc.isConnected())
        {
            logger.log(test.getTestResult());
        }
    }

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

    private void setupOOB(int prog_num)
    {
        // TODO: add wait loop for OOB tuning, after VWB starts supporting OOB.
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
            String msg = e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
        }
    }

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
                    String msg = e.getMessage();
                    test.assertTrue(msg, false);
                    log(msg);
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

    /**
     * Impementation of ServiceContextListener interface notification of
     * ServiceContext event. Start reading inband carousel files, after tuning
     * is finished.
     */
    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
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
                DSMCCObject dsmccObj = carouselAttach(carousel_id);
                scene.show();
                scene.requestFocus();
                viewOC(dsmccObj);
            }
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            test.assertTrue(msg, false);
            log(msg);
            e.printStackTrace();
        }
    }

    private DSMCCObject carouselAttach(int carouselID)
    {
        DSMCCObject dsmccObj = null;
        DSMCCObject mount_point = null;
        boolean mount_failed = true;

        for (tries = 0; tries < MAX_TRIES && mount_failed; tries++)
        {
            System.out.println("Carousel mount attempt: (" + (tries + 1) + ")\n");

            try
            {
                sd = new ServiceDomain();
                sd.attach(ocap_loc, carouselID);
                mount_failed = false;
            }
            catch (Exception e1)
            {
                mount_failed = true;

                if (tries == MAX_TRIES - 1)
                {
                    String msg = "Failed to mount carousel: " + e1.getMessage();
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

        mount_point = sd.getMountPoint();
        dsmccObj = new DSMCCObject(mount_point, "/");

        if (dsmccObj == null)
        {
            return null;
        }
        return dsmccObj;
    }

    public void viewOC(DSMCCObject parentDir)
    {
        String[] dirContents;
        String absPath;
        DSMCCObject dirItem;

        if (started == false) return;

        if (parentDir == null)
        {
            s_log = DIVIDER + "OCXLET: invalid carouselID: " + carousel_id + DIVIDER;
            log(s_log);
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
                System.out.println(DIVIDER + "OCXLET: Entering Directory: " + parentDir.getAbsolutePath()
                        + File.separator + dirContents[ii] + DIVIDER);

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
                    reading_start = System.currentTimeMillis();

                    FileInputStream myFileIS = new FileInputStream(dirItem);
                    myFileIS.read(buff);
                    String mybuffString = new String(buff);
                    System.out.println(DIVIDER + "OCXLET: Reading File: " + absPath + File.separator + dirContents[ii]);
                    myFileIS.close();

                    reading_stop = System.currentTimeMillis();
                    read_time = reading_stop - reading_start;

                    System.out.println("OCXLET: Read Time: " + read_time + DIVIDER);
                    vbox.write(absPath + File.separator + dirContents[ii]);

                    test.assertTrue(true); // Passed the test.

                    log(absPath + File.separator + dirContents[ii]);
                    file_counter = file_counter + 1;
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

                oc_contents[index] = parentDir.getPath() + File.separator + dirContents[ii] + read_time + " ms "
                        + buff.length + " bytes ";
                index++;
            }
        }
    }

    /**
     * This method inherits from org.davic.net.tuning.NetworkInterfaceLIstener,
     * and gets called when the tuning API generates an event for the
     * NetworkInterface object, which we have registered as a listener.
     */
    public void receiveNIEvent(NetworkInterfaceEvent anEvent)
    {
        if (anEvent instanceof NetworkInterfaceTuningOverEvent)
        {
            NetworkInterfaceTuningOverEvent e = (NetworkInterfaceTuningOverEvent) anEvent;
            if (e.getStatus() == NetworkInterfaceTuningOverEvent.FAILED)
            {
                String msg = "Tuning Error: NetworkInterfaceTuningOverEvent.FAILED";
                test.assertTrue(msg, false);
                log(msg);
            }
        }
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

    public void resetDisplay()
    {
        for (int ii = 0; ii < MAX_CONTENT; ii++)
        {
            oc_contents[ii] = new String("");
        }
        index = 0;
    }

    /**
     * Stop directory reading.
     */
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

    public void pauseXlet()
    {
        if (scene != null)
        {
            scene.setVisible(false);
        }
        if (service_context != null)
        {
            service_context.removeListener(this);
        }

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
        if (file_counter == 0)
        {
            test.fail("Number of files read = 0.");
        }

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
}
