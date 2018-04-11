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

// Declare package.
package org.cablelabs.xlet.RiScriptlet;

// Import Personal Java packages.
import javax.tv.xlet.*;

import java.util.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import bsh.Interpreter;
import bsh.EvalError;

import org.apache.log4j.Logger;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.OcapAppDriverInterfaceCore;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverInterfaceDVR;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverInterfaceHN;
import org.cablelabs.lib.utils.oad.hndvr.OcapAppDriverHNDVR;
import org.cablelabs.lib.utils.oad.hndvr.OcapAppDriverInterfaceHNDVR;


/**
 * The class RiScriptlet runs a BeanShell script provided by either a command line arg
 * or over a telnet interface.
 * 
 * @author Steve Arendt
 */
public class RiScriptlet implements Xlet
{
    private static final Logger m_log = Logger.getLogger(RiScriptlet.class);

    private static String m_strChannelFile;

    private static final String USE_JAVA_TV = "use_javatv_channel_map";

    private static final String CONFIG_FILE = "config_file";
    
    private static boolean m_useJavaTVChannelMap;
    private XletContext m_ctx;

    private String[] m_scriptFilesFromArgs = null;
    private boolean[] m_scriptFilesFromArgsComplete = null;
    private String[] m_scriptFileResults = null;
    private boolean m_writeCompletionFlagFile = false;

    private boolean m_isSyncServer = false;
    private String m_syncServerAddr = null;
    private int m_syncServerPort = 0;

    private int m_ScriptRunID = 0;
    private Map m_mapScriptStatus = Collections.synchronizedMap(new HashMap());  

    private RiScriptletTelnetInterface m_telnetInterface = null;
    private RiScriptletSyncServer m_syncServer = null;
    private int m_waitForRecordingStateSecs = 0;
    private int m_tuneTimeoutSecs = 0;
    private int m_tuneTimeoutSecsExt = 0;
    private int m_localMediaServerTimeoutSecs = 0;
    private long m_hnActionTimeoutMS = 0;
    private int m_droolingBabyChannelIdx = 0;
    private int m_golfChannelIdx = 0;
    private int m_clockChannelIdx = 0;
    private String m_serverName  = null;
    private String m_channelName1 = null;
    private String m_channelName2 = null;
    private String m_channelName3 = null;
    private String m_vpopServerName = null;
    private int m_tolerance = 0;
    private static final String ROOT_CONTAINER_ID = "0";

    public class ScriptStatus
    {
        public int m_scriptId;
        public String m_status;
        public String m_scriptName;
        public Boolean m_returnCode;
        public String m_returnString;
        
        public static final String STATUS_RUNNING = "RUNNING";
        public static final String STATUS_COMPLETE = "COMPLETE";
        public static final String STATUS_ERROR = "ERROR";
        
        public ScriptStatus(String scriptName, int scriptId)
        {
            m_scriptName = scriptName;
            m_scriptId = scriptId;
            
            m_status = STATUS_RUNNING;
            m_returnCode = Boolean.FALSE;
            m_returnString = "";
        }
        
        public String toString()
        {
            String strTemp = m_scriptName + " (" + m_scriptId + "): " + m_status;
            
            if (!m_status.equals(ScriptStatus.STATUS_RUNNING))
            {
                strTemp += ": " + (m_returnCode.booleanValue()?"PASS":"FAIL") + ": " + m_returnString;
            }
            return strTemp;
        }
    }
    

    public RiScriptlet()
    {
        if (m_log.isInfoEnabled())
        {
            m_log.info("RiScriptlet()");
        }
    }


