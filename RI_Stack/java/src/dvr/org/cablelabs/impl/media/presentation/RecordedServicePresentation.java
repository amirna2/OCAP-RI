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

package org.cablelabs.impl.media.presentation;

import javax.media.Time;
import javax.tv.service.SIManager;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.DVRStorageManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.RecordingExt;
import org.cablelabs.impl.manager.RecordingManager;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowChangedListener;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.TimeShiftWindowStateChangedEvent;
import org.cablelabs.impl.manager.pod.CASession;
import org.cablelabs.impl.manager.pod.CASessionListener;
import org.cablelabs.impl.manager.recording.RecordedServiceImpl;
import org.cablelabs.impl.manager.recording.RecordingImplInterface;
import org.cablelabs.impl.media.access.CASessionMonitor;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.mpe.DVRAPI;
import org.cablelabs.impl.media.mpe.DVRAPIImpl;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.player.AbstractDVRServicePlayer;
import org.cablelabs.impl.media.player.BroadcastAuthorization;
import org.cablelabs.impl.media.protocol.recording.DataSource;
import org.cablelabs.impl.media.session.DVRSession;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.media.session.NoSourceException;
import org.cablelabs.impl.media.session.RecordingSession;
import org.cablelabs.impl.media.source.DVRDataSource;
import org.cablelabs.impl.pod.mpe.CASessionEvent;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceChangeEvent;
import org.cablelabs.impl.service.ServiceChangeListener;
import org.cablelabs.impl.service.ServiceChangeMonitor;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.util.TimeTable;
import org.dvb.application.AppID;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.net.OcapLocator;
import org.ocap.service.AlternativeContentErrorEvent;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;

public class RecordedServicePresentation extends AbstractDVRServicePresentation
{
    /**
     * logging object
     */
    private static final Logger log = Logger.getLogger(RecordedServicePresentation.class);

    private static final int HEX_BASE = 16;

    private RecordingMonitor recordingMonitor = new RecordingMonitor();

    private TimeShiftWindowClient inProgressLivePointTimeShiftWindowClient = null;

    private ServiceDetailsExt initialServiceDetails;

    private final NetworkConditionMonitor networkConditionMonitor;
    private final ServiceChangeMonitor serviceChangeMonitor;
    private final CASessionMonitor caSessionMonitor;
    private TimeShiftWindowChangedListener timeShiftWindowChangedListener;

    public RecordedServicePresentation(DVRServicePresentationContext pc,
            boolean showVideo, Selection initialSelection, boolean startLive, ScalingBounds bounds, Time startMediaTime, float startRate)
    {
        super(pc, showVideo, initialSelection, startLive, bounds, startMediaTime, startRate);
        NetworkConditionListener networkConditionListener = new NetworkConditionListenerImpl();
        ServiceChangeListener serviceChangeListener = new ServiceChangeListenerImpl();
        //networkConditionMonitor cleanup removes the reference to the listener - only use for sync indications, not remap
        //remap will be provided by the TSW listener
        //TSW not_ready_to_buffer won't notify if other conditions prevent buffering, so TSW listener can't provide sync notifications
        networkConditionMonitor = new NetworkConditionMonitor(getLock(), networkConditionListener, true);
        serviceChangeMonitor = new ServiceChangeMonitor(getLock(), serviceChangeListener);
        caSessionMonitor = new CASessionMonitor(getLock(), new CASessionListenerImpl());
        initialServiceDetails = initialSelection.getServiceDetails();

        timeShiftWindowChangedListener = new TimeShiftWindowChangedListenerImpl();
        if (log.isDebugEnabled())
        {
            log.debug(this + " RecordedServicePresentation constructor: Recording "
                    + getRecordingDataSource().getRecording());
        }

        // Register listener for recording change events.
        RecordingExt recording = getRecordingDataSource().getRecording();
        if (recording != null)
        {
            recording.addRecordingUpdateListener(recordingMonitor);
            DVRStorageManager dsm = (DVRStorageManager) ManagerManager.getInstance(org.cablelabs.impl.manager.StorageManager.class);
            dsm.addMediaStorageVolumeUpdateListener(recordingMonitor);
        }
    }

