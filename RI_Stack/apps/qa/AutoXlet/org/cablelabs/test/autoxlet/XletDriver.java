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

package org.cablelabs.test.autoxlet;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Enumeration;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.StdXMLReader;
import net.n3.nanoxml.XMLParserFactory;
import net.n3.nanoxml.StdXMLBuilder;

import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsDatabase;
import org.dvb.io.ixc.IxcRegistry;
import org.havi.ui.HVisible;
import org.ocap.ui.event.OCRcEvent;

/**
 * Automated Xlet Driver -- This Xlet is used to automate the execution of
 * multiple test xlets. It parses an XML script file that loads and runs xlets
 * and sends them remote control events. It also exports a logging mechanism
 * (via IXC) to running xlets that provides a simple way to log test results and
 * debug information to standard output, local files, or remote files via UDP
 * 
 * The following Xlet arguments are used to configure the XletDriver:
 * 
 * <pre>
 *  XletDriverScript=[Path to script file]
 *       -- Specfies the location of the XML script file.
 *  
 *  DebugFile=[Path to debug file]
 *       -- Specifes the location of a local file to which the Xlet driver will write
 *       Xlet debugging log statements
 *       
 *  DebugServer=[IP or name of UDP debug log server]
 *       -- Specifies the IP address or hostname of the UDP log server to which the Xlet
 *       driver will write Xlet debugging log statements.  If DebugFile argument is
 *       present, this argument is ignored.
 *       
 *  DebugPort=[Port]
 *       -- Specifies the port number of the UDP log server to which the Xlet driver
 *       will write Xlet debugging log statements.  If DebugFile argument is present,
 *       this argument is ignored.  If DebugServer argument is present, this argument
 *       must be present. 
 *  
 *  ResultsFile=[Path to results file]
 *       -- Specifes the location of a local file to which the Xlet driver will write
 *       Xlet test results statements
 *       
 *  ResultsServer=[IP or name of UDP results server]
 *       -- Specifies the IP address or hostname of the UDP log server to which the Xlet
 *       driver will write Xlet test results statements.  If ResultsFile argument is
 *       present, this argument is ignored.
 *       
 *  ResultsPort=[Port]
 *       -- Specifies the port number of the UDP log server to which the Xlet driver
 *       will write Xlet test results statements.  If ResultsFile argument is present,
 *       this argument is ignored.  If ResultsServer argument is present, this argument
 *       must be present.
 * </pre>
 * 
 * It is very important to always signal the XletDriver application with the
 * following AppID and OrgID values. The AutoXletClient object assumes that
 * XletDriver has a particular set of IDs so that it can establish communication
 * via IXC.
 * 
 * XletDriver: OrgID = 0x1 AppID = 0x7000
 * 
 * @author Greg Rutz
 */
