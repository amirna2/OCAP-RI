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
 * Provides the ability to obtain media times with various special
 * characteristics when applied to the content being played by this JMF player.
 * The behaviour of these media times is implementation dependent if used with
 * any other JMF player.
 * <p>
 * Methods returning instances of Time shall not check whether that Time
 * references a point outside the content being played by this JMF player.
 * Checking for this condition shall happen when the instance of Time is used -
 * in the same way as for instances of Time created using the constructor of
 * that class.
 */

public interface MediaTimeFactoryControl extends javax.media.Control
{
    /**
     * Obtain a media time relative to the current location
     * 
     * @param offset
     *            the offset relative to the current location measured in
     *            nanoseconds
     * @return a media time
     */
    public Time getRelativeTime(long offset);

    /**
     * Enables applications to precisely control the position where playback
     * starts following a call to Player.setMediaTime. This method takes an
     * original media time as input and returns a new media time which
     * encapsulates the original media time and an indication of how that
     * original media time is to be interpreted when used in a call to
     * Player.setMediaTime.
     * <p>
     * 
     * The present document does not define the return values from calling
     * getSeconds and getNanoseconds on the new media time instance. They may be
     * copies of the return values from the original time. They may be new
     * values accurately reflecting where playback would start if the new media
     * time was passed to Player.setMediaTime.
     * 
     * @param original
     *            the original media time
     * @param beforeOrAfter
     *            if true, the media time where playback starts will be on or
     *            before the original one (i.e. content at the original media
     *            time is guaranteed to be presented in playback). If false, the
     *            media time where playback starts will be after the original
     *            one (i.e. neither content at the original media time nor any
     *            content before that original time will be presented in
     *            playback).
     * 
     * @return a new media time
     */
    public Time setTimeApproximations(Time original, boolean beforeOrAfter);
}
