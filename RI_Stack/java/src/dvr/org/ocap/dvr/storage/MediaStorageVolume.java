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

package org.ocap.dvr.storage;

import org.ocap.storage.LogicalStorageVolume;

/**
 * This interface represents a media volume on a storage device and is contained
 * within a {@link org.ocap.storage.StorageProxy}. A MediaStorageVolume is a
 * specialized LogicalStorageVolume that supports the recording and playback of
 * media content through the DVR. The volume also provides a mechanism for
 * allocating a fixed amount of storage for use by recordings on the volume,
 * as well as minimum time-shift buffer storage size.
 */
public interface MediaStorageVolume extends LogicalStorageVolume
{
    /**
     * Allocates the specified amount of storage from the containing
     * StorageProxy for use by recordings made to this volume. The volume is
     * guaranteed to be able to use this amount of storage without requiring the
     * deletion of the contents of other volumes and is also limited to using no
     * more than the allocated amount of storage. The amount of space allocated
     * may be rounded up to meet platform requirements. Once the storage on the
     * volume reaches the amount allocated, the behavior is the same as if the
     * storage device were full, e.g. a SpaceFullException is thrown or a
     * RecordingAlertEvent generated.
     * <p>
     * A value of zero indicates that the volume has no minimum guaranteed size
     * and may also use as much space as is available. Until set with the
     * allocate() method, the space allocated is zero.
     * <p>
     * Subsequent calls to allocate() change the existing allocation. However,
     * if a new allocation size is too small to contain existing recordings a
     * IllegalArgumentException is thrown and the allocation size is not
     * changed. Except when the allocation size is changed to zero which removes
     * the limit and the guaranteed storage size. The allocated space can only
     * be released by an explicit call to allocate() or through the deletion of
     * the storage volume.
     * <p>
     *
     * @param bytes
     *            Number of bytes to allocate.
     *
     * @throws SecurityException
     *             if the calling application does not have
     *             MonAppPermission("storage") permission.
     * @throws IllegalArgumentException
     *             if the requested amount of storage exceeds the amount
     *             available for allocation, or reduces the previous allocation
     *             making it too small for existing recordings.
     */
    public void allocate(long bytes);

    /**
     * Gets the amount of space allocated on this volume. If the allocate method
     * has not been called for the volume this method returns 0.
     *
     * @return Number of bytes allocated.
     */
    public long getAllocatedSpace();

    /**
     * Gets the remaining available space from an allocation after accounting
     * for all used space (including recordings, time shift buffers, and
     * meta-data). If no allocated
     * space has been used, this method returns the same value as the
     * getAllocatedSpace method. When this method is called on a
     * MediaStorageVolume without an explicit allocation, as is the case when
     * allocate has not been called or was called with a value of 0, then the
     * value returned is the space available on the associated StorgeProxy's
     * MEDIAFS that has not been explicitly allocated to another
     * MediaStorageVolume or TimeShiftBufferOption.
     *
     * @return Number of bytes available for use from an allocation.
     */
    public long getFreeSpace();

    /**
     * Adds a list of Organization strings to the set of organizations that are
     * allowed to use this volume.  The volume is owned by the application
     * that created the volume but is accessible to any record requests
     * where the Organization string matches one of the strings in the
     * organization array.
     * <p>
     * Note: Given Organization strings should represent an application's
     * <code>organization_id</code>, formatted as would be found in the
     * OrganizationName field of the application's leaf certificate.
     * That is, the {@link org.dvb.application.AppID#getOID organization_id}
     * as a fixed length 8 character hexadecimal string (with leading zeros
     * where required).
     *
     * @param organizations
     *            An array of strings representing organizations that are
     *            allowed to use this volume. The String passed as a parameter
     *            to the record method should match one of this strings to
     *            record onto this volume. If an array of length 0 is passed,
     *            any application can use this volume. If null is passed in and
     *            all access to this volume has been removed by a call to the
     *            <code>removeAccess</code> method, then access is restored to
     *            the same organizations that had access before all access was
     *            removed.
     *
     * @throws SecurityException
     *             if the calling application is not the owner of the volume and
     *             does not have MonitorAppPermission("storage") permission.
     */

    public void allowAccess(String[] organizations);

    /**
     * Removes an Organization from the list of Organization who are allowed to
     * use this volume. When application access to the volume is in progress
     * from either the Java I/O (java.io package) or recording manager
     * (org.ocap.dvr, org.ocap.shared.dvr packages) APIs and that application's
     * access is removed by this method, the implementation SHALL terminate the
     * reads or writes immediately and generate the appropriate response, e.g.
     * IOException, interrupted recording request. This includes all forms of
     * access from those APIs including file I/O, service recording, and
     * recording playback.
     *
     * @param organization
     *            A string representing an organization that should be removed
     *            from the list of allowed organizations. Passing in null
     *            removes all application access to this volume.
     *
     * @throws SecurityException
     *             if the calling application is not the owner and the calling
     *             application does not have MonitorAppPermission("storage")
     *             permission.
     */
    public void removeAccess(String organization);

    /**
     * Returns the list of Organizations who are allowed to use this volume. The
     * volume is owned by the application that created the volume but is
     * accessible to any record requests where the Organization string matches
     * one of the strings in the organization array.
     *
     * @return An array of strings representing organizations that are allowed
     *         to use this volume.
     */
    public String[] getAllowedList();

    /**
     * Adds a listener that is notified when available free space is less than a
     * specified level. The parameter level is a percentage of the total
     * available space in the volume. For example, a level of 10 would cause the
     * listener to be notified when less than 10% of the volume is available for
     * use. Determination of the level is implementation specific and the
     * listener is notified whenever the threshold indicated by the level is
     * crossed and available storage is less than the level parameter
     *
     * @param listener
     *            The listener to be added.
     * @param level
     *            The level of free space remaining at which to notify the
     *            listener.
     */
    public void addFreeSpaceListener(FreeSpaceListener listener, int level);

    /**
     * Removes a free space listener. If the parameter listener was not
     * previously added or has already been removed this method does nothing
     * successfully.
     *
     * @param listener
     *            The listener to remove.
     */
    public void removeFreeSpaceListener(FreeSpaceListener listener);

    /**
     * Gets the minimum storage space size for time-shift buffer use.  If the
     * <code>setMinimumTSBSize</code> method has been called then the value
     * set SHALL be returned.  If the <code>setMinimumTSBSize</code> method
     * has not been called but a minimum time-shift buffer size has been
     * configured by the implementation then that value SHALL be returned.
     * Otherwise, if neither an application nor the implementation has set the
     * value then 0 SHALL be returned.  The implementation SHALL NOT override
     * a value set by an application.
     *
     * @return The minimum time-shift buffer size.
     */
    public long getMinimumTSBSize();

    /**
     * Sets the minimum storage space for time-shift buffer use.  The
     * implementation SHALL make at least the minimum storage set by this
     * method available to satisfy the requirements of
     * <code>TimeShiftProperties.setMinimumDuration</code>.  Storage
     * allocated by a call to this method SHALL NOT be used for scheduled
     * recordings.  This method SHALL NOT affect any existing recorded content.
     * If the specified size is too large for the MSV to accommodate existing permanent
     * recordings, an IllegalArgumentException is thrown and the minimum TSB allocation
     * is not changed.
     *
     * @param size The size in bytes of the minimum time-shift buffer storage
     *      to set.
     *
     * @throws IllegalArgumentException if size > getFreeSpace() + current TSB size.
     * @throws SecurityException if the calling application does not have
     *      MonitorAppPermission("storage") permission.
     */
    public void setMinimumTSBSize(long size);
}
