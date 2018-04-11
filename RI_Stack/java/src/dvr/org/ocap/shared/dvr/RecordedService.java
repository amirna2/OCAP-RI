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

package org.ocap.shared.dvr;

import javax.media.Time;

/**
 * This interface represents the recorded portion of a service that is being
 * recorded or was recorded for a period of time. As soon as recording begins a
 * recorded service will be created. If the recording request fails for any
 * reason the recording shall be stopped normally and the recorded service shall
 * be available containing however much of the service was recorded.
 * <p>
 * A RecordedService has a media time attribute that determines where playback
 * begins when the recorded service is selected on a ServiceContext. This media
 * time is persistent and is applicable for all future service selections by all
 * applications until the media time is changed with a call to setMediaTime.
 * <p>
 *
 * Note the following subinterface-specific behavior for methods defined by the
 * javax.tv.service.Service superinterface:
 * <ul>
 * <li>The hasMultipleInstances() method shall always return false.
 * <li>The getServiceType() method shall always return
 * RecordedServiceType.RECORDED_SERVICE.
 * <li>The getLocator() method shall return a locator corresponding to the
 * recorded service that is different from the locator of the originating
 * service. This locator when passed in to the SIManager.getService(..) should
 * return this RecordedService.
 * <li>The getName() method shall return a human readable string.
 * <li>RecordedServices shall not be included in service lists returned by the
 * method SIManager.filterServices(..).
 * </ul>
 */
public interface RecordedService extends javax.tv.service.Service
{
    /**
     * Gets the {@link RecordingRequest} corresponding to the RecordedService.
     *
     * @return The <code>RecordingRequest</code> for the service.
     */
    public RecordingRequest getRecordingRequest();

    /**
     * Gets the the actual duration of the content recorded. For recordings in
     * progress this will return the duration of the completed part of the
     * recording.
     *
     * @return The duration of the recording in milli-seconds.
     */
    public long getRecordedDuration();

    /**
     * Returns the MediaLocator corresponding to the RecordedService.
     *
     * @return RecordedServce MediaLocator.
     */
    public javax.media.MediaLocator getMediaLocator();

    /**
     * Set the JMF media time for the location from where the playback will
     * begin when this recorded service is selected on a ServiceContext.
     * <p>
     * If an instance of Time less than the JMF media time at the start of the
     * recorded service is set, or if this method was not called for this recorded
     * service, the playback will begin at the start of the recorded content. If
     * the instance of Time set corresponds to positive infinity or a value more
     * than the duration of the recorded content, the playback will begin at the
     * live point if recording is in progress for the recorded service. If the
     * recording is not in progress, the playback will immediately hit the
     * end-of-media.
     *
     * Note: The media time set will be applicable for all future service
     * selections by all applications.
     *
     * @param mediaTime
     *            the media time corresponding to the location from where the
     *            playback will begin when this service is selected.
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes
     *             corresponding to the RecordingRequest associated with this
     *             recorded service.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)
     */
    public void setMediaTime(Time mediaTime) throws AccessDeniedException;

    /**
     * Gets the JMF media time that was set using the method setMediaTime(..)
     *
     * @return the value of JMF media time that was set using the method
     *         setMediaTime(..), if that method was called on this
     *         RecordedService before. If the method setMediaTime was not called
     *         before, this method Should return the JMF media time
     *         corresponding to the beginning of the RecordedService.
     */
    public Time getMediaTime();

    /**
     * Gets the time when the recording was initiated.
     *
     * @return the time when the recording was initiated by the implementation.
     */
    public java.util.Date getRecordingStartTime();

    /**
     * Deletes the recorded service. The method removes the recorded service and
     * all recorded elementary streams (e.g., files and directory entries)
     * associated with the RecordedService. The corresponding recording request
     * will transition to DELETED_STATE.
     * <p>
     * If the recording request is in the IN_PROGRESS_STATE or the
     * IN_PROGRESS_INSUFFICIENT_SPACE_STATE, the implementation
     * will stop the recording before deleting the recorded service. If the
     * RecordedService was being presented when it was deleted, a
     * {@link javax.tv.service.selection.PresentationTerminatedEvent} will be
     * sent with reason SERVICE_VANISHED.
     * </p>
     *
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("delete",..) or
     *             RecordingPermission("*",..)
     */
    public void delete() throws AccessDeniedException;

    /**
     * Gets the JMF media time at the start of the recorded service.
     *
     * @return a media time
     */
    public Time getFirstMediaTime();

}
