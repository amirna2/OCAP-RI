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

package org.ocap.storage;

import org.ocap.storage.ExtendedFileAccessPermissions;
import org.davic.resources.ResourceProxy;
import java.io.IOException;

/**
 * This interface represents a persistent storage device. The current set of
 * storage proxies is queried from the {@link org.ocap.storage.StorageManager}.
 * <p>
 * A StorageProxy may contain one or more logical volumes {see @link
 * org.ocap.storage.LogicalStorageVolume}. A LogicalStorageVolume is a construct
 * for organizing files on a disk and corresponds to a directory subtree that is
 * treated as a whole for some purposes. A StorageProxy only represents the
 * application visible storage on the device, i.e. it does not include portions
 * of the device reserved for internal system use.
 * <p>
 * If this proxy represents a detachable or hot-pluggable device, the proxy is
 * not listed until the device is connected. When a storage device is attached,
 * the proxy is added to the list returned by the StorageManager and an
 * appropriate event is sent to any
 * {@link org.ocap.storage.StorageManagerListener} registered with the
 * {@link org.ocap.storage.StorageManager}. When a storage device is no longer
 * available for use (or for reactivation after being made detachable), the
 * corresponding proxy is removed from the StorageManager's list and a
 * {@link org.ocap.storage.StorageManagerEvent} is sent to any
 * StorageManagerListeners registered with the StorageManager.
 * <p>
 * StorageProxy extends ResourceProxy as an implementation convenience for
 * resource contention handling. This ResourceProxy is not meant for access by
 * applications and the StorageProxy.getClient method SHALL always return null.
 * </p>
 **/

public interface StorageProxy extends ResourceProxy
{
    /**
     * Returned by {@link #getStatus} to indicate that the device is
     * initialized, mounted and ready for use.
     **/
    public static final int READY = 0;

    /**
     * Returned by {@link #getStatus} to indicate that the device is present but
     * some other action is required before the device can be used (e.g.,
     * {@link org.ocap.storage.DetachableStorageOption#makeReady}).
     **/
    public static final int OFFLINE = 1;

    /**
     * Returned by {@link #getStatus} to indicate that the device is busy, e.g.,
     * being initialized, configured, checked for consistency or being made
     * ready to detach. This value is not used to indicate that the device is
     * currently reading or writing data.
     **/
    public static final int BUSY = 2;

    /**
     * Returned by {@link #getStatus} to indicate that the device that has been
     * plugged in is not supported by the platform.
     **/
    public static final int UNSUPPORTED_DEVICE = 3;

    /**
     * Returned by {@link #getStatus} to indicate that although the device is a
     * supported type and model, it currently has a format, e.g., partitions or
     * filesystems, that is not usable by the platform without reinitialization
     * and the loss of the existing contents.
     **/
    public static final int UNSUPPORTED_FORMAT = 4;

    /**
     * Returned by {@link #getStatus} to indicate that the device is completely
     * uninitialized and contains no existing data. It must be initialized by
     * calling the initialize method to make the device is usable.
     **/
    public static final int UNINITIALIZED = 5;

    /**
     * Returned by {@link #getStatus} to indicate that the device is in an
     * unrecoverable error state and cannot be used.
     **/
    public static final int DEVICE_ERROR = 6;

    /**
     * Returned by {@link #getStatus} to indicate that a detected storage device
     * bay does not contain a removable storage device, i.e.
     * <code>StorageProxy</code> containing a {@link RemovableStorageOption}.
     **/
    public static final int NOT_PRESENT = 7;

    /**
     * Gets the storage device name assigned by the implementation. This name
     * must be unique across all storage devices. The name can be used to
     * determine equality between two storage devices, but does not contain path
     * information.
     * 
     * @return The name of the resource represented by the proxy.
     **/
    public String getName();

    /**
     * Gets a storage device name that can be displayed to a user for selection.
     * The implementation must keep this name at or below 40 characters in
     * length. This name should match naming conventions displayed to the
     * consumer via any implementation specific setup and configuration menus.
     * 
     * @return The display name of the resource represented by the proxy.
     **/
    public String getDisplayName();

    /**
     * Gets the array of storage device options (e.g.,
     * {@link org.ocap.storage.DetachableStorageOption}).
     * 
     * @return The array of StorageOptions associated with this StorageProxy.
     **/
    public StorageOption[] getOptions();

    /**
     * Returns the status of the storage device. An application can be notified
     * of changes in the status of storage proxies by registering a
     * {@link org.ocap.storage.StorageManagerListener} with
     * {@link org.ocap.storage.StorageManager#addStorageManagerListener
     * StorageManager.addStorageManagerListener() }.
     **/
    public int getStatus();

