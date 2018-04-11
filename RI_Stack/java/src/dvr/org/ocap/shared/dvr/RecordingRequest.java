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

import org.dvb.application.AppID;

/**
 * This interface represents information corresponding to a recording
 * request. The recording request represented by this interface may
 * correspond to a single recording request or a series of other recording
 * requests.  Recording requests are hierarchical in nature. Implementations
 * may resolve a recording request to a single recording request or to a
 * series of other recording requests each of which may get further resolved
 * into a single recording request or a series of recording requests.  For
 * example, a recording request for "Sex and the City" may resolve to multiple
 * recording requests, each for a particular season of the show. Each of these
 * recording requests may get further resolved into multiple recording requests,
 * each for a single episode.  The implementation creates a recording request
 * in response to the RecordingManager.record(RecordingSpec) method. The
 * implementation also creates recording requests when a recording request is
 * further resolved.
 *<p>
 * A recording request may either be a parent recording request or a leaf
 * recording request.  States for a recording request are defined in
 * {@link ParentRecordingRequest} and {@link LeafRecordingRequest}.  A
 * recording request may be in any of the states corresponding to a leaf
 * recording request or a parent recording request.
 * <p>
 */
public interface RecordingRequest
{
    /**
     * Returns the state of the recording request.
     *
     * @return State of the recording request.
     */
    public int getState();

    /**
     * Checks whether the recording request was a root recording request
     * generated when the application called the RecordingManager.record(..)
     * method. The implementation should create a root recording request
     * corresponding to each successful call to the record method.
     *
     * @return True, if the recording request is a root recording request, false
     *         if the recording request was generated during the process of
     *         resolving another recording request.
     */
    public boolean isRoot();

    /**
     * Gets the root recording request corresponding to this recording request.
     * A root recording request is the recording request that was returned when
     * the application called the RecordingManager.record(..) method.
     * <p>
     * If the current recording request is a root recording request, the current
     * recording request is returned.
     *
     * @return the root recording request for this recording request, null if
     *         the application does not have read accesss permission for the
     *         root recording request.
     */
    public RecordingRequest getRoot();

    /**
     * Gets the parent recording request corresponding to this recording
     * request.
     *
     * @return the parent recording request for this recording request, null if
     *         the application does not have read accesss permission for the
     *         parent recording request or if this recording request is the root
     *         recording request.
     */
    public RecordingRequest getParent();

    /**
     * Returns the RecordingSpec corresponding to the recording request. This
     * will be either the source as specified in the call to the record(..)
     * method which caused this recording request to be created or the
     * RecordingSpec generated by the system during the resolution of the
     * original application specified RecordingSpec. Any modification to the
     * RecordingSpec due to any later calls to the SetRecordingProperties
     * methods on this instance will be reflected on the returned RecordingSpec.
     * <p>
     * When the implementation generates a recording request while resolving
     * another recording request, a new instance of the RecordingSpec is created
     * with an identical copy of the RecordingProperties of the parent recording
     * request.
     *
     * @return a RecordingSpec containing information about this recording
     *         request.
     */
    public RecordingSpec getRecordingSpec();

    /**
     * Modify the RecordingProperties corresponding to the RecordingSpec for
     * this recording request. Applications may change any properties associated
     * with a recording request by calling this method. Changing the properties
     * may result in changes in the states of this recording request. Changing
     * the properties of a parent recording request will not automatically
     * change the properties of any of its child recording requests that are
     * already created. Any child recording requests created after the
     * invocation of this method will inherit the new values for the properties.
     *
     * @param properties
     *            the new recording properties to set.
     *
     * @throws IllegalStateException
     *             if changing one of the parameters that has been modified in
     *             the new recording properties is not legal for the current
     *             state of the recording request.
     *
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)
     */
    public void setRecordingProperties(RecordingProperties properties) throws IllegalStateException,
            AccessDeniedException;

