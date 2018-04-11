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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIElement;
import javax.tv.service.SIManager;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TransportStream;
import org.davic.mpeg.TuningException;
import org.davic.mpeg.sections.ConnectionLostException;
import org.davic.mpeg.sections.EndOfFilteringEvent;
import org.davic.mpeg.sections.FilterResourceException;
import org.davic.mpeg.sections.FilterResourcesAvailableEvent;
import org.davic.mpeg.sections.FilteringInterruptedException;
import org.davic.mpeg.sections.ForcedDisconnectedEvent;
import org.davic.mpeg.sections.IllegalFilterDefinitionException;
import org.davic.mpeg.sections.InvalidSourceException;
import org.davic.mpeg.sections.NoDataAvailableException;
import org.davic.mpeg.sections.Section;
import org.davic.mpeg.sections.SectionFilterEvent;
import org.davic.mpeg.sections.SectionFilterGroup;
import org.davic.mpeg.sections.SectionFilterListener;
import org.davic.mpeg.sections.TableSectionFilter;
import org.davic.mpeg.sections.VersionChangeDetectedEvent;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.net.tuning.StreamTable;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.dvb.spi.selection.KnownServiceReference;
import org.dvb.spi.selection.SelectionSession;
import org.ocap.application.OcapAppAttributes;
import org.ocap.mpeg.PODExtendedChannel;
import org.ocap.net.OcapLocator;
import org.ocap.si.Descriptor;
import org.ocap.si.DescriptorTag;
import org.ocap.si.PMTElementaryStreamInfo;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.ProgramMapTableManager;
import org.ocap.si.StreamType;
import org.ocap.si.TableChangeListener;

import org.cablelabs.impl.davic.mpeg.sections.AITSectionFilterGroup;
import org.cablelabs.impl.davic.mpeg.sections.BasicSection;
import org.cablelabs.impl.davic.mpeg.sections.XAITSectionFilterGroup;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.signalling.Ait;
import org.cablelabs.impl.signalling.AitImpl;
import org.cablelabs.impl.signalling.SignallingListener;
import org.cablelabs.impl.signalling.Xait;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.spi.SelectionProviderInstance;
import org.cablelabs.impl.spi.ProviderInstance.SelectionSessionWrapper;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.LocatorUtil;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * <code>DavicSignallingMgr</code> provides an implementation of the
 * <code>SignallingManager</code> that relies public Java APIs, including DAVIC
 * Section Filtering API, for monitoring and accessing application signalling.
 * 
 * @see org.davic.mpeg.sections
 * @see org.davic.net.tuning
 * @see org.ocap.si
 * 
 * @author Aaron Kamienski
 */
public class DavicSignallingMgr extends SignallingMgr
{
    /**
     * No public constructor.
     */
    DavicSignallingMgr()
    { /* empty */
    }

