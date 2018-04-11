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
import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.cablelabs.impl.manager.TimeShiftBuffer;
import org.cablelabs.impl.recording.RecordedSegmentInfo;
import org.cablelabs.impl.util.TimeTable;
import org.cablelabs.impl.util.LightweightTriggerEventTimeTable;

public class SequentialMediaTimeStrategy
{
    public static final int SEC_TO_NS = 1000000000;

    public static final int MS_TO_NS = 1000000;

    public static final int SEC_TO_MS = 1000;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(SequentialMediaTimeStrategy.class.getName());

    /**
     * Returns a Vector of events from all the segments. No future events are
     * returned.
     * 
     * @param TimeTable
     *            that links segment duration to a RecordedSegmentInfo
     * @return Vector of events associated with the media time of the event
     */
    public static Vector getMediaTimeAndEventsFromSegments(TimeTable ttSegs)
    {
        Vector mediaTimeVector = new Vector();
        long mediaTimeOfSegmentNs = 0; // nanoseconds from beginning of
                                       // recording
        Enumeration segEnum = ttSegs.elements();
        int lastSegIdx = ttSegs.getSize() - 1;
        int curSegIdx = 0;

        while (segEnum.hasMoreElements())
        {
            RecordedSegmentInfo segInfo = (RecordedSegmentInfo) segEnum.nextElement();

            // duration is in seconds, will be converted
            long durationNs = segInfo.getTimeMillis() * MS_TO_NS;
            // startTime is in system time (milliseconds)
            // long startTimeNs = segInfo.getActualStartTime() * 1000; //
            // milliseconds to nanoseconds
            // all times are now in nanoseconds

            Enumeration lwteEnum = segInfo.getLightweightTriggerEventTimeTable()
                    .getLightWeightTriggerEvents()
                    .elements();
            while (lwteEnum.hasMoreElements())
            {
                LightweightTriggerEvent lwte = (LightweightTriggerEvent) lwteEnum.nextElement();

                // lwte.time is in media time (ns) from beginning of segment.
                // check for future event and don't put them in the returned
                // list.
                /*
                 * lwte.time > durationNs only works when the recording is
                 * completed, but this code is often called while a recording is
                 * in progress. Just skip this check. If events are beyond the
                 * recording bounds we can catch this later.
                 */
                // if(lwte.time > durationNs)
                // {
                // if (Logging.LOGGING)
                // {
                // if(curSegIdx != lastSegIdx)
                // {
                // log.error("getMediaTimeAndEventsFromSegments -- Future event found in segment before the last");
                // }
                // }
                // continue;
                // }

                // convert to media time from beginning of recording.
                long elemMediaTimeNs = lwte.getTimeNanos() + mediaTimeOfSegmentNs;

                LightweightTriggerEvent mediaTimeEvent = new LightweightTriggerEvent(elemMediaTimeNs, lwte);

                mediaTimeVector.addElement(mediaTimeEvent);
            }
            mediaTimeOfSegmentNs += durationNs;
            curSegIdx++;
        }
        return mediaTimeVector;
    }

    public static TimeTable getFutureEvents(long segDurationMS, TimeTable events)
    {
        long segDurationNs = segDurationMS * MS_TO_NS; // 1000000;
        // return back any event past the duration of the segment
        return events.subtableFromTimespan(segDurationNs, Long.MAX_VALUE, false);
    }

