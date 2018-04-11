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

package org.cablelabs.impl.manager.timeshift;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.media.TimeBase;
import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIChangeType;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.navigation.StreamType;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterfaceController;
import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.TimeShiftBuffer;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowStateChangedEvent;
import org.cablelabs.impl.manager.TimeShiftWindowChangedListener;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventChangeListener;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventStoreChangeImpl;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventStoreReadChange;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventStoreWriteChange;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientAddedEvent;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientRemovedEvent;
import org.cablelabs.impl.manager.lightweighttrigger.SequentialMediaTimeStrategy;
import org.cablelabs.impl.manager.pod.CADecryptParams;
import org.cablelabs.impl.manager.pod.CASession;
import org.cablelabs.impl.manager.pod.CASessionListener;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.player.AbstractDVRServicePlayer;
import org.cablelabs.impl.media.player.MediaTimeBase;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.ocap.dvr.TimeShiftBufferResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.pod.mpe.CASessionEvent;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.spi.ProviderInstance.SelectionSessionWrapper;
import org.cablelabs.impl.util.LightweightTriggerEventTimeTable;
import org.cablelabs.impl.util.MediaStreamType;
import org.cablelabs.impl.util.PidMapEntry;
import org.cablelabs.impl.util.PidMapTable;
import org.cablelabs.impl.util.SimpleCondition;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.TimeAssociatedElement;
import org.cablelabs.impl.util.TimeTable;
import org.davic.net.InvalidLocatorException;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NoFreeInterfaceException;
import org.davic.net.tuning.NotOwnerException;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.application.AppID;
import org.ocap.dvr.TimeShiftBufferResourceUsage;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceUsage;
import org.ocap.service.ServiceContextResourceUsage;
import org.ocap.si.PATProgram;
import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramAssociationTableManager;
import org.ocap.si.TableChangeListener;