    /**
     * Initializes the OCAP Xlet.
     * 
     * @param ctx
     *            The context for this Xlet is passed in.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             initialised.
     */
    public void initXlet(XletContext ctx)
    {
        if (m_log.isInfoEnabled())
        {
            m_log.info("initXlet");
        }

        m_ctx = ctx;
        
        m_useJavaTVChannelMap = configureChannelMap();
        try 
        {
            m_telnetInterface = new RiScriptletTelnetInterface(this);
        }
        catch (Exception ex) 
        {
            if (m_log.isInfoEnabled())
            {
                m_log.info("Error starting telnet interface", ex);
            }
        }

        ArgParser ap = null;
        try
        {
            ap = new ArgParser((String[]) (ctx.getXletProperty(XletContext.ARGS)));

            // Lookup test runner's event handler object
            
            int index=0;
            ArrayList scriptList = new ArrayList();
            while(true)
            {
                try
                {
                    String temp = ap.getStringArg("script_" + index);
                    scriptList.add(temp);
                    if (m_log.isInfoEnabled())
                    {
                        m_log.info("script_" + index + " = " + temp);
                    }
                }
                catch (Exception ex)
                {
                    // end of script list
                    break;
                }
                
                
                index++;
            }
            
            m_scriptFilesFromArgs = (String []) scriptList.toArray(new String[0]);
            
            // set up the completion boolean array -- this will be used to trackwhen all the
            // start-up scripts have been completed
            m_scriptFilesFromArgsComplete = new boolean[m_scriptFilesFromArgs.length];
            m_scriptFileResults = new String[m_scriptFilesFromArgs.length];
            for (int i=0; i<m_scriptFilesFromArgsComplete.length; i++)
            {
                m_scriptFilesFromArgsComplete[i] = false;
            }
            
            m_writeCompletionFlagFile = (ap.getIntArg("writeCompletionFlagFile") == 1);
            
            m_isSyncServer = (ap.getIntArg("isSyncServer") == 1);
            m_syncServerAddr = (ap.getStringArg("syncServerAddr"));
            m_syncServerPort = (ap.getIntArg("syncServerPort"));
            m_waitForRecordingStateSecs = (ap.getIntArg("rxDriver.wait_for_recording_state_secs"));
            m_tuneTimeoutSecs = (ap.getIntArg("rxDriver.tune_timeout_secs"));
            m_tuneTimeoutSecsExt = (ap.getIntArg("rxDriver.tune_timeout_secs_ext"));
            m_localMediaServerTimeoutSecs = (ap.getIntArg("rxDriver.local_media_server_timeout_secs"));
            m_hnActionTimeoutMS = (ap.getLongArg("rxDriver.hn_action_timeout_ms"));
            m_droolingBabyChannelIdx = (ap.getIntArg("rxDriver.drooling_baby_channel_index"));
            m_golfChannelIdx = (ap.getIntArg("rxDriver.golf_channel_index"));
            m_clockChannelIdx = (ap.getIntArg("rxDriver.clock_channel_index"));
            m_serverName = (ap.getStringArg("rxDriver.server_name"));
            m_channelName1 = (ap.getStringArg("rxDriver.channel_name1"));
            m_channelName2 = (ap.getStringArg("rxDriver.channel_name2"));
            m_channelName3 = (ap.getStringArg("rxDriver.channel_name3"));
            m_tolerance = (ap.getIntArg("rxDriver.tolerance_secs"));
            m_vpopServerName = (ap.getStringArg("vpop_server_name"));
            
            String oid = (String) m_ctx.getXletProperty("dvb.org.id");
            String aid = (String) m_ctx.getXletProperty("dvb.app.id");
        }
        catch (Exception ex)
        {
            if (m_log.isInfoEnabled())
            {
                m_log.info("Exception reading command line args", ex);
            }
        }

        if (m_log.isInfoEnabled())
        {
            m_log.info("m_writeCompletionFlagFile = " + m_writeCompletionFlagFile + 
                    ", m_isSyncServer = " + m_isSyncServer + 
                    ", m_isSyncServer = " + m_isSyncServer + 
                    ", m_syncServerAddr = " + m_syncServerAddr + 
                    ", m_syncServerPort = " + m_syncServerPort);
        }
        
        if (m_isSyncServer)
        {
            try
            {
                m_syncServer = new RiScriptletSyncServer(m_syncServerPort);
                m_syncServer.start();
            }
            catch (Exception ex)
            {
                
                if (m_log.isErrorEnabled())
                {
                    m_log.error("Exception starting Sync Server.", ex);
                }
            }
        }
    }

    /**
     * Starts the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             started.
     */
    public void startXlet()
    {
        for (int i=0; i<m_scriptFilesFromArgs.length; i++)
        {
            runScript(m_scriptFilesFromArgs[i], i, null, m_writeCompletionFlagFile);
        }

//        testBeanShellPaths();

        // test
//        runScript ("../testScript.bsh", null, false);   


//        doBeanShellTest();
    }

    private void testBeanShellPaths()
    {
        runScript ("../testScript.bsh", -1, null, false);   
        runScript ("scripts/testScript.bsh", -1, null, false);   
        runScript ("../../testScript.bsh", -1, null, false);   
        runScript ("cygdrive/c/testScript.bsh", -1, null, false);   
    }

