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

package javax.media.protocol;

/**
 * Describes the speed at which data flows.
 * 
 * @version 1.6, 97/08/23.
 * 
 */
public class RateRange
{

    // $jdr: Java needs number range objects.
    float minimum;

    float maximum;

    float current;

    boolean exact;

    RateRange()
    {
        super();
    }

    /**
     * Copy constructor.
     * 
     */
    public RateRange(RateRange r)
    {
        minimum = r.minimum;
        maximum = r.maximum;
        current = r.current;
        exact = r.exact;
    }

    /**
     * Constructor using required values.
     * 
     * @param init
     *            The initial value for this rate.
     * @param min
     *            The minimum value that this rate can take.
     * @param max
     *            The maximum value that this rate can take.
     * @param isExact
     *            Set to <CODE>true</CODE> if the source rate does not vary when
     *            using this rate range.
     */
    public RateRange(float init, float min, float max, boolean isExact)
    {
        minimum = min;
        maximum = max;
        current = init;
        exact = isExact;
    }

    /**
     * Set the current rate. Returns the rate that was actually set. This
     * implementation just returns the specified rate, subclasses should return
     * the rate that was actually set.
     * 
     * @param rate
     *            The new rate.
     */
    public float setCurrentRate(float rate)
    {
        current = rate;
        return current;
    }

    /**
     * Get the current rate.
     * 
     * @return The current rate.
     * 
     */
    public float getCurrentRate()
    {
        return current;
    }

    /**
     * Get the minimum rate supported by this range.
     * 
     * @return The minimum rate.
     */
    public float getMinimumRate()
    {
        return minimum;
    }

    /**
     * Get the maximum rate supported by this range.
     * 
     * @return The maximum rate.
     */
    public float getMaximumRate()
    {
        return maximum;
    }

    /**
     * Determine whether or not a particular value is within the range of
     * supported rates.
     * 
     * @param The
     *            rate to test.
     * @return Returns <CODE>true</CODE> if the specified rate is supported.
     */
    /*
     * public boolean inRange(float rate) { return (minimum <= rate) && (rate <=
     * maximum); }
     */

    /**
     * Determine whether or not the source will maintain a constant speed when
     * using this rate. If the rate varies, synchronization is usually
     * impractical.
     * 
     * @return Returns <CODE>true</CODE> if the source will maintain a constant
     *         speed at this rate.
     */
    public boolean isExact()
    {
        return exact;
    }

}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
