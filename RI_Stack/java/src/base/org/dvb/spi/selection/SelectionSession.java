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

package org.dvb.spi.selection;

import org.davic.net.Locator;

/**
 * A session for presentation of one or more services over a period of time by a
 * SelectionProvider. The first operation on a new session will always be
 * selection of a service in a service context, and all subsequent operations
 * will pertain to the same service context.
 * 
 * @since MHP 1.1.3
 **/
public interface SelectionSession
{

    /**
     * Sets up delivery of a stream representing the service, and delivers a
     * locator for reception of that stream. For example, either a unicast
     * locator or a frequency locator might be returned, under the apprlpriate
     * circumstances.
     * 
     * @return a locator for where the stream can be found
     **/
    public Locator select();

    /**
     * Called by the platform when the service bound to this session is no
     * longer being used by the implementation. The selection provider should do
     * any needed clean-up here, e.g. informing a server that the service is not
     * needed any more. Once destroy is called, the terminal implementation
     * discards the SelectionSession.
     **/
    public void destroy();

    /**
     * Called when the implementation is ready to receive content on the locator
     * returned by the select method. When using unicast protocols where the
     * content must only be sent when the MHP terminal is ready, it is now safe
     * to request the content be sent.
     * <p>
     * A SelectionSession might be destroyed after a select, but before this
     * method is called. In this case, this method may not be called.
     * Applications should therefore not expect this method to be called after
     * destroy() is called on this session.
     * 
     * @see #destroy()
     **/
    public void selectionReady();

    /**
     * Sets the speed of playback. If this session does not support trick modes,
     * this method returns 1 and has no effect. Calling this method with the
     * value 1.0f shall always succeed, and shall result in a return value of
     * 1.0f.
     * 
     * @param newRate
     *            New playback rate. Implementations shall make a best effort to
     *            approximate this rate.
     * 
     * @return The new rate, if known. If not known, the value
     *         java.lang.Float.NEGATIVE_INFINITY is returned.
     **/
    public float setRate(float newRate);

    /**
     * Set the position within the media. If this session does not support trick
     * modes, this method returns -1 and has no effect.
     * 
     * @param position
     *            The position within the program, in milliseconds
     * 
     * @return The new position, or -1 if position setting isn't supported.
     **/
    public long setPosition(long position);
}
