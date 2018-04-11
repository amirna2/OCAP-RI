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

package org.cablelabs.xlet.PATPMTTest;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIManager;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;

import javax.tv.service.navigation.ServiceList;
import javax.tv.service.navigation.ServiceIterator;

import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractServiceType;

import org.ocap.si.Descriptor;
import org.ocap.si.PMTElementaryStreamInfo;
import org.ocap.si.PATProgram;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.ProgramMapTableManager;
import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramAssociationTableManager;
import org.ocap.si.TableChangeListener;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.event.HRcEvent;

import java.io.*;
import java.util.Vector;

import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.test.autoxlet.*;

public class PATPMTTestXlet extends Container implements Xlet, KeyListener, Driveable, UserEventListener
{
    private ProgramAssociationTable m_pat = null;

    private ProgramMapTable m_pmt = null;

    private int m_oobpmt_progNum = -1;

    private boolean m_isOOB = false;

    private boolean m_oobpmt_error = true;

    private int channel_index = 0;

    private boolean drawInfoBox = true;

    private boolean pat_req_canceled = false;

    private boolean pmt_req_canceled = false;

    private String pat_change_type = null;

    private String pmt_change_type = null;

    private final int MAX_TRIES = 10;

    private int tries = 0;

    private TableChangeListener pat_tcls[];

    private TableChangeListener pmt_tcls[];

    private TableChangeListener pat_oob_tcl = null;

    private TableChangeListener pmt_oob_tcl = null;

    private SIRequest pat_requests[];

    private SIRequest pmt_requests[];

    private int x, y;

    private OcapLocator m_currentLocator;

    private static Vector services = new Vector();

    private HScene m_scene = null;

    private NetworkInterfaceController nic = null;

    private NetworkInterfaceListener nil = null;

    private static final Font FONT = new Font("tiresias", Font.PLAIN, 14);

    private int tuneState = TUNESTATE_NOT_TUNED;

    // Tuning States
    public static final int TUNESTATE_TUNED = 0x1;

    public static final int TUNESTATE_TUNING = 0x2;

    public static final int TUNESTATE_NOT_TUNED = 0x3;

    public static final int TUNESTATE_TUNE_FAILED = 0x4;

    private static final String CONFIG_FILE = "config_file";

    private static final String PATPMT = "pat_pmt";

    private static final String HIDDEN = "_hidden";

    private static final String NO_PAT = "_no_pat";

    private static final String LOCATOR = "_locator_";

    private static final String FREQUENCY = "_frequency_";

    private static final String PROG_NUM = "_prog_num_";

    private static final String QAM = "_qam_";

    private static final String DIVIDER = "***********************************************************************\n";

    // autoXlet
    private AutoXletClient axc = null;

    private Logger logger = null;

    private Test test = null;

    public void dispatchEvent(KeyEvent arg0, boolean arg1, int arg2)
    {
        keyPressed(arg0);
    }

    public void userEventReceived(UserEvent e)
    {
    }

