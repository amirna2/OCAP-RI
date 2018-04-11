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

/*
 * Created on July 26, 2005
 */
package org.cablelabs.xlet.DvrSecurityTest;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.event.HRcEvent;

import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingStateFilter;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.ui.event.OCRcEvent;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.storage.MediaStorageVolume;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;

/**
 * Xlet to submit one or more recording requests which will then be accessed by
 * other Xlets.
 * 
 */
public class TestMgrXlet implements Xlet, KeyListener
{
    private static final String BANNER = "+---------------------------------------------------------+";

    private static final String WHOAMI = "TestMgrXlet: ";

    private static final String CFG_ARG = "config_file";

    // We expect four LocatorRecordingSpecification tokens
    private static final int NUM_LRS_TOKENS = 6;

    private Vector m_locRecSpecs;

    // Instance variables in alphabetical order ...
    private MediaStorageVolume m_dest;

    private OcapRecordingManager m_mgr;

    private OcapRecordingRequest m_orr;

    private HScene m_scene;

    private XletContext m_xctx;

    private String m_configFname;

    private VidTextBox m_vbox;

    public void startXlet()
    {
        init();
        m_scene.show();
        m_scene.repaint();
        m_scene.requestFocus();
    }

    public void pauseXlet()
    {
        m_scene.setVisible(false);
    }

    public void destroyXlet(boolean x)
    {
        m_scene.dispose();
    }

