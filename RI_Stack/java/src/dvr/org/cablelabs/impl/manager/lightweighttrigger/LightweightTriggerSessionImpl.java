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

package org.cablelabs.impl.manager.lightweighttrigger;

import java.util.Date;
import java.util.Vector;

import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextPermission;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamImpl;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.timeshift.TimeShiftWindow.TimeShiftWindowClientImpl;
import org.cablelabs.impl.security.PersistentStoragePermission;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.util.SecurityUtil;
import org.davic.net.InvalidLocatorException;
import org.davic.net.tuning.NetworkInterface;
import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.event.LightweightTriggerSession;
import org.ocap.dvr.event.StreamChangeListener;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.RecordingRequest;

/**
 * Concrete implementation of the LightweightTriggerSession
 * 
 * @author Eric Koldinger (kolding@enabletv.com)
 * 
 */
public class LightweightTriggerSessionImpl implements LightweightTriggerSession
{
    ExtendedNetworkInterface m_ni = null;

    short m_type;

    Vector m_pidsVec = null;

    int[] m_pids = null;

    Object m_mutex = new Object();

    OcapLocator m_locator = null;

    ProgramMonitor m_pm = null;

    boolean m_presenting = false;

    boolean m_stopped = false;

    boolean m_buffering = false;

    boolean m_storing = false;

    int m_stopReason = -1; // Default. TODO: Better value?

    TimeShiftWindowClientImpl m_tsw = null;

    LightweightTriggerSessionStoreListener m_storeChangeListener = null;

    LightweightTriggerEventStoreWriteChange m_bufferStore = null;

    LightweightTriggerEventStoreWrite m_recordingStore = null;

    ServiceExt m_service = null;

    StreamChangeListener m_listener = null;

    CallerContext m_cc;

    // any fields added may require changes to internalStop()

    private Vector m_registeredNames = new Vector();

    private Vector m_registeredIDs = new Vector();

    // Log4J Logger
    private static final Logger log = Logger.getLogger(LightweightTriggerSessionImpl.class.getName());
    
    private String m_logPrefix = "";

    private static final int MAX_DATA_SIZE = 4096;

