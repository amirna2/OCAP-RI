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

import java.util.Enumeration;
import javax.media.Time;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.TimeShiftBuffer;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowChangedListener;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.TimeShiftWindowStateChangedEvent;
import org.cablelabs.impl.manager.pod.CASession;
import org.cablelabs.impl.manager.pod.CASessionListener;
import org.cablelabs.impl.manager.service.SISnapshotManager;
import org.cablelabs.impl.media.access.CASessionMonitor;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.mpe.DVRAPI;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.player.AbstractDVRServicePlayer;
import org.cablelabs.impl.media.player.AlarmClock;
import org.cablelabs.impl.media.player.BroadcastAuthorization;
import org.cablelabs.impl.media.player.FixedAlarmSpec;
import org.cablelabs.impl.media.player.Util;
import org.cablelabs.impl.media.session.DVRSession;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.media.session.NoSourceException;
import org.cablelabs.impl.media.session.TSBSession;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.media.source.TSBDataSource;
import org.cablelabs.impl.pod.mpe.CASessionEvent;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceChangeEvent;
import org.cablelabs.impl.service.ServiceChangeListener;
import org.cablelabs.impl.service.ServiceChangeMonitor;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.PidMapTable;
import org.cablelabs.impl.util.SimpleCondition;
import org.cablelabs.impl.util.TimeTable;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.net.OcapLocator;
import org.ocap.service.AlternativeContentErrorEvent;

/**
 * This is a presentation of a regular {@link Service} through a timeshift
 * buffer (TSB).
 * 
 * @author schoonma
 */
public class TSBServicePresentation extends AbstractDVRServicePresentation
{
    private static final Logger log = Logger.getLogger(TSBServicePresentation.class);

    private final NetworkConditionMonitor networkConditionMonitor;
    private final ServiceChangeMonitor serviceChangeMonitor;
    private final CASessionMonitor caSessionMonitor;
    private static final long INITIAL_BUFFERING_TIMEOUT_MILLIS = 30000L;

    private boolean cciRestrictionExists = false;
    private boolean cciRestrictionOngoing = false;
    //will contain a valid value if restrictionexists is true and restrictionongoing is false
    //(the tsb contains a cci restriction but was followed by a non-restriction event 
    private long cciLastRestrictionEndMediaTimeNanos = DVRAPI.ALARM_MEDIATIME_NANOS_NOT_SPECIFIED;

    //90 minute restriction
    private static final long RESTRICTION_LIMIT_NANOS = 90 * 60 * ONE_SECOND_NANOS;

    private AlarmClock.Alarm cciAlarm;
    private AlarmClock.Alarm.Callback cciCallback = new CCICallback();

    private AlarmClock.Alarm endOfTSBAlarm;
    private AlarmClock.Alarm startOfTSBAlarm;
    private AlarmClock.Alarm.Callback startOfTSBCallback = new StartOfTSBCallback();
    private AlarmClock.Alarm.Callback endOfTSBCallback = new EndOfTSBCallback();
    private boolean tsbAlarmsActivated;
    private final SimpleCondition initialBufferingStartedCondition = new SimpleCondition(false); 

    public TSBServicePresentation(DVRServicePresentationContext pc, boolean showVideo, Selection initialSelection,
            boolean startLive, ScalingBounds bounds, Time startMediaTime, float startRate)
    {
        super(pc, showVideo, initialSelection, startLive, bounds, startMediaTime, startRate);
        TSBDataSource ds = (TSBDataSource) getDVRDataSource();

        evaluateCCIBasedRestriction();
        registerCCIBasedRestrictionAlarm();
        registerTSWListener(ds.getTSW());

        NetworkConditionListener networkConditionListener = new NetworkConditionListenerImpl();
        ServiceChangeListener serviceChangeListener = new ServiceChangeListenerImpl();
        //networkConditionMonitor cleanup removes the reference to the listener - only use for sync indications, not remap
        //remap will be provided by the TSW listener
        //TSW not_ready_to_buffer won't notify if other conditions prevent buffering, so TSW listener can't provide sync notifications
        networkConditionMonitor = new NetworkConditionMonitor(getLock(), networkConditionListener, true);
        serviceChangeMonitor = new ServiceChangeMonitor(getLock(), serviceChangeListener);
        caSessionMonitor = new CASessionMonitor(getLock(), new CASessionListenerImpl());
    }

    protected void doStart()
    {
        getTSBDataSource().getTSW().addPlayer((AbstractDVRServicePlayer) getDVRContext());
        super.doStart();
    }

