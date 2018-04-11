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

package org.cablelabs.impl.media.player;

import java.io.IOException;
import java.util.Vector;

import javax.media.GainControl;
import javax.media.IncompatibleSourceException;
import javax.media.IncompatibleTimeBaseException;
import javax.media.RateChangeEvent;
import javax.media.Time;
import javax.media.TimeBase;
import javax.media.protocol.DataSource;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.DVRAPIManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.mpe.DVRAPI;
import org.cablelabs.impl.media.presentation.DVRServicePresentationContext;
import org.cablelabs.impl.media.source.DVRDataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.davic.media.MediaTimeEvent;
import org.davic.media.MediaTimeEventControl;
import org.davic.media.MediaTimeEventListener;
import org.ocap.media.MediaTimer;
import org.ocap.media.MediaTimerListener;
import org.ocap.media.S3DConfiguration;
import org.ocap.net.OcapLocator;
import org.ocap.shared.media.BeginningOfContentEvent;
import org.ocap.shared.media.EndOfContentEvent;
import org.ocap.shared.media.EnteringLiveModeEvent;
import org.ocap.shared.media.LeavingLiveModeEvent;

/**
 * This is a framework base class for players that present services from a DVR
 * source&mdash;namely, TSBs and recordings.
 * 
 * @author schoonma
 */
public abstract class AbstractDVRServicePlayer extends AbstractServicePlayer implements DVRServicePresentationContext
{
    private static final Logger log = Logger.getLogger(AbstractDVRServicePlayer.class);
    private GainControlImpl gainControl;

    protected AbstractDVRServicePlayer(CallerContext cc, Object lock, ResourceUsageImpl resourceUsage)
    {
        super(cc, lock, resourceUsage);
        MediaTimeEventControlImpl mediaTimeEventControl = new MediaTimeEventControlImpl();
        gainControl = new GainControlImpl();
        addControls(new ControlBase[] { mediaTimeEventControl, gainControl });
    }

    public void setSource(DataSource ds) throws IncompatibleSourceException, IOException
    {
        // Assign clock initial media time and rate from data source.
        if (ds instanceof DVRDataSource)
        {
            DVRDataSource dvrSrc = (DVRDataSource) ds;
            // If the start time is greater than live mediatime, start the clock
            // at the live media time, rate 1.0
            // otherwise, start it at the specified mediatime & rate
            Time startTime = dvrSrc.getStartMediaTime();
            float startRate = dvrSrc.getStartRate();
            if (startTime.getNanoseconds() > dvrSrc.getLiveMediaTime().getNanoseconds())
            {
                startTime = dvrSrc.getLiveMediaTime();
                startRate = 1.0F;
                if (log.isInfoEnabled())
                {
                    log.info(getId() + "requested startTime was past live mediatime - using live mediatime and rate 1.0");
                }
            }
            if (log.isInfoEnabled())
            {
                log.info(getId() + "setSource - start media time: " + startTime + ", start rate: " + startRate);
            }
            clockSetMediaTime(startTime, false);
            clockSetRate(startRate, false);
        }
        super.setSource(ds);
    }