    public void initXlet(XletContext ctx)
    {
        m_xctx = ctx;

        try
        {
            ArgParser xletArgs = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));
            m_configFname = xletArgs.getStringArg(CFG_ARG);
            if (null == m_configFname)
            {
                throw new NullPointerException(WHOAMI + "requires an arg that identifies the config file!!!");
            }
        }
        catch (Exception e)
        {
            throw new IllegalStateException(WHOAMI + "failed to read config filename: " + e.getMessage());
        }

        // Remote Control key listener
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        // x, y, w, h, font_size, #chars
        // m_vbox = new VidTextBox(50,200,530,200,14,5000);
        m_vbox = new VidTextBox(50, 50, 530, 400, 14, 5000);
        if (null == m_vbox)
        {
            System.err.println(WHOAMI + "FAILED TO INSTANTIATE VidTextBox!!!");
        }
        m_scene.add(m_vbox);
        m_scene.addKeyListener(this);
        m_scene.addKeyListener(m_vbox);
        m_scene.setLayout(new BorderLayout());
        m_scene.validate();
        printMenu();
    }

    private void init()
    {
        StorageProxy proxyAry[] = StorageManager.getInstance().getStorageProxies();
        if (0 == proxyAry.length)
        {
            throw new IllegalStateException(WHOAMI + "StorageManager.getStorageProxies() returned zero-length array");
        }

        m_mgr = (OcapRecordingManager) OcapRecordingManager.getInstance();
        if (null == m_mgr)
        {
            throw new NullPointerException(WHOAMI + "Failed to retrieve OcapRecordingManager Object ref");
        }
        else
        {
            System.out.println(WHOAMI + "Retrieved OcapRecordingManager Object ref: " + m_mgr);
        }
    }

    private void dumpEntries(boolean purgeFlag)
    {
        RecordingList rlist = m_mgr.getEntries();
        log(BANNER);
        log(WHOAMI + "Number of entries: " + rlist.size());
        for (int i = 0; i < rlist.size(); ++i)
        {
            OcapRecordingRequest req = (OcapRecordingRequest) (rlist.getRecordingRequest(i));

            LocatorRecordingSpec lrs = (LocatorRecordingSpec) req.getRecordingSpec();
            javax.tv.locator.Locator[] srcAry = lrs.getSource();
            log(srcAry[0].toString() + " " + lrs.getStartTime() + " " + lrs.getDuration() + " "
                    + TestUtil.stateToString(req.getState()));

            if (purgeFlag)
            {
                try
                {
                    log("Deleting request ...");
                    req.delete();
                }
                catch (AccessDeniedException e)
                {
                    // No detail message associated with this Exception
                    log(WHOAMI + "OcapRecordingRequest.delete() threw AccessDeniedException");
                }
                catch (Exception e)
                {
                    log(WHOAMI + "OcapRecordingRequest.delete() threw Exception: " + e.getMessage());
                }
            }
        }
        log(BANNER);
    }

    // TODO - display meaningful information
    private void dumpVector()
    {
        System.out.println(BANNER);
        if ((null == m_locRecSpecs) || (0 >= m_locRecSpecs.size()))
        {
            System.out.println("No LocatorRecordingSpecs in list!");
            return;
        }
        Enumeration enumer = m_locRecSpecs.elements();
        int i = 0;
        while (enumer.hasMoreElements())
        {
            LocatorRecordingSpec lrs = (LocatorRecordingSpec) enumer.nextElement();
            System.out.println("LRS[" + i + "]: " + lrs);
            i++;
        }
        System.out.println(BANNER);
    }

    private void initRecReq()
    {
        log("Initializing RecordingRequests ...");
        if (!readConfig(m_configFname))
        {
            throw new IllegalStateException(WHOAMI + "failed to read config info!!!");
        }

        if (0 >= m_locRecSpecs.size())
        {
            log("Vector of LocatorRecordingSpecs is empty ... doing nothing ...");
            return;
        }

        Enumeration enumer = m_locRecSpecs.elements();
        while (enumer.hasMoreElements())
        {
            LocatorRecordingSpec lrs = (LocatorRecordingSpec) enumer.nextElement();
            try
            {
                OcapRecordingRequest orr = (OcapRecordingRequest) m_mgr.record(lrs);
                if (null == orr)
                {
                    log("Failed to instantiate OcapRecordingRequest Object!");
                    throw new NullPointerException(WHOAMI + "Failed to instantiate OcapRecordingRequest Object");
                }
            }
            catch (AccessDeniedException e)
            {
                log("RecordingManager.record() threw AccessDeniedException");
            }
            catch (Exception e)
            {
                log("RecordingManager.record() threw : " + e.toString());
                log("RecordingManager.record() threw : " + e.getMessage());
            }
        }
        dumpEntries(false);
    }

    public void printMenu()
    {
        log(BANNER);
        log(" Menu:");
        log("<1> - Initialize Recording Requests");
        log("<2> - Display Current List of Recording Requests");
        log("<3> - Delete All Recording Requests");
        log("<4> - Display internal list of LocatorRecordingSpecs");
        log("<5> - Dump Overlapping Entries (PRF-dependent)");
        log("<6> - Dump Entries (no filter; PRF-dependent)");
        log("<7> - Dump Entries (hardcoded PENDING_WITH_CONFLICT_STATE filter; PRF-dependent)");
        log("<8> - Dump Entries and Root Requests");
        log("<9> - Dump Entries and Parent Requests");
        log(BANNER);
        m_scene.repaint();
    }

    /***
     *** KeyListener Interface
     ***/
    public void keyTyped(KeyEvent e)
    {
        System.out.println(WHOAMI + "keyTyped(): " + e);
    }

    public void keyReleased(KeyEvent e)
    {
        System.out.println(WHOAMI + "keyReleased(): " + e);
    }

    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();

        if (HRcEvent.VK_INFO == key)
        {
            e.consume();
            printMenu();
        }
        else if (HRcEvent.VK_1 == key)
        {
            e.consume();
            initRecReq();
        }
        else if (HRcEvent.VK_2 == key)
        {
            e.consume();
            // dump --- do not purge --- entries ...
            dumpEntries(false);
        }
        else if (HRcEvent.VK_3 == key)
        {
            e.consume();
            // purge entries ...
            dumpEntries(true);
        }
        else if (HRcEvent.VK_4 == key)
        {
            e.consume();
            dumpVector();
        }
        else if (HRcEvent.VK_5 == key)
        {
            e.consume();
            // behavior depends on MA Perms: "handler.recording" vs. "recording"
            TestUtil.dumpOverlappingEntries();
        }
        else if (HRcEvent.VK_6 == key)
        {
            e.consume();
            // behavior depends on MA Perms: "handler.recording" vs. "recording"
            TestUtil.dumpEntries(null);
        }
        else if (HRcEvent.VK_7 == key)
        {
            e.consume();
            // behavior depends on MA Perms: "handler.recording" vs. "recording"
            TestUtil.dumpEntries(new RecordingStateFilter(LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE));
        }
        else if (HRcEvent.VK_8 == key)
        {
            e.consume();
            // behavior depends on MA Perms: "handler.recording" vs. "recording"
            TestUtil.dumpRoot();
        }
        else if (HRcEvent.VK_9 == key)
        {
            e.consume();
            // behavior depends on MA Perms: "handler.recording" vs. "recording"
            TestUtil.dumpParent();
        }
        else
        {
            m_vbox.keyPressed(e);
        }
    }

    /*
     * Read configuration information from ASCII input file.
     * 
     * TODO - XML
     */
    private boolean readConfig(String fname)
    {
        log("Reading configuration file " + fname);
        boolean amOkay = true;
        m_locRecSpecs = new Vector();
        BufferedReader rdr = null;
        String line = null;
        try
        {
            rdr = new BufferedReader(new FileReader(fname));
            while (null != (line = rdr.readLine()))
            {
                if (0 == line.indexOf('|'))
                {
                    System.out.println("line: " + line);
                    // ignore comments and blank lines ...
                    StringTokenizer st = new StringTokenizer(line, "|");
                    if (NUM_LRS_TOKENS == st.countTokens())
                    {
                        // "rwrwrw:arbitraryIdentifier"
                        String reqId = st.nextToken();
                        log("reqId: " + reqId);

                        // prune the arbitrary identifier from the
                        // request ID ... retain the token-separating ":"
                        ExtendedFileAccessPermissions fap = TestConfig.getEFAP(reqId.substring(0,
                                reqId.indexOf(":") + 1)
                                + st.nextToken());

                        // Effective 20050818, the EFAP can be null
                        log("fap: " + fap);

                        String srcId = st.nextToken();
                        log("srcId: " + srcId);

                        Date startTime = TestConfig.getTime(st.nextToken());
                        if (null == startTime)
                        {
                            throw new NullPointerException(WHOAMI + "Failed to retrieve Date Object ref");
                        }
                        log("startTime: " + startTime);

                        int durMsec = Integer.parseInt(st.nextToken());
                        log("durMsec: " + durMsec);

                        long expPerSec = Long.parseLong(st.nextToken());
                        log("expPerSec: " + expPerSec);

                        LocatorRecordingSpec lrs = buildLRS(srcId, startTime, durMsec, expPerSec, fap);
                        if (null == lrs)
                        {
                            log("Failed to retrieve LocatorRecordingSpec Object ref!");
                            throw new NullPointerException(WHOAMI
                                    + "Failed to retrieve LocatorRecordingSpec Object ref");
                        }
                        m_locRecSpecs.addElement(lrs);
                    }
                    else
                    {
                        log("Badly-formed input line: \"" + line + "\"");
                    }
                }
            }
        }
        catch (Exception e)
        {
            log("Failed to read config file: " + e.getMessage());
            throw new IllegalStateException("Failed to read config file: " + e.getMessage());
        }

        return (amOkay);
    }

    private LocatorRecordingSpec buildLRS(String srcId, Date startTime, int durMsec, long expPerSec,
            ExtendedFileAccessPermissions fap)
    {
        log("Building LocatorRecordingSpec ...");
        OcapLocator[] source = null;
        try
        {
            source = new OcapLocator[] { new OcapLocator(srcId) };
        }
        catch (org.davic.net.InvalidLocatorException e)
        {
            log("Failed to instantiate OcapLocator array: " + e.getMessage());
            throw new IllegalStateException("Failed to instantiate OcapLocator array: " + e.getMessage());
        }

        OcapRecordingProperties orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, expPerSec,
                OcapRecordingProperties.DELETE_AT_EXPIRATION, OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, fap,
                "Acme Video", m_dest);
        if (null == orp)
        {
            log("Failed to instantiate OcapRecordingProperties Object");
            throw new NullPointerException(WHOAMI + "Failed to instantiate OcapRecordingProperties Object");
        }

        LocatorRecordingSpec lrs = null;
        try
        {
            lrs = new LocatorRecordingSpec(source, startTime, durMsec, orp);
            if (null == lrs)
            {
                log("Failed to instantiate LocatorRecordingSpec Object (null)");
                throw new NullPointerException(WHOAMI + "Failed to instantiate LocatorRecordingSpec Object (null)");
            }
        }
        catch (Exception e)
        {
            log("Failed to instantiate LocatorRecordingSpec Object: " + e.getMessage());
            throw new NullPointerException(WHOAMI + "Failed to instantiate LocatorRecordingSpec Object: "
                    + e.getMessage());
        }
        return (lrs);
    }

    private void log(String msg)
    {
        System.out.println(WHOAMI + msg);
        m_vbox.write(msg);
    }
}
