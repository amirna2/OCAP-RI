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

package org.cablelabs.impl.manager.recording;

import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;

import org.dvb.application.AppID;
import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.RequestResolutionHandler;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.NoMoreDataEntriesException;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingPermission;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListFilter;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.dvr.storage.MediaStorageVolume;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.DisableBufferingListener;
import org.cablelabs.impl.manager.recording.RecordingManagerImpl.RecordingDisabledListener;
import org.cablelabs.impl.manager.recording.RecordingManagerImpl.RecordingRequestFAPReadFilter;
import org.cablelabs.impl.recording.RecordingInfo2;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.storage.StorageProxy;

/**
 * A <code>Manager</code> that provides the system's recording management
 * functionality.
 */
public interface RecordingManagerInterface
{
    /**
     * Construct a RecordingImpl from a RecordingInfo record.
     */
    RecordingImplInterface createRecordingImpl(RecordingInfo2 info) throws IllegalArgumentException;

    /**
     * Construct a RecordingImpl from a LocatorRecordingSpec et. al.
     */
    public RecordingImplInterface createRecordingImpl(LocatorRecordingSpec lrs);

    /**
     * Construct a RecordingImpl from a ServiceRecordingSpec et. al.
     */
    public RecordingImplInterface createRecordingImpl(ServiceRecordingSpec srs);

    /**
     * Construct a RecordingImpl from a ServiceContextRecordingSpec et. al.
     */
    public RecordingImplInterface createRecordingImpl(ServiceContextRecordingSpec scrs);

    /**
     * Invokes the {@link SpaceAllocationHandler} set with the
     * {@link OcapRecordingManager} and returns its return value. If there is no
     * <code>SpaceAllocationHandler</code> set, then the requested value is
     * returned.
     * 
     * @param volume
     *            The LogicalStorageVolume on which the reserved space is
     *            requested.
     * @param app
     *            The requesting application.
     * @param spaceRequested
     *            The new value of the reservation if the request is granted.
     * 
     * @return the space granted.
     * 
     * @see SpaceAllocationHandler#allowReservation(LogicalStorageVolume, AppID,
     *      long)
     */
    public long checkAllocation(LogicalStorageVolume volume, AppID app, long spaceRequested);

    /**
     * Creates a BufferingRequest object.
     * 
     * @param service
     *            The service to buffer.
     * @param minDuration
     *            Minimum duration in seconds to buffer.
     * @param maxDuration
     *            Maximum duration in seconds to buffer.
     * @param efap
     *            Extended file access permissions for this request. If this
     *            parameter is null, no write permissions are given to this
     *            request. Read permissions for <code>BufferingRequest</code>
     *            instances are always world regardless of read permissions set
     *            by this parameter.
     * @param cctx
     *            The callers execution context.
     * @throws IllegalArgumentException
     *             if the service parameter is not a valid <code>Service</code>,
     *             or if <code>minDuration</code> is less than
     *             {@link OcapRecordingManager#getSmallestTimeShiftDuration}, or
     *             if <code>maxDuration</code> is less than
     *             <code>minDuration</code> or if the <code>CallerContext</code>
     *             is null.
     */
    public BufferingRequest createBufferingRequest(Service service, long minDuration, long maxDuration,
            ExtendedFileAccessPermissions efap, CallerContext ctx);

    /**
     * Creates a BufferingRequest object.
     * 
     * @param service
     *            The service to buffer.
     * @param minDuration
     *            Minimum duration in seconds to buffer.
     * @param maxDuration
     *            Maximum duration in seconds to buffer.
     * @param efap
     *            Extended file access permissions for this request. If this
     *            parameter is null, no write permissions are given to this
     *            request. Read permissions for <code>BufferingRequest</code>
     *            instances are always world regardless of read permissions set
     *            by this parameter.
     * @param cctx
     *            The callers execution context.
     * @throws IllegalArgumentException
     *             if the service parameter is not a valid <code>Service</code>,
     *             or if <code>minDuration</code> is less than
     *             {@link OcapRecordingManager#getSmallestTimeShiftDuration}, or
     *             if <code>maxDuration</code> is less than
     *             <code>minDuration</code>.
     */
    public BufferingRequest createBufferingRequest(Service service, long minDuration, long maxDuration,
            ExtendedFileAccessPermissions efap);

    /**
     * Adds a buffering disabled listener to the recording manager. This
     * listener will be notified of any changes to the stack-wide buffering
     * enabled state as implied by the OcapRecordingManager
     * enable/disableBuffering() methods.
     * 
     * If the listener passed in is already registered, this method will have no
     * affect.
     * 
     * @param bdl
     *            the listener to add
     */
    public void addDisableBufferingListener(DisableBufferingListener bdl);

    /**
     * Adds a schedule disabled listener to the recording manager. This listener
     * will be notified of any changes to the schedule enabled/disabled state
     * 
     * If the listener passed in is already registered, this method will have no
     * affect.
     * 
     * @param dsl
     *            the listener to add
     */
    public void addRecordingDisabledListener(RecordingDisabledListener dsl);

    /**
     * Removes the specified buffering disabled listener from the recording
     * manager.
     * 
     * If the listener passed in is not already registered, this method will
     * have no affect.
     * 
     * @param bdl
     *            the listener to remove
     */
    public void removeDisableBufferingListener(DisableBufferingListener bdl);

