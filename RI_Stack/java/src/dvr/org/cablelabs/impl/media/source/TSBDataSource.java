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

package org.cablelabs.impl.media.source;

import java.util.Enumeration;

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.TimeShiftBuffer;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.service.javatv.selection.DVRServiceContextExt;

/**
 * This is a {@link DVRDataSource DVRDataSource}
 * that delivers content from a time-shift buffer (@link
 * org.cablelabs.impl.manager.TimeShiftWindowClient TimeShiftWindowClient}.
 * 
 * @author schoonma
 */
public class TSBDataSource extends DVRDataSource implements Asserting
{
    /** log4j logging */
    private static final Logger log = Logger.getLogger(TSBDataSource.class);

    /** This is the {@link DVRServiceContextExt} that created the data source. */
    private final DVRServiceContextExt dvrCtx;

    /**
     * The cached {@link TimeShiftWindowClient} from the
     * {@link DVRServiceContextExt} that created the data source; assigned when
     * {@link #connect()} is called.
     */
    private final TimeShiftWindowClient timeShiftWindowClient;

    /**
     * Construct a {@link TSBDataSource} from the parameters.
     * 
     * @param ctx
     *            - The time-shift buffer (@link TimeShiftWindowClient} that
     *            delivers content.
     * @param mt
     *            - the media time at which playback should commence; if this is
     *            set to a value greater than the live mediatime, it should
     *            start at the live point.
     * @param rate
     *            - the rate at which playback should commence.
     */
    public TSBDataSource(DVRServiceContextExt ctx, Time mt, float rate)
    {
        super(mt, rate);
        if (ASSERTING)
        {
            Assert.condition(ctx != null);
            Assert.condition(mt != null);
        }

        dvrCtx = ctx;
        // reserve for buffered playback
        // Note: Not registering a listener initially
        timeShiftWindowClient = dvrCtx.getTimeShiftWindowClient().addClient(
                TimeShiftManager.TSWUSE_BUFFERPLAYBACK | TimeShiftManager.TSWUSE_LIVEPLAYBACK, 
                null, TimeShiftManager.LISTENER_PRIORITY_DEFAULT );
    }

    public void disconnect()
    {
        timeShiftWindowClient.release();
        super.disconnect();
    }

    // private static long tswAttachCount = 0;

    public String getContentType()
    {
        return "ocap.tsb";
    }

    public Time getBeginningOfBuffer()
    {
        try
        {
            TimeShiftBuffer tsb = getTSW().getFirstTSB();
            if (tsb == null)
            {
                return new Time(0);
            }

            return new Time(tsb.getContentStartTimeInMediaTime() + tsb.getTSWStartTimeOffset());
        }
        catch (Exception e)
        {
            return new Time(0);
        }
    }

    public Time getEndOfBuffer()
    {
        try
        {
            TimeShiftBuffer tsb = getTSW().getLastTSB();
            if (tsb == null)
            {
                return new Time(0);
            }

            return new Time(tsb.getContentEndTimeInMediaTime() + tsb.getTSWStartTimeOffset());
        }
        catch (Exception e)
        {
            return new Time(0);
        }
    }

    // returns the current duration of the recorded content
    public Time getDuration()
    {
        try
        {
            Enumeration tsbs = getTSW().elements();
            if (tsbs == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getDuration - null elements");
                }
                return new Time(0);
            }
            long duration = 0;
            while (tsbs.hasMoreElements())
            {
                TimeShiftBuffer tsb = (TimeShiftBuffer) tsbs.nextElement();
                if (tsb != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getDuration - found tsb: " + tsb + " - adding duration: " + tsb.getDuration());
                    }
                    duration += tsb.getDuration();
                }
            }
            if (log.isDebugEnabled())
            {
                log.debug("getDuration returning: " + duration);
            }
            return new Time(duration);
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getDuration - unable to get TSB - Returning 0");
            }
            return new Time(0);
        }
    }

    // returns the maximum duration (limit) of recorded content
    public Time getMaxDuration()
    {
        try
        {
            double seconds = (double) getTSW().getSize();
            if (log.isDebugEnabled())
            {
                log.debug("getMaxDuration returning: " + seconds);
            }
            return new Time(seconds);
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getMaxDuration - unable to get TSW size - returning 0");
            }
            return new Time(0);
        }
    }

    public boolean shouldStartLive()
    {
        return isLiveMediaTime(getStartMediaTime());
    }

    public Time getLiveMediaTime()
    {
        return new Time(getTSW().getTimeBase().getNanoseconds() - getTSW().getTimeBaseStartTime());
    }

    /**
     * @return Returns the {@link #timeShiftWindowClient} asssigned to the
     *         {@link TSBDataSource}.
     */
    public TimeShiftWindowClient getTSW()
    {
        return timeShiftWindowClient;
    }

    public void requestBuffering()
    {
        dvrCtx.requestBuffering();
    }
}
