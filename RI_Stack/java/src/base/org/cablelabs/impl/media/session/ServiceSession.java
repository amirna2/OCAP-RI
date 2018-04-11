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

import javax.media.GainControl;
import javax.media.Time;

import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.service.ServiceDetailsExt;

/**
 * Objects implementing the Session interface are used to represent decoding
 * sessions in the native layer.
 * 
 */
public interface ServiceSession extends Session
{
    /**
     * Present the requested streams
     * 
     * This method can throw an unchecked exception
     * 
     * @param details
     *            The service details of the service whose components are to be
     *            selected.
     * @param elementaryStreams
     *            the elementary streams to present (may be null if elementary
     *            streams are not needed to present)
     * @throws MPEException
     *             if a native error occurs.
     * @throws NoSourceException
     *             if the source being decoded becomes unusable by the time the
     *             decode session is to be started.
     */
    void present(ServiceDetailsExt details, ElementaryStreamExt[] elementaryStreams) throws MPEException,
            NoSourceException;

    /**
     * Update the set of presenting streams. Media time and rate aren't
     * affected.
     * 
     * This method can throw an unchecked exception
     * 
     * @param currentMediaTime
     *            current mediatime
     * @param elementaryStreams
     *            the elementary streams to present (may be null if elementary
     *            streams are not needed to present)
     * @throws MPEException
     *             if a native error occurs.
     * @throws NoSourceException
     *             if the source being decoded becomes unusable by the time the
     *             decode session is to be started.
     * @throws MPEException
     *             if a native error occurs.
     */
    void updatePresentation(Time currentMediaTime, ElementaryStreamExt[] elementaryStreams) throws MPEException,
            NoSourceException;

    /**
     * Accessor
     *
     * @return true if the decode has been successfully initiated
     */
    boolean isDecodeInitiated();

    /**
     * Mute or unmute the audio of a decode session
     *
     * @param mute new mute state
     */
    void setMute(boolean mute);

    /**
     * Update the audio gain value as described by {@link GainControl#setDB}
     *
     * @param gain new audio gain value in decibels
     */
    float setGain(float gain);

    /**
     * Block presentation (mute audio, display black video)
     *
     * @param blockPresentation
     */
    void blockPresentation(boolean blockPresentation);
}