    public void notifyBeginningOfContent()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "notifyBeginningOfContent - rate: 1.0");
        }
        postEvent(new BeginningOfContentEvent(this, 1));
    }

    public GainControl getGainControl()
    {
        return gainControl;
    }

    public boolean getMute()
    {
        return gainControl.getMute();
    }

    public float getGain()
    {
        return gainControl.getDB();
    }

    public void notifyEndOfContent(float newRate)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "notifyEndOfContent - rate: " + newRate);
        }
        postEvent(new EndOfContentEvent(this, newRate));
    }

    public void notifyEnteringLiveMode()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "notifyEnteringLiveMode");
        }
        postEvent(new EnteringLiveModeEvent(this));
    }

    public void notifyLeavingLiveMode()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "notifyLeavingLiveMode");
        }
        postEvent(new LeavingLiveModeEvent(this));
    }

    /**
     * Is TSB buffering enabled
     * 
     * @return false by default
     */
    public boolean isBufferingEnabled()
    {
        return false;
    }

    protected boolean isServiceBound()
    {
        // DVR-based Players cannot select components outside of the
        // current service because:
        // a. TSB players are always created by a ServiceContext, which
        // automatically
        // disallows selection outside the Service.
        // b. A Recording player may or may not be created by a ServiceContext.
        // If created
        // by a ServiceContext, then it falls under (a). However, if created by
        // an
        // application (using
        // Manager.createPlayer(RecordedService.getMediaLocator()),
        // it isn't clear what should happen. We have decided this is not
        // allowed
        // because it is problematic what it means for a Player to change
        // "personality" when selection components outside the service. In other
        // words,
        // The player has to dynamically change between a broadcast and
        // recording player, and
        // visa versa. However, it is not clear that this behavior is required
        // by the spec.
        // Pat Ladd confirmed, also, that it is implementation-specific what
        // happens in this case.
        // For now, we will disallow it.

        return true;
    }

    /**
     * Used to save the caller of a setRate with a rate != 1.0 for
     * OCAP-DVR-O-0.6.0.862-2
     */
    private CallerContext setRateCallerCtx = null;

    /**
     * An instance of this is callback data is registered for the caller context
     * of last the app that set the rate to a value other than 1. If this app
     * dies while the player is presenting, we want to set the rate back to 1 as
     * described in OCAP-DVR-O-0.6.0862-2 and OCAP-DVR-N-0.5.0819-1.
     */
    class SetRateCB implements CallbackData
    {
        public void destroy(CallerContext ctx)
        {
            synchronized (getLock())
            {
                // Remove this CallbackData instance.
                ctx.removeCallbackData(AbstractDVRServicePlayer.this);

                // If this is being called for a CallerContext that is NOT the
                // one that
                // currently assigned as the setRate() CallerContext, then just
                // ignore.
                if (ctx != setRateCallerCtx) return;

                // If the presentation is not in progress, just return.
                if (isClosed() || !isPresenting()) return;

                // This is THE ONE. So it can be cleared out now.
                setRateCallerCtx = null;

                // if the player is not at rate 1.0, set it back to 1.0
                if (getRate() != 1.0)
                {
                    baseSetRate(1);
                }
            }
        }

        public void active(CallerContext ctx)
        {
        } // ignore

        public void pause(CallerContext ctx)
        {
        } // ignore
    }

    private void baseSetRate(float r)
    {
        super.setRate(r);
    }

    /**
     * Called by base class to see if application is allowed to exectue
     * setRate(), setMediaTime(), setStopTime(), stop(), syncStart() or
     * setTimeBase() using the rules described in OCAP-DVR-O-0.6.0862-2.
     * 
     * @return true if allowed to execute
     */
    private boolean allowForCaller()
    {
        CallerContext callerCtx = ccMgr.getCurrentContext();

        // If there is no ServiceContext associated with this player, it is not
        // a
        // Service related player and therefore cannot be shared by two or more
        // applications. The rules for the OCAP-DVR-O-0.6.0862-2 will not apply
        // in this case so allow all calls.
        if (!isServiceContextPlayer()) return true;

        // If there is no saved setRate( rate ) caller or the current rate is
        // 1.0, allow the api call.
        if (setRateCallerCtx == null || getRate() == 1.0) return true;

        // If the calling application is the same that set the current rate !=
        // 1.0,
        // allow the api call.
        if (setRateCallerCtx == callerCtx) return true;

        // Get aplication priorities.
        Integer callerPriority = (Integer) callerCtx.get(CallerContext.APP_PRIORITY);
        Integer setRateCallerPriority = (Integer) setRateCallerCtx.get(CallerContext.APP_PRIORITY);

        // Get source id for caller. In this check, either the service context
        // or service could
        // be null, in which case, the call is always allowed: Can't apply the
        // Rules if ServiceContext
        // isn't presenting.
        ServiceContext sctx = (ServiceContext) callerCtx.get(CallerContext.SERVICE_CONTEXT);
        if (sctx == null) return true;
        Service svc = sctx.getService();
        if (svc == null) return true;
        OcapLocator ocapLoc = (OcapLocator) svc.getLocator();
        if (ocapLoc == null) return true;
        int source_id_caller = ocapLoc.getSourceID();

        // Get source id for setRate caller.
        sctx = (ServiceContext) setRateCallerCtx.get(CallerContext.SERVICE_CONTEXT);
        if (sctx == null) return true;
        svc = sctx.getService();
        if (svc == null) return true;
        ocapLoc = (OcapLocator) (svc.getLocator());
        if (ocapLoc == null) return true;
        int source_id_setRateCaller = ocapLoc.getSourceID();

        // If the calling application is in the same service and has equal or
        // higher priority than the application that set current rate,
        // allow the call.
        if (source_id_caller == source_id_setRateCaller
                && callerPriority.intValue() >= setRateCallerPriority.intValue()) return true;

        // else block the caller
        return false;

    }

    public float setRate(float rate)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "DVR.setRate(" + rate + ")");
        }

        synchronized (getLock())
        {
            // Determine if allowed; if not, just return current rate.
            if (!allowForCaller())
            {
                float currentRate = getRate();
                postEvent(new RateChangeEvent(this, currentRate));
                return currentRate;
            }

            float setRate = super.setRate(rate);

            // special case of set rate when rate becomes not equal to 1.0 as
            // described in
            // OCAP-DVR-O-0.6.0862-2 and OCAP-DVR-N-0.5.0819-1
            if (setRate != 1.0)
            {
                CallerContext callerCtx = ccMgr.getCurrentContext();

                // if this is a new caller context, save a reference to it and
                // register
                // for termination callback notification
                if (setRateCallerCtx != callerCtx)
                {
                    // if for some reason setRateCallerCtx did not get removed,
                    // remove the callback notification for termination
                    if (setRateCallerCtx != null) setRateCallerCtx.removeCallbackData(AbstractDVRServicePlayer.this); // using
                                                                                                                      // player
                                                                                                                      // as
                                                                                                                      // key

                    // save copy of setRate caller context
                    setRateCallerCtx = callerCtx;

                    // register for setRate caller termination callback
                    setRateCallerCtx.addCallbackData(new SetRateCB(), AbstractDVRServicePlayer.this);// using
                                                                                                     // player
                                                                                                     // as
                                                                                                     // key
                }
            }
            else
            // (setRate == 1.0)
            {
                // Un-register interest in application termination notification
                if (setRateCallerCtx != null) setRateCallerCtx.removeCallbackData(AbstractDVRServicePlayer.this);// for
                                                                                                                 // this
                                                                                                                 // key

                // Remove calling_application reference
                setRateCallerCtx = null;
            }

            return setRate;
        }
    }

    public void setMediaTime(Time mt)
    {
        synchronized (getLock())
        {
            // If not allowed, just return.
            if (allowForCaller()) super.setMediaTime(mt);
        }
    }

    public void setStopTime(Time stopMT)
    {
        synchronized (getLock())
        {
            if (allowForCaller()) super.setStopTime(stopMT);
        }
    }

    public void setTimeBase(TimeBase master) throws IncompatibleTimeBaseException
    {
        synchronized (getLock())
        {
            if (allowForCaller()) super.setTimeBase(master);
        }
    }

    public void stop()
    {
        synchronized (getLock())
        {
            if (allowForCaller()) super.stop();
        }
    }

    public void syncStart(Time tbt)
    {
        synchronized (getLock())
        {
            if (allowForCaller()) super.syncStart(tbt);
        }
    }

    // Helpers

    protected DVRDataSource getDVRDataSource()
    {
        return (DVRDataSource) getSource();
    }

    protected int doGetInputVideoScanMode(int handle)
    {
        return getDVRAPI().getInputVideoScanMode(handle);
    }

    protected S3DConfiguration doGetS3DConfiguration(int handle)
    {
        return getDVRAPI().getS3DConfiguration(handle);
    }

    /**
     * @return Returns the {@link org.cablelabs.impl.media.mpe.DVRAPI}
     *         associated with this player, cast to a {@link DVRAPI}.
     */
    DVRAPI getDVRAPI()
    {
        return (DVRAPIManager) ManagerManager.getInstance(DVRAPIManager.class);
    }

    private class MediaTimeEventControlImpl extends ControlBase implements MediaTimeEventControl
    {
        private Vector timers;

        public MediaTimeEventControlImpl()
        {
            super(true);
            timers = new Vector();
        }

        public void notifyWhen(MediaTimeEventListener i, long mediaTime, int id)
        {
            registerListener(i, mediaTime, id);
        }

        public void notifyWhen(MediaTimeEventListener i, long mediaTime)
        {
            registerListener(i, mediaTime, 0);
        }

        private void registerListener(MediaTimeEventListener l, long time, int id)
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "registering mediaTimeEventListener: " + l + ", mediaTime: " + time + ", id: " + id);
            }
            MediaTimer timer = new MediaTimer(AbstractDVRServicePlayer.this, new MediaTimerListenerImpl(l, time, id));
            timer.setFirstTime(new Time(time));
            timer.setLastTime(new Time(time));
            timer.start();
            timers.add(timer);
        }

        protected void release()
        {
            for (int i = 0; i < timers.size(); i++)
            {
                MediaTimer timer = (MediaTimer) timers.get(i);
                timer.stop();
            }
        }

        private class MediaTimerListenerImpl implements MediaTimerListener
        {
            private long time;

            private int id;

            private MediaTimeEventListener listener;

            public MediaTimerListenerImpl(MediaTimeEventListener l, long mt, int timerId)
            {
                this.listener = l;
                this.time = mt;
                this.id = timerId;
            }

            public void notify(int event, javax.media.Player p)
            {
                if ((event == TIMER_WENTOFF_FIRST || event == TIMER_WENTOFF_LAST) && p == AbstractDVRServicePlayer.this)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(getId() + "notifying mediaTimeEventListener: " + listener + ", mediaTime: " + time + ", id: "
                                + id);
                    }
                    listener.receiveMediaTimeEvent(new MediaTimeEvent(MediaTimeEventControlImpl.this, time, id));
                }
            }
        }
    }
}
