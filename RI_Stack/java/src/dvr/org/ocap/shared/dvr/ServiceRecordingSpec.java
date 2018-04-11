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

package org.ocap.shared.dvr;

import java.util.Date;
import java.lang.IllegalArgumentException;
import javax.tv.service.Service;

import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;

/**
 * Specifies a recording request in terms of a Service.<p>
 * When instances of this class are passed to RecordingManager.record(..),
 * the following additional failure mode shall apply - if the end time
 * (computed as the start time + the duration) is in the past when the
 * record method is called, the record method shall throw an IllegalArgumentException.<p>
 * When an instance of this recording spec is passed in as a parameter to
 * the RecordingRequest.reschedule(..) method, an IllegalArgumentException shall
 * be thrown if either of the following apply;<ul>
 * <li>if the source is different from the source specified in the current
 * recording spec for the recording request and if the recording request
 * is in progress state.
 * <li>if the properties parameter of the instance is an instance of an application defined class
 * </ul>
 * When instances of this class are passed to RecordingManager.record(..),
 * if the start time is in the past and either<ul>
 * <li>none of the content concerned is already recorded
 * <li>some of the content concerned is already recorded but the implementation
 * does not support including already recorded content in a scheduled recording
 * </ul>
 * then the current time shall be used as the start time and the duration
 * reduced accordingly. The present document does not require implementations
 * to include already recorded content in scheduled recordings however GEM
 * recording specifications may require this.
 */


public class ServiceRecordingSpec extends RecordingSpec
{
    /**
     * Constructor.
     *
     * @param source
     *            the service to be recorded
     * @param startTime
     *            Start time of the recording.
     * @param duration
     *            Length of time to record in milli-seconds.
     * @param properties
     *            The definition of how the recording is to be done.
     * @throws IllegalArgumentException
     *             if the source is not a broadcast service or if the duration
     *             is negative
     */
    public ServiceRecordingSpec(Service source, Date startTime, long duration, RecordingProperties properties)
            throws IllegalArgumentException
    {
        super(properties);
        if (duration < 0)
        {
            throw new IllegalArgumentException("Duration is negative");
        }
        if (source instanceof AbstractService || source instanceof RecordedService)
        {
            throw new IllegalArgumentException("Source is not a broadcast service");
        }
        m_source = source;
        m_startTime = (Date) startTime.clone();
        m_duration = duration;
    }

    /**
     * Returns the source of the recording
     *
     * @return the source passed into the constructor
     */
    public javax.tv.service.Service getSource()
    {
        return m_source;
    }

    /**
     * Returns the start time passed as an argument to the constructor.
     *
     * @return the start time passed into the constructor
     */
    public Date getStartTime()
    {
        return m_startTime;
    }

    /**
     * Returns the duration passed as an argument to the constructor.
     *
     * @return the duration passed into the constructor
     */
    public long getDuration()
    {
        return m_duration;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("ServiceRecordingSpec 0x")
          .append(Integer.toHexString(this.hashCode()))
          .append(":[source ")
          .append((m_source == null) ? "null" : m_source.toString())
          .append(",starttime ")
          .append((m_startTime == null) ? "null" : m_startTime.toString())
          .append(",duration ")
          .append(m_duration)
          .append(']');

        return sb.toString();
    }

    private Service m_source;

    private Date m_startTime;

    private long m_duration;
}