    private void doBeanShellTest()
    {
        if (m_log.isInfoEnabled())
        {
            m_log.info("BeanShell test start");
        }

        try
        {
            Interpreter i = new Interpreter(); // Construct an interpreter
            i.set("foo", 5); // Set variables
            i.set("date", new Date() );

            Date date = (Date)i.get("date"); // retrieve a variable

            // Eval a statement and get the result
            i.eval("bar = foo*10");

            if (m_log.isInfoEnabled())
            {
                m_log.info("BeanShell test: bar = " + i.get("bar") );
            }

        }
        catch (EvalError ex)
        {
            if (m_log.isInfoEnabled())
            {
                m_log.info("EvalError during BeanShellTest", ex);
            }
        }   

        if (m_log.isInfoEnabled())
        {
            m_log.info("BeanShell test end");
        }
    }

    private synchronized int getScriptRunID()
    {
        return m_ScriptRunID++;
    }

    public String getScriptStatus (int scriptID)
    {
        return ((ScriptStatus) m_mapScriptStatus.get(new Integer(scriptID))).toString();
    } 

    public String[] getScriptStatusList ()
    {
        int sz = m_mapScriptStatus.size();
        String[] list = new String[sz];

        Iterator it = (m_mapScriptStatus.keySet()).iterator();
        int i=0;
        while (it.hasNext())
        {
            int scriptID = ((Integer)it.next()).intValue();
            list[i] = ((ScriptStatus) m_mapScriptStatus.get(new Integer(scriptID))).toString();

            i++;
        }

        return list;
    }
    
    /**
     * This method attempts to find a channel map from a configuration file 
     * as designated by the config_file arg in hostapp.properties. It also 
     * looks for the use_javatv_channel_map arg. This method returns true if
     * use_javatv_channel_map is true and false otherwise.
     */
    private boolean configureChannelMap()
    {
        boolean useJavaTVChannelMap = false;
        
        try
        {
            // Get the value of the config_file arg
            ArgParser args = new ArgParser((String[]) m_ctx.getXletProperty(XletContext.ARGS));
            m_strChannelFile = args.getStringArg(CONFIG_FILE);

            FileInputStream fis = new FileInputStream(m_strChannelFile);
            ArgParser fopts = new ArgParser(fis);
            fis.close();

            // Check to see if we should use the Java TV channel map.
            try
            {
                String value = fopts.getStringArg(USE_JAVA_TV);
                if (value.equalsIgnoreCase("true"))
                {
                    useJavaTVChannelMap = true;
                }
                else if (value.equalsIgnoreCase("false"))
                {
                    useJavaTVChannelMap = false;
                }
                else
                {
                    useJavaTVChannelMap = false;
                }
            }
            catch (Exception e)
            {
                useJavaTVChannelMap = false;
                if (m_log.isErrorEnabled())
                {
                    m_log.error("Exception in configureChannelMap() ", e);
                }
            }
        }
        catch (FileNotFoundException fnfex)
        {
            if (m_log.isErrorEnabled())
            {
                m_log.error("RiScriptlet channel file " + m_strChannelFile + " not found");
            }
            m_strChannelFile = "";
        }
        catch (IOException e)
        {
            if (m_log.isErrorEnabled())
            {
                m_log.error(e);
            }
        }
        catch (Exception e)
        {
            if (m_log.isErrorEnabled())
            {
                m_log.error("Unable to find config_file arg");
            }
        }
        return useJavaTVChannelMap;
    }

