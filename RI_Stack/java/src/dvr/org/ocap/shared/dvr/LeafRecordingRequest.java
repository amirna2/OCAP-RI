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

/**
 * This interface represents information corresponding to a leaf level recording
 * request. The recording request represented by this interface corresponds to a
 * recording request that has been completely resolved to a single recording.
 *<p>
 * A leaf recording request may be pending (i.e. waiting for the start-time to
 * occur), in-progress, completed, incomplete, or failed.
 * <p>
 * While in pending state, a recording request may be in conflict for resources
 * with other recordings. Any such conflicts must be resolved before the
 * scheduled start time of the recording, if not, the pending recording request
 * is expected to result in a failed recording.
 */
public interface LeafRecordingRequest extends RecordingRequest
{
    /**
     * The recording request is Pending. Recording for this request is expected
     * to complete successfully.
     */
    public static final int PENDING_NO_CONFLICT_STATE = 1;

    /**
     * The recording request may not be intiated due to resource conflicts. The
     * implementation has detected a resource conflict for the scheduled time of
     * this recording request and the current resolution of the conflict does
     * not allow this recording request to be initiated successfully.
     */
    public static final int PENDING_WITH_CONFLICT_STATE = 2;

    /**
     * Recording has been initiated for this recording request and is ongoing.
     * Recording is expected to complete successfully.
     */
    public static final int IN_PROGRESS_STATE = 4;

    /**
     * Recording has been initiated for this recording request and is ongoing,
     * but the implementation has detected that storage space may not be
     * sufficient to complete the recording.
     */
    public static final int IN_PROGRESS_INSUFFICIENT_SPACE_STATE = 5;

    /**
     * Recording for this recording request was initiated but failed.
     */
    public static final int INCOMPLETE_STATE = 6;

    /**
     * Recording for this recording request has completed successfully.
     */
    public static final int COMPLETED_STATE = 7;

    /**
     * The recording request has failed.
     */
    public static final int FAILED_STATE = 8;

    /**
     * The recorded service corresponding to this recording request has been
     * deleted.
     */
    public static final int DELETED_STATE = 14;

    /**
     * The recording request is in progress but recording cannot take place due
     * to some error; e.g. lack of resources.
     */
    public static final int IN_PROGRESS_WITH_ERROR_STATE = 15;

    /**
     * The recording request is in progress and recording, but cannot be
     * completed because it was started after the start_time or it was
     * interrupted and re-started.
     */
    public static final int IN_PROGRESS_INCOMPLETE_STATE = 16;

    /**
     * Cancels a pending recording request. If this method completes
     * successfully without throwing an exception then the recording request
     * will have been deleted from the database. Cancelling a recording request
     * may resolve one or more conflicts. In this case some pending recordings
     * with conflicts would be changed to pending without conflicts.
     * 
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("cancel",..) or
     *             RecordingPermission("*",..)
     * @throws IllegalStateException
     *             if the stateof the recording is not in
     *             PENDING_NO_CONFLICT_STATE or PENDING_WITH_CONFLICT_STATE.
     */
    public void cancel() throws IllegalStateException, AccessDeniedException;

    /**
     * Stops the recording for an in-progress recording request regardless of how
     * much of the duration has been recorded.  Moves the recording to the INCOMPLETE_STATE
     * in the event any duration has been recorded, to the FAILED_STATE in the event 
     * no duration has been recorded, or to the COMPLETED_STATE in the event the entire 
     * duration has been recorded.
     * 
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("cancel",..) or
     *             RecordingPermission("*",..)
     * @throws IllegalStateException
     *             if the recording is not in the IN_PROGRESS_STATE, or
     *             IN_PROGRESS_INSUFFICIENT_SPACE_STATE or
     *             IN_PROGRESS_WITH_ERROR_STATE, or
     *             IN_PROGRESS_INCOMPLETE_STATE..
     */
    public void stop() throws IllegalStateException, AccessDeniedException;

    /**
     * Gets the exception that caused the recording request to enter the
     * <code>FAILED_STATE</code>, or <code>INCOMPLETE_STATE</code> or
     * <code>IN_PROGRESS_WITH_ERROR_STATE</code>, or
     * <code>IN_PROGRESS_INCOMPLETE_STATE</code>.
     * 
     * @return The exception that caused the failure. The exception returned
     *         will be a RecordingFailedException.
     * 
     * 
     * @throws IllegalStateException
     *             if the recording request is not in the FAILED_STATE or
     *             INCOMPLETE_STATE or IN_PROGRESS_WITH_ERROR_STATE, or
     *             IN_PROGRESS_INCOMPLETE_STATE.
     */
    public Exception getFailedException() throws IllegalStateException;

    /**
     * Returns the {@link SegmentedRecordedService} corresponding to the
     * recording request.
     * 
     * @return The recorded service associated with the recording request.
     * @throws IllegalStateException
     *             if the recording request is not in INCOMPLETE_STATE,
     *             IN_PROGRESS_STATE, IN_PROGRESS_INSUFFICIENT_SPACE_STATE,
     *             IN_PROGRESS_WITH_ERROR_STATE, IN_PROGRESS_INCOMPLETE_STATE,
     *             or COMPLETED_STATE.
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     */
    public SegmentedRecordedService getService() throws IllegalStateException, AccessDeniedException;

    /**
     * Gets detailed information about the deletion of the recorded service
     * corresponding to this recording request.
     * 
     * @return The deletion details for this recording request.
     * 
     * @throws IllegalStateException
     *             if the recording request is not in the DELETED_STATE.
     */
    public DeletionDetails getDeletionDetails() throws IllegalStateException;

}
