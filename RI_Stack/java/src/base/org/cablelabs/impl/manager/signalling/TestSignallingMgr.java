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

package org.cablelabs.impl.manager.signalling;

import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.signalling.Ait;
import org.cablelabs.impl.signalling.AitImpl;
import org.cablelabs.impl.signalling.Xait;
import org.cablelabs.impl.manager.signalling.AitProps;
import org.cablelabs.impl.manager.signalling.XaitProps;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.ocap.net.OcapLocator;

/**
 * <code>TestSignallingMgr</code> provides an implementation of the
 * <code>SignallingManager</code> that does not rely on actual network
 * signalling. It can be used to test other aspects of the implementation.
 * 
 * @author Aaron Kamienski
 */
public class TestSignallingMgr extends SignallingMgr
{
    /**
     * No public constructor.
     */
    private TestSignallingMgr()
    { /* EMPTY */
    }

    /**
     * Creates and returns an instance of <code>TestSignallingMgr</code>.
     * Satisfies a requirement of a <code>Manager</code> implementation.
     * 
     * @return an instance of this <code>Manager</code>.
     */
    public static Manager getInstance()
    {
        return new TestSignallingMgr();
    }

    /**
     * Factory method used to create an instance of a
     * <code>SignallingMonitor</code> for monitoring out-of-band XAIT
     * signalling. There will be only one XAIT <code>SignallingMonitor</code> in
     * use at a time.
     * 
     * @return an instance of {@link XaitMonitor}
     */
    protected SignallingMonitor createXaitMonitor()
    {
        return new XaitMonitor();
    }

    /**
     * Factory method used to create an instance of
     * <code>SignallingMonitor</code> for monitoring in-band AIT signalling.
     * There will be only one AIT <code>SignallingMonitor</code> in use for a
     * given <i>service</i> locator at a time; however, there may be as many
     * <code>SignallingMonitor</code>s in use as there are
     * <code>NetworkInterface</code>s.
     * 
     * @param service
     * @return an instance of {@link AitMonitor}
     */
    protected SignallingMonitor createAitMonitor(OcapLocator service)
    {
        return new AitMonitor(service);
    }