    public static TimeTable getMediaTimeAndEventsFromTsbs(ArrayList timeShiftBuffers)
    {
        if (log.isDebugEnabled())
        {
            log.debug("SequentialMediaTimeStrategy.getMediaTimeAndEventsFromTsbs, num tsbs=" + timeShiftBuffers.size());
        }

        TimeTable mediaTimeTimeTable = new TimeTable();
        // int lastTsbIdx = timeShiftBuffers.size() - 1;

        // start from oldest events to newest events to make the insertion of
        // events into TT more efficient
        for (int i = 0; i < timeShiftBuffers.size(); i++)
        {
            TimeShiftBuffer tsb = (TimeShiftBuffer) timeShiftBuffers.get(i);
            if (log.isDebugEnabled())
            {
                log.debug("SequentialMediaTimeStrategy.getMediaTimeAndEventsFromTsbs, tsb[" + i + "]=" + tsb);
            }

            // get the media time from the start of the tsb at which content
            // first is stored
            // long tsbStartMediaTimeNs = tsb.getMediaStartTime();
            // get the media time from the start of the tsb at which content
            // last is stored
            // long tsbEndMediaTimeNs = tsb.getMediaStartTime();
            // acossitt: these aren't used at this stage, need to implement code
            // that removes
            // events not a TSB

            // long tsbDurationNs = tsb.getSize() *
            // SequentialMediaTimeStrategy.SEC_TO_NS; //tsb.getDuration();
            // end and start have offset
            long tsbMediaStartTimeNs = tsb.getContentStartTimeInMediaTime();
            // long tsbMediaEndTimeNs = tsb.getMediaEndTime();

            LightweightTriggerEventTimeTable lwtt = tsb.getLightweightTriggerEventTimeTable();
            if (log.isDebugEnabled())
            {
                log.debug("SequentialMediaTimeStrategy.getMediaTimeAndEventsFromTsbs, tsb[" + i + "] " + "ttc = "
                        + lwtt);
            }

            TimeTable tte = lwtt.getLightWeightTriggerEvents();
            Enumeration eventEnum = tte.elements();
            while (eventEnum.hasMoreElements())
            {
                LightweightTriggerEvent lwte = (LightweightTriggerEvent) eventEnum.nextElement();
                // this is the player media time
                // lwte.time is in nanoseconds from beginning of this TSB (media
                // time from beginning of segment)
                long eventPlayerMediaTimeNs = lwte.getTimeNanos();

                if (log.isDebugEnabled())
                {
                    log.debug("SequentialMediaTimeStrategy.getMediaTimeAndEventsFromTsbs, tsb[" + i + "] " + "lwte = "
                            + lwte);
                }
                // lwte.time is in media time (ns) from beginning of TSB.
                // check for past events no longer in the TSW and don't put them
                // in the returned list.
                if (eventPlayerMediaTimeNs <= tsbMediaStartTimeNs)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("SequentialMediaTimeStrategy.getMediaTimeAndEventsFromTsbs, event before the TSW has been thrown out, "
                                + lwte);
                    }

                    continue;
                }

                // time from buffer start

                if (log.isDebugEnabled())
                {
                    log.debug("SequentialMediaTimeStrategy.getMediaTimeAndEventsFromTsbs, tsb[" + i + "] " + "lwte = "
                            + lwte + ", elemMediaTimeNs = " + eventPlayerMediaTimeNs);
                }

                LightweightTriggerEvent mediaTimeEvent = new LightweightTriggerEvent(eventPlayerMediaTimeNs, lwte);

                mediaTimeTimeTable.addElement(mediaTimeEvent);
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("SequentialMediaTimeStrategy.getMediaTimeAndEventsFromTsbs, returning " + mediaTimeTimeTable);
        }
        return mediaTimeTimeTable;
    }

    /**
     * For moving event from one segment or TSB to the next. Used for moving
     * "future" events to the last segment or TSB.
     * 
     * Note: expects the two segments/TSB to be contiguous in the list of
     * segs/TSBs.
     * 
     * first segment |-------------------.....
     * 
     * ^ future event time
     * 
     * second segment is created but its duration is unknown.
     * |----------------------| |.................. ^ future event is now placed
     * in the new segment
     * 
     * @param currentLocDurMS
     *            duration in milliseconds of the segment or TSB where the event
     *            currently resides
     * @param futureEvent
     *            event setup up to be in the next segment or TSB from where it
     *            currently resides
     * @return
     */
    public static LightweightTriggerEvent getTimeCorrectedEvent(long currentLocDurMS,
            LightweightTriggerEvent futureEvent)
    {
        long correctTime = futureEvent.getTimeNanos() - (currentLocDurMS * MS_TO_NS);
        return new LightweightTriggerEvent(correctTime, futureEvent);
    }
}
