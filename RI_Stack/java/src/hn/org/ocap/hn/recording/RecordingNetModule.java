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

import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetModule;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.content.ContentEntry;

/**
 * <p>
 * An interface representing a NetModule which provides DVR functionality.
 * </p>
 * <p>
 * NetModules which implement this interface SHALL have a
 * NetModule.PROP_NETMODULE_TYPE property value of NetModule.CONTENT_RECORDER.
 * </p>
 */
public interface RecordingNetModule extends NetModule
{

    /**
     * Requests that a recording be scheduled on this network recording device.
     * metadata added to the NetRecordingSpec prior to calling this method will
     * be utilized by the remote device in identifying the recording or
     * recordings to be scheduled. Upon completion of this operation, a
     * NetActionEvent SHALL be delivered to the given handler indicating success
     * or failure. Upon success, values returned by calls to
     * {@link NetActionEvent#getResponse()} SHALL contain a NetRecordingEntry
     * representing the newly created recording.
     *
     * @param recordingSpec
     *            a recording spec containing the metadata used to identify the
     *            recordings to be scheduled.
     * @param handler
     *            The NetActionHandler which gets informed once this request
     *            completes.
     *
     * @return NetActionRequest to inform calling application of results.
     *
     * @throws SecurityException
     *             if the caller does not have HomeNetPermission("recording")
     *
     * @throws IllegalArgumentException
     *             if recordingSpec has an empty MetadataNode, or if
     *             MetadataNode which is associated with recordingSpec does not
     *             contain the necessary metadata entry such as
     *             scheduledChannelID, scheduledStartDateTime, scheduledDuration
     */
    NetActionRequest requestSchedule(NetRecordingSpec recordingSpec, NetActionHandler handler);

    /**
     * Requests that a recording be rescheduled on this network recording
     * device. Metadata added to the NetRecordingSpec prior to calling this
     * method will be utilized by the remote device in identifying changes the
     * recording or recordings to be rescheduled. Upon completion of this
     * operation, a NetActionEvent SHALL be delivered to the given handler
     * indicating success or failure.
     *
     * @param recording
     *            the previously scheduled RecordingContentItem or
     *            NetRecordingEntry to be rescheduled.
     * @param recordingSpec
     *            a recording spec containing the metadata used to identify the
     *            changes to recordings to be rescheduled.
     * @param handler
     *            The NetActionHandler which gets informed once this request
     *            completes.
     *
     * @return NetActionRequest to inform calling application of results.
     *
     * @throws SecurityException
     *             if the caller does not have HomeNetPermission("recording")
     *
     * @throws IllegalArgumentException
     *             if the recording parameter is neither the NetRecordingEntry
     *             with upnp:srsRecordScheduleID metadata entry nor the
     *             RecordingContentItem with upnp:srsRecordTaskID metadata entry
     *             in its own MetadataNode,or if recordingSpec has an empty
     *             MetadataNode, or if MetadataNode which is associated with
     *             recordingSpec does not contain the necessary metadata entry
     *             such as scheduledChannelID, scheduledStartDateTime,
     *             scheduledDuration
     */
    NetActionRequest requestReschedule(ContentEntry recording, NetRecordingSpec recordingSpec, NetActionHandler handler);

    /**
     * Requests that an in progress recording be disabled on this network
     * recording device. If the recording is in progress, this method requests
     * that the recording be stopped. If the recording is pending, this method
     * requests that the recording be canceled. Upon completion of this
     * operation, a NetActionEvent SHALL be delivered to the given handler
     * indicating success or failure.
     *
     * @param recording
     *            a RecordingContentItem or NetRecordingEntry that identifies
     *            the recording(s) to be canceled.
     *
     * @param handler
     *            The NetActionHandler which gets informed once this request
     *            completes.
     *
     * @return NetActionRequest to inform calling application of results.
     *
     * @throws SecurityException
     *             if the caller does not have HomeNetPermission("recording")
     *
     * @throws IllegalArgumentException
     *             if the recording parameter is neither the NetRecordingEntry
     *             with upnp:srsRecordScheduleID metadata entry nor the
     *             RecordingContentItem with upnp:srsRecordTaskID metadata entry
     *             in its own MetadataNode
     */
    NetActionRequest requestDisable(ContentEntry recording, NetActionHandler handler);

    /**
     * Requests that content associated with a scheduled recording be deleted
     * from storage. Upon completion of this operation, a NetActionEvent SHALL
     * be delivered to the given handler indicating success or failure.
     *
     * @param recording
     *            a RecordingContentItem or NetRecordingEntry that identifies
     *            the recording(s) to be deleted.
     *
     * @param handler
     *            The NetActionHandler which gets informed once this request
     *            completes.
     *
     * @return NetActionRequest to inform calling application of results.
     *
     * @throws SecurityException
     *             if the caller does not have HomeNetPermission("recording")
     */
    NetActionRequest requestDeleteService(ContentEntry recording, NetActionHandler handler);

    /**
     * Requests that metadata associated with a scheduled recording be deleted
     * from storage. Upon completion of this operation, a NetActionEvent SHALL
     * be delivered to the given handler indicating success or failure.
     *
     * @param recording
     *            a recording that identifies the recording to be deleted.
     *
     * @param handler
     *            The NetActionHandler which gets informed once this request
     *            completes.
     *
     * @return NetActionRequest to inform calling application of results.
     *
     * @throws SecurityException
     *             if the caller does not have HomeNetPermission("recording")
     */
    NetActionRequest requestDelete(ContentEntry recording, NetActionHandler handler);

    /**
     * Requests that a group of scheduled individual recordings be prioritized
     * on this network recording device. Prioritization is determined by the
     * ordering of recordings in the array of RecordingContentItems, with
     * highest priority given to the entry at element 0 in the array. Upon
     * completion of this operation, a NetActionEvent SHALL be delivered to the
     * given handler indicating success or failure.
     *
     * @param recordings
     *            a prioritized array of RecordingContentItems
     * @param handler
     *            The NetActionHandler which gets informed once this request
     *            completes.
     *
     * @return NetActionRequest to inform calling application of results.
     *
     * @throws SecurityException
     *             if the caller does not have HomeNetPermission("recording")
     */
    NetActionRequest requestPrioritize(RecordingContentItem recordings[], NetActionHandler handler);

    /**
     * Requests that a group of scheduled recording request be prioritized on
     * this network recording device, where each recording request may represent
     * one or more individual recordings on the remote device. Prioritization is
     * determined by the ordering of recordings in the array of
     * NetRecordingEntries with highest priority given to the entry at element 0
     * in the array. Upon completion of this operation, a NetActionEvent SHALL
     * be delivered to the given handler indicating success or failure.
     *
     * @param recordings
     *            a prioritized array of NetRecordingEntries
     * @param handler
     *            The NetActionHandler which gets informed once this request
     *            completes.
     *
     * @return NetActionRequest to inform calling application of results.
     *
     * @throws SecurityException
     *             if the caller does not have HomeNetPermission("recording")
     */
    NetActionRequest requestPrioritize(NetRecordingEntry recordings[], NetActionHandler handler);

}
