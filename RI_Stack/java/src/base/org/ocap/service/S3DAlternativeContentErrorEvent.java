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
package org.ocap.service;

import javax.tv.service.selection.ServiceContext;

/**
 * <code>S3DAlternativeContentErrorEvent</code> is generated to indicate that
 * "alternative" content may be presenting due to a detected incompatibility
 * between the selected 3D content and the display device.
 * The configuration of the Host device determines the nature of the
 * alternative content presented; e.g., no video or the 3D video as
 * requested.  If the Host device attempts to present the requested 3D video,
 * it is possible that it is being incorrectly displayed on the display device.
 * </p>
 * A device detects 3D content metadata (i.e., frame packing) based on signaling
 * as defined in [OCCEP].  The implementation SHALL compare the signaled content
 * format to HDMI display device capabilities reported in E-EDID and generate this
 * event as warranted.  This event will be generated due to the following
 * situations:
 *
 * <ul>
 * <li> 3D video content selected but 3D video not supported by the HDMI display device.
 * <li> 3D video content selected but 3D format not supported by the HDMI display device.
 * <li> 3D video content selected but no display device connected to an HDMI port.
 * </ul>
 *
 * Such events are not considered selection or presentation failures.
 * </p>
 * Note (informative):
 * <ul>
 * <li> If 3D video is selected for presentation but the Host device detects
 * that no display devices are connected to an HDMI port,
 * it will black out the video and mute the audio on any other connected outputs.
 * See [HOST 2.1]
 * <li> If the Host device detects a possible incompatibility between the 3D content
 * format and the display device, it may black out the video or send the requested
 * 3D video to the display device, depending on the Host device configuration
 * per [HOST 2.1] and [MIB-HOST].
 * </ul>
 *
 */
public class S3DAlternativeContentErrorEvent extends AlternativeContentErrorEvent
{
    /**
     * Reason code: 3D video content has been selected but 3D video is not
     * supported by the HDMI display device.
     */
    // Value should be AlternativeContentErrorEvent.TUNING_FAILURE + 1.
    public static final int S3D_NOT_SUPPORTED = 104 + 1;

    /**
     * Reason code: 3D video content has been selected but its 3D format is not
     * supported by the HDMI display device.
     */
    public static final int S3D_FORMAT_NOT_SUPPORTED = S3D_NOT_SUPPORTED + 1;

    /**
     * Reason code : 3D video content has been selected but no display is
     * connected to an HDMI port.
     */
    public static final int S3D_NO_HDMI_CONNECTION = S3D_FORMAT_NOT_SUPPORTED + 1;


    /**
     * Constructs an event with a reason code.
     *
     * @param source The <code>ServiceContext</code> that generated the event.
     *
     * @param reason The reason why alternative content is potentially being
     *      presented.
     */
    public S3DAlternativeContentErrorEvent(ServiceContext source, int reason)
    {
        super(source, reason);
    }

}