    /**
     * Gets the total storage capacity of the device in bytes. Storage that is
     * reserved for system use is not included in this number.
     * 
     * @return Total storage capacity in bytes.
     **/
    public long getTotalSpace();

    /**
     * Gets the available storage capacity in bytes. The value returned may
     * already have changed by the time this method returns because other
     * applications or the system may be writing files, deleting files, or
     * otherwise allocating space.
     * 
     * @return Available storage capacity in bytes.
     **/
    public long getFreeSpace();

    /**
     * Gets the set of logical volumes present on the StorageProxy. If a
     * StorageProxy has no logical volumes present, one or more must be created
     * before the device may be used for application storage.
     * 
     * @return The partitioned storage volumes.
     **/
    public LogicalStorageVolume[] getVolumes();

    /**
     * Gets the permissions supported by this storage device.
     * 
     * @return An array of booleans indicating which access rights are supported
     *         where location 0 is world read access right, 1 is world write
     *         access right, 2 is application read access right, 3 is
     *         application write access right, 4 is application's organization
     *         read access right, 5 is application's organization write access
     *         right, 6 is other organization read access right, and 7 is other
     *         organization write access right. If the boolean for one of the
     *         access rights is true, the storage device supports it, otherwise
     *         the storage device does not support that access right.
     */
    public boolean[] getSupportedAccessRights();

    /**
     * Allocates a general purpose {@link org.ocap.storage.LogicalStorageVolume}
     * . A general purpose volume can be accessed through file locators and
     * java.io with the absolute path retrieved from
     * {@link org.ocap.storage.LogicalStorageVolume#getPath()}. Specialized
     * storage proxies may support other types of volumes, such as media volumes
     * used to store DVR content. The volume is owned by the application that
     * allocated it (see {@link #deleteVolume}).
     * 
     * @param name
     *            Name of the new LogicalStorageVolume. Must be unique on this
     *            StorageProxy.
     * @param fap
     *            Application access permissions of the new
     *            LogicalStorageVolume. Applies to the last directory in the
     *            path returned by getPath, which is equivalent to the name
     *            parameter.
     * 
     * @return Allocated volume storage proxy.
     * 
     * @throws IllegalArgumentException
     *             if the name does not meet the from specified by chapter 16
     *             section regarding Files and File Names, or if the name is not
     *             unique, or if the storage device does not support an access
     *             permission specified in the fap parameter.
     * @throws IOException
     *             if the storage device represented by the StorageProxy is
     *             read-only based on a hardware constraint.
     * 
     * @throws SecurityException
     *             if the calling application does not have persistent storage
     *             permission as requested by its permission request file.
     **/
    public LogicalStorageVolume allocateGeneralPurposeVolume(String name, ExtendedFileAccessPermissions fap)
            throws IOException;

    /**
     * Deletes a {@link org.ocap.storage.LogicalStorageVolume}. Only the owning
     * application or a privileged application with
     * MonitorAppPermission("storage") may delete a volume. This causes all of
     * the file and directories within the volume to be destroyed.
     * 
     * @param vsp
     *            LogicalStorageVolume to delete.
     * 
     * @throws SecurityException
     *             if the calling application is not the owner of the volume or
     *             an application with MonitorAppPermission("storage").
     **/
    public void deleteVolume(LogicalStorageVolume vsp);

    /**
     * Initializes the StorageProxy for use. This method is usually invoked on
     * the proxy for a newly attached storage device which is not currently
     * suitable for use, e.g., is in the {@link #UNSUPPORTED_FORMAT} state. It
     * is only required to be effective on detachable storage devices, but may
     * be implemented for other types of devices as well. Successful invocation
     * of this method destroys all application visible contents of the device
     * and should not be called unless the application has determined, e.g., by
     * prompting the user, that it is safe to do so. If the StorageProxy was in
     * the {@link #READY} state and has storage visible to the application,
     * access to that storage is removed and the StorageProxy enters the
     * {@link #BUSY} state until this method returns.
     * 
     * @param userAuthorized
     *            True if the application has received authorization from the
     *            user for the destruction of the contents of this device. The
     *            implementation may use this to determine whether it needs to
     *            perform additional user prompting.
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("storage").
     * 
     * @throws IllegalStateException
     *             if the system is unable to initialize the storage device. If
     *             the device was in the {@link #UNINITIALIZED} state and the
     *             error is permanent, the StorageProxy status is set to
     *             {@link #DEVICE_ERROR}.
     **/
    void initialize(boolean userAuthorized);
}
