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

import org.ocap.shared.dvr.navigation.RecordingList;

/**
 * This interface represents information corresponding to a parent recording
 * request. The recording request represented by this interface may have one or
 * more child recording requests.
 * <p>
 * A parent recording request may be in the unresolved state, partially resolved
 * state, completely resolved state, or the cancelled state.
 * <p>
 * A recording request would be in the unresolved state if the implementation
 * does not have enough information to process this recording request. A
 * recording request in this state may transition to partially resolved state,
 * completely resolved state, or failed state.
 * <p>
 * A recording request would be in the partially resolved state if the
 * implementation has enough information to schedule some, but not all, child
 * recording requests corresponding to this recording request. This would be the
 * case of a recording request for a series where some episodes for the series
 * are scheduled. A recording request is in completely resolved state if all its
 * child recordings are known and scheduled.
 */
public interface ParentRecordingRequest extends RecordingRequest
{
    /**
     * The implementation does not have enough information to process this
     * recording request.
     */
    public static final int UNRESOLVED_STATE = 9;

    /**
     * All child recordings corresponding to this recording request have been
     * scheduled. A recording request is in completely resolved state, if the
     * implementation has enough information to schedule all recordings
     * corresponding to the recording request. A recording request in completely
     * resolved state would have one or more child recordings.
     */
    public static final int COMPLETELY_RESOLVED_STATE = 10;

    /**
     * Some recordings corresponding to this recording request have been
     * scheduled. A recording request is in partially resolved state, if the
     * implementation has enough information to schedule some, but not all
     * recordings corresponding to the recording request. A recording request in
     * partially resolved state may have zero, one or more child recordings.
     */
    public static final int PARTIALLY_RESOLVED_STATE = 11;

    /**
     * A recording request is in cancelled state, if an application has
     * successfully called the cancel method for this recording request, but not
     * all child recording request have been deleted. A recording request in
     * this state shall be deleted by the implementation once all child
     * recording requests have been deleted.
     */
    public static final int CANCELLED_STATE = 13;

    /**
     * Cancels the parent recording request. The implementation shall also
     * cancel all the child recording requests through calling their cancel()
     * method. If any of these calls throw an exception,
     * the process shall continue with the next child.
     * No more child recordings will be scheduled for this recording
     * request or for any of its child recordings. The recording request will be
     * deleted from the database once all child recording requests have been
     * deleted. Canceling a parent recording request does not delete any child
     * recordings that cannot be cancelled (i.e. if a child recording request is
     * not in a pending state). At the successful completion of this method the
     * recording request would be deleted from the database or changed state to
     * CANCELLED_STATE.
     *
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("cancel",..) or
     *             RecordingPermission("*",..)
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws IllegalStateException
     *             if the stateof the recording request is not in
     *             UNRESOLVED_STATE, PARTIALLY_RESOLVED_STATE or
     *             COMPLETELY_RESOLVED_STATE.
     */
    public void cancel() throws IllegalStateException, AccessDeniedException;

    /**
     * Gets all the immediate child Recordings corresponding to this parent
     * RecordingRequest. For a recording request in completely resolved state
     * this method returns all children that are still maintained in the
     * recording manager database (i.e. children removed from the database by
     * calling the delete() or cancel() method will not be included in the list
     * of child recordings). For a recording request in partially resolved state
     * this method only returns currently known children for series.
     *
     * @return The list of child Recordings corresponding to this Recording;
     *         null if there are no child recording requests in the
     *         RecordingManager database.
     * @throws IllegalStateException
     *             if the recording request is not in PARTIALLY_RESOLVED_STATE
     *             or COMPLETELY_RESOLVED_STATE.
     */
    public RecordingList getKnownChildren() throws IllegalStateException;
}
