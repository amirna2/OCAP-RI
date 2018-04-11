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

/**
 * This exception is returned when applications call the getFailedException()
 * for a failed recording request or an incomplete recording request.
 */

public class RecordingFailedException extends java.lang.Exception
{
    /**
     * Constructs a RecordingFailedException with no detail message The reason
     * code instances created by this constructor shall be REASON_NOT_KNOWN.
     */
    public RecordingFailedException()
    {
        this(new RecordingFailedException(CA_REFUSAL).REASON_NOT_KNOWN);
    }

    /**
     * Reason code : Recording failed due to the CA system refusing to permit
     * it.
     */
    public static final int CA_REFUSAL = 1;

    /**
     * Reason code : Recording failed because the requested content could not be
     * found in the network.
     */
    public static final int CONTENT_NOT_FOUND = 2;

    /**
     * Reason code : Recording failed due to problems with tuning.
     */
    public static final int TUNING_FAILURE = 3;

    /**
     * Reason code : Recording failed due to a lack of resources required to
     * present this service.
     */
    public static final int INSUFFICIENT_RESOURCES = 4;

    /**
     * Reason code : Recording did not complete successfully because access to
     * the service or some component of it were withdrawn by the system before
     * the scheduled completion of the recording.
     */
    public static final int ACCESS_WITHDRAWN = 5;

    /**
     * Reason code : Recording did not complete sucessfully because Resources
     * needed to present the service were removed before the scheduled
     * completion of the recording.
     */
    public static final int RESOURCES_REMOVED = 6;

    /**
     * Reason code : Recording did not complete sucessfully because the service
     * vanished from the network before the completion of the recording.
     */
    public static final int SERVICE_VANISHED = 7;

    /**
     * Reason code : Recording did not complete successfully because the
     * application selected another service on the service context. This is
     * applicable only if the recording spec for the recording request is an
     * instance of ServiceContextRecordingSpec.
     */
    public static final int TUNED_AWAY = 8;

    /**
     * Reason code : The application terminated the recording using
     * LeafRecordingRequest.stop method or by calling the stop on the service
     * context (if the recording spec is an instance of
     * ServiceContextRecordingSpec).
     */
    public static final int USER_STOP = 9;

    /**
     * Reason code : Recording could not complete due to lack of storage space.
     */
    public static final int SPACE_FULL = 10;

    /**
     * Reason code : Recording failed due to lack of IO bandwidth to record this
     * program.
     */
    public static final int OUT_OF_BANDWIDTH = 11;

    /**
     * Reason code : The recording request failed due to errors in request
     * resolution.
     */
    public static final int RESOLUTION_ERROR = 12;

    /**
     * Reason code : When the device is powered off and the power returns after
     * the scheduled end time of a recording but before the expiration time of
     * the recording request, the request shall be in the failed state with this
     * reason code.
     */

    public static final int POWER_INTERRUPTION = 13;

    /**
     * Reason code: reason not known
     */
    public int REASON_NOT_KNOWN = 14;

    /**
     * Constructs a RecordingFailedException with a detail message
     *
     * @param reason
     *            the reason why the exception was thrown
     */
    public RecordingFailedException(int reason)
    {
        m_reason = reason;
    }

    /**
     * Reports the reason for which the recording request failed to complete
     * successfully.
     *
     * @return the reason code for which the recording request failed to
     *         complete successfully.
     */
    public int getReason()
    {
        return m_reason;
    }

    private int m_reason;
}