    /**
     * Creates and returns an instance of <code>DavicSignallingMgr</code>.
     * Satisfies a requirement of a <code>Manager</code> implementation.
     * 
     * @return an instance of this <code>Manager</code>.
     */
    public static Manager getInstance()
    {
        return new DavicSignallingMgr();
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
     *            locator
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
        File ifile;
        String xaitBase = MPEEnv.getEnv("OCAP.persistent.xaitstorage") + "/xait";
        int s = 0;

        // Determine number of section files xait1, xait2,...
        while (true)
        {
            ifile = new File(xaitBase + (s + 1));
            if (!ifile.exists()) break;
            s += 1; // next xait name
        }
        if (s == 0) return false; // no persistent xait

        BasicSection[] sections = new BasicSection[s]; // allocate storage for
                                                       // sections

        java.io.InputStream is;
        for (int x = 0; x < s; x++)
        {
            try
            {
                ifile = new File(xaitBase + (x + 1)); // xait1, xait2...
                is = new java.io.FileInputStream(ifile);
                byte[] buf = new byte[(int) ifile.length()]; // create buffer
                                                             // for whole file
                is.read(buf); // read whole xait section
                is.close();
                sections[x] = new BasicSection(buf); // convert buffer to
                                                     // Section
            }
            catch (java.io.FileNotFoundException ex)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Persistant Xait File Not Found ");
                }
                return false;
            }
            catch (IOException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("IOExecption " + (e.getMessage()));
                }
                return false;
            }
        }
        // Parse the XAIT contained in the given sections
        XaitParser xaitParser = new XaitParser(Xait.NETWORK_SIGNALLING);
        xaitParser.parse(sections);

        SystemEventUtil.logEvent("XAIT parsed from persistent data");
        xaitMonitor.handleSignalling(xaitParser.getSignalling(), true, false);
        return true;
    }

    // ECR 1083
    // Copy from SignallingManager
    public void deletePersistentXait()
    {
        // Get path and append file root name
        String xaitSeg = MPEEnv.getEnv("OCAP.persistent.xaitstorage") + "/xait";

        int i = 1; // xait segment names end with a number starting with '1'.
        while (true)
        {
            // delete xait segments
            String xaitSegNum = xaitSeg + i;
            File dfile = new File(xaitSegNum);
            if (dfile.exists())
            {
                dfile.delete();
                i += 1;
                if (log.isDebugEnabled())
                {
                    log.debug("Deleted persistent xait file: " + xaitSegNum);
                }
            }
            else
            {
                return;
            }
        }
    }

    /**
     * @see DavicSignallingMgr#createXaitMonitor
     * @author Aaron Kamienski
     */
    private class XaitMonitor extends SignallingMonitor implements SectionFilterListener, ResourceStatusListener,
            ResourceClient
    {
        /**
         * <ol>
         * <li>Create the <code>SectionFilterGroup</code>
         * <li>Create a <code>TableSectionFilter</code>
         * <li>Start filtering.
         * <li>Attach the group.
         * </ol>
         */
        public synchronized void startMonitoring()
        {
            if (log.isInfoEnabled())
            {
                log.info("XaitMonitor - startMonitoring");
            }
            // Create filterGroup
            group = new XAITSectionFilterGroup(1);
            group.addResourceStatusEventListener(this);

            // Create filter
            filter = group.newTableSectionFilter();
            filter.addSectionFilterListener(this);

            if (setFilter(false, 0)) attach();
        }

        /**
         * Sets the filter used to acquire the XAIT.
         * 
         * @param useVersion
         *            if <code>true</code> then <i>version</i> is used to set a
         *            negative <code>version_number</code> filter
         * @param version
         *            the version (as retuned by {@link Section#version_number})
         *            of the last received table
         * @return <code>true</code> if successful and filter should be attached
         */
        private boolean setFilter(boolean useVersion, int version)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Setting filter... " + useVersion + " " + version);
            }

            try
            {
                // startFiltering
                // given PID=0x1FFC, table_id=0x74, and a positive filter
                // application_type==0x0001
                if (!POS_NEG_FILTER)
                    filter.startFiltering(this, 0x1FFC, 0x74);
                else if (!useVersion)
                    filter.startFiltering(this, 0x1FFC, 0x74, APP_TYPE_FILTER, APP_TYPE_MASK);
                else
                {
                    // This starts 3 bytes into the section header.
                    // See DAVIC 1.4.1p9 E.8.2
                    byte[] version_filter = { 0, 0, (byte) (version << 1) };
                    byte[] version_mask = { 0, 0, 0x3e };
                    filter.startFiltering(this, 0x1FFC, 0x74, APP_TYPE_FILTER, APP_TYPE_MASK, version_filter,
                            version_mask);
                }
            }
            catch (ConnectionLostException e)
            {
                // Unexpected for XAIT - it's OOB!
                SystemEventUtil.logRecoverableError(new Exception("XaitMonitor::setFilter: Unexpected exception" + e));
                reacquire = true;
                return false;
            }
            catch (NotAuthorizedException e)
            {
                // Unexpected - don't expect CA to limit access to XAIT
                SystemEventUtil.logRecoverableError(new Exception("XaitMonitor::setFilter: Unexpected exception" + e));
                reacquire = true;
                return false;
            }
            catch (IllegalFilterDefinitionException e)
            {
                // Unexpected - as we should've created a good filter
                SystemEventUtil.logRecoverableError(new Exception("XaitMonitor::setFilter: Unexpected exception" + e));
                reacquire = true;
                return false;
            }
            catch (FilterResourceException e)
            {
                // Unexpected - as the group should have enough
                SystemEventUtil.logRecoverableError(new Exception("XaitMonitor::setFilter: Unexpected exception" + e));
                reacquire = true;
                return false;
            }
            return true;
        }

        public synchronized void resignal()
        {
            filter.stopFiltering();
            setFilter(false, 0);
        }

        /**
         * Attach the <code>SectionFilterGroup</code> to the
         * <code>PODExtendedChannel</code> to begin filtering.
         */
        private void attach()
        {
            if (log.isDebugEnabled())
            {
                log.debug("Attaching group... ");
            }

            try
            {
                // attach filterGroup
                group.attach(PODExtendedChannel.getInstance(), this, null);
                reacquire = false;
            }
            catch (TuningException e)
            {
                // Shouldn't have to tune!
                SystemEventUtil.logRecoverableError(new Exception("XaitMonitor::attach: Unexpected exception" + e));
                // Would need to wait to be notified that we are tuned correctly
                reacquire = true;
            }
            catch (NotAuthorizedException e)
            {
                // XAIT shouldn't be scrambled!
                SystemEventUtil.logRecoverableError(new Exception("XaitMonitor::attach: Unexpected exception" + e));
            }
            catch (InvalidSourceException e)
            {
                // Unexpected
                SystemEventUtil.logRecoverableError(new Exception("XaitMonitor::attach: Unexpected exception" + e));
            }
            catch (FilterResourceException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("XAIT not available (filter group attach failed)", e);
                }

                // Wait until we can be notified that a filter is available
                reacquire = true;
            }
        }

        /**
         * <ol>
         * <li>Stop filtering.
         * <li>Detach the group.
         * </ol>
         */
        public synchronized void stopMonitoring()
        {
            if (log.isInfoEnabled())
            {
                log.info("XaitMonitor - stopMonitoring");
            }
            // Cancel outstanding filter
            if (filter != null)
            {
                filter.stopFiltering();
                filter = null;
            }

            // detach the filterGroup
            if (group != null)
            {
                group.detach();
                group = null;
            }
        }

        // ECR 1083
        // Save the xait sections to persistant storage
        private void savePersistentXait(Section[] sections)
        {
            if (sections == null || sections.length == 0) return;

            // Delete whatever is there.
            deletePersistentXait();

            // Get directory and base file name
            String xaitName = MPEEnv.getEnv("OCAP.persistent.xaitstorage") + "/xait";

            for (int s = 0; s < sections.length; s++)
            {
                java.io.OutputStream out;
                byte[] buf;
                try
                {
                    buf = sections[s].getData();
                }
                catch (NoDataAvailableException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("NoDataAvailableException " + (e.getMessage()));
                    }
                    return;
                }

                File ofile = new File(xaitName + (s + 1)); // xait1, xait2,...

                // For Overwrite the file.
                try
                {
                    out = new java.io.FileOutputStream(ofile);
                }
                catch (java.io.FileNotFoundException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("FileNotFoundException " + (e.getMessage()));
                    }
                    return;
                }

                try
                {
                    out.write(buf, 0, buf.length);
                    if (log.isDebugEnabled())
                    {
                        log.debug("Wrote " + buf.length + " bytes to xait" + (s + 1));
                    }
                    out.close();
                }
                catch (IOException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("IOExecption " + (e.getMessage()));
                    }
                    return;
                }
            }
        }

        /* ================== SectionFilterListener ================== */

        /**
         * Called when new sections are received for the set
         * <code>TableSectionFilter</code>. We are most interested in the
         * <code>EndOfFilteringEvent</code>, to which we respond by:
         * <ol>
         * <li>Acquiring the <code>Section</code>s of the table.
         * <li>Restarting filtering (with a negative filter for the new
         * version_number)
         * <li>Calling {@link SignallingMonitor#handleSignalling} to dispatch to
         * listeners
         * </ol>
         * Secondarily, we are interested in the
         * <code>VersionChangeDetectedEvent</code>, to which we respond by
         * restarting the filter. This is necessary because we may not see a
         * complete table for the previous version.
         * 
         * @param event
         */
        public void sectionFilterUpdate(SectionFilterEvent event)
        {
            Section[] sections = null;
            short version = 0;

            if (log.isDebugEnabled())
            {
                log.debug("sectionFilterUpdate(" + event + ")");
            }

            synchronized (this)
            {
                TableSectionFilter currFilter = this.filter;

                if (currFilter != event.getSource())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("sectionFilterUpdate(): Ignoring unexpected filter source.");
                    }

                    return;
                }

                // Receive table sections and restart filtering
                if (event instanceof EndOfFilteringEvent)
                {
                    try
                    {
                        sections = currFilter.getSections();
                    }
                    catch (FilteringInterruptedException e)
                    {
                        sections = null;
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("sectionFilterUpdate(): sections = " + sections);
                    }

                    // Restart filtering (ignoring current version_number)
                    boolean useVersion = false;
                    if (sections != null && sections.length > 0)
                    {
                        try
                        {
                            version = sections[0].version_number();
                            useVersion = true;
                            if (log.isDebugEnabled())
                            {
                                log.debug("sectionFilterUpdate(): version = " + version);
                            }
                        }
                        catch (NoDataAvailableException e)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("XAIT section data inaccessible", e);
                            }
                    }
                    }
                    setFilter(useVersion, version);
                }
                else if (event instanceof VersionChangeDetectedEvent)
                {
                    // Assume we need to start over, forgetting what we had
                    // acquired
                    setFilter(false, 0);
                }
            }

            // Dispatch sections
            if (sections != null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("XAIT section[" + sections.length + "] received");
                }

                savePersistentXait(sections); // ECR 1083

                // Parse the XAIT contained in the given sections
                XaitParser xaitParser = new XaitParser(Xait.NETWORK_SIGNALLING);
                xaitParser.parse(sections);
                Xait xait = (Xait)xaitParser.getSignalling();
                SystemEventUtil.logEvent("XAIT parsed");
                handleSignalling(xait, true, false);
            }
        }

        /* ================== ResourceStatusListener ================== */

        /**
         * Watches for resources being made available as well as the loss of
         * resources because of a tune.
         * <p>
         * We should not receive <code>ForcedDisconnectedEvent</code>s because
         * XAIT is acquired on an OOB <code>TransportStream</code>.
         * <p>
         * We will receive a <code>FilterResourcesAvailableEvent</code> when
         * filters become available. If we are currently waiting to reacquire
         * filtering resources (e.g., because they never were available or they
         * were taken from us), then we will set the filter(s).
         */
        public synchronized void statusChanged(ResourceStatusEvent event)
        {
            if (event instanceof FilterResourcesAvailableEvent && reacquire)
            {
                // Attempt to re-attach
                group.detach();
                if (setFilter(false, 0)) attach();
            }
            /*
             * else if (event instanceof ForcedDisconnectedEvent) { // Should
             * not encounter for XAIT // Watch for re-tune }
             */
            else
            {
                // Don't expect anything else...
                if (log.isInfoEnabled())
                {
                    log.info("Unexpected event: " + event);
                }
        }
        }

        /* ================== ResourceClient ================== */

        /**
         * @return <code>false</code>
         */
        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            return false;
        }

        /**
         * Notifies us that we are losing the resource. Remember this so that
         * when when it's available again we can re-initiate monitoring.
         */
        public synchronized void release(ResourceProxy proxy)
        {
            reacquire = true;
        }

        /**
         * Notifies us that we have <i>lost</i> the resource. Remember this so
         * that when when it's available again we can re-initiate monitoring.
         */
        public synchronized void notifyRelease(ResourceProxy proxy)
        {
            reacquire = true;
        }

        /**
         * Set to <code>true</code> to indicate that we need to reacquire
         * resources because the original access failed or we eventually lost
         * access to the resources.
         */
        private boolean reacquire = false;

        private SectionFilterGroup group;

        private TableSectionFilter filter;

        private int lastXaitVersion = -1;
    }

    /**
     * The <code>AitMonitor</code> is used to monitor the AIT for a given
     * service. The monitoring of the AIT is slightly more complicated than for
     * the XAIT because:
     * <ul>
     * <li>Signalling may be carried on more than one PID and it's possible for
     * the PID(s) to change over time.
     * <li>A tune away may occur (without service selection), requiring the
     * implementation to monitor the <code>NetworkInterface</code>(s) for a
     * re-tune.
     * </ul>
     * 
     * @see DavicSignallingMgr#createAitMonitor
     * @author Aaron Kamienski
     */
    private class AitMonitor extends SignallingMonitor implements SectionFilterListener, ResourceStatusListener,
            ResourceClient, NetworkInterfaceListener, TableChangeListener
    {
        /**
         * Construct an <code>AitMonitor</code> to monitor the service indicated
         * by the given <i>serviceId</i>
         * 
         * @param service
         */
        AitMonitor(OcapLocator service)
        {
            this.service = service;
        }

        /**
         * This method does the following to kick off AIT acquisition:
         * <ol>
         * <li>Retrieve PMT information asynchronously.
         * </ol>
         * When the PMT is acquired we can examine it for potential application
         * signalling descriptors which indicate the PID(s) on which the AIT may
         * be carried. At that time we will be able to begin filtering.
         */
        public synchronized void startMonitoring()
        {
            isMonitoring = true;

            // Determine the PID(s) containing the AIT
            // To do so initially, we must retrieve the PMT
            requestPMT();

            // We will be notified when the PMT is available
            // Once it is available, we will look up the PIDs and set filters
        }

        /**
         * <ol>
         * <li>Cancels filters (via {@link #cancelFilters}).
         * <li>Remove any outstanding listeners
         * </ol>
         */
        public synchronized void stopMonitoring()
        {
            cancelFilters();

            // Cancel any (potential) outstanding listeners

            // - SIRequest
            cancelPMT();

            // - PMT listener
            ProgramMapTableManager pmtMgr = ProgramMapTableManager.getInstance();
            pmtMgr.removeInBandChangeListener(this);

            // - NI listener(s)
            unwatchInterfaces();

            // Flag as stopped
            isMonitoring = false;
        }

        public synchronized void resignal()
        {
            if (filters != null)
            {
                for (Enumeration e = filters.elements(); e.hasMoreElements();)
                {
                    TableSectionFilter filter = (TableSectionFilter) e.nextElement();
                    filter.stopFiltering();
                    resetFilter(filter, false, 0);
                }
            }
        }

        /**
         * Requests the PMT so that we can determine the PIDs that may be
         * carrying the AIT. If the PMT cannot be retrieved, then
         * {@link #pmtNotAvailable} will be invoked. If th PMT can be retrieved,
         * then {@link #pmtRetrieved} will be invoked.
         */
        private synchronized void requestPMT()
        {
            // Don't do anything if monitoring has been cancelled
            if (!isMonitoring)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Monitoring cancelled, not requesting new PMT");
                }
                return;
            }

            // Cancel any previous request if necessary
            cancelPMT();

            ProgramMapTableManager pmtMgr = ProgramMapTableManager.getInstance();

            if (log.isDebugEnabled())
            {
                log.debug("Requesting PMT...");
            }

            // Flag used to determine if invoked synchronously
            class PMTRequestor implements SIRequestor
            {
                private final OcapLocator loc = service;

                public void notifyFailure(SIRequestFailureType reason)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("AIT not found (PMT not available for " + loc + ")");
                    }
                    pmtNotAvailable(this, reason);
                }

                public void notifySuccess(SIRetrievable[] result)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("PMT received for " + loc);
                    }
                    pmtRetrieved(this, result);
                }
            }
            OcapLocator newLoc = service;
            try
            {
	            SIManager manager = SIManager.createInstance();	            
	            Service s = manager.getService(newLoc);
	            if (s instanceof SPIService)
	            {	            	
	                ProviderInstance spi = (ProviderInstance) ((SPIService) s).getProviderInstance();	               
	                SelectionSessionWrapper session = (SelectionSessionWrapper) spi.getSelectionSession((SPIService)s);	                
	                newLoc = LocatorUtil.convertJavaTVLocatorToOcapLocator(session.getMappedLocator());
	            }
            }
            catch (Exception e)
            {
                
            }
            siRequestor = new PMTRequestor();
            // Retrieve In-band PMT
            siRequest = pmtMgr.retrieveInBand(siRequestor, newLoc);
        }

        /**
         * Cancels the outstanding inband PMT request, if any.
         * 
         * @see #requestPMT
         */
        private synchronized void cancelPMT()
        {
            siRequestor = null;
            if (siRequest != null) siRequest.cancel();
            siRequest = null;
        }

        /**
         * Cancels the given inband PMT request, if it is the current request.
         * 
         * @param requestor
         */
        private synchronized boolean cancelCurrentPMT(SIRequestor requestor)
        {
            if (requestor == siRequestor)
            {
                // May be redundant, but shouldn't be a problem.
                cancelPMT();
                return true;
            }
            return false;
        }

        /**
         * Sets up the filter(s) to monitor the AIT. If <i>pids</i> indicates
         * that there are no PIDs on which an AIT can be found, then any
         * existing filters are simply cancelled.
         * 
         * @param pids
         *            <code>Vector</code> of <code>PID</code>s indicating the
         *            PIDs to monitor for an AIT
         */
        private synchronized void setFilters(Vector pids)
        {
            cancelFilters();

            if (pids.size() <= 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("No AIT signaled on " + service + "; not filtering.");
                }

                return;
            }

            // Create a new group
            int nFilters = Math.min(MAX_PIDS, pids.size());
            group = new AITSectionFilterGroup(nFilters);
            group.addResourceStatusEventListener(this);

            // Remember this set of PIDs
            lastPids = pids;

            // Create filters, and start them
            filters = new Vector();
            filterToPidMap = new HashMap();

            int nSet = 0;
            for (int i = 0; nSet < nFilters && i < pids.size(); ++i)
            {
                PID pid = (PID) pids.elementAt(i);

                TableSectionFilter filter = group.newTableSectionFilter();
                filters.addElement(filter);
                filterToPidMap.put(filter, pid);

                filter.addSectionFilterListener(this);
                if (startFilter(filter, pid, false, 0))
                    ++nSet;
            }

            TransportStream ts = findTransportStream(service);
            if (ts == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Cannot filter for AIT: TransportStream unavailable for " + service);
                }
                watchInterfaces();
                return;
            }

            try
            {
                group.attach(ts, this, null);
            }
            catch (TuningException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Tuned away from TransportStream for " + service);
                }
                watchInterfaces();
                return;
            }
            catch (NotAuthorizedException e)
            {
                // AIT shouldn't be scrambled!
                SystemEventUtil.logRecoverableError("CA prevented filtering for AIT", e);
            }
            catch (InvalidSourceException e)
            {
                // Unexpected - this should be a real TS
                SystemEventUtil.logRecoverableError("Problems filtering for AIT", e);
            }
            catch (FilterResourceException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Cannot filter for AIT: filter attach failed", e);
                }

                // Wait until we can be notified that a filter is available
                reacquire = true;
            }

            if (log.isDebugEnabled())
            {
                log.debug("Setup " + nSet + " filters on pids: " + pids);
            }
        }

        /**
         * Reset a filter once it has been matched.
         */
        private synchronized void resetFilter(TableSectionFilter filter, boolean useVersion, int version)
        {

            filter.stopFiltering();

            // Workaround bug #1288 by not worrying about changes to AIT
            if (IGNORE_AIT_CHANGES)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Note: AIT versioning is ignored");
                }

                return;
            }
            PID pid = (PID) filterToPidMap.get(filter);
            if (log.isDebugEnabled())
            {
                log.debug("Resetting filter " + filter + " on PID " + pid);
            }

            if (pid != null)
            {
                startFilter(filter, pid, useVersion, version);
            }
        }

        /*
         * // Additional debugging of filters private String toString(byte[]
         * array) { if (array == null) return "null"; StringBuffer sb = new
         * StringBuffer("["); String sep = ""; for(int i = 0; i < array.length;
         * ++i) {
         * sb.append(sep).append("0x").append(Integer.toHexString(array[i]
         * &0xFF)); sep = ","; } return sb.toString(); }
         */

        /**
         * Initiates filtering for the given <i>pid</i>.
         * 
         * @param filter
         *            the filter to start
         * @param pid
         *            the pid on which to start the filter
         * @param useVersion
         *            if true, look for anything other than the given version
         * @param version
         *            given first
         * 
         * @return <code>true</code> if a filter was started; <code>false</code>
         *         otherwise
         */
        private boolean startFilter(TableSectionFilter filter, PID pid, boolean useVersion, int version)
        {
            try
            {
                // startFiltering
                // given PID=0x1FFC, table_id=0x74, and a positive filter
                // application_type==0x0001
                if (!POS_NEG_FILTER)
                {
                    if (FILTER_LOGGING)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("startFiltering(" + pid + ",0x74)");
                        }
                    }
                    filter.startFiltering(this, pid.pid, 0x74);
                }
                else if (!useVersion)
                {
                    if (FILTER_LOGGING)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("startFiltering(" + pid + ",0x74," + Arrays.toString(APP_TYPE_FILTER) + ","
                                + Arrays.toString(APP_TYPE_MASK) + ")");
                        }
                    }
                    filter.startFiltering(this, pid.pid, 0x74, APP_TYPE_FILTER, APP_TYPE_MASK);
                }
                else if (pid.version != version)
                {
                    // This starts 3 bytes into the section header.
                    // See DAVIC 1.4.1p9 E.8.2
                    byte[] version_filter = { 0, 0, (byte) (version << 1) };
                    byte[] version_mask = { 0, 0, 0x3e };

                    if (FILTER_LOGGING)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("startFiltering(" + pid + ",0x74," + Arrays.toString(APP_TYPE_FILTER) + ","
                                + Arrays.toString(APP_TYPE_MASK) + "," + Arrays.toString(version_filter) + ","
                                + Arrays.toString(version_mask) + ")");
                        }
                    }

                    filter.startFiltering(this, pid.pid, 0x74, APP_TYPE_FILTER, APP_TYPE_MASK, version_filter,
                            version_mask);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Not filtering due to optimized signaling on " + pid);
                    }
                }
                return true;
            }
            catch (ConnectionLostException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Possible tune-away prior to acquiring AIT", e);
                }
                reacquire = true;
                watchInterfaces();
            }
            catch (NotAuthorizedException e)
            {
                // Unexpected - don't expect CA to limit access to AIT
                SystemEventUtil.logRecoverableError("CA prevented filtering for AIT", e);
                reacquire = true;
            }
            catch (IllegalFilterDefinitionException e)
            {
                // Unexpected - as we should've created a good filter
                SystemEventUtil.logRecoverableError("Problem setting filter to acquire AIT", e);
                reacquire = true;
            }
            catch (FilterResourceException e)
            {
                // Unexpected - as the group should have enough
                SystemEventUtil.logRecoverableError("Insufficient filter resources for AIT", e);
                reacquire = true;
            } // try/catch

            return false;
        }

        /**
         * Examines the given <code>ProgramMapTable</code> and determines the
         * elementary stream PIDs on which an AIT may be carried. A
         * <code>Vector</code> containing <code>PID</code>'s specifying the PIDs
         * is returned.
         * 
         * @return <code>Vector</code> containing <code>PID</code>'s specifying
         *         the PIDs is returned
         */
        private Vector findPids(ProgramMapTable pmt)
        {
            Vector pids = new Vector();
            PMTElementaryStreamInfo[] streams = pmt.getPMTElementaryStreamInfoLoop();

            // foreach PMTElementaryStreamInfo...
            if (streams != null)
            {
                for (int pidIdx = 0; pidIdx < streams.length; ++pidIdx)
                {
                    // If might hold AIT...
                    if (streams[pidIdx].getStreamType() == StreamType.MPEG_PRIVATE_SECTION)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Found MPEG_PRIVATE_SECTION pid=" + streams[pidIdx].getElementaryPID());
                        }

                        // foreach descriptor in loop...
                        Descriptor[] loop = streams[pidIdx].getDescriptorLoop();
                        for (int descIdx = 0; descIdx < loop.length; ++descIdx)
                        {
                            // If application signalling descriptor...
                            if (loop[descIdx].getTag() == DescriptorTag.APPLICATION_SIGNALING)
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("Found APPLICATION_SIGNALING pid="
                                            + streams[pidIdx].getElementaryPID());
                                }

                                PID pid = new PID(streams[pidIdx].getElementaryPID(), getAITVersion(loop[descIdx]));
                                pids.addElement(pid);

                                break;
                            }
                            else
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("Ignoring tag=0x" + Integer.toHexString(loop[descIdx].getTag()));
                                }
                        }
                    }
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Ignoring streamType=0x" + Integer.toHexString(streams[pidIdx].getStreamType()));
                        }
                }
                }
                if (log.isDebugEnabled())
                {
                    log.debug("Done examining PMT, found AIT pids for filtering: " + pids);
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("stream was null");
                }
            }

            if (USE_DEFAULT_PID && pids.size() == 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Using default PID: " + Integer.toHexString(DEFAULT_PID));
                }
                pids.addElement(new PID(DEFAULT_PID, -1));
            }

            return pids;
        }

        /**
         * Examines the given <code>APP_SIGNALLING_DESCRIPTOR</code> for
         * <i>optimized</i> signalling. The return value indicates the version
         * of the AIT that is signalled on the given PID. If <code>-1</code>
         * then optimized signalling isn't present.
         * 
         * @param appDesc
         *            the app signalling descriptor
         * @return the version signalled in the PMT or -1 of optimized
         *         signalling isn't provided
         */
        private int getAITVersion(Descriptor appDesc)
        {
            if (SUPPORT_OPTIMIZED_SIGNALLING)
            {
                final byte[] content = appDesc.getContent();
                final int n = content.length / 3;

                // If optimized signalling is present...
                if (n > 0)
                {
                    int ofs = 0;
                    for (int i = 0; i < n; ++i, ofs += 3)
                    {
                        int application_type = ((content[ofs + 0] & 0xFF) << 8) | (content[ofs + 1] & 0xFF);

                        if (application_type == OcapAppAttributes.OCAP_J)
                        {
                            return content[ofs + 2] & 0x1F;
                        }
                    }

                }
            }

            return -1;
        }

        /**
         * Examines the <code>NetworkInterface</code>(s) available on the system
         * for the one currently tuned to a <code>TransportStream</code> that
         * carries the service specified by the given <i>service</i> locator.
         * 
         * @param svcLocator
         *            locator
         * @return the currently tuned-to <code>TransportStream</code> that
         *         carries the given service; or <code>null</code> if not found
         */
        private TransportStream findTransportStream(OcapLocator svcLocator)
        {
            // Get all transport streams that carry the requested service
            TransportStream[] ts = getTransportStreams(svcLocator);
            if (ts == null || ts.length == 0)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("StreamTable cannot find TransportStream for " + svcLocator);
                }

                return null;
            }

            // Get all network interfaces
            NetworkInterface[] ni = NetworkInterfaceManager.getInstance().getNetworkInterfaces();

            // Find a network interface that's tuned to one of these transport
            // streams
            for (int i = 0; i < ni.length; ++i)
            {
                TransportStream currTs = ni[i].getCurrentTransportStream();
                if (currTs == null)
                    continue;
                for (int j = 0; j < ts.length; ++j)
                {
                    if (ts[j].equals(currTs))
                        return ts[j];
                }
            }

            if (log.isDebugEnabled())
            {
                log.debug("Cannot find TransportStream for " + svcLocator);
            }
            return null;
        }

        /**
         * Examines the given <code>NetworkInterface</code> to see if it is
         * currently tuned to a <code>TransportStream</code> that carries the
         * given <i>service</i> locator.
         * 
         * @param svcLocator
         *            locator
         * @return the currently tuned-to <code>TransportStream</code> that
         *         carries the given service; or <code>null</code> if not found
         */
        private TransportStream findTransportStream(OcapLocator svcLocator, NetworkInterface ni)
        {
            // Get currently tuned transport stream
            TransportStream currTs = ni.getCurrentTransportStream();
            if (currTs == null) return null;

            // Check each transport stream which carries the service to see if
            // it is the
            // one which is tuned.
            TransportStream[] ts = null;
            if ((ts = getTransportStreams(svcLocator)) != null)
            {
                for (int j = 0; j < ts.length; ++j)
                {
                    if (ts[j].equals(currTs))
                        return ts[j];
                }
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("StreamTable cannot find TransportStream for " + svcLocator);
                }
            }

            if (log.isDebugEnabled())
            {
                log.debug("Cannot find TransportStream for " + svcLocator);
            }
            return null;
        }

        /**
         * Invokes {@link StreamTable#getTransportStreams}, returning
         * <code>null</code> if any exceptions are thrown.
         * 
         * @return array of transport streams or <code>null</code> if an
         *         exception was thrown
         */
        private TransportStream[] getTransportStreams(OcapLocator svcLocator)
        {
            try
            {
                return StreamTable.getTransportStreams(svcLocator);
            }
            catch (NetworkInterfaceException e)
            {
                return null;
            }
        }

        /**
         * Cancels all outstanding filterings and <i>forgets</i> any references
         * (clearing out the known <i>group</i> and filter set).
         */
        private synchronized void cancelFilters()
        {
            if (group != null)
            {
                group.detach();
                group.removeResourceStatusEventListener(this);
            }

            if (filters != null)
            {
                for (Enumeration e = filters.elements(); e.hasMoreElements();)
                {
                    TableSectionFilter filter = (TableSectionFilter) e.nextElement();
                    filter.removeSectionFilterListener(this);
                    filter.stopFiltering();
                }
            }

            group = null;
            filters = null;
        }

        /**
         * Invoked when it becomes necessary to wait for a re-tune to occur.
         * This will install listeners (this object) on each of the network
         * interfaces.
         * 
         * @see #receiveNIEvent
         */
        private synchronized void watchInterfaces()
        {
            // Do nothing if monitoring is stopped
            if (!isMonitoring) return;

            if (needsTune)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Listeners already installed on NetworkInterface(s)");
                }

                return;
            }
            needsTune = true;

            // Watch for re-tune
            NetworkInterface[] ni = NetworkInterfaceManager.getInstance().getNetworkInterfaces();
            for (int i = 0; i < ni.length; ++i)
            {
                ni[i].addNetworkInterfaceListener(this);
                // When re-tune occurs:
                // - unwatchInterfaces
                // - retrievePMT()
            }
        }

        /**
         * Called when we no longer need to watch interfaces (e.g., after a call
         * to {@link #receiveNIEvent} notifying us of a successful
         * <i>re-tune</i>).
         */
        private synchronized void unwatchInterfaces()
        {
            needsTune = false;

            // Remove listene
            NetworkInterface[] ni = NetworkInterfaceManager.getInstance().getNetworkInterfaces();
            for (int i = 0; i < ni.length; ++i)
            {
                ni[i].removeNetworkInterfaceListener(this);
            }
        }

        /**
         * Sends an empty AIT. This is invoked in response to various errors or
         * conditions that make an AIT inaccessible. This may be invoked under
         * the following conditions:
         * <ul>
         * <li>no AIT signalled in PMT
         * <li>PMT not accessible (due to lack of resources)
         * <li>Filter cannot be set (due to lack of resources)
         * </ul>
         * This simply invokes <code>handleSignalling()</code>.
         */
        private void sendEmptyAit()
        {
            AitImpl emptyAit = new AitImpl();
            emptyAit.initialize(0, new Vector(), new Vector(), new Hashtable(), new Hashtable());
            handleSignalling(emptyAit, false, false);
        }

        /**
         * Called when we could not acquire the PMT. Will do the following:
         * <ul>
         * <li>Wait for PMT changes.
         * <li>Wait for a re-tune if our service is not currently available.
         * </ul>
         * 
         * @param requestor
         *            the <code>SIRequestor</code> that originally made the
         *            request; to be compared with the current outstanding
         *            request
         * @param reason
         *            the reason given to the {@link SIRequestor#notifyFailure}
         *            method
         */
        private void pmtNotAvailable(SIRequestor requestor, SIRequestFailureType reason)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Could not receive PMT for reason: " + reason);
            }

            // If cancelled, it was in response to stopMonitoring() or
            // replacement.
            // simply don't do anything further
			// Added for findbugs issues fix
			// removed listeners null check as it is done down the call hierarchy
            if (!cancelCurrentPMT(requestor) || reason == SIRequestFailureType.CANCELED)
            {
                return;
            }

            // Either a tune away or resource problem occurred.
            // Send an empty AIT to clear any non-service bound apps
            sendEmptyAit();

            synchronized (this)
            {
                // Do nothing if monitoring has been cancelled
                if (!isMonitoring)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Monitoring cancelled, ignore PMT retrieval failure");
                    }

                    return;
                }

                // Determine if a tune-away occurred
                // If so, wait for re-tune
                TransportStream ts = findTransportStream(service);
                if (ts == null) // then wait for re-tune!
                {
                    watchInterfaces();
                }

                // Simply wait for changes in PMT...
                ProgramMapTableManager pmtMgr = ProgramMapTableManager.getInstance();
                pmtMgr.removeInBandChangeListener(this);
                pmtMgr.addInBandChangeListener(this, service);
            }
        }

        /**
         * Called to provide the <code>ProgramMapTable</code> associated with a
         * given <code>Service</code>. The PMT is inspected to determine the
         * PID(s) on which the AIT may be carried.
         * 
         * @param requestor
         *            the <code>SIRequestor</code> that originally made the
         *            request; to be compared with the current outstanding
         *            request
         * @param result
         *            the array of <code>SIRetrievable</code>s, one of which
         *            should be the desired PMT (actually would expect a single
         *            array containing the PMT)
         * 
         * @see #findPids
         * @see #setFilters
         */
        private synchronized void pmtRetrieved(SIRequestor requestor, SIRetrievable[] result)
        {
            // Do nothing if monitoring has been stopped
            if (!isMonitoring)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Monitoring has been cancelled, ignoring PMT");
                }

                return;
            }

            // Simply ignore if out-of-date request.
            if (!cancelCurrentPMT(requestor))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Ignoring old PMT request");
                }

                return;
            }

            boolean filtersSet = false;
            if (result != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Received SIRetrievable x" + result.length);
                }

                for (int i = 0; i < result.length; ++i)
                {
                    if (result[i] instanceof ProgramMapTable)
                    {
                        ProgramMapTable pmt = (ProgramMapTable) result[i];

                        if (log.isDebugEnabled())
                        {
                            log.debug("Received PMT " + pmt.getLocator());
                        }

                        // Can we verify that it's for the right service????
                        Vector pids = findPids(pmt);

                        if (pids.size() > 0)
                        {
                            // We have the PIDs, create the filters
                            setFilters(pids);
                            filtersSet = true;
                        }
                    }
                }
            }

            // If no AIT is signalled (and hence no filters have been set),
            // then send an empty AIT to clear out non-service_bound apps.
            if (!filtersSet) sendEmptyAit();

            // Add listener to wait for PMT changes...
            ProgramMapTableManager pmtMgr = ProgramMapTableManager.getInstance();
            pmtMgr.removeInBandChangeListener(this);
            pmtMgr.addInBandChangeListener(this, service);
        }

        /* ================== SectionFilterListener ================== */

        /**
         * Called when new sections are received for the set
         * <code>TableSectionFilter</code>. We are most interested in the
         * <code>EndOfFilteringEvent</code>, to which we respond by:
         * <ol>
         * <li>Acquiring the <code>Section</code>s of the table.
         * <li>Restarting filtering (with a negative filter for the new
         * version_number)
         * <li>Calling {@link SignallingMonitor#handleSignalling} to dispatch to
         * listeners
         * </ol>
         * Secondarily, we are interested in the
         * <code>VersionChangeDetectedEvent</code>, to which we respond by
         * restarting the filter. This is necessary because we may not see a
         * complete table for the previous version.
         * 
         * @param event
         */
        public void sectionFilterUpdate(SectionFilterEvent event)
        {
            Section[] sections = null;

            synchronized (this)
            {
                // Do nothing, if monitoring has been cancelled.
                if (!isMonitoring)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Monitoring cancelled, ignoring received sections");
                    }

                    return;
                }

                if (log.isDebugEnabled())
                {
                    log.debug("Received event " + event.getClass().getName() + " :: " + event);
                }

                // Make sure it's one of our current filters
                Vector currFilters = this.filters;
                if (currFilters == null || currFilters.indexOf(event.getSource()) < 0)
                {
                    return;
                }

                TableSectionFilter filter = (TableSectionFilter) event.getSource();
                PID pid = (PID) filterToPidMap.get(filter);

                // Receive table sections and restart filtering
                if (event instanceof EndOfFilteringEvent)
                {
                    try
                    {
                        sections = filter.getSections();
                    }
                    catch (FilteringInterruptedException e)
                    {
                        sections = null;
                    }

                    // Restart filtering (ignoring current version_number)
                    short version = 0;
                    boolean useVersion = false;
                    if (sections != null && sections.length > 0)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Received AIT with " + sections.length + " sections");
                        }
                        try
                        {
                            version = sections[0].version_number();
                            // Don't both if it's the previous version
                            if (version == pid.lastVersion)
                            {
                                sections = null;
                            }
                            else
                            {
                                pid.lastVersion = version;
                                pid.sections = sections;
                            }
                            useVersion = true;
                        }
                        catch (NoDataAvailableException e)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("Section data inaccessible", e);
                            }
                    }
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Received empty section set for AIT " + sections);
                        }
                    }

                    // Reset all filters with version information
                    resetFilter(filter, useVersion, version);
                }
                else if (event instanceof VersionChangeDetectedEvent)
                {
                    // Assume that we may have a partial table, and need to
                    // restart filtering.

                    // Only REALLY need to restart this filter...
                    // But if the version is changing it will likely change for
                    // others, so we'll reset them all!
                    resetFilter(filter, false, 0);
                    // ???? Should we ignore last version...?
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Unexpected event: " + event);
                    }
            }
            }

            // Dispatch sections
            if (sections != null)
            {
                if (lastPids.size() == 1)
                {
                    // If there's only one PID containing a AIT, go directly to
                    // the AIT parser.
                    if (log.isDebugEnabled())
                    {
                        log.debug("Calling handleSignalling with single subtable");
                    }

                    // Parse the AIT contained in the sections
                    AitParser aitParser = new AitParser();
                    aitParser.parse(sections);
                    handleSignalling(aitParser.getSignalling(), true, false);
                }
                else
                {
                    // Otherwise, bundle up all the AIT's we've seen, and send
                    // them along.
                    // BUG:? TODO:? FIXME:? Do we need to wait until we've
                    // retrieved all PID's?
                    // Or can we do this iteratively as each PID completes?
                    Vector aits = collectSections();

                    if (log.isDebugEnabled())
                    {
                        log.debug("Calling handleSignalling with multiple (" + aits.size() + ") subtables");
                    }

                    // Parse the AIT contained in the sections
                    AitParser aitParser = new AitParser();
                    aitParser.parse(aits);
                    handleSignalling(aitParser.getSignalling(), true, true);
                }
            }
        }

        /**
         * Collect all the sections we've accumulated for the various AIT's
         * signalled, and place them in a vector
         * 
         * @return The collection of all section sets.
         */
        private Vector collectSections()
        {
            if (lastPids == null)
            {
                return null;
            }
            if (log.isDebugEnabled())
            {
                log.debug("Collecting AIT sections from " + lastPids.size() + " PIDs");
            }
            Vector ret = new Vector();
            for (Enumeration e = lastPids.elements(); e.hasMoreElements();)
            {
                PID pid = (PID) e.nextElement();
                if (pid.sections != null)
                {
                    ret.add(pid.sections);
                }
            }
            return ret;
        }

        /* ================== ResourceStatusListener ================== */

        /**
         * Watches for resources being made available as well as the loss of
         * resources because of a tune.
         * <p>
         * We will receive a <code>ForcedDisconnectedEvent</code> when filtering
         * ends because of a <i>tune-away</i>. We add a listener to the
         * <code>NetworkInterface</code>s so that we can be notified when a
         * re-tune occurs.
         * <p>
         * We will receive a <code>FilterResourcesAvailableEvent</code> when
         * filters become available. If we are currently waiting to reacquire
         * filtering resources (e.g., because they never were available or they
         * were taken from us), then we will set the filter(s).
         */
        public synchronized void statusChanged(ResourceStatusEvent event)
        {
            // Do nothing if monitoring has been stopped
            if (!isMonitoring)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Monitoring has been cancelled, not responding to " + event);
                }

                return;
            }

            if (event instanceof FilterResourcesAvailableEvent && reacquire)
            {
                // Start all over again... using last known PIDs
                setFilters(lastPids);
            }
            else if (event instanceof ForcedDisconnectedEvent)
            {
                watchInterfaces();
            }
            else
            {
                // Don't expect anything else...
                if (log.isInfoEnabled())
                {
                    log.info("Unexpected event " + event);
                }
        }
        }

        /* ================== TableChangeListener ================== */

        /**
         * Called to notify us of a change in the PMT. The PMT is inspected to
         * determine the PID(s) on which the AIT may be carried. Filtering is
         * initiated on these PID(s).
         * 
         * @see #findPids
         * @see #setFilters
         */
        public synchronized void notifyChange(SIChangeEvent event)
        {
            // Do nothing if monitoring has been stopped
            if (!isMonitoring)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Monitoring has been cancelled, not responding to " + event);
                }

                return;
            }

            SIElement element = event.getSIElement();
            if (element instanceof ProgramMapTable)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("PMT change detected for " + ((ProgramMapTable) element).getLocator() + " " + event);
                }

                Vector pids = findPids((ProgramMapTable) element);

                // We have the PIDs, set the filters
                // (will cancel if no PIDs are found)
                setFilters(pids);
            }
        }

        /* ================== NetworkInterfaceListener ================== */

        /**
         * Will be called when tuning has completed. This method will check to
         * see if the <code>TransportStream</code> that contains the AIT, which
         * previously was tuned away from, has been re-tuned to.
         * <p>
         * Reinitiates the process of acquiring signalling by re-requesting the
         * PMT (as it may be different than what it was when we were previously
         * tuned to the <code>TransportStream</code>).
         */
        public void receiveNIEvent(NetworkInterfaceEvent event)
        {
            // We only care if tuning is complete...
            if (event instanceof NetworkInterfaceTuningOverEvent)
            {
                // If this TransportStream will work for us...
                TransportStream ts = findTransportStream(service, (NetworkInterface) event.getSource());
                if (ts != null)
                {
                    // Remove listeners
                    unwatchInterfaces();

                    // Start things over, by retrieving the PMT
                    requestPMT();

                    // We will be notified when the PMT is available
                    // Once it is available, we will look up the PIDs and set
                    // filters
                }
            }
        }

        /* ================== ResourceClient ================== */

        /**
         * @return <code>false</code>
         */
        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            return false;
        }

        /**
         * Notifies us that we are losing the resource. Remember this so that
         * when when it's available again we can re-initiate monitoring.
         */
        public synchronized void release(ResourceProxy proxy)
        {
            reacquire = true;
        }

        /**
         * Notifies us that we have <i>lost</i> the resource. Remember this so
         * that when when it's available again we can re-initiate monitoring.
         */
        public synchronized void notifyRelease(ResourceProxy proxy)
        {
            reacquire = true;
        }

        /* ================== Instance Vars ================== */

        /**
         * Represents information about an elementary stream where an AIT might
         * be found. A <code>PID</code> encapsulates the {@link #pid} of the
         * elementary stream and the {@link #version} (-1 if unknown) of the AIT
         * to be found there.
         * 
         * @author Aaron Kamienski
         */
        private class PID
        {
            PID(short pid, int version)
            {
                this.pid = pid;
                this.version = version;
            }

            public String toString()
            {
                if (version == -1)
                    return "0x" + Integer.toHexString(pid);
                else
                    return "0x" + Integer.toHexString(pid) + ":ver=" + version;
            }

            final short pid;

            final int version;

            int lastVersion = -1;

            Section[] sections = null;
        }

        /**
         * Set to <code>true</code> to indicate that we need to reacquire
         * resources because the original access failed or we eventually lost
         * access to the resources.
         */
        private boolean reacquire = false;

        /**
         * Set to <code>true</code> to indicate that listener(s) have been added
         * to the <code>NetworkInterface</code>(s) while awaiting a re-tune of
         * the a <code>TransportStream</code> carrying our service.
         */
        private boolean needsTune = false;

        /**
         * Set to <code>SIRequest</code> object returned by
         * <code>ProgramMapTableManager</code>'s <code>retrieveInBand()</code>
         * method. It is saved so that it may be called upon by
         * {@link #stopMonitoring} to cancel the request. The variable is set by
         * a call to {@link #requestPMT}. The variable is cleared by a call to
         * {@link #requestPMT} or {@link #stopMonitoring}.
         * 
         * @see #siRequestor
         */
        private volatile SIRequest siRequest = null;

        /**
         * Set to the <code>SIRequestor</code> object that is currently waiting
         * on a PMT. It is saved so that checks may be made against the current
         * outstanding request so that old requests may be ignored. The variable
         * is set by a call to {@link #requestPMT}. The variable is cleared by a
         * call to {@link #cancelPMT}.
         * 
         * @see #siRequest
         */
        private volatile SIRequestor siRequestor = null;

        /**
         * The <code>SectionFilterGroup</code> used to filter for an AIT. The
         * group is created to manage (at least) as many filters as there are
         * PIDs that may contain the AIT.
         */
        private SectionFilterGroup group;

        /**
         * A <code>Vector</code> of the set of currently set filters. Each
         * filter at index <i>i</i> is set on the <i>PID</i> with the same index
         * in {@link #lastPids}.
         */
        private Vector filters;

        private HashMap filterToPidMap;

        /**
         * The most recent set of known <i>PID</i>s that may contain the AIT
         * (according to the PMT).
         */
        private Vector lastPids;

        /**
         * The locator for the service for which AIT monitoring should be
         * performed.
         */
        private OcapLocator service;

        /**
         * A single flag that indicates whether monitoring is ongoing or not.
         * (This is used rather than keying off of other variables like
         * {@link #service} or {@link #listener}.
         */
        private boolean isMonitoring;
    }

    /**
     * Used to disable use of pos/neg filtering. Set to true to enable pos/neg
     * filtering, false to disable. If disabled, then we will receive EVERY
     * AIT/XAIT that comes in, and have to examine it before turning it down.
     * <p>
     * Set to <code>true</code> to make use of negative filters in order to
     * detect changes to AIT using filters. This had been set to
     * <code>false</code> because of bug #1288 (where negative filters weren't
     * working correctly).
     */
    private static final boolean POS_NEG_FILTER = true;

    /**
     * Used to disable watching for changes to the AIT. Set to true to disable
     * watching for changes to AIT. With this true the AIT will only be acquired
     * once upon startMonitoring().
     * <p>
     * When {@link #POS_NEG_FILTER} is <code>false</code>, then this should be
     * <code>true</code> (because we don't want to get every AIT that comes
     * along if we can't set the filter).
     */
    private static final boolean IGNORE_AIT_CHANGES = !POS_NEG_FILTER;

    /**
     * If set, then {@link #DEFAULT_PID} will be used if no AIT pids are found.
     */
    private static final boolean USE_DEFAULT_PID = false;

    /**
     * The default pid to be used when {@link #USE_DEFAULT_PID} is
     * <code>true</code> and no PIDs are found for the given service.
     */
    private static final short DEFAULT_PID = 0x1FFC;

    /**
     * The maximum number of PIDs to filter on.
     */
    private static final int MAX_PIDS = 3;

    /**
     * If set, then optimized signalling via the contents of the app signalling
     * descriptor is supported.
     * <p>
     * This is currently defined in terms of <code>"OCAP.ait.optim"</code>
     * MPEEnv. If <code>true</code> (the default), then optimized signalling is
     * supported. If optimized signalling is supported and present, then filters
     * will only be set when necessary.
     */
    private static final boolean SUPPORT_OPTIMIZED_SIGNALLING = "true".equals(MPEEnv.getEnv("OCAP.ait.optim", "true"));

    /**
     * Additional logging for set filters.
     */
    private static final boolean FILTER_LOGGING = true;

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(DavicSignallingMgr.class);

    /**
     * Bytes used to filter on <code>OCAP_J</code>-type AIT/XAIT. This starts 3
     * bytes into the section header.
     * 
     * @see "DAVIC 1.4.1p9 E.8.2"
     * 
     *      This array is of length 3 to match the length of the negative filter
     *      specified when the filter is started.
     * @see SectionFilter#startFiltering(Object, int, int, int, byte[], byte[],
     *      byte[], byte[])
     */
    private static final byte[] APP_TYPE_FILTER = { 0, OcapAppAttributes.OCAP_J, 0 };

    /**
     * Mask bytes used to filter on <code>OCAP_J</code>-type AIT/XAIT. This
     * starts 3 bytes into the section header.
     * 
     * @see "DAVIC 1.4.1p9 E.8.2"
     * 
     *      This array is of length 3 to match the length of the negative mask
     *      specified when the filter is started.
     * @see SectionFilter#startFiltering(Object, int, int, int, byte[], byte[],
     *      byte[], byte[])
     */
    private static final byte[] APP_TYPE_MASK = { (byte) 0xFF, (byte) 0xFF, 0 };
}
