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

import java.net.InetAddress;
import org.ocap.hn.content.ContentEntry;

/**
 * <P>
 * A class implementing this interface processes recording requests received
 * from devices on the home network.
 * </P>
 * <P>
 * An application which has a class implementing this interface may set an
 * instance of it in a local RecordingNetModule. It is up to the application to
 * interpret metadata associated with NetRecordingSpecs and
 * RecordingContentItems delivered in the callback methods, and translate these
 * requests into local DVR recordings.
 * </P>
 */
public interface NetRecordingRequestHandler
{

    /**
     * Notifies this NetRecordingRequestHandler that a device on the home
     * network has requested that a recording be scheduled. Handler applications
     * MAY inspect any metadata associated with the NetRecordingEntry passed
     * with the method invocation, and translate such metadata in one or more
     * local DVR recordings. Applications SHOULD associate such recordings with
     * the NetRecordingEntry by adding the recordings to the entry using the
     * <i>NetRecordingEntry.addRecordingContentItem() </i> method.
     * 
     * @param address
     *            IP address of the device on the home network which has issues
     *            this request
     * @param spec
     *            the NetRecordingEntry which describes the requested recording
     * 
     * @return true if the schedule request can be successfully processed, or
     *         false if the request will not be processed.
     * 
     * @see NetRecordingEntry#addRecordingContentItem(RecordingContentItem)
     */
    boolean notifySchedule(InetAddress address, NetRecordingEntry spec);

    /**
     * Notifies this NetRecordingRequestHandler that a device on the home
     * network has requested that a recording be rescheduled. Handler
     * applications MAY inspec any metadata contained within the
     * NetRecordingEntry passed into this method, and utilize such metadata to
     * reschedule the local DVR recording represented by the given ContentEntry.
     * This ContentEntry may represent an individual recording as a
     * RecordingContentItem, or may represent a collection of recordings
     * contained within a NetRecordingEntry object.
     * 
     * @param address
     *            the IP address of the device on the home network which has
     *            issues this request
     * @param recording
     *            the RecordingContentItem or NetRecordingEntry to be
     *            rescheduled
     * @param spec
     *            the NetRecordingEntry object containing the metadata to be
     *            used to reschedule the recording
     * 
     * @return true if the reschedule request can be successfully processed,or
     *         false if the request will not be processed.
     */
    boolean notifyReschedule(InetAddress address, ContentEntry recording, NetRecordingEntry spec);

    /**
     * Notifies this NetRecordingRequestHandler that a device on the home
     * network has requested that a recording be disabled. If the recording is
     * in progress, this is a request to stop the recording. If the recording is
     * pending, this is a request to cancel the recording. Applications MAY
     * cancel or stop the given recording in response to this request.
     * 
     * @param address
     *            the IP address of the device on the home network which has
     *            issues this request
     * @param recording
     *            the RecordingContentItem or RecordingNetEntry to be disabled
     * 
     * @return true if the disable request can be successfully processed,or
     *         false if the request will not be processed.
     */
    boolean notifyDisable(InetAddress address, ContentEntry recording);

    /**
     * Notifies this NetRecordingRequestHandler that a device on the home
     * network has requested that metadata associated with a recording be
     * deleted. Applications MAY delete the given recording in response to this
     * request.
     * 
     * @param address
     *            the IP address of the device on the home network which has
     *            issues this request
     * @param recording
     *            the RecordingContentItem or NetRecordingEntry to be deleted
     * 
     * @return true if the delete request can be successfully processed,or false
     *         if the request will not be processed.
     */
    boolean notifyDelete(InetAddress address, ContentEntry recording);

    /**
     * Notifies this NetRecordingRequestHandler that a device on the home
     * network has requested that content associated with a recorded service be
     * deleted. Applications MAY delete the content associated with the given
     * recording in response to this request.
     * 
     * @param address
     *            the IP address of the device on the home network which has
     *            issues this request
     * @param recording
     *            requested the RecordingContentItem or NetRecordingEntry
     * 
     * @return true if the delete request can be successfully processed,or false
     *         if the request will not be processed.
     */
    boolean notifyDeleteService(InetAddress address, ContentEntry recording);

    /**
     * Notifies this NetRecordingRequestHandler that a device on the home
     * network has requested that a group of recordings be re-prioritized. The
     * requested prioritization is represented by the ordering of the
     * NetRecordingEntry objects in the given array, with the highest priority
     * at index 0 of the array. Applications MAY prioritize some or all of the
     * local DVR recordings contained within the NetRecordingEntry array.
     * 
     * @param address
     *            the IP address of the device on the home network which has
     *            issues this request
     * @param recordings
     *            the NetRecordingEntrys to be prioritized
     * 
     * @return true if the prioritization request can be successfully
     *         processed,or false if the request will not be processed.
     */
    boolean notifyPrioritization(InetAddress address, NetRecordingEntry recordings[]);

    /**
     * Notifies this NetRecordingRequestHandler that a device on the home
     * network has requested that a group of individual recordings be
     * re-prioritized. The requested prioritization is represented by the
     * ordering of the RecordingContentItem objects in the given array, with the
     * highest priority at index 0 of the array. Applications MAY prioritize the
     * local DVR recordings contained within the RecordingContentItem array.
     * 
     * @param address
     *            IP address of the device on the home network which has issued
     *            this request.
     * @param recordings
     *            The recording content items associated with the recordings to
     *            be prioritized.
     * 
     * @return True if the prioritization request can be successfully
     *         processed,or false if the request will not be processed.
     */
    boolean notifyPrioritization(InetAddress address, RecordingContentItem recordings[]);
}
