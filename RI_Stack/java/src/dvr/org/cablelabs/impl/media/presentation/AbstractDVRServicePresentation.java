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

import javax.media.Clock;
import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.media.access.ComponentAuthorization;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.mpe.DVRAPI;
import org.cablelabs.impl.media.mpe.DVRAPIImpl;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.session.BroadcastSession;
import org.cablelabs.impl.media.session.DVRSession;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.media.session.NoSourceException;
import org.cablelabs.impl.media.session.ServiceSession;
import org.cablelabs.impl.media.session.Session;
import org.cablelabs.impl.media.source.DVRDataSource;
import org.cablelabs.impl.media.source.TSBDataSource;
import org.cablelabs.impl.util.TimeTable;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.service.AlternativeContentErrorEvent;

/**
 * This presentation is used to present content from a DVR data source. It
 * operates in either "live" mode (decoding directly from the network interface)
 * or "timeshift" (decoding from TSB, recording, or segment). This is
 * accomplished through the use of the State Pattern, which allows the behavior
 * of an object to change dynamically as its state changes. Here are the main
 * features of how this is implemented by this class:
 * 
 * @author schoonma
 */
public abstract class AbstractDVRServicePresentation extends AbstractServicePresentation
{
    /**
     * log4j Logger
     */
    private static final Logger log = Logger.getLogger(AbstractDVRServicePresentation.class);

    /**
     * This boolean switch indicates whether the presentation is currently live
     * or timeshifted. <code>true</code> means it is live; <code>false</code>
     * means it is timeshifted.
     *
     * When isLive is set to true, make sure to set the player rate to 1.0
     */
    protected boolean isLive = false;

    private boolean decoderStarved;

    protected String recordingName = null;

    static final long ONE_SECOND_NANOS = 1000000000;

    /*
     The mediatime passed to DVRAPIImpl.setAlarm, which will result in a DVRAPI.Event.PLAYBACK_ALARM notification being sent by the platform when the mediatime is encountered
     default to no alarm set.
     
     This field, when not NOT_SPECIFIED, represents a mediatime relative to the beginning of the content being presented (segment-relative if presenting a segment) 
    */
    protected long nextCCIUpdateAlarmMediaTimeNanos = DVRAPI.ALARM_MEDIATIME_NANOS_NOT_SPECIFIED;

    private static final String PRESENT_LIVE_FROM_BUFFER_PARAM = "OCAP.dvr.presentLiveFromBuffer";
    
    protected boolean isPresentLiveFromBuffer;

    /**
     * Extension of {@link SelectionTrigger} to define additional triggers for
     * DVR.
     */
    public static class DVRSessionTrigger extends SelectionTrigger
    {
        /**
         * Session due to switch from live to time-shift mode (or vice versa).
         */
        public static final DVRSessionTrigger MODE = new DVRSessionTrigger("MODE");

        /**
         * Session due to crossing segment boundaries.
         */
        public static final DVRSessionTrigger SEGMENT = new DVRSessionTrigger("SEGMENT");

        protected DVRSessionTrigger(String triggerName)
        {
            super(triggerName);
        }
    }

    /**
     * Construct the session. The parameters are the same as the parent class
     * {@link AbstractServicePresentation#AbstractServicePresentation
     * constructor}, with the addition of one parameter (@param startLive),
     * which indicates whether the presentation should start in live mode.
     * 
     * @param pc
     *            - the {@link DVRServicePresentationContext}
     * @param startShown
     *            - indicates whether the video should be shown at startup
     * @param initialSelection
     *            - initial components to select: true means start live; false
     *            means start time-shifted
     * @param startLive
     *            - whether to start live
     * @param bounds
     *            the initial scaling bounds
     */
    protected AbstractDVRServicePresentation(DVRServicePresentationContext pc, boolean startShown,
            Selection initialSelection, boolean startLive, ScalingBounds bounds, Time startMediaTime, float startRate)

    {
        super(pc, startShown, initialSelection, bounds, startMediaTime, startRate);
        isLive = startLive;
        isPresentLiveFromBuffer = "true".equalsIgnoreCase(PropertiesManager.getInstance().getProperty(PRESENT_LIVE_FROM_BUFFER_PARAM, "false"));
        if (log.isInfoEnabled()) 
        {
            log.info("present live point from buffer: " + isPresentLiveFromBuffer);
        }
    }

