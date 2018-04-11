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

package org.cablelabs.impl.util;

import java.io.Serializable;
import java.util.Enumeration;

import org.apache.log4j.Logger;

import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventStoreWrite;
import org.cablelabs.impl.manager.lightweighttrigger.SequentialMediaTimeStrategy;
import org.cablelabs.impl.manager.service.SISnapshotManager;

/**
 * This class contains all the TimeTables needed for a recording or TSB. The
 * idea is that TimeTable.elements are not all stored in one large TimeTable,
 * but that TimeTable.elements containing different types of information are
 * stored in their own buckets. These buckets are derived from TimeTableBucket
 * and this bucket is stored in a vector in the TimeTableCollection object. So,
 * for example, LightweightTriggerEvents will be stored in the TimeTable of a
 * LightweightTriggerEventBucket, etc.
 * <p>
 * You can have several elements of the same class stored in the
 * TimeTableCollection's vector but you will need to create a class derived from
 * TimeTableBucket that has enough metadata to distinguish between the different
 * objects of the same class.
 * <p>
 * If serialization of TimeTableCollection is needed the objects stored in the
 * TimeTables will need to be serializable.
 * <p>
 * Only objects that should be serialized as part of the TimeTableCollection
 * should be placed in it. Entities such as PidTables should not be in here
 * since they are persisted in other forms.
 */
public class LightweightTriggerEventTimeTable extends TimeTable implements Serializable,
        LightweightTriggerEventStoreWrite
{
    private static final long serialVersionUID = 8717853581884504629L;

    // private final TimeTable lwteTT =
    // LightWeightTriggerEventTimeTable.newTimeTable();

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(LightweightTriggerEventTimeTable.class.getName());

    /**
     * Default construction of a LightWeightTriggerEventTimeTable
     * 
     */
    public LightweightTriggerEventTimeTable()
    {
    } // END LightWeightTriggerEventTimeTable

    public LightweightTriggerEventTimeTable(int initialCapacity)
    {
        super(initialCapacity);
    }

    public boolean addLightweightTriggerEvent(LightweightTriggerEvent lwte)
    {
        if (log.isDebugEnabled())
        {
            log.debug("LightWeightTriggerEventTimeTable.addLightweightTriggerEvent");
        }
        if (checkStore(lwte))
            return false; // already exists, so don't add

        this.addElement(lwte);
        return true;
    }

    /**
     * cacheLightweightTriggerEvent
     * 
     * Not implemented. Don't call
     */
    public boolean cacheLightweightTriggerEvent(Object src, LightweightTriggerEvent lwte)
    {
        return false;
    }

    public void store(Object src)
    {
        // do nothing
    }

    private boolean checkStore(LightweightTriggerEvent lwte)
    {
        // use elements() for thread safety
        Enumeration lwteEnum = this.elements();
        while (lwteEnum.hasMoreElements())
        {
            LightweightTriggerEvent storedEvent = (LightweightTriggerEvent) lwteEnum.nextElement();
            if (storedEvent.id == lwte.id || storedEvent.eventName.equals(lwte.eventName)) return true;
        }
        return false; // nothing found, okay to add
    }

    public void addLightweightTriggerEvents(TimeTable tt)
    {
        this.merge(tt);
    }

    /**
     * Thread-safe method to get a TimeTable of LightWeightTriggerEvents and
     * their activation time in system time.
     * 
     * @return A TimeTable associating the event's system time with the
     *         LightweightTriggerEvent
     */
    public TimeTable getLightWeightTriggerEvents()
    {
        return this.getThreadSafeCopy();
    }

    /**
     * In our design, future events always reside in the last TSB/Segment. Thus,
     * when a new TSB/Segment is added, future events need to be moved to the
     * new "last" TSB/Segment.
     * 
     * Future events means events whose media time put them outside the end of
     * the current TSW or recording. Until the recording or time shift window
     * ends we don't know the value of the actual media time end, so we store
     * the future events in the last TSB/Segment until the total length can be
     * resolved.
     * 
     * This method moves future LightweightTriggerEvents from nextToLastEvents
     * to this LightWeightTriggerEventTimeTable. This usually occurs when a new
     * segment or TSB is added and the future events that are stored in the next
     * to last TSB/Segment need to be moved to the last TSB/Segment. This
     * previous TSB/Segment is the next to last item.
     * 
     * @param nextToLastDurSec
     * @param nextToLastEvents
     */
    public void moveFutureLwte(long nextToLastDurMS, LightweightTriggerEventTimeTable nextToLast)
    {
        // work through the list of events in the nextToLast
        // LightWeightTriggerEventTimeTable and move any future events.
        TimeTable nextToLastTimeTable = nextToLast.getLightWeightTriggerEvents();
        TimeTable lastTimeTable = getLightWeightTriggerEvents();
        TimeTable futureEvents = SequentialMediaTimeStrategy.getFutureEvents(nextToLastDurMS, nextToLastTimeTable);
        /*
         * first TSB/Segment |-------------------.....
         * 
         * ^ future event time
         * 
         * second TSB/Segment is created but its duration is unknown. Duration
         * of first TSB/Segment is now known. |----------------------|
         * |.................. ^ future event is now placed in the new
         * TSB/Segment and may or may not end up being in this new TSB/Segment
         * when the new TSB/Segment duration is known.
         */

        Enumeration futureEnum = futureEvents.elements();
        while (futureEnum.hasMoreElements())
        {
            LightweightTriggerEvent futureEvent = (LightweightTriggerEvent) futureEnum.nextElement();
            // remove future event from next to last
            nextToLastTimeTable.removeElement(futureEvent);
            // play it safe and clone the event and put it in the last
            // collection. Cloning probably is
            // not necessary since the event shouldn't be in any TimeTable but
            // better safe then
            // spending hours debugging a weird side-effect bug.
            LightweightTriggerEvent correctFutureEvent = SequentialMediaTimeStrategy.getTimeCorrectedEvent(
                    nextToLastDurMS, futureEvent);
            lastTimeTable.addElement(correctFutureEvent);
        }
    }

    // public String toString()
    // {
    // StringBuffer sb = new
    // StringBuffer("LightWeightTriggerEventTimeTable 0x");
    // sb.append(Integer.toHexString(this.hashCode()));
    // sb.append(":{");
    // sb.append(lwteTT);
    // sb.append('}');
    // return sb.toString();
    // }
}
