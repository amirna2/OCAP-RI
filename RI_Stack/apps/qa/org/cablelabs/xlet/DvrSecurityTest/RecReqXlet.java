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

import javax.media.Time;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.service.Service;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;

import org.ocap.net.OcapLocator;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;

import org.cablelabs.lib.utils.ArgParser;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.util.StringTokenizer;

import org.cablelabs.lib.utils.VidTextBox;

/**
 * Xlet to submit one or more recording requests which will then be accessed by
 * other Xlets.
 * 
 */
public class RecReqXlet implements Xlet
{
    private static final String BANNER = "+---------------------------------------------------------+";

    private static final String WHOAMI = "RecReqXlet ";

    private static final String CFG_ARG = "config_file";

    // the primary configuration file provides complete definitions
    // of the recording requests
    private static final String PRI_CFG_ARG = "pri_config_file";

    private static final int NUM_CFGLINE_TOKENS = 3;

    // Instance variables in alphabetical order ...
    private OcapRecordingManager m_mgr;

    private OcapRecordingRequest m_orr;

    private OcapLocator[] m_source;

    private XletContext m_xctx;

    private HScene m_scene;

    private VidTextBox m_vbox;

    // N.B. record() throwing of AccessDeniedException is
    // exercised by the TestMgrXlet (easier than trying to
    // to do that within this Xlet)
    private static final int ACTION_CANCEL = 0;

    private static final int ACTION_DELETE = 1;

    private static final int ACTION_STOP = 2;

    private static final int ACTION_GETSVC = 3;

    private static final int ACTION_ADDAPPDATA = 4;

    private static final int ACTION_REMAPPDATA = 5;

    private static final int ACTION_RESCHED = 6;

    private static final int ACTION_SETRECPROPS = 7;

    private static final String[] ACTION_DESC = { "cancel", "delete", "stop", "getService", "addAppData",
            "removeAppData", "reschedule", "setRecordingProperties" };

    private String m_configFname;

    private String m_priConfigFname;

    public void startXlet()
    {
        init();

        m_scene.show();
        m_scene.repaint();
        m_scene.requestFocus();

        boolean pass = doTests();
        log(BANNER);
        if (pass)
        {
            log("RecReqXlet PASS");
        }
        else
        {
            log("RecReqXlet FAIL");
        }
        log(BANNER);

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

        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        // x, y, w, h, font_size, #chars
        // m_vbox = new VidTextBox(50,200,530,200,14,5000);
        m_vbox = new VidTextBox(50, 50, 530, 400, 14, 5000);
        if (null == m_vbox)
        {
            System.err.println(WHOAMI + "FAILED TO INSTANTIATE VidTextBox!!!");
        }
        m_scene.add(m_vbox);
        m_scene.addKeyListener(m_vbox);
        // m_scene.setLayout(null);
        m_scene.setLayout(new BorderLayout());
        m_scene.validate();
    }

    private boolean doTests()
    {
        boolean passed = true;
        BufferedReader rdr = null;
        String line = null;
        try
        {
            rdr = new BufferedReader(new FileReader(m_configFname));
            while (null != (line = rdr.readLine()))
            {
                if (0 == line.indexOf('|'))
                {
                    log("line: " + line);
                    StringTokenizer st = new StringTokenizer(line, "|");
                    if (NUM_CFGLINE_TOKENS == st.countTokens())
                    {

                        String reqId = st.nextToken();
                        String action = st.nextToken();
                        String expected = st.nextToken();

                        OcapRecordingRequest req = TestConfig.findORR(m_priConfigFname, reqId);
                        if (null == req)
                        {
                            log("Failed to find ORR for: \"" + reqId + "\"");
                            passed = false;
                            continue;
                        }

                        Object exc = null;
                        if (expected.endsWith("Exception"))
                        {
                            exc = Class.forName(expected).newInstance();
                        }

                        String desc = null;
                        try
                        {
                            if (action.equals("cancel"))
                            {
                                desc = "cancel()";
                                req.cancel();
                            }
                            else if (action.equals("delete"))
                            {
                                desc = "delete()";
                                req.delete();
                            }
                            else if (action.equals("stop"))
                            {
                                desc = "stop()";
                                req.stop();
                            }
                            else if (action.equals("getService"))
                            {
                                desc = "getService()";
                                Service svc = req.getService();
                                log(WHOAMI + "getService(): " + svc.toString());
                            }
                            else if (action.equals("addAppData"))
                            {
                                desc = "addAppData()";
                                TestAppData appData = new TestAppData(999);
                                req.addAppData(WHOAMI + "1", appData);
                            }
                            else if (action.equals("removeAppData"))
                            {
                                desc = "removeAppData()";
                                req.removeAppData(WHOAMI + "1");
                            }
                            else if (action.equals("reschedule"))
                            {
                                desc = "reschedule()";
                                req.reschedule(req.getRecordingSpec());
                            }
                            else if (action.equals("setRecordingProperties"))
                            {
                                desc = "setRecordingProperties()";
                                req.setRecordingProperties(req.getRecordingSpec().getProperties());
                            }
                            else if (action.equals("setMediaTime"))
                            {
                                desc = "RecordedService.setMediaTime()";
                                Time setTime = new Time(10.0);
                                req.getService().setMediaTime(setTime);
                            }
                            else if (action.equals("deleteService"))
                            {
                                desc = "RecordedService.delete()";
                                req.getService().delete();
                            }
                            else
                            {
                                log("UNRECOGNIZED ACTION: " + action);
                                passed = false;
                            }
                            if (null != exc)
                            {
                                log(WHOAMI + "." + desc + " failed to throw expected Exception: " + exc.toString());
                                passed = false;
                            }
                        }
                        catch (Exception e)
                        {
                            if (null == exc)
                            {
                                log(WHOAMI + "." + desc + " threw Exception when none expected: " + e);
                                passed = false;
                            }
                            else
                            {
                                Class ec = e.getClass();
                                if (!ec.isInstance(exc))
                                {
                                    log(WHOAMI + "." + desc + " threw unexpected Exception: " + e);
                                    passed = false;
                                }
                            }
                        }
                    }
                    else
                    {
                        log("Badly-formed input line: \"" + line + "\"");
                        passed = false;
                    }
                }
            }
        }
        catch (Exception e)
        {
            log("Failed to read config file: " + e.getMessage());
            throw new IllegalStateException("Failed to read config file: " + e.getMessage());
        }
        return (passed);
    }

