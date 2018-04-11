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
import javax.tv.service.selection.ServiceContext;

/**
 * Specifies a recording request in terms of what is being presented on a
 * ServiceContext.
 * The streams that are being presented in the indicated ServiceContext
 * parameter are recorded. If the Service being recorded from is tuned
 * away, recording SHALL be terminated. If the startTime is in
 * the past and the source javax.tv.service.selection.ServiceContext
 * is associated with a time shift buffer, the
 * contents of the time-shift buffer may be immediately stored
 * to the destination beginning at the startTime, if possible, up to the
 * live broadcast point. If the time-shift buffer does not contain the
 * source from the startTime, as much of the source may be
 * recorded as possible. If the startTime is in the past, but a time-shift
 * buffer cannot be associated with the recording, the recording begins
 * from the live broadcast point. From there the contents of the
 * live broadcast are recorded until the remaining duration is satisified.
 * <p>
 * When instances of this class are passed to RecordingManager.record(..),
 * the following additional failure modes shall apply;<ul>
 * <li>IllegalArgumentException SHALL be thrown if serviceContext is not
 * in the presenting state or if it is not
 * presenting a broadcast service or if the startTime is in the future or
 * if the properties parameter to the instance is an instance of an application defined class
 * <li>SecurityException SHALL be thrown if the application does not have
 * permission to access the service context.
 * <li>AccessDeniedException shall be thrown where the calling application
 * is not permitted to perform this operation by RecordingRequest specific
 * security attributes.
 * </ul>
 * <p>
 * When an instance of this recording spec is passed in as a parameter
 * to the RecordingRequest.reschedule(..) method, an IllegalArgumentException
 * is thrown if the service context parameter is different from the service
 * context specified in the current recording spec for the recording request.
 *
 */
public class ServiceContextRecordingSpec extends RecordingSpec
{
    /**
     * Constructor
     *
     * @param serviceContext
     *            The ServiceContext to record from.
     * @param startTime
     *            Start time of the recording. If the start time is in the
     *            future when the RecordingManager.record(..) method is called
     *            with this ServiceContextRecordingSpec as an argument the
     *            record method will throw an IllegalArgumentException.
     * @param duration
     *            Length of time to record in milli-seconds.
     * @param properties
     *            the definition of how the recording is to be done
     * @throws IllegalArgumentException
     *             if duration is negative
     */
    public ServiceContextRecordingSpec(ServiceContext serviceContext, Date startTime, long duration,
            RecordingProperties properties) throws IllegalArgumentException
    {
        super(properties);

        // check for invalid parameters
        if (duration < 0)
        {
            throw new IllegalArgumentException("Illegal negative duration: " + duration);
        }

        m_startTime = (Date) startTime.clone();
        m_duration = duration;
        m_serviceContext = serviceContext;

    }

    /**
     * Returns the ServiceContext to record from
     *
     * @return the ServiceContext instance passed into the constructor
     */
    public ServiceContext getServiceContext()
    {
        return m_serviceContext;
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
        sb.append("ServiceContextRecordingSpec 0x")
          .append(Integer.toHexString(this.hashCode()))
          .append(":[servicecontext ")
          .append((m_serviceContext == null) ? "null" : m_serviceContext.toString())
          .append(",starttime ")
          .append((m_startTime == null) ? "null" : m_startTime.toString())
          .append(",duration ")
          .append(m_duration)
          .append(']');

        return sb.toString();
    }

    private Date m_startTime;

    private long m_duration;

    private ServiceContext m_serviceContext;
}
