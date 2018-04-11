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

package org.cablelabs.impl.manager.recording;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListFilter;
import org.ocap.shared.dvr.navigation.RecordingListIterator;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.util.SystemEventUtil;

import org.cablelabs.impl.manager.timer.TimerMgrJava2;
import org.cablelabs.impl.manager.timer.TimerSpecExt;

/**
 * The Scheduler assist in timing related functionality for scheduled
 * recordings. It will trigger the recording manager to start and stop
 * recordings according to their scheduled times.
 */

public class Scheduler implements TVTimerWentOffListener
{

    /**
     * <code>m_instance</code> singleton
     */
    private static Scheduler m_instance = null;

    /**
     * <code>ccm</code> the caller context.
     */
    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * <code>m_beforeStartNotificands</code>
     */
    private Vector m_beforeStartNotificands = new Vector(0, 0);

    /**
     * <code>m_startRecordNotificands</code>
     */
    private Vector m_startRecordNotificands = new Vector(0, 0);

    /**
     * <code>m_theTimer</code>
     */
    TVTimer m_theTimer;
    
    /**
     * Minimum interval to use for scheduling BeforeStartRecordingListener
     * notifications.
     */
    private long m_minimumBeforeStartNotificationIntervalMs;

    /**
     * state of scheduler
     */
    boolean m_running = false;

    /**
     * Internal synchronization object
     */
    Object m_inUseLock;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(Scheduler.class.getName());

    /**
     * Specifically monitors when an application terminates.
     * 
     * @author Jeff Spruiel
     */
    class Client implements CallbackData
    {
        // Called when an application is terminated.
        public void destroy(CallerContext ctx)
        {
            releaseClientResources(ctx);
        }

        public void pause(CallerContext ctx)
        {
        }

        public void active(CallerContext callerContext)
        {
        }
    }

    /**
     * Returns the singleton instance of the Scheduler.
     */
    public static synchronized Scheduler getInstance()
    {
        if (m_instance == null)
        {
            m_instance = new Scheduler();
        }
        return m_instance;
    }

    /**
     * Constructor
     */
    Scheduler()
    {
        m_inUseLock = new Object();
        m_theTimer = TVTimer.getTimer();
        m_running = true;
    }

    /**
     * Expose the internal sync object to allow RecordingManager to implement a
     * single-lock implementation. Will only be called at initialization (so
     * lock wll not be held).
     */
    void setSyncObject(Object sync)
    {
        m_inUseLock = sync;
    }

    void setMinimumBeforeStartNotificationInterval(long beforeStartNotificationInterval)
    {
        m_minimumBeforeStartNotificationIntervalMs = beforeStartNotificationInterval;
    }
    
    /**
     * Adds a recording to the schedule for future start/end. The scheduler will
     * notify the recording manager when start/end events should be processed.
     * 
     * @param recording
     *            recording object to add to schedule
     * @param startTime
     *            time that the recording is to start (or was started)
     * @param duration
     *            length of scheduled recording
     * @param expirationTime
     *            the time when the Recording is expired.
     * @param isRecordingStarted
     *            true if the recording has already started (true time shift).
     * @param act
     *            asynchronous completion token returned unmodified as events
     *            are delivered.
     */
    void scheduleRecording(RecordingImplInterface recordingImpl, long startTime, long duration, long expirationTime,
            boolean isRecordingStarted)
    {
        synchronized (m_inUseLock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("scheduleRecording rec       = " + recordingImpl);
            }

            ExpirationSpec expirSpec = new ExpirationSpec(this, recordingImpl, expirationTime);

            // IF the recording has already started
            if (isRecordingStarted)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Recording already started..");
                }

                // for this recording, schedule a timer event to stop recording
                // after the duration has elapsed
                StopSpec stopRecSpec = new StopSpec(this, recordingImpl);
                recordingImpl.setAlarmSpec(stopRecSpec);
                try
                {
                    stopRecSpec.schedule();
                    recordingImpl.setExpirSpec(expirSpec);
                    expirSpec.schedule();
                }
                catch (TVTimerScheduleFailedException e1)
                {
                    SystemEventUtil.logRecoverableError(e1);
                    return;
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Recording is not started..");
                }

                // Create a start alarm spec
                StartSpec startAlarmRoot = new StartSpec(this, recordingImpl);

                recordingImpl.setAlarmSpec(startAlarmRoot);
                recordingImpl.setExpirSpec(expirSpec);

