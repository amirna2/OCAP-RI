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

package org.ocap.shared.media;

import javax.media.Control;
import javax.media.Time;

/**
 * This interface represents a trick-mode control that can be used for retrieving more
 * information corresponding to the playback of the time-shift buffer. This control will
 * only be available if the service being presented on the service context is a broadcast
 * service and if there is a time-shift buffer associated with the service context.
 */
public interface TimeShiftControl extends Control
{
    /**
     * Get the media time corresponding to the current beginning of the
     * time-shift buffer. This could be the media time corresponding to start of
     * the buffer, before the buffer wrap around or the media time corresponding
     * to the beginning of the valid buffer area after the wrap around.
     *
     * @return media time corresponding to the beginning of the time-shift
     *         buffer.
     */
    public abstract Time getBeginningOfBuffer();

    /**
     * Get the media time corresponding to the end of the time-shift buffer.
     * This could be the current system time if the time-shift recording is
     * still ongoing or the media time corresponding to the end point for the
     * valid area of the time-shift buffer.
     *
     * @return media time corresponding to the end of the time-shift buffer.
     */
    public abstract Time getEndOfBuffer();

    /**
     * Get the duration of content currently in the time-shift buffer. The value
     * returned is the content's duration when played at a rate of 1.0.
     *
     * @return A Time object representing the duration.
     */
    public abstract Time getDuration();

    /**
     * Get the estimated value for the maximum duration of content that could be
     * buffered using this time-shift buffer. The value returned is the
     * content's duration when played at a rate of 1.0.
     *
     * @return A Time object representing the maximum value for duration.
     */
    public abstract Time getMaxDuration();
}
