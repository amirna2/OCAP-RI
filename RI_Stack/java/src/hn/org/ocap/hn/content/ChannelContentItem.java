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

package org.ocap.hn.content;

import java.lang.UnsupportedOperationException;
import javax.tv.locator.InvalidLocatorException;
import org.ocap.net.OcapLocator;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * This interface represents a video or audio broadcast channel object.
 */
public interface ChannelContentItem extends ContentItem
{

    /**
     * Gets The channel type for this ChannelContentItem
     *
     * @return the String channel type for this item, or null if unknown.
     */
    public String getChannelType();

    /**
     * Gets The channel number for this ChannelContentItem
     *
     * @return The String channel number for this item, or null if unknown.
     */
    public String getChannelNumber();

    /**
     * Gets The channel name for this ChannelContentItem
     *
     * @return the String channel name for this item, or null if unknown.
     */
    public String getChannelName();

    /**
     * Gets The title for this ChannelContentItem, or null if the title is unknown.
     *
     * @return the String title for this item, or null if unknown.
     */
    public String getChannelTitle();

    /**
     * Gets the locator for this ChannelContentItem set in createChannelContentItem.
     *
     * @return The locator for this ChannelContentItem, returns
     *      null if the isLocal method returns false.
     */
    public OcapLocator getChannelLocator();

    /**
     * Gets the extended file access permissions for this ChannelContentItem.
     *
     * @return The extended file access permissions.
     */
    public ExtendedFileAccessPermissions getExtendedFileAccessPermissions();

    /**
     * Gets the frequency based tuning locator used for service resolution.
     *
     * @return The frequency based tuning locator if previously resolved,
     * null otherwise.
     */
    public OcapLocator getTuningLocator();

    /**
     * <p>Sets the tuning locator for this ChannelContentItem that the
     * implementation can use for tuning a broadcast channel.  Returns false
     * if the #isLocal method returns false.
     * </p>
     * <p>
     * An application may call this method to update a channel's tuning parameters (for example,
     * when an SDV channel's program number or frequency changes). Upon a successful update of
     * the channel's tuning parameters the implementation SHALL be responsible for updating
     * any active streaming sessions to the new tuning parameters. When a JavaTV Service represents
     * this ChannelContentItem the implementation SHALL modify the transport dependent locator
     * of the Service to match the locator parameter.
     * </p>
     * <p>
     * Setting the channel tuning locator to a null locator makes the channel
     * item no longer tunable (for example, when the application ends an SDV
     * session).
     * Setting the tuning locator to null while streaming ends
     * streaming, removes the locator,
     * and {@link StreamingActivityListener#notifyStreamingEnded}
     * is called with an activityID of <code>ACTIVITY_END_SERVICE_VANISHED
     * </code>.
     * </p>
     *
     * @param locator A frequency-based locator for the channel, or null to
     *      remove the current locator.
     *
     * @return true when the locator parameter is set correctly, otherwise,
     *      returns false.
     *
     * @throws InvalidLocatorException if the locator parameter is not
     *      frequency based and is not null.
     *
     * @throws SecurityException if the calling application is not granted
     *      write permission by the permissions returned from the
     *      getExtendedFileAccessPermissions method.
     */
    public boolean setTuningLocator(OcapLocator locator) throws InvalidLocatorException;
}
