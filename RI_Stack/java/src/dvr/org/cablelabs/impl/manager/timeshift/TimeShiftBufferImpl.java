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

import java.util.Date;
import java.util.Enumeration;

import javax.media.TimeBase;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.locator.LocatorFactory;
import javax.tv.service.navigation.ServiceDetails;

import org.apache.log4j.Logger;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.TimeShiftBuffer;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.manager.lightweighttrigger.SequentialMediaTimeStrategy;
import org.cablelabs.impl.manager.service.SISnapshotManager;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.ocap.OcapMain;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.spi.SelectionProviderInstance;
import org.cablelabs.impl.spi.ProviderInstance.SelectionSessionWrapper;
import org.cablelabs.impl.util.GenericTimeAssociatedElement;
import org.cablelabs.impl.util.LightweightTriggerEventTimeTable;
import org.cablelabs.impl.util.LocatorFactoryImpl;
import org.cablelabs.impl.util.MediaStreamType;
import org.cablelabs.impl.util.PidMapEntry;
import org.cablelabs.impl.util.PidMapTable;
import org.cablelabs.impl.util.TimeTable;

/**
 * This class wraps the native TimeShiftBuffer and provides functions for
 * managing the TSB and the TSB buffering.
 * 
 * This class does NOT wrap presentation and conversion.
 * 
 * @author Craig Pratt
 */
