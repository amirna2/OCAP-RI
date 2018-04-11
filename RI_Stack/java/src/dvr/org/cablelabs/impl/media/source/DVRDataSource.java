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

import javax.media.Time;

import org.apache.log4j.Logger;

/**
 * This is the abstract base class of all DVR-related data sources. It defines
 * methods that are unique to DVR data sources.
 * 
 * @author schoonma
 */
public abstract class DVRDataSource extends ServiceDataSource
{
    private static final Logger log = Logger.getLogger(DVRDataSource.class);

    protected Time startMediaTime;

    private float startRate;

    public DVRDataSource(Time startMediaTime, float startRate)
    {
        if (log.isDebugEnabled())
        {
            log.debug("datasource startMediaTime: " + startMediaTime + ", startRate: " + startRate);
        }
        this.startMediaTime = startMediaTime;
        this.startRate = startRate;
    }

    public DVRDataSource()
    {
        this.startMediaTime = new Time(0);
        this.startRate = 1.0F;
    }

    /**
     * @return Returns the rate at which the clock should start ticking.
     */
    public float getStartRate()
    {
        return startRate;
    }

    /**
     * @return Returns the media time at which playback should commence. If
     *         playback should start live, this should return
     *         <code>Time(Double.POSITIVE_INFINITY)</code>.
     */
    public Time getStartMediaTime()
    {
        return startMediaTime;
    }

    /**
     * This method checks whether a specified media time would be considered
     * "live" for the data source.
     * 
     * @param mt
     *            - the media time to check
     * @return Returns <code>true</code> if the media time is on or past the
     *         live point; returns
     *         <code>false<code> if it is earlier than the live point.
     */
    final public boolean isLiveMediaTime(Time mt)
    {
        if (log.isDebugEnabled())
        {
            log.debug("isLiveMediaTime - mediaTime: " + mt + ", liveMediaTime: " + getLiveMediaTime());
        }
        return mt.getNanoseconds() >= getLiveMediaTime().getNanoseconds();
    }

    /**
     * @return Returns <code>true</code> if playback should start in live mode;
     *         otherwise, <code>false</code>.
     */
    abstract public boolean shouldStartLive();

    /**
     * @return Returns a time value representing the live media time&mdash;that
     *         is, the media time closest to live content.
     */
    abstract public Time getLiveMediaTime();

    public String toString()
    {
        // getService returns service member, it doesn't look up the service
        return getClass().getName() + " - startMediaTime: " + startMediaTime + ", startRate: " + startRate
                + ", service: " + getService();
    }
}
