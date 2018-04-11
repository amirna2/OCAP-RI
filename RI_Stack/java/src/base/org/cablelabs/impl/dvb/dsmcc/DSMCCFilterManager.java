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
package org.cablelabs.impl.dvb.dsmcc;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TransportStream;
import org.davic.mpeg.TuningException;
import org.davic.mpeg.sections.ConnectionLostException;
import org.davic.mpeg.sections.EndOfFilteringEvent;
import org.davic.mpeg.sections.FilterResourceException;
import org.davic.mpeg.sections.FilteringInterruptedException;
import org.davic.mpeg.sections.IllegalFilterDefinitionException;
import org.davic.mpeg.sections.InvalidSourceException;
import org.davic.mpeg.sections.NoDataAvailableException;
import org.davic.mpeg.sections.RingSectionFilter;
import org.davic.mpeg.sections.Section;
import org.davic.mpeg.sections.SectionAvailableEvent;
import org.davic.mpeg.sections.SectionFilter;
import org.davic.mpeg.sections.SectionFilterEvent;
import org.davic.mpeg.sections.SectionFilterException;
import org.davic.mpeg.sections.SectionFilterGroup;
import org.davic.mpeg.sections.SectionFilterListener;
import org.davic.mpeg.sections.SimpleSectionFilter;
import org.davic.net.Locator;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.net.tuning.StreamTable;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.davic.mpeg.TransportStreamExt;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * Manage filtering for DSMCC Stream Events and NPT descriptors.
 * 
 */
public class DSMCCFilterManager implements Manager, NetworkInterfaceListener
{
    static private DSMCCFilterManager m_instance = null;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(DSMCCFilterManager.class.getName());

    // Stream event table ID (for filtering)
    private static final int STREAM_EVENT_TABLE = 0x3d;

    // Index offsets for accessing section descriptor information.
    private static final int SECTION_OFFSET_DESCRIPTORS = 8;

    // Different descriptors for reading from section data
    private static final byte NPT_REFERENCE_DESCRIPTOR = 23;

    private static final byte NPT_ENDPOINT_DESCRIPTOR = 24; // Unused descriptor
                                                            // in MHP

    private static final byte STREAM_MODE_DESCRIPTOR = 25; // Unused descriptor
                                                           // in MHP

    private static final byte STREAM_EVENT_DESCRIPTOR = 26;

    private static final int TID_SECTION_EVENT_DOITNOW = 0x0000;

    private static final int TID_SECTION_NPT_REF = 0x4000;

    private static final int TID_SECTION_EVENT_SCHEDULED = 0x8000;

    private static final int TID_SECTION_MASK = 0xc000;

    private static final int ENI_PRIORITY = 1;

    private static final int RING_SIZE = 64;

    private static final int SECTION_SIZE = 4096;

    private static final int CRC_SIZE = 4;

    private static final int MAX_PID_FILTERS = 8;

    /**
     * Make the constructor private so nobody will invoke it.
     */
    private DSMCCFilterManager()
    {
    }