    // ECR 1083
    // Copy from SignallingManager
    public boolean loadPersistentXait()
    {
        // load file from the persistent directory
        String xaitprops = MPEEnv.getEnv("OCAP.persistent.xaitstorage") + "/xait.properties";
        File ifile = new File(xaitprops);
        if (!ifile.exists())
        {
            if (log.isDebugEnabled())
            {
                log.debug("Persistant Xait File Not Found " + xaitprops);
            }
            return false;
        }

        java.io.InputStream is;
        try
        {
            is = new java.io.FileInputStream(ifile);
            if (is == null) return false;
        }
        catch (java.io.FileNotFoundException ex)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Persistant Xait File Not Found " + xaitprops);
            }
            return false;
        }

        XaitProps xaitProps = new XaitProps(Xait.NETWORK_SIGNALLING);
        try
        {
            xaitProps.parse(is, -1); // -1 first time
            is.close();
            xaitMonitor.handleSignalling(xaitProps.getSignalling(), true, false); // process the xait
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Problem parsing persistent xait ", e);
            }
            return false;
        }

        if (log.isDebugEnabled())
        {
            log.debug("Read persistent xait");
        }

        return true;
    }

    // ECR 1083
    // Copy from SignallingManager
    public void deletePersistentXait()
    {
        String xaitprops = MPEEnv.getEnv("OCAP.persistent.xaitstorage") + "/xait.properties";
        File dfile = new File(xaitprops);
        if (dfile.exists())
        {
            dfile.delete();
            if (log.isDebugEnabled())
            {
                log.debug("Deleted persistent xait file: " + xaitprops);
            }
        }
    }

    /**
     * Monitors XAIT signalling specified by a file on the local filesystem. The
     * local file is found on the system classpath and is called
     * <code>"xait.properties"</code>.
     * 
     * @see TestSignallingMgr#createXaitMonitor
     * @author Aaron Kamienski
     */
    private class XaitMonitor extends SignallingMonitor implements Runnable
    {
        /**
         * Initiates monitoring of application signalling. Launches a thread
         * that will periodically check an external file for changes.
         */
        public synchronized void startMonitoring()
        {
            if (thread != null)
            {
                throw new IllegalStateException("stopMonitoring hasn't been called");
            }

            thread = new Thread(this, getName());
            thread.start();
        }

        /**
         * Cancels monitoring of application signalling.
         */
        public synchronized void stopMonitoring()
        {
            if (thread == null)
            {
                throw new IllegalStateException("startMonitoring hasn't been called");
            }

            Thread tmp = thread;
            thread = null;
            tmp.interrupt();
        }

        /**
         * Generates the name that describes this monitor. This is used to
         * generate a name for the monitoring thread as well as the name of the
         * monitored properties file.
         * 
         * @return <code>"xait"</code>
         */
        protected String getName()
        {
            return "xait";
        }

        public synchronized void resignal()
        {
            lastVersion = -1;
        }

        /**
         * Opens the local file that contains signalling.
         * <p>
         * The local file is found on the system classpath based upon the value
         * returned by {@link #getName}. Assuming that <code>getName()</code>
         * returns <code>"xait"</code>, then the file opened would be
         * <code>"xait.properties"</code>.
         * 
         * @return the local file or <code>null</code> if not available
         */
        protected java.io.InputStream openFile()
        {
            return getClass().getResourceAsStream("/" + getName() + ".properties");
        }

        /**
         * Creates and returns an <code>XaitProps</code> instance to parse the
         * file.
         */
        protected AitProps createParser(int lastVersion)
        {
            return new XaitProps(Xait.NETWORK_SIGNALLING);
        }

        /**
         * Parses the latest file and returns the XAIT/AIT as an instance of
         * <code>Xait</code> or <code>Ait</code>. If no XAIT/AIT is signalled,
         * then an empty <code>Ait</code> is returned.
         * 
         * @return the parsed <code>Xait</code> or <code>Ait</code>;
         *         <code>null</code> if there has been no change
         */
        private synchronized Ait parseSignalling()
        {
            java.io.InputStream is = openFile();
            if (is == null)
            {
                boolean wasEmpty = empty;

                if (!wasEmpty)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Could not open " + getName());
                    }
                }

                empty = true;
                if (!wasEmpty) return emptyAit();

                return null;
            }
            empty = false;

            try
            {
                AitProps aitProps = createParser(lastVersion);
                Ait ait;
                try
                {
                    if (false)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Parsing " + getName());
                        }
                    }

                    aitProps.parse(is, lastVersion);
                    ait = aitProps.getSignalling();
                    if (lastVersion == ait.getVersion())
                    {
                        if (false)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("No version change " + getName() + " ver=" + lastVersion);
                            }
                        }
                        return null;
                    }

                    SystemEventUtil.logEvent("XAIT.properties read/parsed");
                }
                catch (Exception e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Problem parsing " + getName(), e);
                    }
                    return null;
                }
                lastVersion = ait.getVersion();

                if (log.isDebugEnabled())
                {
                    log.debug("Just read " + getName() + " ver=" + lastVersion);
                }

                return ait;
            } finally
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Could not close " + getName(), e);
                    }
            }
        }
        }

        /**
         * Returns null. No notification is made if no XAIT is present.
         * 
         * @return an empty <code>Ait</code>
         */
        protected Ait emptyAit()
        {
            return null;
        }

        /**
         * ECR 1083. Copy the xait.properties file to the
         * "OCAP.persistent.xaitstorage" directory.
         */
        protected void savePersistentXait()
        {
            try
            {
                java.io.InputStream in = openFile();
                if (null == in) return;

                String xaitprops = MPEEnv.getEnv("OCAP.persistent.xaitstorage") + "/xait.properties";
                File ofile = new File(xaitprops);

                // For Overwrite the file.
                java.io.OutputStream out = new java.io.FileOutputStream(ofile);

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                if (log.isDebugEnabled())
                {
                    log.debug("Persistent xait saved in " + xaitprops);
                }
            }
            catch (java.io.FileNotFoundException ex)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Save Persistent Xait File Not Found " + ex.getMessage() + " in the specified directory.");
                }
            }
            catch (IOException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Save Persistant Xait IOExecption " + (e.getMessage()));
                }
            }

        }

        /**
         * Examines signalling and dispatches to listeners. Performs the
         * following algorithm in a loop:
         * <ol>
         * <li>Test for interruption, stop if interrupted.
         * <li>Parse file, generate Ait/Xait
         * <li>If updated, then dispatch to listeners (invoke
         * {@link SignallingMgr#handleSignalling})
         * <li>Sleep 10 seconds before checking again
         * </ol>
         */
        public void run()
        {
            while (!Thread.interrupted())
            {
                synchronized (this)
                {
                    if (thread != Thread.currentThread())
                    {
                        break;
                    }
                        
                    // Create Ait/Xait
                    Ait ait = parseSignalling();
    
                    // Dispatch to listeners
                    if (ait != null)
                    {
                        savePersistentXait(); // ECR 1083.
                        handleSignalling(ait, true, false);
                    }
                }

                // Sleep for a period
                try
                {
                    Thread.sleep(10000);
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }

        private boolean empty = false;

        protected Thread thread;

        protected int lastVersion = -1;
    }

    /**
     * Monitors AIT signalling specified by a file on the local filesystem. The
     * local file is found on the system classpath and named using the sourceID
     * or frequency/program of the service.
     * <p>
     * If the service is known by a sourceID, then the file is called
     * <code>"ait.<i>serviceId</i>.properties"</code> where <i>serviceId</i> is
     * the 16-bit service identifier expressed using the following format:
     * 
     * <pre>
     * serviceId := [1-9a-f][0-9a-f]*
     * </pre>
     * 
     * That is, it is expressed as a lower-case hexadecimal number with no
     * leading "0x" and no leading zeroes.
     * <p>
     * If the service is known by frequency/program, then the file is called
     * <code>"ait.<i>frequency.program</i>.properties"</code> where
     * <i>frequency.program</i> is the frequency/program identifying the service
     * expressed using the following format:
     * 
     * <pre>
     * frequency := [1-9a-f][0-9a-f]*
     * program := [1-9a-f][0-9a-f]*
     * </pre>
     * 
     * That is, it is expressed as a lower-case hexadecimal number with no
     * leading "0x" and no leading zeroes.
     * 
     * @see #createAitMonitor
     * @author Aaron Kamienski
     */
    private class AitMonitor extends XaitMonitor
    {
        AitMonitor(OcapLocator service)
        {
            this.service = service;

            int sourceID = service.getSourceID();
            if (sourceID != -1)
            {
                this.name = "ait-" + Integer.toHexString(sourceID);
            }
            else
            {
                this.name = "ait-" + Integer.toHexString(service.getFrequency()) + "."
                        + Integer.toHexString(service.getProgramNumber());
            }
        }

        /**
         * Generates the name that describes this monitor. This is used to
         * generate a name for the monitoring thread as well as the name of the
         * monitored properties file.
         * 
         * @return <code>"ait-xxxx"</code> where <i><code>xxxx</code></i> is a
         *         lowercase hexadecimal representation for the 16-bit source
         *         id, with no leading "0x" and no leading zeroes.
         */
        protected String getName()
        {
            return name;
        }

        /**
         * Creates and returns an <code>XaitProps</code> instance to parse the
         * file.
         */
        protected AitProps createParser(int lastVersion)
        {
            return new AitProps();
        }

        /**
         * Returns an empty <code>Ait</code>. This is called when no AIT or XAIT
         * is signalled at all.
         * 
         * @return an empty <code>Ait</code>
         */
        protected Ait emptyAit()
        {
            AitImpl emptyAit = new AitImpl();
            emptyAit.initialize(0, new Vector(), new Vector(), new Hashtable(), new Hashtable());
            return emptyAit;
        }

        /**
         * The service locator for which AIT monitoring should be performed.
         */
        private OcapLocator service;

        /**
         * The name of this monitor.
         */
        private String name;
    }

    private static final Logger log = Logger.getLogger(TestSignallingMgr.class);

}
