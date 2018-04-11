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

import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.content.ContentItem;
import java.util.Date;
import javax.media.Time;

/**
 * This ContentItem represents a recording that has been scheduled on the home
 * network.
 *
 * This interface represents a DVR recording which can be published to the home
 * network. On devices which support both the OCAP Home Networking API and the
 * OCAP DVR API, objects implementing
 * <code> org.ocap.dvr.OcapRecordingRequest </code> will also implement this
 * interface. When a RecordingRequest is deleted, implementations SHALL call the
 * RecordingContentItem.deleteEntry method in the same object.
 */
public interface RecordingContentItem extends ContentItem
{
    /**
     * Key constant for retrieving the state of this recording item from this
     * item's metadata. Values returned for this key will be represented as an
     * Integer.
     */
    public static final String PROP_RECORDING_STATE = "ocap:taskState";

    /**
     * Key constant for retrieving the start time of this recording item from
     * this item's metadata. Values returned for this key will be represented as
     * a java.util.Date.
     */
    public static final String PROP_START_TIME = "ocap:scheduledStartDateTime";

    /**
     * Key constant for retrieving the duration in milliseconds of this
     * recording item from this item's metadata. Values returned for this key
     * will be represented as an Integer.
     */
    public static final String PROP_DURATION = "ocap:scheduledDuration";

    /**
     * Key constant for retrieving the source ID of this recording item from
     * this item's metadata. Values returned for this key will be represented as
     * a String.
     */
    public static final String PROP_SOURCE_ID = "ocap:scheduledChannelID";

    /**
     * Key constant for retrieving the source ID type of this recording item
     * from this item's metadata. Values returned for this key will be represented
     * as a String.
     */
    public static final String PROP_SOURCE_ID_TYPE = "ocap:scheduledChannelIDType";

    /**
     * Key constant for retrieving the destination of this recording item from
     * this item's metadata. Values returned for this key will be represented as
     * a String.
     */
    public static final String PROP_DESTINATION = "ocap:destination";

    /**
     * Key constant for retrieving the priority flag of this recording item from
     * this item's metadata. Values returned for this key will be represented as
     * an Integer.
     */
    public static final String PROP_PRIORITY_FLAG = "ocap:priorityFlag";

    /**
     * Key constant for retrieving the retention priority of this recording item
     * from this item's metadata. Values returned for this key will be
     * represented as an Integer.
     */
    public static final String PROP_RETENTION_PRIORITY = "ocap:retentionPriority";

    /**
     * Key constant for retrieving the file access permissions of this recording
     * item from this item's metadata. Values returned for this key will be
     * represented as an org.ocap.storage.ExtendedFileAccessPermissions.
     */
    public static final String PROP_ACCESS_PERMISSIONS = "ocap:accessPermissions";

    /**
     * Key constant for retrieving the organization of this recording item from
     * this item's metadata. Values returned for this key will be represented as
     * a String.
     */
    public static final String PROP_ORGANIZATION = "ocap:organization";

    /**
     * Key constant for retrieving the application ID of this recording item
     * from this item's metadata. Values returned for this key will be
     * represented as an org.dvb.application.AppID.
     */
    public static final String PROP_APP_ID = "ocap:appID";

    /**
     * Key constant for retrieving the estimated space required for this
     * recording item from this item's metadata. Values returned for this key
     * will be represented as a Long.
     */
    public static final String PROP_SPACE_REQUIRED = "ocap:spaceRequired";

    /**
     * Key constant for retrieving the location of content associated with this
     * recording item from this item's metadata. Values returned for this key
     * will be represented as a String.
     */
    public static final String PROP_CONTENT_URI = "ocap:contentURI";

    /**
     * Key constant for retrieving the media first time for this recording item
     * from this item's metadata. Values returned for this key will be represented
     * as a Long.
     */
    public static final String PROP_MEDIA_FIRST_TIME = "ocap:mediaFirstTime";

    /**
     * Key constant for retrieving the presentation point for this recording
     * item from this item's metadata. Values returned for this key will be
     * represented as a Long.
     */
    public static final String PROP_PRESENTATION_POINT = "ocap:mediaPresentationPoint";

    /**
     * Key constant for retrieving the expiration period for this recording item
     * from this item's metadata. Values returned for this key will be
     * represented as an Long.
     */
    public static final String PROP_EXPIRATION_PERIOD = "ocap:expirationPeriod";

    /**
     * Key constant for retrieving the MSO content indicator for this recording
     * item from this item's metadata. Values returned for this key will be
     * represented as an Boolean.
     */
    public static final String PROP_MSO_CONTENT = "ocap:msoContentIndicator";

    /**
     * Key constant for retrieving the ID of any NetRecordingEntry containing
     * this RecordingContentItem. Values returned for this key will be
     * represented as a String.
     */
    public static final String PROP_NET_RECORDING_ENTRY = "ocap:netRecordingEntry";

    /**
     * Requests that the presentation point of this recording be updated.
     *
     * @param time
     *            The presentation point of this recording.
     *
     * @param handler
     *            The NetActionHandler which gets informed once this request
     *            completes.
     *
     * @return NetActionRequest which can be used to monitor asynchronous action
     *         progress
     */
    NetActionRequest requestSetMediaTime(Time time, NetActionHandler handler);

    /**
     * Requests a list of recordings whose usage of resources conflict
     * with this recording content item. The resulting list of recordings SHALL
     * be returned as an array of RecordingContentItem objects from the
     * NetActionEvent.getResponse() method of the resulting NetActionEvent.
     *
     * @param handler
     *            The NetActionHandler implementation to receive the
     *            asynchronous response to this request
     *
     * @return NetActionRequest which can be used to monitor asynchronous action
     *         progress
     */
    NetActionRequest requestConflictingRecordings(NetActionHandler handler);

    /**
     * Returns the NetRecordingEntry which contains this recording content item
     * if the NetRecordingEntry is available.
     *
     * @return null if this RecordingContentItem is not added to any
     *         NetRecordingEntry or if the NetRecordingEntry containing this
     *         RecordingContentItem is not available. Otherwise the
     *         NetRecordingEntry containing this RecordingContentItem
     */
    NetRecordingEntry getRecordingEntry();

    /**
     * Returns the ObjectID of the NetRecordingEntry which contains this
     * recording content item. The ObjectID can be obtained from
     * ocap:netRecordingEntry property of this recording content item.
     *
     * @return null if this RecordingContentItem does not contain
     *         ocap:netRecordingEntry property. Otherwise, the value contained
     *         in ocap:netRecordingEntry property of this RecordingContentItem.
     */
    String getRecordingEntryID();
}