    /**
     * Get the singleton instance of the DSMCCFilterManager.
     * 
     * @return The DSMCCFilterManager.
     */
    public static synchronized DSMCCFilterManager getInstance()
    {
        if (m_instance == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DSMCCFilterManager: Constructing singleton");
            }
            m_instance = new DSMCCFilterManager();
        }
        return m_instance;
    }

    /**
     * Attach this class as a listener to every NetworkInterface to allow us to
     * receive events when the network interface is tuning.
     */
    private void attachListeners()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Attaching DSMCCFilterManager to NetworkInterface's");
        }
        NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
        NetworkInterface[] nis = nim.getNetworkInterfaces();
        // Attach as a listener to each network interface.
        for (int i = 0; i < nis.length; i++)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Adding listener to NI: " + nis[i]);
            }
            nis[i].addNetworkInterfaceListener(this);
        }
    }

    /**
     * Remove this class as a listener from the NetworkInterfaces if we're no
     * longer listening to anything.
     */
    private void detachListeners()
    {
        {
            if (log.isDebugEnabled())
            {
                log.debug("Detaching DSMCCFilterManager from NetworkInterfaces");
            }
            NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
            NetworkInterface[] nis = nim.getNetworkInterfaces();
            // Attach as a listener to each network interface.
            for (int i = 0; i < nis.length; i++)
            {
                nis[i].removeNetworkInterfaceListener(this);
            }
        }
    }

    /**
     * A hashtable of all the services currently in place.
     */
    private Hashtable services = new Hashtable();

    /**
     * Find a transport stream which is tuned to a given locator.
     * 
     * @param loc
     *            The locator to look for.
     * @param streams
     *            A list of streams to look in.
     * 
     * @return The TransportStream object corresponding to a
     */
    private TransportStream getTransportStream(Locator loc, TransportStream streams[])
    {
        if (log.isDebugEnabled())
        {
            log.debug("Looking for transport stream for " + loc + " on " + streams.length + " locations");
        }

        for (int i = 0; i < streams.length; i++)
        {
            TransportStreamExt stream = (TransportStreamExt) streams[i];
            TransportStreamExt currentStream = (TransportStreamExt) stream.getNetworkInterface()
                    .getCurrentTransportStream();
            if (log.isDebugEnabled())
            {
                String output;
                if (currentStream == null)
                    output = "empty";
                else
                    output = currentStream.getLocator().toExternalForm();
                log.debug("Checking " + i + " NI: " + stream.getNetworkInterface() + " Current TS: " + output);
            }
            if (currentStream != null && currentStream.equals(stream))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Found tuned transport stream for: " + loc + " on NI " + stream.getNetworkInterface());
                }
                return stream;
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("Did not find transport stream for: " + loc);
        }
        return null; // Return transport stream or null
    }

    /**
     * Get data related to a given Service, namely the service which contains an
     * object carousel.
     * 
     * @param oc
     *            The carousel we're interested in.
     * @return The ServiceData object for the service containing the carousel.
     * @throws NetworkInterfaceException
     */
    private synchronized ServiceData getServiceData(ObjectCarousel oc) throws NetworkInterfaceException
    {
        Service ser = oc.getService();
        ServiceData sd = (ServiceData) services.get(ser);
        if (sd == null)
        {
            sd = new ServiceData(ser);
            services.put(ser, sd);
        }
        return sd;
    }

    /**
     * Get the PIDData structure associated with an object carousel/association
     * tag pair.
     * 
     * @param oc
     * @param tag
     * @return
     * @throws NetworkInterfaceException
     */
    private synchronized PIDData getPIDData(ObjectCarousel oc, int tag) throws NetworkInterfaceException
    {
        ServiceData sd = getServiceData(oc);
        PIDData pid = sd.getPIDData(tag);
        return pid;
    }

    /**
     * Add an event on a pid, and start filtering for it.
     * 
     * @param ev
     *            The event in question.
     * 
     * @throws MPEGDeliveryException
     * @throws TuningException
     * @throws NotAuthorizedException
     * @throws SIRequestException
     * @throws InterruptedException
     * @throws SectionFilterException
     * @throws NetworkInterfaceException
     */
    public void addFilter(DSMCCFilterableObject ev) throws MPEGDeliveryException, TuningException,
            NotAuthorizedException, SIRequestException, InterruptedException, SectionFilterException,
            NetworkInterfaceException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Add Filter: " + ev);
        }
        ObjectCarousel oc = ev.getObjectCarousel();
        int tag = ev.getAssociationTag();

        if (services.size() == 0)
        {
            attachListeners();
        }

        // TODO: Clean this up. It's just kind of ugly right now.
        PIDData pd = getPIDData(oc, tag);
        pd.addFilter(ev);
        pd.buildFilters();
        try
        {
            pd.setFilters();
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to set DSMCCFilters in ...", e);
            }
        }
    }

    /**
     * Remove an event record, and if necessary, stop filtering.
     * 
     * @param ev
     * @throws NetworkInterfaceException
     */
    public void removeFilter(DSMCCFilterableObject ev) throws NetworkInterfaceException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Remove filter: " + ev);
        }

        ObjectCarousel oc = ev.getObjectCarousel();
        int tag = ev.getAssociationTag();

        PIDData pd = getPIDData(oc, tag);
        pd.removeFilter(ev);
        pd.buildFilters();
        try
        {
            pd.setFilters();
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to set DSMCCFilters in ...", e);
            }
        }
        if (services.size() == 0)
        {
            detachListeners();
        }
    }

    /**
     * Track all data relating to a given service. Contains a list of all
     * PIDData objects, which contain the actual references to the actual
     * filterable objects and filters. This turns the lot of them on and off,
     * primarily.
     */
    private class ServiceData implements ResourceClient, NetworkInterfaceCallback
    {
        Service m_service;

        OcapLocator m_loc;

        SectionFilterGroup m_filterGroup;

        Hashtable m_pids = new Hashtable();

        Vector m_filters = new Vector();

        TransportStreamExt m_ts = null;

        ExtendedNetworkInterface m_eni = null;

        TransportStream m_streams[] = null;

        boolean m_connected = false;

        /**
         * Create a new ServiceData object.
         * 
         * @param s
         *            The service we're basing this on.
         * @throws NetworkInterfaceException
         */
        ServiceData(Service s) throws NetworkInterfaceException
        {
            m_service = (ServiceExt) s;
            m_loc = (OcapLocator) m_service.getLocator();
            m_streams = StreamTable.getTransportStreams(m_loc);
            if (log.isDebugEnabled())
            {
                log.debug("Creating new service object for service at: " + m_loc.toString());
            }

            m_filterGroup = new SectionFilterGroup(MAX_PID_FILTERS, true); // TODO:
                                                                           // What's
                                                                           // the
                                                                           // right
                                                                           // value?
            m_pids = new Hashtable();
        }

        /**
         * Connect this service data object to a stream to start filtering.
         * 
         * @throws MPEGDeliveryException
         * @throws NotAuthorizedException
         * @throws TuningException
         * @throws SectionFilterException
         * @throws NetworkInterfaceException
         */
        synchronized boolean connect() throws MPEGDeliveryException, NotAuthorizedException, TuningException,
                SectionFilterException
        {
            if (log.isDebugEnabled())
            {
                log.debug("Attempting to connect ServiceData group.  Current state:" + m_connected);
            }

            if (!m_connected)
            {
                TransportStreamExt ts = (TransportStreamExt) getTransportStream(m_loc, m_streams);
                if (ts == null)
                {
                    return false;
                    // throw new MPEGDeliveryException("Transport stream for " +
                    // m_loc + " not connected");
                }
                if (log.isDebugEnabled())
                {
                    log.debug("Connecting to transport stream " + m_loc);
                }
                connect(ts);
            }
            return m_connected;
        }

        /**
         * Connect this service to the specified transport stream.
         * 
         * @param ts
         *            The transport stream to connect to.
         * @throws FilterResourceException
         * @throws InvalidSourceException
         * @throws TuningException
         * @throws NotAuthorizedException
         */
        synchronized void connect(TransportStreamExt ts) throws FilterResourceException, InvalidSourceException,
                TuningException, NotAuthorizedException
        {
            if (!m_connected)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Connecting to transport stream: " + ts.getNetworkInterface());
                }
                m_filterGroup.attach(ts, this, null);
                ExtendedNetworkInterface newNi = (ExtendedNetworkInterface) ts.getNetworkInterface();
                // If we already have a network interface set, and we're not on
                // it,
                // Remove ourselves as a listener
                if (m_eni != newNi)
                {
                    if (m_eni != null)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Removing network interface callback");
                        }
                        m_eni.removeNetworkInterfaceCallback(this);
                    }
                    if (log.isDebugEnabled())
                    {
                        log.debug("Setting network interface callback");
                    }
                    newNi.addNetworkInterfaceCallback(this, ENI_PRIORITY);
                    m_eni = newNi;
                }
                m_connected = true;
                startFilters();
            }
        }

        /**
         * Disconnect the filtering stream.
         */
        synchronized void disconnect()
        {
            if (log.isDebugEnabled())
            {
                log.debug("Disconnecting ServiceData group");
            }

            m_connected = false;
            stopFilters();
            m_filterGroup.detach();
        }

        /**
         * Stop the filters in this service. Walk the list of PID's, and for
         * each one, stop the filters within it.
         */
        private void stopFilters()
        {
            Enumeration keys = m_pids.keys();
            while (keys.hasMoreElements())
            {
                Object k = keys.nextElement();
                PIDData p = (PIDData) m_pids.get(k);
                p.stopFilters();
            }
        }

        /**
         * Start all the filters in the varias PID's.
         */
        private void startFilters()
        {
            Enumeration keys = m_pids.keys();
            while (keys.hasMoreElements())
            {
                Object k = keys.nextElement();
                PIDData p = (PIDData) m_pids.get(k);
                try
                {
                    p.setFilters();
                }
                catch (Exception e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Caught exception starting filters: " + e.getMessage(), e);
                    }
                }
            }
        }

        /**
         * Find a PIDData structure based on the association tag for that PID.
         * Create a new one if one doesn't already exist.
         * 
         * @param tag
         *            The Association tag to look for.
         * @return The appropriate PIDData.
         */
        PIDData getPIDData(int tag)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Searching for tag " + tag + " in service " + m_loc);
            }
            Integer iTag = new Integer(tag);
            PIDData pid = (PIDData) m_pids.get(iTag);

            if (pid == null)
            {
                pid = new PIDData(this, tag);

                m_pids.put(iTag, pid);
            }
            return pid;
        }

        /**
         * Remove a PIDData object from this service. If this was the final
         * service, shut down all the filtering for this service.
         * 
         * @param data
         *            The PIDData item to remove.
         */
        void removePIDData(PIDData data)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Removing PID " + data.m_tag + " from service " + m_service);
            }
            m_pids.remove(data);
            if (m_pids.size() == 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("All PID data removed.  Removing Service : " + m_service);
                }
                // Added synchronization block for findbugs issues fix
                synchronized(this)
                {
                    m_eni.removeNetworkInterfaceCallback(this);
                }
                services.remove(this);
            }
        }

        /**
         * Get the PID which corresponds to a tag at this point in time. Not the
         * PIDData, but the actual integer PID.
         * 
         * @param tag
         *            The association tag.
         * @return The PID that corresponds to that association tag.
         * @throws SIRequestException
         *             Thrown if association tag cannot be translated.
         * @throws InterruptedException
         */
        int getPID(int tag) throws SIRequestException, InterruptedException
        {
            ServiceDetailsExt sd = (ServiceDetailsExt) ((ServiceExt) m_service).getDetails();
            ServiceComponentExt comp = (ServiceComponentExt) (sd.getComponentByAssociationTag(tag));
            return comp.getPID();
        }

        // Implementation of ResourceClient
        public void notifyRelease(ResourceProxy proxy)
        {
        }

        public void release(ResourceProxy proxy)
        {
        }

        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            return false;
        }

        // Implementation of NetworkInterfaceCallback
        /**
         * Receive notification of tune pending, and detach.
         */
        public void notifyTunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyTunePending called");
            }
            // Added synchronization block for findbugs issues fix
            synchronized(this)
            {
                if (ni != m_eni)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Ignoring.  Wrong NI");
                    }
                    return;
                }
            }
            disconnect();
        }

        /**
         * Recevie notification of tune complete. Ignore.
         */
        public void notifyTuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success, boolean synced)
        {
            // TODO Auto-generated method stub
            if (log.isDebugEnabled())
            {
                log.debug("notifyTuneComplete called");
            }
        }

        /**
         * Receive notification of retune occurring. Detach.
         */
        public void notifyRetunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyRetunePending called");
            }
            // Added synchronization block for findbugs issues fix
            synchronized(this)
            {
                if (ni != m_eni)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Ignoring.  Wrong NI");
                    }
                    return;
                }
            }
            disconnect();
        }

        /**
         * Receive notification of retune complete. If successful, reattach on
         * the new location.
         */
        public void notifyRetuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success,
                boolean synced)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyRetuneComplete called");
            }
            // Added synchronization block for findbugs issues fix
            synchronized(this)
            {
                if (success && m_eni == ni)
                {
                    try
                    {
                        // Service changed, so we need to get a new list of
                        // transport stream objects
                        m_streams = StreamTable.getTransportStreams(m_loc);
                        connect();
                    }
                    catch (Exception e)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Caught exception while reconnecting", e);
                        }
                    }
                }
            }
        }

        /**
         * Receive notification that the service has disappeared. Detach.
         */
        public void notifyUntuned(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyUntuned called");
            }
            // Added synchronization block for findbugs issues fix
            synchronized(this)
            {
                if (ni != m_eni)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Ignoring.  Wrong NI");
                    }
                    return;
                }
            }
            // TODO: Should we disconnect here?
            disconnect();
        }

        public void notifySyncAcquired(ExtendedNetworkInterface ni, Object tuneInstance)
        {
        }

        public void notifySyncLost(ExtendedNetworkInterface ni, Object tuneInstance)
        {
        }

        /**
         * Returns whether or not this ServiceData object is currently
         * connected.
         * 
         * @return True if connected, false otherwise.
         */
        boolean isConnected()
        {
            return m_connected;
        }
    }

    // Section Header:
    // 1 byte: Table ID
    // 2 bytes: Section Syntax + Private + Length
    // 2 bytes: Table ID Extension
    // 1 byte: version

    private static final int VERSION_FILTER_LEN = 3;

    private static final int TRANSID_FILTER_LEN = 3;

    private static final int VERSION_POS = 2;

    private static final int TRANSID_POS = 0;

    private byte[] versionMask = {/* 0x00 0x00 0x00 */(byte) 0x00, (byte) 0x00, (byte) 0x3e };

    private class Filter
    {
        SectionFilter sf;

        SectionFilterGroup group;

        byte[] posMatch = null;

        byte[] posMask = null;

        byte[] negMatch = null;

        byte[] negMask = null;

        int lastPid = -1;

        boolean filtering = false;

        /**
         * Create a filter for a single
         * 
         * @param obj
         * @param grp
         */
        Filter(DSMCCFilterableObject obj, SectionFilterGroup grp, SectionFilterListener listener)
        {

            group = grp;
            boolean matchAllBits = false;
            if (obj instanceof DSMCCEventRecord)
            {
                matchAllBits = !((DSMCCEventRecord) obj).isScheduled();
            }
            if (log.isDebugEnabled())
            {
                log.debug("Creating new specific filter: " + obj.getID() + " : " + matchAllBits);
            }

            setTransIDFilter((short) obj.getID(), matchAllBits);
            sf = group.newSimpleSectionFilter();
            sf.addSectionFilterListener(listener);
        }

        Filter(SectionFilterGroup grp, SectionFilterListener listener, int size)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Creating new global filter");
            }
            group = grp;
            sf = group.newRingSectionFilter(size);
            sf.addSectionFilterListener(listener);
        }

        void startFilter(int pid) throws FilterResourceException, IllegalFilterDefinitionException,
                ConnectionLostException, NotAuthorizedException
        {
            if (!filtering)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Starting filter on pid " + pid + " :: " + (posMatch != null) + " : "
                            + (negMatch != null));
                }
                if (posMatch != null)
                {
                    dumpArray("PosMatch: ", posMatch);
                    dumpArray("PosMask : ", posMask);
                }
                if (negMatch != null)
                {
                    dumpArray("NegMatch: ", negMatch);
                    dumpArray("NegMask : ", negMask);
                }
                lastPid = pid;
                sf.startFiltering(this, pid, STREAM_EVENT_TABLE, posMatch, posMask, negMatch, negMask);
                filtering = true;
            }
        }

        void stopFilter()
        {
            if (filtering)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Stopping filter");
                }
                sf.stopFiltering();
                filtering = false;
            }
        }

        void setVersionFilter(byte version)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Setting version mask: " + version);
            }

            if (negMask == null)
            {
                negMask = versionMask;
                negMatch = new byte[VERSION_FILTER_LEN];
            }
            negMatch[VERSION_POS] = (byte) ((version & (byte) 0x1f) << 1);
        }

        void clearVersionFilter()
        {
            negMask = null;
            negMatch = null;
        }

        void setTransIDFilter(short transID, boolean matchAllBits)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Setting Transaction ID " + transID + " : " + matchAllBits);
            }

            if (posMask == null)
            {
                posMask = new byte[TRANSID_FILTER_LEN];
                posMatch = new byte[TRANSID_FILTER_LEN];
            }
            byte b = (byte) (transID >> 8);
            posMatch[TRANSID_POS] = b;
            b = (byte) transID;
            posMatch[TRANSID_POS + 1] = b;
            if (matchAllBits)
            {
                posMask[TRANSID_POS] = (byte) 0xff;
                posMask[TRANSID_POS + 1] = (byte) 0xff;
            }
            else
            {
                posMask[TRANSID_POS] = (byte) 0xc0; // 1100 0000
                posMask[TRANSID_POS] = (byte) 0x00;
            }
        }

        boolean isFiltering()
        {
            return filtering;
        }
    }

    /**
     * This class represents all that can be signalled on a given PID. Manages
     * the setting of filters, tracking of the version number, and parsing of
     * the table, and signalling of the data back to the calling application.
     * 
     * Actually, it represents a single association tag value, and if multiple
     * association tags map to the same PID, they will not be combined, but
     * that's a pathological case.
     */
    private class PIDData implements SectionFilterListener
    {
        int m_tag;

        ServiceData m_sd;

        Vector m_doItNowEvents = new Vector();

        Vector m_scheduledEvents = new Vector();

        Vector m_npts = new Vector();

        boolean m_usingGlobal = false;

        Hashtable m_doItNowFilters = new Hashtable();

        Filter m_nptFilter = null;

        Filter m_scheduledFilter = null;

        Filter m_globalFilter = null;

        PIDData(ServiceData s, int t)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Creating new PID Data element for tag " + t + "in service " + s.m_loc);
            }
            m_tag = t;
            m_sd = s;
        }

        /**
         * Build the filters for this PID.
         */
        void buildFilters()
        {
            // Added synchronization block for findbugs issues fix
            synchronized(this)
            {
            if (totalFilters() > MAX_PID_FILTERS)
            {
                if (!m_usingGlobal)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Switching to global filter");
                    }
                    stopFilters();
                    m_usingGlobal = true;
                }
            }
            else
            {
                if (m_usingGlobal)
                {

                    if (log.isDebugEnabled())
                    {
                        log.debug("Switching to individual filters");
                    }
                    stopFilters();
                    m_usingGlobal = false;
                }

            }
            if (!m_usingGlobal)
            {
                for (int i = 0; i < m_doItNowEvents.size(); i++)
                {
                    DSMCCEventRecord ev = (DSMCCEventRecord) m_doItNowEvents.get(i);
                    if (m_doItNowFilters.get(ev) == null)
                    {
                        Filter f = new Filter(ev, m_sd.m_filterGroup, this);
                        m_doItNowFilters.put(ev, f);
                    }
                }
                if (!m_scheduledEvents.isEmpty() && m_scheduledFilter == null)
                {
                    m_scheduledFilter = new Filter((DSMCCEventRecord) m_scheduledEvents.get(0), m_sd.m_filterGroup,
                            this);
                }
                if (!m_npts.isEmpty() && m_nptFilter == null)
                {
                    m_nptFilter = new Filter((DSMCCFilterableObject) m_npts.get(0), m_sd.m_filterGroup, this);
                }
            }
            else
            {
                if (m_globalFilter == null)
                {
                    m_globalFilter = new Filter(m_sd.m_filterGroup, this, RING_SIZE);
                }
            }
            }
        }

        /**
         * Figure out the total number of filters needed, should we filter for
         * each event separately.
         * 
         * @return The total number of filters.
         */
        int totalFilters()
        {
            int ret = 0;
            // Total filters is 1 for each do it now, 1 for all scheduled, and 1
            // for all NPT's.
            ret = m_doItNowEvents.size() + (m_npts.isEmpty() ? 0 : 1) + (m_scheduledEvents.isEmpty() ? 0 : 1);
            return ret;
        }

        /**
         * Turn on any filters engaged on a PID.
         * 
         * @throws NotAuthorizedException
         * @throws SIRequestException
         * @throws InterruptedException
         * @throws NetworkInterfaceException
         * @throws SectionFilterException
         * @throws TuningException
         * @throws MPEGDeliveryException
         */
        synchronized void setFilters() throws NotAuthorizedException, SIRequestException, InterruptedException,
                MPEGDeliveryException, TuningException, SectionFilterException, NetworkInterfaceException
        {
            if (!m_sd.isConnected())
            {
                if (!m_sd.connect())
                {
                    throw new MPEGDeliveryException("Unable to connect to stream");
                }
            }
            int pid = m_sd.getPID(m_tag);
            if (m_usingGlobal)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Setting global filter for tag " + m_tag + " on pid " + pid + " in " + m_sd.m_loc);
                }
                m_globalFilter.startFilter(pid);
            }
            else
            {
                for (int i = 0; i < m_doItNowEvents.size(); i++)
                {
                    DSMCCEventRecord ev = (DSMCCEventRecord) m_doItNowEvents.get(i);
                    if (log.isDebugEnabled())
                    {
                        log.debug("Setting Do It Now Filter for event " + i + ":: " + ev.getName() + " (" + ev.getID()
                                + ")");
                    }
                    Filter f = (Filter) m_doItNowFilters.get(ev);
                    f.startFilter(pid);
                }
                if (m_nptFilter != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Starting NPT filter");
                    }
                    m_nptFilter.startFilter(pid);
                }
                if (m_scheduledFilter != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Starting Scheduled Event filter");
                    }
                    m_scheduledFilter.startFilter(pid);
                }
            }
        }

        /**
         * Disable all filters associated with this PID.
         */
        synchronized void stopFilters()
        {
            if (log.isDebugEnabled())
            {
                log.debug("Stopping  filter on tag " + m_tag + " in " + m_sd.m_loc);
            }
            if (m_usingGlobal)
            {
                if (m_globalFilter != null)
                {
                    m_globalFilter.stopFilter();
                }
            }
            else
            {
                for (int i = 0; i < m_doItNowEvents.size(); i++)
                {
                    Filter f = (Filter) m_doItNowFilters.get(m_doItNowEvents.get(i));
                    if (f != null)
                    {
                        f.stopFilter();
                        f.clearVersionFilter();
                    }
                }
                if (m_scheduledFilter != null)
                {
                    m_scheduledFilter.stopFilter();
                    m_scheduledFilter.clearVersionFilter();
                }
                if (m_nptFilter != null)
                {
                    m_nptFilter.stopFilter();
                    m_nptFilter.clearVersionFilter();
                }
            }
        }

        /**
         * Add a filterable object to this PID.
         * 
         * @param ev
         *            The object/event we want to filter on.
         * 
         * @throws MPEGDeliveryException
         * @throws TuningException
         * @throws NotAuthorizedException
         * @throws SIRequestException
         * @throws InterruptedException
         * @throws SectionFilterException
         * @throws NetworkInterfaceException
         */
        synchronized void addFilter(DSMCCFilterableObject ev) throws MPEGDeliveryException, TuningException,
                NotAuthorizedException, SIRequestException, InterruptedException, SectionFilterException,
                NetworkInterfaceException
        {
            if (log.isDebugEnabled())
            {
                log.debug("addFilter (" + m_sd.m_loc + ", " + m_tag + "): " + ev + " Listeners: "
                        + m_doItNowEvents.size());
            }
            Vector eventVector = getVector(ev);

            if (!eventVector.contains(ev))
            {
                eventVector.add(ev);
            }
        }

        /**
         * Remove a filterable object from this PID.
         * 
         * @param ev
         *            The object/event to be removed.
         */
        synchronized void removeFilter(DSMCCFilterableObject ev)
        {
            if (log.isDebugEnabled())
            {
                log.debug("removeFilter (" + m_sd.m_loc + ", " + m_tag + "): " + ev + " Listeners: "
                        + m_doItNowEvents.size());
            }

            Vector v = getVector(ev);
            v.remove(ev);
            Filter f = (Filter) m_doItNowFilters.get(ev);
            if (f != null)
            {
                f.stopFilter();
            }
            if (m_scheduledEvents.isEmpty() && m_scheduledFilter != null)
            {
                m_scheduledFilter.stopFilter();
            }
            if (m_npts.isEmpty() && m_nptFilter != null)
            {
                m_nptFilter.stopFilter();
            }
        }

        /**
         * Return the appropriate vector to store this filterable object.
         * 
         * @param ev
         *            The object to lookup.
         * @return The appropriate vector to hold this object.
         */
        Vector getVector(DSMCCFilterableObject ev)
        {
            if (ev instanceof DSMCCEventRecord)
            {
                DSMCCEventRecord dev = (DSMCCEventRecord) ev;
                if (dev.isScheduled())
                {
                    return m_scheduledEvents;
                }
                else
                {
                    return m_doItNowEvents;
                }
            }
            else
            {
                return m_npts;
            }
        }

        /**
         * Process a section filtering event when the section hardware indicates
         * that a new section has arrived. Parse the TableIdExtension field to
         * determine which type of section, and process appropriately.
         * 
         * @param ev
         *            The SectionFilterEvent that occurred.
         */
        public void sectionFilterUpdate(SectionFilterEvent ev)
        {
            Section[] sections = null;
            Section s = null;

            if (log.isDebugEnabled())
            {
                log.debug("Got section filtering event: " + ev.getClass().getName());
            }

            Filter f = (Filter) ev.getAppData();

            if (ev instanceof SectionAvailableEvent)
            {
                SectionFilter sf = (SectionFilter) ev.getSource();
                if (sf instanceof RingSectionFilter)
                {
                    sections = ((RingSectionFilter) sf).getSections(); // Get
                                                                       // Section
                                                                       // data
                    for (int i = 0; i < sections.length; i++)
                    {
                        try
                        {
                            s = sections[i];
                            if (s.getFullStatus())
                            {
                                processSection(s);
                            }
                        }
                        catch (Exception e)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("caught exception processing section: " + e.getClass().getName() + ": "
                                        + e.getMessage());
                            }
                            SystemEventUtil.logRecoverableError(e);
                        }
                        // Mark that we're done with the section.
                        s.setEmpty();
                    }

                }
                else
                {
                    try
                    {
                        f.stopFilter();
                        s = ((SimpleSectionFilter) sf).getSection();
                        processSection(s);

                        // Mark that we're done with the section.
                        f.setVersionFilter((byte) s.version_number());
                        s.setEmpty();
                    }
                    catch (Exception e)
                    {
                    };
                    try
                    {
                        f.startFilter(f.lastPid);
                    }
                    catch (Exception e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Unable to restart filter", e);
                        }
                }
            }
            }
            else if (ev instanceof EndOfFilteringEvent)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Filter shut down.  Restarting");
                }
                try
                {
                    f.startFilter(f.lastPid);
                }
                catch (Exception e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Caught exception restarting filters.  Filters not set", e);
                    }
                }
            }
        }

        /**
         * Parse and process an individual section.
         * 
         * @param s
         *            The section to process.
         */
        void processSection(Section s)
        {
            try
            {
                if (s.getFullStatus())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Got section: " + " TableID: " + s.table_id() + " TableID Extension: "
                                + s.table_id_extension() + " Version: " + s.version_number() + " Number: "
                                + s.section_number() + " Syntax: " + s.section_syntax_indicator() + " Length: "
                                + s.section_length());
                    }
                    dumpArray("Section: ", s.getData());

                    // If not a stream Event...trouble!!!!
                    if (s.table_id() != STREAM_EVENT_TABLE)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Unexpected TableID: " + s.table_id());
                        }
                        s.setEmpty();
                        return;
                    }

                    int tableType = (s.table_id_extension() & TID_SECTION_MASK);
                    switch (tableType)
                    {
                        case TID_SECTION_EVENT_DOITNOW:
                            handleSectionDoItNow(s);
                            break;
                        case TID_SECTION_EVENT_SCHEDULED:
                            handleSectionScheduled(s);
                            break;
                        case TID_SECTION_NPT_REF:
                            handleSectionNPT(s);
                            break;
                        default:
                            if (log.isDebugEnabled())
                            {
                                log.debug("Invalid Table ID value: " + Integer.toHexString(tableType));
                            }
                            break;
                    }
                }
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Caught exception processing section", e);
                }
            }
        }

        /**
         * Handle a section which contains only a single do it now event
         * descriptor.
         * 
         * @param s
         *            The section containing the descriptor
         * @throws NoDataAvailableException
         */
        private void handleSectionDoItNow(Section s) throws NoDataAvailableException
        {
            if (log.isDebugEnabled())
            {
                log.debug("Processing do it now section");
            }
            // Parse the descriptor.
            StreamEventDescriptor sed = new StreamEventDescriptor(s.getData(), SECTION_OFFSET_DESCRIPTORS);
            // Find an appropriate event and fire it.
            int id = sed.getEventId();
            int version = s.version_number();
            if (log.isDebugEnabled())
            {
                log.debug("Got StreamEvent Descriptor: ID: " + id + " Version: " + version + " NPT: " + sed.getNPT()
                        + " Content: " + sed.getContentLength());
            }
            for (int i = 0; i < m_doItNowEvents.size(); i++)
            {
                DSMCCFilterableObject er = (DSMCCFilterableObject) m_doItNowEvents.get(i);
                if ((er instanceof DSMCCEventRecord) && (er.getID() == id))
                {
                    ((DSMCCEventRecord) er).processStreamEventDescriptor(version, sed);
                }
            }
        }

        /**
         * Handle a section which contains multiple scheduled stream event
         * descriptors. These sections can also contain stream mode descriptors,
         * and NPT Endpoint descriptors, but they're ignored.
         * 
         * @param s
         *            The section in question.
         * @throws NoDataAvailableException
         */
        private void handleSectionScheduled(Section s) throws NoDataAvailableException
        {
            if (log.isDebugEnabled())
            {
                log.debug("Processing Scheduled event section");
            }

            byte data[] = s.getData();
            short version = s.version_number();
            int index = SECTION_OFFSET_DESCRIPTORS; // Start of descriptor list

            while (index < data.length - CRC_SIZE)
            {
                byte tag = data[index];
                if (tag == STREAM_EVENT_DESCRIPTOR)
                {
                    // Process a stream eventdescriptor
                    StreamEventDescriptor sed = new StreamEventDescriptor(data, index);
                    // Find an appropriate event and fire it.
                    int id = (sed.getEventId() & 0x0000ffff);
                    if (log.isDebugEnabled())
                    {
                        log.debug("Processing descriptor at index " + index + " ID " + id);
                    }
                    for (int i = 0; i < m_scheduledEvents.size(); i++)
                    {
                        DSMCCFilterableObject er = (DSMCCFilterableObject) m_scheduledEvents.get(i);
                        if ((er instanceof DSMCCEventRecord) && (er.getID() == id))
                        {
                            ((DSMCCEventRecord) er).processStreamEventDescriptor(version, sed);
                        }
                    }
                    index += sed.getDescriptorLength();
                }
                else
                {
                    // Ignore any other descriptors.
                    byte length = data[index + 1];
                    index += length + 1; // TODO: Make sure length is only the
                                         // length of the header.
                }
            }
        }

        /**
         * Handle a section which contains NPT Reference Descriptors, and other
         * NPT descriptors.
         * 
         * @param s
         *            The Section to process.
         * @throws NoDataAvailableException
         */
        private void handleSectionNPT(Section s) throws NoDataAvailableException
        {
            if (log.isDebugEnabled())
            {
                log.debug("Handling NPT Reference section");
            }

            byte data[] = s.getData();
            short version = s.version_number();
            int index = SECTION_OFFSET_DESCRIPTORS; // Start of descriptor list

            while (index < data.length - CRC_SIZE)
            {
                byte tag = data[index];

                if (tag == NPT_REFERENCE_DESCRIPTOR)
                {
                    // Process a stream eventdescriptor
                    NPTReferenceDescriptor nptref = new NPTReferenceDescriptor(data, index);

                    int id = nptref.getContentId();

                    for (int i = 0; i < m_npts.size(); i++)
                    {
                        DSMCCFilterableObject npt = (DSMCCFilterableObject) m_npts.get(i);

                        if ((npt instanceof NPTTimebase) && npt.getID() == id)
                        {
                            ((NPTTimebase) npt).processNPTDescriptor(version, nptref);
                        }
                    }
                    index += nptref.getDescriptorLength();
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Skipping descriptor at index " + index + ".  Tag: " + Integer.toHexString(tag));
                    }
                    // Skip over this descriptor
                    byte length = data[index + 1];
                    index += length + 1; // TODO: Make sure length is only the
                                         // length of the header.
                }
            }
        }
    }

    public void destroy()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Destroy called.  Ok.");
        }
    }

    /**
     * Receive a network tuning event. If it's a
     * NetworkInterfaceTuningOverEvent, we know that a NetworkInterface has
     * arrived someplace, and we can attempt to determine if it contains
     * anything we're interested in, and if we should filter on it.
     * 
     * @param ev
     *            The Event.
     */
    public synchronized void receiveNIEvent(NetworkInterfaceEvent ev)
    {
        // Shortcut. Don't do the rest if we don't have any services active.
        if (services.size() == 0)
        {
            return;
        }
        if (ev instanceof NetworkInterfaceTuningOverEvent)
        {

            NetworkInterfaceTuningOverEvent toe = (NetworkInterfaceTuningOverEvent) ev;
            if (toe.getStatus() == NetworkInterfaceTuningOverEvent.SUCCEEDED)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Received tuning over event.  Checking for reconnect.  " + services.size() + " services");
                }

                // If we've succeeded, attempt to connect to each of the
                // services which are
                // defined.
                Enumeration keys = services.keys();

                while (keys.hasMoreElements())
                {
                    Object key = keys.nextElement();
                    ServiceData sd = (ServiceData) services.get(key);
                    if (log.isDebugEnabled())
                    {
                        log.debug("Checking service: " + sd.m_loc + " : Connected: " + sd.isConnected());
                    }

                    if (!sd.isConnected())
                    {
                        try
                        {
                            // If the service isn't marked as connected
                            // currently, attempt to
                            // connect it now.
                            if (sd.connect())
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("Reconnection succeeded");
                                }
                            }
                            else
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("Reconnection failed");
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("Caught exception while attempting to reconnect:", e);
                            }
                        }
                    }
                }

            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Received Tuning Over failed event");
                }
    }
        }
    }

    static void dumpArray(String name, byte[] array)
    {
        //avoid construction of stringbuffer if debug logging is not enabled
        if (log.isDebugEnabled())
        {
            StringBuffer str = new StringBuffer(name);
            for (int i = 0; i < array.length; i++)
            {
                String x = Integer.toHexString(((int) array[i]) & 0x00ff);
                if (x.length() == 1) str.append("0");
                str.append(x);
                str.append(" ");
            }
            log.debug(str);
        }
    }
}
