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

package org.cablelabs.impl.ocap.hn.recording;

import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.content.ContentItemExt;
import org.cablelabs.impl.ocap.hn.content.ContentItemImpl;
import org.cablelabs.impl.ocap.hn.content.ContentResourceExt;
import org.cablelabs.impl.ocap.hn.transformation.Transformable;
import org.ocap.hn.recording.RecordingContentItem;

/**
 * This interface defines the HN-facing API to a local RecordingContentItem and also acts 
 * to identify locally-instantiated RecordingContentItems.
 * 
 * As such, an object implementing this interface must be a locally-instantiated 
 * RecordingContentItem and also be a OcapRecordingRequest.
 * 
 * @author craig
 */
public interface LocalRecordingContentItem 
    extends RecordingContentItem, ContentItemExt, Transformable
{
    /**
     * Return the ContentItemImpl associated with the LocalRecordingContentItem
     * 
     * @return The ContentItemImpl associated with the LocalRecordingContentItem
     */
    public ContentItemImpl getContentItemImpl();

    /**
     * Return all the HNStreamProtocolInfo elements associated with the RecordingContentItem.
     * 
     * @return an array of HNStreamProtocolInfo elements associated with the RecordingContentItem
     */
    public HNStreamProtocolInfo[] getProtocolInfo();

    /**
     * This function will return the array of ContentResources contained in the local
     * RecordingContentItem.
     */
    public ContentResourceExt[] getContentResourceList();
    
    /**
     * This function will set the RecordedService media time for the RecordedService
     * associated with the local RecordingContentItem (@see RecordedService#setMediaTime)
     * 
     * @param mediaTime The playback media time to set (in milliseconds)
     */
    public void setPlaybackMediaTime(long mediaTime);
    
    /**
     * Remove the RecordingContentItem from a NetRecordingEntry.
     * Also removes RecordSchedule if present
     *
     * @return True if the removal was successful; else false.
     */
    public boolean removeFromNetRecordingEntry();
}