public class XletDriver implements Xlet, AppStateChangeEventListener
{

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#initXlet(javax.tv.xlet.XletContext)
     */
    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {
        String argValue;
        ArgParser ap = null;

        m_ctx = xletContext;

        // get class of OCRcEvent to obtain keycodes
        try
        {
            m_ocRcEventClass = Class.forName("org.ocap.ui.event.OCRcEvent");
        }
        catch (ClassNotFoundException e1)
        {
            e1.printStackTrace();
        }

        // Create arg parser object
        try
        {
            ap = new ArgParser((String[]) (xletContext.getXletProperty(XletContext.ARGS)));

            m_configFile = ap.getStringArg("config_file");
        }
        catch (Exception e)
        {
            throw new XletStateChangeException("XletDriver: Could not initialize argument parser! -- " + e.getMessage());
        }

        String persistentRoot = System.getProperty("dvb.persistent.root");
        String oid = (String) m_ctx.getXletProperty("dvb.org.id");
        String aid = (String) m_ctx.getXletProperty("dvb.app.id");
        String outputFilePath = persistentRoot + "/" + oid + "/" + aid + "/";

        try
        {
            FileInputStream fis = new FileInputStream(m_configFile);
            ArgParser fopts = new ArgParser(fis);
            fis.close();

            // Debug logging
            if ((argValue = fopts.getStringArg("DebugFile")) != null)
            {
                try
                {
                    m_dLog = new XletLogger(outputFilePath + argValue);
                }
                catch (IOException e)
                {
                    throw new XletStateChangeException("Could not initialize Xlet Debug Logger (File = " + argValue
                            + ")");
                }
            }
            else if ((argValue = fopts.getStringArg("DebugServer")) != null)
            {
                Integer port = null;
                if ((port = fopts.getIntegerArg("DebugPort")) != null)
                {
                    try
                    {
                        m_dLog = new XletLogger(argValue, port.intValue());
                    }
                    catch (Exception e)
                    {
                        throw new XletStateChangeException("Could not initialize UDP Xlet Debug Logger (Server = "
                                + argValue + ", Port = " + port + ")");
                    }
                }
                else
                {
                    throw new XletStateChangeException("Must specify DebugPort in order to use DebugServer.");
                }
            }
            else
            {
                System.out.println("XletDriver:  Xlet debugging will be written to STDOUT.");
                m_dLog = new XletLogger();
            }

            // Results logging
            if ((argValue = fopts.getStringArg("ResultsFile")) != null)
            {
                try
                {
                    m_rLog = new XletLogger(outputFilePath + argValue);
                }
                catch (IOException e)
                {
                    throw new XletStateChangeException("Could not initialize Xlet Results Logger (File = " + argValue
                            + ")");
                }
            }
            else if ((argValue = fopts.getStringArg("ResultsServer")) != null)
            {
                Integer port = null;
                if ((port = fopts.getIntegerArg("ResultsPort")) != null)
                {
                    try
                    {
                        m_rLog = new XletLogger(argValue, port.intValue());
                    }
                    catch (Exception e)
                    {
                        System.out.println("Could not initialize UDP Xlet Results Logger (Server = " + argValue
                                + ", Port = " + port + ")");
                    }
                }
                else
                {
                    throw new XletStateChangeException("Must specify ResultsPort in order to use ResultsServer.");
                }
            }
            else
            {
                System.out.println("XletDriver:  Test results will be written to STDOUT.");
                m_rLog = new XletLogger();
            }

            // XletDriver script file
            if ((argValue = fopts.getStringArg("XletDriverScript")) != null)
            {
                // Get XML and parse it.
                try
                {
                    // workaround post removal of ALL_PERMISSION backdoor
                    // m_parser = XMLParserFactory.createDefaultXMLParser();
                    m_parser = XMLParserFactory.createXMLParser("net.n3.nanoxml.StdXMLParser", new StdXMLBuilder());
                    m_parser.setReader(StdXMLReader.fileReader(argValue));
                    m_rootElt = (IXMLElement) m_parser.parse();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    throw new XletStateChangeException("XletDriver: Error parsing xml script.  Caused by: " + e);
                }
            }
            else
            {
                throw new XletStateChangeException("XletDriver:  XML script file not provided!");
            }
        }
        catch (Exception e)
        {
            throw new XletStateChangeException("XletDriver: error reading config file: " + e.getMessage());
        }

        // Get reference to apps database
        m_adb = AppsDatabase.getAppsDatabase();

        // Export the debug logging object so that the test Xlets can log to it
        try
        {
            IxcRegistry.bind(xletContext, "IXCLogger", m_dLog);
        }
        catch (AlreadyBoundException e)
        {
            throw new XletStateChangeException("XletDriver:  Could not expose debug logger via IXC! -- "
                    + e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        int numXlets = m_rootElt.getChildrenCount();

        m_dLog.log(LOG_HEADER);
        m_dLog.log("Starting to run script with " + numXlets + " Xlets.");
        m_dLog.log(LOG_FOOTER);

        Enumeration xlets = m_rootElt.enumerateChildren();

        // Handle AutoXlet children
        while (xlets.hasMoreElements())
        {
            IXMLElement elem = (IXMLElement) xlets.nextElement();

            if (elem.getName().equals(XLET_ELEM)) doXlet(elem);
        }

        m_dLog.log(LOG_HEADER);
        m_dLog.log("# Finished running script with " + numXlets + " Xlets.");
        m_dLog.log(LOG_FOOTER);

        m_dLog.close();
        m_rLog.close();
        System.exit(0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {

    }

    /**
     * This method may be called by the stack to kill the Xlet. This method in
     * turn destroys the Xlet (if any) that is currently being run by the
     * XletDriver.
     * 
     * @param unconditional
     *            Indicates whether the Xlet MUST stop, or is being asked
     *            politely to stop.
     * @throws XletStateChangeException
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        int s = m_ap.getState();

        // Kill the Xlet beig tested if running or paused.
        if (s == AppProxy.PAUSED || s == AppProxy.STARTED)
        {
            m_ap.stop(true);
        }
    }

    /**
     * This method is called by the stack when Xlets being driven by XletDriver
     * change their application states. XletDriver will register to receive
     * AppStateChangeEvent notifications for each Xlet that is is running.
     * XletDriver uses a monitor to synchronously start and stop the Xlets that
     * are running. When the state change event is received in this method (by a
     * stack thread), the monitor is "notified", which allows the waiting
     * XletDriver main thread to continue execution.
     * 
     * @param asce
     *            The AppStateChangeEvent from the OCAP stack.
     * @see org.dvb.application.AppStateChangeEventListener#stateChange(org.dvb.application.AppStateChangeEvent)
     */
    public void stateChange(AppStateChangeEvent asce)
    {
        // get the AppID of the event
        AppID src = asce.getAppID();

        /*
         * Check to see that the Xlet was successfully started. If so, wake up
         * the monitor in order to lookup the Xlet's Driveable and TestCollector
         * interfaces.
         */
        if (src.equals(m_curAppID) && asce.getToState() == AppProxy.STARTED)
        {
            m_dLog.log(LOG_HEADER);
            m_dLog.log("Xlet \"" + m_curXletName + "\" started.");
            m_dLog.log(LOG_FOOTER);
            m_monitor.notifyReady();
        }

        /*
         * Check to see that the Xlet was successfully destroyed and unloaded.
         * If so, wake up the monitor in order to continue to the next Xlet (if
         * any).
         */
        if (src.equals(m_curAppID) && asce.getToState() == AppProxy.NOT_LOADED)
        {
            m_dLog.log(LOG_HEADER);
            m_dLog.log("Xlet \"" + m_curXletName + "\" stopped (destroyed).");
            m_dLog.log(LOG_FOOTER);
            m_monitor.notifyReady();
        }

    }

    /**
     * This private method handles an Xlet element within the XML Xlet
     * automation script. The attributes of the Xlet tag are read in, which
     * specify the orgId and appID of the Xlet, and control the behavior of this
     * method when running the Xlet.
     * <p>
     * This method obtains an AppProxy representing the Xlet, and then starts
     * the Xlet. Once the Xlet is started, the Driveable and TestCollector
     * interfaces exposed by the Xlet via IXC are obtained. The RCEvent tags
     * which are child elements are processed individually. Finally, the
     * TestResult is retrieved via the TestCollector interface, and the Xlet is
     * destroyed.
     * 
     * @param element
     *            The object representing the Xlet tag from the XML document.
     */
    private void doXlet(IXMLElement xlet)
    {
        String tmp;
        int pauseAfterLast = -1;
        int startupTimeout = 900000;
        int o = 0;
        int a = 0;

        // get Xlet name attribute
        m_curXletName = xlet.getAttribute(XLET_NAME_ATTR, null);

        // get orgID, appID, pause attributes
        try
        {
            tmp = xlet.getAttribute(XLET_ORGID_ATTR, null);
            o = Integer.parseInt(tmp.substring(2), 16);
            tmp = xlet.getAttribute(XLET_APPID_ATTR, null);
            a = Integer.parseInt(tmp.substring(2), 16);
        }
        catch (NumberFormatException e)
        {
            m_dLog.log(LOG_ERROR_HEADER);
            m_dLog.log("Error obtaining appID and/or orgID for Xlet " + m_curXletName + ".  Skipping Xlet.");
            m_dLog.log(e);
            m_dLog.log(LOG_ERROR_FOOTER);
            return;
        }

        // create new AppID, get app proxy
        m_curAppID = new AppID(o, a);
        m_ap = m_adb.getAppProxy(m_curAppID);

        // Ensure that AppID actually refers to a signaled app
        if (m_ap == null)
        {
            m_dLog.log(LOG_ERROR_HEADER);
            m_dLog.log("The Xlet (" + m_curXletName + ") is not signalled with orgID = " + Integer.toHexString(o)
                    + ", appId = " + Integer.toHexString(a));
            m_dLog.log(LOG_ERROR_FOOTER);
            return;
        }

        // add listener for application state changes
        m_ap.addAppStateChangeEventListener(this);

        // pauseAfterLast attribute
        try
        {
            tmp = xlet.getAttribute(XLET_PAUSEAFTERLAST_ATTR, "-1");
            pauseAfterLast = Integer.parseInt(tmp);
        }
        catch (NumberFormatException e)
        {
            pauseAfterLast = -1;
            m_dLog.log(LOG_ERROR_HEADER);
            m_dLog.log("The Xlet (" + m_curXletName + ") has invalid pauseAfterLast attribute (" + tmp
                    + ") using default of = " + pauseAfterLast);
            m_dLog.log(LOG_ERROR_FOOTER);
        }

        // app startup timeout
        try
        {
            tmp = xlet.getAttribute(XLET_STARTUP_TIMEOUT_ATTR, "300000");
            startupTimeout = Integer.parseInt(tmp);
        }
        catch (NumberFormatException e)
        {
            m_dLog.log(LOG_ERROR_HEADER);
            m_dLog.log("The Xlet (" + m_curXletName + ") has invalid startupTimeout attribute (" + tmp
                    + ") using default of = " + startupTimeout);
            m_dLog.log(LOG_ERROR_FOOTER);
        }
        m_monitor.setTimeout(startupTimeout);

        m_dLog.log(LOG_HEADER);
        m_dLog.log("Starting xlet \"" + m_curXletName + "\", orgID=0x" + Integer.toHexString(o) + ", appID=0x"
                + Integer.toHexString(a) + ((pauseAfterLast >= 0) ? ", pauseAfterLast=" + pauseAfterLast : "")
                + ", startupTimeout=" + startupTimeout);
        m_dLog.log(LOG_FOOTER);

        /*
         * Use monitor to make starting the Xlet synchronous. Call the start
         * method, and then wait in the monitor to be notified when the xlet has
         * entered the AppProxy.STARTED state. When this happens, the monitor is
         * notified within the stateChange method.
         */
        synchronized (m_monitor)
        {
            m_ap.start();
            m_monitor.waitForReady();
        }

        // make sure the xlet actually started.
        if (m_ap.getState() != AppProxy.STARTED)
        {
            m_dLog.log(LOG_ERROR_HEADER);
            m_dLog.log("Xlet \"" + m_curXletName + "\" failed to start.  Skipping to next Xlet.");
            m_dLog.log(LOG_ERROR_FOOTER);
            m_ap.removeAppStateChangeEventListener(this);
            m_ap.stop(true);
            return;
        }

        /*
         * Look up the Drivable and TestCollector objects the Xlet has exposed
         * via IXC.
         */
        try
        {
            tmp = "/" + Integer.toHexString(o) + "/" + Integer.toHexString(a) + "/";
            m_driveable = (Driveable) IxcRegistry.lookup(m_ctx, tmp + "Driveable");
            m_collector = (TestCollector) IxcRegistry.lookup(m_ctx, tmp + "TestCollector");
        }
        catch (Exception e2)
        {
            m_dLog.log(LOG_ERROR_HEADER);
            m_dLog.log("Error looking up IXC interfaces.  Skipping to next Xlet.");
            m_dLog.log(e2);
            m_dLog.log(LOG_ERROR_FOOTER);
            m_ap.removeAppStateChangeEventListener(this);
            m_ap.stop(true);
            return;
        }

        /*
         * This loop handles the sub-tags within the Xlet
         */
        Enumeration elems = xlet.enumerateChildren();
        while (elems.hasMoreElements())
        {
            IXMLElement elem = (IXMLElement) elems.nextElement();

            // RCEvent
            if (elem.getName().equals(RCEVENT_ELEM))
            {
                doRCEvent(elem);
            }
            // Repeat
            else if (elem.getName().equals(REPEAT_ELEM))
            {
                // Get count attribute
                tmp = elem.getAttribute(REPEAT_COUNT_ATTR, null);
                int count = Integer.parseInt(tmp);

                // Loop over the children executing each RCEvent
                for (int i = 0; i < count; ++i)
                {
                    Enumeration repeatEvents = elem.enumerateChildren();

                    while (repeatEvents.hasMoreElements())
                    {
                        doRCEvent((IXMLElement) repeatEvents.nextElement());
                    }
                }
            }
        }

        /*
         * Everything below this point is cleaning up after running an Xlet.
         */
        if (pauseAfterLast > 0)
        {
            m_dLog.log(LOG_HEADER);
            m_dLog.log("Pausing " + pauseAfterLast + " ms after Xlet " + m_curXletName);
            m_dLog.log(LOG_FOOTER);
            try
            {
                Thread.sleep(pauseAfterLast);
            }
            catch (InterruptedException e1)
            {
                m_dLog.log(LOG_ERROR_HEADER);
                m_dLog.log("Unexpected exception during pauseAfterLast thread sleep: " + e1);
                m_dLog.log(e1);
                m_dLog.log(LOG_ERROR_FOOTER);
            }
        }

        /*
         * Write the results header for this Xlet.
         */
        m_rLog.log(LOG_HEADER);
        m_rLog.log("# Begin Xlet test results:" + "\n#\tname=" + m_curXletName + "\n#\torgID=0x"
                + Integer.toHexString(o) + "\n#\tappID=0x" + Integer.toHexString(a));
        m_rLog.log(LOG_FOOTER);

        // retrieve and log and log test results from Xlet
        TestResult result = null;
        try
        {
            result = m_collector.getTestResults();

            m_rLog.log("\nTest results at end of Xlet execution:\n"
                    + "-----------------------------------------------------------------------");

            if (result.runCount() > 0)
            {
                m_rLog.log(result);
                m_rLog.log("\n");
                m_collector.clearTestResults();
            }
            else
            {
                m_rLog.log("No test results available -- possibly all have been logged already.\n");
            }
        }
        catch (RemoteException e)
        {
            m_dLog.log(e);
        }

        /*
         * Use monitor to synchronously stop the xlet. This is done to make sure
         * that any resources in use by the xlet will be released before the
         * next xlet is started. First call the stop method, then wait in the
         * monitor to be notified when the xlet has entered the
         * AppProxy.NOT_LOADED state (the xlet is destroyed, then unloaded).
         * When this happens, the monitor is notified within the stateChange
         * mehod.
         */
        synchronized (m_monitor)
        {
            m_ap.stop(true);
            m_monitor.waitForReady();
        }

        // remove the state change listener
        m_ap.removeAppStateChangeEventListener(this);

        tmp = "# Finished \"" + m_curXletName + "\", orgID=0x" + Integer.toHexString(o) + ", appID=0x"
                + Integer.toHexString(a) + ((pauseAfterLast >= 0) ? ", pauseAfterLast=" + pauseAfterLast : "");
        m_dLog.log(LOG_HEADER);
        m_dLog.log(tmp);
        m_dLog.log(LOG_FOOTER);
        m_rLog.log(LOG_HEADER);
        m_rLog.log(tmp);
        m_rLog.log(LOG_FOOTER);
    }

    /**
     * This private method handles an RCEvent element for a specific Xlet within
     * the XML Xlet automation script. Each RCEvent element within the script
     * simulates a remote control key press submitted to the Xlet.
     * <p>
     * The attributes of the RCEvent element are read in, which specify the
     * behavior of this method when passing the event to the Xlet. The content
     * of the RCEvent element is obtained, which represents the key event to be
     * sent to the Xlet. These key events are defined as constants in the
     * OCRcEvent class and its superclasses. For example, VK_CHANNEL_UP is the
     * channel up key on the remote control.
     * 
     * @param event
     *            The object representing the RCEvent tag from the XML document.
     */
    private void doRCEvent(IXMLElement event)
    {
        String tmp;
        String evtName;
        String evtKeyName;
        Field evtKeyField;
        boolean getResultsAfter;
        int keyCode = 0;
        int pauseBeforeNext = -1;
        int monitorTimeout = -1;
        OCRcEvent ocrcEvt;

        // get the attributes
        evtName = event.getAttribute(RCEVENT_NAME_ATTR, null);
        tmp = event.getAttribute(RCEVENT_GETRESULTSAFTER_ATTR, "false");
        getResultsAfter = Boolean.valueOf(tmp).booleanValue();

        try
        {
            tmp = event.getAttribute(RCEVENT_PAUSEBEFORENEXT_ATTR, "-1");
            pauseBeforeNext = Integer.parseInt(tmp);
            tmp = event.getAttribute(RCEVENT_MONITORTIMEOUT_ATTR, "-1");
            monitorTimeout = Integer.parseInt(tmp);
        }
        catch (NumberFormatException e)
        {
            m_dLog.log(LOG_ERROR_HEADER);
            m_dLog.log("Error loading attributes for RCEvent " + (evtName != null ? evtName + " " : "") + "in Xlet "
                    + m_curXletName);
            m_dLog.log(e);
            m_dLog.log(LOG_ERROR_FOOTER);
            return;
        }

        /*
         * Get the key code string, which is the content of the RCEvent element.
         * Use reflection to convert the key code string to the key code.
         */
        try
        {
            evtKeyName = event.getContent().trim();
            evtKeyField = m_ocRcEventClass.getField(evtKeyName);
            keyCode = evtKeyField.getInt(m_ocRcEventClass);
        }
        catch (Exception e1)
        {
            m_dLog.log(LOG_ERROR_HEADER);
            m_dLog.log("Error obtaining key code from RCEvent element.");
            m_dLog.log(e1);
            m_dLog.log(LOG_ERROR_FOOTER);
            return;
        }

        /*
         * Create a new OCRcEvent from the keycode. The first parameter of this
         * constructor must be a subclass of java.awt.Component, so a dummy
         * HVisible object is used.
         */
        ocrcEvt = new OCRcEvent(m_hvisible, keyCode, System.currentTimeMillis(), 0, keyCode, Character.forDigit(
                keyCode, 10));

        /*
         * Pass the event to the Xlet. Specify to use monitor if monitorTimeout
         * >= 0
         */
        try
        {
            m_driveable.dispatchEvent(ocrcEvt, (monitorTimeout >= 0 ? true : false), monitorTimeout);
            tmp = "Sent RCEvent:" + "\n\tname=" + evtName + "\n\tkeyCode=" + keyCode + "(" + evtKeyName + ")"
                    + "\n\tuseMonitor=" + (monitorTimeout >= 0 ? true : false) + "\n\tmonitorTimeout=" + monitorTimeout
                    + (pauseBeforeNext > 0 ? "\n\tpauseBeforeNext=" + pauseBeforeNext : "")
                    + (getResultsAfter ? "\n\tgetResultsAfter=true" : "");
            m_dLog.log(LOG_HEADER);
            m_dLog.log(tmp);
            m_dLog.log(LOG_FOOTER);
            m_rLog.log(LOG_HEADER);
            m_rLog.log(tmp);
            m_rLog.log(LOG_FOOTER);
        }
        catch (RemoteException e2)
        {
            m_dLog.log(LOG_ERROR_HEADER);
            m_dLog.log(e2);
            m_dLog.log(LOG_ERROR_FOOTER);
        }

        /*
         * Pause if necessary before next event.
         */
        if (pauseBeforeNext > 0)
        {
            m_dLog.log(LOG_HEADER);
            m_dLog.log("Pausing " + pauseBeforeNext + " ms after RCEvent " + (evtName != null ? evtName : ""));
            m_dLog.log(LOG_FOOTER);
            try
            {
                Thread.sleep(pauseBeforeNext);
            }
            catch (InterruptedException e1)
            {
                m_dLog.log(LOG_ERROR_HEADER);
                m_dLog.log("Unexpected exception during pauseBeforeNext thread sleep: " + e1);
                m_dLog.log(e1);
                m_dLog.log(LOG_ERROR_HEADER);
            }
        }

        /*
         * Retrieve the test results if getResultsAfter attribute is specified
         * as "true" in the RCEvent tag.
         */
        if (getResultsAfter)
        {
            // retrieve and log and log test results from Xlet
            TestResult result;
            try
            {
                result = m_collector.getTestResults();

                if (result.runCount() > 0)
                {
                    // print a header for this set of test results
                    m_rLog.log("\n\nTest results after RCEvent \"" + evtName + "\"" + "\n\tkeyCode=" + keyCode + "("
                            + evtKeyName + ")"
                            + "\n-----------------------------------------------------------------------");

                    m_rLog.log(result);
                    m_rLog.log("\n\n\n");
                    m_collector.clearTestResults();
                }
                else
                {
                    m_rLog.log("No test results available -- possibly all have been logged already.\n");
                }
            }
            catch (RemoteException e)
            {
                m_dLog.log(LOG_ERROR_HEADER);
                m_dLog.log(e);
                m_dLog.log(LOG_ERROR_FOOTER);
            }
        }
    }

    // static string constants for XML element and attribute names
    public static final String XLET_ELEM = "Xlet";

    public static final String XLET_NAME_ATTR = "name";

    public static final String XLET_ORGID_ATTR = "orgID";

    public static final String XLET_APPID_ATTR = "appID";

    public static final String XLET_STARTUP_TIMEOUT_ATTR = "startupTimeout";

    public static final String XLET_PAUSEAFTERLAST_ATTR = "pauseAfterLast";

    public static final String RCEVENT_ELEM = "RCEvent";

    public static final String RCEVENT_NAME_ATTR = "name";

    public static final String RCEVENT_PAUSEBEFORENEXT_ATTR = "pauseBeforeNext";

    public static final String RCEVENT_MONITORTIMEOUT_ATTR = "monitorTimeout";

    public static final String RCEVENT_GETRESULTSAFTER_ATTR = "getResultsAfter";

    public static final String REPEAT_ELEM = "Repeat";

    public static final String REPEAT_COUNT_ATTR = "count";

    // Logging pretty-print
    private static final String LOG_HEADER = "\n################\n" + "## XletDriver ##\n"
            + "################################################################################";

    private static final String LOG_FOOTER = "################################################################################\n";

    private static final String LOG_ERROR_HEADER = "\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
            + "!! XletDriver --  < < < ERROR > > > !!\n"
            + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!";

    private static final String LOG_ERROR_FOOTER = "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n";

    // XML parser
    private IXMLParser m_parser;

    // Root element of XML document
    private IXMLElement m_rootElt;

    // Apps database used to control xlets
    private AppsDatabase m_adb;

    // App proxy used to start and stop xlet
    private AppProxy m_ap;

    // AppID set to current xlet
    private AppID m_curAppID;

    // Name of current xlet (from XML Xlet tag)
    private String m_curXletName;

    // IXC Driveable interface of Xlet
    private Driveable m_driveable;

    // IXC TestCollector interface of Xlet
    private TestCollector m_collector = null;

    // OCRcEvent class reference to obtain keycodes
    private Class m_ocRcEventClass;

    // Dummy HVisible needed to create OCRcEvent to pass into xlet
    private HVisible m_hvisible = new HVisible();

    // Monitor used to synchronously start and stop xlets
    private Monitor m_monitor = new Monitor();

    // debug and results loggers
    private XletLogger m_dLog = null;

    private XletLogger m_rLog = null;

    // The XletContext of this xlet
    private XletContext m_ctx = null;

    private String m_configFile;
}
