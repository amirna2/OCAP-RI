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
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.recording.OcapRecordedServiceExt;
import org.cablelabs.impl.manager.recording.RecordedServiceImpl;
import org.cablelabs.impl.manager.recording.SegmentedRecordedServiceImpl;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.mpe.DVRAPI;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.protocol.segrecsvc.DataSource;
import org.cablelabs.impl.media.session.DVRSession;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.media.session.NoSourceException;
import org.cablelabs.impl.media.session.RecordingSession;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.TimeTable;
import org.ocap.service.AlternativeContentErrorEvent;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.SegmentedRecordedService;

/**
 * @author schoonma
 */
public class SegmentedRecordedServicePresentation extends RecordedServicePresentation
{
    private static final Logger log = Logger.getLogger(SegmentedRecordedServicePresentation.class);

    /**
     * The index of the current segment.
     */
    private int currentSegment = -1;

    public SegmentedRecordedServicePresentation(DVRServicePresentationContext pc, boolean showVideo,
            Selection initialSelection, boolean startLive, ScalingBounds bounds, Time startMediaTime, float startRate)
    {
        super(pc, showVideo, initialSelection, startLive, bounds, startMediaTime, startRate);
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

            // Get the segment index for the specified media time.
            DataSource srsds = getSRSDataSource();
            currentSegment = srsds.getSegmentIndex(mediaTime);
            if (Asserting.ASSERTING)
            {
                Assert.condition(currentSegment != -1);
            }

            // Get the segment and start time.
            SegmentedRecordedService srs = (SegmentedRecordedService) srsds.getService();
            RecordedService recsvc = srs.getSegments()[currentSegment];
            Time segmentStartMediaTime = srs.getSegmentMediaTimes()[currentSegment];

            // Compute offset into current segment for start time.
            Time offset = new Time(mediaTime.getNanoseconds() - segmentStartMediaTime.getNanoseconds());

            if (log.isDebugEnabled())
            {
                log.debug("createTimeShiftSession - current segment: " + currentSegment + ", current segment start mediatime=" + segmentStartMediaTime.getSeconds() + "s, offset into current segment="
                        + offset.getSeconds() + "s, all segment start mediatimes: " + Arrays.toString(srs.getSegmentMediaTimes()));
            }

            //use the segment timetable
            TimeTable cciTimeTable = ((OcapRecordedServiceExt)recsvc).getCCITimeTable();

            //presenting a segmented recording - use offset to find timetable entries
            CopyControlInfo precedingEntry = (CopyControlInfo) cciTimeTable.getEntryBefore(offset.getNanoseconds() + 1);
            
            byte cci = precedingEntry != null ? precedingEntry.getCCI() : CopyControlInfo.EMI_COPY_FREELY;
            if (log.isInfoEnabled())
            {
                log.info("createTimeShiftSession - initial CCI for mediatime: " + mediaTime + ": " + cci);
            }

            nextCCIUpdateAlarmMediaTimeNanos = DVRAPI.ALARM_MEDIATIME_NANOS_NOT_SPECIFIED;

            if (rate > 0)
            {
                CopyControlInfo nextEntry = (CopyControlInfo) cciTimeTable.getEntryAfter(offset.getNanoseconds());
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
                CopyControlInfo nextEntry = (CopyControlInfo) cciTimeTable.getEntryBefore(offset.getNanoseconds());
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
            return new CreateSessionResult(false, mediaTime, rate, new RecordingSession(getLock(), sessionListener, selection.getServiceDetails(), getVideoDevice(),
                    (OcapRecordedServiceExt)recsvc, offset, rate, context.getMute(), context.getGain(), cci, nextCCIUpdateAlarmMediaTimeNanos));
        }
    }

