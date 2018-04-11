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

import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.media.player.Util;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.service.ServiceDetailsExt;

public abstract class AbstractServiceSession extends AbstractSession implements ServiceSession
{
    /*
     * Modifiable Fields - fields that are set at construction, but that may be
     * modified
     */

    /**
     * The {@link ServiceDetailsExt} of the service whose components are to be
     * decoded.
     */
    protected ServiceDetailsExt details;
    //initial mute value
    protected boolean mute;
    //initial gain value
    protected float gain;

    /**
     * @param sync
     *            an Object to use for synchronization.
     * @param listener
     *            the SessionListener that will receive any events from this
     *            session.
     * @param serviceDetails
     *            {@link ServiceDetailsExt} of the service to be decoded.
     * @param videoDevice
     *            the video device that will be used to display the contents
     * @param mute
     *            the initial mute value
     * @param gain
     *            the initial gain value
     */
    protected AbstractServiceSession(Object sync, SessionListener listener, ServiceDetailsExt serviceDetails,
            VideoDevice videoDevice, boolean mute, float gain)
    {
        super(sync, listener, videoDevice);
        if (Asserting.ASSERTING)
        {
            Assert.condition(serviceDetails != null);
        }

        details = serviceDetails;
        this.mute = mute;
        this.gain = gain;
    }

    public boolean isDecodeInitiated()
    {
        return isStarted();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("lock=");
        sb.append(lock);
        sb.append(", details(id)=");
        sb.append(details.getID());
        sb.append(", video=");
        sb.append(Util.toHexString(videoDevice.getHandle()));
        sb.append(", started=");
        sb.append(isStarted());
        sb.append(", blocked=");
        sb.append(blocked);
        sb.append(", mute=");
        sb.append(mute);
        sb.append(", gain=");
        sb.append(gain);

        return sb.toString();
    }
}
