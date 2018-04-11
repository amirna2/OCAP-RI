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

package org.ocap.hn.recording;

import java.io.IOException;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.content.ContentEntry;

/**
 * This ContentEntry represents a series recording that has been scheduled on
 * the home network.
 */
public interface NetRecordingEntry extends ContentEntry
{
    /**
     * Key constant for retrieving the CDS reference of this recording entry
     * from this entry's metadata. Values returned for this key will be represented
     * as a String.
     */
    public static final String PROP_CDS_REFERENCE = "ocap:cdsReference";

    /**
     * Key constant for retrieving the RCI list of this recording entry
     * from this entry's metadata. Values returned for this key will be represented
     * as a String.
     */
    public static final String PROP_RCI_LIST = "ocap:RCIList";

    /**
     * Key constant for retrieving the scheduled CDS entry ID of this recording entry
     * from this entry's metadata. Values returned for this key will be represented
     * as a String.
     */
    public static final String PROP_SCHEDULED_CDS_ENTRY_ID = "ocap:scheduledCDSEntryID";

    /**
     * Retrieves the local individual RecordingContentItems that make up this
     * series recording.
     *
     * @throws IOException
     *             if this isLocal() method of this object does not return true
     *
     * @return the RecordingContentItems in this series
     *
     */
    RecordingContentItem[] getRecordingContentItems() throws IOException;

    /**
     * Adds a local RecordingContentItem to this recording object
     *
     * @param item
     *            The recording content item to add to this series
     *
     * @throws IOException
     *             if this isLocal() method of this object does not return true
     *
     * @throws IllegalStateException
     *             if this recording object is not associated with a UPnP AV
     *             Scheduled Recording Service Object (RerordSchedule)
     *
     * @throws IllegalArgumentException
     *             if the RecordingContentItem paramter has the associated UPnP
     *             AV Scheduled Recording Service Object (RecordTask)
     *
     * @throws SecurityException
     *             if the caller does not have
     *             HomeNetPermission("recordinghandler")
     *
     */
    void addRecordingContentItem(RecordingContentItem item) throws IOException;

    /**
     * Removes a local RecordingContentItem from this recording object. If the
     * RecordingContentItem passed into this method is not contained in this
     * NetRecordingObject, this method has no effect.
     *
     * @param item
     *            The recording content item to remove from this series
     *
     * @throws IOException
     *             if this isLocal() method of this object does not return true
     *
     * @throws IllegalArgumentException
     *             if the RecordingContentItem paramter has the associated UPnP
     *             AV Scheduled Recording Service Object (RecordTask)
     *
     * @throws SecurityException
     *             if the caller does not have
     *             HomeNetPermission("recordinghandler")
     */
    void removeRecordingContentItem(RecordingContentItem item) throws IOException;

    /**
     * Retrieves ObjectIDs of the individual RecordingContentItems that make up
     * this series recording.
     *
     * @return the ObjectIDs of the RecordingContentItems in this series
     *
     */
    String[] getRecordingContentItemIDs();
}
