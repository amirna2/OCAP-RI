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
package org.cablelabs.impl.media.session;

import javax.media.Time;

import org.cablelabs.impl.media.player.VideoDevice;

/**
 * Objects implementing the Session interface are used to represent decoding
 * sessions in the native layer.
 */
public interface Session
{
    /**
     * Indicates an invalid session; returned by {@link #getNativeHandle()}.
     */
    int INVALID = -1;

    /**
     * Assign a new video device to be used by the session.
     * 
     * @param vd
     *            - the new video device
     */
    void setVideoDevice(VideoDevice vd);

    /**
     * Stop the session and release any resources it holds.
     * @param holdFrame display the last frame if true and supported by the session implementation
     */
    void stop(boolean holdFrame);

    /**
     * Changes the media time position.
     * 
     * @param mediaTime
     *            The desired media time
     * 
     * @throws MPEException
     *             if a native error occurs.
     * @return actual media time of the playback, which may be different than
     *         the time requested. If the media time is outside the range of the
     *         media, the returned media time will represent the closest "edge"
     *         of the media.
     *         <p/>
     *         If setting the media time is not supported, as is the case for a
     *         {@link BroadcastSession}, <code>null</code> is returned.
     */
    Time setMediaTime(Time mediaTime) throws MPEException;

    /**
     * Changes the playback rate.
     * 
     * @param rate
     *            the requested rate
     * @throws MPEException
     *             if a native error occurs.
     * @return The value that was effectively set as the rate
     */
    float setRate(float rate) throws MPEException;

    /**
     * @return Returns the media time of the playback point in the session. If
     *         the session does not keep a playback media time (e.g.,
     *         {@link BroadcastSession}), this should return <code>null</code>.
     * 
     * @throws MPEException
     *             if a native error occurs.
     */
    Time getMediaTime() throws MPEException;

    /**
     * @return Returns the current playback rate for the session.
     * 
     * @throws MPEException
     *             if a native error occurs.
     */
    float getRate() throws MPEException;

    /**
     * Freeze the decoder output at the last frame decoded, and don't display
     * any new frames until a subsequent call to {@link #resume()}.
     * 
     * @throws MPEException
     *             if a native error occurs.
     */
    void freeze() throws MPEException;

    /**
     * Resume decoder output at the last frame decoded.
     * 
     * @throws MPEException
     *             if a native error occurs.
     */
    void resume() throws MPEException;

    /**
     * @return Returns an <code>int</code> representing the native decode handle
     *         for this session. If the session is invalid at the time this is
     *         called&mdash;e.g., it hasn't been started or it has been
     *         stopped&mdash;return the value {@link #INVALID}.
     */
    int getNativeHandle();

    /**
     * Indicator of whether the decode session is currently started.
     *
     * @return true if decode session is started
     */
    boolean isStarted();
}