    /**
     * Deletes the recording request from the database. The method removes the
     * recording request, all its descendant recording requests, as well as the
     * corresponding {@link RecordedService} objects and all recorded elementary
     * streams (e.g., files and directory entries) associated with the
     * RecordedService. If any application calls any method on stale references
     * of removed objects the implementation shall throw an
     * IllegalStateException.
     * <p>
     * If the recording request is in any of the IN_PROGRESS states the implementation
     * will stop the recording before deleting the recording request. If a
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
     * Add application specific private data. If the key is already in use, the
     * data corresponding to key is overwritten.
     * All applications which can obtain a reference to a recording
     * request shall have access to the same set of application specific data.
     *
     * @param key
     *            the ID under which the data is to be added
     * @param data
     *            the data to be added
     *
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)
     *
     * @throws IllegalArgumentException
     *             if the size of the data is more than the size supported by
     *             the implementation. The implementation shall support at least
     *             256 bytes of data.
     * @throws NoMoreDataEntriesException
     *             if the recording request is unable to store any more
     *             Application data. The implementation shall support atleast 16
     *             data entries per recording request.
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     */
    public void addAppData(String key, java.io.Serializable data) throws NoMoreDataEntriesException,
            AccessDeniedException;

    /**
     * Gets the application identifier of the application that owns this
     * recording request. The owner of a root recording request is the
     * application that called the RecordingManager.record(..) method. The owner
     * of a non-root recording request is the owner of the root for the
     * recording request.
     *
     * @return Application identifier of the owning application.
     */
    public AppID getAppID();

    /**
     * Get all keys for Application specific data associated with
     * this recording request.
     *
     * @return All keys corresponding to the RecordingRequest; Null if there if
     *         no application data.
     */
    public String[] getKeys();

    /**
     * Get application data corresponding to specified key.
     * All applications which can obtain a reference to a recording
     * request shall have access to the same set of application specific data.
     *
     * @param key
     *            the key under which any data is to be returned
     *
     * @return the application data corresponding to the specified key; Null if
     *         there if no data corresponding to the specified key.
     */
    public java.io.Serializable getAppData(String key);

    /**
     * Remove Application specific private data corresponding to the specified
     * key. This method exits silently if there was no data corresponding to the
     * key.
     *
     * @param key
     *            the key under which data is to be removed
     *
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)
     */
    public void removeAppData(String key) throws AccessDeniedException;

    /**
     * Modify the details of a recording request. The recording request shall be
     * re-evaluated based on the newly provided RecordingSpec. Rescheduling a
     * root recording request may result in state transitions for the root
     * recording request or its child recording requests. Rescheduling a root
     * recording request may also result in the scheduling of one or more new
     * child recording requests, or the deletion of one or more pending child
     * recording requests.
     * <p>
     * Note: If the recording request or one of its child recording request is
     * in IN_PROGRESS_STATE or IN_PROGRESS_INSUFFICIENT_SPACE_STATE, any changes
     * to the start time shall be ignored. In this case all other valid
     * parameters are applied. If the new value for a parameter is not valid
     * (e.g. the start-time and the duration is in the past), the implementation
     * shall ignore that parameter. In-progress recordings shall continue
     * uninterrupted, if the new recording spec does not request the recording
     * to be stopped.
     *
     * @param newRecordingSpec
     *            the new recording spec that shall be used to reschedule the
     *            root RecordingRequest.
     *
     * @throws IllegalArgumentException
     *             if the new recording spec and the current recording spec for
     *             the recording request are different sub-classes of
     *             RecordingSpec.
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)*
     */
    public void reschedule(RecordingSpec newRecordingSpec) throws AccessDeniedException;

    /**
     * Returns an identifier for this recording request. The identifier shall
     * uniquely identify this recording request among all others in the GEM
     * recording terminal. The identifier shall be permanently associated with
     * this recording request as long as this recording request remains in the
     * GEM recording terminal and in particular shall survive power to the GEM
     * recording terminal being interrupted. This is to enable applications to
     * store these IDs in persistent storage for later retrieval by another
     * application or another instance of the same application.
     * <p>
     * Since identifiers may be held in persistent storage by applications,
     * implementations should not re-use identifiers of recording requests which
     * are no longer held in the GEM recording terminal as this would confuse
     * applications which still have references to those recording requests in
     * their persistent storage.
     *
     * @see RecordingManager#getRecordingRequest
     * @return an identifier
     */
    public int getId();
}
