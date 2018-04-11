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

import javax.media.Time;

/**
 * Represents a transmitted time line. Transmitted time lines start at one media
 * time within a piece of content and finish at a later media time in that
 * content. Transmitted time lines are valid at all media times between these
 * points. They are either increment linearly or are paused. The value of a
 * transmitted time line does not have any discontinuities.
 */

public interface TimeLine
{
    /**
     * Returns the first media time at which this time line is valid. For a
     * scheduled recording, this is the first point within the piece of content
     * where the time line is valid. For a timeshift recording, if the time line
     * starts within the time shift buffer then the media time where it starts
     * will be returned. If the time line starts before the start of the time
     * shift buffer, the media time of the start of the time shift buffer will
     * be returned. Note that if the time shift buffer is full and time shift
     * recording is in progress, the start of the buffer will be moving as newly
     * written data over-writes the former start of the buffer.
     *
     * @return a media time
     * @throws TimeLineInvalidException
     *             if the time line is no longer valid in this piece of content.
     *             e.g. the piece of content is a time shift recording and the
     *             end of the time line is no longer within the buffer
     */
    Time getFirstMediaTime() throws TimeLineInvalidException;

    /**
     * Returns the last media time at which this time line is valid. For a scheduled
     * recording, this is the last point within the piece of content where the
     * time line is valid. For a timeshift recording, if the time line ends
     * within the time shift buffer then the media time where it ends will be
     * returned. If the time line ends after the end of the time shift buffer,
     * the media time of the end of the time shift buffer will be returned. Note
     * that if the time shift buffer is full and time shift recording is in
     * progress, the end of the buffer will be moving as newly written data
     * over-writes the former start of the buffer.
     *
     * @return a media time
     * @throws TimeLineInvalidException
     *             if the time line is no longer valid in this piece of content.
     *             e.g. the piece of content is a time shift recording and the
     *             end of the time line is no longer within the buffer
     */
    Time getLastMediaTime() throws TimeLineInvalidException;

    /**
     * Returns the first valid time in this time line. For a scheduled
     * recording, this is the first point within the piece of content where the
     * time line is valid. For a timeshift recording, if the time line starts
     * within the time shift buffer then the time where it starts will be
     * returned. If the time line starts before the start of the time shift
     * buffer, the time of the start of the time shift buffer will be returned.
     * Note that if the time shift buffer is full and time shift recording is in
     * progress, the start of the buffer will be moving as newly written data
     * over-writes the former start of the buffer.
     *
     * @return a time in this time line
     * @throws TimeLineInvalidException
     *             if the time line is no longer valid in this piece of content.
     *             e.g. the piece of content is a time shift recording and the
     *             end of the time line is no longer within the buffer
     */
    long getFirstTime() throws TimeLineInvalidException;

    /**
     * Returns the last valid time in this time line. For a scheduled recording,
     * this is the last point within the piece of content where the time line is
     * valid. For a timeshift recording, if the time line ends within the time
     * shift buffer then the media time where it end will be returned. If the
     * media time ends after the end of the time shift buffer, the media time of
     * the end of the time shift buffer will be returned. Note that if the time
     * shift buffer is full and time shift recording is in progress, the end of
     * the buffer will be moving as newly written data over-writes the former
     * start of the buffer.
     *
     * @return a time in this time line
     * @throws TimeLineInvalidException
     *             if the time line is no longer valid in this piece of content.
     *             e.g. the piece of content is a time shift recording and the
     *             end of the time line is no longer within the buffer
     */
    long getLastTime() throws TimeLineInvalidException;

    /**
     * Translates a time in this time line into the corresponding media time. If
     * the time is one where the time line pauses, the returned media time shall
     * be the highest media time corresponding to the time specified.
     *
     * @param time
     *            a time in this time line
     * @return the corresponding media time
     * @throws TimeLineInvalidException
     *             if the time line is no longer valid in this piece of content.
     *             e.g. the piece of content is a time shift recording and the
     *             end of the time line is no longer within the buffer
     * @throws TimeOutOfRangeException
     *             if the time specified is not within this timeline
     */
    Time getMediaTime(long time) throws TimeLineInvalidException, TimeOutOfRangeException;

    /**
     * Translates a media time into the corresponding time in this timeline
     *
     * @param mediatime
     *            a media time
     * @return the corresponding time in this timeline
     * @throws TimeLineInvalidException
     *             if the time line is no longer valid in this piece of content.
     *             e.g. the piece of content is a time shift recording and the
     *             end of the time line is no longer within the buffer
     * @throws TimeOutOfRangeException
     *             if the media time specified is not within this timeline
     */
    long getTime(Time mediatime) throws TimeLineInvalidException, TimeOutOfRangeException;
}