    //DO NOT call super.presentTimeShiftSession, as the media time must be set correctly 
    public void presentTimeShiftSession(DVRSession session, Selection selection, Time mediaTime, float rate, boolean mediaTimeChangeRequest)
            throws NoSourceException, MPEException
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("presentTimeShiftSession: " + session + ", " + selection + ", mediatime: " + mediaTime + ", rate: " + rate);
            }
            //attempt to present from a segment may fail due to native failure - if so, and a next segment is available in the direction of play, 
            //attempt to present from the next segment...otherwise, propagate the error
            try
            {
                session.present(selection.getServiceDetails(), selection.getElementaryStreams(null,
                        selection.getServiceComponents()));

                // Adjust Player's clock based on media time / rate from native.
                Time segmentRelativeMediaTime = session.getMediaTime();
                float nativeRate = session.getRate();
                context.clockSetMediaTime(new Time(getSRS().getSegmentMediaTimes()[currentSegment].getNanoseconds() + segmentRelativeMediaTime.getNanoseconds()), mediaTimeChangeRequest);
                context.clockSetRate(nativeRate, false);
            }
            catch (MPEException e)
            {
                //native error trying to present a segment - attempt to use the following segment in the direction of play
                RecordedService[] segments = getSRS().getSegments();
                Time[] startTimes = getSRS().getSegmentMediaTimes();
                //check for postive-rate failure
                if (rate > 0.0F)
                {
                    if (currentSegment < (segments.length - 1))
                    {
                        //increment current segment prior to finding start mediatime and attempt to initiate playback of the next segment
                        currentSegment++;
                        Time newMediaTime = startTimes[currentSegment];
                        if (log.isInfoEnabled())
                        {
                            log.info("Unable to present time shift session from segment: " + currentSegment + " - will attempt to present from next segment - new mediatime: " + newMediaTime, e);
                        }
                        //update the player clock - session not started, any calls to Player.getMediaTime prior to session start 
                        //will get the media time from the player clock - and from the session after it is started
                        //no need to post a MediaTimeSetEvent
                        context.clockSetMediaTime(newMediaTime, false);
                        //notify apps of the interruption
                        sendSegmentInterruptionServiceContextEvents();
                        //update state since we are going to start a new session, and are currently trying to start one
                        
                        startNewSession(selection, selection.getTrigger(), newMediaTime, rate, mediaTimeChangeRequest, false);
                    }
                    else
                    {
                        //switch to live if in progress, still posting altcontent/normalcontent
                        if (getRecordingDataSource().recordingInProgress())
                        {
                            Time liveMediaTime = getDVRDataSource().getLiveMediaTime();
                            if (log.isInfoEnabled())
                            {
                                log.info("Unable to present time shift session from segment: " + currentSegment + ", positive rate and last segment but recording in progress, switching to live - new mediatime : " + liveMediaTime, e);
                            }
                            //update the player clock - session not started, any calls to Player.getMediaTime prior to session start 
                            //will get the media time from the player clock - and from the session after it is started
                            //no need to post a MediaTimeSetEvent
                            context.clockSetMediaTime(liveMediaTime, false);
                            //notify apps of the interruption
                            sendSegmentInterruptionServiceContextEvents();
                            switchToLive(DVRSessionTrigger.MODE, mediaTimeChangeRequest);
                        }
                        else
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("Unable to present time shift session from segment: " + currentSegment + ", positive rate and last segment");
                            }
                            throw e;
                        }
                    }
                }
                //check for negative-rate failure
                else if (rate < 0.0F)
                {
                    if (currentSegment > 0)
                    {
                        Time newMediaTime = new Time((startTimes[currentSegment].getNanoseconds() - 1));
                        if (log.isInfoEnabled())
                        {
                            log.info("Unable to present time shift session from segment: " + currentSegment + " - will attempt to present from previous segment - new mediatime: " + newMediaTime, e);
                        }
                        //decrement after finding the new mediatime by using the 'old' currentSegment index
                        currentSegment--;
                        //update the player clock - session not started, any calls to Player.getMediaTime prior to session start 
                        //will get the media time from the player clock - and from the session after it is started
                        //no need to post a MediaTimeSetEvent
                        context.clockSetMediaTime(newMediaTime, false);
                        //notify apps of the interruption
                        sendSegmentInterruptionServiceContextEvents();
                        startNewSession(selection, selection.getTrigger(), newMediaTime, rate, mediaTimeChangeRequest, false);
                    }
                    else
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("Unable to present time shift session from segment: " + currentSegment + ", negative rate and first segment");
                        }
                        throw e;
                    }
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Unable to present time shift session from segment: " + currentSegment + ", zero rate");
                    }
                    //rate zero - don't attempt to play from a different segment
                    throw e;
                }
            }
        }
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
            super.updateSelectionDetails(selection, mediaTime, rate);
        }
        else
        {
            // update servicedetails & default SIManager
            ServiceDetailsExt updatedDetails = ((SegmentedRecordedServiceImpl) getSRS()).getServiceDetailsForMediaTime(mediaTime.getNanoseconds());
            if (updatedDetails != null)
            {
                selection.update((SIManagerExt) SIManager.createInstance(), updatedDetails);
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to update details - requested details was null");
                }
            }
        }
    }

    private DataSource getSRSDataSource()
    {
        synchronized (getLock())
        {
            return (DataSource) context.getSource();
        }
    }

    private void sendSegmentInterruptionServiceContextEvents()
    {
        //always the same reason
        notifyAlternativeContent(AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
        notifyNormalContent(getDVRContext());
    }
    
    private SegmentedRecordedService getSRS()
    {
        synchronized (getLock())
        {
            return (SegmentedRecordedService) getSRSDataSource().getService();
        }
    }

    protected void handleEndOfFile()
    {
        synchronized (getLock())
        {
            // we may receive endoffile when the segmented recorded service is in deleted or destroyed states
            // calling getSegmentedMediaTimes on a destroyed or deleted segmented recorded service will trigger an IllegalStateException
            try
            {
                // Get the segments and media times.
                RecordedService[] segments = getSRS().getSegments();
                Time[] startTimes = getSRS().getSegmentMediaTimes();

                // If we are already at the last segment, then handle as a
                // regular end-of-file.
                if (currentSegment >= (segments.length - 1))
                {
                    super.handleEndOfFile();
                }
                // If not at last segment, start at the beginning of the next
                // segment.
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("handleEndOfFile - current segment index before transitioning: " + currentSegment + ", segment start times: " + Arrays.toString(startTimes));
                    }
                    // Assign the timeshift session start media time so new
                    // session starts at right location.
                    Time mt = startTimes[currentSegment + 1];
                    // Start the new session.
                    startNewSessionAsync(getCurrentSelection(), DVRSessionTrigger.SEGMENT, mt,
                            context.getClock().getRate(), false, false);
                }
            }
            catch (IllegalStateException ise)
            {
                if (log.isInfoEnabled())
                {
                    log.info("received end of file notification but recording was deleted or destroyed - ignoring");
                }
            }
        }
    }

    protected void handleStartOfFile()
    {
        synchronized (getLock())
        {
            // If we are already at the first segment, then handle as regular
            // beginning-of-file.
            if (currentSegment == 0)
            {
                super.handleStartOfFile();
            }
            // Start from the end of the previous segment.
            else
            {
                // Get start media time of current segment, and substract 1 from
                // it
                // to represent end of previous segment.
                Time[] startTimes = getSRS().getSegmentMediaTimes();
                if (log.isInfoEnabled())
                {
                    log.info("handleStartOfFile - current segment index before transitioning: " + currentSegment + ", segment start times: " + Arrays.toString(startTimes));
                }
                // Assign timeshift session start time so new session starts at
                // the right location.
                Time mt = new Time(startTimes[currentSegment].getNanoseconds() - 1);
                // Start the new session.
                startNewSessionAsync(getCurrentSelection(), DVRSessionTrigger.SEGMENT, mt, context.getClock().getRate(), false, false);
            }
        }
    }

    protected TimeTable getCCITimeTableForCurrentSession()
    {
        DataSource srsds = getSRSDataSource();
        SegmentedRecordedService srs = (SegmentedRecordedService) srsds.getService();
        RecordedServiceImpl recsvc = (RecordedServiceImpl) srs.getSegments()[currentSegment];
        return recsvc.getCCITimeTable();
    }

    /*
     * Don't use super.setsessionmediatime - update the current segment playback session mediatime and update the clock with the correct mediatime
     */
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
                context.clockSetMediaTime(mediaTime, postMediaTimeSetEvent);
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug("setSessionMediaTime(" + mediaTime.getSeconds() + "s)");
            }

            // Determine which segment holds the specified media time.
            DataSource srsds = getSRSDataSource();
            int targetSegment = srsds.getSegmentIndex(mediaTime);
            if (log.isDebugEnabled())
            {
                log.debug("setSessionMediaTime(): currentSegment=" + currentSegment + ", targetSegment="
                        + targetSegment);
            }
            if (Asserting.ASSERTING)
            {
                Assert.condition(targetSegment != -1);
            }

            // If the target segment is the same as the current segment,
            // simply compute the offset and set the time on the current
            // session.
            if (targetSegment == currentSegment)
            {
                // Determine the media time offset within the segment.
                SegmentedRecordedService srs = (SegmentedRecordedService) srsds.getService();

                Time segmentStartTime = srs.getSegmentMediaTimes()[currentSegment];
                Time offsetTime = new Time(mediaTime.getNanoseconds() - segmentStartTime.getNanoseconds());

                if (log.isDebugEnabled())
                {
                    log.debug("setSessionMediaTime(): segmentStart=" + segmentStartTime.getSeconds()
                            + "s, segmentOffset=" + offsetTime.getSeconds() + "s");
                }
                
                // If target is the same as current
                //NOT calling super.setsessionmediatime as the evaluation of cci is relative to this segment's mediatime
                try
                {
                    currentSession.setMediaTime(offsetTime);
                    context.clockSetMediaTime(mediaTime, postMediaTimeSetEvent);
                    evaluateAndSetCCIMediaAlarm(context.getClock().getRate(), offsetTime.getNanoseconds());
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
            else
            {
                // The target segment is different than the current segment,
                // so start a new session.
                startNewSessionAsync(getCurrentSelection(), DVRSessionTrigger.SEGMENT, mediaTime, context.getClock().getRate(), postMediaTimeSetEvent, false);
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
                DataSource srsds = getSRSDataSource();
                SegmentedRecordedService srs = (SegmentedRecordedService) srsds.getService();
                long currentSegmentMediaTimeNanos = currentSession.getMediaTime().getNanoseconds();
                long currentSegmentStartTimeNanos = srs.getSegmentMediaTimes()[currentSegment].getNanoseconds();
                sessionTime = new Time(currentSegmentStartTimeNanos + currentSegmentMediaTimeNanos);
                if (log.isDebugEnabled()) 
                {
                    log.debug("getSessionMediaTime - current segment start time: " + currentSegmentStartTimeNanos + ", segment mediatime: " + currentSegmentMediaTimeNanos + ", returning: " + sessionTime);
                }
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

    public Time getMediaTimeForFrame(Time mt, int direction)
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("getMediaTimeForFrame(time=" + mt.getSeconds() + ", direction=" + direction + ")");
            }

            // Determine which segment holds the specified media time.
            DataSource srsds = getSRSDataSource();
            int targetSegment = srsds.getSegmentIndex(mt);
            if (log.isDebugEnabled())
            {
                log.debug("getMediaTimeForFrame(): currentSegment=" + currentSegment + ", targetSegment="
                        + targetSegment);
            }
            if (Asserting.ASSERTING)
            {
                Assert.condition(targetSegment != -1);
            }

            // Determine the media time offset within the segment.
            SegmentedRecordedServiceImpl srs = (SegmentedRecordedServiceImpl) srsds.getService();
            this.recordingName = ((OcapRecordedServiceExt) (srs.getSegments()[targetSegment])).getNativeName();

            Time segmentStartTime = srs.getSegmentMediaTimes()[currentSegment];
            Time offsetTime = new Time(mt.getNanoseconds() - segmentStartTime.getNanoseconds());

            if (log.isDebugEnabled())
            {
                log.debug("getMediaTimeForFrame(): segmentStart=" + segmentStartTime.getSeconds() + "s, segmentOffset="
                        + offsetTime.getSeconds() + "s" + "nativeName=" + srs.getNativeName());
            }

            return super.getMediaTimeForFrame(offsetTime, direction);
        }
    }
}