    protected CreateSessionResult createTimeShiftSession(Selection selection, Time mediaTime, float rate)
            throws NoSourceException
    {
        synchronized (getLock())
        {
            if (selection == null || mediaTime == null)
            {
                throw new IllegalArgumentException(
                        "createTimeShiftSession called with null selection or mediatime - selection: " + selection
                                + ", media time: " + mediaTime);
            }

            if (log.isDebugEnabled())
            {
                log.debug("createTimeShiftSession(time=" + mediaTime.getSeconds() + "s, rate=" + rate + ")");
            }

            TimeTable cciTimeTable = ((DataSource) context.getSource()).getOcapRecordedServiceExt().getCCITimeTable();

            //presenting a recording (unsegmented) - use mediatime to find timetable entries
            CopyControlInfo activeEntry = (CopyControlInfo) cciTimeTable.getEntryBefore(mediaTime.getNanoseconds() + 1);

            byte cci = activeEntry != null ? activeEntry.getCCI() : CopyControlInfo.EMI_COPY_FREELY;
            if (log.isInfoEnabled())
            {
                log.info("createTimeShiftSession - initial CCI for mediatime: " + mediaTime + ": " + cci);
            }

            nextCCIUpdateAlarmMediaTimeNanos = DVRAPI.ALARM_MEDIATIME_NANOS_NOT_SPECIFIED;

            if (rate > 0)
            {
                CopyControlInfo nextEntry = (CopyControlInfo) cciTimeTable.getEntryAfter(mediaTime.getNanoseconds());
                if (nextEntry != null)
                {
                    nextCCIUpdateAlarmMediaTimeNanos = nextEntry.getTimeNanos();
                    if (log.isInfoEnabled())
                    {
                        log.info("createTimeShiftSession - positive rate and CCI in direction of play - initial alarm nanos: " + nextCCIUpdateAlarmMediaTimeNanos);
                    }
                }
            }
            else if (rate < 0)
            {
                CopyControlInfo nextEntry = (CopyControlInfo) cciTimeTable.getEntryBefore(mediaTime.getNanoseconds());
                if (nextEntry != null)
                {
                    //ignore alarm at zero
                    if (nextEntry.getTimeNanos() != 0)
                    {
                        nextCCIUpdateAlarmMediaTimeNanos = nextEntry.getTimeNanos();
                        if (log.isInfoEnabled())
                        {
                            log.info("createTimeShiftSession - negative rate and CCI in direction of play - initial alarm nanos: " + nextCCIUpdateAlarmMediaTimeNanos);
                        }
                    }
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("createTimeShiftSession - zero rate - initial alarm nanos: " + nextCCIUpdateAlarmMediaTimeNanos);
                }
            }

            return new CreateSessionResult(false, mediaTime, rate, new RecordingSession(getLock(), sessionListener, selection.getServiceDetails(), getVideoDevice(),
                    ((DataSource) context.getSource()).getOcapRecordedServiceExt(), mediaTime, rate, context.getMute(), context.getGain(), cci, nextCCIUpdateAlarmMediaTimeNanos));
        }
    }