    public void keyTyped(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void initXlet(XletContext context) throws XletStateChangeException
    {
        // Set up the AutoXlet mechanism and populate local Test and Logger
        // references.
        axc = new AutoXletClient(this, context);
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
        logger.log("**************************** PAT PMT Test *****************************");
        logger.log("***********************************************************************");
        logger.log("***********************************************************************");

        // Set up the scene to display text strings.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setVisible(false);
        m_scene.add(this);

        setBounds(0, 0, 640, 480);
        setBackground(Color.blue);
        setForeground(Color.white);
        setFont(FONT);

        ServiceList sl = null;
        Vector hiddenChannelLocators = null;
        Vector noPATChannelLocators = null;
        int totalNumServices = 0;
        try
        {
            // Get path name of config file.
            ArgParser args = new ArgParser((String[]) context.getXletProperty(XletContext.ARGS));
            if (args == null) throw new XletStateChangeException("No arguments specified.");
            String configFile = args.getStringArg(CONFIG_FILE);

            // Read xlet parameters from config file.
            hiddenChannelLocators = getChannelLocators(configFile, HIDDEN);
            noPATChannelLocators = getChannelLocators(configFile, NO_PAT);

            // Load the services from the SIManager.
            SIManager simgr = SIManager.createInstance();
            sl = simgr.filterServices(null);
            totalNumServices = sl.size() + hiddenChannelLocators.size();

            // Add hidden channels to the services array.
            for (int k = 0; k < hiddenChannelLocators.size(); k++)
            {
                services.addElement(simgr.getService((OcapLocator) (hiddenChannelLocators.elementAt(k))));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.log(e.getMessage());
        }

        // Finish populating our service list with visible services queried from
        // the SIManager
        ServiceIterator sitter = sl.createServiceIterator();

        while (sitter.hasNext())
        {
            Service s = sitter.nextService();
            OcapLocator ol = (OcapLocator) (s.getLocator());

            // Services with no PAT and abstract services are not added to the
            // 'services' vector.
            if (noPATChannelLocators.contains(ol.toString())
                    || s.getServiceType().equals(AbstractServiceType.OCAP_ABSTRACT_SERVICE))
            {
                --totalNumServices;
            }
            else
            {
                services.addElement(s);
            }
        }

        printAllChannels();

        pat_tcls = new TableChangeListener[sl.size() + totalNumServices];
        pmt_tcls = new TableChangeListener[sl.size() + totalNumServices];
        pat_requests = new SIRequest[sl.size() + totalNumServices];
        pmt_requests = new SIRequest[sl.size() + totalNumServices];

        nic = new NetworkInterfaceController(new ResourceClient()
        {
            public boolean requestRelease(ResourceProxy proxy, Object requestData)
            {
                return true;
            }

            public void release(ResourceProxy proxy)
            {
            }

            public void notifyRelease(ResourceProxy proxy)
            {
            }
        });

        nil = new TestNetworkInterfaceListener(this)
        {
            public void receiveNIEvent(NetworkInterfaceEvent anEvent)
            {
                if (anEvent instanceof NetworkInterfaceTuningOverEvent)
                {
                    NetworkInterfaceTuningOverEvent e = (NetworkInterfaceTuningOverEvent) anEvent;
                    tries++;
                    if (e.getStatus() == NetworkInterfaceTuningOverEvent.FAILED)
                    {
                        testXlet.setTuneState(PATPMTTestXlet.TUNESTATE_TUNE_FAILED);

                        if (tries == MAX_TRIES - 1)
                        {
                            String msg = "Failed to tune: " + e;
                            test.assertTrue(msg, false);
                            logger.log(msg);
                            throw new RuntimeException("Failed to tune: " + e);
                        }
                    }
                    if (e.getStatus() == NetworkInterfaceTuningOverEvent.SUCCEEDED)
                    {
                        testXlet.setTuneState(PATPMTTestXlet.TUNESTATE_TUNED);
                    }
                }
            }
        };

        for (int i = 0; i < services.size(); ++i)
        {
            pat_tcls[i] = null;
            pmt_tcls[i] = null;
        }
    }

    public void startXlet() throws XletStateChangeException
    {
        if (services.size() <= 0)
        {
            System.out.println(DIVIDER + "PATPMTTestXlet: The service list is empty.\n" + DIVIDER);
            throw new XletStateChangeException("The service list is empty.");
        }
        else
        {
            // Establish the locator for our first channel
            m_currentLocator = (OcapLocator) (((Service) (services.elementAt(channel_index))).getLocator());

            m_scene.show();
            m_scene.requestFocus();
            m_scene.repaint();

            m_scene.addKeyListener(this);
        }
    }

    public void pauseXlet()
    {
        if (nic.getNetworkInterface() != null) nic.getNetworkInterface().removeNetworkInterfaceListener(nil);

        m_scene.setVisible(false);
        m_scene.removeKeyListener(this);
    }

    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        if (nic.getNetworkInterface() != null) nic.getNetworkInterface().removeNetworkInterfaceListener(nil);
        nil = null;
        nic = null;

        m_scene.removeKeyListener(this);
        m_scene.remove(this);
        m_scene.setVisible(false);
        HSceneFactory.getInstance().dispose(m_scene);
        m_scene = null;
    }

    /*
     * Get xlet parameters from config file.
     */
    private Vector getChannelLocators(String configFile, String channelType)
    {
        ArgParser config_args = null;
        FileInputStream fis = null;
        Vector locators = new Vector();
        try
        {
            fis = new FileInputStream(configFile);
            config_args = new ArgParser(fis);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            logger.log(e.getMessage());
            return locators;
        }

        try
        {
            if (channelType == HIDDEN)
            {
                while (true)
                {
                    int frequency = config_args.getIntArg(PATPMT + channelType + FREQUENCY + locators.size());
                    int programNumber = config_args.getIntArg(PATPMT + channelType + PROG_NUM + locators.size());
                    int qam = config_args.getIntArg(PATPMT + channelType + QAM + locators.size());
                    locators.addElement(new OcapLocator(frequency, programNumber, qam));
                }
            }
            else if (channelType == NO_PAT)
            {
                while (true)
                {
                    locators.addElement(config_args.getStringArg(PATPMT + channelType + LOCATOR + locators.size()));
                }
            }
        }
        catch (Exception e)
        { /* Thrown when there are no more entries */
        }

        finally
        {
            try
            {
                fis.close();
            }
            catch (IOException e)
            {
            }
        }

        return locators;
    }

    private void printAllChannels()
    {
        logger.log(DIVIDER + "\nChannel List:\n" + DIVIDER);

        for (int i = 0; i < services.size(); i++)
        {
            OcapLocator ol = (OcapLocator) (((Service) (services.elementAt(i))).getLocator());
            logger.log(i + ". " + ol.toExternalForm());
        }
    }

    public void keyPressed(KeyEvent e)
    {
        if (e == null) return;

        synchronized (e)
        {
            int key = e.getKeyCode();

            switch (key)
            {
                case HRcEvent.VK_1:
                    pat_req_canceled = true;

                    if (channel_index >= 0 && channel_index < pat_requests.length
                            && pat_requests[channel_index] != null)
                    {
                        pat_requests[channel_index].cancel();
                    }

                    m_scene.repaint();
                    break;

                case HRcEvent.VK_2:
                    pmt_req_canceled = true;

                    if (channel_index >= 0 && channel_index < pmt_requests.length
                            && pmt_requests[channel_index] != null)
                    {
                        pmt_requests[channel_index].cancel();
                    }

                    m_scene.repaint();
                    break;

                // Toggle Register/Unregister of PAT TableChangeListener for the
                // current channel
                case HRcEvent.VK_3:
                    if (pat_tcls[channel_index] == null)
                    {
                        pat_tcls[channel_index] = new TestTableChangeListener(this, false)
                        {
                            public void notifyChange(SIChangeEvent arg0)
                            {
                                testXlet.setPAT((ProgramAssociationTable) (arg0.getSIElement()), arg0.getChangeType()
                                        .toString(), false);
                                testXlet.outputPAT();
                                testXlet.getScene().repaint();
                            }
                        };
                        ProgramAssociationTableManager.getInstance().addInBandChangeListener(pat_tcls[channel_index],
                                m_currentLocator);
                    }
                    else
                    {
                        ProgramAssociationTableManager.getInstance()
                                .removeInBandChangeListener(pat_tcls[channel_index]);
                        pat_tcls[channel_index] = null;
                    }

                    m_scene.repaint();
                    break;

                // Toggle Register/Unregister of PMT TableChangeListener for the
                // current channel
                case HRcEvent.VK_4:
                    if (pmt_tcls[channel_index] == null)
                    {
                        pmt_tcls[channel_index] = new TestTableChangeListener(this, false)
                        {
                            public void notifyChange(SIChangeEvent arg0)
                            {
                                testXlet.setPMT((ProgramMapTable) (arg0.getSIElement()), arg0.getChangeType()
                                        .toString(), false);
                                testXlet.outputPMT();
                                testXlet.getScene().repaint();
                            }
                        };
                        ProgramMapTableManager.getInstance().addInBandChangeListener(pmt_tcls[channel_index],
                                m_currentLocator);
                    }
                    else
                    {
                        ProgramMapTableManager.getInstance().removeInBandChangeListener(pmt_tcls[channel_index]);
                        pmt_tcls[channel_index] = null;
                    }

                    m_scene.repaint();
                    break;

                // Request out-of-band PAT
                case HRcEvent.VK_5:
                    ProgramAssociationTableManager.getInstance().retrieveOutOfBand(new TestRequestor(this)
                    {
                        public void notifySuccess(SIRetrievable[] result)
                        {
                            testXlet.setPAT((ProgramAssociationTable) (result[0]), null, true);
                            testXlet.outputPAT();
                            if (m_pat != null)
                            {
                                test.assertTrue(true);
                            }
                            testXlet.getScene().repaint();
                        }

                        public void notifyFailure(SIRequestFailureType reason)
                        {
                            testXlet.setPAT(null, null, true);
                            testXlet.outputPAT();
                            testXlet.getScene().repaint();
                            test.fail("Failed to get OOB PAT.");
                        }
                    });
                    break;

                // Request out-of-band PMT
                case HRcEvent.VK_6:
                    if (m_oobpmt_progNum == -1)
                    {
                        m_oobpmt_error = true;
                        m_scene.repaint();
                        break;
                    }
                    ProgramMapTableManager.getInstance().retrieveOutOfBand(new TestRequestor(this)
                    {
                        public void notifySuccess(SIRetrievable[] result)
                        {
                            testXlet.setPMT((ProgramMapTable) (result[0]), null, true);
                            testXlet.outputPMT();
                            if (m_pat != null)
                            {
                                test.assertTrue(true);
                            }
                            testXlet.getScene().repaint();
                        }

                        public void notifyFailure(SIRequestFailureType reason)
                        {
                            testXlet.setPMT(null, null, true);
                            testXlet.outputPMT();
                            testXlet.getScene().repaint();
                            test.fail("Failed to get OOB PMT.");
                        }
                    }, m_oobpmt_progNum);
                    break;

                // Toggle Register/Unregister of Out-of-Band PAT
                // TableChangeListener
                case HRcEvent.VK_7:
                    if (pat_oob_tcl == null)
                    {
                        pat_oob_tcl = new TestTableChangeListener(this, true)
                        {
                            public void notifyChange(SIChangeEvent arg0)
                            {
                                testXlet.setPAT((ProgramAssociationTable) (arg0.getSIElement()), arg0.getChangeType()
                                        .toString(), true);
                                testXlet.outputPAT();
                                testXlet.getScene().repaint();
                            }
                        };
                        ProgramAssociationTableManager.getInstance().addOutOfBandChangeListener(pat_oob_tcl);
                    }
                    else
                    {
                        ProgramAssociationTableManager.getInstance().removeOutOfBandChangeListener(pat_oob_tcl);
                        pat_oob_tcl = null;
                    }

                    m_scene.repaint();
                    break;

                // Toggle Register/Unregister of Out-of-Band PMT
                // TableChangeListener
                case HRcEvent.VK_8:
                    if (m_oobpmt_progNum == -1)
                    {
                        m_oobpmt_error = true;
                        m_scene.repaint();
                        break;
                    }
                    if (pmt_oob_tcl == null)
                    {
                        pmt_oob_tcl = new TestTableChangeListener(this, true)
                        {
                            public void notifyChange(SIChangeEvent arg0)
                            {
                                testXlet.setPMT((ProgramMapTable) (arg0.getSIElement()), arg0.getChangeType()
                                        .toString(), true);
                                testXlet.outputPMT();
                                testXlet.getScene().repaint();
                            }
                        };
                        ProgramMapTableManager.getInstance().addOutOfBandChangeListener(pmt_oob_tcl, m_oobpmt_progNum);
                    }
                    else
                    {
                        ProgramMapTableManager.getInstance().removeOutOfBandChangeListener(pmt_oob_tcl);
                        pmt_oob_tcl = null;
                    }

                    m_scene.repaint();
                    break;

                case HRcEvent.VK_CHANNEL_UP:

                    // If we were tuned to the previous channel, remove the
                    // PAT/PMT requests
                    if (tuneState == TUNESTATE_TUNED)
                    {
                        if (m_pat == null)
                        {
                            test.fail("Failed to get PAT for Source ID 0x"
                                    + Integer.toHexString(m_currentLocator.getSourceID()) + ".");
                        }
                        if (m_pmt == null)
                        {
                            test.fail("Failed to get PMT for Source ID 0x"
                                    + Integer.toHexString(m_currentLocator.getSourceID()) + ".");
                        }

                        if (!pat_req_canceled)
                        {
                            if (channel_index >= 0 && channel_index < pat_requests.length
                                    && pat_requests[channel_index] != null)
                            {
                                pat_requests[channel_index].cancel();
                            }
                        }
                        if (!pmt_req_canceled)
                        {
                            if (channel_index >= 0 && channel_index < pmt_requests.length
                                    && pmt_requests[channel_index] != null)
                            {
                                pmt_requests[channel_index].cancel();
                            }
                        }
                    }

                    // Select new channel index
                    channel_index += 1;
                    if (channel_index == services.size()) channel_index = 0;
                    m_currentLocator = (OcapLocator) (((Service) (services.elementAt(channel_index))).getLocator());

                    setTuneState(TUNESTATE_NOT_TUNED);

                    break;

                case HRcEvent.VK_CHANNEL_DOWN:

                    // If we were tuned to the previous channel, remove the
                    // PAT/PMT requests
                    if (tuneState == TUNESTATE_TUNED)
                    {
                        if (m_pat == null)
                        {
                            test.fail("Failed to get PAT for Source ID 0x"
                                    + Integer.toHexString(m_currentLocator.getSourceID()) + ".");
                        }
                        if (m_pmt == null)
                        {
                            test.fail("Failed to get PMT for Source ID 0x"
                                    + Integer.toHexString(m_currentLocator.getSourceID()) + ".");
                        }

                        if (!pat_req_canceled)
                        {
                            if (channel_index >= 0 && channel_index < pat_requests.length
                                    && pat_requests[channel_index] != null)
                            {
                                pat_requests[channel_index].cancel();
                            }
                        }
                        if (!pmt_req_canceled)
                        {
                            if (channel_index >= 0 && channel_index < pmt_requests.length
                                    && pmt_requests[channel_index] != null)
                            {
                                pmt_requests[channel_index].cancel();
                            }
                        }
                    }

                    // Select new channel index
                    channel_index -= 1;
                    if (channel_index == -1) channel_index = services.size() - 1;
                    m_currentLocator = (OcapLocator) (((Service) (services.elementAt(channel_index))).getLocator());

                    setTuneState(TUNESTATE_NOT_TUNED);

                    break;

                case HRcEvent.VK_ENTER:
                    if (tuneState == TUNESTATE_TUNED) break;
                    tries = 0;

                    // Attempt tuning up to MAX_TRIES times, because tuning may
                    // fail,
                    // if the SI data is not available yet.
                    while (tries < MAX_TRIES && tuneState != TUNESTATE_TUNED)
                    {
                        try
                        {
                            tune();
                        }
                        catch (Exception e1)
                        {
                            tries++;
                            if (tries == MAX_TRIES - 1)
                            {
                                String msg = "Failed to tune: " + e1.getMessage();
                                test.assertTrue(msg, false);
                                logger.log(msg);
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

                        try
                        {
                            Thread.sleep(2000);
                        }
                        catch (Exception e2)
                        {
                        }
                    }

                    break;

                default:
                    break;
            }
        }
    }

    private synchronized void tune() throws Exception
    {
        Service service = (Service) (services.elementAt(channel_index));

        // Reserve the NetworkInterface and add a listener
        nic.reserveFor((OcapLocator) service.getLocator(), null);

        nic.getNetworkInterface().addNetworkInterfaceListener(nil);

        // Attempt to tune to the service and wait for the completion event
        nic.tune((OcapLocator) service.getLocator());

        setTuneState(TUNESTATE_TUNING);
    }

    public HScene getScene()
    {
        return m_scene;
    }

    public synchronized void setPAT(ProgramAssociationTable pat, String change_type, boolean isOOB)
    {
        m_pat = pat;
        pat_change_type = change_type;
        m_isOOB = isOOB;

        // Set the program number for the out-of-band PMT. There will only be
        // one
        // out-of-band program signaled in the PAT
        if (m_isOOB)
        {
            if (m_pat != null && m_pat.getPrograms().length > 0)
                m_oobpmt_progNum = m_pat.getPrograms()[0].getProgramNumber();
            else
                m_oobpmt_progNum = -1;
        }
    }

    public synchronized void setPMT(ProgramMapTable pmt, String change_type, boolean isOOB)
    {
        m_pmt = pmt;
        pmt_change_type = change_type;
        m_isOOB = isOOB;
    }

    public String getChannelID(OcapLocator loc)
    {
        if (loc == null)
        {
            return "NULL LOCATOR";
        }

        String channel_id = Integer.toHexString(loc.getSourceID());

        if (channel_id.startsWith("fff"))
        {
            channel_id = "FR=" + loc.getFrequency() / 1000000 + " QAM=" + loc.getModulationFormat() + " PID="
                    + loc.getProgramNumber();
        }
        else
        {
            channel_id = "Source ID : 0x" + channel_id;
        }
        return channel_id;
    }

    public synchronized void outputPAT()
    {
        synchronized (logger)
        {
            if (m_isOOB)
            {
                logger.log("<<<<<<<<<< Out - Of - Band >>>>>>>>>>");
            }
            else if (m_currentLocator != null)
            {
                logger.log("<<<<<<<<<< " + getChannelID(m_currentLocator) + " >>>>>>>>>>");
            }

            if (pat_change_type != null)
            {
                logger.log("**************** P A T ( " + pat_change_type + ") ****************");
            }
            else
            {
                logger.log("**************** P A T ****************");
            }

            if (m_pat == null)
            {
                logger.log("!!!!! PAT is null !!!!!");
                return;
            }

            PATProgram[] Programs = m_pat.getPrograms();
            if (Programs == null)
            {
                return;
            }

            synchronized (Programs)
            {
                for (int i = 0; i < Programs.length; i++)
                {
                    PATProgram program = Programs[i];
                    if (program == null)
                    {
                        continue;
                    }

                    logger.log("PAT Program " + i);
                    logger.log("    Program Number: " + program.getProgramNumber());
                    logger.log("    PMT PID: " + program.getPID());
                }
            }
        }
    }

    public synchronized void outputPMT()
    {
        synchronized (logger)
        {
            if (m_isOOB)
            {
                logger.log("<<<<<<<<<< Out - Of - Band >>>>>>>>>>");
            }
            else if (m_currentLocator != null)
            {
                logger.log("<<<<<<<<<< " + getChannelID(m_currentLocator) + " >>>>>>>>>>");
            }

            if (pmt_change_type != null)
            {
                logger.log("**************** P M T ( " + pmt_change_type + ") ****************");
            }
            else
            {
                logger.log("**************** P M T ****************");
            }

            if (m_pmt == null)
            {
                logger.log("!!!!! PMT is null !!!!!");
                return;
            }

            logger.log("Program Number: " + m_pmt.getProgramNumber() + "\n" + "PCR PID: " + m_pmt.getPcrPID());

            // Outer Descriptors

            Descriptor[] OuterDescriptors = m_pmt.getOuterDescriptorLoop();

            if (OuterDescriptors == null || OuterDescriptors.length == 0)
            {
                logger.log("No Outer Descriptors");
            }
            else
            {
                synchronized (OuterDescriptors)
                {
                    for (int i = 0; i < OuterDescriptors.length; i++)
                    {
                        Descriptor desc = OuterDescriptors[i];
                        if (desc == null)
                        {
                            continue;
                        }

                        logger.log("OuterDescriptor " + i + "\n" + "    Tag: " + desc.getTag() + "\n"
                                + "    Content Length: " + desc.getContentLength());

                        String descContent = "";
                        for (int j = 0; j < desc.getContent().length; ++j)
                        {
                            int data = desc.getContent()[j] & 0xFF;
                            descContent += Integer.toHexString(data) + " ";
                        }
                        logger.log("    " + descContent);
                    }
                }
            }

            // Elementary Streams

            PMTElementaryStreamInfo[] ElementaryStreams = m_pmt.getPMTElementaryStreamInfoLoop();

            if (ElementaryStreams == null || ElementaryStreams.length == 0)
            {
                logger.log("No Elementary Streams");
            }
            else
            {
                synchronized (ElementaryStreams)
                {
                    for (int i = 0; i < ElementaryStreams.length; i++)
                    {
                        PMTElementaryStreamInfo esInfo = ElementaryStreams[i];

                        if (esInfo == null)
                        {
                            continue;
                        }

                        logger.log("Elementary Stream " + i + "\n" + "    Stream Type: " + esInfo.getStreamType()
                                + "\n" + "    Elementary PID: " + esInfo.getElementaryPID() + "\n"
                                + "    Locator String: " + esInfo.getLocatorString());

                        // Stream Descriptors

                        Descriptor[] StreamDescriptors = esInfo.getDescriptorLoop();

                        if (StreamDescriptors == null || StreamDescriptors.length == 0)
                        {
                            logger.log("    No descriptors");
                        }
                        else
                        {
                            synchronized (StreamDescriptors)
                            {
                                for (int j = 0; j < StreamDescriptors.length; j++)
                                {
                                    Descriptor desc = StreamDescriptors[j];
                                    if (desc == null)
                                    {
                                        continue;
                                    }

                                    logger.log("    Descriptor " + j + "\n" + "        Tag: " + desc.getTag() + "\n"
                                            + "        Content Length: " + desc.getContentLength());

                                    String descContent = "";
                                    for (int k = 0; k < desc.getContent().length; k++)
                                    {
                                        int data = desc.getContent()[k] & 0xFF;
                                        descContent += Integer.toHexString(data) + " ";
                                    }
                                    logger.log("        " + descContent);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public synchronized void paint(Graphics g)
    {
        int indent = 10;

        setForeground(Color.white);

        g.drawLine(320, 5, 320, 480);

        x = 45;
        y = 35;

        g.drawString("<<<<<<<<<<<<<<<<<<<<< " + getChannelID(m_currentLocator) + " >>>>>>>>>>>>>>>>>>>>>", x, y);
        crlf();

        // PAT
        if (pat_change_type != null)
            g.drawString("**************** P A T ( " + pat_change_type + ") ****************", x, y);
        else
            g.drawString("**************** P A T ****************", x, y);
        crlf();

        if (m_pat == null)
        {
            g.drawString("!!!!! PAT is null !!!!!", x, y);
            crlf();
        }
        else
        {
            for (int i = 0; i < m_pat.getPrograms().length; ++i)
            {
                PATProgram program = m_pat.getPrograms()[i];

                g.drawString("PAT Program " + i, x, y);
                crlf();

                g.drawString("Program Number: " + program.getProgramNumber(), x + indent, y);
                crlf();

                g.drawString("PMT PID: " + program.getPID(), x + indent, y);
                crlf();
            }
        }

        crlf();

        // PMT
        if (pmt_change_type != null)
            g.drawString("**************** P M T ( " + pmt_change_type + ") ****************", x, y);
        else
            g.drawString("**************** P M T ****************", x, y);

        crlf();
        if (m_pmt == null)
        {
            g.drawString("!!!!! PMT is null !!!!!", x, y);
            crlf();
        }
        else
        {
            g.drawString("Program Number: " + m_pmt.getProgramNumber(), x, y);
            crlf();

            g.drawString("PCR PID: " + m_pmt.getPcrPID(), x, y);
            crlf();

            // Outer Descriptors
            if (m_pmt.getOuterDescriptorLoop() == null || m_pmt.getOuterDescriptorLoop().length == 0)
            {
                g.drawString("No Outer Descriptors", x, y);
                crlf();
            }
            else
            {
                for (int i = 0; i < m_pmt.getOuterDescriptorLoop().length; ++i)
                {
                    Descriptor desc = m_pmt.getOuterDescriptorLoop()[i];

                    g.drawString("OuterDescriptor " + i, x, y);
                    crlf();

                    g.drawString("Tag: " + desc.getTag(), x + indent, y);
                    crlf();

                    g.drawString("Content Length: " + desc.getContentLength(), x + indent, y);
                    crlf();

                    String descContent = "";
                    for (int j = 0; j < desc.getContent().length; ++j)
                    {
                        int data = desc.getContent()[j] & 0xFF;
                        descContent += Integer.toHexString(data) + " ";
                    }

                    g.drawString(descContent, x + indent, y);
                    crlf();
                }
            }

            // Elementary Streams
            if (m_pmt.getPMTElementaryStreamInfoLoop() == null || m_pmt.getPMTElementaryStreamInfoLoop().length == 0)
            {
                g.drawString("No Elementary Streams", x, y);
                crlf();
            }
            else
            {
                for (int i = 0; i < m_pmt.getPMTElementaryStreamInfoLoop().length; ++i)
                {
                    PMTElementaryStreamInfo esInfo = m_pmt.getPMTElementaryStreamInfoLoop()[i];

                    g.drawString("Elementary Stream " + i, x, y);
                    crlf();

                    g.drawString("Stream Type: " + esInfo.getStreamType(), x + indent, y);
                    crlf();

                    g.drawString("Elementary PID: " + esInfo.getElementaryPID(), x + indent, y);
                    crlf();

                    g.drawString("Locator String: " + esInfo.getLocatorString(), x + indent, y);
                    crlf();

                    // Stream Descriptors
                    if (esInfo.getDescriptorLoop() == null || esInfo.getDescriptorLoop().length == 0)
                    {
                        g.drawString("No descriptors", x + indent, y);
                        crlf();
                    }
                    else
                    {
                        for (int j = 0; j < esInfo.getDescriptorLoop().length; ++j)
                        {
                            Descriptor desc = esInfo.getDescriptorLoop()[j];

                            g.drawString("OuterDescriptor " + j, x + indent, y);
                            crlf();

                            g.drawString("Tag: " + desc.getTag(), x + indent + indent, y);
                            crlf();

                            g.drawString("Content Length: " + desc.getContentLength(), x + indent + indent, y);
                            crlf();

                            String descContent = "";
                            for (int k = 0; k < desc.getContent().length; ++k)
                            {
                                int data = desc.getContent()[k] & 0xFF;
                                descContent += Integer.toHexString(data) + " ";
                            }

                            g.drawString(descContent, x + indent + indent, y);
                            crlf();
                        }
                    }
                }
            }
        }

        if (drawInfoBox)
        {
            g.setColor(Color.white);
            g.fillRect(120, 280, 370, 160);
            g.setColor(Color.black);

            if (m_oobpmt_error)
            {
                g.drawString("Must receive OOB PAT before OOB PMT!!", 150, 300);
                m_oobpmt_error = false;
            }
            else
            {
                switch (tuneState)
                {
                    case TUNESTATE_TUNING:
                        g.drawString("Tuning to " + getChannelID(m_currentLocator), 145, 300);
                        break;

                    case TUNESTATE_NOT_TUNED:
                        g.drawString("Current " + getChannelID(m_currentLocator) + ". Press [SELECT] to tune.", 125,
                                300);
                        break;

                    case TUNESTATE_TUNE_FAILED:
                        g.drawString("Tune to " + getChannelID(m_currentLocator) + " FAILED!", 145, 300);
                        break;
                }
            }

            int xpos = 145;
            int ypos = 325;

            if (tuneState != TUNESTATE_TUNE_FAILED)
            {
                if (pat_req_canceled)
                    g.drawString("In-Band PAT request canceled.", xpos, ypos);
                else
                    g.drawString("Press 1 to cancel In-Band PAT request.", xpos, ypos);
                ypos += 15;

                if (pmt_req_canceled)
                    g.drawString("In-Band PMT request canceled.", xpos, ypos);
                else
                    g.drawString("Press 2 to cancel In-Band PMT request.", xpos, ypos);
                ypos += 15;

                if (pat_tcls[channel_index] == null)
                    g.drawString("Press 3 to register In-Band PAT change listener.", xpos, ypos);
                else
                    g.drawString("Press 3 to unregister In-Band PAT change listener.", xpos, ypos);
                ypos += 15;

                if (pmt_tcls[channel_index] == null)
                    g.drawString("Press 4 to register In-Band PMT change listener.", xpos, ypos);
                else
                    g.drawString("Press 4 to unregister In-Band PMT change listener.", xpos, ypos);
                ypos += 15;

                // Out-of-band
                g.drawString("Press 5 to request Out-of-Band PAT.", xpos, ypos);
                ypos += 15;

                g.drawString("Press 6 to request Out-of-Band PMT.", xpos, ypos);
                ypos += 15;

                if (pat_oob_tcl == null)
                    g.drawString("Press 7 to register Out-of-Band PAT change listener.", xpos, ypos);
                else
                    g.drawString("Press 7 to unregister Out-of-Band PAT change listener.", xpos, ypos);
                ypos += 15;

                if (pmt_oob_tcl == null)
                    g.drawString("Press 8 to register Out-of-Band PMT change listener.", xpos, ypos);
                else
                    g.drawString("Press 8 to unregister Out-of-Band PMT change listener.", xpos, ypos);
                ypos += 15;
            }
        }
    }

    // Simulate line feed
    void crlf()
    {
        final int vert_offset = 13;
        final int horiz_offset = 320;

        y += vert_offset;

        if (y > 480)
        {
            y = 48;
            x += horiz_offset;
        }
    }

    abstract class TestRequestor implements SIRequestor
    {
        public TestRequestor(PATPMTTestXlet testXlet)
        {
            this.testXlet = testXlet;
        }

        protected PATPMTTestXlet testXlet;
    }

    abstract class TestTableChangeListener implements TableChangeListener
    {
        public TestTableChangeListener(PATPMTTestXlet testXlet, boolean isOOB)
        {
            this.testXlet = testXlet;
            this.isOOB = isOOB;
        }

        protected PATPMTTestXlet testXlet;

        protected boolean isOOB;
    }

    abstract class TestNetworkInterfaceListener implements NetworkInterfaceListener
    {
        public TestNetworkInterfaceListener(PATPMTTestXlet testXlet)
        {
            this.testXlet = testXlet;
        }

        protected PATPMTTestXlet testXlet;
    }

    public void setTuneState(int tuneState)
    {
        this.tuneState = tuneState;

        switch (this.tuneState)
        {
            case TUNESTATE_NOT_TUNED:
                drawInfoBox = true;
                m_pat = null;
                m_pmt = null;
                pat_change_type = null;
                pmt_change_type = null;
                pat_req_canceled = false;
                pmt_req_canceled = false;
                break;

            case TUNESTATE_TUNED:

                drawInfoBox = false;

                try
                {
                    Thread.sleep(1000);
                }
                catch (Exception e)
                {
                }

                try
                {
                    Thread.sleep(1000);
                }
                catch (Exception e)
                {
                }

                // Register for PAT notification
                if (!pat_req_canceled)
                    pat_requests[channel_index] = ProgramAssociationTableManager.getInstance().retrieveInBand(
                            new TestRequestor(this)
                            {
                                public void notifySuccess(SIRetrievable[] result)
                                {
                                    testXlet.setPAT((ProgramAssociationTable) (result[0]), null, false);
                                    testXlet.outputPAT();
                                    if (m_pat != null)
                                    {
                                        test.assertTrue(true);
                                    }
                                    testXlet.getScene().repaint();
                                }

                                public void notifyFailure(SIRequestFailureType reason)
                                {
                                    testXlet.setPAT(null, null, false);
                                    testXlet.outputPAT();
                                    testXlet.getScene().repaint();
                                }
                            }, m_currentLocator);

                // Register for PMT notification
                if (!pmt_req_canceled)
                    pmt_requests[channel_index] = ProgramMapTableManager.getInstance().retrieveInBand(
                            new TestRequestor(this)
                            {
                                public void notifySuccess(SIRetrievable[] result)
                                {
                                    testXlet.setPMT((ProgramMapTable) (result[0]), null, false);
                                    testXlet.outputPMT();
                                    if (m_pat != null)
                                    {
                                        test.assertTrue(true);
                                    }
                                    testXlet.getScene().repaint();
                                }

                                public void notifyFailure(SIRequestFailureType reason)
                                {
                                    testXlet.setPMT(null, null, false);
                                    testXlet.outputPMT();
                                    testXlet.getScene().repaint();
                                }
                            }, m_currentLocator);

                break;

            case TUNESTATE_TUNING:
            case TUNESTATE_TUNE_FAILED:
                break;
        }
        m_scene.repaint();
    }
}