    public int runScript(String scriptPath, int scriptIndex, final RiScriptletStatusListener listener, boolean writeCompletionFlagFile)
    {
        if (m_log.isInfoEnabled())
        {
            m_log.info("Running script: " + scriptPath);
        }

        final String scriptPathTemp = scriptPath;
        final int scriptID = getScriptRunID();
        final boolean writeCompletionFlagFileTemp = writeCompletionFlagFile;
        final int scriptIndexTemp = scriptIndex;

        ScriptStatus scriptStatus = new ScriptStatus(new File(scriptPath).getName(), scriptID);
        m_mapScriptStatus.put (new Integer(scriptID), scriptStatus);
        
        
        boolean bDVREnabled = false;
        boolean bHNEnabled = false;
        if (System.getProperty("ocap.api.option.hn") != null)
        {
            bHNEnabled = true;
        }
        if (System.getProperty("ocap.api.option.dvr") != null)
        {
            bDVREnabled = true;
        }
        final OcapAppDriverInterfaceCore rxDriverCore = OcapAppDriverCore.getOADCoreInterface();
        
        rxDriverCore.initChannelMap(m_useJavaTVChannelMap, m_strChannelFile);
        rxDriverCore.setResourceContentionHandling(false);
        
        rxDriverCore.setOrganization((String) m_ctx.getXletProperty("dvb.org.id"));
        
        OcapAppDriverInterfaceDVR rxDriverDVRTemp = null;
        if (bDVREnabled)
        {
            rxDriverDVRTemp = OcapAppDriverDVR.getOADDVRInterface();
            rxDriverDVRTemp.setNumTuners(2);
        }
        final OcapAppDriverInterfaceDVR rxDriverDVR = rxDriverDVRTemp;
        
        OcapAppDriverInterfaceHN rxDriverHNTemp = null;
        if (bHNEnabled)
        {
            rxDriverHNTemp = OcapAppDriverHN.getOADHNInterface();
        }
        final OcapAppDriverInterfaceHN rxDriverHN = rxDriverHNTemp;
        
        OcapAppDriverInterfaceHNDVR rxDriverHNDVRTemp = null;
        if (bDVREnabled && bHNEnabled)
        {
            rxDriverHNDVRTemp = OcapAppDriverHNDVR.getOADHNDVRInterface();
        }
        final OcapAppDriverInterfaceHNDVR rxDriverHNDVR = rxDriverHNDVRTemp;
        


        // run the script in a different thread -- this way, multiple scripts can be run simultaneously
        Thread myThread = new Thread()
        {
            public void run()
            {
                // in case we decide that we need to track each script run, save
                // a unique ID for this run

                Boolean returnCode = Boolean.TRUE;
                String returnString = "";

                if (m_log.isInfoEnabled())
                {
                    m_log.info("Script thread start: script = " + scriptPathTemp + ", ID = " + scriptID);
                }

                try
                {
                    Interpreter i = new Interpreter(); // Construct an interpreter
                    i.setStrictJava(true);

                    RiScriptletLogger log = new RiScriptletLogger (RiScriptlet.class, scriptPathTemp);
                    
                    RiScriptletSyncClient syncClient = new RiScriptletSyncClient(m_syncServerAddr, m_syncServerPort);

                    // set global vars to be used in scripts

                    i.set("rxDriverCore", rxDriverCore);
                    i.set("rxDriverDVR", rxDriverDVR);
                    i.set("rxDriverHN", rxDriverHN);
                    i.set("rxDriverHNDVR", rxDriverHNDVR);
                    i.set("WAIT_FOR_RECORDING_STATE_SECS", m_waitForRecordingStateSecs);
                    i.set("TUNE_TIMEOUT_SECS", m_tuneTimeoutSecs);
                    i.set("TUNE_TIMEOUT_SECS_EXT", m_tuneTimeoutSecsExt);
                    i.set("LOCAL_MEDIA_SERVER_TIMEOUT_SECS", m_localMediaServerTimeoutSecs);
                    i.set("HN_ACTION_TIMEOUT_MS", m_hnActionTimeoutMS);
                    i.set("DROOLING_BABY_CHANNEL_IDX", m_droolingBabyChannelIdx);
                    i.set("GOLF_CHANNEL_IDX", m_golfChannelIdx);
                    i.set("CLOCK_CHANNEL_IDX", m_clockChannelIdx);
                    i.set("SERVER_NAME", m_serverName);
                    i.set("CHANNEL_NAME_1", m_channelName1);
                    i.set("CHANNEL_NAME_2", m_channelName2);
                    i.set("CHANNEL_NAME_3", m_channelName3);
                    i.set("TOLERANCE_SECS", m_tolerance);
                    i.set("rxLog", log);
                    i.set("rxSyncClient", syncClient);
                    i.set("rxReturn", returnCode);
                    i.set("rxReturnString", returnString);
                    i.set("VPOP_SERVER_NAME", m_vpopServerName);
                    i.set("ROOT_CONTAINER_ID", ROOT_CONTAINER_ID);

                    i.source(scriptPathTemp); 

                    returnCode = (Boolean) (i.get ("rxReturn"));
                    returnString = (String)(i.get ("rxReturnString"));

                    ScriptStatus scriptStatus = (ScriptStatus) m_mapScriptStatus.get(new Integer(scriptID));
                    if (scriptStatus != null)
                    {
                        scriptStatus.m_status = ScriptStatus.STATUS_COMPLETE;
                        scriptStatus.m_returnCode = returnCode;
                        scriptStatus.m_returnString = returnString;
                    }
                }
                catch (FileNotFoundException ex)
                {
                    if (m_log.isInfoEnabled())
                    {
                        m_log.info("FileNotFoundException while executing script " + scriptPathTemp + "(" + scriptID + ")", ex);
                    }

                    returnCode = Boolean.FALSE;
                    returnString = "FileNotFoundException starting script: " + ex.getMessage();

                    ScriptStatus scriptStatus = (ScriptStatus) m_mapScriptStatus.get(new Integer(scriptID));
                    if (scriptStatus != null)
                    {
                        scriptStatus.m_status = ScriptStatus.STATUS_ERROR;
                        scriptStatus.m_returnCode = returnCode;
                        scriptStatus.m_returnString = returnString;
                    }
                }   
                catch (IOException ex)
                {
                    if (m_log.isInfoEnabled())
                    {
                        m_log.info("IOException while executing script " + scriptPathTemp + "(" + scriptID + ")", ex);
                    }

                    returnCode = Boolean.FALSE;
                    returnString = "IOException starting script: " + ex.getMessage();

                    ScriptStatus scriptStatus = (ScriptStatus) m_mapScriptStatus.get(new Integer(scriptID));
                    if (scriptStatus != null)
                    {
                        scriptStatus.m_status = ScriptStatus.STATUS_ERROR;
                        scriptStatus.m_returnCode = returnCode;
                        scriptStatus.m_returnString = returnString;
                    }
                }   
                catch (EvalError ex)
                {
                    if (m_log.isInfoEnabled())
                    {
                        m_log.info("EvalError while executing script " + scriptPathTemp + "(" + scriptID + ")", ex);
                    }

                    returnCode = Boolean.FALSE;
                    returnString = "EvalError while executing script: " + ex.toString();

                    ScriptStatus scriptStatus = (ScriptStatus) m_mapScriptStatus.get(new Integer(scriptID));
                    if (scriptStatus != null)
                    {
                        scriptStatus.m_status = ScriptStatus.STATUS_ERROR;
                        scriptStatus.m_returnCode = returnCode;
                        scriptStatus.m_returnString = returnString;
                    }
                }

                if (listener != null)
                {
                    listener.notifyScriptComplete (scriptPathTemp, scriptID, returnCode.booleanValue(), returnString);
                }

                if (m_log.isInfoEnabled())
                {
                    m_log.info("Script thread end: script = " + scriptPathTemp + "(" + scriptID + ")");
                }

                if (writeCompletionFlagFileTemp && scriptIndexTemp >= 0)
                {
                    synchronized (m_scriptFilesFromArgsComplete)
                    {
                        String passFail = returnCode.booleanValue()?"Pass":"FAIL";
                        m_scriptFileResults[scriptIndexTemp] = passFail + "\t" + 
                                scriptPathTemp + "\t" + returnString;

                        
                        m_scriptFilesFromArgsComplete[scriptIndexTemp] = true;
                        
                        boolean allComplete = true;
                        for (int i=0; i<m_scriptFilesFromArgsComplete.length; i++)
                        {
                            if (!m_scriptFilesFromArgsComplete[i])
                            {
                                allComplete = false;
                                break;
                            }
                        }
                        
                        if (allComplete)
                        {
                            writeCompletionFile();
                        }
                    }
                }
            }
        };

        myThread.start();

        return scriptID;
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
    }