    protected boolean validateResources()
    {
        synchronized (getLock())
        {
            // don't validate 'resources' (network) unless we're presenting live from the NetworkInterface
            if (isLive && !isPresentLiveFromBuffer)
            {
                if (networkConditionMonitor.isNetworkSyncLost())
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("initial network sync lost");
                    }
                    switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.TUNING_FAILURE);
                    return false;
                }
                if (serviceChangeMonitor.isPMTRemoved())
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("pmt is removed");
                    }
                    switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                    return false;
                }
            }
        }
        return super.validateResources();
    }

    protected void initializeResources(Selection selection)
    {
        synchronized (getLock())
        {
            // don't initialize 'resource' (network) monitoring unless we're
            // presenting live from the NetworkInterface
            if (isLive && !isPresentLiveFromBuffer)
            {
                //ensure a live-point timeshiftwindowclient is available if presenting at the live point
                if (inProgressLivePointTimeShiftWindowClient == null)
                {
                    inProgressLivePointTimeShiftWindowClient = ((RecordingImplInterface) getRecordingDataSource().getRecording())
                       .getNewTSWClient(TimeShiftManager.TSWUSE_LIVEPLAYBACK, timeShiftWindowChangedListener);
                }
                networkConditionMonitor.initialize(getNetworkInterface());
                serviceChangeMonitor.initialize(selection.getServiceDetails());
            }
            super.initializeResources(selection);
        }
    }

    protected void releaseResources(boolean shuttingDown)
    {
        synchronized(getLock())
        {
            //only shut down the CASessionMonitor and NetworkConditionMonitor if presentation is shutting down
            //NetworkConditionMonitor is needed to recover from SPI-initiated service changes
            if (shuttingDown)
            {
                caSessionMonitor.cleanup();
                networkConditionMonitor.cleanup();
            }
            if (!isPresentLiveFromBuffer)
            {
                //may be attached for live use -detach here, not in doStop..
                detachForLiveUse();
            }

            serviceChangeMonitor.cleanup();
            super.releaseResources(shuttingDown);
        }
    }

    protected void handleServiceChangeEventAsync(int event)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("service change event: " + eventToString(event) + ", mediatime: "
                        + getMediaTime());
            }

            // note: PAT removal will trigger PMT removal and generate alt
            // content with the same reason code,
            // not adding a separate check for that
            switch (event)
            {
                case ServiceChangeEvent.PMT_REMOVED:
                    handlePMTRemovedNotification();
                    return;
                case ServiceChangeEvent.PMT_CHANGED:
                    handlePMTChangedNotification();
                    return;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unknown service change event type - ignoring: " + event);
                    }
            }
        }
    }

    protected void handleNetworkConditionEventAsync(int event)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("network condition event: " + eventToString(event) + ", mediatime: "
                        + getMediaTime());
            }

            switch (event)
            {
                case NetworkConditionEvent.TUNE_SYNC_LOST:
                    handleTuneLockLostNotification();
                    break;
                case NetworkConditionEvent.TUNE_SYNC_ACQUIRED:
                    handleTuneLockAcquiredNotification();
                    break;
                case NetworkConditionEvent.RETUNE_PENDING:
                    //ignore - will come from the TSW listener
                    break;
                case NetworkConditionEvent.RETUNE_FAILED:
                    //ignore - will come from the TSW listener
                    break;
                case NetworkConditionEvent.UNTUNED:
                    //ignore - will come from the TSW listener
                    break;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unknown network condition event type - ignoring: " + event);
                    }
            }
        }
    }

    private void handleTuneLockLostNotification()
    {
        synchronized (getLock())
        {
            if (isLive)
            {
                if (log.isInfoEnabled())
                {
                    log.info("tune lock lost - triggering switchToAlternativeContent");
                }
                switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.TUNING_FAILURE);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring tune lock lost when not live");
                }
            }
        }
    }

    private void handleTuneLockAcquiredNotification()
    {
        synchronized (getLock())
        {
            if (isLive)
            {
                if (log.isInfoEnabled())
                {
                    log.info("tune lock acquired - triggering reselection");
                }
                reselect(SelectionTrigger.RESOURCES);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring tune lock acquired when not live");
                }
            }
        }
    }

    private void handlePMTRemovedNotification()
    {
        synchronized (getLock())
        {
            if (isLive)
            {
                if (log.isInfoEnabled())
                {
                    log.info("pmt removed - triggering switchToAlternativeContent");
                }
                switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring pmt removed when not live");
                }
            }
        }
    }

    private void handlePMTChangedNotification()
    {
        synchronized (getLock())
        {
            if (isLive)
            {
                if (log.isInfoEnabled())
                {
                    log.info("pmt changed - triggering reselection");
                }
                reselect(MediaPresentationEvaluationTrigger.PMT_CHANGED);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("ignoring pmt changed when not live");
                }
            }
        }
    }

    public void presentTimeShiftSession(DVRSession session, Selection selection, Time mediaTime, float rate, boolean mediaTimeChangeRequest)
            throws NoSourceException, MPEException
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("presentTimeShiftSession: " + session + ", " + selection + ", mediatime: " + mediaTime + ", rate: " + rate);
            }
            session.present(selection.getServiceDetails(), selection.getElementaryStreams(null,
                    selection.getServiceComponents()));
            Time nativeMT = session.getMediaTime();
            float nativeRate = session.getRate();
            context.clockSetMediaTime(nativeMT, mediaTimeChangeRequest);
            context.clockSetRate(nativeRate, false);
        }
    }

    public void updateTimeShiftSession(DVRSession session, Selection selection) throws NoSourceException, MPEException
    {
        synchronized (getLock())
        {
            session.updatePresentation(context.getClock().getMediaTime(), selection.getElementaryStreams(null,
                    selection.getServiceComponents()));
            // no need to update clock since mediatime and rate didn't change
        }
    }

    protected long getStartMediaTimeNanos() {
        RecordedServiceImpl rs = (RecordedServiceImpl) getDVRDataSource().getService();
        return rs.getFirstMediaTime().getNanoseconds();
    }

    public void switchToLive(MediaPresentationEvaluationTrigger trigger, boolean mediaTimeChangeRequest)
    {
        synchronized(getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("switchToLive - trigger: " + trigger + ", live: " + isLive);
            }
            //may be called due to disabling buffering - set rate to zero if at live point and buffering isn't enabled
            Time liveMediaTime = getDVRDataSource().getLiveMediaTime();
            if (getRecordingDataSource().recordingInProgress())
            {
                if (isPresentLiveFromBuffer)
                {
                    //no need to update uses - staying in the buffer
                    //set rate before an attempt to set live media time
                    float result = setSessionRate(1.0F);
                    context.clockSetRate(result, false);
                    getDVRContext().notifyEndOfContent(result);
                    setSessionMediaTime(liveMediaTime, mediaTimeChangeRequest);
                    setLive(true);
                    getDVRContext().notifyEnteringLiveMode();
                }
                else
                {
                    getDVRContext().notifyEndOfContent(1.0F);
                    attachForLiveUse();
                    super.switchToLive(trigger, mediaTimeChangeRequest);
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("switchToLive - recording not in progress - notifying end of content");
                }
                getDVRContext().notifyEndOfContent(0.0F);
                float result = setSessionRate(0.0F);
                context.clockSetRate(result, false);
                setSessionMediaTime(liveMediaTime, mediaTimeChangeRequest);
            }
        }
    }

    protected void switchToTimeshift(Time mediaTime, float rate, boolean rateChangeRequest, boolean mediaTimeChangeRequest)
    {
        synchronized(getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("switchTotimeShift - time: " + mediaTime + ", rate: " + rate);
            }
            if (!isPresentLiveFromBuffer)
            {
                detachForLiveUse();
            }
            super.switchToTimeshift(mediaTime, rate, rateChangeRequest, mediaTimeChangeRequest);
        }
    }

    private void detachForLiveUse()
    {
        try
        {
            if (inProgressLivePointTimeShiftWindowClient != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("detachForLiveUse");
                }
                inProgressLivePointTimeShiftWindowClient.release();
                inProgressLivePointTimeShiftWindowClient = null;
            }
        }
        catch (IllegalStateException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to detach live use", e);
            }
        }
        catch (IllegalArgumentException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to detach live use", e);
            }
        }
    }

    private void attachForLiveUse()
    {
        // failure to attach is not an error
        if (log.isDebugEnabled())
        {
            log.debug("attachForLiveUse");
        }
        try
        {
            if (inProgressLivePointTimeShiftWindowClient == null)
            {
                inProgressLivePointTimeShiftWindowClient = ((RecordingImplInterface) getRecordingDataSource().getRecording())
                                                           .getNewTSWClient(TimeShiftManager.TSWUSE_LIVEPLAYBACK, timeShiftWindowChangedListener);
            }

            if ((inProgressLivePointTimeShiftWindowClient.getUses() & TimeShiftManager.TSWUSE_LIVEPLAYBACK) == 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("attaching for live use");
                }
                inProgressLivePointTimeShiftWindowClient.attachFor(TimeShiftManager.TSWUSE_LIVEPLAYBACK);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("already attached for live use");
                }
            }
        }
        catch (IllegalStateException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Unable to attach for live use (not fatal) - " + e.getMessage());
            }
        }
        catch (IllegalArgumentException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Unable to attach for live use (not fatal) - " + e.getMessage());
            }
        }
    }

    /**
     * This function returns the media time of the closest renderable frame to
     * the given mediaTime in the given direction. It is expected that calling
     * mpeos_dvrPlaybackGetTime() after calling mpeos_dvrPlaybackSetTime() with
     * a value returned from this function will result in the same value
     * returned in frameTime.
     */
    public Time getMediaTimeForFrame(Time mt, int direction)
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("RecordedServicePresentation::getMediaTimeForFrame(time=" + mt.getSeconds() + ", direction="
                        + direction + ")");
            }

            if (null == this.recordingName)
            {
                DVRDataSource ds = this.getDVRDataSource();
                RecordedServiceImpl rs = (RecordedServiceImpl) ds.getService();
                this.recordingName = rs.getNativeName();
            }

            return DVRAPIImpl.getInstance().getRecordingMediaTimeForFrame(recordingName, mt, direction);
        }
    }

    protected void setSessionMediaTime(Time mediaTime, boolean postMediaTimeSetEvent)
    {
        synchronized (getLock())
        {
            if (!isSessionStarted())
            {
                if (log.isInfoEnabled())
                {
                    log.info("setSessionMediaTime: " + mediaTime + " - session is not started - updating clock");
                }
                //recording starts at zero (no segments) - just setting session relative mediatime
                context.clockSetMediaTime(mediaTime, postMediaTimeSetEvent);
                return;
            }

            if (Asserting.ASSERTING)
            {
                Assert.preCondition(currentSession != null);
            }

            try
            {
                currentSession.setMediaTime(mediaTime);
                context.clockSetMediaTime(mediaTime, postMediaTimeSetEvent);
                evaluateAndSetCCIMediaAlarm(context.getClock().getRate(), mediaTime.getNanoseconds());
            }
            catch (MPEException x)
            {
                closePresentation(x.toString(), x);
                if (log.isWarnEnabled())
                {
                    log.warn("setSessionMediaTime - unable to set mediatime");
                }
            }
        }
    }

    protected Time getSessionMediaTime()
    {
        synchronized (getLock())
        {
            if (!isSessionStarted())
            {
                if (log.isTraceEnabled())
                {
                    log.trace("getSessionMediaTime - session is not started - returning null");
                }
                return null;
            }

            if (Asserting.ASSERTING)
            {
                Assert.preCondition(currentSession != null);
            }

            Time sessionTime = null;
            try
            {
                sessionTime = currentSession.getMediaTime();
            }
            catch (MPEException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Unable to get media time", e);
                }
            }
            return sessionTime;
        }
    }

    protected TimeTable getCCITimeTableForCurrentSession()
    {
        return ((DataSource) context.getSource()).getOcapRecordedServiceExt().getCCITimeTable();
    }

    protected float setSessionRate(float rate)
    {
        synchronized (getLock())
        {
            if (!isSessionStarted())
            {
                float newRate = context.getClock().getRate();
                if (log.isInfoEnabled())
                {
                    log.info("setSessionRate: " + rate + " - session is not started - returning player clock rate: "
                            + newRate);
                }
                return newRate;
            }

            if (Asserting.ASSERTING)
            {
                Assert.preCondition(currentSession != null);
            }

            try
            {
                float result = currentSession.setRate(rate);
                if (log.isDebugEnabled())
                {
                    log.debug("setSessionRate - result: " + result);
                }
                long sessionMediaTimeNanos = getSessionMediaTime().getNanoseconds(); 
                evaluateAndSetCCIMediaAlarm(result, sessionMediaTimeNanos);
                return result;
            }
            catch (MPEException x)
            {
                closePresentation(x.toString(), x);
            }

            // unexpected
            float newRate = context.getClock().getRate();
            if (log.isWarnEnabled())
            {
                log.warn("setSessionRate - unable to set the rate on the session - returning player clock rate: "
                        + newRate);
            }
            return newRate;
        }
    }

    /*
     * This should be registered when decoding starts the first time. It is
     * supposed to monitor when the recording completes. If the decoder is in
     * live mode, then it should stop decoding.
     */
    class RecordingMonitor implements RecordingExt.RecordingUpdateListener,
            DVRStorageManager.MediaStorageVolumeUpdateListener
    {

        // RecordingUpdateChangeListener interface methods:

        /**
         * Notifies the listener that the ongoing recording process is being
         * terminated. Any uses of recording owned resources (NetworkInterface)
         * must be terminated.
         */
        public void notifyRecordingTermination(final RecordingExt recording)
        {
            synchronized (getLock())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(this + " - notifyRecordingTermination called for recording: " + recording);
                }

                // Make sure this is for an active recording monitor.
                if (this != recordingMonitor)
                {
                    return;
                }

                // Only care if in live mode. (This should only be called in
                // live mode
                // since the listener is only registered while live. )
                if (!isLive)
                {
                    return;
                }

                // If the event is for this recording...
                if (getRecordingDataSource() != null && recording == getRecordingDataSource().getRecordingRequest())
                {
                    // If recording is not in progress, decoder should stop
                    // decoding,
                    // send events, and switch to stopped state.
                    if (!getRecordingDataSource().recordingInProgress())
                    {
                        // Unregister the listener so we don't get any more
                        // notifications.
                        // It will become registered again when switching to
                        // live mode.

                        recording.removeRecordingUpdateListener(this);
                        getStorageManager().removeMediaStorageVolumeUpdateListener(this);
                        recordingMonitor = null;

                        // Stop the live decoding and send a Stop event.
                        getDVRContext().notifyNoSource("recording deleted", null);
                    }
                }
            }
        }

        /**
         * Notifies the listener that the recording is being disabled by the
         * system. Ongoing uses (playback) of this recording must be terminated.
         */
        public void notifyRecordingDisable(final RecordingExt recording)
        {
            synchronized (getLock())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(this + " - notifyRecordingDisable called for " + recording);
                }

                // Make sure this is for an active recording monitor.
                if (this != recordingMonitor)
                {
                    return;
                }

                // Only care if in live mode. (This should only be called in
                // live mode
                // since the listener is only registered while live. )
                if (!isLive)
                {
                    return;
                }

                // If the event is for this recording...
                if (getRecordingDataSource() != null && recording == getRecordingDataSource().getRecordingRequest())
                {
                    // If recording is not in progress, decoder should stop
                    // decoding,
                    // send events, and switch to stopped state.
                    if (!getRecordingDataSource().recordingInProgress())
                    {
                        recording.removeRecordingUpdateListener(this);
                        getStorageManager().removeMediaStorageVolumeUpdateListener(this);
                        recordingMonitor = null;

                        // Stop the live decoding and send a Stop event.
                        getDVRContext().notifyNoSource("recording deleted", null);
                    }
                }
            }
        }

        // Description copied from MediaStorageVolumeUpdateListener public void
        // notifyVolumeAccessStateChanged(MediaStorageVolume msv)
        public void notifyVolumeAccessStateChanged(MediaStorageVolume msv)
        {
            synchronized (getLock())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(this + " - notifyVolumeAccessStateChanged called for " + msv);
                }

                // Verify that we still have access to the recording's media
                // volume
                // FIXME: For ER3, only a complete disable is supported. In
                // future,
                // we'll need to pass in the organization of the owner of this
                // player
                RecordingExt recording = (getRecordingDataSource() != null ? getRecordingDataSource().getRecording()
                        : null);

                //
                if (recording != null && !recording.getDestination().hasAccess(recording.getAppID()))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(this + " - access disabled detected");
                    }

                    // Unregister the listener so we don't get any more
                    // notifications.
                    // It will become registered again when switching to live
                    // mode.
                    recording.removeRecordingUpdateListener(this);
                    getStorageManager().removeMediaStorageVolumeUpdateListener(this);
                    recordingMonitor = null;

                    // Stop the live decoding and send a Stop event.
                    getDVRContext().notifyNoSource("recording deleted", null);
                }

            }
        }

        // Description copied from MediaStorageVolumeUpdateListener
        public void notifySpaceAvailable(MediaStorageVolume msv)
        {
            // Ignore space change notifications - shouldn't impact recording
            // playback
        }

    }

    protected void doStart()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("doStart");
            }
            // Determine if the application has rights to view the recording
            DataSource recordingDataSource = getRecordingDataSource();
            if (recordingDataSource != null)
            {
                RecordingRequest recordingRequest = recordingDataSource.getRecordingRequest();
                if (recordingRequest != null)
                {
                    RecordingSpec recordingSpec = recordingRequest.getRecordingSpec();
                    if (recordingSpec != null)
                    {
                        OcapRecordingProperties recordingProperties = (OcapRecordingProperties) recordingSpec.getProperties();
                        if (recordingProperties != null)
                        {
                            String organization = recordingProperties.getOrganization();
                            CallerContext cc = context.getOwnerCallerContext();
                            AppID id = (AppID) cc.get(CallerContext.APP_ID);
                            if (organization == null)
                            {
                                recordingDataSource.getRecording().addPlayer((AbstractDVRServicePlayer) getDVRContext());
                                super.doStart();
                            }
                            else
                            {
                                organization = organization.trim();
                                int orgId = Integer.parseInt(organization, HEX_BASE);
                                if (id.getOID() == orgId)
                                {
                                    recordingDataSource.getRecording().addPlayer((AbstractDVRServicePlayer) getDVRContext());
                                    super.doStart();
                                }
                            }
                        }
                        else
                        {
                            throw new IllegalStateException("recording properties was null - unable to start - datasource: "
                                        + recordingDataSource + ", recording request: " + recordingRequest
                                        + ", recording spec: " + recordingSpec);
                        }
                    }
                    else
                    {
                        throw new IllegalStateException("recording spec was null - unable to start - datasource: " + recordingDataSource
                                    + ", recording request: " + recordingRequest);
                    }

                }
                else
                {
                    throw new IllegalStateException("recording request was null - unable to start - datasource: " + recordingDataSource);
                }
            }
            else
            {
                throw new IllegalStateException("recordingDataSource was null - unable to start");
            }
        }
    }

    protected void doStopInternal(boolean shuttingDown)
    {
        synchronized (getLock())
        {
            final RecordingExt recording = (getRecordingDataSource() != null ? getRecordingDataSource().getRecording() : null);
            // Remove recording listener.
            if (recordingMonitor != null)
            {
                if (recording != null)
                {
                    recording.removeRecordingUpdateListener(recordingMonitor);
                    getStorageManager().removeMediaStorageVolumeUpdateListener(recordingMonitor);
                    recordingMonitor = null;
                }
            }
            if (recording != null && shuttingDown)
            {
                CallerContextManager ccManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                ccManager.getSystemContext().runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        recording.removePlayer((AbstractDVRServicePlayer) getDVRContext());
                    }
                });
            }
            // Call parent doStopInternal().
            super.doStopInternal(shuttingDown);
        }
    }

    protected OcapRecordingManager getRecordingManager()
    {
        RecordingManager mgr = (RecordingManager) ManagerManager.getInstance(RecordingManager.class);
        return (OcapRecordingManager) mgr.getRecordingManager();
    }

    protected DataSource getRecordingDataSource()
    {
        synchronized (getLock())
        {
            return (DataSource) getDVRContext().getSource();
        }
    }

    protected void handleEndOfFile()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("handleEndOfFile");
            }
            if (getRecordingDataSource().recordingInProgress())
            {
                if (log.isInfoEnabled())
                {
                    log.info("recording in progress - switching to live");
                }
                switchToLive(DVRSessionTrigger.MODE, false);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("recording not in progress - notifying endOfContent and setting player rate to zero");
                }
                getDVRContext().notifyEndOfContent(0.0F);
                //no need to update presentation point - at end of file, set rate to zero
                context.clockSetRate(0.0F, false);
            }
        }
    }

    /**
     * Authorization -is- required for broadcast service presentation
     *
     * @return
     */
    protected boolean mediaAccessAuthorizationRequired()
    {
        return isLive;
    }

    // we may come here on an initial recorded service start, or from a switch
    // to live
    protected void updateSelectionDetails(Selection selection, Time mediaTime, float rate)
    {

        if (log.isInfoEnabled())
        {
            log.info("updateSelectionDetails - selection: " + selection + ", mediaTime: " + mediaTime + ", rate: "
                    + rate);
        }
        if (isLive && getRecordingDataSource().recordingInProgress())
        {
            if (inProgressLivePointTimeShiftWindowClient == null)
            {
                inProgressLivePointTimeShiftWindowClient = ((RecordingImplInterface) getRecordingDataSource().getRecording())
                                                           .getNewTSWClient(TimeShiftManager.TSWUSE_LIVEPLAYBACK, null);
                if (log.isInfoEnabled())
                {
                    log.info("getting new TimeShiftWindowclient: " + inProgressLivePointTimeShiftWindowClient);
                }
            }
            // Notify that end of content was reached (prior to starting the
            // session, since this implies rate 0)
            ServiceExt service = inProgressLivePointTimeShiftWindowClient.getService();
            // update current selection to use broadcast SI and service details
            try
            {
                ServiceDetailsExt updatedDetails = (ServiceDetailsExt) service.getDetails();
                selection.update((SIManagerExt) SIManager.createInstance(), updatedDetails);
                if (log.isInfoEnabled())
                {
                    log.info("updated selection with default SIManager and service details: " + updatedDetails);
                }
            }
            catch (SIRequestException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to retrieve service details", e);
                }
            }
            catch (InterruptedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to retrieve service details", e);
                }
            }

        }
        else
        {
            // we'll use initial service details to present the recorded service
            // (default SIManager)
            selection.update((SIManagerExt) SIManager.createInstance(), initialServiceDetails);
            if (log.isInfoEnabled())
            {
                log.info("updated selection with null SIManager and service details: " + initialServiceDetails);
            }
        }
    }

    protected void updateMediaAccessAuthorization(Selection selection)
    {
        if (isLive)
        {
            if (log.isDebugEnabled())
            {
                log.debug("updateMediaAccessAuthorization: " + selection);
            }
            selection.setMediaAccessAuthorization(((ServicePresentationContext) context).getBroadcastAuthorization().verifyMediaAccessAuthorization(
                    selection, doGetNetworkInterface()));
            if (log.isInfoEnabled())
            {
                log.info("selection after media access authorization: " + selection);
            }
        }
    }

    protected boolean conditionalAccessAuthorizationRequired()
    {
        return isLive;
    }

    protected void updateConditionalAccessAuthorization(Selection selection) throws MPEException
    {
        if (isLive)
        {
            if (log.isDebugEnabled())
            {
                log.debug("updateConditionalAccessAuthorization: " + selection);
            }
            //component validation (and retrieval) happens prior to CA - use current components
            BroadcastAuthorization broadcastAuthorization = ((ServicePresentationContext) context).getBroadcastAuthorization();
            ServiceComponentExt[] components = selection.getCurrentComponents();
            MediaPresentationEvaluationTrigger trigger = selection.getTrigger();
            boolean startNewSession = (trigger == MediaPresentationEvaluationTrigger.PMT_CHANGED) ||
                    (trigger == MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE) ||
                    (trigger == MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE_COMPONENTS) ||
                    (trigger == SelectionTrigger.SERVICE_CONTEXT_RESELECT);
            selection.setConditionalAccessAuthorization(broadcastAuthorization.verifyConditionalAccessAuthorization(selection.getServiceDetails(),
                doGetNetworkInterface(), selection.getElementaryStreams(doGetNetworkInterface(), components),
                components, caSessionMonitor, selection.isDefault(), (OcapLocator) selection.getServiceDetails().getService().getLocator(),
                selection.getTrigger(), selection.isDigital(), startNewSession));
        }
        if (log.isInfoEnabled())
        {
            log.info("selection after conditional access authorization: " + selection);
        }
    }

    public void switchToAlternativeContent(int alternativeContentMode, Class alternativeContentClass, int alternativeContentReasonCode)
    {
        synchronized(getLock())
        {
            // remove live use only if decode is stopping
            if (!isPresentLiveFromBuffer && ALTERNATIVE_CONTENT_MODE_STOP_DECODE == alternativeContentMode)
            {
                detachForLiveUse();
            }
            super.switchToAlternativeContent(alternativeContentMode, alternativeContentClass, alternativeContentReasonCode);
            if (log.isDebugEnabled())
            {
                log.debug("switchToAlternativeContent - reason: " + alternativeContentReasonCode);
            }
        }
    }

    public ExtendedNetworkInterface doGetNetworkInterface()
    {
        // only valid if we have an in-progress recording and we're trying to
        // present at the live point
        if (getRecordingDataSource().recordingInProgress())
        {
            if (inProgressLivePointTimeShiftWindowClient != null)
            {
                return inProgressLivePointTimeShiftWindowClient.getNetworkInterface();
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("doGetNetworkInterface called but TSWClient was null");
                }
            }
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("doGetNetworkInterface called but recording not in progress");
            }
        }
        return null;
    }

    public short doGetLTSID()
    {
        // only valid if we have an in-progress recording and we're trying to
        // present at the live point
        if (getRecordingDataSource().recordingInProgress())
        {
            if (inProgressLivePointTimeShiftWindowClient != null)
            {
                return inProgressLivePointTimeShiftWindowClient.getLTSID();
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("doGetLTSID called but TSWClient was null");
                }
            }
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("doGetLTSID called but recording not in progress");
            }
        }
        return CASession.LTSID_UNDEFINED;
    }

    protected void handleStartOfFile()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("handleStartOfFile - notifying beginningOfContent and setting player and session to rate 1.0 - current mediatime: "
                        + getMediaTime());
            }
            getDVRContext().notifyBeginningOfContent();
            float result = setSessionRate(1.0F);
            context.clockSetRate(result, false);
        }
    }

    protected void handleSessionClosed()
    {
        synchronized (getLock())
        {
            getDVRContext().notifySessionClosed();
        }
    }

    private DVRStorageManager getStorageManager()
    {
        return (DVRStorageManager) ManagerManager.getInstance(org.cablelabs.impl.manager.StorageManager.class);
    }

    class NetworkConditionListenerImpl implements NetworkConditionListener
    {
        public void networkConditionEvent(final int event)
        {
            synchronized (getLock())
            {
                context.getTaskQueue().post(new Runnable()
                {
                    public void run()
                    {
                        synchronized (getLock())
                        {
                            handleNetworkConditionEventAsync(event);
                        }
                    }
                });
            }
        }
    }

    class ServiceChangeListenerImpl implements ServiceChangeListener
    {
        public void serviceChangeEvent(final int event)
        {
            synchronized (getLock())
            {
                context.getTaskQueue().post(new Runnable()
                {
                    public void run()
                    {
                        synchronized (getLock())
                        {
                            handleServiceChangeEventAsync(event);
                        }
                    }
                });
            }
        }
    }

    private class CASessionListenerImpl implements CASessionListener
    {
        public void notifyCASessionChange(CASession session, CASessionEvent event)
        {
            if (conditionalAccessAuthorizationRequired())
            {
                if (log.isInfoEnabled())
                {
                    log.info("CASessionListener received CASessionChange event - notification session: " + session + ", notification event id: 0x" + Integer.toHexString(event.getEventID()));
                }
                reselect(SelectionTrigger.CONDITIONAL_ACCESS);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ignoring CASessionChange notification when conditional access not required - session: " + session + ", notification event id: 0x" + Integer.toHexString(event.getEventID()));
                }
            }
        }
    }

    private class TimeShiftWindowChangedListenerImpl implements TimeShiftWindowChangedListener
    {
        //NOTE: no support for 'service added' after removed
        public void tswStateChanged(TimeShiftWindowClient tswc, TimeShiftWindowStateChangedEvent e)
        {
            synchronized (getLock())
            {
                // Log the events, just for debugging purposes.
                if (log.isDebugEnabled())
                {
                    log.debug("tswStateChanged - oldState: "
                            + TimeShiftManager.stateString[e.getOldState()] + ", newState: "
                            + TimeShiftManager.stateString[e.getNewState()] + ", reason: "
                            + TimeShiftManager.reasonString[e.getReason()]);
                }
                if (!isLive)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("ignoring tswStateChanged when not at live point");
                    }
                    return;
                }
                int newState = e.getNewState();
                switch (newState)
                {
                    case TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN:
                    case TimeShiftManager.TSWSTATE_BUFFERING:
                    case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                        break;
                    case TimeShiftManager.TSWSTATE_IDLE:
                        handleTunerLost();
                        break;
                    case TimeShiftManager.TSWSTATE_INTSHUTDOWN:
                        //if intshutdown is due to a service remap, stop but freeze frame (will receive an event
                        if (e.getReason() == TimeShiftManager.TSWREASON_SERVICEREMAP)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("tswStateChanged - intshutdown/service remap - stopping presentation and releasing resources");
                            }
                            //release live point TSW client and stop the session holding the last frame..will recover with another notification
                            try
                            {
                                doStop(false);
                            }
                            finally
                            {
                                releaseResources(false);
                            }
                        }
                        else
                        {
                            //switch to the buffer if available
                            handleTunerLost();
                        }
                        break;
                    case TimeShiftManager.TSWSTATE_TUNE_PENDING:
                    case TimeShiftManager.TSWSTATE_RESERVE_PENDING:
                        break;
                    //reselect when going to ready to buffer/not ready to buffer
                    case TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER:
                    case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
                        if (log.isDebugEnabled())
                        {
                            log.debug("tswStateChanged - reselecting");
                        }
                        reselect(SelectionTrigger.RESOURCES);
                        break;
                    default:
                }
            }
        }

        // for responding to changes in CCI
        public void tswCCIChanged(TimeShiftWindowClient tswc, CopyControlInfo cciEvent)
        {
            //ignore
        }
    }

    private void handleTunerLost()
    {
        // switch to buffer if buffering enabled and
        // we've lost the NI permanently
        if (log.isDebugEnabled())
        {
            log.debug("TSWCL - lost interface - pausing");
        }
        // switching to rate zero will take us
        // out of live point
        float result = doSetRate(0);
        if (result > 0)
        {
            // failed to setrate to zero,
            // terminate
            closePresentation("Unable to switch to timeshift from intshutdown", null);
        }
        else
        {
            context.clockSetRate(result, false);
        }
    }
}
