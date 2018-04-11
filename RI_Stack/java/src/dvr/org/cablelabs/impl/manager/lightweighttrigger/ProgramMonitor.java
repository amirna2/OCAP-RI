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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.tv.locator.Locator;
import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIChangeType;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.navigation.StreamType;
import javax.tv.service.selection.PresentationChangedEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextDestroyedEvent;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowStateChangedEvent;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.recording.RecordingImpl;
import org.cablelabs.impl.manager.timeshift.TimeShiftWindow;
import org.cablelabs.impl.manager.timeshift.TimeShiftWindowMonitorListener;
import org.cablelabs.impl.manager.timeshift.TimeShiftWindow.TimeShiftWindowClientImpl;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.ocap.dvr.TimeShiftBufferResourceUsageImpl;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.util.SimpleCondition;
import org.davic.net.InvalidLocatorException;
import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.event.LightweightTriggerManager;
import org.ocap.dvr.event.LightweightTriggerSession;
import org.ocap.dvr.event.StreamChangeListener;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceUsage;
import org.ocap.service.ServiceContextResourceUsage;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.si.PMTElementaryStreamInfo;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.ProgramMapTableManager;
import org.ocap.si.TableChangeListener;

public class ProgramMonitor implements TableChangeListener, TimeShiftWindowMonitorListener, ServiceContextListener
{
    private TimeShiftWindow m_tsw;
    
    private ExtendedNetworkInterface m_ni;

    private int m_program;

    private ProgramMapTable m_pmt;

    private Locator m_locator = null;

    private Hashtable m_streamTypes = null;

    private boolean m_started = false;

    private boolean m_stopped = false;

    private int m_frequency;

    private ServiceExt m_service;

    private TimeShiftWindowClientImpl m_tswc;

    private int m_qamMode;

    private SIRequest m_request;

    private ServiceContextExt m_serviceContext;

    private RecordingRequest m_recordingReq;
    
    private List m_bufferingReqs = new ArrayList();

    static ProgramMapTableManager pmtMgr = ProgramMapTableManager.getInstance();

    static StreamTypeHandlerList handlers = StreamTypeHandlerList.getInstance();

    static CallerContextManager ccMgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    static CallerContext systemContext = ccMgr.getSystemContext();

    static LightweightTriggerManagerImpl lwtm = (LightweightTriggerManagerImpl) LightweightTriggerManager.getInstance();

    private boolean m_isPresenting = false;
    
    private String m_logPrefix = "";

