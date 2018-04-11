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
package org.cablelabs.impl.media.session;

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.TimeShiftBuffer;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.PidMapEntry;
import org.cablelabs.impl.util.PidMapTable;

public class TSBSession extends DVRSession
{
    /**
     * Log4J
     */
    private static final Logger log = Logger.getLogger(TSBSession.class);

    /**
     * The time-shift window for which content is being decoded.
     */
    private final TimeShiftWindowClient tsw;

    /**
     * The time-shift buffer (belonging to {@link #tsw}) that is being decoded.
     */
    private final TimeShiftBuffer tsb;

    public TSBSession(Object sync, SessionListener sessionListener, ServiceDetailsExt sdx, VideoDevice vd,
            TimeShiftWindowClient tsw, TimeShiftBuffer tsb, Time startMediaTime, float startRate, boolean mute, float gain, byte cci, long alarmMediaTime)
    {
        super(sync, sessionListener, sdx, vd, startMediaTime, startRate, mute, gain, cci, alarmMediaTime);

        if (Asserting.ASSERTING)
        {
            Assert.condition(tsw != null);
            Assert.condition(tsb != null);
        }

        this.tsw = tsw;
        this.tsb = tsb;

        if (log.isDebugEnabled())
        {
            log.debug("created TSBSession: " + this);
        }
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("TSBSession - tsw: ");
        sb.append(tsw);
        sb.append(", tsb:");
        sb.append(tsb);
        sb.append(", ");
        sb.append(Integer.toHexString(hashCode()));
        sb.append(", ");
        sb.append(super.toString());
        return sb.toString();
    }

    public void present(ServiceDetailsExt sdx, ElementaryStreamExt[] elementaryStreams) throws MPEException
    {
        synchronized (lock)
        {
            try
            {
                if (log.isInfoEnabled())
                {
                    log.info("present: " + sdx + ", " + Arrays.toString(elementaryStreams) + ", " + this);
                }

                if (isStarted())
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("present called when started - ignoring");
                    }
                    return;
                }

                // ignoring return value
                tsw.attachToTSB(tsb);

                PidMapTable table = tsb.getPidMapForMediaTime(startTime.getNanoseconds());
                //details member needs to be set prior to calling pidMapTableFromElementaryStreams
                details = sdx;

                // walk the pidmaptable for the and populate a new pidmaptable
                // from the requested elementary streams
                PidMapTable newTable = pidMapTableFromElementaryStreams(elementaryStreams, table);

                playback = dvrAPI.decodeTSB(this, videoDevice.getHandle(), tsb.getNativeTSBHandle(), newTable, cci, alarmMediaTime,
                        startTime.getNanoseconds(), startRate, false, mute, gain);
                // TODO: The last parameter is the blocked status of the
                // TSB playback upon starup. Rather than hardcode to false
                // blocked must be settable (member variable) in this class.
                started = true;
            }
            catch (MPEMediaError err)
            {
                throw new MPEException("present failed", err);
            }
        }
    }

    public void updatePresentation(Time currentMediaTime, ElementaryStreamExt[] elementaryStreams) throws MPEException,
            NoSourceException
    {
        synchronized (lock)
        {
            try
            {
                if (log.isInfoEnabled())
                {
                    log.info("updatePresentation: " + Arrays.toString(elementaryStreams) + ", " + this);
                }

                if (!isStarted())
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("updatePresentation called when not started - ignoring");
                    }
                    return;
                }

                PidMapTable table = tsb.getPidMapForMediaTime(currentMediaTime.getNanoseconds());
                dvrAPI.playbackChangePids(playback.handle, pidMapTableFromElementaryStreams(elementaryStreams, table));
            }
            catch (MPEMediaError err)
            {
                throw new MPEException("updatePresentation failed", err);
            }
        }
    }

    public void stop(boolean holdFrame)
    {
        if (log.isInfoEnabled())
        {
            log.info("stop: " + this + " - hold frame: " + holdFrame);
        }
        synchronized (lock)
        {
            if (isStarted())
            {
                // Do the base class stop.
                super.stop(holdFrame);
                // Detach from the TSB when this TSBSession is stopped.
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("detach from tsb: " + tsb);
                    }
                    tsw.detachFromTSB(tsb);
                }
                catch (Throwable t)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("recoverable error detaching TSB", t);
                    }
                }
                if (log.isInfoEnabled())
                {
                    log.info("stopped: " + this);
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("stop called when not started - not stopping: " + this);
                }
            }
        }
    }

    private PidMapTable pidMapTableFromElementaryStreams(ElementaryStreamExt[] elementaryStreams, PidMapTable table)
    {
        // table size is requested streams +1 for pcr entry
        PidMapTable newTable = new PidMapTable(elementaryStreams.length + 1);
        newTable.setServiceDetails(details);
        int i = 0;
        for (; i < elementaryStreams.length; i++)
        {
            PidMapEntry thisEntry = table.findEntryBySourcePID(elementaryStreams[i].getPID());
            // copy recorded pid info to source fields
            PidMapEntry newEntry = new PidMapEntry(thisEntry.getStreamType(),
                    thisEntry.getRecordedElementaryStreamType(), thisEntry.getRecordedPID(),
                    PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, PidMapEntry.PID_UNKNOWN,
                    thisEntry.getServiceComponentReference());

            newTable.addEntryAtIndex(i, newEntry);
        }
        // add pcr entry
        PidMapEntry pcrEntry = table.getPCRPidMapEntry();
        PidMapEntry newPcrEntry = new PidMapEntry(pcrEntry.getStreamType(), pcrEntry.getRecordedElementaryStreamType(),
                pcrEntry.getRecordedPID(), PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, PidMapEntry.PID_UNKNOWN,
                pcrEntry.getServiceComponentReference());
        newTable.addEntryAtIndex(i, newPcrEntry);
        // no need for pmt pidmap entry
        return newTable;
    }

    public TimeShiftBuffer getTimeShiftBuffer()
    {
        return tsb;
    }
}