    private void init()
    {
        try
        {
            ArgParser xletArgs = new ArgParser((String[]) m_xctx.getXletProperty(XletContext.ARGS));
            m_configFname = xletArgs.getStringArg(CFG_ARG);
            if (null == m_configFname)
            {
                log("requires an arg that identifies the config file!!!");
                throw new NullPointerException(WHOAMI + "requires an arg that identifies the config file!!!");
            }
            m_priConfigFname = xletArgs.getStringArg(PRI_CFG_ARG);
            if (null == m_priConfigFname)
            {
                log("requires an arg that identifies the primary config file!!!");
                throw new NullPointerException(WHOAMI + "requires an arg that identifies the primary config file!!!");
            }
        }
        catch (Exception e)
        {
            log("failed to read config filename: " + e.getMessage());
            throw new IllegalStateException(WHOAMI + "failed to read config filename: " + e.getMessage());
        }

        m_mgr = (OcapRecordingManager) OcapRecordingManager.getInstance();
        if (null == m_mgr)
        {
            log("Failed to retrieve OcapRecordingManager Object ref");
            throw new NullPointerException("Failed to retrieve OcapRecordingManager Object ref");
        }
        else
        {
            log("Retrieved OcapRecordingManager Object ref: " + m_mgr);
        }
    }

    private OcapRecordingRequest findORR(String srcId, Date startTime, long durMsec)
    {
        RecordingList rlist = m_mgr.getEntries();
        int numEntries = rlist.size();
        System.out.println("Number of entries: " + numEntries);
        if (0 == numEntries)
        {
            log("RecReqXlet.findORR() retrieved empty RecordingList from RecordingManager ...");
            return (null);
        }
        for (int i = 0; i < numEntries; ++i)
        {
            OcapRecordingRequest orr = (OcapRecordingRequest) (rlist.getRecordingRequest(i));
            LocatorRecordingSpec lrs = (LocatorRecordingSpec) orr.getRecordingSpec();
            javax.tv.locator.Locator[] srcAry = lrs.getSource();
            Date sTime = lrs.getStartTime();
            long dMsec = lrs.getDuration();

            // Peform the easy checks first ...
            if ((dMsec != durMsec) || (!(srcAry[0].toString().equals(srcId))))
            {
                continue;
            }

            // Date.equals() is sensitive to milliseconds (which is
            // "in the noise" for us) which is preventing us from
            // matching Dates.

            long time1 = startTime.getTime();
            long time2 = sTime.getTime();
            long delta = time1 - time2;
            if (0 > delta)
            {
                delta *= -1;
            }

            if (1000 > delta)
            {
                return (orr);
            }
        }
        return (null);
    }

    private void log(String msg)
    {
        System.out.println(WHOAMI + msg);
        m_vbox.write(msg);
    }

    static class TestAppData implements java.io.Serializable
    {
        private int m_cnt = 0;

        public int getData()
        {
            return m_cnt;
        }

        TestAppData(int cnt)
        {
            m_cnt = cnt;
        }

        public boolean equals(Object obj)
        {
            return (m_cnt == ((TestAppData) obj).getData()) ? true : false;
        }
    }
}
