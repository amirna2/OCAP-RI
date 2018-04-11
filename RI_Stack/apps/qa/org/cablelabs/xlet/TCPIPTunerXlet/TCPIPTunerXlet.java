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
 * TCPIPTunerXlet
 *
 */
package org.cablelabs.xlet.TCPIPTunerXlet;

import javax.tv.locator.Locator;
import javax.tv.service.Service;
import javax.tv.service.selection.PresentationChangedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractServiceType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import org.ocap.application.AppManagerProxy;

import org.cablelabs.lib.utils.ArgParser;

public class TCPIPTunerXlet implements Xlet, Runnable, ServiceContextListener, SIRequestor
{

    protected XletContext m_ctx = null;

    protected Thread th = null;

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#initXlet(javax.tv.xlet.XletContext)
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        this.m_ctx = ctx;
        m_portnum = DFLT_PORTNUM;
        m_tunesec = DFLT_TUNESEC;
        m_dwellmsec = DFLT_DWELLMSEC;
        m_mode = DFLT_MODE;
        m_useNetwork = false;

        try
        {
            ArgParser xletArgs = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));
            m_portnum = xletArgs.getIntArg("portnum");

            String modeStr = xletArgs.getStringArg("mode");
            System.out.println(m_strPrefix + "Configured mode: " + modeStr);
            if (modeStr.equals("normal"))
            {
                m_mode = MODE_NORMAL;
                m_useNetwork = true;
            }
            else if (modeStr.equals("canned") || (modeStr.equals("randomCanned")))
            {
                m_mode = MODE_CANNED;
                if (modeStr.equals("randomCanned"))
                {
                    m_random = true;
                    m_randomGen = new Random(new Date().getTime());
                }
                String locators = xletArgs.getStringArg("locators");
                if (null != locators)
                {
                    buildCannedList(locators);
                }
                m_cannedNdx = 0;
                m_maxCannedNdx = m_cannedLocators.size() - 1;
            }
            else if (modeStr.equals("test"))
            {
                m_mode = MODE_TEST;
                m_useNetwork = true;
            }
            else
            {
                System.out.println(m_strPrefix + "Defaulting to normal mode ...");
                m_mode = MODE_NORMAL;
                m_useNetwork = true;
            }
            m_tunesec = xletArgs.getIntArg("tunesec");
            m_dwellmsec = xletArgs.getIntArg("dwellmsec");
        }
        catch (Exception e)
        {
        }
        System.out.println(m_strPrefix + " port number: " + m_portnum);
        System.out.println(m_strPrefix + "tune seconds: " + m_tunesec);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        // Start monitor thread
        th = new Thread(this);
        th.setName("DefaultMonitor");
        th.start();
        if (debugOn) System.out.println(m_strPrefix + "startXlet finished.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        if (null != m_srvSock)
        {
            try
            {
                m_srvSock.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean force) throws XletStateChangeException
    {
        // tell the monitor thread to close
        finish = true;
        synchronized (this)
        {
            this.notify();
        }
        if (th != null)
        {
            try
            {
                th.join();
            }
            catch (InterruptedException e)
            {
                // e.printStackTrace();
            }
        }
    }

    public void run()
    {

        ServiceContextFactory factory = ServiceContextFactory.getInstance();
        try
        {
            m_svcCtx = factory.createServiceContext();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (m_svcCtx != null)
        {
            m_svcCtx.addListener(this);
        }

        try
        {
            if (m_useNetwork)
            {
                m_srvSock = new ServerSocket(m_portnum, 0, InetAddress.getLocalHost());
                System.out.println(m_strPrefix + "created ServerSocket ...");
            }
            else
            {
                System.out.println(m_strPrefix + "using canned service identifiers ...");
            }

            while (!finish)
            {
                Socket cliSock = null;
                BufferedWriter out = null;
                BufferedReader in = null;

                String svcStr = "";
                if (m_useNetwork)
                {
                    System.out.println(m_strPrefix + "Waiting for client connection ...");
                    cliSock = m_srvSock.accept();
                    System.out.println(m_strPrefix + "accepted client connection ...");
                    in = new BufferedReader(new InputStreamReader(cliSock.getInputStream()));
                    out = new BufferedWriter(new OutputStreamWriter(cliSock.getOutputStream()));
                    // 20050503 - ASSUME STRING REPRESENTATION OF OcapLocator
                    // ...
                    svcStr = in.readLine();
                    System.out.println(m_strPrefix + "read service identifier from network: " + svcStr);
                }
                else
                {
                    svcStr = getCannedSourceId(m_random);
                    System.out.println(m_strPrefix + "read canned service identifier: " + svcStr);
                }

                if (!svcStr.startsWith("ocap://"))
                {
                    System.err.println(m_strPrefix + "IGNORING INVALID SERVICE IDENTIFIER: " + svcStr);
                    cliSock.close();
                    continue;
                }

                if (!(MODE_TEST == m_mode))
                {
                    boolean retval = doSvcSelect(svcStr);
                    if (true == retval)
                    {
                        System.out.println(m_strPrefix + "doSvcSelect() succeeded ...");
                        if (null != out)
                        {
                            out.write(m_strPrefix + "Service Selection " + svcStr + " SUCCESS");
                        }
                    }
                    else
                    {
                        System.out.println(m_strPrefix + "doSvcSelect() FAILED ...");
                        if (null != out)
                        {
                            out.write(m_strPrefix + "Service Selection " + svcStr + " FAIL");
                        }
                    }
                }
                else
                {
                    System.out.println(m_strPrefix + "TEST MODE - no service-selection attempted ...");
                    if (null != out)
                    {
                        out.write(m_strPrefix + "Service Selection " + svcStr + " SUCCESS");
                    }
                }

                if (null != out)
                {
                    out.newLine();
                    out.flush();
                }

                if (null != cliSock)
                {
                    cliSock.close();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            // wait until someone shuts us down
            while (!finish)
            {
                try
                {
                    synchronized (this)
                    {
                        this.wait();
                    }
                }
                catch (InterruptedException e)
                {
                    // do nothing
                }
            }
        }
    }

    synchronized private SIRequestFailureType getFailureType()
    {
        return (m_ftype);
    }

    synchronized private void setFailureType(SIRequestFailureType ftype)
    {
        System.out.println(m_strPrefix + "setFailureType(" + ftype + ")");
        m_ftype = ftype;
    }

    synchronized private boolean getSvcSelOkay()
    {
        return (m_svcSelOkay);
    }

    synchronized private void setSvcSelOkay(boolean value)
    {
        System.out.println(m_strPrefix + "setSvcSelOkay(" + value + ")");
        m_svcSelOkay = value;
    }

    private boolean doSvcSelect(String svcStr) throws Exception
    {
        boolean status = true;
        Locator[] loc = new Locator[] { new OcapLocator(svcStr) };

        setSvcSelOkay(false);
        setFailureType(null);

        // TODO - add code to guard against select()'ing while
        // still in PRESENTING state ...

        // brute force
        m_svcCtx.stop();

        m_svcCtx.select(loc);

        int cnt = 0;
        while ((cnt < m_tunesec) && (null == getFailureType()) && !getSvcSelOkay())
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (Exception e)
            {
            }
            ++cnt;
        }
        if ((null != getFailureType()) || (!getSvcSelOkay()))
        {
            System.out.println(m_strPrefix + "Service Selection did not succeed within " + cnt + " seconds ...");
            System.out.println(m_strPrefix + "SELECT " + svcStr + " FAILED");
            System.out.println(m_strPrefix + "getFailureType(): " + getFailureType());
            System.out.println(m_strPrefix + "getSvcSelOkay(): " + getSvcSelOkay());
            status = false;
        }
        else
        {
            System.out.println(m_strPrefix + "SELECT " + svcStr + " SUCCEEDED");
            try
            {
                Thread.sleep(m_dwellmsec);
            }
            catch (Exception e)
            {
            }
            // Prepare for the next service-selection ...
            m_svcCtx.stop();
        }
        return (status);
    }

    // implementation of SIRequestor iface
    public synchronized void notifyFailure(SIRequestFailureType ftype)
    {
        System.err.println(m_strPrefix + "notifyFailure() ftype: " + ftype);
        setFailureType(ftype);
        this.notifyAll();
    }

    public synchronized void notifySuccess(SIRetrievable[] result)
    {
        System.err.println(m_strPrefix + "notifySuccess() ...");
        if (result != null && result.length > 0 && result[0] != null)
        {
            System.err.println(m_strPrefix + "notifySuccess() result.length: " + result.length);
            m_table = result[0];
        }
        this.notifyAll();
    }

    private String getCannedSourceId(boolean random)
    {
        String svcStr = "";

        if (random)
        {
            m_cannedNdx = (int) ((m_maxCannedNdx) * m_randomGen.nextDouble());
            svcStr = (String) m_cannedLocators.elementAt(m_cannedNdx);
            System.out.println(m_strPrefix + "getCannedSourceId(random) m_cannedNdx: " + m_cannedNdx);
        }
        else
        {
            svcStr = (String) m_cannedLocators.elementAt(m_cannedNdx);
            System.out.println(m_strPrefix + "getCannedSourceId() m_cannedNdx: " + m_cannedNdx);
            if (m_cannedNdx >= m_maxCannedNdx)
            {
                m_cannedNdx = 0;
            }
            else
            {
                m_cannedNdx++;
            }
        }
        return svcStr;
    }

    // implementation of ServiceContextListener iface
    public void receiveServiceContextEvent(ServiceContextEvent e)
    {
        if (e instanceof PresentationChangedEvent)
        {
            setSvcSelOkay(true);
            System.out.println(m_strPrefix + "receiveServiceContextEvent() service selection succeeded.");
        }
        else
        {
            setSvcSelOkay(false);
            System.out.println(m_strPrefix + "receiveServiceContextEvent() service selection failed.");
        }
    }

    private void buildCannedList(String locators)
    {
        StringTokenizer st = new StringTokenizer(locators, " ");
        m_cannedLocators = new Vector();
        while (st.hasMoreTokens())
        {
            m_cannedLocators.addElement(st.nextToken());
        }
    }

    private static boolean finish = false;

    // debug aiding stuff
    private static final String m_strPrefix = "[TCPIPTunerXlet]: ";

    private boolean debugOn = true;

    private SIRequestFailureType m_ftype = null;

    private boolean m_svcSelOkay = false;

    private ServiceContext m_svcCtx = null;

    private Object m_table = null;

    private static final int MODE_NORMAL = 1;

    private static final int MODE_CANNED = 2;

    private static final int MODE_TEST = 3;

    private static final int DFLT_PORTNUM = 8888;

    private static final int DFLT_TUNESEC = 30;

    private static final int DFLT_DWELLMSEC = 10000;

    private static final int DFLT_MODE = MODE_CANNED;

    private int m_portnum = DFLT_PORTNUM;

    private int m_tunesec = DFLT_TUNESEC;

    private int m_dwellmsec = DFLT_DWELLMSEC;

    private ServerSocket m_srvSock;

    private int m_cannedNdx;

    private int m_maxCannedNdx;

    private boolean m_useNetwork = true;

    private boolean m_random = false;

    private int m_mode = MODE_NORMAL;

    private Random m_randomGen;

    private Vector m_cannedLocators;
}
