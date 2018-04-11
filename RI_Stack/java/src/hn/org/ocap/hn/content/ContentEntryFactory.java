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

import org.cablelabs.impl.ocap.hn.content.ContentEntryFactoryImpl;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.net.OcapLocator;

/**
 * This factory can be used to create <code>ContentEntry</code> instances.
 * There are specialty methods for application convenience when creating
 * channel content items.
 */
public abstract class ContentEntryFactory
{
    private static final ContentEntryFactory INSTANCE = new ContentEntryFactoryImpl();

    /**
     * Singleton behavior.
     */
    protected ContentEntryFactory()
    {
    }

    /**
     * Gets an instance of the factory.
     *
     * @return A content entry factory instance.
     */
    public static ContentEntryFactory getInstance()
    {
        return INSTANCE;
    }

    /**
     * Creates a new <code>ChannelContentItem</code> representing a broadcast
     * channel.  A <code>ChannelContentItem</code> can only be added to a
     * container created by <code>createChannelGroupContainer</code>.
     * </p>
     * This content item is not automatically added to a parent container.
     * Applications can publish multiple channels in a single method call by
     * creating an array of <code>ChannelContentItem</code> instances and
     * passing it to the <code>addContentEntries</code> of a container
     * created by <code>createChannelGroupContainer</code> in CDS.
     * </p>
     * At the point that the created ChannelContentItem is requested by a DMC, 
     * the implementation SHALL determine if the channelLocator is 
     * transport-dependent (e.g. a frequency-based Locator) and use the Locator 
     * to acquire the Service for streaming. If the channelLocator is a 
     * transport-independent (e.g. a sourceID-based) Locator and can be resolved 
     * via JavaTV, the implementation SHALL use JavaTV (e.g. SIManager.getService) 
     * to acquire the Service for streaming. If the transport-independent Locator 
     * cannot be resolved via JavaTV, and the Locator returned from 
     * getTuningLocator() is null, the implementation SHALL invoke the 
     * ServiceResolutionHandler.resolveChannelItem method.  If resolveChannelItem 
     * returns true, the 
     * implementation SHALL use this Locator returned from getTuningLocator() 
     * to acquire the Service for streaming.
     * </p>
     *
     * @param channelType The type of broadcast channel, can be one of
     *      <code>VIDEO_ITEM_BROADCAST</code> or <code>VIDEO_ITEM_BROADCAST_VOD</code>
     *      <code>AUDIO_ITEM_BROADCAST</code>
     *      defined in { @link ContentItem }.
     * @param channelTitle The title of the new ChannelContentItem.
     * @param channelName The name of the new ChannelContentItem.
     * @param channelNumber String representing type and channel number, where
     *      type must be "Analog" or "Digital", number is (major channel number)
     *      for analog channels, and number is (major channel and minor channel)
     *      for digital channels, in the format "type, number [, number]". For
     *      example: "Analog,12", or "Digital,15,2". May also be null, when
     *      application is not providing this information.
     * @param channelLocator An <code>OcapLocator</code> for the channel
     *      
     * @param permissions Access permissions of the new
     *      <code>ChannelContentItem</code> for local server applications only.
     * 
     * @return true if a new ChannelContentItem has been created, otherwise
     *      return false.
     *
     * @throws IllegalArgumentException if channelType is not
     *      AUDIO_ITEM_BROADCAST or VIDEO_ITEM_BROADCAST or VIDEO_ITEM_BROADCAST_VOD,
     *      or if channelNumber format is invalid.     
     *
     * @throws NullPointerException if channelName, channelTitle,
     *      or channelLocator arguments passed to this method are null.
     *
     * @throws SecurityException if the caller does not have
     *      HomeNetPermission("contentmanagement").
     */
    public abstract ChannelContentItem createChannelContentItem(
            String channelType,
            String channelTitle,
            String channelName,
            String channelNumber,
            OcapLocator channelLocator,
            ExtendedFileAccessPermissions permissions);
}