    /**
     * Destroys the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             destroyed.
     */
    public void destroyXlet(boolean x)
    {
    }

    private void writeCompletionFile()
    {
        try
        {
            String aidStr = (String) m_ctx.getXletProperty("dvb.app.id");
            String oidStr = (String) m_ctx.getXletProperty("dvb.org.id");
            String filePath = "/syscwd/persistent/usr/" + oidStr + "/" + aidStr + "/tmp";
            String resultsFilePath = "/syscwd/persistent/usr/" + oidStr + "/" + aidStr + "/SCRIPT_COMPLETE";

            File file = new File(filePath);
            OutputStream out = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(out);

            for (int i=0; i<m_scriptFileResults.length; i++)
            {
                pw.println(m_scriptFileResults[i]);
            }

            pw.close();
            out.close();
            
            // Rename file when done writing output
            file.renameTo(new File(resultsFilePath));
        }
        catch (FileNotFoundException e)
        {
            if (m_log.isErrorEnabled())
            {
                m_log.error("FileNotFoundException writing completion file", e);
            }
        }
        catch (IOException e)
        {
            if (m_log.isErrorEnabled())
            {
                m_log.error("IOException writing completion file", e);
            }
        }
        if (m_log.isInfoEnabled())
        {
            m_log.info("Done writing completion file");
        }
    }
}
