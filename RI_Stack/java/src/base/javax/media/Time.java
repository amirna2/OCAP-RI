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

package javax.media;

/**
 * <code>Time</code> abstracts time in the Java Media framework.
 * 
 * @see Clock
 * @see TimeBase
 * 
 * @version 1.10, 97/08/28.
 */
public class Time
{

    public static final long ONE_SECOND = 1000000000L;

    static final double NANO_TO_SEC = 1.0E-9;

    /**
     * Time is kept to a granularity of nanoseconds. Converions to and from this
     * value are done to implement construction or query in seconds.
     */
    protected long nanoseconds;

    /**
     * This field keeps track of whether the {@link Time} instance represents a
     * "live" time, as defined by the OCAP DVR specification (i.e., time was
     * constructed using {@link Double#POSITIVE_INFINITY} or
     * {@link Long#MAX_VALUE}
     */
    private boolean isLiveTime;

    /**
     * Construct a time in nanoseconds.
     * 
     * @param nano
     *            Number of nanoseconds for this time.
     */
    public Time(long nano)
    {
        nanoseconds = nano;
        if (nano == Long.MAX_VALUE)
        {
            isLiveTime = true;
        }
    }

    /**
     * Construct a time in seconds.
     * 
     * @param seconds
     *            Time specified in seconds.
     */
    public Time(double seconds)
    {
        if (seconds == Double.POSITIVE_INFINITY)
        {
            isLiveTime = true;
            nanoseconds = Long.MAX_VALUE;
        }
        else
            nanoseconds = secondsToNanoseconds(seconds);
    }

    /**
     * Convert seconds to nanoseconds.
     */
    protected long secondsToNanoseconds(double seconds)
    {
        if (seconds == Double.POSITIVE_INFINITY)
            return Long.MAX_VALUE;
        else
            return (long) (seconds * ONE_SECOND);
    }

    /**
     * Get the time value in nanoseconds.
     * 
     * @return The time in nanoseconds.
     */
    public long getNanoseconds()
    {
        return nanoseconds;
    }

    /**
     * Get the time value in seconds.
     * 
     */
    public double getSeconds()
    {
        if (isLiveTime)
            return Double.POSITIVE_INFINITY;
        else
            return nanoseconds * NANO_TO_SEC;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        double seconds = getSeconds();
        sb.append(Double.toString(getSeconds()));
        if (seconds != Double.POSITIVE_INFINITY)
        {
            sb.append("s (");
            sb.append(nanoseconds);
            sb.append("ns)");
        }
        return sb.toString();
    }
}
