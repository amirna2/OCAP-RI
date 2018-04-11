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

package org.dvb.media;

import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.MediaLocator;

/**
 * This event is generated whenever the content being presented by a player
 * changes for reasons outside the control of the application. The state of the
 * player does not change - only the content being presented.
 */

public class PresentationChangedEvent extends ControllerEvent
{
    /**
     * Constructor for the event
     * 
     * @param source
     *            the controller whose presentation changed
     * @param stream
     *            the stream now being presented.
     * @param reason
     *            the reason for the change encoded as one of the constants in
     *            this class
     */

    public PresentationChangedEvent(Controller source, javax.media.MediaLocator stream, int reason)
    {
        super(source);
        if (reason != STREAM_UNAVAILABLE && reason != CA_FAILURE && reason != CA_RETURNED)
            throw new IllegalArgumentException("invalid reason: " + reason);
        this.mediaLocator = stream;
        this.reason = reason;
    }

    /**
     * The stream being presented is no longer available in the transport
     * stream.
     * 
     * @see PresentationChangedEvent#getReason
     */

    public static final int STREAM_UNAVAILABLE = 0x00;

    /**
     * Presentation changed due an action by the CA subsystem. Alternate content
     * is being played, not the content selected by the user (e.g. adverts in
     * place of a scrambled service)
     * 
     * @see PresentationChangedEvent#getReason
     */

    public static final int CA_FAILURE = 0x01;

    /**
     * Presentation changed due to an action by the CA subsystem. Normal content
     * is now being presented as requested by the user. This reason code is used
     * when the CA subsystem commands the MHP terminal to switch back to the
     * normal presentation after having previously selected an alternate
     * content.
     * 
     * @see PresentationChangedEvent#getReason
     */
    public static final int CA_RETURNED = 0x02;

    private MediaLocator mediaLocator;

    /**
     * This method returns the locator for the stream now being presented.
     * 
     * @return the locator for the stream now being presented
     */
    public javax.media.MediaLocator getStream()
    {
        return mediaLocator;
    }

    private int reason;

    /**
     * This method returns the reason why access has been withdrawn.
     * 
     * @return the reason for the change specified when the event was
     *         constructed
     */

    public int getReason()
    {
        return reason;
    }
}