    /**
     * Removes the specified schedule disabled listener from the recording
     * manager.
     * 
     * If the listener passed in is not already registered, this method will
     * have no affect.
     * 
     * @param dsl
     *            the listener to remove
     */
    public void removeRecordingDisabledListener(RecordingDisabledListener dsl);

    /**
     * queries the recording manager to determine if state wide buffering is
     * enabled.
     * 
     * @return true if buffering is enabled
     */
    public boolean isBufferingEnabled();

    /**
     * queries the recording manager to determine if state wide buffering is
     * enabled.
     * 
     * @return true if buffering is enabled
     */
    public boolean isRecordingEnabled();

    /**
     * Notifies the registered listeners when the recorded content is played
     * back
     * 
     * @param serviceContext
     * @param artificialCarouselId
     * @param pids
     */
    public void notifyPlayBackStart(final ServiceContext serviceContext, final int artificialCarouselId,
            final int[] pids);

    /**
     * Gets the smallest time-shift duration supported by the implementation.
     * This method SHALL return a value greater than zero.
     * 
     * @return The smallest time-shift duration in seconds that is supported by
     *         the implementation.
     */
    public long getSmallestTimeShiftDuration();

    /**
     * Requests the implementation to start buffering a service using
     * implementation specific time-shift storage. If successful, the service
     * will be bufferred, but audio and video presentation will not take place.
     * 
     * @param request
     *            The <@link BufferingRequest> to make active.
     * 
     * @throws SecurityException
     *             if the calling application does not have the "file" element
     *             set to true in its permission request file.
     */
    public void requestBuffering(BufferingRequest request);

    /**
     * Cancels an active buffering request. If the <@link BufferingRequest>
     * parameter is not active this method does nothing and returns
     * successfully.
     * 
     * @param request
     *            The <code>BufferingRequest</code> to cancel.
     * 
     * @throws SecurityException
     *             if the calling application does not have write permission for
     *             the request as determined by the
     *             <code>ExtendedFileAccessPermissions</code> returned by the
     *             <code>getExtendedFileAccessPermissions</code> method in the
     *             parameter, or if the calling application does not have
     *             MonitorAppPermission("handler.recording").
     */
    public void cancelBufferingRequest(BufferingRequest request);

    /**
     * Get the MediaStorageVolume that is the default storage volume location
     * for recordings. This will be called if and only if orp.getDestination
     * returns null at the start of a recording.
     * 
     * @return default MediaStorageVolume for the default device
     *         (m_defaultStorageProxy in StorageManagerImpl)
     */
    public MediaStorageVolume getDefaultMediaStorageVolume();

    /**
     * Request to get the default device
     * 
     * @return m_storageProxy The proxy representing the default device
     * 
     */
    public StorageProxy getDefaultStorageProxy();

    public RequestResolutionHandler getRequestResolutionHandler(String key);

    /**
     * Gets the list of entries maintained by the RecordingManager. This list
     * includes both parent and leaf recording requests. For applications with
     * RecordingPermission("read", "own"), only RecordingRequests of which the
     * calling application has visibility as defined by any RecordingRequest
     * specific security attributes will be returned. For applications with
     * RecordingPermission("read", "*"), all RecordingRequests will be returned.
     * 
     * @return an instance of RecordingList
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("read",..) or RecordingPermission("*",..)
     */
    public RecordingList getEntries();

    /**
     * Gets the list of recording requests matching the specified filter. For
     * applications with RecordingPermission(?read?, ?own?), only
     * RecordingRequests of which the calling application has visibility as
     * defined by any RecordingRequest specific security attributes will be
     * returned. For applications with RecordingPermission(?read?, ?*?), all
     * RecordingRequests matching the specified filter will be returned.
     * 
     * @param filter
     *            the filter to use on the total set of recording requests
     * 
     * @return an instance of RecordingList
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("read",..) or RecordingPermission("*",..)
     */
    public RecordingList getEntries(RecordingListFilter filter);

    /**
     * Adds an event listener for changes in status of recording requests. For
     * applications with RecordingPermission("read", "own"), the listener
     * parameter will only be informed of changes that affect RecordingRequests
     * of which the calling application has visibility as defined by any
     * RecordingRequest specific security attributes. For applications with
     * RecordingPermission("read", "*"), the listener parameter will be informed
     * of all changes.
     * 
     * @param rll
     *            The listener to be registered.
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("read",..) or RecordingPermission("*",..)
     */
    public void addRecordingChangedListener(RecordingChangedListener rll);

    /**
     * Removes a registed event listener for changes in status of recording
     * requests. If the listener specified is not registered then this method
     * has no effect.
     * 
     * @param rll
     *            the listener to be removed.
     */
    public void removeRecordingChangedListener(RecordingChangedListener rll);

    /**
     * This will establish what minimum interval will be used when scheduling
     * BeforeStartRecordingListener notifications. This will use either the
     * timer granularity or an property to define the minimum interval.
     * 
     * @return minimum interval to be used when scheduling
     * BeforeStartRecordingListener
     */
    public long getMinimumBeforeStartNotificationInterval();
    
    /**
     * Return true of the host is able to store content in a "host-bound" fashion. 
     * This implies that content is protected in a fashion where it cannot be transferred
     * off the host storage (see <tru2way> HOST DEVICE LICENSE AGREEMENT)
     */
    public boolean isContentHostBound();
}