class TimeShiftBufferImpl implements TimeShiftBuffer, Asserting
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(TimeShiftBufferImpl.class);

    private String logPrefix = "TimeShiftBufferImpl 0x" + Integer.toHexString(this.hashCode()) + ": ";
    
    private static final long NANOS_PER_MILLI = 1000000;

    /** Initialize the JNI code */
    private static native void nInit();

    /** On success, will save the native TSB handle in this.nativeTSBHandle */
    private native int nTSBCreate(long size);

    /** Will attempt to resize TSB with handle nativeTSBHandle */
    private native int nTSBChangeSize(int nativeTSBHandle, long size);

    /** Will delete nativeTSBHandle */
    private native int nTSBDelete(int nativeTSBHandle);

    /**
     * Will start buffering from tuner tunerID to nativeTSBHandle and set
     * this.nativeBufferingHandle to the buffering session handle on success.
     * TSB status indications will be passed to edListener. The pidMap will have
     * it's recElementaryStreamType and recPID fields set.
     */
    private native int nTSBBufferingStart(int tunerID, short ltsid, int nativeTSBHandle, EDListener edListener, PidMapTable pidMap,
            int bitrate, long desiredDuration, long maxDuration );

    /**
     * Get the start time for the TSB, in nanoseconds (1 billionth of sec).
     * returns a negative number on error.
     */
    private native long nTSBGetStartTime(int nativeTSBHandle);

    /**
     * Get the end time for the TSB, in nanoseconds (1 billionth of sec).
     * returns a negative number on error.
     */
    private native long nTSBGetEndTime(int nativeTSBHandle);

    /** Get the buffered duration for the TSB, in nanoseconds */
    private native long nTSBGetDuration(int nativeTSBHandle);

    /**
     * Attempt to change the PIDs being buffered on an active buffering session.
     * The pidMap will have it's recElementaryStreamType and recPID fields set.
     */
    private native int nTSBBufferingChangePids(int nativeTSBHandle, int nativeBufferingHandle, PidMapTable pidMap);

    /**
     * Attempt to change the maximum/desired duration on an active buffering 
     * session. 0 values indicate no change in the duration.
     */
    private native int nTSBBufferingChangeDuration( int nativeBufferingHandle, 
                                                    long desiredDuration, 
                                                    long maxDuration );
    
    /** Stop buffering */
    private native int nTSBBufferingStop(int nativeBufferingHandle);

    /**
     * Event code to indicate that this recording has run out of disk space
     * TODO: integrate Java event codes w/ MPE level definitions
     */
    static final int MPE_DVR_EVT_OUT_OF_SPACE = 0x1000;

    static final int MPE_DVR_EVT_CONVERSION_STOP = 0x1003;

    static final int MPE_DVR_EVT_PLAYBACK_PID_CHANGE = 0x1004;

    static final int MPE_DVR_EVT_SESSION_CLOSED = 0x1005;

    static final int MPE_DVR_EVT_SESSION_RECORDING = 0x1006;

    static final int MPE_DVR_EVT_SESSION_NO_DATA = 0x1007;

    static final int MPE_DVR_EVT_CCI_UPDATE = 0x1008;

    /**
     * Native DVR error codes TODO: This REALLY should to be centralized...
     */
    static final int MPE_DVR_ERR_NOERR = 0x00; // no error

    static final int MPE_DVR_ERR_INVALID_PID = 0x01; // invalid pid error

    static final int MPE_DVR_ERR_INVALID_PARAM = 0x02; // a parameter is invalid

    static final int MPE_DVR_ERR_OS_FAILURE = 0x03; // error occured at the
                                                    // OSlevel

    static final int MPE_DVR_ERR_PATH_ENGINE = 0x04;

    static final int MPE_DVR_ERR_UNSUPPORTED = 0x05;

    static final int MPE_DVR_ERR_NOT_ALLOWED = 0x06;

    static final int MPE_DVR_ERR_DEVICE_ERR = 0x07; // hardware device error

    static final int MPE_DVR_ERR_OUT_OF_SPACE = 0x08; // no more space on the
                                                      // HDD

    static final int MPE_DVR_ERR_NOT_IMPLEMENTED = 0x09;

    static final int MPE_DVR_ERR_NO_ACTIVE_SESSSION = 0x0a;

    /*
     * Properties. Package-access properties will be accessed by TimeShiftWindow
     * directly. Public-access properties will have set-ers and get-ers.
     */
    PODManager m_podm;
    
    /** Handle for the native TimeShiftBuffer. JNI code will write this field. */
    int nativeTSBHandle;

    /** Handle for the native buffering session. JNI code will write this field. */
    int nativeBufferingHandle;

    /**
     * Handle for the effective media time of nTSBBufferingChangePids. JNI code
     * will write this field.
     */
    long nativeEffectiveMediaTime;

    /** TunerID being buffered from */
    int sourceTunerID;

    /** Bitrate/quality being buffered */
    int bitrate;

    /** Minimum of content the TimeShiftBuffer must accommodate (in seconds). */
    long bufferSize;

    /** Time the TSB was started, in TimeBase time (in nanoseconds) */
    long timeBaseTimeAtBufferingStart;

    /** Time the TSB was stopped, in TimeBase time (in nanoseconds)*/
    long timeBaseTimeAtBufferingStop;

    /** Time the TSB was started, in system time (in nanoseconds)*/
    long systemTimeAtBufferingStart;

    /** Time the TSB was stopped, in system time (in milliseconds) */
    long systemTimeAtBufferingStop;
    
    /** Currently-effective desired duration (in seconds) */
    long desiredDuration;
    
    /** Currently-effective maximum duration (in seconds) */
    long maxDuration;
    
    /**
     * Components in the TSB are tracked according to their media time and are
     * contained in a PidMapTable - which maps the recorded components to the
     * original signaled components.
     * 
     * A TimeTableCollection is not used because PidMapTables are not persisted
     * at the Java level.
     */
    TimeTable pidMapTimeTable;

    /**
     * Used to store LightweightTriggerEvents and their time.
     */
    LightweightTriggerEventTimeTable lightweightTriggerEventTimeTable;

    /**
     * Used to store CCIStatusEvents and their associated times (in nanoseconds).
     */
    TimeTable cciEventTimeTable;
    
    /**
     * Used to store SISnapshotManagers (snapshots)
     */
    // TimeTable siSnapshotTimeTable;

    /* Some state tracking for use by TimeShiftWindow */
    /** Signifies that the associated TimeShiftBuffer is being recorded into */
    boolean buffering;

    /** Signifies that the associated TimeShiftBuffer is being presented from */
    boolean presenting;

    /**
     * Signifies that the associated TimeShiftBuffer is being recorded
     * (converted) from
     */
    boolean copying;

    /**
     * Indicates the reason the TSB buffering/recording process has been stopped
     */
    private int bufferingStopReason;

    /** The TSB's offset into the TimeShiftWindow, when the TSB is contained (in nanoseconds) */
    long tswOffset;

    /**
     * Static class initializer.
     */
    static
    {
        OcapMain.loadLibrary();

        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        nInit();
    }

    /**
     * Create an uninitialized TimeShiftBuffer. Memvars initialized via reset()
     * since this object, and the associated native/on-disk resources that this
     * object represents, are recycled.
     */
    TimeShiftBufferImpl()
    {
        reset();
    }

    /**
     * Set the size of the TimeShiftBuffer to the specified duration. This will
     * attempt to create a native TSB and store its native handle, if
     * successful.
     * 
     * @param duration
     *            Desired TSB duration, in seconds.
     * @throws IllegalArgumentException
     *             if the duration cannot be supported.
     */
    int setSize(final long duration) throws IllegalArgumentException
    {
        synchronized (this)
        {
            int mpeError;

            if (duration == 0)
            {
                throw new IllegalArgumentException("Invalid TSB size specified: " + duration);
            }

            mpeError = nTSBCreate(duration);

            if (mpeError == MPE_DVR_ERR_NOERR)
            {
                // Assert: nativeTSBHandle is set and refers to a TSB of given
                // duration
                this.bufferSize = duration;
            }

            return mpeError;
        } // END synchronized(this)
    } // END setSize(long duration)

    /**
     * {@inheritDoc}
     */
    public long getSize() throws IllegalStateException
    {
        return bufferSize;
    }

    /**
     * {@inheritDoc}
     */
    public long getContentStartTimeInSystemTime() throws IllegalStateException
    {
        synchronized (this)
        {
            // TODO: If the TSB is stopped, use cached start time
            if (nativeTSBHandle == 0)
            {
                throw new IllegalStateException("nativeTSBHandle is null!");
            }

            if (systemTimeAtBufferingStart == 0)
            {
                throw new IllegalStateException("Buffering never started");
            }

            final long startTime = nTSBGetStartTime(nativeTSBHandle);
            // Note: Native media start time is in nanoseconds (1 billionth of
            // sec)

            if (startTime < 0)
            {
                throw new IllegalStateException("Native TSB start time is invalid");
            }

            // Convert to milliseconds. We'll stick to integer math here
            return (systemTimeAtBufferingStart + (startTime / NANOS_PER_MILLI));
        } // END synchronized(this)
    } // END getSystemStartTime()

    /**
     * {@inheritDoc}
     */
    public long getContentEndTimeInSystemTime() throws IllegalStateException
    {
        synchronized (this)
        {
            // TODO: If the TSB is stopped, use cached end time
            if (nativeTSBHandle == 0)
            {
                throw new IllegalStateException("nativeTSBHandle is null!");
            }

            if (systemTimeAtBufferingStart == 0)
            {
                throw new IllegalStateException("Buffering never started");
            }

            long endTime;

            /*
             * currentTimeMillis is used because accuracy is not needed and
             * because native end time may not move linearly depending on
             * buffering etc. It may stay "stuck" for a while before
             * incrementing
             */
            if (buffering)
            {
                return System.currentTimeMillis();
            }
            else
            {
                endTime = nTSBGetEndTime(nativeTSBHandle);
            }
            // Note: Native media end time is in nanoseconds (1 billionth of
            // sec)

            if (endTime < 0)
            {
                throw new IllegalStateException("Native TSB end time is invalid");
            }

            // Convert to milliseconds. We'll stick to integer math here
            return (systemTimeAtBufferingStart + (endTime / NANOS_PER_MILLI));
        } // END synchronized(this)
    } // END getSystemEndTime()

    /**
     * {@inheritDoc}
     */
    public long getTimeBaseStartTime() throws IllegalStateException
    {
        return timeBaseTimeAtBufferingStart;
    }

    /**
     * {@inheritDoc}
     */
    public long getTimeBaseEndTime() throws IllegalStateException
    {
        return (isBuffering() ? Long.MAX_VALUE : timeBaseTimeAtBufferingStop);
    }

    /**
     * {@inheritDoc}
     */
    public long getSystemStartTime() throws IllegalStateException
    {
        return (systemTimeAtBufferingStart);
    }

    /**
     * {@inheritDoc}
     */
    public long getSystemEndTime() throws IllegalStateException
    {
        return (isBuffering() ? Long.MAX_VALUE : systemTimeAtBufferingStop);
    }

    /**
     * {@inheritDoc}
     */
    public long getTSWStartTimeOffset() throws IllegalStateException
    {
        return tswOffset;
    }

    /**
     * {@inheritDoc}
     */
    public long getContentStartTimeInMediaTime() throws IllegalStateException
    {
        synchronized (this)
        {
            // TODO: If the TSB is stopped, use cached start time
            if (nativeTSBHandle == 0)
            {
                throw new IllegalStateException("nativeTSBHandle is null!");
            }

            if (systemTimeAtBufferingStart == 0)
            {
                throw new IllegalStateException("Buffering never started");
            }

            final long startTime = nTSBGetStartTime(nativeTSBHandle);
            // Note: Native media start time is in nanoseconds (1 billionth of
            // sec)

            if (startTime < 0)
            {
                throw new IllegalStateException("Native TSB start time is invalid");
            }

            // Have nanoseconds, return nanoseconds
            return (startTime);
        } // END synchronized(this)
    } // END getMediaStartTime()

    /**
     * {@inheritDoc}
     */
    public long getContentEndTimeInMediaTime() throws IllegalStateException
    {
        synchronized (this)
        {
            // TODO: If the TSB is stopped, use cached end time
            if (nativeTSBHandle == 0)
            {
                throw new IllegalStateException("nativeTSBHandle is null!");
            }

            if (systemTimeAtBufferingStart == 0)
            {
                throw new IllegalStateException("Buffering never started");
            }

            final long endTime = nTSBGetEndTime(nativeTSBHandle);
            // Note: Native media end time is in nanoseconds (1 billionth of
            // sec)

            if (endTime < 0)
            {
                throw new IllegalStateException("Native TSB end time is invalid");
            }

            // Have nanoseconds, return nanoseconds
            return (endTime);
        } // END synchronized(this)
    } // END getMediaEndTime()

    /**
     * {@inheritDoc}
     */
    public long getDuration() throws IllegalStateException
    {
        synchronized (this)
        {
            // TODO: If the TSB is stopped, use cached duration
            if (nativeTSBHandle == 0)
            {
                throw new IllegalStateException("nativeTSBHandle is null!");
            }

            final long duration = nTSBGetDuration(nativeTSBHandle);

            if (duration < 0)
            {
                throw new IllegalStateException("Native TSB duration is less than 0");
            }

            return duration;
        } // END synchronized(this)
    } // END getDuration()

    /**
     * {@inheritDoc}
     */
    public int getNativeTSBHandle() throws IllegalStateException
    {
        synchronized (this)
        {
            if (nativeTSBHandle == 0)
            {
                throw new IllegalStateException("No TSB handle present");
            }

            return nativeTSBHandle;
        } // END synchronized(this)
    }

    /**
     * Start buffering into the TimeShiftBuffer from the designated tuner with
     * the designated parameters.
     * 
     * @param tunerID
     *            ID of the source tuner to buffer content from
     * @param bitrate
     *            The requested recording quality
     * @param pidMap
     *            Designates the components (e.g. PIDs) to record and a place to
     *            store the effective PIDs
     * @param service
     *            The Service to buffer
     * @param desiredDuration     
     *            duration (in seconds) that is requested for buffering.
     *            This value may be larger than the TSB's pre-allocated
     *            size (only intended for platforms with expandable/
     *            variable-rate TSBs).
     * @param maxDuration
     *            maximum duration of content (in seconds) that the
     *            platform may hold in the TSB (e.g. when buffering is
     *            restricted due to copy control). Note: This value may
     *            be smaller than the TSB's pre-allocated duration.
     * @param timeBase
     *            The timeBase to use for recording the effective TSB start/end
     *            times
     * @param tswStartTime
     *            The start time of the containing TimeShiftWindow, in the
     *            provided TimeBase
     * @param listener
     *            The ED listener to be notified of any buffering- related
     *            events.
     * 
     * @return The MPE error code
     * @throws IllegalArgumentException
     *             If any of the arguments are invalid or not supported
     * @throws IllegalStateException
     *             If there is no native TimeShiftBuffer (deleteBuffer() has
     *             been called) or the TSB is already buffering.
     */
    int startBuffering(final int tunerID, final short ltsid, final int bitrate, 
            final PidMapTable pidMap, final ServiceExt service,
            long desiredDuration, final long maxDuration, 
            EDListener listener) 
        throws IllegalArgumentException, IllegalStateException
    {
        synchronized (this)
        {
            if (pidMap == null)
            {
                throw new IllegalArgumentException("Null pid map specified");
            }

            if (nativeTSBHandle == 0)
            {
                throw new IllegalStateException("nativeTSBHandle is null!");
            }

            if (buffering == true)
            {
                throw new IllegalStateException("TimeShiftBuffer is already buffering");
            }

            // We'll set these when we get the buffering notification
            this.timeBaseTimeAtBufferingStart = 0;
            this.systemTimeAtBufferingStart = 0;
            this.tswOffset = 0;

            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + "startBuffering: Calling nTSBBufferingStart(tuner " 
                          + tunerID + ",tsb 0x" + Integer.toHexString(nativeTSBHandle)
                          + ",desiredDur " + desiredDuration + "s,MaxDur " + 
                          ((maxDuration == Long.MAX_VALUE) ? "MAX_VAL" : Long.toString(maxDuration))                
                          + "s,listener " + listener + ",pidMap " + pidMap + ')');
            }
            
            int mpeError;
            mpeError = nTSBBufferingStart(tunerID, ltsid, nativeTSBHandle, listener, 
                                          pidMap, bitrate, desiredDuration,
                                          maxDuration );

            if (mpeError == MPE_DVR_ERR_NOERR)
            {
                try
                {
                    // Assert: nativeBufferingHandle is set and refers to a TSB
                    // buffering session
                    this.sourceTunerID = tunerID;
                    this.bitrate = bitrate;
                    this.desiredDuration = desiredDuration;
                    this.maxDuration = maxDuration;
                    this.buffering = true;

                    // Add the initial PID Map to the component TimeTable
                    GenericTimeAssociatedElement elem = new GenericTimeAssociatedElement(0, pidMap);
                    this.pidMapTimeTable.addElement(elem);
                    
                    // Add initial CCI (will effectively be masked by platform CCI indication if/when provided)
                    final CopyControlInfo initialCCI 
                            = new CopyControlInfo(0, CopyControlInfo.EMI_COPY_FREELY); 
                    this.cciEventTimeTable.addElement(initialCCI);

                    // save ServiceDetailsExt from the service into the pidMap for
                    // later use do this after saving in componentTimeTable so that there is a
                    // valid pidMap there even if details cannot be saved
                    saveDetailsInPidMap(service, pidMap);
                }
                catch (Exception e)
                { // We want to make sure to terminate the native buffering session on any error
                    nTSBBufferingStop(nativeBufferingHandle);
                    nativeBufferingHandle = 0;
                    throw new IllegalStateException("Error after starting TSB: " + e.getMessage());
                }
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn(logPrefix + "startBuffering: Error starting the TSB (mpe error " + mpeError + ')');
                }
            }

            return mpeError;
        } // END synchronized(this)
    } // END startBuffering()

    public void handleBufferingStarted(final long effectiveTimeBaseTime, final long tswStartTime)
    {
        this.timeBaseTimeAtBufferingStart = effectiveTimeBaseTime;
        this.systemTimeAtBufferingStart = System.currentTimeMillis();
        this.tswOffset = effectiveTimeBaseTime - tswStartTime;
        if (log.isDebugEnabled())
        {
            log.debug(logPrefix + "notifyBufferingStarted: systemTime " 
                      + systemTimeAtBufferingStart + ",tswOffset " + tswOffset );
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isBeingCopied() throws IllegalStateException
    {
        return copying;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isBeingPlayed() throws IllegalStateException
    {
        return presenting;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isBuffering() throws IllegalStateException
    {
        return buffering;
    }

    public boolean hasContent()
    {
        return (this.systemTimeAtBufferingStart != 0);
    }

    private void saveDetailsInPidMap(final ServiceExt service, PidMapTable table) throws IllegalStateException
    {
        synchronized (this)
        {
            try
            {
                /**
                 * Check the PIDMapTable to ensure the platform has provided sane
                 * values. All AV PIDs must be recorded - with recorded PID/types
                 * provided.
                 */
                final int tsbPidMapSize = table.getSize();                
                
                for (int i = 0; i < tsbPidMapSize; i++)
                {
                    PidMapEntry tsbEntry = table.getEntryAtIndex(i);
                    
                    if ( ( (tsbEntry.getStreamType() == MediaStreamType.AUDIO)
                           || (tsbEntry.getStreamType() == MediaStreamType.VIDEO) )
                         && ( (tsbEntry.getRecordedElementaryStreamType() == PidMapEntry.ELEM_STREAMTYPE_UNKNOWN)
                              || (tsbEntry.getRecordedPID() == PidMapEntry.PID_UNKNOWN) ) )
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn(logPrefix + "Invalid TSB PID entry found in position [" + i + "]: " + tsbEntry);
                        }
                        throw new IllegalArgumentException("Invalid TSB PID entry found: " + tsbEntry);
                    }
                } // END for (tsbPidMap entries)

                /*
                 * Now create a snapshot so that information is not mutable (the
                 * values in the ServiceExt can change in the future and the
                 * code needs the values as they are right now.)
                 */
                Locator locator = null;
                if (service instanceof SPIService)
                {                                                         
                    ProviderInstance spi = (ProviderInstance) ((SPIService) service).getProviderInstance();
                    SelectionSessionWrapper session = (SelectionSessionWrapper) spi.getSelectionSession( (SPIService)service);
                    ServiceExt mappedService = session.getMappedService();                    
                    locator = mappedService.getLocator();
                }
                else
                {
                    locator = service.getLocator();
                }
                Locator[] snapshotLocators = { locator };
                SISnapshotManager snapshot = new SISnapshotManager(snapshotLocators);

                // the details array should only have one object (one locator
                // added to snapshot)
                ServiceDetails[] detailsArr = snapshot.getServiceDetails(locator);

                if (detailsArr.length == 0)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(logPrefix + " saveDetailsInPidMap failed.  ServiceDetails array is 0");
                    }
                    throw new IllegalArgumentException("ServiceDetails array == 0");
                }

                ServiceDetailsExt detailsExt = (ServiceDetailsExt) detailsArr[0];

                table.setSISnapshot(snapshot);
                table.setServiceDetails(detailsExt, true);
            }
            catch (SIRequestException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error(logPrefix + " saveDetailsInPidMap failed", e);
                }
                throw new IllegalStateException(logPrefix
                        + "saveDetailsInPidMap failed, exception in log.  Exception msg=" + e.getMessage());
            }
            catch (InterruptedException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error(logPrefix + " saveDetailsInPidMap failed", e);
                }
                throw new IllegalStateException(logPrefix
                        + "saveDetailsInPidMap failed, exception in log.  Exception msg=" + e.getMessage());
            }
            catch (InvalidLocatorException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error(logPrefix + " saveDetailsInPidMap failed", e);
                }
                throw new IllegalStateException(logPrefix
                        + "saveDetailsInPidMap failed, exception in log.  Exception msg=" + e.getMessage());
            }
        } // END synchronized (this)
    } // END saveDetailsInPidMap()

    /**
     * Stops buffering into the TSB.
     * 
     * @param timeBase
     *            The timeBase to use to record the effective stop time.
     * @param reason
     *            The reason the TSB is being stopped
     * 
     * @throws IllegalStateException
     *             If there is no native TimeShiftBuffer (deleteBuffer() has
     *             been called) or the TSB is not buffering.
     */
    int stopBuffering(final TimeBase timeBase, final int reason) throws IllegalStateException
    {
        synchronized (this)
        {
            int mpeError;

            if (nativeBufferingHandle == 0)
            {
                throw new IllegalStateException("nativeBufferingHandle is null!");
            }

            if (buffering != true)
            {
                throw new IllegalStateException("TimeShiftBuffer is not buffering");
            }

            // Assert: nativeBufferingHandle is set and refers to a TSB
            // buffering session

            mpeError = nTSBBufferingStop(nativeBufferingHandle);

            // Assert: Buffering is stopped regardless of the returned error
            // code

            this.buffering = false;
            this.sourceTunerID = 0;
            this.bitrate = 0;
            this.nativeBufferingHandle = 0;
            this.timeBaseTimeAtBufferingStop = timeBase.getNanoseconds();
            this.systemTimeAtBufferingStop = System.currentTimeMillis();
            this.bufferingStopReason = reason;

            return mpeError;
        } // END synchronized(this)
    } // END stopBuffering()

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.TimeShiftBuffer#getBufferingStopReason()
     */
    public int getBufferingStopReason()
    {
        return bufferingStopReason;
    }

    /**
     * Change the size of the TimeShiftBuffer. This operation may be attempted
     * while buffering is ongoing or not. If this operation fails, the existing
     * TSB retains its current content and duration.
     * 
     * @param newSize
     *            Desired TSB duration, in seconds.
     * @return The MPE error code
     * @throws IllegalArgumentException
     *             if the duration change cannot be supported
     * @throws IllegalStateException
     *             if there is no native TimeShiftBuffer (deleteBuffer() has
     *             been called) or no active buffering session
     */
    int changeSize(final long newSize) throws IllegalArgumentException, IllegalStateException
    {
        synchronized (this)
        {
            int mpeError;
            
            if (this.nativeTSBHandle == 0)
            {
                throw new IllegalStateException("No native TSB");
            }

            if (log.isDebugEnabled())
            {
                log.debug( logPrefix + "changeSize: Calling nTSBChangeSize(tsb 0x" 
                           + Integer.toHexString(nativeTSBHandle)
                           + ",newSize " + newSize + "s)" );
            }
            
            mpeError = nTSBChangeSize(nativeTSBHandle, newSize);

            if (mpeError == MPE_DVR_ERR_OUT_OF_SPACE)
            {
                throw new IllegalArgumentException("Insufficient disk space for TSB resize");
            }

            if (mpeError == MPE_DVR_ERR_NOERR)
            {
                this.bufferSize = newSize;
            }

            return mpeError;
        } // END synchronized(this)
    } // END changeSize()

    /**
     * Change the buffering duration properties of the TimeShiftBuffer. This 
     * operation may be attempted only while buffering is ongoing. If this 
     * operation fails, the existing TSB retains its current content and duration.
     * 
     * @param newDesiredDuration
     *            New value for the desired TSB duration, in seconds, or 0
     *            for no change.
     * @param newMaxDuration
     *            New value for the max TSB duration, in seconds, or 0 for
     *            no change.
     * @return The MPE error code
     * 
     * @throws IllegalArgumentException
     *             if the duration cannot be supported due to lack of space.
     * @throws IllegalStateException
     *             if the TimeShiftBuffer doesn't allow duration changes 
     *             while in use (e.g. buffering)
     */
    int changeBufferingDurations( final long newDesiredDuration, 
                                  final long newMaxDuration ) 
        throws IllegalArgumentException, IllegalStateException
    {
        synchronized (this)
        {
            if (this.nativeBufferingHandle == 0)
            {
                throw new IllegalStateException("TSB is not buffering");
            }

            int mpeError;

            if (log.isDebugEnabled())
            {
                log.debug( logPrefix + "changeSize: Calling nTSBBufferingChangeDuration(bufsess 0x" 
                           + Integer.toHexString(nativeBufferingHandle)
                           + ",newDesDur " + newDesiredDuration 
                           + "s,newMaxDur " + newMaxDuration + "s)" );
            }

            mpeError = nTSBBufferingChangeDuration( nativeBufferingHandle, 
                                                    newDesiredDuration, 
                                                    newMaxDuration );

            if (mpeError == MPE_DVR_ERR_NOERR)
            {
                this.desiredDuration = newDesiredDuration;
                this.maxDuration = newMaxDuration;
            }

            return mpeError;
        } // END synchronized(this)
    } // END changeBufferingDurations()

    /**
     * @param pidMap
     *            Designates the components (e.g. PIDs) to record and will also
     *            contain the components recorded to disk
     * @param service
     *            service being buffered
     * 
     * @return The MPE error code
     * @throws IllegalArgumentException
     *             if the component change cannot be supported due to invalid
     *             PIDs.
     * @throws IllegalStateException
     *             if there is no native TimeShiftBuffer (deleteBuffer() has
     *             been called) or the TimeShiftBuffer doesn't allow component
     *             changes while in use (e.g. buffering)
     */
    int changeComponents(final PidMapTable pidMap, ServiceExt service) throws IllegalArgumentException,
            IllegalStateException
    {
        synchronized (this)
        {
            if (pidMap == null)
            {
                throw new IllegalArgumentException("Null pid map specified");
            }

            if (nativeBufferingHandle == 0)
            {
                throw new IllegalStateException("nativeBufferingHandle is null!");
            }

            if (nativeTSBHandle == 0)
            {
                throw new IllegalStateException("nativeTSBHandle is null!");
            }

            if (buffering != true)
            {
                throw new IllegalStateException("TimeShiftBuffer is not buffering");
            }

            int mpeError;

            mpeError = nTSBBufferingChangePids(nativeTSBHandle, nativeBufferingHandle, pidMap);

            if (mpeError == MPE_DVR_ERR_NOERR)
            {
                // Assert: nativeBufferingHandle is set and refers to a TSB
                // buffering session
                // Add the initial PID Map to the component TimeTable

                GenericTimeAssociatedElement elem = new GenericTimeAssociatedElement(this.nativeEffectiveMediaTime,
                        pidMap);
                this.pidMapTimeTable.addElement(elem);

                // save ServiceDetailsExt from the service into the pidMap for
                // later use
                // do this after saving in componentTimeTable so that there is a
                // valid pidMap there even if details can
                // not be saved
                saveDetailsInPidMap(service, pidMap);
            }

            return mpeError;
        } // END synchronized(this)
    } // END changeComponents()

    /**
     * Reset the TSB. This will reset all values back to initial and flush the
     * contents of the TSB but leave it in a usable state.
     */
    void reset()
    {
        // TODO: Flush the contents of the native TSB (when there's a way to do
        // that)
        nativeBufferingHandle = 0;
        nativeEffectiveMediaTime = 0;
        sourceTunerID = 0;
        bitrate = 0;
        systemTimeAtBufferingStart = 0;
        systemTimeAtBufferingStop = Long.MAX_VALUE;
        timeBaseTimeAtBufferingStart = 0;
        timeBaseTimeAtBufferingStop = Long.MAX_VALUE;
        tswOffset = Long.MIN_VALUE;
        bufferingStopReason = TimeShiftManager.TSWREASON_NOREASON;

        pidMapTimeTable = new TimeTable();
        lightweightTriggerEventTimeTable = new LightweightTriggerEventTimeTable();
        cciEventTimeTable = new TimeTable();

        buffering = false;
        presenting = false;
        copying = false;
    } // END clearBuffer

    /**
     * Delete the native TimeShiftBuffer
     * 
     * @return The MPE error code
     * @throws IllegalStateException
     *             if there is no native TimeShiftBuffer (deleteBuffer() has
     *             been called) or the TimeShiftBuffer is in use (e.g.
     *             buffering)
     */
    int deleteBuffer() throws IllegalStateException
    {
        synchronized (this)
        {
            int mpeError;

            if (nativeTSBHandle == 0)
            {
                throw new IllegalStateException("nativeTSBHandle is null!");
            }

            if (buffering == true)
            {
                throw new IllegalStateException("TimeShiftBuffer is still buffering");
            }

            mpeError = nTSBDelete(nativeTSBHandle);

            nativeTSBHandle = 0;

            return mpeError;
        } // END synchronized(this)
    } // END deleteBuffer()

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.TimeShiftBuffer#getPidMapForMediaTime(long)
     */
    public PidMapTable getPidMapForMediaTime(long mediaTime) throws IllegalStateException
    {
        synchronized (this)
        {
            // The components that apply at any particular time will be the
            // components
            // at or immediately preceding the given time
            // The TimeTable is in media time - so need to translate and convert
            // to ns
            GenericTimeAssociatedElement elem = (GenericTimeAssociatedElement) pidMapTimeTable.getEntryBefore(mediaTime+1);

            PidMapTable pmt = (PidMapTable) elem.value;
            return pmt;
        } // END synchronized(this)
    } // END getPidMapForMediaTime()

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.TimeShiftBuffer#getComponentsForTime(long)
     */
    public PidMapTable getPidMapForSystemTime(long systemTime) throws IllegalStateException
    {
        synchronized (this)
        {
            // The components that apply at any particular time will be the
            // components
            // at or immediately preceeding the given time
            // The TimeTable is in media time - so need to translate and convert
            // to ns

            GenericTimeAssociatedElement elem = 
                (GenericTimeAssociatedElement) 
                pidMapTimeTable.getEntryBefore( (systemTime - this.systemTimeAtBufferingStart) 
                                                * NANOS_PER_MILLI + 1 );

            PidMapTable pmt = (PidMapTable) elem.value;
            return pmt;

        } // END synchronized(this)
    } // END getComponentsForTime()

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.TimeShiftBuffer#getComponentTimeTable()
     */
    public TimeTable getPidMapTimeTable() throws IllegalStateException
    {
        synchronized (this)
        {
            return pidMapTimeTable;
        } // END synchronized(this)
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.manager.lightweighttrigger.
     * LightweightTriggerEventStoreWrite#addLightweightTriggerEvent(long,
     * org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent)
     */
    public boolean addLightweightTriggerEvent(LightweightTriggerEvent lwte)
    {
        if (log.isDebugEnabled())
        {
            log.debug(logPrefix + ".addLightweightTriggerEvent");
        }

        synchronized (this)
        {
            Enumeration lwteEnum = getLightweightTriggerEventTimeTable().getLightWeightTriggerEvents().elements();

            while (lwteEnum.hasMoreElements())
            {
                LightweightTriggerEvent storedLwte = (LightweightTriggerEvent) lwteEnum.nextElement();
                if (storedLwte.hasSameIdentity(lwte))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(logPrefix + ".addLightweightTriggerEvent -- event already exists");
                    }
                    return false;
                }
            }

            getLightweightTriggerEventTimeTable().addLightweightTriggerEvent(lwte);
            if (log.isDebugEnabled())
            {
                log.debug(logPrefix + ".addLightweightTriggerEvent -- ttc after add="
                        + getLightweightTriggerEventTimeTable());
            }

            return true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.TimeShiftBuffer#getTimeTableCollection()
     */
    public LightweightTriggerEventTimeTable getLightweightTriggerEventTimeTable()
    {
        synchronized (this)
        {
            return lightweightTriggerEventTimeTable;
        } // END synchronized(this)
    }

    /**
     * {@inheritDoc}
     */
    public TimeTable getCCITimeTable()
    {
        synchronized (this)
        {
            return cciEventTimeTable;
        } // END synchronized(this)
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone()
     */
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("TimeShiftBuffers cannot be cloned");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable
    { // This better not happen. But to prevent TSB leaking...
        if (nativeBufferingHandle != 0)
        {
            nTSBBufferingStop(nativeBufferingHandle);
        }

        if (nativeTSBHandle != 0)
        {
            try
            {
                deleteBuffer();
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("TSB finalize(): TSB + " + toString() + " deleteBuffer threw: " + e);
                }
        }
        }
        super.finalize();
    } // END finalize()

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        synchronized (this)
        {
            try
            {
                final long timeBaseStartTime = getTimeBaseStartTime();
                final long timeBaseEndTime = getTimeBaseEndTime();
                return "TSB 0x" + Integer.toHexString(this.hashCode()) 
                        + "[size " + bufferSize + ",ntsb 0x"
                        + Integer.toHexString(nativeTSBHandle) + ",sysbufstart/end "
                        + new Date(getContentStartTimeInSystemTime()) + "/"
                        + new Date(getContentEndTimeInSystemTime()) + ",tbstart/end "
                        + ( (timeBaseStartTime < Long.MAX_VALUE) 
                            ? ((timeBaseStartTime / SequentialMediaTimeStrategy.MS_TO_NS) + "ms") 
                            : "maxval" ) 
                        + "/"
                        + ( (timeBaseEndTime < Long.MAX_VALUE)
                            ? ((timeBaseEndTime / SequentialMediaTimeStrategy.MS_TO_NS) + "ms")
                            : "maxval" )
                        + ",tswoffset "
                        + (tswOffset / SequentialMediaTimeStrategy.MS_TO_NS) + "ms" + ",bufstopreason "
                        + TimeShiftManager.reasonString[bufferingStopReason] + ']';
            }
            catch (IllegalStateException ise)
            {
                return "TSB[size " + bufferSize + ",ntsb 0x" + Integer.toHexString(nativeTSBHandle) + ",ntsb sess 0x"
                        + Integer.toHexString(nativeBufferingHandle) + ",bufstart n/a,bufend n/a]";
            }
        } // END synchronized(this)
    } // END toString()
} // END class TimeShiftBufferImpl

