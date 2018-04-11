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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NoFreeInterfaceException;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.TimeShiftBufferResourceUsage;
import org.ocap.resource.ApplicationResourceUsage;
import org.ocap.resource.ResourceUsage;
import org.ocap.service.ServiceContextResourceUsage;
import org.ocap.si.ProgramMapTableManager;

import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceImpl;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowChangedListener;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.TimeShiftWindowListener;
import org.cablelabs.impl.manager.timeshift.TimeShiftWindow.TimeShiftWindowClientImpl;
import org.cablelabs.impl.ocap.si.ProgramMapTableManagerImpl;
import org.cablelabs.impl.util.MPEEnv;

public class TimeShiftManagerImpl implements TimeShiftManager
{
    /**
     * Singleton instance variable. All Managers should be singletons.
     */
    protected static TimeShiftManagerImpl m_instance;

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(TimeShiftManagerImpl.class.getName());

    /**
     * internal sync object. Used for all TimeShiftManager synchronization.
     */
    protected static final Object m_sync = new Object();

    /**
     * internal sync object to prevent multiple calls into ni.reserve() by TSM
     */
    static Object m_niReserveMonitor = new Object();

    /**
     * The list of active TimeShiftWindows
     */
    protected Vector m_timeShiftWindows = new Vector();

    /**
     * Reference to the SI Manager
     */
    static SIManager m_sim;

    /**
     * Reference to the ProgramMapTableManager
     */
    static ProgramMapTableManagerImpl m_pmtm;

    /**
     * Reference to the CallerContextManager
     */
    static CallerContextManager m_ccm;

    /**
     * Reference to the PODManager
     */
    static PODManager m_podm;

    /*************************************************************************/
    /**
     * The default TSB size to allocate when a TSB needs to be setup to support
     * a TSB usage (e.g. recording) that doesn't designate a size constraint.
     * This value is set in non-volatile storage and is persistent across stack
     * initializations. Value is in seconds.
     */
    protected long m_defaultDuration;

    /**
     * The amount of time to sleep (in ms) when the TSM is performing a
     * getTSWFor*() method and it finds a TSW in state RESERVE_PENDING.
     */
    protected long m_reservePendingSleepTime;

    /**
     * Property to read for the initial default duration. Only used if the
     * persistent value has not been set.
     */
    protected static final String INITIAL_DEFAULT_DURATION_PROP = "OCAP.dvr.tsb.defaultsize";

    /**
     * Default for the initial default duration
     */
    protected static final long INITIAL_DEFAULT_DURATION_DEF = 65 * 60;

    /**
     * Property to read for the RESERVE_PENDING TimeShiftWindow retry wait time.
     * This value controls how long the TSM will sleep when performing a
     * getTSWFor*() method and it finds a TSW in state RESERVE_PENDING. The
     * value is in milliseconds.
     */
    protected static final String RESERVE_PENDING_SLEEP_PROP = "OCAP.dvr.tsb.reservependingsleep";

    /**
     * Default for the RESERVE_PENDING TimeShiftWindow retry wait time.
     */
    protected static final long RESERVE_PENDING_SLEEP_DEF = 150;

    /*************************************************************************/
    /**
     * Directory for the persistent TimeShiftBuffer settings
     */
    protected String m_persistentSettingsDirName;

    /**
     * Property to read for persistent TimeShiftBuffer settings directory
     */
    protected static final String PERSISTENT_SETTINGS_DIR_PROP = "OCAP.dvr.tsb.settingsDir";

    protected static final String PERSISTENT_SETTINGS_DIR_BAK_PROP = "OCAP.persistent.dvr";

    /**
     * Default persistent TimeShiftBuffer settings directory
     */
    protected static final String PERSISTENT_SETTINGS_DIR_DEF = "/syscwd"; // not
                                                                           // a
                                                                           // very
                                                                           // good
                                                                           // default...

    /*************************************************************************/
    /**
     * File name for the persistent TimeShiftBuffer settings
     */
    protected String m_persistentSettingsFileName;

    /**
     * Property to read for persistent TimeShiftBuffer settings file name
     */
    protected static final String PERSISTENT_SETTINGS_FILE_PROP = "OCAP.dvr.tsb.settingsFile";

    /**
     * Default persistent TimeShiftBuffer settings directory
     */
    protected static final String PERSISTENT_SETTINGS_FILE_DEF = "tsm_data.dat";

    /*************************************************************************/
    /**
     * Initial size of the TSB pool
     */
    protected int m_initialTSBPoolSize;

    /**
     * Property to read for initial TSB pool size
     */
    protected static final String INIT_TSB_POOL_SIZE_PROP = "OCAP.dvr.tsb.initTSBPoolSize";

    /**
     * Default for initial TSB pool size
     */
    protected static final int INIT_TSB_POOL_SIZE_DEF = 2;

    /*************************************************************************/
    /**
     * Maximum size of the TSB pool
     */
    protected int m_maxTSBPoolSize;

    /**
     * Property to read for max TSB pool size
     */
    protected static final String MAX_TSB_POOL_SIZE_PROP = "OCAP.dvr.tsb.maxTSBPoolSize";

    /**
     * Default max TSB pool size
     */
    protected static final int MAX_TSB_POOL_SIZE_DEF = 50;

    /*************************************************************************/
    /**
     * TimeShiftWindow death timer value. This setting will determine how long
     * after the last TimeShiftWindow client is detached that resources will be
     * released. Value is in seconds.
     */
    protected int m_deathTime;

    /**
     * Property to read for the initializing the death timer. This timer is set
     * when the last resource-holding reservation is detached from a
     * TimeShiftwindow so that consecutive TSW uses (e.g. consecutive recordings
     * on the same Service) can be efficiently processed (can avoid
     * releasing/retuning the NetworkInterface)
     */
    protected static final String DEATH_TIME_PROP = "OCAP.dvr.tsb.tswDeathTime";

    /**
     * Default value for the death timer (used if the property isn't set)
     */
    protected static final int DEATH_TIME_DEF = 30; // Seconds

    /*************************************************************************/

    /**
     * Persistent settings file
     */
    protected File m_persistentSettingsFile;