    public ProgramMonitor(TimeShiftWindow tsw)
    {
        m_tsw = tsw;
        m_ni = (ExtendedNetworkInterface) tsw.getNetworkInterface();
        if ( (tsw.getState() == TimeShiftManager.TSWSTATE_IDLE) 
             || (m_ni == null) )
        {
            throw new IllegalArgumentException(tsw + " IDLE or no longer has an NI");
        }
        
        m_tswc = (TimeShiftWindowClientImpl) tsw.addClient( 0, 0, 0, null, this, 
                                                            TimeShiftManager.LISTENER_PRIORITY_LOW);
        tsw.setMonitor(this);
        m_service = (ServiceExt) tsw.getService();
        synchronized (this)
        {
            ServiceContextExt serviceContext = findServiceContext(m_ni.getResourceUsages());
            if (serviceContext != null)
            {
                m_isPresenting = serviceContext.isPresenting();
            }
    
            m_logPrefix = "PM 0x" + Integer.toHexString(this.hashCode()) + ": ";
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Creating ProgramMonitor for " + m_service.getLocator() + " :: " + m_ni);
            }
    
            if (!handlers.isEmpty())
            {
                systemContext.runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        start();
                    }
                });
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("No handlers registered. Postponing.");
                }
            }
        } // END synchronized (this)
    }

    /**
     * Notify this monitor that a new callback has been added. Callback should
     * already have been added to the StreamTypeHandler before invoking this
     * method.
     * 
     * @param cb
     *            The new callback, containing the caller context and object to
     *            invoke.
     * @param streamType
     *            The streamtype this callback is listening too.
     */
    public synchronized void addCallback(LightweightTriggerCallback cb, short streamType)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + m_service.getLocator() + ": Adding new handler for stream type: " + streamType);
        }

        if (!m_started)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Session not started.  Starting now async..");
            }
            systemContext.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    start();
                }
            });
        }
        else
        {
            if (m_streamTypes != null)
            {
                StreamTypeEntry entry = (StreamTypeEntry) m_streamTypes.get(new Short(streamType));
                if (entry != null)
                {
                    LightweightTriggerSessionImpl s = entry.addHandler(cb);
                    cb.invokeHandler(s);
                }
                else
                {
                    processPMT(m_pmt);
                    entry = (StreamTypeEntry) m_streamTypes.get(new Short(streamType));
                    if (entry != null)
                    {
                        entry.pids = entry.newPids;
                        entry.newPids = null;
                        LightweightTriggerSessionImpl session = entry.getSession(cb);
                        cb.invokeHandler(session);
                    }
                    // If entry is null, something is very wrong here.
                    // processPMT should
                    // have created it.
                }
            }
        }
    }

    /**
     * Start processing this monitor.
     */
    synchronized void start()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Starting handlers on " + m_service.getLocator());
        }

        // Make sure we haven't started already.
        if (m_started)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Session already started");
            }
            return;
        }
        if (m_stopped)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Session already stopped");
            }
            return;
        }
        m_started = true;

        Requestor requestor = new Requestor(m_logPrefix + "ServiceDetails");

        synchronized (requestor)
        {
            m_request = m_service.retrieveDetails(requestor);
        }
        ServiceDetailsExt sde = (ServiceDetailsExt) requestor.getData();
        if (sde == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Unable to retrieve Service Details for " + m_service + ".  Aborting start");
            }
            m_started = false;
            return;
        }
        m_program = sde.getProgramNumber();
        TransportStreamExt ts = (TransportStreamExt) sde.getTransportStream();
        m_frequency = ts.getFrequency();
        m_qamMode = ts.getModulationFormat();

        // If analog service no need to monitor PMT
        if(sde.isAnalog())
        	return;
        
        try
        {
            m_locator = new OcapLocator(m_frequency, m_program, m_qamMode);
        }
        catch (InvalidLocatorException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix + "Unable to create locator for PMT acquisition", e);
            }
            m_started = false;
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Using PMT locator: " + m_locator);
        }
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Starting PMT listener");
        }
        pmtMgr.addInBandChangeListener(this, m_locator);
        ProgramMapTable pmt = getPMT();

        m_pmt = pmt;
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Retrieved PMT: " + pmt);
        }

        if (m_pmt != null)
        {
            processPMT(pmt);
            updateUsagesAndNotify();
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Unable to retrieve PMT.  Waiting for ADD");
            }
    }
    }

    /**
     * Update the ServiceContext, BufferingRequest, RecordingRequest and isPresenting()
     *  state of the ProgramMonitor.
     */
    private void updateUsagesAndNotify()
    {
        final List rus = m_ni.getResourceUsages();

        // Determine what we have attached, and do any notifications.
        updateServiceContext(findServiceContext(rus));
        updateRecordingRequest(findRecordingRequest(rus));
        updateBufferingRequests(findBufferingRequests(rus));
        updatePresentingState(isPresenting());
    }
    
    /**
     * Process a PMT, looking for any PID's specified in a lightweight trigger
     * handler.
     * 
     * @param pmt
     *            The PMT to process.
     * @return True if any PID's are found with a stream type that has been
     *         registered for are found, false if not.
     */
    private boolean processPMT(ProgramMapTable pmt)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        boolean processNewStreams = false;

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Processing PMT: " + pmt);
        }

        // TODO: Is there any case where this isn't the right thing to do?
        if (pmt == null)
        {
            return false;
        }

        PMTElementaryStreamInfo[] loop = pmt.getPMTElementaryStreamInfoLoop();
        for (int i = 0; i < loop.length; i++)
        {
            Short pidType = new Short(loop[i].getStreamType());

            if (handlers.isHandled(pidType))
            {
                Integer pid = new Integer(loop[i].getElementaryPID());
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Pid: " + pid + " Type: " + pidType + " Handled");
                }
                StreamTypeEntry entry = getStreamTypeEntry(pidType);
                entry.addPID(pid);
                processNewStreams = true;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Pid: " + loop[i].getElementaryPID() + " Type: " + pidType + " Not Handled");
                }
        }
        }
        return processNewStreams;
    }

    /**
     * Compare the PID's in a set of stream elements to see if they've changed.
     */
    private void checkPIDs()
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Checking PIDs for changes");
        }
        // Walk all the stream types.
        if (m_streamTypes != null)
        {
            Enumeration x = m_streamTypes.elements();
            while (x.hasMoreElements())
            {
                StreamTypeEntry entry = (StreamTypeEntry) x.nextElement();
                entry.updatePids();
            }
        }
    }

    /**
     * Notify all the handlers that we have sessions.
     * Note: The actual notifications will be done on the notifand's thread,
     *       not on the thread making this call and not while holding this
     *       object's monitor (this).
     */
    private void notifyHandlers()
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Notifying handlers");
        }
        if (m_streamTypes != null)
        {
            Enumeration x = m_streamTypes.elements();
            while (x.hasMoreElements())
            {
                StreamTypeEntry entry = (StreamTypeEntry) x.nextElement();
                entry.notifyHandlers();
            }
        }
    }

    private void notifyPresentationChanged()
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Notifying Presentation Changed");
        }
        if (m_streamTypes != null)
        {
            Enumeration x = m_streamTypes.elements();
            while (x.hasMoreElements())
            {
                StreamTypeEntry entry = (StreamTypeEntry) x.nextElement();
                entry.notifyPresentationChanged(m_isPresenting);
            }
        }
    }

    private void setRecording(RecordingImpl req)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "setRecordingRequest: " + req);
        }
        if (m_streamTypes != null)
        {
            Enumeration x = m_streamTypes.elements();
            while (x.hasMoreElements())
            {
                StreamTypeEntry entry = (StreamTypeEntry) x.nextElement();
                entry.setRecording(req);
            }
        }
    }

    /**
     * Get the StreamTypeEntry object for this stream type.
     * 
     * @param streamType
     *            The stream type to look for.
     * @return The StreamTypeEntry for this type, if found or created, null
     *         otherwise.
     */
    private StreamTypeEntry getStreamTypeEntry(Short streamType)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        StreamTypeEntry ret = null;
        // Check to see if the table exists. If not, create it.
        if (m_streamTypes == null)
        {
            m_streamTypes = new Hashtable();
        }
        else
        {
            // It existed. Let's see if this guy was there.
            ret = (StreamTypeEntry) m_streamTypes.get(streamType);
        }
        // If nothing found, create one.
        if (ret == null)
        {
            ret = new StreamTypeEntry();
            ret.streamType = streamType;
            m_streamTypes.put(streamType, ret);
        }

        return ret;
    }

    private RecordingRequest findRecordingRequest(List rus)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (rus == null)
        {
            return null;
        }
        
        Iterator it = rus.iterator();
        while (it.hasNext())
        {
            final RecordingRequest rr = findRecordingRequest((ResourceUsage)it.next());
            if (rr != null)
            {
                return rr;
            }
        }
        
        return null;
    }

    private RecordingRequest findRecordingRequest(ResourceUsage ru)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (ru instanceof RecordingResourceUsage)
        {
            return ((RecordingResourceUsage) ru).getRecordingRequest();
        }
        else
        {
            return null;
        }
    }

    private ServiceContextExt findServiceContext(List rus)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (rus == null)
        {
            return null;
        }

        Iterator it = rus.iterator();
        while (it.hasNext())
        {
            ServiceContextExt sce = findServiceContext((ResourceUsage)it.next());
            if (sce != null)
            {
                return sce;
            }
        }
        
        return null;
    }

    private ServiceContextExt findServiceContext(ResourceUsage ru)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (ru instanceof ServiceContextResourceUsage)
        {
            return (ServiceContextExt) ((ServiceContextResourceUsage) ru).getServiceContext();
        }
        else
        {
            return null;
        }
    }
    
    private List findBufferingRequests(List rus)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        List brList = new ArrayList(rus.size()); // worst case, they're all BRs...
        
        if (rus != null)
        {
            Iterator it = rus.iterator();
            while (it.hasNext())
            {
                final ResourceUsage ru = (ResourceUsage)it.next();
                
                if (ru instanceof TimeShiftBufferResourceUsageImpl)
                {
                    BufferingRequest br = ((TimeShiftBufferResourceUsageImpl) ru).getBufferingRequest();
                    brList.add(br);
                }
            }
        }
        
        return brList;
    }
    
    /**
     * Stop monitoring this program, and notify all session that they're done.
     */
    public synchronized void stop()
    {
        if (log.isDebugEnabled())
        {
            log.debug("stop");
        }
        if (m_stopped)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Program monitor already stopped.  Ignoring");
            }
            return;
        }
        m_stopped = true;

        pmtMgr.removeInBandChangeListener(this);
        if (m_serviceContext != null)
        {
            try
            {
                m_serviceContext.removeListener(this);
            }
            catch (IllegalStateException ise)
            { // Only thrown when ServiceContext is destroyed - it which case 
              //  it has/will be removed by the SC
                // Fall through and let it get nulled
            }
            m_serviceContext = null;
        }
        m_tswc.release();
        m_tswc = null;
        m_tsw.setMonitor(null);
        m_tsw = null;
        if (m_request != null)
        {
            m_request.cancel();
        }
        signalAllSessions(StreamChangeListener.TRANSPORT_STREAM_LOST_REASON);
        m_streamTypes = null;
    }

    /**
     * Find a LightweightTriggerSession that matches a given locator.
     * 
     * @param loc
     *            The locator in question.
     * 
     * @return The session, if found.
     */
    public synchronized LightweightTriggerSession getSessionByLocator(OcapLocator loc)
    {
        if (log.isDebugEnabled())
        {
            log.debug("searching for " + loc.toExternalForm());
        }
        // Check each of the stream types to see if any sessions match this
        // locator.
        Enumeration x = m_streamTypes.elements();
        while (x.hasMoreElements())
        {
            StreamTypeEntry entry = (StreamTypeEntry) x.nextElement();
            LightweightTriggerSession session = entry.getSessionByLocator(loc);
            if (session != null)
            {
                return session;
            }
        }
        return null;
    }

    synchronized void signalAllSessions(int reason)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "SignalAllSessions: " + reason);
        }
        if (m_streamTypes != null)
        {
            Enumeration x = m_streamTypes.elements();
            while (x.hasMoreElements())
            {
                StreamTypeEntry entry = (StreamTypeEntry) x.nextElement();
                entry.stopSessions(reason);
            }
        }
    }

    public synchronized RecordingRequest getRecordingRequest()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "getRecordingRequest: returning " + m_recordingReq);
        }
        return m_recordingReq;
    }

    public synchronized ServiceContext getServiceContext()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "getServiceContext: returning " + m_serviceContext);
        }
        return m_serviceContext;
    }

    public synchronized BufferingRequest getBufferingRequest()
    {
        BufferingRequest br;
        
        if (m_bufferingReqs.size() < 1)
        {
            br = null;
        }
        else
        {
            br = (BufferingRequest)m_bufferingReqs.get(0);
        }
      
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "getBufferingRequest: returning " + br);
        }
        return br;
    }

    public synchronized boolean isPresenting()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "isPresenting");
        }

        if (m_serviceContext != null)
        {
            return m_serviceContext.isPresenting();
        }
        else
        {
            return false;
        }
    }

    // TableChangeListener

    /**
     * Implementation of notifyChange for TableChangeListener.
     */
    public void notifyChange(SIChangeEvent event)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Received table change event: " + event.getChangeType());
        }

        SIChangeType change = event.getChangeType();
        if (change == SIChangeType.REMOVE)
        {
            signalAllSessions(StreamChangeListener.TRANSPORT_STREAM_LOST_REASON);
        }
        else if (change == SIChangeType.MODIFY || change == SIChangeType.ADD)
        {
            ProgramMapTable pmt = getPMT();
            synchronized (this)
            {
                processPMT(pmt);
                m_pmt = pmt;
                checkPIDs();
            }
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix + "Unknown notification type: " + change);
            }
    }
    }

    private void updateRecordingRequest(RecordingRequest req)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "updateRecordingRequest: " + req + " Change: " + (req != m_recordingReq));
        }
        if (req != null)
        {
            // We must notify each time a request is added, so one got added, do
            // it.
            if (m_recordingReq != req)
            {
                notifyHandlers();
            }
        }
        // TODO: Set the recording request into all LWTS's
        setRecording((RecordingImpl) req);
        m_recordingReq = req;
    }

    private void updateServiceContext(ServiceContextExt sc)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "updateServiceContext: " + sc + " Change: " + (sc != m_serviceContext));
        }

        if (m_serviceContext == sc)
        { // ServiceContext didn't change - nothing to do
            return;
        }

        if (m_serviceContext != null)
        {
            try
            {
                m_serviceContext.removeListener(this);
            }
            catch (IllegalStateException ise)
            { // Only thrown when ServiceContext is destroyed - it which case 
              //  it has/will be removed by the SC
            }
        }
        
        if (sc != null)
        {
            sc.addListener(this);
            notifyHandlers();
        }

        m_serviceContext = sc;
    }

    private void updateBufferingRequests(List requests)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "updateBufferingRequests: " + requests.toString());
        }
        
        Iterator it = requests.iterator();
        while (it.hasNext())
        {
            final BufferingRequest br = (BufferingRequest)it.next();

            if (!m_bufferingReqs.contains(br))
            {
                // We must notify each time a request is added, so one got added, do
                // it.
                notifyHandlers();
            }
        }
        
        // Now the passed list is our new list
        m_bufferingReqs = requests;
    }

    private void updatePresentingState(boolean isPresenting)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "updatePresentingState: " + isPresenting + " Current: " + m_isPresenting);
        }
        if (m_isPresenting != isPresenting)
        {
            m_isPresenting = isPresenting;
            notifyPresentationChanged();
        }
    }

    public synchronized void tswStateChanged(TimeShiftWindowClient tswc, TimeShiftWindowStateChangedEvent e)
    {
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "tswChanged: " + m_started + " : " 
                       + TimeShiftManager.stateString[e.getNewState()] );
        }
        // TRANSPORT_STREAM_LOST_REASON)
        if (e.getNewState() == TimeShiftManager.TSWSTATE_IDLE)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Not Tuned. Shutting down");
            }
            this.stop();
            lwtm.removePM(this);
        }
        else if (m_started)
        {
            if (m_pmt == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("tswStateChanged:m_pmt is null. Hence trying to get pmt.");
                }
                ProgramMapTable pmt = getPMT();
                processPMT(pmt);
                m_pmt = pmt;
            }
            updateUsagesAndNotify();
        }
    }

    public void tswCCIChanged(TimeShiftWindowClient tswc, CopyControlInfo cci)
    {
        // CCI changes don't factor into ProgramMonitor's requirements
    }

    public synchronized void tswNIUsageChange(TimeShiftWindow tsw)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "tswNIUsageChange");
        }
        if (m_started)
        {
            updateUsagesAndNotify();
        }
    }

    public synchronized void receiveServiceContextEvent(ServiceContextEvent e)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "receiveServiceContextEvent " + e);
        }
        if ( e instanceof PresentationChangedEvent 
             || e instanceof PresentationTerminatedEvent
             || e instanceof ServiceContextDestroyedEvent )
        {
            updatePresentingState(isPresenting());
        }
    }

    /**
     * Get the PMT for this program.
     * 
     * @return The appropriate PMT if it was recovered, null if not.
     */
    private ProgramMapTable getPMT()
    {
    	// Added code for caching for findbugs issues fix
    	Locator  l_locator;
    	synchronized(this)
    	{
    		l_locator = m_locator;
    	}
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Retrieving PMT: " + l_locator);
        }
        Requestor req = new Requestor("PMT");
        synchronized (req)
        {
            m_request = pmtMgr.retrieveInBand(req, l_locator);
        }
        return (ProgramMapTable) req.getData();
    }

    // Internal classes

    /**
     * Local class to track all PID's related to a stream type. Container only.
     */
    class StreamTypeEntry
    {
        Short streamType;

        Hashtable sessions = new Hashtable();

        Vector pids = null;

        Vector newPids = null;

        /**
         * Add a handler to this stream type, and create a session to go with
         * it.
         * 
         * @param cb
         *            The callback containing the handler.
         * @return A new lightweight trigger session.
         */
        LightweightTriggerSessionImpl addHandler(LightweightTriggerCallback cb)
        {
            // Internal method - caller should hold the lock
            Assert.lockHeld(ProgramMonitor.this);
    		// Added code for caching for findbugs issues fix
            TimeShiftWindowClientImpl l_tswc;
            synchronized(ProgramMonitor.this)
            {
            	l_tswc = m_tswc;
            }
            LightweightTriggerSessionImpl session = new LightweightTriggerSessionImpl(streamType.shortValue(),
                    ProgramMonitor.this, l_tswc, m_service, pids);
            sessions.put(cb, session);
            return session;
        }

        public void setRecording(RecordingImpl req)
        {
            // Internal method - caller should hold the lock
            Assert.lockHeld(ProgramMonitor.this);
            
            Enumeration y = sessions.elements();
            while (y.hasMoreElements())
            {
                LightweightTriggerSessionImpl session = (LightweightTriggerSessionImpl) y.nextElement();
                if (pids != null)
                {
                    session.setRecordingStore(req);
                }
            }
        }

        void updatePids()
        {
            // Internal method - caller should hold the lock
            Assert.lockHeld(ProgramMonitor.this);
            
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Updating PIDs for stream type " + streamType + ":: Old: " + pids + ":: New: " + newPids);
            }
            if (newPids == null && pids != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "PIDs for type " + streamType + " Lost.  Ending sessions.");
                }
                // StreamType lost.
                stopSessions(StreamChangeListener.STREAM_TYPE_LOST_REASON);
                clearSessions();
                pids = null;
            }
            else if (pids == null && newPids != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "PIDs found for type " + streamType + ".  None previously.  Notifying handlers");
                }
                notifyHandlers();
            }
            else if (!newPids.equals(pids))
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "PIDs for type " + streamType + " changed.  Updating sessions.");
                }
                setPids();
            }
            newPids = null;
        }

        /**
         * Clear all the sessions from a given stream type.
         */
        void clearSessions()
        {
            // Internal method - caller should hold the lock
            Assert.lockHeld(ProgramMonitor.this);
            
            sessions.clear();
        }

        void stopSessions(int reason)
        {
            // Internal method - caller should hold the lock
            Assert.lockHeld(ProgramMonitor.this);
            
            Enumeration y = sessions.elements();
            while (y.hasMoreElements())
            {
                LightweightTriggerSessionImpl session = (LightweightTriggerSessionImpl) y.nextElement();
                session.streamEnded(reason);
            }
            clearSessions();
        }

        /**
         * Inform all sessions that the PID list has changed.
         */
        void setPids()
        {
            // Internal method - caller should hold the lock
            Assert.lockHeld(this);
            
            pids = newPids;
            Enumeration y = sessions.elements();
            while (y.hasMoreElements())
            {
                LightweightTriggerSessionImpl session = (LightweightTriggerSessionImpl) y.nextElement();
                if (pids != null)
                {
                    session.setPids(pids);
                }
            }
        }

        /**
         * Get the session for this callback. Create a new one if need be.
         * 
         * @param cb
         *            The callback to get the session for.
         * @return The LightweightTriggerSession corresponding to this callback.
         */
        LightweightTriggerSessionImpl getSession(LightweightTriggerCallback cb)
        {
            // Internal method - caller should hold the lock
            Assert.lockHeld(ProgramMonitor.this);
            
            LightweightTriggerSessionImpl session = (LightweightTriggerSessionImpl) sessions.get(cb);
            if (session == null)
            {
                session = addHandler(cb);
            }
            return session;
        }

        /**
         * Invoke all handlers.
         */
        void notifyHandlers()
        {
            // Internal method - caller should hold the lock
            Assert.lockHeld(ProgramMonitor.this);
            
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Notifying handlers for stream type " + streamType);
            }
            pids = newPids;
            newPids = null;

            Object[] callbacks = handlers.getHandlersForType(streamType.shortValue());
            if(callbacks != null)
            {
                for (int i = 0; i < callbacks.length; i++)
                {
                    LightweightTriggerCallback cb = (LightweightTriggerCallback) callbacks[i];
                    LightweightTriggerSessionImpl session = getSession(cb);
                    cb.invokeHandler(session);
                }
            }
        }

        /**
         * Invoke presentation changed for all handlers.
         */
        void notifyPresentationChanged(boolean isPresenting)
        {
            // Internal method - caller should hold the lock
            Assert.lockHeld(ProgramMonitor.this);
            
            Enumeration y = sessions.elements();
            while (y.hasMoreElements())
            {
                LightweightTriggerSessionImpl session = (LightweightTriggerSessionImpl) y.nextElement();
                session.setPresentation(isPresenting);
            }
        }

        /**
         * Add a PID to this stream type. Create the newPids vector if need be.
         * 
         * @param pid
         *            The PID to add.
         */
        void addPID(Integer pid)
        {
            // Internal method - caller should hold the lock
            Assert.lockHeld(ProgramMonitor.this);
            
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Adding PID " + pid + " to newPids list");
            }
            if (newPids == null)
            {
                newPids = new Vector();
            }
            insertVec(newPids, pid);
        }

        /**
         * Look at all the sessions, and pick one that has the matching locator.
         * 
         * @param loc
         *            Locator to look for.
         * @return The matching session, if found, otherwise, null.
         */
        LightweightTriggerSessionImpl getSessionByLocator(OcapLocator loc)
        {
            // Internal method - caller should hold the lock
            Assert.lockHeld(ProgramMonitor.this);
            
            Enumeration s = sessions.elements();
            while (s.hasMoreElements())
            {
                LightweightTriggerSessionImpl session = (LightweightTriggerSessionImpl) s.nextElement();
                if (session.getLocator().equals(loc))
                {
                    return session;
                }
            }
            return null;
        }
    } // END class StreamTypeEntry

    /**
     * Utility class for retrieving SI data.
     * 
     * @author kolding
     * 
     */
    private class Requestor implements SIRequestor
    {
        private String m_name;
		// Added condition check for findbugs issues fix
        private final SimpleCondition m_requestorCondition = new SimpleCondition(false);

        private SIRetrievable m_data = null;

        Requestor(String type)
        {
            m_name = type;
        }

        public void notifyFailure(SIRequestFailureType reason)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + m_name + "Requestor::notifyFailure " + reason);
            }
            synchronized (this)
            {
                m_request = null;
            }
            m_requestorCondition.setTrue();
        }

        public void notifySuccess(SIRetrievable[] result)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + m_name + "Requestor::notifySuccess");
            }
            if (result != null)
            {
                m_request = null;
                m_data = result[0];
            }
            m_requestorCondition.setTrue();
        }

        SIRetrievable getData()
        {
            try
            {
                m_requestorCondition.waitUntilTrue();
            }
            catch (InterruptedException e)
            {
                // Do nothing
            }
            return m_data;
        }
    } // END class Requestor

    // Utilities
    /**
     * Insert into a vector such that the vector remains sorted. Used to create
     * vectors of PID's. Uses a simple linear insertion algorithm because we
     * assume that these lists aren't very long, most often only a single
     * element.
     * 
     * @param vec
     *            The vector to insert into.
     * @param insert
     *            An integer to insert.
     */
    void insertVec(Vector vec, Integer insert)
    {
        // Internal method - caller should hold the lock
        Assert.lockHeld(this);
        
        int size = vec.size();
        if (size == 0)
        {
            vec.add(insert);
        }
        else if (insert.compareTo((Integer) vec.lastElement()) > 0)
        {
            vec.add(insert);
        }
        else
        {
            for (int i = 0; i < size; i++)
            {
                if (insert.compareTo((Integer) vec.elementAt(i)) <= 0)
                {
                    vec.add(i, insert);
                    break;
                }
            }
        }
    }

    // Log4J Logger
    private static final Logger log = Logger.getLogger(ProgramMonitor.class.getName());
} // END class ProgramMonitor