    public LightweightTriggerSessionImpl(short type, ProgramMonitor pm, TimeShiftWindowClientImpl tsw,
            ServiceExt service, Vector pids)
    {

        m_type = type;
        m_stopped = false;
        m_tsw = tsw;
        m_pm = pm;
        m_service = service;
        m_ni = (ExtendedNetworkInterface) tsw.getNetworkInterface();
        try
        {
            m_locator = new LightweightTriggerCarouselLocator();
        }
        catch (InvalidLocatorException e)
        {
            // Just doesn't happen.
        }

        // sort and organize pids

        m_logPrefix = "LTSI 0x" + Integer.toHexString(this.hashCode()) + ": ";
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "constructor: Type: " + m_type + " Locator: " + m_locator
                       + " PIDS: " + pids + " NI: " + m_ni );
        }
        setPids(pids);
        setBufferStore(m_tsw);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#getLocator()
     */
    public OcapLocator getLocator()
    {
        return m_locator;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#getNetworkInterface()
     */
    public NetworkInterface getNetworkInterface()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getNetworkInterface: " + m_ni);
        }

        if (m_ni != null && m_ni.isReserved())
        {
            return m_ni;
        }
        else
        {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#getPIDs()
     */
    public int[] getPIDs()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getPIDs");
        }

        return (int[]) m_pids.clone(); // TODO: is this atomic? I don't think
                                       // so.
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#getService()
     */
    public Service getService()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getService(): " + m_service);
        }
        return m_service;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#getRecordingRequest()
     */
    public RecordingRequest getRecordingRequest()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getRecordingRequest");
        }
        return m_pm.getRecordingRequest();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#getServiceContext()
     */
    public ServiceContext getServiceContext()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getServicContext");
        }

        ServiceContextExt sc = (ServiceContextExt) m_pm.getServiceContext();
        if (sc == null)
        {
            return null;
        }

        // First, check to see if we have permission to access all
        // ServiceContexts
        try
        {
            SecurityUtil.checkPermission(new ServiceContextPermission("access", "*"));
            // If we didn't throw an expection, we're good. Return the context.
            return sc;
        }
        catch (SecurityException e)
        {
            try
            {
                // Check that we own this service context
                if (sc.getCreatingContext() == m_cc)
                {
                    SecurityUtil.checkPermission(new ServiceContextPermission("access", "own"));
                    return sc;
                }
            }
            catch (SecurityException e1)
            { /* Do Nothing */
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#getBufferingRequest()
     */
    public BufferingRequest getBufferingRequest()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "getBufferingRequest");
        }
        
        return m_pm.getBufferingRequest();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#getStreamType()
     */
    public short getStreamType()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "getStreamType");
        }

        return m_type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#isPresenting()
     */
    public boolean isPresenting()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "isPresenting");
        }
        
        return m_pm.isPresenting();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#isAuthorized()
     */
    public boolean isAuthorized()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "isAuthorized");
        }
        
        return (m_tsw != null) ? m_tsw.isAuthorized() : false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.dvr.event.LightweightTriggerSession#registerEvent(java.util.
     * Date, java.lang.String, int, byte[])
     */
    public void registerEvent(Date date, String name, int id, byte[] data)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "registerEvent, session="
                                        + toString() + "(" + name + ", " + id + ", " + date);
        }
        if (data != null && data.length > MAX_DATA_SIZE)
        {
            throw new IllegalArgumentException("Data too big.");
        }

        Integer ID = new Integer(id);

        synchronized (m_mutex)
        {
            if (m_stopped)
            {
                throw new IllegalStateException("Session already stopped");
            }
            if (m_registeredIDs.contains(ID))
            {
                throw new IllegalArgumentException("Event ID " + id + " already registered");
            }
            if (m_registeredNames.contains(name))
            {
                throw new IllegalArgumentException("Event name " + name + " already registered");
            }
            m_registeredIDs.add(ID);
            m_registeredNames.add(name);

            LightweightTriggerEvent lwte = new LightweightTriggerEvent(date.getTime(), name, id, data);
            if (getBufferStore() != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("registerEvent -- adding event to buffer store");
                }
                if (!m_bufferStore.addLightweightTriggerEvent(lwte))
                {
                    throw new IllegalArgumentException("event name or id already exists");
                }
            }
            if (m_recordingStore != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("registerEvent -- caching event to recording store");
                }
                if (!m_recordingStore.cacheLightweightTriggerEvent(this, lwte))
                {
                    throw new IllegalArgumentException("event name or id already exists");
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.dvr.event.LightweightTriggerSession#setStreamChangeListener(
     * org.ocap.dvr.event.StreamChangeListener)
     */
    public void setStreamChangeListener(StreamChangeListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Setting streamChangeListener " + listener);
        }
        m_listener = listener;
        if (m_stopped && listener != null)
        {
            final StreamChangeListener l = m_listener;
            final int reasonCode = m_stopReason;

            m_cc.runInContext(new Runnable()
            {
                public void run()
                {
                    l.notifySessionStopped(reasonCode);
                }
            });
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#stop()
     */
    public void stop()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "stop");
        }
        synchronized (m_mutex)
        {
            if (!m_stopped)
            {
                store();
                streamEnded(StreamChangeListener.STREAM_ACTIVITY_ENDED_REASON);
                internalStop();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.event.LightweightTriggerSession#store()
     */
    public void store()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "store");
        }
        // Check for file permission assigned in the PRF.
        SecurityUtil.checkPermission(new PersistentStoragePermission());
        synchronized (m_mutex)
        {
            if (m_stopped)
            {
                throw new IllegalStateException("Session already stopped");
            }
            if (m_recordingStore != null)
            {
                m_recordingStore.store(this);
            }
        }
    }

    /**
     * Called by implementation code to stop the session. Used when transport
     * stream associated with session is no longer tuned, etc. Main purpose is
     * to release unneeded resources so they can be garbage collected.
     * 
     */
    private void internalStop()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "internalStop");
        }
        synchronized (m_mutex)
        {
            // clean things up so they can be garbage collected even while the
            // user has this session
            // Theoretically, the user could hold onto the session forever.
            if (m_cc != null)
            {
                CCData data = getCCData(m_cc);
                Vector sessions = data.sessions;
                sessions.remove(this);
                m_cc = null;
            }

            m_listener = null;
            m_ni = null;
            m_pidsVec = null;
            m_pids = null;
            m_recordingStore = null;
            m_storeChangeListener = null;
            setBufferStore(null);

            m_stopped = true;
        }
    }

    /**
     * Called by LightweightTriggerManagerImpl when the manager detects a
     * session has been "orphaned" by a tune to another transport stream.
     */
    void transportStreamLost()
    {
        synchronized (m_mutex)
        {
            streamEnded(StreamChangeListener.TRANSPORT_STREAM_LOST_REASON);
            internalStop();
        }
    }

    /**
     * Called by LightweightTriggerManagerImpl when the manager detects a
     * session has been "orphaned" because a TS no longer contains the type
     * associated with the session.
     */
    void streamTypeLost()
    {
        synchronized (m_mutex)
        {
            streamEnded(StreamChangeListener.STREAM_TYPE_LOST_REASON);
            internalStop();
        }
    }

    /**
     * Is the session stopped?
     * 
     * @return true if stopped, false otherwise.
     */
    boolean isStopped()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "isStopped " + m_stopped);
        }
        return m_stopped;
    }

    public String toString()
    {
        String s = super.toString();

        int idx = s.lastIndexOf(".") + 1;
        s = s.substring(idx);

        StringBuffer sb = new StringBuffer();
        sb.append(s).append(" -- \n\t{ (" + m_type + ") ");

        if (m_pidsVec != null)
        {
            sb.append(m_pidsVec.toString());
        }
        sb.append(" ::");
        sb.append(" m_bufferStore=" + m_bufferStore + ", m_recordingStore=" + m_recordingStore + " }\n"); // ni="+m_ni+"
                                                                                                          // sc="+m_serviceContext + ",
                                                                                                          // presenting=" + m_presenting + ",
                                                                                                          // recRequest="
                                                                                                          // +
                                                                                                          // m_recordingRequest);
        return sb.toString();
    }

    /**
     * Set the array of PID's in the vector. Assumes that the vector is sorted
     * in ascending order. Notifies the listener if it's set.
     * 
     * @param pids
     *            A Vector containing a SORTED list of PID's of the appropriate
     *            type. If NULL, then assumes streams of the appropriate type
     *            went away, and stops and notifies appropriately.
     */
    void setPids(Vector pids)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "setPids " + pids);
        }
        synchronized (m_mutex)
        {
            if (m_stopped) return;

            final StreamChangeListener listener = m_listener;
            m_pidsVec = pids;
            if (pids != null)
            {
                // Convert the PIDs list to an array.
                m_pids = new int[pids.size()];
                for (int i = 0; i < m_pids.length; i++)
                {
                    m_pids[i] = ((Integer) m_pidsVec.elementAt(i)).intValue();
                }

                // Notify the listener, if one is registered.
                if (listener != null)
                {
                    m_cc.runInContext(new Runnable()
                    {
                        public void run()
                        {
                            listener.notifyPIDsChanged(m_pids);
                        }
                    });
                }
            }
            else
            {
                // No PID's. Stream type lost. Shut down.
                m_pids = null;
                internalStop();
                streamEnded(StreamChangeListener.STREAM_TYPE_LOST_REASON);
            }
        }
    }

    /**
     * Stop the session and signal the listener.
     * 
     * @param reasonCode
     *            The reason code to send to the listener.
     */
    void streamEnded(final int reasonCode)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "streamEnded(" + reasonCode + ")");
        }
        synchronized (m_mutex)
        {
            if (m_stopped) return;
            m_stopReason = reasonCode;
            m_stopped = true;

            final StreamChangeListener l = m_listener;
            if (l != null)
            {
                m_cc.runInContext(new Runnable()
                {
                    public void run()
                    {
                        l.notifySessionStopped(reasonCode);
                    }
                });
            }
        }
    }

    private void setBufferStore(LightweightTriggerEventStoreWriteChange store)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + " setBufferStore: " + store);
        }
        synchronized (m_mutex)
        {
            m_bufferStore = store;
            m_buffering = (m_bufferStore != null || m_recordingStore != null);
            if (m_storeChangeListener != null)
            {
                m_storeChangeListener.bufferStoreChanged(this, store);
            }
        }
    }

    LightweightTriggerEventStoreWriteChange getBufferStore()
    {
        return m_bufferStore;
    }

    void setRecordingStore(LightweightTriggerEventStoreWrite store)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Setting recording store: " + store);
        }
        synchronized (m_mutex)
        {
            m_recordingStore = store;
            m_buffering = (m_bufferStore != null || m_recordingStore != null);
        }
    }

    /**
     * Set the presentation state. Notify listeners if they exist.
     * 
     * @param state
     *            The new presentation state.
     */
    void setPresentation(final boolean state)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "setPresentation: " + state);
        }
        m_presenting = state;
        if (!m_stopped && m_listener != null)
        {
            m_cc.runInContext(new Runnable()
            {
                final StreamChangeListener l = m_listener;

                public void run()
                {
                    l.notifyPresentationChanged(state);
                }
            });

        }
    }

    /**
     * Set the caller context in which this session will run.
     * 
     * @param cc
     *            The destination context.
     */
    void setContext(CallerContext cc)
    {
        synchronized (m_mutex)
        {
            m_cc = cc;
            CCData data = getCCData(m_cc);
            Vector sessions = data.sessions;
            sessions.add(this);
        }
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context. Create one if this caller context does not have one yet.
     * 
     * @param cc
     *            the caller context whose data object is to be returned
     * @return the data object for the specified caller context
     */
    private static synchronized CCData getCCData(CallerContext cc)
    {
        // Retrieve the data for the caller context
        CCData data = (CCData) cc.getCallbackData(LightweightTriggerSessionImpl.class);

        // If a data block has not yet been assigned to this caller context
        // then allocate one.
        if (data == null)
        {
            data = new CCData();
            cc.addCallbackData(data, LightweightTriggerSessionImpl.class);
        }
        return data;
    }

    /**
     * Per caller context data
     */
    static class CCData implements CallbackData
    {
        /**
         * The stream objects list is used to keep track of all DSMCCStreamImpl
         * objects currently in the attached state for this caller context.
         */
        public volatile Vector sessions = new Vector();

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            // Discard the caller context data for this caller context.
            cc.removeCallbackData(DSMCCStreamImpl.class);
            // Remove each ServiceDomain object from the domains list, and
            // delete it.
            int size = sessions.size();

            for (int i = 0; i < size; i++)
            {
                try
                {
                    // Grab the next element in the queue
                    LightweightTriggerSessionImpl session = (LightweightTriggerSessionImpl) sessions.elementAt(i);
                    // And detach it

                    // TODO: Do anything to shut it down???
                    // Delete the listener reference, allow it to shut down.
                    session.m_listener = null;
                    /*
                     * TODO: Check on this. if (session.m_serviceContext !=
                     * null) { // Is this necessary? Will the service context do
                     * this on it's own?
                     * session.m_serviceContext.removeListener(session);
                     * session.m_serviceContext = null; }
                     */

                    // And get rid of it
                }
                catch (Exception e)
                {
                    // Ignore any exceptions
                    if (log.isDebugEnabled())
                    {
                        log.debug("destroy() ignoring Exception " + e);
                    }
            }
            }
            // Toss the whole thing
            sessions = null;
        }
    }

    public void registerStoreListener(LightweightTriggerSessionStoreListener l)
    {
        m_storeChangeListener = l;
    }
}