    protected CreateSessionResult doCreateSession(Selection selection, Time mediaTime, float rate) throws NoSourceException,
            MPEException
    {
        synchronized (getLock())
        {
            if (selection == null)
            {
                throw new IllegalArgumentException("doCreateSession - null selection");
            }
            if (log.isInfoEnabled())
            {
                log.info("doCreateSession: " + selection + ", mediaTime: " + mediaTime + ", rate: " + rate + ", live: "
                        + isLive + ", presentLiveFromBuffer: " + isPresentLiveFromBuffer);
            }
            // ensure we don't try to use a negative mediatime (set to zero)
            if (mediaTime.getNanoseconds() < 0L)
            {
                if (log.isInfoEnabled())
                {
                    log.info("negative mediaTime requested - updating to zero");
                }
                mediaTime = new Time(0L);
            }
            if (isLive)
            {
                if (isPresentLiveFromBuffer)
                {
                    log.debug("live and presentLiveFromBuffer = true, creating timeshift session");
                    return createTimeShiftSession(selection, mediaTime, rate);                    
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("live and presentLiveFromBuffer = false, creating broadcast session");
                    }
                    //using copy freely CCI for initial broadcast CCI value - will be updated via a CCI async event 
                    byte cci = CopyControlInfo.EMI_COPY_FREELY;
                    // live requires network condition monitor
                    return new CreateSessionResult(false, mediaTime, rate, new BroadcastSession(getLock(), sessionListener, selection.getServiceDetails(),
                            getVideoDevice(), doGetNetworkInterface(), doGetLTSID(), context.getMute(), context.getGain(), cci));
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("not live - creating timeshift session");
                }
                return createTimeShiftSession(selection, mediaTime, rate);
            }
        }
    }

    public void doStartSession(ServiceSession session, Selection selection, Time mediaTime, float rate, boolean mediaTimeChangeRequest, MediaPresentationEvaluationTrigger trigger) throws MPEException, NoSourceException
    {
        synchronized(getLock())
        {
            if (isLive)
            {
                if (isPresentLiveFromBuffer)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("starting timeshift session");
                    }
                    presentTimeShiftSession((DVRSession) session, selection, mediaTime, rate, mediaTimeChangeRequest);
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("starting broadcast session");
                    }
                    presentBroadcastSession((BroadcastSession) session, selection, mediaTimeChangeRequest);
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("starting timeshift session");
                }
                presentTimeShiftSession((DVRSession) session, selection, mediaTime, rate, mediaTimeChangeRequest);
                long startMediaTimeNanos = getStartMediaTimeNanos();
                //dvr switch at beginning, post beginning of content event
                //ensure a call to setMediatime resulting in playback at the boundary of  
                //of buffered content results in beginningofcontent/rate 1.0 - not applicable to setRate
                //in the case of setRate, there is no jump in mediatime, and the platform can post start_of_file
                //if the start of file is encountered
                if (trigger.equals(DVRSessionTrigger.MODE) && mediaTimeChangeRequest)
                {
                    if (mediaTime.getNanoseconds() <= startMediaTimeNanos + (ONE_SECOND_NANOS * rate))
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("doStartSession - mediatime within one second of start mediatime at current rate - notifying beginning of content and setting rate to 1.0");
                        }
                        getDVRContext().notifyBeginningOfContent();
                        float newRate = setSessionRate(1.0F);
                        //no need to call clocksetmediatime here (don't trigger mediatimesetevent), will be triggered by startnewsession w/mediatimechangerequest flag
                        context.clockSetRate(newRate, false);
                    }
                }
            }
        }
    }

    protected void setLive(boolean live)
    {
        if (log.isInfoEnabled())
        {
            log.info("setLive: " + live + ", was: " + isLive);
        }
        isLive = live;
    }

    protected void doUpdateSession(Selection selection) throws NoSourceException, MPEException
    {
        //only getting here if CA is approved (if needed) and MAH is approved (if needed)
        synchronized (getLock())
        {
            if (selection == null)
            {
                throw new IllegalArgumentException("doUpdateSession - null selection");
            }
            if (log.isInfoEnabled())
            {
                log.info("doUpdateSession: " + selection + ", live: " + isLive);
            }
            if (isLive)
            {
                updateBroadcastSession((BroadcastSession) currentSession, selection);
            }
            else
            {
                // use current mediatime & rate
                updateTimeShiftSession((DVRSession) currentSession, selection);
            }
        }
    }

    /**
     * NetworkInterface required to support broadcast service presentation.
     * 
     * Subclasses must implement in order to support presentation of broadcast
     * services
     * 
     * @return networkinterface used to decode broadcast services
     */
    public ExtendedNetworkInterface doGetNetworkInterface()
    {
        throw new IllegalStateException("unexpected call to doGetNetworkInterface");
    }

    /**
     * LTSID is required to support broadcast service presentation.
     * 
     * Subclasses must implement in order to support presentation of broadcast
     * services
     * 
     * @return LTSID used to decode broadcast services
     */
    public short doGetLTSID()
    {
        throw new IllegalStateException("unexpected call to doGetLTSID");
    }

    /**
     * Create a timeshiftsession - will return a CreateSessionResult with non-null session if successful and presentationPointChanged value of false,
     * or presentationPointChanged value of true with a null session (in which case, the createTimeShiftSession method should have
     * 
     * @param selection
     * @param mediaTime
     * @param rate
     * @return service session
     */
    protected abstract CreateSessionResult createTimeShiftSession(Selection selection, Time mediaTime, float rate)
            throws NoSourceException;

    /**
     * Begin presentation of a BroadcastSession and ensure the mediatime is updated
     * 
     * @param session
     * @param selection
     * @param mediaTimeChangeRequest
     * @throws NoSourceException
     * @throws MPEException
     */
    private void presentBroadcastSession(BroadcastSession session, Selection selection, boolean mediaTimeChangeRequest) throws NoSourceException,
            MPEException
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("presentBroadcastSession: " + session + ", " + selection);
            }
            ComponentAuthorization mediaAccessComponentAuth = selection.getMediaAccessComponentAuthorization();
            ElementaryStreamExt[] authorizedStreams = mediaAccessComponentAuth.getAuthorizedStreams();
            session.present(selection.getServiceDetails(), authorizedStreams);
            context.clockSetMediaTime(getDVRDataSource().getLiveMediaTime(), mediaTimeChangeRequest);
            context.clockSetRate(1.0F, false);
        }
    }

    private void updateBroadcastSession(BroadcastSession session, Selection selection) throws NoSourceException,
            MPEException
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("updateBroadcastSession: " + session + ", " + selection);
            }
            ComponentAuthorization mediaAccessComponentAuth = selection.getMediaAccessComponentAuthorization();
            ElementaryStreamExt[] authorizedStreams = mediaAccessComponentAuth.getAuthorizedStreams();
            session.updatePresentation(context.getClock().getMediaTime(), authorizedStreams);
        }
    }

    /**
     * Start a "timeshift" session (
     * {@link org.cablelabs.impl.media.session.DVRSession DVRSession} subtype),
     * at the specified rate and media time.
     * 
     * @param session
     *            - the session to start
     * @param mediaTime
     *            - the starting media time, not segment-relative (possibly from Clock#setMediaTime)
     * @param rate
     *            - the starting playback rate
     *
     * @param mediaTimeChangeRequest if true, implementation should ensure a MediaTimeSetEvent is generated
     * @return Returns a {@link org.cablelabs.impl.media.session.DVRSession
     *         DVRSession} subtype instance.
     * @throws MPEException
     *             native exception
     * @throws NoSourceException
     *             unable to start session
     */
    protected abstract void presentTimeShiftSession(DVRSession session, Selection selection, Time mediaTime, float rate, boolean mediaTimeChangeRequest)
            throws NoSourceException, MPEException;

    /**
     * Update a "timeshift" session (
     * {@link org.cablelabs.impl.media.session.DVRSession DVRSession} subtype),
     * at the specified rate and media time.
     * 
     * @param session
     *            - the session to start
     * @throws MPEException
     *             native exception
     * @throws NoSourceException
     *             unable to start session
     */
    protected abstract void updateTimeShiftSession(DVRSession session, Selection selection) throws NoSourceException,
            MPEException;

    protected Time doGetMediaTime()
    {
        synchronized (getLock())
        {
            if (!isSessionStarted() || isLive)
            {
                return null;
            }
            else
            {
                Time sessionTime = getSessionMediaTime();
                if (sessionTime != null)
                {
                    return sessionTime;
                }
                // unexpected
                if (log.isWarnEnabled())
                {
                    log.warn("doGetMediaTime - session time was null - returning null");
                }
                return null;
            }
        }
    }

    protected void doSetMediaTime(Time mt, boolean postMediaTimeSetEvent)
    {
        synchronized (getLock())
        {
            DVRServicePresentationContext ctx = getDVRContext();
            if (log.isDebugEnabled())
            {
                log.debug("doSetMediaTime: " + mt + ", at live point: " + isLive + ", presentLiveFromBuffer: " + isPresentLiveFromBuffer + ", current mediatime: " +
                        context.getClock().getMediaTime() + ", current rate: " + ctx.getRate());
            }
            // If session change is in progress, wait for it to complete.
            waitForSessionChangeToComplete();
            //get livemediatime before validating
            boolean isLiveMediaTime = isWithinOneSecondOfLiveMediaTime(mt, getDVRDataSource().getLiveMediaTime());
            Time result = validateAcceptableClockMediaTime(mt, ctx.getRate());
            long startMediaTimeNanos = getStartMediaTimeNanos();

            DVRDataSource ds = getDVRDataSource();
            if (log.isDebugEnabled())
            {
                log.debug("doSetMediaTime - acceptable mediatime: " + result + ", live mediatime: " + ds.getLiveMediaTime());
            }

            if (isLive)
            {
                // If already live and live time is specified, just return
                // current media time.
                if (isLiveMediaTime)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("doSetMediaTime - at live point - called with a mediatime representing the live point - updating clock");
                    }
                    context.clockSetMediaTime(result, postMediaTimeSetEvent);
                }
                // Time prior to live point specified
                else
                {
                    //live but presenting from the buffer, call setsessionmediatime
                    if (isPresentLiveFromBuffer)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("doSetMediaTime - at live point - called with a mediatime not representing the live point - calling setSessionMediaTime");
                        }
                        setLive(false);
                        getDVRContext().notifyLeavingLiveMode();
                        setSessionMediaTime(result, postMediaTimeSetEvent);
                        //if a mediatime representing the datasource's start mediatime was returned, post a notifybeginningofcontent event
                        if (result.getNanoseconds() <= startMediaTimeNanos + (ONE_SECOND_NANOS * ctx.getRate()))
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("doSetMediaTime - mediatime updated to start mediatime - notifying beginning of content and setting rate to 1.0");
                            }
                            getDVRContext().notifyBeginningOfContent();
                            float rate = setSessionRate(1.0F);
                            //setSessionMediaTime triggered an update of the clock mediatime and posted the event, update the rate
                            context.clockSetRate(rate, false);
                        }                    
                    }
                    else
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("doSetMediaTime - at live point - called with a mediatime not representing the live point - switching to timeshift");
                        }
                        // Start timeshift at the specified media time.
                        switchToTimeshift(result, ctx.getRate(), false, postMediaTimeSetEvent);
                    }
                }
            }
            else
            // isTimeshift
            {
                if (isLiveMediaTime)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("doSetMediaTime - not at live point - called with a mediatime representing the live point - switching to live");
                    }
                    switchToLive(DVRSessionTrigger.MODE, true);
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("doSetMediaTime - not at live point - called with a mediatime not representing the live point - calling setSessionMediaTime");
                    }
                    setSessionMediaTime(result, postMediaTimeSetEvent);
                    //if a mediatime representing the datasource's start mediatime was returned, post a notifybeginningofcontent event
                    if (result.getNanoseconds() <= startMediaTimeNanos + (ONE_SECOND_NANOS * ctx.getRate()))
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("doSetMediaTime - mediatime updated to start mediatime - notifying beginning of content and setting rate to 1.0");
                        }
                        getDVRContext().notifyBeginningOfContent();
                        float rate = setSessionRate(1.0F);
                        //setSessionMediaTime triggered an update of the clock mediatime and posted the event, update the rate
                        context.clockSetRate(rate, false);
                   }
                }
            }
        }
    }

    protected boolean isWithinOneSecondOfLiveMediaTime(Time inputTime, Time liveMediaTime)
    {
        return (inputTime.getNanoseconds() > (liveMediaTime.getNanoseconds() - ONE_SECOND_NANOS));
    }

    /**
     * An overridable method that allows subclasses to be notified that the rate was updated via a call to SetRate even though a session wasn't started.
     *
     * Will trigger a RatechangeEvent by the Player
     * @param rate the new rate
     */
    protected void handleSessionNotStartedSetRate(float rate)
    {
        //no-op
    }

    /**
     * An overridable method that allows subclasses to be notified that the mediatime was updated.
     *
     * Returns a valid mediatime (constrained by start and end of buffer).
     *
     * @param time
     * @param rate
     * @return time set on the clock
     */
    protected Time validateAcceptableClockMediaTime(Time time, float rate)
    {
        //gate mediatime in
        long startMediaTimeNanos = getStartMediaTimeNanos();
        Time result;
        if (time.getNanoseconds() < startMediaTimeNanos)
        {
            result = new Time(startMediaTimeNanos);
        }
        else
        {
            result = time;
        }
        if (log.isDebugEnabled())
        {
            log.debug("validateAcceptableClockMediaTime to: " + result);
        }

        return result;
    }

    /**
     * Allow subclasses to provide a 'start' mediatime that will be used to determine if beginningofcontent event should be posted
     * @return
     */
    protected abstract long getStartMediaTimeNanos();

    protected boolean validateResources()
    {
        synchronized (getLock())
        {
            if (decoderStarved)
            {
                switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                return false;
            }
            return super.validateResources();
        }
    }

    protected float doSetRate(float rate)
    {
        synchronized (getLock())
        {
            // If session change is in progress, wait for it to complete.
            // If, after the session change completes, the presentation is no
            // longer
            // started, then return the current rate obtained from the context.
            waitForSessionChangeToComplete();

            if (log.isDebugEnabled())
            {
                log.debug("doSetRate(" + rate + "); isLive=" + isLive + ", presentLiveFromBuffer=" + isPresentLiveFromBuffer);
            }

            if (isLive)
            {
                // Can't ffwd into the future.
                if (rate >= 1.0F)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("attempting to set rate to 1.0 or greater but already live - returning rate 1.0");
                    }
                    return 1.0F;
                }
                // Switch to timeshift mode at live point.
                else
                // rate < 1
                {
                    //session may not be started (at live point presenting altcontent), ok to switch to timeshift

                    //already presenting live from a TSB, just set the session rate   
                    if (isPresentLiveFromBuffer)
                    {
                        setLive(false);
                        // Notify that presentation is leaving live mode only if we were
                        // previously live
                        getDVRContext().notifyLeavingLiveMode();
                        if (isSessionStarted())
                        {
                            return setSessionRate(rate);
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("setRate called while not live and session not started, returning: " + rate);
                            }
                            handleSessionNotStartedSetRate(rate);
                            return rate;
                        }
                    }
                    else
                    {
                        // Start time-shift session at the live point at the
                        // specified rate.
                        Time mediaTime = context.getClock().getMediaTime();
                        if (log.isDebugEnabled())
                        {
                            log.debug("at live point and requesting rate less than 1.0 - switching to timeshift with mediatime from player clock: "
                                    + mediaTime + " and rate: " + rate);
                        }
                        switchToTimeshift(mediaTime, rate, true, false);
                        //rate result available because switchtoTimeshift was ran synchronously due to the true rate flag
                        float result = getSessionRate();
                        if (log.isInfoEnabled())
                        {
                            log.info("doSetRate - returning rate: " + result);
                        }
                        return result;
                    }
                }
            }
            else
            {
                //setRate called while not live...if session is started, call setSessionRate..otherwise, just update player clock with requested rate
                if (isSessionStarted())
                {
                    return setSessionRate(rate);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("setRate called while not live and session not started, returning: " + rate);
                    }
                    handleSessionNotStartedSetRate(rate);
                    return rate;
                }
            }
        }
    }

    /**
     * This method is used to transition -from- the 'live point' to the timeshift.
     *
     * Subclasses which must present the live point from the buffer (due to isPresentLiveFromBuffer=true) 
     * may call this superclass method.  Subclasses are responsible for posting LeavingLiveModeEvent,
     * not the callers.  
     * 
     * This method posts a LeavingLiveModeEvent.
     *
     * Authorization is not required if the switch to timeshift is due to a rate change (components haven't changed -
     * component changes will be detected and will trigger re-evaluation).
     *
     * This method must not be called when already in the timeshift.
     *
     * @param mediaTime
     *            - media time at which to start the playback
     * @param rate
     *            - rate at which to start the playback
     *
     * @param rateChangeRequest true if the switch to timeshift requires media access authorization and conditional access checks (false if change is due to rate change)
     * @param mediaTimeChangeRequest true if the switch to timeshift is due to a call to setMediaTime (no authorization checks required)
     */
    protected void switchToTimeshift(Time mediaTime, float rate, boolean rateChangeRequest, boolean mediaTimeChangeRequest)
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("switchToTimeshift(time=" + mediaTime.getSeconds() + "s, rate=" + rate + ")");
            }

            boolean wasLive = isLive;
            // Set flag indicating timeshift mode.
            setLive(false);
            // Stop the current session and start a new session using the current selection (may be presenting from a different timeshift session or live
            doStop(false);
            //if change is due to rate change, use synchronous version and RATE_CHANGE trigger
            //will by pass MAH authorization check
            if (rateChangeRequest)
            {
                //not calling async form
                startNewSession(getCurrentSelection(), DVRSessionTrigger.MODE, mediaTime, rate, mediaTimeChangeRequest, rateChangeRequest);
            }
            else
            {
                startNewSessionAsync(getCurrentSelection(), DVRSessionTrigger.MODE, mediaTime, rate, mediaTimeChangeRequest, false);
            }
            if (wasLive && (wasLive != isLive))
            {
                // Notify that presentation is leaving live mode only if we were
                // previously live
                getDVRContext().notifyLeavingLiveMode();
            }
        }
    }

    /**
     * This method is used to transition -from- the timeshift -to- the 'live point' (live point buffer or network interface).
     * 
     * Subclasses which must present the live point from the buffer (due to isPresentLiveFromBuffer=true) 
     * MUST NOT call this superclass implementation.  
     *
     * Subclasses are responsible for posting both EndOfContentEvent and EnteringLiveModeEvent and setting isLive to true, 
     * not the callers.
     * 
     * This method is NOT used to initiate -initial- playback from the live point.
     *
     * This method must not be called when already at the live point.
     * 
     */
    public void switchToLive(MediaPresentationEvaluationTrigger trigger, boolean mediaTimeChangeRequest)
    {
        synchronized (getLock())
        {
            // Set flag indicating it is now live, THEN call startNewSession
            setLive(true);
            //going to live point, set rate to 1.0 on the player clock
            context.clockSetRate(1.0F, false);

            Time liveMediaTime = getDVRDataSource().getLiveMediaTime();
            //may be switching to live when already at the live point due to authorization change..don't resignal entering live mode in that case
            if (log.isDebugEnabled())
            {
                log.debug("switchToLive - setting rate to 1.0, mediatime to live point, and starting broadcast session - live mediatime: " + liveMediaTime);
            }
            // stop current session and start a new session using the current selection (may be presenting from timeshift)
            //starting a new 'live' session will result in endofcontent notification
            doStop(false);
            startNewSessionAsync(getCurrentSelection(), trigger, liveMediaTime, 1.0F, mediaTimeChangeRequest, false);
            getDVRContext().notifyEnteringLiveMode();
        }
    }

    protected void doStopInternal(boolean shuttingDown)
    {
        if (log.isDebugEnabled())
        {
            log.debug("doStopInternal - removing any existing CCI entry from PODManager for current service");
        }
        ((PODManager) ManagerManager.getInstance(PODManager.class)).removeCCIForService(this);
        super.doStopInternal(shuttingDown);
    }

    /**
     * Override this method to notify the presenting of whether it is entering
     * live or timeshift mode; then call super implementation.
     * 
     * @see org.cablelabs.impl.media.presentation.AbstractServicePresentation#doStart()
     */
    protected void doStart()
    {
        synchronized (getLock())
        {
            if (isLive)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doStart - isLive - switching to live - trigger NEW_SELECTED_SERVICE");
                }

                Time liveMediaTime = getDVRDataSource().getLiveMediaTime();
                startNewSessionAsync(getCurrentSelection(), MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE, 
                    liveMediaTime, 1.0F, false, false);
                getDVRContext().notifyEnteringLiveMode();
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doStart - not isLive - calling super.doStart");
                }
                super.doStart();
            }
        }
    }

    /**
     * Set the media time in a presentation-specific manner. This may involve
     * creating a new session in the case of segmented data sources.
     *
     * Implementations of this method MUST call context.clockSetMediaTime(mediatime,postMediaTimeSetEvent)
     *
     * If setSessionRate and setSessionMediaTime are called one after the other, always update rate first and then mediatime.
     * 
     * @param mediaTime
     *            - the requested media time, as it was passed to
     *            {@link Clock#setMediaTime(Time)}
     *
     * @param postMediaTimeSetEvent
     * @return Returns the actual media time that was assigned, which may
     *         differs from the requested time.
     */
    protected abstract void setSessionMediaTime(Time mediaTime, boolean postMediaTimeSetEvent);

    /**
     * Set the playback rate on the current session.  Implementations do not update the player clock.
     *
     * If setSessionRate and setSessionMediaTime are called one after the other, always update rate first and then mediatime.
     * 
     * @param rate
     *            - the requested rate
     * @return Returns the actual rate assigned, which may differ fom the
     *         requested rate (will be the rate returned by the player clock if
     *         the session is not started)
     */
    protected abstract float setSessionRate(float rate);

    /**
     * This function returns the media time of the closest renderable frame to
     * the given mediaTime in the given direction. It is expected that calling
     * mpeos_dvrPlaybackGetTime() after calling mpeos_dvrPlaybackSetTime() with
     * a value returned from this function will result in the same value
     * returned in frameTime.
     * 
     * @param original
     *            original time
     * @param i
     *            direction
     * @return media time of closest renderable frame
     */
    public Time getMediaTimeForFrame(Time original, int i)
    {
        synchronized (getLock())
        {
            // implements support for TSB datasources..subclasses must implement
            // support for other datasources
            int nativeHandle = ((TSBDataSource) getDVRDataSource()).getTSW().getBufferingTSBHandle();
            return DVRAPIImpl.getInstance().getTsbMediaTimeForFrame(nativeHandle, original, i);
        }
    }

    /**
     * This function steps one video frame forward or backward on a paused
     * playback session. The next video frame may be the next fully-coded frame
     * (e.g. an MPEG-2 I/P frame) or an intermediate frame, if the platform
     * supports it. After a successful call, the media time returned by
     * mpeos_dvrPlaybackGetTime() must reflect the selected frame.
     * 
     * @param direction
     *            the direction
     * @return true if stepframe was successful
     */
    public boolean stepFrame(int direction)
    {
        synchronized (getLock())
        {
            if (!isSessionStarted())
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to call stepFrame: " + direction + " when session is not started");
                }
                return false;
            }
            boolean result = false;
            if (log.isDebugEnabled())
            {
                log.debug("stepFrame(direction=" + direction + ")");
            }

            try
            {
                if (0 != currentSession.getRate())
                {
                    // Can't step frame unless session is paused.
                    return false;
                }

                result = ((DVRSession) currentSession).stepFrame(direction);

            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("stepFrame(direction=" + direction + " FAILED");
                }
            }
            return result;
        }
    }

    /**
     * Get the media time in a presentation-specific manner or null if the time is not available from the session.
     * 
     * @return Returns the actual media time which can be used to set the Player clock mediatime (not segment-specific)
     */
    protected abstract Time getSessionMediaTime();

    private float getSessionRate()
    {
        try
        {
            if (isSessionStarted())
            {
                return currentSession.getRate();
            }
        }
        catch (MPEException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("current session exists but unable to retrieve rate", e);
            }
        }
        if (log.isInfoEnabled())
        {
            log.info("session not started or failure to get session rate - returning player clock rate");
        }
        return context.getClock().getRate();
    }

    protected DVRServicePresentationContext getDVRContext()
    {
        synchronized (getLock())
        {
            return (DVRServicePresentationContext) context;
        }
    }

    protected DVRDataSource getDVRDataSource()
    {
        synchronized (getLock())
        {
            return (DVRDataSource) context.getSource();
        }
    }

    /*
     * Native Event Handling
     */

    protected String eventToString(int event)
    {
        switch (event)
        {
            case DVRAPI.Event.END_OF_FILE:
                return "DVR.END_OF_FILE";
            case DVRAPI.Event.START_OF_FILE:
                return "DVR.START_OF_FILE";
            case DVRAPI.Event.SESSION_CLOSED:
                return "DVR.SESSION_CLOSED";
            case DVRAPI.Event.PLAYBACK_PIDCHANGE:
                return "DVR.PLAYBACK_PIDCHANGE";
            default:
                return super.eventToString(event);
        }
    }

    protected void handleSessionEventAsync(Session session, int event, int data1, int data2)
    {
        synchronized (getLock())
        {
            switch (event)
            {
                case MediaAPI.Event.DECODER_STARVED:
                    if (session != currentSession || !isSessionStarted())
                    {
                        return;
                    }
                    logAsyncEvent(event, data1, data2);
                    handleDecoderStarvedNotification();
                    break;
                case MediaAPI.Event.DECODER_NO_LONGER_STARVED:
                    if (session != currentSession || !isSessionStarted())
                    {
                        return;
                    }
                    logAsyncEvent(event, data1, data2);
                    handleDecoderNoLongerStarvedNotification();
                    break;
                case DVRAPI.Event.END_OF_FILE:
                    // Event must be for current session.
                    if (session != currentSession || !isSessionStarted())
                    {
                        return;
                    }
                    logAsyncEvent(event, data1, data2);
                    handleEndOfFile();
                    break;

                case DVRAPI.Event.START_OF_FILE:
                    // Event must be for current session.
                    if (session != currentSession || !isSessionStarted())
                    {
                        return;
                    }
                    logAsyncEvent(event, data1, data2);
                    handleStartOfFile();
                    break;

                case DVRAPI.Event.PLAYBACK_PIDCHANGE:
                    // Event must be for current session.
                    if (session != currentSession || !isSessionStarted())
                    {
                        return;
                    }
                    logAsyncEvent(event, data1, data2);
                    // logic identical to PMT_CHANGED handling in
                    // TSBServicePresentation
                    reselect(MediaPresentationEvaluationTrigger.PMT_CHANGED);
                    break;

                case DVRAPI.Event.SESSION_CLOSED:
                    // Event must be for current session.
                    if (session != currentSession || !isSessionStarted())
                    {
                        return;
                    }
                    logAsyncEvent(event, data1, data2);
                    handleSessionClosed();
                    break;

                case DVRAPI.Event.PLAYBACK_ALARM:
                    // Event must be for current session.
                    if (session != currentSession || !isSessionStarted())
                    {
                        return;
                    }
                    logAsyncEvent(event, data1, data2);
                    handleAlarm();
                    break;
                
                case MediaAPI.Event.CCI_UPDATE:
                    if (session != currentSession || !isSessionStarted())
                    {
                        return;
                    }
                    logAsyncEvent(event, data1, data2);
                    //cci passed in data2
                    handleCCIUpdate((byte)(data2 & 0xFF));
                    break;
                
                default:
                    // Not handled by this class, so pass up to the parent to
                    // handle.
                    super.handleSessionEventAsync(session, event, data1, data2);
            }
        }
    }

    private void handleCCIUpdate(byte cci)
    {
        synchronized (getLock())
        {
            if (isLive)
            {
                if (log.isInfoEnabled())
                {
                    log.info("handleCCI update: " + cci + " - updating CCI on the session and on PODManager for the current service");
                }
                ((BroadcastSession)currentSession).setCCI(cci);
                ((PODManager) ManagerManager.getInstance(PODManager.class)).setCCIForService(this, getCurrentSelection().getServiceDetails(), cci);
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("received cci update but not live - ignoring - cci: " + cci);
                }
            }
        }
    }

    protected void handleAlarm()
    {
        synchronized(getLock())
        {
            //CCI boundary encountered, update current CCI and reset alarm time so a new alarm can be set 
            long oldCCIUpdateAlarmMediaTimeNanos = nextCCIUpdateAlarmMediaTimeNanos;

            //dvr alarm is now cancelled
            //reset alarm - will be reset if needed when alarm is re-evaluated
            nextCCIUpdateAlarmMediaTimeNanos = DVRAPI.ALARM_MEDIATIME_NANOS_NOT_SPECIFIED;
            if (log.isInfoEnabled())
            {
                log.info("handleAlarm - mediatime nanos: " + oldCCIUpdateAlarmMediaTimeNanos + ", resetting alarm nanos to -1");
            }

            if (!isSessionStarted())
            {
                if (log.isInfoEnabled())
                {
                    log.info("handleAlarm - session not started - ignoring");
                }
                return;
            }
            
            if (isLive)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("received alarm notification but live - ignoring");
                }
                return;
            }

            TimeTable timeTable = getCCITimeTableForCurrentSession();
            if (timeTable == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("handleAlarm - no timetable available for mediatime nanos - unable to update CCI or set alarm - current alarm nanos: " + oldCCIUpdateAlarmMediaTimeNanos);
                }
                return;
            }
            //update CCI - expecting an entry at the exact old alarm mediatime nanos
            CopyControlInfo entry = (CopyControlInfo) timeTable.getEntryAt(oldCCIUpdateAlarmMediaTimeNanos);
            if (entry != null)
            {
                byte cci = entry.getCCI();
                if (log.isDebugEnabled())
                {
                    log.debug("handleAlarm - calling setCCI with: " + cci + ", from timetable nanos: " + oldCCIUpdateAlarmMediaTimeNanos);
                }
                ((DVRSession)currentSession).setCCI(cci);
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("handleAlarm - no CCI entry found for alarm nanos: " + oldCCIUpdateAlarmMediaTimeNanos + " - unable to set CCI");
                }
            }
            //check to see if another alarm needs to be set
            evaluateAndSetCCIMediaAlarm(context.getClock().getRate(), oldCCIUpdateAlarmMediaTimeNanos);
        }
    }

    /**
     * Evaluate the cci timetable for the next CCI entry in rate direction relative to sessionRelativeMediaTime time
     * @param rate the rate to use when finding an entry
     * @param sessionRelativeMediaTime the value to use to find the correct entry in the timetable
     */
    void evaluateAndSetCCIMediaAlarm(float rate, long sessionRelativeMediaTime)
    {
        synchronized (getLock())
        {
            if (!isSessionStarted())
            {
                if (log.isInfoEnabled())
                {
                    log.info("evaluateAndSetCCIMediaAlarm - rate: " + rate + ", mediatime: " + sessionRelativeMediaTime + " - session is not started");
                }
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug("evaluateAndSetCCIMediaAlarm - rate: " + rate + ", sessionRelativeMediaTime: " + sessionRelativeMediaTime + ", current CCI alarm nanos: " + nextCCIUpdateAlarmMediaTimeNanos);
            }
            
            //default new alarm to current alarm - if different, alarm will be updated
            long newCCIUpdateAlarmMediaTimeNanos = nextCCIUpdateAlarmMediaTimeNanos;

            TimeTable cciTimeTable = getCCITimeTableForCurrentSession();
            if (rate > 0)
            {
                CopyControlInfo nextEntry = (CopyControlInfo) cciTimeTable.getEntryAfter(sessionRelativeMediaTime);
                if (nextEntry != null)
                {
                    newCCIUpdateAlarmMediaTimeNanos = nextEntry.getTimeNanos();
                    if (log.isDebugEnabled())
                    {
                        log.debug("evaluateAndSetCCIMediaAlarm - positive rate and CCI in direction of play - using CCI alarm nanos: " + newCCIUpdateAlarmMediaTimeNanos);
                    }
                }
                else
                {
                    newCCIUpdateAlarmMediaTimeNanos = DVRAPI.ALARM_MEDIATIME_NANOS_NOT_SPECIFIED;
                    if (log.isDebugEnabled())
                    {
                        log.debug("evaluateAndSetCCIMediaAlarm - positive rate and no CCI in direction of play - resetting CCI alarm");
                    }
                }
            }
            else if (rate < 0)
            {
                CopyControlInfo nextEntry = (CopyControlInfo) cciTimeTable.getEntryBefore(sessionRelativeMediaTime);
                if (nextEntry != null)
                {
                    //ignore alarm at zero, which would be handled via beginning of file notification
                    if (nextEntry.getTimeNanos() != 0)
                    {
                        newCCIUpdateAlarmMediaTimeNanos = nextEntry.getTimeNanos();
                        if (log.isDebugEnabled())
                        {
                            log.debug("evaluateAndSetCCIMediaAlarm - negative rate and CCI in direction of play - using CCI alarm nanos: " + newCCIUpdateAlarmMediaTimeNanos);
                        }
                    }
                }
                else
                {
                    newCCIUpdateAlarmMediaTimeNanos = DVRAPI.ALARM_MEDIATIME_NANOS_NOT_SPECIFIED;
                    if (log.isDebugEnabled())
                    {
                        log.debug("evaluateAndSetCCIMediaAlarm - negative rate and no CCI in direction of play - resetting CCI alarm");
                    }
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("evaluateAndSetCCIMediaAlarm - zero rate - not setting alarm");
                }
            }
            if (newCCIUpdateAlarmMediaTimeNanos != nextCCIUpdateAlarmMediaTimeNanos)
            {
                if (log.isInfoEnabled())
                {
                    log.info("evaluateAndSetCCIMediaAlarm - updating CCI alarm nanos: " + newCCIUpdateAlarmMediaTimeNanos);
                }
                nextCCIUpdateAlarmMediaTimeNanos = newCCIUpdateAlarmMediaTimeNanos;
                ((DVRSession)currentSession).setAlarm(nextCCIUpdateAlarmMediaTimeNanos);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("evaluateAndSetCCIMediaAlarm - not updating CCI alarm - current CCI alarm nanos: " + nextCCIUpdateAlarmMediaTimeNanos);
                }
            }
        }
    }

    /**
     * Retrieve a TimeTable for the provided time.   
     * @return
     */
    protected abstract TimeTable getCCITimeTableForCurrentSession();

    ExtendedNetworkInterface getNetworkInterface()
    {
        return doGetNetworkInterface();
    }

    private void logAsyncEvent(int event, int data1, int data2)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("handleSessionEventAsync - event: " + eventToString(event)
                        + ", data1: " + data1 + ", data2: " + data2 +  " - session: " + currentSession);
            }
        }
    }

    private void handleDecoderStarvedNotification()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("decoder starved - not presenting alt content and components valid - switching to alt content");
            }
            switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_RENDER_BLACK, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
            decoderStarved = true;
        }
    }

    private void handleDecoderNoLongerStarvedNotification()
    {
        synchronized (getLock())
        {
            decoderStarved = false;
            if (log.isInfoEnabled())
            {
                log.info("decoder no longer starved - presenting alt content, components and state valid");
            }
            reselect(SelectionTrigger.RESOURCES);
        }
    }

    protected abstract void handleEndOfFile();

    protected abstract void handleStartOfFile();

    protected abstract void handleSessionClosed();

    public void setMute(boolean mute)
    {
        currentSession.setMute(mute);
    }

    public float setGain(float gain)
    {
        return currentSession.setGain(gain);
    }
}
