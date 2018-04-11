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

package org.dvb.dsmcc;

/**
 * Sent when an MHP terminal detects a permanent discontinuity in NPT as defined
 * in the main body of the present document. This represents an error condition in
 * the incoming broadcast.
 * <p>
 * This event shall be sent following a PCR discontinuity when the MHP terminal
 * has enough information to determine that there will be an NPT discontinuity.
 * If the <code>NPTDiscontinuityEvent</code> is because of invalid data in a new
 * NPTReferenceDescriptor then the event will be generated when that new
 * NPTReferenceDescriptor is detected by the MHP terminal. If the
 * <code>NPTDiscontinuityEvent</code> is because no new NPTReferenceDescriptor
 * is detected within the time allowed by the main body of the present document
 * then it will be generated when that time interval has elapsed.
 *
 * @since MHP 1.0.1
 */

public class NPTDiscontinuityEvent extends NPTStatusEvent
{

    private long NptBefore = -1;

    private long NptAfter = -1;

    /**
     * Construct an event. The <code>before</code> and <code>after</code> values
     * used shall be the values at the time when the receiver determined that a
     * NPT discontinuity has happened. If the <code>NPTDiscontinuityEvent</code>
     * is because of invalid data in a new NPTReferenceDescriptor then this is
     * the time when that new descriptor was known to be invalid. If
     * <code>NPTDiscontinuityEvent</code> is because of the absence of a new
     * NPTReferenceDescriptor then this will be when the MHP terminal detects
     * that the time interval allowed by the present document for such new
     * descriptors has elapsed. Where an NPT value is unknown or cannot be
     * computed, -1 shall be used.
     *
     * @param source
     *            the stream whose NPT suffered a discontinuity
     * @param before
     *            the last NPT value detected before the discontinuity
     * @param after
     *            the first NPT value detected after the discontinuity
     */
    public NPTDiscontinuityEvent(DSMCCStream source, long before, long after)
    {
        super(source);
        NptBefore = before;
        NptAfter = after;
    }

    /**
     * Return the last known stable value of NPT before the discontinuity
     *
     * @return an NPT value
     */
    public long getLastNPT()
    {
        return NptBefore;
    }

    /**
     * Return the first known stable value of NPT after the discontinuity
     *
     * @return an NPT value
     */
    public long getFirstNPT()
    {
        return NptAfter;
    }

}