                try
                {
                    startAlarmRoot.schedule();
                    expirSpec.schedule();
                }
                catch (TVTimerScheduleFailedException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("schedule tspec failed: " + e);
                    }
                    return;
                }
                // creates before start alarm leaves for this recording.
                scheduleBeforeStartAlarms(recordingImpl);
            }
        }
    }

    void scheduleExpiration(RecordingImplInterface recordingImpl)
    {
        synchronized (m_inUseLock)
        {
            ExpirationSpec expirSpec = new ExpirationSpec(this, recordingImpl, recordingImpl.getRecordingInfo()
                    .getExpirationDate()
                    .getTime());
            if (log.isDebugEnabled())
            {
                log.debug("scheduling expiration: date = " + recordingImpl.getRecordingInfo().getExpirationDate());
            }
            recordingImpl.setExpirSpec(expirSpec);

            try
            {
                // add the TVTimerSpec to the TVTimer object.
                expirSpec.schedule();
            }
            catch (TVTimerScheduleFailedException e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        }
    }

    void descheduleExpiration(RecordingImplInterface rImpl)
    {
        synchronized (m_inUseLock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("descheduling expiration");
            }

            AlarmSpec expirSpec = (AlarmSpec) rImpl.getExpirSpec();

            if (expirSpec != null)
            {
                expirSpec.disable(); // disabling the spec also de-schedules it
            }
        }
    }

    void descheduleRecording(RecordingImplInterface rImpl)
    {
        synchronized (m_inUseLock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Scheduler:descheduleRecording");
            }

            // abort the alarm stop timer
            AlarmSpec alarmSpec = (AlarmSpec) rImpl.getAlarmSpec();
            AlarmSpec expirSpec = (AlarmSpec) rImpl.getExpirSpec();

            if (alarmSpec != null)
            {
                if (alarmSpec instanceof StartSpec)
                {
                    ((StartSpec) alarmSpec).descheduleLeaves();
                }
                alarmSpec.disable(); // disabling the spec also de-schedules it
            }

            if (expirSpec != null)
            {
                expirSpec.disable(); // disabling the spec also de-schedules it
            }
        }
    }

    /**
     * Removes all AlarmSpecs pertaining to this recording.
     * 
     * @param recording
     *            the recording to retrieve the AlarmSpec.
     */
    void cancelRecording(RecordingImplInterface recordingImpl)
    {
        if (log.isDebugEnabled())
        {
            log.debug("entered");
        }

        descheduleRecording(recordingImpl);
    }

    /**
     * Deschedules and removes the AlarmSpec for the end of duration for the
     * specified recording.
     * 
     * @param recording
     *            recording to be stopped
     */
    void stopRecording(RecordingImplInterface recordingImpl)
    {
        descheduleRecording(recordingImpl);
    }

    /**
     * Adds a recording alert event listener for before-event notification of
     * pending recording activity.
     * 
     * @param listener
     *            listener to be notified before a recording event
     * 
     * @param time
     *            time in milliseconds before a recording event for notification
     * 
     */
    public void addBeforeStartListener(RecordingAlertListener ral, long time)
    {
        // synchronized with removeListener()
        synchronized (m_inUseLock)
        {
            CallerContext cctx = ccm.getCurrentContext();
            
            if (time == 0)
            {
                addStartRecordingListener(ral, cctx);
            }
            else
            {
                addBeforeStartRecordingListener(ral, ccm.getCurrentContext(), time, false);
            }
        }
    }

    /**
     * When a new BeforeStartNotificand is created, this method is called to add
     * a new before start alarm spec to each pending recording.
     */
    private void createLeafAlarmSpecs(BeforeStartNotificand bsNotif)
    {
        // Returns recordings to who this caller has read access.
        RecordingList recordingList = NavigationManager.getInstance().getPendingRecordings();
        if (recordingList.size() == 0)
        {
            return;
        }

        RecordingListIterator iterator = recordingList.createRecordingListIterator();
        while (iterator.hasNext())
        {
            RecordingImplInterface recordingImpl = (RecordingImplInterface) iterator.nextEntry();
            addNotificandToRecording(recordingImpl, bsNotif);
        }
    }

    /**
     * If a before start listener is added, and is an "always notify" listener,
     * then this method is used to send an event to that listener for all
     * recordings that are currently pending.
     */
    private void sendAlwaysNotifyEvent(RecordingAlertListener listener, long time)
    {
        // Returns recordings to who this caller has read access.
        RecordingList recordingList = NavigationManager.getInstance().getPendingRecordings();
        if (recordingList.size() == 0)
        {
            return;
        }

        RecordingListIterator iterator = recordingList.createRecordingListIterator();
        while (iterator.hasNext())
        {
            RecordingImplInterface recordingImpl = (RecordingImplInterface) iterator.nextEntry();
            // get before time value and create the absolute beforeTime
            long notifyTime = recordingImpl.getRequestedStartTime() - time;

            // If the notification time is in the past ...
            if (System.currentTimeMillis() >= notifyTime)
            {
                listener.recordingAlert(new RecordingAlertEvent((RecordingRequest) recordingImpl));
            }
        }
    }

    private void addNotificandToRecording(RecordingImplInterface recordingImpl, BeforeStartNotificand bsNotif)
    {
        // get before time value and create the absolute beforeTime
        long beforeStartOffset = bsNotif.getBeforeTime();
        final long recStartTime = recordingImpl.getRequestedStartTime();

        // Adjust the Before Start time to ensure it fires before the start timer 
        if (beforeStartOffset <= m_minimumBeforeStartNotificationIntervalMs)
        {
            beforeStartOffset = m_minimumBeforeStartNotificationIntervalMs;
        }
    
        final long notifyTime = recStartTime - beforeStartOffset;

        // If the notification time is in the past (or nearly so) ...
        if ( (System.currentTimeMillis() + m_minimumBeforeStartNotificationIntervalMs)
              >= notifyTime )
        {
            // If there are some "always notify" listeners, call them now ...
            if (bsNotif.hasAlwaysStartListeners())
            {
                List dispatchq = (List) bsNotif.clone();
                notifyListeners(dispatchq, new RecordingAlertEvent((RecordingRequest) recordingImpl), true);
            }
            return;
        }
        // create new before start alarm spec. It will tracked
        // by the parent AlarmSpec
        BeforeStartSpec beforeStartSpec = new BeforeStartSpec(this, recordingImpl, bsNotif, notifyTime);

        // Add the leaf before start alarm to the parent.
        StartSpec startSpec = (StartSpec) recordingImpl.getAlarmSpec();
        startSpec.addLeaf(beforeStartSpec);

        // Now add to the TVTimer.
        try
        {
            beforeStartSpec.schedule();
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    /**
     * Removes the listener from receiving recording alert events. The listener
     * is removed from all lists.
     * 
     * @param ral
     *            listener to be removed
     */
    public void removeListener(RecordingAlertListener ral)
    {
        synchronized (m_inUseLock)
        {
            // First remove the listener from the start record listener list.
            CallerContext cctx = ccm.getCurrentContext();
            ContextNotificand ctxNotif = getContextNotificand(m_startRecordNotificands, cctx);

            if (ctxNotif != null)
            {
                if (ctxNotif.remove(ral) == true)
                {
                    /*
                     * If the list is now empty, then discard it.
                     */
                    if (ctxNotif.size() == 0)
                    {
                        m_startRecordNotificands.remove(ctxNotif);
                    }
                }
            }

            // Next remove the listener from before start lists.
            removeBeforeStartRecordingListener(ral, cctx);
        }
    }

    /**
     * Releases all application resources.
     * 
     * @param cctx
     *            the applications context.
     */
    private void releaseClientResources(CallerContext cctx)
    {
        // First remove the listener from the start record listener list.
        ContextNotificand ctxNotif = getContextNotificand(m_startRecordNotificands, cctx);

        if (ctxNotif != null)
        {
            ctxNotif.clear();
            m_startRecordNotificands.remove(ctxNotif);
        }

        // Next remove the listener from before start lists.

        // Next Check all BeforeStartNotificands.
        int size = this.m_beforeStartNotificands.size();
        BeforeStartNotificand bsNotif = null;
        List bsNotifTrash = new ArrayList();
        List ctxNotifTrash = new ArrayList();
        for (int i = 0; i < size; i++)
        {
            bsNotif = (BeforeStartNotificand) m_beforeStartNotificands.get(i);
            ctxNotif = getContextNotificand(bsNotif, cctx);

            // Will be null iff it has no listeners.
            if (ctxNotif != null)
            {
                ctxNotif.clear();
                ctxNotifTrash.add(ctxNotif);
            }
            // if the number to be deleted is equal to the number
            int cnt = ctxNotifTrash.size();
            while ((--cnt) > -1)
            {
                bsNotif.remove(ctxNotifTrash.get(cnt));
            }

            if (bsNotif.size() == 0)
            {
                bsNotifTrash.add(bsNotif);
            }
            ctxNotifTrash.clear();
        }// next bsNotif

        int x = bsNotifTrash.size();
        while (--x > -1)
        {
            this.m_beforeStartNotificands.remove(bsNotifTrash.get(x));
        }
        bsNotifTrash.clear();
        ctxNotifTrash = null;
        bsNotifTrash = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @seejavax.tv.util.TVTimerWentOffListener#timerWentOff(javax.tv.util.
     * TVTimerWentOffEvent)
     */
    public void timerWentOff(TVTimerWentOffEvent event)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Entered timerWentOff handler  = " + event.getTimerSpec().toString());
        }

        TVTimerSpec retSpec = event.getTimerSpec();

        synchronized (m_inUseLock)
        {
            if (!m_running)
            {
                return;
            }

            AlarmSpec tvSpec = (AlarmSpec) retSpec;
            tvSpec.deschedule();

            if (!tvSpec.isEnabled())
            {
                return;
            }

            List dispatchq;

            // 6.2.1.1.4 Managing completed recordings
            if (tvSpec instanceof ExpirationSpec)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Handle ExpirationSpec");
                }
                RecordingImplInterface rImpl = ((ExpirationSpec) tvSpec).getRecording();
                rImpl.expire();

                return;
            }

            if (tvSpec instanceof StopSpec)
            {
                StopSpec spec = (StopSpec) tvSpec;
                if (log.isDebugEnabled())
                {
                    log.debug("Handle StopSpec");
                }

                spec.getRecording().stopInternal();
                return;
            }

            if (tvSpec instanceof StartSpec)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Handle StartSpec");
                }
                StartSpec spec = (StartSpec) tvSpec;
                RecordingImplInterface rImpl = spec.getRecording();

                if (rImpl.getState() == (LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                        || (rImpl.getState() == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)
                        || (rImpl.getState() == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE)
                        || (rImpl.getState() == LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE))
                {

                    rImpl.startInternal();

                    StopSpec stopSpec = new StopSpec(this, rImpl);
                    rImpl.setAlarmSpec(stopSpec);
                    try
                    {
                        stopSpec.schedule();
                        if (log.isDebugEnabled())
                        {
                            log.debug("scheduled stopSpec");
                        }
                    }
                    catch (TVTimerScheduleFailedException e)
                    {
                        SystemEventUtil.logRecoverableError(e);
                    }
                    dispatchq = (Vector) m_startRecordNotificands.clone();
                    notifyListeners(dispatchq, new RecordingAlertEvent((RecordingRequest) rImpl), false);
                }
            }
            else if (tvSpec instanceof BeforeStartSpec)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Handle BeforeStartSpec");
                }
                BeforeStartSpec spec = (BeforeStartSpec) tvSpec;
                RecordingImplInterface rImpl = spec.getRecording();

                if (log.isDebugEnabled())
                {
                    long refTime = System.currentTimeMillis();
                    if (log.isDebugEnabled())
                {
                    log.debug("before time = "
                            + ((refTime - spec.getTime()) - (refTime - rImpl.getRequestedStartTime())));
                }
                    if (log.isDebugEnabled())
                    {
                        log.debug("before time for " + rImpl);
                    }
                }

                if (rImpl.getState() == (LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                        || (rImpl.getState() == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE))
                {
                    BeforeStartNotificand bfn = ((BeforeStartSpec) tvSpec).getBeforeStartNotificand();

                    ((StartSpec) rImpl.getAlarmSpec()).removeLeaf(spec);
                    dispatchq = (List) bfn.clone();
                    notifyListeners(dispatchq, new RecordingAlertEvent((RecordingRequest) rImpl), false);
                }
            }
        }
    }

    /**
     * Sends events each listener.
     * 
     * @param contextNotificandList
     *            a list of ContextNotificand objects that further reference
     *            listeners.
     * @param event
     *            the event delivered.
     * @param onlyAlwaysNotify
     *            Set this to true to only send an event to the listeners that
     *            are in the "alwaysNotify" list, false to send to all
     *            listeners.
     * @see hasReadAccess
     */
    private void notifyListeners(final List contextNotificandList, final RecordingAlertEvent event,
            boolean onlyAlwaysNotify)
    {
        OcapSecurityManager osm = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);
        RecordingRequest rr = event.getRecordingRequest();

        // For each start recording listener, deliver the event
        int numCtxs = contextNotificandList.size();
        for (int i = 0; i < numCtxs; i++)
        {
            final ContextNotificand ctxNotif = (ContextNotificand) contextNotificandList.get(i);

            // now notify each listener in this context
            CallerContext cctx = ctxNotif.getContext();

            final List listeners;

            if (onlyAlwaysNotify)
            {
                listeners = ctxNotif.getAlwaysNotifyListeners();
            }
            else
            {
                listeners = ctxNotif.getAllListeners();
            }

            final int numListeners = listeners.size();
            if (cctx.equals(ccm.getSystemContext()))
            {
                cctx.runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        for (int pos = 0; pos < numListeners; pos++)
                        {
                            RecordingAlertListener ral = (RecordingAlertListener) listeners.get(pos);
                            ral.recordingAlert(event);
                        }
                    }
                });
            }
            else
            {
                AppID listenerAppID = (AppID) (cctx.get(CallerContext.APP_ID));
                if (hasReadAccess(rr, osm, listenerAppID))
                {
                    cctx.runInContext(new Runnable()
                    {
                        public void run()
                        {
                            for (int pos = 0; pos < numListeners; pos++)
                            {
                                RecordingAlertListener ral = (RecordingAlertListener) listeners.get(pos);
                                ral.recordingAlert(event);
                            }
                        }
                    });
                }
            }
        }
    }

    /**
     * Get the corresponding BeforeStartNotificand.
     * 
     * @param time
     *            the unique early remind time.
     * @return the unique object.
     */
    private BeforeStartNotificand getBeforeStartNotif(long time)
    {
        BeforeStartNotificand bsNotif = null;

        int size = m_beforeStartNotificands.size();
        for (int i = 0; i < size; i++)
        {
            bsNotif = (BeforeStartNotificand) m_beforeStartNotificands.get(i);
            if (bsNotif.getBeforeTime() == time) return bsNotif;
        }
        return null;
    }

    /**
     * A leaf TVTimerSpec is created for each BeforeStartNotificand. The leaf is
     * added to the root AlarmSpec.
     * 
     * @param startRecSpec
     * @param aspec
     *            the AlarmSpec
     * @param recImpl
     *            the RecordingImplInterface
     * @param startTime
     *            the recordings starting time.
     */
    private void scheduleBeforeStartAlarms(RecordingImplInterface recImpl)
    {
        int numNotificands = m_beforeStartNotificands.size();

        // For each BeforeStartNotificand add a new leaf to the
        // specified recImpl.
        for (int i = 0; i < numNotificands; i++)
        {
            BeforeStartNotificand bsNotif = (BeforeStartNotificand) m_beforeStartNotificands.get(i);
            addNotificandToRecording(recImpl, bsNotif);
        }
    }

    /**
     * Get the system timer from the scheduler Because there is no direct way of
     * getting another caller context's timer, expose the Scheduler's timer
     * (obtained from the system context at boot) to the recording
     * implementation.
     * 
     * @return
     */
    TVTimer getSystemTimer()
    {
        return m_theTimer;
    }

    /**
     * Releases resources due to system shutdown and sets the m_instance
     * singleton to null. After shutDown a Scheduler.getInstance() invocation
     * will result in a the createion of a new object.
     */
    void shutDown()
    {
        /**
         * A local class of type <code>RecordingListFilter</code> used in the
         * <code>shutDown</code> method to acquire all
         * <code>RecordingRequest</code> objects containing scheduled
         * <code>TVTimerSpec</code> objects.
         * 
         * @author jspruiel
         */
        class ScheduledTVTimerFilter extends RecordingListFilter
        {
            public boolean accept(RecordingRequest rle)
            {
                RecordingImplInterface rImpl = (RecordingImplInterface) rle;
                return ((rImpl.getState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                        || (rImpl.getState() == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)
                        || (rImpl.getState() == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE) || (rImpl.getState() == LeafRecordingRequest.IN_PROGRESS_STATE)) ? true
                        : false;
            }
        }

        synchronized (m_inUseLock)
        {
            // tells timerWentOff to discard new events.
            m_running = false;

            // acquire tvTimerSpec for each recording that is returned
            // and deschedule it.
            RecordingList rec = NavigationManager.getInstance().getEntries(new ScheduledTVTimerFilter());
            if (rec != null)
            {
                // RecordingImplInterface tmp = null;
                for (int i = 0; i < rec.size(); i++)
                {
                    AlarmSpec tmp = (AlarmSpec) ((RecordingImplInterface) rec.getRecordingRequest(i)).getAlarmSpec();
                    AlarmSpec tmpExp = (AlarmSpec) ((RecordingImplInterface) rec.getRecordingRequest(i)).getExpirSpec();
                    if (tmp != null)
                    {
                        tmp.disable(); // disabling the spec also de-schedules
                                       // it
                    }
                    if (tmpExp != null)
                    {
                        tmpExp.disable(); // disabling the spec also
                                          // de-schedules it
                    }
                }
            }

            m_theTimer = null;
            this.ccm = null;
            this.m_beforeStartNotificands.clear();
            this.m_beforeStartNotificands = null;
            this.m_startRecordNotificands.clear();
            this.m_startRecordNotificands = null;
            Scheduler.m_instance = null;
            return;
        }
    }

    private ContextNotificand getContextNotificand(List ctxNotificands, CallerContext cctx)
    {
        ContextNotificand cn = null;
        int cnt = ctxNotificands.size();
        boolean found = false;

        while (((--cnt) > -1) && (!found))
        {
            cn = (ContextNotificand) ctxNotificands.get(cnt);
            if (cn.getContext() == cctx)
            {
                found = true;
            }
        }

        return found ? cn : null;
    }

    void addStartRecordingListener(RecordingAlertListener ral, CallerContext cctx)
    {
        int size = this.m_startRecordNotificands.size();

        for (int i = 0; i < size; i++)
        {
            ContextNotificand ctxNotif = (ContextNotificand) m_startRecordNotificands.get(i);

            if (cctx == ctxNotif.getContext())
            {
                if (ctxNotif.contains(ral) == false)
                {
                    ctxNotif.add(ral);
                    // Add object to signify owned resources. It is only added
                    // once.
                    // Check if the Callback data which is used for monitoring
                    // is installed.
                    if (cctx.getCallbackData(m_instance) == null)
                    {
                        cctx.addCallbackData(new Client(), m_instance);
                    }
                }
                return;
            }
        }

        // Application context not found, create a new Context node
        // Add the listener to the context
        // Add context to list of start recording listeners and return.
        ContextNotificand newCtxNotif = new ContextNotificand(cctx);

        newCtxNotif.add(ral);
        this.m_startRecordNotificands.add(newCtxNotif);
    }

    void addBeforeStartRecordingListener(RecordingAlertListener ral, CallerContext cctx, long time, boolean alwaysNotify)
    {
        synchronized (m_inUseLock)
        {
            BeforeStartNotificand bsNotif = getBeforeStartNotif(time);

            if (bsNotif == null)
            {

                if (log.isDebugEnabled())
                {
                    log.debug("New B4SNotificand");
                }

                bsNotif = new BeforeStartNotificand(time);
                ContextNotificand newCtxNotif = new ContextNotificand(cctx);
                newCtxNotif.add(ral, alwaysNotify);
                bsNotif.add(newCtxNotif);
                m_beforeStartNotificands.add(bsNotif);

                // create AlarmSpec leafs because there was no listener.
                createLeafAlarmSpecs(bsNotif);
            }
            else
            // found bsNotif
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Found B4SNotificand");
                }

                // Register the listener if the ContextNotificand exist
                // and the listener is not registered.
                ContextNotificand ctxNotif = getContextNotificand(bsNotif, cctx);
                if (ctxNotif != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Found ctxNotif");
                    }

                    if (ctxNotif.contains(ral) == false)
                    {
                        ctxNotif.add(ral, alwaysNotify);
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("New ctxNotif");
                    }
                    ContextNotificand newCtxNotif = new ContextNotificand(cctx);
                    newCtxNotif.add(ral, alwaysNotify);
                    bsNotif.add(newCtxNotif);
                }

                if (alwaysNotify)
                {
                    sendAlwaysNotifyEvent(ral, time);
                }
            }

            // Add object to signify owned resources. It is only added
            // once.
            // Check if the Callback data which is used for monitoring is
            // installed.
            if (cctx.getCallbackData(m_instance) == null)
            {
                cctx.addCallbackData(new Client(), m_instance);
            }
        }
    }

    void removeBeforeStartRecordingListener(RecordingAlertListener ral, CallerContext cctx)
    {
        synchronized (m_inUseLock)
        {
            // Next Check all BeforeStartNotificands.
            int size = this.m_beforeStartNotificands.size();
            BeforeStartNotificand bsNotif = null;
            List bsNotifTrash = new ArrayList();
            List ctxNotifTrash = new ArrayList();
            for (int i = 0; i < size; i++)
            {
                bsNotif = (BeforeStartNotificand) m_beforeStartNotificands.get(i);
                ContextNotificand ctxNotif = getContextNotificand(bsNotif, cctx);
                if (ctxNotif != null)
                {
                    if (ctxNotif.remove(ral) == true)
                    {
                        if (ctxNotif.size() == 0)
                        {
                            ctxNotifTrash.add(ctxNotif);
                        }
                    }
                }
                // if the number to be deleted is equal to the number
                int cnt = ctxNotifTrash.size();
                while ((--cnt) > -1)
                {
                    bsNotif.remove(ctxNotifTrash.get(cnt));
                }

                if (bsNotif.size() == 0)
                {
                    bsNotifTrash.add(bsNotif);
                }
                ctxNotifTrash.clear();

            }// next bsNotif

            int x = bsNotifTrash.size();
            while (--x > -1)
            {
                this.m_beforeStartNotificands.remove(bsNotifTrash.get(x));
            }
            bsNotifTrash.clear();
            ctxNotifTrash = null;
            bsNotifTrash = null;
        }
    }

    /*
     * Returns <code>true</code> if the caller has read access to the recording
     * request.
     * 
     * @param rr the <code>RecordingRequest</code> whose accessibility is being
     * tested.
     * 
     * @return <code>true</code> if the caller has read access to the recording
     * request; otherwise returns false.
     * 
     * @see notifyListeners
     */
    private boolean hasReadAccess(RecordingRequest rr, OcapSecurityManager osm, AppID listenerAppID)
    {
        if (null != osm)
        {
            // Retrieving the FAPs should be more straightforward!
            RecordingSpec rs = rr.getRecordingSpec();
            OcapRecordingProperties orp = (OcapRecordingProperties) rs.getProperties();
            return (osm.hasReadAccess(rr.getAppID(), orp.getAccessPermissions(), listenerAppID,
                    OcapSecurityManager.FILE_PERMS_RECORDING));
        }

        return false;
    }

    /**
     * 
     * @author Jeff Spruiel
     * 
     *         A TVTimerSpec for expiration.
     */
    private class ExpirationSpec extends AlarmSpec
    {
        TVTimerSpec retSpec = null;

        TVTimer timer = m_sched.getSystemTimer();

        /**
         * @param sched
         * @param rImpl
         * @param expiration
         */
        public ExpirationSpec(Scheduler sched, RecordingImplInterface rImpl, long expiration)
        {
            super(sched, rImpl);
            setAbsoluteTime(expiration);
            addTVTimerWentOffListener(sched);
        }

        public void schedule() throws TVTimerScheduleFailedException
        {
            retSpec = timer.scheduleTimerSpec(this);
        }

        public void deschedule()
        {
            if (retSpec != null)
            {
                timer.deschedule(retSpec);
                retSpec = null;
            }
        }
    }

    /**
     * @author Jeff Spruiel
     * 
     *         A TVTimerSpec corresponding to a recording's stop time.
     */
    private class StopSpec extends AlarmSpec
    {
        TVTimerSpec retSpec = null;

        TVTimer timer = m_sched.getSystemTimer();

        public StopSpec(Scheduler sched, RecordingImplInterface rImpl)
        {
            super(sched, rImpl);
            if (log.isDebugEnabled())
            {
                log.debug("StopSpec setAbsoluteTime <start,dur> <" + m_rImpl.getRequestedStartTime() + ","
                        + m_rImpl.getDuration() + ">");
            }

            /**
             * Implemenations of the TVTimer may arbitrarily choose trigger
             * order for timer specs which have identical trigger times. If
             * recording A is scheduled to stop at the same time that recording
             * B is scheduled to stop (say, at 8:00PM), then the timer impl may
             * trigger the start timer before the stop timer. This may cause the
             * appearence of a resource contention, even though one usage is
             * just about to terminate.
             * 
             * As a workaround, we artificially shorted the stop timer duration
             * by one second, which is enough to move the stop timer up the
             * list. This causes stop timers to get triggered before start
             * timers in the event that their trigger time is identical.
             * 
             */
            setAbsoluteTime(m_rImpl.getRequestedStartTime() + m_rImpl.getDuration() - 1000);
            addTVTimerWentOffListener(sched);
        }

        public void schedule() throws TVTimerScheduleFailedException
        {
            retSpec = timer.scheduleTimerSpec(this);
        }

        public void deschedule()
        {
            if (retSpec != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("StopSpec deschedule called, retSpec: " + retSpec);
                }
                timer.deschedule(retSpec);
                retSpec = null;
            }
        }

    }

    /**
     * @author Jeff Spruiel
     * 
     *         A TVTimerSpec corresponding to a recording's start time.
     * 
     * 
     * 
     */
    private class StartSpec extends AlarmSpec
    {
        List m_leaves;

        TVTimerSpec retSpec = null;

        TVTimer timer = m_sched.getSystemTimer();

        public StartSpec(Scheduler sched, RecordingImplInterface rImpl)
        {
            super(sched, rImpl);
            setAbsoluteTime(m_rImpl.getRequestedStartTime());
            addTVTimerWentOffListener(sched);
        }

        void addLeaf(BeforeStartSpec spec)
        {
            if (m_leaves == null)
            {
                m_leaves = new ArrayList();
            }
            m_leaves.add(spec);
        }

        public void schedule() throws TVTimerScheduleFailedException
        {
            retSpec = timer.scheduleTimerSpec(this);
        }

        public void deschedule()
        {
            if (retSpec != null)
            {
                timer.deschedule(retSpec);
                retSpec = null;
            }
        }

        void descheduleLeaves()
        {
            if (m_leaves != null)
            {
                int size = m_leaves.size();

                while (--size > -1)
                {
                    BeforeStartSpec bspec = (BeforeStartSpec) m_leaves.get(size);
                    bspec.deschedule();
                }
                m_leaves.clear();
                m_leaves = null;
            }
        }

        void removeLeaf(BeforeStartSpec aspec)
        {
            if (m_leaves != null)
            {
                m_leaves.remove(aspec);
            }
        }

        List getLeaves()
        {
            return (m_leaves != null) ? (List) m_leaves : null;
        }
    }

    /**
     * An object of this type represents a mapping of listeners to a
     * CallerContext.
     */
    private class ContextNotificand
    {
        ArrayList listeners = new ArrayList();

        ArrayList alwaysNotifyListeners = new ArrayList();

        /**
         * <code>m_context</code> a reference to the application context.
         */
        private CallerContext m_context;

        /**
         * Constructor
         * 
         * @param cctx
         *            the context used to initialize an instance of this class.
         */
        ContextNotificand(CallerContext cctx)
        {
            m_context = cctx;
        }

        /**
         * Returns this stored caller context field for registered listeners.
         * 
         * @return the store CallerContext
         */
        CallerContext getContext()
        {
            return m_context;
        }

        int size()
        {
            return listeners.size() + alwaysNotifyListeners.size();
        }

        void clear()
        {
            listeners.clear();
            alwaysNotifyListeners.clear();
        }

        void add(RecordingAlertListener listener)
        {
            listeners.add(0, listener);
        }

        void add(RecordingAlertListener listener, boolean alwaysNotify)
        {
            if (alwaysNotify)
            {
                alwaysNotifyListeners.add(0, listener);
            }
            else
            {
                listeners.add(0, listener);
            }
        }

        boolean remove(RecordingAlertListener listener)
        {
            // Could be in either one, no harm in doing both ...
            return (listeners.remove(listener) || alwaysNotifyListeners.remove(listener));
        }

        boolean contains(RecordingAlertListener listener)
        {
            return (listeners.contains(listener) || alwaysNotifyListeners.contains(listener));
        }

        List getAlwaysNotifyListeners()
        {
            return alwaysNotifyListeners;
        }

        List getAllListeners()
        {
            List allListeners = new ArrayList(listeners);
            allListeners.addAll(alwaysNotifyListeners);
            return allListeners;
        }
    }

    /**
     * A TVTimerSpec corresponding to a recording's before start time. Instances
     * are created for pending recordings only.
     */
    private class BeforeStartSpec extends AlarmSpec
    {
        // Notificand corresponding to the before start time.
        BeforeStartNotificand m_notificand;

        TVTimerSpec retSpec = null;

        TVTimer timer = m_sched.getSystemTimer();

        public BeforeStartSpec(Scheduler sched, RecordingImplInterface rImpl, BeforeStartNotificand notificand,
                long absBeforeStartTime)
        {
            super(sched, rImpl);
            m_notificand = notificand;

            addTVTimerWentOffListener(sched);
            setAbsoluteTime(absBeforeStartTime);

            // AlarmSpec aspec = (AlarmSpec) rImpl.getAlarmSpec();
            // ((StartSpec)aspec).addLeaf(this);
        }

        public void schedule() throws TVTimerScheduleFailedException
        {
            retSpec = timer.scheduleTimerSpec(this);
        }

        public void deschedule()
        {
            if (retSpec != null)
            {
                timer.deschedule(retSpec);
                retSpec = null;
            }
        }

        BeforeStartNotificand getBeforeStartNotificand()
        {
            return m_notificand;
        }
    }

    /**
     * An instance of this class represents a map between a unique before start
     * time to platform-wide registered listeners.
     */
    private class BeforeStartNotificand extends ArrayList
    {

        /**
         * The contructor.
         * 
         * @param cctx
         *            The caller context.
         * @param ral
         *            The listener.
         * @param time
         *            The time in milliseconds before an absolute start time.
         */
        BeforeStartNotificand(long time)
        {
            m_time = time;
        }

        /**
         * Returns true if any of the ContextNotificand objects contain
         * listeners that should always be notified, regardless if their start
         * time has passed.
         */
        boolean hasAlwaysStartListeners()
        {
            int cnt = size();

            for (int i = 0; i < cnt; i++)
            {
                ContextNotificand ctxNotif = (ContextNotificand) get(i);

                if (ctxNotif.getAlwaysNotifyListeners().size() != 0)
                {
                    return true;
                }
            }

            return false;
        }

        /**
         * Returns the before time.
         * 
         * @return the before start time.
         */
        long getBeforeTime()
        {
            return m_time;
        }

        /**
         * <code>m_time</code> the before start time.
         */
        private long m_time;

    }

    private abstract class AlarmSpec extends TVTimerSpec
    {
        protected Scheduler m_sched;

        protected RecordingImplInterface m_rImpl;

        boolean m_enabled;

        public AlarmSpec(Scheduler sched, RecordingImplInterface rImpl)
        {
            super();
            this.setAbsolute(true);
            this.setRegular(true);
            this.setRepeat(false);
            m_sched = sched;
            m_rImpl = rImpl;
            m_enabled = true;
        }

        RecordingImplInterface getRecording()
        {
            return m_rImpl;
        }

        public abstract void schedule() throws TVTimerScheduleFailedException;

        public abstract void deschedule();

        /**
         * Checks to determine if this timer is still scheduled
         * 
         * @return true is timer is still scheduled to trigger
         */
        boolean isEnabled()
        {
            return m_enabled;
        }

        /**
         * disables this timer - indicates that this timer is no longer expected
         * to trigger
         */
        void disable()
        {
            m_enabled = false;
            deschedule();
        }
    }
}