    /**
     * Collection of TSBs to retain, to prevent continual de/re-allocation of
     * TSBs
     */
    protected Vector m_timeShiftBufferPool = new Vector();

    /**
     * A Listener to be notified when a new TimeShiftWindow is created.
     */
    private TimeShiftWindowListener m_newTimeShiftWindowListener = null;

    /**
     * Get a TimeShiftManager instance.
     * 
     * @return a TimeShiftManager instance.
     */
    public static TimeShiftManagerImpl getInstance()
    {
        // Assert: We may be called in app context
        if (log.isDebugEnabled())
        {
            log.debug("getTimeShiftManagerInstance() called");
        }

        synchronized (m_sync)
        {
            if (m_instance == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getTimeShiftManagerInstance: Initializing TimeShiftmanager");
                }

                //
                // This code block must only be run ONCE
                //
                m_instance = new TimeShiftManagerImpl();

                TimeShiftWindow.s_tsm = m_instance;
            } // END if (m_instance == null)

            return m_instance;
        } // END synchronized(m_sync)
    } // END getTimeShiftManagerInstance()

    /**
     * Constructor for the TimeShiftManager.
     */
    private TimeShiftManagerImpl()
    {
        // Assert: We may be called in app context
        // Assert: Caller is synchronized
        m_ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        m_sim = SIManager.createInstance();
        m_pmtm = (ProgramMapTableManagerImpl) ProgramMapTableManager.getInstance();
        m_podm = (PODManager) ManagerManager.getInstance(PODManager.class);

        m_defaultDuration = MPEEnv.getEnv(INITIAL_DEFAULT_DURATION_PROP, INITIAL_DEFAULT_DURATION_DEF);

        m_persistentSettingsDirName = MPEEnv.getEnv(PERSISTENT_SETTINGS_DIR_PROP, null);

        if (m_persistentSettingsDirName == null)
        {
            m_persistentSettingsDirName = MPEEnv.getEnv(PERSISTENT_SETTINGS_DIR_BAK_PROP, PERSISTENT_SETTINGS_DIR_DEF);
        }

        m_persistentSettingsFileName = MPEEnv.getEnv(PERSISTENT_SETTINGS_FILE_PROP, PERSISTENT_SETTINGS_FILE_DEF);

        m_initialTSBPoolSize = MPEEnv.getEnv(INIT_TSB_POOL_SIZE_PROP, INIT_TSB_POOL_SIZE_DEF);

        m_maxTSBPoolSize = MPEEnv.getEnv(MAX_TSB_POOL_SIZE_PROP, MAX_TSB_POOL_SIZE_DEF);

        m_deathTime = MPEEnv.getEnv(DEATH_TIME_PROP, DEATH_TIME_DEF);

        this.m_reservePendingSleepTime = MPEEnv.getEnv(RESERVE_PENDING_SLEEP_PROP, RESERVE_PENDING_SLEEP_DEF);

        try
        {
            m_persistentSettingsFile = new File(m_persistentSettingsDirName, m_persistentSettingsFileName);
            loadPersistentSettings();
            // Assert: m_defaultDuration loaded from disk

            if (log.isDebugEnabled())
            {
                log.debug("Successfully loaded persistent settings.");
            }
        }
        catch (IOException ioe)
        {
            m_defaultDuration = INITIAL_DEFAULT_DURATION_DEF;

            if (log.isInfoEnabled())
            {
                log.info("Unable to load persistent settings (" + ioe + ')');
            }
            if (log.isDebugEnabled())
            {
                log.debug("Setting default TSB duration to " + m_defaultDuration + 's');
            }

            try
            {
                savePersistentSettings();
            }
            catch (IOException ioe2)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Could not save default duration (" + ioe2 + ')');
                }
        }
        }

        if (log.isInfoEnabled())
        {
            log.info("Default TSB duration: " + m_defaultDuration);
        }
        if (log.isInfoEnabled())
        {
            log.info("Persistent settings directory: " + m_persistentSettingsDirName);
        }
        if (log.isInfoEnabled())
        {
            log.info("Persistent settings file name: " + m_persistentSettingsFileName);
        }
        if (log.isInfoEnabled())
        {
            log.info("Initial TSB pool size: " + m_initialTSBPoolSize);
        }
        if (log.isInfoEnabled())
        {
            log.info("Max TSB pool size: " + m_maxTSBPoolSize);
        }
        if (log.isInfoEnabled())
        {
            log.info("Death timer setting: " + m_deathTime);
        }
        if (log.isInfoEnabled())
        {
            log.info("Reserve pending sleep time: " + m_reservePendingSleepTime);
        }

        initTSBPool();
    } // END TimeShiftManagerImpl()

    /**
     * Read persistent TimeShiftManager settings from persistent storage.
     */
    protected void loadPersistentSettings() throws IOException
    {
        // Assert: Caller is synchronized
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws IOException
                {
                    DataInputStream dis = new DataInputStream(new FileInputStream(m_persistentSettingsFile));
                    try
                    {
                        long defaultDuration = dis.readLong();
                        long csum = dis.readLong();

                        if ((csum != ~defaultDuration) || (defaultDuration == 0))
                        {
                            throw new IOException("Data corruption detected in " + m_persistentSettingsFileName);
                        }

                        // Assert: defaultDuration successfully read and
                        // checksummed
                        m_defaultDuration = defaultDuration;
                    }
                    finally
                    {
                        dis.close();
                    }

                    return null;
                } // END run()
            }); // END doPrivileged/PrivilegedExceptionAction
        }
        catch (PrivilegedActionException pae)
        {
            Exception e = pae.getException();
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else if (e instanceof IOException)
                throw (IOException) e;
            else
                throw new RuntimeException(pae.getMessage());
        }
    } // END loadPersistentSettings()

    /**
     * Write persistent TimeShiftManager settings to persistent storage
     */
    protected void savePersistentSettings() throws IOException
    {
        // Assert: Caller is synchronized
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws IOException
                {
                    DataOutputStream dos = new DataOutputStream(new FileOutputStream(m_persistentSettingsFile));
                    dos.writeLong(m_defaultDuration);
                    long cksum = ~m_defaultDuration; // Simple 1-value checksum
                    dos.writeLong(cksum);
                    dos.close();
                    return null;
                }
            });
        }
        catch (PrivilegedActionException pae)
        {
            Exception e = pae.getException();
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else if (e instanceof IOException)
                throw (IOException) e;
            else
                throw new RuntimeException(pae.getMessage());
        }
    } // END saveDefaultTSBDuration()

    protected void initTSBPool()
    {
        synchronized (m_timeShiftBufferPool)
        {
            for (int i = 0; i < m_initialTSBPoolSize; i++)
            {
                TimeShiftBufferImpl newTSB = new TimeShiftBufferImpl();
    
                int mpeError = newTSB.setSize(m_defaultDuration);
    
                if (mpeError == TimeShiftBufferImpl.MPE_DVR_ERR_NOERR)
                {
                    m_timeShiftBufferPool.add(newTSB);
                    if (log.isInfoEnabled())
                    {
                        log.info("Allocated " + m_defaultDuration + "s TSB for TSB pool - entry: " + i);
                    }
                }
                else
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Couldn't allocate " + m_defaultDuration + "s TimeShiftBuffer for TSB pool - entry: " + i);
                    }
    
                    // TODO: Try allocating minimally-sized TSB to support
                    // recording on a very-full disk. Since we always have
                    // TSBs around, not sure how this would happen...
                }
            }
        } // END synchronized (m_timeShiftBufferPool)
    } // END initTSBPool()

    /**
     * This interface encapsulates TSW suitability criteria and allows much of
     * the mechanics of setting up a TSW to be shared
     */
    interface TSWSuitabilityEvaluator
    {
        /**
         * @param tsw
         * 
         *            Preconditions:
         * 
         *            tsw is active, is bound to Service s, and can support
         *            uses.
         */

        void consider(TimeShiftWindow tsw);

        /**
         * @return Return the best TSW considered or a new one if a new one can
         *         meet the criteria of this evaluator. New TSWs will be IDLE
         */
        TimeShiftWindow getBestTSW();
    } // END interface TSWSuitabilityEvaluator

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.TimeShiftManager#getTSWByInterface(javax.tv
     * .service.Service, org.davic.net.tuning.NetworkInterface, int,
     * org.ocap.resource.ResourceUsage,
     * org.cablelabs.impl.manager.TimeShiftWindowChangedListener)
     */
    public TimeShiftWindowClient getTSWByInterface(final Service s, final NetworkInterface ni, final long startTime,
            final long duration, final int uses, final ResourceUsage ru, final TimeShiftWindowChangedListener tswcl, 
            final int tswclPriority)
            throws NoFreeInterfaceException
    {
        if (log.isDebugEnabled())
        {
            log.debug("getTSWByInterface(Service " + s.getLocator() + ",ni " + ni + ",startTime " + startTime
                    + ",duration " + duration + ",uses " + useString(uses) + ",ru " + ru + ')');
        }

        // This evaluator will attempt to return a TSW with the most buffered
        // content for Service s - or create a new one if none are available
        final TSWSuitabilityEvaluator networkInterfaceEvaluator = new TSWSuitabilityEvaluator()
        {
            TimeShiftWindow bestTSW = null;

            public void consider(TimeShiftWindow tsw)
            {
                // No need to match 'ni' in this case
                if (tsw.getNetworkInterface() == ni)
                {
                    bestTSW = tsw;
                }
            } // END consider()

            public TimeShiftWindow getBestTSW()
            {
                if (bestTSW == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getTSWByInterface.networkInterfaceEvaluator: No active TSW found for NI " + ni);
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getTSWByInterface.networkInterfaceEvaluator: Found active TSW for NI: " + bestTSW);
                    }
                }
                return bestTSW;
            }
        }; // END class networkInterfaceEvaluator

        final long defaultDur = getDefaultDuration();
        
        return getTSWByEvaluation( s, defaultDur, defaultDur, uses, 
                                   ru, tswcl, tswclPriority, networkInterfaceEvaluator );
    } // END getTSWByInterface()

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.TimeShiftManager#getTSWByDuration(javax.tv
     * .service.Service, long, long, int, org.ocap.resource.ResourceUsage,
     * org.cablelabs.impl.manager.TimeShiftWindowChangedListener)
     */
    public TimeShiftWindowClient getTSWByDuration(final Service s, final long minDuration, final long maxDuration,
            final int uses, final ResourceUsage ru, final TimeShiftWindowChangedListener tswcl, int tswclPriority)
            throws NoFreeInterfaceException
    {
        if (log.isDebugEnabled())
        {
            log.debug("getTSWByDuration(Service " + s.getLocator() + ",minD " + minDuration + ",maxD " + maxDuration
                    + ",uses " + useString(uses) + ",ru " + ru + ')');
        }

        synchronized (m_sync)
        {
            // Establish our default duration according to the caller's
            // timeshift parameters
            updateDefaultDuration(minDuration);
        }

        // This evaluator will attempt to return a TSW with the most buffered
        // content for Service s - or create a new one if none are available
        final TSWSuitabilityEvaluator durationEvaluator = new TSWSuitabilityEvaluator()
        {
            long bestDuration = -1;

            TimeShiftWindow bestTSW = null;

            /**
             * Assert: tsw is not IDLE and tsw.getService() == s
             */
            public void consider(TimeShiftWindow tsw)
            {
                final TimeShiftBufferImpl tsb = tsw.getBufferingTimeShiftBuffer();
                final long curTSWsize = (tsb == null) ? 0 : tsb.bufferSize;

                // Just going to go with the biggest TSW we can find...
                if (curTSWsize > bestDuration)
                {
                    bestTSW = tsw;
                    bestDuration = curTSWsize;
                    // Note: This may be a non-buffering/tune-pending TSW
                }
            } // END consider()

            public TimeShiftWindow getBestTSW()
            {
                if (bestTSW == null)
                { // Since content is in the future, we have the option of
                  // creating a new one
                    if (log.isDebugEnabled())
                    {
                        log.debug("getTSWByDuration.durationEvaluator: No active TSW found for service - creating new TSW for  "
                                + s.getLocator());
                    }

                    // Didn't find one ready to use or recycle - create a new
                    // one
                    bestTSW = new TimeShiftWindow(TimeShiftManagerImpl.this, s);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getTSWByDuration.durationEvaluator: Found active TSW for use: " + bestTSW);
                    }
                }

                return bestTSW;
            }
        }; // END class durationEvaluator

        return getTSWByEvaluation(s, minDuration, maxDuration, uses, ru, tswcl, tswclPriority, durationEvaluator);
    } // END getTSWByDuration()

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.TimeShiftManager#getTSWByTimeSpan(javax.tv
     * .service.Service, java.util.Date, long, int,
     * org.ocap.resource.ResourceUsage,
     * org.cablelabs.impl.manager.TimeShiftWindowChangedListener)
     */
    public TimeShiftWindowClient getTSWByTimeSpan(final Service s, final long startTime, final long duration,
            final int uses, final ResourceUsage ru, final TimeShiftWindowChangedListener tswcl, int tswclPriority)
            throws NoFreeInterfaceException, IllegalStateException
    {
        if (log.isDebugEnabled())
        {
            log.debug("getTSWByTimeSpan(Service " + s.getLocator() + ",startTime " + startTime + ",duration "
                    + duration + ",uses " + useString(uses) + ",ru " + ru + ')');
        }

        final long curTime = System.currentTimeMillis();
        final long requestedStartTime = startTime;
        final long requestedEndTime = requestedStartTime + duration;

        TSWSuitabilityEvaluator timespanEvaluatorForFuture = new TSWSuitabilityEvaluator()
        {
            long mostOverlap = -1;

            TimeShiftWindow bestTSW = null;

            /**
             * Assert: tsw is not IDLE and tsw.getService() == s
             */
            public void consider(TimeShiftWindow tsw)
            {
                final TimeShiftBufferImpl tsb = tsw.getBufferingTimeShiftBuffer();
                final long curTSWsize = (tsb == null) ? 0 : tsb.bufferSize;

                final long curTSWoverlap = tsw.timeSpanOverlap(requestedStartTime, requestedEndTime);
                // Note: This will return 0 if there's no TSB

                if (log.isDebugEnabled())
                {
                    log.debug("getTSWByTimeSpan.timespanEvaluatorForFuture: overlap is " + curTSWoverlap);
                }

                final int curTSWstate = tsw.getState();

                if ((curTSWstate != TSWSTATE_IDLE) && (curTSWstate != TSWSTATE_INTSHUTDOWN)
                        && (curTSWoverlap > mostOverlap))
                {
                    bestTSW = tsw;
                    mostOverlap = curTSWoverlap;
                    // Note: This may be a non-buffering/tune-pending TSW
                }
            } // END consider()

            public TimeShiftWindow getBestTSW()
            {
                if (bestTSW == null)
                { // Since content is in the future, we have the option of
                  // creating a new one
                    // Couldn't use an existing TSW - need to create a new one
                    if (log.isDebugEnabled())
                    {
                        log.debug("getTSWByTimeSpan.timespanEvaluatorForFuture: No active TSW found for service - creating new TSW for "
                                + s.getLocator());
                    }

                    // Didn't find one ready to use or recycle - create a new
                    // one
                    bestTSW = new TimeShiftWindow(TimeShiftManagerImpl.this, s);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getTSWByTimeSpan.timespanEvaluatorForFuture: Found active TSW for use: " + bestTSW);
                    }
                }

                return bestTSW;
            }
        }; // END class timespanEvaluatorForFuture

        TSWSuitabilityEvaluator timespanEvaluatorForPast = new TSWSuitabilityEvaluator()
        {
            long mostOverlap = -1;

            TimeShiftWindow bestTSW = null;

            /**
             * Assert: tsw is not IDLE and tsw.getService() == s
             */
            public void consider(TimeShiftWindow tsw)
            {
                final TimeShiftBufferImpl tsb = tsw.getBufferingTimeShiftBuffer();
                final long curTSWsize = (tsb == null) ? 0 : tsb.bufferSize;

                final long curTSWoverlap = tsw.timeSpanOverlap(requestedStartTime, requestedEndTime);
                // Note: This will return 0 if there's no TSB

                if (log.isDebugEnabled())
                {
                    log.debug("getTSWByTimeSpan.timespanEvaluatorForPast: overlap is " + curTSWoverlap);
                }

                if ((curTSWoverlap > 0) && (curTSWoverlap > mostOverlap))
                {
                    bestTSW = tsw;
                    mostOverlap = curTSWoverlap;
                }
            } // END consider()

            public TimeShiftWindow getBestTSW()
            { // May return null (can't conjure up content from the past)
                if (bestTSW == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getTSWByTimeSpan.timespanEvaluatorForFuture: Could not find TSW with overlapping content");
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getTSWByTimeSpan.timespanEvaluatorForFuture: Found active TSW with overlapping content: "
                                + bestTSW);
                    }
                }
                return bestTSW;
            }
        };

        final long defaultDur = getDefaultDuration();
        
        if (requestedEndTime <= curTime)
        {
            return getTSWByEvaluation( s, defaultDur, defaultDur, uses, 
                                       ru, tswcl, tswclPriority, timespanEvaluatorForPast );
        }
        else
        {
            return getTSWByEvaluation( s, defaultDur, defaultDur, uses, 
                                       ru, tswcl, tswclPriority, timespanEvaluatorForFuture );
        }
    } // END getTSWByTimeSpan()

    /**
     * {@inheritDoc}
     */
    public TimeShiftWindowClient getTSWByService(final Service s, 
            final int uses, final ResourceUsage ru, final TimeShiftWindowChangedListener tswcl, int tswclPriority)
            throws NoFreeInterfaceException
    {
        if (log.isDebugEnabled())
        {
            log.debug("getTSWByService(Service " + s.getLocator()
                    + ",uses " + useString(uses) + ",ru " + ru + ')');
        }

        // This evaluator will attempt to return a TSW with the most buffered
        // content for Service s - or create a new one if none are available
        final TSWSuitabilityEvaluator durationEvaluator = new TSWSuitabilityEvaluator()
        {
            long bestDuration = -1;

            TimeShiftWindow bestTSW = null;

            /**
             * Assert: tsw is not IDLE and tsw.getService() == s
             */
            public void consider(TimeShiftWindow tsw)
            {
                final TimeShiftBufferImpl tsb = tsw.getBufferingTimeShiftBuffer();
                final long curTSWsize = (tsb == null) ? 0 : tsb.bufferSize;

                // Just going to go with the biggest TSW we can find...
                if (curTSWsize > bestDuration)
                {
                    bestTSW = tsw;
                    bestDuration = curTSWsize;
                    // Note: This may be a non-buffering/tune-pending TSW
                }
            } // END consider()

            public TimeShiftWindow getBestTSW()
            {
                if (bestTSW == null)
                { // Since content is in the future, we have the option of
                  // creating a new one
                    if (log.isDebugEnabled())
                    {
                        log.debug("getTSWByService.durationEvaluator: No active TSW found for service - creating new TSW for  "
                                + s.getLocator());
                    }

                    // Didn't find one ready to use or recycle - create a new
                    // one
                    bestTSW = new TimeShiftWindow(TimeShiftManagerImpl.this, s);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getTSWByService.durationEvaluator: Found active TSW for use: " + bestTSW);
                }
                }

                return bestTSW;
            }
        }; // END class durationEvaluator

        final long defaultDur = getDefaultDuration();
        
        return getTSWByEvaluation( s, defaultDur, defaultDur, uses, 
                                   ru, tswcl, tswclPriority, durationEvaluator );
    } // END getTSWByService()

    /**
     * @see org.cablelabs.impl.manager.TimeShiftManager#getTSWByDuration(javax.tv.service.Service,
     *      long, long, int, org.ocap.resource.ResourceUsage,
     *      org.cablelabs.impl.manager.TimeShiftWindowChangedListener, int)
     */
    public TimeShiftWindowClient getTSWByEvaluation(final Service s, long minDuration, long desiredDuration,
            final int uses, final ResourceUsage ru, final TimeShiftWindowChangedListener tswcl,
            final int tswclPriority, TSWSuitabilityEvaluator suitabilityEval) 
    throws NoFreeInterfaceException
    {
        Assert.lockNotHeld(m_sync);
        
        if (log.isDebugEnabled())
        {
            log.debug("getTSWByEvaluation(Service " + s.getLocator() 
                    + ",minDur " + minDuration 
                    + ",desiredDur " + desiredDuration
                    + ",uses " + useString(uses)
                    + ",ru " + ru + ",se " + suitabilityEval.getClass().toString() + ')');
        }

        TimeShiftWindow usableTSW = null;
        boolean tswNeedsToBeTuned = false;
        boolean notifyNewTSW = false;
        TimeShiftWindowClient tswClient = null;
        boolean foundReservationPending = false;

        //
        // We may need to release this monitor and retry acquisition - so we
        // acquire the
        // reserve monitor in a loop. This should be very, very rare...
        //
        do
        {
            synchronized (m_niReserveMonitor)
            { // Note the order of monitor acquisition - should always get the
              // ReserveMonitor first
                synchronized (m_sync)
                {
                    // Search the list for a TimeShiftWindow on Service s that
                    // can
                    // support use(s) and can support the designated duration.
                    // If usable TSW found, reserve it for uses

                    // Acceptability rules (in order):
                    // 1) The TSW is tuned to service s, is buffering, the
                    // buffering TSW is of sufficient size to handle the
                    // requested
                    // duration, and can support the requested use(s).
                    // 2) The TSW is tuned to service s, is buffering, the TSW
                    // can
                    // support the requested use(s), but the buffering TSB is
                    // not
                    // of sufficient size to handle the requested duration.
                    // Attempt
                    // to resize the buffering TSB.
                    // 3) The TSW is tuned to service s, the TSW can support the
                    // requested use(s), but is not buffering.
                    // 4) The TSW is tuned to service s, the TSW can support the
                    // requested use(s), but buffering is being shut down.
                    // 5) The TSW is not tuned to a service and has no clients
                    // attached.

                    // Flow chart:
                    // If a TSW has clients, is tuned to service s, and can
                    // support
                    // the request uses
                    // If the TSW is buffering
                    // If the buffering TSB is of sufficient size, use the TSW.
                    // If the TSW is not of sufficient size, attempt TSW resize
                    // and use the TSW. (It may go to the BUF_SHUTDOWN state)
                    // If the TSW is not buffering, use the TSW.

                    //
                    // First, we'll loop through tuned/buffering
                    // TimeShiftWindows
                    //
                    foundReservationPending = false;

                    for (final Enumeration tswe = m_timeShiftWindows.elements(); tswe.hasMoreElements();)
                    {
                        TimeShiftWindow curTSW = (TimeShiftWindow) tswe.nextElement();

                        if (log.isDebugEnabled())
                        {
                            log.debug("getTSWByEvaluation: considering " + curTSW);
                        }

                        if (curTSW.getState() == TimeShiftManager.TSWSTATE_RESERVE_PENDING)
                        {
                            foundReservationPending = true;
                            break;
                        }

                        if ((curTSW.getService().equals(s)) 
                                && (curTSW.getState() != TimeShiftManager.TSWSTATE_IDLE)
                                && (curTSW.getState() != TimeShiftManager.TSWSTATE_INTSHUTDOWN)
                                && ((curTSW.m_constraints.uses & TimeShiftWindow.exclusiveUses & uses) == 0)
                                && ((curTSW.m_constraints.reservations & TimeShiftWindow.exclusiveUses & uses) == 0))
                        { // curTSW is the right service with available use(s)
                            // and is in a usable/soon-to-be-usable state
                            suitabilityEval.consider(curTSW);
                        } // END if curTSW is right service, ok state, with
                          // available use(s)
                    } // END loop through TimeShiftWindows

                    if (!foundReservationPending)
                    {
                        usableTSW = suitabilityEval.getBestTSW();

                        if (usableTSW == null)
                        {
                            throw new NoFreeInterfaceException("No TimeShiftWindow has recorded content for service "
                                    + s.getLocator());
                        }

                        if (usableTSW.getState() == TimeShiftManager.TSWSTATE_IDLE)
                        { // A new TSW was created - get it ready to go...
                            m_timeShiftWindows.add(usableTSW);
                            // Note: the tsw will be removed from this list via
                            // natural
                            // death - even on tune failure

                            notifyNewTSW = true;
                            tswNeedsToBeTuned = true;
                        }

                        // Assert: usableTSW refs either a new or existing
                        // TimeShiftWindow
                        // Assert: usableTSW is set to service s
                        tswClient = usableTSW.addClient(minDuration, desiredDuration, uses, ru, tswcl, tswclPriority);

                        tswNeedsToBeTuned = (usableTSW.getState() == TimeShiftManager.TSWSTATE_IDLE);

                        if (tswNeedsToBeTuned)
                        {
                            // Assert: usableTSW.getState() ==
                            // TimeShiftManager.TSWSTATE_NOTTUNED

                            // Before we leave the sync block, we want to make
                            // sure that any
                            // caller looking for a TSW for Service s will find
                            // the one that's
                            // in the process of being setup and not setup a
                            // redundant tsw -
                            // using a second NI in the process. So we'll setup
                            // this new TSW
                            // for Service s in RESERVE_PENDING.

                            usableTSW.setStateNoNotify(TimeShiftManager.TSWSTATE_RESERVE_PENDING,
                                    TimeShiftManager.TSWREASON_NOREASON);
                        }
                        else
                        { // Assert: TSW already has an NI
                            // Make sure the NI's ResourceUsages are updated to reflect
                            //  the newly-added client - in case the NI goes to resource
                            //  contention prior to the client attaching
                            usableTSW.updateNIResourceUsages();
                        }

                        //
                        // Initiate the async reserve/tuning process
                        //
                        notifyOrTuneAsync(usableTSW, ru, notifyNewTSW, tswNeedsToBeTuned);

                    } // END else/if (foundReservationPending)
                } // END synchronized(m_sync)
            } // END synchronized (m_niReserveMonitor)

            if (foundReservationPending)
            {
                // If we're here, it means that we got the ReserveMonitor prior
                // to an (async) reserveNIAndTune()
                // getting the monitor. Sleep a short duration and release the
                // monitor, so it can do its thing
                // and get us out of reserve() limbo
                if (log.isDebugEnabled())
                {
                    log.debug("getTSWByEvaluation: found TSW with reserve pending - restarting search..."
                            + s.getLocator());
                }
                try
                { // Controlled via OCAP.dvr.tsb.reservependingsleep
                    Thread.sleep(m_reservePendingSleepTime);
                }
                catch (InterruptedException e)
                {
                    // Go ahead and spin again...
                }
            } // END if (foundReservationPending)
        }
        while (foundReservationPending);

        // Assert: usableTSW could support the new usage and has an NI.
        // Return the newly-added tsw client interface

        return tswClient;
    } // END getTSWByEvaluation()

    void notifyOrTuneAsync(final TimeShiftWindow tsw, final ResourceUsage ru, final boolean notifyNewTSW,
            final boolean tuneNeeded)
    {
        // We will initiate the tune and notification on another thread (if
        // needed), since we can
        // end up blocked waiting for the ResourceContentionHandler
        // to return. We don't want to hold our lock during this time
        // since the TSM can be re-entered in a variety of ways.

        // Assert: m_sync held by caller
        final CallerContext ctx = TimeShiftManagerImpl.m_ccm.getSystemContext();

        ctx.runInContextAsync(new Runnable()
        {
            public void run()
            {
                if (tuneNeeded)
                {
                    tsw.reserveNIAndTune(ru);
                }
                
                if (notifyNewTSW && (tsw.getState() != TimeShiftManager.TSWSTATE_IDLE))
                {
                    notifyListenerCreated(tsw);
                }
            }
        });
    } // END notifyOrTuneAsync()

    /**
     * @return Returns a default duration.
     */
    public long getDefaultDuration()
    {
        return m_defaultDuration;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.TimeShiftManager#getRULWithoutRecs(org.cablelabs
     * .impl.davic.net.tuning.NetworkInterfaceImpl, java.util.ArrayList,
     * org.ocap.dvr.RecordingResourceUsage[])
     */
    public Vector getRULWithoutRecs(NetworkInterfaceImpl ni, Vector rul, RecordingResourceUsage[] rrul)
    {
        if (log.isInfoEnabled())
        {
            log.info("getRULWithoutRecs(ni " + ni + ",...)");
        }

        // Note: This implementation makes assumptions about the shared
        // uses of a NetworkInterface/TSB:
        // 1) A Recording use will always be accompanied with a
        // Buffering use.
        // 2) Only 1 Recoring use can be associated with a TSW.

        synchronized (m_sync)
        {
            // First get the TSW associated with the ni (if any)
            final TimeShiftWindow tsw = getTSWByInterface(ni);
            final RecordingResourceUsage theRRU = rrul[0];

            Vector rulWithoutRecs = new Vector();

            if (tsw == null)
            { // Can't determine if some usages are just associated with recs,
                // so everything but the RRUs in the rrul are in the result
                rulWithoutRecs.addAll(rul);

                rulWithoutRecs.remove(theRRU);
            }
            else
            { // There's a TSW we can dig into and find out if it's just
                // in use for the recording(s)
                // Walk the list of TSWCs
                synchronized (tsw)
                {
                    for (final Iterator tswce = tsw.m_clients.iterator(); tswce.hasNext();)
                    {
                        TimeShiftWindowClientImpl tswci = (TimeShiftWindowClientImpl) tswce.next();

                        if ((!(tswci.m_resourceUsage != theRRU)) && rul.contains(tswci.m_resourceUsage)
                                && (tswci.m_constraints.uses & TimeShiftWindow.exclusiveUses) != 0)
                        {
                            rulWithoutRecs.add(tswci.m_resourceUsage);
                        }
                    }
                } // END synchronized (tsw)
                // If TSWC is attached for something other than recording,
                // then return rul minus the rruls
                // otherwise, return an empty
            }
        } // END synchronized(m_sync)

        return rul;
    } // END getRULWithoutRecs()

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.TimeShiftManager#canRULSupportRecording(java
     * .util.ArrayList, org.ocap.dvr.RecordingResourceUsage,
     * javax.tv.service.Service)
     */
    public boolean canRULSupportRecording(Vector rul, NetworkInterface ni, RecordingResourceUsage rru, Service s)
    {
        if (log.isInfoEnabled())
        {
            log.info("getRULWithoutRecs(...,rru " + rru + ",service " + s.getLocator() + ')');
        }

        // This function is going to be called by the ResourceContentionWarning
        // logic to determine if the recording with RecordingResourceUsage rru
        //  

        // See if there's already a recording on the RUL (service doesn't matter
        // - we can't support the recording) or an AppResourceUsage
        // If there are other uses on the same service, the recording can be
        // supported
        // If there's a TSW associated with the NI and it's waiting to die,
        // the recording can be supported

        // If RUL is empty, no problem
        if (rul.isEmpty())
        {
            return true;
        }

        Service rulService = null;

        // Check for RRUs or ARU in the list - extract the service while we're
        // at it
        for (final Enumeration rue = rul.elements(); rue.hasMoreElements();)
        {
            ResourceUsage ru = (ResourceUsage) rue.nextElement();

            if (ru instanceof RecordingResourceUsage)
            { // There's a RecordingResourceUsage in the rul - and we can't
              // support
                // more than one recording with one NI, regardless of service
                if (log.isDebugEnabled())
                {
                    log.debug("canRULSupportRecording: rul can't support second recording");
                }
                return false;
            }

            if (ru instanceof ApplicationResourceUsage)
            { // There's an ApplicationResource in the rul - and we can't
              // support
                // sharing it, regardless of service
                if (log.isDebugEnabled())
                {
                    log.debug("canRULSupportRecording: rul can't support sharing with ARU");
                }
                return false;
            }

            if (ru instanceof TimeShiftBufferResourceUsage)
            {
                rulService = ((TimeShiftBufferResourceUsage) ru).getService();
                continue;
            }

            if (ru instanceof ServiceContextResourceUsage)
            {
                rulService = ((ServiceContextResourceUsage) ru).getRequestedService();
                continue;
            }
        } // END loop through rul

        // Assert: rul doesn't contain a RRU or ARU

        if (rulService == null)
        { // It's not empty, doesn't contain an ARU, RRU
            // or a TSRU or SCRU (rulService would be non-null)
            // What the heck is in there then?
            if (log.isWarnEnabled())
            {
                log.warn("canRULSupportRecording: Didn't find a recognized ResourceUsage (RUL " + rul + ')');
            }
            return false;
        }

        if (s.equals(rulService))
        {
            // Assert: rul doesn't contain a tsru, scru, or both on the same
            // service
            if (log.isDebugEnabled())
            {
                log.debug("canRULSupportRecording: recording can share with TSRU/SCRU");
            }
            return true;
        }

        // Assert: There's an rul with a SCRU and/or TSRU on another service

        // Now start analyzing the TimeShiftWindow associated with the passed NI
        synchronized (m_sync)
        {
            final TimeShiftWindow tsw = getTSWByInterface(ni);

            if (tsw != null)
            {
                if (tsw.willGiveUpNetworkInterface())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("canRULSupportRecording: Associated TSW will give up NI");
                    }
                    return true;
                }
            }
        } // END synchronized(m_sync)

        // Assert: The TSW associated with the passed NI is not going to give
        // up the NI willingly or no NI was specified
        if (log.isDebugEnabled())
        {
            log.debug("canRULSupportRecording: Associated TSW will not give up NI");
        }
        return false;
    } // END canRULSupportRecording()

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.Manager#destroy()
     */
    public void destroy()
    {
        // TODO Auto-generated method stub

    } // END destroy()

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer("TimeShiftManagerImpl: Current TSWs:[");

        synchronized (m_sync)
        {
            for (final Enumeration tswe = m_timeShiftWindows.elements(); tswe.hasMoreElements();)
            {
                TimeShiftWindow curTSW = (TimeShiftWindow) tswe.nextElement();
                sb.append(curTSW.toString());
                if (!tswe.hasMoreElements())
                {
                    sb.append(',');
                }
            }
            sb.append(']');
        }

        return sb.toString();
    } // END toString()

    /**
     * Package-internal function.
     * 
     * Find the TSW associated with the given NetworkInterface, if any.
     * 
     * @param ni
     *            The NetworkInterface
     * @return The TimeShiftWindow using ni, or null of no TSW is using ni
     */
    TimeShiftWindow getTSWByInterface(final NetworkInterface ni)
    {
        // Assert: Caller holds tsm lock
        TimeShiftWindow usableTSW = null;

        // Find the TSW associated with the NI
        for (final Enumeration tswe = m_timeShiftWindows.elements(); tswe.hasMoreElements();)
        {
            TimeShiftWindow curTSW = (TimeShiftWindow) tswe.nextElement();

            if (log.isDebugEnabled())
            {
                log.debug("getTSWByInterface: curTSW: " + curTSW);
            }

            // No need to match 'ni' in this case
            if (curTSW.getNetworkInterface() == ni)
            {
                usableTSW = curTSW;
                break;
            }
        } // END loop through m_timeShiftWindows

        return usableTSW;
    } // END getTSWByInterface()

    /**
     * Package-internal function.
     * 
     * Get the TimeShiftWindow death time
     */
    long getTSWDeathTime()
    {
        return m_deathTime;
    }
    
    /**
     * {@inheritDoc}
     */
    public void releaseUnusedTSWResources(TimeShiftWindow tsw) 
    {
        boolean removed = false;
        synchronized (this.m_sync)
        {
            tsw.releaseUnusedResources();
            
            if (tsw.getNumberOfClients() == 0)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Releasing TSW 0x" + Integer.toHexString(tsw.hashCode()));
                }
                
                m_timeShiftWindows.remove(tsw);
                removed = true;
            }
        }
        
        if (removed)
        {
            notifyListenerDestroyed(tsw);
        }
    } // END releaseUnusedTSWResources()

    /**
     * Conditionally update the default duration and persist the value.
     * 
     */
    public void updateDefaultDuration(long duration)
    {
        // Assert: Caller holds tsm lock

        if ((duration != 0) && (duration > m_defaultDuration))
        {
            if (log.isDebugEnabled())
            {
                log.debug("setDefaultDuration: Changing default duration to " + duration + 's');
            }

            m_defaultDuration = duration;

            resizeTSBsSmallerThan(duration);

            try
            {
                savePersistentSettings();
            }
            catch (IOException ioe)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("setDefaultDuration: Could not save default duration (" + ioe + ')');
                }
        }
        }
    } // END setDefaultDuration()

    /**
     * Package-internal function.
     * 
     * Get a TimeShiftBuffer of size minSize or larger. This function may
     * allocate a new TSB or return one from the TSB Pool.
     */
    TimeShiftBufferImpl getTSBForSize(final long minSize) throws IllegalArgumentException
    {
        synchronized (m_timeShiftBufferPool)
        {
            for (final Enumeration tsbe = m_timeShiftBufferPool.elements(); tsbe.hasMoreElements();)
            {
                TimeShiftBufferImpl curTSB = (TimeShiftBufferImpl) tsbe.nextElement();
    
                if (curTSB.bufferSize >= minSize)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Retrieved " + curTSB + " from TSB pool");
                    }
                    m_timeShiftBufferPool.remove(curTSB);
                    return curTSB;
                }
            }
    
            // Assert: Didn't find one in the pool
    
            TimeShiftBufferImpl newTSB = new TimeShiftBufferImpl();
            // Note: May throw IllegalArgumentException
    
            int mpeError = newTSB.setSize(minSize);
    
            if (mpeError != TimeShiftBufferImpl.MPE_DVR_ERR_NOERR)
            {
                throw new IllegalArgumentException("Error creating " + minSize + "s TSB (MPE Error " + mpeError + ')');
            }
    
            if (log.isDebugEnabled())
            {
                log.debug("No sufficient TSB found - created " + newTSB);
            }
    
            return newTSB;
        } // END synchronized (m_timeShiftBufferPool)
    } // END getTSBForSize()

    /**
     * Make TSBs meet a minimum duration.
     * 
     * Resize any TSBs in the pool smaller than newDuration to be size
     * newDuration
     */
    void resizeTSBsSmallerThan(long newDuration)
    {
        synchronized (m_timeShiftBufferPool)
        {
            for (final Enumeration tsbe = m_timeShiftBufferPool.elements(); tsbe.hasMoreElements();)
            {
                TimeShiftBufferImpl curTSB = (TimeShiftBufferImpl) tsbe.nextElement();
    
                if (curTSB.bufferSize < newDuration)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("resizeTSBsSmallerThan: Resizing " + curTSB + " to " + newDuration + "s");
                    }
    
                    curTSB.changeSize(newDuration);
                }
            } // END for loop
        } // END synchronized (m_timeShiftBufferPool)
    } // END resizeTSBsSmallerThan()

    /**
     * Package-internal function.
     * 
     * Return the tsb to the pool or deallocate it.
     */
    void returnTSB(TimeShiftBufferImpl tsb)
    {
        synchronized (m_timeShiftBufferPool)
        {
            if (m_timeShiftBufferPool.size() < m_maxTSBPoolSize)
            {
                if (tsb.bufferSize < m_defaultDuration)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("returnTSB: Resizing " + tsb + " to " + m_defaultDuration + "s");
                    }
    
                    tsb.changeSize(m_defaultDuration);
                }
    
                // Return the TSB to the pool - after resetting it
                tsb.reset();
                m_timeShiftBufferPool.add(tsb);
    
                if (log.isDebugEnabled())
                {
                    log.debug("Returned " + tsb + " to the TSB pool");
                }
            }
            else
            {
                tsb.deleteBuffer();
    
                if (log.isDebugEnabled())
                {
                    log.debug("Deleted " + tsb + " (pool at max)");
                }
            }
        } // END synchronized (m_timeShiftBufferPool)
    } // END returnTSB()

    public Vector getAllTSWs()
    {

        return (Vector) m_timeShiftWindows.clone();
    }

    // Extremely primitive listener structure.
    // Create a new one if I ever care.
    public void setNewTimeShiftWindowListener(TimeShiftWindowListener l)
    {
        m_newTimeShiftWindowListener = l;
    }

    private void notifyListenerCreated(TimeShiftWindow tsw)
    {
        TimeShiftWindowListener l = m_newTimeShiftWindowListener;
        if (l != null)
        {
            TimeShiftWindowMonitorListener tsm = l.timeShiftWindowCreated(tsw);
            tsw.setMonitor(tsm);
        }
    }

    private void notifyListenerDestroyed(TimeShiftWindow tsw)
    {
        TimeShiftWindowListener l = m_newTimeShiftWindowListener;
        if (l != null)
        {
            l.timeShiftWindowDestroyed(tsw.getMonitor());
        }
    }
    
    public PODManager getPODManager()
    {
        return m_podm;
    }

    public static String useString(final int uses)
    {
        if (uses == 0)
        {
            return "NO USES";
        }
        
        StringBuffer sb = new StringBuffer();
        
        if ((uses & TimeShiftManager.TSWUSE_NIRES) != 0)
        {
            sb.append("NI+");
        }
        if ((uses & TimeShiftManager.TSWUSE_LIVEPLAYBACK) != 0)
        {
            sb.append("LIVEPLAYBACK+");
        }
        if ((uses & TimeShiftManager.TSWUSE_BUFFERING) != 0)
        {
            sb.append("BUFFERING+");
        }
        if ((uses & TimeShiftManager.TSWUSE_RECORDING) != 0)
        {
            sb.append("RECORDING+");
        }
        if ((uses & TimeShiftManager.TSWUSE_BUFFERPLAYBACK) != 0)
        {
            sb.append("BUFFPLAYBACK+");
        }
        if ((uses & TimeShiftManager.TSWUSE_NETPLAYBACK) != 0)
        {
            sb.append("NETPLAYBACK+");
        }
        
        // Remove the extra '+'
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
} // END class TimeShiftManagerImpl