public class TimeShiftWindow implements ResourceClient, NetworkInterfaceCallback, TableChangeListener,
        EDListener, CASessionListener, PlaybackClient, Asserting
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(TimeShiftWindow.class);
    private static final Logger performanceLog = Logger.getLogger("Performance.Tuning");

    /**
     * Back-reference to the TimeShiftManager
     */
    static TimeShiftManagerImpl s_tsm;

    /**
     * The Service the TimeShiftWindow is operating on.
     */
    private final ServiceExt m_service;
    
    private ServiceDetailsExt m_serviceDetails;

    /**
     * The currently-applicable PIDMapTable. If this is null, it implies we're
     * not ready to buffer. Should change any time there's a change in SI
     */
    private PidMapTable m_curPidMap;
    
    /** 
     * The decrypt session. 
     */
    CASession m_caSession;

    /**
     * Priority for receiving retune callbacks from ExtendedNetworkInterface
     */
    static final int ENI_CALLBACK_PRIORITY = 20;

    /**
     * Priority for receiving retune callbacks from ExtendedNetworkInterface
     */
    static final int EPMTM_NOTIFYCHANGE_PRIORITY = 20;

    /**
     * These uses represent functions which require a tuned NetworkInterface
     */
    static final int usesThatRequireTuner = TimeShiftManager.TSWUSE_NIRES | TimeShiftManager.TSWUSE_BUFFERING
            | TimeShiftManager.TSWUSE_LIVEPLAYBACK | TimeShiftManager.TSWUSE_RECORDING;

    /**
     * These uses represent functions which require a synced NetworkInterface
     */
    static final int usesThatRequireSync = TimeShiftManager.TSWUSE_BUFFERING | TimeShiftManager.TSWUSE_LIVEPLAYBACK;

    /**
     * These uses represent functions which require a TSB to be present
     */
    static final int usesThatRequireTSB = TimeShiftManager.TSWUSE_BUFFERING | TimeShiftManager.TSWUSE_BUFFERPLAYBACK
            | TimeShiftManager.TSWUSE_RECORDING | TimeShiftManager.TSWUSE_NETPLAYBACK;

    /**
     * These uses represent functions which only 1 client may perform on a
     * TimeShiftWindow at any time
     */
    static final int exclusiveUses = TimeShiftManager.TSWUSE_BUFFERPLAYBACK | TimeShiftManager.TSWUSE_LIVEPLAYBACK
            | TimeShiftManager.TSWUSE_RECORDING;

    private static final long NANOS_PER_MILLI = 1000000;
    private static final long NANOS_PER_SEC = 1000000000;
    
    /**
     * Container for the parameters which constrain the TimeShiftWindow. These
     * parameters are tracked on a per-client basis and as a whole on the
     * TimeShiftWindow. A value of 0/null for any field signifies that the
     * parameter is not a constraint.
     */
    class TimeShiftConstraints
    {
        int uses;

        int reservations;

        long minDuration;

        long maxDuration;

        long desiredDuration;

        ServiceComponentExt[] components;

        public TimeShiftConstraints()
        {
            this.uses = 0;
            this.reservations = 0;
            this.minDuration = 0;
            this.maxDuration = Long.MAX_VALUE;
            this.desiredDuration = 0;
            this.components = null;
        }

        public TimeShiftConstraints(final int uses, final int reservations, final long minDuration,
                final long maxDuration, final long desiredDuration, final ServiceComponentExt[] components)
        {
            this.uses = uses;
            this.reservations = reservations;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
            this.desiredDuration = desiredDuration;
            this.components = components;
        }

        public TimeShiftConstraints(final TimeShiftConstraints source)
        {
            this.uses = source.uses;
            this.reservations = source.reservations;
            this.minDuration = source.minDuration;
            this.maxDuration = source.maxDuration;
            this.desiredDuration = source.desiredDuration;
            this.components = source.components;
        } // END copy constructor

        public String toString()
        {
            return "TSC:[uses " + TimeShiftManagerImpl.useString(uses) + ",res " + TimeShiftManagerImpl.useString(reservations)
                    + ",mind " + minDuration + ",desd " + desiredDuration + ",maxd "
                    + ((maxDuration == Long.MAX_VALUE) ? "MAX_VAL" : Long.toString(maxDuration)) + ']';
        }
    } // END class TimeShiftConstraints

    //
    // These properties are directly set/get-able by TimeShiftManager
    //
    
    ArrayList m_clients = new ArrayList();

    volatile TimeShiftConstraints m_constraints = new TimeShiftConstraints();

    /**
     * TimeShift objects make up the TimeShiftWindow. This list is sorted by
     * TimeShift.startTime.
     * 
     * Any TimeShift may have TimeShift.presenting==true. Any TimeShift may have
     * TimeShift.recording==true. Only the last TimeShift may have
     * TimeShift.buffering==true.
     */
    private ArrayList m_timeShifts = new ArrayList();

    /**
     * State of the TimeShiftWindow. The state codes are defined in
     * {@link org.cablelabs.impl.manager.TimeShiftManager}.
     */
    private volatile int m_curState;

    /**
     * The reason associated with the last state change. This is effectively
     * used as a minor state in some cases.
     */
    private volatile int m_stateChangeReason = TimeShiftManager.TSWREASON_NOREASON;

    // The TimeShiftWindow itself is the ResourceClient for the NIC
    private final ExtendedNetworkInterfaceController m_nic = new ExtendedNetworkInterfaceController(this);

    private ExtendedNetworkInterface m_eni = null;

    private final Object m_tuneCookie;

    // This value is read from ini file
    private static long m_expirationTimeSec = 0;

    private boolean m_deathTimerStarted = false;

    /** A list of Players presenting this TSB */
    private Vector m_players = new Vector();

    /** A list of PlaybackClientObservers. */
    private Vector m_playbackListeners = new Vector();

    private ExpirationTrigger m_expireTimer = null;

    private final String m_logPrefix;

    /**
     * This condition is used to block ResourceClient.release() while NI
     * shutdown is underway.
     */
    private final SimpleCondition m_niReleasedCondition = new SimpleCondition(false);

    /**
     * This condition is used to block
     * NetworkInterfaceCallback.notifyRetunePending() and
     * NetworkInterfaceCallback.notifyUntune() while NI shutdown is underway.
     */
    private final SimpleCondition m_niUsesRemovedCondition = new SimpleCondition(false);

    /**
     * This condition is used to block TableChangeListener.notifyChange() while
     * buffering shutdown is underway.
     */
    private final SimpleCondition m_bufUsesRemovedCondition = new SimpleCondition(false);

    /*
     * Assumption here that different TimeShiftWindowClientImpl do not have
     * different lists of TSBs.
     */
    private LightweightTriggerEventStoreChangeImpl m_lwteChangeHelper = new LightweightTriggerEventStoreChangeImpl();

    private TimeShiftWindowMonitorListener m_tswm;

    private TimeBase m_timeBase;

    private final long m_timeBaseStartTimeNs;

    private long m_systemStartTimeMs;

    private boolean m_isAuthorized;

    /**
     * The TSW constructor will only be called by the TimeShiftManager and only
     * from within a sync block.
     * 
     * @param tsm
     *            Back reference to the TimeShiftManager.
     */
    TimeShiftWindow(final TimeShiftManagerImpl tsmi, final Service service)
    {
        m_curState = TimeShiftManager.TSWSTATE_IDLE;

        s_tsm = tsmi;

        m_service = (ServiceExt) service;

        m_tuneCookie = new Object();
        
        m_logPrefix = "TSW 0x" + Integer.toHexString(this.hashCode()) + ": ";

        m_expirationTimeSec = s_tsm.getTSWDeathTime() * SequentialMediaTimeStrategy.SEC_TO_MS; // 1000;

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "TSW death timer set to " + m_expirationTimeSec + "ms");
        }

        m_timeBase = new MediaTimeBase();

        m_timeBaseStartTimeNs = m_timeBase.getNanoseconds();
        m_systemStartTimeMs = System.currentTimeMillis();
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "TSW timeBaseStartTime is " + m_timeBaseStartTimeNs
                    / SequentialMediaTimeStrategy.MS_TO_NS + "ms");
            log.debug(m_logPrefix + "TSW systemStartTime is " + m_systemStartTimeMs + "ms");
        }
    } // END TimeShiftWindow constructor
    
    public String toString()
    {
        final TimeShiftBuffer lastTSB = getNewestTimeShiftBuffer();
        return "TSW 0x" + Integer.toHexString(this.hashCode()) + ":[service " + m_service.getID() 
                + " (" + m_service.getLocator() + "),state "
                + TimeShiftManager.stateString[m_curState] + ",tuner "
                + ((m_eni == null) ? "none" : Integer.toString(m_eni.getHandle())) + ",tbst "
                + (m_timeBaseStartTimeNs / SequentialMediaTimeStrategy.MS_TO_NS) + "ms" + ",ltsb "
                + ((lastTSB == null) ? "none" : lastTSB.toString())
                + ", nclients " + m_clients.size() + ", " + m_constraints + ']';
    } // END toString()

    /**
     * Get the Service being timeshifted by the TimeShiftWindow.
     * 
     * @return Service being timeshifted by the TimeShiftWindow.
     */
    public ServiceExt getService()
    {
        return m_service;
    } // END getService()

    /**
     * Return the current state of the TimeShiftWindow.
     * 
     * @return The current state of the TimeShiftWindow.
     */
    public int getState()
    {
        return m_curState;
    } // END getState()

    /**
     * Return the NI associated with the TimeShiftWindow.
     * 
     * @return The NI associated with the TimeShiftWindow.
     */
    public NetworkInterface getNetworkInterface()
    {
        return m_eni;
    } // END getNetworkInterface()

    /**
     * Set the state of the TimeShiftWindow. Normally, the TSW manages its own
     * state. In some cases, the TimeShiftManager will explicitly set the state.
     * 
     * @param state
     *            The new state of the TimeShiftWindow.
     */
    void setStateNoNotify(int state, int reason)
    {
        synchronized (this)
        {
            int oldState = m_curState;
            m_curState = state;
            m_stateChangeReason = reason;

            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "setStateNoNotify(): Changed from state "
                        + TimeShiftManager.stateString[oldState] + " to " + TimeShiftManager.stateString[state]);
            }
        }
    } // END setStateNoNotify()

    /**
     * Set the state of the TimeShiftWindow and notify registered listeners
     * asynchronously.
     * 
     * @param newState
     *            The new state of the TimeShiftWindow.
     * @param reason 
     *            Reason for the state change
     */
    void setStateAndNotify(int newState, int reason)
    {
        // Assert: Caller holds the TSW monitor
        List notifications 
            = buildStateChangeNotifications(newState, reason);
        
        if (notifications != null)
        {
            final CallerContext ctx = TimeShiftManagerImpl.m_ccm.getSystemContext();
            
            for (Iterator it=notifications.iterator(); it.hasNext();)
            {
                Runnable notify = (Runnable)it.next();
                ctx.runInContextAsync(notify);
            }
        }
    } // END setStateAndNotify()

    /**
     * Build a state change notification Runnable from the current state
     * to newState.
     * 
     * @param newState
     *            The new state of the TimeShiftWindow.
     * @param reason
     *            State change reason.
     *            
     * @return List of Runnable state change notifications or null if the
     *         current state is the same as the new state
     */
    List buildStateChangeNotifications(int newState, int reason)
    {
        // Assert: TSW lock is held

        if (newState == m_curState)
        {
            return null;
        }

        int oldState = m_curState;
        m_curState = newState;
        m_stateChangeReason = reason;
        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "Changed from state "
                    + TimeShiftManager.stateString[oldState] + " to " + TimeShiftManager.stateString[newState]);
        }

        return buildStateChangeNotifications(oldState, newState, reason);
    } // END buildStateChangeNotifications()

    /**
     * Perform the CCI notifications.
     * 
     * Handle a change to the CCI bits.
     * 
     * @param cci
     * 
     * CCI Bits:   7      6      5      4     3      2      1      0
     *            resv   resv   resv   CIT   APS1   APS0   EMM1   EMM0
     *           
     * EMM 00: Copying not restricted
     * EMM 01: No further copying permitted
     * EMM 10: One generation copy is permitted
     * EMM 11: Copying is prohibited
     * 
     * 
     */
    void notifyCCIChange(final CopyControlInfo cci)
    {
        List notifications = null;

        synchronized (this)
        {
            notifications = buildCCIChangeNotifications(cci);
        } // END synchronized(this)

        if (notifications != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Signaling " + cci);
            }

            final CallerContext ctx = TimeShiftManagerImpl.m_ccm.getSystemContext();
            for (Iterator it=notifications.iterator(); it.hasNext();)
            {
                ctx.runInContextAsync((Runnable)it.next());
            }
        }
    } // END notifyCCIChange()
    
    /**
     * Attempt to reserve a NetworkInterface and tune the service set on the the
     * TimeShiftWindow.
     * 
     * This essentially reconfigures the TimeShiftWindow for an entirely
     * different service and is only called by the TimeShiftManager. This method
     * will usually cause a NetworkInterface to be reserved and a tune to be
     * initiated.
     * 
     * This call should only be made when: (1) the TimeShiftWindow is in the
     * IDLE state (2) no clients are attached (which implies that the TSB(s) in
     * the TSW are not in use). (3) setService() has been invoked.
     * 
     * @param eru
     *            The resource usage to use to reserve the NI
     * 
     */
    void reserveNIAndTune(ResourceUsage ru)
    {
        // Assert: We're not being called on a TSWClient's thread
        // Assert: We're running in System context
        // Assert: TSW state is RESERVE_PENDING

        //
        // We will reserve and initiate tune holding this TSM-global monitor
        // The states of all TSW's is really unknown until the reservation
        // process has completed (since NIs may be taken from one TSW and
        // given to another).
        //
       
        synchronized (TimeShiftManagerImpl.m_niReserveMonitor)
        { // Note the order of monitor acquisition - should always get the
          // ReserveMonitor first
            synchronized (this)
            {
                try
                {
                    if (((m_constraints.uses & usesThatRequireTuner) == 0)
                            && ((m_constraints.reservations & usesThatRequireTuner) == 0))
                    { // The tuner is no longer required by any clients
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "reserveNIAndTune: current state is "
                                    + TimeShiftManager.stateString[m_curState]
                                    + " and no tuner uses remaining - bailing out.");
                        }

                        // Bailing out due to lack of interest
                        setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_NOREASON);
                        return;
                    }

                    if (m_curState != TimeShiftManager.TSWSTATE_RESERVE_PENDING)
                    {
                        // OK - tuner was release()-ed before we could use it
                        // State change notification should have already
                        // happened
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "reserveNIAndTune: NI released before we could use it");
                        }
                        return;
                    }

                    // We're going to try and reserve/tune.
                    // Consider the NI "not released" until further notice
                    m_niReleasedCondition.setFalse();
                } // END try
                catch (Exception e)
                {
                    setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_NOFREEINT);

                    if (log.isInfoEnabled())
                    {
                        log.info(m_logPrefix + "reserveNIAndTune: Unexpected error during setup:" + e);
                    }

                    SystemEventUtil.logRecoverableError(e);
                    return;
                } // END try/catch
            } // END synchronized (this)

            //
            // Attempt to reserve a NetworkInterface
            //

            //
            // Note: We do this while *not* holding the TSW lock - since the
            // ResourceContentionHandler may be invoked as part of
            // the reserve. But we will hold a monitor that will prevent
            // multiple calls to reserveFor() by different TSWs - since
            // that has led to some fairly terrible resource contention issues
            //

            boolean reserved;

            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "reserveNIAndTune: Attempting to reserve a NI with ru " + ru);
                }

                if (performanceLog.isInfoEnabled())
                {
                    performanceLog.info("Tuner Acquisition Start: Locator " + m_service.getLocator().toExternalForm());
                }
               
                // Note: This will throw NoFreeInterfaceException if a NI cannot
                // be reserved for the locator
                m_nic.reserveFor((ResourceUsageImpl) ru, (org.davic.net.Locator)m_service.getLocator(), null);

                // Assert: An NI is reserved (else we're in the exception
                // handler)
                m_eni = (ExtendedNetworkInterface) m_nic.getNetworkInterface();
                reserved = true;
     
                if (performanceLog.isInfoEnabled())
                {
                    performanceLog.info("Tuner Acquisition Complete: Tuner " +
                                                m_eni.getHandle() +
                                                ", Locator " +
                                                m_service.getLocator().toExternalForm());
                }      
                
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "reserveNIAndTune: RESERVED an NI (tuner " + m_eni.getHandle() + ')');
                }

            } // END try
            catch (NoFreeInterfaceException nfie)
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "reserveNIAndTune: failed with NoFreeInterfaceException");
                }

                reserved = false;
            }
            // For any other NetworkInterfaceException
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "reserveNIAndTune: NetworkInterface failed to reserve", e);
                }

                SystemEventUtil.logRecoverableError(e);

                reserved = false;
            }

            boolean tuneStarted = false;
            
            if (reserved)
            {
                try
                {
                    // Register for ENI callback. We'll use this for all
                    // tune-related notifications
                    m_eni.addNetworkInterfaceCallback(this, ENI_CALLBACK_PRIORITY);

                    if (log.isInfoEnabled())
                    {
                        log.info(m_logPrefix + "reserveNIAndTune: TUNING NI (" + m_eni.getHandle() + ") to "
                                + m_service.getLocator());
                    }

                    // If we find a null cookie in a NetworkInterfaceCallback,
                    // we got called synchronously
                    m_nic.extendedTune(m_service, m_tuneCookie);

                    if (log.isInfoEnabled())
                    {
                        log.info( m_logPrefix + "reserveNIAndTune: TUNE STARTED: 0x" 
                                  + Integer.toHexString((m_tuneCookie == null) ? 0 : m_tuneCookie.hashCode()) );
                    }
                    tuneStarted = true;
                }
                catch (NetworkInterfaceException nie)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix
                                  + "reserveNIAndTune: NetworkInterface failed to tune due to NetworkInterfaceException.", nie);
                    }

                    if (m_eni != null)
                    {
                        m_eni.removeNetworkInterfaceCallback(this);
                    }
                } // END catch (NetworkInterfaceException nie)
                catch (Exception e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix
                                + "reserveNIAndTune: NetworkInterface failed to tune due to Exception.", e);
                    }

                    SystemEventUtil.logRecoverableError(e);

                    if (m_eni != null)
                    {
                        m_eni.removeNetworkInterfaceCallback(this);
                    }
                }
            } // END if (reserved)
            
            synchronized (this)
            {
                try
                {
                    // Do this again under lock - just to be sure
                    ExtendedNetworkInterface eni = (ExtendedNetworkInterface) m_nic.getNetworkInterface();

                    if (!reserved || (eni == null))
                    {
                        m_eni = null;
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "reserveNIAndTune: Failed to reserve an NI");
                        }

                        m_niReleasedCondition.setTrue();

                        // On any error that prevents successful tune, go back
                        // to IDLE
                        setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_NOFREEINT);

                        // Note: Everything else gets cleaned up when client(s)
                        // detach
                        return;
                    }

                    //
                    // Assert: We have a reserved NI
                    //

                    //
                    // Make sure we're still wanted
                    //
                    if (((m_constraints.uses & usesThatRequireTuner) == 0)
                            && ((m_constraints.reservations & usesThatRequireTuner) == 0))
                    { // The tuner is no longer required by any clients
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "reserveNIAndTune: current state is "
                                    + TimeShiftManager.stateString[m_curState]
                                    + " and no tuner uses remaining - bailing out.");
                        }

                        // Bailing out due to lack of interest (we'll give up the NI elsewhere)
                        setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_NOREASON);

                        return;
                    }

                    if (tuneStarted)
                    {
                        // Tune cookie was already assigned
                        setStateAndNotify(TimeShiftManager.TSWSTATE_TUNE_PENDING, TimeShiftManager.TSWREASON_NOREASON);
                        // Note: We may have already transitioned to TUNE_PENDING, if we received the
                        //  notifyTunePending before getting here 
                    }
                    else
                    { // Tuning couldn't start
                        m_eni = null;
                        m_niReleasedCondition.setTrue();
    
                        // On any error that prevents successful tune, go back to
                        // IDLE
                        setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_TUNEFAILURE);
                        // Note: Everything else gets cleaned up when client(s) detach
                    }
                }
                catch (Exception e)
                {
                    m_eni = null;
                    m_niReleasedCondition.setTrue();

                    // On any error that prevents successful tune, go back to
                    // IDLE
                    setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_TUNEFAILURE);

                    
                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "reserveNIAndTune: Caught an exception after reserve/tune", e);
                    }
                    // Note: Everything else gets cleaned up when client(s) detach
                }
            } // END synchronized (this)
        } // END synchronized (TimeShiftManagerImpl.m_niReserveMonitor)
    } // END reserveNIAndTune()

    /**
     * Add a client to the TimeShiftWindow. Note that the TimeShiftManager
     * should have checked this TimeShiftWindow for suitability of the
     * TimeShiftwindow for the added client. i.e. The TSW should have been
     * checked to see if the service, duration, usage can support the client.
     */
    public TimeShiftWindowClient addClient(long minDuration, long desiredDuration, int reservations,
            ResourceUsage initUsage, TimeShiftWindowChangedListener tswcl, final int tswclPriority)
    {
        final TimeShiftWindowClientImpl newClient; 
        synchronized (this)
        {
            // Create a new TimeShiftWindowClient
            newClient = new TimeShiftWindowClientImpl( this, minDuration, Long.MAX_VALUE,
                                                       desiredDuration, 0, /* no use yet */
                                                       reservations, initUsage, tswcl, tswclPriority );
            
            // Add this client to the TimeShiftWindow's client list according to its priority
            final int numClients = m_clients.size();
            int insertPosition = 0;
            while (insertPosition < numClients)
            {
                final TimeShiftWindowClientImpl curClient = (TimeShiftWindowClientImpl) 
                                                            m_clients.get(insertPosition);
                // Stop when we encounter a client with a lower priority
                //  (doing a ">" here rather than ">=" to reduce the number shifted...)
                if (tswclPriority > curClient.m_listenerPriority)
                {
                    break;
                }
                insertPosition++;
            }
            
            m_clients.add(insertPosition, newClient);

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "addClient: Prior constraints: " + m_constraints);
            }

            // Update the TimeShiftWindow's constraints accordingly
            m_constraints = calculateConstraints();

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "addClient: New constraints: " + m_constraints);
            }

            // Note: We don't necessarily need/want to stop the death timer
            // at this point. We may want to stop buffering and/or
            // release the TSB, for example. But we won't release the
            // the TSW (since m_clients is non-empty)

            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "addClient: Created client: " + newClient);
            }
        } // END synchronized (this)
        
        return newClient;
    } // END addClient()

    /**
     * Remove the TimeShiftWindowClient from the attached client list and update
     * the TimeShiftWindow accordingly.
     * 
     * @param tswc
     *            The TimeShiftWindowClient to detach
     */
    void removeClient(final TimeShiftWindowClient tswc)
    {
        synchronized (this)
        {
            // Assert: tswc is detached

            TimeShiftWindowClientImpl tswci = (TimeShiftWindowClientImpl) tswc;

            m_clients.remove(tswci);

            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "removeClient: Removing client: " + tswci);
            }

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "removeClient: Prior constraints: " + m_constraints);
            }

            // Update the TimeShiftWindow's constraints accordingly
            m_constraints = calculateConstraints();

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "removeClient: New constraints: " + m_constraints);
            }

            if (m_clients.isEmpty())
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "removeClient: Last client removed");
                }
                // Start the death timer if it's not already started
                startDeathTimer();
            }
        } // END synchronized (this)
    } // END removeClient()

    /**
     * Construct a composite Runnable to perform state change notifications for all listeners
     * on all clients.
     * 
     * @param oldState
     *            Previous state
     * @param newState
     *            New state
     * @param reason
     *            Reason for state change
     *            
     * @return List of Runnables that perform the notifications
     */
    private List buildStateChangeNotifications(int oldState, int newState, int reason)
    {
        // Assert: this held by caller
        final ArrayList notifications = new ArrayList();

        final TimeShiftWindowStateChangedEvent changedEvent 
                  = new TimeShiftWindowStateChangedEvent(oldState, newState, reason);

        // Walk the list of TSWCs
        for (final Iterator tswce = m_clients.iterator(); tswce.hasNext();)
        {
            final TimeShiftWindowClientImpl client = (TimeShiftWindowClientImpl) tswce.next();
            final TimeShiftWindowChangedListener tswcl = client.m_listener;

            if (tswcl != null)
            {
                notifications.add(new Runnable()
                {
                    public void run()
                    {
                        tswcl.tswStateChanged(client, changedEvent);
                    }
                });
            }
        } // END loop through tsw clients
        
        return notifications;
    } // END buildStateChangeNotifications()

    /**
     * Construct a composite Runnable to perform CCI change notifications for all listeners
     * on all clients.
     * 
     * @param cci
     *            New CCI byte
     * @param timeBaseTime
     *            The effective timebase time for the notification
     */
    private List buildCCIChangeNotifications(final CopyControlInfo cci)
    {
        // Assert: this held by caller
        final ArrayList notifications = new ArrayList();

        // Walk the list of TSWCs
        for (final Iterator tswce = m_clients.iterator(); tswce.hasNext();)
        {
            final TimeShiftWindowClientImpl client = (TimeShiftWindowClientImpl) tswce.next();
            final TimeShiftWindowChangedListener tswcl = (TimeShiftWindowChangedListener) client.m_listener;

            notifications.add( new Runnable()
                               {
                                   public void run()
                                   {
                                       if (log.isDebugEnabled())
                                       {
                                           log.debug(m_logPrefix + "Issuing CCI change notification...");
                                       }
                                       tswcl.tswCCIChanged(client, cci);
                                   }
                               } );
        } // END loop through tsw clients

        return notifications;
    } // END buildCCIChangeNotifications()
    
    /**
     * Update all TimeShiftWindow constraint parameters to conform to all
     * attached TimeShiftWindowClients and their associated usages.
     */
    TimeShiftConstraints calculateConstraints()
    {
        // Assert: Caller holds lock
        TimeShiftConstraints newConstraints = new TimeShiftConstraints();

        // Assert: newConstraints.uses == 0
        // Assert: newConstraints.maxOfAllMins == 0
        // Assert: newConstraints.maxOfAllDesired == 0
        // Assert: newConstraints.minOfAllMaxs == Integer.MAX_VALUE
        // Assert: newConstraints.sumOfAllUses == 0

        final int numClients = m_clients.size();

        // Walk the list of TSWCs
        for (final Iterator tswce = m_clients.iterator(); tswce.hasNext();)
        {
            TimeShiftWindowClientImpl client = (TimeShiftWindowClientImpl) tswce.next();

            if (client.m_constraints.minDuration > newConstraints.minDuration)
            {
                newConstraints.minDuration = client.m_constraints.minDuration;
            }

            if (client.m_constraints.desiredDuration > newConstraints.desiredDuration)
            {
                newConstraints.desiredDuration = client.m_constraints.desiredDuration;
            }

            if (client.m_constraints.maxDuration < newConstraints.maxDuration)
            {
                newConstraints.maxDuration = client.m_constraints.maxDuration;
            }

            // Since uses is (currently) represented as a bitmask,
            // just or-in the client's uses

            newConstraints.uses |= client.m_constraints.uses;

            // Since reservations are (currently) represented as a bitmask,
            // just or-in the client's reservation

            newConstraints.reservations |= client.m_constraints.reservations;
        } // END loop through all clients

        return newConstraints;
    } // END calculateConstraints()

    void updateForAddedUses(TimeShiftWindowClientImpl tswClient, int addedUses) 
        throws IllegalStateException, IllegalArgumentException
    {
        // Assert: Caller holds lock
        // Clients can be removed in any state
        // newClients constraints have been incorporated during addClient()
        // newClient's uses haven't been updated with uses yet
        // Adding uses can remove client reservations

        // Check for valid attachment state
        if ( ((addedUses & usesThatRequireTuner) != 0)
             && (! ( (m_curState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER)
                     || (m_curState == TimeShiftManager.TSWSTATE_BUFFERING) 
                     || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING) 
                     || (m_curState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER) ) ) )
        {
            throw new IllegalStateException("Illegal attachment for (tuned) tuner use "
                    + TimeShiftManagerImpl.useString(addedUses) + " in state " + TimeShiftManager.stateString[m_curState]
                    + " - " + tswClient);
        }

        if ( ((addedUses & usesThatRequireSync) != 0)
             && (! ( (m_curState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER)
                     || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING)
                     || (m_curState == TimeShiftManager.TSWSTATE_BUFFERING) ) ) )
        {
            throw new IllegalStateException("Illegal attachment for (synced) tuner use "
                    + TimeShiftManagerImpl.useString(addedUses) + " in state " + TimeShiftManager.stateString[m_curState]
                    + " client: " + tswClient);
        }

        if ((m_constraints.uses & exclusiveUses & addedUses) != 0)
        {
            throw new IllegalStateException("Illegal exclusive attachment for use "
                    + TimeShiftManagerImpl.useString(m_constraints.uses & exclusiveUses & addedUses) + " - " + tswClient);
        }

        // Assert: We've handled all cases where we know the client can't
        // attach

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "updateForAddedUses: Adding use(s) " + TimeShiftManagerImpl.useString(addedUses)
                    + " for client " + tswClient);
        }

        TimeShiftConstraints newConstraints = calculateConstraints();

        // Update our active buffering session if necessary
        if ( ((addedUses & TimeShiftManager.TSWUSE_BUFFERING) != 0)
             && ( (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING)
                  || (m_curState == TimeShiftManager.TSWSTATE_BUFFERING) ) )
        {
            // This will take care of updating m_constraints for the durations
            //  and attempting duration changes if/when necessary
            updateForChangedDuration(newConstraints);
            // Note: May throw IllegalArgumentException

            // Assert: duration change completed (in TSWSTATE_BUFFERING)
            // or TSB restart cycle started (in TSWSTATE_BUFSHUTDOWN)
        }

        // Initiate buffering, if required
        if (((addedUses & TimeShiftManager.TSWUSE_BUFFERING) != 0)
                && (m_curState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER))
        { // New client wants buffering, and we're ready to buffer
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "updateForAddedUses: New use requires buffering.");
            }

            if (m_caSession == null)
            {
                startDescrambling();
            }
            
            if (m_isAuthorized)
            {
                startBuffering();
                // startBuffering will use m_constraints to setup the TSB and
                // update the current buffer use.
                // If it fails, it will throw an IllegalArgumentException exception
                // or IllegalStateException
    
                setStateNoNotify(TimeShiftManager.TSWSTATE_BUFF_PENDING, TimeShiftManager.TSWREASON_NOREASON);
            }
            else
            {
                setStateAndNotify(TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER, TimeShiftManager.TSWREASON_ACCESSWITHDRAWN);
                tswClient.m_constraints.uses &= ~TimeShiftManager.TSWUSE_BUFFERING;
                // Note: We have an active CASession that may get us out of this state
            }
        } // END if (buffering is now required)

        // Update the constraints on the TimeShiftWindow
        m_constraints.uses = newConstraints.uses;
        m_constraints.reservations = newConstraints.reservations;

        if ((m_eni != null) && (m_curState != TimeShiftManager.TSWSTATE_IDLE))
        {
            // Incorporate the client's ResourceUsage
            updateNIResourceUsages();
        }
    } // END updateForAddedUses()

    void updateForRemovedUses(TimeShiftWindowClientImpl tswClient, int removedUses)
    {
        // Assert: Caller holds lock
        // Clients can be removed in any state
        // oldClient uses have not been updated yet
        // Removing uses can remove reservations

        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "updateForRemovedUses: Updating for removed use(s) " 
                       + TimeShiftManagerImpl.useString(removedUses)
                       + " from client " + tswClient );
        }

        // We need to recalculate the new uses - since removal of a use
        // by a client does not necessarily mean the use is not needed
        TimeShiftConstraints newConstraints = calculateConstraints();

        // Update the constraints on the TimeShiftWindow
        m_constraints.uses = newConstraints.uses;
        m_constraints.reservations = newConstraints.reservations;

        //
        // Now see if there are things we should shut down/start shutting down
        //

        int newState = TimeShiftManager.TSWSTATE_INVALID;
        int reason = TimeShiftManager.TSWREASON_NOREASON;

        // case 1: If updated client uses (m_constraints) indicate we don't
        // need to be buffering, but we are, then stop buffering now
        // if the state is BUFFSHUTDOWN, or later if BUFFERING
        if ((m_constraints.uses & TimeShiftManager.TSWUSE_BUFFERING) == 0)
        { // Buffering is no longer required by any clients
            // If current state is TSWSTATE_BUFF/INTSHUTDOWN we need to stop
            // buffering immediately, in all other states start the death timer
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "updateForRemovedUses: current state is "
                        + TimeShiftManager.stateString[m_curState] + ", and no buffering uses remaining.");
            }

            m_bufUsesRemovedCondition.setTrue();

            switch (m_curState)
            {
                case TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN:
                {
                    if (ableToBuffer())
                    {
                        newState = TimeShiftManager.TSWSTATE_READY_TO_BUFFER;
                    }
                    else
                    {
                        newState = TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER;
                    }
                    reason = m_stateChangeReason;
                    // DROP THROUGH
                }
                case TimeShiftManager.TSWSTATE_INTSHUTDOWN:
                {
                    // Stop buffering now if we're in BUFF/INTSHUTDOWN
                    stopBuffering(m_stateChangeReason);
                    
                    if ( (m_stateChangeReason == TimeShiftManager.TSWREASON_PIDCHANGE)
                         || (m_stateChangeReason == TimeShiftManager.TSWREASON_SERVICEREMAP) ) 
                    {
                        stopDescrambling();
                    }
                    break;
                }
                case TimeShiftManager.TSWSTATE_BUFFERING:
                case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                {
                    // Reschedule the death timer
                    stopTimer();

                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "updateForRemovedUses: No buffering uses remaining.");
                    }

                    // Start death timer
                    startDeathTimer();
                    break;
                }
                default: // Don't do anything in other states
            } // END switch (m_curState)
        } // END buffering shutdown check

        // case 2: Updated client uses indicate we don't need
        // the tuner
        if ((m_constraints.uses & usesThatRequireTuner) == 0)
        { // The tuner is no longer required by any clients
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "updateForRemovedUses: current state is "
                        + TimeShiftManager.stateString[m_curState] + '/'
                        + TimeShiftManager.reasonString[m_stateChangeReason] + " and no tuner uses remaining.");
            }

            // Will unblock NetworkInterfaceCallback.notifyRetunePending()
            m_niUsesRemovedCondition.setTrue();

            switch (m_curState)
            {
                case TimeShiftManager.TSWSTATE_INTSHUTDOWN:
                {
                    // Have to use the reason code to establish the next state
                    // (essentially treating it as a minor state)
                    switch (m_stateChangeReason)
                    {
                        case TimeShiftManager.TSWREASON_INTLOST:
                        {
                            releaseTuner(false);
                            newState = TimeShiftManager.TSWSTATE_IDLE;
                            break;
                        }
                        case TimeShiftManager.TSWREASON_SERVICEREMAP:
                        case TimeShiftManager.TSWREASON_SERVICEVANISHED:
                        {
                            newState = TimeShiftManager.TSWSTATE_TUNE_PENDING;
                            break;
                        }
                        default:
                        { // This doesn't make any sense - we should have come
                            // to INTSHUTDOWN for a reason...
                            if (log.isWarnEnabled())
                            {
                                log.warn(m_logPrefix + "updateForRemovedUses: Unexpected state change reason: "
                                        + TimeShiftManager.reasonString[m_stateChangeReason]);
                            }
                            releaseTuner(true);
                            newState = TimeShiftManager.TSWSTATE_IDLE;
                            break;
                        }
                    } // END switch (m_stateChangeReason)
                    break;
                }
                default:
                {
                    // Start the timer. We'll release the NI at that time if
                    // it's still no longer in use
                    startDeathTimer();
                    break;
                }
            } // END switch (m_curState)
        } // END tuner shutdown check

        // case 3: Updated client uses indicate we don't need the TSB (for
        // playback/conversion), start the death timer
        if (((m_constraints.uses & usesThatRequireTSB) == 0)
                && (m_stateChangeReason == TimeShiftManager.TSWSTATE_INVALID))
        { // TSB is no longer needed
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "updateForRemovedUses: No TSB uses remaining.");
            }
            startDeathTimer();
        } // END TSB shutdown check

        // If newState is INVALID, it means that the removal of the client
        // didn't affect the TSW state in any capacity
        if (newState != TimeShiftManager.TSWSTATE_INVALID)
        { // We did something to require state change - signal it
            setStateAndNotify(newState, reason);
        }

        if ((m_eni != null) && (tswClient.m_resourceUsage != null))
        {
            // Unincorporate the client's ResourceUsage
            updateNIResourceUsages();
        }
    } // END updateForRemovedUses()

    void updateForChangedDuration(final TimeShiftConstraints newConstraints) 
        throws IllegalStateException, IllegalArgumentException
    {
        // Update the uses on the TimeShiftWindow
        m_constraints.minDuration = newConstraints.minDuration;
        m_constraints.maxDuration = newConstraints.maxDuration;
        m_constraints.desiredDuration = newConstraints.desiredDuration;

        if ( (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING)
             || (m_curState == TimeShiftManager.TSWSTATE_BUFFERING) )
        { // We only want to adjust if we're in a stable buffering state
          //  (e.g. not in the process of shutting down buffering)
            final TimeShiftBufferImpl bufferingTSB = getNewestTimeShiftBuffer();

            if (bufferingTSB.bufferSize < m_constraints.minDuration)
            {
                resizeBufferingTSB(m_constraints.minDuration);
                // Note: May throw IllegalArgumentException
    
                // Assert: curTSW resize complete (in TSWSTATE_BUFFERING)
                // or TSB restart cycle started (in TSWSTATE_BUFSHUTDOWN)
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "updateForChangedDuration: No TSB size change required.");
                }
            }
            
            long newDesiredDuration = 0;
            long newMaxDuration = 0;
            
            if (bufferingTSB.desiredDuration != m_constraints.desiredDuration)
            {
                newDesiredDuration = m_constraints.desiredDuration;

                if (log.isInfoEnabled())
                {
                    log.info( m_logPrefix + "updateForChangedDuration: New use requires duration change to "
                              + newDesiredDuration + 's' );
                }
            }

            if (bufferingTSB.maxDuration != m_constraints.maxDuration)
            {
                newMaxDuration = m_constraints.maxDuration;

                if (log.isInfoEnabled())
                {
                    log.info( m_logPrefix + "updateForChangedDuration: New use requires max duration change to "
                              + newMaxDuration + 's' );
                }
            }

            if ((newDesiredDuration != 0) || (newMaxDuration != 0))
            {
                adjustBufferingTSBDurations(newDesiredDuration, newMaxDuration);
                // Note: May throw IllegalArgumentException

                // Assert: duration change completed (in TSWSTATE_BUFFERING)
                // or TSB restart cycle started (in TSWSTATE_BUFSHUTDOWN)
            } // END if (need to adjust the durations)
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "updateForChangedDuration: No duration change required.");
                }
            }
        } // END if (buffering)
    } // END updateForChangedDuration()

    void startDeathTimer()
    {
        // Assert: Caller holds lock
        if (m_deathTimerStarted)
        {
            return;
        }

        // Instantiate timer
        m_expireTimer = new ExpirationTrigger(System.currentTimeMillis() + m_expirationTimeSec);

        // attempt to schedule the start trigger
        try
        {
            m_expireTimer.scheduleTimer();
            m_deathTimerStarted = true;
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "startTimer: death timer scheduled");
            }
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    } // END startTimer()

    void stopTimer()
    {
        // Assert: Caller holds lock

        if (!m_deathTimerStarted)
        {
            return;
        }

        if (m_expireTimer != null)
        {
            m_expireTimer.descheduleTimer();
            m_deathTimerStarted = false;
            m_expireTimer = null;

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "stopTimer: death timer cancelled");
            }
        }
    } // END stopTimer()

    /**
     * Start the descrambling session 
     */
    void startDescrambling() 
    {
        // Assert: Caller holds the TSM lock
        if (m_curPidMap == null)
        {
            throw new IllegalArgumentException("PID map is null");
        }

        if (m_caSession != null)
        {
            throw new IllegalStateException("m_caSession is not null");
        }
        
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Starting descrambling session...");
            }
            
            final int numAVStreams = this.m_curPidMap.getAVEntryCount();
            
            int pids [] = new int[numAVStreams];
            short types [] = new short[numAVStreams];

            final int pidMapSize = m_curPidMap.getSize();
            int pidCount = 0;
            for (int i = 0; i < pidMapSize; i++)
            {
                PidMapEntry tsbEntry = m_curPidMap.getEntryAtIndex(i);

                if ( (tsbEntry.getStreamType() == MediaStreamType.AUDIO)
                     || (tsbEntry.getStreamType() == MediaStreamType.VIDEO) )
                {
                    pids[pidCount] = tsbEntry.getSourcePID();
                    types[pidCount] = tsbEntry.getSourceElementaryStreamType();
                    pidCount++;
                }
            }
            
            if (ASSERTING) Assert.condition(pidCount == numAVStreams);
            
            CADecryptParams decryptParams;

            decryptParams = new CADecryptParams( this,
                                                 m_serviceDetails,
                                                 m_eni.getHandle(),
                                                 pids,
                                                 types,
                                                 CADecryptParams.BUFFERING_PRIORITY );
            
            m_caSession = s_tsm.getPODManager().startDecrypt(decryptParams); 

            m_isAuthorized = (m_caSession == null   // No authorization required (analog/unencrypted)
                             || ( m_caSession.getLastEvent().getEventID() 
                                  == CASessionEvent.EventID.FULLY_AUTHORIZED ) );
            
                if (m_caSession != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "Descrambling session started: " + m_caSession);
                    }
                }
                else
                {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "No descrambling session needed.");
                }
            }
        }
        catch (MPEException e)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "error while trying to start decryption session (continuing)", e);
            }
        }
    } // END startDescrambling()

    void stopDescrambling()
    {
        synchronized (this)
        {
            if (m_caSession != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Stopping descrambling session...");
                }

                try
                {
                    m_caSession.stop();
                }
                catch (Throwable err)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Error shutting down decrypt session", err);
                    }
                }
                m_caSession = null;
            }
            m_isAuthorized = false;
        }
    } // END stopDescrambling()
    
    /**
     * Return true if all conditions necessary to start buffering are satisfied
     * 
     * @return true if the preconditions for startBuffering() are satisfied
     */
    boolean ableToBuffer()
    {
        try
        {
            if ((m_eni == null) || (!m_eni.isSynced(m_tuneCookie)))
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "ableToBuffer: FALSE (not synced) ");
                }
                // Can't buffer if we're not synced
                return false;
            }
        }
        catch (NotOwnerException e)
        {
            if (log.isDebugEnabled()) 
            {
                log.debug(m_logPrefix + "ableToBuffer: false (not owner)");
            }
            return false;
        }

        if (m_curPidMap == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "ableToBuffer: FALSE (no PID Map) ");
            }
            // Can't buffer if we don't have a PID map table
            return false;
        }
        
        if ((m_caSession != null) && !m_isAuthorized)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "ableToBuffer: FALSE (not authorized) ");
            }
            return false;
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "ableToBuffer: TRUE");
        }

        return true;
    } // END ableToBuffer()

    void startBuffering() throws IllegalStateException, IllegalArgumentException
    {
        // Assert: Caller holds lock
        // We'll try keeping policy here. But how we react to failure
        // may become context-sensitive. So be prepared to move the
        // exception handling up the call stack...

        TimeShiftBufferImpl tsb = s_tsm.getTSBForSize(m_constraints.minDuration);
        // Note: May throw IllegalArgumentException

        if (m_eni == null)
        { // Network interface has been yanked away, apparently
            // We will expect that release() has been/will be called to
            // take care of things.
            throw new IllegalStateException("NetworkInterface has been removed from " + this
                    + " during TimeShiftWindow.startBuffering()");
        }

        final int tunerID = m_eni.getHandle();

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "startBuffering: Starting buffering of service " + m_service.getLocator() + " ("
                    + tsb.getSize() + "s TSB)");
        }

        // Assert: PIDMapTable should be loaded if we're in the READY state
        int mpeError;

        // Note: May throw IllegalArgumentException, IllegalStateException
        mpeError = tsb.startBuffering( tunerID, getLTSID(), 
                                       0, m_curPidMap, m_service, 
                                       m_constraints.desiredDuration,
                                       m_constraints.maxDuration,
                                       this );

        // Assert: on tsb.startBuffering() success, pidMap will be added
        // to its component TimeTable

        if (mpeError != TimeShiftBufferImpl.MPE_DVR_ERR_NOERR)
        {
            s_tsm.returnTSB(tsb);
            throw new IllegalStateException("Error starting TSB (MPE Error " + mpeError + ')');
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "startBuffering: PID map: " + m_curPidMap + " - adding tsb: " + tsb);
        }

        // Add the TSB to the end of the TSB list, which is kept chronological
        m_timeShifts.add(tsb);

        // move lightweight trigger events located in previous tsb to the
        // correct segment. These events are called "future" events since they
        // occur
        // in the future (i.e., in a tsb that dosen't exist at the time they
        // were added).
        moveFutureEvents();

    } // END startBuffering()

    private void moveFutureEvents()
    {
        synchronized (this)
        {
            // need two or more tsbs for future events
            if (m_timeShifts.size() < 2)
            {
                return;
            }

            // future events are always in the next to last segment
            TimeShiftBufferImpl nextToLastTSB = getNextToLastTimeShiftBuffer();
            TimeShiftBufferImpl lastTSB = getNewestTimeShiftBuffer();

            // work through the list of events in the nextToLastSegInfo and move
            // any future events
            // event time is media time from beginning of segment so if the
            // media time is past the end of
            // the segment, it needs to move
            long nextToLastDurNS = nextToLastTSB.getDuration();
            LightweightTriggerEventTimeTable nextToLastLWTT = nextToLastTSB.getLightweightTriggerEventTimeTable();
            LightweightTriggerEventTimeTable lastLWTT = lastTSB.getLightweightTriggerEventTimeTable();
            // moves future events from next to last tt to newest (last) tt.
            lastLWTT.moveFutureLwte(nextToLastDurNS/NANOS_PER_MILLI, nextToLastLWTT);
        }
    }

    void registerForComponentChanges()
    {
        // Assert: caller holds lock
        TimeShiftManagerImpl.m_pmtm.addInBandChangeListener(this, m_service.getLocator(), EPMTM_NOTIFYCHANGE_PRIORITY);
    } // END registerForComponentChanges()

    void unregisterForComponentChanges()
    {
        // Assert: caller holds lock
        TimeShiftManagerImpl.m_pmtm.removeInBandChangeListener(this);
    } // END unregisterForComponentChanges()

    /**
     * Load a PidMapTable with components from the designated Service. If the
     * service contains no components (e.g. it's an analog service), the
     * PidMapTable will contain blank entries for the native code to supply the
     * recorded PIDs, if recording is possible.
     * 
     * @param service
     * @return PidMapTable with entries for all the ServiceComponents and a PCR
     *         PID
     * @throws IllegalArgumentException
     *             callers need to handle any exceptions thrown.
     */
    PidMapTable loadPidMapTable(ServiceExt service) throws IllegalArgumentException
    {
        try
        {
            if (service instanceof SPIService)
            {         
                ProviderInstance spi = (ProviderInstance) ((SPIService) service).getProviderInstance();
                SelectionSessionWrapper session = (SelectionSessionWrapper) spi.getSelectionSession((SPIService)service);
                service = session.getMappedService();
            }
            
            ServiceDetailsExt svcDetails = (ServiceDetailsExt) service.getDetails();

            m_serviceDetails = svcDetails;

            final ServiceComponentExt[] svcComponents = (ServiceComponentExt[]) svcDetails.getComponents();
            
            /*
             * check for analog service
             */
            if ((svcComponents == null) || (svcComponents.length == 0))
            {
                final PidMapTable blankPidTable = loadBlankPidMapTable();

                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "loadPidMapTable: Created analog pidMapTable: " + blankPidTable);
                }

                // return here so digital service code is not reached
                return blankPidTable;
            }

            /*
             * Digital Service Code
             * 
             * Test a digital service for at least one audio or video stream. If
             * there is no audio or video stream, throw an exception.
             */
            int numAVServiceComponents = 0;
            ServiceComponentExt vsc = null;
            ServiceComponentExt asc = null;
            for (int i = 0; i < svcComponents.length; i++)
            {
                final ServiceComponentExt sce = svcComponents[i];
                if (sce.isMediaStream())
                {
                    numAVServiceComponents++;
                }

                if (sce.getStreamType() == StreamType.AUDIO)
                {
                    asc = sce;
                }
                else if (sce.getStreamType() == StreamType.VIDEO)
                {
                    vsc = sce;
                }
            }

            if (numAVServiceComponents == 0)
            {
                throw new IllegalArgumentException("Service " + m_service.getLocator() + " contains no A/V streams");
            }

            int pcrPid = svcDetails.getPcrPID();
            if ((pcrPid == -1) || (pcrPid == 0x1FFF))
            { // No A/V components found in service
                throw new IllegalArgumentException("Service doesn't refer to a valid PCR PID (" + pcrPid + ')');
            }

            PidMapEntry pcrEntry = null;

            // the number of AV service components plus a entry for the PCR and
            // PMT PIDs.
            PidMapTable pidMap = new PidMapTable(numAVServiceComponents + 2);

            int avSvcComponentIndex = 0;
            for (int i = 0; i < svcComponents.length; i++)
            {
                final ServiceComponentExt sce = svcComponents[i];

                StreamType streamType = sce.getStreamType();
                if (!sce.isMediaStream())
                {
                    continue;
                }

                PidMapEntry mapEntry = new PidMapEntry(streamType, sce.getElementaryStreamType(), sce.getPID(),
                        PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, PidMapEntry.PID_UNKNOWN, sce);

                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "loadPidMapTable: Entry added " + mapEntry.toString());
                }

                pidMap.addEntryAtIndex(avSvcComponentIndex++, mapEntry);

                /*
                 * If this is the video stream it may (always?) be PCR stream.
                 */
                if (sce.getPID() == pcrPid)
                {
                    pcrEntry = mapEntry;
                }
            } // END curSvcComponents loop

            //
            // Put the PCR PID in the table
            //
            PidMapEntry pcrMapEntry = new PidMapEntry(MediaStreamType.PCR, PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, pcrPid,
                    PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, PidMapEntry.PID_UNKNOWN,
                    (pcrEntry != null) ? pcrEntry.getServiceComponentReference() : null);

            pidMap.addEntryAtIndex(avSvcComponentIndex++, pcrMapEntry);
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "loadPidMapTable: Entry added " + pcrMapEntry.toString());
            }

            //
            // Put the PMT PID in the table
            //
            ServiceExt serviceExt = (ServiceExt) service;
            ServiceDetailsExt sdExt = (ServiceDetailsExt) serviceExt.getDetails();
            TransportStreamExt tsExt = (TransportStreamExt) sdExt.getTransportStream();

            OcapLocator transportDepLocator = new OcapLocator(tsExt.getFrequency(), sdExt.getProgramNumber(),
                    tsExt.getModulationFormat());

            ProgramAssociationTableManager patm = ProgramAssociationTableManager.getInstance();

            class PATRequestor implements SIRequestor
            {
                public ProgramAssociationTable pat = null;
    			// Added conditional wait for findbugs issues fix
                private final SimpleCondition patAcquired = new SimpleCondition(false);

                public void notifyFailure(SIRequestFailureType reason)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "loadPidMapTable: PATRequestor::notifyFailure called");
                    }
                    patAcquired.setTrue();
                }

                public void notifySuccess(SIRetrievable[] result)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "loadPidMapTable: PATRequestor::notifySuccess called");
                    }
                    if (result != null)
                        pat = (ProgramAssociationTable) result[0];
                    patAcquired.setTrue();
                }

                public ProgramAssociationTable getPAT()
                {
                    try
                    {
                        patAcquired.waitUntilTrue(10000);
                    }
                    catch (InterruptedException e)
                    {
                    }

                    return pat;
                }
            } // END class PATRequestor

            PATRequestor patRequestor = new PATRequestor();

            // Retrieve In-band PAT
            SIRequest siRequest = patm.retrieveInBand(patRequestor, transportDepLocator);

            int pmtPid = -1;
            if (patRequestor.getPAT() != null)
            {
                PATProgram[] programs = patRequestor.getPAT().getPrograms();
                for (int i = 0; i < programs.length; i++)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "loadPidMapTable: Program[" + i + "]: "
                                + programs[i].getProgramNumber());
                    }
                    if (programs[i].getProgramNumber() == sdExt.getProgramNumber())
                    {
                        pmtPid = programs[i].getPID();
                        break;
                    }
                }
            }

            if (pmtPid == -1)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(m_logPrefix + "loadPidMapTable: Could not find the PMT PID for " + transportDepLocator
                            + "- leaving it out of the PidMapTable");
                }
                throw new IllegalArgumentException("Error retrieving the PMT PID for Service " + m_service.getLocator()
                        + " (program " + sdExt.getProgramNumber() + ')');
            }
            else
            {
                // Put the PMT PID in
                PidMapEntry pmtMapEntry = new PidMapEntry(MediaStreamType.PMT, PidMapEntry.ELEM_STREAMTYPE_UNKNOWN,
                        pmtPid, PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, PidMapEntry.PID_UNKNOWN, null);

                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "loadPidMapTable: Entry added " + pmtMapEntry.toString());
                }

                pidMap.addEntryAtIndex(avSvcComponentIndex++, pmtMapEntry);
            }

            return pidMap;
        }
        catch (InterruptedException ie)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "loadPidMapTable: Unable to retrieve SI", ie);
            }
            throw new IllegalStateException("Error retrieving SI for service " + m_service.getLocator() + '(' + ie + ')');
        }
        catch (SIRequestException sire)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "loadPidMapTable: Unable to retrieve SI", sire);
            }
            throw new IllegalArgumentException("Error retrieving SI for service " + m_service.getLocator() + '(' + sire + ')');
        }
        catch (InvalidLocatorException ile)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "loadPidMapTable: Unable to create transport-specific OcapLocator", ile);
            }
            throw new IllegalArgumentException("Error getting transport-specific Locator for Service "
                    + m_service.getLocator() + '(' + ile + ')');
        } // END try
    } // END loadPidMapTable

    /**
     * Load a PidMapTable with components from the designated Service. If the
     * service contains no components (e.g. it's an analog service), the
     * PidMapTable
     * 
     * @param service
     * @return
     */
    PidMapTable loadBlankPidMapTable()
    {
        // Create a default PidMapTable with 3 entries (one video, two audio
        // (primary, secondary))
        PidMapTable pidMapTable = new PidMapTable(3 + 1); // 3 elementary
                                                          // streams, 1 PCR

        PidMapEntry pidEntryVideo = new PidMapEntry(MediaStreamType.VIDEO, PidMapEntry.NTSC_VIDEO,
                PidMapEntry.PID_UNKNOWN, PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, PidMapEntry.PID_UNKNOWN, null);
        pidMapTable.addEntryAtIndex(0, pidEntryVideo);
        PidMapEntry pidEntryAudio1 = new PidMapEntry(MediaStreamType.AUDIO, PidMapEntry.NTSC_PRIMARY_AUDIO,
                PidMapEntry.PID_UNKNOWN, PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, PidMapEntry.PID_UNKNOWN, null);
        pidMapTable.addEntryAtIndex(1, pidEntryAudio1);
        PidMapEntry pidEntryAudio2 = new PidMapEntry(MediaStreamType.AUDIO, PidMapEntry.NTSC_SECONDARY_AUDIO,
                PidMapEntry.PID_UNKNOWN, PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, PidMapEntry.PID_UNKNOWN, null);
        pidMapTable.addEntryAtIndex(2, pidEntryAudio2);

        // Add entry for PCR (create one with default values)
        PidMapEntry pidEntryPCR = new PidMapEntry();
        pidEntryPCR.setStreamType(MediaStreamType.PCR);
        pidMapTable.addEntryAtIndex(3, pidEntryPCR);

        return pidMapTable;
    } // END loadBlankPidMapTable

    void stopBuffering(final int reason) throws IllegalArgumentException
    {
        // Assert: Caller holds lock

        // Since the TSB list is chronological, the active TSB is
        // always at the end of the list
        final TimeShiftBufferImpl activeTSB = getBufferingTimeShiftBuffer();

        // Clear the service-CCI association table
        s_tsm.getPODManager().removeCCIForService(this);
        
        // we are not returning the tsb to the pool since the tsb is needed to
        // support the duration contract
        if (activeTSB != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "stopBuffering - " + activeTSB);
            }

            activeTSB.stopBuffering(m_timeBase, reason);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "stopBuffering called but no active TSB");
            }
        }
    } // END stopBuffering()

    /**
     * Calculate the amount of overlapping content, in seconds, currently
     * overlapping with the content stored in the TimeShiftWindow
     * 
     * @param spanStart
     *            The start of the span, in milliseconds
     * @param spanEnd
     *            The end of the span, in milliseconds
     * 
     * @return The amount of content overlapping in the TSW, in milliseconds.
     */
    public long timeSpanOverlap(final long spanStart, final long spanEnd)
    {
        synchronized (this)
        {
            final TimeShiftBufferImpl tsb = getBufferingTimeShiftBuffer();
    
            if (tsb == null)
            {
                return 0;
            }
    
            // Note: This will have to change when multi-tsb support is added
            // All units here are in milliseconds
            final long tswStart = tsb.getContentStartTimeInSystemTime();
            final long tswEnd = tsb.getContentEndTimeInSystemTime();
    
            long overlap;
    
            if ((spanEnd < tswStart) || (spanStart > tswEnd))
            { // No overlap at all
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "timeSpanOverlap: no overlap");
                }
                return 0;
            }
    
            // Assert: There is some overlap
    
            // There are 4 cases of non-zero overlap
            if (spanStart < tswStart)
            {
                if (spanEnd < tswEnd)
                { // Span overlaps front end of TSW
                    overlap = spanEnd - tswStart;
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "timeSpanOverlap: Span overlaps front of TSW by " + overlap);
                    }
                }
                else
                { // TSW is fully covered by the span
                    overlap = tswEnd - tswStart;
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "timeSpanOverlap: TSW contained within span - overlap is" + overlap);
                    }
                }
            }
            else
            {
                if (spanEnd < tswEnd)
                { // Span is fully covered by the TSW
                    overlap = spanEnd - spanStart;
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "timeSpanOverlap: Span contained within TSW - overlap is " + overlap);
                    }
                }
                else
                { // Span overlaps back end of the TSW
                    overlap = tswEnd - spanStart;
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "timeSpanOverlap: Span overlaps end of TSW by " + overlap);
                    }
                }
            } // END else/if (spanStart < tswStart)
    
            return overlap;
        } // END synchronized (this)
    } // END timeSpanOverlap()

    int getBufferingTSBHandle()
    {
        // Assert: Caller holds lock

        TimeShiftBufferImpl tsb = getBufferingTimeShiftBuffer();

        if (tsb == null)
        {
            throw new IllegalStateException("TimeShiftWindow does not have a buffering TSB");
        }

        return tsb.nativeTSBHandle;
    } // END getBufferingTSBHandle()

    TimeShiftBufferImpl getNewestTimeShiftBuffer()
    { // The newest TSB will be the last one in the list
        if (m_timeShifts.size() > 0)
        {
            return (TimeShiftBufferImpl) (m_timeShifts.get(m_timeShifts.size() - 1));
        }
        else
        {
            return null;
        }
    } // END getNewestTimeShiftBuffer()

    TimeShiftBufferImpl getNextToLastTimeShiftBuffer()
    { // The newest TSB will be the last one in the list
        if (m_timeShifts.size() >= 2)
        {
            return (TimeShiftBufferImpl) (m_timeShifts.get(m_timeShifts.size() - 2));
        }
        else
        {
            return null;
        }
    } // END getNewestTimeShiftBuffer()

    TimeShiftBufferImpl getBufferingTimeShiftBuffer()
    {
        synchronized (this)
        {
            if ( m_curState == TimeShiftManager.TSWSTATE_BUFFERING
                    || m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING
                    || m_curState == TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN
                    || m_curState == TimeShiftManager.TSWSTATE_INTSHUTDOWN )
            {
                TimeShiftBufferImpl tsb = getNewestTimeShiftBuffer();
                if (tsb != null && tsb.isBuffering())
                {
                    return tsb;
                }
            }

            return null;
        }
    } // END getBufferingTimeShiftBuffer()

    void resizeBufferingTSB(long duration) throws IllegalArgumentException
    {
        // Assert: Caller holds lock
        // Note: TSW may be in any number of states

        final TimeShiftBufferImpl bufferingTSB = getBufferingTimeShiftBuffer();

        if (bufferingTSB == null)
        {
            throw new IllegalArgumentException("No buffering TSB found!");
        }

        int mpeError = TimeShiftBufferImpl.MPE_DVR_ERR_NOERR;

        mpeError = bufferingTSB.changeSize(duration);
        // Note: will throw IllegalArgumentException for no disk space

        if (mpeError == TimeShiftBufferImpl.MPE_DVR_ERR_UNSUPPORTED)
        { // The TSB cannot be resized while it's buffering
            if ( (m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
                 || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING) )
            {
                // Initiate TSB shutdown/restart cycle
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "resizeBufferingTSB: TimeShiftBuffer resize failed (not supported).");
                }
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "resizeBufferingTSB: Initiating TSB restart cycle.");
                }

                setStateAndNotify(TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN,
                        (duration > bufferingTSB.bufferSize) ? TimeShiftManager.TSWREASON_SIZEINCREASE
                                : TimeShiftManager.TSWREASON_SIZEREDUCTION );
                // Note: Buffering will be stopped when the last client detaches
                // their buffering session and restarted when clients start
                // reattaching after returning to the TUNED state.
            }
            else
            {
                throw new IllegalArgumentException("Could not resize non-buffering TSB");
            }
        }

        // Assert: TSW may be in any number of states since there isn't a
        // restriction on the start state
    } // END resizeBufferingTSB()

    void adjustBufferingTSBDurations(long desiredDuration, long maxDuration) 
        throws IllegalArgumentException
    {
        // Assert: Caller holds lock
        // Note: TSW may be in any number of states

        final TimeShiftBufferImpl bufferingTSB = getBufferingTimeShiftBuffer();

        if (bufferingTSB == null)
        {
            throw new IllegalArgumentException("No buffering TSB found!");
        }

        int mpeError = TimeShiftBufferImpl.MPE_DVR_ERR_NOERR;

        mpeError = bufferingTSB.changeBufferingDurations( desiredDuration, 
                                                          maxDuration );

        if (mpeError == TimeShiftBufferImpl.MPE_DVR_ERR_UNSUPPORTED)
        { // The TSB cannot be resized while it's buffering
            if (m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
            {
                // Initiate TSB shutdown/restart cycle
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "adjustBufferingTSBDurations: TimeShiftBuffer resize failed (not supported).");
                    log.debug(m_logPrefix + "adjustBufferingTSBDurations: Initiating TSB restart cycle.");
                }

                setStateAndNotify( TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN,
                                   TimeShiftManager.TSWREASON_SIZEINCREASE );
                // Note: Buffering will be stopped when the last client detaches
                // their buffering session and restarted when clients start
                // reattaching after returning to the TUNED state.
            }
            else
            {
                throw new IllegalArgumentException("Could not adjust non-buffering TSB");
            }
        }

        // Assert: TSW may be in any number of states since there isn't a
        // restriction on the start state
    } // END adjustBufferingTSBDurations()
    
    void updateNIResourceUsages()
    {
        //
        // Calculate what the list of resource usages should be
        //
        Vector newRUList = new Vector();
        boolean foundTimeShiftRU = false;
        boolean foundSCRU = false;
        AppID ruAppID = null;
        int ruPriority = 0;

        // Walk the list of TSWCs
        for (final Iterator tswce = m_clients.iterator(); tswce.hasNext();)
        {
            TimeShiftWindowClientImpl client = (TimeShiftWindowClientImpl) tswce.next();

            if ( ((client.m_constraints.uses & usesThatRequireTuner) != 0)
                 || ((client.m_constraints.reservations & usesThatRequireTuner) != 0 ) )
            {
                // Assert: client.m_resourceUsage != null
                newRUList.add(client.m_resourceUsage);

                ruAppID = client.m_resourceUsage.getAppID();
                ruPriority = ((ResourceUsageImpl)client.m_resourceUsage).getPriority();

                if (client.m_resourceUsage instanceof TimeShiftBufferResourceUsage)
                {
                    foundTimeShiftRU = true;
                }
                if (client.m_resourceUsage instanceof ServiceContextResourceUsage)
                {
                    foundSCRU = true;
                }
            }
        } // END loop through TSW clients

        // If (we're buffering, there's no TSRU in the TSW clients,
        // and we've found a ServiceContextResourceUsage,)
        // or the list is empty (can this happen?), add TSRU to the newRUList
        if ( (!foundTimeShiftRU && foundSCRU)
             && ( (m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
                     || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING) 
                  || (m_curState == TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN) 
                  || newRUList.isEmpty() ) )
        {
            if (ruAppID == null)
            {
                ruAppID = new AppID(0, 0);
            }

            final TimeShiftBufferResourceUsage tsbru = new TimeShiftBufferResourceUsageImpl(m_service, ruAppID, ruPriority);
            newRUList.add(tsbru);
        }

        //
        // Get the list of RUs from the NIC (nicrus)
        //

        Vector curRUList;

        try
        {
            curRUList = m_nic.getResourceUsage();
            if (curRUList == null)
            {
                // no resource usages to update
                return;
            }
        }
        catch (NetworkInterfaceException nie)
        {
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "updateNIResourceUsages: Current NI resource usages: " + curRUList);
        }

        //
        // Now reconcile the lists
        //

        boolean changedRUList = false;
        
        // Remove defunct ResourceUsages
        for (final Enumeration currus = curRUList.elements(); currus.hasMoreElements();)
        {
            ResourceUsageImpl rui = (ResourceUsageImpl) currus.nextElement();

            if (!newRUList.contains(rui))
            { // rui is not part of The New Order - toss it
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "updateNIResourceUsages: Removing ResourceUsage " + rui
                                + " from nic reservation");
                    }
                    m_nic.releaseResourceUsage(rui);
                    changedRUList = true;
                }
                catch (NetworkInterfaceException nie)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "updateNIResourceUsages: Could not remove ResourceUsage " + rui
                                + " from nic (" + nie + ')');
                    }
                }
            } // END if (RU rui should be removed from the reservation)
        } // END loop through current ResourceUsages

        // Add unaccounted ResourceUsages

        for (final Enumeration newrus = newRUList.elements(); newrus.hasMoreElements();)
        {
            ResourceUsageImpl rui = (ResourceUsageImpl) newrus.nextElement();

            if (!curRUList.contains(rui))
            {
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "updateNIResourceUsages: Adding ResourceUsage " + rui
                                + " to nic reservation");
                    }
                    m_nic.addResourceUsage(rui);
                    changedRUList = true;
                }
                catch (NetworkInterfaceException nie)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "updateNIResourceUsages: Could not add ResourceUsage " + rui
                                + " to nic (" + nie + ')');
                    }
                }
            } // END if (RU rui needs to be added to the reservation)
        } // END loop through updated ResourceUsages

        // Notify the monitor listener that a client has been added
        final TimeShiftWindowMonitorListener tswm = m_tswm;
        final TimeShiftWindow tsw = this;
        if (changedRUList && (tswm != null))
        {
            final CallerContext ctx = TimeShiftManagerImpl.m_ccm.getSystemContext();

            ctx.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    tswm.tswNIUsageChange(tsw);
                }
            });
        }
    } // END updateNIResourceUsages()

    /**
     * Will stop descrambling, remove the NetworkInterface callbacks, and 
     *  unregister for component change notifications.
     *  
     * @param doRelease If true. call release() on the NIC
     */
    void releaseTuner(boolean doRelease)
    {
        // Unregister the NI listener

        // Remove the NetworkInterface listener
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "releaseTuner: UNREGISTERING NetworkInterfaceCallback");
        }

        stopDescrambling();
        
        if (m_eni == null)
        {
            // ignore redundant call
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "ignoring redundant releaseTuner call");
            }
            return;
        }

        m_eni.removeNetworkInterfaceCallback(TimeShiftWindow.this);

        unregisterForComponentChanges();

        // Release the NetworkInterface, if so commanded
        if (doRelease)
        {
            try
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "releaseTuner: RELEASING NetworkInterface (" + m_eni.getHandle() + ')');
                }
                m_nic.release();
            }
            catch (NetworkInterfaceException nie)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(m_logPrefix + "releaseTuner: NetworkInterface threw exception on release: " + nie);
                }
            }
        } // END if (doRelease)

        m_eni = null;

        // Consider the tuner released
        m_niReleasedCondition.setTrue();
    } // END releaseTuner()

    void releaseUnusedTSBs()
    {
        for (final Iterator tsbe = m_timeShifts.iterator(); tsbe.hasNext();)
        {
            TimeShiftBufferImpl tsb = (TimeShiftBufferImpl) tsbe.next();

            if (!(tsb.buffering || tsb.presenting || tsb.copying))
            {
                tsbe.remove();

                s_tsm.returnTSB(tsb);
            }
        }
    } // END releaseUnusedTSBs()

    /**
     * This method will release unused resources (tuner, buffering, TSB) if they
     * are not currently needed to support attached clients.
     */
    void releaseUnusedResources()
    {
        synchronized (this)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "releaseUnusedResources: current state: "
                        + TimeShiftManager.stateString[m_curState]);
            }
    
            int newState = TimeShiftManager.TSWSTATE_INVALID;
    
            // All latest client 'uses' are contained in m_constraints.uses
    
            // If current state is buffering, but client 'uses' indicate we don't
            // need to be buffering then stop buffering.
            if (((m_constraints.reservations & TimeShiftManager.TSWUSE_BUFFERING) == 0)
                    && ((m_constraints.uses & TimeShiftManager.TSWUSE_BUFFERING) == 0)
                    && ( (m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
                         || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING) ) )
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "releaseUnusedResources: No buffering uses remaining. Stopping buffering.");
                }
    
                stopBuffering(TimeShiftManager.TSWREASON_INTLOST);
                stopDescrambling();
    
                if (ableToBuffer())
                {
                    newState = TimeShiftManager.TSWSTATE_READY_TO_BUFFER;
                }
                else
                {
                    newState = TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER;
                }
            }
    
            // If current state indicates we need tuner (any of the states
            // identified below indicate the tuner is needed),
            // but client uses indicate we don't need the tuner, then go ahead and
            // release it.
            if (((m_constraints.reservations & usesThatRequireTuner) == 0)
                    && ((m_constraints.uses & usesThatRequireTuner) == 0)
                    && ( (m_curState == TimeShiftManager.TSWSTATE_TUNE_PENDING)
                            || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING)
                            || (m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
                            || (m_curState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER)
                            || (m_curState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER) 
                            || (m_curState == TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN) ) )
            { // There's now no use that requires the tuner
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "releaseUnusedResources: No tuner uses remaining. Releasing tuner.");
                }

                releaseTuner(true);
    
                newState = TimeShiftManager.TSWSTATE_IDLE;
            }
    
            // TODO: If any of the time shift buffers (m_timeShifts) in the 'window'
            // is presenting then the state should correctly represent 'presenting'
            // state
            // final TimeShiftBufferImpl activeTSB = getBufferingTimeShiftBuffer();
    
            // If current state indicates we need TSB (ex: for presenting),
            // but our uses indicate we don't need the TSB then release the TSB.
            if (((m_constraints.reservations & usesThatRequireTSB) == 0)
                    && ((m_constraints.uses & usesThatRequireTSB) == 0))
            {
                if (!m_timeShifts.isEmpty())
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(m_logPrefix + "releaseUnusedResources: No TSB uses remaining. Releasing unused TSBs.");
                    }
                    releaseUnusedTSBs();
                }
            }
    
            if (newState != TimeShiftManager.TSWSTATE_INVALID)
            { // We did something to require state change - signal it
                setStateAndNotify(newState, TimeShiftManager.TSWREASON_NOREASON);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "releaseUnusedResources: TSW still in use - state unchanged");
                }
            }
        } // END synchronized (this)
    } // END releaseUnusedResources()

    synchronized int getNumberOfClients()
    {
        return m_clients.size();
    } // END getNumberOfClients()

    boolean willGiveUpNetworkInterface()
    {
        // Assert: Caller has lock
        return ((((m_constraints.reservations & usesThatRequireTuner) == 0) && ((m_constraints.uses & usesThatRequireTuner) == 0)));
    }

    /*****************************************************************************/
    /*****************************************************************************/
    /* Implementation of org.davic.net.tuning.NetworkInterfaceCallback interface */
    /*****************************************************************************/
    /*****************************************************************************/

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback#
     * notifyTunePending
     * (org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface)
     */
    public void notifyTunePending(ExtendedNetworkInterface eni, Object tuneInstance)
    {
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "notifyTunePending (NI " + eni
                       + ", ti 0x" + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode()) );
        }

        if (tuneInstance != m_tuneCookie)
        { // Note: We can do this check without the lock since m_tuneCookie is final
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix
                        + "notifyTuneComplete: Received notifyTuneComplete() for different tune instance (0x"
                        + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode())
                        + " != 0x" + Integer.toHexString((m_tuneCookie==null) ? 0 : m_tuneCookie.hashCode())
                        + ") - IGNORING");
            }
            return;
        }
        
        synchronized (this)
        {
            if (m_curState == TimeShiftManager.TSWSTATE_RESERVE_PENDING)
            {
                // The tuneInstance has checked out. But the indication has won the race
                //  with the reserveNIAndTune() logic. Move the state forward now so we ensure
                //  that we're in TUNE_PENDING if/when we receive notifyTuneComplete
                setStateAndNotify(TimeShiftManager.TSWSTATE_TUNE_PENDING,
                                  TimeShiftManager.TSWREASON_NOREASON );
            }
        }
            
        return;
    } // END notifyTunePending()

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback#
     * notifyTuneComplete
     * (org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface, boolean)
     */
    public void notifyTuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success, boolean synced)
    {
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "notifyTuneComplete (NI " + ni
            		   + ",ti 0x" + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode())
                       + ",tune " + (success ? "SUCCESSFUL" : "FAILED")
                       + ",sync " + (synced ? "LOCKED" : "UNLOCKED") 
                       + ",loc " + ni.getLocator() + ')' );
        }

        if (tuneInstance != m_tuneCookie)
        { // Note: We can do this check without the lock since m_tuneCookie is final
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix
                        + "notifyTuneComplete: Received notifyTuneComplete() for different tune instance (0x"
                        + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode())
                        + " != 0x" + Integer.toHexString((m_tuneCookie==null) ? 0 : m_tuneCookie.hashCode())
                        + ") - IGNORING");
            }
            return;
        }

        synchronized (this)
        {
            if (m_curState != TimeShiftManager.TSWSTATE_TUNE_PENDING)
            {
                // this may happen if notifications are in-flight prior to
                // releasing the tuner - ignore
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "IGNORING notifyTuneComplete: Received notifyTuneComplete() in state "
                            + TimeShiftManager.stateString[m_curState]);
                }
                return;
            }

            // Assert: We're TUNE_PENDING with a valid tune completion

            ExtendedNetworkInterface reservedNI = (ExtendedNetworkInterface) m_nic.getNetworkInterface();

            if (reservedNI == null)
            { // Since we're still in TUNE_PENDING (see check above), this
                // would only happen if we took so long processing release()
                // that the NI was taken away before we finished processing
                // release - and all before we processed this indication...
                if (log.isWarnEnabled())
                {
                    log.warn(m_logPrefix + "notifyTuneComplete: Found reserved NI is null in "
                            + TimeShiftManager.stateString[m_curState]);
                }

                // We're not going to consider this a valid tune. We should
                // clean up in ResourceClient.release() (since we must be stuck
                // there)
                return;
            }

            if (ni != m_eni)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(m_logPrefix + "notifyTuneComplete: NI tuner mismatch (found " + ni + ", expected " + m_eni
                            + ") - IGNORING");
                }

                // We're not going to consider this a valid tune. We should
                // clean up in ResourceClient.release() (since we must be stuck
                // there)
                return;
            }

            // Assert: This tune notification is not stale

            if (success)
            {
                if (synced)
                { // SUCCESSFUL TUNE/SYNCED
                    final CallerContext ctx = TimeShiftManagerImpl.m_ccm.getSystemContext();

                    // Try to get the PIDMapTable loaded and establish the state
                    // We'll do this async since the SI retrieval can be
                    // blocking.
                    ctx.runInContextAsync(new Runnable()
                    {
                        public void run()
                        {
                            int newState, reason;
                            List notifications = null;

                            synchronized (TimeShiftWindow.this)
                            {
                                // Need to make sure things haven't changed

                                if (m_curState != TimeShiftManager.TSWSTATE_TUNE_PENDING)
                                { // We're behind the curve. Bail out
                                    return;
                                }

                                try
                                {
                                    // The loading of the PIDMapTable involves
                                    // SI acquisition
                                    // so we're doing this on a different thread
                                    // than the NI's
                                    // sync notify thread
                                    m_curPidMap = loadPidMapTable(m_service);
                                    newState = TimeShiftManager.TSWSTATE_READY_TO_BUFFER;
                                    reason = TimeShiftManager.TSWREASON_TUNESUCCESS;
                                }
                                catch (IllegalArgumentException iae)
                                {
                                    if (log.isDebugEnabled())
                                    {
                                        log.debug(m_logPrefix
                                                + "Could not create a PID map table from initial components ("
                                                + iae.toString() + ')');
                                    }
                                    m_curPidMap = null; // Signifies no
                                                        // buffer-able
                                                        // components
                                    newState = TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER;
                                    reason = TimeShiftManager.TSWREASON_NOCOMPONENTS;
                                }

                                // Register for component changes after our
                                // initial SI acquisition
                                registerForComponentChanges();
                                
                                notifications = 
                                    buildStateChangeNotifications(newState, reason);
                            } // END synchronized (TimeShiftWindow.this)

                            // Each runnable has the potential to block - run each on its own thread
                            final CallerContext ctx = TimeShiftManagerImpl.m_ccm.getSystemContext();
                            if (notifications != null)
                            {
                                for (Iterator it=notifications.iterator(); it.hasNext();)
                                {
                                    ctx.runInContextAsync((Runnable)it.next());
                                }
                            }
                        }
                    }); // END ctx.runInContextAsync()
                } // END if (synced)
                else
                { // SUCCESSFUL TUNE/UNSYNCED
                    setStateAndNotify(TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER,
                            TimeShiftManager.TSWREASON_SYNCLOST );
                }
            } // END if (success)
            else
            { // FAILED TUNE
                setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_TUNEFAILURE );

                releaseTuner(true);
            }
        } // END synchronized (this)

        return;
    } // END notifyTuneComplete()

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback#
     * notifyRetunePending
     * (org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface)
     */
    public void notifyRetunePending(ExtendedNetworkInterface eni, Object tuneInstance)
    {
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "notifyRetunePending (NI " + eni
                       + ",ti 0x" + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode()) 
                       + ')' );
        }

        if (tuneInstance != m_tuneCookie)
        { // Note: We can do this check without the lock since m_tuneCookie is final
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix
                        + "notifyRetunePending: Received notifyRetunePending() for different tune instance (0x"
                        + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode())
                        + " != 0x" + Integer.toHexString((m_tuneCookie==null) ? 0 : m_tuneCookie.hashCode())
                        + ") - IGNORING");
            }
            return;
        }
        
        synchronized (this)
        {
            // The tuneInstance will be new - so we can only check the tuner #
            if (eni != m_eni)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "notifyRetunePending: Received notifyRetunePending() for different tuner ("
                            + eni + " != " + m_eni + ") - IGNORING");
                }
                return;
            }

            // Check to see if there are clients that are using the tuner
            if (willGiveUpNetworkInterface())
            { // The NI is not needed/requested by any clients - no coordinated
              // shutdown required
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix
                            + "NetworkInterfaceCallback.notifyRetunePending: TSW not in use - releasing resources");
                }

                // TODO: Can we just call requestRelease() here?

                if ( (m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
                     || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING) )
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix
                                + "NetworkInterfaceCallback.notifyRetunePending: TSW not in use - stopping buffering");
                    }

                    stopBuffering(TimeShiftManager.TSWREASON_SERVICEREMAP);
                }

                if ((m_curState == TimeShiftManager.TSWSTATE_TUNE_PENDING)
                        || (m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
                        || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING)
                        || (m_curState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER)
                        || (m_curState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix
                                + "NetworkInterfaceCallback.notifyRetunePending: TSW not in use - releasing tuner");
                    }

                    releaseTuner(true); // Want to do an actual release() here
                }

                // Go straight to the NOT_TUNED state since there are
                // not any uses that require the tuner
                setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_SERVICEREMAP);
                // And we're OUT
                return;
            } // END if (willGiveUpNetworkInterface())
            
            // Assert: Still have clients which are attach for or reserved tuner uses
          
            if ((m_constraints.uses & usesThatRequireTuner) == 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "NetworkInterfaceCallback.notifyRetunePending: No clients attached to tuner");
                }
                
                // No need to wait for clients to detach - let the switch start... 
                setStateAndNotify(TimeShiftManager.TSWSTATE_TUNE_PENDING, TimeShiftManager.TSWREASON_SERVICEREMAP);
                
                return;
            }
            
            // Assert: There are still clients attached for a tuner use

            //
            // Otherwise, start a coordinated NI shutdown
            //
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "NetworkInterfaceCallback.notifyRetunePending: Starting coordinated shutdown");
            }

            // We go to INTSHUTDOWN regardless of what state we're in
            setStateAndNotify(TimeShiftManager.TSWSTATE_INTSHUTDOWN, TimeShiftManager.TSWREASON_SERVICEREMAP);

            // As clients detach, as they are contractually required to do,
            // we'll shutdown the buffering and the condition will become true

            m_niUsesRemovedCondition.setFalse();
        } // END synchronized (this)

        //
        // Wait for clients to detach
        //

        // Note: We have to wait outside the sync block or no one will
        // be able to detach
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix
                        + "NetworkInterfaceCallback.notifyRetunePending: Waiting for shutdown to complete...");
            }

            // TODO: Should we do this with a timeout?
            m_niUsesRemovedCondition.waitUntilTrue();

            // Assert: m_curState is now TUNE_PENDING
        }
        catch (InterruptedException ie)
        {
            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix + "NetworkInterfaceCallback.notifyRetunePending: Wait interrupted (" + ie + ')');
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix
                    + "NetworkInterfaceCallback.notifyRetunePending: Coordinated shutdown for notifyRetunePending() complete.");
        }

        // Now, we expect the NI is going to be moved to a different TS (or
        // maybe the same?
        // and we'll learn about the process completing on
        // notifyRetuneComplete()
    } // END notifyRetunePending()

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback#
     * notifyRetuneComplete
     * (org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface, boolean)
     */
    public void notifyRetuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success, boolean synced)
    {
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "notifyRetuneComplete (NI " + ni
                       + ",ti 0x" + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode())
                       + ",tune " + (success ? "SUCCESSFUL" : "FAILED")
                       + ",sync " + (synced ? "LOCKED" : "UNLOCKED") 
                       + ",loc " + ni.getLocator() + ')' );
        }

        if (tuneInstance != m_tuneCookie)
        { // Note: We can do this check without the lock since m_tuneCookie is final
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix
                        + "notifyRetuneComplete: Received notifyRetuneComplete() for different tune instance (0x"
                        + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode())
                        + " != 0x" + Integer.toHexString((m_tuneCookie==null) ? 0 : m_tuneCookie.hashCode())
                        + ") - IGNORING");
            }
            return;
        }

        synchronized (this)
        {
            if (m_curState != TimeShiftManager.TSWSTATE_TUNE_PENDING)
            {
                // this may happen if notifications are in-flight prior to
                // releasing the tuner - ignore
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "IGNORING notifyRetuneComplete: Received notifyRetuneComplete() in state "
                            + TimeShiftManager.stateString[m_curState]);
                }

                return;
            }

            if (success)
            {
                if (synced)
                { // SUCCESSFUL TUNE/SYNCED
                    // Assert: this held by caller
                    final CallerContext ctx = TimeShiftManagerImpl.m_ccm.getSystemContext();

                    // Try to get the PIDMapTable loaded and establish the state
                    // We'll do this async since the SI retrieval can be
                    // blocking.
                    ctx.runInContextAsync(new Runnable()
                    {
                        public void run()
                        {
                            int newState, reason;
                            List notifications = null;

                            synchronized (TimeShiftWindow.this)
                            {
                                try
                                {
                                    // The loading of the PIDMapTable involves
                                    // SI acquisition so we're doing this on a different thread
                                    // than the NI's sync notify thread
                                    if (log.isInfoEnabled())
                                    {
                                        log.info(m_logPrefix + "m_service: " + m_service );
                                    }
                                    m_curPidMap = loadPidMapTable(m_service);
                                    newState = TimeShiftManager.TSWSTATE_READY_TO_BUFFER;
                                    reason = TimeShiftManager.TSWREASON_SERVICEREMAP;
                                }
                                catch (IllegalArgumentException iae)
                                {
                                    if (log.isDebugEnabled())
                                    {
                                        log.debug(m_logPrefix
                                                + "Could not create a PID map table from initial components ("
                                                + iae.toString() + ')');
                                    }
                                    m_curPidMap = null; // Signifies no buffer-able components
                                    newState = TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER;
                                    reason = TimeShiftManager.TSWREASON_NOCOMPONENTS;
                                }

                                // Create the notifications while holding the monitor
                                notifications 
                                    = buildStateChangeNotifications(newState, reason);
                            } // END synchronized (TimeShiftWindow.this)

                            // Each runnable has the potential to block - run each on its own thread
                            final CallerContext ctx = TimeShiftManagerImpl.m_ccm.getSystemContext();
                            if (notifications != null)
                            {
                                for (Iterator it=notifications.iterator(); it.hasNext();)
                                {
                                    ctx.runInContextAsync((Runnable)it.next());
                                }
                            }
                        }
                    });
                } // END if (synced)
                else
                {
                    setStateAndNotify( TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER,
                                       TimeShiftManager.TSWREASON_SYNCLOST );
                }
            }
            else
            // Retune failure
            {
                setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_TUNEFAILURE);
            }
        } // END synchronized (this)
    } // END notifyRetuneComplete()

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback#notifyUntuned
     * (org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface)
     */
    public void notifyUntuned(ExtendedNetworkInterface ni, Object tuneInstance)
    {
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "notifyUntuned (NI " + ni
                       + ",ti 0x" + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode()) );
        }

        if (tuneInstance != m_tuneCookie)
        { // Note: We can do this check without the lock since m_tuneCookie is final
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix
                        + "notifyUntuned: Received notifyUntuned() for different tune instance (0x"
                        + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode())
                        + " != 0x" + Integer.toHexString((m_tuneCookie==null) ? 0 : m_tuneCookie.hashCode())
                        + ") - IGNORING");
            }
            return;
        }

        synchronized (this)
        {
            // Check to see if there are clients that require the tuner
            if (willGiveUpNetworkInterface())
            { // The NI is not needed/requested by any clients
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix
                            + "NetworkInterfaceCallback.notifyUntuned: TSW not in use - releasing resources");
                }

                if ( (m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
                     || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING) )
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix
                                + "NetworkInterfaceCallback.notifyUntuned: TSW not in use - stopping buffering");
                    }

                    stopBuffering(TimeShiftManager.TSWREASON_SERVICEREMAP);
                }
                
                if ((m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
                        || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING)
                        || (m_curState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER)
                        || (m_curState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix
                                + "NetworkInterfaceCallback.notifyUntuned: TSW not in use - releasing tuner");
                    }

                    releaseTuner(true); // Want to do an actual release() in
                                        // this case
                }

                // Go straight to the NOT_TUNED state since there are
                // not any uses that require the tuner
                setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_SERVICEVANISHED);
                // And we're out
                return;
            } // END if (willGiveUpNetworkInterface())
            
            // Assert: Still have clients which are attach for or reserved tuner uses

            if ((m_constraints.uses & usesThatRequireTuner) == 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "NetworkInterfaceCallback.notifyUntuned: No clients attached to tuner");
                }
                
                // No need to wait for clients to detach - let the switch start... 
                setStateAndNotify(TimeShiftManager.TSWSTATE_TUNE_PENDING, TimeShiftManager.TSWREASON_SERVICEREMAP);
                
                return;
            }

            // Assert: There's still a use that requires the tuner

            //
            // start a coordinated NI shutdown
            //
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "NetworkInterfaceCallback.notifyUntuned: Starting coordinated shutdown");
            }

            // We go to INTSHUTDOWN regardless of what state we're in
            setStateAndNotify(TimeShiftManager.TSWSTATE_INTSHUTDOWN, TimeShiftManager.TSWREASON_SERVICEVANISHED);

            // As clients detach, as they are contractually required to do,
            // we'll shutdown the buffering and the condition will become true

            m_niUsesRemovedCondition.setFalse();
        } // END synchronized (this)

        //
        // Wait for clients to detach
        //

        // Note: We have to wait outside the sync block or no one will
        // be able to detach
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "NetworkInterfaceCallback.notifyUntuned: Waiting for shutdown to complete...");
            }

            m_niUsesRemovedCondition.waitUntilTrue();
        }
        catch (InterruptedException ie)
        {
            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix + "NetworkInterfaceCallback.notifyUntuned: Wait interrupted (" + ie + ')');
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix
                    + "NetworkInterfaceCallback.notifyUntuned: Coordinated shutdown for notifyUntuned() complete.");
        }
    } // END notifyUntuned()

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback#
     * notifySyncAcquired
     * (org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface,
     * java.lang.Object)
     */
    public void notifySyncAcquired(ExtendedNetworkInterface ni, Object tuneInstance)
    {
        // We'll only receive this after receiving a notifyTuned(true,false...),
        // notifyRetune(true,false,...), or notifySyncLost
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "notifySyncAcquired (NI " + ni
                       + ",ti 0x" + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode()) 
                       + ')' );
        }
        
        if (tuneInstance != m_tuneCookie)
        { // Note: We can do this check without the lock since m_tuneCookie is final
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix
                         + "notifySyncAcquired: Received notifySyncAcquired() for different tune instance (0x"
                         + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode())
                         + " != 0x" + Integer.toHexString((m_tuneCookie==null) ? 0 : m_tuneCookie.hashCode())
                         + ") - IGNORING");
            }
            return;
        }

        synchronized (this)
        {
        // Really only means something if we're in the NOT_READY_TO_BUFFER state
        if (m_curState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER)
        {
            // Sync is a necessary but not sufficient condition for being READY
            // Check the other conditions necessary for going to READY
            if (ableToBuffer())
            {
                setStateAndNotify( TimeShiftManager.TSWSTATE_READY_TO_BUFFER,
                                   TimeShiftManager.TSWREASON_SYNCACQUIRED );
                // Note: Clients may attach for buffering after getting this
                // indication
            }
            else if (m_curPidMap == null)
            {
                // We may have never acquired components - so do it now
                final CallerContext ctx = TimeShiftManagerImpl.m_ccm.getSystemContext();

                // Try to get the PIDMapTable loaded and establish the state
                // We'll do this async since the SI retrieval can be blocking.
                ctx.runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        synchronized (this)
                        {
                            try
                            {
                                if (m_curState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER)
                                { // We're behind the curve. Bail out
                                    return;
                                }

                                m_curPidMap = loadPidMapTable(m_service);

                                if (ableToBuffer())
                                {
                                    // Each runnable has the potential to block - run each on its own thread
                                    final List notifications =
                                        buildStateChangeNotifications( 
                                                TimeShiftManager.TSWSTATE_READY_TO_BUFFER,
                                                TimeShiftManager.TSWREASON_SYNCACQUIRED );
                                    if (notifications != null)
                                    {
                                        final CallerContext ctx = TimeShiftManagerImpl.m_ccm.getSystemContext();
                                        for (Iterator it=notifications.iterator(); it.hasNext();)
                                        {
                                            ctx.runInContextAsync((Runnable) (it.next()));
                                        }
                                    }
                                }
                            }
                            catch (IllegalArgumentException iae)
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug(m_logPrefix + "Could not create a PID map table (" + iae.toString() + ')');
                                }
                                m_curPidMap = null; // Signifies no buffer-able
                                                    // components
                            }
                        } // END synchronized (this)
                    }
                });
            }
        } // END if (m_curState == TimeShiftManager.TSWSTATE_TUNED_NOT_READY)
       }// End of synchronized(this)
    } // END notifySyncAcquired()

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback#notifySyncLost
     * (org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface,
     * java.lang.Object)
     */
    public void notifySyncLost(ExtendedNetworkInterface ni, Object tuneInstance)
    {
        // We'll only receive this after receiving a notifyTuned(true,false...),
        // notifyRetune(true,false,...), or notifySyncLost
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "notifySyncLost (NI " + ni
                       + ",ti 0x" + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode()) 
                       + ')' );
        }

        if (tuneInstance != m_tuneCookie)
        { // Note: We can do this check without the lock since m_tuneCookie is final
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix
                        + "notifySyncLost: Received notifySyncLost() for different tune instance (0x"
                        + Integer.toHexString((tuneInstance==null) ? 0 : tuneInstance.hashCode())
                        + " != 0x" + Integer.toHexString((m_tuneCookie==null) ? 0 : m_tuneCookie.hashCode())
                        + ") - IGNORING");
            }
            return;
        }

        // sync is a necessary conditions. We need to become NOT_READY

        synchronized (this)
        {
            switch (m_curState)
            {
                case TimeShiftManager.TSWSTATE_BUFFERING:
                case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                {
                    if ((m_constraints.uses & usesThatRequireTSB) == 0)
                    { // The TSB is not needed/requested by any clients
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix
                                    + "NetworkInterfaceCallback.notifySyncLost: BTSB not in use - stopping buffering");
                        }

                        // Note: We'll keep descrambling when we lose sync
                        stopBuffering(TimeShiftManager.TSWREASON_SYNCLOST);

                        // Go straight to the NOT_READY_TO_BUFFER state since there
                        // are not any uses that require the tuner
                        setStateAndNotify( TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER,
                                           TimeShiftManager.TSWREASON_SYNCLOST );
                        return;
                    } // END if (no uses require tuner)

                    // Assert: The BTSB is still requested by a client - so
                    // start a coordinated buffering shutdown
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix
                                + "NetworkInterfaceCallback.notifySyncLost: Starting coordinated buffering shutdown");
                    }

                    setStateAndNotify( TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN, 
                                       TimeShiftManager.TSWREASON_SYNCLOST );

                    // When all buffering clients are detached, we'll stop
                    // buffering and either transition
                    // to TSWSTATE_NOT_READY_TO_BUFFER or TSWSTATE_READY_TO_BUFFER -
                    // based on the
                    // sync status at that time
                    break;
                }
                case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
                {
                    // Go straight to the NOT_READY_TO_BUFFER state since we're not
                    // buffering
                    setStateAndNotify( TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER,
                                       TimeShiftManager.TSWREASON_SYNCLOST );
                    break;
                }
                default:
                { // We'll just ignore the sync lost in any other state - it's
                  // irrelevant
                    break;
                }
            } // END switch (m_curState)
        } // END synchronized (this)
    } // END notifySyncLost()

    /***************************************************************************/
    /***************************************************************************/
    /* Implementation of synchronous org.ocap.si.TableChangeListener interface */
    /***************************************************************************/
    /***************************************************************************/

    public void notifyChange(SIChangeEvent event)
    { // Notified for change in PAT/PMT associated with service
        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "notifyChange(" + event + ')'
                              + " - " + event.getChangeType()
                              + "/" + event.getSIElement() );
        }

        //
        // We can accomplish three things by processing a
        // TableChangeListener.notifyChange():
        //
        // 1) Change the buffering components when buffering is on-going (which
        //    may involve an on-the-fly PID change or a buffering shutdown/restart)
        // 2) Take the TSW from the BUFFERING to the NOT_READY state due to
        //    complete loss of buffer-able components
        // 3) Take the TSW from the NOT_READY to the BUFFERING state due to a
        //    re-acquisition of buffer-able components

        synchronized (this)
        {
            int changeReason;
            int targetState;
            PidMapTable newPidMap;

            // Calculate the new PID map. Can throw IllegalArgumentException if
            // there are insufficient components to buffer
            try
            {
                newPidMap = loadPidMapTable(m_service);
            }
            catch (IllegalArgumentException iae)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "notifyChange: Could not create a PID map table from updated components ("
                            + iae.toString() + ')');
                }
                newPidMap = null;
            }

            if (m_curPidMap != null && m_curPidMap.equals(newPidMap))
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "notifyChange: New PID tables are equivalent - nothing to do");
                }
                return;
            }

            switch (m_curState)
            {
                case TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER:
                {
                    m_curPidMap = newPidMap;
                    
                    // Check to see if this makes us READY
                    if (ableToBuffer())
                    {
                        setStateAndNotify( TimeShiftManager.TSWSTATE_READY_TO_BUFFER,
                                           TimeShiftManager.TSWREASON_COMPONENTSADDED );
                    }

                    // Nothing else to do in this state
                    return;
                } // END case TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER
                case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
                {
                    if (event.getChangeType() == SIChangeType.ADD)
                    {
                        // An ADD when already buffering represents a latent notification
                        return;
                    }

                    m_curPidMap = newPidMap;
                    
                    if (m_curPidMap == null)
                    {
                        setStateAndNotify( TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER,
                                           TimeShiftManager.TSWREASON_NOCOMPONENTS );
                    }
                    // Else we just have a new PID map now...

                    // Nothing else to do in this state
                    return;
                } // END case TimeShiftManager.TSWSTATE_READY_TO_BUFFER
                case TimeShiftManager.TSWSTATE_BUFFERING:
                case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                {
                    if (event.getChangeType() == SIChangeType.ADD)
                    {
                        // An ADD when already buffering represents a latent notification
                        return;
                    }
                    
                    m_curPidMap = newPidMap;
                    if (m_curPidMap != null)
                    {
                        // Attempt to change the timeshifted components
                        // on-the-fly
                        final TimeShiftBufferImpl bufferingTSB = getBufferingTimeShiftBuffer();

                        try
                        {
                            int mpeError = bufferingTSB.changeComponents(m_curPidMap, m_service);

                            if (mpeError == TimeShiftBufferImpl.MPE_DVR_ERR_NOERR)
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug(m_logPrefix
                                            + "notifyChange: Successfully completed mid-stream TSB PID change");
                                }
                                // No state change - keep on rolling...
                                stopDescrambling();
                                startDescrambling();

                                // Entities with a lower callback priority (e.g.
                                // RecordingImpl)
                                // will get their callback after we return, and
                                // react accordingly

                                // If we handled the SI change and PID change,
                                // we're done
                                return;
                            }

                            // We're going to shut down buffering due to lack of
                            // on-the-fly support
                            // (clients will re-attach and buffering will
                            // resume)
                            // Dropping through
                        }
                        catch (Exception e)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(m_logPrefix
                                        + "notifyChange: Caught exception while changing components  assuming change failure", e);
                            }

                            // We're going to shut down buffering due to lack of
                            // on-the-fly support
                            // (clients will re-attach and buffering will
                            // resume)
                            // Dropping through
                        }

                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "notifyChange: Failed mid-stream BTSB PID change");
                        }

                        targetState = TimeShiftManager.TSWSTATE_READY_TO_BUFFER;
                        changeReason = TimeShiftManager.TSWREASON_PIDCHANGE;
                        // State change signaling below
                    }
                    else
                    { // newPidMap == null
                        // We're going to shut down buffering due to lack of
                        // components

                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "notifyChange: No components to present");
                        }

                        // Dropping through
                        targetState = TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER;
                        changeReason = TimeShiftManager.TSWREASON_NOCOMPONENTS;
                        // State change signaling below
                    }

                    break;
                } // END case TimeShiftManager.TSWSTATE_BUFFERING
                default:
                { // Nothing to do in other states except to save off the new map
                    m_curPidMap = newPidMap;
                    return;
                }
            } // END switch (m_curState)

            // Check to see if there are clients that require the buffer
            if ((m_constraints.uses & usesThatRequireTSB) == 0)
            { // The TSB is not needed/requested by any clients
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "notifyChange: BTSB not in use - stopping buffering");
                }

                // Stop buffering and go straight to the READY_TO_BUFFER/NOT_READY
                // state since there are not any uses that require the tuner
                stopBuffering(changeReason);
                stopDescrambling();

                setStateAndNotify(targetState, changeReason);

                // Fully handled the case where we can immediately stop
                // buffering
                // So we're done here
                return;
            } // END if (no uses require tuner)

            // Assert: The BTSB is still requested by a client - so start a
            // coordinated buffering shutdown
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "notifyChange: Starting coordinated buffering shutdown");
            }

            setStateAndNotify(TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN, changeReason);

            // We need to now stay in the notifychange() function until the
            // change is fully processed
            // Set the condition variable and let go of the TSW monitor

            m_bufUsesRemovedCondition.setFalse();

            // As clients detach, as they are contractually required to do,
            // we'll shutdown the buffering and the condition will become true
        } // END synchronized (this)

        //
        // Wait for clients to detach for buffering
        //

        // Note: We have to wait outside the sync block or no one will
        // be able to detach
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "notifyChange: Waiting for buffering shutdown to complete...");
            }

            // TODO: Should we do this with a timeout?
            m_bufUsesRemovedCondition.waitUntilTrue();
        }
        catch (InterruptedException ie)
        {
            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix + "notifyChange: Wait interrupted (" + ie + ')');
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "notifyChange: Coordinated buffering shutdown complete.");
        }
    } // END notifyChange()

    /*************************************************************************/
    /*************************************************************************/
    /* Implementation of org.davic.resources.ResourceClient interface */
    /*************************************************************************/
    /*************************************************************************/

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#requestRelease(org.davic.resources
     * .ResourceProxy, java.lang.Object)
     * 
     * Called to request the voluntary release of the resource (the NI in this
     * case)
     */
    public boolean requestRelease(ResourceProxy proxy, Object requestData)
    {
        boolean released = false;

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "ResourceClient.requestRelease: proxy " + proxy + ", reqData " + requestData);
        }

        synchronized (this)
        {
            // Check to see if there are clients that require the tuner
            if (willGiveUpNetworkInterface())
            { // The NI is not needed/requested by any clients
                // Assert: Not in a BUF_/NI_SHUTDOWN state - since that would
                // imply a useThatRequiresTuner is still outstanding
                // Assert: We own the NI (why else would we be here?)

                if ( (m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
                     || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING) )
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "ResourceClient.requestRelease: TSW not in use - stopping buffering");
                    }

                    stopBuffering(TimeShiftManager.TSWREASON_INTLOST);
                }

                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "ResourceClient.requestRelease: TSW not in use - releasing tuner");
                }

                // Go straight to the NOT_TUNED state since there are
                // not any uses that require the tuner
                setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_INTLOST);

                // Do everything to release the tuner - except calling
                // nic.release()
                releaseTuner(false);

                // Release unused TimeShiftBuffers
                releaseUnusedTSBs();

                // Allow this TSW to be cleaned up when the timer goes off
                startDeathTimer();

                released = true;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "ResourceClient.requestRelease: NI still in use for/reserved for "
                            + TimeShiftManagerImpl.useString(m_constraints.uses) + '/'
                            + TimeShiftManagerImpl.useString(m_constraints.reservations));
                }
            }
        } // END synchronized (this)

        return released;
    } // END requestRelease()

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#release(org.davic.resources.ResourceProxy
     * )
     * 
     * Called to notify the ResourceClient that the resource (the NI in this
     * case) has been reassigned to another function.
     */
    public void release(ResourceProxy proxy)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "ResourceClient.release: proxy " + proxy);
        }

        synchronized (this)
        {
            // Check to see if there are clients that require the tuner
            if (willGiveUpNetworkInterface())
            { // The NI is not needed/requested by any clients
                // Note: If we're here, we should have given up the interface
                // in requestRelease(). But if a client drops off between
                // requestRelease() and release, we would be here.

                // Assert: Not in a BUF_/NI_SHUTDOWN state - since that would
                // imply a useThatRequiresTuner is still outstanding

                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "ResourceClient.release: TSW not in use - releasing resources");
                }

                // TODO: Can we just try calling requestRelease() here?

                if ( (m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
                     || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING) )
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "ResourceClient.release: TSW not in use - stopping buffering");
                    }

                    stopBuffering(TimeShiftManager.TSWREASON_INTLOST);
                }

                if ((m_curState == TimeShiftManager.TSWSTATE_BUFFERING)
                        || (m_curState == TimeShiftManager.TSWSTATE_BUFF_PENDING)
                        || (m_curState == TimeShiftManager.TSWSTATE_READY_TO_BUFFER)
                        || (m_curState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "ResourceClient.release: TSW not in use - releasing tuner");
                    }

                    releaseTuner(false);
                }

                // Go straight to the NOT_TUNED state since there are
                // not any uses that require the tuner
                setStateAndNotify(TimeShiftManager.TSWSTATE_IDLE, TimeShiftManager.TSWREASON_INTLOST);
                // And we're out
                return;
            } // END if (no uses require tuner)

            // Assert: The NI is still requested by a client

            //
            // Start a coordinated NI shutdown.
            //
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "ResourceClient.release: Starting coordinated shutdown");
            }

            // We go to INTSHUTDOWN regardless of what state we're currently in
            setStateAndNotify(TimeShiftManager.TSWSTATE_INTSHUTDOWN, TimeShiftManager.TSWREASON_INTLOST);

            // As clients detach, as they are contractually required to do,
            // we'll shutdown the buffering and release the NI

            // Assert: m_niReleasedCondition is false (set false when we
            // tune the NI and true when we release the NI)
        } // END synchronized (this)

        //
        // Wait for the NI to be released
        //

        // Note: We have to wait outside the sync block or no one will
        // be able to detach
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "ResourceClient.release: Waiting for shutdown to complete...");
            }

            // TODO: Should we do this with a timeout?
            m_niReleasedCondition.waitUntilTrue();
        }
        catch (InterruptedException ie)
        {
            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix + "ResourceClient.release: Wait interrupted (" + ie + ')');
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "ResourceClient.release: Coordinated shutdown for release() complete.");
        }
    } // END release()

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#notifyRelease(org.davic.resources.
     * ResourceProxy)
     * 
     * Called to notify the ResourceClient that the resource (the NI in this
     * case) was forcibly taken before the release() call returned.
     */
    public void notifyRelease(ResourceProxy proxy)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "ResourceClient.notifyRelease: proxy " + proxy);
        }

        // Assert: We're still in release()

        // Not sure what else we can do here. Stopping TSB buffering with
        // on-going
        // TSB conversions and allowing re-allocation of the tuner while
        // buffering
        // are violations of the MPE contract.

        try
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "ResourceClient.release: Waiting for shutdown to complete...");
            }

            // TODO: Should we do this with a timeout?
            m_niReleasedCondition.waitUntilTrue();
        }
        catch (InterruptedException ie)
        {
            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix + "ResourceClient.release: Condition wait interrupted (" + ie + ')');
            }
        }

        // Assert: state is TSWSTATE_NOTTUNED
    } // END notifyRelease()

    /*************************************************************************/
    /*************************************************************************/
    /* Implementation of org.cablelabs.impl.manager.ed.EDListener interface */
    /*************************************************************************/
    /*************************************************************************/

    /**
     * The TimeShiftWindow is an ED async notification listener
     */
    public void asyncEvent(int eventCode, int eventData1, int eventData2)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "asyncEvent: TSB event received (code 0x" 
                                  + Integer.toHexString(eventCode) 
                                  + ",ed1 0x" + Integer.toHexString(eventData1)
                                  + ",ed2 0x" + Integer.toHexString(eventData2) + ')');
        }
        
        switch (eventCode)
        {
            case TimeShiftBufferImpl.MPE_DVR_EVT_OUT_OF_SPACE:
            case TimeShiftBufferImpl.MPE_DVR_EVT_PLAYBACK_PID_CHANGE:
            case TimeShiftBufferImpl.MPE_DVR_EVT_CONVERSION_STOP:
            case TimeShiftBufferImpl.MPE_DVR_EVT_SESSION_CLOSED:
            {
                /**
                 * If we receive notice from the native layer that our native buffering
                 * session has been terminated, we need to change state accordingly.
                 */
                break;
            }
            case TimeShiftBufferImpl.MPE_DVR_EVT_CCI_UPDATE:
            {
                // Get the effective TimeBase time before acquiring the lock (which may block)
                final long effectiveTimeBaseTimeNs = this.getTimeBase().getNanoseconds();
                
                final CopyControlInfo cciChangedEvent 
                                      = new CopyControlInfo( 
                                             System.currentTimeMillis()*NANOS_PER_MILLI,
                                             (byte)(eventData2 & 0x000000FF) );

                // Update the service-CCI association table
                s_tsm.getPODManager().setCCIForService( this, m_serviceDetails, 
                                                             cciChangedEvent.getCCI() );
                
                synchronized (this)
                {
                    TimeShiftBufferImpl tsbi = this.getBufferingTimeShiftBuffer();
                    
                    // Unless there's some latency in receiving/processing this event,
                    //  and buffering was shut down in the interim, we wouldn't expect
                    //  to get this event when we're not buffering
                    if (tsbi != null)
                    {
                        long tsbOffsetNs = (getState() == TimeShiftManager.TSWSTATE_BUFFERING)
                                           ? (effectiveTimeBaseTimeNs - tsbi.getTimeBaseStartTime())
                                           : 0; // If we receive the CCI_UPDATE before SESSION_RECORDING
                        TimeAssociatedElement elem 
                            = new CopyControlInfo( tsbOffsetNs,
                                                   cciChangedEvent.getCCI() );

                        tsbi.cciEventTimeTable.addElement(elem);
                        if (log.isDebugEnabled())
                        {
                            log.debug( m_logPrefix + "asyncEvent: MPE_DVR_EVT_CCI_UPDATE: Adding " + elem 
                                      + " to CCI TimeTable: " + tsbi.cciEventTimeTable );
                        }
                    }
                } // END synchronized (this)

                // Initiate the notifications after we've stored the CCI event
                notifyCCIChange(cciChangedEvent);
                break;
            }
            case TimeShiftBufferImpl.MPE_DVR_EVT_SESSION_RECORDING:
            { // Confirmation that buffering has started
                // Get the effective TimeBase time before acquiring the lock (which may block)
                final long effectiveTimeBaseTime = this.getTimeBase().getNanoseconds();
                
                synchronized (this)
                {
                    if (getState() == TimeShiftManager.TSWSTATE_BUFF_PENDING)
                    {
                        TimeShiftBufferImpl tsbi = getNewestTimeShiftBuffer();
                        tsbi.handleBufferingStarted(effectiveTimeBaseTime, m_timeBaseStartTimeNs);
                        if (log.isDebugEnabled())
                        {
                            log.debug( m_logPrefix + "asyncEvent: MPE_DVR_EVT_SESSION_RECORDING: Effective timebase time: " 
                                       + effectiveTimeBaseTime/NANOS_PER_MILLI + "ms" );
                        }
                        setStateAndNotify(TimeShiftManager.TSWSTATE_BUFFERING, TimeShiftManager.TSWREASON_NOREASON);
                    }
                    else
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info(m_logPrefix + "asyncEvent: MPE_DVR_EVT_SESSION_RECORDING: in state "
                            		+ TimeShiftManager.stateString[getState()]
                            		+ " - IGNORING" );
                        }
                    }
                } // END synchronized (this)
                break;
            }
        } // END switch (eventCode)
    } // END asyncEvent
    
    /*************************************************************************/
    /*************************************************************************/
    /* Implementation of org.cablelabs.impl.pod.mpe.CASessionListener interface */
    /*************************************************************************/
    /*************************************************************************/

    /**
     * {@inheritDoc}
     */
    public void notifyCASessionChange(CASession session, CASessionEvent event)
    {
        // We should only receive this when we have an active CASession
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "notifyCASessionChange(" + session.toString() + ", " 
                                   + event.toString() + ')' );
        }
        
        synchronized (this)
        {
            m_isAuthorized = (event.getEventID() == CASessionEvent.EventID.FULLY_AUTHORIZED);
            
            if (m_isAuthorized)
            {
                // Really only means something if we're in the NOT_READY_TO_BUFFER state
                if (m_curState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER)
                {
                    // CA is a necessary but not sufficient condition for being READY
                    // Check the other conditions necessary for going to READY
                    if (ableToBuffer())
                    {
                        setStateAndNotify( TimeShiftManager.TSWSTATE_READY_TO_BUFFER,
                                           TimeShiftManager.TSWREASON_ACCESSGRANTED );
                        // Note: Clients may attach for buffering after getting this
                        // indication
                    }
                } // END if (m_curState == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER)
            } // END if (m_isAuthorized)
            else
            {
                switch (m_curState)
                {
                    case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                    case TimeShiftManager.TSWSTATE_BUFFERING:
                    {
                        if ((m_constraints.uses & usesThatRequireTSB) == 0)
                        { // The TSB is not needed/requested by any clients
                            if (log.isDebugEnabled())
                            {
                                log.debug(m_logPrefix
                                        + "notifyCASessionChange: BTSB not in use - stopping buffering");
                            }
    
                            // Note: We keep the CASession open so the session can recover
                            stopBuffering(TimeShiftManager.TSWREASON_ACCESSWITHDRAWN);
    
                            // Go straight to the NOT_READY_TO_BUFFER state since there
                            //  are not any uses that require the tuner
                            setStateAndNotify( TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER,
                                               TimeShiftManager.TSWREASON_ACCESSWITHDRAWN );
                            break;
                        } // END if (no uses require tuner)
    
                        // Assert: The BTSB is still requested by a client - so
                        // start a coordinated buffering shutdown
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix
                                    + "notifyCASessionChange: Starting coordinated buffering shutdown");
                        }
    
                        setStateAndNotify( TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN, 
                                           TimeShiftManager.TSWREASON_ACCESSWITHDRAWN );
    
                        // When all buffering clients are detached, we'll stop buffering 
                        //  and either transition to TSWSTATE_NOT_READY_TO_BUFFER or TSWSTATE_READY_TO_BUFFER - 
                        //  based on the CA status at that time
                        break;
                    } // END case TimeShiftManager.TSWSTATE_BUFFERING
                    case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
                    {
                        // Go straight to the NOT_READY_TO_BUFFER state since we're not
                        //  buffering
                        // Note: We keep the CASession open so the session can recover
                        setStateAndNotify( TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER,
                                           TimeShiftManager.TSWREASON_ACCESSWITHDRAWN );
                        break;
                    } // END case TimeShiftManager.TSWSTATE_READY_TO_BUFFER
                    default:
                    { // We'll just ignore the CA state change in any other state - it's
                      // irrelevant
                        break;
                    }
                } // END switch (m_curState)
            } // END else/if (m_isAuthorized)
        } // END synchronized(this)
    } // END notifyCASessionChange
        
    // Code to support PlaybackClient interface.
        
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#addObserver
     * (org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver)
     */
    public void addObserver(PlaybackClientObserver listener)
    {
        synchronized (this)
        {
            if (!m_playbackListeners.contains(listener)) m_playbackListeners.addElement(listener);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#removeObserver
     * (org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver)
     */
    public void removeObserver(PlaybackClientObserver listener)
    {
        synchronized (this)
        {
            m_playbackListeners.removeElement(listener);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#getPlayers()
     */
    public Vector getPlayers()
    {
        synchronized (this)
        {
            if (log.isDebugEnabled())
            {
                log.debug("TimeShiftWindowClient@" + Integer.toHexString(System.identityHashCode(this))
                        + ".getPlayers(): " + m_players);
            }

            return (Vector) m_players.clone();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#addPlayer
     * (org.cablelabs.impl.media.player.AbstractDVRServicePlayer)
     */
    public void addPlayer(final AbstractDVRServicePlayer player)
    {
        Object a[];
        int i;
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "addPlayer(" + player + ')');
        }

        synchronized (this)
        {
            m_players.addElement(player);

            // Notify observers player attached.
            i = m_playbackListeners.size();
            a = new Object[i];
            m_playbackListeners.copyInto(a);
        }

        // Notify observers Player attached. Observers
        // must perform state checking to determine how to proceed since
        // we do not hold a lock.
        for (int ndx = 0; ndx < i; ndx++)
        {
            PlaybackClientObserver psl = ((PlaybackClientObserver) a[i]);
            try
            {
                psl.clientNotify(new PlaybackClientAddedEvent(player));
            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Exception thrown by listener, continue looping");
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#removePlayer
     * (org.cablelabs.impl.media.player.AbstractDVRServicePlayer)
     */
    public void removePlayer(final AbstractDVRServicePlayer player)
    {
        Object a[];
        int i;
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "removePlayer(" + player + ')');
        }

        synchronized (this)
        {
            m_players.remove(player);
            
            i = m_playbackListeners.size();
            a = new Object[i];
            m_playbackListeners.copyInto(a);
        }

        // Notify observers a Player dettached. Observers
        // must perform state checking to determine how to proceed since
        // we do not hold a lock.
        for (int ndx = 0; ndx < i; ndx++)
        {
            PlaybackClientObserver psl = ((PlaybackClientObserver) a[i]);
            try
            {
                psl.clientNotify(new PlaybackClientRemovedEvent(player));
            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Exception thrown by listener, continue looping");
                }
            }
        }
    }

    /**
     * Timer specification used to start death timer
     */
    class ExpirationTrigger implements TVTimerWentOffListener
    {
        TVTimerSpec spec = null;

        TVTimer m_timer;

        ExpirationTrigger(long l)
        {
            m_timer = TVTimer.getTimer();
            spec = new TVTimerSpec();
            spec.setAbsoluteTime(l);
            spec.addTVTimerWentOffListener(this);
        }

        public void descheduleTimer()
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "descheduleTimer spec: " + spec);
            }
            if (spec != null)
            {
                m_timer.deschedule(spec);
                spec = null;
            }
        }

        public void scheduleTimer() throws TVTimerScheduleFailedException
        {
            spec = m_timer.scheduleTimerSpec(spec);
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "scheduleTimerSpec spec: " + spec);
            }
        }

        public void timerWentOff(TVTimerWentOffEvent ev)
        {
            m_deathTimerStarted = false;
            // Call TSM to do the release to avoid acquiring the TSW lock
            //  ahead of TSW lock(s)
            TimeShiftWindow.s_tsm.releaseUnusedTSWResources(TimeShiftWindow.this);
        } // END timerWentOff()
    } // END class ExpirationTrigger

    /*****************************************************************************/
    /*****************************************************************************/
    /**
     * Inner class for implementation of TimeShiftWindowClient.
     * 
     * The TimeShiftWindowClient serves as both the interface to the
     * TimeShiftWindow and a container for client-specific parameters.
     * 
     * @author Craig Pratt
     * 
     */
    /*****************************************************************************/
    /*****************************************************************************/
    public class TimeShiftWindowClientImpl implements org.cablelabs.impl.manager.TimeShiftWindowClient,
            LightweightTriggerEventStoreWriteChange, LightweightTriggerEventStoreReadChange
    {
        /** The associated TimeShiftWindow. */
        TimeShiftWindow m_tsw;

        /** The listener registered via this client */
        TimeShiftWindowChangedListener m_listener;

        /** The listener priority */
        int m_listenerPriority;
        
        /** The TimeShiftWindow constraints */
        TimeShiftConstraints m_constraints;

        /** ResourceUsage associated with this client */
        final ResourceUsage m_resourceUsage;

        /** List of TSBs attached by this client */
        ArrayList m_attachedTSBs;

        /**
         * For tracking which TimeShift this client is either presenting from or
         * recording (converting) from
         */
        TimeShiftBufferImpl m_usingTimeShift;

        final String logPrefix = "TimeShiftWindowClient@" + Integer.toHexString(System.identityHashCode(this));

        public TimeShiftWindowClientImpl(TimeShiftWindow tsw, long minDuration, long maxDuration, long desiredDuration,
                int uses, int reservations, ResourceUsage ru, TimeShiftWindowChangedListener tswcl, int tswclPriority)
        {
            m_tsw = tsw;
            m_constraints = new TimeShiftConstraints(uses, reservations, minDuration, maxDuration, desiredDuration,
                    null);
            m_resourceUsage = ru;

            m_attachedTSBs = new ArrayList();

            m_listener = tswcl;
            m_listenerPriority = tswclPriority;
        } // END TimeShiftWindowClientImpl()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#changeListener(org.
         * cablelabs.impl.manager.TimeShiftWindowChangedListener)
         */
        public void changeListener(TimeShiftWindowChangedListener tswcl, int tswclPriority)
        {
            if (log.isInfoEnabled())
            {
                log.info( m_logPrefix + " changing listener to " + tswcl
                           + " (was " + m_listener + ')' );
            }
            synchronized (TimeShiftWindow.this)
            {
                m_listener = tswcl;
                m_listenerPriority = tswclPriority;
            } // END synchronized (TimeShiftWindow.this)
        } // END addListener()

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#attachFor(int)
         */
        public void attachFor(int uses) throws IllegalStateException, IllegalArgumentException
        {
            synchronized (TimeShiftWindow.this)
            {
                uses = uses & ~m_constraints.uses;

                // Assert: uses only contains uses not previously attached

                if (uses == 0)
                {
                    // Client is already attached for the uses
                    return;
                }

                // Assert: Use allowed on the TimeShiftWindow

                // Track the fact that this client is using
                m_constraints.uses |= uses;

                try
                {
                    // Note: This may initiate buffering
                    // Note: This will throw on error
                    TimeShiftWindow.this.updateForAddedUses(this, uses);
                }
                catch (IllegalArgumentException iae)
                {
                    m_constraints.uses &= ~uses;
                    TimeShiftWindow.this.calculateConstraints();
                    throw iae;
                }
                catch (IllegalStateException ise)
                {
                    m_constraints.uses &= ~uses;
                    TimeShiftWindow.this.calculateConstraints();
                    throw ise;
                }
            } // END synchronized (TimeShiftWindow.this)
        } // END attachFor()

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#release()
         */
        public void release()
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + " - release: " + this);
            }
            synchronized (TimeShiftWindow.this)
            {
                m_constraints.reservations = 0;
                // Assert: m_uses = 0
                detachForAll();
                detachAllTSBs();

                if (m_tsw != null)
                {
                    m_tsw.removeClient(this);
                    m_tsw = null;
                }

                m_listener = null;
            } // END synchronized (TimeShiftWindow.this)
        } // END release()

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#detachFor(int)
         */
        public void detachFor(int uses) throws IllegalArgumentException
        {
            synchronized (TimeShiftWindow.this)
            {
                // Check that client is attached for the uses it's detaching for
                if ((m_constraints.uses & uses) != uses)
                {
                    throw new IllegalArgumentException(
                            "TimeShiftWindowClient not attached for one or more designated uses (uses "
                                    + TimeShiftManagerImpl.useString(uses) + ')');
                }

                // Since uses is (currently) represented as a bitmask,
                // just and-out the bits set in uses
                m_constraints.uses &= ~uses;

                TimeShiftWindow.this.updateForRemovedUses(this, uses);
            } // END synchronized (TimeShiftWindow.this)
        } // END detachFor()

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#detachForAll()
         */
        public void detachForAll()
        {
            detachFor(m_constraints.uses);
            // Note: This can't throw IllegalArgumentException the way
            //       we're calling this
        } // END detachForAll()

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#getState()
         */
        public int getState()
        {
            if (m_tsw == null)
            {
                return TimeShiftManager.TSWSTATE_INVALID;
            }
            else
            {
                return m_tsw.getState();
            }
        } // END getState()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#addClient(org.cablelabs
         * .impl.manager.TimeShiftWindowChangedListener)
         */
        public TimeShiftWindowClient addClient( int reservations, 
                                                TimeShiftWindowChangedListener tswcl, 
                                                int tswclPriority ) 
            throws IllegalArgumentException
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "addClient - reservations: " + TimeShiftManagerImpl.useString(reservations) + ", listener: " + tswcl);
            }
            final int combinedUse = (m_constraints.uses | m_constraints.reservations) & reservations;
            if ((combinedUse & exclusiveUses) != 0)
            {
                throw new IllegalArgumentException("TimeShiftWindow already in (exclusive) use for "
                        + TimeShiftManagerImpl.useString(combinedUse));
            }
            return m_tsw.addClient(0, // No minumum duration
                    0, // No desired duration
                    reservations, m_resourceUsage, // Use this client's RU
                    tswcl, tswclPriority);
        } // END addClient()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#setDesiredDuration
         * (long)
         */
        public void setDesiredDuration(long duration)
        {
            synchronized (TimeShiftWindow.this)
            {
                this.m_constraints.desiredDuration = duration;

                TimeShiftWindow.this.updateForChangedDuration(calculateConstraints());
            } // END synchronized (TimeShiftWindow.this)
        } // END setDesiredDuration()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#setMaximumDuration
         * (long)
         */
        public void setMaximumDuration(long duration)
        {
            synchronized (TimeShiftWindow.this)
            {
                this.m_constraints.maxDuration = duration;

                TimeShiftWindow.this.updateForChangedDuration(calculateConstraints());
            } // END synchronized (TimeShiftWindow.this)
        } // END setMaximumDuration()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#setMinimumDuration
         * (long)
         */
        public void setMinimumDuration(long duration)
        {
            synchronized (TimeShiftWindow.this)
            {
                this.m_constraints.minDuration = duration;

                TimeShiftWindow.this.updateForChangedDuration(calculateConstraints());
            } // END synchronized (TimeShiftWindow.this)
        } // END setMinimumDuration()

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#getDuration()
         */
        public long getDuration()
        {
            synchronized (TimeShiftWindow.this)
            {
                long totalDuration = 0;

                for (final Iterator tsbe = m_timeShifts.iterator(); tsbe.hasNext();)
                {
                    TimeShiftBufferImpl tsbi = (TimeShiftBufferImpl) tsbe.next();

                    totalDuration += tsbi.getDuration();
                }

                return totalDuration;
            } // END synchronized (this)
        } // END getDuration()

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#getSize()
         */
        public long getSize()
        {
            synchronized (TimeShiftWindow.this)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "getSize: minDuration " + m_tsw.m_constraints.minDuration
                            + ", desiredDuration : " + m_tsw.m_constraints.desiredDuration);
                }

                return (Math.max(m_tsw.m_constraints.minDuration, m_tsw.m_constraints.desiredDuration));
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#attachToTSB(org.
         * cablelabs.impl.manager.TimeShiftBuffer)
         */
        public TimeShiftBuffer attachToTSB(TimeShiftBuffer tsb) throws IllegalArgumentException
        {
            synchronized (TimeShiftWindow.this)
            {
                // Verify that the TSB is in the TSW
                if (!m_timeShifts.contains(tsb))
                {
                    throw new IllegalArgumentException(tsb + " is not in " + TimeShiftWindow.this);
                }

                m_attachedTSBs.add(tsb);

                // TODO: Update buffering/presenting/recording flags on TSB

                return tsb;
            } // END synchronized (TimeShiftWindow.this)
        } // END attachToTSB()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#detachFromTSB(org
         * .cablelabs.impl.manager.TimeShiftBuffer)
         */
        public void detachFromTSB(TimeShiftBuffer tsb) throws IllegalArgumentException
        {
            synchronized (TimeShiftWindow.this)
            {
                if (!m_attachedTSBs.remove(tsb))
                {
                    throw new IllegalArgumentException(tsb + " has not been attached by " + this);
                }

                // TODO: Update buffering/presenting/recording flags on TSB

            } // END synchronized (TimeShiftWindow.this)
        } // END detachFromTSB()

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#detachAllTSBs()
         */
        public void detachAllTSBs()
        {
            synchronized (TimeShiftWindow.this)
            {
                final Object[] attachedTSBs = m_attachedTSBs.toArray();

                for (int i = 0; i < attachedTSBs.length; i++)
                {
                    detachFromTSB((TimeShiftBuffer) attachedTSBs[i]);
                }
            } // END synchronized (TimeShiftWindow.this)
        } // END detachAllTSBs()

        public int getBufferingTSBHandle() throws IllegalStateException
        {
            synchronized (TimeShiftWindow.this)
            {
                return m_tsw.getBufferingTSBHandle();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#elements()
         */
        public Enumeration elements()
        {
            final List tsbListCopy;
            synchronized (TimeShiftWindow.this)
            {
                tsbListCopy = new ArrayList(m_tsw.m_timeShifts);
            }
            
            final Enumeration elementEnumeration = new java.util.Enumeration()
            {
                Iterator e = tsbListCopy.iterator();

                public boolean hasMoreElements()
                {
                    return e.hasNext();
                }

                public Object nextElement()
                {
                    return e.next();
                }
            };

            return elementEnumeration;
        } // END elements()

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#getFirstTSB()
         */
        public TimeShiftBuffer getFirstTSB()
        {
            synchronized (TimeShiftWindow.this)
            {
                if (m_tsw.m_timeShifts.size() > 0)
                {
                    return (TimeShiftBuffer) m_tsw.m_timeShifts.get(0);
                }
                return null;
            } // END synchronized (this)
        } // END getFirstTSB()

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#getLastTSB()
         */
        public TimeShiftBuffer getLastTSB()
        {
            synchronized (TimeShiftWindow.this)
            {
                if (m_tsw.m_timeShifts.size() > 0)
                {
                    return (TimeShiftBuffer) m_tsw.m_timeShifts.get(m_tsw.m_timeShifts.size() - 1);
                }
                return null;
            } // END synchronized (this)
        } // END getLastTSB()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getBufferingTSB()
         */
        public TimeShiftBuffer getBufferingTSB() throws IllegalStateException
        {
            synchronized (TimeShiftWindow.this)
            {
                return m_tsw.getNewestTimeShiftBuffer();
            } // END synchronized (this)
        } // END getBufferingTSB()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getTSBPreceeding
         * (org.cablelabs.impl.manager.TimeShiftBuffer)
         */
        public TimeShiftBuffer getTSBPreceeding(TimeShiftBuffer tsb)
        {
            synchronized (TimeShiftWindow.this)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getTSBPreceeding: " + tsb + ", tsbs: " + m_timeShifts);
                }
                Object[] tsbs = m_timeShifts.toArray();

                // Note: We're starting at 1
                for (int i = 1; i < tsbs.length; i++)
                {
                    if ((TimeShiftBuffer) (tsbs[i]) == tsb)
                    {
                        return (TimeShiftBuffer) (tsbs[i - 1]);
                    }
                }
                // Assert: tsb not found or was the first in the list
                return null;
            } // END synchronized (this)
        } // END getTSBPreceeding()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getTSBFollowing(
         * org.cablelabs.impl.manager.TimeShiftBuffer)
         */
        public TimeShiftBuffer getTSBFollowing(TimeShiftBuffer tsb)
        {
            synchronized (TimeShiftWindow.this)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getTSBFollowing: " + tsb + ", tsbs: " + m_timeShifts);
                }
                Object[] tsbs = m_timeShifts.toArray();

                // Note: We're not going to the end
                for (int i = 0; i < tsbs.length - 1; i++)
                {
                    if ((TimeShiftBuffer) (tsbs[i]) == tsb)
                    {
                        return (TimeShiftBuffer) (tsbs[i + 1]);
                    }
                }
                // Assert: tsb not found or was the last in the list
                return null;
            } // END synchronized (this)
        } // END getTSBFollowing()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getTSBForTimeBaseTime
         * (long, int)
         */
        public TimeShiftBuffer getTSBForTimeBaseTime(long targetTime, int proximity)
        {
            synchronized (TimeShiftWindow.this)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getTSBForTimeBaseTime - target time: " + targetTime + ", proximity: " + proximity
                            + ", tsb count: " + m_timeShifts.size());
                }
                Object[] tsbs = m_timeShifts.toArray();

                TimeShiftBuffer preceedingTSB = null;
                TimeShiftBuffer followingTSB = null;

                for (int i = 0; i < tsbs.length; i++)
                {
                    TimeShiftBufferImpl tsbi = (TimeShiftBufferImpl) tsbs[i];
                    if (log.isDebugEnabled())
                    {
                        log.debug("examining tsb: " + tsbi);
                    }

                    // Note: There's a slightly more optimal way to do this:
                    // Check the start/end time of the TSB and only
                    // compare media times if there's more than one TSB
                    // for the media time
                    long st = tsbi.getTimeBaseStartTime();
                    long et = tsbi.getTimeBaseEndTime();

                    if ((targetTime >= st) && (targetTime < et))
                    { // Found the media time in a TSB - we're done
                        if (log.isDebugEnabled())
                        {
                            log.debug("found target time (" + targetTime + ") in tsb - returning tsb: " + tsbi);
                        }
                        return tsbi;
                    }

                    // Last tsb with end time less than time is the preceeding
                    // TSB
                    if (et < targetTime)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("tsb endtime (" + et + ") less than target time (" + targetTime
                                    + ") - setting tsb as preceeding: " + tsbi);
                        }
                        preceedingTSB = tsbi;
                    }

                    // First tsb with start time after time is the following TSB
                    if (st > targetTime)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("tsb starttime (" + st + ") greater than target time (" + targetTime
                                    + ") - setting tsb as following: " + tsbi);
                        }
                        followingTSB = tsbi;
                        break; // No point in continuing once on the tsb beyond
                               // time
                    }
                } // END loop through tsbs

                // Assert: didn't find an exact match
                switch (proximity)
                {
                    case PROXIMITY_EXACT:
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("proximity exact, but tsb containing target time not found - returning null");
                        }
                        return null; // Would have returned in loop
                    }
                    case PROXIMITY_BACKWARD:
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("proximity backward - returning tsb (preceeding): " + preceedingTSB);
                        }
                        return preceedingTSB;
                    }
                    case PROXIMITY_FORWARD:
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("proximity forward - returning tsb (following): " + followingTSB);
                        }
                        return followingTSB;
                    }
                    default:
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("getTSBForTimeBaseTime: Bad proximity code: " + proximity);
                        }
                        return null;
                    }
                }
            } // END synchronized (this)
        } // END getTSBForTimeBaseTime()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getTSBForSystemTime
         * (long, int)
         */
        public TimeShiftBuffer getTSBForSystemTime(long systemTime, int proximity)
        {
            synchronized (TimeShiftWindow.this)
            {
                Object[] tsbs = m_timeShifts.toArray();

                TimeShiftBuffer preceedingTSB = null;
                TimeShiftBuffer followingTSB = null;

                for (int i = 0; i < tsbs.length; i++)
                {
                    TimeShiftBufferImpl tsbi = (TimeShiftBufferImpl) tsbs[i];

                    // Note: There's a slightly more optimal way to do this:
                    // Check the start/end time of the TSB and only
                    // compare media times if there's more than one TSB
                    // for the media time
                    long st = tsbi.getContentStartTimeInSystemTime();
                    long et = tsbi.getContentEndTimeInSystemTime();

                    if ((systemTime >= st) && (systemTime < et))
                    { // Found the media time in a TSB - we're done
                        return tsbi;
                    }

                    // Last tsb with met less than time is the preceeding TSB
                    if (et < systemTime)
                    {
                        preceedingTSB = tsbi;
                    }

                    // First tsb with start time after time is the following TSB
                    if (st > systemTime)
                    {
                        followingTSB = tsbi;
                        break; // No point in continuing once on the tsb beyond
                               // time
                    }
                } // END loop through tsbs

                // Assert: didn't find an exact match
                switch (proximity)
                {
                    case PROXIMITY_EXACT:
                        return null; // Would have returned in loop
                    case PROXIMITY_BACKWARD:
                        return preceedingTSB;
                    case PROXIMITY_FORWARD:
                        return followingTSB;
                    default:
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("getTSBForSystemTime: Bad proximity code: " + proximity);
                        }
                        return null;
                    }
                }
            } // END synchronized (this)
        } // END getTSBForSystemTime()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getTSBsForTimeSpan
         * (long, long)
         */
        public Enumeration getTSBsForSystemTimeSpan( final long startTimeInSystemTimeMs, 
                                                     final long durationMs )
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "getTSBsForTimeSpan(starttime " + new Date(startTimeInSystemTimeMs)
                          + ", dur " + durationMs + ')');
            }
            
            synchronized (TimeShiftWindow.this)
            {
                Vector tsbsInSpan = new Vector();
                final long endTime = startTimeInSystemTimeMs + durationMs;

                final Iterator tsbe = m_tsw.m_timeShifts.iterator();
                while (tsbe.hasNext())
                {
                    TimeShiftBuffer tsb = (TimeShiftBuffer) tsbe.next();
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "getTSBsForTimeSpan: considering " + tsb);
                    }

                    if (!( (tsb.getContentStartTimeInSystemTime() > endTime) 
                           || ( (tsb.getContentEndTimeInSystemTime() + TVTimer.getTimer().getGranularity())
                                < startTimeInSystemTimeMs ) ) )
                    { // If the span doesn't end before the tsb start
                        // or the span doesn't start after the tsb end,
                        // there must be some overlap
                        tsbsInSpan.add(tsb);
                    }
                } // END while (tsbe.hasMoreElements())

                return tsbsInSpan.elements();
            } // END synchronized (this)
        } // END getTSBsForTimeSpan

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getNetworkInterface
         * ()
         */
        public ExtendedNetworkInterface getNetworkInterface()
        {
            return m_eni;
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean isAuthorized()
        {
            return m_tsw.m_isAuthorized;
        }

        /**
         * {@inheritDoc}
         */
        public short getLTSID()
        {
            return m_tsw.getLTSID();
        }

        public String toString()
        {
            return "TSWC 0x" + Integer.toHexString(this.hashCode()) 
                    + ":[listener " 
                    + ((m_listener != null) ? m_listener.getClass().toString() : "null") 
                    + ",lpri " + m_listenerPriority 
                    + ",RU " + m_resourceUsage 
                    + ",tsw " + "0x" + (m_tsw==null ? "0" : Integer.toHexString(m_tsw.hashCode()))
                    + ",constrain " + m_constraints 
                    + ",1st tsb:" + ((m_attachedTSBs.size() > 0) ? m_attachedTSBs.get(0) : " none") + ']';
        } // END toString()

        private TimeTable m_lwteCache = null;

        private boolean m_lwteCacheStale = true;

        private void setMediaTimeAndEventsCache()
        {
            synchronized (TimeShiftWindow.this)
            {
                if (m_lwteCacheStale || m_lwteCache == null)
                {
                    m_lwteCache = SequentialMediaTimeStrategy.getMediaTimeAndEventsFromTsbs(m_tsw.m_timeShifts);
                    if (log.isDebugEnabled())
                    {
                        log.debug(logPrefix + ".setMediaTimeAndEventsCache() -- m_lwteCache.size=="
                                + m_lwteCache.getSize());
                    }
                    m_lwteCacheStale = false;
                }
            }
        }

        public boolean addLightweightTriggerEvent(LightweightTriggerEvent lwte)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + ".addLightweightTriggerEvent(LightweightTriggerEvent)");
            }

            // this is done here and is not pushed down into the buffer since
            // events can fail to be stored
            // in a buffer for reasons other than an existing lwte has the same
            // identity.
            // acossitt: I'm not sure that is a good reason
            // if(checkStore(lwte)) return false;

            return addLightweightTriggerEvent(true, lwte);
        }

        /**
         * cacheLightweightTriggerEvent
         * 
         * does nothing. don't call.
         */
        public boolean cacheLightweightTriggerEvent(Object src, LightweightTriggerEvent lwte)
        {
            return false;
        }

        public void store(Object src)
        {
            // do nothing
        }

        /*
         * modifies lwte.time
         */
        private boolean addLightweightTriggerEvent(boolean notify, LightweightTriggerEvent lwte)
        {
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + ".addLightweightTriggerEvent(boolean, LightweightTriggerEvent)");
            }

            // it is possible that no TSB exists that can take the event. If so
            // found==false
            // and no one is notified since nothing has changed.
            boolean added = false;
            // acossitt remove try catch
            try
            {
                synchronized (TimeShiftWindow.this)
                {
                    // I deliberately did not use getEndTime(). Future events,
                    // which may be past
                    // the end time of any TSB must be stored in the last TSB.
                    // That is why the
                    // iteration goes backwards.
                    for (int i = m_tsw.m_timeShifts.size() - 1; i >= 0; i--)
                    {
                        TimeShiftBuffer tsb = (TimeShiftBuffer) m_tsw.m_timeShifts.get(i);
                        long startTimeMs = tsb.getContentStartTimeInSystemTime();
                        long eventTimeMs = lwte.getTimeMillis();
                        if (eventTimeMs >= startTimeMs)
                        {
                            // store as media time (ns) from beginning of TSB
                            // lwte time original starts as system time (ms)
                            long eventTimeFromTsbStartMs = eventTimeMs - startTimeMs;
                            lwte.setTimeNanos(eventTimeFromTsbStartMs * NANOS_PER_MILLI);

                            if (!tsb.addLightweightTriggerEvent(lwte))
                            {
                                // already exists
                                return false;
                            }

                            if (log.isDebugEnabled())
                            {
                                log.debug(logPrefix + ".addLightweightTriggerEvent() -- tsb after add:" + tsb);
                            }
                            if (log.isDebugEnabled())
                            {
                                log.debug(logPrefix + ".addLightweightTriggerEvent() -- m_lwteCacheStale==true");
                            }
                            m_lwteCacheStale = true;
                            added = true;
                            break;
                        }
                    }
                }
                if (notify && added)
                {
                    m_tsw.m_lwteChangeHelper.notifyListeners();
                }
            }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error(logPrefix + ".addLightweightTriggerEvent(boolean, LightweightTriggerEvent) exception", e);
                }
            }
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + ".addLightweightTriggerEvent -- added = " + added);
            }
            return true; // did not fail because of previously existing event
        }

        public LightweightTriggerEvent getEventByName(String name)
        {
            synchronized (TimeShiftWindow.this)
            {
                setMediaTimeAndEventsCache();

                Enumeration lwteEnum = m_lwteCache.elements();
                while (lwteEnum.hasMoreElements())
                {
                    LightweightTriggerEvent lwte = (LightweightTriggerEvent) lwteEnum.nextElement();
                    if (name.equals(lwte.eventName))
                    {
                        return lwte;
                    }
                }
                return null;
            }
        }

        public String[] getEventNames()
        {
            synchronized (TimeShiftWindow.this)
            {
                setMediaTimeAndEventsCache();

                String[] names = new String[m_lwteCache.getSize()];
                Enumeration lwteEnum = m_lwteCache.elements();
                int i = 0;
                while (lwteEnum.hasMoreElements())
                {
                    LightweightTriggerEvent lwte = (LightweightTriggerEvent) lwteEnum.nextElement();
                    names[i++] = lwte.eventName;
                }
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "getEventNames returning names: " + names);
                }
                return names;
            }
        }

        public void registerChangeNotification(LightweightTriggerEventChangeListener listener)
        {
            m_tsw.m_lwteChangeHelper.registerChangeNotification(listener);

        }

        public void unregisterChangeNotification(LightweightTriggerEventChangeListener listener)
        {
            m_tsw.m_lwteChangeHelper.unregisterChangeNotification(listener);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#addObserver
         * (
         * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver)
         */
        public void addObserver(PlaybackClientObserver listener)
        {
            m_tsw.addObserver(listener);
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#
         * removeObserver
         * (org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver
         * )
         */
        public void removeObserver(PlaybackClientObserver listener)
        {
            m_tsw.removeObserver(listener);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#getPlayers
         * ()
         */
        public Vector getPlayers()
        {
            return m_tsw.getPlayers();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#addPlayer
         * (org.cablelabs.impl.media.player.AbstractDVRServicePlayer)
         */
        public void addPlayer(AbstractDVRServicePlayer player)
        {
            m_tsw.addPlayer(player);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#removePlayer
         * (org.cablelabs.impl.media.player.AbstractDVRServicePlayer)
         */
        public void removePlayer(AbstractDVRServicePlayer player)
        {
            m_tsw.removePlayer(player);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#sameAs(org.cablelabs
         * .impl.manager.TimeShiftWindowClient)
         */
        public boolean sameAs(TimeShiftWindowClient other)
        {
            if (!(other instanceof TimeShiftWindowClientImpl))
            {
                return false;
            }
            if (((TimeShiftWindowClientImpl) other).m_tsw == m_tsw)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        public ServiceExt getService()
        {
            return m_tsw.getService();
        }

        public ResourceUsage getResourceUsage()
        {
            return m_resourceUsage;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#sameAs(org.cablelabs
         * .impl.manager.TimeShiftWindowClient)
         */
        public int getUses()
        {
            synchronized (TimeShiftWindow.this)
            {
                return m_constraints.uses;
            }
        }

        public TimeBase getTimeBase()
        {
            return m_tsw.getTimeBase();
        }

        public long getTimeBaseStartTime()
        {
            return m_tsw.getTimeBaseStartTime();
        }

        public long getSystemStartTimeMs()
        {
            return m_tsw.m_systemStartTimeMs;
        }
    } // END class TimeShiftWindowClientImpl

    /*************************************************************************/

    public void setMonitor(TimeShiftWindowMonitorListener tsm)
    {
        m_tswm = tsm;
    }

    public TimeShiftWindowMonitorListener getMonitor()
    {
        return m_tswm;
    }

    public TimeBase getTimeBase()
    {
        return m_timeBase;
    }

    public long getTimeBaseStartTime()
    {
        return m_timeBaseStartTimeNs;
    }
    public short getLTSID()
    {
        return (m_caSession != null ? m_caSession.getLTSID() 
                                    : CASession.LTSID_UNDEFINED);
    }
} // END class TimeShiftWindow