    private void registerCCIBasedRestrictionAlarm()
    {
        if (cciAlarm != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("registerCCIBasedRestrictionAlarm - alarm already exists - deactivating current alarm and recreating new alarm");
            }
            cciAlarm.deactivate();
            context.destroyAlarm(cciAlarm);
        }
        if (cciRestrictionExists)
        {
            if (cciRestrictionOngoing)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("registerCCIBasedRestrictionAlarm - register sliding alarm");
                }
                //sliding alarm needs to be registered in forward direction (rate < 1) and reverse direction
                cciAlarm = context.createAlarm(new SlidingAlarmSpecImpl("slidingCCIAlarm"), cciCallback);
                try
                {
                    cciAlarm.activate();
                }
                catch (AlarmClock.AlarmException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("unable to register sliding CCI alarm", e);
                    }
                }
            }
            else
            {

                //create 'fixed' alarm - setMediaTime calls will be handled explicitly in setSessionMediaTime to prevent playout of restricted content,
                //only an alarm with a negative direction is needed
                long limit = cciLastRestrictionEndMediaTimeNanos - RESTRICTION_LIMIT_NANOS;
                if (limit > 0)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("registerCCIBasedRestrictionAlarm - setting fixed alarm");
                    }
                    cciAlarm = context.createAlarm(new FixedAlarmSpec("fixedCCIAlarm", limit, AlarmClock.AlarmSpec.Direction.REVERSE), cciCallback);
                    try
                    {
                        cciAlarm.activate();
                    }
                    catch (AlarmClock.AlarmException e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("unable to register fixed CCI alarm", e);
                        }
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("registerCCIBasedRestrictionAlarm - fixed alarm not needed - less than restriction duration in the tsb");
                    }
                }
            }
        }
    }

    /*
    The TSW listener is responsible for handling loss of tuner and service remap at the live point - everything else is handled
    by the ServiceChangeMonitor and NetworkConditionMonitors.
     */
    private void registerTSWListener(TimeShiftWindowClient tswClient)
    {
        // responsible for ensuring we switch to timeshift if we lose the NI
        tswClient.changeListener(new TimeShiftWindowChangedListener()
        {
            //NOTE: no support for 'service added' after removed
            public void tswStateChanged(TimeShiftWindowClient tswc, TimeShiftWindowStateChangedEvent e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("tswStateChanged - oldState: "
                            + TimeShiftManager.stateString[e.getOldState()] + ", newState: "
                            + TimeShiftManager.stateString[e.getNewState()] + ", reason: "
                            + TimeShiftManager.reasonString[e.getReason()]);
                }
                //TSW notifications are ignored when not at the live point except for triggering the initial buffering started condition.
                //Presentation out of the TSB may occur due to live point buffered presentation or a fast transition from live 
                //prior to the buffering notification being received.  Set the initial buffering started condition in this case while
                //not holding the shared lock.
                if (e.getNewState() == TimeShiftManager.TSWSTATE_BUFFERING)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("new state is BUFFERING - setting initial buffering started condition to true");
                    }
                    initialBufferingStartedCondition.setTrue();
                }
                synchronized (getLock())
                {
                    if (!isLive)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("ignoring tswStateChanged when not at live point");
                        }
                        return;
                    }
                    int newState = e.getNewState();
                    switch (newState)
                    {
                        case TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN:
                            break;
                        case TimeShiftManager.TSWSTATE_BUFFERING:
                            if (isPresentLiveFromBuffer)
                            {
                                if (!isSessionStarted() && e.getOldState() == TimeShiftManager.TSWSTATE_BUFF_PENDING)
                                {
                                    //at live point and going from buff pending to buffering - recover
                                    reselect(SelectionTrigger.RESOURCES);
                                }
                            }
                            break;
                        case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                            break;
                        case TimeShiftManager.TSWSTATE_IDLE:
                            handleTunerLostAtLivePoint();
                            break;
                        case TimeShiftManager.TSWSTATE_INTSHUTDOWN:
                            //if intshutdown is due to a service remap, stop but freeze frame (will receive an event
                            if (e.getReason() == TimeShiftManager.TSWREASON_SERVICEREMAP)
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("tswStateChanged - intshutdown due to service remap - stopping presentation and releasing resources");
                                }
                                //release live point TSW client and stop the session holding the last frame
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
                                handleTunerLostAtLivePoint();
                            }
                            break;
                        case TimeShiftManager.TSWSTATE_TUNE_PENDING:
                        case TimeShiftManager.TSWSTATE_RESERVE_PENDING:
                        case TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER:
                            break;
                        case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
                            //ignore BUFFERING->READY_TO_BUFFER transitions, but reselect if remap or presenting live from buffer (recovering buffering)
                            if (isPresentLiveFromBuffer || e.getReason() == TimeShiftManager.TSWREASON_SERVICEREMAP)
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("tswStateChanged - READY_TO_BUFFER - reselecting");
                                }
                                reselect(SelectionTrigger.RESOURCES);
                            }
                            break;
                        default:
                    }
                }
            }

            // for responding to changes in CCI
            public void tswCCIChanged(TimeShiftWindowClient tswc, CopyControlInfo cciEvent)
            {
                synchronized(getLock())
                {
                    if (!isSessionStarted())
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("received tswCCIChanged but session is not started - ignoring: " + cciEvent);
                        }
                        return;
                    }
                    //ignore the event - the time in the event is from the epoch...look up the 'last' entry in the tsb's timetable
                    if (log.isInfoEnabled())
                    {
                        log.info("tswCCIChanged: " + cciEvent);
                    }
                    
                    if (isLive)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("ignoring tswCCIChanged when live");
                        }
                        return;
                    }
                    long currentMediaTimeNanos = getMediaTime().getNanoseconds();
                    boolean wasCciRestrictionOngoing = cciRestrictionOngoing;
                    
                    TimeShiftBuffer tsb = getTSBForMediaTime( new Time(currentMediaTimeNanos), 
                                                              context.getClock().getRate() );
    
                    // Get the most-recently-added CCI TimeTable
                    final CopyControlInfo cci = (CopyControlInfo) tsb.getCCITimeTable().getLastEntry();
                    final long cciMediaTimeNs = tsb.getTimeBaseStartTime() + cci.getTimeNanos();
                    if (log.isDebugEnabled())
                    {
                        log.debug("tswCCIChanged - last CCI entry: " + cci + ", mediatime ns: " + cciMediaTimeNs);
                    }
                    
                    //set a CCI alarm if one is not already set and positive rate
                    if (getRate() > 0)
                    {
                        if (nextCCIUpdateAlarmMediaTimeNanos == DVRAPI.ALARM_MEDIATIME_NANOS_NOT_SPECIFIED)
                        {
                            nextCCIUpdateAlarmMediaTimeNanos = cci.getTimeNanos();
                            if (log.isInfoEnabled())
                            {
                                log.info("tswCCIChanged - positive rate, no alarm previously set - setting alarm to: " + nextCCIUpdateAlarmMediaTimeNanos);
                            }
                            //alarm is not set, set it
                            ((DVRSession)currentSession).setAlarm(nextCCIUpdateAlarmMediaTimeNanos);
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("tswCCIChanged - positive rate but but alarm previously set - ignoring");
                            }
                        }
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("tswCCIChanged - cci changed but zero or negative rate - ignoring");
                        }
                    }
                    
                    // Note: cci's from TSBs have TSB-relative time 
                    if (cci.isTimeshiftRestricted())
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("tswCCIChanged - received CCI restriction event - cci time: " + new Time(cciMediaTimeNs) + ", current mediatime: " + new Time(currentMediaTimeNanos));
                        }
    
                        cciRestrictionExists = true;
                        cciRestrictionOngoing = true;
                        cciLastRestrictionEndMediaTimeNanos = -1L;
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("tswCCIChanged - received CCI non-restriction event - cci time: " + new Time(cciMediaTimeNs) + ", restriction already exists: " + cciRestrictionExists + ", current mediatime: " + new Time(currentMediaTimeNanos));
                        }
                        if (cciRestrictionExists)
                        {
                            cciRestrictionOngoing = false;
                            cciLastRestrictionEndMediaTimeNanos = cciMediaTimeNs;
                        }
                    }
                    //ensure playback is within restriction if applicable
    
                    if (cciRestrictionExists)
                    {
                        //may get multiple restrictions in a row - already enforced if currently ongoing
                        if (cciRestrictionOngoing)
                        {
                            if (!wasCciRestrictionOngoing)
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("tswCCIChanged - restriction was not ongoing and now is ongoing - ensuring mediatime within restriction");
                                }
                                long livePointLimit = getDVRDataSource().getLiveMediaTime().getNanoseconds() - RESTRICTION_LIMIT_NANOS;
                                if (currentMediaTimeNanos < livePointLimit)
                                {
                                    if (log.isInfoEnabled())
                                    {
                                        log.info("tswCCIChanged - current mediatime less than live point limit - enforcing mediatime restriction - current mediatime: " + new Time(currentMediaTimeNanos) + ", limit: " + new Time(livePointLimit));
                                    }
                                    enforceCCIRestriction();
                                }
                                else
                                {
                                    if (log.isDebugEnabled())
                                    {
                                        log.debug("tswCCIChanged - current mediatime within restriction - no need to adjust mediatime");
                                    }
                                }
                            }
                            else
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("tswCCIChanged - restriction was ongoing - ignoring duplicate restriction notification");
                                }
                            }
                        }
                        else
                        {
                            long lastRestrictionLimit = cciLastRestrictionEndMediaTimeNanos - RESTRICTION_LIMIT_NANOS;
                            if (log.isDebugEnabled())
                            {
                                log.debug("tswCCIChanged - restriction exists but is not ongoing - ensuring mediatime within restriction - current mediatime: " + new Time(currentMediaTimeNanos));
                            }
                            if (currentMediaTimeNanos < lastRestrictionLimit)
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("tswCCIChanged - current mediatime less than last restriction limit - enforcing mediatime restriction - current mediatime: " + new Time(currentMediaTimeNanos) + ", limit: " + new Time(lastRestrictionLimit));
                                }
                                enforceCCIRestriction();
                            }
                            else
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("tswCCIChanged - inside restriction - no need to restrict mediatime");
                                }
                            }
                        }
                    }
                    registerCCIBasedRestrictionAlarm();
                }
            }
        }, TimeShiftManager.LISTENER_PRIORITY_LOW);
    }

    private void handleTunerLostAtLivePoint()
    {
        // switch to buffer if buffering enabled and
        // we've lost the NI permanently
        if (getDVRContext().isBufferingEnabled())
        {
            if (log.isDebugEnabled())
            {
                log.debug("TSWCL - lost interface - buffering enabled - pausing");
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
                //no live point presentation, end of content - rate zero
                getDVRContext().notifyEndOfContent(0.0F);
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("TSWCL - lost interface - buffering not enabled, closing presentation");
            }
            closePresentation("Lost interface and not buffering", null);
        }
    }

    protected void updateSelectionDetails(Selection selection, Time mediaTime, float rate)
    {
        if (log.isDebugEnabled())
        {
            log.debug("updateSelectionDetails - selection: " + selection + ", mediaTime: " + mediaTime + ", live: " + isLive + ", rate: "
                    + rate);
        }
        synchronized(getLock())
        {
            if (selection == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("updateSelectionDetails called but selection null - ignoring");
                }
                return;
            }
            //wait for initial buffering started condition if presentation from TSB is required but TSB is null/no TSB content is available 
            if (!initialBufferingStartedCondition.getState() && 
                    /*(isPresentLiveFromBuffer && selection.getTrigger() == MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE) ||*/
                    (selection.getTrigger() == DVRSessionTrigger.MODE && !isLive))
            {
                TimeShiftBuffer tsb = getTSBForMediaTime(mediaTime, rate);
                if (tsb == null || !tsb.hasContent())
                {
                    try
                    {
                        initialBufferingStartedCondition.waitUntilTrue(INITIAL_BUFFERING_TIMEOUT_MILLIS);
                        if (initialBufferingStartedCondition.getState())
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("updateSelectionDetails - initial buffering started condition set to true");
                            }
                        }
                        else
                        {
                            throw new IllegalStateException("Unable to start new session - initial buffering started condition not set prior to timeout");
                        }
                    }
                    catch (InterruptedException e)
                    {
                        throw new IllegalStateException("Unable to start new session - interrupted waiting for initial buffering started condition");
                    }
                }
                else
                {
                    //TSB already has content, no need to wait
                    initialBufferingStartedCondition.setTrue();
                }            
            }

            if (isLive)
            {
                SIManagerExt siManager = (SIManagerExt) SIManager.createInstance();
                ServiceDetailsExt newDetails = Util.getServiceDetails(((ServiceDataSource) context.getSource()).getService());
                if (log.isDebugEnabled())
                {
                    log.debug("updateSelectionDetails (live) - new SI manager: " + siManager + ", new details: "
                            + newDetails);
                }
                selection.update(siManager, newDetails);
            }
            else
            {
                // based on current media time, update authorization components from
                // pid map table
                TimeShiftBuffer tsb = getTSBForMediaTime(mediaTime, rate);
                if (tsb != null)
                {
                    // use 'effective' mediatime to find the pidMapTable (handling
                    // initial tsb buffering offset & making sure value is inside
                    // the tsb)
                    PidMapTable pidMapTable = tsb.getPidMapForMediaTime(getEffectiveMediaTimeNanos(mediaTime, tsb));
                    SISnapshotManager snapshotManager = pidMapTable.getSISnapshot();
                    ServiceDetailsExt newDetails = pidMapTable.getServiceDetails();
                    selection.update(snapshotManager, newDetails);
                    if (log.isDebugEnabled())
                    {
                        log.debug("updateSelectionDetails (not live) - new SI manager: " + snapshotManager
                                + ", new details: " + newDetails);
                    }
                }
                else
                {
                    // we wanted to use a snapshot but a tsb doesn't exist for that
                    // mediatime, do nothing
                    if (log.isDebugEnabled())
                    {
                        log.debug("updateSelectionDetails - no tsb found - not updating selection");
                    }
                }
            }
        }
    }

    protected CreateSessionResult doCreateSession(Selection selection, Time mediaTime, float rate) throws NoSourceException,
            MPEException
    {
        if (isLive)
        {
            //check TSW state to ensure live point presentation is supported..otherwise, treat as a tuner loss
            //if live and TSW is idle, return a timeshiftsession at rate zero 
            if (getTimeShiftWindowClient().getState() == TimeShiftManager.TSWSTATE_IDLE)
            {
                if (getDVRContext().isBufferingEnabled())
                {
                    return createTimeShiftSession(selection, getTSBDataSource().getLiveMediaTime(), 0.0F);
                }
            }
        }
        return super.doCreateSession(selection, mediaTime, rate);
    }

    protected CreateSessionResult createTimeShiftSession(Selection selection, Time mediaTime, float rate)
            throws NoSourceException
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("createTimeShiftSession(time=" + mediaTime.getSeconds() + "s, rate=" + rate + ")");
            }
            if (selection == null)
            {
                throw new IllegalArgumentException("createTimeShiftSession cannot be called with a null selection");
            }

            TimeShiftBuffer tsb = getTSBForMediaTime(mediaTime, rate);
            //if a tsb is unavailable and rate is greater than zero, go to the live point
            //otherwise, play at rate 1.0/mediatime zero
            if (tsb == null)
            {
                //unable to find a tsb at positive rate, present at the live point
                if (rate > 0.0F)
                {
                    if (isPresentLiveFromBuffer && isLive)
                    {
                        return new CreateSessionResult(AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.TUNING_FAILURE);
                    }
                    else
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("createTimeShiftSession - no tsb at positive rate - switching to live");
                        }
                        setLive(true);
                        return new CreateSessionResult(true, getDVRDataSource().getLiveMediaTime(), 1.0F, null);
                    }
                }
                else
                {
                    //tsb unavailable at negative rate, start session at mediatime 0, rate 1.0
                    if (log.isInfoEnabled())
                    {
                        log.info("createTimeShiftSession - no tsb at zero or negative rate - attempting playback at mediatime zero, rate 1.0");
                    }
                    //generate beginningofcontent here
                    //starting the new session will result in mediatime/rate update
                    getDVRContext().notifyBeginningOfContent();
                    return new CreateSessionResult(true, new Time(0), 1.0F, null);
                }
            }
            else
            {
                long cciRestrictedMediaTimeNanos = getCCIRestrictedMediaTimeNanos(mediaTime.getNanoseconds());
                boolean presentationPointChanged = (cciRestrictedMediaTimeNanos != mediaTime.getNanoseconds());
                if (presentationPointChanged)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("createTimeShiftSession - tsb found but CCI constraining mediatime - generating beginningofcontentevent and " +
                                "attempting playback at mediatime: " + new Time(cciRestrictedMediaTimeNanos) + ", rate 1.0");
                    }
                    getDVRContext().notifyBeginningOfContent();
                    //boundary was hit due to CCI - use rate 1.0 and bounded mediatime to create a new session
                    //starting the new session will result in mediatime/rate update
                    return new CreateSessionResult(true, new Time(cciRestrictedMediaTimeNanos), 1.0F, null);
                }
                else
                {
                    long tsbRelativeMediaTimeNanos = mediaTime.getNanoseconds() - tsb.getContentStartTimeInMediaTime();
                    
                    if (log.isInfoEnabled())
                    {
                        log.info("createTimeShiftSession - tsb found - creating TSBsession for mediaTime nanos: " +
                                new Time(cciRestrictedMediaTimeNanos) + ", rate: " + rate + ", tsb-relative mediatime nanos: " + tsbRelativeMediaTimeNanos);
                    }
                    
                    TimeTable cciTimeTable = tsb.getCCITimeTable();

                    //use tsb-relative media time to find timetable entries
                    CopyControlInfo activeEntry = (CopyControlInfo) cciTimeTable.getEntryBefore(tsbRelativeMediaTimeNanos + 1);

                    byte cci = activeEntry != null ? activeEntry.getCCI() : CopyControlInfo.EMI_COPY_FREELY;
                    if (log.isInfoEnabled())
                    {
                        log.info("createTimeShiftSession - initial CCI for mediatime: " + mediaTime + ": " + cci);
                    }

                    nextCCIUpdateAlarmMediaTimeNanos = DVRAPI.ALARM_MEDIATIME_NANOS_NOT_SPECIFIED;

                    if (rate > 0)
                    {
                        CopyControlInfo nextEntry = (CopyControlInfo) cciTimeTable.getEntryAfter(tsbRelativeMediaTimeNanos);
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
                        CopyControlInfo nextEntry = (CopyControlInfo) cciTimeTable.getEntryBefore(tsbRelativeMediaTimeNanos);
                        if (nextEntry != null)
                        {
                            //ignore alarm at zero, which would be handled via beginning of file notification
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
                    
                    return new CreateSessionResult(false, mediaTime, rate, 
                            new TSBSession(getLock(), sessionListener, selection.getServiceDetails(), getVideoDevice(),
                                    getTimeShiftWindowClient(), tsb, new Time(getEffectiveMediaTimeNanos(mediaTime, tsb)), 
                                    rate, context.getMute(), context.getGain(), cci, nextCCIUpdateAlarmMediaTimeNanos));
                }
            }
        }
    }

    private long getEffectiveMediaTimeNanos(Time mediaTime, TimeShiftBuffer tsb)
    {
        long tsbMediaTimeOffsetNanos = tsb.getTSWStartTimeOffset();
        // remove tsb mediatime offset from nanos passed to session (and ensure
        // non-negative value)
        long targetTSBMediaTimeNanos = mediaTime.getNanoseconds() - tsbMediaTimeOffsetNanos;

        // fence in the mediatime into navigable area of the tsb
        long effectiveMediaTimeNanos = Math.min(tsb.getContentEndTimeInMediaTime(), Math.max(tsb.getContentStartTimeInMediaTime(),
                targetTSBMediaTimeNanos));
        if (log.isDebugEnabled())
        {
            log.debug("calculating TSBsession mediatime - input mediatime: " + mediaTime.getNanoseconds()
                    + ", tsb startimeoffset: " + tsbMediaTimeOffsetNanos + ", target TSB mediatime: "
                    + targetTSBMediaTimeNanos + ", current tsb native media start/end times: "
                    + tsb.getContentStartTimeInMediaTime() + "/" + tsb.getContentEndTimeInMediaTime() + ", effective mediatimeNanos: " + effectiveMediaTimeNanos);
        }
        return effectiveMediaTimeNanos;
    }

    public void presentTimeShiftSession(DVRSession session, Selection selection, Time mediaTime, float rate, boolean mediaTimeChangeRequest)
            throws MPEException, NoSourceException
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("presentTimeShiftSession: " + session + ", " + selection + ", mediatime: " + mediaTime + ", rate: " + rate);
            }
            session.present(selection.getServiceDetails(), selection.getMediaAccessComponentAuthorization().getAuthorizedStreams());

            // Adjust Player's clock based on media time / rate returned from native.
            long nativeMediaTimeNanos = session.getMediaTime().getNanoseconds();
            float nativeRate = session.getRate();
            TimeShiftBuffer sessionTSB = ((TSBSession)session).getTimeShiftBuffer();
            long tsbMediaTimeOffsetNanos = sessionTSB.getTSWStartTimeOffset();
            if (log.isInfoEnabled())
            {
                log.info("TSBsession started - native mediatime: " + nativeMediaTimeNanos + ", tsb startTimeOffset: "
                        + tsbMediaTimeOffsetNanos + ", native rate: " + nativeRate
                        + ", updating clock with mediatime: " + (nativeMediaTimeNanos + tsbMediaTimeOffsetNanos)
                        + " and rate: " + nativeRate + ", session: " + session);
            }
            context.clockSetRate(nativeRate, false);
            //use updateClockMediaTime - interruption events may need to be fired
            context.clockSetMediaTime(new Time(nativeMediaTimeNanos + tsbMediaTimeOffsetNanos), mediaTimeChangeRequest);

            long targetTSBMediaTimeNanos = mediaTime.getNanoseconds() - tsbMediaTimeOffsetNanos;

            // we may have asked for a mediatime before the 1st tsb or after the
            // last..in that case, don't send interruption events
            if ((targetTSBMediaTimeNanos < 0 && (getTimeShiftWindowClient().getTSBPreceeding(sessionTSB) != null))
                    || (targetTSBMediaTimeNanos > sessionTSB.getContentEndTimeInMediaTime() && (getTimeShiftWindowClient().getTSBFollowing(sessionTSB) != null)))
            {
                if (log.isInfoEnabled())
                {
                    log.info("mediatime change crossed a TSB interruption - triggering TSB interruption events");
                }
                if (rate >= 0.0F)
                {
                    // attempted to enter at an interruption but at positive
                    // rate - use preceeding TSB's interruption reason
                    sendTSBInterruptionServiceContextEvents(getTimeShiftWindowClient().getTSBPreceeding(sessionTSB));
                }
                else
                {
                    // attempted to enter an interruption but at negative rate -
                    // use current TSB's interruption reason
                    sendTSBInterruptionServiceContextEvents(sessionTSB);
                }
            }
        }
    }

    protected void updateTimeShiftSession(DVRSession session, Selection selection) throws NoSourceException,
            MPEException
    {
        synchronized (getLock())
        {
            session.updatePresentation(context.getClock().getMediaTime(), selection.getMediaAccessComponentAuthorization()
                    .getAuthorizedStreams());
            // no need to update clock since we didn't change media time
        }
    }

    protected long getStartMediaTimeNanos() {
        return getTSBDataSource().getBeginningOfBuffer().getNanoseconds();
    }

    /**
     * Determine which TSB should be used to present at the specified media time
     * and playback rate.
     * 
     * @param mediaTime
     *            - the media time
     * @param rate
     *            - the playback rate
     * 
     * @return Returns the closest TSB. Returns <code>null</code> if no TSB can
     *         be found that satisifies the media time and rate.
     */
    private TimeShiftBuffer getTSBForMediaTime(Time mediaTime, float rate)
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("getTSBForMediaTime - mediaTime: " + mediaTime + ", rate: " + rate);
            }
            // Determine 'proximity' for getTSBForMediaTime() call, based on
            // rate.
            int proximity;
            if (rate >= 0.0F)
            {
                // first try forward proximity for paused tsbs (may pause prior to start of buffering)
                proximity = TimeShiftWindowClient.PROXIMITY_FORWARD;
            }
            else
            {
                proximity = TimeShiftWindowClient.PROXIMITY_BACKWARD;
            }
            // Get TSB for the media time/proximity.
            TimeShiftWindowClient timeShiftWindowClient = getTimeShiftWindowClient();
            long timeBaseStartTime = timeShiftWindowClient.getTimeBaseStartTime();
            long mediaTimeTimeBaseNanos = mediaTime.getNanoseconds() + timeBaseStartTime;
            if (log.isDebugEnabled())
            {
                log.debug("mediatime + timebasestarttime: " + new Time(mediaTimeTimeBaseNanos));
            }

            TimeShiftBuffer tsb = timeShiftWindowClient.getTSBForTimeBaseTime(mediaTimeTimeBaseNanos, proximity);
            if (log.isDebugEnabled())
            {
                log.debug("tsb for proximity: " + proximity + ": " + tsb);
            }
            //if rate zero and no forward TSB found, use backward proximity
            if (tsb == null && rate == 0.0F)
            {
                tsb = timeShiftWindowClient.getTSBForTimeBaseTime(mediaTimeTimeBaseNanos, TimeShiftWindowClient.PROXIMITY_BACKWARD);
                if (log.isDebugEnabled())
                {
                    log.debug("no tsb found at rate zero in forward proximity, tsb found in backward proximity: " + tsb);
                }
            }

            if (log.isDebugEnabled())
            {
                log.debug("getTSBForMediaTime returning: " + tsb);
            }
            return tsb;
        }
    }

    protected void doStopInternal(boolean shuttingDown)
    {
        synchronized (getLock())
        {
            //deactivate alarms here..tsb presentation blocked due to rating will re-register alarms
            deactivateTSBAlarms();
            if (shuttingDown)
            {
                getTSBDataSource().getTSW().removePlayer((AbstractDVRServicePlayer) getDVRContext());
            }
            super.doStopInternal(shuttingDown);
        }
    }

    /**
     * Authorization -is- required for broadcast service presentation (and
     * buffered broadcast service presentation)
     * 
     * @return true
     */
    protected boolean mediaAccessAuthorizationRequired()
    {
        return true;
    }

    protected void updateMediaAccessAuthorization(Selection selection)
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

    /**
     * Conditional access for TSB presentation is only required at the live point
     * @return true conditional access is required (when presenting at the live point for TSB presentation)
     */
    protected boolean conditionalAccessAuthorizationRequired()
    {
        return isLive;
    }

    protected void updateConditionalAccessAuthorization(Selection selection) throws MPEException
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
                doGetNetworkInterface(), selection.getElementaryStreams(doGetNetworkInterface(), components), components, caSessionMonitor,
                selection.isDefault(), (OcapLocator) selection.getServiceDetails().getService().getLocator(), selection.getTrigger(), selection.isDigital(), startNewSession));
        if (log.isInfoEnabled())
        {
            log.info("selection after conditional access authorization: " + selection);
        }
    }

    public void switchToAlternativeContent(int alternativeContentMode, Class alternativeContentClass, int alternativeContentReasonCode)
    {
        synchronized(getLock())
        {
            //always remove buffer playback use
            detachForBufferPlaybackUse();
            // remove live use only if decode is stopping
            if (ALTERNATIVE_CONTENT_MODE_STOP_DECODE == alternativeContentMode)
            {
                detachForLiveUse();
            }
            super.switchToAlternativeContent(alternativeContentMode, alternativeContentClass, alternativeContentReasonCode);
            //if reason is RATING_PROBLEM, set alarms at start and end of TSB in order to post interruption events and keep us inside the tsb boundaries
            if (log.isDebugEnabled())
            {
                log.debug("switchToAlternativeContent - reason: " + alternativeContentReasonCode);
            }
            if (AlternativeContentErrorEvent.RATING_PROBLEM == alternativeContentReasonCode)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("reason is RATING_PROBLEM - activating TSBalarms");
                }
                activateTSBAlarms();
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("reason is not RATING_PROBLEM - not activating TSBalarms");
                }
            }
        }
    }

    protected void handleSessionNotStartedSetRate(float rate)
    {
        synchronized(getLock())
        {
            //if in alternative content due to rating, activate alarms
            if (getAlternativeContentReasonCode() == AlternativeContentErrorEvent.RATING_PROBLEM)
            {
                //use clock mediatime but provided rate (not clock rate)
                activateTSBAlarms(context.getClock().getMediaNanoseconds(),  rate);
            }
        }
        super.handleSessionNotStartedSetRate(rate);
    }

    /**
     * If interruptions are encountered between current mediatime and requested mediatime, send interruption events and provide an updated mediatime
     * Will also activate alarms if not currently presenting normal content.
     *
     * This method should be ran any time a TSBAlarm is fired or when doSetMediaTime is called to ensure interruptions are fired.
     * 
     * @param requestedMediaTime requested mediatime
     * @param currentRate the current clock rate
     * @return mediaTime adjusted for interruptions
     */
    protected Time validateAcceptableClockMediaTime(Time requestedMediaTime, float currentRate)
    {
        //default to requested mediatime
        Time timeToUse = requestedMediaTime;
        synchronized(getLock())
        {
            Time currentMediaTime = context.getClock().getMediaTime();
            long currentMediaTimeNanos = currentMediaTime.getNanoseconds();
            if (log.isDebugEnabled())
            {
                log.debug("validateAcceptableClockMediaTime - current mediatime: " + currentMediaTime +  ", current rate: " +
                        currentRate + ", requested mediatime: " + requestedMediaTime);
            }
            //may not be presenting out of this TSB (may be in altcontent) - look up the
            TimeShiftBuffer tsb = getTSBForMediaTime(new Time(currentMediaTimeNanos), currentRate);
            long requestedMediaTimeNanos = requestedMediaTime.getNanoseconds();
            if (tsb == null)
            {
                //if a preceeding TSB is found whose end time was greater than the requested mediatime, there are interruptions
                //update presentationPointTSB & trigger notifications
                if (requestedMediaTimeNanos < currentMediaTimeNanos)
                {
                    //find one forward or backward
                    TimeShiftBuffer preceedingTSB = getTSBForMediaTime(new Time(currentMediaTimeNanos), 0.0F);
                    if (preceedingTSB != null)
                    {
                        long preceedingTSBMediaEndTimeNanos = preceedingTSB.getContentEndTimeInMediaTime() + preceedingTSB.getTSWStartTimeOffset();
                        if (requestedMediaTimeNanos < preceedingTSBMediaEndTimeNanos)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("no active TSB for current mediatime, but requested mediatime less than preceeding TSB media end time - triggering interruption events for: " + preceedingTSB);
                            }
                            sendTSBInterruptionServiceContextEvents(preceedingTSB);
                            tsb = preceedingTSB;
                        }
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("no active TSB for current mediatime - returning requested mediatime: " + timeToUse);
                    }
                }
            }
            if (tsb != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Found TSB for current mediatime - evaluating: " + tsb);
                }
                //iterate over TSBs in forward or backward direction looking for a TSB with the requested mediatime
                //and trigger interruption events

                boolean navigating = true;
                while (navigating)
                {
                    long lastTSBMediaStartTimeNanos = tsb.getContentStartTimeInMediaTime() + tsb.getTSWStartTimeOffset();
                    long lastTSBMediaEndTimeNanos = tsb.getContentEndTimeInMediaTime() + tsb.getTSWStartTimeOffset();

                    if (requestedMediaTimeNanos >= lastTSBMediaStartTimeNanos && requestedMediaTimeNanos <= lastTSBMediaEndTimeNanos)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("mediatime found in TSB - using requested mediatime");
                        }
                        navigating = false;
                    }
                    //Somewhere b/w live and end media time of last tsb there won't be any following TSB in this case'
                    else if(requestedMediaTimeNanos > lastTSBMediaEndTimeNanos && requestedMediaTimeNanos < currentMediaTimeNanos)
                    {
                        navigating = false;

                        if (log.isDebugEnabled())
                        {
                            log.debug("Requested Media Time("+requestedMediaTimeNanos+") is greater than TSB end media time("+lastTSBMediaEndTimeNanos+")");
                        }
                        timeToUse = new Time(lastTSBMediaEndTimeNanos - ONE_SECOND_NANOS);
                    }
                    ////Somewhere before start media time of 1st tsb.there won't be any preceding TSB in this case'
                    else if(requestedMediaTimeNanos < lastTSBMediaStartTimeNanos && requestedMediaTimeNanos > currentMediaTimeNanos)
                    {
                        navigating = false;

                        if (log.isDebugEnabled())
                        {
                            log.debug("Requested Media Time("+requestedMediaTimeNanos+") is less than TSB start media time("+lastTSBMediaStartTimeNanos+")");
                        }
                        timeToUse = new Time(lastTSBMediaStartTimeNanos + ONE_SECOND_NANOS);
                    }
                    else
                    {
                        //examine each TSB between the current TSB and the TSB supporting the requested mediatime
                        if (requestedMediaTimeNanos > currentMediaTimeNanos)
                        {
                            tsb = getTimeShiftWindowClient().getTSBFollowing(tsb);
                            if (log.isDebugEnabled())
                            {
                                log.debug("examining TSBs following active TSB - examining TSB: " + tsb);
                            }
                            if (tsb == null)
                            {
                                navigating = false;
                                //no further TSB and walking forward - use end mediatime from last tsb
                                timeToUse = new Time(lastTSBMediaEndTimeNanos);
                                if (log.isDebugEnabled())
                                {
                                    log.debug("following TSB not found - using lastTSB media end time: " + timeToUse);
                                }
                            }
                            else
                            {
                                long followingTSBMediaStartTimeNanos = tsb.getContentStartTimeInMediaTime() + tsb.getTSWStartTimeOffset();
                                if (requestedMediaTimeNanos < followingTSBMediaStartTimeNanos)
                                {
                                    if (currentRate >= 0.0F)
                                    {
                                        //positive rate, use following TSB start
                                        timeToUse = new Time(followingTSBMediaStartTimeNanos);
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug("following TSB found but TSB start time greater than requested mediatime - rate zero or greater - using following TSB start mediatime: " + timeToUse);
                                        }
                                    }
                                    else
                                    {
                                        //negative rate, use lastTSB end
                                        timeToUse = new Time(lastTSBMediaEndTimeNanos);
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug("following TSB found but TSB start time greater than requested mediatime - rate less than zero - using last TSB end mediatime: " + timeToUse);
                                        }
                                    }
                                    navigating = false;
                                }
                                if (log.isDebugEnabled())
                                {
                                    log.debug("following TSB exists - triggering interruption events for: " + tsb);
                                }
                                sendTSBInterruptionServiceContextEvents(tsb);
                            }
                        }
                        else
                        {
                            tsb = getTimeShiftWindowClient().getTSBPreceeding(tsb);
                            if (log.isDebugEnabled())
                            {
                                log.debug("examining TSBs preceeding active TSB - examining TSB: " + tsb);
                            }
                            if (tsb == null)
                            {
                                navigating = false;
                                //no further tsb and walking backward - use start mediatime from last tsb
                                timeToUse = new Time(lastTSBMediaStartTimeNanos);
                                if (log.isDebugEnabled())
                                {
                                    log.debug("preceeding TSB not found - using TSB media start time: " + timeToUse);
                                }
                            }
                            else
                            {
                                long preceedingTSBMediaEndTimeNanos = tsb.getContentEndTimeInMediaTime() + tsb.getTSWStartTimeOffset();
                                if (preceedingTSBMediaEndTimeNanos < requestedMediaTimeNanos)
                                {
                                    //preceeding end time before requested mediatime, use rate to determine if we use preceeding end or last start
                                    if (currentRate >= 0.0F)
                                    {
                                        //positive rate, use lastTSB start
                                        timeToUse = new Time(lastTSBMediaStartTimeNanos);
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug("preceeding TSB found but TSB end time greater than requested mediatime - rate zero or greater - using last TSB start mediatime: " + timeToUse);
                                        }
                                    }
                                    else
                                    {
                                        //negative rate, use preceedingTSB end
                                        timeToUse = new Time(preceedingTSBMediaEndTimeNanos);
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug("preceeding TSB found but TSB end time greater than requested mediatime - rate less than zero - using preceeding TSB end mediatime: " + timeToUse);
                                        }
                                    }
                                    navigating = false;
                                }
                                if (log.isDebugEnabled())
                                {
                                    log.debug("preceeding TSB exists - triggering interruption events for preceeding TSB: " + tsb);
                                }
                                sendTSBInterruptionServiceContextEvents(tsb);
                            }
                        }
                    }
                }
            }
            Time result = super.validateAcceptableClockMediaTime(timeToUse, currentRate);
            return result;
        }
    }

    /**
     * Activate TSB alarms using current mediatime and rate if necessary (may no-op).
     *
     * Should be activated when transitioning to alternative content due to RATING, to support playback and trick modes
     * while not presenting while ensuring playback stays within the boundaries of the available TSBs
     */
    private void activateTSBAlarms()
    {
        synchronized(getLock())
        {
            activateTSBAlarms(context.getClock().getMediaTime().getNanoseconds(), context.getClock().getRate());
        }
    }

    /**
     * Activate TSB alarms using provided mediatime and rate if necessary (may no-op).
     *
     * This method should be called when transitioning to alternative content due to RATING.
     *
     * This method will not register alarms if the starting point mediatime nanos represents live mediatime
     *
     * @param startingPointMediaTimeNanos
     * @param startingPointRate
     */
    private void activateTSBAlarms(long startingPointMediaTimeNanos, float startingPointRate)
    {
        synchronized(getLock())
        {
            Time liveMediaTime = getDVRDataSource().getLiveMediaTime();
            Time startingPointMediaTime = new Time(startingPointMediaTimeNanos);
            if (log.isDebugEnabled())
            {
                log.debug("activateTSBAlarms - starting point mediatime: " + startingPointMediaTime + ", starting point rate: " +
                        startingPointRate);
            }

            if (isWithinOneSecondOfLiveMediaTime(startingPointMediaTime, liveMediaTime))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("activateTSBAlarms called with starting point mediatime representing live - not activating alarms");
                }
                return;
            }
            if (tsbAlarmsActivated)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("activateTSBAlarms called while already active - ignoring");
                }
                return;
            }
            try
            {
                TimeShiftBuffer tsb = getTSBForMediaTime(startingPointMediaTime, startingPointRate);
                if (tsb == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("TSB not found in direction of rate - looking for TSB in either direction");
                    }
                    //if TSB not found in direction of current rate, look for TSB in either direction (by using rate zero)
                    tsb = getTSBForMediaTime(startingPointMediaTime, 0.0F);
                }
                if (tsb != null)
                {
                    long tsbMediaEndTimeNanos = tsb.getContentEndTimeInMediaTime() + tsb.getTSWStartTimeOffset();
                    long tsbMediaStartTimeNanos = tsb.getContentStartTimeInMediaTime() + tsb.getTSWStartTimeOffset();
                    if (log.isInfoEnabled())
                    {
                        log.info("creating and activating start: " + new Time(tsbMediaStartTimeNanos) + " and end: " + new Time(tsbMediaEndTimeNanos) +
                                " TSB mediatime alarms - starting point mediaTime: " + context.getClock().getMediaTime() +
                                ", starting point rate: " + startingPointRate);
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("not within one second of start or end of tsb - activating alarms - starting point mediatime: " + new Time(startingPointMediaTimeNanos) +
                                ", tsb start mediatime: " + new Time(tsbMediaStartTimeNanos) + ", tsb end mediatime: " + new Time(tsbMediaEndTimeNanos) +
                                ", starting point rate: " + startingPointRate);
                    }
                    AlarmClock.AlarmSpec specEndOFTSB = new FixedAlarmSpec("endOfTSB", (long) (tsbMediaEndTimeNanos + (ONE_SECOND_NANOS * startingPointRate)),
                            AlarmClock.AlarmSpec.Direction.FORWARD);
                    //if the livePointAlarm callback is notified, check the end of TSB and see if we're there..if so, set rate 1.0
                    endOfTSBAlarm = context.createAlarm(specEndOFTSB, endOfTSBCallback);

                    //set start alarm to start mediatime - 1 SECOND before start of tsb to ensure the alarm scheduled time isn't behind the current media time (or zero)
                    AlarmClock.AlarmSpec specStartOfTSB = new FixedAlarmSpec("startOfTSB", (long) Math.max((tsbMediaStartTimeNanos - (ONE_SECOND_NANOS * startingPointRate)), 0),
                            AlarmClock.AlarmSpec.Direction.REVERSE);
                    startOfTSBAlarm = context.createAlarm(specStartOfTSB, startOfTSBCallback);
                    endOfTSBAlarm.activate();
                    startOfTSBAlarm.activate();
                    tsbAlarmsActivated = true;
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("no TSB found - not creating alarms (TSB mediatime not known)");
                    }
                }
            }
            catch (AlarmClock.AlarmException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to activate alarms", e);
                }
            }
        }
    }

    protected boolean validateResources()
    {
        synchronized (getLock())
        {
            //if presenting live from the buffer, ensure a buffering TSB is available before proceeding
            if (isPresentLiveFromBuffer && getCurrentSelection().getTrigger() == MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE)
            {
                TimeShiftBuffer tsb = getTSBForMediaTime(startMediaTime, startRate);
                if (tsb == null || !tsb.hasContent())
                {
                    try
                    {
                        initialBufferingStartedCondition.waitUntilTrue(INITIAL_BUFFERING_TIMEOUT_MILLIS);
                    }
                    catch (InterruptedException e)
                    {
                        if (log.isInfoEnabled()) 
                        {
                            log.info("interrupted waiting for initial buffering started condition - switching to alternative content");
                        }
                        switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                        return false;
                    }
                    if (!initialBufferingStartedCondition.getState())
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("initial buffering started condition didn't complete before timeout - switching to alternative content");
                        }
                        switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                        return false;
                    }
                }
                else
                {
                    initialBufferingStartedCondition.setTrue();
                }
                return super.validateResources();
            }
            
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
            return super.validateResources();
        }
    }

    private void detachForBufferPlaybackUse()
    {
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("detachForBufferPlaybackUse");
            }
            // bitwise operation
            TimeShiftWindowClient timeShiftWindowClient = getTimeShiftWindowClient();
            if (timeShiftWindowClient != null)
            {
                if ((timeShiftWindowClient.getUses() & TimeShiftManager.TSWUSE_BUFFERPLAYBACK) != 0)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("detaching for bufferPlayback use");
                    }
                    timeShiftWindowClient.detachFor(TimeShiftManager.TSWUSE_BUFFERPLAYBACK);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("already detached for bufferplayback use");
                    }
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getTimeShiftWindowClient returned null - not detaching for bufferplayback use");
                }
            }
        }
        catch (IllegalStateException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to detach bufferplayback use", e);
            }
        }
        catch (IllegalArgumentException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to detach bufferplayback use", e);
            }
        }
    }

    private void attachForBufferPlaybackUse()
    {
        try
        {
            TimeShiftWindowClient timeShiftWindowClient = getTimeShiftWindowClient();
            if (timeShiftWindowClient != null)
            {
                if ((timeShiftWindowClient.getUses() & TimeShiftManager.TSWUSE_BUFFERPLAYBACK) == 0)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("attaching for bufferplayback use");
                    }
                    timeShiftWindowClient.attachFor(TimeShiftManager.TSWUSE_BUFFERPLAYBACK);
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("already attached for bufferplayback use");
                    }
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getTimeShiftWindowClient returned null - not attaching for buffer playback use");
                }
            }
        }
        catch (IllegalStateException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to attach for bufferplayback use", e);
            }
        }
        catch (IllegalArgumentException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to attach for bufferplayback use", e);
            }
        }
    }

    private void detachForLiveUse()
    {
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("detachForLiveUse");
            }
            // bitwise operation
            TimeShiftWindowClient timeShiftWindowClient = getTimeShiftWindowClient();
            if (timeShiftWindowClient != null)
            {
                if ((timeShiftWindowClient.getUses() & TimeShiftManager.TSWUSE_LIVEPLAYBACK) != 0)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("detaching for live use");
                    }
                    timeShiftWindowClient.detachFor(TimeShiftManager.TSWUSE_LIVEPLAYBACK);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("already detached for live use");
                    }
                }
            }
            else
            {
                if (log.isDebugEnabled()) 
                {
                    log.debug("getTimeShiftWindowClient returned null - not detaching for live use");
                }
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
        if (log.isDebugEnabled())
        {
            log.debug("attachForLiveUse");
        }
        // failure to attach is not an error (we may be trying to go to
        // alternative content)
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("attaching for live use");
            }
            TimeShiftWindowClient timeShiftWindowClient = getTimeShiftWindowClient();
            if (timeShiftWindowClient != null){
                if ((timeShiftWindowClient.getUses() & TimeShiftManager.TSWUSE_LIVEPLAYBACK) == 0)
                {
                    timeShiftWindowClient.attachFor(TimeShiftManager.TSWUSE_LIVEPLAYBACK);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("already attached for live use");
                    }
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getTimeShiftWindowClient returned null - not attaching for live use");
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

    public ExtendedNetworkInterface doGetNetworkInterface()
    {
        return getDVRContext().getNetworkInterface();
    }

    public short doGetLTSID()
    {
        return caSessionMonitor.getLTSID();
    }

    protected void releaseResources(boolean shuttingDown)
    {
        //only shut down the CASessionMonitor and NetworkConditionMonitor if presentation is shutting down
        //NetworkConditionMonitor is needed to recover from SPI-initiated service changes
        if (shuttingDown)
        {
            caSessionMonitor.cleanup();
            networkConditionMonitor.cleanup();
        }
        //only need to detach if not shutting down
        if (!shuttingDown)
        {
            detachForLiveUse();
            detachForBufferPlaybackUse();
        }

        serviceChangeMonitor.cleanup();
        initialBufferingStartedCondition.setFalse();
        super.releaseResources(shuttingDown);
    }

    protected void initializeResources(Selection selection)
    {
        synchronized (getLock())
        {
            //if presenting live from buffer, all presentation is from the buffer - on initial startup, request buffering and attach for buffered playback use
            if (isPresentLiveFromBuffer)
            {
                getTSBDataSource().requestBuffering();
                attachForBufferPlaybackUse();
            }
            else
            {
                // don't initialize 'resource' (network) monitoring unless we're
                // presenting live
                if (isLive)
                {
                    attachForLiveUse();
                    networkConditionMonitor.initialize(getNetworkInterface());
                    serviceChangeMonitor.initialize(selection.getServiceDetails());
                }
            }
            super.initializeResources(selection);
        }
    }

    /**
     * Not called when isLive = true.
     *
     * @param mediaTime
     * @param postMediaTimeSetEvent
     */
    protected void setSessionMediaTime(Time mediaTime, boolean postMediaTimeSetEvent)
    {
        synchronized (getLock())
        {
            if (!isSessionStarted())
            {
                if (log.isInfoEnabled())
                {
                    log.info("setSessionMediaTime: " + mediaTime + " - session is not started - updating clock, tsb offset unknown");
                }
                context.clockSetMediaTime(mediaTime, postMediaTimeSetEvent);
                return;
            }
            // Get the TSB containing the media time, taking into account the
            // playback rate.
            // Not finding a valid TSB is a fatal error, which closes the
            // player.

            float rate = context.getClock().getRate(); // Get the current
                                                       // playback rate from
                                                       // clock

            TimeShiftBuffer tsb = getTSBForMediaTime(mediaTime, rate);
            if (log.isDebugEnabled())
            {
                log.debug("setSessionMediaTime - targetMediaTime: " + mediaTime + ", rate: " + rate
                        + ", tsb containing mediatime: " + tsb);
            }
            if (tsb != null && tsb.equals(getTSBForCurrentSession()))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("current TSB holds requested mediatime - setting the mediatime on the current session");
                }
                try
                {
                    //the CCI alarm could be used to ensure the mediatime is gated by CCI, but presentation at the requested mediatime would begin and the
                    //player clock would then be adjusted, triggering the CCI alarm, which would adjust the presentation point
                    //to prevent playback in the CCI-restricted time, evaluating the mediatime & CCI restrictions prior to setting the mediatime on the session
                    long restrictedMediaTimeNanos = getCCIRestrictedMediaTimeNanos(mediaTime.getNanoseconds());
                    if (restrictedMediaTimeNanos != mediaTime.getNanoseconds())
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("mediatime adjusted due to CCI - calling setSessionMediaTime with adjusted mediatime: " + new Time(restrictedMediaTimeNanos));
                        }
                        //boundary hit - post beginningofcontent and re-trigger setSessionMediaTime, update rate to 1.0 and return
                        getDVRContext().notifyBeginningOfContent();
                        float result = setSessionRate(1.0F);
                        context.clockSetRate(result, false);
                        setSessionMediaTime(new Time(restrictedMediaTimeNanos), postMediaTimeSetEvent);
                        return;
                    }

                    long tsbMediaTimeOffsetNanos = tsb.getTSWStartTimeOffset();
                    if (log.isDebugEnabled())
                    {
                        log.debug("tsb mediatime offset: " + new Time(tsbMediaTimeOffsetNanos));
                    }

                    // remove tsb mediatime offset from nanos passed to
                    // session (and ensure non-negative value)
                    Time currentMediaTime = getSessionMediaTime();
                    if (currentMediaTime == null)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("setSessionMediaTime called when session not started - calling context.clockSetMediaTime and generating the mediatime event");
                        }
                        context.clockSetMediaTime(mediaTime, postMediaTimeSetEvent);
                        return;
                    }
                    long currentMediaTimeNanos = currentMediaTime.getNanoseconds();
                    // if current time is greater than target mediatime,
                    // make sure we're inside buffer (within media end time)
                    long targetTSBMediaTimeNanos = mediaTime.getNanoseconds() - tsbMediaTimeOffsetNanos;
                    long effectiveTSBMediaTimeNanos;
                    if (currentMediaTimeNanos > targetTSBMediaTimeNanos)
                    {
                        effectiveTSBMediaTimeNanos = Math.min(targetTSBMediaTimeNanos, tsb.getContentEndTimeInMediaTime());
                    }
                    else if (currentMediaTimeNanos < targetTSBMediaTimeNanos)
                    {
                        effectiveTSBMediaTimeNanos = Math.max(targetTSBMediaTimeNanos, tsb.getContentStartTimeInMediaTime());
                    }
                    else
                    {
                        // same
                        effectiveTSBMediaTimeNanos = targetTSBMediaTimeNanos;
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("mediatime to pass to session: " + new Time(effectiveTSBMediaTimeNanos));
                    }

                    /*
                     * We should not minus the start time because when we
                     * tune to any channel TSB starts at that time. If max
                     * TSB size is 60 min then up to 60 mins media start
                     * time is 0 after that media start time is what ever
                     * that time elapsed after 60 mins. so if user want to
                     * set media time to 10 mins and elapsed time is 5 mins
                     * then final value is 10 - 5 is 5 mins but user want
                     * time to set 10 mins
                     */
                    Time result = currentSession.setMediaTime(new Time(effectiveTSBMediaTimeNanos));
                    if (log.isDebugEnabled())
                    {
                        log.debug("setSessionMediaTime: " + result);
                    }

                    // unexpected
                    if (result == null)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("mediaTime returned by session was null");
                        }
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("setSessionMediaTime - result: " + (new Time(result.getNanoseconds() + tsbMediaTimeOffsetNanos)) +
                                    ", updating clock with mediatime and generating mediatime event");
                        }
                        context.clockSetMediaTime(new Time(result.getNanoseconds() + tsbMediaTimeOffsetNanos), postMediaTimeSetEvent);
                        evaluateAndSetCCIMediaAlarm(rate, result.getNanoseconds());
                    }
                }
                catch (MPEException x)
                {
                    closePresentation(x.toString(), x);
                }
            }
            // Media time is in a different TSB.
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("setSessionMediaTime - current TSB does not hold requested media time - starting new session");
                }
                // Start new session.
                startNewSessionAsync(getCurrentSelection(), DVRSessionTrigger.SEGMENT, mediaTime, context.getClock().getRate(), postMediaTimeSetEvent, false);
            }
        }
    }

    private void evaluateCCIBasedRestriction()
    {
        if (log.isDebugEnabled())
        {
            log.debug("evaluateCCIBasedRestriction - examining TSBs for CCI information");
        }
        long lastRestrictedEventMediaTimeNanos = -1L;
        long lastUnrestrictedEventMediaTimeNanos = -1L;

        Enumeration tsbs = getTimeShiftWindowClient().elements();
        while (tsbs.hasMoreElements())
        {
            TimeShiftBuffer thisTSB = (TimeShiftBuffer) tsbs.nextElement();
            if (log.isDebugEnabled())
            {
                log.debug("evaluating CCI for TSB: " + thisTSB);
            }
            TimeTable cciTimeTable = thisTSB.getCCITimeTable();
            Enumeration cciStatusEnum = cciTimeTable.elements();
            while (cciStatusEnum.hasMoreElements())
            {
                CopyControlInfo cci = (CopyControlInfo)cciStatusEnum.nextElement();
                if (cci.isTimeshiftRestricted())
                {
                    lastRestrictedEventMediaTimeNanos = thisTSB.getTSWStartTimeOffset() + cci.getTimeNanos();
                    if (log.isDebugEnabled())
                    {
                        log.debug("found restricted CCI event: " + cci.getEMI() 
                                  + ", timebase: " + new Time(cci.getTimeNanos()) 
                                  + "ms, setting lastRestrictedEventMediaTimeNanos to: " 
                                  + new Time(lastRestrictedEventMediaTimeNanos) + " and unrestricted to -1");
                    }
                    //reset lastunrestricted to -1 - will be used as a flag below
                    lastUnrestrictedEventMediaTimeNanos = -1L;
                }
                else
                {
                    //only set right after restriction or initial
                    if (lastUnrestrictedEventMediaTimeNanos == -1L)
                    {
                        lastUnrestrictedEventMediaTimeNanos = thisTSB.getTSWStartTimeOffset() + cci.getTimeNanos();
                        if (log.isDebugEnabled())
                        {
                            log.debug("found unrestricted CCI event: " + cci.getEMI() + ", timebase: " + new Time(cci.getTimeMillis()) + "ms, setting lastUnRestrictedEventMediaTimeNanos to: " + new Time(lastUnrestrictedEventMediaTimeNanos));
                        }

                    }
                }
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("finished evaluating TSBs for CCI information");
        }
        //set flags based on last unrestricted & last restricted values

        //reset flags which won't be set if a restriction doesn't exist
        cciRestrictionExists = lastRestrictedEventMediaTimeNanos >= 0;
        cciRestrictionOngoing = cciRestrictionExists && lastUnrestrictedEventMediaTimeNanos < 0;
        //will be -1 if ongoing
        cciLastRestrictionEndMediaTimeNanos = lastUnrestrictedEventMediaTimeNanos;
        if (log.isDebugEnabled())
        {
            log.debug("evaluateCCIBasedRestriction - restriction exists: " + cciRestrictionExists + ", restriction ongoing: " + cciRestrictionOngoing + ", restriction end mediatime: " + (cciLastRestrictionEndMediaTimeNanos == -1? "unset": new Time(cciLastRestrictionEndMediaTimeNanos).toString()));
        }
    }

    /**
     * Provide a valid media time in nanos if TSB presentation is restricted due to CCI (90 minute playback restriction)
     * @param requestedMediaTimeNanos the requested mediatime
     * @return adjusted mediatime if CCI restricts TSB playback
     */
    private long getCCIRestrictedMediaTimeNanos(long requestedMediaTimeNanos)
    {
        //if requested mediatime is within 30 seconds of limit, allow it (if this method returns a mediatime other than the requestedmediatimenanos,
        //this method will be called with the new value - adding a mediatime threshold of 30 seconds to prevent cycles due to updated mediatimes
        long allowedThresholdNanos = 30 * ONE_SECOND_NANOS;

        if (log.isDebugEnabled())
        {
            log.debug("getCCIRestrictedMediaTimeNanos - requested mediatime: " + new Time(requestedMediaTimeNanos) +
                    ", restriction exists: " + cciRestrictionExists + ", restriction ongoing: " + cciRestrictionOngoing +
                    ", last restriction end mediatime: " + (cciLastRestrictionEndMediaTimeNanos == -1?"unset":new Time(cciLastRestrictionEndMediaTimeNanos).toString()));
        }
        if (!cciRestrictionExists)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getCCIRestrictedMediaTimeNanos - not restricted - returning requested mediatime: " + new Time(requestedMediaTimeNanos));
            }
            return requestedMediaTimeNanos;
        }

        if (cciRestrictionOngoing)
        {
            long liveMediaTimeNanos = getDVRDataSource().getLiveMediaTime().getNanoseconds();

            long ongoingLimit = liveMediaTimeNanos - RESTRICTION_LIMIT_NANOS;
            //evaluate requested mediatime - if requested mediatime is less than the allowed limit (minus threshold), restrict
            if (requestedMediaTimeNanos < (ongoingLimit - allowedThresholdNanos))
            {
                long result = Math.max(0, ongoingLimit);
                if (log.isInfoEnabled())
                {
                    log.info("getCCIRestrictedMediaTimeNanos - restriction exists and is ongoing - requested mediatime: " + new Time(requestedMediaTimeNanos) +
                            " less than allowed playback limit minus allowed threshold: " + new Time(ongoingLimit - allowedThresholdNanos) + " - returning: " + new Time(result));
                }
                return result;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getCCIRestrictedMediaTimeNanos - restriction exists and is ongoing - " +
                            " not outside restricted mediatime plus allowed threshold - returning requested mediatime: " + new Time(requestedMediaTimeNanos));
                }
                return requestedMediaTimeNanos;
            }
        }
        else
        {
            long restrictionLimit = cciLastRestrictionEndMediaTimeNanos - RESTRICTION_LIMIT_NANOS;
            //evaluate requested mediatime - if requested mediatime is outside the allowed threshold, restrict
            if (requestedMediaTimeNanos < (restrictionLimit - allowedThresholdNanos))
            {
                long result = Math.max(0, restrictionLimit);
                if (log.isDebugEnabled())
                {
                    log.debug("getCCIRestrictedMediaTimeNanos - restriction exists but is not ongoing - requested mediatime: " + new Time(requestedMediaTimeNanos) +
                            " less than restricted mediatime minus allowed threshold: " + new Time(restrictionLimit - allowedThresholdNanos) +" - returning: " + new Time(result));
                }
                return result;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getCCIRestrictedMediaTimeNanos - restriction exists but is not ongoing - requested mediatime: " + new Time(requestedMediaTimeNanos) +
                            " not outside restricted mediatime plus allowed threshold - returning requested mediatime: " +
                            new Time(requestedMediaTimeNanos));
                }
                return requestedMediaTimeNanos;
            }
        }
    }

    /**
     * Not called when isLive = true
     * @return session mediatime
     */
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

            Time sessionTime = null;
            try
            {
                TimeShiftBuffer currentTSB = getTSBForCurrentSession();
                if (currentTSB == null)
                {
                    if (log.isTraceEnabled())
                    {
                        log.trace("getSessionMediaTime - TSB not found - returning null");
                    }
                    return null;
                }
                long tsbMediaTimeOffsetNanos = currentTSB.getTSWStartTimeOffset();

                // add tsb offset to value returned by session
                Time sessionMediaTime = currentSession.getMediaTime();
                if (sessionMediaTime == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("session media time was null - returning null");
                    }
                    return null;
                }
                // sessionMediaTime may be null if the current session isn't
                // started - if so, return the player's mediatime
                long mediaTime = sessionMediaTime.getNanoseconds() + tsbMediaTimeOffsetNanos;
                if (log.isTraceEnabled())
                {
                    log.trace("getSessionMediaTime - mediaTime from session: " + sessionMediaTime
                            + ", tsb startTimeOffsetNanos: " + tsbMediaTimeOffsetNanos + ", player clock mediatime: " + context.getClock().getMediaTime() + ", returning: " + mediaTime);
                }
                sessionTime = new Time(mediaTime);
            }
            catch (MPEException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("unable to get mediatime from current session: " + currentSession, e);
                }
            }

            return sessionTime;
        }
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

                TimeShiftBuffer tsb = getTSBForCurrentSession();
                Time sessionMediaTime = getSessionMediaTime();
                if (sessionMediaTime == null)
                {
                    sessionMediaTime = context.getClock().getMediaTime();
                }
                long tsbRelativeMediaTime = sessionMediaTime.getNanoseconds() + tsb.getContentStartTimeInMediaTime();
                evaluateAndSetCCIMediaAlarm(result, tsbRelativeMediaTime);
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

    private TimeShiftWindowClient getTimeShiftWindowClient()
    {
        synchronized (getLock())
        {
            return getTSBDataSource().getTSW();
        }
    }

    /**
     * This method is overridden to ensure that buffering is started on the TSB
     * before switching to timeshifted playback. If buffering it not started, it
     * starts buffering. If buffering is started successfully, it invokes
     * {@link AbstractDVRServicePresentation#switchToTimeshift(Time,float,boolean,boolean)}. If
     * buffering cannot be started, it won't call that method, leaving the
     * player in live mode.
     * 
     * @see AbstractDVRServicePresentation#switchToTimeshift(Time,float,boolean,boolean)
     */
    protected void switchToTimeshift(Time mt, float rate, boolean rateChangeRequest, boolean mediaTimeChangeRequest)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("switchToTimeShift - mediaTime: " + mt + ", rate: " + rate);
            }

            if (isPresentLiveFromBuffer)
            {
                super.switchToTimeshift(mt, rate, rateChangeRequest, mediaTimeChangeRequest);
            }
            else
            {
                // If buffering of TSB is not started, attempt to start it.
                getTSBDataSource().requestBuffering();
                attachForBufferPlaybackUse();
                detachForLiveUse();
                super.switchToTimeshift(mt, rate, rateChangeRequest, mediaTimeChangeRequest);
            }
        }
    }

    public void switchToLive(MediaPresentationEvaluationTrigger trigger, boolean mediaTimeChangeRequest)
    {
        synchronized(getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("switchToLive - trigger: " + trigger + ", live: " + isLive);
            }
            Time liveMediaTime = getDVRDataSource().getLiveMediaTime();
            if (isPresentLiveFromBuffer)
            {
                //no need to update uses - staying in the buffer
                //set rate before an attempt to set live media time
                float result = setSessionRate(1.0F);
                getDVRContext().notifyEndOfContent(result);
                context.clockSetRate(result, false);
                setSessionMediaTime(liveMediaTime, mediaTimeChangeRequest);
                setLive(true);
                getDVRContext().notifyEnteringLiveMode();
            }
            else
            {
                getDVRContext().notifyEndOfContent(1.0F);
                attachForLiveUse();
                detachForBufferPlaybackUse();
                super.switchToLive(trigger, mediaTimeChangeRequest);
            }
        }
    }

    private void deactivateTSBAlarms()
    {
        synchronized(getLock())
        {
            tsbAlarmsActivated = false;
            if (startOfTSBAlarm != null)
            {
                startOfTSBAlarm.deactivate();
                context.destroyAlarm(startOfTSBAlarm);
            }
            if (endOfTSBAlarm != null)
            {
                endOfTSBAlarm.deactivate();
                context.destroyAlarm(endOfTSBAlarm);
            }
        }
    }

    /**
     * TSB provider for current session
     * @return current TSB or null if session is null
     */
    private TimeShiftBuffer getTSBForCurrentSession()
    {
        synchronized(getLock())
        {
            if (currentSession == null)
            {
                return null;
            }
            if (isLive && !isPresentLiveFromBuffer)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("getTSBForCurrentSession called but live and not presenting from buffer - returning null");
                }
                return null;
            }
            return ((TSBSession)currentSession).getTimeShiftBuffer();
        }
    }

    protected void handleEndOfFile()
    {
        synchronized (getLock())
        {
            if (currentSession == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("received endoffile notification but session is null - ignoring");
                }
                return;
            }
            TimeShiftBuffer tsb = getTSBForCurrentSession();
            TimeShiftBuffer followingTSB = getTimeShiftWindowClient().getTSBFollowing(tsb);
            if (followingTSB == null)
            {
                if (isPresentLiveFromBuffer)
                {
                    if (getRate() > 1.0F)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("endOfFile - no followingTSB and presenting live from buffer at rate > 1.0 - switching to live - current mediatime: " + getMediaTime());
                        }
                        // last tsb - switch to live
                        // Notify that end of content was reached (prior to starting the
                        // session)
                        switchToLive(DVRSessionTrigger.MODE, false);
                    }
                    else
                    {
                        //rate 1.0 end of file with no following TSB...if there is an interruption, post it (but not NormalContent)
                        if (log.isInfoEnabled())
                        {
                            log.info("endoffile - no followingTSB and presenting live from buffer at rate 1.0 - posting EnteringLiveMode and switching to altcontent");
                        }
                        getDVRContext().notifyEnteringLiveMode();
                        switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, getBufferingStopAlternativeContentReasonCode(tsb.getBufferingStopReason()));
                    }
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("endOfFile - no followingTSB and not presenting live from buffer - switching to live - current mediatime: " + getMediaTime());
                    }
                    // last tsb - switch to live
                    // Notify that end of content was reached (prior to starting the
                    // session)
                    switchToLive(DVRSessionTrigger.MODE, false);
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("endOfFile - followingTSB exists - current mediatime: " + getMediaTime());
                }
                // post altcontent/normalcontent prior to starting new session
                sendTSBInterruptionServiceContextEvents(tsb);
                startNewSessionAsync(getCurrentSelection(), DVRSessionTrigger.SEGMENT, new Time(
                        followingTSB.getContentStartTimeInMediaTime() + followingTSB.getTSWStartTimeOffset()), context.getClock()
                        .getRate(), false, false);
            }
        }
    }

    protected void handleStartOfFile()
    {
        synchronized (getLock())
        {
            if (currentSession == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("received startoffile notification but session is null - ignoring");
                }
                return;
            }
            TimeShiftBuffer preceedingTSB = getTimeShiftWindowClient().getTSBPreceeding(getTSBForCurrentSession());

            if (preceedingTSB == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("startOfFile - no preceedingTSB - notifying beginning of content and setting rate to 1.0 - current mediatime: "
                            + getMediaTime());
                }
                getDVRContext().notifyBeginningOfContent();
                // ensure we begin playback at rate 1.0 and update the jmf clock's rate
                float result = setSessionRate(1.0F);
                context.clockSetRate(result, false);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("startOfFile - preceedingTSB exists - current mediatime: " + getMediaTime());
                }
                // post altcontent/normalcontent prior to starting new session
                sendTSBInterruptionServiceContextEvents(preceedingTSB);
                startNewSessionAsync(getCurrentSelection(), DVRSessionTrigger.SEGMENT, new Time(
                        preceedingTSB.getContentEndTimeInMediaTime() + preceedingTSB.getTSWStartTimeOffset()), context.getClock()
                        .getRate(), false, false);
            }
        }
    }

    protected void handleSessionClosed()
    {
        // ignore - may get a session closed due to trigger or reselect
    }
    
    private int getBufferingStopAlternativeContentReasonCode(int bufferingStopReason)
    {
        int altContentReasonCode = 0;
        switch (bufferingStopReason)
        {
            //always coming through this logic even though there may be no interruptions...ignore NOREASON
            case TimeShiftManager.TSWREASON_NOREASON:
            case TimeShiftManager.TSWREASON_SERVICEREMAP:
            case TimeShiftManager.TSWREASON_PIDCHANGE:
            case TimeShiftManager.TSWREASON_SIZEINCREASE:
            case TimeShiftManager.TSWREASON_SIZEREDUCTION:
            case TimeShiftManager.TSWREASON_INTLOST:
                if (log.isDebugEnabled())
                {
                    log.debug("Ignoring tsb buffering stop reason: " + TimeShiftManager.reasonString[bufferingStopReason]);
                }
                break;
            case TimeShiftManager.TSWREASON_ACCESSWITHDRAWN:
                altContentReasonCode = AlternativeContentErrorEvent.CA_REFUSAL;
                break;
            case TimeShiftManager.TSWREASON_SERVICEVANISHED:
                altContentReasonCode = AlternativeContentErrorEvent.CONTENT_NOT_FOUND;
                break;
            case TimeShiftManager.TSWREASON_SYNCLOST:
                altContentReasonCode = AlternativeContentErrorEvent.TUNING_FAILURE;
                break;
            case TimeShiftManager.TSWREASON_NOCOMPONENTS:
                altContentReasonCode = AlternativeContentErrorEvent.CONTENT_NOT_FOUND;
                break;
            default:
                if (log.isWarnEnabled())
                {
                    log.warn("Unexpected buffering stop reason: "
                            + TimeShiftManager.reasonString[bufferingStopReason]);
                }
        }
        return altContentReasonCode;
    }

    private void sendTSBInterruptionServiceContextEvents(TimeShiftBuffer tsb)
    {
        Class altContentClass = AlternativeContentErrorEvent.class;
        int bufferingStopReason = tsb.getBufferingStopReason();
        synchronized (getLock())
        {
            int altContentReasonCode = getBufferingStopAlternativeContentReasonCode(bufferingStopReason);
            if (altContentReasonCode != 0)
            {
                if (log.isInfoEnabled())
                {
                    log.info("triggering altcontent and normalcontent events due to TSB interruption - altcontent class: " + altContentClass.getName() + ", reason: " + altContentReasonCode);
                }
                notifyAlternativeContent(altContentClass, altContentReasonCode);
                notifyNormalContent(getDVRContext());
            }
        }
    }

    private TSBDataSource getTSBDataSource()
    {
        synchronized (getLock())
        {
            return (TSBDataSource) context.getSource();
        }
    }

    // TODO: implementation same as superclass, except it allows stepping while
    // rate is not zero - determine if this is needed
    public boolean stepFrame(int direction)
    {
        synchronized (getLock())
        {
            if (!isSessionStarted())
            {
                if (log.isInfoEnabled())
                {
                    log.info("session not started - unable to step frame - direction: " + direction);
                }
                return false;
            }
            boolean result = false;
            try
            {
                result = ((DVRSession) currentSession).stepFrame(direction);
            }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Unable to step frame - direction: " + direction, e);
                }
            }
            return result;
        }
    }

    protected String eventToString(int event)
    {
        switch (event)
        {
            case ServiceChangeEvent.PMT_CHANGED:
                return "ServiceChangeEvent.PMT_CHANGED";
            case ServiceChangeEvent.PMT_REMOVED:
                return "ServiceChangeEvent.PMT_REMOVED";
            case NetworkConditionEvent.RETUNE_FAILED:
                return "NetworkConditionEvent.RETUNE_FAILED";
            case NetworkConditionEvent.UNTUNED:
                return "NetworkConditionEvent.UNTUNED";
            case NetworkConditionEvent.TUNE_SYNC_ACQUIRED:
                return "NetworkConditionEvent.TUNE_SYNC_ACQUIRED";
            case NetworkConditionEvent.TUNE_SYNC_LOST:
                return "NetworkConditionEvent.TUNE_SYNC_LOST";
            case NetworkConditionEvent.RETUNE_PENDING:
                return "NetworkConditionEvent.RETUNE_PENDING";
            default:
                return super.eventToString(event);
        }
    }

    protected TimeTable getCCITimeTableForCurrentSession()
    {
        return getTSBForCurrentSession().getCCITimeTable();
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

    private void handleStartOfTSBAlarmCallback()
    {
        synchronized(getLock())
        {
            final Time currentMediaTime = context.getClock().getMediaTime();
            final float currentRate = context.getClock().getRate();
            if (log.isDebugEnabled())
            {
                log.debug("handleStartOfTSBAlarmCallback - current mediaTime: " + currentMediaTime + ", current rate: " + currentRate + " - deactivating TSB alarms");
            }
            //will reactivate at the end after possibly updating mediatime
            deactivateTSBAlarms();
            CallerContextManager ccMgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            //async notification of start of tsb callback since a clock update may cause this alarm to be re-triggered
            ccMgr.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    //using rate zero because that will look first for a TSB in the forward direction and then backward direction for the current mediatime
                    TimeShiftBuffer tsbForCurrentMediaTime = getTSBForMediaTime(currentMediaTime, 0.0F);
                    if (tsbForCurrentMediaTime != null)
                    {
                        long playerClockNanos = currentMediaTime.getNanoseconds();
                        long tsbMediaStartTimeNanos = tsbForCurrentMediaTime.getContentStartTimeInMediaTime() + tsbForCurrentMediaTime.getTSWStartTimeOffset();
                        //delta is player clock - tsb start time
                        long delta = playerClockNanos - tsbMediaStartTimeNanos;
                        if (log.isDebugEnabled())
                        {
                            log.debug("handleStartOfTSBAlarmCallback - currentTSB not null - player clock nanos: " + playerClockNanos + ", tsbmediaStartTimeNanos: " + tsbMediaStartTimeNanos + ", delta: " + delta);
                        }

                        //within one second of TSB start at current (negative) rate or rate zero
                        if (delta < (ONE_SECOND_NANOS * (-currentRate)) || currentRate == 0.0F)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("handleStartOfTSBAlarmCallback - delta less than one second at current rate or rate zero - delta: " + delta + ", current rate: " + currentRate);
                            }
                            //no preceeding TSB exists - post beginningOfContent event
                            TimeShiftBuffer preceedingTSB = getTimeShiftWindowClient().getTSBPreceeding(tsbForCurrentMediaTime);
                            if (preceedingTSB == null)
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("handleStartOfTSBAlarmCallback - preceeding tsb does not exist - setting rate to 1.0 and triggering beginningofcontent event");
                                }
                                getDVRContext().notifyBeginningOfContent();
                                Time result = validateAcceptableClockMediaTime(new Time(tsbMediaStartTimeNanos), currentRate);
                                context.clockSetMediaTime(result, false);
                                context.clockSetRate(1.0F, false);
                            }
                            else
                            {
                                Time newMediaTime = new Time(preceedingTSB.getContentEndTimeInMediaTime() + preceedingTSB.getTSWStartTimeOffset());
                                if (log.isInfoEnabled())
                                {
                                    log.info("handleStartOfTSBAlarmCallback - preceeding tsb exists - triggering TSB interruption events, setting mediatime to: " + newMediaTime);
                                }
                                //may send ServiceContextEvents for preceeding TSB (bufferingStop reason) prior to updating the currentTSB reference (bufferingStop reason) and update mediatime
                                Time result = validateAcceptableClockMediaTime(newMediaTime, currentRate);
                                context.clockSetMediaTime(result, false);
                            }
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("handleStartOfTSBAlarmCallback - delta greater than one second at current rate and rate not zero - delta: " + delta + ", current rate: " + currentRate);
                            }
                        }
                        //reactivate alarms
                        activateTSBAlarms();
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("no TSB found - not updating mediatime or triggering TSB interruption events");
                        }
                    }
                }
            });
        }
    }

    private void enforceCCIRestriction()
    {
        if (log.isInfoEnabled())
        {
            log.info("enforceCCIRestriction - notifying beginning of content, updating rate to 1.0 and mediatime to beginning of non-restricted tsb playback");
        }
        //set rate to 1.0 and update the jmf clock's rate
        float currentRate = context.getClock().getRate();
        float rateResult = doSetRate(1.0F);
        context.clockSetRate(rateResult, false);
        getDVRContext().notifyBeginningOfContent();
        //reset mediatime back to the non-restricted mediatime
        //if restriction is ongoing, this value is live mediatime - restriction limit
        //if restriction is not ongoing, this value is the lastrestrictionendmediatimenanos - restriction limit
        Time newMediaTime;
        if (cciRestrictionOngoing)
        {
            Time liveMediaTime = getDVRDataSource().getLiveMediaTime();
            long liveMediaTimeNanos = liveMediaTime.getNanoseconds();
            newMediaTime = new Time(Math.max(0, liveMediaTimeNanos - RESTRICTION_LIMIT_NANOS));
            if (log.isInfoEnabled())
            {
                log.info("restriction is ongoing - live mediatime: " + liveMediaTime + ", new mediatime: " + newMediaTime);
            }
        }
        else
        {
            newMediaTime = new Time(Math.max(0, cciLastRestrictionEndMediaTimeNanos - RESTRICTION_LIMIT_NANOS));
            if (log.isInfoEnabled())
            {
                log.info("restriction exists but is not ongoing - new mediatime: " + newMediaTime);
            }
        }
        //only update mediatime if current mediatime is more than 1 second at current rate from the restriction
        Time currentMediaTime = context.getClock().getMediaTime();
        if (Math.abs(newMediaTime.getNanoseconds() - currentMediaTime.getNanoseconds()) > Math.abs(currentRate * Time.ONE_SECOND))
        {
            //update presentation mediatime but don't fire mediatimesetevent
            doSetMediaTime(newMediaTime, false);
            if (log.isInfoEnabled())
            {
                log.info("CCI alarm updated clock mediaTime from: " + currentMediaTime + ", to: " + newMediaTime + " and clock rate from: " + currentRate + ", to: " + rateResult);
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("CCI alarm updated clock rate from: " + currentRate + ", to: " + rateResult + ", but current mediaTime " + currentMediaTime +
                        " was already within one second at current rate of requested mediatime " + newMediaTime + " - not updating mediatime");
            }
        }
    }

    private void handleEndOfTSBAlarmCallback()
    {
        synchronized(getLock())
        {
            final Time currentMediaTime = context.getClock().getMediaTime();
            final float currentRate = context.getClock().getRate();
            if (log.isDebugEnabled())
            {
                log.debug("handleEndOfTSBAlarmCallback - current mediaTime: " + currentMediaTime + ", current rate: " + currentRate);
            }

            //will re-activate at the end after possibly updating mediatime
            deactivateTSBAlarms();
            CallerContextManager ccMgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            //async notification of end of tsb callback since a clock update may cause this alarm to be re-triggered
            ccMgr.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    //using rate zero because that will look first for a TSB in the forward direction and then backward direction for the current mediatime
                    TimeShiftBuffer tsbForCurrentMediaTime = getTSBForMediaTime(currentMediaTime, 0.0F);
                    if (tsbForCurrentMediaTime != null)
                    {
                        long playerClockNanos = currentMediaTime.getNanoseconds();
                        long tsbMediaEndTimeNanos = tsbForCurrentMediaTime.getContentEndTimeInMediaTime() + tsbForCurrentMediaTime.getTSWStartTimeOffset();
                        //delta is media end time minus player clock
                        long delta = tsbMediaEndTimeNanos - playerClockNanos;
                        if (log.isDebugEnabled())
                        {
                            log.debug("handleEndOfTSBAlarmCallback - currentTSB not null - player clock nanos: " + playerClockNanos +
                                    ", tsbmediaEndTimeNanos: " + tsbMediaEndTimeNanos + ", delta: " + delta);
                        }

                        //within one second of TSB end at current rate or rate is zero
                        if ((delta < (ONE_SECOND_NANOS * currentRate)) || currentRate == 0.0F)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("handleEndOfTSBAlarmCallback - delta less than one second at current rate or rate zero - delta: " +
                                        delta + ", current rate: " + currentRate);
                            }

                            TimeShiftBuffer followingTSB = getTimeShiftWindowClient().getTSBFollowing(tsbForCurrentMediaTime);
                            //no following TSB exists - post endOfContent event
                            if (followingTSB == null)
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info("handleEndOfTSBAlarmCallback - following tsb does not exist - setting rate to 1.0 and triggering endofContent/enteringLiveMode events");
                                }
                                //update rate -and- notify
                                //deactivate alarms prior to updating rate and mediatime
                                setLive(true);
                                //update rate to 1.0
                                context.clockSetRate(1.0F, false);
                                //may have been at a rate > 1.0 - ensure we're 'at' the tsb end time before notifying end of content
                                Time result = validateAcceptableClockMediaTime(new Time(tsbMediaEndTimeNanos), currentRate);
                                context.clockSetMediaTime(result, false);
                                getDVRContext().notifyEndOfContent(1.0F);
                                getDVRContext().notifyEnteringLiveMode();
                            }
                            else
                            {
                                Time newMediaTime = new Time(followingTSB.getContentStartTimeInMediaTime() + followingTSB.getTSWStartTimeOffset());
                                if (log.isInfoEnabled())
                                {
                                    log.info("handleEndOfTSBAlarmCallback - following tsb exists - triggering TSB interruption events and updating mediatime to: " + newMediaTime);
                                }
                                //will send ServiceContextEvents for the current tsb (bufferingStop reason) and update mediatime
                                Time result = validateAcceptableClockMediaTime(newMediaTime, currentRate);
                                context.clockSetMediaTime(result, false);
                            }
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("handleEndOfTSBAlarmCallback - delta greater than one second at current rate and rate not zero - delta: " + delta + ", current rate: " + currentRate);
                            }
                        }
                        //reactivate alarms
                        activateTSBAlarms();
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("handleEndOfTSBAlarmCallback - no TSB found - not updating mediatime or triggering TSB interruption events");
                        }
                    }
                }
            });
        }
    }

    class StartOfTSBCallback implements AlarmClock.Alarm.Callback
    {
        public void fired(AlarmClock.Alarm alarm)
        {
            if (log.isDebugEnabled())
            {
                log.debug("StartOfTSBCallback fired");
            }
            handleStartOfTSBAlarmCallback();
        }

        public void destroyed(AlarmClock.Alarm alarm, AlarmClock.AlarmException reason)
        {
            //no-op
        }
    }

    class CCICallback implements AlarmClock.Alarm.Callback
    {
        public void fired(AlarmClock.Alarm alarm)
        {
            if (log.isDebugEnabled())
            {
                log.debug("CCI callback fired");
            }

            enforceCCIRestriction();
        }

        public void destroyed(AlarmClock.Alarm alarm, AlarmClock.AlarmException reason)
        {
            //no-op
        }
    }

    class EndOfTSBCallback implements AlarmClock.Alarm.Callback
    {
        public void fired(AlarmClock.Alarm alarm)
        {
            if (log.isDebugEnabled())
            {
                log.debug("EndOfTSBCallback fired");
            }

            handleEndOfTSBAlarmCallback();
        }

        public void destroyed(AlarmClock.Alarm alarm, AlarmClock.AlarmException reason)
        {
            //no-op
        }
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

    /**
     * Represents an alarmspec whose alarm time is moving at rate 1.0 and is only active when playback is in the reverse direction.
     */
    public class SlidingAlarmSpecImpl implements AlarmClock.AlarmSpec
    {
        private String alarmName;

        public SlidingAlarmSpecImpl(String alarmName)
        {
            this.alarmName = alarmName;
        }

        public String getName()
        {
            return alarmName;
        }

        public long getMediaTimeNanos()
        {
            return Math.max(0, getDVRDataSource().getLiveMediaTime().getNanoseconds() - RESTRICTION_LIMIT_NANOS);
        }

        public long getDelayWallTimeNanos(long baseMediaTimeNanos, float rate)
        {
            long mediaTimeNanos = getMediaTimeNanos();

            //subtract one from rate to represent the alarm point moving forward at rate 1.0
            //may be negative - ensure result is not negative
            long result = (long) ((mediaTimeNanos - baseMediaTimeNanos) / (rate - 1.0F));
            if (log.isDebugEnabled())
            {
                log.debug("slidingAlarmSpec - getDelayWallTimeNanos - firing mediatime: " + new Time(mediaTimeNanos) + ", requested mediatime: " + new Time(baseMediaTimeNanos) + ", rate: " + rate + ", returning: " + new Time(result));
            }
            if (result < 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("slidingAlarmSpec - getDelayWallTimeNanos - result was negative - setting to zero");
                }
                result = 0;
            }

            return result;
        }

        public boolean canSchedule(float rate, long baseMediaTimeNanos)
        {
            long liveMediaTimeNanos = getDVRDataSource().getLiveMediaTime().getNanoseconds();
            long playbackLimitNanos = liveMediaTimeNanos - RESTRICTION_LIMIT_NANOS;
            boolean result = rate < 1.0F;
            if (log.isDebugEnabled())
            {
                log.debug("slidingAlarmSpec - canSchedule - rate: " + rate + ", requested mediatime: " + new Time(baseMediaTimeNanos) +
                        ", live mediatime: " + new Time(liveMediaTimeNanos) + ", playbackLimit: " + new Time(playbackLimitNanos) + " returning: " + result);
            }
            return result;
        }

        public boolean shouldFire(long currentMediaTimeNanos, long newMediaTimeNanos)
        {
            float currentRate = context.getClock().getRate();
            long liveMediaTimeNanos = getDVRDataSource().getLiveMediaTime().getNanoseconds();
            long playbackLimitNanos = liveMediaTimeNanos - RESTRICTION_LIMIT_NANOS;

            //only fire if live mediatime is greater than restriction + 1 second and new mediatime is outside of the limit by one second
            boolean result = playbackLimitNanos > Math.abs(Time.ONE_SECOND * currentRate) && newMediaTimeNanos < (playbackLimitNanos - Math.abs(Time.ONE_SECOND * currentRate));
            if (log.isDebugEnabled())
            {
                log.debug("slidingAlarmSpec - shouldFire - current rate: " + currentRate + ", current mediatime: " + new Time(currentMediaTimeNanos) + ", new mediatime: " + new Time(newMediaTimeNanos) +
                        ", playbackLimit: " + new Time(playbackLimitNanos) + ", result: " + result);
            }
            return result;
        }

        public String toString()
        {
            return "SlidingAlarmSpec - id: 0x" + Integer.toHexString(hashCode()).toUpperCase() +"]";
        }
    }
}
